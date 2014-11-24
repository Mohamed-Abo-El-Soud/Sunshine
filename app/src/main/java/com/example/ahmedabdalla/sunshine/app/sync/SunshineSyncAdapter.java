package com.example.ahmedabdalla.sunshine.app.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SyncRequest;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import android.preference.PreferenceManager;

import com.example.ahmedabdalla.sunshine.app.MainActivity;
import com.example.ahmedabdalla.sunshine.app.R;
import com.example.ahmedabdalla.sunshine.app.Utility;
import com.example.ahmedabdalla.sunshine.app.data.WeatherContract;
import com.example.ahmedabdalla.sunshine.app.data.WeatherContract.WeatherEntry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.Vector;

/**
 * Created by ahmedabdalla on 2014-11-20.
 */
public class SunshineSyncAdapter extends AbstractThreadedSyncAdapter{

    // Interval at which to sync with the weather, in milliseconds.
    // 60 seconds (1 minute) * 180 = 3 hours
    public static final int SYNC_INTERVAL = 60 * 180;
    public static final int SYNC_FLEXTIME = SYNC_INTERVAL/3;

    private static final String Log_TAG = SunshineSyncAdapter.class.getSimpleName();

    // Global variables
    // Define a variable to contain a content resolver instance
    ContentResolver mContentResolver;
    static Context mContext;

    private static final long DAY_IN_MILLIS = 1000 * 60 * 60 * 24;
    private static final int WEATHER_NOTIFICATION_ID = 3004;

    private static final String[] NOTIFY_WEATHER_PROJECTION = new String[] {
            WeatherEntry.COLUMN_WEATHER_ID,
            WeatherEntry.COLUMN_MAX_TEMP,
            WeatherEntry.COLUMN_MIN_TEMP,
            WeatherEntry.COLUMN_SHORT_DESC
    };

    // these indices must match the projection
    private static final int INDEX_WEATHER_ID = 0;
    private static final int INDEX_MAX_TEMP = 1;
    private static final int INDEX_MIN_TEMP = 2;
    private static final int INDEX_SHORT_DESC = 3;

