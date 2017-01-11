package net.oschina.app.improve.tweet.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import net.oschina.app.R;
import net.oschina.app.improve.base.activities.BaseBackActivity;
import net.oschina.app.improve.utils.AssimilateUtils;
import net.oschina.app.improve.utils.CacheManager;
import net.oschina.app.improve.utils.DialogHelper;
import net.oschina.common.adapter.TextWatcherAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import butterknife.Bind;

public class TweetTopicActivity extends BaseBackActivity {
    private static final String CACHE_FILE = "TweetTopicLocalCache";
    @Bind(R.id.edit_enter_tag)
    EditText mTopicContent;

    @Bind(R.id.recycler)
    RecyclerView mRecycler;

    private List<Object> mHotList = new ArrayList<>();
    private List<TopicBean> mLocalList = new ArrayList<>();
    private LinkedList<String> mCache;
    private String[] mLabels = new String[]{"热门", "本地"};

    @Override
    protected int getContentView() {
        return R.layout.activity_tweet_topic;
    }

    @Override
    protected void initWidget() {
        super.initWidget();
        mRecycler.setLayoutManager(new LinearLayoutManager(this));
        mRecycler.setAdapter(adapter);
        mTopicContent.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.ACTION_DOWN) {
                    onSubmit();
                    return true;
                }
                return false;
            }
        });
        mTopicContent.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == KeyEvent.ACTION_DOWN || actionId == EditorInfo.IME_ACTION_DONE) {
                    onSubmit();
                    return true;
                }
                return false;
            }
        });
        mTopicContent.addTextChangedListener(new TextWatcherAdapter() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                sortLocalList(getContent());
            }
        });
    }

    @Override
    protected void initData() {
        super.initData();

        mHotList.add(new TopicBean("开源中国"));
        mHotList.add(new TopicBean("开源中国客户端"));

        loadCache();

        adapter.notifyDataSetChanged();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_tweet_topic, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_submit) {
            onSubmit();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private String getContent() {
        return mTopicContent.getText()
                .toString().trim().replace("#", "");
    }

    private void loadCache() {
        List<String> cache = CacheManager.readListJson(this, CACHE_FILE, String.class);
        mCache = new LinkedList<>();
        if (cache != null)
            mCache.addAll(cache);
        int size = mCache.size();
        for (int i = 0; i < size; i++) {
            mLocalList.add(new TopicBean(mCache.get(i), true));
        }
    }

    private void saveCache(String str) {
        final LinkedList<String> cache = mCache;
        // 避免重复添加
        boolean isHave = false;
        for (String s : cache) {
            if (isHave = s.equals(str))
                break;
        }
        if (!isHave)
            cache.addFirst(str);

        // 至多存储15条
        while (cache.size() > 15) {
            cache.removeLast();
        }

        CacheManager.saveToJson(this, CACHE_FILE, cache);
    }

    private void onSubmit() {
        String str = getContent();
        if (TextUtils.isEmpty(str))
            finish();
        else {
            saveCache(str);
            doResult(str);
        }
    }

    private void doResult(String topic) {
        Intent result = new Intent();
        result.putExtra("topic", topic);
        setResult(RESULT_OK, result);
        finish();
    }

    private void doDeleteCache(TopicBean bean, boolean clear) {
        final LinkedList<String> cache = mCache;
        final List<TopicBean> cacheLocal = mLocalList;
        if (clear) {
            cache.clear();
            cacheLocal.clear();
        } else {
            Iterator<TopicBean> itr = cacheLocal.iterator();
            while (itr.hasNext()) {
                if (itr.next().equals(bean))
                    itr.remove();
            }
            Iterator<String> itrCache = cache.iterator();
            while (itrCache.hasNext()) {
                if (itrCache.next().equals(bean.text))
                    itrCache.remove();
            }
        }
        CacheManager.saveToJson(this, CACHE_FILE, cache);
        adapter.notifyDataSetChanged();
    }

    private void sortLocalList(String text) {
        final String py = TextUtils.isEmpty(text) ? "!#" : AssimilateUtils.returnPinyin(text, false);
        Pattern pattern = Pattern.compile(py);
        for (TopicBean bean : mLocalList) {
            Matcher matcher = pattern.matcher(bean.py);
            if (matcher.find()) {
                bean.sort = matcher.start();
            } else {
                bean.sort = ORDER_MAX;
            }
        }

        Collections.sort(mLocalList, new Comparator<TopicBean>() {
            @Override
            public int compare(TopicBean o1, TopicBean o2) {
                if (o1.sort == ORDER_MAX && o2.sort == ORDER_MAX) {
                    return o1.order - o2.order;
                }
                return o1.sort - o2.sort;
            }
        });

        adapter.notifyDataSetChanged();
    }

    private RecyclerView.Adapter adapter = new RecyclerView.Adapter() {
        @Override
        public int getItemViewType(int position) {
            if (get(position) instanceof String) {
                return R.layout.list_item_sample_label;
            } else {
                if (position == getItemCount() - 1 || position == mHotList.size())
                    return 0;
                else
                    return R.layout.list_item_topic;
            }
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            switch (viewType) {
                case R.layout.list_item_sample_label:
                    return new LabelHolder(LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.list_item_sample_label, parent, false));
                case R.layout.list_item_topic: {
                    View root = LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.list_item_topic, parent, false);
                    return new DataHolder(root, true);
                }
                default:
                    View root = LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.list_item_topic, parent, false);
                    return new DataHolder(root, false);
            }
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof LabelHolder) {
                ((LabelHolder) holder).set((String) get(position));
            } else {
                TopicBean bean = (TopicBean) get(position);
                ((DataHolder) holder).set(bean);
                // Set tag
                holder.itemView.setTag(bean);
            }
        }

        @Override
        public int getItemCount() {
            int hotCount = mHotList.size();
            int localCount = mLocalList.size();
            return hotCount + localCount + (localCount > 0 ? 2 : 1);
        }

        private Object get(int position) {
            if (position == 0)
                return mLabels[0];

            int hotCount = mHotList.size();
            int localCount = mLocalList.size();

            if (localCount > 0) {
                if (position == hotCount + 1)
                    return mLabels[1];
                if ((position >= hotCount + 2))
                    return mLocalList.get(position - hotCount - 2);
            }

            return mHotList.get(position - 1);
        }
    };

    private class LabelHolder extends RecyclerView.ViewHolder {
        private TextView mText;

        LabelHolder(View itemView) {
            super(itemView);
            mText = (TextView) itemView.findViewById(R.id.txt_string);
        }


        void set(String data) {
            mText.setText(data);
        }
    }

    private class DataHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        private TextView mTitle;


        DataHolder(View itemView, boolean needLine) {
            super(itemView);
            mTitle = (TextView) itemView.findViewById(R.id.txt_title);
            if (needLine)
                itemView.findViewById(R.id.line).setVisibility(View.VISIBLE);
            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
        }

        void set(TopicBean data) {
            mTitle.setText(data.text);
        }

        @Override
        public void onClick(View v) {
            Object obj = v.getTag();
            if (obj != null && obj instanceof TopicBean) {
                doResult(((TopicBean) obj).text);
            }
        }

        @Override
        public boolean onLongClick(View v) {
            Object obj = v.getTag();
            if (obj != null && obj instanceof TopicBean) {
                final TopicBean bean = (TopicBean) obj;
                if (!bean.isLocal)
                    return false;

                String[] items = new String[2];
                items[0] = getResources().getString(R.string.delete);
                items[1] = getResources().getString(R.string.delete_all);
                DialogHelper.getSelectDialog(TweetTopicActivity.this, items, "取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (i == 0) {
                            doDeleteCache(bean, false);
                        } else if (i == 1) {
                            doDeleteCache(null, true);
                        }
                    }
                }).show();

                return true;
            }
            return false;
        }
    }

    private static int ORDER_COUNT = 0;
    private static int ORDER_MAX = 100;

    private class TopicBean {
        String text;
        String py;
        private boolean isLocal;
        private int sort = ORDER_MAX;
        private int order = ORDER_COUNT++;

        TopicBean(String text) {
            this.text = text;
            this.py = AssimilateUtils.returnPinyin(text, false);
        }

        TopicBean(String text, boolean isLocal) {
            this.text = text;
            this.py = AssimilateUtils.returnPinyin(text, false);
            this.isLocal = isLocal;
        }


        @Override
        public boolean equals(Object obj) {
            if (obj == null || !(obj instanceof TopicBean))
                return false;
            String oth = ((TopicBean) obj).text;
            if (oth == null)
                return this.text == null;
            return oth.equals(this.text);
        }
    }

}