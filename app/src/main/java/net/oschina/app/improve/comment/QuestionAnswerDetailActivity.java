package net.oschina.app.improve.comment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.reflect.TypeToken;
import com.loopj.android.http.TextHttpResponseHandler;

import net.oschina.app.AppContext;
import net.oschina.app.R;
import net.oschina.app.api.remote.OSChinaApi;
import net.oschina.app.improve.activities.BaseBackActivity;
import net.oschina.app.improve.adapter.tweet.TweetCommentAdapter;
import net.oschina.app.improve.bean.base.ResultBean;
import net.oschina.app.improve.bean.simple.Comment;
import net.oschina.app.improve.bean.simple.CommentEX;
import net.oschina.app.improve.behavior.KeyboardInputDelegation;
import net.oschina.app.util.HTMLUtil;
import net.oschina.app.util.StringUtils;
import net.oschina.app.util.UIHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import butterknife.Bind;
import butterknife.OnClick;
import cz.msebera.android.httpclient.Header;
import de.hdodenhof.circleimageview.CircleImageView;

/**
 * 问答的评论详情
 * Created by thanatos on 16/6/16.
 */
public class QuestionAnswerDetailActivity extends BaseBackActivity{

    public static final String BUNDLE_KEY = "BUNDLE_KEY";
    public static final String BUNDLE_ARTICLE_KEY = "BUNDLE_ARTICLE_KEY";

    @Bind(R.id.iv_portrait) CircleImageView ivPortrait;
    @Bind(R.id.tv_nick) TextView tvNick;
    @Bind(R.id.tv_time) TextView tvTime;
    @Bind(R.id.iv_vote_up) ImageView ivVoteUp;
    @Bind(R.id.iv_vote_down) ImageView ivVoteDown;
    @Bind(R.id.tv_up_count) TextView tvVoteCount;
    @Bind(R.id.webview) WebView mWebView;
    @Bind(R.id.tv_comment_count) TextView tvCmnCount;
    @Bind(R.id.layout_container) LinearLayout mLayoutContainer;
    @Bind(R.id.layout_coordinator) CoordinatorLayout mCoorLayout;
    @Bind(R.id.layout_scroll) NestedScrollView mScrollView;

    private long sid;
    private CommentEX comment;
    private CommentEX.Reply reply;
    private List<CommentEX.Reply> replies = new ArrayList<>();
    private KeyboardInputDelegation mDelegation;
    private TextHttpResponseHandler onSendCommentHandler;
    private View.OnClickListener onReplyButtonClickListener;

    /**
     *
     * @param context context
     * @param comment comment
     * @param sid 文章id
     */
    public static void show(Context context, CommentEX comment, long sid){
        Intent intent = new Intent(context, QuestionAnswerDetailActivity.class);
        intent.putExtra(BUNDLE_KEY, comment);
        intent.putExtra(BUNDLE_ARTICLE_KEY, sid);
        context.startActivity(intent);
    }

    @Override
    protected boolean initBundle(Bundle bundle) {
        comment = (CommentEX) getIntent().getSerializableExtra(BUNDLE_KEY);
        sid = getIntent().getLongExtra(BUNDLE_ARTICLE_KEY, 0);
        return !(comment == null || comment.getId() <= 0) && super.initBundle(bundle);
    }

    @Override
    protected int getContentView() {
        return R.layout.activity_post_answer_detail;
    }

