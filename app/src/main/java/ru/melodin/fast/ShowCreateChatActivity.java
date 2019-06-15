package ru.melodin.fast;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import ru.melodin.fast.adapter.ShowCreateAdapter;
import ru.melodin.fast.api.VKApi;
import ru.melodin.fast.api.model.VKUser;
import ru.melodin.fast.common.ThemeManager;
import ru.melodin.fast.concurrent.AsyncCallback;
import ru.melodin.fast.concurrent.ThreadExecutor;
import ru.melodin.fast.util.ColorUtil;
import ru.melodin.fast.util.ViewUtil;

public class ShowCreateChatActivity extends AppCompatActivity {

    private ShowCreateAdapter adapter;

    private EditText title;
    private Toolbar tb;
    private RecyclerView list;

    private ArrayList<VKUser> users;

    private View empty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(ThemeManager.getCurrentTheme());
        ViewUtil.applyWindowStyles(getWindow());
        super.onCreate(savedInstanceState);

        users = (ArrayList<VKUser>) getIntent().getSerializableExtra("users");

        setContentView(R.layout.activity_show_create);
        initViews();

        setSupportActionBar(tb);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.md_clear);

        getSupportActionBar().setTitle(R.string.create_chat);

        tb.getNavigationIcon().setTint(ThemeManager.getMain());

        findViewById(R.id.refresh).setEnabled(false);

        title.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                invalidateOptionsMenu();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }

        });

        LinearLayoutManager manager = new LinearLayoutManager(this);
        manager.setOrientation(RecyclerView.VERTICAL);

        list.setHasFixedSize(true);
        list.setLayoutManager(manager);

        createAdapter();
    }

    private void initViews() {
        title = findViewById(R.id.title);
        tb = findViewById(R.id.tb);
        list = findViewById(R.id.list);
        empty = findViewById(R.id.no_items_layout);
    }

    private void createAdapter() {
        adapter = new ShowCreateAdapter(this, users);
        list.setAdapter(adapter);

        tb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                list.scrollToPosition(0);
            }
        });

        checkCount();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
            case R.id.create:
                if (adapter != null)
                    createChat();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void checkCount() {
        empty.setVisibility(adapter == null ? View.VISIBLE : adapter.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void createChat() {
        ThreadExecutor.execute(new AsyncCallback(this) {

            int peerId;
            StringBuilder title_;

            @Override
            public void ready() {
                ArrayList<Integer> ids = new ArrayList<>();
                for (VKUser user : adapter.getValues()) {
                    ids.add(user.getId());
                }

                title_ = new StringBuilder(title.getText().toString().trim());

                if (TextUtils.isEmpty(title_.toString())) {
                    if (users.size() == 1) {
                        title_.append(users.get(0).getName());
                    } else
                        for (int i = 0; i < users.size(); i++) {
                            VKUser user = adapter.getItem(i);
                            title_.append(user.getName()).append(i == users.size() ? "" : ", ");
                        }
                }

                VKApi.messages().createChat().title(title_.toString()).userIds(ids).execute(Integer.class, new VKApi.OnResponseListener<Integer>() {
                    @Override
                    public void onSuccess(ArrayList<Integer> models) {
                        peerId = 2000000000 + models.get(0);

                        Intent intent = new Intent();
                        intent.putExtra("title", title_.toString());
                        intent.putExtra("peer_id", peerId);
                        setResult(Activity.RESULT_OK, intent);
                        finish();
                    }

                    @Override
                    public void onError(Exception exception) {
                        Log.e("Error create chat", Log.getStackTraceString(exception));
                    }
                });
            }

            @Override
            public void done() {

            }

            @Override
            public void error(Exception e) {
                Log.e("Error create chat", Log.getStackTraceString(e));
                Toast.makeText(ShowCreateChatActivity.this, getString(R.string.error) + ": " + e.toString(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_create_chat, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.create).getIcon().setTint(ColorUtil.alphaColor(ThemeManager.getMain()));
        return super.onPrepareOptionsMenu(menu);
    }
}
