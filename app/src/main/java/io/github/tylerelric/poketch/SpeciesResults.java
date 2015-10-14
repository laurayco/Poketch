package io.github.tylerelric.poketch;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.MenuItem;
import android.view.ViewGroup;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Emitter;
import com.couchbase.lite.Manager;
import com.couchbase.lite.android.AndroidContext;

import java.io.IOException;
import java.util.Map;

import io.github.tylerelric.poketch.data.DataFilter;
import io.github.tylerelric.poketch.data.MapReduceAdapter;
import io.github.tylerelric.poketch.data.PokeapiLoader;
import io.github.tylerelric.poketch.data.QueryViewAdapter;
import io.github.tylerelric.poketch.data.QueryViewHolder;
import io.github.tylerelric.poketch.data.ResourceSelected;

public class SpeciesResults extends AppCompatActivity implements ResourceSelected {
    DataFilter filter;
    MapReduceAdapter adapter;
    String origin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_species_results);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Intent intent = getIntent();
        String doc_uri = intent.getStringExtra("resource_uri");
        String search_field = intent.getStringExtra("query_field");
        origin = intent.getStringExtra("origin");
        if(intent.hasExtra("title")) {
            setTitle(intent.getStringExtra("title"));
        }

        try {
            Manager mgr = new Manager(new AndroidContext(this),Manager.DEFAULT_OPTIONS);
            PokeapiLoader pi = new PokeapiLoader(mgr);
            PokeapiLoader.Resource rsrc = null;
            if(search_field.equals("pokemon")) {
                rsrc = pi.get_pokemon();
            } else if(search_field.equals("moves")) {
                rsrc = pi.get_moves();
            }
            filter = new DataFilter(pi.get_database().getDocument(doc_uri).getProperties(),search_field);
            if(search_field.equals("pokemon")) {
                adapter = new SpeciesAdapter(pi.get_database(),this);
            } else {
                adapter = new MapReduceAdapter(rsrc,this) {
                    @Override
                    public void map(Map<String, Object> document, Emitter emitter) {
                        return;
                    }
                };
            }
            adapter.setFilter(filter);
            RecyclerView recycle = ((RecyclerView)findViewById(R.id.main_recycle_view));
            recycle.setLayoutManager(new LinearLayoutManager(this));
            recycle.setAdapter(adapter);
            adapter.set_on_resource_selected(this);
        } catch(IOException e) {
            Log.e("MainActivity", "Error getting database manager.", e);
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                Intent upIntent = NavUtils.getParentActivityIntent(this);
                Intent intent = getIntent();
                upIntent.putExtra("resource_uri",origin);
                if (NavUtils.shouldUpRecreateTask(this, upIntent)) {
                    // This activity is NOT part of this app's task, so create a new task
                    // when navigating up, with a synthesized back stack.
                    TaskStackBuilder.create(this)
                            // Add all of this activity's parents to the back stack
                            .addNextIntentWithParentStack(upIntent)
                                    // Navigate up to the closest parent
                            .startActivities();
                } else {
                    // This activity is part of this app's task, so simply
                    // navigate up to the logical parent activity.
                    NavUtils.navigateUpTo(this, upIntent);
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void on_resource_selected(Map<String, Object> data) {

    }
}
