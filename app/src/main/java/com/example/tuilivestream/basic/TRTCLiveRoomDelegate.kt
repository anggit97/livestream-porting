package com.example.tuilivestream.basic


interface TRTCLiveRoomDelegate {
    /**
     * Component error message, which must be listened for and handled
     */
    fun onError(code: Int, message: String)

    /**
     * Component warning message
     */
    fun onWarning(code: Int, message: String)

    /**
     * Component log message
     */
    fun onDebugLog(message: String)

    /**
     * Notification of room information change
     * <p>
     * Callback for room information change.
     * This callback is usually used to notify users of room status change in co-anchoring
     * and cross-room communication scenarios.
     *
     * @param roomInfo Room information
     */
    fun onRoomInfoChange(roomInfo: TRTCLiveRoomDef.TRTCLiveRoomInfo)

    /**
     * Callback for room termination, which will be received by audience members after the anchor calls `destroyRoom`
     */
    fun onRoomDestroy(roomId: String)

    /**
     * The anchor entered the room. Call `startPlay()` to start playing back the user's video
     */
    fun onAnchorEnter(userId: String)

    /**
     * The anchor exited the room. Call `stopPlay()` to stop playing back the user's video
     */
    fun onAnchorExit(userId: String)

    /**
     * An audience member entered the room
     */
    fun onAudienceEnter(userInfo: TRTCLiveRoomDef.TRTCLiveUserInfo)

    /**
     * An audience member exited the room
     */
    fun onAudienceExit(userInfo: TRTCLiveRoomDef.TRTCLiveUserInfo)

    /**
     * an user video stream available or unAvailable
     *
     * @param userId    userId
     * @param available video stream available or unAvailable
     */
    fun onUserVideoAvailable(userId: String, available: Boolean)

    /**
     * The anchor received a co-anchoring request
     *
     * @param userInfo Co-anchoring user
     * @param reason   Reason
     * @param timeOut  Timeout period for response from the anchor.
     *                 If the anchor does not respond to the request within the period,
     *                 it will be discarded automatically.
     */
    fun onRequestJoinAnchor(userInfo: TRTCLiveRoomDef.TRTCLiveUserInfo, reason: String, timeOut: Int)

    /**
     * An audience member canceled a co-anchoring request
     */
    fun onCancelJoinAnchor()

    /**
     * An audience member was removed from co-anchoring
     */
    fun onKickoutJoinAnchor()

    /**
     * A cross-room communication request was received
     * <p>
     * If the anchor accepts a cross-room communication request from an anchor in another room,
     * he or she should wait for the `onAnchorEnter` callback from `TRTCLiveRoomDelegate`
     * and then call `startPlay()` to play the other anchor's video.
     *
     * @param userInfo
     * @param timeout  Timeout period for response from the anchor.
     *                 If the anchor does not respond to the request within the period,
     *                 it will be discarded automatically.
     */
    fun onRequestRoomPK(userInfo: TRTCLiveRoomDef.TRTCLiveUserInfo, timeout: Int)

    /**
     * The anchor received a request to cancel cross-room communication from another anchor
     */
    fun onCancelRoomPK()

    /**
     * The anchor ended cross-room communication.
     */
    fun onQuitRoomPK()

    /**
     * A text chat message was received.
     */
    fun onRecvRoomTextMsg(message: String, userInfo: TRTCLiveRoomDef.TRTCLiveUserInfo)

    /**
     * A custom message was received.
     */
    fun onRecvRoomCustomMsg(cmd: String, message: String, userInfo: TRTCLiveRoomDef.TRTCLiveUserInfo)

    /**
     * The co-anchoring response timed out
     */
    fun onAudienceRequestJoinAnchorTimeout(userId: String)

    /**
     * The cross-room communication response timed out
     */
    fun onAnchorRequestRoomPKTimeout(userId: String)
}

