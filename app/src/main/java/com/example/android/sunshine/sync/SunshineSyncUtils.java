/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.sunshine.sync;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.example.android.sunshine.MainActivity;
import com.example.android.sunshine.R;
import com.example.android.sunshine.data.WeatherContract;
import com.example.android.sunshine.utilities.SunshineDateUtils;
import com.example.android.sunshine.utilities.SunshineWeatherUtils;
import com.firebase.jobdispatcher.Constraint;
import com.firebase.jobdispatcher.Driver;
import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.firebase.jobdispatcher.Job;
import com.firebase.jobdispatcher.Lifetime;
import com.firebase.jobdispatcher.Trigger;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;

import static com.google.android.gms.wearable.DataMap.TAG;

public class SunshineSyncUtils {

    /*
     * Interval at which to sync with the weather. Use TimeUnit for convenience, rather than
     * writing out a bunch of multiplication ourselves and risk making a silly mistake.
     */
    private static final int SYNC_INTERVAL_HOURS = 3;
    private static final int SYNC_INTERVAL_SECONDS = (int) TimeUnit.HOURS.toSeconds(SYNC_INTERVAL_HOURS);
    private static final int SYNC_FLEXTIME_SECONDS = SYNC_INTERVAL_SECONDS / 3;

    private static boolean sInitialized;

    private static final String SUNSHINE_SYNC_TAG = "sunshine-sync";

    public static GoogleApiClient sGoogleApiClient;
    public static boolean sWearConnected;

    private static final String WEATHER_PATH = "/weather";

