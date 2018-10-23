package com.inno.innochat.xmpp
//
//import android.annotation.SuppressLint
//import android.os.Looper
//import org.jivesoftware.smack.XMPPConnection
//import org.jivesoftware.smack.ConnectionListener
//import org.jivesoftware.smack.SmackException
//import org.jivesoftware.smack.XMPPException
//import android.os.AsyncTask
//import android.util.Log
//import com.inno.innochat.Constants.HOST
//import com.inno.innochat.Constants.PORT
//import org.jivesoftware.smack.tcp.XMPPTCPConnection
//import org.jivesoftware.smack.ConnectionConfiguration
//import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration
//import org.jivesoftware.smack.AbstractXMPPConnection
//import org.jivesoftware.smack.chat2.Chat
//import org.jivesoftware.smack.chat2.ChatManager
//import java.io.IOException
//
//
//class XMPPHelper {
//    companion object {
//        val TAG = "XMPPHelper"
//    }
//
//    private var userName = ""
//    private var passWord = ""
//    private var connection: AbstractXMPPConnection? = null
//    internal var chatmanager: ChatManager? = null
//    internal var newChat: Chat? = null
//    internal var connectionListener = XMPPConnectionListener()
//    private var connected: Boolean = false
//    private val isToasted: Boolean = false
//    private var chat_created: Boolean = false
//    private var loggedin: Boolean = false
//
//
//    //Initialize
//    fun init(userId: String, pwd: String) {
//        Log.i("XMPP", "Initializing!")
//        this.userName = userId
//        this.passWord = pwd
//        val configBuilder = XMPPTCPConnectionConfiguration.builder()
//        configBuilder.setUsernameAndPassword(userName, passWord)
//        configBuilder.setSecurityMode(ConnectionConfiguration.SecurityMode.disabled)
//        configBuilder.setResource("Android")
//        //configBuilder.setServiceName(Constants.DOMAIN)
//        configBuilder.setHost(HOST)
//        configBuilder.setPort(PORT)
//        //configBuilder.setDebuggerEnabled(true);
//        connection = XMPPTCPConnection(configBuilder.build())
//        (connection as XMPPTCPConnection).addConnectionListener(connectionListener)
//
//    }
//
//    // Disconnect Function
//    fun disconnectConnection() {
//
//        Thread(Runnable { connection.disconnect() }).start()
//    }
//
//    @SuppressLint("StaticFieldLeak")
//    fun connectConnection() {
//        val connectionThread = object : AsyncTask<Void, Void, Boolean>() {
//
//            override fun doInBackground(vararg arg0: Void): Boolean? {
//
//                // Create a connection
//                try {
//                    connection!!.connect()
//                    login()
//                    connected = true
//
//                } catch (e: IOException) {
//                    Log.e(TAG, e.localizedMessage, e)
//                } catch (e: SmackException) {
//                    Log.e(TAG, e.localizedMessage, e)
//                } catch (e: XMPPException) {
//                    Log.e(TAG, e.localizedMessage, e)
//                } catch (e: Exception) {
//                    Log.e(TAG, e.localizedMessage, e)
//                }
//                return null
//            }
//        }
//        connectionThread.execute()
//    }
//
//
//    fun sendMsg() {
//        if (connection?.isConnected == true) {
//            // Assume we've created an XMPPConnection name "connection"._
//            chatmanager = ChatManager.getInstanceFor(connection)
//            newChat = chatmanager.createChat("concurer@nimbuzz.com")
//
//            try {
//                newChat.sendMessage("Howdy!")
//            } catch (e: SmackException.NotConnectedException) {
//                e.printStackTrace()
//            }
//
//        }
//    }
//
//    fun login() {
//
//        try {
//            connection.login(userName, passWord)
//            //Log.i("LOGIN", "Yey! We're connected to the Xmpp server!");
//
//        } catch (e: XMPPException) {
//            e.printStackTrace()
//        } catch (e: SmackException) {
//            e.printStackTrace()
//        } catch (e: IOException) {
//            e.printStackTrace()
//        } catch (e: Exception) {
//        }
//
//    }
//
//
//    //Connection Listener to check connection state
//    inner class XMPPConnectionListener : ConnectionListener {
//        override fun connected(connection: XMPPConnection) {
//
//            Log.d("xmpp", "Connected!")
//            connected = true
//            if (!connection.isAuthenticated) {
//                login()
//            }
//        }
//
//        override fun connectionClosed() {
//            if (isToasted)
//
//                Handler(Looper.getMainLooper()).post(Runnable {
//                    // TODO Auto-generated method stub
//                })
//            Log.d("xmpp", "ConnectionCLosed!")
//            connected = false
//            chat_created = false
//            loggedin = false
//        }
//
//        override fun connectionClosedOnError(arg0: Exception) {
//            if (isToasted)
//
//                Handler(Looper.getMainLooper()).post(Runnable { })
//            Log.d("xmpp", "ConnectionClosedOn Error!")
//            connected = false
//
//            chat_created = false
//            loggedin = false
//        }
//
//        fun reconnectingIn(arg0: Int) {
//
//            Log.d("xmpp", "Reconnectingin $arg0")
//
//            loggedin = false
//        }
//
//        fun reconnectionFailed(arg0: Exception) {
//            if (isToasted)
//
//                Handler(Looper.getMainLooper()).post(Runnable { })
//            Log.d("xmpp", "ReconnectionFailed!")
//            connected = false
//
//            chat_created = false
//            loggedin = false
//        }
//
//        fun reconnectionSuccessful() {
//            if (isToasted)
//
//                Handler(Looper.getMainLooper()).post(Runnable {
//                    // TODO Auto-generated method stub
//                })
//            Log.d("xmpp", "ReconnectionSuccessful")
//            connected = true
//
//            chat_created = false
//            loggedin = false
//        }
//
//        override fun authenticated(arg0: XMPPConnection, arg1: Boolean) {
//            Log.d("xmpp", "Authenticated!")
//            loggedin = true
//
//            chat_created = false
//            Thread(Runnable {
//                try {
//                    Thread.sleep(500)
//                } catch (e: InterruptedException) {
//                    // TODO Auto-generated catch block
//                    e.printStackTrace()
//                }
//            }).start()
//            if (isToasted)
//
//                Handler(Looper.getMainLooper()).post(Runnable {
//                    // TODO Auto-generated method stub
//                })
//        }
//    }
//
//
//
//
//}