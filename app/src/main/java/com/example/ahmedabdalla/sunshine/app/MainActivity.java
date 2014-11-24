package com.example.ahmedabdalla.sunshine.app;

import android.app.ActivityOptions;
import android.app.FragmentManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.Fragment;
import android.os.Bundle;
//import android.support.v7.widget.Toolbar;
import android.transition.Explode;
import android.transition.Transition;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.example.ahmedabdalla.sunshine.app.data.WeatherContract;
import com.example.ahmedabdalla.sunshine.app.data.WeatherProvider;
import com.example.ahmedabdalla.sunshine.app.sync.SunshineSyncAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;



public class MainActivity extends ActionBarActivity
implements ForecastFragment.Callback
{


    public boolean mTwoPane = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        //getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
        //set the transition
        //Transition ts = new Explode();
        //ts.setStartDelay(2000);
        //set the duration
        //ts.setDuration(5000);
        //getWindow().setEnterTransition(ts);
        //set an exit transition so it is activated when the current activity exits
        //getWindow().setExitTransition(ts);

        setContentView(R.layout.activity_main);
        android.support.v7.widget.Toolbar toolbar =
                (android.support.v7.widget.Toolbar) findViewById(R.id.toolbar_main);
        setSupportActionBar(toolbar);

        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayUseLogoEnabled(true);
        getSupportActionBar().setLogo(R.drawable.ic_logo);
        getSupportActionBar().setDisplayShowTitleEnabled(false); //optional

        SunshineSyncAdapter.initializeSyncAdapter(this);
        if (findViewById(R.id.weather_detail_container) != null){
            // if the device being used is a tablet, the layout directory
            // will contain the detail FrameLayout in its activity_main.xml
            mTwoPane = true;
            if (savedInstanceState ==null) {
                Fragment detailFragment = new DetailFragment();
                getSupportFragmentManager().beginTransaction()
                        //.add(R.id.weather_detail_container, new DetailFragment())
                        .replace(R.id.weather_detail_container, detailFragment)
                        .commit();

                Fragment anotherFragment = getSupportFragmentManager()
                        .findFragmentById(R.id.fragment_forecast);
                if (anotherFragment !=null){
                    ForecastFragment forecast = (ForecastFragment) anotherFragment;
                    forecast.setUseTodayLayout(false);}
            }

        }

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        MenuInflater inflater= getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
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
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }



        return super.onOptionsItemSelected(item);
    }


//    private void openPreferredLocationOnMap () {
//        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
//        Intent intent = new Intent(Intent.ACTION_VIEW)
//                .setData(Uri.parse("geo:0,0?q="
//                        + pref.getString(getString(R.string.pref_location_key)
//                        ,getString(R.string.pref_location_default))))
//                .setData(WeatherContract.LocationEntry.)
//                ;
//        if (intent.resolveActivity(getPackageManager()) != null)
//            startActivity(intent);
//
//    }

    @Override
    public void onItemSelected(String date) {
        if (mTwoPane) {
            android.support.v4.app.FragmentManager manager = getSupportFragmentManager();
            Fragment tempFragment = manager
                    .findFragmentById(R.id.weather_detail_container);
            if (tempFragment !=null){
                DetailFragment temp = (DetailFragment) tempFragment;
                if (temp !=null) {
                    temp.restart(date);
                }
            } else {
                Bundle bundle = new Bundle();
                bundle.putString(DetailFragment.DATE_STRING,date);
                Fragment detailFragment = new DetailFragment();
                detailFragment.setArguments(bundle);
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.weather_detail_container, detailFragment)
                        .commit();
            }
        }
        else {
            Intent intent = new Intent(this, DetailActivity.class);
            intent.putExtra(DetailFragment.DATE_STRING,date);
            startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle());
            //startActivity(intent);
//            android.support.v4.app.ActivityOptionsCompat options;
//            options = android.support.v4.app.ActivityOptionsCompat
//                            .makeSceneTransitionAnimation(this,new View(this),"cow");
//                            //(
//                    //this, transitionView, DetailActivity.EXTRA_IMAGE);
//            android.support.v4.app.ActivityCompat.startActivity(this, intent,
//                    options.toBundle());
        }
    }

}
