package com.inno.innochat.model

import android.content.Context
import android.preference.PreferenceManager
import com.inno.innochat.Constants

class UsersModel {
    val users = arrayOf(arrayOf("zuck123@im.koderoot.net", "Zuck", "https://fortunedotcom.files.wordpress.com/2018/07/gettyimages-961697338.jpg"),
            arrayOf("ob123@im.koderoot.net", "Obama", "http://img.timeinc.net/time/photoessays/2008/people_who_mattered/obama_main_1216.jpg"),
            arrayOf("james123@im.koderoot.net", "James", "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcT9Xc44TEpV4ySTduEsyDn2RrDwEJrJ0sPwwv7I-wLBMpYanCX8"),
            arrayOf("kayla@im.koderoot.net", "Kayla", "https://engineering.unl.edu/images/staff/Kayla_Person-small.jpg"),
            arrayOf("honey@im.koderoot.net", "Honey", ""))
    companion object {
        private var sUserModel : UsersModel? = null

        fun getInstance() : UsersModel {
            if (sUserModel == null) {
                sUserModel = UsersModel()
            }
            return sUserModel!!
        }
    }
    public var currentUser : User? = null

    private var mUsers : ArrayList<User> = ArrayList()

    private constructor() {

    }

    fun prepareInitialUsers() {
        mUsers = ArrayList()
        for (item in users) {
            if (item[0] != currentUser?.id) {
                mUsers.add(User(item[0], item[1], item[2]))
            }
        }
    }


    fun addUser(userId:String, name:String, avatar:String="") {
        mUsers.add(User(userId, name, avatar))
    }

    fun getUsers(): List<User> {
        return mUsers
    }

    fun prepareCurrentUser(context: Context) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val cId = prefs.getString(Constants.SP_USER_NAME,"")
        for (item in users) {
            if (item[0] == cId) {
                currentUser = User(item[0], "You", item[2])
                break
            }
        }
    }

}