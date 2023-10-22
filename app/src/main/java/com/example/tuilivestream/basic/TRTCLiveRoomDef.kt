package com.example.tuilivestream.basic

class TRTCLiveRoomDef {
    companion object {
        const val ROOM_STATUS_NONE = 0
        const val ROOM_STATUS_SINGLE = 1
        const val ROOM_STATUS_LINK_MIC = 2
        const val ROOM_STATUS_PK = 3
    }

    data class TRTCLiveRoomConfig(val useCDNFirst: Boolean, val cdnPlayDomain: String) {
        override fun toString(): String {
            return "TRTCLiveRoomConfig{" +
                    "useCDNFirst=" + useCDNFirst +
                    ", cdnPlayDomain='$cdnPlayDomain'" +
                    '}'
        }
    }

    data class TRTCLiveUserInfo(var userId: String? = null, var userName: String? = null, var userAvatar: String? = null) {
        override fun toString(): String {
            return "TRTCLiveUserInfo{" +
                    "userId='$userId'" +
                    ", userName='$userName'" +
                    ", userAvatar='$userAvatar'" +
                    '}'
        }
    }

    data class TRTCCreateRoomParam(var roomName: String = "", var coverUrl: String = "") {
        override fun toString(): String {
            return "TRTCCreateRoomParam{" +
                    "roomName='$roomName'" +
                    ", coverUrl='$coverUrl'" +
                    '}'
        }
    }

    data class TRTCLiveRoomInfo(
        var roomId: Int = 0,
        var roomName: String = "",
        var coverUrl: String = "",
        var ownerId: String = "",
        var ownerName: String = "",
        var streamUrl: String = "",
        var roomStatus: Int = 0,
        var memberCount: Int = 0
    ) {
        override fun toString(): String {
            return "TRTCLiveRoomInfo{" +
                    "roomId=$roomId" +
                    ", roomName='$roomName'" +
                    ", coverUrl='$coverUrl'" +
                    ", ownerId='$ownerId'" +
                    ", ownerName='$ownerName'" +
                    ", streamUrl='$streamUrl'" +
                    ", roomStatus=$roomStatus" +
                    ", memberCount=$memberCount" +
                    '}'
        }
    }
}

