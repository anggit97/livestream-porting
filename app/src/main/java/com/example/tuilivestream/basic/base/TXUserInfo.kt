package com.example.tuilivestream.basic.base

/**
 * Created by Anggit Prayogo on 19/10/23.
 */
data class TXUserInfo(
    var userId: String = "",
    var userName: String = "",
    var avatarURL: String = ""
) {
    override fun toString(): String {
        return "TXUserInfo{" +
                "userId='$userId'" +
                ", userName='$userName'" +
                ", avatarURL='$avatarURL'" +
                '}'
    }
}
