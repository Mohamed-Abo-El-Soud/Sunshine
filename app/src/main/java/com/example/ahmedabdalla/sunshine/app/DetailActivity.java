package com.example.ahmedabdalla.sunshine.app;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.ShareActionProvider;
import android.os.Bundle;
import android.transition.Slide;
import android.transition.Transition;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import com.example.ahmedabdalla.sunshine.app.data.WeatherContract;

import java.util.Date;
import java.util.zip.Inflater;


public class DetailActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //doTransition();
        setContentView(R.layout.activity_detail);
        android.support.v7.widget.Toolbar toolbar =
                (android.support.v7.widget.Toolbar) findViewById(R.id.toolbar_main);
        if(toolbar!=null) {
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        if (savedInstanceState == null) {
            String date = getIntent().getStringExtra(DetailFragment.DATE_STRING);
            Bundle arguments = new Bundle();
            arguments.putString(DetailFragment.DATE_STRING,date);
            Fragment detailFragment = new DetailFragment();
            detailFragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.weather_detail_container, detailFragment)
                    .commit();
        }
    }

    public void doTransition(){

        //enable window content transition
        getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
        //set the transition
        Transition ts = new Slide();
        ts.setDuration(3000);
        getWindow().setEnterTransition(ts);
        getWindow().setExitTransition(ts);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_detail, menu);

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
            //Log.v("console", getCallingActivity().toString());
            startActivity(new Intent(this, SettingsActivity.class));

            return true;
        }

        return super.onOptionsItemSelected(item);
    }


}
