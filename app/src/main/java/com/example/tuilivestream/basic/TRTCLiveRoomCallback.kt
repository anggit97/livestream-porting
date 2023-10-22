package com.example.tuilivestream.basic

interface TRTCLiveRoomCallback {
    /**
     * General callbacks
     */
    interface ActionCallback {
        fun onCallback(code: Int, msg: String)
    }

    /**
     * Callback for getting room information
     */
    interface RoomInfoCallback {
        fun onCallback(code: Int, msg: String, list: List<TRTCLiveRoomDef.TRTCLiveRoomInfo>?)
    }

    /**
     * Callback for getting member information
     */
    interface UserListCallback {
        fun onCallback(code: Int, msg: String, list: List<TRTCLiveRoomDef.TRTCLiveUserInfo>)
    }
}

