package com.example.ahmedabdalla.sunshine.app;

/**
 * Created by ahmedabdalla on 2014-11-14.
 */

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
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.ahmedabdalla.sunshine.app.data.WeatherContract;

import java.util.Date;

/**
 * A placeholder fragment containing a simple view.
 */
public class DetailFragment extends Fragment implements
        LoaderManager.LoaderCallbacks <Cursor>{

    private static final String Log_TAG = DetailFragment.class.getSimpleName();

    private static final String FORECAST_SHARE_HASHTAG = "#SunshineApp";

    private static final int FORECAST_LOADER = 0;

    // For the forecast view we're showing only a small subset of the stored data.
    // Specify the columns we need.
    private static final String[] FORECAST_COLUMNS = {
            // In this case the id needs to be fully qualified with a table name, since
            // the content provider joins the location & weather tables in the background
            // (both have an _id column)
            // On the one hand, that's annoying.  On the other, you can search the weather table
            // using the location set by the user, which is only in the Location table.
            // So the convenience is worth it.
            WeatherContract.WeatherEntry.TABLE_NAME + "." + WeatherContract.WeatherEntry._ID,
            WeatherContract.WeatherEntry.COLUMN_DATETEXT,
            WeatherContract.WeatherEntry.COLUMN_SHORT_DESC,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
            WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING,
            WeatherContract.WeatherEntry.COLUMN_WIND_SPEED,
            WeatherContract.WeatherEntry.COLUMN_DEGREES,
            WeatherContract.WeatherEntry.COLUMN_PRESSURE,
            WeatherContract.WeatherEntry.COLUMN_HUMIDITY,
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID
    };

    // These indices are tied to FORECAST_COLUMNS.  If FORECAST_COLUMNS changes, these
    // must change.
    public static final int COL_WEATHER_ID = 0;
    public static final int COL_WEATHER_DATE = 1;
    public static final int COL_WEATHER_DESC = 2;
    public static final int COL_WEATHER_MAX_TEMP = 3;
    public static final int COL_WEATHER_MIN_TEMP = 4;
    public static final int COL_LOCATION_SETTING = 5;
    public static final int  COL_WIND_SPEED = 6;
    public static final int  COL_DEGREES = 7;
    public static final int  COL_PRESSURE = 8;
    public static final int  COL_HUMIDITY = 9;
    public static final int  COL_WEATHER_CONDITIONS_ID = 10;

    private String mLocation;

    private String mForecastStr;

    private ShareActionProvider mShareActionProvider;

    private String mDate;

    public static final String DATE_STRING = ".date";
    public static final String LOCATION_KEY = ".location";

    private TextView day;
    private TextView date;
    private TextView forecast;
    private TextView _high;
    private TextView _low;
    private TextView _humidity;
    private TextView _pressure;
    private TextView _wind;
    private ImageView icon;

    @Override
    public void onCreate(Bundle savedInstanceState){
        //getLoaderManager().initLoader(FORECAST_LOADER,null,this);

        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(LOCATION_KEY, mLocation);
        super.onSaveInstanceState(outState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_detail, container, false);
        day = (TextView)rootView.findViewById(R.id.detail_day_textview);
        date = (TextView)rootView.findViewById(R.id.detail_date_textview);
        forecast = (TextView)rootView.findViewById(R.id.detail_forecast_textview);
        _high = (TextView)rootView.findViewById(R.id.detail_high_textview);
        _low = (TextView)rootView.findViewById(R.id.detail_low_textview);
        _humidity = (TextView)rootView.findViewById(R.id.detail_humidity_textview);
        _pressure = (TextView)rootView.findViewById(R.id.detail_pressure_textview);
        _wind = (TextView)rootView.findViewById(R.id.detail_wind_textview);
        icon = (ImageView)rootView.findViewById(R.id.detail_icon_textview);
        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        inflater.inflate(R.menu.detailfragment, menu);

        MenuItem menuItem = menu.findItem(R.id.action_share);

        //if (true) return;
        // Get the provider and hold onto it to set/change the share intent.
        mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(menuItem);

        if (mShareActionProvider != null) {
            mShareActionProvider.setShareIntent(createShareForecastIntent());
        } else {
            Log.d(Log_TAG, "Share Action Provider is null?");
        }

    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Bundle arguments = getArguments();
        if (savedInstanceState != null){
            mLocation = savedInstanceState.getString(LOCATION_KEY);
        }
        if (arguments != null && arguments.containsKey(DATE_STRING)) {
            mDate = arguments.getString(DATE_STRING);
            getLoaderManager().initLoader(FORECAST_LOADER, null, this);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mLocation != null && !mLocation.equals(Utility.getPreferredLocation(getActivity()))){
            getLoaderManager().restartLoader(FORECAST_LOADER,null,this);
        }
    }

    private Intent createShareForecastIntent() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT,mForecastStr);
        return shareIntent;
    }


    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        // This is called when a new Loader needs to be created.  This
        // fragment only uses one loader, so we don't care about checking the id.

        // To only show current and future dates, get the String representation for today,
        // and filter the query to return weather only for dates after or including today.
        // Only return data after today.
        mDate = mDate!=null? mDate:WeatherContract.getDbDateString(new Date());
        String startDate = mDate;
        //String startDate = WeatherContract.getDbDateString(new Date());


        // Sort order:  Ascending, by date.
        String sortOrder = WeatherContract.WeatherEntry.COLUMN_DATETEXT + " ASC";

        mLocation = Utility.getPreferredLocation(getActivity());
        Uri weatherForLocationUri = WeatherContract.WeatherEntry.buildWeatherLocationWithStartDate(
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
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {

        if (mDate == null) return;

        if (cursor != null && cursor.moveToFirst()) {
            String dayString = Utility.getDayName(getActivity()
                    , cursor.getString(COL_WEATHER_DATE));
            String dateString = Utility.getFormattedMonthDay(getActivity(),
                    cursor.getString(COL_WEATHER_DATE));
            String weatherDescription = cursor.getString(COL_WEATHER_DESC);
            boolean isMetric = Utility.isMetric(getActivity());
            String high = Utility.formatTemperature(getActivity(),
                    cursor.getDouble(COL_WEATHER_MAX_TEMP), isMetric);
            String low = Utility.formatTemperature(getActivity(),
                    cursor.getDouble(COL_WEATHER_MIN_TEMP), isMetric);


            Float humidity = cursor.getFloat(COL_HUMIDITY);
            Float pressure = cursor.getFloat(COL_PRESSURE);
            Float wind = cursor.getFloat(COL_WIND_SPEED);
            Float degrees = cursor.getFloat(COL_DEGREES);
            Integer weatherId = cursor.getInt(COL_WEATHER_CONDITIONS_ID);

            Activity activity = getActivity();


            mForecastStr = String.format("%s - %s - %s/%s",
                    dateString, weatherDescription, high, low);

            mForecastStr += " " + FORECAST_SHARE_HASHTAG;

            if (mShareActionProvider != null)
                mShareActionProvider.setShareIntent(createShareForecastIntent());

            day.setText(dayString);
            date.setText(dateString);

            forecast.setText(weatherDescription);

            _high.setText(high);

            _low.setText(low);

            _humidity.setText(activity.getString(R.string.format_humidity,humidity));
            _pressure.setText(activity.getString(R.string.format_pressure,pressure));
            _wind.setText(Utility.getFormattedWind(activity,wind,degrees));

            //icon.setImageResource(R.drawable.ic_launcher);
            icon.setImageResource(Utility.getArtResourceForWeatherCondition(weatherId));

        }

    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mForecastStr = null;
    }


    public void restart (String date){
        mDate = date;
        getLoaderManager().restartLoader(FORECAST_LOADER,null,this);
    }


}
