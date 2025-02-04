package id.psw.vshlauncher

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import id.psw.vshlauncher.submodules.GamepadSubmodule
import id.psw.vshlauncher.submodules.GamepadUISubmodule
import id.psw.vshlauncher.submodules.PadKey
import id.psw.vshlauncher.submodules.PadType
import id.psw.vshlauncher.submodules.XMBAdaptiveIconRenderer
import id.psw.vshlauncher.types.CIFLoader
import id.psw.vshlauncher.types.items.*
import id.psw.vshlauncher.views.XMBLayoutType
import id.psw.vshlauncher.views.dialogviews.*
import id.psw.vshlauncher.views.formatStatusBar
import id.psw.vshlauncher.views.showDialog
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.roundToInt

object SettingsCategoryID {
    const val CATEGORY_SETTINGS_WAVE = "settings_category_wave"
    const val CATEGORY_SETTINGS_ANDROID = "settings_category_android"
    const val CATEGORY_SETTINGS_DISPLAY = "settings_category_display"
    const val CATEGORY_SETTINGS_AUDIO = "settings_category_audio"
    const val CATEGORY_SETTINGS_DEBUG = "settings_category_debug"
    const val CATEGORY_SETTINGS_SYSTEMINFO = "settings_category_systeminfo"
    const val CATEGORY_SETTINGS_SYSTEM = "settings_category_system"
}

fun VSH.fillSettingsCategory(){
    threadPool.execute {
        addToCategory(VSH.ITEM_CATEGORY_SETTINGS, createCategoryDisplay())
        addToCategory(VSH.ITEM_CATEGORY_SETTINGS, createCategoryAudio())
        addToCategory(VSH.ITEM_CATEGORY_SETTINGS, createCategorySystem())
        addToCategory(VSH.ITEM_CATEGORY_SETTINGS, createCategoryAndroidSetting())
        addToCategory(VSH.ITEM_CATEGORY_SETTINGS, createCategoryDebug())
        addToCategory(VSH.ITEM_CATEGORY_SETTINGS, settingsAddInstallPackage())

    }
}

private fun VSH.getCurrentLocaleName() : String {
    return if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
        resources.configuration.locales[0].displayName
    }else{
        resources.configuration.locale.displayName
    }
}

private fun VSH.makeVolume(setting: XMBSettingsItem, volume : (Float) -> Unit ){
    setting.hasMenu = true
    val v = arrayListOf<XMBMenuItem>()
    for(i in 10 downTo 0)
    {
        v.add(XMBMenuItem.XMBMenuItemLambda({ i.toString() }, {false}, (10 - i) - 5){
            volume.invoke(i / 10.0f)
            vsh.xmbView?.state?.itemMenu?.isDisplayed = false
        })
    }
    setting.menuItems = v
}

private fun volumeToString(v: Float) : String = (v * 10).roundToInt().toString()

private fun VSH.createCategoryAudio(): XMBSettingsCategory{
    val vsh = this
    return XMBSettingsCategory(this, SettingsCategoryID.CATEGORY_SETTINGS_AUDIO,
        R.drawable.icon_volume,
        R.string.settings_category_audio_name, R.string.settings_category_audio_title
    ).apply {
        content.add(XMBSettingsItem(vsh, "audio_volume_master",
            R.string.settings_audio_master_volume_name,
            R.string.settings_audio_master_volume_desc,
            R.drawable.icon_volume, { volumeToString(M.audio.master) }
        ){
            vsh.xmbView?.state?.itemMenu?.isDisplayed = true
        }.apply {
            makeVolume( this) { M.audio.master = it }
        })

        content.add(XMBSettingsItem(vsh, "audio_volume_bgm",
            R.string.settings_audio_bgm_volume_name,
            R.string.settings_audio_bgm_volume_desc,
            R.drawable.category_music, { volumeToString(M.audio.bgm) }
        ){
            vsh.xmbView?.state?.itemMenu?.isDisplayed = true
        }.apply {
            makeVolume( this) { M.audio.bgm = it }
        })

        content.add(XMBSettingsItem(vsh, "audio_volume_sysbgm",
            R.string.settings_audio_sysbgm_volume_name,
            R.string.settings_audio_sysbgm_volume_desc,
            R.drawable.ic_component_audio, { volumeToString(M.audio.systemBgm) }
        ){
            vsh.xmbView?.state?.itemMenu?.isDisplayed = true
        }.apply {
            makeVolume( this) { M.audio.systemBgm = it }
        })

        content.add(XMBSettingsItem(vsh, "audio_volume_sfx",
            R.string.settings_audio_sfx_volume_name,
            R.string.settings_audio_sfx_volume_desc,
            R.drawable.ic_speaker_phone, { volumeToString(M.audio.sfx) }
        ){
            vsh.xmbView?.state?.itemMenu?.isDisplayed = true
        }.apply {
            makeVolume( this) { M.audio.sfx = it }
        })
        content.add(XMBSettingsItem(vsh, "audio_reload_sfx",
            R.string.settings_audio_sfx_reload_name,
            R.string.settings_audio_sfx_reload_desc,
            R.drawable.icon_refresh, { "" }
        ){
            M.audio.loadSfxData(false)
        })
    }
}

