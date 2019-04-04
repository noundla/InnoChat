package com.inno.innochat.model

import android.content.Context
import android.preference.PreferenceManager
import android.text.TextUtils
import android.util.Log
import com.inno.innochat.Constants
import io.realm.Realm
import io.realm.RealmResults
import org.jivesoftware.smack.roster.RosterEntry
/**
 * @author Sandeep Noundla
 * */
class UsersModel {
    // Static image urls which can be used to display temporary images in users list screen
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

    /**
     * Adds a user into database
     *
     * @userId Jid of user
     * @name User's name
     * @avatar User's image url
     * */
    fun addUser(userId:String, name:String, avatar:String="") {
        val realm = Realm.getDefaultInstance()
        realm.executeTransaction(object : Realm.Transaction {
            override fun execute(realm: Realm) {
                val user = User(userId, name, avatar, isCommTo = currentUser!!.id)
                realm.insertOrUpdate(user)
            }
        })
    }

    /**
     * Returns the friends list of the current user
     *
     * @return Friends list
     * */
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

    /**
     * Prepares the current user instance with jid.
     * */
    fun prepareCurrentUser(context: Context) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val cId = prefs.getString(Constants.SP_USER_NAME,"")
        currentUser = User(cId, "You", "")
    }

    /**
     * Saves the users details in database. These users are associated with current user as a friend.
     *
     * @entries User details fetched from XMPP server.
     * */
    fun saveUsers(entries: Set<RosterEntry>) {
        Log.d("UsersModel", "saveUsers: ${entries.size}")
        val realm = Realm.getDefaultInstance()
        realm.executeTransaction(object : Realm.Transaction {
            override fun execute(realm: Realm) {
                for ((i, item) in entries.withIndex()) {
                    var name = if (TextUtils.isEmpty(item.name)) {
                        item.user.toString().split("@".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]
                    } else {
                        item.name
                    }
                    val user = User(item.user.toString(), name,
                            imageUrls[i%imageUrls.size],
                            isCommTo = currentUser!!.id)
                    realm.insertOrUpdate(user)
                }
            }
        })
    }

    /**
     * This method is used to update a user's presence in database.
     *
     * @from Jid of a user whose presence needs to be updated
     * @available Available status of user
     * */
    fun updatePresence(from: String, available: Boolean) {
        val realm = Realm.getDefaultInstance()
        realm.executeTransaction(object : Realm.Transaction {
            override fun execute(realm: Realm) {
                val results = realm.where(User::class.java)
                        .equalTo("id", from)
                        .findAll()
                for (user in results) {
                    user.isAvailable = available
                }
            }
        })
    }

}