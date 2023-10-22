package com.example.tuilivestream.basic

import android.content.Context
import android.os.Bundle
import com.example.tuilivestream.basic.trtc.TXCallback
import com.example.tuilivestream.utils.TRTCLogger
import com.tencent.rtmp.ITXLivePlayListener
import com.tencent.rtmp.TXLiveConstants
import com.tencent.rtmp.TXLivePlayConfig
import com.tencent.rtmp.TXLivePlayer
import com.tencent.rtmp.ui.TXCloudVideoView

class TXLivePlayerRoom private constructor() : ITXLivePlayerRoom {
    private val TAG = "TXLivePlayerRoom"
    private var mLivePlayerMap: MutableMap<String, TXLivePlayer> = HashMap()
    private var mContext: Context? = null

    companion object {
        private var sInstance: TXLivePlayerRoom? = null

        @Synchronized
        fun getInstance(): TXLivePlayerRoom {
            if (sInstance == null) {
                sInstance = TXLivePlayerRoom()
            }
            return sInstance as TXLivePlayerRoom
        }
    }

    private fun isURLValid(url: String?): Boolean {
        return !url.isNullOrBlank() && url.startsWith("http") && url.endsWith("flv")
    }

    override fun init(context: Context) {
        mContext = context.applicationContext
    }

    override fun startPlay(playURL: String, view: TXCloudVideoView, callback: TXCallback?) {
        if (!isURLValid(playURL)) {
            TRTCLogger.e(TAG, "invalid play url:$playURL")
            callback?.onCallback(-1, "无效的链接:$playURL")
            return
        }
        TRTCLogger.i(TAG, "start play, url:$playURL view:$view")
        var player = mLivePlayerMap[playURL]
        if (player != null) {
            TRTCLogger.w(TAG, "already have player with url, stop and restart.")
            player.stopPlay(true)
            player.setPlayerView(null)
            player.setPlayListener(null)
        }
        player = TXLivePlayer(mContext)
        player.setPlayListener(object : ITXLivePlayListener {
            override fun onPlayEvent(event: Int, bundle: Bundle) {
                if (event == TXLiveConstants.PLAY_EVT_RCV_FIRST_I_FRAME) {
                    callback?.onCallback(0, "播放成功")
                } else if (event < 0) {
                    callback?.onCallback(event, "播放失败")
                }
            }

            override fun onNetStatus(bundle: Bundle) {}
        })
        val config = TXLivePlayConfig()
        config.isAutoAdjustCacheTime = true
        player.setConfig(config)
        mLivePlayerMap[playURL] = player
        player.setPlayerView(view)
        val result = player.startLivePlay(playURL, TXLivePlayer.PLAY_TYPE_LIVE_FLV)
        if (result != 0) {
            callback?.onCallback(result, "play fail, errCode:$result")
        }
    }

    override fun stopPlay(playURL: String, callback: TXCallback) {
        if (!isURLValid(playURL)) {
            TRTCLogger.e(TAG, "invalid play url:$playURL")
            callback.onCallback(-1, "invalid play url:$playURL")
            return
        }
        val player = mLivePlayerMap[playURL]
        TRTCLogger.i(TAG, "stop play, url:$playURL")
        if (player == null) {
            TRTCLogger.i(TAG, "stop play fail, can't find player.")
            callback.onCallback(-1, "can't find player with url.")
        } else {
            player.stopPlay(true)
            callback.onCallback(0, "stop play success.")
        }
    }

    override fun stopAllPlay() {
        for (livePlayer in mLivePlayerMap.values) {
            livePlayer.stopPlay(true)
        }
        mLivePlayerMap.clear()
    }

    override fun muteRemoteAudio(playURL: String, mute: Boolean) {
        if (!isURLValid(playURL)) {
            TRTCLogger.e(TAG, "invalid play url:$playURL")
        }
        val player = mLivePlayerMap[playURL]
        if (player == null) {
            TRTCLogger.e(TAG, "mute player audio fail, can't find player with url.")
        } else {
            player.setMute(mute)
            TRTCLogger.i(TAG, "mute player audio success.")
        }
    }

    override fun muteAllRemoteAudio(mute: Boolean) {
        TRTCLogger.i(TAG, "mute all player audio, mute:$mute")
        for (player in mLivePlayerMap.values) {
            player.setMute(mute)
        }
    }

    override fun showVideoDebugLog(isShow: Boolean) {}
}

