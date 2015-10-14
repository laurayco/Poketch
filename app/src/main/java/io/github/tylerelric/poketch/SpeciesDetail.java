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
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Manager;
import com.couchbase.lite.android.AndroidContext;
import com.facebook.common.executors.CallerThreadExecutor;
import com.facebook.common.references.CloseableReference;
import com.facebook.common.util.UriUtil;
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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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


    CardView details;
    TextView details_txt;

    private static final Map<String,Integer> type_icons = new HashMap<String,Integer>();
    static {
        type_icons.put("bug",R.drawable.bug);
        type_icons.put("dark",R.drawable.dark);
        type_icons.put("dragon",R.drawable.dragon);
        type_icons.put("electric",R.drawable.electric);
        type_icons.put("fairy",R.drawable.fairy);
        type_icons.put("fighting",R.drawable.fighting);
        type_icons.put("fire",R.drawable.fire);
        type_icons.put("flying",R.drawable.flying);
        type_icons.put("ghost",R.drawable.ghost);
        type_icons.put("grass",R.drawable.grass);
        type_icons.put("ground",R.drawable.ground);
        type_icons.put("ice",R.drawable.ice);
        type_icons.put("normal",R.drawable.normal);
        type_icons.put("poison",R.drawable.poison);
        type_icons.put("psychic",R.drawable.psycic);
        type_icons.put("rock",R.drawable.rock);
        type_icons.put("steel",R.drawable.steel);
        type_icons.put("water",R.drawable.water);
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

        try {
            mgr = new Manager(new AndroidContext(this),Manager.DEFAULT_OPTIONS);
            pi = new PokeapiLoader(mgr);
        } catch(IOException e) {
            Log.e("MainActivity", "Error getting database manager.", e);
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        }

        findViewById(R.id.breed_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launch_breeds();
            }
        });

        findViewById((R.id.move_button)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launch_moves();
            }
        });

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

    protected void launch_breeds() {
        List<Object> eggs = (List<Object>) species_data.get("egg_groups");
        Log.i("SpeciesDetail","Launch Breeds");
        if(eggs!=null) {
            Intent intent = new Intent(getApplicationContext(),SpeciesResults.class);
            Map<String,Object> data = (Map<String,Object>)eggs.get(0);
            intent.putExtra("resource_uri",data.get("resource_uri").toString());
            intent.putExtra("query_field","pokemon");
            intent.putExtra("origin",species_data.get("resource_uri").toString());
            intent.putExtra("title","Breeding for " + species_data.get("name").toString());
            startActivity(intent);
        }
    }

    protected void launch_moves() {
        Intent intent = new Intent(getApplicationContext(),SpeciesResults.class);
        intent.putExtra("resource_uri",species_data.get("resource_uri").toString());
        intent.putExtra("query_field","moves");
        intent.putExtra("origin",species_data.get("resource_uri").toString());
        intent.putExtra("title","Moveset for " + species_data.get("name").toString());
        startActivity(intent);
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
        LayoutInflater inflater = getLayoutInflater();

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

        LinearLayout types_layout = (LinearLayout) findViewById(R.id.species_detail_type_information);
        List<Object> types = (List<Object>) species_data.get("types");
        for(Object o:types) {
            Map<String,Object> dat = (Map<String,Object>)o;
            if(dat!=null) {
                LinearLayout type_layout = (LinearLayout) inflater.inflate(R.layout.pokemon_type_list_item,null);
                String type_name = dat.get("name").toString();
                Uri uri = new Uri.Builder().scheme(UriUtil.LOCAL_RESOURCE_SCHEME).path(String.valueOf(type_icons.get(type_name))).build();
                ((SimpleDraweeView)type_layout.findViewById(R.id.type_sprite_holder)).setImageURI(uri);
                ((TextView)type_layout.findViewById(R.id.species_type_list_label)).setText(type_name.substring(0,1).toUpperCase()+type_name.substring(1));
                types_layout.addView(type_layout);
            }
        }


        hp.setText(species_data.get("hp").toString());
        atk.setText(species_data.get("attack").toString());
        def.setText(species_data.get("defense").toString());
        spatk.setText(species_data.get("sp_atk").toString());
        spdef.setText(species_data.get("sp_def").toString());
        speed.setText(species_data.get("speed").toString());

        List<Object> egg_groups = (List<Object>) species_data.get("egg_groups");
        TableRow egg_group_container = new TableRow(this);
        for(Object o:egg_groups) {
            Map<String,Object> dat = (Map<String,Object>)o;
            if(o!=null) {
                String egg_name = dat.get("name").toString();
                View nv = inflater.inflate(R.layout.egg_group_info_item, null);
                TextView tv = (TextView)nv.findViewById(R.id.egg_group_name);
                tv.setText(egg_name);
                egg_group_container.addView(nv);
            }
        }
        ((TableLayout)findViewById(R.id.egg_group_info_container)).addView(egg_group_container);

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
    }

}
