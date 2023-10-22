package com.example.tuilivestream.basic

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import com.example.tuilivestream.basic.trtc.ITRTCTXLiveRoom
import com.example.tuilivestream.basic.trtc.ITXTRTCLiveRoomDelegate
import com.example.tuilivestream.basic.trtc.TXCallback
import com.example.tuilivestream.basic.trtc.TXTRTCMixUser
import com.example.tuilivestream.utils.TRTCLogger
import com.tencent.liteav.TXLiteAVCode
import com.tencent.liteav.audio.TXAudioEffectManager
import com.tencent.liteav.beauty.TXBeautyManager
import com.tencent.qcloud.tuicore.TUIConstants
import com.tencent.qcloud.tuicore.TUICore
import com.tencent.rtmp.ui.TXCloudVideoView
import com.tencent.trtc.TRTCCloud
import com.tencent.trtc.TRTCCloudDef
import com.tencent.trtc.TRTCCloudListener
import org.json.JSONException
import org.json.JSONObject

/**
 * Created by Anggit Prayogo on 19/10/23.
 */
class TXTRTCLiveRoom private constructor() : ITRTCTXLiveRoom, TRTCCloudListener() {

    private val TAG = "TXTRTCLiveRoom"
    private val PLAY_TIME_OUT = 5000L
    private val KTC_COMPONENT_LIVEROOM = 4

    private var mTRTCCloud: TRTCCloud? = null
    private var mTXBeautyManager: TXBeautyManager? = null
    private var mOriginRole: Int = 0
    private var mEnterRoomCallback: TXCallback? = null
    private var mExitRoomCallback: TXCallback? = null
    private var mPKCallback: TXCallback? = null
    private var mIsInRoom: Boolean = false
    private var mDelegate: ITXTRTCLiveRoomDelegate? = null
    private var mUserId: String? = null
    private var mRoomId: Int = 0
    private var mTRTCParams: TRTCCloudDef.TRTCParams? = null
    private var mPlayCallbackMap: MutableMap<String, TXCallback> = HashMap()
    private var mPlayTimeoutRunnable: MutableMap<String, Runnable> = HashMap()
    private val mMainHandler = Handler(Looper.getMainLooper())

    private val mVideoEncParam = TRTCCloudDef.TRTCVideoEncParam()

    companion object {
        private var sInstance: TXTRTCLiveRoom? = null

        @Synchronized
        fun getInstance(): TXTRTCLiveRoom {
            if (sInstance == null) {
                sInstance = TXTRTCLiveRoom()
            }
            return sInstance!!
        }
    }

    override fun init(context: Context?) {
        mTRTCCloud = TRTCCloud.sharedInstance(context)
        mTXBeautyManager = mTRTCCloud?.beautyManager
        mPlayCallbackMap.clear()
        mPlayTimeoutRunnable.clear()
    }

    override fun setDelegate(delegate: ITXTRTCLiveRoomDelegate?) {
        mDelegate = delegate
    }

    override fun enterRoom(
        sdkAppId: Int,
        roomId: Int,
        userId: String?,
        userSign: String?,
        role: Int,
        callback: TXCallback?
    ) {
        if (sdkAppId == 0 || TextUtils.isEmpty(userId) || TextUtils.isEmpty(userSign)) {
            callback?.onCallback(-1, "enter trtc room fail. params invalid.")
            return
        }

        mUserId = userId
        mRoomId = roomId
        mOriginRole = role
        mEnterRoomCallback = callback
        mTRTCParams =
            TRTCCloudDef.TRTCParams(sdkAppId, userId, userSign, roomId, "", role.toString())
        internalEnterRoom()
    }

