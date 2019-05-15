package ru.melodin.fast.fragment;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import ru.melodin.fast.MessagesActivity;
import ru.melodin.fast.R;
import ru.melodin.fast.adapter.FriendAdapter;
import ru.melodin.fast.api.UserConfig;
import ru.melodin.fast.api.VKApi;
import ru.melodin.fast.api.model.VKConversation;
import ru.melodin.fast.api.model.VKUser;
import ru.melodin.fast.common.ThemeManager;
import ru.melodin.fast.concurrent.AsyncCallback;
import ru.melodin.fast.concurrent.ThreadExecutor;
import ru.melodin.fast.current.BaseFragment;
import ru.melodin.fast.database.CacheStorage;
import ru.melodin.fast.database.DatabaseHelper;
import ru.melodin.fast.util.ArrayUtil;
import ru.melodin.fast.util.Util;

public class FragmentFriends extends BaseFragment implements SwipeRefreshLayout.OnRefreshListener {

    private Toolbar tb;
    private View empty;
    private RecyclerView list;
    private SwipeRefreshLayout refreshLayout;

    private FriendAdapter adapter;

    @Override
    public void onRefresh() {
        getFriends(0, 0);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.title = getString(R.string.fragment_friends);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        initViews(view);

        tb.setTitle(title);

        refreshLayout.setOnRefreshListener(this);
        refreshLayout.setColorSchemeColors(ThemeManager.getAccent());
        refreshLayout.setProgressBackgroundColorSchemeColor(ThemeManager.getBackground());

        LinearLayoutManager manager = new LinearLayoutManager(getActivity(), RecyclerView.VERTICAL, false);
        list.setHasFixedSize(true);
        list.setLayoutManager(manager);

        getCachedFriends();
        getFriends(0, 0);
    }

    private void initViews(View v) {
        tb = v.findViewById(R.id.tb);
        empty = v.findViewById(R.id.no_items_layout);
        list = v.findViewById(R.id.list);
        refreshLayout = v.findViewById(R.id.refresh);
    }

    private void checkCount() {
        empty.setVisibility(adapter == null ? View.VISIBLE : adapter.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void createAdapter(ArrayList<VKUser> friends, int offset) {
        if (ArrayUtil.isEmpty(friends)) return;

        checkCount();

        if (adapter == null) {
            adapter = new FriendAdapter(this, friends);
            list.setAdapter(adapter);

            checkCount();
            return;
        }

        if (offset != 0) {
            adapter.changeItems(friends);
            adapter.notifyDataSetChanged();

            checkCount();
            return;
        }

        adapter.changeItems(friends);
        adapter.notifyDataSetChanged();

        checkCount();
    }

    private void getCachedFriends() {
        ArrayList<VKUser> users = CacheStorage.getFriends(UserConfig.userId, false);

        if (ArrayUtil.isEmpty(users))
            return;

        createAdapter(users, 0);
    }

    private void getFriends(final int count, final int offset) {
        if (!Util.hasConnection()) {
            refreshLayout.setRefreshing(false);
            return;
        }

        refreshLayout.setRefreshing(true);

        ThreadExecutor.execute(new AsyncCallback(getActivity()) {

            private ArrayList<VKUser> users;

            @Override
            public void ready() throws Exception {
                users = VKApi.friends().get().userId(UserConfig.userId).order("hints").fields(VKUser.FIELDS_DEFAULT).execute(VKUser.class);

                if (offset == 0) {
                    CacheStorage.delete(DatabaseHelper.FRIENDS_TABLE);
                    CacheStorage.insert(DatabaseHelper.FRIENDS_TABLE, users);
                }

                CacheStorage.insert(DatabaseHelper.USERS_TABLE, users);
            }

            @Override
            public void done() {
                createAdapter(users, offset);
                refreshLayout.setRefreshing(false);
            }

            @Override
            public void error(Exception e) {
                refreshLayout.setRefreshing(false);
                Toast.makeText(getActivity(), getString(R.string.error), Toast.LENGTH_LONG).show();
            }
        });
    }

    public void openChat(int position) {
        VKUser user = adapter.getItem(position);

        Intent intent = new Intent(getActivity(), MessagesActivity.class);
        intent.putExtra("title", user.toString());
        intent.putExtra("photo", user.photo_200);
        intent.putExtra("peer_id", user.id);

        boolean canWrite = !user.isDeactivated();

        intent.putExtra("can_write", canWrite);

        if (!canWrite) {
            intent.putExtra("reason", VKConversation.REASON_USER_BLOCKED_DELETED);
        }

        startActivity(intent);
    }

    public void showDialog(final int position, View v) {
        PopupMenu menu = new PopupMenu(getActivity(), v);
        menu.inflate(R.menu.fragment_friends_funcs);
        menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                showConfirmDeleteFriend(position);
                return true;
            }
        });
        menu.show();
    }

    private void showConfirmDeleteFriend(final int position) {
        AlertDialog.Builder adb = new AlertDialog.Builder(getActivity());
        adb.setTitle(R.string.confirmation);
        adb.setMessage(R.string.confirm_delete_friend);
        adb.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                deleteFriend(position);
            }
        });
        adb.setNegativeButton(R.string.no, null);
        adb.create().show();
    }

    private void deleteFriend(final int position) {
        if (!Util.hasConnection()) {
            refreshLayout.setRefreshing(false);
            return;
        }

        refreshLayout.setRefreshing(true);

        ThreadExecutor.execute(new AsyncCallback(getActivity()) {

            @Override
            public void ready() throws Exception {
                VKApi.friends().delete().userId(adapter.getItem(position).getId()).execute(Integer.class);
            }

            @Override
            public void done() {
                adapter.remove(position);
                adapter.notifyDataSetChanged();
                refreshLayout.setRefreshing(false);
            }

            @Override
            public void error(Exception e) {
                Log.e("Error delete friend", Log.getStackTraceString(e));
                refreshLayout.setRefreshing(false);
                Toast.makeText(getActivity(), getString(R.string.error) + "!", Toast.LENGTH_SHORT).show();
            }
        });
    }

}