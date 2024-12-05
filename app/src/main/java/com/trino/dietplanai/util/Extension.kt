package com.trino.dietplanai.util

import android.app.Activity
import android.content.Intent
import android.widget.Toast

object Extension {
    //todo show toast
    fun Activity.showMessage(message: String){
        Toast.makeText(this,message,Toast.LENGTH_SHORT).show()
    }
    //todo navigation
    fun Activity.navigation(targetClass:Class<*>){
        startActivity(Intent(this,targetClass))
    }
}