    @Override
    public void onPerformSync(Account account,
                              Bundle extras, String authority,
                              ContentProviderClient provider,
                              SyncResult syncResult) {

        Log.v(Log_TAG, "Starting sync adapter, weather should be acquired shortly...");

        try {
            if (mContext != getContext()) throw new Exception("the contexts aren't the same.");
        } catch (Exception e) {
            Log.e(Log_TAG, "Error: " + e);
        } finally {
            mContext = getContext();
            otherStuff();
            removeOldStuff();
        }


        notifyWeather();

    }
    public void otherStuff(){

        String locationQuery =
            PreferenceManager.getDefaultSharedPreferences(mContext)
                    .getString(mContext.getString(R.string.pref_location_key)
                            ,mContext.getString(R.string.pref_location_default));

        //Log.v(Log_TAG,"starting...");

        if (locationQuery == null) return;

        // These two need to be declared outside the try/catch
        // so that they can be closed in the finally block.
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        String format = "json";
        String units = "metric";
        int numDays = 14;
        Uri.Builder builder = new Uri.Builder();

        // Will contain the raw JSON response as a string.
        String forecastJsonStr = null;

        try {
            // Construct the URL for the OpenWeatherMap query
            // Possible parameters are available at OWM's forecast API page, at
            // http://openweathermap.org/API#forecast

            final String FORECAST_BASE_URL =
                    "http://api.openweathermap.org/data/2.5/forecast/daily?";
            final String QUERY_PARAM =
                    "q";
            final String FORMAT_PARAM =
                    "mode";
            final String UNITS_PARAM =
                    "units";
            final String DAYS_PARAM =
                    "cnt";

            Uri builtUri = Uri.parse(FORECAST_BASE_URL).buildUpon()
                    .appendQueryParameter(QUERY_PARAM, locationQuery)
                    .appendQueryParameter(FORMAT_PARAM, format)
                    .appendQueryParameter(UNITS_PARAM, units)
                    .appendQueryParameter(DAYS_PARAM, Integer.toString(numDays))
                    .build();
            URL url = new URL(builtUri.toString());


            // Create the request to OpenWeatherMap, and open the connection
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            // Read the input stream into a String
            InputStream inputStream = urlConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if (inputStream == null) {
                // Nothing to do.
                return;
            }
            reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                // But it does make debugging a *lot* easier if you print out the completed
                // buffer for debugging.
                buffer.append(line + "\n");
            }

            if (buffer.length() == 0) {
                // Stream was empty.  No point in parsing.
                Log.v(Log_TAG, "Info not found");
                return;
            }
            forecastJsonStr = buffer.toString();

        } catch (IOException e) {
            Log.e(Log_TAG, "Error ", e);
            // If the code didn't successfully get the weather data, there's no point in attempting
            // to parse it.
            return;
        } finally{
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    Log.e(Log_TAG, "Error closing stream", e);
                }
            }
        }

        try {
            //return
            getWeatherDataFromJson(forecastJsonStr, numDays, locationQuery);
        }
        catch (JSONException e1){
            Log.e(Log_TAG, "Error " + e1);
            e1.printStackTrace();
        }
    }

    public void removeOldStuff(){

        Calendar cal = Calendar.getInstance(); //Get's a calendar object with the current time.
        cal.add(Calendar.DATE, -1); //Signifies yesterday's date
        String yesterdayDate = WeatherContract.getDbDateString(cal.getTime());
        getContext().getContentResolver().delete(WeatherEntry.CONTENT_URI,
                WeatherEntry.COLUMN_DATETEXT + " <= ?",
                new String[] {yesterdayDate});
    }

    private Void getWeatherDataFromJson(String forecastJsonStr, int numDays,String locationSetting)
            throws JSONException {
        // These are the names of the JSON objects that need to be extracted.

        // Location information
        final String OWM_CITY = "city";
        final String OWM_CITY_NAME = "name";
        final String OWM_COORD = "coord";
        final String OWM_COORD_LAT = "lat";
        final String OWM_COORD_LONG = "lon";

        // Weather information.  Each day's forecast info is an element of the "list" array.
        final String OWM_LIST = "list";

        final String OWM_DATETIME = "dt";
        final String OWM_PRESSURE = "pressure";
        final String OWM_HUMIDITY = "humidity";
        final String OWM_WINDSPEED = "speed";
        final String OWM_WIND_DIRECTION = "deg";

        // All temperatures are children of the "temp" object.
        final String OWM_TEMPERATURE = "temp";
        final String OWM_MAX = "max";
        final String OWM_MIN = "min";

        final String OWM_WEATHER = "weather";
        final String OWM_DESCRIPTION = "main";
        final String OWM_WEATHER_ID = "id";

        JSONObject forecastJson = new JSONObject(forecastJsonStr);
        JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

        JSONObject cityJson = forecastJson.getJSONObject(OWM_CITY);
        String cityName = cityJson.getString(OWM_CITY_NAME);
        JSONObject coordJSON = cityJson.getJSONObject(OWM_COORD);
        double cityLatitude = coordJSON.getLong(OWM_COORD_LAT);
        double cityLongitude = coordJSON.getLong(OWM_COORD_LONG);

        Log.v(Log_TAG, cityName + ", with coord: " + cityLatitude + " " + cityLongitude);

        // Insert the location into the database.
        // The function referenced here is not yet implemented, so we've commented it out for now.
        long locationID = addLocation(locationSetting, cityName, cityLatitude, cityLongitude);

        // Get and insert the new weather information into the database
        Vector<ContentValues> cVVector = new Vector<ContentValues>(weatherArray.length());

        for(int i = 0; i < weatherArray.length(); i++) {
            // For now, using the format "Day, description, hi/low"
            String description;
            long dateTime;
            double pressure;
            int humidity;
            double windSpeed;
            double windDirection;
            int weatherId;

            // Get the JSON object representing the day
            JSONObject dayForecast = weatherArray.getJSONObject(i);

            // The date/time is returned as a long.  We need to convert that
            // into something human-readable, since most people won't read "1400356800" as
            // "this saturday".
            dateTime = dayForecast.getLong(OWM_DATETIME);
            pressure = dayForecast.getDouble(OWM_PRESSURE);
            humidity = dayForecast.getInt(OWM_HUMIDITY);
            windSpeed = dayForecast.getDouble(OWM_WINDSPEED);
            windDirection = dayForecast.getDouble(OWM_WIND_DIRECTION);

            // description is in a child array called "weather", which is 1 element long.
            JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
            description = weatherObject.getString(OWM_DESCRIPTION);
            weatherId = weatherObject.getInt(OWM_WEATHER_ID);

            // Temperatures are in a child object called "temp".  Try not to name variables
            // "temp" when working with temperature.  It confuses everybody.
            JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
            double high = temperatureObject.getDouble(OWM_MAX);
            double low = temperatureObject.getDouble(OWM_MIN);

            ContentValues weatherValues = new ContentValues();

            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_LOC_KEY, locationID);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_DATETEXT,
                    WeatherContract.getDbDateString(new Date(dateTime * 1000L)));
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_HUMIDITY, humidity);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_PRESSURE, pressure);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_WIND_SPEED, windSpeed);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_DEGREES, windDirection);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_MAX_TEMP, high);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_MIN_TEMP, low);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_SHORT_DESC, description);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_WEATHER_ID, weatherId);

            cVVector.add(weatherValues);
        }
        if (cVVector.size() > 0){
            ContentValues[] contentValues = new ContentValues[cVVector.size()];
            cVVector.toArray(contentValues);

            mContentResolver.bulkInsert(
                    WeatherContract.WeatherEntry.CONTENT_URI,
                    contentValues
            );
        }
        cVVector.remove(1);
        return null;
    }

    private long addLocation (String locationSetting, String cityName, Double lat, Double lon) {

        Log.v(Log_TAG,"Inserting "+cityName+" with coords: "+lat + " , "+lon);
        Cursor cursor = mContentResolver.query(
                WeatherContract.LocationEntry.CONTENT_URI,
                new String[]{WeatherContract.LocationEntry._ID},
                WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING + " = ?",
                new String[]{locationSetting},
                null
        );
        if (cursor.moveToFirst()){
            int locationIdIndex = cursor.getColumnIndex(WeatherContract.LocationEntry._ID);
            return cursor.getLong(locationIdIndex);
        }

        ContentValues values = new ContentValues();
        values.put(WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING,locationSetting);
        values.put(WeatherContract.LocationEntry.COLUMN_CITY_NAME,cityName);
        values.put(WeatherContract.LocationEntry.COLUMN_COORD_LAT,lat);
        values.put(WeatherContract.LocationEntry.COLUMN_COORD_LONG,lon);
        Uri retUri = mContentResolver.insert(
                WeatherContract.LocationEntry.CONTENT_URI, values);
        return ContentUris.parseId(retUri);
    }

    public SunshineSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        mContext = context;
        mContentResolver = context.getContentResolver();
    }

    /**
     * Helper method to have the sync adapter sync immediately
     * @param context The context used to access the account service
     */
    public static void syncImmediately(Context context) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        ContentResolver.requestSync(getSyncAccount(context),
                context.getString(R.string.content_authority), bundle);
    }

    /**
     * Helper method to get the fake account to be used with SyncAdapter, or make a new one
     * if the fake account doesn't exist yet.  If we make a new account, we call the
     * onAccountCreated method so we can initialize things.
     *
     * @param context The context used to access the account service
     * @return a fake account.
     */
    public static Account getSyncAccount(Context context) {
        // Get an instance of the Android account manager
        AccountManager accountManager =
                (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);

        // Create the account type and default account
        Account newAccount = new Account(
                context.getString(R.string.app_name), context.getString(R.string.sync_account_type));

        // If the password doesn't exist, the account doesn't exist
        if ( null == accountManager.getPassword(newAccount) ) {

        /*
         * Add the account and account type, no password or user data
         * If successful, return the Account object, otherwise report an error.
         */
            if (!accountManager.addAccountExplicitly(newAccount, "", null)) {
                return null;
            }
            /*
             * If you don't set android:syncable="true" in
             * in your <provider> element in the manifest,
             * then call ContentResolver.setIsSyncable(account, AUTHORITY, 1)
             * here.
             */

            onAccountCreated(newAccount, context);
        }
        return newAccount;
    }

    /**
     * Helper method to schedule the sync adapter periodic execution
     */
    public static void configurePeriodicSync(Context context, int syncInterval, int flexTime) {
        Account account = getSyncAccount(context);
        if (account == null) return;
        String authority = context.getString(R.string.content_authority);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // we can enable inexact timers in our periodic sync
            Bundle bundle = new Bundle();
            bundle.putString("cow","mouse");
            SyncRequest.Builder builder = new SyncRequest.Builder().
                    syncPeriodic(syncInterval, flexTime).
                    setSyncAdapter(account, authority)
                    .setExtras(bundle)
                    ;
            SyncRequest request = null;
            if (builder !=null)
                request = builder.build();
            else
                return;
            ContentResolver.requestSync(request);
        } else {
            ContentResolver.addPeriodicSync(account,
                    authority, new Bundle(), syncInterval);
        }

    }


    private static void onAccountCreated(Account newAccount, Context context) {
        /*
         * Since we've created an account
         */
        SunshineSyncAdapter.configurePeriodicSync(context, SYNC_INTERVAL, SYNC_FLEXTIME);

        /*
         * Without calling setSyncAutomatically, our periodic sync will not be enabled.
         */
        ContentResolver.setSyncAutomatically(newAccount, context.getString(R.string.content_authority), true);

        /*
         * Finally, let's do a sync to get things started
         */
        syncImmediately(context);
    }

    public static void initializeSyncAdapter(Context context) {
        getSyncAccount(context);

    }

    private void makeNotification(Context context, String header
            ,String content, int resIdIcon, int trackingId){

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context)
                        .setSmallIcon(resIdIcon)
                        .setContentTitle(header)
                        .setContentText(content);
        // Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(context, MainActivity.class);

        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(MainActivity.class);
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        // trackingId allows you to update the notification later on.
        mNotificationManager.notify(trackingId, mBuilder.build());
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void notifyWeather() {
        mContext = getContext();

        //checking the last update and notify if it' the first of the day
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        String lastNotificationKey = mContext.getString(R.string.pref_last_notification);
        long lastSync = prefs.getLong(lastNotificationKey, 0);
        if (!prefs.getBoolean(mContext.getString(R.string.pref_enable_notifications_key),true))
            return;

        //if (System.currentTimeMillis() - lastSync < DAY_IN_MILLIS) return;

        Cursor cursor = mContext.getContentResolver().query(
                WeatherEntry.buildWeatherLocationWithDate(
                        Utility.getPreferredLocation(mContext)
                        ,WeatherContract.getDbDateString(new Date())
                ),NOTIFY_WEATHER_PROJECTION
                ,null,null,null
        );
        if (cursor == null) return;
        if (!cursor.moveToFirst()) return;
        float high = cursor.getFloat(INDEX_MAX_TEMP);
        float low = cursor.getFloat(INDEX_MIN_TEMP);
        String desc = cursor.getString(INDEX_SHORT_DESC);

        String content = String.format(mContext.getString(
                R.string.format_notification), desc, high, low);

        makeNotification(mContext,"Weather Update"
                ,content
                ,R.drawable.art_clear,WEATHER_NOTIFICATION_ID);

        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(lastNotificationKey, System.currentTimeMillis());
        editor.commit();

    }

}
