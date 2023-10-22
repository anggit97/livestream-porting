package com.example.tuilivestream.basic

import android.content.Context
import com.example.tuilivestream.basic.room.ITXRoomServiceDelegate
import com.example.tuilivestream.basic.trtc.TXCallback

/**
 * Created by Anggit Prayogo on 19/10/23.
 */
interface ITXRoomService {
    fun init(context: Context)

    fun setDelegate(delegate: ITXRoomServiceDelegate)

    fun login(sdkAppId: Int, userId: String, userSign: String, callback: TXCallback)

    fun logout(callback: TXCallback)

    fun setSelfProfile(userName: String, avatarURL: String, callback: TXCallback)

    fun createRoom(roomId: String, roomInfo: String, coverUrl: String, callback: TXCallback)

    fun destroyRoom(callback: TXCallback)

    fun enterRoom(roomId: String, callback: TXCallback)

    fun exitRoom(callback: TXCallback)

    fun getRoomInfos(roomId: List<String>, callback: TXRoomInfoListCallback)

    fun updateStreamId(streamId: String, callback: TXCallback)

    fun getAudienceList(callback: TXUserListCallback)

    fun getUserInfo(userList: List<String>, callback: TXUserListCallback)

    fun sendRoomTextMsg(msg: String, callback: TXCallback)

    fun sendRoomCustomMsg(cmd: String, message: String, callback: TXCallback)

    fun requestJoinAnchor(reason: String, timeout: Int, callback: TXCallback): String

    fun responseJoinAnchor(userId: String, agree: Boolean, reason: String)

    fun cancelRequestJoinAnchor(requestId: String, reason: String, callback: TXCallback)

    fun kickoutJoinAnchor(userId: String, callback: TXCallback): String

    fun requestRoomPK(roomId: String, userId: String, timeout: Int, callback: TXCallback): String

    fun responseRoomPK(userId: String, agree: Boolean, reason: String)

    fun cancelRequestRoomPK(requestId: String, reason: String, callback: TXCallback)

    fun resetRoomStatus()

    fun quitRoomPK(callback: TXCallback): String

    fun exchangeStreamId(userId: String): String

    fun isLogin(): Boolean

    fun isEnterRoom(): Boolean

    fun getOwnerUserId(): String

    fun isOwner(): Boolean

    fun isPKing(): Boolean

    fun getPKRoomId(): String

    fun getPKUserId(): String
}
