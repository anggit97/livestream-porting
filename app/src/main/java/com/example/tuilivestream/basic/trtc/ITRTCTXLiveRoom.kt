package com.example.tuilivestream.basic.trtc

import android.content.Context
import com.tencent.liteav.audio.TXAudioEffectManager
import com.tencent.liteav.beauty.TXBeautyManager
import com.tencent.rtmp.ui.TXCloudVideoView

/**
 * Created by Anggit Prayogo on 19/10/23.
 */
interface ITRTCTXLiveRoom {
    fun init(context: Context?)
    fun setDelegate(deleagte: ITXTRTCLiveRoomDelegate?)
    fun enterRoom(
        sdkAppId: Int,
        roomId: Int,
        userId: String?,
        userSign: String?,
        role: Int,
        callback: TXCallback?
    )

    fun exitRoom(callback: TXCallback?)
    fun startCameraPreview(isFront: Boolean, view: TXCloudVideoView?, callback: TXCallback?)
    fun stopCameraPreview()
    fun switchCamera()
    fun setMirror(isMirror: Boolean)
    fun muteLocalAudio(mute: Boolean)
    fun startPublish(streamId: String?, callback: TXCallback?)
    fun stopPublish(callback: TXCallback?)
    fun startPlay(userId: String?, view: TXCloudVideoView?, callback: TXCallback?)
    fun stopPlay(userId: String?, callback: TXCallback?)
    fun stopAllPlay()
    fun muteRemoteAudio(userId: String?, mute: Boolean)
    fun muteAllRemoteAudio(mute: Boolean)
    fun showVideoDebugLog(isShow: Boolean)
    fun startPK(roomId: String?, userId: String?, callback: TXCallback?)
    fun stopPK()
    fun isEnterRoom(): Boolean
    fun setMixConfig(list: List<TXTRTCMixUser?>?, isPK: Boolean)
    fun getTXBeautyManager(): TXBeautyManager?
    fun getAudioEffectManager(): TXAudioEffectManager?
    fun setAudioQuality(quality: Int)
    fun setVideoResolution(resolution: Int)
    fun setVideoFps(fps: Int)
    fun setVideoBitrate(bitrate: Int)
}