package com.example.tuilivestream.utils;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.content.Context;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.blankj.utilcode.util.CollectionUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.example.tuilivestream.basic.LiveRoomManager;
import com.example.tuilivestream.basic.TRTCLiveRoom;
import com.example.tuilivestream.basic.TRTCLiveRoomCallback;
import com.example.tuilivestream.basic.TRTCLiveRoomDef;
import com.example.tuilivestream.widgets.SpaceDecoration;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.something.plugin.R;
import com.tencent.qcloud.tuicore.util.ToastUtil;

import java.util.ArrayList;
import java.util.List;

public class AnchorPKSelectView extends BottomSheetDialog {
    private Context                                mContext;
    private RecyclerView                           mPusherListRv;
    private List<TRTCLiveRoomDef.TRTCLiveRoomInfo> mRoomInfos;
    private RoomListAdapter                        mRoomListAdapter;
    private OnPKSelectedCallback                   mOnPKSelectedCallback;
    private ConstraintLayout                       mLayoutInvitePkById;
    private TextView                               mPusherTagTv;
    private TextView                               mTextCancel;
    private int                                    mSelfRoomId;
    private EditText                               mEditRoomId;
    private TextView                               mTextEnterRoom;

    private final TextWatcher mEditTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            mTextEnterRoom.setEnabled(!TextUtils.isEmpty(mEditRoomId.getText().toString()));
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    };

    public AnchorPKSelectView(Context context) {
        super(context, R.style.TRTCLiveRoomInputDialog);
        initView(context);
    }


    private void initView(Context context) {
        mContext = context;
        setContentView(R.layout.trtcliveroom_view_pk_select);
        mPusherListRv = (RecyclerView) findViewById(R.id.rv_anchor_list);
        mLayoutInvitePkById = findViewById(R.id.layout_invite_pk_by_id);
        mEditRoomId = findViewById(R.id.et_input_room_id);
        mTextEnterRoom = findViewById(R.id.tv_enter_room_by_id);
        mEditRoomId.addTextChangedListener(mEditTextWatcher);
        mRoomInfos = new ArrayList<>();
        mRoomListAdapter = new RoomListAdapter(mContext, mRoomInfos, new RoomListAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                if (mRoomInfos == null || mOnPKSelectedCallback == null) {
                    return;
                }
                mOnPKSelectedCallback.onSelected(mRoomInfos.get(position));
            }
        });
        mPusherListRv.setLayoutManager(new LinearLayoutManager(mContext));
        mPusherListRv.addItemDecoration(new SpaceDecoration(getContext().getResources()
                .getDimensionPixelOffset(R.dimen.trtcliveroom_space_select_pk_rv), 1));
        mPusherListRv.setAdapter(mRoomListAdapter);

        mPusherTagTv = (TextView) findViewById(R.id.tv_pusher_tag);

        mTextCancel = (TextView) findViewById(R.id.tv_cancel);
        mTextCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
                mOnPKSelectedCallback.onCancel();
            }
        });
        if (LiveRoomManager.Companion.getInstance().isAddCallBack()) {
            mPusherListRv.setVisibility(VISIBLE);
            mLayoutInvitePkById.setVisibility(GONE);
        } else {
            mPusherTagTv.setText(getContext().getResources().getString(R.string.trtcliveroom_title_pk_request));
            mPusherListRv.setVisibility(GONE);
            mLayoutInvitePkById.setVisibility(VISIBLE);
        }
        mTextEnterRoom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<Integer> list = new ArrayList<>();
                list.add(Integer.valueOf(mEditRoomId.getText().toString().trim()));
                TRTCLiveRoom.Companion.sharedInstance(mContext).getRoomInfos(list, new TRTCLiveRoomCallback.RoomInfoCallback() {
                    @Override
                    public void onCallback(int code, String msg, List<TRTCLiveRoomDef.TRTCLiveRoomInfo> list) {
                        if (code == 0) {
                            if (list == null || list.size() == 0) {
                                ToastUtil.toastShortMessage(msg);
                            } else if (list.get(0).getRoomId() == mSelfRoomId || TextUtils.isEmpty(list.get(0).getOwnerId())) {
                                ToastUtil.toastShortMessage(msg);
                            } else {
                                mOnPKSelectedCallback.onSelected(list.get(0));
                            }
                        } else {
                            ToastUtil.toastShortMessage(msg);
                        }
                    }
                });
            }
        });
    }

    public void setSelfRoomId(int roomId) {
        mSelfRoomId = roomId;
    }

    public void setOnPKSelectedCallback(OnPKSelectedCallback onPKSelectedCallback) {
        mOnPKSelectedCallback = onPKSelectedCallback;
    }

    public void refreshView() {
        LiveRoomManager.Companion.getInstance().getRoomIdList(new LiveRoomManager.GetCallback() {
            @Override
            public void onSuccess(List<Integer> list) {
                TRTCLiveRoom.Companion.sharedInstance(mContext).getRoomInfos(list, new TRTCLiveRoomCallback.RoomInfoCallback() {
                    @Override
                    public void onCallback(int code, String msg, List<TRTCLiveRoomDef.TRTCLiveRoomInfo> list) {
                        if (code == 0) {
                            mRoomInfos.clear();
                            for (TRTCLiveRoomDef.TRTCLiveRoomInfo info : list) {
                                if (info.getRoomId() == mSelfRoomId || TextUtils.isEmpty(info.getOwnerId())) {
                                    continue;
                                }
                                mRoomInfos.add(info);
                            }
                            if (CollectionUtils.isEmpty(mRoomInfos)) {
                                mPusherTagTv.setText(getContext().getResources()
                                        .getString(R.string.trtcliveroom_no_anchor_for_pk));
                            } else {
                                mPusherTagTv.setText(getContext().getResources()
                                        .getString(R.string.trtcliveroom_title_pk_request));
                            }
                            mRoomListAdapter.notifyDataSetChanged();
                        }
                    }
                });
            }

            @Override
            public void onError(int errorCode, String message) {
                ToastUtils.showShort(getContext().getResources().getString(R.string.trtcliveroom_get_anchor_list_fail));
            }
        });
    }

    @Override
    public void show() {
        super.show();
        refreshView();
    }

    public interface OnPKSelectedCallback {
        void onSelected(TRTCLiveRoomDef.TRTCLiveRoomInfo roomInfo);

        void onCancel();
    }


    public static class RoomListAdapter extends
            RecyclerView.Adapter<RoomListAdapter.ViewHolder> {

        private static final String TAG = RoomListAdapter.class.getSimpleName();

        private Context                                context;
        private List<TRTCLiveRoomDef.TRTCLiveRoomInfo> list;
        private OnItemClickListener                    onItemClickListener;

        public RoomListAdapter(Context context, List<TRTCLiveRoomDef.TRTCLiveRoomInfo> list,
                               OnItemClickListener onItemClickListener) {
            this.context = context;
            this.list = list;
            this.onItemClickListener = onItemClickListener;
        }


        static class ViewHolder extends RecyclerView.ViewHolder {
            private TextView  mUserNameTv;
            private TextView  mRoomNameTv;
            private Button    mButtonInvite;
            private ImageView mImageAvatar;

            ViewHolder(View itemView) {
                super(itemView);
                initView(itemView);
            }

            private void initView(@NonNull final View itemView) {
                mUserNameTv = (TextView) itemView.findViewById(R.id.tv_user_name);
                mRoomNameTv = (TextView) itemView.findViewById(R.id.tv_room_name);
                mButtonInvite = (Button) itemView.findViewById(R.id.btn_invite_anchor);
                mImageAvatar = (ImageView) itemView.findViewById(R.id.iv_avatar);
            }

            public void bind(Context context, final TRTCLiveRoomDef.TRTCLiveRoomInfo model,
                             final OnItemClickListener listener) {
                if (model == null) {
                    return;
                }
                if (!TextUtils.isEmpty(model.getRoomName())) {
                    mRoomNameTv.setText(model.getRoomName());
                }
                if (TextUtils.isEmpty(model.getOwnerName())) {
                    mUserNameTv.setText(model.getOwnerId());
                } else {
                    mUserNameTv.setText(model.getOwnerName());
                }

                ImageLoader.loadImage(context, mImageAvatar, model.getCoverUrl(), R.drawable.trtcliveroom_bg_cover);
                mButtonInvite.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        listener.onItemClick(getLayoutPosition());
                    }
                });
            }
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            Context context = parent.getContext();
            LayoutInflater inflater = LayoutInflater.from(context);

            View view = inflater.inflate(R.layout.trtcliveroom_item_select_pusher, parent, false);

            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            TRTCLiveRoomDef.TRTCLiveRoomInfo item = list.get(position);
            holder.bind(context, item, onItemClickListener);
        }


        @Override
        public int getItemCount() {
            return list.size();
        }

        public interface OnItemClickListener {
            void onItemClick(int position);
        }
    }
}
