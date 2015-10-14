package io.github.tylerelric.poketch;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.couchbase.lite.Database;
import com.couchbase.lite.Emitter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.tylerelric.poketch.data.MapReduceAdapter;
import io.github.tylerelric.poketch.data.ResourceSelected;

/**
 * Created by tyler on 10/14/15.
 */
class SpeciesAdapter
        extends MapReduceAdapter implements ResourceSelected {

    protected SpeciesAdapter(Database db, Context c) {
        super(db, "home_view", "2", new SpeciesViewAdapter(), c);
    }

    @Override
    public void on_resource_selected(Map<String, Object> data) {
        super.on_resource_selected(data);
        Intent nintent = new Intent(cntx, SpeciesDetail.class);
        nintent.putExtra("resource_uri",data.get("resource_uri").toString());
        cntx.startActivity(nintent);
    }

    @Override
    public void map(Map<String, Object> document, Emitter emitter) {
        Map<String, Object> no = new HashMap<String, Object>();
        if (document.containsKey("resource_name")) {
            String rsrc = (String) document.get("resource_name");
            if (rsrc.equals("pokemon")) {
                List<Map<String, Object>> sprites = (List<Map<String, Object>>) document.get("sprites");
                if (sprites == null)
                    return;
                else if (sprites.size() < 1)
                    return;
                no.put("sprite", sprites.get(0).get("resource_uri"));
                no.put("resource_uri", document.get("resource_uri"));
                no.put("name", document.get("name"));
                no.put("national_id", document.get("national_id"));
                no.put("kind", "pokemon");
                emitter.emit((String) document.get("resource_uri"), no);
            } else if (rsrc.equals("sprite")) {
                no.put("url", document.get("image"));
                no.put("kind", "sprite");
                no.put("resource_uri", document.get("resource_uri"));
                try {
                    Object rr = document.get("pokemon");
                    if (rr instanceof Map) {
                        Map<String, Map> ref = (Map<String, Map>) rr;
                        no.put("species", ref.get("resource_uri"));
                        emitter.emit(document.get("resource_uri"), no);
                    } else {
                        Log.i("MainActivity", "Species: " + ((String) no.get("species")));
                    }
                } catch (ClassCastException ex) {
                    Log.e("MainActivity", "Error casting.", ex);
                }
            }
        }
    }

    @Override
    public Object reduce(List<Object> keys, List<Object> values, boolean rereduce) {
        Map<String, Object> new_things = new HashMap<>();
        List<Object> lumpsum = new ArrayList<>();
        for (Object o : values) {
            Map<String, Object> data = (Map<String, Object>) o;
            if (data == null) continue;
            String kind = (String) data.get("kind");
            if (kind == null) {
                lumpsum.add(o);
            } else if (kind.equals("pokemon")) {
                String rsrc = (String) data.get("resource_uri");
                if (new_things.containsKey(rsrc)) {
                    Map<String, Object> td = (Map<String, Object>) new_things.get(rsrc);
                    for (String k : data.keySet()) {
                        if (k == "sprite") continue;
                        td.put(k, data.get(k));
                    }
                    if (td.containsKey("sprite")) {
                        td.put("kind", "complete");
                        lumpsum.add(td);
                    }
                } else if (keys.contains(data.get("sprite"))) {
                    new_things.put(rsrc, data);
                } else {
                    lumpsum.add(data);
                }
            } else if (kind.equals("sprite")) {
                String species = (String) data.get("species");
                if (new_things.containsKey(species)) {
                    Map<String, Object> specdata = (Map<String, Object>) new_things.get(species);
                    specdata.put("sprite", data.get("url"));
                    specdata.put("kind", "complete");
                    lumpsum.add(specdata);
                } else if (keys.contains(species)) {
                    Map<String, Object> x = new HashMap<>();
                    x.put("sprite", data.get("url"));
                    new_things.put(species, x);
                } else {
                    lumpsum.add(data);
                }
            } else if (kind == "complete") {
                lumpsum.add(data);
            }
        }
        Collections.sort(lumpsum, new Comparator() {
            @Override
            public int compare(Object lhs, Object rhs) {
                int id_a = 0;
                int id_b = 0;
                Map<String, Object> data = (Map<String, Object>) lhs;
                if (!(data.containsKey("kind") && data.containsKey("national_id")))
                    return 1;
                if (!data.get("kind").equals("complete"))
                    return 1;
                id_a = (int) data.get("national_id");
                data = (Map<String, Object>) rhs;
                if (!(data.containsKey("kind") && data.containsKey("national_id")))
                    return -1;
                if (!data.get("kind").equals("complete"))
                    return -1;
                id_b = (int) data.get("national_id");
                return id_a - id_b;
            }
        });
        return lumpsum;
    }

}
