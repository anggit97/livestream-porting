package com.example.tuilivestream.basic.trtc

/**
 * Created by Anggit Prayogo on 19/10/23.
 */
interface ITXTRTCLiveRoomDelegate {
    fun onTRTCAnchorEnter(userId: String)
    fun onTRTCAnchorExit(userId: String)
    fun onTRTCStreamAvailable(userId: String)
    fun onTRTCStreamUnavailable(userId: String)
}