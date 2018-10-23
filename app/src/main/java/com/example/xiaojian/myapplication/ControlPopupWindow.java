package com.example.xiaojian.myapplication;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import android.widget.Toast;
import android.os.Handler;

import com.hikvision.sdk.VMSNetSDK;
import com.hikvision.sdk.consts.SDKConstant;
import com.hikvision.sdk.net.business.OnVMSNetSDKBusiness;

public class ControlPopupWindow extends PopupWindow implements View.OnClickListener {

    // 窗口视图对象
    private View mPopupView;

    // 父Activity对象
    private Activity mParentActivity;

    // 控制指令
    Integer mPtzCommand;

    // 控制回调对象
    private OnVMSNetSDKBusiness mBusiness = new OnVMSNetSDKBusiness() {
        @Override
        public void onFailure() {
            mMessageHandler.obtainMessage(0,"控制失败");
        }

        @Override
        public void onSuccess(Object o) {
            mMessageHandler.obtainMessage(0,"控制成功");
        }
    };

    // 消息回调显示
    private Handler mMessageHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            showToast((String) msg.obj);
            return false;
        }
    });

    ControlPopupWindow(Activity context) {
        super(context);
        mParentActivity = context;
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mPopupView = inflater.inflate(R.layout.popup_center_control, null);
        setContentView(mPopupView);
        setWidth(ViewGroup.LayoutParams.MATCH_PARENT);
        setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        setFocusable(true);
        setBackgroundDrawable(new ColorDrawable(0xb0000000));
        initEvent();
    }


    private void initEvent() {
        mPopupView.findViewById(R.id.btn_up).setOnClickListener(this);
        mPopupView.findViewById(R.id.btn_left).setOnClickListener(this);
        mPopupView.findViewById(R.id.btn_right).setOnClickListener(this);
        mPopupView.findViewById(R.id.btn_down).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_up:
                mPtzCommand = SDKConstant.PTZCommandConstant.CUSTOM_CMD_UP;
                break;
            case R.id.btn_left:
                mPtzCommand = SDKConstant.PTZCommandConstant.CUSTOM_CMD_LEFT;
                break;
            case R.id.btn_right:
                mPtzCommand = SDKConstant.PTZCommandConstant.CUSTOM_CMD_RIGHT;
                break;
            case R.id.btn_down:
                mPtzCommand = SDKConstant.PTZCommandConstant.CUSTOM_CMD_DOWN;
                break;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                VMSNetSDK.getInstance().sendPTZCtrlCommand(MainActivity.PLAY_WINDOW_NO, false,
                        SDKConstant.PTZCommandConstant.ACTION_START, mPtzCommand, 30, mBusiness);
            }
        }).start();
    }

    private void showToast(String message) {
        Toast.makeText(mParentActivity.getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }
}
