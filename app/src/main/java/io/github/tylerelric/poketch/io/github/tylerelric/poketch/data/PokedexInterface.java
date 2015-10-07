package io.github.tylerelric.poketch.io.github.tylerelric.poketch.data;

import android.util.Log;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.Document;
import com.couchbase.lite.Manager;
import com.couchbase.lite.QueryEnumerator;
import com.couchbase.lite.QueryOptions;
import com.couchbase.lite.android.AndroidContext;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.github.tylerelric.poketch.JSONUtilities;

public class PokedexInterface
implements Callback {

    OkHttpClient web_client = new OkHttpClient();
    Manager manager = null;
    Database database = null;
    JSONObject temp_pkdx;
    List<Map<String,Object>> pending_species;

    public PokedexDatabaseHandler getPkdx() {
        return pkdx;
    }

    public void setPkdx(PokedexDatabaseHandler pkdx) {
        this.pkdx = pkdx;
    }

    PokedexDatabaseHandler pkdx;

    public interface PokedexDatabaseHandler {
        void on_database_ready(Database db);
        void on_database_progress(Map<String, Object> m, int done,int total);
    }

    public PokedexInterface(Manager m) throws IOException{
        manager = m;
    }

    public void get_pkdx_database() {
        try {
            database = manager.getDatabase("pkdx");
            if (database.getExistingDocument("pokedex")==null) {
                seed_database();
            }
            check_sprites();
            pkdx.on_database_ready(database);
        } catch(CouchbaseLiteException ex) {
            Log.e("PokedexData", "Couchbase Lite", ex);
        }
    }

    private void check_sprites() {
        try {
            QueryEnumerator qe = database.createAllDocumentsQuery().run();
            while(qe.hasNext()) {
                Map<String,Object> data = qe.next().getDocument().getProperties();
                String kind = (String)data.get("resource_uri");
                if(kind.contains("/pokemon/")) {
                    List<Map<String,Object>> sprites = (List<Map<String,Object>>)data.get("sprites");
                    for(Map<String,Object> sprite:sprites) {
                        final String uri = (String)sprite.get("resource_uri");
                        if(database.getExistingDocument(uri)==null) {
                            Request req = new Request.Builder().url("http://pokeapi.co/" + uri).build();
                            web_client.newCall(req).enqueue(new Callback() {
                                @Override
                                public void onFailure(Request request, IOException e) {
                                    //
                                }

                                @Override
                                public void onResponse(Response response) throws IOException {
                                    Document doc = database.getDocument(uri);
                                    try {
                                        JSONObject jobj = new JSONObject(response.body().string());
                                        Map<String,Object> props = (Map<String,Object>) JSONUtilities.object_to_map(jobj);
                                        doc.putProperties(props);
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    } catch (CouchbaseLiteException e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                        }
                    }
                }
            }
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        }
    }

    void seed_database() {
        Request req = new Request.Builder().url("http://pokeapi.co/api/v1/pokedex/1/").build();
        final PokedexInterface pi = this;
        web_client.newCall(req).enqueue(new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                //durp.
                Log.e("PokedexData","Data downloading",e);
            }

            @Override
            public void onResponse(Response response) throws IOException {
                try {
                    temp_pkdx = new JSONObject(response.body().string());
                    JSONArray arr = temp_pkdx.getJSONArray("pokemon");
                    pending_species = new ArrayList<Map<String,Object>>();
                    for(int i=0;i<arr.length();i++) {
                        String url = "http://pokeapi.co/" + arr.getJSONObject(i).getString("resource_uri");
                        Request req = new Request.Builder().url(url).build();
                        web_client.newCall(req).enqueue(pi);
                    }
                } catch(JSONException ex) {
                    Log.e("PokedexData","JSON Parsing",ex);
                }
            }
        });
    }

    void save_data() {
        for(Map<String,Object> map:pending_species) {
            try {
                Document doc = database.getDocument((String)map.get("resource_uri"));
                doc.putProperties(map);
            } catch (CouchbaseLiteException e) {
                e.printStackTrace();
            }
        }
        try {
            Map<String,Object> pkdx_map = (Map<String, Object>) JSONUtilities.object_to_map(temp_pkdx);
            Document doc = database.getDocument("pokedex");
            doc.putProperties(pkdx_map);
            pkdx.on_database_ready(database);
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onFailure(Request request, IOException e) {
        Log.e("PokedexData","Data downloading",e);
    }

    @Override
    public void onResponse(Response response) throws IOException {
        try {
            JSONObject species_data = new JSONObject(response.body().string());
            Map<String,Object> m = (Map<String, Object>) JSONUtilities.object_to_map(species_data);
            pending_species.add(m);
            if(pending_species.size()>=temp_pkdx.getJSONArray("pokemon").length()) {
                Log.i("MainActivity","Finished downloading all of it.");
                save_data();
            }
            pkdx.on_database_progress(m, pending_species.size(), temp_pkdx.getJSONArray("pokemon").length());
        } catch (JSONException ex) {
            Log.e("MainActivity", "Error parsing JSON",ex);
        } catch (ClassCastException ex) {
            Log.e("MainActivity", "Error casting object.", ex);
        }
    }


}
