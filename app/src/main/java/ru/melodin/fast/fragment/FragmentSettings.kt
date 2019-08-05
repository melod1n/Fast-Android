package ru.melodin.fast.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.toolbar.*
import org.greenrobot.eventbus.EventBus
import ru.melodin.fast.R
import ru.melodin.fast.adapter.GroupAdapter
import ru.melodin.fast.adapter.UserAdapter
import ru.melodin.fast.api.UserConfig
import ru.melodin.fast.common.AppGlobal
import ru.melodin.fast.common.TaskManager
import ru.melodin.fast.common.ThemeManager
import ru.melodin.fast.current.BaseActivity
import ru.melodin.fast.database.CacheStorage
import ru.melodin.fast.database.DatabaseHelper
import ru.melodin.fast.util.ArrayUtil

class FragmentSettings : PreferenceFragmentCompat(), Preference.OnPreferenceClickListener,
    Preference.OnPreferenceChangeListener {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.prefs, rootKey)

        val hideTyping = findPreference<Preference>(KEY_HIDE_TYPING)
        val darkTheme = findPreference<Preference>(KEY_DARK_STYLE)

        findPreference<Preference>(KEY_ABOUT)!!.onPreferenceClickListener = this
        findPreference<Preference>(KEY_MESSAGES_CLEAR_CACHE)!!.onPreferenceClickListener = this
        findPreference<Preference>(KEY_SHOW_CACHED_USERS)!!.onPreferenceClickListener = this
        findPreference<Preference>(KEY_SHOW_CACHED_GROUPS)!!.onPreferenceClickListener = this

        darkTheme!!.onPreferenceChangeListener = this

        val user = UserConfig.getUser() ?: return
        val hideTypingSummary = String.format(
            getString(R.string.hide_typing_summary),
            user.name,
            user.surname!!.substring(0, 1) + "."
        )
        hideTyping!!.summary = hideTypingSummary
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tb.setBackVisible(true)
        tb.setTitle(R.string.settings)
    }

    private fun switchTheme(dark: Boolean) {
        ThemeManager.switchTheme(dark)
        (activity as BaseActivity).recreate()
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        when (preference.key) {
            KEY_DARK_STYLE -> switchTheme(newValue as Boolean)
        }
        return true
    }

    override fun onPreferenceClick(preference: Preference): Boolean {
        when (preference.key) {
            KEY_ABOUT -> Toast.makeText(
                context,
                String.format(
                    getString(R.string.about_toast),
                    AppGlobal.app_version_name,
                    AppGlobal.app_version_code
                ),
                Toast.LENGTH_LONG
            ).show()
            KEY_MESSAGES_CLEAR_CACHE -> showConfirmClearCacheDialog(users = false, groups = false)
            KEY_SHOW_CACHED_USERS -> showCachedUsers()
            KEY_SHOW_CACHED_GROUPS -> showCachedGroups()
        }
        return true
    }

    @SuppressLint("InflateParams")
    private fun showCachedGroups() {
        val v = layoutInflater.inflate(R.layout.recycler_list, null, false)

        v.findViewById<View>(R.id.refresh).isEnabled = false
        v.findViewById<View>(R.id.emptyView).visibility = View.GONE
        val list = v.findViewById<RecyclerView>(R.id.list)

        val manager = LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
        list.setHasFixedSize(true)
        list.layoutManager = manager

        val groups = CacheStorage.groups
        if (ArrayUtil.isEmpty(groups)) {
            Toast.makeText(activity, R.string.no_data, Toast.LENGTH_SHORT).show()
            return
        }

        val adapter = GroupAdapter(context!!, groups!!)
        list.adapter = adapter

        val adb = AlertDialog.Builder(context!!)
        adb.setTitle(R.string.cached_groups)

        adb.setView(v)
        adb.setPositiveButton(android.R.string.ok, null)
        adb.setNeutralButton(R.string.clear) { _, _ ->
            showConfirmClearCacheDialog(
                users = false,
                groups = true
            )
        }
        adb.show()
    }

    @SuppressLint("InflateParams")
    private fun showCachedUsers() {
        val v = layoutInflater.inflate(R.layout.recycler_list, null, false)

        v.findViewById<View>(R.id.refresh).isEnabled = false
        v.findViewById<View>(R.id.emptyView).visibility = View.GONE
        val list = v.findViewById<RecyclerView>(R.id.list)

        val manager = LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
        list.setHasFixedSize(true)
        list.layoutManager = manager

        val users = CacheStorage.users
        if (ArrayUtil.isEmpty(users)) {
            Toast.makeText(activity, R.string.no_data, Toast.LENGTH_SHORT).show()
            return
        }

        val adapter = UserAdapter(context!!, users)
        list.adapter = adapter

        val adb = AlertDialog.Builder(context!!)
        adb.setTitle(R.string.cached_users)

        adb.setView(v)
        adb.setPositiveButton(android.R.string.ok, null)
        adb.setNeutralButton(R.string.clear) { _, _ ->
            showConfirmClearCacheDialog(
                users = true,
                groups = false
            )
        }
        adb.show()
    }

    private fun showConfirmClearCacheDialog(users: Boolean, groups: Boolean) {
        AlertDialog.Builder(activity!!)
            .setTitle(R.string.confirmation)
            .setMessage(R.string.clear_cache_confirm)
            .setPositiveButton(R.string.yes) { _, _ ->
                TaskManager.execute {
                    val helper = DatabaseHelper.getInstance(activity!!)
                    val db = AppGlobal.database

                    when {
                        users -> helper.dropUsersTable(db)
                        groups -> helper.dropGroupsTable(db)
                        else -> {
                            helper.dropMessagesTable(db)
                            EventBus.getDefault().postSticky(arrayOf<Any>(KEY_MESSAGES_CLEAR_CACHE))
                        }
                    }
                }
            }
            .setNegativeButton(R.string.no, null)
            .show()
    }

    companion object {
        const val KEY_NOT_READ_MESSAGES = "not_read"
        const val KEY_DARK_STYLE = "dark_theme"
        const val KEY_MESSAGE_TEMPLATE = "template"
        const val KEY_HIDE_TYPING = "hide_typing"
        const val KEY_SHOW_ERROR = "show_error"
        const val DEFAULT_TEMPLATE_VALUE = "¯\\_(ツ)_/¯"
        const val KEY_MESSAGES_CLEAR_CACHE = "clear_messages_cache"
        const val KEY_HIDE_KEYBOARD_ON_SCROLL = "hide_keyboard_on_scroll"
        const val KEY_OFFLINE = "offline"

        private const val KEY_ABOUT = "about"
        private const val KEY_SHOW_CACHED_GROUPS = "show_cached_groups"
        private const val KEY_SHOW_CACHED_USERS = "show_cached_users"

        const val KEY_CRASH_LOG = "crashLog"
        const val KEY_CRASHED = "isCrashed"
    }

}
