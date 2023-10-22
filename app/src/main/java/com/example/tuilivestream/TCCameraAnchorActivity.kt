package com.example.tuilivestream

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.Window
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.constraintlayout.widget.Guideline
import com.blankj.utilcode.util.ToastUtils
import com.example.tuilivestream.basic.LiveRoomManager
import com.example.tuilivestream.basic.TRTCLiveRoom
import com.example.tuilivestream.basic.TRTCLiveRoomCallback
import com.example.tuilivestream.basic.TRTCLiveRoomDef
import com.example.tuilivestream.basic.TRTCLiveRoomDelegate
import com.example.tuilivestream.basic.UserModel
import com.example.tuilivestream.basic.UserModelManager
import com.example.tuilivestream.credentials.GenerateTestUserSig.XMAGIC_LICENSE_KEY
import com.example.tuilivestream.credentials.GenerateTestUserSig.XMAGIC_LICENSE_URL
import com.example.tuilivestream.utils.AnchorCountDownView
import com.example.tuilivestream.utils.AnchorFunctionView
import com.example.tuilivestream.utils.AnchorPreFunctionView
import com.example.tuilivestream.utils.PermissionHelper
import com.example.tuilivestream.utils.RTCubeUtils
import com.example.tuilivestream.utils.TCUtils
import com.example.tuilivestream.utils.TCVideoView
import com.example.tuilivestream.utils.TRTCLogger
import com.example.tuilivestream.widgets.ExitConfirmDialogFragment
import com.example.tuilivestream.widgets.FinishDetailDialogFragment
import com.something.plugin.R
import com.something.plugin.databinding.ActivityTccameraAnchorBinding
import com.tencent.qcloud.tuicore.TUIConstants
import com.tencent.qcloud.tuicore.TUICore
import com.tencent.qcloud.tuicore.TUILogin
import com.tencent.qcloud.tuicore.interfaces.TUILoginListener
import com.tencent.qcloud.tuicore.permission.PermissionCallback
import com.tencent.rtmp.ui.TXCloudVideoView
import java.util.Locale
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

open class TCCameraAnchorActivity : AppCompatActivity(), TRTCLiveRoomDelegate {


    protected var mCoverPicUrl: String? = null
    protected var mSelfAvatar: String? = null
    protected var mSelfName: String? = null
    protected var mSelfUserId: String? = null
    protected var mRoomName: String? = null
    protected var mRoomId = 0
    protected var mTotalMemberCount: Long = 0
    protected var mCurrentMemberCount: Long = 0
    protected var mHeartCount: Long = 0
    protected var mIsEnterRoom = false
    protected var mIsCreatingRoom = false

    protected var mLiveRoom: TRTCLiveRoom? = null

    private var mEditLiveRoomName: EditText? = null

    private var mToolbar: View? = null
    protected var mRootView: ConstraintLayout? = null
    protected var mMainHandler = Handler(Looper.getMainLooper())

    private var mBroadcastTimer: Timer? = null
    private var mBroadcastTimerTask: BroadcastTimerTask? =
        null
    protected var mSecond: Long = 0
    private var mErrorDialog: AlertDialog? = null

    private var mPreFunctionView: AnchorPreFunctionView? = null
    private var mPreView: AnchorPreView? = null
    private var mFunctionView: AnchorFunctionView? = null


    private var mImagesAnchorHead: ImageView? = null

    private var mGuideLineVertical: Guideline? = null
    private var mPKContainer: RelativeLayout? = null
    private var mImagePKLayer: ImageView? = null
    private var mVideoViewMgr: TCVideoViewMgr? = null
    private var mCountDownView: AnchorCountDownView? = null
    private var mTXCloudVideoView: TXCloudVideoView? = null
    private val mVideoViewPKAnchor: TXCloudVideoView? = null
//    private val mPKConfirmDialogFragment: ConfirmDialogFragment? = null

    private var mShowLog = false
    private val mIsPaused = false
    private val mAnchorUserIdList: List<String> = ArrayList()
    private val mCurrentStatus: Int = TRTCLiveRoomDef.ROOM_STATUS_NONE


//    private val mLinkMicConfirmDialogFragmentMap: Map<String, ConfirmDialogFragment> =
//        HashMap<String, ConfirmDialogFragment>()


    private val mUserInfoMap: ConcurrentMap<String, TRTCLiveRoomDef.TRTCLiveUserInfo> =
        ConcurrentHashMap<String, TRTCLiveRoomDef.TRTCLiveUserInfo>()

