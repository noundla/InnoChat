package com.inno.innochat.xmpp

import org.jivesoftware.smack.XMPPConnection
import android.content.*
import android.preference.PreferenceManager
import android.support.v4.content.LocalBroadcastManager
import android.text.TextUtils
import android.util.Log
import com.inno.innochat.Constants
import com.inno.innochat.model.MessagingModel
import com.inno.innochat.model.UsersModel
import org.jivesoftware.smack.SmackException
import org.jxmpp.stringprep.XmppStringprepException
import org.jxmpp.jid.impl.JidCreate
import org.jxmpp.jid.EntityBareJid
import org.jivesoftware.smack.ReconnectionManager
import org.jivesoftware.smack.chat2.IncomingChatMessageListener
import org.jivesoftware.smack.tcp.XMPPTCPConnection
import org.jivesoftware.smack.ConnectionConfiguration
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration
import org.jivesoftware.smack.XMPPException
import org.jivesoftware.smack.ConnectionListener
import org.jivesoftware.smack.chat2.Chat
import org.jivesoftware.smack.chat2.ChatManager
import org.jivesoftware.smack.packet.Message
import org.jivesoftware.smack.packet.Presence
import org.jivesoftware.smack.packet.Stanza
import org.jivesoftware.smack.roster.Roster
import org.jivesoftware.smack.roster.RosterEntry
import org.jivesoftware.smack.roster.RosterListener
import org.jivesoftware.smackx.receipts.DeliveryReceiptManager
import org.jivesoftware.smackx.receipts.DeliveryReceiptRequest
import org.jivesoftware.smackx.receipts.ReceiptReceivedListener
import org.jxmpp.jid.Jid
import java.io.IOException
import java.net.InetAddress
/**
 * This class is used to initiate the XMPP connection and handle the users/presence change.
 * All the xmpp calls will be placed here.
 *
 * @author Sandeep Noundla
 * */
class InnoChatConnection(context: Context) : ConnectionListener {
    companion object {
        private val TAG = "InnoChatConnection"
    }
    private val mApplicationContext: Context
    private val mUsername: String
    private val mPassword: String?
    //private val mServiceName: String
    private var mConnection: XMPPTCPConnection? = null
    private var mRoster : Roster? = null
    private var mChatManager: ChatManager? = null
    private var uiThreadMessageReceiver: BroadcastReceiver? = null//Receives messages from the ui thread.
    private var mDeliveryReceiptManager : DeliveryReceiptManager? = null;

    enum class ConnectionState {
        CONNECTED, AUTHENTICATED, CONNECTING, DISCONNECTING, DISCONNECTED
    }

    enum class LoggedInState {
        LOGGED_IN, LOGGED_OUT
    }


    init {
        Log.d(TAG, "InnoChatConnection Constructor called.")
        mApplicationContext = context.applicationContext
        val jid = PreferenceManager.getDefaultSharedPreferences(mApplicationContext)
                .getString(Constants.SP_USER_NAME, "")
        mPassword = PreferenceManager.getDefaultSharedPreferences(mApplicationContext)
                .getString(Constants.SP_PASSWORD, null)

        mUsername = if (!TextUtils.isEmpty(jid)) {
            jid.split("@".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]
        } else {
            ""
        }
    }


