package net.oschina.app.adapter.general;

import net.oschina.app.R;
import net.oschina.app.adapter.ViewHolder;
import net.oschina.app.adapter.base.BaseListAdapter;
import net.oschina.app.bean.event.Event;
import net.oschina.app.util.StringUtils;

/**
 * Created by huanghaibin
 * on 16-5-25.
 */
public class EventAdapter extends BaseListAdapter<Event> {
    public EventAdapter(Callback callback) {
        super(callback);
    }

    @Override
    protected void convert(ViewHolder vh, Event item, int position) {
        vh.setText(R.id.tv_event_title, item.getTitle());
        vh.setImageForNet(R.id.iv_event,item.getImg());
        vh.setText(R.id.tv_event_state,"is jie su");
        vh.setText(R.id.tv_event_pub_date, StringUtils.friendly_time(item.getPubDate()));
        vh.setText(R.id.tv_event_member,item.getApplyCount() + "参与");
    }

    @Override
    protected int getLayoutId(int position, Event item) {
        return R.layout.item_list_event;
    }
}