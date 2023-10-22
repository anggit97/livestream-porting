package com.example.tuilivestream

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.tuilivestream.basic.UserModel
import com.example.tuilivestream.basic.UserModelManager
import com.example.tuilivestream.constant.AvatarConstant
import com.example.tuilivestream.credentials.GenerateTestUserSig
import com.something.plugin.databinding.ActivityLoginBinding
import java.util.Random

class LoginActivity : AppCompatActivity() {
    lateinit var binding: ActivityLoginBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initView()
    }

    private fun initView() {
        with(binding){
            tvLogin.setOnClickListener {
                login()
            }
        }
    }

    private fun login() {
        val userId: String = binding.etUserId.getText().toString().trim { it <= ' ' }
        val userModel = UserModel()
        userModel.userId = userId
        userModel.userName = userId
        val index = Random().nextInt(AvatarConstant.USER_AVATAR_ARRAY.size)
        val coverUrl: String = AvatarConstant.USER_AVATAR_ARRAY[index]
        userModel.userAvatar = coverUrl
        userModel.userSig = GenerateTestUserSig.genTestUserSig(userId)
        val manager: UserModelManager = UserModelManager.getInstance()
        manager.setUserModel(userModel)
        val intent = Intent(this@LoginActivity, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}