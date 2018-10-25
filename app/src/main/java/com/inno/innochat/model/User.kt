package com.inno.innochat.model

import android.os.Parcel
import android.os.Parcelable
import io.realm.RealmObject
/**
 * @author Sandeep Noundla
 * */
open class User(var id: String="",
                var name: String="",
                var avatar: String="",
                var isAvailable:Boolean=false,
                var isCommTo : String = ""): RealmObject(), Parcelable {
    constructor(parcel: Parcel) : this(
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readByte() != 0.toByte(),
            parcel.readString()) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(name)
        parcel.writeString(avatar)
        parcel.writeByte(if (isAvailable) 1 else 0)
        parcel.writeString(isCommTo)
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