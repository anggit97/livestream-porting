package com.example.tuilivestream.utils

interface TUILiveRoomListener {
    /**
     * Create room callback
     * @param code 0: success. else: fail
     * @param message result message
     */
    fun onRoomCreate(code: Int, message: String)

    /**
     * Enter room callback
     * @param code 0: success. else: fail
     * @param message result message
     */
    fun onRoomEnter(code: Int, message: String)
}

