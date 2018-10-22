package com.inno.innochat.ui


import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView

import com.inno.innochat.R
import com.inno.innochat.adapter.MessagingAdapter
import com.inno.innochat.model.User
import kotlinx.android.synthetic.main.fragment_chat.*

class ChatFragment : Fragment() {
    companion object {
        const val EXTRA_RECEIVER = "EXTRA_RECEIVER"
    }

    private val mRecyclerView: RecyclerView by lazy {
        val linearLayoutManager = LinearLayoutManager(context!!, LinearLayoutManager.VERTICAL, false)
        linearLayoutManager.reverseLayout = true
        reyclerview_message_list?.layoutManager = linearLayoutManager
        reyclerview_message_list.setHasFixedSize(true)
        reyclerview_message_list
    }

    private var mReceiver : User? = null
    private val mAdapter = MessagingAdapter(context!!)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_chat, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadArguments()
        if (mReceiver!=null) {
            updateTitle(mReceiver!!.nickname)
        }

        button_chatbox_send.setOnClickListener {
            sendMessage()
        }

        edittext_chatbox.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(text: Editable?) {
                button_chatbox_send.isEnabled = !(text.isNullOrEmpty())
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }
        })


        edittext_chatbox.setOnEditorActionListener(object : TextView.OnEditorActionListener{
            override fun onEditorAction(p0: TextView?, i: Int, p2: KeyEvent?): Boolean {
                if (i == EditorInfo.IME_ACTION_SEND) {
                    sendMessage()
                    return true;
                }
                return false;
            }
        })
        loadMessages()
        observeMessages()
    }

    private fun loadMessages() {

    }

    private fun observeMessages() {
        // Todo: observe the database changes for send/receive messages
    }

    private fun loadArguments() {
        if (arguments != null) {
            if (arguments!!.containsKey(EXTRA_RECEIVER)) {
                mReceiver = arguments!!.getParcelable(EXTRA_RECEIVER)
            }
        }
    }
    

    private fun updateTitle(to:String) {
        val activity = activity as AppCompatActivity
        activity.supportActionBar.let { actionBar ->
            actionBar!!.title = to
        }
    }

    private fun sendMessage() {
        val text = edittext_chatbox.text
        if (text.isNullOrEmpty())
            return

        //TODO: process sendMessage
        //viewModel.sendMessage(text!!.toString())
        mRecyclerView.scrollToPosition(0)
        edittext_chatbox.text = null
    }
}
