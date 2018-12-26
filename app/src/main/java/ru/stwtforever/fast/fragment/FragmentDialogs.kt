package ru.stwtforever.fast.fragment

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast

import org.greenrobot.eventbus.EventBus

import java.util.ArrayList
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import ru.stwtforever.fast.CreateChatActivity
import ru.stwtforever.fast.MessagesActivity
import ru.stwtforever.fast.R
import ru.stwtforever.fast.adapter.DialogAdapter
import ru.stwtforever.fast.adapter.RecyclerAdapter
import ru.stwtforever.fast.api.UserConfig
import ru.stwtforever.fast.api.VKApi
import ru.stwtforever.fast.api.model.VKConversation
import ru.stwtforever.fast.api.model.VKGroup
import ru.stwtforever.fast.api.model.VKMessage
import ru.stwtforever.fast.cls.BaseFragment
import ru.stwtforever.fast.common.ThemeManager
import ru.stwtforever.fast.concurrent.AsyncCallback
import ru.stwtforever.fast.concurrent.ThreadExecutor
import ru.stwtforever.fast.db.CacheStorage
import ru.stwtforever.fast.db.DatabaseHelper
import ru.stwtforever.fast.db.MemoryCache
import ru.stwtforever.fast.service.LongPollService
import ru.stwtforever.fast.util.ArrayUtil
import ru.stwtforever.fast.util.Utils
import ru.stwtforever.fast.util.ViewUtils

class FragmentDialogs : BaseFragment(), SwipeRefreshLayout.OnRefreshListener, RecyclerAdapter.OnItemClickListener, RecyclerAdapter.OnItemLongClickListener {

    private var refreshLayout: SwipeRefreshLayout? = null

    private var tb: Toolbar? = null

    private var adapter: DialogAdapter? = null

    private var loading: Boolean = false

    override fun onRefresh() {
        getDialogs(0, DIALOGS_COUNT)
    }

