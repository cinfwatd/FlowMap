/*
  MIT License

  Copyright (c) 2017 Cinfwat Dogak

  Permission is hereby granted, free of charge, to any person obtaining a copy
  of this software and associated documentation files (the "Software"), to deal
  in the Software without restriction, including without limitation the rights
  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
  copies of the Software, and to permit persons to whom the Software is
  furnished to do so, subject to the following conditions:

  The above copyright notice and this permission notice shall be included in all
  copies or substantial portions of the Software.

  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
  SOFTWARE.
 */

package me.dcii.flowmap.service;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Geocoder;
import android.location.Location;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.ResultReceiver;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.text.TextUtils;
import android.widget.Toast;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.UUID;

import io.realm.Realm;
import me.dcii.flowmap.model.Journey;
import me.dcii.flowmap.util.Constants;

/**
 * Location service. Handles all location related operations.
 *
 * @author Dogak Cinfwat.
 */
public class FlowLocationService extends Service {

    /**
     * Class name tag for debugging.
     */
    @SuppressWarnings("unused")
    private final  static String TAG = FlowLocationService.class.getSimpleName();

    /**
     * The desired interval for location updates (milliseconds). Updates maybe more or less
     * frequent.
     */
    private static final long UPDATE_INTERVAL = 10000;

    /**
     * The fastest rate for active location updates. Updates will not be more frequent than this
     * value.
     */
    private static final long FASTEST_UPDATE_INTERVAL = UPDATE_INTERVAL / 2;

    /**
     * The smallest displacement distance in meters between location changed callbacks.
     */
    private static final float SMALLEST_DISPLACEMENT = 0.25f;

    /**
     * Provides access to the fused location provider API
     */
    private FusedLocationProviderClient mFusedLocationClient;

    /**
     * Provides access to the location settings Api.
     */
    private SettingsClient mSettingsClient;

    /**
     * Stores the types of location services the user is interested in using. Used for checking
     * settings to determine if the device has optimal location settings.
     */
    private LocationSettingsRequest mLocationSettingsRequest;

    /**
     * Stores parameters for request to the FusedLocationProviderApi.
     */
    private LocationRequest mLocationRequest;

    /**
     * Keeps track of the location update request. Allows the user to start and stop
     * the location tracking process.
     */
    private boolean mRequestingLocationUpdates;

    /**
     * Callback for the location events.
     */
    private LocationCallback mLocationCallback;

    /**
     * Represents the user's current geographical location at any given point.
     */
    private Location mCurrentLocation;

    /**
     * Represents the user {@link Journey}.
     */
    private Journey mJourney;

    /**
     * Represents the {@link Realm} instance.
     */
    Realm mRealm;

    /**
     * Binder instance for client connections.
     */
    private IBinder mBinder;

    /**
     * Receives the {@link FetchAddressIntentService} results.
     */
    private AddressResultReceiver mResultReceiver;

    /**
     * The location receiver where results are forwarded from this service.
     */
    private ResultReceiver mLocationResultReceiver;

    /**
     * Flags used to check if the address lookup intent service should be dispatched.
     */
    private boolean mIsStartLocationAddressFetched;
    private boolean mIsEndLocationAddressFetched;

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class FlowLocationServiceBinder extends Binder {
        public FlowLocationService getService() {
            // Return this instance of LocalService so clients can call public methods
            return FlowLocationService.this;
        }
    }

    @Override
    public void onCreate() {

        mJourney = null;
        mIsStartLocationAddressFetched = false;
        mIsEndLocationAddressFetched = false;

        mRealm = Realm.getDefaultInstance();  // opens the default realm.

        mResultReceiver = new AddressResultReceiver(new Handler());
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mSettingsClient = LocationServices.getSettingsClient(this);

        mBinder = new FlowLocationServiceBinder();

        // Kick of the process of building the LocationCallback, LocationRequest, and
        // LocationSettingsRequest objects.
        createLocationCallback();
        createLocationRequest();
        buildLocationSettingsRequest();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Gets the location receiver from the intent.
        mLocationResultReceiver = intent.getParcelableExtra(Constants.LOCATION_RECEIVER);

        return START_NOT_STICKY;
    }

    /**
     * Creates the callback for receiving location events.
     */
    private void createLocationCallback() {
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);

