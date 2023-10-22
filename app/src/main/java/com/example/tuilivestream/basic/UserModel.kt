package com.example.tuilivestream.basic
import java.io.Serializable

/**
 * Created by Anggit Prayogo on 19/10/23.
 */
data class UserModel(
    var phone: String? = null,
    var email: String? = null,
    var userId: String? = null,
    var appId: Int = 0,
    var userSig: String? = null,
    var userName: String? = null,
    var userAvatar: String? = null,
    var userType: UserType = UserType.NONE
) : Serializable {
    enum class UserType {
        NONE,
        ROOM,
        CALLING,
        CHAT_SALON,
        VOICE_ROOM,
        LIVE_ROOM,
        CHORUS,
        KARAOKE
    }
}
