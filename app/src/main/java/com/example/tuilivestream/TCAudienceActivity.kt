package com.example.tuilivestream

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.constraintlayout.widget.Guideline
import com.blankj.utilcode.util.ToastUtils
import com.example.tuilivestream.basic.TRTCLiveRoom
import com.example.tuilivestream.basic.TRTCLiveRoomCallback
import com.example.tuilivestream.basic.TRTCLiveRoomDef
import com.example.tuilivestream.basic.TRTCLiveRoomDelegate
import com.example.tuilivestream.basic.UserModel
import com.example.tuilivestream.basic.UserModelManager
import com.example.tuilivestream.constant.TCConstants
import com.example.tuilivestream.utils.AudienceFunctionView
import com.example.tuilivestream.utils.PermissionHelper
import com.example.tuilivestream.utils.TCUtils
import com.example.tuilivestream.utils.TCVideoView
import com.something.plugin.R
import com.something.plugin.databinding.ActivityTcaudienceBinding
import com.tencent.qcloud.tuicore.TUILogin
import com.tencent.qcloud.tuicore.interfaces.TUILoginListener
import com.tencent.rtmp.ui.TXCloudVideoView
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

class TCAudienceActivity : AppCompatActivity() {
    lateinit var binding: ActivityTcaudienceBinding
    private val mHandler = Handler(Looper.getMainLooper())
    private var mRootView: ConstraintLayout? = null
    private var mVideoViewAnchor: TXCloudVideoView? = null
    private var mVideoViewPKAnchor: TXCloudVideoView? = null
    private var mGuideLineVertical: Guideline? = null
    private var mGuideLineHorizontal: Guideline? = null
    private var mTextAnchorLeave: TextView? = null
    private var mImageBackground: ImageView? = null
    private var mButtonLinkMic: Button? = null
    private var mDialogError: AlertDialog? = null
    private var mVideoViewMgr: TCVideoViewMgr? = null
    private var mPKContainer: RelativeLayout? = null
    private var mToastNotice: Toast? = null
    private var mNoticeTimer: Timer? = null
    private var mFunctionView: AudienceFunctionView? = null
    private lateinit var mLiveRoom: TRTCLiveRoom

    private val mUserInfoMap: ConcurrentMap<String, TRTCLiveRoomDef.TRTCLiveUserInfo> =
        ConcurrentHashMap<String, TRTCLiveRoomDef.TRTCLiveUserInfo>()
    private val mShowLog = false
    private var mLastLinkMicTime: Long = 0
    private var mCurrentAudienceCount: Long = 0
    private var isEnterRoom = false
    private var isUseCDNPlay = false
    private var mIsAnchorEnter = false
    private var mIsBeingLinkMic = false
    private var mRoomId = 0
    private var mCurrentStatus: Int = TRTCLiveRoomDef.ROOM_STATUS_NONE
    private var mAnchorAvatarURL: String? = null
    private var mAnchorNickname: String? = null
    private var mAnchorId: String? = null
    private var mSelfUserId = ""
    private var mSelfNickname = ""
    private var mSelfAvatar = ""
    private var mCoverUrl = ""
    private var mGetAudienceRunnable: Runnable? = null

    // If the anchor does not enter within a certain period of time
    private val mShowAnchorLeave = Runnable {
        if (mTextAnchorLeave != null) {
//            mTextAnchorLeave?.visibility = if (mIsAnchorEnter) View.GONE else View.VISIBLE
//            mImageBackground?.visibility = if (mIsAnchorEnter) View.GONE else View.VISIBLE

            mTextAnchorLeave?.visibility = View.GONE
            mImageBackground?.visibility = View.GONE
        }
    }

