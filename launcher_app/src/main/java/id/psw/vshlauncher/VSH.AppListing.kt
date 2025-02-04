package id.psw.vshlauncher

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.ResolveInfo
import android.os.Build
import android.util.Log
import id.psw.vshlauncher.types.items.XMBAppItem
import id.psw.vshlauncher.types.items.XMBItemCategory
import java.io.File

fun VSH.isAGame(rInfo: ResolveInfo): Boolean {
    val appInfo = packageManager.getApplicationInfo(rInfo.activityInfo.packageName, 0)
    var retval = false
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        retval = retval || appInfo.flags hasFlag ApplicationInfo.FLAG_IS_GAME
    }
    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
        retval = retval || appInfo.category == ApplicationInfo.CATEGORY_GAME
    }
    return retval
}

fun VSH.appCategorySorting(it: XMBItemCategory) {
        val oldSort = it.getProperty(Consts.XMB_KEY_APP_SORT_MODE, AppItemSorting.FileSize)
        val newSort = when(oldSort){
            AppItemSorting.Name -> AppItemSorting.PackageName
            AppItemSorting.PackageName -> AppItemSorting.FileSize
            AppItemSorting.FileSize -> AppItemSorting.UpdateTime
            AppItemSorting.UpdateTime -> AppItemSorting.Name
        }
        it.setSort(newSort)
        it.setProperty(Consts.XMB_KEY_APP_SORT_MODE, newSort)
}

fun VSH.appCategorySetSorting(it:XMBItemCategory, newSort:Any){
    if(newSort is AppItemSorting){
        it.content.sortBy { item ->
            if(item is XMBAppItem){
                when(newSort){
                    AppItemSorting.Name -> item.displayName
                    AppItemSorting.UpdateTime -> item.sortUpdateTime
                    AppItemSorting.FileSize -> item.fileSize
                    AppItemSorting.PackageName -> item.resInfo.uniqueActivityName
                    else -> item.displayName
                }
            }else{
                it.displayName
            }
        }

    }
}

fun VSH.appCategorySortingName(it : XMBItemCategory) : String{
    val newSort = it.getProperty(Consts.XMB_KEY_APP_SORT_MODE, AppItemSorting.Name)
    return when(newSort){
        AppItemSorting.Name -> vsh.getString(R.string.app_sorting_name)
        AppItemSorting.UpdateTime -> vsh.getString(R.string.app_sorting_updtime)
        AppItemSorting.FileSize -> vsh.getString(R.string.app_sorting_filesize)
        AppItemSorting.PackageName -> vsh.getString(R.string.app_sorting_actname)
        else -> ""
    }
}

fun VSH.tryMigrateOldGameDirectory(){
    val TAG = "GameCustomMigrate"
    if(!runtimeTriageCheck(TAG)) return

    val storages = getExternalFilesDirs(null)
    for(storage in storages){
        val src = storage.combine("dev_hdd0","games")
        val dst = storage.combine("dev_hdd0","game")

        if(src == null || dst == null) continue

        if(!src.isDirectory) continue

        Log.w(TAG, "Found old directory of app customization, migrating...")
        if(dst.isDirectory) {
            Log.w(TAG, "Both new and old directory is present, Migration cancelled")
            continue
        }

        try{
            src.renameTo(dst)
        }catch (e:Exception){
            Log.e(TAG, "Migration error", e)
        }
    }
}

fun VSH.reloadAppList(){
    threadPool.execute {
        XMBAppItem.showHiddenByConfig = false
        val gameCat = categories.find {it.id == VSH.ITEM_CATEGORY_GAME }
        if(gameCat != null){
            synchronized(gameCat){
                gameCat.onSetSortFunc = { it, sort ->  appCategorySetSorting(it, sort) }
                gameCat.onSwitchSortFunc = { appCategorySorting(it) }
                gameCat.getSortModeNameFunc = { appCategorySortingName(it) }
            }
        }
        val appCat = categories.find{ it.id == VSH.ITEM_CATEGORY_APPS }
        if(appCat != null){
            synchronized(appCat){
                appCat.onSetSortFunc = { it, sort -> appCategorySetSorting(it, sort) }
                appCat.onSwitchSortFunc = { appCategorySorting(it) }
                appCat.getSortModeNameFunc = { appCategorySortingName(it) }
            }
        }

        val intent = Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER)
        val lh = addLoadHandle()
        packageManager.queryIntentActivities(intent, 0).forEach {
            val item = XMBAppItem(vsh, it)
            val isGame = isAGame(it)
            addToCategory(isGame.select(VSH.ITEM_CATEGORY_GAME, VSH.ITEM_CATEGORY_APPS), item)
            isGame.select(gameCat, appCat)?.setSort(AppItemSorting.Name)
        }

        setLoadingFinished(lh)
    }
}