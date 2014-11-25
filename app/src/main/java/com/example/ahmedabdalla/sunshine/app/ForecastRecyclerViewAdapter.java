package com.example.ahmedabdalla.sunshine.app;

import android.content.Context;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Created by ahmedabdalla on 2014-11-24.
 */
public class ForecastRecyclerViewAdapter extends RecyclerView.Adapter
        <ForecastRecyclerViewAdapter.ViewHolder>{

    //VARIABLES & CONSTANTS...
    private static final int VIEW_TYPE_TODAY = 0;
    private static final int VIEW_TYPE_FUTURE_DAY = 1;
    private static final int VIEW_TYPE_COUNT = 2;

    private boolean mUseTodayLayout = true;

    protected final String Log_TAG = ForecastRecyclerViewAdapter.class.getSimpleName();

    OnItemClickListener mListener;
    Cursor mCursor;
    Context mContext;

    public ForecastRecyclerViewAdapter(Context context,Cursor mCursor) {
        this.mCursor = mCursor;
        this.mContext = context;
    }

    public void swapCursor(Cursor cursor){
        mCursor = cursor;
    }

    public Cursor getCursor() {
        return mCursor;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        // Determine layoutId from viewType
        int layoutId = viewType == VIEW_TYPE_TODAY? R.layout.list_item_forecast_today:R.layout.list_item_forecast;
        final LayoutInflater layoutInflater = LayoutInflater.from(viewGroup.getContext());
        final View mView = layoutInflater.inflate(layoutId,viewGroup,false);
        return new ViewHolder(mView);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int position) {

        // error checking...
//        if(true) return;
        if (mCursor
                == null || mContext == null) return;
        if (!mCursor.moveToPosition(position)) return;

        // Read weather icon ID from cursor
        int weatherId = mCursor.getInt(ForecastFragment.COL_WEATHER_CONDITION_ID);

        // get the view type from the position of the cursor
        int viewType = getItemViewType(position);
        // Determine image to get from viewType
        if(viewType != VIEW_TYPE_TODAY)
            viewHolder.iconView.setImageResource(Utility.getIconResourceForWeatherCondition(weatherId));
        else
            viewHolder.iconView.setImageResource(Utility.getArtResourceForWeatherCondition(weatherId));


        // Read date from cursor
        String dateString = mCursor.getString(ForecastFragment.COL_WEATHER_DATE);
        // Find TextView and set formatted date on it
        viewHolder.dateView.setText((position == 0 && !mUseTodayLayout) ?
                Utility.getDayName(mContext, dateString)
                : Utility.getFriendlyDayString(mContext, dateString));

        // Read weather forecast from cursor
        String description = mCursor.getString(ForecastFragment.COL_WEATHER_DESC);
        // Find TextView and set weather forecast on it
        viewHolder.descriptionView.setText(description);

        // set the content description of the image to be the same as the one written
        // for accessibility purposes
        viewHolder.iconView.setContentDescription(description);

        // Read user preference for metric or imperial temperature units
        boolean isMetric = Utility.isMetric(mContext);

        // Read high temperature from cursor
        float high = mCursor.getFloat(ForecastFragment.COL_WEATHER_MAX_TEMP);
        // Find TextView and set formatted high temperature on it
        viewHolder.highTempView.setText(Utility.formatTemperature(mContext,high,isMetric));

        // Read low temperature from cursor
        float low = mCursor.getFloat(ForecastFragment.COL_WEATHER_MIN_TEMP);
        // Find TextView and set formatted low temperature on it
        viewHolder.lowTempView.setText(Utility.formatTemperature(mContext,low,isMetric));
    }

    /**
    * helper functions
    * */
    @Override
    public int getItemCount() {
        return 14;
    }


    @Override
    public int getItemViewType(int position) {
        return (position == 0 && mUseTodayLayout) ? VIEW_TYPE_TODAY : VIEW_TYPE_FUTURE_DAY;
    }


    public void setUseTodayLayout (boolean useTodayLayout){
        mUseTodayLayout = useTodayLayout;
    }


    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{

        public final ImageView iconView;
        public final TextView dateView;
        public final TextView descriptionView;
        public final TextView highTempView;
        public final TextView lowTempView;

        public ViewHolder(View view) {
            super(view);
            iconView = (ImageView) view.findViewById(R.id.list_item_icon);
            dateView = (TextView) view.findViewById(R.id.list_item_date_textview);
            descriptionView = (TextView) view.findViewById(R.id.list_item_forecast_textview);
            highTempView = (TextView) view.findViewById(R.id.list_item_high_textview);
            lowTempView = (TextView) view.findViewById(R.id.list_item_low_textview);
            view.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if(mListener !=null)
                mListener.onItemClick(v,getPosition());
        }
    }





    // region click listeners

    public interface OnItemClickListener {
        public void onItemClick(View view, int position);
    }

    public void SetOnItemClickListener(OnItemClickListener listener) {
        mListener = listener;
    }
    //endregion
}
