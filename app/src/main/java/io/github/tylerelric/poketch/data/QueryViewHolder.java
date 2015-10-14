package io.github.tylerelric.poketch.data;

import android.support.v7.widget.RecyclerView;
import android.view.View;

import java.util.Map;

public class QueryViewHolder
extends RecyclerView.ViewHolder
implements View.OnClickListener{

    public void setClick(ResourceSelected click) {
        this.click = click;
    }

    ResourceSelected click;
    Map<String,Object> data;

    public QueryViewHolder(View itemView) {
        super(itemView);
        itemView.setOnClickListener(this);
    }

    public void update_data(Map<String,Object> dat) {
        data = dat;
    }

    @Override
    public void onClick(View v) {
        click.on_resource_selected(data);
    }
}
