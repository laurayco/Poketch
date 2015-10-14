package io.github.tylerelric.poketch.data;

import android.app.Activity;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.ListView;

import com.couchbase.lite.Database;
import com.couchbase.lite.Emitter;
import com.couchbase.lite.LiveQuery;
import com.couchbase.lite.Mapper;
import com.couchbase.lite.Query;
import com.couchbase.lite.QueryEnumerator;
import com.couchbase.lite.QueryRow;
import com.couchbase.lite.Reducer;
import com.couchbase.lite.View;
import com.couchbase.lite.support.LazyJsonObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.tylerelric.poketch.MoveViewAdapter;
import io.github.tylerelric.poketch.MoveViewHolder;
import io.github.tylerelric.poketch.SpeciesViewAdapter;

public abstract class MapReduceAdapter
extends RecyclerView.Adapter
implements LiveQuery.ChangeListener, Mapper, Reducer, ResourceSelected {

    Database database;
    LiveQuery lq;
    QueryEnumerator current_data;
    protected QueryViewAdapter adapter;

    public void set_on_resource_selected(ResourceSelected _on_resource_selected) {
        this._on_resource_selected = _on_resource_selected;
    }

    ResourceSelected _on_resource_selected;

    public MapReduceAdapter(PokeapiLoader.Resource rsrc, QueryViewAdapter adapter, Context c) {
        this.cntx = c;
        this.adapter = adapter;
        database = rsrc.get_database();
        View view = database.getView(rsrc.get_view_name());
        Query q = view.createQuery();
        lq = q.toLiveQuery();
        lq.addChangeListener(this);
    }

    public MapReduceAdapter(PokeapiLoader.Resource rsrc, Context c) {
        this.cntx = c;
        this.adapter = resource_adapter(rsrc);
        database = rsrc.get_database();
        View view = database.getView(rsrc.get_view_name());
        Query q = view.createQuery();
        lq = q.toLiveQuery();
        lq.addChangeListener(this);
    }

    protected static QueryViewAdapter resource_adapter(PokeapiLoader.Resource rsrc) {
        if(rsrc instanceof PokeapiLoader.Pokemon) {
            return new SpeciesViewAdapter();
        } else if (rsrc instanceof PokeapiLoader.Moves) {
            return new MoveViewAdapter();
        }
        return null;
    }

    protected MapReduceAdapter(Database db, String viewname, String version, QueryViewAdapter adapter, Context c) {
        super();
        database = db;
        this.adapter = adapter;
        View view = db.getView(viewname);
        view.setMapReduce(this,this,version);
        Query q = view.createQuery();
        lq = q.toLiveQuery();
        lq.addChangeListener(this);
        cntx = c;
    }

    public void setFilter(DataFilter filter) {
        this.filter = filter;
        filter_data();
    }

    DataFilter filter;
    protected final Context cntx;
    protected List<Map<String,Object>> last_results, filtered_results;

    @Override
    public void on_resource_selected(Map<String, Object> data) {
        if(_on_resource_selected==null) return;
        _on_resource_selected.on_resource_selected(data);
    }

    public abstract void map(Map<String, Object> document, Emitter emitter);

    public Object reduce(List<Object> keys, List<Object> values, boolean rereduce) {
        return values;
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        Log.i("MapReduceAdapter", "onAttachedToRecyclerView");
        lq.start();
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        Map<String,Object> data = null;
        if(filtered_results!=null) {
            data = filtered_results.get(position);
        } else if (last_results!=null) {
            data = last_results.get(position);
        }
        adapter.bind_view_holder(holder,data);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        QueryViewHolder adap = adapter.create_view_holder(parent,viewType);
        adap.setClick(this);
        return adap;
    }

    @Override
    public int getItemCount() {
        if(filtered_results!=null) {
            return filtered_results.size();
        }
        if(last_results!=null){
            return last_results.size();
        }
        return 0;
    }

    void filter_data() {
        filtered_results = last_results;
        if(last_results!=null) {
            if(filter!=null) {
                if(filter.effective()) {
                    filtered_results = new ArrayList();
                    for(Map<String,Object> doc:last_results) {
                        if(filter.match(doc)) {
                            filtered_results.add(doc);
                        }
                    }
                }
            }
        }
        ((Activity)this.cntx).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                notifyDataSetChanged();
            }
        });
    }

    @Override
    public void changed(LiveQuery.ChangeEvent event) {
        QueryEnumerator rows = event.getRows();
        if(rows.getCount()<1) {
            last_results = null;
        } else if(rows.getCount()==1) {
            last_results = (List<Map<String,Object>>) rows.next().getValue();
        } else if(rows.getCount()>1){
            last_results = new ArrayList<>();
            while(rows.hasNext()) {
                Map<String,Object> data = (Map<String, Object>) rows.next().getValue();
                last_results.add(data);
            }
        }
        filter_data();
    }
}
