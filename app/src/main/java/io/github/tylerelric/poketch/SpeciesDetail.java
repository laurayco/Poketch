package io.github.tylerelric.poketch;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.Adapter;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Manager;
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
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;

import org.w3c.dom.Text;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import io.github.tylerelric.poketch.data.PokeapiLoader;

public class SpeciesDetail extends AppCompatActivity
{

    PokeapiLoader pi;
    Manager mgr = null;
    Map<String,Object> species_data;
    Map<String,Object> species_sprite;
    String species_uri;
    ImagePipeline imagePipeline;
    AbilityAdapter ability_adapter;

    CardView details;
    TextView details_txt;

    class AbilityListViewHolder extends RecyclerView.ViewHolder {

        TextView ability_name;

        public AbilityListViewHolder(View itemView) {
            super(itemView);
            ability_name = (TextView)itemView.findViewById(R.id.species_ability_list_item_name);
        }

        public void update(Map<String,Object> data) {
            ability_name.setText(data.get("name").toString());
        }
    }

    class AbilityAdapter
    extends RecyclerView.Adapter {

        List<Map<String,Object>> data = new ArrayList<Map<String,Object>>();

        public void setData(List<Map<String,Object>> data) {
            this.data = data;
            notifyDataSetChanged();
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = (LayoutInflater) SpeciesDetail.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View convertView = inflater.inflate(R.layout.species_ability_list_item,parent,false);
            return new AbilityListViewHolder(convertView);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            ((AbilityListViewHolder)holder).update(data.get(position));
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_species_detail);
        imagePipeline = Fresco.getImagePipeline();

        Toolbar toolbar = (Toolbar) findViewById(R.id.anim_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        details = (CardView) findViewById(R.id.species_description);
        details_txt = (TextView) details.findViewById(R.id.species_description_text);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show();
            }
        });

        Intent intent = getIntent();
        species_uri = intent.getStringExtra("resource_uri");
        Log.i("SpeciesDetail", species_uri);

        try {
            mgr = new Manager(new AndroidContext(this),Manager.DEFAULT_OPTIONS);
            pi = new PokeapiLoader(mgr);
        } catch(IOException e) {
            Log.e("MainActivity", "Error getting database manager.", e);
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        }
    }

    private void processImageWithPaletteApi(ImageRequest request, DraweeController controller) {
        SimpleDraweeView img = (SimpleDraweeView)findViewById(R.id.sprite_holder);
        DataSource<CloseableReference<CloseableImage>> dataSource =
                imagePipeline.fetchDecodedImage(request, img.getContext());
        dataSource.subscribe(new BaseBitmapDataSubscriber() {
            @Override
            protected void onFailureImpl(DataSource<CloseableReference<CloseableImage>> dataSource) {

            }

            @Override
            protected void onNewResultImpl(@Nullable Bitmap bitmap) {
                if (bitmap == null) {
                    return;
                }
                Palette.Builder builder = new Palette.Builder(bitmap);
                final Palette palette = builder.generate();
                ((Activity) SpeciesDetail.this).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        int color = palette.getVibrantColor(0);
                        if (color == 0) {
                            color = palette.getMutedColor(0);
                        }
                        if (color == 0) {
                            color = 0xFF00FFFF;
                        }
                        findViewById(R.id.sprite_holder).setBackgroundColor(color);
                        ((CollapsingToolbarLayout) findViewById(R.id.collapsing_toolbar)).setBackgroundColor(color);
                    }
                });
            }
        }, CallerThreadExecutor.getInstance());

        img.setController(controller);
    }

    protected void show_species() {
        SimpleDraweeView sprite = ((SimpleDraweeView)findViewById(R.id.sprite_holder));
        TextView atk = ((TextView)findViewById(R.id.species_stats_atk)),
                def = ((TextView)findViewById(R.id.species_stats_def)),
                spatk = ((TextView)findViewById(R.id.species_stats_spatk)),
                spdef = ((TextView)findViewById(R.id.species_stats_spdef)),
                speed = ((TextView)findViewById(R.id.species_stats_speed)),
                hp = ((TextView)findViewById(R.id.species_stats_hp));

        List<Object> descriptions_ = (List<Object>)species_data.get("descriptions");
        Map<String,Object> desc = (Map<String, Object>) descriptions_.get(descriptions_.size()-1);
        String desc_uri = (String) desc.get("resource_uri");
        details_txt.setText(pi.get_database().getDocument(desc_uri).getProperty("description").toString());

        List<Object> abilities = (List<Object>) species_data.get("abilities");
        LinearLayout ability_list = ((LinearLayout)findViewById(R.id.species_ability_list));
        LayoutInflater inflater = getLayoutInflater();
        for(Object o:abilities) {
            Map<String,Object> dat = (Map<String,Object>)o;
            if(dat!=null) {
                Map<String,Object> data = pi.get_database().getDocument(dat.get("resource_uri").toString()).getProperties();
                View nv = inflater.inflate(R.layout.species_ability_list_item,null);
                TextView tv = (TextView)nv.findViewById(R.id.species_ability_list_item_name);
                tv.setText(data.get("name").toString());
                ability_list.addView(nv);
            }
        }

        hp.setText(species_data.get("hp").toString());
        atk.setText(species_data.get("attack").toString());
        def.setText(species_data.get("defense").toString());
        spatk.setText(species_data.get("sp_atk").toString());
        spdef.setText(species_data.get("sp_def").toString());
        speed.setText(species_data.get("speed").toString());

        String url = PokeapiLoader.server + ((String) species_sprite.get("image"));
        Log.i("SpeciesDetail",url);
        ((Toolbar) findViewById(R.id.anim_toolbar)).setTitle((String)species_data.get("name"));
        ((CollapsingToolbarLayout) findViewById(R.id.collapsing_toolbar)).setTitle((String)species_data.get("name"));
        Uri uri = Uri.parse(url);
        ImageRequestBuilder builder = ImageRequestBuilder.newBuilderWithSource(uri);
        ImageRequest req = builder.build();
        PipelineDraweeController controller = (PipelineDraweeController)
            Fresco.newDraweeControllerBuilder()
                .setImageRequest(builder.build())
                .setOldController(sprite.getController())
                .build();
        processImageWithPaletteApi(req, controller);
        sprite.setImageURI(uri);
    }

    @Override
    public void onStart() {
        super.onStart();
        new Thread() {
            @Override
            public void run() {
            species_data = (Map<String, Object>) pi.get_database().getDocument(species_uri).getProperties();
            List<Object> sprites = (List<Object>) species_data.get("sprites");
            Map<String,Object> sprite_data = (Map<String, Object>) sprites.get(0);
            String sprite_uri = (String) sprite_data.get("resource_uri");
            species_sprite = (Map<String,Object>) pi.get_database().getDocument(sprite_uri).getProperties();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    show_species();
                }
            });
            }
        }.start();
    }

}