                mCurrentLocation = locationResult.getLastLocation();
                deliverLocationResult(mCurrentLocation);
                updateLocation();
            }
        };
    }

    private void deliverLocationResult(Location mCurrentLocation) {
        Bundle bundle = new Bundle();
        bundle.putParcelable(Constants.LOCATION_DATA_EXTRA, mCurrentLocation);
        mLocationResultReceiver.send(Constants.SUCCESS_RESULT, bundle);
    }

    /**
     * Updates user location with provided location information.
     */
    private void updateLocation() {
        if (mCurrentLocation != null && mRequestingLocationUpdates) {

            final LatLng latLng = new LatLng(mCurrentLocation.getLatitude(),
                    mCurrentLocation.getLongitude());
            Toast.makeText(this, latLng.toString(), Toast.LENGTH_SHORT).show();

            // Update the Journey instance from Realm.
            updateRealmObject(latLng);

            // Check to ensure the address lookup service is only requested if the current
            // Journey's start address is not yet found.
            if (!mIsStartLocationAddressFetched) {
                // Start the address service to get the Location address for the start location.
                startAddressIntentService(Constants.START_ADDRESS_LOOKUP);
            }
        }
    }

    /**
     * Creates the location request and sets the intervals and priority.
     */
    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();

        // Sets the desired interval for active location updates.
        mLocationRequest.setInterval(UPDATE_INTERVAL);

        // Sets the fastest rate for active location updates.
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL);
        mLocationRequest.setSmallestDisplacement(SMALLEST_DISPLACEMENT);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    /**
     * Uses the {@link LocationSettingsRequest.Builder} to build a {@link LocationSettingsRequest}
     * that is used for checking if the user's device has the needed location settings.
     */
    private void buildLocationSettingsRequest() {
        final LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        mLocationSettingsRequest = builder.build();
    }


    /**
     * Creates an intent, adds location data to it as an extra, and starts the intent service for
     * fetching an address from {@link LatLng} locations.
     *
     * @param addressRequestCode Specifies if start or end location address is requested.
     */
    private void startAddressIntentService(int addressRequestCode) {
        // Stop execution if no Geocoder is present.
        if (!Geocoder.isPresent()) return;

        // Create an intent for passing to the intent service responsible for fetching the address.
        final Intent intent = new Intent(this, FetchAddressIntentService.class);

        // Pass the result receiver as an extra to the service.
        intent.putExtra(Constants.ADDRESS_RECEIVER, mResultReceiver);

        // Specify the address being requested. Start or end address.
        intent.putExtra(Constants.ADDRESS_LOOKUP, addressRequestCode);

        // Speficy the journey Id. This enables the address to be passed on to the appropriate
        // Journey even if the journey was ended before the address was returned.
        intent.putExtra(Constants.JOURNEY_ID_ADDRESS_LOOK, mJourney.getId());

        // Pass last location data as an extra to the service.
        intent.putExtra(Constants.LOCATION_DATA_EXTRA, mCurrentLocation);

        // Start the service. If the service isn't already running, it is instantiated and started
        // (creating a process for it if needed); if it is running then it remains running. The
        // service kills itself automatically once all intents are processed.
        startService(intent);
    }

    /**
     * Requests location updates from the FuseLocationClientApi. This request is only performed
     * when runtime location permission has been granted in devices running android M and above.
     * Also, the user is notified if enabled.
     */
    @SuppressWarnings("MissingPermission")
    public void startLocationUpdates() {

        // Set mRequestingLocationUpdates to true and set mJourney to null.
        setRequestingLocationUpdates(true);
        initialiseJourney();
        // Check if the device has the necessary location settings.
        mSettingsClient.checkLocationSettings(mLocationSettingsRequest)

                .addOnSuccessListener(new OnSuccessListener<LocationSettingsResponse>() {
                    @Override
                    public void onSuccess(LocationSettingsResponse locationSettingsResponse) {

                        //noinspection MissingPermission
                        mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                                mLocationCallback, Looper.myLooper());
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        final int statusCode = ((ApiException) e).getStatusCode();
                        switch (statusCode) {
                            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
//                                 Location settings are not satisfied.
                                try {
                                    // Show dialog
                                    ResolvableApiException rae = (ResolvableApiException) e;
                                    // TODO: Handle: startResolutionForResult
                                    throw new IntentSender.SendIntentException();
                                } catch (IntentSender.SendIntentException sendEx) {
                                    // pendingIntent unable to execute request.
                                }
                                break;
                            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                setRequestingLocationUpdates(false);
                        }
                    }
                });
    }

    /**
     * Used to make necessary initialisations for a new {@link Journey}.
     */
    private void initialiseJourney() {
        mJourney = null;  // new Journey is created if the value is null;

        if (mIsStartLocationAddressFetched && !mIsEndLocationAddressFetched) {
            // If start address of the just concluded journey is found and the end address is not,
            // make a last request for the end location address lookup before this new journey
            // actually starts (Overrides last location, which is the previous journey's end location).
            startAddressIntentService(Constants.END_ADDRESS_LOOK_UP);
        }
    }

    /**
     * Removes location updates from the FusedLocationApi.
     */
    public void stopLocationUpdates() {
        if (!mRequestingLocationUpdates) {
            // Updates were never requested.
            return;
        }

        // Remove location request when activity is in a paused or stopped state.
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        setRequestingLocationUpdates(false);

        // Request address lookup with lastLocation as the end location.
        // Start address intent with the last recorded location.
        startAddressIntentService(Constants.END_ADDRESS_LOOK_UP);
    }

    /**
     * Returns the location updates status.
     *
     * @return the location update status flag.
     */
    public boolean isRequestingLocationUpdates() {
        return mRequestingLocationUpdates;
    }

    public Journey getJourney() {
        return mJourney;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Close Realm distance.
        mRealm.close();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    /**
     * Sets {@link #mRequestingLocationUpdates} status from the provided value.
     *
     * @param requestingUpdates current request status.
     */
    private void setRequestingLocationUpdates(boolean requestingUpdates) {
        mRequestingLocationUpdates = requestingUpdates;
    }

    /**
     * Sets the {@link LatLng} journey position in the Realm {@link #mJourney} instance. If Journey
     * is null, a new Journey is started.
     *
     * @param latLng the location to update.
     */
    private void updateRealmObject(LatLng latLng) {
        // Persist realm objects in a transaction. This is done on the main thread as Realm is
        // quite fast as specified in the docs but this could be updated to an async transaction.
        mRealm.beginTransaction();
        if (mJourney == null) {

            // mJourney is null; Create new journey.
            mJourney = mRealm.createObject(Journey.class, UUID.randomUUID().toString());
        }

        // TODO: Check user transport type.
        // Adds LatLng position in the model list of intermediate locations.
        mJourney.addLocation(latLng);

        // Commit transaction if all goes well.
        mRealm.commitTransaction();
    }

    /**
     * Checks the current state of location permissions needed.
     *
     * @return the permission status.
     */
    private boolean checkLocationPermission() {

        final int permissionState = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }


    /**
     * Sets the start address for the {@link Journey} with the given {@link Journey#id} identifier.
     *
     * @param journeyId the {@link Journey#id}.
     * @param address the {@link Journey#startAddress}.
     */
    private void setJourneyStartAddress(final String journeyId, final String address) {

        // Check if the address is for the current journey. This is because the user could have
        // Started a new journey since the lookup address request.
        if (mJourney != null && TextUtils.equals(mJourney.getId(), journeyId)) {

            // Begin Realm transaction to update journey. Realm transactions are compulsory.
            mRealm.beginTransaction();

            // Update the journey start address.
            mJourney.setStartAddress(address);

            // update flag to stop further start location address lookup.
            mIsStartLocationAddressFetched = true;

            // commit transaction if all goes well.
            mRealm.commitTransaction();
        } else {
            // Address belongs to a different journey.
            // Update the journey from the Realm store.

            mRealm.executeTransactionAsync(new Realm.Transaction() {
                @Override
                public void execute(@NonNull Realm realm) {
                    Journey journey = realm.where(Journey.class)
                            .equalTo(Journey.FIELD_ID, journeyId).findFirst();
                    if (journey != null) {
                        journey.setStartAddress(address);
                    }
                }
            });
        }
    }

    /**
     * Sets the end address for the {@link Journey} with the given {@link Journey#id} identifier.
     *
     * @param journeyId the {@link Journey#id}.
     * @param address the {@link Journey#endAddress}.
     */
    private void setJourneyEndAddress(final String journeyId, final String address) {

        // Check if the address is for the current journey. This is because the user could have
        // Started a new journey since the lookup address request.
        if (mJourney != null && TextUtils.equals(mJourney.getId(), journeyId)) {

            //Begin Realm transaction to update the journey. Realm transactions are compulsory.
            mRealm.beginTransaction();

            // Update the journey end address asynchronously.
            mJourney.setEndAddress(address);

            // update flag to stop further start location address lookup.
            mIsEndLocationAddressFetched = true;

            // Commit transactions.
            mRealm.commitTransaction();

        } else {
            // Address belongs to a different journey.
            // Update the journey from the Realm store.

            mRealm.executeTransactionAsync(new Realm.Transaction() {
                @Override
                public void execute(@NonNull Realm realm) {
                    Journey journey = realm.where(Journey.class)
                            .equalTo(Journey.FIELD_ID, journeyId).findFirst();
                    if (journey != null) {
                        journey.setEndAddress(address);
                    }
                }
            });
        }
    }

    /**
     * Receives address results from the {@link FetchAddressIntentService}.
     */
    class AddressResultReceiver extends ResultReceiver {
        AddressResultReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {

            // Get the address string
            // or an error message sent from the intent service.
            final String resultString = resultData.getString(Constants.RESULT_DATA_KEY);

            if (resultCode == Constants.SUCCESS_RESULT) {
                // Check the location address lookup requested: Start or end address.
                final int addressRequestCode = resultData.getInt(Constants.ADDRESS_LOOKUP);

                // Get the journey Id. Used to pass the address to the appropriate journey.
                final String journeyId = resultData.getString(Constants.JOURNEY_ID_ADDRESS_LOOK);

                switch (addressRequestCode) {
                    case Constants.START_ADDRESS_LOOKUP:
                        setJourneyStartAddress(journeyId, resultString);
                        break;
                    case Constants.END_ADDRESS_LOOK_UP:
                        setJourneyEndAddress(journeyId, resultString);
                }
            } else {
                // Request failed.
                Toast.makeText(FlowLocationService.this, resultString, Toast.LENGTH_SHORT).show();
            }
        }
    }
}