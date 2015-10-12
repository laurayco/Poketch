package io.github.tylerelric.poketch.io.github.tylerelric.poketch.data;

import android.util.Log;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.Document;
import com.couchbase.lite.Emitter;
import com.couchbase.lite.Manager;
import com.couchbase.lite.Mapper;
import com.couchbase.lite.QueryEnumerator;
import com.couchbase.lite.QueryRow;
import com.couchbase.lite.TransactionalTask;
import com.couchbase.lite.UnsavedRevision;
import com.couchbase.lite.View;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import io.github.tylerelric.poketch.JSONUtilities;

public class PokapiLoader {

    final String server = "http://pokeapi.co";

    OkHttpClient web_client = new OkHttpClient();
    Database database = null;

    public interface ResourceAvailable {
        public void response(boolean success);
    }

    public class Resource
    implements Mapper{
        protected final String resource_name;
        protected final Database database;
        protected final View view;
        protected boolean loaded = false;

        protected Resource(String rsrc,Database db) {
            resource_name = rsrc;
            database = db;
            view = database.getView("all_"+resource_name);
            view.setMap(this,"1");
            //view.deleteIndex();
        }

        public void load(final OkHttpClient client, final ResourceAvailable ra) {
            // check to see if numbers match up.
            // if not, download bulk, store.
            Request req = (new Request.Builder()).url(build_url(false)).build();
            client.newCall(req).enqueue(new Callback() {
                @Override
                public void onFailure(Request request, IOException e) {
                    ra.response(false);
                }

                @Override
                public void onResponse(Response response) throws IOException {
                    try {
                        QueryEnumerator qe = view.createQuery().run();
                        //QueryEnumerator qe = database.createAllDocumentsQuery().run();
                        int existing = 0;
                        while(qe.hasNext()) {
                            Document doc = qe.next().getDocument();
                            if(!doc.getProperties().containsKey("resource_name"))
                                continue;
                            if(doc.getProperty("resource_name").equals(resource_name)) {
                                existing++;
                            }
                        }
                        Log.i("Resource","Load found " + existing + " total documents for " + resource_name + ".");
                        JSONObject jobj = new JSONObject(response.body().string());
                        JSONObject meta = jobj.getJSONObject("meta");
                        int server_count = meta.getInt("total_count");
                        if(existing<server_count) {
                            Request req = (new Request.Builder()).url(build_url(true)).build();
                            client.newCall(req).enqueue(new Callback() {
                                @Override
                                public void onFailure(Request request, IOException e) {
                                    ra.response(false);
                                }

                                @Override
                                public void onResponse(Response response) throws IOException {
                                    try {
                                        JSONObject jobj = new JSONObject(response.body().string());
                                        JSONArray data = jobj.getJSONArray("objects");
                                        for(int i=0;i<data.length();i++) {
                                            JSONObject o = data.getJSONObject(i);
                                            Map<String,Object> props = (Map<String, Object>) JSONUtilities.object_to_map(o);
                                            props.put("resource_name", resource_name);
                                            save_data(o.getString("resource_uri"), props);
                                        }
                                        loaded = true;
                                        ra.response(true);
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                        ra.response(false);
                                    } catch (CouchbaseLiteException e) {
                                        e.printStackTrace();
                                        ra.response(false);
                                    }
                                }
                            });
                        } else {
                            loaded = true;
                            ra.response(true);
                        }
                    } catch (CouchbaseLiteException e) {
                        e.printStackTrace();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        private String build_url(boolean download_all) {
            return server + "/api/v1/" + resource_name + "?limit=" + ((download_all)?0:1);
        }

        protected void save_data(String uid, final Map<String,Object> properties) throws CouchbaseLiteException {
            Document doc = database.getExistingDocument(uid);
            if(doc==null) {
                doc = database.getDocument(uid);
            }
            doc.update(new Document.DocumentUpdater() {
                @Override
                public boolean update(UnsavedRevision newRevision) {
                    newRevision.setUserProperties(properties);
                    return true;
                }
            });
        }

        @Override
        public void map(Map<String, Object> document, Emitter emitter) {
            if(!document.containsKey("resource_name")) return;
            String rsrckind = (String) document.get("resource_name");
            if(rsrckind!=null) {
                if(rsrckind.equals(resource_name)) {
                    emitter.emit(document.get("resource_uri"),document);
                }
            }
        }
    }

    public class Pokemon extends Resource {

        public Pokemon(Database db) throws CouchbaseLiteException {
            super("pokemon", db);
        }

        protected void download_species(final String rsrc_ui,final List<Map<String,Object>> storage, final ResourceAvailable ra) {
            String url = server + rsrc_ui;
            web_client.newCall(new Request.Builder().url(url).build()).enqueue(new Callback() {
                @Override
                public void onFailure(Request request, IOException e) {
                    Log.e("Pokemon","download_species() - miss:" + rsrc_ui, e);
                    ra.response(false);
                }

                @Override
                public void onResponse(final Response response) throws IOException {
                    Log.i("Pokemon", "download_species() - hit:" + rsrc_ui);
                    try {
                        JSONObject specdata = new JSONObject(response.body().string());
                        Map<String,Object> props = (Map<String, Object>) JSONUtilities.object_to_map(specdata);
                        props.put("resource_name","pokemon");
                        Log.i("PokeApi", (String) props.get("resource_name"));
                        storage.add(props);
                        ra.response(true);
                    } catch (JSONException e) {
                        Log.e("Pokemon", "download_species() " + rsrc_ui, e);
                        ra.response(false);
                    } catch (IOException e) {
                        Log.e("Pokemon", "download_species() " + rsrc_ui, e);
                        ra.response(false);
                    }
                }
            });
        }

        protected void download_missing_pokemon(Collection<String> download,final ResourceAvailable ra) {
            final List<Map<String,Object>> new_species_info = new ArrayList<>();
            ResourceAvailable all_species_downloaded = new ResourceAvailable() {
                @Override
                public void response(boolean success) {
                    if(!success) {
                        Log.i("PokeApi","Download of species failed!");
                        ra.response(false);
                    } else {
                        ra.response(database.runInTransaction(new TransactionalTask() {
                            @Override
                            public boolean run() {
                                for(Map<String,Object>props:new_species_info) {
                                    Document doc = database.getDocument((String) props.get("resource_uri"));
                                    try {
                                        doc.putProperties(props);
                                        Log.i("PokeApi", "Species stored:" + ((String) props.get("resource_uri")));
                                    } catch (CouchbaseLiteException e) {
                                        Log.e("PokeApiLoader", "download_missing_pokemon() - error", e);
                                        return false;
                                    }
                                }
                                Log.i("PokeApi","Download of species succeeded!");
                                return true;
                            }
                        }));
                    }
                }
            };
            WallBlock wb = new WallBlock(all_species_downloaded,download.size());
            for(String uri:download) {
                download_species(uri, new_species_info, wb);
            }
        }

        @Override
        public void load(final OkHttpClient web_client, final ResourceAvailable ra) {
            Document pkdx = database.getExistingDocument("pkdx");
            String pkdx_location = "/api/v1/pokedex/1";
            if(pkdx==null) {
                Log.i("Pokemon","load() - pkdx does not already exist. Building from scratch.");
                web_client.newCall(new Request.Builder().url(server+pkdx_location).build()).enqueue(new Callback() {
                    @Override
                    public void onFailure(Request request, IOException e) {
                        Log.e("Pokemon",".load() - fail",e);
                    }

                    @Override
                    public void onResponse(Response response) throws IOException {
                        try {
                            final JSONObject response_data = new JSONObject(response.body().string());
                            List<String> to_download = new ArrayList<String>();
                            try {
                                Map<String,Object> props = (Map<String, Object>) JSONUtilities.object_to_map(response_data);
                                Document pkdx = database.getDocument("pkdx");
                                Object spec_list[] = (Object[]) props.get("pokemon");
                                for(Object s:spec_list){
                                    Map<String,Object> spec = (Map<String,Object>)s;
                                    spec.put("resource_uri","/" + ((String)spec.get("resource_uri")));
                                    to_download.add((String)spec.get("resource_uri"));
                                }
                                pkdx.putProperties(props);
                            } catch (JSONException e) {
                                Log.e("Pokemon", ".load() - fail", e);
                                ra.response(false);
                            } catch (CouchbaseLiteException e) {
                                Log.e("Pokemon", ".load() - fail", e);
                                ra.response(false);
                            }
                            download_missing_pokemon(to_download, new ResourceAvailable() {
                                @Override
                                public void response(final boolean success) {
                                    ra.response(true);
                                }
                            });
                        } catch (JSONException e) {
                            Log.e("Pokemon", ".load() - fail", e);
                        }

                    }
                });
            } else {
                Log.i("Pokemon","load() - pkdx already exists. Downloading missing species.");
                try {
                    QueryEnumerator qe = database.createAllDocumentsQuery().run();
                    Set<String> set = new HashSet<String>();
                    List<Map<String,Object>> t = (List<Map<String,Object>>) pkdx.getProperty("pokemon");
                    for(Map<String,Object>r:t) {
                        set.add((String) r.get("resource_uri"));
                    }
                    for(QueryRow qr:qe) {
                        Map<String,Object> props = qr.getDocument().getProperties();
                        if(props.containsKey("resource_name")) {
                            String rsrc_kind = (String) props.get("resource_name");
                            if(rsrc_kind!=null) {
                                if(rsrc_kind.equals("pokemon")) {
                                    if(set.contains(props.get("resource_uri"))) {
                                        set.remove(props.get("resource_uri"));
                                    }
                                }
                            }
                        }
                    }
                    if(set.size()>0) {
                        download_missing_pokemon(set, ra);
                    } else {
                        ra.response(true);
                    }
                } catch (CouchbaseLiteException e) {
                    Log.e("Pokemon", ".load() - fail", e);
                }
            }
        }
    }

    public class PokemonType extends Resource {
        public PokemonType(Database db) throws CouchbaseLiteException {
            super("type",db);
        }
    }

    public class Moves extends Resource {
        public Moves(Database db) throws CouchbaseLiteException {
            super("move",db);
        }
    }

    public class Abilities extends Resource {
        public Abilities(Database db) throws CouchbaseLiteException {
            super("ability",db);
        }
    }

    public class EggGroups extends Resource {
        public EggGroups(Database db) throws CouchbaseLiteException {
            super("egg",db);
        }
    }

    public class Sprites extends Resource {
        public Sprites(Database db) throws CouchbaseLiteException {
            super("sprite",db);
        }
    }

    final protected Pokemon pokemon;
    final protected PokemonType pokemon_types;
    final protected Moves moves;
    final protected Abilities abilities;
    final protected EggGroups egg_groups;
    final protected Sprites sprites;

    protected class WallBlock implements ResourceAvailable {
        ResourceAvailable callback;
        int not_loaded = 0;
        public WallBlock(ResourceAvailable ra,int needed) {
            callback = ra;
            not_loaded = needed;
        }

        @Override
        public void response(boolean success) {
            Log.i("WallBlock","Num left " + not_loaded);
            if(!success) {
                not_loaded = -1;
                callback.response(false);
            }
            if(not_loaded>=1) {
                not_loaded--;
            } else if(not_loaded<0) {
                //errrr
            } if(not_loaded==0) {
                callback.response(true);
                not_loaded = -1;
            }
        }
    }

    public PokapiLoader(Manager mgr) throws CouchbaseLiteException {
        database = mgr.getDatabase("pkdx");
        pokemon = new Pokemon(database);
        pokemon_types = new PokemonType(database);
        moves = new Moves(database);
        abilities = new Abilities(database);
        egg_groups = new EggGroups(database);
        sprites = new Sprites(database);
    }

    public void load_all(final ResourceAvailable ra) {
        WallBlock wb = new WallBlock(new ResourceAvailable() {
            @Override
            public void response(boolean success) {
                Log.i("PokeApiLoader",".load_all() Finished!");
                ra.response(success);
            }
        }, 6);
        pokemon.load(web_client,wb);
        pokemon_types.load(web_client,wb);
        moves.load(web_client,wb);
        abilities.load(web_client,wb);
        egg_groups.load(web_client,wb);
        sprites.load(web_client,wb);
    }

    public void get_pokemon(ResourceAvailable ra){
        pokemon.load(web_client,ra);
    }

    public void get_pokemon_types(ResourceAvailable ra){
        pokemon_types.load(web_client,ra);
    }

    public void get_moves(ResourceAvailable ra){
        moves.load(web_client,ra);
    }

    public void get_abilities(ResourceAvailable ra){
        abilities.load(web_client,ra);
    }

    public void get_egg_groups(ResourceAvailable ra){
        egg_groups.load(web_client,ra);
    }

    public void get_sprites(ResourceAvailable ra){
        sprites.load(web_client,ra);
    }

    public Database get_database() {
        return database;
    }

}
