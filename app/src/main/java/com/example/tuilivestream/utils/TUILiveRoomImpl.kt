package com.example.tuilivestream.utils

import com.example.tuilivestream.basic.TRTCLiveRoom
import android.content.Context
import android.content.Intent
import android.text.TextUtils
import android.util.Log
import com.blankj.utilcode.util.ToastUtils
import com.example.tuilivestream.TCAudienceActivity
import com.example.tuilivestream.TCCameraAnchorActivity
import com.example.tuilivestream.basic.LiveRoomManager
import com.example.tuilivestream.basic.TRTCLiveRoomCallback
import com.example.tuilivestream.basic.TRTCLiveRoomDef
import com.example.tuilivestream.basic.UserModel
import com.example.tuilivestream.basic.UserModelManager
import com.example.tuilivestream.constant.TCConstants
import com.something.plugin.R
import com.tencent.imsdk.v2.V2TIMGroupInfoResult
import com.tencent.qcloud.tuicore.TUILogin

class TUILiveRoomImpl private constructor(private val context: Context) : TUILiveRoom() {
    private val TAG = "TUILiveRoomImpl"
    private var mListener: TUILiveRoomListener? = null
    private val mLiveRoom: TRTCLiveRoom = TRTCLiveRoom.sharedInstance(context)

    companion object {
        private var sInstance: TUILiveRoomImpl? = null

        @Synchronized
        fun sharedInstance(context: Context): TUILiveRoomImpl {
            if (sInstance == null) {
                sInstance = TUILiveRoomImpl(context)
            }
            return sInstance!!
        }
    }

    fun release() {
        sInstance = null
    }

    override fun createRoom(roomId: Int, name: String, coverUrl: String) {
        if (roomId == 0) {
            TRTCLogger.e(TAG, "roomId is empty")
            mListener?.onRoomCreate(-1, "roomId is empty")
            return
        }

        if (TextUtils.isEmpty(name)) {
            TRTCLogger.e(TAG, "roomId is empty")
            mListener?.onRoomCreate(-1, "roomId is empty")
            return
        }

        if (!TUILogin.isUserLogined()) {
            TRTCLogger.e(TAG, "user not login")
            mListener?.onRoomCreate(-1, "user not login")
            return
        }
        liveVideoLogin(object : TRTCLiveRoomCallback.ActionCallback {
            override fun onCallback(code: Int, msg: String) {
                if (code == 0) {
                    val userModel = UserModel()
                    userModel.userId = roomId.toString()
                    userModel.userName = name
                    userModel.userAvatar = coverUrl
                    val manager = UserModelManager.getInstance()
                    manager.setUserModel(userModel)
                    val intent = Intent(context, TCCameraAnchorActivity::class.java)
                    context.startActivity(intent)
                } else {
                    TRTCLogger.e(TAG, msg)
                    mListener?.onRoomCreate(code, msg)
                }
            }
        })
    }

    override fun enterRoom(roomId: Int) {
        if (roomId == 0) {
            TRTCLogger.e(TAG, "roomId is empty")
            mListener?.onRoomEnter(-1, "roomId is empty")
            return
        }

        if (!TUILogin.isUserLogined()) {
            TRTCLogger.e(TAG, "user not login")
            mListener?.onRoomEnter(-1, "user not login")
            return
        }
        liveVideoLogin(object : TRTCLiveRoomCallback.ActionCallback {
            override fun onCallback(code: Int, msg: String) {
                if (code == 0) {
                    LiveRoomManager.instance?.getGroupInfo(roomId.toString(),
                        object : LiveRoomManager.GetGroupInfoCallback {
                            override fun onSuccess(result: V2TIMGroupInfoResult?) {
                                if (isRoomExist(result)) {
                                    realEnterRoom(roomId)
                                } else {
                                    ToastUtils.showLong(R.string.trtcliveroom_room_not_exist)
                                }
                            }

                            override fun onFailed(code: Int, msg: String?) {
                                ToastUtils.showLong(msg)
                            }
                        })
                } else {
                    TRTCLogger.e(TAG, msg)
                    mListener?.onRoomEnter(code, msg)
                }
            }
        })
    }

    override fun setListener(listener: TUILiveRoomListener) {
        mListener = listener
    }

    private fun isRoomExist(result: V2TIMGroupInfoResult?): Boolean {
        if (result == null) {
            Log.e(TAG, "room not exist result is null")
            return false
        }
        return result.resultCode == 0
    }

    private fun realEnterRoom(roomId: Int) {
        mLiveRoom.getRoomInfos(listOf(roomId), object : TRTCLiveRoomCallback.RoomInfoCallback {
            override fun onCallback(code: Int, msg: String, list: List<TRTCLiveRoomDef.TRTCLiveRoomInfo>?) {
                if (code == 0 && !list.isNullOrEmpty()) {
                    val info = list[0]
                    gotoAudience(roomId, info.ownerId, info.roomName)
                }
            }
        })
    }

    private fun gotoAudience(roomId: Int, anchorId: String, anchorName: String) {
        val intent = Intent(context, TCAudienceActivity::class.java)
        intent.putExtra(TCConstants.GROUP_ID, roomId)
        intent.putExtra(TCConstants.PUSHER_ID, anchorId)
        intent.putExtra(TCConstants.PUSHER_NAME, anchorName)
        context.startActivity(intent)
    }

    private fun liveVideoLogin(callback: TRTCLiveRoomCallback.ActionCallback) {
        val config = TRTCLiveRoomDef.TRTCLiveRoomConfig(false, "")
        mLiveRoom.login(
            TUILogin.getSdkAppId(), TUILogin.getUserId(), TUILogin.getUserSig(), config,
            object : TRTCLiveRoomCallback.ActionCallback {
                override fun onCallback(code: Int, msg: String) {
                    if (code == 0) {
                        mLiveRoom.setSelfProfile(
                            TUILogin.getNickName(),
                            TUILogin.getFaceUrl(),
                            object : TRTCLiveRoomCallback.ActionCallback {
                                override fun onCallback(code: Int, msg: String) {
                                    callback.onCallback(code, msg)
                                }
                            }
                        )
                    } else {
                        callback.onCallback(code, msg)
                    }
                }
            }
        )
    }
}

