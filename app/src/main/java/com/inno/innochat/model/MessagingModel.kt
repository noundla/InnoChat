package com.inno.innochat.model

import android.content.Context
import java.util.*

class MessagingModel {

    companion object {
        private var sMessagingModel: MessagingModel? = null

        fun getInstance(): MessagingModel {
            if (sMessagingModel == null) {
                sMessagingModel = MessagingModel()
            }
            return sMessagingModel!!
        }
    }
    private var mMessages : ArrayList<Message> = ArrayList()

    private constructor() {

    }

    fun addMessage(message: Message) {
        mMessages.add(0, message)
    }

    fun sendMessage(body: String) {
        mMessages.add(0, Message(body, UsersModel.getInstance().currentUser!!, Date().time))
    }



    fun getMessages() : List<Message> {
        return mMessages
    }
}