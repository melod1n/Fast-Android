package ru.stwtforever.fast;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import ru.stwtforever.fast.api.Auth;
import ru.stwtforever.fast.api.Scopes;
import ru.stwtforever.fast.api.UserConfig;
import ru.stwtforever.fast.common.ThemeManager;
import ru.stwtforever.fast.concurrent.AsyncCallback;
import ru.stwtforever.fast.concurrent.ThreadExecutor;
import ru.stwtforever.fast.helper.FontHelper;
import ru.stwtforever.fast.util.ViewUtils;

public class WebViewLoginActivity extends AppCompatActivity {

    private WebView webView;
    private ProgressBar bar;

    private Toolbar tb;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        ViewUtils.applyWindowStyles(this);
        setTheme(ThemeManager.getCurrentTheme());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_login);

        tb = findViewById(R.id.tb);
        setSupportActionBar(tb);

        ViewUtils.applyToolbarStyles(tb);

        bar = findViewById(R.id.progress);
        webView = findViewById(R.id.web);

        webView.setVisibility(View.GONE);

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
        webView.getSettings().setRenderPriority(WebSettings.RenderPriority.HIGH);
        webView.setWebViewClient(new VKWebViewClient());

        webView.loadUrl(Auth.getUrl(UserConfig.FAST_ID, Scopes.all()));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_login, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.token_login:
                showTokenLoginDialog();
                break;
            case R.id.refresh:
                if (webView == null) break;
                webView.reload();
                break;
            case R.id.back:
                if (webView == null) break;
                if (webView.canGoBack()) webView.goBack();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showTokenLoginDialog() {
        AlertDialog.Builder adb = new AlertDialog.Builder(this);

        View v = LayoutInflater.from(this).inflate(R.layout.token_login, null, false);
        adb.setView(v);
        adb.setMessage(R.string.token_login_message);

        Button ok = v.findViewById(R.id.ok);
        Button cancel = v.findViewById(R.id.cancel);

        final EditText etToken = v.findViewById(R.id.token);
        final EditText etUserId = v.findViewById(R.id.user_id);

        FontHelper.setFont(new EditText[]{etToken, etUserId}, FontHelper.PS_REGULAR);

        final AlertDialog dialog = adb.create();
        dialog.show();

        ok.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                final String token = etToken.getText().toString();
                final String uId = etUserId.getText().toString();

                if (!TextUtils.isEmpty(token) && !TextUtils.isEmpty(uId)) {
                    ThreadExecutor.execute(new AsyncCallback(WebViewLoginActivity.this) {
                        int id;

                        @Override
                        public void ready() {
                            id = Integer.parseInt(uId);
                        }

                        @Override
                        public void done() {
                            Intent intent = new Intent();
                            intent.putExtra("token", token);
                            intent.putExtra("id", id);
                            setResult(Activity.RESULT_OK, intent);
                            finish();
                            dialog.dismiss();
                        }

                        @Override
                        public void error(Exception e) {
                            Toast.makeText(WebViewLoginActivity.this, getString(R.string.error), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        });

        cancel.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }


    private void parseUrl(String url) {
        if (TextUtils.isEmpty(url)) return;

        try {
            if (url.startsWith(Auth.REDIRECT_URL) && !url.contains("error=")) {
                String[] auth = Auth.parseRedirectUrl(url);
                Intent intent = new Intent();
                intent.putExtra("token", auth[0]);
                intent.putExtra("id", Integer.parseInt(auth[1]));
                setResult(Activity.RESULT_OK, intent);
                finish();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (webView != null) {
            webView.removeAllViews();
            webView.clearCache(true);
            webView.destroy();
            webView = null;
        }

    }

    private class VKWebViewClient extends WebViewClient {
        @Override
        public void onPageFinished(WebView view, String url) {
            bar.setVisibility(View.GONE);
            webView.setVisibility(View.VISIBLE);
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            parseUrl(url);
        }
    }
}
