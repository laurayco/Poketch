package io.github.tylerelric.poketch.data;

import android.content.DialogInterface;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import java.util.Map;

public abstract class QueryViewAdapter
implements ResourceSelected {

    ResourceSelected on_select;
    Map<String,Object> data;

    public abstract QueryViewHolder create_holder(ViewGroup parent, int viewType);

    public void set_select(ResourceSelected sel) {
        on_select = sel;
    }

    public QueryViewHolder create_view_holder(ViewGroup parent, int viewType) {
        QueryViewHolder vh = create_holder(parent,viewType);
        vh.setClick(this);
        return vh;
    }

    public void bind_view_holder(RecyclerView.ViewHolder holder, Map<String,Object> props) {
        data = props;
        update_view((QueryViewHolder)holder, props);
    }

    public void update_view(QueryViewHolder holder, Map<String, Object> props) {
        holder.update_data(props);
    }

    @Override
    public void on_resource_selected(Map<String, Object> data) {
        if(on_select==null) return;
        on_select.on_resource_selected(data);
    }
}