    override fun onDestroy() {
        if (adapter != null) {
            adapter!!.destroy()
        }
        super.onDestroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        this.title = getString(R.string.fragment_messages)
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_dialogs, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val list: RecyclerView? = view.findViewById(R.id.list)
        tb = view.findViewById(R.id.tb)

        ViewUtils.applyToolbarStyles(tb!!)

        tb!!.title = title

        tb!!.inflateMenu(R.menu.fragment_dialogs_menu)
        tb!!.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.create_chat) {
                startActivity(Intent(activity, CreateChatActivity::class.java))
            } else if (item.itemId == R.id.restart_longpoll) {
                activity!!.stopService(Intent(activity, LongPollService::class.java))
                activity!!.startService(Intent(activity, LongPollService::class.java))
            }
            true
        }

        for (i in 0 until tb!!.menu.size()) {
            val item = tb!!.menu.getItem(i)
            item.icon.setTint(ViewUtils.mainColor)
        }

        setList(list)

        refreshLayout = view.findViewById(R.id.refresh)
        refreshLayout!!.setColorSchemeColors(ThemeManager.getAccent())
        refreshLayout!!.setOnRefreshListener(this)
        refreshLayout!!.setProgressBackgroundColorSchemeColor(ThemeManager.getBackground())

        val manager = LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
        list!!.setHasFixedSize(true)
        list.layoutManager = manager

        getMessages()
    }

    private fun showDialog(position: Int) {
        val features = arrayOf(getString(R.string.clean_history))

        val item = adapter!!.values[position]
        val group = CacheStorage.getGroup(VKGroup.toGroupId(item.last.peerId))
        val user = CacheStorage.getUser(item.last.peerId)

        val adb = AlertDialog.Builder(activity!!)
        adb.setTitle(adapter!!.getTitle(item, user, group))
        adb.setItems(features, object : DialogInterface.OnClickListener {

            override fun onClick(dialog: DialogInterface, which: Int) {
                when (which) {
                    0 -> showDeleteConfirmDialog()
                }
            }

            private fun showDeleteConfirmDialog() {
                val adb = AlertDialog.Builder(activity!!)
                adb.setTitle(R.string.warning)
                adb.setMessage(R.string.confirm_delete_dialog_message)
                adb.setPositiveButton(R.string.yes, object : DialogInterface.OnClickListener {

                    override fun onClick(dialog: DialogInterface, which: Int) {
                        deleteDialog()
                    }

                    private fun deleteDialog() {
                        ThreadExecutor.execute(object : AsyncCallback(activity) {
                            internal var response = -1

                            @Throws(Exception::class)
                            override fun ready() {
                                val m = adapter!!.values[position]
                                response = VKApi.messages().deleteConversation().peerId(m.last.peerId.toLong()).execute(Int::class.java)[0]
                            }

                            override fun done() {
                                adapter!!.values.removeAt(position)
                                adapter!!.notifyDataSetChanged()
                            }

                            override fun error(e: Exception) {
                                Toast.makeText(activity, getString(R.string.error), Toast.LENGTH_LONG).show()
                            }
                        })
                    }

                })
                adb.setNegativeButton(R.string.no, null)
                adb.show()
            }
        })
        adb.show()
    }

    internal fun getMessages() {
        getCachedDialogs()
        getDialogs(0, DIALOGS_COUNT)
    }

    private fun createAdapter(messages: ArrayList<VKConversation>, offset: Int) {
        if (ArrayUtil.isEmpty(messages)) {
            return
        }
        if (offset != 0) {
            adapter!!.changeItems(messages)
            adapter!!.notifyDataSetChanged()
            return
        }

        if (adapter != null) {
            adapter!!.changeItems(messages)
            adapter!!.notifyDataSetChanged()
            return
        }
        adapter = DialogAdapter(activity, messages)
        list!!.adapter = adapter
        adapter!!.setOnItemClickListener(this)
        adapter!!.setOnItemLongClickListener(this)
    }

    private fun getCachedDialogs() {
        val dialogs = CacheStorage.getDialogs()
        if (ArrayUtil.isEmpty(dialogs)) {
            return
        }

        createAdapter(dialogs, 0)
    }

    private fun getDialogs(offset: Int, count: Int) {
        if (!Utils.hasConnection()) {
            refreshLayout!!.isRefreshing = false
            return
        }

        refreshLayout!!.isRefreshing = true
        ThreadExecutor.execute(object : AsyncCallback(activity) {
            private lateinit var messages: ArrayList<VKConversation>

            @Throws(Exception::class)
            override fun ready() {
                messages = VKApi.messages().conversations.filter("all").extended(true).offset(offset).count(count).execute(VKConversation::class.java)

                if (messages!!.isEmpty()) {
                    loading = true
                }

                if (offset == 0) {
                    CacheStorage.delete(DatabaseHelper.DIALOGS_TABLE)
                    CacheStorage.insert(DatabaseHelper.DIALOGS_TABLE, messages)
                }

                val users = messages!![0].profiles
                val groups = messages!![0].groups
                val last_messages = ArrayList<VKMessage>()

                for (i in messages!!.indices) {
                    val last = messages!![i].last
                    last_messages.add(last)
                }

                if (!ArrayUtil.isEmpty(last_messages))
                    CacheStorage.insert(DatabaseHelper.MESSAGES_TABLE, last_messages)

                if (!ArrayUtil.isEmpty(users))
                    CacheStorage.insert(DatabaseHelper.USERS_TABLE, users)

                if (!ArrayUtil.isEmpty(groups))
                    CacheStorage.insert(DatabaseHelper.GROUPS_TABLE, messages!![0].groups)
            }

            override fun done() {
                EventBus.getDefault().postSticky(MemoryCache.getUser(UserConfig.userId))
                createAdapter(messages, offset)
                refreshLayout!!.isRefreshing = false

                if (!messages!!.isEmpty()) {
                    loading = false
                }
            }

            override fun error(e: Exception) {
                refreshLayout!!.isRefreshing = false
            }
        })
    }

    private fun openChat(position: Int) {
        val c = adapter!!.values[position]
        val user = CacheStorage.getUser(c.last.peerId)
        val g = CacheStorage.getGroup(VKGroup.toGroupId(c.last.peerId))

        val intent = Intent(activity, MessagesActivity::class.java)
        intent.putExtra("title", adapter!!.getTitle(c, user, g))
        intent.putExtra("photo", adapter!!.getPhoto(c, user, g))
        intent.putExtra("conversation", c)
        intent.putExtra("peer_id", c.last.peerId)
        intent.putExtra("can_write", c.can_write)

        if (!c.can_write) {
            intent.putExtra("reason", c.reason)
        }

        startActivity(intent)
    }

    override fun onItemClick(v: View, position: Int) {
        openChat(position)
        val conversation = adapter!!.getItem(position)

        if (!conversation.read && !Utils.getPrefs().getBoolean(FragmentSettings.KEY_NOT_READ_MESSAGES, false)) {
            readMessage(position)
        }
    }

    private fun readMessage(position: Int) {
        ThreadExecutor.execute(object : AsyncCallback(activity) {
            @Throws(Exception::class)
            override fun ready() {
                VKApi.messages().markAsRead().peerId(adapter!!.getItem(position).last.peerId.toLong()).execute(Int::class.java)
            }

            override fun done() {}

            override fun error(e: Exception) {

            }
        })
    }


    override fun onItemLongClick(v: View, position: Int) {
        showDialog(position)
    }

    companion object {


        private val DIALOGS_COUNT = 60
    }
}


