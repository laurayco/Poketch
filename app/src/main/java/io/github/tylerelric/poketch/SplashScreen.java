package io.github.tylerelric.poketch;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Manager;
import com.couchbase.lite.android.AndroidContext;

import java.io.IOException;

import io.github.tylerelric.poketch.data.PokeapiLoader;

public class SplashScreen extends AppCompatActivity implements PokeapiLoader.ResourceAvailable {

    PokeapiLoader pi;
    Manager mgr = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);
        try {
            mgr = new Manager(new AndroidContext(this),Manager.DEFAULT_OPTIONS);
            pi = new PokeapiLoader(mgr);
            (new Thread() {
                @Override
                public void run() {
                    pi.load_all(SplashScreen.this);
                }
            }).start();
        } catch(IOException e) {
            Log.e("MainActivity", "Error getting database manager.", e);
        } catch (CouchbaseLiteException e) {
            Log.e("MainActivity", "Error getting database manager.", e);
        }
    }

    @Override
    public void response(boolean success) {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }
}
