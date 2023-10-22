package com.example.tuilivestream.basic.base

/**
 * Created by Anggit Prayogo on 19/10/23.
 */
data class TXRoomInfo(
    var roomId: String = "",
    var roomName: String = "",
    var ownerId: String = "",
    var ownerName: String = "",
    var streamUrl: String = "",
    var coverUrl: String = "",
    var memberCount: Int = 0,
    var ownerAvatar: String = "",
    var roomStatus: Int = 0
) {
    override fun toString(): String {
        return "TXRoomInfo{" +
                "roomId='$roomId'" +
                ", roomName='$roomName'" +
                ", ownerId='$ownerId'" +
                ", ownerName='$ownerName'" +
                ", streamUrl='$streamUrl'" +
                ", coverUrl='$coverUrl'" +
                ", memberCount=$memberCount" +
                ", ownerAvatar='$ownerAvatar'" +
                ", roomStatus=$roomStatus" +
                '}'
    }
}
