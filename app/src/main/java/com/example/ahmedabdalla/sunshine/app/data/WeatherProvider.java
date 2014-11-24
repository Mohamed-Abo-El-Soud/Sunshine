package com.example.ahmedabdalla.sunshine.app.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import com.example.ahmedabdalla.sunshine.app.data.WeatherContract.WeatherEntry;
import com.example.ahmedabdalla.sunshine.app.data.WeatherContract.LocationEntry;

import java.sql.SQLDataException;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by ahmedabdalla on 2014-11-10.
 */
public class WeatherProvider extends ContentProvider {



    // The URI Matcher used by this content provider.
    private static final UriMatcher sUriMatcher = buildUriMatcher();
    private WeatherDBHelper mOpenHelper;
    private static final SQLiteQueryBuilder sWeatherByLocationSettingQueryBuilder;

    static {
        sWeatherByLocationSettingQueryBuilder = new SQLiteQueryBuilder();
        sWeatherByLocationSettingQueryBuilder.setTables(
                WeatherEntry.TABLE_NAME + " INNER JOIN " +
                        LocationEntry.TABLE_NAME +
                        " ON " + WeatherEntry.TABLE_NAME +
                        "." + WeatherEntry.COLUMN_LOC_KEY +
                        " = " + LocationEntry.TABLE_NAME +
                        "." + LocationEntry._ID
        );
    }

    private static final String sLocationSettingSelection =
            LocationEntry.TABLE_NAME + "." + LocationEntry.COLUMN_LOCATION_SETTING
            + " = ? ";

    private static final String sLocationSettingWithStartDateSelection =
            LocationEntry.TABLE_NAME + "." + LocationEntry.COLUMN_LOCATION_SETTING
                    + " = ? AND " + WeatherEntry.COLUMN_DATETEXT + " >= ? ";

    private static final String sLocationSettingWithDaySelection =
            LocationEntry.TABLE_NAME + "." + LocationEntry.COLUMN_LOCATION_SETTING
                    + " = ? AND " + WeatherEntry.COLUMN_DATETEXT + " = ? ";


    private Cursor getWeatherByLocationSetting(Uri uri, String[] projection, String sortOrder) {
        String locationSetting = WeatherEntry.getLocationSettingFromUri(uri);
        String startDate = WeatherEntry.getStartDateFromUri(uri);

        String[] selectionArgs;
        String selection;

        if (startDate == null) {
            selection = sLocationSettingSelection;
            selectionArgs = new String[]{locationSetting};
        } else {
            selectionArgs = new String[]{locationSetting, startDate};
            selection = sLocationSettingWithStartDateSelection;
        }

        return sWeatherByLocationSettingQueryBuilder.query(mOpenHelper.getReadableDatabase(),
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder
        );
    }
    private Cursor getWeatherByLocationSettingWithDate(Uri uri, String[] projection, String sortOrder) {
        String day = WeatherEntry.getDateFromUri(uri);
        String locationSetting = WeatherEntry.getLocationSettingFromUri(uri);

        return sWeatherByLocationSettingQueryBuilder.query(
                mOpenHelper.getReadableDatabase(),
                projection,
                sLocationSettingWithDaySelection,
                new String[]{locationSetting,day},
                null,
                null,
                sortOrder
        );
    }

    private static final int WEATHER = 100;
    private static final int WEATHER_WITH_LOCATION = 101;
    private static final int WEATHER_WITH_LOCATION_AND_DATE = 102;
    private static final int LOCATION = 300;
    private static final int LOCATION_ID = 301;

    private static UriMatcher buildUriMatcher() {
        // I know what you're thinking.  Why create a UriMatcher when you can use regular
        // expressions instead?  Because you're not crazy, that's why.

        // All paths added to the UriMatcher have a corresponding code to return when a match is
        // found.  The code passed into the constructor represents the code to return for the root
        // URI.  It's common to use NO_MATCH as the code for this case.
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = WeatherContract.CONTENT_AUTHORITY;

        // For each type of URI you want to add, create a corresponding code.
        matcher.addURI(authority, WeatherContract.PATH_WEATHER, WEATHER);
        matcher.addURI(authority, WeatherContract.PATH_WEATHER + "/*", WEATHER_WITH_LOCATION);
        matcher.addURI(authority, WeatherContract.PATH_WEATHER + "/*/*", WEATHER_WITH_LOCATION_AND_DATE);

        matcher.addURI(authority, WeatherContract.PATH_LOCATION, LOCATION);
        matcher.addURI(authority, WeatherContract.PATH_LOCATION + "/#", LOCATION_ID);

        return matcher;
    }

