package io.github.tylerelric.poketch;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.couchbase.lite.Database;
import com.couchbase.lite.Document;
import com.couchbase.lite.Emitter;
import com.couchbase.lite.Manager;
import com.couchbase.lite.Mapper;
import com.couchbase.lite.Reducer;
import com.couchbase.lite.View;
import com.couchbase.lite.android.AndroidContext;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.stetho.Stetho;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.tylerelric.poketch.io.github.tylerelric.poketch.data.PokedexInterface;
import io.github.tylerelric.poketch.io.github.tylerelric.poketch.data.SpeciesAdapter;

public class MainActivity
extends AppCompatActivity
implements PokedexInterface.PokedexDatabaseHandler, Mapper, Reducer {

    PokedexInterface pi;
    Manager mgr = null;
    Database pkdx = null;
    RecyclerView recycle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fresco.initialize(this);
        setContentView(R.layout.activity_main);
        recycle = (RecyclerView)findViewById(R.id.main_recycle_view);
        recycle.setLayoutManager(new LinearLayoutManager(this));
        try {
            Stetho.initializeWithDefaults(this);
            mgr = new Manager(new AndroidContext(this),Manager.DEFAULT_OPTIONS);
            pi = new PokedexInterface(mgr);
            pi.setPkdx(this);
            pi.get_pkdx_database();
        } catch(IOException e) {
            Log.e("MainActivity", "Error getting database manager.", e);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void on_database_ready(Database db) {
        Log.i("MainActivity","Database loaded. Documents: " + db.getName());
        pkdx = db;
        com.couchbase.lite.View data_view = db.getView("species_overview");
        data_view.setMapReduce(this,this,"14");
        recycle.setAdapter(new SpeciesAdapter(db, data_view, this));
    }

    @Override
    public void on_database_progress(Map<String, Object> m, int done, int total) {
        Log.i("MainActivity","Database downloaded " + m.get("name"));
        float progress = (float)done / total;
        Log.i("MainActivity","Database download progress: " + progress*100);
    }

    @Override
    public void map(Map<String, Object> document, Emitter emitter) {
        Map<String,Object> no = new HashMap<String, Object>();
        String kind = null;
        String rsrc = (String)document.get("resource_uri");
        if(rsrc==null) {
            return;
        }
        if(rsrc.contains("/pokemon/")) {
            kind = "species";
        } else if(rsrc.contains("/sprite/")) {
            kind = "sprite";
        }
        if(kind=="species") {
            no.put("resource_uri",document.get("resource_uri"));
            no.put("kind","species");
            no.put("name",document.get("name"));
            no.put("national_id",document.get("national_id"));
            List<Map<String,Object>> sprites = (List<Map<String,Object>>)document.get("sprites");
            if(sprites.size()>0) {
                no.put("sprite",sprites.get(0).get("resource_uri"));
            } else {
                no.put("sprite",null);
            }
            emitter.emit((String)document.get("resource_uri"),no);
        } else if(kind=="sprite") {
            no.put("url", document.get("image"));
            no.put("kind", "sprite");
            no.put("resource_uri", document.get("resource_uri"));
            try {
                Object rr = document.get("pokemon");
                if(rr instanceof Map) {
                    Map<String,Map> ref = (Map<String, Map>) document.get("pokemon");
                    no.put("species",ref.get("resource_uri"));
                } else {
                    Log.i("MainActivity", "Species: " + ((String) no.get("species")));
                }
            } catch(ClassCastException ex) {
                Log.e("MainActivity","Error casting.",ex);
            }
            emitter.emit(document.get("resource_uri"), no);
        }
    }

    @Override
    public Object reduce(List<Object> keys, List<Object> values, boolean rereduce) {
        Map<String,Object> new_things = new HashMap<>();
        List<Object> lumpsum = new ArrayList<>();
        for(Object o:values) {
            Map<String,Object> data = (Map<String,Object>)o;
            if(data==null) continue;
            String kind = (String)data.get("kind");
            if(kind==null) {
                lumpsum.add(o);
            } else if (kind.equals("species")) {
                String rsrc = (String)data.get("resource_uri");
                if(new_things.containsKey(rsrc)) {
                    Map<String,Object> td = (Map<String, Object>) new_things.get(rsrc);
                    for(String k:data.keySet()) {
                        if(k=="sprite") continue;
                        td.put(k,data.get(k));
                    }
                    if(td.containsKey("sprite")) {
                        td.put("kind","complete");
                        lumpsum.add(td);
                    }
                } else if(keys.contains(data.get("sprite"))) {
                    new_things.put(rsrc,data);
                } else {
                    lumpsum.add(data);
                }
            } else if (kind.equals("sprite")) {
                String species = (String)data.get("species");
                if(new_things.containsKey(species)) {
                    Map<String,Object> specdata = (Map<String, Object>) new_things.get(species);
                    specdata.put("sprite",data.get("url"));
                    specdata.put("kind","complete");
                    lumpsum.add(specdata);
                } else if(keys.contains(species)) {
                    Map<String,Object> x = new HashMap<>();
                    x.put("sprite",data.get("url"));
                    new_things.put(species,x);
                } else {
                    lumpsum.add(data);
                }
            } else if (kind=="complete") {
                lumpsum.add(data);
            }
        }
        Collections.sort(lumpsum, new Comparator() {
            @Override
            public int compare(Object lhs, Object rhs) {
                int id_a = 0;
                int id_b = 0;
                Map<String,Object> data = (Map<String,Object>)lhs;
                if(!(((String)data.get("kind")).equals("complete"))) {
                    return -1;
                }
                id_a = (int)data.get("national_id");
                data = (Map<String,Object>)rhs;
                if(!(((String)data.get("kind")).equals("complete"))) {
                    return -1;
                }
                id_b = (int)data.get("national_id");
                return id_a - id_b;
            }
        });
        return lumpsum;
    }
}