    private val mTRTCLiveRoomDelegate: TRTCLiveRoomDelegate = object : TRTCLiveRoomDelegate {
        override fun onError(code: Int, message: String) {}
        override fun onWarning(code: Int, message: String) {}
        override fun onDebugLog(message: String) {}
        override fun onRoomInfoChange(roomInfo: TRTCLiveRoomDef.TRTCLiveRoomInfo) {
            if (isUseCDNPlay) {
                return
            }
            val oldStatus = mCurrentStatus
            mCurrentStatus = roomInfo.roomStatus
            setAnchorViewFull(mCurrentStatus != TRTCLiveRoomDef.ROOM_STATUS_PK)
            Log.d(TAG, "onRoomInfoChange: $mCurrentStatus")
            if (oldStatus == TRTCLiveRoomDef.ROOM_STATUS_PK
                && mCurrentStatus != TRTCLiveRoomDef.ROOM_STATUS_PK
            ) {
                val videoView: TCVideoView = mVideoViewMgr!!.pkUserView
                mVideoViewPKAnchor = videoView.getPlayerVideo()
                if (mPKContainer!!.childCount != 0) {
                    mPKContainer?.removeView(mVideoViewPKAnchor)
                    videoView.addView(mVideoViewPKAnchor)
                    mVideoViewMgr?.clearPKView()
                    mVideoViewPKAnchor = null
                }
            } else if (mCurrentStatus == TRTCLiveRoomDef.ROOM_STATUS_PK) {
                val videoView: TCVideoView = mVideoViewMgr!!.pkUserView
                mVideoViewPKAnchor = videoView.getPlayerVideo()
                videoView.removeView(mVideoViewPKAnchor)
                mPKContainer!!.addView(mVideoViewPKAnchor)
            }
        }

        override fun onRoomDestroy(roomId: String) {
            showErrorAndQuit(0, getString(R.string.trtcliveroom_warning_room_disband))
        }

        override fun onAnchorEnter(userId: String) {
            if (userId == mAnchorId) {
                mIsAnchorEnter = true
                mTextAnchorLeave?.visibility = View.GONE
                mVideoViewAnchor?.visibility = View.VISIBLE
                mImageBackground?.visibility = View.GONE
                mLiveRoom.startPlay(userId, mVideoViewAnchor!!, object : TRTCLiveRoomCallback.ActionCallback {
                    override fun onCallback(code: Int, msg: String) {
                        if (code != 0) {
                            onAnchorExit(userId)
                        }
                    }
                })
            } else {
                val view: TCVideoView = mVideoViewMgr!!.applyVideoView(userId)
                view.showKickoutBtn(false)
                mLiveRoom.startPlay(userId, view.getPlayerVideo(), null)
            }
        }

        override fun onAnchorExit(userId: String) {
            if (userId == mAnchorId) {
                mVideoViewAnchor?.visibility = View.GONE
                mImageBackground?.visibility = View.VISIBLE
                mTextAnchorLeave?.visibility = View.VISIBLE
                mLiveRoom.stopPlay(userId, null)
            } else {
                mVideoViewMgr?.recycleVideoView(userId)
                mLiveRoom.stopPlay(userId, null)
            }
        }

        override fun onAudienceEnter(userInfo: TRTCLiveRoomDef.TRTCLiveUserInfo) {
            Log.d(TAG, "onAudienceEnter: $userInfo")
        }

        override fun onAudienceExit(userInfo: TRTCLiveRoomDef.TRTCLiveUserInfo) {
            Log.d(TAG, "onAudienceExit: $userInfo")
        }

        override fun onUserVideoAvailable(userId: String, available: Boolean) {
            if (userId != mAnchorId) {
                return
            }
            if (available) {
                mTextAnchorLeave?.visibility = View.GONE
                mVideoViewAnchor?.visibility = View.VISIBLE
                mImageBackground?.visibility = View.GONE
                mVideoViewAnchor?.let {
                    mLiveRoom.startPlay(userId, it, object : TRTCLiveRoomCallback.ActionCallback {
                        override fun onCallback(code: Int, msg: String) {
                            if (code != 0) {
                                onAnchorExit(userId)
                            }
                        }
                    })
                }
            } else {
                mVideoViewAnchor?.visibility = View.GONE
                mImageBackground?.visibility = View.VISIBLE
                mTextAnchorLeave?.visibility = View.VISIBLE
                mLiveRoom.stopPlay(userId, null)
            }
        }

        override fun onRequestJoinAnchor(
            userInfo: TRTCLiveRoomDef.TRTCLiveUserInfo,
            reason: String,
            timeout: Int
        ) {}
        override fun onCancelRoomPK() {}
        override fun onCancelJoinAnchor() {}
        override fun onKickoutJoinAnchor() {
            makeToast(
                resources.getString(R.string.trtcliveroom_warning_kick_out_by_anchor),
                Toast.LENGTH_LONG
            ).show()
            stopLinkMic()
        }

        override fun onRequestRoomPK(userInfo: TRTCLiveRoomDef.TRTCLiveUserInfo, timeout: Int) {}
        override fun onQuitRoomPK() {}
        override fun onRecvRoomTextMsg(message: String, userInfo: TRTCLiveRoomDef.TRTCLiveUserInfo) {}
        override fun onRecvRoomCustomMsg(cmd: String, message: String, userInfo: TRTCLiveRoomDef.TRTCLiveUserInfo) {}
        override fun onAudienceRequestJoinAnchorTimeout(userId: String) {}
        override fun onAnchorRequestRoomPKTimeout(userId: String) {}
    }

