package com.inno.innochat.xmpp

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import org.jivesoftware.smack.XMPPException
import org.jivesoftware.smack.SmackException
import com.inno.innochat.xmpp.InnoChatConnection.LoggedInState
import java.io.IOException


class InnoChatConnectionService : Service() {
    companion object {
        val UI_AUTHENTICATED = "com.blikoon.rooster.uiauthenticated"
        val UI_AUTH_FAILED = "com.blikoon.rooster.uiauthFailed"
        val SEND_MESSAGE = "com.blikoon.rooster.sendmessage"
        val BUNDLE_MESSAGE_BODY = "b_body"
        val BUNDLE_TO = "b_to"

        val NEW_MESSAGE = "com.blikoon.rooster.newmessage"
        val BUNDLE_FROM_JID = "b_from"
        var sConnectionState: InnoChatConnection.ConnectionState? = null
        var sLoggedInState: InnoChatConnection.LoggedInState? = null

        private val TAG = "RoosterService"

        fun getState(): InnoChatConnection.ConnectionState {
            return if (sConnectionState == null) {
                InnoChatConnection.ConnectionState.DISCONNECTED
            } else sConnectionState!!
        }

        fun getLoggedInState(): InnoChatConnection.LoggedInState {
            return if (sLoggedInState == null) {
                InnoChatConnection.LoggedInState.LOGGED_OUT
            } else sLoggedInState!!
        }
    }

    private var mActive: Boolean = false//Stores whether or not the thread is active
    private var mThread: Thread? = null
    private var mTHandler: Handler? = null//We use this handler to post messages to
    //the background thread.
    private var mConnection: InnoChatConnection? = null

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate()")
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand()")
        start()
        return Service.START_STICKY
        //RETURNING START_STICKY CAUSES OUR CODE TO STICK AROUND WHEN THE APP ACTIVITY HAS DIED.
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy()")
        stop()
        super.onDestroy()

    }

    fun start() {
        Log.d(TAG, " Service Start() function called.")
        if (!mActive) {
            mActive = true
            if (mThread == null || !mThread!!.isAlive) {
                mThread = Thread {
                    Looper.prepare()
                    mTHandler = Handler()
                    initConnection()
                    //THE CODE HERE RUNS IN A BACKGROUND THREAD.
                    Looper.loop()
                }
                mThread!!.start()
            }
        }
    }

    private fun initConnection() {
        Log.d(TAG, "initConnection()")
        if (mConnection == null) {
            mConnection = InnoChatConnection(this)
        }
        try {
            mConnection!!.connect()
        } catch (e: IOException) {
            Log.e(TAG, "Something went wrong while connecting ,make sure the credentials are right and try again",e)
            sendLoginFailedBroadcast()
            //Stop the service all together.
            stopSelf()
        } catch (e: SmackException) {
            Log.e(TAG, "Something went wrong while connecting ,make sure the credentials are right and try again",e)
            sendLoginFailedBroadcast()
            stopSelf()
        } catch (e: XMPPException) {
            sendLoginFailedBroadcast()
            Log.e(TAG, "Something went wrong while connecting ,make sure the credentials are right and try again",e)
            stopSelf()
        } catch (e: Exception) {
            sendLoginFailedBroadcast()
            Log.e(TAG, "Do we reach here? Even though stop service, because something went wrong",e)
            stopSelf()
        }
    }

    private fun sendLoginFailedBroadcast() {
        val i = Intent(InnoChatConnectionService.UI_AUTH_FAILED)
        i.setPackage(packageName)
        sendBroadcast(i)
    }

    /**
     * Helper method to stop necessary things before service destroys
     * */
    private fun stop() {
        Log.d(TAG, "stop()")
        mActive = false
        mTHandler?.post {
            mConnection?.disconnect()
        }
    }
}