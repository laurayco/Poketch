package io.github.tylerelric.poketch;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.couchbase.lite.Emitter;

import java.util.Map;

import io.github.tylerelric.poketch.data.MapReduceAdapter;
import io.github.tylerelric.poketch.data.PokeapiLoader;
import io.github.tylerelric.poketch.data.QueryViewAdapter;
import io.github.tylerelric.poketch.data.QueryViewHolder;

public class MoveViewAdapter extends QueryViewAdapter {

    @Override
    public QueryViewHolder create_holder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        android.view.View view = inflater.inflate(R.layout.move_list_item, parent, false);
        return new MoveViewHolder(view);
    }
}
