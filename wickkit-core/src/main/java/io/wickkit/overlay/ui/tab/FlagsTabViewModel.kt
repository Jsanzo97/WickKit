package io.wickkit.overlay.ui.tab

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.wickkit.flags.FlagType
import io.wickkit.flags.RemoteConfigEntry
import io.wickkit.flags.SharedPreferencesEntry
import io.wickkit.flags.WickKitFlagsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

internal class FlagsTabViewModel(application: Application) : AndroidViewModel(application) {

    init {
        viewModelScope.launch(Dispatchers.IO) { WickKitFlagsManager.init(getApplication()) }
    }

    fun setRcOverride(key: String, value: String) {
        viewModelScope.launch(Dispatchers.IO) { WickKitFlagsManager.setRcOverride(key = key, value = value) }
    }

    fun toggleRcOverride(entry: RemoteConfigEntry) {
        viewModelScope.launch(Dispatchers.IO) {
            if (entry.overrideValue.isNotEmpty()) {
                WickKitFlagsManager.toggleRcOverride(entry.key)
            } else {
                WickKitFlagsManager.setRcOverride(key = entry.key, value = entry.remoteValue)
            }
        }
    }

    fun setSpOverride(prefsName: String, key: String, type: FlagType, value: String) {
        viewModelScope.launch(Dispatchers.IO) {
            WickKitFlagsManager.setSpOverride(prefsName = prefsName, key = key, type = type, value = value)
        }
    }

    fun toggleSpOverride(prefsName: String, entry: SharedPreferencesEntry) {
        viewModelScope.launch(Dispatchers.IO) {
            if (entry.hasOverride) {
                WickKitFlagsManager.toggleSpOverride(prefsName = prefsName, key = entry.key)
            } else {
                WickKitFlagsManager.setSpOverride(
                    prefsName = prefsName,
                    key = entry.key,
                    type = entry.type,
                    value = entry.currentValue,
                )
            }
        }
    }
}
