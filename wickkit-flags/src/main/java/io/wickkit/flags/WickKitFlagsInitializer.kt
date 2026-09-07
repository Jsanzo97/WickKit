package io.wickkit.flags

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import io.wickkit.overlay.ui.tab.FlagsTab
import io.wickkit.overlay.ui.tab.WickKitFlagsTabSlot

class WickKitFlagsInitializer : ContentProvider() {

    override fun onCreate(): Boolean {
        val context = context ?: return false
        WickKitFlagsManager.init(context)
        WickKitFlagsTabSlot.register { FlagsTab() }
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?,
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<String>?,
    ): Int = 0
}