private fun VSH.createCategorySystem() : XMBSettingsCategory{
    val vsh = this
    return XMBSettingsCategory(this,
        SettingsCategoryID.CATEGORY_SETTINGS_SYSTEM,
        R.drawable.category_setting,
        R.string.settings_category_system_name,
        R.string.settings_category_system_desc
    ).apply {

        content.add(
            XMBSettingsItem(vsh, "settings_system_show_fps",
                R.string.settings_system_show_fps_name,
                R.string.settings_system_show_fps_desc,
                R.drawable.category_setting,
                {
                    showLauncherFPS.select(vsh.getString(R.string.common_yes),vsh.getString(R.string.common_no))
                }
            ){
                showLauncherFPS = !showLauncherFPS
                M.pref.set(PrefEntry.SHOW_LAUNCHER_FPS, showLauncherFPS.select(1,0))
            }
        )

        content.add(
            XMBSettingsItem(vsh, "settings_system_show_detailed_mem",
                R.string.settings_system_detailed_mem_name,
                R.string.settings_system_detailed_mem_desc,
                R.drawable.icon_device_info,
                {
                    (xmbView?.showDetailedMemory ?: false).select(vsh.getString(R.string.common_yes),vsh.getString(R.string.common_no))
                }
            ){
                xmbView?.showDetailedMemory = xmbView?.showDetailedMemory != true
                M.pref.set(PrefEntry.SHOW_DETAILED_MEMORY, (xmbView?.showDetailedMemory == true).select(1,0))
            }
        )

        content.add(
            XMBSettingsItem(vsh, "settings_system_rearrange",
                R.string.settings_system_rearrange_category_name,
                R.string.settings_system_rearrange_category_desc,
                R.drawable.category_setting, { "" }
            ){
                vsh.xmbView?.showDialog(ArrangeCategoryDialogView(vsh))
            }
        )

        content.add(
            XMBSettingsItem(vsh, "settings_system_reorder_icon_loading",
                R.string.settings_system_reorder_icon_loading_name,
                R.string.settings_system_reorder_icon_loading_desc,
                R.drawable.icon_storage, {
                    val i = XMBAdaptiveIconRenderer.getIconPriorityAt(0)
                    val id = IconPriorityDialogView.iconTypeToStrId[i]
                    vsh.getString(R.string.settings_system_reorder_icon_loading_value).format(vsh.getString(id))
                }
            ){
                vsh.xmbView?.showDialog(IconPriorityDialogView(vsh))
            }
        )

        settingsAddSystemSetting2(this)
        content.add(XMBSettingsItem(vsh, "settings_system_orientation",
            R.string.item_orientation,
            R.string.item_orientation_desc, R.drawable.icon_orientation, {
                val xmb = xmbView?.context?.xmb
                getString(when(xmb?.requestedOrientation){
                    ActivityInfo.SCREEN_ORIENTATION_USER -> R.string.orient_user
                    ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE -> R.string.orient_landscape
                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT -> R.string.orient_portrait
                    else -> R.string.orient_unknown
                })
            }){
            val xmb = xmbView?.context?.xmb
            xmb?.requestedOrientation = when(xmb?.requestedOrientation){
                ActivityInfo.SCREEN_ORIENTATION_USER -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_USER
                else -> ActivityInfo.SCREEN_ORIENTATION_USER
            }
            M.pref.set(PrefEntry.DISPLAY_ORIENTATION, xmb?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_SENSOR)
        }.apply {
            val xmb = xmbView?.context?.xmb
            hasMenu = true
            val dMenuItems = arrayListOf<XMBMenuItem>()
            arrayOf(
                ActivityInfo.SCREEN_ORIENTATION_USER,
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT).forEachIndexed { i, it ->
                val nameStr = getString(when(it){
                    ActivityInfo.SCREEN_ORIENTATION_USER -> R.string.orient_user
                    ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE -> R.string.orient_landscape
                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT -> R.string.orient_portrait
                    else -> R.string.orient_unknown
                })
                dMenuItems.add(XMBMenuItem.XMBMenuItemLambda({nameStr}, {false}, i){
                    xmb?.requestedOrientation = it
                })
            }
            menuItems = dMenuItems
        })

        content.add(XMBSettingsItem(vsh, "settings_system_button",
        R.string.settings_system_asian_console_name, R.string.settings_system_asian_console_desc,
        R.drawable.category_games, {
                getString(
                    if(PadKey.spotMarkedByX)
                        R.string.settings_system_asian_console_false
                    else
                        R.string.settings_system_asian_console_true
                )
            }
        ){
            PadKey.spotMarkedByX = !PadKey.spotMarkedByX
            M.pref.set(PrefEntry.CONFIRM_BUTTON,
                PadKey.spotMarkedByX.select(1,0))
        })

        content.add(XMBSettingsItem(vsh, "settings_system_epimsg_disable",
            R.string.settings_system_disable_splash_message_title,
            R.string.settings_system_disable_splash_message_desc,
            R.drawable.icon_info, {
                getString(xmbView?.state?.coldBoot?.hideEpilepsyWarning?.select(
                    R.string.common_yes, R.string.common_no
                ) ?: R.string.unknown)
            })
        {
            val xv = xmbView
            if(xv != null){
                xv.state.coldBoot.hideEpilepsyWarning = !xv.state.coldBoot.hideEpilepsyWarning
                M.pref.set(
                        PrefEntry.DISABLE_EPILEPSY_WARNING,
                        xv.state.coldBoot.hideEpilepsyWarning.select(1,0)
                    )
            }
        }
        )


        content.add(
            XMBSettingsItem(vsh, "settings_system_disable_video_icon",
                R.string.settings_system_disable_video_icon_name,
                R.string.settings_system_disable_video_icon_desc,
                R.drawable.category_video,
                {
                    getString(CIFLoader.disableAnimatedIcon.select(R.string.common_yes, R.string.common_no))
                }
            ){
                CIFLoader.disableAnimatedIcon = !CIFLoader.disableAnimatedIcon
                M.pref.set(
                    PrefEntry.DISABLE_VIDEO_ICON,
                    CIFLoader.disableAnimatedIcon.select(1,0)
                )
            }
        )

        val kTypeAppDescKey = mapOf<XMBAppItem.DescriptionDisplay, Int>(
            XMBAppItem.DescriptionDisplay.None to R.string.settings_system_visible_desc_val_none,
            XMBAppItem.DescriptionDisplay.Date to R.string.settings_system_visible_desc_val_date,
            XMBAppItem.DescriptionDisplay.FileSize to R.string.settings_system_visible_desc_val_filesize,
            XMBAppItem.DescriptionDisplay.ModificationId to R.string.settings_system_visible_desc_val_modid,
            XMBAppItem.DescriptionDisplay.PackageName to R.string.settings_system_visible_desc_val_packagename,
            XMBAppItem.DescriptionDisplay.NkFileStyle to R.string.settings_system_visible_desc_val_nkfile
        )
        content.add(
            XMBSettingsItem(vsh, "settings_system_visible_app_desc",
                R.string.settings_system_visible_app_desc_name,
                R.string.settings_system_visible_app_desc_desc,
                R.drawable.icon_info,
                {
                    getString(
                        kTypeAppDescKey[XMBAppItem.descriptionDisplay] ?: R.string.settings_system_visible_desc_val_none
                    )
                }
            ){
                vsh.xmbView?.state?.itemMenu?.isDisplayed = true
            }.apply {
                hasMenu = true
                val menu = arrayListOf<XMBMenuItem>()
                var i = -(kTypeAppDescKey.size / 2)
                for((k, m) in kTypeAppDescKey){
                    menu.add(XMBMenuItem.XMBMenuItemLambda( { getString(m) }, {false}, i++){
                        XMBAppItem.descriptionDisplay = k
                        M.pref.set(PrefEntry.SYSTEM_VISIBLE_APP_DESC, k.ordinal)
                        vsh.xmbView?.state?.itemMenu?.isDisplayed = false
                    })
                }
                menuItems= menu
            }
        )

        content.add(
            XMBSettingsItem(vsh, "settings_system_skip_gameboot",
                R.string.setting_show_gameboot_name,
                R.string.setting_show_gameboot_desc,
                R.drawable.icon_dynamic_theme_effect, {
                    val i = xmbView?.state?.gameBoot?.defaultSkip?.select(
                        R.string.common_no,
                        R.string.common_yes
                    ) ?: R.string.empty_string
                    getString(i)
                }
            ){
                xmbView?.state?.gameBoot?.defaultSkip = !(xmbView?.state?.gameBoot?.defaultSkip ?: true)
                M.pref.set(PrefEntry.SKIP_GAMEBOOT,
                    xmbView?.state?.gameBoot?.defaultSkip?.select(1, 0) ?: 0
                )
            }
        )

        content.add(
            XMBSettingsItem(vsh, "settings_system_show_hidden_app",
                R.string.settings_system_show_hidden_app_name,
                R.string.settings_system_show_hidden_app_desc,
                R.drawable.icon_hidden, {
                    getString(XMBAppItem.showHiddenByConfig.select(
                        R.string.common_yes,
                        R.string.common_no
                    ))
                }
            ){
                XMBAppItem.showHiddenByConfig = !XMBAppItem.showHiddenByConfig
            }
        )


        content.add(
            XMBSettingsItem(vsh, "settings_system_prioritze_tv",
                R.string.settings_system_prioritize_tv_intent_name,
                R.string.settings_system_prioritize_tv_intent_desc,
                R.drawable.icon_video_anim_icon, {
                    getString(vsh._prioritizeTvIntent.select(
                        R.string.common_yes,
                        R.string.common_no
                    ))
                }
            ){
                _prioritizeTvIntent = !_prioritizeTvIntent
                M.pref.set(PrefEntry.LAUNCHER_TV_INTENT_FIRST, _prioritizeTvIntent.select(1, 0))
            }
        )

        content.add(
            XMBSettingsItem(vsh, "settings_system_info_dialog_open",
                R.string.setting_systeminfo_name,
                R.string.setting_systeminfo_desc,
                R.drawable.icon_info, { "" }
            ){
                vsh.xmbView?.showDialog(AboutDeviceDialogView(vsh))
            }
        )

        content.add(
            XMBSettingsItem(vsh, "settings_license_dialog_open",
                R.string.setting_license_name,
                R.string.setting_license_desc,
                R.drawable.icon_info, { "" }
            ){
                vsh.xmbView?.showDialog(LicenseDialogView(vsh))
            }
        )

        val cal = Calendar.getInstance()
        val mon = cal.get(Calendar.MONTH)
        val day = cal.get(Calendar.DAY_OF_MONTH)
        if(mon == 3 && day == 1){
            content.add(XMBSettingsItem(vsh, "settings_what_is_it",
                R.string.settings_system_test_option_name,
                R.string.settings_system_test_option_desc,
                R.drawable.icon_developer, {
                    val i = xmbView?.keygenActive?.select(
                        R.string.settings_system_test_option_value_on,
                        R.string.settings_system_test_option_value_off) ?: R.string.empty_string
                    getString(i)
                }
            ){
                xmbView?.keygenActive = true
            })
        }

    }
}

