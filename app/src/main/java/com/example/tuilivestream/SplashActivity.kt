package com.example.tuilivestream

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import com.example.tuilivestream.basic.UserModelManager

class SplashActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        navigationMain()
    }

    private fun navigationMain() {
        val userModelManager: UserModelManager = UserModelManager.getInstance()
        if (TextUtils.isEmpty(userModelManager.getUserModel().userId)) {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        } else {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
        finish()
    }
}