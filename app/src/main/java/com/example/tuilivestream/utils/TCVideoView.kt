package com.example.tuilivestream.utils

import android.content.Context
import android.graphics.drawable.AnimationDrawable
import android.util.AttributeSet
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RelativeLayout
import com.something.plugin.R
import com.tencent.rtmp.ui.TXCloudVideoView

/**
 * Created by Anggit Prayogo on 19/10/23.
 */
class TCVideoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : RelativeLayout(context, attrs, defStyle) {

    private val mPlayerVideo: TXCloudVideoView
    private val mImageLoading: ImageView
    private val mLayoutBackgroundLoading: FrameLayout
    private val mButtonKickOut: Button
    private var mOnRoomViewListener: OnRoomViewListener? = null

    var userId: String? = null
    var isUsedData = false

    init {
        inflate(context, R.layout.trtcliveroom_view_tc_video, this)
        mPlayerVideo = findViewById(R.id.video_player)
        mImageLoading = findViewById(R.id.loading_imageview)
        mLayoutBackgroundLoading = findViewById(R.id.loading_background)
        mButtonKickOut = findViewById(R.id.btn_kick_out)

        mButtonKickOut.setOnClickListener {
            mButtonKickOut.visibility = View.INVISIBLE
            val userId = this.userId
            if (userId != null && mOnRoomViewListener != null) {
                mOnRoomViewListener?.onKickUser(userId)
            }
        }
    }

    fun getPlayerVideo(): TXCloudVideoView {
        return mPlayerVideo
    }

    fun setOnRoomViewListener(onRoomViewListener: OnRoomViewListener?) {
        mOnRoomViewListener = onRoomViewListener
    }

    fun showLog(show: Boolean) {
        mPlayerVideo.showLog(show)
    }

    fun showKickoutBtn(show: Boolean) {
        mButtonKickOut.visibility = if (show) View.VISIBLE else View.INVISIBLE
    }

    interface OnRoomViewListener {
        fun onKickUser(userId: String)
    }

    fun startLoading() {
        mButtonKickOut.visibility = View.INVISIBLE
        mLayoutBackgroundLoading.visibility = View.VISIBLE
        mImageLoading.visibility = View.VISIBLE
        mImageLoading.setImageResource(R.drawable.trtcliveroom_linkmic_loading)
        val ad = mImageLoading.drawable as? AnimationDrawable
        ad?.start()
    }

    fun stopLoading(showKickoutBtn: Boolean) {
        mButtonKickOut.visibility = if (showKickoutBtn) View.VISIBLE else View.GONE
        mLayoutBackgroundLoading.visibility = View.GONE
        mImageLoading.visibility = View.GONE
        val ad = mImageLoading.drawable as? AnimationDrawable
        ad?.stop()
    }

    fun stopLoading() {
        stopLoading(false)
    }

    fun setUsed(used: Boolean) {
        mPlayerVideo.visibility = if (used) View.VISIBLE else View.GONE
        visibility = if (used) View.VISIBLE else View.GONE
        if (!used) {
            stopLoading(false)
        }
        isUsedData = used
    }
}