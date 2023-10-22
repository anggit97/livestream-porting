package com.example.tuilivestream.utils

import com.example.tuilivestream.basic.TRTCLiveRoomDelegate
import com.tencent.liteav.basic.log.TXCLog
import java.lang.ref.WeakReference

object TRTCLogger {
    private var sDelegate: WeakReference<TRTCLiveRoomDelegate>? = null

    fun setDelegate(delegate: TRTCLiveRoomDelegate?) {
        sDelegate = delegate?.let { WeakReference(it) }
    }

    fun e(tag: String, message: String) {
        TXCLog.e(tag, message)
        callback("e", tag, message)
    }

    fun w(tag: String, message: String) {
        TXCLog.w(tag, message)
        callback("w", tag, message)
    }

    fun i(tag: String, message: String) {
        TXCLog.i(tag, message)
        callback("i", tag, message)
    }

    fun d(tag: String, message: String) {
        TXCLog.d(tag, message)
        callback("d", tag, message)
    }

    private fun callback(level: String, tag: String, message: String) {
        sDelegate?.get()?.onDebugLog("[$level][$tag] $message")
    }
}

