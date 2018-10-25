package com.inno.innochat.model

import io.realm.RealmObject
import ir.rainday.easylist.Diffable

/**
 * @author Sandeep Noundla
 * */
open class Message(
        var message: String = "",
        var from : String = "",
        var to :String ="",
        var createdAt: Long = 0
) : RealmObject(), Diffable {
    override val diffableIdentity: String
        get() = createdAt.toString()

    override fun isEqualTo(other: Any): Boolean {

        if (other is Message)
            return this == other

        return false
    }
}