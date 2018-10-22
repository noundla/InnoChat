package com.inno.innochat.model

import android.os.Parcel
import android.os.Parcelable
import com.inno.innochat.R
import ir.rainday.easylist.Diffable

data class Message(
        val message: String,
        val sender: User,
        val createdAt: Long
) : Diffable {
    override val diffableIdentity: String
        get() = createdAt.toString()

    override fun isEqualTo(other: Any): Boolean {

        if (other is Message)
            return this == other

        return false
    }
}

data class User(val id: String,
                val nickname: String,
                val avatar: String): Parcelable {
    constructor(parcel: Parcel) : this(
            parcel.readString(),
            parcel.readString(),
            parcel.readString()) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(nickname)
        parcel.writeString(avatar)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<User> {
        override fun createFromParcel(parcel: Parcel): User {
            return User(parcel)
        }

        override fun newArray(size: Int): Array<User?> {
            return arrayOfNulls(size)
        }
    }
}

var me: User = User("101","me", "")
var honey: User = User("102","honey", "")