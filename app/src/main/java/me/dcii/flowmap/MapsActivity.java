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

package me.dcii.flowmap;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
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
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.text.DateFormat;
import java.util.Date;
import java.util.UUID;

import io.realm.Realm;
import io.realm.RealmList;
import me.dcii.flowmap.model.Journey;

/**
 * Presents the map to the user. Implements {@link OnMapReadyCallback} interface for when
 * the map is ready to be used; {@link LocationSource} interface to provide location updates using
 * the {@link GoogleMap#setMyLocationEnabled(boolean)} option; and
 * {@link android.view.View.OnClickListener} to handle the {@link FloatingActionButton} clicks.
 *
 * @author Dogak Cinfwat.
 */
public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback,
        LocationSource, View.OnClickListener {


    /**
     * Class name tag for debugging.
     */
    @SuppressWarnings("unused")
    private final  static String TAG = MapsActivity.class.getSimpleName();

    /**
     * Code used in requesting runtime location permission.
     */
    private static final int REQUEST_CODE_LOCATION = 1;

    /**
     * Code used in checking if location settings are satisfied.
     */
    private static final int REQUEST_CODE_CHECK_SETTINGS = 10;

    /**
     * Keys for storing activity state in bundle.
     */
    private static final String KEY_REQUESTING_LOCATION_UPDATES = "requesting-location-update";
    private static final String KEY_LOCATION = "last-location";
    private static final String KEY_LAST_UPDATED_TIME = "last-updated-time";
    private static final String KEY_START_MARKER_POSITION = "start-marker-position";
    private static final String KEY_END_MARKER_POSITION = "end-marker-position";
    private static final String KEY_JOURNEY_ID = "journey-id";

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
     * Represents the Google map object.
     */
    private GoogleMap mMap;

    /**
     * Floating action button.
     */
    private FloatingActionButton mFab;

    /**
     * Map marker for journey starting location.
     */
    private Marker mStartMarker = null;

    /**
     * Map marker {@link LatLng} position. Used to rebuild start marker on configuration change.
     */
    private LatLng mStartMarkerPosition = null;

    /**
     * Map marker for journey end location.
     */
    private Marker mEndMarker = null;

    /**
     * Map marker {@link LatLng} position. Used to rebuild end marker on configuration change.
     */
    private LatLng mEndMarkerPosition = null;

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
     * Provides the location change listener used for the location source interface.
     */
    private OnLocationChangedListener mLocationChangeListener;

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
     * Time when location was last updated represented as a string.
     */
    private String mLastUpdateTime;

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
     * Represents the {@link Journey} primary identifier.
     */
    private String mJourneyId;

    /**
     * Represents the {@link Realm} instance.
     */
    Realm mRealm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        final SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        final Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mFab = findViewById(R.id.fab);
        mFab.setOnClickListener(this);

        mRequestingLocationUpdates = false;
        mLastUpdateTime = "";
        mJourney = null;
        mJourneyId = null;

        mRealm = Realm.getDefaultInstance();  // opens the default realm.

        // Updates values from previous instance of the activity.
        updateValuesFromBundle(savedInstanceState);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mSettingsClient = LocationServices.getSettingsClient(this);
        mLocationChangeListener = null;

        // Kick of the process of building the LocationCallback, LocationRequest, and
        // LocationSettingsRequest objects.
        createLocationCallback();
        createLocationRequest();
        buildLocationSettingsRequest();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        // inflate maps menu.
        inflater.inflate(R.menu.maps_menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.navigation_journeys) {
            final Intent journeysActivity = new Intent(this, JourneysActivity.class);
            startActivity(journeysActivity);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Sets the tint color of the {@link FloatingActionButton} to white.
     *
     * @param colorRes Color resource id.
     */
    private void setFabTintColor(int colorRes) {
        mFab.getDrawable().setColorFilter(
                ContextCompat.getColor(this, colorRes), PorterDuff.Mode.SRC_IN);
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
                mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());

                if (mLocationChangeListener != null) {
                    mLocationChangeListener.onLocationChanged(mCurrentLocation);
                }
                updateLocation();
            }
        };
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
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setMyLocationButtonEnabled(false);
        mMap.setLocationSource(this);
        setMyLocationEnabled(mRequestingLocationUpdates);
        updateUI();
    }

    /**
     * Sets {@link GoogleMap#setMyLocationEnabled(boolean)}.
     *
     * @param myLocationEnabled boolean location enabled flag.
     */
    private void setMyLocationEnabled(boolean myLocationEnabled) {
        try {
            mMap.setMyLocationEnabled(myLocationEnabled);
        } catch (SecurityException ex) {
            // Initiate location permission request.
            requestLocationPermission();
        }
    }

    /**
     * Updates app state from provided previous instance of the activity.
     *
     * @param savedInstanceState previous activity instance bundle.
     */
    private void updateValuesFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            // Update the value of mRequestingLocationUpdates.
            if (savedInstanceState.keySet().contains(KEY_REQUESTING_LOCATION_UPDATES)) {
                mRequestingLocationUpdates = savedInstanceState.getBoolean(
                        KEY_REQUESTING_LOCATION_UPDATES);
            }

            // Update the value of mCurrentLocation.
            if (savedInstanceState.keySet().contains(KEY_LOCATION)) {
                mCurrentLocation = savedInstanceState.getParcelable(KEY_LOCATION);
            }

            // Update the value of mLastUpdateTime.
            if (savedInstanceState.keySet().contains(KEY_LAST_UPDATED_TIME)) {
                mLastUpdateTime = savedInstanceState.getString(KEY_LAST_UPDATED_TIME);
            }

            // Update the value start marker.
            if (savedInstanceState.keySet().contains(KEY_START_MARKER_POSITION)) {
                mStartMarkerPosition = savedInstanceState.getParcelable(KEY_START_MARKER_POSITION);
            }

            // Update the value end marker.
            if (savedInstanceState.keySet().contains(KEY_END_MARKER_POSITION)) {
                mEndMarkerPosition = savedInstanceState.getParcelable(KEY_END_MARKER_POSITION);
            }

            // Update journey model id.
            if (savedInstanceState.keySet().contains(KEY_JOURNEY_ID)) {
                mJourneyId = savedInstanceState.getString(KEY_JOURNEY_ID);
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!checkLocationPermission()) {
            requestLocationPermission();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mRequestingLocationUpdates && checkLocationPermission()) {
            startLocationUpdates(false);
        }
