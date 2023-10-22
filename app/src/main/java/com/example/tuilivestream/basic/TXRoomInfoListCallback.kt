package com.example.tuilivestream.basic

import com.example.tuilivestream.basic.base.TXRoomInfo

/**
 * Created by Anggit Prayogo on 19/10/23.
 */
interface TXRoomInfoListCallback {
    fun onCallback(code: Int, msg: String, list: List<TXRoomInfo>)
}