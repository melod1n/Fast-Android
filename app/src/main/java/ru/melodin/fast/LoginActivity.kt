package ru.melodin.fast

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.appcompat.app.AlertDialog
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_login.*
import org.json.JSONObject
import org.jsoup.Jsoup
import ru.melodin.fast.api.Scopes
import ru.melodin.fast.api.UserConfig
import ru.melodin.fast.api.VKApi
import ru.melodin.fast.api.model.VKUser
import ru.melodin.fast.common.TaskManager
import ru.melodin.fast.common.ThemeManager
import ru.melodin.fast.concurrent.LowThread
import ru.melodin.fast.current.BaseActivity
import ru.melodin.fast.database.CacheStorage
import ru.melodin.fast.database.DatabaseHelper
import ru.melodin.fast.util.ArrayUtil
import ru.melodin.fast.util.ColorUtil
import ru.melodin.fast.util.Util
import ru.melodin.fast.util.ViewUtil
import java.util.*

class LoginActivity : BaseActivity() {

    private var login: String? = null
    private var password: String? = null

    private var timer: Timer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(ThemeManager.loginTheme)
        ViewUtil.applyWindowStyles(window, ThemeManager.background)
        setContentView(R.layout.activity_login)

        progress.visibility = View.INVISIBLE
        progress.indeterminateTintList = ColorStateList.valueOf(ColorUtil.saturateColor(ThemeManager.accent, 2f))

        buttonLogin.shrink()
        buttonLogin.extend()

        buttonLogin.setOnClickListener {
            if (!buttonLogin.isExtended) {
                toggleButton()
            } else {
                startTick()
                login(false)
            }
        }

        logoText.setOnClickListener { toggleTheme() }

        if (ThemeManager.isDark) {
            logoText.setTextColor(Color.WHITE)
            val stateList = ColorStateList.valueOf(ThemeManager.accent)
            iconEmail.imageTintList = stateList
            iconKey.imageTintList = stateList
        } else {
            @ColorInt val boxColor = ColorUtil.darkenColor(ThemeManager.background, 0.98f)
            inputLogin.boxBackgroundColor = boxColor
            inputPassword.boxBackgroundColor = boxColor
        }

        val anim = intent.getBooleanExtra("show_anim", true)

        if (anim)
            card.animate().translationY(200f).setDuration(0).withEndAction { card.animate().translationY(0f).setDuration(500).start() }.start()

        val bundle = intent.getBundleExtra("data")
        if (bundle != null)
            onRestoreInstanceState(bundle)

        inputPassword.editText!!.setOnEditorActionListener { _, _, _ ->
            ViewUtil.hideKeyboard(inputPassword.editText!!)
            login(true)
            true
        }

