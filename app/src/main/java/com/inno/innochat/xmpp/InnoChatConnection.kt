package com.inno.innochat.xmpp

import android.content.*
import android.preference.PreferenceManager
import android.support.v4.content.LocalBroadcastManager
import android.text.TextUtils
import android.util.Log
import com.inno.innochat.AppHelpers
import com.inno.innochat.Constants
import com.inno.innochat.model.MessagingModel
import com.inno.innochat.model.UsersModel
import org.jivesoftware.smack.*
import org.jxmpp.stringprep.XmppStringprepException
import org.jivesoftware.smack.tcp.XMPPTCPConnection
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration
import org.jivesoftware.smack.chat.Chat
import org.jivesoftware.smack.chat.ChatManager
import org.jivesoftware.smack.chat.ChatManagerListener
import org.jivesoftware.smack.chat.ChatMessageListener
import org.jivesoftware.smack.filter.StanzaFilter
import org.jivesoftware.smack.packet.Message
import org.jivesoftware.smack.packet.Presence
import org.jivesoftware.smack.packet.Stanza
import org.jivesoftware.smack.roster.Roster
import org.jivesoftware.smack.roster.RosterEntry
import org.jivesoftware.smack.roster.RosterListener
import org.jivesoftware.smackx.muc.MultiUserChat
import org.jivesoftware.smackx.muc.MultiUserChatManager
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
    private var mMultiUserChat: MultiUserChat? = null
    private var mStanzaListener: StanzaListener? = null
    private var mRoster: Roster? = null
    private var mChatManager: ChatManager? = null
    private var uiThreadMessageReceiver: BroadcastReceiver? = null//Receives messages from the ui thread.


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
                .setServiceName(Constants.DOMAIN)
                .setHost(Constants.HOST)
                .setPort(Constants.PORT)
                .setUsernameAndPassword(mUsername, mPassword!!)
                //For http domains- security mode should be disabled and keystoreType should be commented
                .setSecurityMode(ConnectionConfiguration.SecurityMode.disabled)
                //Was facing this issue
                //https://discourse.igniterealtime.org/t/connection-with-ssl-fails-with-java-security-keystoreexception-jks-not-found/62566
