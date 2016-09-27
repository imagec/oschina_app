package net.oschina.app.improve.media;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import net.oschina.app.R;
import net.oschina.app.improve.app.AppOperator;
import net.oschina.app.improve.base.activities.BaseActivity;
import net.oschina.app.improve.utils.PicturesCompressor;
import net.oschina.app.improve.utils.StreamUtils;
import net.qiujuer.genius.ui.widget.Loading;

import java.io.File;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;


/**
 * 图片预览Activity
 */
public class ImageGalleryActivity extends BaseActivity implements ViewPager.OnPageChangeListener, View.OnClickListener {
    public static final String KEY_IMAGE = "images";
    public static final String KEY_POSITION = "position";
    public static final String KEY_NEED_SAVE = "save";
    private PreviewerViewPager mImagePager;
    private TextView mIndexText;
    private String[] mImageSources;
    private int mCurPosition;
    private boolean mNeedSaveLocal;

    public static void show(Context context, String images) {
        show(context, images, true);
    }

    public static void show(Context context, String images, boolean needSaveLocal) {
        if (images == null)
            return;
        show(context, new String[]{images}, 0, needSaveLocal);
    }

    public static void show(Context context, String[] images, int position) {
        show(context, images, position, true);
    }

    public static void show(Context context, String[] images, int position, boolean needSaveLocal) {
        if (images == null || images.length == 0)
            return;
        Intent intent = new Intent(context, ImageGalleryActivity.class);
        intent.putExtra(KEY_IMAGE, images);
        intent.putExtra(KEY_POSITION, position);
        intent.putExtra(KEY_NEED_SAVE, needSaveLocal);
        context.startActivity(intent);
    }

    @Override
    protected boolean initBundle(Bundle bundle) {
        mImageSources = bundle.getStringArray(KEY_IMAGE);
        mCurPosition = bundle.getInt(KEY_POSITION, 0);
        mNeedSaveLocal = bundle.getBoolean(KEY_NEED_SAVE, true);
        return mImageSources != null;
    }

    @Override
    protected void initWindow() {
        super.initWindow();
        getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
    }

    @Override
    protected int getContentView() {
        return R.layout.activity_image_gallery;
    }

    @Override
    protected void initWidget() {
        super.initWidget();
        setTitle("");
        mImagePager = (PreviewerViewPager) findViewById(R.id.vp_image);
        mIndexText = (TextView) findViewById(R.id.tv_index);
        mImagePager.addOnPageChangeListener(this);
        if (mNeedSaveLocal)
            findViewById(R.id.iv_save).setOnClickListener(this);
        else
            findViewById(R.id.iv_save).setVisibility(View.GONE);
    }

