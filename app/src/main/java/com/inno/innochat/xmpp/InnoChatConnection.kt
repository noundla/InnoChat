package com.inno.innochat.xmpp

import org.jivesoftware.smack.XMPPConnection
import android.content.*
import android.preference.PreferenceManager
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import com.inno.innochat.Constants
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
import java.io.IOException
import java.net.InetAddress

class InnoChatConnection(context: Context) : ConnectionListener {
    companion object {
        private val TAG = "InnoChatConnection"
    }
    private val mApplicationContext: Context
    private val mUsername: String
    private val mPassword: String?
    private val mServiceName: String
    private var mConnection: XMPPTCPConnection? = null
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
                .getString(Constants.SP_USER_NAME, null)
        mPassword = PreferenceManager.getDefaultSharedPreferences(mApplicationContext)
                .getString(Constants.SP_PASSWORD, null)

        if (jid != null) {
            // TODO: sandeep, need to check whether this is required or not
            mUsername = jid.split("@".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]
            mServiceName = jid.split("@".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1]
        } else {
            mUsername = ""
            mServiceName = ""
        }
    }


    @Throws(IOException::class, XMPPException::class, SmackException::class)
    fun connect() {
        Log.d(TAG, "Connecting to server $mServiceName")

        val conf = XMPPTCPConnectionConfiguration.builder()
                .setXmppDomain(Constants.DOMAIN)
                .setHost(Constants.HOST)
                .setPort(Constants.PORT)
                .setSecurityMode(ConnectionConfiguration.SecurityMode.required)
                .setUsernameAndPassword(mUsername,mPassword!!)
                //.setHostAddress(InetAddress.getByName(Constants.HOST))
                //Was facing this issue
                //https://discourse.igniterealtime.org/t/connection-with-ssl-fails-with-java-security-keystoreexception-jks-not-found/62566
                .setKeystoreType(null) //This line seems to get rid of the problem
//                .setCompressionEnabled(true)

//                .setSendPresence(true)
                .setResource("Android")
                .build()

        Log.d(TAG, "Username : $mUsername")
        Log.d(TAG, "Password : " + mPassword!!)
        Log.d(TAG, "Server : $mServiceName")
        //Set up the ui thread broadcast message receiver.
        setupUiThreadBroadCastMessageReceiver()

        mConnection = XMPPTCPConnection(conf)
        mConnection!!.addConnectionListener(this)
        try {
            Log.d(TAG, "Calling connect() ")
            mConnection!!.connect()
            mConnection!!.login(mUsername, mPassword!!)
            Log.d(TAG, " login() Called ")
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        ChatManager.getInstanceFor(mConnection).addIncomingListener(object : IncomingChatMessageListener {
            override fun newIncomingMessage(messageFrom: EntityBareJid, message: Message, chat: Chat) {
                ///ADDED
                Log.d(TAG, "message.getBody() :" + message.body)
                Log.d(TAG, "message.getFrom() :" + message.from)

                val from = message.from.toString()

                var contactJid = ""
                if (from.contains("/")) {
                    contactJid = from.split("/".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()[0]
                    Log.d(TAG, "The real jid is :$contactJid")
                    Log.d(TAG, "The message is from :$from")
                } else {
                    contactJid = from
                }

                //Bundle up the intent and send the broadcast.
                val intent = Intent(InnoChatConnectionService.NEW_MESSAGE)
                intent.setPackage(mApplicationContext.packageName)
                intent.putExtra(InnoChatConnectionService.BUNDLE_FROM_JID, contactJid)
                intent.putExtra(InnoChatConnectionService.BUNDLE_MESSAGE_BODY, message.body)
                mApplicationContext.sendBroadcast(intent)

                Log.d(TAG, "Received message from :$contactJid broadcast sent.")
                ///ADDED

            }
        })


        val reconnectionManager = ReconnectionManager.getInstanceFor(mConnection)
//        reconnectionManager.setEnabledPerDefault(true)//TODO: sandeep: need to check what is alternative
        reconnectionManager.enableAutomaticReconnection()

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

        try {
            jid = JidCreate.entityBareFrom(toJid)
        } catch (e: XmppStringprepException) {
            e.printStackTrace()
        }

        val chat = chatManager.chatWith(jid)
        try {
            val message = Message(jid, Message.Type.chat)
            message.body = body
            chat.send(message)
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
        Log.d(TAG, "Disconnecting from serser $mServiceName")

        val prefs = PreferenceManager.getDefaultSharedPreferences(mApplicationContext)
        prefs.edit().putBoolean(Constants.SP_LOGIN_STATUS, false).commit()

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
    }


    override fun connectionClosed() {
        InnoChatConnectionService.sConnectionState = ConnectionState.DISCONNECTED
        Log.d(TAG, "Connectionclosed()")

    }

    override fun connectionClosedOnError(e: Exception) {
        InnoChatConnectionService.sConnectionState = ConnectionState.DISCONNECTED
        Log.d(TAG, "ConnectionClosedOnError, error " + e.toString())

    }

    fun reconnectingIn(seconds: Int) {
        InnoChatConnectionService.sConnectionState = ConnectionState.CONNECTING
        Log.d(TAG, "ReconnectingIn() ")

    }

    fun reconnectionSuccessful() {
        InnoChatConnectionService.sConnectionState = ConnectionState.CONNECTED
        Log.d(TAG, "ReconnectionSuccessful()")

    }

    fun reconnectionFailed(e: Exception) {
        InnoChatConnectionService.sConnectionState = ConnectionState.DISCONNECTED
        Log.d(TAG, "ReconnectionFailed()")

    }

    private fun showContactListActivityWhenAuthenticated() {

        val i = Intent(InnoChatConnectionService.UI_AUTHENTICATED)
        i.setPackage(mApplicationContext.packageName)
        mApplicationContext.sendBroadcast(i)
//        LocalBroadcastManager.getInstance(mApplicationContext).sendBroadcast(i)
        Log.d(TAG, "Sent the broadcast that we are authenticated")
    }


}