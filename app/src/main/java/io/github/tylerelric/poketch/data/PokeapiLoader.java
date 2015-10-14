package io.github.tylerelric.poketch.data;

import android.util.Log;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.Document;
import com.couchbase.lite.Emitter;
import com.couchbase.lite.Manager;
import com.couchbase.lite.Mapper;
import com.couchbase.lite.QueryEnumerator;
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
import java.util.List;
import java.util.Map;

public class PokeapiLoader {

    public static final String server = "http://pokeapi.co";

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
            view.setMap(this, "1");
        }

        protected Callback resource_page_download(final ResourceAvailable ra, final List<Map<String,Object>> store) {
            return new Callback() {
                @Override
                public void onFailure(Request request, IOException e) {
                    Log.e("PokeApi","Resource.resource_page_download failed",e);
                    ra.response(false);
                }

                @Override
                public void onResponse(Response response) throws IOException {
                    try {
                        JSONObject jobj = new JSONObject(response.body().string());
                        JSONArray data = jobj.getJSONArray("objects");
                        JSONObject meta = jobj.getJSONObject("meta");
                        for (int i = 0; i < data.length(); i++) {
                            JSONObject o = data.getJSONObject(i);
                            Map<String, Object> props = (Map<String, Object>) JSONUtilities.object_to_map(o);
                            props.put("resource_name", resource_name);
                            store.add(props);
                        }
                        data = null;
                        jobj = null;
                        if(!meta.isNull("next")) {
                            String url = server + meta.getString("next");
                            Log.i("PokeApi","continuing page to " + url);
                            web_client.newCall(new Request.Builder().url(url).build()).enqueue(
                                    resource_page_download(ra, store)
                            );
                        } else {
                            Log.i("PokeApi","All " + resource_name + " have been downloaded.");
                            ra.response(true);
                        }
                    } catch (JSONException e) {
                        Log.i("PokeApi",response.body().string());
                        Log.e("PokeApi","Resource.resource_page_download failed",e);
                        ra.response(false);
                    }
                }
            };
        }

        public void load(final OkHttpClient client, final ResourceAvailable ra) {
            // check to see if numbers match up.
            // if not, download bulk, store.
            if(loaded) {
                ra.response(true);
            }
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
                        int existing = qe.getCount();
                        Log.i("Resource", "Load found " + existing + " total documents for " + resource_name + ".");
                        qe = null;
                        JSONObject jobj = new JSONObject(response.body().string());
                        JSONObject meta = jobj.getJSONObject("meta");
                        int server_count = meta.getInt("total_count");
                        jobj = meta = null;
                        final List<Map<String,Object>> ndocs = new ArrayList<Map<String, Object>>();
                        if (existing < server_count) {
                            Request req = (new Request.Builder()).url(build_url(true)).build();
                            client.newCall(req).enqueue(resource_page_download(
                                    new ResourceAvailable() {
                                        @Override
                                        public void response(boolean success) {
                                            ra.response(loaded=database.runInTransaction(new TransactionalTask() {
                                                @Override
                                                public boolean run() {
                                                    for(Map<String,Object> props:ndocs) {
                                                        try {
                                                            database.getDocument((String)props.get("resource_uri")).putProperties(props);
                                                            Log.i("PokeApi","Resource.load() stored " + props.get("resource_uri").toString());
                                                        } catch (CouchbaseLiteException e) {
                                                            Log.e("PokeApi", "Resource.load() fail", e);
                                                            ra.response(false);
                                                            return false;
                                                        }
                                                    }
                                                    return true;
                                                }
                                            }));
                                        }
                                    },
                                    ndocs
                            ));
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

        protected int page_limit = 0;

        private String build_url(boolean download) {
            if(download) {
                return server + "/api/v1/" + resource_name + "?limit=" + page_limit;
            } else {
                return server + "/api/v1/" + resource_name + "?meta_only=true";
            }
        }

        protected void save_data(String uid, final Map<String,Object> properties) throws CouchbaseLiteException {
            Document doc = database.getDocument(uid);
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
            page_limit = 50;
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

    public class Descriptions extends Resource {
        public Descriptions(Database db) {
            super("description",db);
        }
    }

    final protected Pokemon pokemon;
    final protected PokemonType pokemon_types;
    final protected Moves moves;
    final protected Abilities abilities;
    final protected EggGroups egg_groups;
    final protected Sprites sprites;
    final protected Descriptions descriptions;

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

    public PokeapiLoader(Manager mgr) throws CouchbaseLiteException {
        database = mgr.getDatabase("pkdx");
        pokemon = new Pokemon(database);
        pokemon_types = new PokemonType(database);
        moves = new Moves(database);
        abilities = new Abilities(database);
        egg_groups = new EggGroups(database);
        sprites = new Sprites(database);
        descriptions = new Descriptions(database);
    }

    public void load_all(final ResourceAvailable ra) {
        WallBlock wb = new WallBlock(new ResourceAvailable() {
            @Override
            public void response(boolean success) {
                Log.i("PokeApiLoader",".load_all() Finished!");
                ra.response(success);
            }
        }, 7);
        pokemon.load(web_client,wb);
        pokemon_types.load(web_client,wb);
        moves.load(web_client,wb);
        abilities.load(web_client,wb);
        egg_groups.load(web_client,wb);
        sprites.load(web_client,wb);
        descriptions.load(web_client,wb);
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
