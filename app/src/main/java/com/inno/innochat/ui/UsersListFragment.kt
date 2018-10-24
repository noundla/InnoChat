package com.inno.innochat.ui


import android.app.SearchManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SearchView
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import com.inno.innochat.AppHelpers
import com.inno.innochat.Constants

import com.inno.innochat.R
import com.inno.innochat.adapter.UsersAdapter
import com.inno.innochat.model.Message
import com.inno.innochat.model.MessagingModel
import com.inno.innochat.model.User
import com.inno.innochat.model.UsersModel
import com.inno.innochat.xmpp.InnoChatConnectionService
import io.realm.RealmChangeListener
import io.realm.RealmResults
import ir.rainday.easylist.FilterableAdapter
import ir.rainday.easylist.GenericViewHolder
import ir.rainday.easylist.RecyclerViewAdapter
import ir.rainday.easylist.setEmptyView
import kotlinx.android.synthetic.main.fragment_users_list.*
import java.util.*
import org.jxmpp.jid.impl.JidCreate
import org.jxmpp.jid.EntityBareJid



class UsersListFragment : Fragment(), SearchView.OnQueryTextListener, GenericViewHolder.OnItemClicked<User> {

    private lateinit var searchView: SearchView
    private lateinit var mBroadcastReceiver: BroadcastReceiver
    private var mNavigationListener : NavigationListener? = null
    private var mRealmUsers: RealmResults<User>? = null

    private val mRecyclerView: RecyclerView by lazy {
        val linearLayoutManager = LinearLayoutManager(context!!, LinearLayoutManager.VERTICAL, false)
        recyclerView?.layoutManager = linearLayoutManager
        recyclerView
    }

    private val adapter: RecyclerViewAdapter<User> by lazy {
        UsersAdapter(context!!, this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_users_list, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateTitle()
        mRecyclerView.adapter = adapter
        mRecyclerView.setEmptyView(R.layout.layout_no_item)
        loadUsers()
        activity!!.findViewById<View>(R.id.fab).setOnClickListener{
            val fragment = AddUserDialogFragment()
            fragment.userAddListener = object : AddUserDialogFragment.UserAddListener {
                override fun onUserAdded(name: String, id: String) {
                    var uID = id
                    if (!uID.contains("@")) {
                        uID = "$uID@${Constants.HOST}"
                    }

                    UsersModel.getInstance().addUser(uID, name)

                    val i = Intent(InnoChatConnectionService.ADD_USER)
                    i.putExtra(InnoChatConnectionService.BUNDLE_TO, uID)
                    i.setPackage(context!!.packageName)
                    context!!.sendBroadcast(i)

                }
            }
            fragment.show(childFragmentManager, "AddUserDialogFragment")
        }
    }


    override fun onStart() {
        super.onStart()
        updateTitle()
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (context is NavigationListener) {
            mNavigationListener = context
        }
    }

    override fun onDetach() {
        super.onDetach()
        mNavigationListener = null
    }

    private fun updateTitle() {
        val activity = activity as AppCompatActivity
        activity.supportActionBar.let { actionBar ->
            actionBar!!.title = getString(R.string.inno_chat)
            actionBar.setDisplayHomeAsUpEnabled(false)
            actionBar.setHomeButtonEnabled(false)
        }
    }

    private fun loadUsers() {
        mRealmUsers = UsersModel.getInstance().getUsers()
        mRealmUsers!!.addChangeListener(object : RealmChangeListener<RealmResults<User>>{
            override fun onChange(users: RealmResults<User>?) {
                adapter.items = mRealmUsers
            }
        })
        adapter.items = mRealmUsers
    }

    override fun onRecyclerViewItemClicked(adapter: RecyclerView.Adapter<*>, view: View, position: Int, item: User) {
        mNavigationListener?.showChatScreen(item)
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)
        if (menu!=null && inflater!=null) {
            inflater.inflate(R.menu.menu_user_filter, menu)

            val searchManager = context!!.getSystemService(Context.SEARCH_SERVICE) as SearchManager
            val searchMenuItem = menu.findItem(R.id.search)
            searchView = searchMenuItem.actionView as SearchView

            searchView.setSearchableInfo(searchManager.getSearchableInfo(activity!!.componentName))
            searchView.setOnQueryTextListener(this)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when(item?.itemId){
            R.id.logout -> {
                val intent = Intent(context, InnoChatConnectionService::class.java)
                context?.stopService(intent)
                mNavigationListener?.showLoginScreen()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        return false
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        (adapter as FilterableAdapter).setFilterConstraint(newText)
        return true
    }

}
