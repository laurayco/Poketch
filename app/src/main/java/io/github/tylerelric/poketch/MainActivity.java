package io.github.tylerelric.poketch;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.couchbase.lite.Database;
import com.couchbase.lite.Document;
import com.couchbase.lite.Manager;
import com.couchbase.lite.android.AndroidContext;
import com.facebook.stetho.Stetho;

import java.io.IOException;
import java.util.Map;

import io.github.tylerelric.poketch.io.github.tylerelric.poketch.data.PokedexInterface;

public class MainActivity
extends AppCompatActivity
implements PokedexInterface.PokedexDatabaseHandler {

    PokedexInterface pi;
    Manager mgr = null;
    Database pkdx = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        try {
            Stetho.initializeWithDefaults(this);
            mgr = new Manager(new AndroidContext(this),Manager.DEFAULT_OPTIONS);
            pi = new PokedexInterface(mgr);
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
    }

    @Override
    public void on_database_progress(Map<String, Object> m, int done, int total) {
        Log.i("MainActivity","Database downloaded " + m.get("name"));
        float progress = (float)done / total;
        Log.i("MainActivity","Database download progress: " + progress*100);
    }
}
