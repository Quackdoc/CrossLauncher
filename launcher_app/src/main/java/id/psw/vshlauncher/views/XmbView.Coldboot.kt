package id.psw.vshlauncher.views

import android.app.Activity
import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import androidx.core.graphics.drawable.toBitmap
import id.psw.vshlauncher.*
import id.psw.vshlauncher.livewallpaper.NativeGL
import id.psw.vshlauncher.livewallpaper.XMBWaveRenderer
import id.psw.vshlauncher.livewallpaper.XMBWaveSurfaceView
import id.psw.vshlauncher.submodules.GamepadSubmodule
import id.psw.vshlauncher.submodules.PadKey
import id.psw.vshlauncher.types.FileQuery
import id.psw.vshlauncher.typography.FontCollections
import java.io.File
import java.nio.file.Files.exists
import kotlin.system.exitProcess

class VshViewColdBootState(
){
    var currentTime : Float = 0.0f
    var image : Bitmap? = null
    val imagePaint : Paint = Paint().apply{
        alpha = 0
    }
    var isL1Down = false
    val epiwarnPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        alpha = 255
        textSize = 20f
        color = Color.WHITE
        typeface = FontCollections.masterFont
    }
    var waveSpeed = 1.0f
    var hideEpilepsyWarning = false
}

fun XmbView.playColdBootSound() {
    var isFound = false
    val vsh = context.vsh
    val vshIterator : (File) -> Unit = { it ->
        if(it.exists() && !isFound){
            isFound = true
            M.audio.setSystemAudioSource(it)
        }
    }

    FileQuery(VshBaseDirs.VSH_RESOURCES_DIR)
        .withNames(VshResName.GAMEBOOT)
        .withExtensionArray(VshResTypes.SOUNDS)
        .execute(vsh)
        .forEach(vshIterator)
}

fun XmbView.cbStart(){
    state.coldBoot.currentTime = 0.0f
    cbEnsureImageLoaded()
    playColdBootSound()
    state.crossMenu.coldBootTransition = 1.0f
    val pref = context.vsh.getSharedPreferences(XMBWaveSurfaceView.PREF_NAME, Context.MODE_PRIVATE)

    state.coldBoot.waveSpeed = pref.getFloat(XMBWaveSurfaceView.KEY_SPEED, 1.0f)
    NativeGL.setSpeed(0.0f)
    NativeGL.setVerticalScale(0.0f)
}

fun XmbView.cbEnsureImageLoaded(){
    if(state.coldBoot.image == null){

        // Load custom coldboot if exists
        val i = FileQuery(VshBaseDirs.VSH_RESOURCES_DIR)
            .withNames(VshResName.GAMEBOOT)
            .withExtensionArray(VshResTypes.IMAGES)
            .onlyIncludeExists(true)
            .execute(context.vsh).firstOrNull()
        if(i != null) { state.coldBoot.image = BitmapFactory.decodeFile(i.absolutePath) }

        // Load default if no custom coldboot can be loaded
        if(state.coldBoot.image == null) {
            state.coldBoot.image = getDrawable(R.drawable.coldboot_internal)?.toBitmap(1280, 720)
        }
    }
}

fun XmbView.cbRender(ctx: Canvas){
    cbEnsureImageLoaded()
    with(state.coldBoot) {

        NativeGL.setSpeed(0.0f)
        NativeGL.setVerticalScale(0.0f)

        val img = image
        val cTime = currentTime
        if (cTime < 5.0f && img != null) {
            imagePaint.alpha = when {
                cTime < 1.0f -> {
                    (cTime.toLerp(0f, 255f)).toInt().coerceIn(0, 255)
                }
                cTime > 4.0f && cTime < 5.0f -> {
                    (cTime.lerpFactor(5.0f, 4.0f) * 255).toInt().coerceIn(0, 255)
                }
                else -> 255
            }

            ctx.drawARGB((imagePaint.alpha * 0.75f).toInt(), 0, 0, 0)
            ctx.drawBitmap(img, null, scaling.target, imagePaint, FittingMode.FIT)
        } else if (cTime > 5.0f && cTime < 10.0f && !hideEpilepsyWarning) {
            epiwarnPaint.alpha = when {
                cTime > 5.0f && cTime < 6.0f -> {
                    (cTime.lerpFactor(5.0f, 6.0f) * 255).toInt().coerceIn(0, 255)
                }
                cTime > 9.0f && cTime < 10.0f -> {
                    (cTime.lerpFactor(10.0f, 9.0f) * 255).toInt().coerceIn(0, 255)
                }
                else -> 255
            }

            // if(cTime > 5.0f && cTime < 10.0f){
            //     NativeGL.setSpeed(state.coldBoot.waveSpeed * cTime.lerpFactor(5.0f, 10.0f).toLerp(10.0f, -10.0f).coerceIn(1.0f, 10.0f));
            // }

            ctx.drawARGB((epiwarnPaint.alpha * 0.75f).toInt(), 0, 0, 0)
            val lines =
                epiwarnPaint.wrapText(context.getString(R.string.photoepilepsy_warning), scaling.target.width() - 300.0f).lines()
            val lCount = lines.size
            val centerY = scaling.target.centerY()
            val hCount = lines.maxOf {
                val arr = FloatArray(it.length)
                epiwarnPaint.getTextWidths(it, arr)
                arr.sum()
            }
            val centerX = scaling.target.centerX()
            val xPos = centerX - (hCount * 0.5f)

            lines.forEachIndexed { i, it ->
                ctx.drawText(it, xPos, centerY + ((i - (lCount * 0.5f)) * epiwarnPaint.textSize), epiwarnPaint, 0.5f)
            }
        } else {
            switchPage(VshViewPage.MainMenu)
        }
    }
}

fun XmbView.cbOnTouchScreen(a: PointF, b:PointF, act:Int){
    if(act == MotionEvent.ACTION_DOWN){
        // Skip coldboot
        with(state.coldBoot){
            when {
                currentTime <= 5.0f -> currentTime = 5.0f
                currentTime > 5.0f && currentTime <= 10.0f && !hideEpilepsyWarning -> currentTime = 10.0f
            }
        }
    }
}

fun XmbView.cbEnd(){
    // Unload coldboot image
    state.coldBoot.image?.recycle()
    state.coldBoot.image = null
}

fun XmbView.cbOnGamepad(k:PadKey, isPress:Boolean) : Boolean {
    var retval =false

    with(state.coldBoot){
        if(isPress){
            when(k){
                PadKey.Confirm, PadKey.StaticConfirm -> {
                    if(currentTime <= 5.0f) currentTime = 5.0f
                    else if(currentTime <= 10.0f) currentTime = 10.0f
                    retval = true
                }
                PadKey.Cross -> {
                    if(isL1Down){
                        (context as Activity).finish() // L1 + Cross = Finish Activity
                    }
                }
                else -> {}
            }
        }

        if(k == PadKey.L1){
            isL1Down = isPress
        }
    }

    return retval
}
