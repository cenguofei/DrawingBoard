package com.example.drawingboard

import android.Manifest
import android.animation.Animator
import android.app.Instrumentation.ActivityResult
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.BounceInterpolator
import android.view.animation.TranslateAnimation
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.coroutineScope
import com.example.drawingboard.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    private val launcher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        val deniedPermissions = mutableListOf<String>()
        it.entries.forEach { entry ->
            if (!entry.value) {
                deniedPermissions.add(entry.key)
            }
        }
        if (deniedPermissions.isEmpty()) {
            Log.v("test","全部授权")
        } else {
            Log.v("test","未授权：$deniedPermissions")
        }
    }

    //画笔尺寸按钮的状态
    private var brushContainerIsOpen = false
    //记录颜色的视图状态
    private var colorIsOpen = false
    //防止用户连续点击
    private var colorAnimationIsFinished = true

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater, null, false)
        setContentView(binding.root)

        brushBtnEvent()
        brushContainerSizeBtnEvent()
        floatingActionBtnEvent()
        colorBtnEvent()
        eraseBtnEvent()
        undoEvent()
        saveBtnEvent()
    }
    //下载分享
    private fun saveBtnEvent(){
        binding.savaImageBtn.setOnClickListener{
//将DrawView上绘制的内容转化成一张图片
            val bitmap = convertViewToBitmap(binding.drawView)
            //1.将图片保存到本地
            saveToAlbum(bitmap)
            //2.使用内部应用分享
        }
    }
    //1.将图片保存到本地
    private fun saveToAlbum(bitmap: Bitmap){
        lifecycle.coroutineScope.launch{
            val uri = saveImage(bitmap)
            val shareIntent = Intent().apply{
                action= Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM,uri)
                type= "image/jpg"
            }
            startActivity(Intent.createChooser(shareIntent,"share"))
        }

        //判断是否有权限
//        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
//            == PackageManager.PERMISSION_GRANTED) {
//            Log.v("test","有权限")
//            //有权限  保存
//            lifecycle.coroutineScope.launch{
//                val uri = saveImage(bitmap)
//                val shareIntent = Intent().apply{
//                    action= Intent.ACTION_SEND
//                    putExtra(Intent.EXTRA_STREAM,uri)
//                    type= "image/jpg"
//                }
//                startActivity(Intent.createChooser(shareIntent,"share"))
//            }
//        }else{
//            //请求权限
//            Log.v("test","请求权限")
////            requestPermission()
//            launcher.launch(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.READ_EXTERNAL_STORAGE))
//        }
    }
    //保存图片   耗时操作，可以被挂起 ->suspend
    private suspend fun saveImage(bitmap: Bitmap): Uri? {
        var result = false
        var uri:Uri? = null
        val contents = ContentValues().apply{
            put(MediaStore.Images.ImageColumns.DISPLAY_NAME,"abc.jpg")
            put(MediaStore.Images.ImageColumns.MIME_TYPE,"image/jpg")
            if (Build.VERSION.SDK_INT>= Build.VERSION_CODES.R){
                put(MediaStore.Images.ImageColumns.RELATIVE_PATH,Environment.DIRECTORY_PICTURES)
            }
        }
//切换到IO流子线程
        val imageUri = withContext(Dispatchers.IO){
            contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,contents)
        }
        if (imageUri != null){
            //创建输出流  保存到外存上
            val fos =contentResolver.openOutputStream(imageUri)
            result = bitmap.compress(Bitmap.CompressFormat.JPEG,50,fos)
            uri = imageUri
        }
        if (result){
            Toast.makeText(this,"保存成功",Toast.LENGTH_LONG).show()
        }else{
            Toast.makeText(this,"保存失败",Toast.LENGTH_LONG).show()
        }
        return uri!!
    }
    //请求权限
    private fun requestPermission(){
        ActivityResultContracts.RequestMultiplePermissions()
        requestPermissions(
            arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
            ),1
        )
    }
    //将DrawView上绘制的内容转化成一张图片
    private fun convertViewToBitmap(view: View):Bitmap{
        val bitmap = Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE) //底色  背景色
        view.draw(canvas)
        return bitmap
    }
    //撤销事件
    private fun undoEvent(){
        binding.undoImageBtn.setOnClickListener{
            binding.drawView.undo()
        }
    }
    //橡皮擦事件
    private fun eraseBtnEvent(){
        binding.eraserImageBtn.setOnClickListener{
            binding.drawView.erase()
        }
    }
    //颜色按钮事件
    private fun colorBtnEvent(){
        arrayOf(binding.blueBtn,binding.pinkBtn,binding.purpleBtn,binding.orangeBtn).forEach{
            it.setOnClickListener{_->
                binding.drawView.changeColor(it.tag as String)
            }
        }
    }
    //悬浮按钮事件  展开或者合上
    private fun floatingActionBtnEvent(){
        binding.floatingColorButton.setOnClickListener{
            if (colorAnimationIsFinished){
                var space = 0f
                space = if (colorIsOpen) {
                    //关闭
                    dp2px(this, 70)
                } else {
                    //打开
                    -dp2px(this, 70)
                }
                val colorsBtn =arrayOf(binding.blueBtn, binding.pinkBtn, binding.purpleBtn, binding.orangeBtn)
                colorsBtn.forEach{
                    val index = colorsBtn.indexOf(it)
                    it.animate()
                        .translationYBy((index + 1) * space)
                        .setDuration(500)
                        .setInterpolator(BounceInterpolator())
                        .setListener(object : Animator.AnimatorListener {
                            override fun onAnimationStart(animation: Animator) {
                                colorAnimationIsFinished = false
                            }

                            override fun onAnimationEnd(animation: Animator) {
                                colorAnimationIsFinished = true
                            }

                            override fun onAnimationCancel(animation: Animator) {
                            }

                            override fun onAnimationRepeat(animation: Animator) {
                            }
                        })
                        .start()
                }
                colorIsOpen = !colorIsOpen
            }
        }
    }
    //画笔视图上每个尺寸的视图事件
    private fun brushContainerSizeBtnEvent(){
        arrayOf(binding.size1Btn,binding.size2Btn,binding.size3Btn,binding.size4Btn).forEach{
            it.setOnClickListener{_->
                //隐藏容器
                showOrHideBrushContainer(false)
                //更改画笔粗细
                binding.drawView.changeStrokeSize((it.tag as String).toFloat())
            }
        }
    }
    //画笔尺寸按钮事件
    private fun brushBtnEvent(){
        binding.brushImageBtn.setOnClickListener{
            showOrHideBrushContainer(!brushContainerIsOpen)
        }
    }
    //画笔尺寸选择要隐藏或是要打开
    private fun showOrHideBrushContainer(shouldOpen:Boolean){
        var startY = 0f
        var endY = 1f
        if (!shouldOpen){
            //隐藏控件
            binding.brushSizeContainer.visibility= View.GONE
        }else{
            //显示控件
            startY = 1f
            endY = 0f
            binding.brushSizeContainer.visibility= View.VISIBLE
        }
        val anim = TranslateAnimation(
            Animation.ABSOLUTE,0f,
            Animation.ABSOLUTE,0f,
            Animation.RELATIVE_TO_SELF,startY,
            Animation.RELATIVE_TO_SELF,endY
        ).apply{
            duration= 300
            fillAfter= true
            interpolator= BounceInterpolator()
        }
        binding.brushSizeContainer.startAnimation(anim)
        brushContainerIsOpen = !brushContainerIsOpen
    }
}