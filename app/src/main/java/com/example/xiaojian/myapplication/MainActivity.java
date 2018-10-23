package com.example.xiaojian.myapplication;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.hik.mcrsdk.MCRSDK;
import com.hik.mcrsdk.rtsp.RtspClient;
import com.hik.mcrsdk.talk.TalkClientSDK;
import com.hikvision.sdk.VMSNetSDK;
import com.hikvision.sdk.consts.SDKConstant;
import com.hikvision.sdk.net.business.OnVMSNetSDKBusiness;
import com.hikvision.sdk.utils.FileUtils;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivityLog";
    private static final Integer SHOW_LOADING = 0;
    private static final Integer CLOSE_LOADING = 1;
    private static final Integer PREPARE = 0;
    private static final Integer PLAYING = 1;
    private static final Integer PAUSE = 2;
    public static final Integer PLAY_WINDOW_NO = 1;


    private String mHostAddress = "https://192.168.1.107";
    private String mAccount = "admin";
    private String mPassword = "anasit123456789+";
    private String mMacAddress = "02:00:00:00:00:00";

    // 播放视图
    private SurfaceView mPlayView;
    // 播放状态 0-准备中，1-播放中， 2-暂停中
    private Integer mPlayStatus = PREPARE;
    // 视频恢复播放加载进度视图
    private View mRecoverBar;
    // 视频暂停背景图视图
    private ImageView mPauseImageView;
    // 播放面板视图
    private View mPlayContainerView;
    // 监控点列表视图
    private RecyclerView mCameraListView;
    // 播放控件面板视图
    private View mPlayControlView;
    // 播放头部面板视图
    private View mPlayHeadView;
    // 加载视图
    private View mLoadingView;
    // 工具栏展开/关闭按钮
    private ImageView mPopWindowBtn;
    // 播放/暂停按钮
    private ImageView mPlayBtn;
    // 工具栏面板
    private View mToolPadView;
    // 工具栏状态
    private Boolean mToolPadActive = false;
    // 摄像头列表
    private List<CameraData> mCameras = new ArrayList<>();
    // 当前积极状态的摄像头
    private CameraData mActiveCamera;
    // 动画句柄消息计数
    private Integer mDisappearHandlerCx = 0;
    // 动画消息句柄
    private Handler mDisappearHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            if (msg.what != mDisappearHandlerCx) {
                return false;
            }
            // 消失动画对象
            AlphaAnimation disappearAnimation = new AlphaAnimation(1, 0);
            disappearAnimation.setDuration(1000);

            // 配置消失监听
            disappearAnimation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    mPlayControlView.setVisibility(View.INVISIBLE);
                    mPlayHeadView.setVisibility(View.INVISIBLE);
                }
            });

            // 开始消失动画
            mPlayControlView.startAnimation(disappearAnimation);
            mPlayHeadView.startAnimation(disappearAnimation);
            return false;
        }
    });
    // 加载动画句柄
    private Handler mLoadingHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            if (msg.what == SHOW_LOADING) {
                showVideoLoading();
            } else {
                closeVideoLoading();
            }
            return false;
        }
    });
    // 播放消息句柄
    private Handler mPlayHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            if (mActiveCamera == null) {
                Toast.makeText(getApplicationContext(), "当前没有可用的摄像头，无法播放视频"
                        , Toast.LENGTH_LONG).show();
            } else {
                PlayTask task = new PlayTask();
                task.execute(mActiveCamera.getCameraSns());
            }
            return false;
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mPlayView = findViewById(R.id.play_view);
        findView();
        initEvent();
        if (savedInstanceState == null) {
            mCameras.add(new CameraData("7249206d05e4414bbeffd74e714a407a", "收银台", "https://picsum.photos/600/400?100", true));
            mCameras.add(new CameraData("fcdd5bcafd1c4f4cbefd6d542b33f213", "大门口", "https://picsum.photos/600/400?200", true));
            mCameras.add(new CameraData("00005", "杂物间", "https://picsum.photos/600/360?500", false));
            initData();
            initView();
            initSDK();
        } else {
            String jsonString = savedInstanceState.getString(TAG);
            String cameraSn = savedInstanceState.getString(TAG + "ACTIVE", "NONE");
            mCameras = new CameraData().getCardListFromJsonString(jsonString);
            if(!cameraSn.equals("NONE")){
                mActiveCamera = new CameraData(cameraSn);
            }
            initData();
            initView();
            mPlayHandler.sendEmptyMessage(0);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        Log.d(TAG, "SAVE_DATA");
        super.onSaveInstanceState(outState);
        outState.putSerializable(TAG, new CameraData().cardListToJsonString(mCameras));
        if (mActiveCamera != null) {
            outState.putSerializable(TAG + "ACTIVE", mActiveCamera.getCameraSns());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        VMSNetSDK.getInstance().stopLiveOpt(PLAY_WINDOW_NO);
    }

    /**
     * 找到所有组件
     */
    private void findView() {
        mCameraListView = findViewById(R.id.camera_list);
        mPlayContainerView = findViewById(R.id.play_container);
        mPlayControlView = findViewById(R.id.play_control);
        mPlayHeadView = findViewById(R.id.play_head);
        mPopWindowBtn = findViewById(R.id.btn_popwindow);
        mToolPadView = findViewById(R.id.tool_pad);
        mLoadingView = findViewById(R.id.play_loading);
        mPlayBtn = findViewById(R.id.btn_play);
        mRecoverBar = findViewById(R.id.recover_loading_bar);
        mPlayHeadView = findViewById(R.id.play_head);
        mPauseImageView = findViewById(R.id.recover_holder_image);
    }

    /**
     * 初始化视图
     */
    private void initView() {

        // 填充列表
        GridLayoutManager layoutManager = new GridLayoutManager(getBaseContext(), 2);
        CardAdapter adapter = new CardAdapter();
        mCameraListView.setLayoutManager(layoutManager);
        mCameraListView.setAdapter(adapter);

        // 根据横屏竖屏调整样式
        Configuration mConfiguration = getResources().getConfiguration();
        ViewGroup.LayoutParams layoutParams = mPlayContainerView.getLayoutParams();
        if (mConfiguration.orientation == mConfiguration.ORIENTATION_LANDSCAPE) {
            layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
            // 收起状态栏
            this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }

    /**
     * 初始化事件
     */
    private void initEvent() {

        // 播放控件显示/隐藏
        mPlayContainerView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    mPlayControlView.setVisibility(View.VISIBLE);
                    mPlayHeadView.setVisibility(View.VISIBLE);
                }
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    // 4秒后开始消失动画
                    mDisappearHandler.sendEmptyMessageDelayed(++mDisappearHandlerCx, 5000);
                }
                return false;
            }
        });

        // 工具弹出与隐藏
        mPopWindowBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 收起/展开动画
                ValueAnimator animator = mToolPadActive ? ValueAnimator.ofInt(220, 0) : ValueAnimator.ofInt(0, 220);
                // 旋转动画
                RotateAnimation animation = new RotateAnimation(0, 180,
                        Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
                        0.5f);
                animation.setDuration(500);
                animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

                    @Override
                    public void onAnimationUpdate(ValueAnimator animator) {
                        int value = (int) animator.getAnimatedValue();
                        ViewGroup.LayoutParams layoutParams = mToolPadView.getLayoutParams();
                        layoutParams.height = value;
                        mToolPadView.setLayoutParams(layoutParams);
                    }
                });
                animation.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        Drawable icon = getResources().getDrawable(mToolPadActive ?
                                R.drawable.ic_reduce : R.drawable.ic_plus);
                        mPopWindowBtn.setImageDrawable(icon);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
                animator.start();
                mPopWindowBtn.startAnimation(animation);
                mToolPadActive = !mToolPadActive;
            }
        });

        // 开启|暂停播放
        mPlayBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // 播放器还没就绪，点击操作不生效
                if (mPlayStatus.equals(PREPARE)) {
                    Toast.makeText(getApplicationContext()
                            , "正在加载摄像头数据，请稍后", Toast.LENGTH_SHORT).show();
                    return;
                }

                // 旋转动画
                RotateAnimation animation = new RotateAnimation(0, 180,
                        Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
                        0.5f);
                animation.setDuration(500);
                animation.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        Drawable icon = getResources().getDrawable(mPlayStatus.equals(PAUSE) ?
                                R.drawable.ic_start : R.drawable.ic_pause);
                        mPlayBtn.setImageDrawable(icon);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });

                if (mPlayStatus.equals(PLAYING)) {
                    String overPicPath = getCapture("bootstrap.jpg");
                    Boolean result = VMSNetSDK.getInstance().stopLiveOpt(PLAY_WINDOW_NO);

                    if (result == Boolean.TRUE) {
                        mPlayStatus = PAUSE;
                        mPauseImageView.setImageDrawable(Drawable.createFromPath(overPicPath));
                        mPauseImageView.setVisibility(View.VISIBLE);
                    }

                } else {
                    mPlayStatus = PREPARE;
                    mRecoverBar.setVisibility(View.VISIBLE);
                    mPlayHandler.sendEmptyMessage(0);
                }

                mPlayBtn.startAnimation(animation);
            }
        });

        // 显示对讲菜单
        findViewById(R.id.menu_talk).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new TalkPopupWindow(MainActivity.this)
                        .showAtLocation(findViewById(R.id.main_view),
                                Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);
            }
        });

        // 显示控制菜单
        findViewById(R.id.menu_control).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new ControlPopupWindow(MainActivity.this)
                        .showAtLocation(findViewById(R.id.main_view),
                                Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);
            }
        });

        // 显示截图弹窗
        findViewById(R.id.menu_cut).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String picturePath = getCapture(System.currentTimeMillis() + ".jpg");
                new CutPopupWindow(MainActivity.this, picturePath)
                        .showAtLocation(findViewById(R.id.main_view),
                                Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 0);
            }
        });

        // 显示全屏播放
        findViewById(R.id.btn_fullscreen).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Configuration mConfiguration = getResources().getConfiguration();
                if (mConfiguration.orientation == mConfiguration.ORIENTATION_LANDSCAPE) {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                } else {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                }
            }
        });
    }

    /**
     * 初始化数据
     */
    private void initData() {
        if (mActiveCamera != null){
            Log.d(TAG,"恢复之前选中的摄像头");
            for (CameraData cameraData : mCameras) {
                if (cameraData.getCameraSns().equals(mActiveCamera.getCameraSns())) {
                    mActiveCamera = cameraData;
                    break;
                }
            }
        }else{
            Log.d(TAG,"默认选中第一个处于积极状态的摄像头作为播放摄像头");
            for (CameraData cameraData : mCameras) {
                if (cameraData.getOnline()) {
                    mActiveCamera = cameraData;
                    break;
                }
            }
        }
    }

    /**
     * 显示视频加载样式
     */
    private void showVideoLoading() {
        mLoadingView.setVisibility(View.VISIBLE);
    }

    /**
     * 关闭视频加载样式
     */
    private void closeVideoLoading() {
        mLoadingView.setVisibility(View.INVISIBLE);
        mPlayHeadView.setVisibility(View.INVISIBLE);
        mRecoverBar.setVisibility(View.INVISIBLE);
        mPauseImageView.setVisibility(View.INVISIBLE);
    }

    /**
     * 初始化海康SDK
     */
    private void initSDK() {
        // 初始化SDK
        MCRSDK.init();
        // 初始视频流客户端
        RtspClient.initLib();
        MCRSDK.setPrint(1, null);
        // 初始化语音对讲
        TalkClientSDK.initLib();
        // SDK初始化
        VMSNetSDK.init(getApplication());
        // 登入操作
        Log.d(TAG, "开始登入");
        VMSNetSDK.getInstance().Login(mHostAddress,
                mAccount, mPassword, mMacAddress, new OnVMSNetSDKBusiness() {

                    @Override
                    public void onSuccess(Object o) {
                        Log.d(TAG, "登入成功");
                        // 尝试播放视频
                        mPlayHandler.sendEmptyMessage(0);
                    }

                    @Override
                    public void onFailure() {
                        Log.d(TAG, "登入失败");
                    }
                });
    }

    /**
     * 获取当前直播快照
     */
    private String getCapture(String fileName) {


        // 图标保存地址
        String savePath = FileUtils.getPictureDirPath().getAbsolutePath();
        String filePath = savePath + "/" + fileName;

        Log.d(TAG, "图片保存地址:" + filePath);

        Integer opt = VMSNetSDK.getInstance().captureLiveOpt(PLAY_WINDOW_NO, savePath, fileName);

        switch (opt) {
            case SDKConstant.LiveSDKConstant.SD_CARD_UN_USABLE:
                Toast.makeText(getApplicationContext(), "没有可用的存储卡", Toast.LENGTH_SHORT)
                        .show();
                break;
            case SDKConstant.LiveSDKConstant.SD_CARD_SIZE_NOT_ENOUGH:
                Toast.makeText(getApplicationContext(), "存储空间不足", Toast.LENGTH_SHORT)
                        .show();
                break;
            case SDKConstant.LiveSDKConstant.CAPTURE_FAILED:
                Toast.makeText(getApplicationContext(), "图片快照生成失败", Toast.LENGTH_SHORT)
                        .show();
                break;
            case SDKConstant.LiveSDKConstant.CAPTURE_SUCCESS:
                Toast.makeText(getApplicationContext(), "图片快照生成成功", Toast.LENGTH_SHORT)
                        .show();
                break;
        }
        return filePath;
    }

    protected class PlayTask extends AsyncTask<String, Void, Boolean> {

        @Override
        protected Boolean doInBackground(String... cameraSns) {
            // 清晰度（码率）
            int sysCode = SDKConstant.LiveSDKConstant.SUB_STREAM;
            // 设备号
            String camSn = cameraSns[0];
            // 播放任务接收器
            OnVMSNetSDKBusiness business = new OnVMSNetSDKBusiness() {
                @Override
                public void onFailure() {
                    Log.d(TAG, "获取视频流失败");
                    Toast.makeText(getApplicationContext(), "播放失败，无法获取摄像头的数据",
                            Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onSuccess(Object obj) {
                    Log.d(TAG, "获取视频流成功");
                    mLoadingHandler.sendEmptyMessage(CLOSE_LOADING);
                    mPlayStatus = PLAYING;
                    Drawable icon = getResources().getDrawable(mPlayStatus.equals(PAUSE) ?
                            R.drawable.ic_start : R.drawable.ic_pause);
                    mPlayBtn.setImageDrawable(icon);
                }
            };
            // 开始直播
            VMSNetSDK.getInstance().startLiveOpt(PLAY_WINDOW_NO, camSn, mPlayView, sysCode, business);
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            Log.d(TAG, "任务执行结束");
        }
    }

    /**
     * 卡片适配器（用于把摄像机数据填充到卡片中）
     */
    protected class CardAdapter extends RecyclerView.Adapter<CardAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            View view = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.camera_card, viewGroup, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) viewHolder.cardView.getLayoutParams();
            if (i % 2 > 0) {
                params.leftMargin = 8;
            } else {
                params.rightMargin = 8;
            }
            CameraData cameraData = mCameras.get(i);
            viewHolder.cameraData = cameraData;
            if (cameraData.getOnline()) {
                Glide.with(MainActivity.this)
                        .load(cameraData.getSnapshotUrl())
                        .into(viewHolder.cardImageView);
                viewHolder.cardStatusView.setVisibility(View.VISIBLE);
            } else {
                Glide.with(MainActivity.this)
                        .load(R.drawable.lost)
                        .into(viewHolder.cardImageView);
                viewHolder.cardStatusView.setVisibility(View.INVISIBLE);
            }
            viewHolder.cardTitleView.setText(cameraData.getCameraTitle());
        }

        @Override
        public int getItemCount() {
            return mCameras.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

            CameraData cameraData;
            CardView cardView;
            ImageView cardImageView;
            TextView cardTitleView;
            TextView cardStatusView;

            ViewHolder(View view) {
                super(view);
                cardView = (CardView) view;
                cardImageView = view.findViewById(R.id.camera_image);
                cardTitleView = view.findViewById(R.id.camera_title);
                cardStatusView = view.findViewById(R.id.camera_status);
                cardView.setOnClickListener(this);
            }

            @Override
            public void onClick(View v) {
                if (cameraData.getOnline()) {
                    mActiveCamera = cameraData;
                    closeVideoLoading();
                    // 之前还在准备摄像头
                    if (mPlayStatus.equals(PREPARE)) {
                        showVideoLoading();
                    }
                    // 之前正在播放视频
                    else if (mPlayStatus.equals(PLAYING)) {
                        showVideoLoading();
                        VMSNetSDK.getInstance().stopLiveOpt(PLAY_WINDOW_NO);
                    }
                    // 之前暂停了摄像头
                    else {
                        showVideoLoading();
                    }
                    mPlayHandler.sendEmptyMessage(0);
                } else {
                    Toast.makeText(getApplicationContext(),
                            "当前摄像头暂不可用", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}
