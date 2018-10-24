package com.inno.innochat.ui


import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.inno.innochat.R
import com.inno.innochat.R.id.view
import android.widget.EditText



class AddUserDialogFragment : DialogFragment() {

    interface UserAddListener {
        fun onUserAdded(name:String, id:String)
    }

    public var userAddListener : UserAddListener? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setTitle(getString(R.string.add_person))
        return dialog
    }

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
