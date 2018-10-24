package com.inno.innochat.model

import android.content.Context
import io.realm.Realm
import io.realm.RealmResults
import io.realm.Sort
import java.util.*

object MessagingModel {
    fun addMessage(body: String, from:String, to:String) {
        val realm = Realm.getDefaultInstance()
        realm.executeTransaction(object : Realm.Transaction {
            override fun execute(realm: Realm) {
                val message = Message(body, from, to, Date().time)
                realm.insertOrUpdate(message)
            }
        })
    }


    fun getMessages(person1:String, person2:String) : RealmResults<Message> {
        val realm = Realm.getDefaultInstance()
        return realm.where(Message::class.java)
                .beginGroup()
                .equalTo("from", person1)
                .and()
                .equalTo("to", person2)
                .endGroup()
                .or()
                .beginGroup()
                .equalTo("from", person2)
                .and()
                .equalTo("to", person1)
                .endGroup()
                .sort("createdAt", Sort.DESCENDING)
                .findAll()
    }
}