        webLogin!!.setOnClickListener {
            webLogin!!.isEnabled = false
            startWebLogin()
        }
    }

    private fun startTick() {
        timer = Timer()
        timer!!.schedule(object : TimerTask() {
            override fun run() {
                runOnUiThread {
                    if (!buttonLogin.isExtended) toggleButton()
                    ViewUtil.snackbar(buttonLogin, R.string.error).show()
                }
            }
        }, 15000)
    }

    private fun login(fromKeyboard: Boolean) {
        val login = inputLogin.editText!!.text.toString().trim()
        val password = inputPassword.editText!!.text.toString().trim()

        if (login.isEmpty() || password.isEmpty()) {
            if (!fromKeyboard)
                ViewUtil.snackbar(buttonLogin, R.string.all_necessary_data).show()
            return
        }

        this.login = login
        this.password = password

        login(this.login, this.password, "")
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun login(login: String?, password: String?, captcha: String) {
        if (!Util.hasConnection()) {
            ViewUtil.snackbar(buttonLogin, R.string.connect_to_the_internet).show()
            return
        }

        toggleButton()

        val webView = WebView(this)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.loadsImagesAutomatically = false
        webView.settings.userAgentString = "Chrome/41.0.2228.0 Safari/537.36"

        webView.clearCache(true)

        webView.addJavascriptInterface(HandlerInterface(), "HtmlHandler")
        webView.webViewClient = object : WebViewClient() {
            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                if (Util.hasConnection())
                    view.reload()
            }

            override fun onPageFinished(view: WebView, url: String) {
                webView.loadUrl("javascript:window.HtmlHandler.handleHtml" + "('<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>');")
            }
        }

        LowThread {
            val manager = CookieManager.getInstance()
            manager.removeAllCookies(null)
            manager.flush()
            manager.setAcceptCookie(false)

            val url = "https://oauth.vk.com/token?grant_type=password&client_id=2274003&scope=" + Scopes.all() + "&client_secret=hHbZxrka2uZ6jB1inYsH" +
                    "&username=" + login +
                    "&password=" + password +
                    captcha +
                    "&v=5.68"

            runOnUiThread { webView.loadUrl(url) }
        }.start()
    }

    fun authorize(jsonObject: String) {
        TaskManager.execute {

            lateinit var response: JSONObject

            try {
                response = JSONObject(jsonObject)
                runOnUiThread {
                    toggleButton()

                    timer?.cancel()

                    if (response.has("error")) {
                        val errorDescription = response.optString("error_description")

                        when (response.optString("error", getString(R.string.error))) {
                            "need_validation" -> {
                                val redirectUri = response.optString("redirect_uri")
                                val intent = Intent(this@LoginActivity, ValidationActivity::class.java)
                                intent.putExtra("url", redirectUri)
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                startActivityForResult(intent, REQUEST_VALIDATE)
                            }
                            "need_captcha" -> {
                                val captchaImg = response.optString("captcha_img")
                                val captchaSid = response.optString("captcha_sid")
                                showCaptchaDialog(captchaSid, captchaImg)
                            }
                            else -> Snackbar.make(buttonLogin, errorDescription, Snackbar.LENGTH_LONG).show()
                        }
                    } else {
                        UserConfig.userId = response.optInt("user_id", -1)
                        UserConfig.accessToken = response.optString("access_token")
                        UserConfig.save()

                        getCurrentUser(UserConfig.userId)
                        startMainActivity()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread { Toast.makeText(this@LoginActivity, R.string.error, Toast.LENGTH_SHORT).show() }
            }
        }
    }

    private fun showCaptchaDialog(captcha_sid: String, captcha_img: String) {
        val metrics = resources.displayMetrics

        val image = ImageView(this@LoginActivity)
        image.layoutParams = ViewGroup.LayoutParams((metrics.widthPixels / 3.5).toInt(), resources.displayMetrics.heightPixels / 7)

        Picasso.get().load(captcha_img).priority(Picasso.Priority.HIGH).into(image)

        val input = TextInputEditText(this@LoginActivity)

        input.hint = getString(R.string.captcha)
        input.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        val adb = AlertDialog.Builder(this@LoginActivity)

        val layout = LinearLayout(this@LoginActivity)
        layout.orientation = LinearLayout.VERTICAL
        layout.gravity = Gravity.CENTER
        layout.addView(image)
        layout.addView(input)

        adb.setView(layout)
        adb.setNegativeButton(android.R.string.cancel, null)
        adb.setPositiveButton(android.R.string.ok) { _, _ ->
            val captchaCode = input.text!!.toString().trim()
            login(login, password, "&captcha_sid=$captcha_sid&captcha_key=$captchaCode")
        }
        adb.setTitle(R.string.input_text_from_picture)
        adb.setCancelable(true)
        val alert = adb.create()
        alert.show()
    }

    private fun startWebLogin() {
        startActivityForResult(Intent(this, WebViewLoginActivity::class.java), REQUEST_WEB_LOGIN)
    }

    private fun startMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(createBundle(outState))
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)

        val fields = savedInstanceState.getStringArray("fields") ?: return
        inputLogin.editText!!.setText(fields[0])
        inputPassword.editText!!.setText(fields[1])
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (!webLogin!!.isEnabled)
            webLogin!!.isEnabled = true

        if ((requestCode == REQUEST_VALIDATE || requestCode == REQUEST_WEB_LOGIN) && resultCode == Activity.RESULT_OK) {
            val token = data!!.getStringExtra("token")
            val id = data.getIntExtra("id", -1)

            UserConfig.userId = id
            UserConfig.accessToken = token
            UserConfig.save()
            VKApi.config = UserConfig.restore()

            getCurrentUser(id)
            startMainActivity()
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun getCurrentUser(id: Int) {
        TaskManager.execute {
            var user: VKUser?

            VKApi.users().get().userIds(id).fields(VKUser.FIELDS_DEFAULT).execute(VKUser::class.java, object : VKApi.OnResponseListener {
                override fun onSuccess(models: ArrayList<*>?) {
                    if (ArrayUtil.isEmpty(models)) return
                    models ?: return

                    user = models[0] as VKUser?

                    CacheStorage.insert(DatabaseHelper.USERS_TABLE, user!!)
                    UserConfig.getUser()
                }

                override fun onError(e: Exception) {
                    Toast.makeText(this@LoginActivity, R.string.error, Toast.LENGTH_LONG).show()
                }

            })
        }
    }

    private fun createBundle(savedInstanceState: Bundle): Bundle {
        savedInstanceState.putStringArray("fields", arrayOf(inputLogin.editText!!.text.toString().trim(), inputPassword.editText!!.text.toString().trim()))
        return savedInstanceState
    }

    private fun toggleTheme() {
        ThemeManager.toggleTheme()
        applyStyles()
    }

    override fun applyStyles() {
        finish()
        startActivity(intent.putExtra("data", createBundle(Bundle())).putExtra("show_anim", false))
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }

    private fun toggleButton() {
        if (buttonLogin.isExtended) {
            progress.visibility = View.VISIBLE
            buttonLogin.shrink(true)
            buttonLogin.icon = drawable(R.drawable.ic_refresh_black_24dp)
        } else {
            progress.visibility = View.INVISIBLE
            buttonLogin.extend(true)
            buttonLogin.icon = drawable(R.drawable.md_done)
        }
    }

    private inner class HandlerInterface {

        @JavascriptInterface
        fun handleHtml(html: String) {
            val doc = Jsoup.parse(html)
            val response = doc.select("pre[style=\"word-wrap: break-word; white-space: pre-wrap;\"]").first().text()
            runOnUiThread { authorize(response) }
        }
    }

    companion object {
        const val REQUEST_WEB_LOGIN = 1
        const val REQUEST_VALIDATE = 2
    }
}