    @Override
    public boolean onCreate() {
        mOpenHelper = new WeatherDBHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        // Here's the switch statement that, given a URI, will determine what kind of request it is,
        // and query the database accordingly.
        Cursor retCursor;
        switch (sUriMatcher.match(uri)) {
            // "weather/*/*"
            case WEATHER_WITH_LOCATION_AND_DATE:
            {
                retCursor = getWeatherByLocationSettingWithDate(uri,projection,sortOrder);
                break;
            }
            // "weather/*"
            case WEATHER_WITH_LOCATION: {
                retCursor = getWeatherByLocationSetting(uri,projection,sortOrder);
                break;
            }
            // "weather"
            case WEATHER: {
                retCursor = mOpenHelper.getReadableDatabase().query(
                        WeatherContract.WeatherEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            }
            // "location/*"
            case LOCATION_ID: {
                retCursor = mOpenHelper.getReadableDatabase().query(
                        LocationEntry.TABLE_NAME,
                        projection,
                        LocationEntry._ID + " = '" + ContentUris.parseId(uri) + "'",
                        null,
                        null,
                        null,
                        sortOrder
                );
                break;
            }
            // "location"
            case LOCATION: {
                retCursor = mOpenHelper.getReadableDatabase().query(
                        LocationEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            }

            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        retCursor.setNotificationUri(getContext().getContentResolver(), uri);
        return retCursor;
    }


    @Override
    public String getType(Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match){
            case WEATHER_WITH_LOCATION_AND_DATE:
                return WeatherEntry.CONTENT_ITEM_TYPE;
            case WEATHER_WITH_LOCATION:
                return WeatherEntry.CONTENT_TYPE;
            case WEATHER:
                return WeatherEntry.CONTENT_TYPE;
            case LOCATION_ID:
                return LocationEntry.CONTENT_ITEM_TYPE;
            case LOCATION:
                return LocationEntry.CONTENT_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown Uri: " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        SQLiteDatabase database = mOpenHelper.getReadableDatabase();
        int matcher = sUriMatcher.match(uri);
        Uri returnUri;
        long _id;
        switch (matcher){
            case WEATHER:
                _id = database.insert(WeatherEntry.TABLE_NAME,null,values);
                if (_id <= 0)
                    throw new SQLException("Row was not inserted in " + uri);
                else {
                    returnUri = WeatherEntry.buildWeatherUri(_id);
                }
                break;
            case LOCATION:
                _id = database.insert(LocationEntry.TABLE_NAME,null,values);
                if (_id <= 0)
                    throw new SQLException("Row was not inserted in " + uri);
                else{
                    returnUri = LocationEntry.buildLocationUri(_id);
                }
                break;
            default:
                throw new UnsupportedOperationException("Unknown Uri: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri,null);



        return returnUri;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase database = mOpenHelper.getReadableDatabase();
        long rowId;
        int matcher = sUriMatcher.match(uri);
        switch (matcher){
            case WEATHER:
                rowId = database.delete(WeatherEntry.TABLE_NAME,selection,selectionArgs);
                break;
            case LOCATION:
                rowId = database.delete(LocationEntry.TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown Uri: " + uri);
        }
        if (rowId == -1) throw new UnsupportedOperationException("Cannot delete " +
                "row or row doesn't exist");
        getContext().getContentResolver().notifyChange(uri,null);
        return (int)rowId;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        SQLiteDatabase database = mOpenHelper.getReadableDatabase();
        long rowId;
        int matcher = sUriMatcher.match(uri);
        switch (matcher){
            case WEATHER:
                rowId = database.update(WeatherEntry.TABLE_NAME, values, selection, selectionArgs);
                break;
            case LOCATION:
                rowId = database.update(LocationEntry.TABLE_NAME, values, selection, selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown Uri: " + uri);
        }
        if (rowId == -1) throw new UnsupportedOperationException("Cannot update " +
                "row or row doesn't exist");
        getContext().getContentResolver().notifyChange(uri,null);
        return (int)rowId;
    }


    // Specify the columns we need.
    private static final String[] PROJECTION_COLUMNS = {
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
            WeatherEntry.COLUMN_WEATHER_ID
    };

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case WEATHER:
                db.beginTransaction();
                int returnCount = 0;
                try {
                    for (ContentValues value : values) {
                        long _id = db.insert(WeatherContract.WeatherEntry.TABLE_NAME, null, value);
                        if (_id != -1) {
                            returnCount++;
                        }
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }

                getContext().getContentResolver().notifyChange(uri, null);

                return returnCount;
            default:
                return super.bulkInsert(uri, values);
        }
    }
}
