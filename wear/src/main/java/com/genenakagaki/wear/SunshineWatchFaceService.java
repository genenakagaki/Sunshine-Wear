package com.genenakagaki.wear;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.util.Log;
import android.view.SurfaceHolder;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;

import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import static android.R.id.message;

/**
 * Created by gene on 1/24/17.
 */

public class SunshineWatchFaceService extends CanvasWatchFaceService {

    private static final String TAG = SunshineWatchFaceService.class.getSimpleName();

    private static final Typeface BOLD_TYPEFACE = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD);

    private static final long NORMAL_UPDATE_RATE_MS = 500;

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private class Engine extends CanvasWatchFaceService.Engine
            implements
            GoogleApiClient.ConnectionCallbacks,
            GoogleApiClient.OnConnectionFailedListener,
            MessageApi.MessageListener {

        static final int MSG_UPDATE_TIME = 0;

        long mInteractiveUpdateRateMs = TimeUnit.MINUTES.toMillis(1);

        // Handler to update the time periodically in interactive mode.
        final Handler mUpdateTimeHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MSG_UPDATE_TIME:
                        invalidate();
                        if (shouldTimerBeRunning()) {
                            long timeMs = System.currentTimeMillis();
                            long delayMs = mInteractiveUpdateRateMs - (timeMs % mInteractiveUpdateRateMs);
                            mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
                        }
                        break;
                }
            }
        };

        SunshineWatchFaceUI mSunshineWatchFaceUI;

        private GoogleApiClient mGoogleApiClient;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            mSunshineWatchFaceUI = new SunshineWatchFaceUI(SunshineWatchFaceService.this);

            mGoogleApiClient = new GoogleApiClient.Builder(SunshineWatchFaceService.this)
                    .addApi(Wearable.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();

            mGoogleApiClient.connect();
        }



        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                mGoogleApiClient.connect();
            } else {
                if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
                    mGoogleApiClient.disconnect();
                }
            }
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);

            mSunshineWatchFaceUI.adjustToCurrentMode(inAmbientMode);
            invalidate();
        }

        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            mSunshineWatchFaceUI.draw(canvas, bounds);
        }

        @Override
        public void onConnected(@Nullable Bundle bundle) {
            //Log.d(TAG, "onConnected: " + bundle);

            Wearable.MessageApi.addListener(mGoogleApiClient, this);
        }

        @Override
        public void onConnectionSuspended(int i) {
            Log.d(TAG, "onConnectionSuspended: " + i);
        }

        @Override
        public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
            //Log.d(TAG, "onConnectionFailed: " + connectionResult);
        }

        @Override
        public void onMessageReceived(final MessageEvent messageEvent) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                public void run() {
                    //Log.d(TAG, "onMessageReceived");
                    String message = new String(messageEvent.getData());
                    //Log.d(TAG, message);

                    String[] values = message.split(",");

                    int weatherId = Integer.valueOf(values[0]);
                    int weatherIcon;

                    if (weatherId >= 200 && weatherId <= 232) {
                        weatherIcon = R.drawable.ic_storm;
                    } else if (weatherId >= 300 && weatherId <= 321) {
                        weatherIcon = R.drawable.ic_light_rain;
                    } else if (weatherId >= 500 && weatherId <= 504) {
                        weatherIcon = R.drawable.ic_rain;
                    } else if (weatherId == 511) {
                        weatherIcon = R.drawable.ic_snow;
                    } else if (weatherId >= 520 && weatherId <= 531) {
                        weatherIcon = R.drawable.ic_rain;
                    } else if (weatherId >= 600 && weatherId <= 622) {
                        weatherIcon = R.drawable.ic_snow;
                    } else if (weatherId >= 701 && weatherId <= 761) {
                        weatherIcon = R.drawable.ic_fog;
                    } else if (weatherId == 761 || weatherId == 771 || weatherId == 781) {
                        weatherIcon = R.drawable.ic_storm;
                    } else if (weatherId == 800) {
                        weatherIcon = R.drawable.ic_clear;
                    } else if (weatherId == 801) {
                        weatherIcon = R.drawable.ic_light_clouds;
                    } else if (weatherId >= 802 && weatherId <= 804) {
                        weatherIcon = R.drawable.ic_cloudy;
                    } else if (weatherId >= 900 && weatherId <= 906) {
                        weatherIcon = R.drawable.ic_storm;
                    } else if (weatherId >= 958 && weatherId <= 962) {
                        weatherIcon = R.drawable.ic_storm;
                    } else {
                        weatherIcon = R.drawable.ic_clear;
                    }

                    mSunshineWatchFaceUI.setWeatherIconResourceId(weatherIcon);

                    mSunshineWatchFaceUI.setMaxTempString(values[1]);
                    mSunshineWatchFaceUI.setMinTempString(values[2]);

                    invalidate();
                }
            });
        }
    }
}
