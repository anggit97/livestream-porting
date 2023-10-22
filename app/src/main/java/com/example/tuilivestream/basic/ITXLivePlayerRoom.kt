package com.example.tuilivestream.basic

import android.content.Context
import com.example.tuilivestream.basic.trtc.TXCallback
import com.tencent.rtmp.ui.TXCloudVideoView

/**
 * Created by Anggit Prayogo on 19/10/23.
 */
interface ITXLivePlayerRoom {
    fun init(context: Context)

    fun startPlay(playURL: String, view: TXCloudVideoView, callback: TXCallback?)

    fun stopPlay(playURL: String, callback: TXCallback)

    fun stopAllPlay()

    fun muteRemoteAudio(playURL: String, mute: Boolean)

    fun muteAllRemoteAudio(mute: Boolean)

    fun showVideoDebugLog(isShow: Boolean)
}
