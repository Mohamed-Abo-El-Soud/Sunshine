package com.example.ahmedabdalla.sunshine.app;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.Date;

import com.example.ahmedabdalla.sunshine.app.ForecastRecyclerViewAdapter.OnItemClickListener;
import com.example.ahmedabdalla.sunshine.app.data.WeatherContract;
import com.example.ahmedabdalla.sunshine.app.data.WeatherContract.WeatherEntry;
import com.example.ahmedabdalla.sunshine.app.data.WeatherContract.LocationEntry;

/**
 * Created by ahmedabdalla on 2014-11-05.
 */
public class ForecastFragment extends Fragment implements
        LoaderManager.LoaderCallbacks <Cursor>{

    // stuff for listview...
    private ForecastAdapter mForecastAdapter;
    private ListView mListview;
    private boolean mUseTodayLayout = true;

    protected final String Log_TAG = ForecastFragment.class.getSimpleName();

    private static final int FORECAST_LOADER = 0;

    // key for getting the position from the instance state
    private static final String POSITION_KEY = ".position";
    // the position of a selected item
    private static int mPostiion = -1;

    private String mLocation;

    // stuff for Recyclerview...
    private ForecastRecyclerViewAdapter mRecyclerAdapter;
    private RecyclerView mRecyclerView;
    private LinearLayoutManager mlayoutManager;

    // For the forecast view we're showing only a small subset of the stored data.
    // Specify the columns we need.
    private static final String[] FORECAST_COLUMNS = {
            // In this case the id needs to be fully qualified with a table name, since
            // the content provider joins the location & weather tables in the background
            // (both have an _id column)
            // On the one hand, that's annoying.  On the other, you can search the weather table
            // using the location set by the user, which is only in the Location table.
            // So the convenience is worth it.

            WeatherEntry.TABLE_NAME + "." + WeatherEntry._ID,
            //WeatherEntry.COLUMN_WEATHER_ID,
            WeatherEntry.COLUMN_DATETEXT,
            WeatherEntry.COLUMN_SHORT_DESC,
            WeatherEntry.COLUMN_MAX_TEMP,
            WeatherEntry.COLUMN_MIN_TEMP,
            LocationEntry.COLUMN_LOCATION_SETTING,
            WeatherEntry.COLUMN_WEATHER_ID,
            LocationEntry.COLUMN_COORD_LAT,
            LocationEntry.COLUMN_COORD_LONG
    };

    // These indices are tied to FORECAST_COLUMNS.  If FORECAST_COLUMNS changes, these
    // must change.
    public static final int COL_WEATHER_ID = 0;
    public static final int COL_WEATHER_DATE = 1;
    public static final int COL_WEATHER_DESC = 2;
    public static final int COL_WEATHER_MAX_TEMP = 3;
    public static final int COL_WEATHER_MIN_TEMP = 4;
    public static final int COL_LOCATION_SETTING = 5;
    public static final int COL_WEATHER_CONDITION_ID = 6;
    public static final int COL_LOCATION_LAT = 7;
    public static final int COL_LOCATION_LONG = 8;


    /**
     * A callback interface that all activities containing this fragment must
     * implement. This mechanism allows activities to be notified of item
     * selections.
     */
    public interface Callback {
        /**
         * Callback for when an item has been selected.
         */
        public void onItemSelected(String date);
    }

    Callback mListener;

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (mPostiion == ListView.INVALID_POSITION)
            outState.putInt(POSITION_KEY, mPostiion);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (Callback) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnArticleSelectedListener");
        }
    }

    public ForecastFragment() {
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        getLoaderManager().initLoader(FORECAST_LOADER, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

//    @Override
//    public void onCreateOptionsMenu(Menu menu,MenuInflater inflater){
//        inflater.inflate(R.menu.forecastfragment,menu);
//    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        int id = item.getItemId();
        if (id==R.id.view_map) {
            openPreferredLocationOnMap();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void openPreferredLocationOnMap () {
        if(mForecastAdapter == null) return;
        Cursor c = mForecastAdapter.getCursor();
        if(c == null) return;
        c.moveToPosition(0);
        String posLat = c.getString(COL_LOCATION_LAT);
        String posLong = c.getString(COL_LOCATION_LONG);
        Uri geoLocation = Uri.parse("geo:" + posLat + "," + posLong + "?");

        Log.v(Log_TAG,"geo:" + posLat + "," + posLong + "?");
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(geoLocation);
        if (intent.resolveActivity(getActivity().getPackageManager()) != null)
            startActivity(intent);
        else
            Log.d(Log_TAG, "Couldn't call " + geoLocation.toString()
                    + ", no receiving apps installed!");

    }

    @Override
    public void onStart(){
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mLocation != null && !mLocation.equals(Utility.getPreferredLocation(getActivity()))){
            getLoaderManager().restartLoader(FORECAST_LOADER, null, this);
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

//        createListView(rootView);

        createRecycleView(rootView);

        /**
            mForecastAdapter = new ForecastAdapter(getActivity(),null,0);

            mForecastAdapter.setUseTodayLayout(mUseTodayLayout);

            mListview = (ListView) rootView.findViewById(
                    R.id.listview_forecast);

            mListview.setAdapter(mForecastAdapter);



            mListview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Cursor cursor = mForecastAdapter.getCursor();
                    if (cursor != null && cursor.moveToPosition(position)) {
                        mListener.onItemSelected(cursor.getString(COL_WEATHER_DATE));
                    }
                    mPostiion = position;
                }
            });
        **/

        if(savedInstanceState != null && savedInstanceState.containsKey(POSITION_KEY))
            mPostiion = savedInstanceState.getInt(POSITION_KEY);


        return rootView;
    }

    private void createListView(View rootView){

        mForecastAdapter = new ForecastAdapter(getActivity(),null,0);

        mForecastAdapter.setUseTodayLayout(mUseTodayLayout);

//        mListview = (ListView) rootView.findViewById(
//                R.id.listview_forecast);

        mListview.setAdapter(mForecastAdapter);



        mListview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Cursor cursor = mForecastAdapter.getCursor();
                if (cursor != null && cursor.moveToPosition(position)) {
                    mListener.onItemSelected(cursor.getString(COL_WEATHER_DATE));
                }
                mPostiion = position;
            }
        });
    }

    private void createRecycleView(View rootView){

        mRecyclerView = (RecyclerView)rootView.findViewById(R.id.recylerview_forecast);
        mRecyclerAdapter = new ForecastRecyclerViewAdapter(getActivity(),null);
        mlayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setAdapter(mRecyclerAdapter);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.setLayoutManager(mlayoutManager);


        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
//        mRecyclerView.setHasFixedSize(true);

        mRecyclerAdapter.SetOnItemClickListener(new ForecastRecyclerViewAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                if(true) return;
//                Log.v(Log_TAG, position + "");

                Cursor cursor = mRecyclerAdapter.getCursor();
                if (cursor != null && cursor.moveToPosition(position)) {
                    mListener.onItemSelected(cursor.getString(COL_WEATHER_DATE));
                }
                mPostiion = position;
            }
        });

    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        // This is called when a new Loader needs to be created.  This
        // fragment only uses one loader, so we don't care about checking the id.

        // To only show current and future dates, get the String representation for today,
        // and filter the query to return weather only for dates after or including today.
        // Only return data after today.
        String startDate = WeatherContract.getDbDateString(new Date());


        Log.v(Log_TAG,"creating loader");
        // Sort order:  Ascending, by date.
        String sortOrder = WeatherEntry.COLUMN_DATETEXT + " ASC";

        mLocation = Utility.getPreferredLocation(getActivity());
        Uri weatherForLocationUri = WeatherEntry.buildWeatherLocationWithStartDate(
                mLocation, startDate);

        // Now create and return a CursorLoader that will take care of
        // creating a Cursor for the data being displayed.
        return new CursorLoader(
                getActivity(),
                weatherForLocationUri,
                FORECAST_COLUMNS,
                null,
                null,
                sortOrder
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader
            , Cursor cursor) {
        if(mForecastAdapter != null){
            mForecastAdapter.swapCursor(cursor);
            mListview.setSelection(mPostiion);
        }
        if(mRecyclerAdapter != null)
            mRecyclerAdapter.swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        if(mForecastAdapter != null)
            mForecastAdapter.swapCursor(null);
        if(mRecyclerAdapter != null)
            mRecyclerAdapter.swapCursor(null);
    }

    public void setUseTodayLayout (boolean useTodayLayout){
        mUseTodayLayout = useTodayLayout;
        if(mForecastAdapter != null)
            mForecastAdapter.setUseTodayLayout(mUseTodayLayout);
        if(mRecyclerAdapter != null)
            mRecyclerAdapter.setUseTodayLayout(mUseTodayLayout);
    }
}
