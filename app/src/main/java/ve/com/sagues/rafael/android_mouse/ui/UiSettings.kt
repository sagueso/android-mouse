package ve.com.sagues.rafael.android_mouse.ui

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Small, app-wide UI preferences that are not part of the Bluetooth connection state.
 */
class UiSettings(context: Context) {

    companion object {
        private const val PREFS = "ui_settings"
        private const val KEY_CLICK_BAR = "show_click_bar"
        private const val KEY_EDGE_LOCK = "edge_lock"
    }

    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _showClickBar = MutableStateFlow(prefs.getBoolean(KEY_CLICK_BAR, true))
    val showClickBar: StateFlow<Boolean> = _showClickBar.asStateFlow()

    private val _edgeLock = MutableStateFlow(prefs.getBoolean(KEY_EDGE_LOCK, true))
    val edgeLock: StateFlow<Boolean> = _edgeLock.asStateFlow()

    fun setShowClickBar(value: Boolean) {
        _showClickBar.value = value
        prefs.edit().putBoolean(KEY_CLICK_BAR, value).apply()
    }

    fun setEdgeLock(value: Boolean) {
        _edgeLock.value = value
        prefs.edit().putBoolean(KEY_EDGE_LOCK, value).apply()
    }
}
