package com.example.tuilivestream.basic

import android.content.Context
import android.os.Handler
import com.tencent.liteav.audio.TXAudioEffectManager
import com.tencent.liteav.beauty.TXBeautyManager
import com.tencent.rtmp.ui.TXCloudVideoView

abstract class TRTCLiveRoom protected constructor(){

    companion object {
        @Synchronized
        fun sharedInstance(context: Context): TRTCLiveRoom {
            return TRTCLiveRoomImpl.sharedInstance(context)!!
        }

        fun destroySharedInstance() {
            TRTCLiveRoomImpl.destroySharedInstance()
        }
    }

    abstract fun setDelegate(delegate: TRTCLiveRoomDelegate?)

    abstract fun setDelegateHandler(handler: Handler)

    abstract fun login(
        sdkAppId: Int,
        userId: String,
        userSig: String,
        config: TRTCLiveRoomDef.TRTCLiveRoomConfig,
        callback: TRTCLiveRoomCallback.ActionCallback
    )

    abstract fun logout(callback: TRTCLiveRoomCallback.ActionCallback)

    abstract fun setSelfProfile(userName: String?, avatarURL: String?, callback: TRTCLiveRoomCallback.ActionCallback)

    abstract fun createRoom(
        roomId: Int,
        roomParam: TRTCLiveRoomDef.TRTCCreateRoomParam,
        callback: TRTCLiveRoomCallback.ActionCallback
    )

    abstract fun destroyRoom(callback: TRTCLiveRoomCallback.ActionCallback?)

    abstract fun enterRoom(roomId: Int, callback: TRTCLiveRoomCallback.ActionCallback)

    abstract fun exitRoom(callback: TRTCLiveRoomCallback.ActionCallback?)

    abstract fun getRoomInfos(roomIdList: List<Int>, callback: TRTCLiveRoomCallback.RoomInfoCallback)

    abstract fun getAnchorList(callback: TRTCLiveRoomCallback.UserListCallback)

    abstract fun getAudienceList(callback: TRTCLiveRoomCallback.UserListCallback)

    abstract fun startCameraPreview(
        isFront: Boolean,
        view: TXCloudVideoView,
        callback: TRTCLiveRoomCallback.ActionCallback?
    )

    abstract fun stopCameraPreview()

    abstract fun startPublish(streamId: String, callback: TRTCLiveRoomCallback.ActionCallback)

    abstract fun stopPublish(callback: TRTCLiveRoomCallback.ActionCallback?)

    abstract fun startPlay(userId: String, view: TXCloudVideoView, callback: TRTCLiveRoomCallback.ActionCallback?)

    abstract fun stopPlay(userId: String, callback: TRTCLiveRoomCallback.ActionCallback?)

    abstract fun requestJoinAnchor(reason: String, timeout: Int, callback: TRTCLiveRoomCallback.ActionCallback)

    abstract fun responseJoinAnchor(userId: String, agree: Boolean, reason: String)

    abstract fun kickoutJoinAnchor(userId: String, callback: TRTCLiveRoomCallback.ActionCallback)

    abstract fun cancelRequestJoinAnchor(reason: String, callback: TRTCLiveRoomCallback.ActionCallback)

    abstract fun requestRoomPK(
        roomId: Int,
        userId: String,
        timeout: Int,
        callback: TRTCLiveRoomCallback.ActionCallback
    )

    abstract fun responseRoomPK(userId: String, agree: Boolean, reason: String)

    abstract fun cancelRequestRoomPK(userId: String, callback: TRTCLiveRoomCallback.ActionCallback)

    abstract fun quitRoomPK(callback: TRTCLiveRoomCallback.ActionCallback?)

    abstract fun switchCamera()

    abstract fun setMirror(isMirror: Boolean)

    abstract fun muteLocalAudio(mute: Boolean)

    abstract fun muteRemoteAudio(userId: String, mute: Boolean)

    abstract fun muteAllRemoteAudio(mute: Boolean)

    abstract fun getAudioEffectManager(): TXAudioEffectManager?

    abstract fun setAudioQuality(quality: Int)

    abstract fun getBeautyManager(): TXBeautyManager?

    abstract fun sendRoomTextMsg(message: String, callback: TRTCLiveRoomCallback.ActionCallback)

    abstract fun sendRoomCustomMsg(cmd: String, message: String, callback: TRTCLiveRoomCallback.ActionCallback)

    abstract fun showVideoDebugLog(isShow: Boolean)

    abstract fun setVideoResolution(resolution: Int)

    abstract fun setVideoFps(fps: Int)

    abstract fun setVideoBitrate(bitrate: Int)
}
