package com.example.tuilivestream.basic.room

import com.example.tuilivestream.basic.base.TXRoomInfo
import com.example.tuilivestream.basic.base.TXUserInfo

/**
 * Created by Anggit Prayogo on 19/10/23.
 */
interface ITXRoomServiceDelegate {
    fun onRoomInfoChange(txRoomInfo: TXRoomInfo)
    fun onRoomDestroy(roomId: String?)
    fun onRoomAnchorEnter(userId: String?)
    fun onRoomAnchorExit(userId: String?)
    fun onRoomAudienceEnter(userInfo: TXUserInfo)
    fun onRoomAudienceExit(userInfo: TXUserInfo)
    fun onRoomStreamAvailable(userId: String)
    fun onRoomStreamUnavailable(userId: String)
    fun onRoomRequestJoinAnchor(userInfo: TXUserInfo, reason: String?, handleMsgTimeout: Int)
    fun onRoomKickoutJoinAnchor()
    fun onRoomCancelJoinAnchor()
    fun onRoomRequestRoomPK(userInfo: TXUserInfo, timeout: Int)
    fun onRoomResponseRoomPK(roomId: String, userInfo: TXUserInfo)
    fun onRoomCancelRoomPK()
    fun onRoomQuitRoomPk()
    fun onRoomRecvRoomTextMsg(roomId: String, message: String, userInfo: TXUserInfo)
    fun onRoomRecvRoomCustomMsg(
        roomId: String,
        cmd: String,
        message: String,
        userInfo: TXUserInfo
    )

    fun onAudienceRequestJoinAnchorTimeout(userId: String?)
    fun onAnchorRequestRoomPKTimeout(userId: String?)
}