package com.inno.innochat.ui

import android.app.Dialog
import android.app.DialogFragment
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import com.inno.innochat.R

/**
 * This fragment is used to display loading screen
 *
 * @author Sandeep Noundla
 * */
class LoaderDialogFragment : DialogFragment() {
    companion object {
        val TAG = LoaderDialogFragment::class.java.simpleName
    }
    init {
        // Required empty public constructor
        isCancelable = false
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_loader_dialog, container, false)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window!!.setBackgroundDrawable(ColorDrawable(0))
        // request a window without the title
        dialog.window!!.requestFeature(Window.FEATURE_NO_TITLE)
        return dialog
    }
}