    private fun setFramework() {
        val jsonObject = JSONObject()
        try {
            jsonObject.put("api", "setFramework")
            val params = JSONObject()
            params.put("framework", 1)
            params.put("component", KTC_COMPONENT_LIVEROOM)
            jsonObject.put("params", params)
            mTRTCCloud?.callExperimentalAPI(jsonObject.toString())
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    private fun internalEnterRoom() {
        if (mTRTCParams == null) {
            return
        }
        setFramework()
        mTRTCCloud?.setListener(this)
        mTRTCCloud?.enterRoom(mTRTCParams, TRTCCloudDef.TRTC_APP_SCENE_LIVE)
    }

    override fun onFirstVideoFrame(userId: String?, streamType: Int, width: Int, height: Int) {
        if (userId == null) {
            // `userId` is `null`, indicating that the captured local camera image starts to be rendered
        } else {
            stopTimeoutRunnable(userId)
            val callback = mPlayCallbackMap.remove(userId)
            callback?.onCallback(0, "$userId 播放成功")
        }
    }

    override fun exitRoom(callback: TXCallback?) {
        mUserId = null
        mTRTCParams = null
        mExitRoomCallback = callback
        mPlayCallbackMap.clear()
        mPlayTimeoutRunnable.clear()
        mMainHandler.removeCallbacksAndMessages(null)
        mTRTCCloud?.exitRoom()
        mEnterRoomCallback = null
    }

    override fun startPublish(streamId: String?, callback: TXCallback?) {
        if (!isEnterRoom()) {
            // The user hasn't entered a room. Enter a room first before pushing a stream
            internalEnterRoom()
        }

        if (mOriginRole == TRTCCloudDef.TRTCRoleAudience) {
            mTRTCCloud?.switchRole(TRTCCloudDef.TRTCRoleAnchor)
            val param = TRTCCloudDef.TRTCVideoEncParam()
            param.videoResolution = TRTCCloudDef.TRTC_VIDEO_RESOLUTION_480_270
            param.videoBitrate = 400
            param.videoFps = 15
            param.videoResolutionMode = TRTCCloudDef.TRTC_VIDEO_RESOLUTION_MODE_PORTRAIT
            mTRTCCloud?.setVideoEncoderParam(param)
        } else if (mOriginRole == TRTCCloudDef.TRTCRoleAnchor) {
            val param = TRTCCloudDef.TRTCVideoEncParam()
            param.videoResolution = TRTCCloudDef.TRTC_VIDEO_RESOLUTION_1280_720
            param.videoBitrate = 1800
            param.videoFps = 15
            param.enableAdjustRes = true
            param.videoResolutionMode = TRTCCloudDef.TRTC_VIDEO_RESOLUTION_MODE_PORTRAIT
            mTRTCCloud?.setVideoEncoderParam(param)
        }

        mTRTCCloud?.startPublishing(streamId, TRTCCloudDef.TRTC_VIDEO_STREAM_TYPE_BIG)
        mTRTCCloud?.startLocalAudio()

        callback?.onCallback(0, "start publish success.")
    }

    override fun stopPublish(callback: TXCallback?) {
        mTRTCCloud?.stopPublishing()
        mTRTCCloud?.stopLocalAudio()

        if (mOriginRole == TRTCCloudDef.TRTCRoleAudience) {
            mTRTCCloud?.switchRole(TRTCCloudDef.TRTCRoleAudience)
        } else if (mOriginRole == TRTCCloudDef.TRTCRoleAnchor) {
            mTRTCCloud?.exitRoom()
        }

        callback?.onCallback(0, "stop publish success.")
    }

    override fun startCameraPreview(
        isFront: Boolean,
        view: TXCloudVideoView?,
        callback: TXCallback?
    ) {
        mTRTCCloud?.startLocalPreview(isFront, view)
        mTRTCCloud?.setLocalVideoProcessListener(
            TRTCCloudDef.TRTC_VIDEO_PIXEL_FORMAT_Texture_2D,
            TRTCCloudDef.TRTC_VIDEO_BUFFER_TYPE_TEXTURE,
            object : TRTCVideoFrameListener {
                override fun onGLContextCreated() {}

                override fun onProcessVideoFrame(
                    trtcVideoFrame: TRTCCloudDef.TRTCVideoFrame,
                    trtcVideoFrame1: TRTCCloudDef.TRTCVideoFrame
                ): Int {
                    val map = mutableMapOf<String, Any>()
                    map[TUIConstants.TUIBeauty.PARAM_NAME_SRC_TEXTURE_ID] =
                        trtcVideoFrame.texture.textureId
                    map[TUIConstants.TUIBeauty.PARAM_NAME_FRAME_WIDTH] = trtcVideoFrame.width
                    map[TUIConstants.TUIBeauty.PARAM_NAME_FRAME_HEIGHT] = trtcVideoFrame.height
                    if (TUICore.callService(
                            TUIConstants.TUIBeauty.SERVICE_NAME,
                            TUIConstants.TUIBeauty.METHOD_PROCESS_VIDEO_FRAME,
                            map
                        ) != null
                    ) {
                        trtcVideoFrame1.texture.textureId = TUICore.callService(
                            TUIConstants.TUIBeauty.SERVICE_NAME,
                            TUIConstants.TUIBeauty.METHOD_PROCESS_VIDEO_FRAME,
                            map
                        ) as Int
                    } else {
                        trtcVideoFrame1.texture.textureId = trtcVideoFrame.texture.textureId
                    }
                    return 0
                }

                override fun onGLContextDestory() {
                    TUICore.callService(
                        TUIConstants.TUIBeauty.SERVICE_NAME,
                        TUIConstants.TUIBeauty.METHOD_DESTROY_XMAGIC,
                        null
                    )
                }
            }
        )

        callback?.onCallback(0, "success")
    }

    override fun stopCameraPreview() {
        mTRTCCloud?.stopLocalPreview()
    }

    override fun switchCamera() {
        mTRTCCloud?.switchCamera()
    }

    override fun setMirror(isMirror: Boolean) {
        if (isMirror) {
            mTRTCCloud?.setLocalViewMirror(TRTCCloudDef.TRTC_VIDEO_MIRROR_TYPE_ENABLE)
        } else {
            mTRTCCloud?.setLocalViewMirror(TRTCCloudDef.TRTC_VIDEO_MIRROR_TYPE_DISABLE)
        }
    }

    override fun muteLocalAudio(mute: Boolean) {
        mTRTCCloud?.muteLocalAudio(mute)
    }

    override fun muteRemoteAudio(userId: String?, mute: Boolean) {
        mTRTCCloud?.muteRemoteAudio(userId, mute)
    }

    override fun muteAllRemoteAudio(mute: Boolean) {
        mTRTCCloud?.muteAllRemoteAudio(mute)
    }

    override fun isEnterRoom(): Boolean {
        return mIsInRoom
    }

    override fun onEnterRoom(l: Long) {
        TRTCLogger.i(TAG, "on enter room, result:$l")
        if (mEnterRoomCallback != null) {
            if (l > 0) {
                mIsInRoom = true
                mEnterRoomCallback?.onCallback(0, "enter room success.")
                mEnterRoomCallback = null
            } else {
                mIsInRoom = false
                mEnterRoomCallback?.onCallback(
                    l.toInt(),
                    if (l == TXLiteAVCode.ERR_TRTC_USER_SIG_CHECK_FAILED.toLong()) "userSig invalid, please login again" else "enter room fail"
                )
                mEnterRoomCallback = null
            }
        }
    }

    override fun setMixConfig(list: List<TXTRTCMixUser?>?, isPK: Boolean) {
        if (list == null) {
            mTRTCCloud?.setMixTranscodingConfig(null)
        } else {
            // 背景大画面宽高
            var videoWidth = 720
            var videoHeight = 1280

            // 小画面宽高
            var subWidth = 180
            var subHeight = 320

            val offsetX = 5
            var offsetY = 50

            var bitrate = 200

            val resolution = TRTCCloudDef.TRTC_VIDEO_RESOLUTION_960_540
            when (resolution) {
                TRTCCloudDef.TRTC_VIDEO_RESOLUTION_160_160 -> {
                    videoWidth = 160
                    videoHeight = 160
                    subWidth = 32
                    subHeight = 48
                    offsetY = 10
                    bitrate = 200
                }

                TRTCCloudDef.TRTC_VIDEO_RESOLUTION_320_180 -> {
                    videoWidth = 192
                    videoHeight = 336
                    subWidth = 54
                    subHeight = 96
                    offsetY = 30
                    bitrate = 400
                }

                TRTCCloudDef.TRTC_VIDEO_RESOLUTION_320_240 -> {
                    videoWidth = 240
                    videoHeight = 320
                    subWidth = 54
                    subHeight = 96
                    offsetY = 30
                    bitrate = 400
                }

                TRTCCloudDef.TRTC_VIDEO_RESOLUTION_480_480 -> {
                    videoWidth = 480
                    videoHeight = 480
                    subWidth = 72
                    subHeight = 128
                    bitrate = 600
                }

                TRTCCloudDef.TRTC_VIDEO_RESOLUTION_640_360 -> {
                    videoWidth = 368
                    videoHeight = 640
                    subWidth = 90
                    subHeight = 160
                    bitrate = 800
                }

                TRTCCloudDef.TRTC_VIDEO_RESOLUTION_640_480 -> {
                    videoWidth = 480
                    videoHeight = 640
                    subWidth = 90
                    subHeight = 160
                    bitrate = 800
                }

                TRTCCloudDef.TRTC_VIDEO_RESOLUTION_960_540 -> {
                    videoWidth = 544
                    videoHeight = 960
                    subWidth = 160
                    subHeight = 288
                    bitrate = 1000
                }

                TRTCCloudDef.TRTC_VIDEO_RESOLUTION_1280_720 -> {
                    videoWidth = 720
                    videoHeight = 1280
                    subWidth = 192
                    subHeight = 336
                    bitrate = 1500
                }

                else -> {
                }
            }

            val config = TRTCCloudDef.TRTCTranscodingConfig()
            config.videoWidth = videoWidth
            config.videoHeight = videoHeight
            config.videoGOP = 1
            config.videoFramerate = 15
            config.videoBitrate = bitrate
            config.audioSampleRate = 48000
            config.audioBitrate = 64
            config.audioChannels = 1

            // Set the anchor image position in the mixed image
            val mixUser = TRTCCloudDef.TRTCMixUser()
            mixUser.userId = mUserId
            mixUser.roomId = mRoomId.toString()
            mixUser.zOrder = 1
            mixUser.x = 0
            mixUser.y = 0
            mixUser.width = videoWidth
            mixUser.height = videoHeight

            config.mixUsers = arrayListOf()
            config.mixUsers.add(mixUser)

            // Set the small image positions in the mixed image
            var index = 0
            for (txtrtcMixUser in list) {
                val mixUserTemp = TRTCCloudDef.TRTCMixUser()
                mixUserTemp.userId = txtrtcMixUser?.userId
                mixUserTemp.roomId = txtrtcMixUser?.roomId ?: mRoomId.toString()
                mixUserTemp.streamType = TRTCCloudDef.TRTC_VIDEO_STREAM_TYPE_BIG
                mixUserTemp.zOrder = 2 + index
                if (index < 3) {
                    // The first three small images are displayed from bottom to top on the right
                    mixUserTemp.x = videoWidth - offsetX - subWidth
                    mixUserTemp.y = videoHeight - offsetY - index * subHeight - subHeight
                    mixUserTemp.width = subWidth
                    mixUserTemp.height = subHeight
                } else if (index < 6) {
                    // The last three small images are displayed from bottom to top on the left
                    mixUserTemp.x = offsetX
                    mixUserTemp.y = videoHeight - offsetY - (index - 3) * subHeight - subHeight
                    mixUserTemp.width = subWidth
                    mixUserTemp.height = subHeight
                } else {
                    // Up to six small images can be added
                }
                config.mixUsers.add(mixUserTemp)
                ++index
            }
            mTRTCCloud?.setMixTranscodingConfig(config)
        }
    }

    override fun getTXBeautyManager(): TXBeautyManager? {
        return mTXBeautyManager
    }

    override fun getAudioEffectManager(): TXAudioEffectManager? {
        return mTRTCCloud?.audioEffectManager
    }

    override fun setAudioQuality(quality: Int) {
        mTRTCCloud?.setAudioQuality(quality)
    }

    override fun setVideoResolution(resolution: Int) {
        mVideoEncParam.videoResolution = resolution
        mTRTCCloud?.setVideoEncoderParam(mVideoEncParam)
        TRTCLogger.i(TAG, "setVideoResolution:$resolution")
    }

    override fun setVideoFps(fps: Int) {
        mVideoEncParam.videoFps = fps
        mTRTCCloud?.setVideoEncoderParam(mVideoEncParam)
        TRTCLogger.i(TAG, "setVideoFps:$fps")
    }

    override fun setVideoBitrate(bitrate: Int) {
        mVideoEncParam.videoBitrate = bitrate
        mTRTCCloud?.setVideoEncoderParam(mVideoEncParam)
        TRTCLogger.i(TAG, "setVideoBitrate:$bitrate")
    }

    override fun showVideoDebugLog(isShow: Boolean) {
        if (isShow) {
            mTRTCCloud?.showDebugView(2)
        } else {
            mTRTCCloud?.showDebugView(0)
        }
    }

    override fun startPK(roomId: String?, userId: String?, callback: TXCallback?) {
        val jsonObject = JSONObject()
        try {
            jsonObject.put("roomId", roomId?.toInt())
            jsonObject.put("userId", userId)
        } catch (e: Exception) {
            e.printStackTrace()
            return
        }
        mTRTCCloud?.ConnectOtherRoom(jsonObject.toString())
    }

    override fun stopPK() {
        mTRTCCloud?.DisconnectOtherRoom()
    }

    override fun startPlay(userId: String?, view: TXCloudVideoView?, callback: TXCallback?) {
        callback?.let {
            userId?.let {
                mPlayCallbackMap[userId] = callback
                mTRTCCloud?.startRemoteView(userId, view)
                stopTimeoutRunnable(userId)

                val runnable = Runnable {
                    callback.onCallback(-1, "play $userId timeout.")
                }
                mPlayTimeoutRunnable[userId] = runnable
                mMainHandler.postDelayed(runnable, PLAY_TIME_OUT)
            }
        }
    }

    private fun stopTimeoutRunnable(userId: String) {
        val runnable = mPlayTimeoutRunnable[userId]
        runnable?.let { mMainHandler.removeCallbacks(it) }
    }

    override fun stopPlay(userId: String?, callback: TXCallback?) {
        mPlayCallbackMap.remove(userId)
        userId?.let { stopTimeoutRunnable(it) }
        mTRTCCloud?.stopRemoteView(userId)
        callback?.onCallback(0, "stop play success.")
    }

    override fun stopAllPlay() {
        mTRTCCloud?.stopAllRemoteView()
    }

    override fun onRemoteUserEnterRoom(userId: String?) {
        TRTCLogger.i(TAG, "on user enter, user id:$userId")
        if (mDelegate != null) {
            mDelegate!!.onTRTCAnchorEnter(userId!!)
        }
    }

    override fun onRemoteUserLeaveRoom(userId: String, i: Int) {
        TRTCLogger.i(TAG, "on user exit, user id:$userId")
        if (mDelegate != null) {
            mDelegate?.onTRTCAnchorExit(userId)
        }
    }

    override fun onUserVideoAvailable(userId: String, available: Boolean) {
        TRTCLogger.i(TAG, "on user available, user id:$userId available:$available")
        if (mDelegate != null) {
            if (available) {
                mDelegate?.onTRTCStreamAvailable(userId)
            } else {
                mDelegate?.onTRTCStreamUnavailable(userId)
            }
        }
    }

    override fun onExitRoom(i: Int) {
        TRTCLogger.i(TAG, "on exit room.")
        if (mExitRoomCallback != null) {
            mIsInRoom = false
            mExitRoomCallback!!.onCallback(0, "exit room success.")
            mExitRoomCallback = null
        }
    }

    override fun onConnectOtherRoom(s: String?, i: Int, s1: String) {
        TRTCLogger.i(TAG, "on connect other room, code:$i msg:$s1")
        if (mPKCallback != null) {
            if (i == 0) {
                mPKCallback?.onCallback(0, "connect other room success.")
            } else {
                mPKCallback?.onCallback(i, "connect other room fail. msg:$s1")
            }
        }
    }
}