package com.example.drawingboard

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

/**
 * 画板canvas  画笔paint  路径path
 * 创建一个类的子类时要看那个类是否有构造函数
 * 如果有构造函数就需要重写它的构造函数
 */
class DrawView:View {
    //画笔默认颜色
    private var mColor = Color.BLACK
    //画笔粗细
    private var mStrokeSize = 5f
    //画笔
    private val mPaint = Paint().apply{
        color= mColor
        strokeWidth= mStrokeSize
        strokeJoin= Paint.Join.ROUND
        strokeCap= Paint.Cap.ROUND
        style= Paint.Style.STROKE
    }
    //路径
    private var mPath:Path? = null
    //保存所有路径
    private val pathList =mutableListOf<CustomPath>()
    constructor(context:Context):super(context)
    constructor(context: Context,attrs:AttributeSet?):super(context,attrs)


    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        //绘制之前的所有路径
        if (pathList.size > 0){
            pathList.forEach{
                mPaint.color=it.color
                mPaint.strokeWidth=it.size
                canvas?.drawPath(it.path,mPaint)
            }
        }
        //绘制当前路径
        if (mPath != null){
            mPaint.color= mColor
            mPaint.strokeWidth= mStrokeSize
            canvas?.drawPath(mPath!!,mPaint)
        }
    }

    //记录画的轨迹
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when(event?.action){
            MotionEvent.ACTION_DOWN->{
                //创建新的路径
                mPath = Path()
                //当前触摸点就是这个路径的起始点
                mPath?.moveTo(event.x,event.y)
            }
            MotionEvent.ACTION_MOVE->{
                mPath?.lineTo(event.x,event.y)
                //重新绘制界面  更新
                //会触发onDraw
                invalidate()
            }
            MotionEvent.ACTION_UP->{
                //保存当前路径
                pathList.add(CustomPath(mPath!!,mColor,mStrokeSize))
                mPath = null
            }
        }
        return true
    }

    //提供几个跟外部交互的接口
    //1.画笔颜色
    fun changeColor(colorString:String){
        mColor = Color.parseColor(colorString)
    }
    //2.画笔的粗细
    fun changeStrokeSize(size:Float){
        mStrokeSize = size
    }
    //3.undo  撤销
    fun undo(){
        if (pathList.size > 0){
            pathList.removeLast()
            invalidate()
        }
    }
    //4.橡皮擦
    fun erase(){
        mColor = Color.WHITE
    }
}