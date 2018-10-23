package com.inno.innochat.adapter

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import com.inno.innochat.AppHelpers
import com.inno.innochat.R
import com.inno.innochat.model.User
import ir.rainday.easylist.FilterableAdapter
import ir.rainday.easylist.GenericViewHolder
import ir.rainday.easylist.RecyclerViewAdapter

class UsersAdapter(context: Context, onRowClickListener: GenericViewHolder.OnItemClicked<User>) : RecyclerViewAdapter<User>(context), FilterableAdapter {

    init {
        onItemClickListener = onRowClickListener
    }
    override fun getLayout(viewType: Int): Int {
        return  R.layout.item_user_list
    }

    override fun bindView(item: User, position: Int, viewHolder: RecyclerView.ViewHolder) {
        viewHolder as GenericViewHolder
        val nameTV: TextView? = viewHolder.getView<TextView>(R.id.nameTV)
        val lastMsgTV: TextView? = viewHolder.getView<TextView>(R.id.lastMessageTV)
        val lastMsgTimeTV: TextView? = viewHolder.getView<TextView>(R.id.lastMessageTime)
        val userImage: ImageView? = viewHolder.getView<ImageView>(R.id.userImage)

        nameTV?.text = item.nickname
        lastMsgTV?.text = ""
        lastMsgTimeTV?.text = ""
        // load thumbnail
        AppHelpers.loadImage(context, item.avatar)
                .error(R.drawable.ic_person2)
                .fallback(R.drawable.ic_person2)
                .into(userImage)
    }


    override fun filterItem(constraint: CharSequence, item: Any): Boolean {
        val name = (item as User).nickname.toLowerCase()
        Log.d("UsersAdapter","name: $name , constraint: ${constraint.toString().toLowerCase()}")
        return name.contains(constraint.toString().toLowerCase())
    }
}