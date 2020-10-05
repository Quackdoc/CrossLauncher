package id.psw.vshlauncher

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.text.TextPaint
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.graphics.contains
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.toRect
import androidx.core.graphics.withClip
import androidx.core.graphics.withTranslation
import androidx.core.view.children

class VshDialogLayout : ViewGroup {

    companion object {
        private const val TAG = ""
        var density = 1f
        var scaledDensity = 1f
        const val iconSize = 32
    }
    fun d(i:Float):Float{return i * density}
    fun d(i:Int):Int{return (i * density).toInt()}
    fun sd(i:Float):Float{return i * density}
    fun sd(i:Int):Int{return (i * scaledDensity).toInt()}
    private var paintText = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private var paintFill = Paint(Paint.ANTI_ALIAS_FLAG)
    private var paintButtonUnselected = Paint(Paint.ANTI_ALIAS_FLAG)
    private var paintOutline = Paint(Paint.ANTI_ALIAS_FLAG)
    private var paintButton = Paint(Paint.ANTI_ALIAS_FLAG)
    private var selectedButtonIndex = 0
    private var titleText = "VshDialogLayout"
    private lateinit var iconBitmap : Bitmap
    private var confirmButton = KeyEvent.KEYCODE_A

    constructor(ctx:Context):super(ctx){
        init()
    }
    constructor(ctx:Context,attrs:AttributeSet):super(ctx,attrs){
        // Load attributes
        val a = context.obtainStyledAttributes(
            attrs, R.styleable.VshDialogLayout, 0, 0
        )

        iconBitmap =
            a.getDrawable(R.styleable.VshDialogLayout_dialogIconBitmap)?.toBitmap(d(iconSize), d(iconSize)) ?:
                    ColorDrawable(Color.TRANSPARENT).toBitmap(1,1)
        titleText = a.getString(R.styleable.VshDialogLayout_dialogTitleText) ?: "VshDialogLayout"

        a.recycle()
        init()
    }
    constructor(ctx:Context,attrs:AttributeSet, styleRes:Int):super(ctx,attrs, styleRes){

        // Load attributes
        val a = context.obtainStyledAttributes(
            attrs, R.styleable.VshDialogLayout, styleRes, 0
        )

        iconBitmap =
            a.getDrawable(R.styleable.VshDialogLayout_dialogIconBitmap)?.toBitmap(d(iconSize), d(iconSize)) ?:
                    ColorDrawable(Color.TRANSPARENT).toBitmap(1,1)
        titleText = a.getString(R.styleable.VshDialogLayout_dialogTitleText) ?: "VshDialogLayout"

        a.recycle()
        init()
    }

