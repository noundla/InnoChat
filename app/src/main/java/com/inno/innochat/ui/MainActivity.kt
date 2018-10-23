package com.inno.innochat.ui

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import com.inno.innochat.R
import com.inno.innochat.model.User
import com.inno.innochat.xmpp.InnoChatConnection
import com.inno.innochat.xmpp.InnoChatConnectionService
import kotlinx.android.synthetic.main.activity_main.*

interface NavigationListener {
    fun showLoginScreen();
    fun showUserListScreen();
    fun showChatScreen(user: User);
}

interface DisplayLoaderListener {
    fun showLoader()
    fun hideLoader()
}

class MainActivity : AppCompatActivity(), NavigationListener, DisplayLoaderListener {

    companion object {
        private val TAG = "MainActivity"
    }
    private var mLoader: LoaderDialogFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        checkAndShowInitialScreen()
    }

    private fun checkAndShowInitialScreen() {
        if (InnoChatConnectionService.getLoggedInState() == InnoChatConnection.LoggedInState.LOGGED_IN) {
            showUserListScreen()
        } else {
            showLoginScreen()
        }
    }

    override fun showLoginScreen() {
        val fragment = LoginFragment()
        supportFragmentManager.beginTransaction().replace(R.id.container, fragment).commitAllowingStateLoss()
        supportActionBar!!.hide()
    }

    override fun showUserListScreen() {
        val fragment = UsersListFragment()
        supportFragmentManager.beginTransaction().replace(R.id.container, fragment).commitAllowingStateLoss()
        supportActionBar!!.show()
    }

    override fun showChatScreen(user: User) {
        val fragment = ChatFragment()
        val bundle = Bundle()
        bundle.putParcelable(ChatFragment.EXTRA_RECEIVER, user)
        fragment.arguments = bundle
        supportFragmentManager.beginTransaction().replace(R.id.container, fragment).commitAllowingStateLoss()
//        supportFragmentManager.beginTransaction()
//                .add(R.id.container, fragment)
//                .addToBackStack(null)
//                .commitAllowingStateLoss()
    }

    override fun showLoader() {
        if (mLoader == null || mLoader!!.isHidden) {
            mLoader = LoaderDialogFragment()
            mLoader!!.show(fragmentManager, LoaderDialogFragment.TAG)
        }
    }

    //hide loader if any loader existed on screen
    override fun hideLoader() {
        try {
            if (!isFinishing && mLoader != null && !mLoader!!.isHidden) {
                runOnUiThread {
                    mLoader!!.dismiss()
                    mLoader = null
                }
            }
        } catch (e:Exception){
            Log.e(TAG,"hideLoader",e)
        }
    }

    override fun onBackPressed() {
        val f = supportFragmentManager.findFragmentById(R.id.container)
        if (f is ChatFragment) {
            showUserListScreen()
        } else {
            super.onBackPressed()
        }

    }

}
