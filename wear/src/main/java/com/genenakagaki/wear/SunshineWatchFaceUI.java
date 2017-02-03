package com.genenakagaki.wear;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.support.v4.content.ContextCompat;
import android.text.format.DateFormat;
import android.util.DisplayMetrics;
import android.util.TypedValue;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import static android.R.attr.resource;
import static android.R.attr.x;
import static android.R.attr.y;
import static java.security.AccessController.getContext;

/**
 * Created by gene on 1/27/17.
 */

public class SunshineWatchFaceUI {

    private static final Typeface BOLD_TYPEFACE =
            Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD);
    private static final Typeface NORMAL_TYPEFACE =
            Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);

    private Context mContext;

    private Paint mBackgroundPaint;
    private Paint mHourPaint;
    private Paint mColonPaint;
    private Paint mMinutePaint;
    private Paint mDatePaint;
    private Paint mLinePaint;
    private Paint mWeatherIconPaint;
    private Paint mMaxTempPaint;
    private Paint mMinTempPaint;

    private int mWeatherIconResourceId = -1;
    private String mMaxTempString = "";
    private String mMinTempString = "";

    private Calendar mCalendar;
    private Date mDate;
    private SimpleDateFormat mDateFormat;

    private float mYOffset;
    private float mLineHeight;

    public SunshineWatchFaceUI(Context context) {
        mContext = context;
        Resources resources = context.getResources();

        mBackgroundPaint = new Paint();
        mBackgroundPaint.setColor(ContextCompat.getColor(context, R.color.background));
        mHourPaint = createTextPaint(R.color.text_light, BOLD_TYPEFACE);
        mHourPaint.setTextSize(resources.getDimension(R.dimen.digital_text_size));
        mColonPaint = createTextPaint(R.color.text_light, NORMAL_TYPEFACE);
        mColonPaint.setTextSize(resources.getDimension(R.dimen.digital_text_size));
        mMinutePaint = createTextPaint(R.color.text_light, NORMAL_TYPEFACE);
        mMinutePaint.setTextSize(resources.getDimension(R.dimen.digital_text_size));
        mDatePaint = createTextPaint(R.color.text_dark, NORMAL_TYPEFACE);
        mDatePaint.setTextSize(resources.getDimension(R.dimen.digital_date_text_size));
        mLinePaint = new Paint();
        mLinePaint.setColor(ContextCompat.getColor(context, R.color.text_dark));
        mWeatherIconPaint = new Paint();
        mMaxTempPaint = createTextPaint(R.color.text_light, BOLD_TYPEFACE);
        mMaxTempPaint.setTextSize(resources.getDimension(R.dimen.digital_temp_text_size));
        mMinTempPaint = createTextPaint(R.color.text_dark, NORMAL_TYPEFACE);
        mMinTempPaint.setTextSize(resources.getDimension(R.dimen.digital_temp_text_size));
        mMinTempPaint.setTextSize(resources.getDimension(R.dimen.digital_temp_text_size));


        mCalendar = Calendar.getInstance();
        mDate = new Date();
        mDateFormat = new SimpleDateFormat("EEE, MMM d yyyy");
        mDateFormat.setCalendar(mCalendar);

        mYOffset = resources.getDimension(R.dimen.digital_y_offset);
        mLineHeight = resources.getDimension(R.dimen.digital_line_height);
    }

    public void adjustToCurrentMode(boolean inAmbientMode) {
        int color;
        if (inAmbientMode) {
            color = ContextCompat.getColor(mContext, R.color.background_ambient);
        } else {
            color = ContextCompat.getColor(mContext, R.color.background);
        }

        mHourPaint.setAntiAlias(!inAmbientMode);
        mColonPaint.setAntiAlias(!inAmbientMode);
        mMinutePaint.setAntiAlias(!inAmbientMode);
        mDatePaint.setAntiAlias(!inAmbientMode);
        mLinePaint.setAntiAlias(!inAmbientMode);
        mWeatherIconPaint.setAntiAlias(!inAmbientMode);
        mMaxTempPaint.setAntiAlias(!inAmbientMode);
        mMinTempPaint.setAntiAlias(!inAmbientMode);

        ColorMatrix cm = new ColorMatrix();
        if (inAmbientMode) {
            cm.setSaturation(0);
        } else {
            cm.setSaturation(1);
        }
        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
        mWeatherIconPaint.setColorFilter(f);

        mBackgroundPaint.setColor(color);
    }

    public void draw(Canvas canvas, Rect bounds) {
        long now = System.currentTimeMillis();
        mCalendar.setTimeInMillis(now);
        mDate.setTime(now);

        // Draw background
        canvas.drawRect(0, 0, bounds.width(), bounds.height(), mBackgroundPaint);

        // Draw time
        String hourString = String.format("%02d", mCalendar.get(Calendar.HOUR_OF_DAY));
        String minuteString = String.format("%02d", mCalendar.get(Calendar.MINUTE));
        float hourWidth = mHourPaint.measureText(hourString);
        float colonWidth = mColonPaint.measureText(":");
        float minuteWidth = mMinutePaint.measureText(minuteString);
        float x = bounds.centerX() - (hourWidth + colonWidth + minuteWidth) / 2;

        canvas.drawText(hourString, x, mYOffset, mHourPaint);
        x += hourWidth;
        canvas.drawText(":", x, mYOffset, mColonPaint);
        x += colonWidth;
        canvas.drawText(minuteString, x, mYOffset, mMinutePaint);

        // Draw date
        String dateString = mDateFormat.format(mDate).toUpperCase();
        float dateWidth = mDatePaint.measureText(dateString);
        x = bounds.centerX() - dateWidth / 2;
        canvas.drawText(dateString, x, mYOffset + mLineHeight * 1.3f, mDatePaint);

        // Draw line
        float lineWidth = mContext.getResources().getDimension(R.dimen.watchface_line_length);
        float lineY = mYOffset + mLineHeight * 2;
        canvas.drawLine(bounds.centerX() - lineWidth/2, lineY, bounds.centerX() + lineWidth/2, lineY, mLinePaint);

        // Draw weather icon
        if (mWeatherIconResourceId != -1) {
            Bitmap weatherIcon = BitmapFactory.decodeResource(mContext.getResources(), mWeatherIconResourceId);
            // resize weather icon
            DisplayMetrics displayMetrics = mContext.getResources().getDisplayMetrics();
            float size = mContext.getResources().getDimension(R.dimen.watchface_weather_icon_size);
            weatherIcon = Bitmap.createScaledBitmap(weatherIcon, (int)size, (int)size, false);
            // draw weather icon
            float y = mYOffset + mLineHeight * 3.8f;
            canvas.drawBitmap(weatherIcon, bounds.centerX() - size * 1.5f, y - mLineHeight * 1.5f, mWeatherIconPaint);
        }

        // Draw max and min temperature
        float maxTempWidth = mMaxTempPaint.measureText(mMaxTempString);
        x = bounds.centerX() - maxTempWidth / 2;
        canvas.drawText(mMaxTempString, x, y, mMaxTempPaint);

        x = bounds.centerX() + maxTempWidth / 1.5f;
        canvas.drawText(mMinTempString, x, y, mMinTempPaint);
    }

    public void setWeatherIconResourceId(int resourceId) {
       mWeatherIconResourceId = resourceId;
    }
    public void setMaxTempString(String maxTempString) {
        mMaxTempString = maxTempString;
    }
    public void setMinTempString(String minTempString) {
        mMinTempString = minTempString;
    }

    private Paint createTextPaint(int colorResource, Typeface typeface) {
        Paint paint = new Paint();
        paint.setColor(ContextCompat.getColor(mContext, colorResource));
        paint.setTypeface(typeface);
        paint.setAntiAlias(true);

        return paint;
    }
}