//                .setKeystoreType(null)
                .setSendPresence(true)
                .setResource("Android")
                .build()

        Log.d(TAG, "Username : $mUsername")
        Log.d(TAG, "Password : $mPassword")
        Log.d(TAG, "Server : ${Constants.HOST}")
        //Set up the ui thread broadcast message receiver.
        setupUiThreadBroadCastMessageReceiver()

        mConnection = XMPPTCPConnection(conf)
        mConnection!!.addConnectionListener(this)
        try {
            Log.d(TAG, "Calling connect() ")
            if (!mConnection!!.isAuthenticated) {
                mConnection!!.connect()
                mConnection!!.login()
            }
            Log.d(TAG, " login() Called ")
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        val multiUserChatManager = MultiUserChatManager.getInstanceFor(mConnection)
        mMultiUserChat = multiUserChatManager.getMultiUserChat("internal" + "@conference." + Constants.DOMAIN)

        try {
            mMultiUserChat!!.join(getLocalJid())
        } catch (e: Exception) {
            Log.e(TAG, "Cannot join room: " + mMultiUserChat!!.getRoom(), e)
        }

        val stanzaFilter = StanzaFilter { stanza ->
            Log.d(TAG, "Inside StanzaFilter.accept " + stanza.toString())
            true
        }


        if (mStanzaListener != null) {
            mConnection!!.removeSyncStanzaListener(mStanzaListener)
        }

        mStanzaListener = object : StanzaListener {
            override fun processPacket(packet: Stanza?) {
                if (packet is Message) {
                    Log.d(TAG, "Received message from ${packet.from} with body : ${packet.body}")
                    val from = packet.from.toString()
                    var contactJid = getJid(from)
                    MessagingModel.addMessage(packet.body, contactJid, UsersModel.getInstance().currentUser!!.id)
                } else if (packet is Presence) {
                    Log.d(TAG, "Received Presence from ${packet.from} with isAvailable : ${packet.isAvailable}")
                }
            }
        }

        mConnection!!.addSyncStanzaListener(mStanzaListener, stanzaFilter)
        val presence = Presence(Presence.Type.available)
        presence.status = "Available"
        mConnection!!.sendStanza(presence)

        val reconnectionManager = ReconnectionManager.getInstanceFor(mConnection)
        reconnectionManager.setReconnectionPolicy(ReconnectionManager.ReconnectionPolicy.FIXED_DELAY)
        reconnectionManager.enableAutomaticReconnection()
    }

    private val mRosterListener = object : RosterListener {
        override fun entriesDeleted(addresses: MutableCollection<String>?) {
            Log.d(TAG, "entriesDeleted for ${addresses}")
        }

        override fun presenceChanged(presence: Presence?) {

            Log.d(TAG, "Presence changed for ${presence?.from}, isAvailable: ${presence?.isAvailable}")
            if (presence != null) {
                val from = getJid(presence!!.from.toString())
                UsersModel.getInstance().updatePresence(from, presence!!.isAvailable)
            }
        }

        override fun entriesUpdated(addresses: MutableCollection<String>?) {
            Log.d(TAG, "entriesUpdated for ${addresses}")
            fetchAndListenRosterChanges()

        }

        override fun entriesAdded(addresses: MutableCollection<String>?) {
            Log.d(TAG, "entriesAdded for ${addresses}")
            fetchAndListenRosterChanges()
        }
    }

    private fun getJid(from: String): String {
        if (from.contains("/")) {
            return from.split("/".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()[0]
        } else {
            return from
        }
    }

    private fun getLocalJid(): String {
        return "$mUsername@${Constants.DOMAIN}"
    }

    /**
     * Get the roster entries and update the same in database.
     * Also listen for roster updates.
     * */
    private fun fetchAndListenRosterChanges() {
        mChatManager = ChatManager.getInstanceFor(mConnection)
        mChatManager!!.addChatListener(object : ChatManagerListener {
            override fun chatCreated(chat: Chat?, createdLocally: Boolean) {
                Log.d(TAG, "chatCreated: createdLocally:$createdLocally, chat: $chat")
            }

        })
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
        try {


            var messageObj: Message = Message(toJid)
            messageObj.body = body
            messageObj.subject = "TEXT"
            mConnection!!.sendPacket(messageObj);
        } catch (e: SmackException.NotConnectedException) {
            Log.e(TAG, "sendMessage", e)
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

        if (mRoster != null) {
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
    }


    override fun connectionClosed() {
        InnoChatConnectionService.sConnectionState = ConnectionState.DISCONNECTED
        Log.d(TAG, "Connectionclosed()")
    }

    override fun connectionClosedOnError(e: Exception) {
        InnoChatConnectionService.sConnectionState = ConnectionState.DISCONNECTED
        Log.d(TAG, "ConnectionClosedOnError, error " + e.toString())

    }

    override fun reconnectionSuccessful() {
        Log.d(TAG, "reconnectionSuccessful()")
    }

    override fun reconnectionFailed(e: java.lang.Exception?) {
        Log.e(TAG, "reconnectionFailed()",e)
    }

    override fun reconnectingIn(seconds: Int) {
        Log.d(TAG, "reconnectingIn() seconds: $seconds")
    }

    /**
     * Initiate a subscription with a user from current user.
     * */
    fun subscribeUser(jid: String) {
        Log.d(TAG, "subscribeUser jid: $jid not required?")
//        val jid = JidCreate.entityBareFrom(jid)
//        try {
//            Roster.getInstanceFor(mConnection).sendSubscriptionRequest(jid)
//        } catch (e: Exception) {
//            Log.e(TAG, "Subscribe User failed to $jid", e)
//        }
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