    @Override
    protected void initData() {
        super.initData();
        int len = mImageSources.length;
        if (mCurPosition < 0 || mCurPosition >= len)
            mCurPosition = 0;

        // If only one, we not need the text to show
        if (len == 1)
            mIndexText.setVisibility(View.GONE);

        mImagePager.setAdapter(new ViewPagerAdapter());
        mImagePager.setCurrentItem(mCurPosition);
        // First we call to init the TextView
        onPageSelected(mCurPosition);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_save:
                saveToFile();
                break;
        }
    }

    private void saveToFile() {
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            Toast.makeText(this, R.string.gallery_save_file_not_have_external_storage, Toast.LENGTH_SHORT).show();
            return;
        }

        String path = mImageSources[mCurPosition];

        // In this save max image size is source
        final Future<File> future = getImageLoader().load(path).downloadOnly(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL);

        AppOperator.runOnThread(new Runnable() {
            @Override
            public void run() {
                try {
                    File sourceFile = future.get();
                    String extension = PicturesCompressor.getExtension(sourceFile.getAbsolutePath());
                    String extDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                            .getAbsolutePath() + File.separator + "开源中国";
                    File extDirFile = new File(extDir);
                    if (!extDirFile.exists()) {
                        if (!extDirFile.mkdirs()) {
                            callSaveStatus(false, null);
                            return;
                        }
                    }
                    final File saveFile = new File(extDirFile, String.format("IMG_%s.%s", System.currentTimeMillis(), extension));
                    final boolean isSuccess = StreamUtils.copyFile(sourceFile, saveFile);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            callSaveStatus(isSuccess, saveFile);
                        }
                    });
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void callSaveStatus(boolean success, File savePath) {
        if (success) {
            // notify
            Uri uri = Uri.fromFile(savePath);
            sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri));
            Toast.makeText(ImageGalleryActivity.this, R.string.gallery_save_file_success, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(ImageGalleryActivity.this, R.string.gallery_save_file_failed, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        mCurPosition = position;
        mIndexText.setText(String.format("%s/%s", (position + 1), mImageSources.length));
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    private Point displayDimens;

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    @SuppressWarnings("deprecation")
    private synchronized Point getDisplayDimens() {
        if (displayDimens != null) {
            return displayDimens;
        }
        WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            displayDimens = new Point();
            display.getSize(displayDimens);
        } else {
            displayDimens = new Point(display.getWidth(), display.getHeight());
        }

        // In this we can only get 75% width and 45% height
        displayDimens.y = (int) (displayDimens.y * 0.45f);
        displayDimens.x = (int) (displayDimens.x * 0.75f);
        return displayDimens;
    }

    private class ViewPagerAdapter extends PagerAdapter implements ImagePreviewView.OnReachBorderListener {

        private View.OnClickListener mFinishClickListener;

        @Override
        public int getCount() {
            return mImageSources.length;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            View view = LayoutInflater.from(container.getContext())
                    .inflate(R.layout.lay_gallery_page_item_contener, container, false);
            ImagePreviewView previewView = (ImagePreviewView) view.findViewById(R.id.iv_preview);
            previewView.setOnReachBorderListener(this);
            Loading loading = (Loading) view.findViewById(R.id.loading);
            ImageView defaultView = (ImageView) view.findViewById(R.id.iv_default);

            // Do load
            loadImage(mImageSources[position], previewView, defaultView, loading);

            previewView.setOnClickListener(getListener());
            container.addView(view);
            return view;
        }

        private View.OnClickListener getListener() {
            if (mFinishClickListener == null) {
                mFinishClickListener = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        finish();
                    }
                };
            }
            return mFinishClickListener;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }

        @Override
        public void onReachBorder(boolean isReached) {
            mImagePager.isInterceptable(isReached);
        }

        private void loadImage(final String path,
                               final ImagePreviewView previewView,
                               final ImageView defaultView,
                               final Loading loading) {
            loadImageDoDownAndGetOverrideSize(path, new DoOverrideSizeCallback() {
                @Override
                public void onDone(int overrideW, int overrideH) {
                    getImageLoader()
                            .load(path)
                            .listener(new RequestListener<String, GlideDrawable>() {
                                @Override
                                public boolean onException(Exception e, String model,
                                                           Target<GlideDrawable> target,
                                                           boolean isFirstResource) {
                                    onError();
                                    return false;
                                }

                                @Override
                                public boolean onResourceReady(GlideDrawable resource, String model,
                                                               Target<GlideDrawable> target,
                                                               boolean isFromMemoryCache,
                                                               boolean isFirstResource) {
                                    loading.stop();
                                    loading.setVisibility(View.GONE);
                                    return false;
                                }
                            }).diskCacheStrategy(DiskCacheStrategy.SOURCE)
                            .override(overrideW, overrideH)
                            .into(previewView);
                }

                @Override
                public void onError() {
                    loading.stop();
                    loading.setVisibility(View.GONE);
                    defaultView.setVisibility(View.VISIBLE);
                }
            });
        }

        private void loadImageDoDownAndGetOverrideSize(final String path, final DoOverrideSizeCallback callback) {
            // In this save max image size is source
            final Future<File> future = getImageLoader().load(path)
                    .downloadOnly(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL);

            AppOperator.runOnThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        File sourceFile = future.get();

                        BitmapFactory.Options options = PicturesCompressor.createOptions();
                        // First decode with inJustDecodeBounds=true to check dimensions
                        options.inJustDecodeBounds = true;
                        BitmapFactory.decodeFile(sourceFile.getAbsolutePath(), options);
                        int width = options.outWidth;
                        int height = options.outHeight;

                        // This max size
                        final int maxLen = 1280 * 5;

                        // Get Screen
                        Point point = getDisplayDimens();
                        // Init override size
                        final int overrideW, overrideH;

                        if ((width / (float) height) > (point.x / (float) point.y)) {
                            overrideH = height > point.y ? point.y : height;
                            overrideW = width > maxLen ? maxLen : width;
                        } else {
                            overrideW = width > point.x ? point.x : width;
                            overrideH = height > maxLen ? maxLen : height;
                        }

                        // Call back on main thread
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                callback.onDone(overrideW, overrideH);
                            }
                        });

                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();

                        // Call back on main thread
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                callback.onError();
                            }
                        });
                    }
                }
            });
        }


    }

    interface DoOverrideSizeCallback {
        void onDone(int overrideW, int overrideH);

        void onError();
    }
}
