package com.example.tuilivestream.basic

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import com.example.tuilivestream.basic.TRTCLiveRoomDef.Companion.ROOM_STATUS_NONE
import com.example.tuilivestream.basic.TRTCLiveRoomDef.Companion.ROOM_STATUS_PK
import com.example.tuilivestream.basic.base.TXRoomInfo
import com.example.tuilivestream.basic.base.TXUserInfo
import com.example.tuilivestream.basic.room.ITXRoomServiceDelegate
import com.example.tuilivestream.basic.trtc.ITXTRTCLiveRoomDelegate
import com.example.tuilivestream.basic.trtc.TXCallback
import com.example.tuilivestream.basic.trtc.TXTRTCMixUser
import com.example.tuilivestream.utils.TRTCLogger
import com.tencent.liteav.audio.TXAudioEffectManager
import com.tencent.liteav.beauty.TXBeautyManager
import com.tencent.rtmp.ui.TXCloudVideoView
import com.tencent.trtc.TRTCCloud
import com.tencent.trtc.TRTCCloudDef
import java.lang.ref.WeakReference

/**
 * Created by Anggit Prayogo on 19/10/23.
 */
class TRTCLiveRoomImpl private constructor(context: Context) : TRTCLiveRoom(),
    ITXTRTCLiveRoomDelegate, ITXRoomServiceDelegate {
    private object Role {
        const val UNKNOWN = 0
        const val TRTC_ANCHOR = 1
        const val TRTC_AUDIENCE = 2
        const val CDN_AUDIENCE = 3
    }

    private class TXCallbackHolder(impl: TRTCLiveRoomImpl) : TXCallback {
        private val wefImpl: WeakReference<TRTCLiveRoomImpl>
        private var realCallback: TRTCLiveRoomCallback.ActionCallback? = null

        init {
            wefImpl = WeakReference(impl)
        }

        fun setRealCallback(callback: TRTCLiveRoomCallback.ActionCallback?) {
            realCallback = callback
        }

        override fun onCallback(code: Int, msg: String?) {
            val impl = wefImpl.get()
            impl?.runOnDelegateThread {
                realCallback?.onCallback(code, msg ?: "")
            }
        }
    }

    private var mDelegate: TRTCLiveRoomDelegate? = null
    private val mMainHandler: Handler
    private var mDelegateHandler: Handler
    private var mSDKAppId: Int
    private var mRoomId = 0
    private var mUserId: String
    private var mUserSign: String
    private var mRoomConfig: TRTCLiveRoomDef.TRTCLiveRoomConfig? = null
    private var mRoomLiveStatus = ROOM_STATUS_NONE
    private val mLiveRoomInfo: TRTCLiveRoomDef.TRTCLiveRoomInfo
    private val mAnchorList: MutableSet<String>
    private val mAudienceList: MutableSet<String>
    private val mPlayViewMap: MutableMap<String, TXCloudVideoView>
    private var mCurrentRole: Int
    private var mTargetRole: Int
    private var mOriginalRole: Int
    private val mJoinAnchorCallbackHolder: TXCallbackHolder
    private val mRequestPKHolder: TXCallbackHolder
    private val mJoinAnchorMap: Map<String, String> = HashMap()
    private val mRoomPkMap: MutableMap<String, String> = HashMap()

    init {
        mCurrentRole = Role.CDN_AUDIENCE
        mOriginalRole = Role.CDN_AUDIENCE
        mTargetRole = Role.CDN_AUDIENCE
        mMainHandler = Handler(Looper.getMainLooper())
        mDelegateHandler = Handler(Looper.getMainLooper())
        mLiveRoomInfo = TRTCLiveRoomDef.TRTCLiveRoomInfo()
        mSDKAppId = 0
        mUserId = ""
        mUserSign = ""
        TXLivePlayerRoom.getInstance().init(context)
        TXTRTCLiveRoom.getInstance().init(context)
        TXTRTCLiveRoom.getInstance().setDelegate(this)
        TXRoomService.getInstance().init(context)
        TXRoomService.getInstance().setDelegate(this)
        mPlayViewMap = HashMap()
        mAnchorList = HashSet()
        mAudienceList = HashSet()
        mJoinAnchorCallbackHolder = TXCallbackHolder(this)
        mRequestPKHolder = TXCallbackHolder(this)
    }

    private fun destroy() {
        TXRoomService.getInstance().destroy()
        TRTCCloud.destroySharedInstance()
    }

    private fun runOnMainThread(runnable: Runnable) {
        val handler = mMainHandler
        if (handler != null) {
            if (handler.looper == Looper.myLooper()) {
                runnable.run()
            } else {
                handler.post(runnable)
            }
        } else {
            runnable.run()
        }
    }

    private fun runOnDelegateThread(runnable: Runnable) {
        val handler = mDelegateHandler
        if (handler.looper == Looper.myLooper()) {
            runnable.run()
        } else {
            handler.post(runnable)
        }
    }

    override fun setDelegate(delegate: TRTCLiveRoomDelegate?) {
        runOnMainThread {
            TRTCLogger.setDelegate(delegate)
            mDelegate = delegate
        }
    }

    override fun setDelegateHandler(handler: Handler) {
        runOnDelegateThread {
            mDelegateHandler = handler
        }
    }

    override fun login(
        sdkAppId: Int,
        userId: String,
        userSig: String,
        config: TRTCLiveRoomDef.TRTCLiveRoomConfig,
        callback: TRTCLiveRoomCallback.ActionCallback
    ) {
        runOnMainThread(Runnable {
            TRTCLogger.i(
                TAG, "start login, sdkAppId:" + sdkAppId + " userId:"
                        + userId + " config:" + config + " sign is empty:" + TextUtils.isEmpty(
                    userSig
                )
            )
            if (sdkAppId == 0 || TextUtils.isEmpty(userId) || TextUtils.isEmpty(userSig) || config == null) {
                TRTCLogger.e(TAG, "start login fail. params invalid.")
                callback.onCallback(-1, "登录失败，参数有误")
                return@Runnable
            }
            mSDKAppId = sdkAppId
            mUserId = userId
            mUserSign = userSig
            mRoomConfig = config
            TRTCLogger.i(TAG, "start login room service")
            TXRoomService.getInstance().login(sdkAppId, userId, userSig, object : TXCallback {
                override fun onCallback(code: Int, msg: String?) {
                    TRTCLogger.i(
                        TAG,
                        "login room service finish, code:$code msg:$msg"
                    )
                    runOnDelegateThread {
                        callback?.onCallback(code, msg ?: "")
                    }
                }
            })
        })
    }

    override fun logout(callback: TRTCLiveRoomCallback.ActionCallback) {
        runOnMainThread {
            TRTCLogger.i(TAG, "start logout")
            mSDKAppId = 0
            mUserId = ""
            mUserSign = ""
            TRTCLogger.i(TAG, "start logout room service")
            TXRoomService.getInstance().logout(object : TXCallback {
                override fun onCallback(code: Int, msg: String?) {
                    TRTCLogger.i(
                        TAG,
                        "logout room service finish, code:$code msg:$msg"
                    )
                    runOnDelegateThread {
                        callback.onCallback(code, msg ?: "")
                    }
                }
            })
        }
    }

    override fun setSelfProfile(
        userName: String?,
        avatarURL: String?,
        callback: TRTCLiveRoomCallback.ActionCallback
    ) {
        runOnMainThread {
            TRTCLogger.i(
                TAG,
                "set profile, user name:$userName avatar url:$avatarURL"
            )
            TXRoomService.getInstance().setSelfProfile(userName ?: "", avatarURL ?: "", object : TXCallback {
                override fun onCallback(code: Int, msg: String?) {
                    TRTCLogger.i(
                        TAG,
                        "set profile finish, code:$code msg:$msg"
                    )
                    runOnDelegateThread {
                        msg?.let { callback.onCallback(code, it) }
                    }
                }
            })
        }
    }

    override fun createRoom(
        roomId: Int,
        roomParam: TRTCLiveRoomDef.TRTCCreateRoomParam,
        callback: TRTCLiveRoomCallback.ActionCallback
    ) {
        runOnMainThread(Runnable {
            TRTCLogger.i(
                TAG,
                "create room, room id:$roomId info:$roomParam"
            )
            if (roomId == 0) {
                TRTCLogger.e(TAG, "create room fail. params invalid")
                return@Runnable
            }
            mRoomId = roomId
            mCurrentRole = Role.UNKNOWN
            mTargetRole = Role.UNKNOWN
            mOriginalRole = Role.TRTC_ANCHOR
            mRoomLiveStatus = ROOM_STATUS_NONE
            mAnchorList.clear()
            mAudienceList.clear()
            mTargetRole = Role.TRTC_ANCHOR
            val roomName = roomParam.roomName
            val roomCover = roomParam.coverUrl
            TXRoomService.getInstance()
                .createRoom(roomId.toString(), roomName, roomCover, object : TXCallback {
                    override fun onCallback(code: Int, msg: String?) {
                        TRTCLogger.i(
                            TAG,
                            "create room in service, code:$code msg:$msg"
                        )
                        if (code == 0) {
                            enterTRTCRoomInner(roomId,
                                mUserId,
                                mUserSign,
                                TRTCCloudDef.TRTCRoleAnchor,
                                object : TRTCLiveRoomCallback.ActionCallback {
                                    override fun onCallback(code: Int, msg: String) {
                                        if (code == 0) {
                                            runOnMainThread {
                                                mAnchorList.add(mUserId)
                                                mCurrentRole = Role.TRTC_ANCHOR
                                            }
                                        }
                                        runOnDelegateThread {
                                            msg?.let { callback.onCallback(code, it) }
                                        }
                                    }
                                })
                            return
                        } else {
                            runOnDelegateThread {
                                val delegate = mDelegate
                                msg?.let { delegate?.onError(code, it) }
                            }
                        }
                        runOnDelegateThread {
                            msg?.let { callback.onCallback(code, it) }
                        }
                    }
                })
        })
    }

    override fun destroyRoom(callback: TRTCLiveRoomCallback.ActionCallback?) {
        runOnMainThread {
            TRTCLogger.i(TAG, "start destroy room.")
            quitRoomPK(null)
            TRTCLogger.i(TAG, "start exit trtc room.")
            TXTRTCLiveRoom.getInstance().exitRoom(object : TXCallback {
                override fun onCallback(code: Int, msg: String?) {
                    TRTCLogger.i(
                        TAG,
                        "exit trtc room finish, code:$code msg:$msg"
                    )
                    if (code != 0) {
                        runOnDelegateThread {
                            val delegate = mDelegate
                            msg?.let { delegate?.onError(code, it) }
                        }
                    }
                }
            })
            TRTCLogger.i(TAG, "start destroy room service.")
            setLiveRoomType(false)
            TXRoomService.getInstance().destroyRoom(object : TXCallback {
                override fun onCallback(code: Int, msg: String?) {
                    TRTCLogger.i(
                        TAG,
                        "destroy room finish, code:$code msg:$msg"
                    )
                    runOnDelegateThread {
                        msg?.let { callback?.onCallback(code, it) }
                    }
                }
            })
            mPlayViewMap.clear()
            mCurrentRole = Role.UNKNOWN
            mTargetRole = Role.UNKNOWN
            mOriginalRole = Role.UNKNOWN
            mRoomLiveStatus = ROOM_STATUS_NONE
            mAnchorList.clear()
            mRoomId = 0
            mJoinAnchorCallbackHolder.setRealCallback(null)
            mRequestPKHolder.setRealCallback(null)
        }
    }

    override fun enterRoom(roomId: Int, callback: TRTCLiveRoomCallback.ActionCallback) {
        runOnMainThread {
            mCurrentRole = Role.UNKNOWN
            mTargetRole = Role.UNKNOWN
            mOriginalRole = Role.UNKNOWN
            mRoomLiveStatus = ROOM_STATUS_NONE
            mAnchorList.clear()
            mRoomId = roomId
            var useCDNFirst = false
            val config: TRTCLiveRoomDef.TRTCLiveRoomConfig? = mRoomConfig
            if (config != null) {
                useCDNFirst = config.useCDNFirst
            }
            mOriginalRole = if (useCDNFirst) {
                Role.CDN_AUDIENCE
            } else {
                Role.TRTC_AUDIENCE
            }
            TRTCLogger.i(
                TAG,
                "start enter room, room id:$roomId use cdn:$useCDNFirst"
            )
            val finalUseCDNFirst = useCDNFirst
            TXRoomService.getInstance().enterRoom(roomId.toString(), object : TXCallback {
                override fun onCallback(code: Int, msg: String?) {
                    TRTCLogger.i(
                        TAG, "enter room service finish, room id:"
                                + roomId + " code:" + code + " msg:" + msg
                    )
                    runOnMainThread {
                        if (finalUseCDNFirst) {
                            runOnDelegateThread {
                                msg?.let { callback.onCallback(code, it) }
                            }
                        } else {
                            TRTCLogger.i(
                                TAG,
                                "start enter trtc room."
                            )
                            mTargetRole = Role.TRTC_AUDIENCE
                            if (code != 0) {
                                runOnDelegateThread {
                                    val delegate = mDelegate
                                    msg?.let { delegate?.onError(code, it) }
                                    msg?.let { callback.onCallback(code, it) }
                                }
                            } else {
                                enterTRTCRoomInner(roomId, mUserId, mUserSign,
                                    TRTCCloudDef.TRTCRoleAudience,
                                    object : TRTCLiveRoomCallback.ActionCallback {
                                        override fun onCallback(code: Int, msg: String) {
                                            TRTCLogger.i(
                                                TAG,
                                                "trtc enter room finish, room id:"
                                                        + roomId + " code:" + code + " msg:" + msg
                                            )
                                            runOnMainThread {
                                                if (code == 0) {
                                                    mCurrentRole =
                                                        Role.TRTC_AUDIENCE
                                                }
                                            }
                                            runOnDelegateThread {
                                                callback.onCallback(code, msg)
                                            }
                                        }
                                    })
                            }
                        }
                    }
                }
            })
        }
    }

    override fun exitRoom(callback: TRTCLiveRoomCallback.ActionCallback?) {
        runOnMainThread {
            TRTCLogger.i(TAG, "start exit room.")
            if (mCurrentRole == Role.TRTC_ANCHOR) {
                stopPublish(null)
            }
            TXTRTCLiveRoom.getInstance().exitRoom(object : TXCallback {
                override fun onCallback(code: Int, msg: String?) {
                    if (code != 0) {
                        runOnDelegateThread {
                            val delegate = mDelegate
                            msg?.let { delegate?.onError(code, it) }
                        }
                    }
                }
            })
            TRTCLogger.i(TAG, "start stop all live player.")
            TXLivePlayerRoom.getInstance().stopAllPlay()
            TRTCLogger.i(TAG, "start exit room service.")
            setLiveRoomType(false)
            TXRoomService.getInstance().exitRoom(object : TXCallback {
                override fun onCallback(code: Int, msg: String?) {
                    TRTCLogger.i(
                        TAG,
                        "exit room finish, code:$code msg:$msg"
                    )
                    runOnDelegateThread {
                        msg?.let { callback?.onCallback(code, it) }
                    }
                }
            })
            mPlayViewMap.clear()
            mAnchorList.clear()
            mRoomId = 0
            mTargetRole = Role.UNKNOWN
            mOriginalRole = Role.UNKNOWN
            mCurrentRole = Role.UNKNOWN
            mJoinAnchorCallbackHolder.setRealCallback(null)
            mRequestPKHolder.setRealCallback(null)
        }
    }

    override fun getRoomInfos(
        roomIdList: List<Int>,
        callback: TRTCLiveRoomCallback.RoomInfoCallback
    ) {
        runOnMainThread {
            val trtcLiveRoomInfoList: MutableList<TRTCLiveRoomDef.TRTCLiveRoomInfo> =
                ArrayList()
            TRTCLogger.i(TAG, "start getRoomInfos: $roomIdList")
            val strings: MutableList<String> =
                ArrayList()
            for (id in roomIdList) {
                strings.add(id.toString())
            }
            TXRoomService.getInstance().getRoomInfos(strings, object : TXRoomInfoListCallback {
                override fun onCallback(code: Int, msg: String, list: List<TXRoomInfo>) {
                    if (code == 0) {
                        for (info in list) {
                            TRTCLogger.i(TAG, info.toString())
                            if (TextUtils.isEmpty(info.ownerId)) {
                                continue
                            }
                            val liveRoomInfo = TRTCLiveRoomDef.TRTCLiveRoomInfo()
                            var translateRoomId: Int
                            try {
                                translateRoomId = Integer.valueOf(info.roomId)
                            } catch (e: NumberFormatException) {
                                continue
                            }
                            liveRoomInfo.roomId = translateRoomId
                            liveRoomInfo.memberCount = info.memberCount
                            liveRoomInfo.roomName = info.roomName
                            liveRoomInfo.ownerId = info.ownerId
                            liveRoomInfo.coverUrl = info.coverUrl
                            liveRoomInfo.streamUrl = info.streamUrl
                            liveRoomInfo.ownerName = info.ownerName
                            trtcLiveRoomInfoList.add(liveRoomInfo)
                        }
                        callback.onCallback(code, msg, trtcLiveRoomInfoList)
                    } else {
                        callback.onCallback(code, msg, trtcLiveRoomInfoList)
                    }
                }
            })
        }
    }

    override fun getAnchorList(callback: TRTCLiveRoomCallback.UserListCallback) {
        runOnMainThread {
            val anchorList: List<String> =
                ArrayList(mAnchorList)
            if (anchorList.isNotEmpty()) {
                TRTCLogger.i(TAG, "start getAnchorList")
                TXRoomService.getInstance().getUserInfo(anchorList, object : TXUserListCallback {
                    override fun onCallback(
                        code: Int,
                        msg: String,
                        list: List<TXUserInfo>
                    ) {
                        if (code == 0) {
                            val trtcLiveUserInfoList: MutableList<TRTCLiveRoomDef.TRTCLiveUserInfo> =
                                ArrayList<TRTCLiveRoomDef.TRTCLiveUserInfo>()
                            for (info in list) {
                                TRTCLogger.i(TAG, info.toString())
                                val userInfo = TRTCLiveRoomDef.TRTCLiveUserInfo()
                                userInfo.userId = info.userId
                                userInfo.userName = info.userName
                                userInfo.userAvatar = info.avatarURL
                                trtcLiveUserInfoList.add(userInfo)
                            }
                            callback.onCallback(code, msg, trtcLiveUserInfoList)
                        } else {
                            callback.onCallback(
                                code,
                                msg,
                                ArrayList<TRTCLiveRoomDef.TRTCLiveUserInfo>()
                            )
                        }
                        TRTCLogger.i(TAG, "onCallback: $code $msg")
                    }
                })
            } else {
                callback.onCallback(
                    0,
                    "用户列表为空",
                    ArrayList()
                )
            }
        }
    }

    override fun getAudienceList(callback: TRTCLiveRoomCallback.UserListCallback) {
        runOnMainThread {
            TXRoomService.getInstance().getAudienceList(object : TXUserListCallback {
                override fun onCallback(
                    code: Int,
                    msg: String,
                    list: List<TXUserInfo>
                ) {
                    TRTCLogger.i(
                        TAG, "get audience list finish, code:"
                                + code + " msg:" + msg + " list:" + list.size
                    )
                    runOnDelegateThread {
                        val userList: MutableList<TRTCLiveRoomDef.TRTCLiveUserInfo> =
                            ArrayList<TRTCLiveRoomDef.TRTCLiveUserInfo>()
                        for (info in list) {
                            if (mAnchorList.contains(info.userId)) {
                                continue
                            }
                            val trtcUserInfo = TRTCLiveRoomDef.TRTCLiveUserInfo()
                            trtcUserInfo.userId = info.userId
                            trtcUserInfo.userAvatar = info.avatarURL
                            trtcUserInfo.userName = info.userName
                            userList.add(trtcUserInfo)
                            TRTCLogger.i(
                                TAG,
                                "info:$info"
                            )
                        }
                        callback.onCallback(code, msg, userList)
                    }
                }
            })
        }
    }

    override fun startCameraPreview(
        isFront: Boolean,
        view: TXCloudVideoView,
        callback: TRTCLiveRoomCallback.ActionCallback?
    ) {
        runOnMainThread {
            TRTCLogger.i(TAG, "start camera preview。")
            TXTRTCLiveRoom.getInstance().startCameraPreview(isFront, view, object : TXCallback {
                override fun onCallback(code: Int, msg: String?) {
                    TRTCLogger.i(
                        TAG,
                        "start camera preview finish, code:$code msg:$msg"
                    )
                    runOnDelegateThread {
                        msg?.let { callback?.onCallback(code, it) }
                    }
                }
            })
        }
    }

    override fun stopCameraPreview() {
        runOnMainThread {
            TRTCLogger.i(TAG, "stop camera preview.")
            TXTRTCLiveRoom.getInstance().stopCameraPreview()
        }
    }

    override fun startPublish(streamId: String, callback: TRTCLiveRoomCallback.ActionCallback) {
        runOnMainThread(Runnable {
            var tempStreamId = streamId
            if (TextUtils.isEmpty(tempStreamId)) {
                tempStreamId = mSDKAppId.toString() + "_" + mRoomId + "_" + mUserId + ""
            }
            val finalStreamId = tempStreamId
            if (!isTRTCMode()) {
                if (mRoomId == 0) {
                    TRTCLogger.e(TAG, "start publish error, room id is empty.")
                    runOnDelegateThread {
                        callback.onCallback(
                            -1,
                            "推流失败, room id 为空"
                        )
                    }
                    return@Runnable
                }
                mTargetRole = Role.TRTC_ANCHOR
                TRTCLogger.i(TAG, "enter trtc room before start publish.")
                enterTRTCRoomInner(mRoomId, mUserId, mUserSign,
                    TRTCCloudDef.TRTCRoleAudience, object : TRTCLiveRoomCallback.ActionCallback {
                        override fun onCallback(code: Int, msg: String) {
                            TRTCLogger.i(
                                TAG,
                                "enter trtc room finish, code:$code msg:$msg"
                            )
                            mCurrentRole = Role.TRTC_ANCHOR
                            if (code == 0) {
                                startPublishInner(finalStreamId, callback)
                                if (mOriginalRole == Role.CDN_AUDIENCE) {
                                    changeToTRTCPlay()
                                }
                            } else {
                                runOnDelegateThread { callback.onCallback(code, msg) }
                            }
                        }
                    })
            } else {
                startPublishInner(finalStreamId, object : TRTCLiveRoomCallback.ActionCallback {
                    override fun onCallback(code: Int, msg: String) {
                        runOnDelegateThread { callback.onCallback(code, msg) }
                    }
                })
            }
            TRTCLogger.i(
                TAG,
                "update room service stream id:$finalStreamId"
            )
            TXRoomService.getInstance().updateStreamId(finalStreamId, object : TXCallback {
                override fun onCallback(code: Int, msg: String?) {
                    TRTCLogger.i(
                        TAG,
                        "room service start publish, code:$code msg:$msg"
                    )
                    if (code != 0) {
                        runOnDelegateThread {
                            val delegate = mDelegate
                            msg?.let { delegate?.onError(code, it) }
                        }
                    }
                }
            })
        })
    }

    override fun stopPublish(callback: TRTCLiveRoomCallback.ActionCallback?) {
        runOnMainThread {
            TRTCLogger.i(TAG, "stop publish")
            TXTRTCLiveRoom.getInstance().stopPublish(object : TXCallback {
                override fun onCallback(code: Int, msg: String?) {
                    TRTCLogger.i(
                        TAG,
                        "stop publish finish, code:$code msg:$msg"
                    )
                    if (mOriginalRole == Role.CDN_AUDIENCE) {
                        mTargetRole = Role.CDN_AUDIENCE
                        TRTCLogger.i(TAG, "start exit trtc room.")
                        TXTRTCLiveRoom.getInstance().exitRoom(object : TXCallback {
                            override fun onCallback(code: Int, msg: String?) {
                                TRTCLogger.i(
                                    TAG,
                                    "exit trtc room finish, code:$code msg:$msg"
                                )
                                runOnMainThread {
                                    mCurrentRole = Role.CDN_AUDIENCE
                                    changeToCDNPlay()
                                }
                            }
                        })
                    } else {
                        runOnDelegateThread {
                            callback?.onCallback(code, msg ?: "")
                        }
                    }
                }
            })
            TRTCLogger.i(TAG, "start update stream id")
            TXRoomService.getInstance().updateStreamId("", object : TXCallback {
                override fun onCallback(code: Int, msg: String?) {
                    TRTCLogger.i(
                        TAG,
                        "room service update stream id finish, code:$code msg:$msg"
                    )
                    if (code != 0) {
                        runOnDelegateThread {
                            val delegate = mDelegate
                            msg?.let { delegate?.onError(code, it) }
                        }
                    }
                }
            })
            if (mOriginalRole == Role.TRTC_AUDIENCE || mOriginalRole == Role.CDN_AUDIENCE) {
                TXRoomService.getInstance().quitLinkMic()
            }
        }
    }

    private fun changeToCDNPlay() {
        TRTCLogger.i(TAG, "switch trtc to cdn play")
        TXTRTCLiveRoom.getInstance().stopAllPlay()
        val ownerId: String = TXRoomService.getInstance().getOwnerUserId()
        if (!TextUtils.isEmpty(ownerId)) {
            val leaveAnchorSet: MutableSet<String> = HashSet()
            val anchorIterator = mAnchorList.iterator()
            while (anchorIterator.hasNext()) {
                val userId = anchorIterator.next()
                if (!TextUtils.isEmpty(userId) && userId != ownerId) {
                    leaveAnchorSet.add(userId)
                    anchorIterator.remove()
                    mPlayViewMap.remove(userId)
                }
            }
            val delegate = mDelegate
            if (delegate != null) {
                for (userId in leaveAnchorSet) {
                    delegate.onAnchorExit(userId)
                }
            }
            val ownerPlayURL = getPlayURL(ownerId)
            if (!TextUtils.isEmpty(ownerPlayURL)) {
                val view = mPlayViewMap[ownerId]
                view?.let { TXLivePlayerRoom.getInstance().startPlay(ownerPlayURL ?: "", it, null) }
            } else {
                TRTCLogger.e(
                    TAG,
                    "change to play cdn fail, can't get owner play url, owner id:$ownerId"
                )
            }
        } else {
            TRTCLogger.e(TAG, "change to play cdn fail, can't get owner user id.")
        }
    }

    private fun changeToTRTCPlay() {
        TRTCLogger.i(TAG, "switch cdn to trtc play")
        TXLivePlayerRoom.getInstance().stopAllPlay()
        for (userId in mAnchorList) {
            TXTRTCLiveRoom.getInstance().startPlay(userId, mPlayViewMap[userId], null)
        }
    }

    override fun startPlay(
        userId: String, view: TXCloudVideoView,
        callback: TRTCLiveRoomCallback.ActionCallback?
    ) {
        runOnMainThread {
            mPlayViewMap[userId] = view
            if (isTRTCMode()) {
                TXTRTCLiveRoom.getInstance().startPlay(userId, view, object : TXCallback {
                    override fun onCallback(code: Int, msg: String?) {
                        TRTCLogger.i(
                            TAG,
                            "start trtc play finish, code:$code msg:$msg"
                        )
                        runOnDelegateThread {
                            msg?.let { callback?.onCallback(code, it) }
                        }
                    }
                })
            } else {
                val playURL = getPlayURL(userId)
                if (!TextUtils.isEmpty(playURL)) {
                    TRTCLogger.i(TAG, "start cdn play, url:$playURL")
                    playURL?.let {
                        TXLivePlayerRoom.getInstance().startPlay(it, view, object : TXCallback {
                            override fun onCallback(code: Int, msg: String?) {
                                TRTCLogger.i(
                                    TAG,
                                    "start cdn play finish, code:$code msg:$msg"
                                )
                                runOnDelegateThread {
                                    msg?.let { it1 -> callback?.onCallback(code, it1) }
                                }
                            }
                        })
                    }
                } else {
                    TRTCLogger.e(
                        TAG,
                        "start cdn play error, can't find stream id by user id:$userId"
                    )
                    runOnDelegateThread {
                        callback?.onCallback(-1, "启动CDN播放失败，找不到对应的流ID")
                    }
                }
            }
        }
    }

    override fun stopPlay(userId: String, callback: TRTCLiveRoomCallback.ActionCallback?) {
        runOnMainThread {
            mPlayViewMap.remove(userId)
            if (isTRTCMode()) {
                TXTRTCLiveRoom.getInstance().stopPlay(userId, object : TXCallback {
                    override fun onCallback(code: Int, msg: String?) {
                        TRTCLogger.i(
                            TAG,
                            "stop trtc play finish, code:$code msg:$msg"
                        )
                        runOnDelegateThread {
                            callback?.onCallback(code, msg ?: "")
                        }
                    }
                })
            } else {
                val playURL = getPlayURL(userId)
                if (!TextUtils.isEmpty(playURL)) {
                    TRTCLogger.i(TAG, "stop play, url:$playURL")
                    playURL?.let {
                        TXLivePlayerRoom.getInstance().stopPlay(it, object : TXCallback {
                            override fun onCallback(code: Int, msg: String?) {
                                TRTCLogger.i(
                                    TAG,
                                    "stop cdn play finish, code:$code msg:$msg"
                                )
                                runOnDelegateThread {
                                    callback?.onCallback(code, msg ?: "")
                                }
                            }
                        })
                    }
                } else {
                    TRTCLogger.e(
                        TAG,
                        "stop cdn play error, can't find stream id by user id:$userId"
                    )
                    runOnDelegateThread {
                        callback?.onCallback(-1, "停止播放失败，找不到对应的流ID")
                    }
                }
            }
        }
    }

    override fun requestJoinAnchor(
        reason: String, timeout: Int,
        callback: TRTCLiveRoomCallback.ActionCallback,
    ) {
        runOnMainThread(Runnable {
            if (mRoomLiveStatus == ROOM_STATUS_PK) {
                //正在PK中
                runOnDelegateThread {
                    callback.onCallback(CODE_ERROR, "正在PK中")
                }
                return@Runnable
            }
            if (TXRoomService.getInstance().isLogin()) {
                TRTCLogger.i(TAG, "start join anchor.")
                mJoinAnchorCallbackHolder.setRealCallback(callback)
                TXRoomService.getInstance()
                    .requestJoinAnchor(reason, timeout, mJoinAnchorCallbackHolder)
            } else {
                TRTCLogger.e(TAG, "request join anchor fail, not login yet.")
                runOnDelegateThread {
                    callback.onCallback(
                        CODE_ERROR,
                        "请求上麦失败，IM未登录"
                    )
                }
            }
        })
    }

    override fun responseJoinAnchor(userId: String, agree: Boolean, reason: String) {
        runOnMainThread {
            if (TXRoomService.getInstance().isLogin()) {
                TRTCLogger.i(TAG, "response join anchor.")
                TXRoomService.getInstance().responseJoinAnchor(userId, agree, reason)
            } else {
                TRTCLogger.e(
                    TAG,
                    "response join anchor fail. not login yet."
                )
            }
        }
    }

    override fun kickoutJoinAnchor(userId: String, callback: TRTCLiveRoomCallback.ActionCallback) {
        runOnMainThread {
            if (TXRoomService.getInstance().isLogin()) {
                TRTCLogger.i(TAG, "kick out join anchor.")
                TXRoomService.getInstance().kickoutJoinAnchor(userId, object : TXCallback {
                    override fun onCallback(code: Int, msg: String?) {
                        TRTCLogger.i(
                            TAG,
                            "kick out finish, code:$code msg:$msg"
                        )
                        runOnDelegateThread {
                            callback.onCallback(code, msg ?: "")
                        }
                    }
                })
            } else {
                TRTCLogger.e(TAG, "kick out fail. not login yet.")
                runOnDelegateThread {
                    callback.onCallback(
                        CODE_ERROR,
                        "踢人失败，IM未登录"
                    )
                }
            }
        }
    }

    override fun cancelRequestJoinAnchor(
        userId: String,
        callback: TRTCLiveRoomCallback.ActionCallback
    ) {
        runOnMainThread {
            if (mJoinAnchorMap.containsKey(userId)) {
                val inviteId = mJoinAnchorMap[userId]
                mJoinAnchorCallbackHolder.setRealCallback(callback)
                TXRoomService.getInstance()
                    .cancelRequestJoinAnchor(inviteId ?: "", "", mJoinAnchorCallbackHolder)
            }
        }
    }

    override fun requestRoomPK(
        roomId: Int, userId: String, timeout: Int,
        callback: TRTCLiveRoomCallback.ActionCallback,
    ) {
        if (TXRoomService.getInstance().isLogin()) {
            TRTCLogger.i(TAG, "request room pk.")
            mRequestPKHolder.setRealCallback(callback)
            val inviteId: String = TXRoomService.getInstance().requestRoomPK(
                roomId.toString(),
                userId, timeout, mRequestPKHolder
            )
            mRoomPkMap[mUserId] = inviteId
        } else {
            TRTCLogger.e(TAG, "request room pk fail. not login yet.")
            runOnDelegateThread {
                callback.onCallback(
                    CODE_ERROR,
                    "请求PK失败，IM未登录"
                )
            }
        }
    }

    override fun responseRoomPK(userId: String, agree: Boolean, reason: String) {
        runOnMainThread {
            if (TXRoomService.getInstance().isLogin()) {
                TRTCLogger.i(TAG, "response pk.")
                TXRoomService.getInstance().responseRoomPK(userId, agree, reason)
            } else {
                TRTCLogger.e(TAG, "response pk fail. not login yet.")
            }
        }
    }

    override fun cancelRequestRoomPK(
        userId: String,
        callback: TRTCLiveRoomCallback.ActionCallback
    ) {
        runOnMainThread {
            if (mRoomPkMap.containsKey(mUserId)) {
                val inviteId = mRoomPkMap[userId]
                mRequestPKHolder.setRealCallback(callback)
                inviteId?.let {
                    TXRoomService.getInstance().cancelRequestRoomPK(it, "", mRequestPKHolder)
                }
                mRoomPkMap.remove(mUserId)
            }
        }
    }

    override fun quitRoomPK(callback: TRTCLiveRoomCallback.ActionCallback?) {
        runOnMainThread {
            if (TXRoomService.getInstance().isLogin()) {
                TRTCLogger.i(TAG, "quit pk.")
                TXRoomService.getInstance().quitRoomPK(object : TXCallback {
                    override fun onCallback(code: Int, msg: String?) {
                        TRTCLogger.i(
                            TAG,
                            "quit pk finish, code:$code msg:$msg"
                        )
                        runOnDelegateThread {
                            callback?.onCallback(code, msg ?: "")
                        }
                    }
                })
            } else {
                TRTCLogger.i(TAG, "quit pk fail.not login yet.")
                runOnDelegateThread {
                    callback?.onCallback(
                        CODE_ERROR,
                        "退出PK失败，IM未登录"
                    )
                }
            }
            TXTRTCLiveRoom.getInstance().stopPK()
        }
    }

    override fun switchCamera() {
        runOnMainThread {
            TRTCLogger.i(TAG, "switch camera.")
            TXTRTCLiveRoom.getInstance().switchCamera()
        }
    }

    override fun setMirror(isMirror: Boolean) {
        runOnMainThread {
            TRTCLogger.i(TAG, "set mirror.")
            TXTRTCLiveRoom.getInstance().setMirror(isMirror)
        }
    }

    override fun muteLocalAudio(mute: Boolean) {
        runOnMainThread {
            TRTCLogger.i(TAG, "mute local audio, mute:$mute")
            TXTRTCLiveRoom.getInstance().muteLocalAudio(mute)
        }
    }

    override fun muteRemoteAudio(userId: String, mute: Boolean) {
        runOnMainThread {
            if (isTRTCMode()) {
                TRTCLogger.i(TAG, "mute trtc audio, user id:$userId")
                TXTRTCLiveRoom.getInstance().muteRemoteAudio(userId, mute)
            } else {
                TRTCLogger.i(TAG, "mute cnd audio, user id:$userId")
                val playURL = getPlayURL(userId)
                if (!TextUtils.isEmpty(playURL)) {
                    // 走 CDN
                    TRTCLogger.i(
                        TAG,
                        "mute cdn audio success, url:$playURL"
                    )
                    playURL?.let { TXLivePlayerRoom.getInstance().muteRemoteAudio(it, mute) }
                } else {
                    TRTCLogger.e(
                        TAG,
                        "mute cdn remote audio fail, exchange stream id fail. user id:$userId"
                    )
                }
            }
        }
    }

    override fun muteAllRemoteAudio(mute: Boolean) {
        runOnMainThread {
            if (isTRTCMode()) {
                TRTCLogger.i(
                    TAG,
                    "mute all trtc remote audio success, mute:$mute"
                )
                TXTRTCLiveRoom.getInstance().muteAllRemoteAudio(mute)
            } else {
                TRTCLogger.i(
                    TAG,
                    "mute all cdn audio success, mute:$mute"
                )
                TXLivePlayerRoom.getInstance().muteAllRemoteAudio(mute)
            }
        }
    }

    override fun sendRoomTextMsg(message: String, callback: TRTCLiveRoomCallback.ActionCallback) {
        runOnMainThread {
            TXRoomService.getInstance().sendRoomTextMsg(message, object : TXCallback {
                override fun onCallback(code: Int, msg: String?) {
                    callback.onCallback(code, msg ?: "")
                }
            })
        }
    }

    override fun sendRoomCustomMsg(
        cmd: String, message: String,
        callback: TRTCLiveRoomCallback.ActionCallback
    ) {
        runOnMainThread {
            TXRoomService.getInstance().sendRoomCustomMsg(cmd, message, object : TXCallback {
                override fun onCallback(code: Int, msg: String?) {
                    callback.onCallback(code, msg ?: "")
                }
            })
        }
    }

    override fun showVideoDebugLog(isShow: Boolean) {
        runOnMainThread {
            TXLivePlayerRoom.getInstance().showVideoDebugLog(isShow)
            TXTRTCLiveRoom.getInstance().showVideoDebugLog(isShow)
        }
    }

    override fun getAudioEffectManager(): TXAudioEffectManager? {
        return TXTRTCLiveRoom.getInstance().getAudioEffectManager()
    }

    override fun setAudioQuality(quality: Int) {
        runOnMainThread { TXTRTCLiveRoom.getInstance().setAudioQuality(quality) }
    }

    override fun getBeautyManager(): TXBeautyManager? {
        return TXTRTCLiveRoom.getInstance().getTXBeautyManager()
    }

    override fun setVideoResolution(resolution: Int) {
        runOnMainThread { TXTRTCLiveRoom.getInstance().setVideoResolution(resolution) }
    }

    override fun setVideoFps(fps: Int) {
        runOnMainThread { TXTRTCLiveRoom.getInstance().setVideoFps(fps) }
    }

    override fun setVideoBitrate(bitrate: Int) {
        runOnMainThread { TXTRTCLiveRoom.getInstance().setVideoBitrate(bitrate) }
    }

    private fun setLiveRoomType(updateType: Boolean) {
        if (updateType) {
            UserModelManager.getInstance().getUserModel().userType = UserModel.UserType.LIVE_ROOM
        } else {
            UserModelManager.getInstance().getUserModel().userType = UserModel.UserType.NONE
        }
    }

    private fun enterTRTCRoomInner(
        roomId: Int, userId: String,
        userSign: String, role: Int,
        callback: TRTCLiveRoomCallback.ActionCallback?
    ) {
        TRTCLogger.i(TAG, "enter trtc room.")
        setLiveRoomType(true)
        mTargetRole = Role.TRTC_ANCHOR
        TXTRTCLiveRoom.getInstance()
            .enterRoom(mSDKAppId, roomId, userId, userSign, role, object : TXCallback {
                override fun onCallback(code: Int, msg: String?) {
                    TRTCLogger.i(
                        TAG,
                        "enter trtc room finish, code:$code msg:$msg"
                    )
                    runOnDelegateThread {
                        callback?.onCallback(code, msg ?: "")
                    }
                }
            })
    }

    private fun startPublishInner(
        streamId: String,
        callback: TRTCLiveRoomCallback.ActionCallback?
    ) {
        TRTCLogger.i(TAG, "start publish stream id:$streamId")
        TXTRTCLiveRoom.getInstance().startPublish(streamId, object : TXCallback {
            override fun onCallback(code: Int, msg: String?) {
                TRTCLogger.i(
                    TAG,
                    "start publish stream finish, code:$code msg:$msg"
                )
                runOnDelegateThread {
                    callback?.onCallback(code, msg ?: "")
                }
            }
        })
    }

    private fun isTRTCMode(): Boolean {
        return mCurrentRole == Role.TRTC_ANCHOR || mCurrentRole == Role.TRTC_AUDIENCE || mTargetRole == Role.TRTC_ANCHOR || mTargetRole == Role.TRTC_AUDIENCE
    }

    private fun updateMixConfig() {
        runOnMainThread {
            TRTCLogger.i(
                TAG,
                "start mix stream:" + mAnchorList.size + " status:" + mRoomLiveStatus
            )
            if (TXRoomService.getInstance().isOwner()) {
                if (mAnchorList.size > 0) {
                    val needToMixUserList: MutableList<TXTRTCMixUser> =
                        ArrayList<TXTRTCMixUser>()
                    val isPKing: Boolean = TXRoomService.getInstance().isPKing()
                    if (isPKing) {
                        if (mAnchorList.size == PK_ANCHOR_NUMS) {
                            val userId: String = TXRoomService.getInstance().getPKUserId()
                            val roomId: String = TXRoomService.getInstance().getPKRoomId()
                            if (!TextUtils.isEmpty(userId) && !TextUtils.isEmpty(userId)) {
                                val user = TXTRTCMixUser()
                                user.userId = userId
                                user.roomId = roomId
                                needToMixUserList.add(user)
                            } else {
                                TRTCLogger.e(
                                    TAG,
                                    "set pk mix config fail, pk user id:"
                                            + userId + " pk room id:" + roomId
                                )
                            }
                        } else {
                            TRTCLogger.e(
                                TAG,
                                "set pk mix config fail, available uer size:s" + mAnchorList.size
                            )
                        }
                    } else {
                        for (userId in mAnchorList) {
                            if (userId == mUserId) {
                                continue
                            }
                            val user = TXTRTCMixUser()
                            user.roomId = null
                            user.userId = userId
                            needToMixUserList.add(user)
                        }
                    }
                    if (needToMixUserList.size > 0) {
                        TXTRTCLiveRoom.getInstance().setMixConfig(needToMixUserList, isPKing)
                    } else {
                        TXTRTCLiveRoom.getInstance().setMixConfig(null, false)
                    }
                } else {
                    TXTRTCLiveRoom.getInstance().setMixConfig(null, false)
                }
            }
        }
    }

    private fun getPlayURL(userId: String): String? {
        val streamId: String = TXRoomService.getInstance().exchangeStreamId(userId)
        if (TextUtils.isEmpty(streamId)) {
            TRTCLogger.e(TAG, "user id:$userId exchange stream id fail.")
            return null
        }
        val config: TRTCLiveRoomDef.TRTCLiveRoomConfig? = mRoomConfig
        if (config == null || TextUtils.isEmpty(config.cdnPlayDomain)) {
            TRTCLogger.e(
                TAG,
                "get play domain in config fail, config:$mRoomConfig"
            )
            return null
        }
        return config.cdnPlayDomain + (if (config.cdnPlayDomain.endsWith("/")) "" else "/") + streamId + ".flv"
    }

    override fun onRoomInfoChange(roomInfo: TXRoomInfo) {
        TRTCLogger.i(TAG, "onRoomInfoChange:$roomInfo")
        runOnMainThread {
            mLiveRoomInfo.ownerId = roomInfo.ownerId
            mLiveRoomInfo.coverUrl = roomInfo.coverUrl
            mLiveRoomInfo.roomId = Integer.valueOf(roomInfo.roomId)
            mLiveRoomInfo.roomName = roomInfo.roomName
            mLiveRoomInfo.ownerName = roomInfo.ownerName
            mLiveRoomInfo.streamUrl = roomInfo.streamUrl
            mLiveRoomInfo.roomStatus = roomInfo.roomStatus
            mLiveRoomInfo.memberCount = roomInfo.memberCount
            mRoomLiveStatus = roomInfo.roomStatus
            updateMixConfig()
            runOnDelegateThread {
                val delegate = mDelegate
                delegate?.onRoomInfoChange(mLiveRoomInfo)
            }
        }
    }

    override fun onRoomDestroy(roomId: String?) {
        runOnDelegateThread {
            val delegate = mDelegate
            delegate?.onRoomDestroy(roomId!!)
        }
    }

    override fun onTRTCAnchorEnter(userId: String) {
        TRTCLogger.i(TAG, "onTRTCAnchorEnter:$userId")
        runOnMainThread {
            TXRoomService.getInstance().handleAnchorEnter(userId)
            if (mAnchorList.add(userId)) {
                handleAnchorEnter(userId)
            } else {
                TRTCLogger.e(
                    TAG,
                    "trtc anchor enter, but already exit:$userId"
                )
            }
        }
    }

    private fun handleAnchorEnter(userId: String) {
        updateMixConfig()
        runOnDelegateThread {
            val delegate = mDelegate
            delegate?.onAnchorEnter(userId)
        }
    }

    override fun onTRTCAnchorExit(userId: String) {
        TRTCLogger.i(TAG, "onTRTCAnchorExit:$userId")
        runOnMainThread {
            TXRoomService.getInstance().handleAnchorExit(userId)
            if (mAnchorList.contains(userId)) {
                mAnchorList.remove(userId)
                updateMixConfig()
                if (TXRoomService.getInstance().isOwner()
                    && mAnchorList.size == 1
                ) {
                    TXRoomService.getInstance().resetRoomStatus()
                }
                runOnDelegateThread {
                    val delegate = mDelegate
                    delegate?.onAnchorExit(userId)
                }
            } else {
                TRTCLogger.e(
                    TAG,
                    "trtc anchor exit, but never throw yet, maybe something error."
                )
            }
        }
    }

    override fun onTRTCStreamAvailable(userId: String) {
        runOnDelegateThread {
            val delegate = mDelegate
            delegate?.onUserVideoAvailable(userId, true)
        }
    }

    override fun onTRTCStreamUnavailable(userId: String) {
        runOnDelegateThread {
            val delegate = mDelegate
            delegate?.onUserVideoAvailable(userId, false)
        }
    }

    override fun onRoomAnchorEnter(userId: String?) {}
    override fun onRoomAnchorExit(userId: String?) {}
    override fun onRoomStreamAvailable(userId: String) {
        TRTCLogger.i(TAG, "onRoomStreamAvailable:$userId")
        runOnMainThread(Runnable {
            if (isTRTCMode()) {
                return@Runnable
            }
            if (!mAnchorList.contains(userId)) {
                mAnchorList.add(userId)
                runOnDelegateThread {
                    val delegate = mDelegate
                    delegate?.onAnchorEnter(userId)
                }
            }
        })
    }

    override fun onRoomStreamUnavailable(userId: String) {
        TRTCLogger.i(TAG, "onRoomStreamUnavailable:$userId")
        runOnMainThread(Runnable {
            if (isTRTCMode()) {
                return@Runnable
            }
            if (mAnchorList.contains(userId)) {
                mAnchorList.remove(userId)
                runOnDelegateThread {
                    val delegate = mDelegate
                    delegate?.onAnchorExit(userId)
                }
            } else {
                TRTCLogger.e(TAG, "room anchor exit, but never throw yet, maybe something error.")
            }
        })
    }

    override fun onRoomAudienceEnter(userInfo: TXUserInfo) {
        runOnDelegateThread(Runnable {
            if (mAudienceList.contains(userInfo.userId)) {
                return@Runnable
            }
            val delegate = mDelegate
            if (delegate != null) {
                mAudienceList.add(userInfo.userId)
                val info = TRTCLiveRoomDef.TRTCLiveUserInfo()
                info.userId = userInfo.userId
                info.userAvatar = userInfo.avatarURL
                info.userName = userInfo.userName
                delegate.onAudienceEnter(info)
            }
        })
    }

    override fun onRoomAudienceExit(userInfo: TXUserInfo) {
        runOnDelegateThread {
            val delegate = mDelegate
            if (delegate != null) {
                mAudienceList.remove(userInfo.userId)
                val info = TRTCLiveRoomDef.TRTCLiveUserInfo()
                info.userId = userInfo.userId
                info.userAvatar = userInfo.avatarURL
                info.userName = userInfo.userName
                delegate.onAudienceExit(info)
            }
        }
    }

    override fun onRoomRequestJoinAnchor(
        userInfo: TXUserInfo,
        reason: String?,
        handleMsgTimeout: Int
    ) {
        runOnDelegateThread {
            val delegate = mDelegate
            if (delegate != null) {
                val info = TRTCLiveRoomDef.TRTCLiveUserInfo()
                info.userId = userInfo.userId
                info.userName = userInfo.userName
                info.userAvatar = userInfo.avatarURL
                delegate.onRequestJoinAnchor(info, reason ?: "", handleMsgTimeout)
            }
        }
    }

    override fun onRoomKickoutJoinAnchor() {
        runOnDelegateThread {
            val delegate = mDelegate
            delegate?.onKickoutJoinAnchor()
        }
    }

    override fun onRoomCancelJoinAnchor() {
        runOnDelegateThread {
            val delegate = mDelegate
            delegate?.onCancelJoinAnchor()
        }
    }

    override fun onRoomRequestRoomPK(userInfo: TXUserInfo, timeout: Int) {
        runOnDelegateThread {
            val delegate = mDelegate
            if (delegate != null) {
                val info = TRTCLiveRoomDef.TRTCLiveUserInfo()
                info.userId = userInfo.userId
                info.userName = userInfo.userName
                info.userAvatar = userInfo.avatarURL
                delegate.onRequestRoomPK(info, timeout)
            }
        }
    }

    override fun onRoomResponseRoomPK(roomId: String, userInfo: TXUserInfo) {
        runOnMainThread {
            mRequestPKHolder.setRealCallback(null)
            TRTCLogger.i(
                TAG,
                "recv pk repsonse, room id:" + roomId + " info:" + userInfo.toString()
            )
            if (mCurrentRole == Role.TRTC_ANCHOR || mTargetRole == Role.TRTC_ANCHOR) {
                TXTRTCLiveRoom.getInstance()
                    .startPK(roomId, userInfo.userId, object : TXCallback {
                        override fun onCallback(code: Int, msg: String?) {
                            TRTCLogger.i(
                                TAG,
                                "start pk, code:$code msg:$msg"
                            )
                            if (code != 0) {
                                runOnDelegateThread {
                                    val delegate = mDelegate
                                    msg?.let { delegate?.onError(code, it) }
                                }
                            }
                        }
                    })
            }
        }
    }

    override fun onRoomCancelRoomPK() {
        runOnDelegateThread {
            val delegate = mDelegate
            delegate?.onCancelRoomPK()
        }
    }

    override fun onRoomQuitRoomPk() {
        runOnDelegateThread {
            val delegate = mDelegate
            delegate?.onQuitRoomPK()
        }
    }

    override fun onRoomRecvRoomTextMsg(roomId: String, message: String, userInfo: TXUserInfo) {
        runOnDelegateThread {
            val delegate = mDelegate
            if (delegate != null) {
                val info = TRTCLiveRoomDef.TRTCLiveUserInfo()
                info.userId = userInfo.userId
                info.userName = userInfo.userName
                info.userAvatar = userInfo.avatarURL
                delegate.onRecvRoomTextMsg(message, info)
            }
        }
    }

    override fun onRoomRecvRoomCustomMsg(
        roomId: String, cmd: String,
        message: String, userInfo: TXUserInfo
    ) {
        runOnDelegateThread {
            val delegate = mDelegate
            if (delegate != null) {
                val info = TRTCLiveRoomDef.TRTCLiveUserInfo()
                info.userId = userInfo.userId
                info.userName = userInfo.userName
                info.userAvatar = userInfo.avatarURL
                delegate.onRecvRoomCustomMsg(cmd, message, info)
            }
        }
    }

    override fun onAudienceRequestJoinAnchorTimeout(userId: String?) {
        runOnDelegateThread {
            val delegate = mDelegate
            delegate?.onAudienceRequestJoinAnchorTimeout(userId!!)
        }
    }

    override fun onAnchorRequestRoomPKTimeout(userId: String?) {
        runOnDelegateThread {
            val delegate = mDelegate
            delegate?.onAnchorRequestRoomPKTimeout(userId!!)
        }
    }

    companion object {
        private const val CODE_SUCCESS = 0
        private const val CODE_ERROR = -1
        const val PK_ANCHOR_NUMS = 2
        private const val TAG = "com.example.tuilivestream.basic.TRTCLiveRoom"
        private var sInstance: TRTCLiveRoomImpl? = null

        @Synchronized
        fun sharedInstance(context: Context): TRTCLiveRoom? {
            if (sInstance == null) {
                sInstance = TRTCLiveRoomImpl(context.applicationContext)
            }
            return sInstance
        }

        @Synchronized
        fun destroySharedInstance() {
            if (sInstance != null) {
                sInstance?.destroy()
                sInstance = null
            }
        }
    }
}