    /**
     * Schedules a repeating sync of Sunshine's weather data using FirebaseJobDispatcher.
     * @param context Context used to create the GooglePlayDriver that powers the
     *                FirebaseJobDispatcher
     */
    static void scheduleFirebaseJobDispatcherSync(@NonNull final Context context) {

        Driver driver = new GooglePlayDriver(context);
        FirebaseJobDispatcher dispatcher = new FirebaseJobDispatcher(driver);

        /* Create the Job to periodically sync Sunshine */
        Job syncSunshineJob = dispatcher.newJobBuilder()
                /* The Service that will be used to sync Sunshine's data */
                .setService(SunshineFirebaseJobService.class)
                /* Set the UNIQUE tag used to identify this Job */
                .setTag(SUNSHINE_SYNC_TAG)
                /*
                 * Network constraints on which this Job should run. We choose to run on any
                 * network, but you can also choose to run only on un-metered networks or when the
                 * device is charging. It might be a good idea to include a preference for this,
                 * as some users may not want to download any data on their mobile plan. ($$$)
                 */
                .setConstraints(Constraint.ON_ANY_NETWORK)
                /*
                 * setLifetime sets how long this job should persist. The options are to keep the
                 * Job "forever" or to have it die the next time the device boots up.
                 */
                .setLifetime(Lifetime.FOREVER)
                /*
                 * We want Sunshine's weather data to stay up to date, so we tell this Job to recur.
                 */
                .setRecurring(true)
                /*
                 * We want the weather data to be synced every 3 to 4 hours. The first argument for
                 * Trigger's static executionWindow method is the start of the time frame when the
                 * sync should be performed. The second argument is the latest point in time at
                 * which the data should be synced. Please note that this end time is not
                 * guaranteed, but is more of a guideline for FirebaseJobDispatcher to go off of.
                 */
                .setTrigger(Trigger.executionWindow(
                        SYNC_INTERVAL_SECONDS,
                        SYNC_INTERVAL_SECONDS + SYNC_FLEXTIME_SECONDS))
                /*
                 * If a Job with the tag with provided already exists, this new job will replace
                 * the old one.
                 */
                .setReplaceCurrent(true)
                /* Once the Job is ready, call the builder's build method to return the Job */
                .build();

        /* Schedule the Job with the dispatcher */
        dispatcher.schedule(syncSunshineJob);
    }
    /**
     * Creates periodic sync tasks and checks to see if an immediate sync is required. If an
     * immediate sync is required, this method will take care of making sure that sync occurs.
     *
     * @param context Context that will be passed to other methods and used to access the
     *                ContentResolver
     */
    synchronized public static void initialize(@NonNull final Context context) {
        Log.d("TEST", "here");

        sGoogleApiClient = new GoogleApiClient.Builder(context)
                .addApi(Wearable.API)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(@Nullable Bundle bundle) {
                        Log.d("TEST", "onConnecteed");
                        sWearConnected = true;

                        String[] projection = {
                                WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
                                WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
                                WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
                                WeatherContract.WeatherEntry.COLUMN_DATE
                        };


                        long normalizedUtcStartDay = SunshineDateUtils.getNormalizedUtcDateForToday();

                        int dateIndex = 3;
                        Cursor weatherCursor = context.getContentResolver().query(
                                WeatherContract.WeatherEntry.CONTENT_URI,
                                /* Columns; leaving this null returns every column in the table */
                                projection,
                                /* Optional specification for columns in the "where" clause above */
                                projection[dateIndex] + " = ?",
                                /* Values for "where" clause */
                                new String[] {String.valueOf(normalizedUtcStartDay)},
                                /* Sort order to return in Cursor */
                                null);

                        sendWeatherMessage(weatherCursor, context);
                    }

                    @Override
                    public void onConnectionSuspended(int i) {
                        sWearConnected = false;
                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
                        Log.d("TEST","onConnectionFailed:"+connectionResult.getErrorCode()+","+connectionResult.getErrorMessage());

                        sWearConnected = false;
                    }
                })
                .build();

        sGoogleApiClient.connect();


        /*
         * Only perform initialization once per app lifetime. If initialization has already been
         * performed, we have nothing to do in this method.
         */
        if (sInitialized) return;

        sInitialized = true;

        /*
         * This method call triggers Sunshine to create its task to synchronize weather data
         * periodically.
         */
        scheduleFirebaseJobDispatcherSync(context);

        /*
         * We need to check to see if our ContentProvider has data to display in our forecast
         * list. However, performing a query on the main thread is a bad idea as this may
         * cause our UI to lag. Therefore, we create a thread in which we will run the query
         * to check the contents of our ContentProvider.
         */
        Thread checkForEmpty = new Thread(new Runnable() {
            @Override
            public void run() {

                /* URI for every row of weather data in our weather table*/
                Uri forecastQueryUri = WeatherContract.WeatherEntry.CONTENT_URI;

                /*
                 * Since this query is going to be used only as a check to see if we have any
                 * data (rather than to display data), we just need to PROJECT the ID of each
                 * row. In our queries where we display data, we need to PROJECT more columns
                 * to determine what weather details need to be displayed.
                 */
                String[] projectionColumns = {WeatherContract.WeatherEntry._ID};
                String selectionStatement = WeatherContract.WeatherEntry
                        .getSqlSelectForTodayOnwards();

                /* Here, we perform the query to check to see if we have any weather data */
                Cursor cursor = context.getContentResolver().query(
                        forecastQueryUri,
                        projectionColumns,
                        selectionStatement,
                        null,
                        null);
                /*
                 * A Cursor object can be null for various different reasons. A few are
                 * listed below.
                 *
                 *   1) Invalid URI
                 *   2) A certain ContentProvider's query method returns null
                 *   3) A RemoteException was thrown.
                 *
                 * Bottom line, it is generally a good idea to check if a Cursor returned
                 * from a ContentResolver is null.
                 *
                 * If the Cursor was null OR if it was empty, we need to sync immediately to
                 * be able to display data to the user.
                 */
                if (null == cursor || cursor.getCount() == 0) {
                    startImmediateSync(context);
                }

                /* Make sure to close the Cursor to avoid memory leaks! */
                cursor.close();
            }
        });

        /* Finally, once the thread is prepared, fire it off to perform our checks. */
        checkForEmpty.start();
    }

    public static void sendWeatherMessage(final Cursor cursor, final Context context) {

        new AsyncTask<Void, Void, HashSet<String>>() {

            @Override
            protected HashSet<String> doInBackground(Void... params) {
                // get nodes
                NodeApi.GetConnectedNodesResult nodes =
                        Wearable.NodeApi.getConnectedNodes(sGoogleApiClient).await();

                HashSet<String> nodeIds = new HashSet<>();
                for (Node node : nodes.getNodes()) {
                    nodeIds.add(node.getId());
                }

                return nodeIds;
            }

            @Override
            protected void onPostExecute(HashSet<String> nodeIds) {
                if (cursor.moveToFirst()) {
                    Log.d("TEST", "cursor is good");
                    // create comma separated values
                    // weatherId, temperature max, temperature low
                    int weatherIdIndex = cursor.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_WEATHER_ID);
                    int tempMaxIndex = cursor.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_MAX_TEMP);
                    int tempMinIndex = cursor.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_MIN_TEMP);

                    String weatherIdString = String.valueOf(cursor.getInt(weatherIdIndex));

                    /* Read high temperature from the cursor (in degrees celsius) */
                    double highInCelsius = cursor.getDouble(tempMaxIndex);
                    String highString = SunshineWeatherUtils.formatTemperature(context, highInCelsius);

                    double lowInCelsius = cursor.getDouble(tempMinIndex);
                    String lowString = SunshineWeatherUtils.formatTemperature(context, lowInCelsius);

                    String messageString = weatherIdString + "," + highString + "," + lowString;
                    byte[] message = messageString.getBytes();

                    for (String node: nodeIds) {
                        Log.d("TEST", "Send Message");
                        Wearable.MessageApi.sendMessage(sGoogleApiClient, node, WEATHER_PATH, message)
                            .setResultCallback(new ResultCallback() {

                                @Override
                                public void onResult(@NonNull Result result) {
                                    if (!result.getStatus().isSuccess()) {
                                        Log.d("HELLO", "Failed to send message");
                                    } else {
                                        Log.d("HELLO", "Success to send message");
                                    }
                                }
                            });
                    }
                } else {
                    Log.d("TEST", "cursor is null");
                }
            }
        }.execute();



    }

    /**
     * Helper method to perform a sync immediately using an IntentService for asynchronous
     * execution.
     *
     * @param context The Context used to start the IntentService for the sync.
     */
    public static void startImmediateSync(@NonNull final Context context) {
        Intent intentToSyncImmediately = new Intent(context, SunshineSyncIntentService.class);
        context.startService(intentToSyncImmediately);
    }
}