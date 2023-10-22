package com.example.tuilivestream.basic

import android.util.Log
import com.tencent.imsdk.v2.V2TIMGroupInfoResult
import com.tencent.imsdk.v2.V2TIMManager
import com.tencent.imsdk.v2.V2TIMValueCallback

/**
 * Created by Anggit Prayogo on 19/10/23.
 */
class LiveRoomManager {
    private var mRoomCallback: RoomCallback? = null
    fun addCallback(callback: RoomCallback?) {
        mRoomCallback = callback
    }

    fun removeCallback() {
        mRoomCallback = null
    }

    fun createRoom(roomId: Int, callback: ActionCallback?) {
        if (mRoomCallback != null) {
            mRoomCallback!!.onRoomCreate(roomId, callback)
        }
    }

    fun destroyRoom(roomId: Int, callback: ActionCallback?) {
        if (mRoomCallback != null) {
            mRoomCallback!!.onRoomDestroy(roomId, callback)
        }
    }

    fun getRoomIdList(callback: GetCallback?) {
        if (mRoomCallback != null) {
            mRoomCallback!!.onGetRoomIdList(callback)
        }
    }

    interface RoomCallback {
        fun onRoomCreate(roomId: Int, callback: ActionCallback?)
        fun onRoomDestroy(roomId: Int, callback: ActionCallback?)
        fun onGetRoomIdList(callback: GetCallback?)
    }

    interface ActionCallback {
        fun onSuccess()
        fun onError(code: Int, message: String?)
    }

    interface GetCallback {
        fun onSuccess(list: List<Int?>?)
        fun onError(code: Int, message: String?)
    }

    fun isAddCallBack(): Boolean {
        return mRoomCallback != null
    }

    fun getGroupInfo(roomId: String, callback: GetGroupInfoCallback) {
        val roomIdList: MutableList<String> = ArrayList()
        roomIdList.add(roomId)
        Log.i(TAG, "get room id list $roomIdList")
        V2TIMManager.getGroupManager()
            .getGroupsInfo(roomIdList, object : V2TIMValueCallback<List<V2TIMGroupInfoResult?>?> {
                override fun onError(i: Int, s: String) {
                    Log.e(
                        TAG,
                        "get group info list fail, code:$i msg: $s"
                    )
                    callback.onFailed(-1, s)
                }

                override fun onSuccess(resultList: List<V2TIMGroupInfoResult?>?) {
                    if (resultList != null && !resultList.isEmpty()) {
                        val result = resultList[0]
                        callback.onSuccess(result)
                    } else {
                        callback.onFailed(-1, "get groupInfo List is null")
                    }
                }
            })
    }

    interface GetGroupInfoCallback {
        fun onSuccess(result: V2TIMGroupInfoResult?)
        fun onFailed(code: Int, msg: String?)
    }

    companion object {
        private const val TAG = "LiveRoomManager"
        private var sInstance: LiveRoomManager? = null
        val instance: LiveRoomManager?
            get() {
                if (sInstance == null) {
                    synchronized(LiveRoomManager::class.java) {
                        if (sInstance == null) {
                            sInstance = LiveRoomManager()
                        }
                    }
                }
                return sInstance
            }
    }
}