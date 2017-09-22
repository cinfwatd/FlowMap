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
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;
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
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.text.DateFormat;
import java.util.Date;

/**
 * Presents the map to the user.
 *
 * @author Dogak Cinfwat.
 */
public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

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
     * Represents the Google map object.
     */
    private GoogleMap mMap;

    /**
     * Map marker.
     */
    private Marker mMarker = null;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Updates values from previous instance of the activity.
        updateValuesFromBundle(savedInstanceState);

        mRequestingLocationUpdates = true;
        mLastUpdateTime = "";

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mSettingsClient = LocationServices.getSettingsClient(this);

        // Kick of the process of building the LocationCallback, LocationRequest, and
        // LocationSettingsRequest objects.
        createLocationCallback();
        createLocationRequest();
        buildLocationSettingsRequest();
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

        if (checkLocationPermission()) mMap.setMyLocationEnabled(true);
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
        if (checkLocationPermission()) {
            startLocationUpdates();
        }
//        TODO: Retrieve shared preferences.
    }

    /**
     * Requests location updates from the FuseLocationClientApi. This request is only performed
     * when runtime location permission has been granted in devices running android M and above.
     */
    @SuppressWarnings("MissingPermission")
    private void startLocationUpdates() {

        // Check if the device has the necessary location settings.
        mSettingsClient.checkLocationSettings(mLocationSettingsRequest)
                .addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
                    @Override
                    public void onSuccess(LocationSettingsResponse locationSettingsResponse) {

                        //noinspection MissingPermission
                        mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                                mLocationCallback, Looper.myLooper());
                        mRequestingLocationUpdates = true;
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
                                final String errorMessage = "Location settings are inadequate " +
                                        "and cannot be fixed here. Fix in settings.";
                                Toast.makeText(MapsActivity.this, errorMessage,
                                        Toast.LENGTH_LONG).show();
                                mRequestingLocationUpdates = false;
                        }
                        updateUI();
                    }
                });
    }

    /**
     * Updates UI fields.
     */
    private void updateUI() {
//        setButtonsEnabledState();
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
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        mRequestingLocationUpdates = false;
//                        setButtonsEnabledState();
                    }
                });
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
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            // Check for integer request code originally supplied to startResolutionForResult().
            case REQUEST_CODE_CHECK_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        startLocationUpdates();
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
     * Handles the start updates button and requests starts of location updates.
     */
    public void startsUpdatesButtonHandler() {
        if (!mRequestingLocationUpdates) {
            mRequestingLocationUpdates = true;
//            setButtonsEnabledState();
            startLocationUpdates();
        }
    }

    /**
     * Handles the stop updates button and requests removal of location updates.
     */
    public void stopUpdateButtonHandler() {
        stopLocationUpdates();
    }

    /**
     * Zooms-in on current user location with some animation.
     *
     * @param latLng provided user {@link LatLng}.
     */
    private void updateLocationAnimate(LatLng latLng) {
            final MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(latLng);

        if (mMap != null) {
            final int zoom = 13;
            mMarker = mMap.addMarker(markerOptions);
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
            final CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(latLng)
                    .zoom(17)
                    .bearing(90)
                    .tilt(40)
                    .build();
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }
    }

    /**
     * Updates user location with provided location information.
     */
    private void updateLocation() {
        if (mCurrentLocation != null) {
            final LatLng latLng = new LatLng(mCurrentLocation.getLatitude(),
                    mCurrentLocation.getLongitude());
            Toast.makeText(this, latLng.toString(), Toast.LENGTH_SHORT).show();
            if (mMarker == null) {
                updateLocationAnimate(latLng);
            } else mMarker.setPosition(latLng);
        }
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
                        startLocationUpdates();
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
