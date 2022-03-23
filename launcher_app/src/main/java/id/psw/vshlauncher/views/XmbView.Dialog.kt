package id.psw.vshlauncher.views

import android.app.Dialog
import android.graphics.*
import android.text.TextPaint
import androidx.core.graphics.contains
import androidx.core.graphics.withClip
import androidx.core.graphics.withTranslation
import id.psw.vshlauncher.*
import id.psw.vshlauncher.submodules.GamepadSubmodule
import id.psw.vshlauncher.typography.FontCollections
import id.psw.vshlauncher.typography.MultifontSpan
import id.psw.vshlauncher.typography.drawText
import java.util.*

class VshViewDialogState {
    var activeDialog : XmbDialogSubview? = null
    var textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 25.0f
        color = Color.WHITE
        typeface = FontCollections.masterFont
    }
    var outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        strokeWidth = 3.0f
        style = Paint.Style.STROKE
        typeface = FontCollections.masterFont
    }
    var currentTime = 0.0f
    var tmpBound = RectF()
    var dBackgroundAlpha = 0.0f
    var iconTmpBound = RectF()
}

fun XmbView.dlgStart(){
    with(state.dialog){
        activeDialog?.onStart()
        dBackgroundAlpha = 0.0f
    }
}

fun XmbView.showDialog(dialog:XmbDialogSubview){
    state.dialog.activeDialog?.onClose() // Close previous dialog in case it's available
    state.dialog.activeDialog = dialog
    switchPage(VshViewPage.Dialog)
}

fun XmbView.dlgRender(ctx: Canvas){
    val ctxState = ctx.save()
    val isPSP = state.crossMenu.layoutMode == XMBLayoutType.PSP
    with(state.dialog){
        if(dBackgroundAlpha < 1.0f){
            dBackgroundAlpha += time.deltaTime * 2.0f
        }
        ctx.drawARGB((dBackgroundAlpha * 128).toInt(), 0,0,0)
        val dlg = activeDialog
        if(dlg != null){
            dlg.isPSP = isPSP

            textPaint.textSize = isPSP.select(30.0f, 25.0f)

            tmpBound.set(scaling.target.left, scaling.target.top + 100f, scaling.target.right, scaling.target.bottom - 100f)
            ctx.drawLine(scaling.viewport.left - 10.0f, tmpBound.top,scaling.viewport.right-1.0f, tmpBound.top, outlinePaint)
            ctx.drawLine(scaling.viewport.left - 10.0f, tmpBound.bottom,scaling.viewport.right-1.0f, tmpBound.bottom, outlinePaint)

            // Draw title
            ctx.drawText(dlg.title, 125f, 65f, textPaint, 0.5f)
            iconTmpBound.set(50f, 65f - 25f, 100f, 65f + 25f)
            ctx.drawBitmap(dlg.icon, null, iconTmpBound, null, FittingMode.FIT, 0.5f, 0.5f)

            if(context.vsh.hasConcurrentLoading){
                drawClock(ctx, Calendar.getInstance(), 65f)
            }

            if(dlg.shouldClose){
                switchPage(dlg.closeDialogTo)
            }

            // Draw subview content
            ctx.withClip(tmpBound){
                ctx.withTranslation(tmpBound.left, tmpBound.top){
                    tmpBound.bottom -= tmpBound.top
                    tmpBound.top = 0.0f
                    dlg.onDraw(ctx, tmpBound)
                }
            }

            val asian = !GamepadSubmodule.Key.spotMarkedByX
            val lAlign = textPaint.textAlign
            textPaint.textAlign = Paint.Align.CENTER
            // draw dialog buttons
            if(dlg.hasNegativeButton){
                ctx.drawText(
                    MultifontSpan().add(FontCollections.buttonFont, asian.select("\uF881", "\uF880"))
                        .add(FontCollections.masterFont, " ${dlg.negativeButton}"),
                    0.5f.toLerp(scaling.target.centerX(), scaling.target.left),
                    scaling.target.bottom - 50.0f, 0.5f,
                    textPaint
                )
            }

            if(dlg.hasPositiveButton){
                ctx.drawText(
                    MultifontSpan().add(FontCollections.buttonFont, asian.select("\uF880", "\uF881"))
                        .add(FontCollections.masterFont, " ${dlg.positiveButton}"),
                    0.5f.toLerp(scaling.target.centerX(), scaling.target.right),
                    scaling.target.bottom - 50.0f, 0.5f,
                    textPaint
                )
            }
            textPaint.textAlign = lAlign
        }else{
            switchPage(VshViewPage.MainMenu)
        }
    }
    ctx.restoreToCount(ctxState)
}

private val tmpPointA = PointF()
private val tmpPointB = PointF()
private val dTmpBound = RectF()

fun XmbView.dlgOnTouchScreen(a: PointF, b: PointF, act:Int){
    with(state.dialog){
        val dlg = activeDialog
        if(dlg != null){
            dTmpBound.set(scaling.target.left, scaling.target.top + 100f, scaling.target.right, scaling.target.bottom - 100f)
            val offA = tmpPointA
            val offB = tmpPointB

            if(dTmpBound.contains(a) || dTmpBound.contains(b)){

                offA.set(a.x - dTmpBound.left, a.y - dTmpBound.top)
                offB.set(b.x - dTmpBound.left, b.y - dTmpBound.top)
                dlg.onTouch(offA,offB,act)
            }else{
                if(scaling.target.contains(a) && a.y > dTmpBound.bottom){
                    if(dlg.hasPositiveButton || dlg.hasNegativeButton){
                        dlg.onDialogButton(a.x > scaling.target.centerX())
                    }
                }
            }
        }
    }
}

fun XmbView.dlgEnd(){
    with(state.dialog){
        activeDialog?.onClose()
        activeDialog = null
    }
}

fun XmbView.dlgOnGamepad(k:GamepadSubmodule.Key, isPress: Boolean) : Boolean {
    var retval = false
    with(state.dialog){
        val dlg = activeDialog
        if(dlg != null){
            when(k){
                GamepadSubmodule.Key.Confirm, GamepadSubmodule.Key.StaticConfirm -> if(isPress) {
                    dlg.onDialogButton(true)
                    retval = true
                }
                GamepadSubmodule.Key.Cancel, GamepadSubmodule.Key.StaticCancel -> if(isPress) {
                    dlg.onDialogButton(false)
                    retval = true
                }
                else -> retval = dlg.onGamepad(k, isPress)
            }
        }
    }
    return retval
}