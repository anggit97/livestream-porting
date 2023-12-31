package com.example.tuilivestream;


import com.example.tuilivestream.utils.TCVideoView;

import java.util.List;

public class TCVideoViewMgr {
    private List<TCVideoView> mVideoViews;
    private TCVideoView       mPKVideoView;

    public TCVideoViewMgr(List<TCVideoView> videoViewList, final TCVideoView.OnRoomViewListener l) {
        mVideoViews = videoViewList;
        for (TCVideoView videoView : mVideoViews) {
            videoView.setOnRoomViewListener(l);
        }
    }

    public synchronized void clearPKView() {
        mPKVideoView = null;
    }

    public synchronized TCVideoView getPKUserView() {
        if (mPKVideoView != null) {
            return mPKVideoView;
        }
        boolean foundUsed = false;
        for (TCVideoView item : mVideoViews) {
            if (item.isUsedData()) {
                foundUsed = true;
                mPKVideoView = item;
                break;
            }
        }
        if (!foundUsed) {
            mPKVideoView = mVideoViews.get(0);
        }
        return mPKVideoView;
    }

    public synchronized boolean containUserId(String id) {
        for (TCVideoView item : mVideoViews) {
            if (item.isUsedData() && item.getUserId().equals(id)) {
                return true;
            }
        }
        return false;
    }

    public synchronized TCVideoView applyVideoView(String id) {
        if (id == null) {
            return null;
        }

        if (mPKVideoView != null) {
            mPKVideoView.setUsed(true);
            mPKVideoView.showKickoutBtn(false);
            mPKVideoView.setUserId(id);
            return mPKVideoView;
        }

        for (TCVideoView item : mVideoViews) {
            if (!item.isUsedData()) {
                item.setUsed(true);
                item.setUserId(id);
                return item;
            } else {
                if (item.getUserId() != null && item.getUserId().equals(id)) {
                    item.setUsed(true);
                    return item;
                }
            }
        }
        return null;
    }

    public synchronized void recycleVideoView(String id) {
        for (TCVideoView item : mVideoViews) {
            if (item.getUserId() != null && item.getUserId().equals(id)) {
                item.setUserId(null);
                item.setUsed(false);
            }
        }
    }

    public synchronized void recycleVideoView() {
        for (TCVideoView item : mVideoViews) {
            item.setUserId(null);
            item.setUsed(false);
        }
    }

    public synchronized void showLog(boolean show) {
        for (TCVideoView item : mVideoViews) {
            if (item.isUsedData()) {
                item.showLog(show);
            }
        }
    }
}