    @Throws(IOException::class, XMPPException::class, SmackException::class)
    fun connect() {
        Log.d(TAG, "Connecting to server ${Constants.HOST}")

        val conf = XMPPTCPConnectionConfiguration.builder()
                .setXmppDomain(Constants.DOMAIN)
                .setHost(Constants.HOST)
                .setPort(Constants.PORT)
                .setSecurityMode(ConnectionConfiguration.SecurityMode.required)
                .setUsernameAndPassword(mUsername, mPassword!!)
                //Was facing this issue
                //https://discourse.igniterealtime.org/t/connection-with-ssl-fails-with-java-security-keystoreexception-jks-not-found/62566
                .setKeystoreType(null)
                .setSendPresence(true)
                .setResource("Android")
                .build()

        Log.d(TAG, "Username : $mUsername")
        Log.d(TAG, "Password : $mPassword")
        Log.d(TAG, "Server : ${Constants.HOST}")
        //Set up the ui thread broadcast message receiver.
        setupUiThreadBroadCastMessageReceiver()

        val presence = Presence(Presence.Type.available)
        presence.status = "Available"

        mConnection = XMPPTCPConnection(conf)
        mConnection!!.addConnectionListener(this)
        try {
            Log.d(TAG, "Calling connect() ")
            mConnection!!.connect()
            mConnection!!.login(mUsername, mPassword)
            mConnection!!.sendStanza(presence)
            Log.d(TAG, " login() Called ")
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        val reconnectionManager = ReconnectionManager.getInstanceFor(mConnection)
        reconnectionManager.setReconnectionPolicy(ReconnectionManager.ReconnectionPolicy.FIXED_DELAY)
        reconnectionManager.enableAutomaticReconnection()

    }

    private val mIncomingMessageListener = object : IncomingChatMessageListener {
        override fun newIncomingMessage(messageFrom: EntityBareJid, message: Message, chat: Chat) {
            Log.d(TAG, "Received message from ${message.from} with body : ${message.body}")
            val from = message.from.toString()
            var contactJid = getJid(from)
            MessagingModel.addMessage(message.body, contactJid, UsersModel.getInstance().currentUser!!.id)
        }
    }

    private val mRosterListener = object : RosterListener {
        override fun entriesDeleted(addresses: MutableCollection<Jid>?) {
            Log.d(TAG,"entriesDeleted for ${addresses}")
        }

        override fun presenceChanged(presence: Presence?) {

            Log.d(TAG,"Presence changed for ${presence?.from}, isAvailable: ${presence?.isAvailable}")
            if (presence!=null) {
                val from = getJid(presence!!.from.toString())
                UsersModel.getInstance().updatePresence(from, presence!!.isAvailable)
            }
        }

        override fun entriesUpdated(addresses: MutableCollection<Jid>?) {
            Log.d(TAG,"entriesUpdated for ${addresses}")
            fetchAndListenRosterChanges()

        }

        override fun entriesAdded(addresses: MutableCollection<Jid>?) {
            Log.d(TAG,"entriesAdded for ${addresses}")
            fetchAndListenRosterChanges()
        }
    }

    private fun getJid(from:String) : String {
        if (from.contains("/")) {
            return from.split("/".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()[0]
        } else {
            return from
        }
    }

    /**
     * Register and listen for message delivery receipts.
     * */
    private fun initDeliveryReceipts() {
        mDeliveryReceiptManager = DeliveryReceiptManager.getInstanceFor(mConnection)
        mDeliveryReceiptManager!!.autoAddDeliveryReceiptRequests()
        mDeliveryReceiptManager!!.addReceiptReceivedListener { fromJid, toJid, receiptId, receipt ->

            Log.d(TAG, "Message receipt received: fromJid: $fromJid , toJid: $toJid, " +
                    "receiptId: $receiptId, receipt: $receiptId")

        }

    }

    /**
     * Get the roster entries and update the same in database.
     * Also listen for roster updates.
     * */
    private fun fetchAndListenRosterChanges() {
        mChatManager = ChatManager.getInstanceFor(mConnection)
        mChatManager!!.addIncomingListener(mIncomingMessageListener)
        mRoster = Roster.getInstanceFor(mConnection)
        // Accepts all subscription automatically. As it is just a sample application.
        mRoster!!.subscriptionMode = Roster.SubscriptionMode.accept_all

        // Save users in db
        UsersModel.getInstance().saveUsers(mRoster!!.entries)
        mRoster!!.addRosterListener(mRosterListener)
    }

    /**
     * Register for a broadcastReceiver which can listen for a message that needs to be send
     * */
    private fun setupUiThreadBroadCastMessageReceiver() {
        uiThreadMessageReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                //Check if the Intents purpose is to send the message.
                val action = intent.action
                if (action == InnoChatConnectionService.SEND_MESSAGE) {
                    //Send the message.
                    sendMessage(intent.getStringExtra(InnoChatConnectionService.BUNDLE_MESSAGE_BODY),
                            intent.getStringExtra(InnoChatConnectionService.BUNDLE_TO))
                }
            }
        }

        val filter = IntentFilter()
        filter.addAction(InnoChatConnectionService.SEND_MESSAGE)
        mApplicationContext.registerReceiver(uiThreadMessageReceiver, filter)
    }

