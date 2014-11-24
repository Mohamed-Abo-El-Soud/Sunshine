package com.example.ahmedabdalla.sunshine.app;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.support.v4.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Created by ahmedabdalla on 2014-11-14.
 */
public class ForecastAdapter extends CursorAdapter{

    private static final int VIEW_TYPE_TODAY = 0;
    private static final int VIEW_TYPE_FUTURE_DAY = 1;
    private static final int VIEW_TYPE_COUNT = 2;

    private boolean mUseTodayLayout = true;

    public ForecastAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
    }

    @Override
    public int getItemViewType(int position) {
        return (position == 0 && mUseTodayLayout) ? VIEW_TYPE_TODAY : VIEW_TYPE_FUTURE_DAY;
    }

    @Override
    public int getViewTypeCount() {
        return VIEW_TYPE_COUNT;
    }


    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        // Choose the layout type
        int viewType = getItemViewType(cursor.getPosition());
        // Determine layoutId from viewType
        int layoutId = viewType == VIEW_TYPE_TODAY? R.layout.list_item_forecast_today:R.layout.list_item_forecast;
        View rootView = LayoutInflater.from(context).inflate(layoutId, parent, false);
        rootView.setTag(new ViewHolder(rootView));
        return rootView;
    }
    @Override
    public void bindView(View view, Context context, Cursor cursor) {

        //holder = new ViewHolder(view);
        ViewHolder tempHolder = (ViewHolder) view.getTag();

        // Read weather icon ID from cursor
        int weatherId = cursor.getInt(ForecastFragment.COL_WEATHER_CONDITION_ID);

        // get the view type from the position of the cursor
        int viewType = getItemViewType(cursor.getPosition());
        // Determine image to get from viewType
        if(viewType != VIEW_TYPE_TODAY)
            tempHolder.iconView.setImageResource(Utility.getIconResourceForWeatherCondition(weatherId));
        else
            tempHolder.iconView.setImageResource(Utility.getArtResourceForWeatherCondition(weatherId));


        // Read date from cursor
        String dateString = cursor.getString(ForecastFragment.COL_WEATHER_DATE);
        // Find TextView and set formatted date on it
        tempHolder.dateView.setText((cursor.getPosition() == 0 && !mUseTodayLayout)?
                Utility.getDayName(context,dateString)
                :Utility.getFriendlyDayString(context, dateString));

        // Read weather forecast from cursor
        String description = cursor.getString(ForecastFragment.COL_WEATHER_DESC);
        // Find TextView and set weather forecast on it
        tempHolder.descriptionView.setText(description);

        // set the content description of the image to be the same as the one written
        // for accessibility purposes
        tempHolder.iconView.setContentDescription(description);

        // Read user preference for metric or imperial temperature units
        boolean isMetric = Utility.isMetric(context);

        // Read high temperature from cursor
        float high = cursor.getFloat(ForecastFragment.COL_WEATHER_MAX_TEMP);
        // Find TextView and set formatted high temperature on it
        tempHolder.highTempView.setText(Utility.formatTemperature(view.getContext(),high,isMetric));

        // Read low temperature from cursor
        float low = cursor.getFloat(ForecastFragment.COL_WEATHER_MIN_TEMP);
        // Find TextView and set formatted low temperature on it
        tempHolder.lowTempView.setText(Utility.formatTemperature(view.getContext(),low,isMetric));
    }

    public void setUseTodayLayout (boolean useTodayLayout){
        mUseTodayLayout = useTodayLayout;
    }


    /**
     * Cache of the children views for a forecast list item.
     */
    public static class ViewHolder {
        public final ImageView iconView;
        public final TextView dateView;
        public final TextView descriptionView;
        public final TextView highTempView;
        public final TextView lowTempView;

        public ViewHolder(View view) {
            iconView = (ImageView) view.findViewById(R.id.list_item_icon);
            dateView = (TextView) view.findViewById(R.id.list_item_date_textview);
            descriptionView = (TextView) view.findViewById(R.id.list_item_forecast_textview);
            highTempView = (TextView) view.findViewById(R.id.list_item_high_textview);
            lowTempView = (TextView) view.findViewById(R.id.list_item_low_textview);
        }
    }





}
