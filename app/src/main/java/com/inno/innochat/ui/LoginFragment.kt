package com.inno.innochat.ui

import com.inno.innochat.R
import kotlinx.android.synthetic.main.fragment_login.*
import android.content.*
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.customtabs.CustomTabsIntent
import android.support.design.widget.CoordinatorLayout
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.util.Log
import com.inno.innochat.Constants
import com.inno.innochat.xmpp.InnoChatConnectionService
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.inno.innochat.model.User
import com.inno.innochat.model.UsersModel


class LoginFragment : Fragment() {

    companion object {
        private val TAG = "LoginFragment"
    }
    private lateinit var mBroadcastReceiver: BroadcastReceiver

    private var mNavigationListener : NavigationListener? = null
    private var mLoaderListener : DisplayLoaderListener? = null
    private var mCoordinateLayout : CoordinatorLayout? = null
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mCoordinateLayout = activity!!.findViewById(R.id.coordinateLayout)
        loginButton.setOnClickListener{
            processLogin()
        }
        registrationTV.setOnClickListener{
            launchRegistrationPage()
        }

    }

    override fun onResume() {
        super.onResume()
        registerAuthenticationBroadcast()
    }

    override fun onPause() {
        super.onPause()
        context?.unregisterReceiver(mBroadcastReceiver)
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (context is NavigationListener) {
            mNavigationListener = context
        }
        if (context is DisplayLoaderListener) {
            mLoaderListener = context
        }
    }

    override fun onDetach() {
        super.onDetach()
        mNavigationListener = null
    }



    private fun registerAuthenticationBroadcast(){
        mBroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {

                val action = intent.action
                when (action) {
                    InnoChatConnectionService.UI_AUTHENTICATED -> {
                        Log.d(TAG, "Got a broadcast to show the main app window")
                        val prefs = PreferenceManager.getDefaultSharedPreferences(activity)
                        prefs.edit().putBoolean(Constants.SP_LOGIN_STATUS, true).commit()
                        UsersModel.getInstance().prepareCurrentUser(context)
                        UsersModel.getInstance().prepareInitialUsers()
                        mLoaderListener?.hideLoader()
                        mNavigationListener?.showUserListScreen()
                    }
                    InnoChatConnectionService.UI_AUTH_FAILED -> {
                        mLoaderListener?.hideLoader()
                        showLoginFailedMessage()
                    }
                }

            }
        }
        val filter = IntentFilter(InnoChatConnectionService.UI_AUTHENTICATED)
        filter.addAction(InnoChatConnectionService.UI_AUTH_FAILED)
        context?.registerReceiver(mBroadcastReceiver, filter)
    }

    /**
     * Attempts to sign-in the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private fun processLogin() {
        // Reset errors.
        userNameET.error = null
        passwordET.error = null

        // Store values at the time of the login attempt.
        val userName = userNameET.text.toString()
        val password = passwordET.text.toString()

        var cancel = false
        var focusView: View? = null

        // Check for a valid password, if the user entered one.
        if (TextUtils.isEmpty(password)) {
            passwordET.error = getString(R.string.error_field_required)
            focusView = passwordET
            cancel = true
        }

        // Check for a valid username
        if (TextUtils.isEmpty(userName)) {
            userNameET.error = getString(R.string.error_field_required)
            focusView = userNameET
            cancel = true
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView!!.requestFocus()
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            mLoaderListener?.showLoader()

            val password = passwordET.text.toString()
            var username = userNameET.text.toString()
            if (!username.contains("@")) {
                username = "$userName@${Constants.HOST}"
            }

            //This is where the login login is fired up.
            //            Log.d(TAG,"Jid and password are valid ,proceeding with login.");
            //            startActivity(new Intent(this,ContactListActivity.class));

            //Save the credentials and login
            saveCredentialsAndLogin(username, password)
        }
    }


    private fun saveCredentialsAndLogin(username:String, password:String) {
        Log.d(TAG, "saveCredentialsAndLogin() called. username: $username, password: $password")
        val prefs = PreferenceManager.getDefaultSharedPreferences(activity)
        prefs.edit()
                .putString(Constants.SP_USER_NAME, username)
                .putString(Constants.SP_PASSWORD, password)
                .commit()
        //Start the service
        val i1 = Intent(activity, InnoChatConnectionService::class.java)
        context?.startService(i1)
    }

    private fun launchRegistrationPage() {
        val customTabsIntent = CustomTabsIntent.Builder()
                .addDefaultShareMenuItem()
                .setToolbarColor(this.resources
                        .getColor(R.color.colorPrimary))
                .setShowTitle(true)
                //.setCloseButtonIcon(getDrawable(R.drawable.ic_close))
                .build()
        customTabsIntent.launchUrl(context!!, Uri.parse(getString(R.string.registration_url)))
    }

    private fun showLoginFailedMessage() {
        if (mCoordinateLayout != null) {
            val snackbar = Snackbar.make(mCoordinateLayout!!,
                    getString(R.string.msg_login_failed),
                    Snackbar.LENGTH_LONG)
            snackbar.setAction(getString(R.string.ok)) {
                snackbar.dismiss()
            }
            snackbar.show()
        } else {
            Log.w(TAG, "showLoginFailedMessage")
        }
    }

}