//        TODO: Retrieve shared preferences.
    }

    /**
     * Requests location updates from the FuseLocationClientApi. This request is only performed
     * when runtime location permission has been granted in devices running android M and above.
     * Also, the user is notified if enabled.
     *
     * @param notify flag used to decide on notifying the user of the location request status.
     */
    @SuppressWarnings("MissingPermission")
    private void startLocationUpdates(final boolean notify) {

        // Check if the device has the necessary location settings.
        mSettingsClient.checkLocationSettings(mLocationSettingsRequest)
                .addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
                    @Override
                    public void onSuccess(LocationSettingsResponse locationSettingsResponse) {

                        // if notify false, device is rotated.
                        if (notify) {
                            setRequestingLocationUpdates(true,
                                    R.string.requesting_location_updates_start);

                            // Clears map markings.
                            reInitialiseMap();
                        } else {
                            setRequestingLocationUpdates(true);
                        }
                        //noinspection MissingPermission
                        mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                                mLocationCallback, Looper.myLooper());
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        final int statusCode = ((ApiException) e).getStatusCode();
                        switch (statusCode) {
                            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                // Location settings are not satisfied.
                                try {
                                    // Show dialog
                                    ResolvableApiException rae = (ResolvableApiException) e;
                                    rae.startResolutionForResult(MapsActivity.this,
                                            REQUEST_CODE_CHECK_SETTINGS);
                                } catch (IntentSender.SendIntentException sendEx) {
                                    // pendingIntent unable to execute request.
                                }
                                break;
                            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                setRequestingLocationUpdates(false,
                                        R.string.error_inadequate_location_settings);
                        }
                    }
                })
                .addOnCompleteListener(this,
                        new OnCompleteListener<LocationSettingsResponse>() {
                            @Override
                            public void onComplete(@NonNull Task<LocationSettingsResponse> task) {
                                updateUI();
                            }
                        });
    }

    /**
     * Updates UI fields.
     */
    private void updateUI() {
        setFabEnabledState();
        updateMarkers();
        updateLocation();
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Remove location updates to save battery.
        stopLocationUpdates();
//        TODO: Set shared preferences.
    }

    /**
     * Removes location updates from the FusedLocationApi
     */
    private void stopLocationUpdates() {
        if (!mRequestingLocationUpdates) {
            // Updates were never requested.
            return;
        }

        // Remove location request when activity is in a paused or stopped state.
        mFusedLocationClient.removeLocationUpdates(mLocationCallback)
                // TODO: OnCompleteListener is always not called on first request despite removing updates.
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {

                        // Show end location marker.
                        final LatLng latLng = new LatLng(mCurrentLocation.getLatitude(),
                                mCurrentLocation.getLongitude());
                        setRequestingLocationUpdates(false,
                                R.string.requesting_location_updates_stop);
                        addLocationEndMarker(latLng);

                        // Update the user's end location.
                        updateRealmObject(latLng);
                        updateUI();
                    }
                });
    }

    /**
     * Sets {@link FloatingActionButton} drawable image to represent available action.
     * The {@link FloatingActionButton} can be used to request update and stop requesting.
     */
    private void setFabEnabledState() {
        if (mRequestingLocationUpdates) {
            // Requesting location updates; show the disable location updates icon.
            mFab.setImageDrawable(ContextCompat.getDrawable(
                    MapsActivity.this, R.drawable.ic_location_off_black_24dp));
        } else {
            // Not requesting location updates; show enable location updates icon.
            mFab.setImageDrawable(ContextCompat.getDrawable(
                    MapsActivity.this, R.drawable.ic_location_on_black_24dp));
        }
        // Set icon tint color to white.
        setFabTintColor(android.R.color.white);
    }

    /**
     * Saves activity data in Bundle.
     *
     * @param outState bundle to store activity.
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(KEY_REQUESTING_LOCATION_UPDATES, mRequestingLocationUpdates);
        outState.putString(KEY_LAST_UPDATED_TIME, mLastUpdateTime);
        outState.putParcelable(KEY_LOCATION, mCurrentLocation);
        outState.putParcelable(KEY_START_MARKER_POSITION, mStartMarkerPosition);
        outState.putParcelable(KEY_END_MARKER_POSITION, mEndMarkerPosition);
        outState.putString(KEY_JOURNEY_ID, mJourneyId);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Close Realm distance.
        mRealm.close();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            // Check for integer request code originally supplied to startResolutionForResult().
            case REQUEST_CODE_CHECK_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        startLocationUpdates(true);
                        break;
                    case Activity.RESULT_CANCELED:
                        // User chose not to make required location changes.
                        mRequestingLocationUpdates = false;
                        updateUI();
                        break;
                }
                break;
        }
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
     * Sets {@link #mRequestingLocationUpdates} status and informs the user of the change.
     *
     * @param requestingUpdates current request status.
     * @param messageRes the user message resource Id.
     */
    private void setRequestingLocationUpdates(boolean requestingUpdates, @StringRes int messageRes) {
        setRequestingLocationUpdates(requestingUpdates);
        Toast.makeText(this, messageRes, Toast.LENGTH_SHORT).show();
    }

    /**
     * Zooms-in on current user location with some animation. Also, adds the start location marker.
     *
     * @param latLng provided user {@link LatLng}.
     */
    private void updateLocationAnimate(LatLng latLng) {
        // Stop execution if mMap is null.
        if (mMap == null) return;

        // add location start marker.
        addLocationStartMarker(latLng);

        // animate and zoom-in to provided LatLng position.
        mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
        final CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(latLng)
                .zoom(17)
                .bearing(90)
                .tilt(40)
                .build();
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
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

            if (mStartMarker == null) {
                // Marker is null on first start. Hence animate and zoom-in on user's location.
                updateLocationAnimate(latLng);
            } else {
                drawRoute();
            }
        }
    }

    /**
     * Sets the {@link LatLng} journey position in the Realm {@link #mJourney} instance. If Journey
     * is null, a new Journey is started and the Id stored in {@link #mJourneyId} else the journey is
     * fetched from the {@link Realm} store using the {@link #mJourneyId} identifier.
     *
     * @param latLng the location to update.
     */
    private void updateRealmObject(LatLng latLng) {
        // Persist realm objects in a transaction. This is done on the main thread as Realm is
        // quite fast as specified in the docs but this could be updated to an async transaction.
        mRealm.beginTransaction();
        if (mJourney == null) {

            if (mJourneyId == null) {
                // Both mJourney and mJourneyId are null; Create new journey.
                mJourney = mRealm.createObject(Journey.class, UUID.randomUUID().toString());

                // Get Journey identifier for restoring on configuration change.
                // There's very little overhead in re-accessing the data from Realm store.
                mJourneyId = mJourney.getId();
            } else {
                // mJourneyId is not null on configuration change if Its value was restored.
                // Get mJourney instance from the Realm store.
                mJourney = mRealm.where(Journey.class).equalTo(Journey.FIELD_ID, mJourneyId).findFirst();
            }
        }
        // TODO: Check to make sure the user has moved.
        // TODO: Check user transport type.
        // Adds LatLng position in the model list of intermediate locations.
        if (mJourney != null) mJourney.addLocation(latLng);  // extra null check as findFirst() might return null.

        // Commit transaction if all goes well.
        mRealm.commitTransaction();
    }

    /**
     * Adds location markers to map provided the respective marker position is not null.
     * This is used during device rotations to restore marker position.
     */
    private void updateMarkers() {

        addLocationStartMarker(mStartMarkerPosition);
        addLocationEndMarker(mEndMarkerPosition);
        drawRoute();
    }

    /**
     * Adds the start location marker to the map.
     *
     * @param latLng the start location {@link LatLng}.
     */
    private void addLocationStartMarker(LatLng latLng) {
        // Stop execution if mMap or latLng is null.
        if (mMap == null || latLng == null) return;

        final MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        markerOptions.title(getString(R.string.start_location));

        // Set marker position. Used to reposition the marker on configuration change.
        mStartMarkerPosition = latLng;

        // Set marker end location color to red.
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
        mStartMarker = mMap.addMarker(markerOptions);
    }

    /**
     * Adds the end location marker to the map and removes the current user location marker.
     *
     * @param latLng the end location {@link LatLng}.
     */
    private void addLocationEndMarker(LatLng latLng) {
        // Stop execution if mMap or lat is null.
        if (mMap == null || latLng == null) return;

        final MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        markerOptions.title(getString(R.string.end_location));

        // Set marker position. Used to reposition the marker on configuration change.
        mEndMarkerPosition = latLng;

        // Set marker end location color to red.
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
        mEndMarker = mMap.addMarker(markerOptions);
    }

    /**
     * Clears map markings and enable {@link GoogleMap#setMyLocationEnabled(boolean)}.
     */
    private void reInitialiseMap() {
        // Stop execution if mMap is mull.
        if (mMap == null) return;

        mMap.clear();
        if (mStartMarker != null) {
            mStartMarker.remove();
            mStartMarker = null;
            mStartMarkerPosition = null;
        }
        if (mEndMarker != null) {
            mEndMarker.remove();
            mEndMarker = null;
            mEndMarkerPosition = null;
        }
        setMyLocationEnabled(mRequestingLocationUpdates);
        // prepare for new Journey. set journey and journeyId to null and a new Journey will be
        // created when location updates are requested.
        mJourney = null;
        mJourneyId = null;
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
     * Request location permission from the user and show the request rationale where necessary.
     */
    private void requestLocationPermission() {

        // Provide rationale to the user. This would happens when the user denies a
        // previous request.
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.ACCESS_FINE_LOCATION)) {
            showRationaleDialog();
        } else {
            // Request permission.
            performLocationPermissionRequest();
        }
    }

    /**
     * Does the actual location permission request.
     */
    private void performLocationPermissionRequest() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                REQUEST_CODE_LOCATION);
    }

    /**
     * Draws the user's route on the map using the locations stored in Realm store.
     */
    private void drawRoute() {
        // Stop execution if mMap or mJourney is empty.
        if (mMap == null || mJourney == null) {
            return;
        }

        final PolylineOptions options = new PolylineOptions().width(5).color(Color.BLUE).geodesic(true);
        final RealmList<me.dcii.flowmap.model.Location> journeyLocations = mJourney.getLocations();

        for (int index = 0; index < journeyLocations.size(); index++) {

            final me.dcii.flowmap.model.Location location = journeyLocations.get(index);
            final LatLng point = new LatLng(location.getLatitude(), location.getLongitude());
            options.add(point);
        }
        mMap.addPolyline(options);
    }

    /**
     * Callback received when the permissions request has completed.
     *
     * @param requestCode request code
     * @param permissions permissions
     * @param grantResults the granted results array.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_LOCATION: {
                if (grantResults.length <= 0) {
                    Toast.makeText(this, "Cancelled", Toast.LENGTH_SHORT).show();
                    // User interaction was interrupted, the permission request was cancelled.
                } else if  (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted.
                    if (mRequestingLocationUpdates) {
                        startLocationUpdates(true);
                    }
                } else {
                    // Permission denied. Notify user that they have rejected a core permission
                    // for the app.
                    Snackbar.make(findViewById(android.R.id.content),
                            R.string.permission_denied_explanation, Snackbar.LENGTH_INDEFINITE)
                            .setAction(R.string.settings, new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    Intent intent = new Intent();
                                    intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                    Uri uri = Uri.fromParts("package",
                                            BuildConfig.APPLICATION_ID, null);
                                    intent.setData(uri);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    startActivity(intent);
                                }
                            }).show();
                }
            }
        }
    }

    @Override
    public void onClick(View view) {
        if (mRequestingLocationUpdates) {
            // Requesting updates at the moment. Hence the request is to stop requiring updates.
            stopLocationUpdates();
        } else {
            // Not requesting updates at the moment. Hence the request is to start requiring updates.
            startLocationUpdates(true);
        }
    }

    @Override
    public void activate(OnLocationChangedListener onLocationChangedListener) {
        mLocationChangeListener = onLocationChangedListener;
    }

    @Override
    public void deactivate() {
        mLocationChangeListener = null;
    }

    /**
     * Creates a dialog to explain to the user the rationale for requesting location.
     */
    public static class LocationRationaleDialogFragment extends DialogFragment {

        public static LocationRationaleDialogFragment newInstance() {
            return new LocationRationaleDialogFragment();
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            setCancelable(false);
            return new AlertDialog.Builder(getActivity())
                    .setTitle(getString(R.string.location_request_rationale_title,
                            getString(R.string.app_name)))
                    .setMessage(getString(
                            R.string.location_request_rationale, getString(R.string.app_name)))
                    .setPositiveButton(R.string.alert_dialog_ok,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    ((MapsActivity) getActivity()).dialogPositiveButtonHandler();
                                }
                            }
                    )
                    .create();
        }
    }

    /**
     * Shows a dialog explaining to the user the rationale for requesting location.
     */
    private void showRationaleDialog() {
        final DialogFragment newFragment = LocationRationaleDialogFragment.newInstance();
        newFragment.show(getSupportFragmentManager(),
                getString(R.string.location_request_rationale));
    }

    /**
     * Handles user closing the dialog from {@link #showRationaleDialog()} by requesting
     * for the location once more.
     */
    private void dialogPositiveButtonHandler() {
        performLocationPermissionRequest();
    }
}
