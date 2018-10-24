package com.inno.innochat.model

import android.content.Context
import android.preference.PreferenceManager
import android.util.Log
import com.inno.innochat.Constants
import io.realm.Realm
import io.realm.RealmResults
import org.jivesoftware.smack.roster.RosterEntry
import org.jxmpp.jid.Jid

class UsersModel {
    val imageUrls = arrayOf("https://fortunedotcom.files.wordpress.com/2018/07/gettyimages-961697338.jpg",
            "http://img.timeinc.net/time/photoessays/2008/people_who_mattered/obama_main_1216.jpg",
            "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcT9Xc44TEpV4ySTduEsyDn2RrDwEJrJ0sPwwv7I-wLBMpYanCX8",
            "https://engineering.unl.edu/images/staff/Kayla_Person-small.jpg")

    companion object {
        private var sUserModel : UsersModel? = null

        fun getInstance() : UsersModel {
            if (sUserModel == null) {
                sUserModel = UsersModel()
            }
            return sUserModel!!
        }
    }
    var currentUser : User? = null

    fun addUser(userId:String, name:String, avatar:String="") {
        val realm = Realm.getDefaultInstance()
        realm.executeTransaction(object : Realm.Transaction {
            override fun execute(realm: Realm) {
                val user = User(userId, name, avatar, isCommTo = currentUser!!.id)
                realm.insertOrUpdate(user)
            }
        })
    }

    fun getUsers(): RealmResults<User> {
        val realm = Realm.getDefaultInstance()
        return realm.where(User::class.java)
                .notEqualTo("id", currentUser!!.id)
                .and()
                .beginGroup()
                .equalTo("isCommTo", currentUser!!.id)
                .or()
                .equalTo("isCommTo", "")
                .endGroup()
                .and()
                .distinct("id")
                .findAll()
    }

    fun prepareCurrentUser(context: Context) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val cId = prefs.getString(Constants.SP_USER_NAME,"")
        currentUser = User(cId, "You", "")
    }

    fun saveUsers(entries: Set<RosterEntry>) {
        Log.d("UsersModel", "saveUsers: ${entries.size}")
        val realm = Realm.getDefaultInstance()
        realm.executeTransaction(object : Realm.Transaction {
            override fun execute(realm: Realm) {
                for ((i, item) in entries.withIndex()) {
                    val user = User(item.jid.toString(), item.name ?: item.jid.toString(),
                            imageUrls[i%imageUrls.size],
                            isCommTo = currentUser!!.id)
                    realm.insertOrUpdate(user)
                }
            }
        })
    }

    fun updatePresence(from: String, available: Boolean) {
        val realm = Realm.getDefaultInstance()
        realm.executeTransaction(object : Realm.Transaction {
            override fun execute(realm: Realm) {
                val results =realm.where(User::class.java)
                        .equalTo("id", from)
                        .findAll()
                for (user in results) {
                    user.isAvailable = available
                }
            }
        })
    }

}