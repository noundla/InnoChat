package com.inno.innochat.ui

import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.inno.innochat.R
import android.widget.EditText

/**
 * This fragment is used to display Add user screen.
 * User can add a friend by entering jid and name of another user. This helps user to start conversation ahead.
 *
 * @author Sandeep Noundla
 * */
class AddUserDialogFragment : DialogFragment() {

    interface UserAddListener {
        fun onUserAdded(name:String, id:String)
    }

    public var userAddListener : UserAddListener? = null

    private lateinit var mNameET: EditText
    private lateinit var mUserIdET: EditText

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_add_user_dialog, container, false)
        mNameET = view.findViewById(R.id.nameET) as EditText
        mUserIdET = view.findViewById(R.id.userIdET) as EditText
        view.findViewById<View>(R.id.addBtn).setOnClickListener{
            userAddListener?.onUserAdded(mNameET.text.toString(), mUserIdET.text.toString())
            dismiss()
        }
        return view
    }

    override fun getTheme(): Int {
        return R.style.AppTheme_AlertDialog
    }

}
