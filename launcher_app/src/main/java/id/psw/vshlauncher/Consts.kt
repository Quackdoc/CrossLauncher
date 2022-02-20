package id.psw.vshlauncher

object Consts {
    const val XMB_DEFAULT_ITEM_ID: String = "id_null"
    const val XMB_DEFAULT_ITEM_DISPLAY_NAME : String = "No Name"
    const val XMB_DEFAULT_ITEM_DESCRIPTION : String = "No Description"
    const val XMB_DEFAULT_ITEM_VALUE : String = "No Value"
    const val XMB_KEY_APP_SORT_MODE = "sort_mode"
}

enum class FittingMode {
    STRETCH,
    FIT,
    FILL
}

enum class AppItemSorting{
    Name,
    PackageName,
    UpdateTime,
    FileSize
}