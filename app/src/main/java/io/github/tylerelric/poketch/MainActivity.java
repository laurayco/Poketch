package io.github.tylerelric.poketch;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.Document;
import com.couchbase.lite.Emitter;
import com.couchbase.lite.Manager;
import com.couchbase.lite.Mapper;
import com.couchbase.lite.Reducer;
import com.couchbase.lite.View;
import com.couchbase.lite.android.AndroidContext;
import com.facebook.common.executors.CallerThreadExecutor;
import com.facebook.common.references.CloseableReference;
import com.facebook.datasource.DataSource;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.backends.pipeline.PipelineDraweeController;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.core.ImagePipeline;
import com.facebook.imagepipeline.datasource.BaseBitmapDataSubscriber;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.request.BasePostprocessor;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.facebook.stetho.Stetho;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import io.github.tylerelric.poketch.io.github.tylerelric.poketch.data.MapReduceAdapter;
import io.github.tylerelric.poketch.io.github.tylerelric.poketch.data.PokapiLoader;

public class MainActivity
extends AppCompatActivity
{

    PokapiLoader pi;
    Manager mgr = null;
    RecyclerView recycle;

    public class SpeciesViewHolder extends RecyclerView.ViewHolder {

        TextView lbl;
        SimpleDraweeView img;
        CardView cv;
        Palette.Builder pbuilder;
        BasePostprocessor img_process;
        ImagePipeline imagePipeline;
        Context cntx;

        public void update_species(Map<String, Object> doc){
            lbl.setText((String) doc.get("name"));
            String url = (String)doc.get("sprite");
            if(!(url instanceof String)) return;
            if(url.contains(".png")) {
                url = "http://pokeapi.co" + url;
            } else {
                url = "http://orig12.deviantart.net/19d6/f/2015/023/b/2/pokeball_wallpaper_1920x1080_by_seyahdoo-d8f1qaw.jpg";
            }
            Uri uri = Uri.parse(url);
            ImageRequestBuilder builder = ImageRequestBuilder.newBuilderWithSource(uri);
            ImageRequest req = builder.build();
            PipelineDraweeController controller = (PipelineDraweeController)
                    Fresco.newDraweeControllerBuilder()
                            .setImageRequest(builder.build())
                            .setOldController(img.getController())
                            .build();
            processImageWithPaletteApi(req,controller);
        }

        private void processImageWithPaletteApi(ImageRequest request, DraweeController controller) {
            DataSource<CloseableReference<CloseableImage>> dataSource =
                    imagePipeline.fetchDecodedImage(request, img.getContext());
            dataSource.subscribe(new BaseBitmapDataSubscriber() {
                @Override
                protected void onFailureImpl(DataSource<CloseableReference<CloseableImage>> dataSource) {

                }

                @Override protected void onNewResultImpl(@Nullable Bitmap bitmap) {
                    if(bitmap==null){
                        return;
                    }
                    Palette.Builder builder = new Palette.Builder(bitmap);
                    final Palette palette = builder.generate();
                    ((Activity)SpeciesViewHolder.this.cntx).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            int color = palette.getVibrantColor(0);
                            if (color == 0) {
                                color = palette.getMutedColor(0);
                            }
                            if (color == 0) {
                                color = 0xFF00FFFF;
                            }
                            cv.setCardBackgroundColor(color);
                        }
                    });
                }
            }, CallerThreadExecutor.getInstance());

            img.setController(controller);
        }

        public SpeciesViewHolder(android.view.View itemView,Context c ) {
            super(itemView);//http://pokeapi.co/media/img/1.png
            lbl = (TextView)itemView.findViewById(R.id.textView);
            img = (SimpleDraweeView)itemView.findViewById(R.id.species_list_image);
            cv = (CardView)itemView.findViewById(R.id.card_view);
            cntx = c;
            imagePipeline = Fresco.getImagePipeline();
            getAdapterPosition();
        }
    }

    class SpeciesAdapter
    extends MapReduceAdapter {

        protected SpeciesAdapter(Database db, Context c) {
            super(db, "home_view", "2", c);
        }

        @Override
        public void map(Map<String, Object> document, Emitter emitter) {
            Map<String,Object> no = new HashMap<String, Object>();
            if(document.containsKey("resource_name")) {
                String rsrc = (String)document.get("resource_name");
                if(rsrc.equals("pokemon")) {
                    List<Map<String,Object>> sprites = (List<Map<String,Object>>)document.get("sprites");
                    if(sprites==null)
                        return;
                    else if(sprites.size()<1)
                        return;
                    no.put("sprite",sprites.get(0).get("resource_uri"));
                    no.put("resource_uri",document.get("resource_uri"));
                    no.put("name",document.get("name"));
                    no.put("national_id",document.get("national_id"));
                    no.put("kind", "pokemon");
                    emitter.emit((String) document.get("resource_uri"), no);
                } else if(rsrc.equals("sprite")) {
                    no.put("url", document.get("image"));
                    no.put("kind", "sprite");
                    no.put("resource_uri", document.get("resource_uri"));
                    try {
                        Object rr = document.get("pokemon");
                        if(rr instanceof Map) {
                            Map<String,Map> ref = (Map<String, Map>) rr;
                            no.put("species",ref.get("resource_uri"));
                            emitter.emit(document.get("resource_uri"), no);
                        } else {
                            Log.i("MainActivity", "Species: " + ((String) no.get("species")));
                        }
                    } catch(ClassCastException ex) {
                        Log.e("MainActivity","Error casting.",ex);
                    }
                }
            }
        }

        @Override
        public Object reduce(List<Object> keys,List<Object> values,boolean rereduce) {
            Map<String,Object> new_things = new HashMap<>();
            List<Object> lumpsum = new ArrayList<>();
            for(Object o:values) {
                Map<String,Object> data = (Map<String,Object>)o;
                if(data==null) continue;
                String kind = (String)data.get("kind");
                if(kind==null) {
                    lumpsum.add(o);
                } else if (kind.equals("pokemon")) {
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
                    if(!(data.containsKey("kind")&&data.containsKey("national_id")))
                        return 1;
                    if(!data.get("kind").equals("complete"))
                        return 1;
                    id_a = (int)data.get("national_id");
                    data = (Map<String,Object>)rhs;
                    if(!(data.containsKey("kind")&&data.containsKey("national_id")))
                        return -1;
                    if(!data.get("kind").equals("complete"))
                        return -1;
                    id_b = (int)data.get("national_id");
                    return id_a - id_b;
                }
            });
            return lumpsum;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            android.view.View view = inflater.inflate(R.layout.species_listing, parent, false);
            RecyclerView.ViewHolder vh = new SpeciesViewHolder(view,cntx);
            return vh;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            SpeciesViewHolder svh = (SpeciesViewHolder)holder;
            if(holder==null)
                return;
            svh.update_species(last_results.get(position));
        }
    }

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
            pi = new PokapiLoader(mgr);
        } catch(IOException e) {
            Log.e("MainActivity", "Error getting database manager.", e);
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        new Runnable() {
            @Override
            public void run() {
                pi.load_all(new PokapiLoader.ResourceAvailable() {
                    @Override
                    public void response(boolean success) {
                        on_database_ready(pi.get_database());
                    }
                });
            }
        }.run();
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

    public void on_database_ready(Database db) {
        Log.i("MainActivity", "Database loaded. Documents: " + db.getName());
        final RecyclerView.Adapter adap = new SpeciesAdapter(db,this);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                recycle.setAdapter(adap);
            }
        });
    }

}
