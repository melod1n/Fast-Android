package ru.melodin.fast.util

import android.content.Context
import android.os.Build
import android.view.View
import android.view.Window
import android.view.inputmethod.InputMethodManager
import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import com.google.android.material.snackbar.Snackbar
import ru.melodin.fast.common.AppGlobal
import ru.melodin.fast.common.ThemeManager

object ViewUtil {

    private val keyboard = AppGlobal.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

    fun showKeyboard(v: View) {
        keyboard.showSoftInput(v, 0)
    }

    fun hideKeyboard(v: View) {
        keyboard.hideSoftInputFromWindow(v.windowToken, 0)
    }

    @JvmOverloads
    fun applyWindowStyles(window: Window, @ColorInt color: Int = ThemeManager.primary) {
        val light = ColorUtil.isLight(color)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            val lightSb = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            window.decorView.systemUiVisibility = if (light) lightSb else 0
            window.statusBarColor = color
            window.navigationBarColor = if (light) ColorUtil.darkenColor(color) else color
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val lightSb = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            val lightNb = View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            window.decorView.systemUiVisibility = if (light) lightSb or lightNb else 0
            window.statusBarColor = color
            window.navigationBarColor = color
        } else {
            window.decorView.systemUiVisibility = 0
            window.statusBarColor = if (light) ColorUtil.darkenColor(color) else color
            window.navigationBarColor = window.statusBarColor
        }
    }

    fun snackbar(v: View, text: String): Snackbar {
        return Snackbar.make(v, text, Snackbar.LENGTH_SHORT)
    }

    fun snackbar(v: View, @StringRes text: Int): Snackbar {
        return Snackbar.make(v, text, Snackbar.LENGTH_SHORT)
    }
}