    private val LINK_MIC_MEMBER_MAX = 3


    private lateinit var binding: ActivityTccameraAnchorBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTccameraAnchorBinding.inflate(layoutInflater)
        UserModelManager.getInstance().getUserModel().userType = UserModel.UserType.LIVE_ROOM
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(binding.root)

        val (_, _, userId, _, _, userName, userAvatar) = UserModelManager.getInstance()
            .getUserModel()
        mSelfUserId = userId
        mSelfName = userName
        mSelfAvatar = userAvatar
        mCoverPicUrl = userAvatar
        mRoomId = getRoomId()

        mLiveRoom = TRTCLiveRoom.sharedInstance(this)
        mIsEnterRoom = false
        if (TextUtils.isEmpty(mSelfName)) {
            mSelfName = mSelfUserId
        }

        initView()
        startPreview()
    }

    protected open fun startPreview() {
        mTXCloudVideoView!!.visibility = View.VISIBLE
        PermissionHelper.requestPermission(this, PermissionHelper.PERMISSION_CAMERA,
            object : PermissionCallback(), PermissionHelper.PermissionCallback {
                override fun onGranted() {
                    mLiveRoom?.startCameraPreview(true, mTXCloudVideoView!!, null)
                }

                override fun onDenied() {}

                override fun onDialogApproved() {

                }

                override fun onDialogRefused() {
                    exitRoom()
                    finish()
                }
            })
    }

    private fun exitRoom() {
//        if (!mIsEnterRoom) {
//            return
//        }
        LiveRoomManager.instance?.destroyRoom(mRoomId, object : LiveRoomManager.ActionCallback {
            override fun onSuccess() {
                Log.d(TAG, "onSuccess: 后台销毁房间成功")
            }

            override fun onError(code: Int, message: String?) {
                Log.d(TAG, "onFailed: 后台销毁房间失败[$code")
            }
        })
        mLiveRoom?.destroyRoom(object : TRTCLiveRoomCallback.ActionCallback {
            override fun onCallback(code: Int, msg: String) {
                if (code == 0) {
                    Log.d(TAG, "IM销毁房间成功")
                } else {
                    Log.d(TAG, "IM销毁房间失败:$msg")
                }
            }
        })
        mIsEnterRoom = false
        mLiveRoom?.setDelegate(null)
    }


    private fun initView() {
        mRootView = findViewById<View>(R.id.root) as ConstraintLayout
        mEditLiveRoomName = findViewById<View>(R.id.et_live_room_name) as EditText
        mEditLiveRoomName?.isFocusableInTouchMode = !RTCubeUtils.isRTCubeApp(this)
        mPreFunctionView = findViewById(R.id.anchor_pre_function)
        mPreView = findViewById(R.id.anchor_preview)
        mFunctionView = findViewById(R.id.anchor_function_view)
        mFunctionView?.setRoomId(mRoomId.toString() + "")
        if (!TextUtils.isEmpty(mSelfName)) {
            mEditLiveRoomName!!.setText(
                getString(
                    R.string.trtcliveroom_create_room_default,
                    mSelfName
                )
            )
        }
        mToolbar = findViewById(R.id.tool_bar_view)
        mPreFunctionView?.setListener(object : AnchorPreFunctionView.OnPreFunctionClickListener {
            override fun onStartClick() {
                if (mIsCreatingRoom) {
                    return
                }
                val roomName = mEditLiveRoomName?.text.toString().trim { it <= ' ' }
                if (TextUtils.isEmpty(roomName)) {
                    ToastUtils.showLong(getText(R.string.trtcliveroom_warning_room_name_empty))
                    return
                }
                if (roomName.toByteArray().size > MAX_LEN) {
                    ToastUtils.showLong(getText(R.string.trtcliveroom_warning_room_name_too_long))
                    return
                }
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(mEditLiveRoomName?.windowToken, 0)
                mRoomName = roomName
                createRoom()
            }

            override fun onCloseClick() {
                if (!mIsEnterRoom) {
//                    finishRoom()
                } else {
//                    showExitInfoDialog(
//                        getString(R.string.trtcliveroom_warning_anchor_exit_room),
//                        false
//                    )
                }
            }
        })
        TUILogin.addLoginListener(mTUILoginListener)

        val map: MutableMap<String, Any> = java.util.HashMap()
        map[TUIConstants.TUIBeauty.PARAM_NAME_CONTEXT] = this
        map[TUIConstants.TUIBeauty.PARAM_NAME_LICENSE_URL] = XMAGIC_LICENSE_URL
        map[TUIConstants.TUIBeauty.PARAM_NAME_LICENSE_KEY] = XMAGIC_LICENSE_KEY
        TUICore.callService(
            TUIConstants.TUIBeauty.SERVICE_NAME,
            TUIConstants.TUIBeauty.METHOD_INIT_XMAGIC,
            map
        )
        mTXCloudVideoView = findViewById<View>(R.id.video_view_anchor) as TXCloudVideoView
        mTXCloudVideoView!!.setLogMargin(10f, 10f, 45f, 55f)

        mPKContainer = findViewById<View>(R.id.pk_container) as RelativeLayout
        mImagePKLayer = findViewById<View>(R.id.iv_pk_layer) as ImageView

        mImagesAnchorHead = findViewById<View>(R.id.iv_anchor_head) as ImageView
        showHeadIcon(mImagesAnchorHead, mSelfAvatar)

        mImagesAnchorHead?.setOnClickListener { showLog() }

        val videoViews: MutableList<TCVideoView> = java.util.ArrayList<TCVideoView>()
        videoViews.add(findViewById<View>(R.id.video_view_link_mic_1) as TCVideoView)
        videoViews.add(findViewById<View>(R.id.video_view_link_mic_2) as TCVideoView)
        videoViews.add(findViewById<View>(R.id.video_view_link_mic_3) as TCVideoView)
        mVideoViewMgr = TCVideoViewMgr(videoViews, object : TCVideoView.OnRoomViewListener {
            override fun onKickUser(userID: String) {
                if (userID != null) {
                    mLiveRoom!!.kickoutJoinAnchor(
                        userID,
                        object : TRTCLiveRoomCallback.ActionCallback {
                            override fun onCallback(code: Int, msg: String) {}
                        })
                }
            }
        })

        mGuideLineVertical = findViewById<View>(R.id.gl_vertical) as Guideline
        mFunctionView = findViewById(R.id.anchor_function_view)
        mFunctionView?.setListener(object : AnchorFunctionView.OnAnchorFunctionListener {
            override fun onClose() {
                if (!mIsEnterRoom) {
                    finishRoom()
                    return
                }
                if (mCurrentStatus == TRTCLiveRoomDef.ROOM_STATUS_PK) {
                    stopPK()
                    return
                }
                showExitInfoDialog(getString(R.string.trtcliveroom_warning_anchor_exit_room), false)
            }
        })
        initCountDownView()
    }

    open fun showExitInfoDialog(msg: String?, isError: Boolean) {
        val dialogFragment = ExitConfirmDialogFragment()
        dialogFragment.isCancelable = false
        dialogFragment.setMessage(msg)
        if (dialogFragment.isAdded) {
            dialogFragment.dismiss()
            return
        }
        if (isError) {
            exitRoom()
            dialogFragment.setPositiveClickListener {
                dialogFragment.dismiss()
                showPublishFinishDetailsDialog()
            }
            dialogFragment.show(fragmentManager, "ExitConfirmDialogFragment")
            return
        }
        dialogFragment.setPositiveClickListener {
            dialogFragment.dismiss()
            exitRoom()
            showPublishFinishDetailsDialog()
        }
        dialogFragment.setNegativeClickListener { dialogFragment.dismiss() }
        dialogFragment.show(fragmentManager, "ExitConfirmDialogFragment")
    }

    protected open fun showPublishFinishDetailsDialog() {
        val args = Bundle()
        args.putString(LIVE_TOTAL_TIME, TCUtils.formattedTime(mSecond))
        args.putString(ANCHOR_HEART_COUNT, String.format(Locale.CHINA, "%d", mHeartCount))
        args.putString(TOTAL_AUDIENCE_COUNT, String.format(Locale.CHINA, "%d", mTotalMemberCount))
        val dialogFragment = FinishDetailDialogFragment()
        dialogFragment.arguments = args
        dialogFragment.isCancelable = false
        if (dialogFragment.isAdded) {
            dialogFragment.dismiss()
        } else {
            dialogFragment.show(fragmentManager, "")
        }
    }

    private fun stopPK() {
        mFunctionView?.setButtonPKState(AnchorFunctionView.PKState.PK)
        mLiveRoom?.quitRoomPK(null)
    }

    private fun initCountDownView() {
        mCountDownView = findViewById(R.id.anchor_count_down)
        mCountDownView?.setListener {
            mCountDownView?.visibility = View.GONE
            startPush()
        }
    }

    private fun startPush() {
        mLiveRoom?.startPublish(
            mSelfUserId + "_stream",
            object : TRTCLiveRoomCallback.ActionCallback {
                override fun onCallback(code: Int, msg: String) {
                    if (code == 0) {
                        Log.d(TAG, "开播成功")
                        showAlertUserLiveTips()
                    } else {
                        Log.e(TAG, "开播失败$msg")
                    }
                }
            })
    }

    private fun showAlertUserLiveTips() {
        if (!isFinishing) {
            try {
                val clz = Class.forName("com.tencent.liteav.privacy.util.RTCubeAppLegalUtils")
                val method = clz.getDeclaredMethod(
                    "showAlertUserLiveTips",
                    Context::class.java
                )
                method.invoke(null, this)
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun finishRoom() {
        mLiveRoom?.stopCameraPreview()
        finish()
    }

    private fun showLog() {
        mShowLog = !mShowLog
        mLiveRoom?.showVideoDebugLog(mShowLog)
        if (mTXCloudVideoView != null) {
            mTXCloudVideoView?.showLog(mShowLog)
        }
        mVideoViewPKAnchor?.showLog(mShowLog)

        mVideoViewMgr?.showLog(mShowLog)
    }

    private fun showHeadIcon(view: ImageView?, avatar: String?) {
        TCUtils.showPicWithUrl(this, view, avatar, R.drawable.trtcliveroom_bg_cover)
    }

    private val mTUILoginListener: TUILoginListener = object : TUILoginListener() {
        override fun onKickedOffline() {
            Log.e(TAG, "onKickedOffline")
            mLiveRoom?.destroyRoom(null)
            finish()
        }
    }

    private fun createRoom() {
        mIsCreatingRoom = true
        enterRoom()
    }

    private fun enterRoom() {
        mLiveRoom?.setDelegate(this)
        val param = TRTCLiveRoomDef.TRTCCreateRoomParam()
        param.roomName = mRoomName ?: ""
        param.coverUrl = mSelfAvatar ?: ""
        mLiveRoom?.createRoom(mRoomId, param, object : TRTCLiveRoomCallback.ActionCallback {
            override fun onCallback(code: Int, msg: String) {
                if (code == 0) {
                    mIsEnterRoom = true
                    mPreFunctionView!!.visibility = View.GONE
                    mPreView!!.visibility = View.GONE
                    mFunctionView!!.visibility = View.VISIBLE
                    freshToolView()
                    startTimer()
                    onCreateRoomSuccess()
                    onTRTCRoomCreateSuccess()
                } else {
                    Log.w(TAG, String.format("创建直播间错误, code=%s,error=%s", code, msg))
                    showErrorAndQuit(
                        code,
                        getString(R.string.trtcliveroom_error_create_live_room, msg)
                    )
                }
                mIsCreatingRoom = false
            }
        })
    }

    private  fun onTRTCRoomCreateSuccess() {
        LiveRoomManager.instance?.createRoom(mRoomId, object : LiveRoomManager.ActionCallback {
            override fun onSuccess() {}
            override fun onError(errorCode: Int, message: String?) {
                if (errorCode == ERROR_ROOM_ID_EXIT) {
                    onSuccess()
                } else {
                    ToastUtils.showLong("create room failed[$errorCode]:$message")
                    finish()
                }
            }
        })
    }

    private fun freshToolView() {
        val set = ConstraintSet()
        set.clone(mRootView)
        set.connect(mToolbar!!.id, ConstraintSet.BOTTOM, R.id.fl_audio_effect, ConstraintSet.TOP)
        set.applyTo(mRootView)
    }

    private fun onCreateRoomSuccess() {
        mFunctionView?.startRecordAnimation()
        mTXCloudVideoView?.visibility = View.VISIBLE
        PermissionHelper.requestPermission(this@TCCameraAnchorActivity,
            PermissionHelper.PERMISSION_MICROPHONE,
            object : PermissionCallback(), PermissionHelper.PermissionCallback {
                override fun onGranted() {
                    mCountDownView?.visibility = View.VISIBLE
                    mCountDownView?.start()
                }

                override fun onDenied() {}
                override fun onDialogApproved() {
                }

                override fun onDialogRefused() {
                    exitRoom()
                    finish()
                }
            })
    }


    protected open fun showErrorAndQuit(errorCode: Int, errorMsg: String?) {
        mFunctionView?.stopRecordAnimation()
        if (mErrorDialog == null) {
            val builder = AlertDialog.Builder(this, R.style.TRTCLiveRoomDialogTheme)
                .setTitle(R.string.trtcliveroom_error)
                .setMessage(errorMsg)
                .setNegativeButton(
                    R.string.trtcliveroom_ok
                ) { _, _ ->
                    mErrorDialog?.dismiss()
                    stopTimer()
                    exitRoom()
                    finish()
                }
            mErrorDialog = builder.create()
        }
        if (mErrorDialog?.isShowing == true) {
            mErrorDialog?.dismiss()
        }
        if (!isFinishing) {
            mErrorDialog?.show()
        }
    }

    private fun stopTimer() {
        if (null != mBroadcastTimer) {
            mBroadcastTimerTask?.cancel()
        }
    }

    inner class BroadcastTimerTask : TimerTask() {
        override fun run() {
            ++mSecond
            runOnUiThread { mFunctionView?.onBroadcasterTimeUpdate(mSecond) }
        }
    }

    private fun startTimer() {
        if (mBroadcastTimer == null) {
            mBroadcastTimer = Timer(true)
            mBroadcastTimerTask = BroadcastTimerTask()
            mBroadcastTimer?.schedule(mBroadcastTimerTask, 1000, 1000)
        }
    }

    private fun getRoomId(): Int {
        var rooId = 0
        try {
            rooId = mSelfUserId!!.toInt()
        } catch (e: Exception) {
            TRTCLogger.e(TAG, "getRoomId failed,roomId:" + mSelfUserId + ",msg:" + e.message)
        }
        return rooId
    }


    companion object {
        private const val TAG = "TCCameraAnchorActivity"

        protected const val LIVE_TOTAL_TIME = "live_total_time"
        protected const val ANCHOR_HEART_COUNT = "anchor_heart_count"
        protected const val TOTAL_AUDIENCE_COUNT = "total_audience_count"
        const val ERROR_ROOM_ID_EXIT = -1301
        private const val MAX_LEN = 30


    }

    override fun onError(code: Int, message: String) {

    }

    override fun onWarning(code: Int, message: String) {

    }

    override fun onDebugLog(message: String) {

    }

    override fun onRoomInfoChange(roomInfo: TRTCLiveRoomDef.TRTCLiveRoomInfo) {

    }

    override fun onRoomDestroy(roomId: String) {
        Log.e(TAG, "onRoomDestroy")
        mLiveRoom?.destroyRoom(null)
        if (!isFinishing) {
            showDestroyDialog()
        }
    }

    private fun showDestroyDialog() {
        try {
            val clz = Class.forName("com.tencent.liteav.privacy.util.RTCubeAppLegalUtils")
            val method = clz.getDeclaredMethod("showRoomDestroyTips", Context::class.java)
            method.invoke(null, this)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    override fun onAnchorEnter(userId: String) {

    }

    override fun onAnchorExit(userId: String) {

    }

    override fun onAudienceEnter(userInfo: TRTCLiveRoomDef.TRTCLiveUserInfo) {
        mTotalMemberCount++
    }

    override fun onAudienceExit(userInfo: TRTCLiveRoomDef.TRTCLiveUserInfo) {
        mMainHandler.post {
//            val fragment: ConfirmDialogFragment =
//                mLinkMicConfirmDialogFragmentMap.remove(userInfo.userId)
//            if (null != fragment && fragment.isAdded()) {
//                fragment.dismissAllowingStateLoss()
//            }
        }
    }

    override fun onUserVideoAvailable(userId: String, available: Boolean) {

    }

    override fun onRequestJoinAnchor(
        userInfo: TRTCLiveRoomDef.TRTCLiveUserInfo,
        reason: String,
        timeOut: Int
    ) {

    }

    override fun onCancelJoinAnchor() {

    }

    override fun onKickoutJoinAnchor() {

    }

    override fun onRequestRoomPK(userInfo: TRTCLiveRoomDef.TRTCLiveUserInfo, timeout: Int) {

    }

    override fun onCancelRoomPK() {

    }

    override fun onQuitRoomPK() {

    }

    override fun onRecvRoomTextMsg(message: String, userInfo: TRTCLiveRoomDef.TRTCLiveUserInfo) {

    }

    override fun onRecvRoomCustomMsg(
        cmd: String,
        message: String,
        userInfo: TRTCLiveRoomDef.TRTCLiveUserInfo
    ) {

    }

    override fun onAudienceRequestJoinAnchorTimeout(userId: String) {

    }

    override fun onAnchorRequestRoomPKTimeout(userId: String) {

    }
}