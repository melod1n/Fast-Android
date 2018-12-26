package ru.stwtforever.fast.adapter

import android.content.Context
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.text.Html
import android.text.SpannableString
import android.text.TextUtils
import android.text.style.ForegroundColorSpan
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

import com.squareup.picasso.Picasso

import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

import java.util.ArrayList
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import ru.stwtforever.fast.R
import ru.stwtforever.fast.api.UserConfig
import ru.stwtforever.fast.api.VKUtils
import ru.stwtforever.fast.api.model.VKConversation
import ru.stwtforever.fast.api.model.VKGroup
import ru.stwtforever.fast.api.model.VKMessage
import ru.stwtforever.fast.api.model.VKUser
import ru.stwtforever.fast.common.ThemeManager
import ru.stwtforever.fast.db.MemoryCache
import ru.stwtforever.fast.helper.FontHelper
import ru.stwtforever.fast.util.ArrayUtil
import ru.stwtforever.fast.util.Utils

class DialogAdapter(context: Context, dialogs: ArrayList<VKConversation>) : RecyclerAdapter<VKConversation, DialogAdapter.ViewHolder>(context, dialogs) {

    init {
        EventBus.getDefault().register(this)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onReceive(data: Array<Any>) {
        if (ArrayUtil.isEmpty(data)) return

        val type = data[0] as Int

        when (type) {
            3 -> {
                val mId = data[1] as Int
                readMessage(mId)
            }
            4 -> {
                val conversation = data[1] as VKConversation
                addMessage(conversation)
            }
            5 -> {
                val message = data[1] as VKMessage
                editMessage(message)
            }
        }
    }

    private fun searchUserPosition(user_id: Int): Int {
        var index = -1
        for (i in 0 until itemCount) {
            val conversation = getItem(i)
            if (conversation.isUser && conversation.last.peerId == user_id) {
                index = i
            }
        }

        return index
    }

    private fun searchPosition(user: VKUser): Int {
        for (i in 0 until values.size) {
            val msg = values[i]
            if (!msg.last.isUser) return -1

            if (msg.last.fromId == user.id) return i
        }

        return -1
    }

    private fun addMessage(conversation: VKConversation) {
        val index = searchMessageIndex(conversation.last.peerId)
        if (index >= 0) {
            val current = getItem(index)

            conversation.photo_50 = current.photo_50
            conversation.photo_100 = current.photo_100
            conversation.photo_200 = current.photo_200
            conversation.pinned = current.pinned
            conversation.title = current.title
            conversation.can_write = current.can_write
            conversation.type = current.type
            conversation.unread++
            conversation.disabled_forever = current.disabled_forever
            conversation.disabled_until = current.disabled_until
            conversation.no_sound = current.no_sound

            if (conversation.last.out) {
                conversation.unread = 0
                conversation.read = false
            }

            values.removeAt(index)
            values.add(0, conversation)
            notifyItemInserted(itemCount - 1)
            notifyItemRangeChanged(0, itemCount, getItem(itemCount - 1))
        } else {
            if (!conversation.last.out)
                conversation.unread++
            values.add(0, conversation)
            notifyItemInserted(itemCount - 1)
            notifyItemRangeChanged(0, itemCount, getItem(itemCount - 1))
        }
    }

    private fun readMessage(id: Int) {
        val position = searchPosition(id)

        if (position == -1) return

        val current = getItem(position)
        current.read = true
        current.unread = 0

        notifyItemChanged(position, current)
    }

    private fun editMessage(edited: VKMessage) {
        val position = searchPosition(edited.id)
        if (position == -1) return

        val current = values[position]
        val last = current.last
        last.mask = edited.mask
        last.text = edited.text
        last.update_time = edited.update_time
        last.attachments = edited.attachments

        notifyItemChanged(position)
    }

    private fun searchPosition(mId: Int): Int {
        for (i in 0 until values.size) {
            val m = values[i].last
            if (m.id == mId) {
                return i
            }
        }

        return -1
    }

    private fun searchPosition(m: VKConversation): Int {
        for (i in 0 until values.size) {
            val msg = values[i]
            if (msg.last.id == m.last.id) {
                return i
            }
        }

        return -1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = inflater.inflate(R.layout.fragment_dialogs_list, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: DialogAdapter.ViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)
        holder.bind(position)
    }

    fun getTitle(item: VKConversation?, user: VKUser?, group: VKGroup?): String? {
        if (item == null) return null

        if (item.isGroup) {
            if (group != null)
                return if (TextUtils.isEmpty(group.name)) "" else group.name
        } else if (item.isUser) {
            if (user != null)
                return if (TextUtils.isEmpty(user.toString())) "" else user.toString()
        } else {
            return if (TextUtils.isEmpty(item.title)) "" else item.title
        }

        return ""
    }

    fun getPhoto(item: VKConversation?, user: VKUser?, group: VKGroup?): String? {
        if (item == null) return null

        if (item.isUser) {
            if (user != null)
                return user.photo_100
        } else if (item.isGroup) {
            if (group != null)
                return group.photo_100
        } else {
            return item.photo_100
        }

        return ""
    }

    fun searchUser(id: Int): VKUser? {
        var user: VKUser? = MemoryCache.getUser(id)
        if (user == null) {
            user = VKUser.EMPTY
        }
        return user
    }

    fun searchGroup(id: Int): VKGroup? {
        var group: VKGroup? = MemoryCache.getGroup(VKGroup.toGroupId(id))
        if (group == null) {
            group = VKGroup.EMPTY
        }
        return group
    }

    fun searchMessageIndex(peerId: Int): Int {
        for (i in 0 until values.size) {
            val conv = getItem(i)
            if (conv.last.peerId == peerId) {
                return i
            }
        }
        return -1
    }

    fun searchMessage(id: Int): VKConversation? {
        for (c in values) {
            if (c.last.id == id) {
                return c
            }
        }
        return null
    }

    fun destroy() {
        values.clear()
        EventBus.getDefault().unregister(this)
    }

    inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {

        var avatar: ImageView
        var avatar_small: ImageView
        var online: ImageView
        var out: ImageView

        var title: TextView
        var body: TextView
        var date: TextView
        var counter: TextView

        var container: LinearLayout

        var p_user: Drawable
        var p_users: Drawable

        var c_pushes_enabled: Int = 0
        var c_pushes_disabled: Int = 0

        init {

            UserConfig.updateUser()

            c_pushes_enabled = ThemeManager.getAccent()
            c_pushes_disabled = if (ThemeManager.isDark()) -0xbababb else -0x333334

            p_user = getDrawable(R.drawable.placeholder_user)
            p_users = getDrawable(R.drawable.placeholder_users)

            avatar = v.findViewById(R.id.avatar)
            avatar_small = v.findViewById(R.id.avatar_small)
            online = v.findViewById(R.id.online)
            out = v.findViewById(R.id.icon_out_message)

            title = v.findViewById(R.id.title)
            body = v.findViewById(R.id.body)
            date = v.findViewById(R.id.date)
            counter = v.findViewById(R.id.counter)

            container = v.findViewById(R.id.container)

            val gd = GradientDrawable()
            gd.setColor(ThemeManager.getAccent())
            gd.cornerRadius = 60f

            counter.background = gd
        }

        fun bind(position: Int) {
            val item = getItem(position) ?: return
            val last = item.last ?: return
            val group = searchGroup(last.fromId)
            val peerGroup = searchGroup(last.peerId)

            val user = searchUser(last.fromId)
            val peerUser = searchUser(last.peerId)

            FontHelper.setFont(title, FontHelper.PS_REGULAR)

            counter.text = if (item.unread > 0) item.unread.toString() else ""
            date.text = Utils.dateFormatter.format(last.date * 1000)

            counter.background.setTint(if (item.isNotificationsDisabled) c_pushes_disabled else c_pushes_enabled)

            body.text = last.text

            title.text = getTitle(item, peerUser, peerGroup)

            avatar_small.visibility = if (!item.isChat && !last.out) View.GONE else View.VISIBLE

            val peerAvatar: String

            if (item.isGroup) {
                peerAvatar = peerGroup!!.photo_100
            } else if (item.isUser) {
                peerAvatar = peerUser!!.photo_100
            } else {
                peerAvatar = item.photo_100
            }

            val fromAvatar: String

            if (last.out && !item.isChat) {
                fromAvatar = UserConfig.user.photo_100
            } else
                fromAvatar = if (item.isFromUser) user!!.photo_100 else group!!.photo_100

            if (TextUtils.isEmpty(fromAvatar)) {
                avatar_small.setImageDrawable(p_user)
            } else {
                Picasso.get()
                        .load(fromAvatar)
                        .priority(Picasso.Priority.HIGH)
                        .placeholder(p_user)
                        .into(avatar_small)
            }

            if (TextUtils.isEmpty(peerAvatar)) {
                avatar.setImageDrawable(if (item.isChat) p_users else p_user)
            } else {
                Picasso.get()
                        .load(peerAvatar)
                        .priority(Picasso.Priority.HIGH)
                        .placeholder(if (item.isChat) p_users else p_user)
                        .into(avatar)
            }

            body.setTextColor(if (!ThemeManager.isDark()) -0x70000000 else -0x6f000001)

            if (TextUtils.isEmpty(last.actionType)) {
                if ((last.attachments != null || !ArrayUtil.isEmpty(last.fwd_messages)) && TextUtils.isEmpty(last.text)) {
                    val body_ = VKUtils.getAttachmentBody(item.last.attachments, item.last.fwd_messages)

                    val r = "<b>$body_</b>"
                    val span = SpannableString(Html.fromHtml(r))
                    span.setSpan(ForegroundColorSpan(ThemeManager.getAccent()), 0, body_.length, 0)

                    body.append(span)
                }
            } else {
                val body_ = VKUtils.getActionBody(last, true)

                body.setTextColor(ThemeManager.getAccent())
                body.text = Html.fromHtml(body_)
            }

            counter.visibility = if (!last.out && item.unread > 0) View.VISIBLE else View.GONE
            out.visibility = if (last.out && !item.read) View.VISIBLE else View.GONE

            if (item.isGroup || item.isChat) {
                online.visibility = View.GONE
            } else {
                if (peerUser!!.online) {
                    online.visibility = View.VISIBLE
                } else {
                    online.visibility = View.GONE
                }
            }
        }
    }

    companion object {

        fun getOnlineIndicator(context: Context, user: VKUser): Drawable? {
            return ContextCompat.getDrawable(context, R.drawable.ic_online_circle)
        }
    }
}