private fun VSH.createCategoryDebug() : XMBSettingsCategory{
    val vsh = this
    return XMBSettingsCategory(this,
        SettingsCategoryID.CATEGORY_SETTINGS_DEBUG,
        R.drawable.category_debug,
        R.string.settings_category_debug_name,
        R.string.settings_category_debug_desc
    ).apply {

        isSettingHidden = { !(BuildConfig.DEBUG || vsh.showDebuggerCount >= 8) }

        content.add(XMBSettingsItem(vsh, "dbg_launch_dialog_test",
            R.string.dbg_launch_dialog_test, R.string.empty_string, R.drawable.category_debug, {""}){
            xmbView?.showDialog(TestDialogView(vsh))
        })
        content.add(XMBSettingsItem(vsh, "dbg_launch_dialog_test_ui",
            R.string.dbg_launch_dialog_ui_test, R.string.empty_string, R.drawable.category_debug, {""}){
            xmbView?.showDialog(UITestDialogView(vsh))
        })

        content.add(
            XMBSettingsItem(vsh, "dbg_custom_file_list",
                R.string.dbg_custom_file_list,
                R.string.empty_string,
                R.drawable.ic_short_line, { "" }
            ){
                vsh.xmbView?.showDialog(CustomResourceListDialogView(vsh))
            }
        )

        if(BuildConfig.DEBUG){
            content.add(
                XMBSettingsItem(vsh, "dbg_throw_exception",
                    R.string.dbg_custom_throw_unhandled_exception,
                    R.string.empty_string,
                    R.drawable.ic_error, { "" }
                ){
                    throw Exception("This is an unhandled exception, present in log file")
                }
            )

            content.add(
                XMBSettingsItem(
                    vsh,
                    "dbg_throw_window",
                    R.string.dbg_bitman_info, R.string.empty_string,
                    R.drawable.settings_category_display, {""})
                {
                    vsh.xmbView?.showDialog(BitManDlgView(vsh))
                }
            )
        }

        content.add(XMBSettingsItem(vsh, "settings_system_language",
            R.string.settings_system_language, R.string.settings_system_language_description,
            R.drawable.icon_language, { getCurrentLocaleName() }
        ) {
            vsh.xmbView?.state?.itemMenu?.isDisplayed = true
        }.apply {
            hasMenu = true
            val dMenuItems = arrayListOf<XMBMenuItem>()
            supportedLocaleList.forEachIndexed { i, it ->
                val item = XMBMenuItem.XMBMenuItemLambda(
                    { it?.displayName ?: "System Default" },
                    { false }, i)
                {
                    vsh.setActiveLocale(it)
                }
                dMenuItems.add(item)
            }
            menuItems = dMenuItems
        })

    }
}

