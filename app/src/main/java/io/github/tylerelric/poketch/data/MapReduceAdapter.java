package io.github.tylerelric.poketch.data;

import android.app.Activity;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import com.couchbase.lite.Database;
import com.couchbase.lite.Emitter;
import com.couchbase.lite.LiveQuery;
import com.couchbase.lite.Mapper;
import com.couchbase.lite.Query;
import com.couchbase.lite.QueryEnumerator;
import com.couchbase.lite.QueryRow;
import com.couchbase.lite.Reducer;
import com.couchbase.lite.View;

import java.util.List;
import java.util.Map;

public abstract class MapReduceAdapter
extends RecyclerView.Adapter
implements LiveQuery.ChangeListener, Mapper, Reducer  {

    Database database;
    LiveQuery lq;
    QueryEnumerator current_data;
    protected final Context cntx;
    protected List<Map<String,Object>> last_results;

    public interface ResourceSelected {
        public void on_resource_selected(String uri);
    }

    protected MapReduceAdapter(Database db, String viewname, String version, Context c) {
        super();
        database = db;
        View view = db.getView(viewname);
        view.setMapReduce(this,this,version);
        Query q = view.createQuery();
        lq = q.toLiveQuery();
        lq.addChangeListener(this);
        cntx = c;
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
    public int getItemCount() {
        if(last_results==null){
            return 0;
        }
        return last_results.size();
    }

    @Override
    public void changed(LiveQuery.ChangeEvent event) {
        QueryEnumerator rows = event.getRows();
        QueryRow row = rows.next();
        if(row==null) {
        } else {
            last_results = (List<Map<String, Object>>) row.getValue();
            ((Activity)this.cntx).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    notifyDataSetChanged();
                }
            });
        }
    }
}
