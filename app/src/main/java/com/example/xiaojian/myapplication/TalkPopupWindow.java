package com.example.xiaojian.myapplication;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import android.widget.Toast;

import com.hikvision.sdk.VMSNetSDK;
import com.hikvision.sdk.net.business.OnVMSNetSDKBusiness;

public class TalkPopupWindow extends PopupWindow implements View.OnTouchListener {

    // 窗口视图对象
    private View mPopupView;

    // 父Activity对象
    private Activity mParentActivity;

    // 对讲任务回调
    private OnVMSNetSDKBusiness mBusiness = new OnVMSNetSDKBusiness() {
        @Override
        public void onFailure() {

        }

        @Override
        public void onSuccess(Object o) {
        }
    };

    public TalkPopupWindow(Activity context) {
        super(context);
        mParentActivity =context;
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mPopupView = inflater.inflate(R.layout.popup_talk_control, null);
        setContentView(mPopupView);
        setWidth(ViewGroup.LayoutParams.MATCH_PARENT);
        setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        setFocusable(true);
        // setAnimationStyle();
        setBackgroundDrawable(new ColorDrawable(0xb0000000));

        // 初始化事件
        initEvent();
    }

    private void initEvent(){
        mPopupView.findViewById(R.id.talk_btn).setOnTouchListener(TalkPopupWindow.this);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {

        switch (event.getAction()) {
            // 按下开启对讲
            case MotionEvent.ACTION_DOWN: {
                Integer talkChannels = VMSNetSDK.getInstance().getTalkChannelsOpt(MainActivity.PLAY_WINDOW_NO);
                if (talkChannels > 0) {
                    VMSNetSDK.getInstance().openLiveTalkOpt(MainActivity.PLAY_WINDOW_NO,
                            talkChannels, mBusiness);
                } else {
                    showToast("抱歉，当前摄像头不支持对讲功能");
                }
                break;
            }
            // 松开关闭对讲
            case MotionEvent.ACTION_UP: {
                VMSNetSDK.getInstance().closeLiveTalkOpt(MainActivity.PLAY_WINDOW_NO);
                break;
            }
        }
        return false;
    }

    private  void showToast(String message){
        Toast.makeText(mParentActivity.getApplicationContext(),message,Toast.LENGTH_SHORT).show();
    }
}
