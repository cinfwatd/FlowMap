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
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.ResultReceiver;
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

import io.realm.Realm;
import io.realm.RealmList;
import me.dcii.flowmap.model.Journey;
import me.dcii.flowmap.service.FetchAddressIntentService;
import me.dcii.flowmap.service.FlowLocationService;
import me.dcii.flowmap.util.Constants;

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
     * Application location service.
     */
    FlowLocationService mFlowLocationService;
    boolean mBound = false;

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
    private static final String KEY_LOCATION = "last-location";
    private static final String KEY_START_MARKER_POSITION = "start-marker-position";
    private static final String KEY_END_MARKER_POSITION = "end-marker-position";
    private static final String KEY_JOURNEY_ID = "journey-id";
    private static final String KEY_JOURNEY_DETAIL_VIEW = "journey-detail-view";

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
     * Map marker {@link LatLng} position. Used to rebuild start marker on configuration change when
     * {@link GoogleMap} is used for journey detail viewing.
     */
    private LatLng mStartMarkerPosition = null;

    /**
     * Map marker for journey end location.
     */
    private Marker mEndMarker = null;

    /**
     * Map marker {@link LatLng} position. Used to rebuild end marker on configuration change when
     * {@link GoogleMap} is used for journey detail viewing.
     */
    private LatLng mEndMarkerPosition = null;

    /**
     * Provides the location change listener used for the location source interface.
     */
    private OnLocationChangedListener mLocationChangeListener;

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

    /**
     * flag used to show that map is used to show journey details from the journey's recycler view.
     */
    private boolean mIsJourneyDetails;

    /**
     * Receives the {@link FetchAddressIntentService} results.
     */
    private LocationResultReceiver mResultReceiver;

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

        mJourney = null;
        mJourneyId = null;
        mIsJourneyDetails = false;

        // Initialise the location result receiver.
        mResultReceiver = new LocationResultReceiver(new Handler());

        mRealm = Realm.getDefaultInstance();  // opens the default realm.

        // Updates values from previous instance of the activity.
        updateValuesFromBundle(savedInstanceState);

        handleIntent();
    }

    private void handleIntent() {
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            final String journeyId = extras.getString(Journey.FIELD_ID);
            restoreJourneyFromId(journeyId);
        }
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
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setMyLocationButtonEnabled(false);
        mMap.setLocationSource(this);
        updateUI();
    }

    /**
     * Sets {@link GoogleMap#setMyLocationEnabled(boolean)}.
     */
    private void setMyLocationEnabled() {
        if (mFlowLocationService == null) return;

        try {
            mMap.setMyLocationEnabled(mFlowLocationService.isRequestingLocationUpdates());
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

            // Update the value of mCurrentLocation.
            if (savedInstanceState.keySet().contains(KEY_LOCATION)) {
                mCurrentLocation = savedInstanceState.getParcelable(KEY_LOCATION);
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

            // update if map is used as a Journey detail view.
            if (savedInstanceState.keySet().contains(KEY_JOURNEY_DETAIL_VIEW)) {
                mIsJourneyDetails = savedInstanceState.getBoolean(KEY_JOURNEY_DETAIL_VIEW);
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!checkLocationPermission()) {
            requestLocationPermission();
        }

        // Bind to FlowLocationService
        Intent intent = new Intent(this, FlowLocationService.class);

        // Pass the result receiver as an extra to the service.
        intent.putExtra(Constants.LOCATION_RECEIVER, mResultReceiver);
        startService(intent);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    /**
     * Requests location updates from the FuseLocationClientApi. This request is only performed
     * when runtime location permission has been granted in devices running android M and above.
     * Also, the user is notified if enabled.
     */
    private void startLocationUpdates() {
        if (mFlowLocationService == null) {
            return;
        }
        clearMap();
        mFlowLocationService.startLocationUpdates();
        notifyRequestingUpdates(R.string.requesting_location_updates_start);
        updateUI();
    }

    /**
     * Updates UI fields.
     */
    private void updateUI() {
        setFabEnabledState();
        updateLocation();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Unbind from the FlowLocation service
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }

    /**
     * Removes location updates from the FusedLocationApi.
     */
    private void stopLocationUpdates() {
        if (mFlowLocationService == null) {
            return;
        }
        // Remove location request when activity is in a paused or stopped state.
        mFlowLocationService.stopLocationUpdates();
        notifyRequestingUpdates(R.string.requesting_location_updates_stop);
        updateUI();
    }

    /**
     * Sets {@link FloatingActionButton} drawable image to represent available action.
     * The {@link FloatingActionButton} can be used to request update and stop requesting.
     */
    private void setFabEnabledState() {
        // Stop execution if mFlowLocationService is null.
        if (mFlowLocationService == null) {
            return;
        }

        if (mFlowLocationService.isRequestingLocationUpdates()) {
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
        outState.putParcelable(KEY_LOCATION, mCurrentLocation);
        outState.putParcelable(KEY_START_MARKER_POSITION, mStartMarkerPosition);
        outState.putParcelable(KEY_END_MARKER_POSITION, mEndMarkerPosition);
        outState.putString(KEY_JOURNEY_ID, mJourneyId);
        outState.putBoolean(KEY_JOURNEY_DETAIL_VIEW, mIsJourneyDetails);
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
                        startLocationUpdates();
                        break;
                    case Activity.RESULT_CANCELED:
                        // User chose not to make required location changes.
                        updateUI();
                        break;
                }
                break;
        }
    }

    /**
     * Notifies the user of the change in {@link FlowLocationService#isRequestingLocationUpdates()}.
     *
     * @param messageRes the user message resource Id.
     */
    private void notifyRequestingUpdates(@StringRes int messageRes) {
        Toast.makeText(this, messageRes, Toast.LENGTH_SHORT).show();
    }

    /**
     * Zooms-in on current user location with some animation. Also, adds the start location marker.
     *
     * @param latLng provided user {@link LatLng}.
     */
    private void focusLocationAnimate(LatLng latLng) {
        // Stop execution if mMap is null.
        if (mMap == null) return;

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
        setMyLocationEnabled();
        drawRoute();
    }

    /**
     * Restores the {@link Journey} instance using the provided {@link Journey#id} identifier. Also,
     * it restores the marker positions from the journey instance. This is used when view store
     * journeys.
     *
     * @param id {@link Journey#id} identifier used to restore the {@link Journey} instance.
     */
    private void restoreJourneyFromId(String id) {
        if (id == null) {
            return;
        }

        mJourney = mRealm.where(Journey.class).equalTo(Journey.FIELD_ID, id).findFirst();
        // Check to make sure the mJourney exist.
        if (mJourney == null) {
            return;
        }

        mJourneyId = id;
        mStartMarkerPosition = new LatLng(mJourney.getStartLocation().getLatitude(),
                mJourney.getStartLocation().getLongitude());
        mEndMarkerPosition = new LatLng(mJourney.getEndLocation().getLatitude(),
                    mJourney.getEndLocation().getLongitude());

        // mMap is used to show journey details.
        mIsJourneyDetails = true;
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
    private void clearMap() {
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
        // Prepare for new Journey. set journey and journeyId to null. These are only used for
        // viewing saved journeys.
        mJourney = null;
        mJourneyId = null;
        mIsJourneyDetails = false;
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
        RealmList<me.dcii.flowmap.model.Location> journeyLocations = null;
        boolean showEndMaker = false;
        if (mFlowLocationService != null && mFlowLocationService.getJourney() != null) {
            // Get active journey locations.
            journeyLocations = mFlowLocationService.getJourney().getLocations();

            // Show end marker when user just stopped tracking.
            showEndMaker = !mFlowLocationService.isRequestingLocationUpdates();
        } else if (mIsJourneyDetails && mJourney != null) {
            // Get old (currently viewed) journey's locations.
            journeyLocations = mJourney.getLocations();

            // Show end marker since mMap is used to view old journey.
            showEndMaker = true;
        }

        // Check to make sure journey locations are provided.
        if (journeyLocations == null) {
            return;
        }

        // Add location markers.
        final int firstIndex = 0;
        final LatLng firstMarkerLatLng = new LatLng(journeyLocations.get(firstIndex).getLatitude(),
                journeyLocations.get(firstIndex).getLongitude());
        if (mStartMarker == null) {
            focusLocationAnimate(firstMarkerLatLng);
        }
        addLocationStartMarker(firstMarkerLatLng);

        // Check if to show the end marker.
        // Show endMarker when mMap is used to view journey or when the user just stopped tracking.
        if (showEndMaker) {
            final int lastIndex = journeyLocations.size() - 1;
            // Not requesting location updates and journey is not null hence, show the endMarker.
            addLocationEndMarker(new LatLng(journeyLocations.get(lastIndex).getLatitude(),
                    journeyLocations.get(lastIndex).getLongitude()));
        }

        // Draw polylines.
        drawRoute(journeyLocations);
    }

    /**
     * Does the actual drawing of polylines on the map.
     *
     * @param journeyLocations {@link me.dcii.flowmap.model.Location} to draw on the map.
     */
    private void drawRoute(RealmList<me.dcii.flowmap.model.Location> journeyLocations) {
        if (mMap == null || journeyLocations.size() == 0) {
            return;
        }

        final PolylineOptions options = new PolylineOptions().width(5).color(Color.BLUE).geodesic(true);
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
                    if (mFlowLocationService != null && mFlowLocationService.isRequestingLocationUpdates()) {
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

    @Override
    public void onClick(View view) {
        // Stop execution if location service is null.
        if (mFlowLocationService == null) return;

        if (mFlowLocationService.isRequestingLocationUpdates()) {
            // Requesting updates at the moment. Hence the request is to stop requiring updates.
            stopLocationUpdates();
        } else {
            // Not requesting updates at the moment. Hence the request is to start requiring updates.
            startLocationUpdates();
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
     * Receives the location results from the {@link FlowLocationService}.
     */
    public class LocationResultReceiver extends ResultReceiver {
        LocationResultReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {

            // Location message received.
            if (resultCode == Constants.SUCCESS_RESULT) {
                mCurrentLocation = resultData.getParcelable(Constants.LOCATION_DATA_EXTRA);

                if (mCurrentLocation != null) {
                    updateLocation();
                }
                if (mLocationChangeListener != null) {
                    mLocationChangeListener.onLocationChanged(mCurrentLocation);
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

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to FlowLocationService, cast the IBinder and get FlowLocationService instance
            FlowLocationService.FlowLocationServiceBinder binder = (FlowLocationService.FlowLocationServiceBinder) service;
            mFlowLocationService = binder.getService();
            mBound = true;
            updateUI();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mFlowLocationService = null;
            mBound = false;
        }
    };
}