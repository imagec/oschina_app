package net.oschina.app.improve.detail.activities;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import com.google.gson.reflect.TypeToken;
import com.loopj.android.http.TextHttpResponseHandler;

import net.oschina.app.AppContext;
import net.oschina.app.R;
import net.oschina.app.api.remote.OSChinaApi;
import net.oschina.app.bean.Tweet;
import net.oschina.app.improve.bean.SoftwareDetail;
import net.oschina.app.improve.bean.base.ResultBean;
import net.oschina.app.improve.detail.contract.SoftDetailContract;
import net.oschina.app.improve.detail.fragments.DetailFragment;
import net.oschina.app.improve.detail.fragments.SoftWareDetailFragment;
import net.oschina.app.service.ServerTaskUtils;
import net.oschina.app.util.HTMLUtil;
import net.oschina.app.util.StringUtils;
import net.oschina.app.util.URLsUtils;

import java.lang.reflect.Type;

import cz.msebera.android.httpclient.Header;

/**
 * Created by fei on 2016/6/13.
 * desc:   news detail  module
 */
public class SoftwareDetailActivity extends DetailActivity<SoftwareDetail, SoftDetailContract.View> implements SoftDetailContract.Operator {

    public static final String TAG = "SoftwareDetailActivity";
    private static final int MAX_TEXT_LENGTH = 160;

    /**
     * show news detail
     *
     * @param context context
     * @param id      id
     */
    public static void show(Context context, long id) {
        Intent intent = new Intent(context, SoftwareDetailActivity.class);
        intent.putExtra("id", id);
        context.startActivity(intent);
    }


    @Override
    protected int getContentView() {
        return R.layout.activity_blog_detail;
    }

    @Override
    void requestData() {
        OSChinaApi.getNewsDetail(getDataId(), OSChinaApi.CATALOG_SOFTWARE_DETAIL, getRequestHandler());
    }

    @Override
    Class<? extends DetailFragment> getDataViewFragment() {
        return SoftWareDetailFragment.class;
    }

    @Override
    Type getDataType() {
        return new TypeToken<ResultBean<SoftwareDetail>>() {
        }.getType();
    }

    @Override
    public void toFavorite() {
        int uid = requestCheck();
        if (uid == 0)
            return;
        showWaitDialog(R.string.progress_submit);
        final SoftwareDetail softwareDetail = getData();
        OSChinaApi.getFavReverse(getDataId(), 1, new TextHttpResponseHandler() {
            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                hideWaitDialog();
                if (softwareDetail.isFavorite())
                    AppContext.showToastShort(R.string.del_favorite_faile);
                else
                    AppContext.showToastShort(R.string.add_favorite_faile);
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, String responseString) {
                try {
                    Type type = new TypeToken<ResultBean<SoftwareDetail>>() {
                    }.getType();

                    ResultBean<SoftwareDetail> resultBean = AppContext.createGson().fromJson(responseString, type);
                    if (resultBean != null && resultBean.isSuccess()) {
                        softwareDetail.setFavorite(!softwareDetail.isFavorite());
                        mView.toFavoriteOk(softwareDetail);
                        if (softwareDetail.isFavorite())
                            AppContext.showToastShort(R.string.add_favorite_success);
                        else
                            AppContext.showToastShort(R.string.del_favorite_success);
                    }
                    hideWaitDialog();
                } catch (Exception e) {
                    e.printStackTrace();
                    onFailure(statusCode, headers, responseString, e);
                }
            }
        });
    }

    @Override
    public void toShare() {
        if (getDataId() != 0 && getData() != null) {
            String content;

            String url = String.format(URLsUtils.URL_MOBILE + "software/%s", getDataId());
            final SoftwareDetail softwareDetail = getData();
            if (softwareDetail.getBody().length() > 55) {
                content = HTMLUtil.delHTMLTag(softwareDetail.getBody().trim());
                if (content.length() > 55)
                    content = StringUtils.getSubString(0, 55, content);
            } else {
                content = HTMLUtil.delHTMLTag(softwareDetail.getBody().trim());
            }
            String title = softwareDetail.getName();

            if (TextUtils.isEmpty(url) || TextUtils.isEmpty(content) || TextUtils.isEmpty(title)) {
                AppContext.showToast("内容加载失败...");
                return;
            }
            toShare(title, content, url);
        } else {
            AppContext.showToast("内容加载失败...");
        }
    }


    @Override
    public void toSendComment(long id, long commentId, long commentAuthorId, String comment) {
        int uid = requestCheck();
        if (uid == 0)
            return;

        if (TextUtils.isEmpty(comment)) {
            AppContext.showToastShort(R.string.tip_comment_content_empty);
            return;
        }

        if (comment.length() > MAX_TEXT_LENGTH) {
            AppContext.showToastShort(R.string.tip_content_too_long);
            return;
        }


        Tweet tweet = new Tweet();

        tweet.setAuthorid(AppContext.getInstance().getLoginUid());
        tweet.setBody(comment);
        ServerTaskUtils.pubTweet(this, tweet);

        mView.toSendCommentOk(null);



//        OSChinaApi.publishComment(id, commentId, 0, commentAuthorId, 1, comment, new TextHttpResponseHandler() {
//
//
//            @Override
//            public void onStart() {
//                super.onStart();
//                showWaitDialog(R.string.progress_submit);
//            }
//
//            @Override
//            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
//                AppContext.showToast("评论失败!");
//                hideWaitDialog();
//            }
//
//            @Override
//            public void onSuccess(int statusCode, Header[] headers, String responseString) {
//                try {
//                    Type type = new TypeToken<ResultBean<Comment>>() {
//                    }.getType();
//
//                    ResultBean<Comment> resultBean = AppContext.createGson().fromJson(responseString, type);
//                    Log.d(TAG, "onSuccess: ------>" + resultBean.getCode());
//                    if (resultBean.isSuccess()) {
//                        Comment respComment = resultBean.getResult();
//                        if (respComment != null) {
//                            SoftDetailContract.View view = mView;
//                            if (view != null) {
//                                view.toSendCommentOk(respComment);
//                            }
//                        }
//                    }
//                    hideWaitDialog();
//                } catch (Exception e) {
//                    e.printStackTrace();
//                    onFailure(statusCode, headers, responseString, e);
//                }
//                hideWaitDialog();
//            }
//        });

    }
}
