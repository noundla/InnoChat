package com.inno.innochat.adapter

import android.content.Context
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.widget.ImageView
import android.widget.TextView
import com.inno.innochat.AppHelpers
import com.inno.innochat.DateUtils
import com.inno.innochat.R
import com.inno.innochat.model.Message
import com.inno.innochat.model.me
import ir.rainday.easylist.GenericViewHolder
import ir.rainday.easylist.RecyclerViewAdapter

class MessagingAdapter(context: Context) : RecyclerViewAdapter<Message>(context) {

    companion object {
        private val VIEW_TYPE_MESSAGE_SENT = 1
        private val VIEW_TYPE_MESSAGE_RECEIVED = 2
    }


    override fun getLayout(viewType: Int): Int {
        return if (viewType == VIEW_TYPE_MESSAGE_SENT)
            R.layout.item_message_sent
        else
            R.layout.item_message_received
    }

    override fun bindView(item: Message, position: Int, viewHolder: RecyclerView.ViewHolder) {
        viewHolder as GenericViewHolder

        viewHolder.getView<TextView>(R.id.text_message_body)?.text = item.message
        viewHolder.getView<TextView>(R.id.text_message_time)?.text = DateUtils.formatDateTime(item.createdAt)
        viewHolder.getView<TextView>(R.id.text_message_name)?.text = item.sender.nickname
        val senderImage: ImageView? = viewHolder.getView<ImageView>(R.id.image_message_profile)
        // load thumbnail
        AppHelpers.loadImage(context, item.sender.avatar)
                .error(R.drawable.ic_person1)
                .fallback(R.drawable.ic_person1)
                .into(senderImage)
    }

    override fun getItemType(position: Int): Int {
        return if (items!![position].sender == me) VIEW_TYPE_MESSAGE_SENT else VIEW_TYPE_MESSAGE_RECEIVED
    }

}