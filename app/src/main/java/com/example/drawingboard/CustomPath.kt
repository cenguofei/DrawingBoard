package com.example.drawingboard

import android.graphics.Path

//保存上一次次MOVE_UP后的内容
//数据模型
data class CustomPath(var path: Path, var color:Int, var size:Float)