    @Override
    protected void initWindow() {
        super.initWindow();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null){
            actionBar.setTitle("返回");
        }
    }

    protected void initWidget(){
        // portrait
        if (TextUtils.isEmpty(comment.getAuthorPortrait())){
            ivPortrait.setImageResource(R.drawable.widget_dface);
        }else{
            getImageLoader().load(comment.getAuthorPortrait()).into(ivPortrait);
        }

        // nick
        tvNick.setText(comment.getAuthor());

        // publish time
        if (!TextUtils.isEmpty(comment.getPubDate()))
            tvTime.setText(StringUtils.friendly_time(comment.getPubDate()));

        // vote state
        switch (comment.getVoteState()){
            case CommentEX.VOTE_STATE_UP:
                ivVoteUp.setSelected(true);
                break;
            case CommentEX.VOTE_STATE_DOWN:
                ivVoteDown.setSelected(true);
        }

        // vote count
        tvVoteCount.setText(String.valueOf(comment.getVoteCount()));

        tvCmnCount.setText("评论 ("+ (comment.getReplies() == null ? 0 : comment.getReplies().length) +")");

        mDelegation = KeyboardInputDelegation.delegation(this, mCoorLayout, mScrollView);
        mDelegation.setAdapter(new KeyboardInputDelegation.KeyboardInputAdapter() {
            @Override
            public void onSubmit(TextView v, String content) {
                if (TextUtils.isEmpty(content.replaceAll("[ \\s\\n]+", ""))) {
                    Toast.makeText(QuestionAnswerDetailActivity.this, "请输入文字", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!AppContext.getInstance().isLogin()) {
                    UIHelper.showLoginActivity(QuestionAnswerDetailActivity.this);
                    return;
                }
                OSChinaApi.publicComment(sid, -1, -1, 2, content, onSendCommentHandler);
            }

            @Override
            public void onFinalBackSpace(View v) {
                if (reply == null) return;
                reply = null;
                mDelegation.getInputView().setHint("发表评论");
            }
        });

        if (comment.getReplies() != null){
            Collections.addAll(replies, comment.getReplies());
            for (int i=0; i<comment.getReplies().length; i++){
                appendComment(i, comment.getReplies()[i]);
            }
        }

        fillWebView();
    }

    private void fillWebView(){
        if (TextUtils.isEmpty(comment.getContent())) return;
        String html = HTMLUtil.setupWebContent(comment.getContent(), true, true, "padding: 16px");
        UIHelper.addWebImageShow(this, mWebView);
        mWebView.loadDataWithBaseURL(null, html, "text/html", "utf-8", null);
    }

    private void appendComment(int i, CommentEX.Reply reply){
        View view = LayoutInflater.from(this).inflate(R.layout.list_item_tweet_comment, mLayoutContainer, false);
        TweetCommentAdapter.TweetCommentHolderView holder = new TweetCommentAdapter.TweetCommentHolderView(view);
        holder.tvName.setText(reply.getAuthor());
        if (TextUtils.isEmpty(reply.getAuthorPortrait())){
            holder.ivPortrait.setImageResource(R.drawable.widget_dface);
        }else{
            getImageLoader().load(reply.getAuthorPortrait()).into(holder.ivPortrait);
        }
        holder.tvTime.setText(String.format("%s楼  %s", i+1, StringUtils.friendly_time(reply.getPubDate())));
        holder.tvContent.setText(reply.getContent());
        holder.btnReply.setTag(reply);
        holder.btnReply.setOnClickListener(getOnReplyButtonClickListener());
        mLayoutContainer.addView(view);
    }

    private View.OnClickListener getOnReplyButtonClickListener(){
        if (onReplyButtonClickListener == null){
            onReplyButtonClickListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    CommentEX.Reply reply = (CommentEX.Reply) v.getTag();
                    mDelegation.notifyWrapper();
                    mDelegation.getInputView().setHint("@" + reply.getAuthor() + " ");
                    QuestionAnswerDetailActivity.this.reply = reply;
                }
            };
        }
        return onReplyButtonClickListener;
    }

    protected void initData(){
        onSendCommentHandler = new TextHttpResponseHandler() {
            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                Toast.makeText(QuestionAnswerDetailActivity.this, "评论失败", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, String responseString) {
                ResultBean<CommentEX.Reply> result = AppContext.createGson().fromJson(
                        responseString,
                        new TypeToken<ResultBean<CommentEX.Reply>>(){}.getType()
                );
                if (result.isSuccess()){
                    replies.add(result.getResult());
                    tvCmnCount.setText("评论 ("+ replies.size() +")");
                    appendComment(replies.size() -1, result.getResult());
                }else{
                    Toast.makeText(QuestionAnswerDetailActivity.this, result.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        };

        OSChinaApi.getComment(comment.getId(), 2, new TextHttpResponseHandler() {
            @Override
            public void onFailure(int statusCode, Header[] headers, String respStr, Throwable throwable) {
                Toast.makeText(QuestionAnswerDetailActivity.this, "请求失败", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, String respStr) {
                ResultBean<CommentEX> result = AppContext.createGson().fromJson(respStr,
                        new TypeToken<ResultBean<CommentEX>>(){}.getType());
                Log.d("oschina", "-------------------\n" + respStr + "\n--------------");
                if (result.isSuccess()){
                    CommentEX cmn = result.getResult();
                    if (cmn != null && cmn.getId() > 0){
                        comment = cmn;
                        initWidget();
                        return;
                    }
                }
                Toast.makeText(QuestionAnswerDetailActivity.this, "请求失败", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @OnClick(R.id.layout_vote) void onClickVote(){

    }


}