package com.example.tuilivestream

import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.tuilivestream.basic.UserModelManager
import com.example.tuilivestream.constant.AvatarConstant
import com.example.tuilivestream.credentials.GenerateTestUserSig
import com.example.tuilivestream.utils.TUILiveRoom
import com.something.plugin.R
import com.something.plugin.databinding.ActivityMainBinding
import com.tencent.qcloud.tuicore.TUILogin
import com.tencent.qcloud.tuicore.interfaces.TUICallback
import com.tencent.qcloud.tuicore.interfaces.TUILoginListener
import java.util.Random

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    lateinit var mLiveVideo: TUILiveRoom

    private val mEditTextWatcher: TextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            binding.tvEnterRoom.isEnabled = !TextUtils.isEmpty(binding.etRoomId.text.toString())
        }

        override fun afterTextChanged(s: Editable) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initData()
        initView()
    }

    private fun initData() {
        TUILogin.addLoginListener(object : TUILoginListener() {
            override fun onKickedOffline() {
                Log.d(TAG, "onKickedOffline: ")
                super.onKickedOffline()
            }

            override fun onUserSigExpired() {
                Log.d(TAG, "onUserSigExpired: ")
                super.onUserSigExpired()
            }
        })
        val userId: String = UserModelManager.getInstance().getUserModel().userId ?: ""
        if (TextUtils.isEmpty(userId)) {
            Toast.makeText(this, getString(R.string.toast_login_success), Toast.LENGTH_SHORT).show()
        }
        TUILogin.login(this, GenerateTestUserSig.SDKAPPID, userId,
            GenerateTestUserSig.genTestUserSig(userId), object : TUICallback() {
                override fun onSuccess() {
                    Log.d(TAG, "onSuccess: ")
                }

                override fun onError(errorCode: Int, errorMessage: String) {
                    Log.d(TAG, "onError: $errorCode - $errorMessage")
                }
            })
    }

    private fun initView() {
        with(binding) {
            etRoomId.addTextChangedListener(mEditTextWatcher)
            mLiveVideo = TUILiveRoom.sharedInstance(this@MainActivity)
            btnCreateRoom.setOnClickListener {
                val index = Random().nextInt(AvatarConstant.USER_AVATAR_ARRAY.size)
                val coverUrl: String = AvatarConstant.USER_AVATAR_ARRAY[index]
                val roomId = TUILogin.getUserId().toInt()
                val roomName = TUILogin.getUserId()
                createRoom(roomId, roomName, coverUrl)
            }
            tvEnterRoom.setOnClickListener {
                val roomId: Int = etRoomId.text.toString().trim { it <= ' ' }.toInt()
                enterRoom(roomId)
            }
        }
    }

    private fun createRoom(roomId: Int, roomName: String, coverUrl: String) {
        mLiveVideo.createRoom(roomId, roomName, coverUrl)
    }

    private fun enterRoom(roomId: Int) {
        mLiveVideo.enterRoom(roomId)
    }

    companion object {
        const val TAG = "TUIIII"
    }
}