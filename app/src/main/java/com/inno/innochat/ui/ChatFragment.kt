package com.inno.innochat.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import android.widget.Toast

import com.inno.innochat.R
import com.inno.innochat.adapter.MessagingAdapter
import com.inno.innochat.model.Message
import com.inno.innochat.model.MessagingModel
import com.inno.innochat.model.User
import com.inno.innochat.model.UsersModel
import com.inno.innochat.xmpp.InnoChatConnection
import com.inno.innochat.xmpp.InnoChatConnectionService
import io.realm.RealmChangeListener
import io.realm.RealmResults
import kotlinx.android.synthetic.main.fragment_chat.*

/**
 * This fragment is used to display chat conversation screen where users can communicate each other.
 *
 * @author Sandeep Noundla
 * */
class ChatFragment : Fragment() {
    companion object {
        const val TAG = "ChatFragment"
        const val EXTRA_RECEIVER = "EXTRA_RECEIVER"
    }

    private var mNavigationListener : NavigationListener? = null
    private var mReceiverUser : User? = null
    private lateinit var adapter : MessagingAdapter
    private var mRealmMessages: RealmResults<Message>? = null

    private val mRecyclerView: RecyclerView by lazy {
        val linearLayoutManager = LinearLayoutManager(context!!, LinearLayoutManager.VERTICAL, false)
        linearLayoutManager.reverseLayout = true
        reyclerview_message_list?.layoutManager = linearLayoutManager
        reyclerview_message_list.setHasFixedSize(true)
        reyclerview_message_list
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_chat, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)
        loadArguments()
        if (mReceiverUser!=null) {
            updateTitle(mReceiverUser!!.name)
            adapter = MessagingAdapter(context!!, mReceiverUser!!.avatar)

            button_chatbox_send.setOnClickListener {
                processSendMessage()
            }

            edittext_chatbox.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(text: Editable?) {
                    //enable the send button if the text is not empty
                    button_chatbox_send.isEnabled = !(text.isNullOrEmpty())
                }

                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                }

                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                }
            })

            mRecyclerView.adapter = adapter
            loadMessages()
        } else {
            Toast.makeText(context!!, "Internal problem occured, please try again.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (context is NavigationListener) {
            mNavigationListener = context
        }
    }

    override fun onDetach() {
        super.onDetach()
        mNavigationListener = null
    }

    override fun onPause() {
        super.onPause()
        mRealmMessages?.removeAllChangeListeners()
    }

    /**
     * Loads the initial conversation messages from local database and displays on UI
     * */
    private fun loadMessages() {
        mRealmMessages = MessagingModel.getMessages(mReceiverUser!!.id, UsersModel.getInstance().currentUser!!.id)
        adapter.items = mRealmMessages
        mRecyclerView.scrollToPosition(0)
        // Listen for new messages
        mRealmMessages!!.addChangeListener(object : RealmChangeListener<RealmResults<Message>> {
            override fun onChange(users: RealmResults<Message>?) {
                // update the messages in conversation window
                adapter.items = mRealmMessages
                mRecyclerView.scrollToPosition(0)
            }
        })

    }

    /**
     * Loads arguments passed to this fragment
     * */
    private fun loadArguments() {
        if (arguments != null) {
            if (arguments!!.containsKey(EXTRA_RECEIVER)) {
                mReceiverUser = arguments!!.getParcelable(EXTRA_RECEIVER)
            }
        }
    }

    /**
     * Updates the screen title with user name
     * */
    private fun updateTitle(to:String) {
        val activity = activity as AppCompatActivity
        activity.supportActionBar.let { actionBar ->
            actionBar!!.title = to
            actionBar.setDisplayHomeAsUpEnabled(true)
        }
    }

    /**
     * Process the send message action.
     * */
    private fun processSendMessage() {
        val text = edittext_chatbox.text
        if (text.isNullOrEmpty()) {
            return
        }

        if (InnoChatConnectionService.getState().equals(InnoChatConnection.ConnectionState.CONNECTED)) {
            Log.d(TAG, "The client is connected to the server,Sending Message")
            //Send the message to the server
            val body = edittext_chatbox.text.toString()
            val intent = Intent(InnoChatConnectionService.SEND_MESSAGE)
            intent.putExtra(InnoChatConnectionService.BUNDLE_MESSAGE_BODY, body)
            intent.putExtra(InnoChatConnectionService.BUNDLE_TO, mReceiverUser!!.id)
            context?.sendBroadcast(intent)
            edittext_chatbox.text = null
        } else {
            Toast.makeText(context,
                    "Client not connected to server ,Message not sent!",
                    Toast.LENGTH_LONG).show()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when(item?.itemId){
            android.R.id.home -> {
                mNavigationListener?.showUserListScreen()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}
