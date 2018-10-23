package com.example.xiaojian.myapplication;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupWindow;

import com.bumptech.glide.Glide;

public class CutPopupWindow extends PopupWindow implements View.OnClickListener {

    // 窗口视图对象
    private View mPopupView;

    // 父Activity对象
    private Activity mParentActivity;

    // 截图展示视图
    private ImageView mImageView;

    // 截图地址
    private String mPicturePath;

    CutPopupWindow(Activity context, String picturePath) {
        super(context);
        mParentActivity = context;
        mPicturePath = picturePath;
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mPopupView = inflater.inflate(R.layout.popup_cut_modal, null);
        setContentView(mPopupView);
        setWidth(ViewGroup.LayoutParams.MATCH_PARENT);
        setHeight(ViewGroup.LayoutParams.MATCH_PARENT);
        setFocusable(true);
        // setAnimationStyle();
        setBackgroundDrawable(new ColorDrawable(0xb0000000));
        findView();
        initView();
    }

    private void findView() {
        mImageView = mPopupView.findViewById(R.id.picture_view);
    }

    private void initView() {
        Glide.with(mParentActivity)
                .load(Drawable.createFromPath(mPicturePath))
//                .load("https://picsum.photos/600/400?100")
                .into(mImageView);
        mPopupView.findViewById(R.id.select_btn).setOnClickListener(CutPopupWindow.this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.select_btn:
                Intent intent = new Intent(mParentActivity,SelectActivity.class);
                mParentActivity.startActivity(intent);
                break;
        }
    }
}
