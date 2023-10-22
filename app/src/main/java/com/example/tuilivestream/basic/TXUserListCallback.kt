package com.example.tuilivestream.basic

import com.example.tuilivestream.basic.base.TXUserInfo

/**
 * Created by Anggit Prayogo on 19/10/23.
 */
interface TXUserListCallback {
    fun onCallback(code: Int, msg: String, list: List<TXUserInfo>)
}