    private fun generatePaint(){
        paintText.apply {
            color = Color.WHITE
            textSize = sd(15f)
            textAlign = Paint.Align.LEFT
        }
        paintOutline.apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = d(1.5f)
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
            alpha = 255
        }
        paintFill.apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        paintButtonUnselected.apply {
            color = Color.argb(32,255,255,255)
            style = Paint.Style.FILL
        }
        paintButton.apply {
            color = Color.argb(64,255,255,255)
            style = Paint.Style.FILL
        }
    }
    private var buttons = arrayListOf<VshDialogView.Button>()


    private fun init(){
        rendRect = getSystemPadding()

        val prefs = context.getSharedPreferences("xRegistry.sys", Context.MODE_PRIVATE)
        confirmButton = prefs.getBoolean("xmb_PS3_FAT_CECHA00", false).choose(KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_BUTTON_B)

        isFocusable = true
        density = resources.displayMetrics.density
        scaledDensity = resources.displayMetrics.scaledDensity
        generatePaint()
    }


    private var rendRect = Rect()

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        rendRect = getSystemPadding()
    }

    private var buttonRects = arrayListOf<RectF>()

    var outlinePath = Path()
    var outlineRect = RectF(0f,0f,0f,0f)
    private fun updatePath(){
        outlinePath.reset()
        val w = rendRect.width()
        val h = rendRect.bottom
        outlinePath.moveTo(-w * 0.25f, h * 0.20f)
        outlinePath.lineTo(w * 1.25f,  h * 0.20f)
        outlinePath.lineTo(w * 1.25f, h * 0.85f)
        outlinePath.lineTo( -w * 0.25f, h * 0.85f)
        outlinePath.close()

        outlineRect.set(
            -w * 0.25f, h * 0.20f,
            w * 1.25f, h * 0.85f
        )
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        var retval  = false
        recalculateButtonSize()

        if(event != null){
            val touchPos = PointF(event.x, event.y)
            for(i in buttons.indices){
                if(buttonRects[i].contains(touchPos)){
                    buttons[i].runnable.run()
                    retval = true
                }
            }
        }

        return retval || super.onTouchEvent(event)
    }

    private fun recalculateButtonSize(){
        buttonRects.clear()
        if(buttons.size < 1) return
        val btnWidth = rendRect.width() / buttons.size
        val renderHeight = rendRect.height()
        val paddingSize = d(3f)
        buttons.forEachIndexed { index, _ ->
            buttonRects.add(RectF(
                (btnWidth * index.toFloat()) + paddingSize + rendRect.left,
                (renderHeight * 0.85f) + paddingSize + rendRect.top,
                (btnWidth * (index + 1f)) - paddingSize + rendRect.left,
                (renderHeight + rendRect.top * 1f) - paddingSize
            ))
        }
    }

    fun setButton(newButtons : ArrayList<VshDialogView.Button>){
        buttons = newButtons
    }

    fun addButton(text:String, runnable:()->Unit){
        buttons.add( VshDialogView.Button(text, Runnable { runnable() }) )
    }

    fun addButton(resId:Int, runnable: () -> Unit){
        buttons.add(
            VshDialogView.Button(context.getString(resId),
            Runnable{ runnable() })
        )
    }
    private fun Canvas.drawTextWithYOffset(text:String, x:Float, y:Float, paint:Paint, yOff : Float = 0.5f){
        val yCalc = y + (paint.textSize * yOff)
        this.drawText(text,x,yCalc,paint)
    }


    private fun mUpdate(canvas:Canvas){

        if(outlinePath.isEmpty) updatePath()
        recalculateButtonSize()
        if(isInEditMode){
            canvas.drawARGB(0xff,0x00,0x99,0xff)
        }

        canvas.drawRect(outlineRect, paintOutline)
        paintText.textAlign = Paint.Align.LEFT

        val leftPadding = d(50f) + rendRect.left

        canvas.drawText(titleText, leftPadding,(height * 0.20f) - d(20f),paintText)
        canvas.drawBitmap(iconBitmap, leftPadding - d(40f), (height * 0.20f) -d(10f) - d(32f), paintFill)

        paintText.textAlign = Paint.Align.CENTER
        buttonRects.forEachIndexed { index, rectF ->
            val btn = buttons[index]
            canvas.drawRect(rectF, (index == selectedButtonIndex).choose(paintButton, paintButtonUnselected))
            canvas.drawTextWithYOffset(btn.text, rectF.centerX(), rectF.centerY(), paintText )
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        updatePath()
        val wSpec = MeasureSpec.makeMeasureSpec(outlineRect.width().toInt(), MeasureSpec.EXACTLY)
        val hSpec = MeasureSpec.makeMeasureSpec(outlineRect.height().toInt(), MeasureSpec.EXACTLY)
        measureChildren(wSpec, hSpec)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val rRect = outlineRect.toRect()
        children.forEach {
            it.layout(rRect.left, rRect.top, rRect.right, rRect.bottom)
        }
    }

    override fun dispatchDraw(canvas: Canvas?) {
        super.dispatchDraw(canvas)
        setWillNotDraw(false)
    }

    override fun onDraw(canvas: Canvas?) {
        if(canvas != null) mUpdate(canvas)
        super.onDraw(canvas)
    }

}