private fun VSH.createCategoryDisplay() : XMBSettingsCategory {
    val vsh = this
    return XMBSettingsCategory(this,
        SettingsCategoryID.CATEGORY_SETTINGS_DISPLAY,
        R.drawable.settings_category_display,
        R.string.settings_category_display_name,
        R.string.settings_category_display_desc
    ).apply {
        // region Layout Setting
        content.add(
            XMBSettingsItem(vsh,
                "setting_display_layout_0",
                R.string.setting_display_layout_type,
                R.string.setting_display_layout_type_desc,
                R.drawable.icon_orientation,
                {
                    when(xmbView?.state?.crossMenu?.layoutMode){
                        XMBLayoutType.PSP -> "PlayStation Portable"
                        XMBLayoutType.PS3 -> "PlayStation 3"
                        XMBLayoutType.PSX -> "PSX DVR"
                        XMBLayoutType.Bravia -> "Bravia TV"
                        else -> getString(R.string.unknown)
                    }
                },
            ) {
                val view = xmbView
                if(view != null){
                    view.state.crossMenu.layoutMode = when(view.state.crossMenu.layoutMode){
                        XMBLayoutType.PSP -> XMBLayoutType.PS3
                        XMBLayoutType.PS3 -> XMBLayoutType.PSP
                        else -> XMBLayoutType.PS3
                    }
                    saveLayoutSetting()
                }
            }.apply{
                hasMenu = true
                val dMenu = ArrayList<XMBMenuItem>()

                dMenu.add(XMBMenuItem.XMBMenuItemLambda(
                    {"PlayStation Portable"}, {false}, 1)
                {
                    xmbView?.state?.crossMenu?.layoutMode = XMBLayoutType.PSP
                    saveLayoutSetting()
                })
                dMenu.add(XMBMenuItem.XMBMenuItemLambda(
                    {"PlayStation 3"}, {false}, 0)
                {
                    xmbView?.state?.crossMenu?.layoutMode = XMBLayoutType.PS3
                    saveLayoutSetting()
                })
                menuItems = dMenu
            }
        )
        //endregion


        content.add(XMBSettingsItem(vsh, "settings_display_hide_bar",
            R.string.settings_display_hide_statusbar_name,
            R.string.settings_display_hide_statusbar_desc,
            R.drawable.icon_hidden, {
                val id = (xmbView?.state?.crossMenu?.statusBar?.disabled == true).select(R.string.common_yes, R.string.common_no)
                vsh.getString(id)
            }){
            val x = xmbView
            if(x != null){
                x.state.crossMenu.statusBar.disabled = !x.state.crossMenu.statusBar.disabled
                M.pref.set(PrefEntry.DISPLAY_DISABLE_STATUS_BAR, x.state.crossMenu.statusBar.disabled.select(1, 0))
            }
        })


        // region Console Button Display
        content.add(
            XMBSettingsItem(vsh, "settings_display_button_type",
                R.string.setting_display_button_type_name,
                R.string.setting_display_button_type_desc,
                R.drawable.icon_button_display,
                {
                    when(M.gamepadUi.activeGamepad){
                        PadType.Unknown -> vsh.getString(R.string.common_default)
                        PadType.PlayStation -> "PlayStation"
                        PadType.Android -> "Android"
                        PadType.Xbox -> "Xbox"
                        PadType.Nintendo -> "Nintendo Switch"
                        else -> vsh.getString(R.string.unknown)
                    }
                }
            ){
//               M.gamepadUi.activeGamepad = when(M.gamepadUi.activeGamepad){
//                    GamepadUISubmodule.PadType.Unknown -> GamepadUISubmodule.PadType.PlayStation
//                    GamepadUISubmodule.PadType.PlayStation -> GamepadUISubmodule.PadType.Xbox
//                    GamepadUISubmodule.PadType.Xbox -> GamepadUISubmodule.PadType.Nintendo
//                    GamepadUISubmodule.PadType.Nintendo -> GamepadUISubmodule.PadType.Android
//                    GamepadUISubmodule.PadType.Android -> GamepadUISubmodule.PadType.PlayStation
//                    else -> GamepadUISubmodule.PadType.PlayStation
//                }
//                saveButtonDisplaySetting()
                vsh.xmbView?.state?.itemMenu?.isDisplayed = true
            }.apply {

                hasMenu = true
                val dMenu = ArrayList<XMBMenuItem>()

                dMenu.add(XMBMenuItem.XMBMenuItemLambda(
                    {"PlayStation"}, {false}, 0)
                {
                   M.gamepadUi.activeGamepad = PadType.PlayStation
                    saveButtonDisplaySetting()
                })
                dMenu.add(XMBMenuItem.XMBMenuItemLambda(
                    {"Xbox"}, {false}, 1)
                {
                   M.gamepadUi.activeGamepad = PadType.Xbox
                    saveLayoutSetting()
                })
                dMenu.add(XMBMenuItem.XMBMenuItemLambda(
                    {"Nintendo Switch"}, {false}, 2)
                {
                   M.gamepadUi.activeGamepad = PadType.Nintendo
                    saveLayoutSetting()
                })
                dMenu.add(XMBMenuItem.XMBMenuItemLambda(
                    {"Android"}, {false}, -1)
                {
                   M.gamepadUi.activeGamepad = PadType.Android
                    saveLayoutSetting()
                })
                menuItems= dMenu
            }
        )
        // endregion
        // region Analog Clock Second Settings
        content.add(
            XMBSettingsItem(vsh, "settings_display_analog_second",
                R.string.settings_display_clock_second_analog_name,
                R.string.settings_display_clock_second_analog_desc,
                R.drawable.icon_clock,
                { (xmbView?.state?.crossMenu?.statusBar?.secondOnAnalog == true).select(vsh.getString(R.string.common_yes),vsh.getString(R.string.common_no))  }
            ){
                val x = xmbView
                if(x != null){
                    x.state.crossMenu.statusBar.secondOnAnalog = !x.state.crossMenu.statusBar.secondOnAnalog
                    M.pref.set(PrefEntry.DISPLAY_SHOW_CLOCK_SECOND, x.state.crossMenu.statusBar.secondOnAnalog.select(1, 0))

                }
            }
        )
        // endregion
        // region Status Bar Text Format
        content.add(
            XMBSettingsItem(vsh, "settings_display_statusbar_fmt",
                R.string.settings_display_statusbar_fmt_name,
                R.string.settings_display_statusbar_fmt_desc,
            R.drawable.icon_clock,
                {
                    val xmb = vsh.xmbView
                    xmb?.formatStatusBar(xmb.state.crossMenu.dateTimeFormat) ?: ""
                }
        ){
                vsh.xmbView?.showDialog(StatusBarFormatDialogView(vsh))
            }
        )
        // endregion
        // region Show Operator Name Settings
        content.add(
            XMBSettingsItem(vsh, "settings_display_operator",
                R.string.settings_display_show_operator_name,
                R.string.settings_display_show_operator_desc,
                R.drawable.icon_network,
                { (xmbView?.state?.crossMenu?.statusBar?.showMobileOperator == true).select(vsh.getString(R.string.common_yes),vsh.getString(R.string.common_no))  }
            ){
                val x = xmbView
                if(x != null){
                    val v = x.state.crossMenu.statusBar.showMobileOperator

                    if(!v){
                        if(Build.VERSION.SDK_INT >= 23){
                            if(
                                vsh.checkSelfPermission(android.Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED ||
                                vsh.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                            ){
                                x.state.crossMenu.statusBar.showMobileOperator = true
                            }else{
                                ActivityCompat.requestPermissions(x.context as Activity, arrayOf(
                                    android.Manifest.permission.READ_PHONE_STATE,
                                    android.Manifest.permission.ACCESS_FINE_LOCATION
                                ), 1999)
                            }
                        }else{
                            x.state.crossMenu.statusBar.showMobileOperator = true
                        }

                    }else{
                        x.state.crossMenu.statusBar.showMobileOperator = false
                    }

                }
            }
        )

        val scRef = XMBSettingsItem(vsh, "settings_screen_reference_size",
            R.string.settings_display_refsize_name,
            R.string.settings_display_refsize_desc,
            R.drawable.ic_fullscreen,
            {
                var r = ""
                val v = xmbView
                if(v != null){
                    val w = v.scaling.landTarget.width().toInt()
                    val h = v.scaling.landTarget.height().toInt()
                    r = "${w}x$h"
                }
                r
            }
        ){
            xmbView?.showDialog(CustomAspectRatioDialogView(vsh))
        }

        content.add(scRef)

        // endregion
        // region Background Dim
        content.add(XMBSettingsItem(vsh, "settings_display_bg_dim",
        R.string.settings_display_background_dim_name,
        R.string.settings_display_background_dim_desc,
            R.drawable.icon_brightness,{
                (xmbView?.state?.crossMenu?.dimOpacity?: 0).toString()
            }
        ){
            xmbView?.state?.itemMenu?.isDisplayed = true
        }.apply {
            hasMenu = true
            val dMenu = arrayListOf<XMBMenuItem>()
            for(i in 0 .. 10){
                dMenu.add(XMBMenuItem.XMBMenuItemLambda(
                    { i.toString() }, {false}, i)
                {
                    xmbView?.state?.crossMenu?.dimOpacity = i
                    M.pref.set(PrefEntry.BACKGROUND_DIM_OPACITY, xmbView?.state?.crossMenu?.dimOpacity?: 0)
                })
            }
            menuItems = dMenu
        }
        )
        // endregion
        content.add(createCategoryWaveSetting())
    }
}

fun VSH.saveLayoutSetting() {
    val view = xmbView
    if(view != null) {
        val srlzLayout = when (view.state.crossMenu.layoutMode) {
            XMBLayoutType.PS3 -> 0
            XMBLayoutType.PSP -> 1
            XMBLayoutType.Bravia -> 2
            XMBLayoutType.PSX -> 3
            else -> 0
        }
        M.pref.set(PrefEntry.MENU_LAYOUT, srlzLayout)
    }
}

fun VSH.saveButtonDisplaySetting() {
    val view = xmbView
    if(view != null) {
        val srlzButton = when (M.gamepadUi.activeGamepad) {
            PadType.PlayStation -> 0
            PadType.Xbox -> 1
            PadType.Nintendo -> 2
            PadType.Android -> 3
            else -> 0
        }
        M.pref.set(PrefEntry.BUTTON_DISPLAY_TYPE, srlzButton)
    }
}

private fun VSH.createCategoryAndroidSetting() : XMBSettingsCategory{
    val vsh = this
    return XMBSettingsCategory(this,
        SettingsCategoryID.CATEGORY_SETTINGS_ANDROID,
        R.drawable.icon_android,
        R.string.setting_android_name,
        R.string.setting_android_desc,
    ).apply {

        content.add(XMBAndroidSettingShortcutItem(
            vsh, R.drawable.category_setting,
            R.string.android_sys_setting_homepage_name,
            R.string.android_sys_setting_homepage_desc,
            android.provider.Settings.ACTION_SETTINGS
        ))


        content.add(XMBAndroidSettingShortcutItem(
            vsh, R.drawable.icon_network,
            R.string.android_sys_network_name,
            R.string.android_sys_network_desc,
            android.provider.Settings.ACTION_WIRELESS_SETTINGS
        ))

        content.add(XMBAndroidSettingShortcutItem(
            vsh, R.drawable.icon_cda,
            R.string.android_sys_devices_name,
            R.string.android_sys_devices_desc,
            android.provider.Settings.ACTION_BLUETOOTH_SETTINGS
        ))

        content.add(XMBAndroidSettingShortcutItem(
            vsh, R.drawable.category_apps,
            R.string.android_sys_apps_name,
            R.string.android_sys_apps_desc,
            "id.psw.vshlauncher.settings.category.apps"
        ).apply {
            hasContent = true
            content.add(
                XMBAndroidSettingShortcutItem(
                    vsh, R.drawable.category_apps,
                    R.string.android_sys_all_apps_name,  R.string.android_sys_all_apps_desc,
                    android.provider.Settings.ACTION_APPLICATION_SETTINGS
                )
            )
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                content.add(
                    XMBAndroidSettingShortcutItem(
                        vsh, R.drawable.category_notifications,
                        R.string.android_sys_nodisturb_name, R.string.android_sys_nodisturb_desc,
                        android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS
                    )
                )
                content.add(
                    XMBAndroidSettingShortcutItem(
                        vsh, R.drawable.category_notifications,
                        R.string.android_sys_notification_name, R.string.android_sys_notification_desc,
                        android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
                    )
                )
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                content.add(
                    XMBAndroidSettingShortcutItem(
                        vsh, R.drawable.category_apps,
                        R.string.android_sys_default_apps_name,  R.string.android_sys_default_apps_desc,
                        android.provider.Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS
                    )
                )
            }

        })

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            content.add(XMBAndroidSettingShortcutItem(
                vsh, R.drawable.icon_battery,
                R.string.android_sys_battery_name,
                R.string.android_sys_battery_desc,
                android.provider.Settings.ACTION_BATTERY_SAVER_SETTINGS
            ))
        }
        content.add(XMBAndroidSettingShortcutItem(
            vsh, R.drawable.icon_brightness,
            R.string.android_sys_display_name,
            R.string.android_sys_display_desc,
            android.provider.Settings.ACTION_DISPLAY_SETTINGS
        ))

        content.add(XMBAndroidSettingShortcutItem(
            vsh, R.drawable.icon_color,
            R.string.android_sys_wallpaper_name,
            R.string.android_sys_wallpaper_desc,
            Intent.ACTION_SET_WALLPAPER
        ))

        content.add(XMBAndroidSettingShortcutItem(
            vsh, R.drawable.icon_volume,
            R.string.android_sys_sound_name,
            R.string.android_sys_sound_desc,
            android.provider.Settings.ACTION_SOUND_SETTINGS
        ))
        content.add(XMBAndroidSettingShortcutItem(
            vsh, R.drawable.icon_storage,
            R.string.android_sys_storage_name,
            R.string.android_sys_storage_desc,
            android.provider.Settings.ACTION_INTERNAL_STORAGE_SETTINGS
        ))
        content.add(XMBAndroidSettingShortcutItem(
            vsh, R.drawable.icon_privacy,
            R.string.android_sys_privacy_name,
            R.string.android_sys_privacy_desc,
            android.provider.Settings.ACTION_PRIVACY_SETTINGS
        ))
        content.add(XMBAndroidSettingShortcutItem(
            vsh, R.drawable.icon_location,
            R.string.android_sys_location_name,
            R.string.android_sys_location_desc,
            android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS
        ))
        content.add(XMBAndroidSettingShortcutItem(
            vsh, R.drawable.icon_security,
            R.string.android_sys_security_name,
            R.string.android_sys_security_desc,
            android.provider.Settings.ACTION_SECURITY_SETTINGS
        ))
        content.add(XMBAndroidSettingShortcutItem(
            vsh, R.drawable.icon_accessibility,
            R.string.android_sys_accessibility_name,
            R.string.android_sys_accessibility_desc,
            android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS
        ))
        content.add(XMBAndroidSettingShortcutItem(
            vsh, R.drawable.icon_info,
            R.string.android_sys_systeminfo_name,
            R.string.android_sys_systeminfo_desc,
            "id.psw.vshlauncher.settings.category.system"
        ).apply {
            hasContent = true
            content.add(XMBAndroidSettingShortcutItem(
                vsh, R.drawable.icon_language,
                R.string.android_sys_locale_name,
                R.string.android_sys_locale_desc,
                android.provider.Settings.ACTION_LOCALE_SETTINGS
            ))

            content.add(XMBAndroidSettingShortcutItem(
                vsh, R.drawable.icon_datetime,
                R.string.android_sys_datetime_name,
                R.string.android_sys_datetime_desc,
                android.provider.Settings.ACTION_DATE_SETTINGS
            ))

            content.add(XMBAndroidSettingShortcutItem(
                vsh, R.drawable.icon_developer,
                R.string.android_sys_developer_name,
                R.string.android_sys_developer_desc,
                android.provider.Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS
            ))
        })
        content.add(XMBAndroidSettingShortcutItem(
            vsh, R.drawable.icon_device_info,
            R.string.android_sys_deviceinfo_name,
            R.string.android_sys_deviceinfo_desc,
            android.provider.Settings.ACTION_DEVICE_INFO_SETTINGS
        ))
    }
}