    private fun setAnchorViewFull(isFull: Boolean) {
        if (isFull) {
            val set = ConstraintSet()
            set.clone(mRootView)
            set.connect(
                mVideoViewAnchor!!.id,
                ConstraintSet.TOP,
                ConstraintSet.PARENT_ID,
                ConstraintSet.TOP
            )
            set.connect(
                mVideoViewAnchor!!.id,
                ConstraintSet.START,
                ConstraintSet.PARENT_ID,
                ConstraintSet.START
            )
            set.connect(
                mVideoViewAnchor!!.id,
                ConstraintSet.BOTTOM,
                ConstraintSet.PARENT_ID,
                ConstraintSet.BOTTOM
            )
            set.connect(
                mVideoViewAnchor!!.id,
                ConstraintSet.END,
                ConstraintSet.PARENT_ID,
                ConstraintSet.END
            )
            set.applyTo(mRootView)
        } else {
            val set = ConstraintSet()
            set.clone(mRootView)
            set.connect(
                mVideoViewAnchor!!.id,
                ConstraintSet.TOP,
                ConstraintSet.PARENT_ID,
                ConstraintSet.TOP
            )
            set.connect(
                mVideoViewAnchor!!.id,
                ConstraintSet.START,
                ConstraintSet.PARENT_ID,
                ConstraintSet.START
            )
            set.connect(
                mVideoViewAnchor!!.id, ConstraintSet.BOTTOM,
                mGuideLineHorizontal!!.id, ConstraintSet.BOTTOM
            )
            set.connect(
                mVideoViewAnchor!!.id,
                ConstraintSet.END,
                mGuideLineVertical!!.id,
                ConstraintSet.END
            )
            set.applyTo(mRootView)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        UserModelManager.getInstance().getUserModel().userType = UserModel.UserType.NONE
        mLiveRoom.showVideoDebugLog(false)
        mHandler.removeCallbacks(mGetAudienceRunnable!!)
        mHandler.removeCallbacks(mShowAnchorLeave)
        exitRoom()
        mVideoViewMgr?.recycleVideoView()
        mVideoViewMgr = null
        stopLinkMic()
        hideNoticeToast()
        TUILogin.removeLoginListener(mTUILoginListener)
    }

    private fun exitRoom() {
        if (isEnterRoom && ::mLiveRoom.isInitialized) {
            mLiveRoom.exitRoom(null)
            isEnterRoom = false
        }
    }


    fun showErrorAndQuit(errorCode: Int, errorMsg: String?) {
        if (isFinishing) {
            return
        }
        if (mDialogError == null) {
            val builder = AlertDialog.Builder(this, R.style.TRTCLiveRoomDialogTheme)
                .setTitle(R.string.trtcliveroom_error)
                .setMessage(errorMsg)
                .setCancelable(false)
                .setNegativeButton(
                    R.string.trtcliveroom_ok
                ) { dialog, which ->
                    mDialogError!!.dismiss()
                    exitRoom()
                    finish()
                }
            mDialogError = builder.create()
        }
        mDialogError?.let {
            if (it.isShowing) {
                it.dismiss()
            }
            it.show()
        }
    }

    private fun makeToast(message: String, duration: Int): Toast {
        val toast = Toast(this)
        val textView = TextView(this)
        textView.setBackgroundColor(resources.getColor(R.color.trtcliveroom_color_bg_toast_green))
        textView.setTextColor(Color.WHITE)
        textView.gravity = Gravity.CENTER_VERTICAL or Gravity.LEFT
        textView.textSize = 16f
        textView.setPadding(30, 40, 30, 40)
        textView.text = message
        toast.view = textView
        toast.duration = duration
        toast.setGravity(Gravity.FILL_HORIZONTAL or Gravity.BOTTOM, 0, 200)
        return toast
    }

    private fun stopLinkMic() {
        mIsBeingLinkMic = false
        if (mButtonLinkMic != null) {
            mButtonLinkMic?.isEnabled = true
            mButtonLinkMic?.setBackgroundResource(R.drawable.trtcliveroom_linkmic_on)
        }
        mFunctionView!!.setSwitchCamVisibility(View.INVISIBLE)
        mLiveRoom.stopCameraPreview()
        mLiveRoom.stopPublish(null)
        mVideoViewMgr?.recycleVideoView(mSelfUserId)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        UserModelManager.getInstance().getUserModel().userType = UserModel.UserType.LIVE_ROOM
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        setContentView(R.layout.trtcliveroom_activity_audience)

        val intent = intent
        isUseCDNPlay = intent.getBooleanExtra(TCConstants.USE_CDN_PLAY, false)
        mRoomId = intent.getIntExtra(TCConstants.GROUP_ID, 0)
        mAnchorId = intent.getStringExtra(TCConstants.PUSHER_ID)
        mAnchorNickname = intent.getStringExtra(TCConstants.PUSHER_NAME)
        mCoverUrl = intent.getStringExtra(TCConstants.COVER_PIC) ?: ""
        mAnchorAvatarURL = intent.getStringExtra(TCConstants.PUSHER_AVATAR)

        val userModel: UserModel = UserModelManager.getInstance().getUserModel()
        mSelfNickname = userModel.userName ?: ""
        mSelfUserId = userModel.userId ?: ""
        mSelfAvatar = userModel.userAvatar ?: ""

        val videoViewList: MutableList<TCVideoView> = ArrayList()
        videoViewList.add(findViewById<View>(R.id.video_view_link_mic_1) as TCVideoView)
        videoViewList.add(findViewById<View>(R.id.video_view_link_mic_2) as TCVideoView)
        videoViewList.add(findViewById<View>(R.id.video_view_link_mic_3) as TCVideoView)
        mVideoViewMgr = TCVideoViewMgr(videoViewList, null)

        mLiveRoom = TRTCLiveRoom.sharedInstance(this)
        mLiveRoom.setDelegate(mTRTCLiveRoomDelegate)

        initView()
        enterRoom()
        mHandler.postDelayed(mShowAnchorLeave, 3000)
        TUILogin.addLoginListener(mTUILoginListener)
    }

    private fun enterRoom() {
        if (isEnterRoom) {
            return
        }
        mLiveRoom.enterRoom(mRoomId, object : TRTCLiveRoomCallback.ActionCallback {
            override fun onCallback(code: Int, msg: String) {
                if (code == 0) {
                    ToastUtils.showShort(R.string.trtcliveroom_tips_enter_room_success)
                    isEnterRoom = true
                    getAudienceList()
                } else {
                    ToastUtils.showLong(getString(R.string.trtcliveroom_tips_enter_room_fail, code))
                    exitRoom()
                    finish()
                }
            }
        })
    }

    private fun getAudienceList() {
        mGetAudienceRunnable = Runnable {
            mLiveRoom.getAudienceList(object : TRTCLiveRoomCallback.UserListCallback {
                override fun onCallback(code: Int, msg: String, list: List<TRTCLiveRoomDef.TRTCLiveUserInfo>) {
                    if (code == 0) {
                        for (info in list) {
                            mUserInfoMap.putIfAbsent(info.userId, info)
                        }
                        mCurrentAudienceCount += list.size.toLong()
                    } else {
                        mHandler.postDelayed(mGetAudienceRunnable!!, 2000)
                    }
                }
            })
        }
        mHandler.postDelayed(mGetAudienceRunnable!!, 2000)
    }


    private fun initView() {
        mFunctionView = findViewById(R.id.audience_function_view)
        mFunctionView?.setRoomId(mRoomId.toString() + "", mAnchorId)
        mFunctionView?.setListener {
            exitRoom()
            finish()
        }
        mFunctionView?.setAnchorInfo(mAnchorAvatarURL, mAnchorNickname, mRoomId.toString() + "")
        mVideoViewAnchor = findViewById<View>(R.id.video_view_anchor) as TXCloudVideoView
        mVideoViewAnchor?.setLogMargin(10f, 10f, 45f, 55f)
        findViewById<View>(R.id.iv_anchor_record_ball).visibility = View.GONE
        mCurrentAudienceCount++
        mImageBackground = findViewById<View>(R.id.audience_background) as ImageView
        mImageBackground?.scaleType = ImageView.ScaleType.CENTER_CROP
        TCUtils.showPicWithUrl(
            this@TCAudienceActivity,
            mImageBackground, mCoverUrl, R.drawable.trtcliveroom_bg_cover
        )
        mButtonLinkMic = findViewById<View>(R.id.audience_btn_linkmic) as Button
        mButtonLinkMic?.visibility = View.VISIBLE
        mButtonLinkMic?.setOnClickListener {
            if (!mIsBeingLinkMic) {
                val curTime = System.currentTimeMillis()
                if (curTime < mLastLinkMicTime + LINK_MIC_INTERVAL) {
                    Toast.makeText(
                        applicationContext,
                        R.string.trtcliveroom_tips_rest, Toast.LENGTH_SHORT
                    ).show()
                } else {
                    mLastLinkMicTime = curTime
                    startLinkMic()
                }
            } else {
                stopLinkMic()
            }
        }
        mGuideLineVertical = findViewById<View>(R.id.gl_vertical) as Guideline
        mGuideLineHorizontal = findViewById<View>(R.id.gl_horizontal) as Guideline
        mPKContainer = findViewById<View>(R.id.pk_container) as RelativeLayout
        mRootView = findViewById<View>(R.id.root) as ConstraintLayout
        mTextAnchorLeave = findViewById<View>(R.id.tv_anchor_leave) as TextView
    }

    private fun startLinkMic() {
        PermissionHelper.requestPermission(this,
            PermissionHelper.PERMISSION_MICROPHONE, object : PermissionHelper.PermissionCallback {
                override fun onGranted() {
                    PermissionHelper.requestPermission(this@TCAudienceActivity,
                        PermissionHelper.PERMISSION_CAMERA, object : PermissionHelper.PermissionCallback {
                            override fun onGranted() {
                                onStartLinkMic()
                            }

                            override fun onDenied() {}
                            override fun onDialogApproved() {}
                            override fun onDialogRefused() {}
                        })
                }

                override fun onDenied() {}
                override fun onDialogApproved() {}
                override fun onDialogRefused() {}
            })
    }

    private fun onStartLinkMic() {
        mButtonLinkMic?.isEnabled = false
        mButtonLinkMic?.setBackgroundResource(R.drawable.trtcliveroom_linkmic_off)
        showNoticeToast(getString(R.string.trtcliveroom_wait_anchor_accept))
        mLiveRoom.requestJoinAnchor(getString(
            R.string.trtcliveroom_request_link_mic_anchor,
            mSelfUserId
        ),
            LINK_MIC_TIMEOUT, object : TRTCLiveRoomCallback.ActionCallback {
                override fun onCallback(code: Int, msg: String) {
                    if (code == 0) {
                        hideNoticeToast()
                        makeToast(
                            getString(R.string.trtcliveroom_anchor_accept_link_mic),
                            Toast.LENGTH_SHORT
                        ).show()
                        joinPusher()
                        return
                    }
                    when (code) {
                        CODE_ERROR -> {
                            makeToast(
                                getString(R.string.trtcliveroom_anchor_refuse_link_request),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        CODE_TIMEOUT -> {
                            ToastUtils.showShort(getString(R.string.trtcliveroom_link_mic_anchor_timeout))
                        }
                        else -> {
                            ToastUtils.showShort(
                                getString(
                                    R.string.trtcliveroom_error_request_link_mic,
                                    msg
                                )
                            )
                        }
                    }
                    mButtonLinkMic?.isEnabled = true
                    hideNoticeToast()
                    mIsBeingLinkMic = false
                    mButtonLinkMic?.setBackgroundResource(R.drawable.trtcliveroom_linkmic_on)
                }
            })
    }

    private fun joinPusher() {
        val videoView = mVideoViewMgr?.applyVideoView(mSelfUserId)
        mLiveRoom.startCameraPreview(true, videoView!!.getPlayerVideo(), object : TRTCLiveRoomCallback.ActionCallback {
            override fun onCallback(code: Int, msg: String) {
                if (code == 0) {
                    mLiveRoom.startPublish(mSelfUserId + "_stream", object : TRTCLiveRoomCallback.ActionCallback {
                        override fun onCallback(code: Int, msg: String) {
                            if (code == 0) {
                                mButtonLinkMic?.isEnabled = true
                                mIsBeingLinkMic = true
                                mFunctionView?.setSwitchCamVisibility(View.VISIBLE)
                            } else {
                                stopLinkMic()
                                mButtonLinkMic?.isEnabled = true
                                mButtonLinkMic?.setBackgroundResource(R.drawable.trtcliveroom_linkmic_on)
                                Toast.makeText(
                                    this@TCAudienceActivity,
                                    getString(R.string.trtcliveroom_fail_link_mic, msg),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    })
                }
            }
        })
    }

    private fun showNoticeToast(text: String) {
        if (mToastNotice == null) {
            mToastNotice = makeToast(text, Toast.LENGTH_LONG)
        }
        if (mNoticeTimer == null) {
            mNoticeTimer = Timer()
        }
        mNoticeTimer?.schedule(object : TimerTask() {
            override fun run() {
                mToastNotice?.show()
            }
        }, 0, 3000)
    }

    private fun hideNoticeToast() {
        if (mToastNotice != null) {
            mToastNotice?.cancel()
            mToastNotice = null
        }
        if (mNoticeTimer != null) {
            mNoticeTimer?.cancel()
            mNoticeTimer = null
        }
    }

    private val mTUILoginListener: TUILoginListener = object : TUILoginListener() {
        override fun onKickedOffline() {
            Log.e(TAG, "onKickedOffline")
            finish()
        }
    }

    companion object {
        private val TAG = TCAudienceActivity::class.java.simpleName
        private const val LINK_MIC_INTERVAL = (3 * 1000).toLong()
        private const val LINK_MIC_TIMEOUT = 15
        private const val CODE_ERROR = -1
        private const val CODE_TIMEOUT = -2
    }
}