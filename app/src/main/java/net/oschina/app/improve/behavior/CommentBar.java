package net.oschina.app.improve.behavior;

import android.content.Context;
import android.support.design.widget.CoordinatorLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import net.oschina.app.R;
import net.oschina.app.improve.widget.BottomSheetBar;

/**
 * Created by haibin
 * on 2016/11/10.
 */
@SuppressWarnings("all")
public class CommentBar {
    private Context mContext;
    private View mRootView;
    private FrameLayout mFrameLayout;
    private CoordinatorLayout mCoordinatorLayout;
    private ImageButton mFavView;
    private ImageButton mShareView;
    private TextView mCommentText;
    private BottomSheetBar mDelegation;

    private CommentBar(Context context) {
        this.mContext = context;
    }

    public static CommentBar delegation(Context context, CoordinatorLayout coordinatorLayout) {
        CommentBar bar = new CommentBar(context);
        bar.mRootView = LayoutInflater.from(context).inflate(R.layout.layout_comment_bar, coordinatorLayout, false);
        bar.mCoordinatorLayout = coordinatorLayout;
        bar.mDelegation = BottomSheetBar.delegation(context);
        bar.mCoordinatorLayout.addView(bar.mRootView);
        bar.initView();
        return bar;
    }

    private void initView() {
        ((CoordinatorLayout.LayoutParams) mRootView.getLayoutParams()).setBehavior(new FloatingAutoHideDownBehavior());
        mFavView = (ImageButton) mRootView.findViewById(R.id.ib_fav);
        mShareView = (ImageButton) mRootView.findViewById(R.id.ib_share);
        mCommentText = (TextView) mRootView.findViewById(R.id.tv_comment);
        mCommentText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDelegation.show(mCommentText.getHint().toString());
            }
        });
    }

    public void setShareListener(View.OnClickListener listener) {
        mShareView.setOnClickListener(listener);
    }

    public void setFavListener(View.OnClickListener listener) {
        mFavView.setOnClickListener(listener);
    }

    public void setCommentListener(View.OnClickListener listener) {
        mCommentText.setOnClickListener(listener);
    }

    public void setCommentHint(String text) {
        mCommentText.setHint(text);
    }

    public void setFavDrawable(int drawable) {
        mFavView.setImageResource(drawable);
    }

    public BottomSheetBar getBottomSheet() {
        return mDelegation;
    }

    public void setCommitButtonEnable(boolean enable) {
        mDelegation.getBtnCommit().setEnabled(enable);
    }

    public TextView getCommentText() {
        return mCommentText;
    }
}