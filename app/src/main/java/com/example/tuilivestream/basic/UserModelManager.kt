package com.example.tuilivestream.basic
import android.util.Log
import com.blankj.utilcode.util.GsonUtils
import com.blankj.utilcode.util.SPUtils
import java.text.SimpleDateFormat
import java.util.Date

/**
 * Created by Anggit Prayogo on 19/10/23.
 */
class UserModelManager private constructor() {
    private val TAG = "UserModelManager"
    private val PER_DATA = "per_profile_manager"
    private val PER_USER_MODEL = "per_user_model"
    private val PER_USER_DATE = "per_user_publish_video_date"
    private val USAGE_MODEL = "usage_model"
    private val HAVE_BACKSTAGE = "have_backstage"
    private var mUserModel: UserModel? = null
    private var mUserPublishVideoDate: String? = null
    private var mHaveBackstage: Boolean? = null

    companion object {
        private var sInstance: UserModelManager? = null

        @Synchronized
        fun getInstance(): UserModelManager {
            if (sInstance == null) {
                sInstance = UserModelManager()
            }
            return sInstance!!
        }
    }

    fun getUserModel(): UserModel {
        if (mUserModel == null) {
            loadUserModel()
        }
        return mUserModel ?: UserModel()
    }

    fun setUserModel(model: UserModel) {
        mUserModel = model
        try {
            SPUtils.getInstance(PER_DATA).put(PER_USER_MODEL, GsonUtils.toJson(mUserModel))
        } catch (e: Exception) {
            Log.d(TAG, "")
        }
    }

    private fun loadUserModel() {
        try {
            val json = SPUtils.getInstance(PER_DATA).getString(PER_USER_MODEL)
            mUserModel = GsonUtils.fromJson(json, UserModel::class.java)
        } catch (e: Exception) {
            Log.d(TAG, "loadUserModel failed:${e.message}")
        }
    }

    fun clearUserModel() {
        try {
            SPUtils.getInstance(PER_DATA).put(PER_USER_MODEL, "")
        } catch (e: Exception) {
            Log.d(TAG, "clea user model error:${e.message}")
        }
    }

    private fun getUserPublishVideoDate(): String {
        if (mUserPublishVideoDate == null) {
            mUserPublishVideoDate = SPUtils.getInstance(PER_DATA).getString(PER_USER_DATE, "")
        }
        return mUserPublishVideoDate ?: ""
    }

    private fun setUserPublishVideoDate(date: String) {
        mUserPublishVideoDate = date
        try {
            SPUtils.getInstance(PER_DATA).put(PER_USER_DATE, mUserPublishVideoDate)
        } catch (e: Exception) {
            Log.d(TAG, "setUserPublishVideoDate failed:${e.message}")
        }
    }

    fun setUsageModel(haveBackstage: Boolean) {
        mHaveBackstage = haveBackstage
        try {
            SPUtils.getInstance(USAGE_MODEL).put(HAVE_BACKSTAGE, mHaveBackstage!!)
        } catch (e: Exception) {
            Log.d(TAG, "setUsageModel failed")
        }
    }

    fun haveBackstage(): Boolean {
        if (mHaveBackstage == null) {
            mHaveBackstage = SPUtils.getInstance(USAGE_MODEL).getBoolean(HAVE_BACKSTAGE, true)
        }
        return mHaveBackstage ?: true
    }

    fun needShowSecurityTips(): Boolean {
        val profileDate = getUserPublishVideoDate()
        val date = Date()
        val formatter = SimpleDateFormat("dd")
        val day = formatter.format(date)
        if (day != profileDate) {
            setUserPublishVideoDate(day)
            return true
        }
        return false
    }
}