    /**
     * Process message with server to send to the receiver
     * */
    private fun sendMessage(body: String, toJid: String) {
        Log.d(TAG, "Sending message to :$toJid")

        var jid: EntityBareJid? = null

        val chatManager = ChatManager.getInstanceFor(mConnection)
        chatManager.addIncomingListener(mIncomingMessageListener)
        try {
            jid = JidCreate.entityBareFrom(toJid)
        } catch (e: XmppStringprepException) {
            e.printStackTrace()
        }
        val chat = chatManager.chatWith(jid)
        try {
            val message = Message(jid, Message.Type.chat)
            message.stanzaId = System.currentTimeMillis().toString()
            message.body = body
            chat?.send(message)
            MessagingModel.addMessage(body, UsersModel.getInstance().currentUser!!.id, toJid)
        } catch (e: SmackException.NotConnectedException) {
            e.printStackTrace()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    /**
     * Disconnect everything.
     * Logout the user, Stop listening for send message broadcasts and also disconnect from XMPPConnection.
     * */
    fun disconnect() {
        Log.d(TAG, "Disconnecting from server")

        val prefs = PreferenceManager.getDefaultSharedPreferences(mApplicationContext)
        prefs.edit().putBoolean(Constants.SP_LOGIN_STATUS, false).commit()

        if(mRoster!=null) {
            mRoster!!.removeRosterListener(mRosterListener)
        }
        if (mConnection != null) {
            mConnection!!.disconnect()
        }

        mConnection = null
        // Unregister the message broadcast receiver.
        if (uiThreadMessageReceiver != null) {
            mApplicationContext.unregisterReceiver(uiThreadMessageReceiver)
            uiThreadMessageReceiver = null
        }
    }


    override fun connected(connection: XMPPConnection) {
        InnoChatConnectionService.sConnectionState = ConnectionState.CONNECTED
        Log.d(TAG, "Connected Successfully")
    }

    override fun authenticated(connection: XMPPConnection, resumed: Boolean) {
        InnoChatConnectionService.sConnectionState = ConnectionState.CONNECTED
        Log.d(TAG, "Authenticated Successfully")
        showContactListActivityWhenAuthenticated()
        // Prepare current user
        UsersModel.getInstance().prepareCurrentUser(mApplicationContext)
        fetchAndListenRosterChanges()
        initDeliveryReceipts()
    }

    override fun connectionClosed() {
        InnoChatConnectionService.sConnectionState = ConnectionState.DISCONNECTED
        Log.d(TAG, "Connectionclosed()")
    }

    override fun connectionClosedOnError(e: Exception) {
        InnoChatConnectionService.sConnectionState = ConnectionState.DISCONNECTED
        Log.d(TAG, "ConnectionClosedOnError, error " + e.toString())

    }

    /**
     * Initiate a subscription with a user from current user.
     * */
    fun subscribeUser(jid:String) {
        val jid = JidCreate.entityBareFrom(jid)
        try {
            Roster.getInstanceFor(mConnection).sendSubscriptionRequest(jid)
        } catch (e:Exception){
            Log.e(TAG,"Subscribe User failed to $jid", e)
        }
    }

    /**
     * Send a broadcast that user is authenticated to use this app.
     * */
    private fun showContactListActivityWhenAuthenticated() {
        val i = Intent(InnoChatConnectionService.UI_AUTHENTICATED)
        i.setPackage(mApplicationContext.packageName)
        mApplicationContext.sendBroadcast(i)
//        LocalBroadcastManager.getInstance(mApplicationContext).sendBroadcast(i)
        Log.d(TAG, "Sent the broadcast that we are authenticated")
    }

}