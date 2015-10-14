package io.github.tylerelric.poketch;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Manager;
import com.couchbase.lite.android.AndroidContext;
import com.facebook.drawee.backends.pipeline.Fresco;

import java.io.IOException;
import java.util.Map;

import io.github.tylerelric.poketch.data.PokeapiLoader;

public class MainActivity
extends AppCompatActivity
{

    PokeapiLoader pi;
    Manager mgr = null;
    RecyclerView recycle;
    SpeciesAdapter data_adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fresco.initialize(this);
        setContentView(R.layout.activity_main);
        recycle = (RecyclerView)findViewById(R.id.main_recycle_view);
        recycle.setLayoutManager(new LinearLayoutManager(this));
        try {
            mgr = new Manager(new AndroidContext(this),Manager.DEFAULT_OPTIONS);
            pi = new PokeapiLoader(mgr);
            data_adapter = new SpeciesAdapter(pi.get_database(),this);
            recycle.setAdapter(data_adapter);
        } catch(IOException e) {
            Log.e("MainActivity", "Error getting database manager.", e);
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
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

}
