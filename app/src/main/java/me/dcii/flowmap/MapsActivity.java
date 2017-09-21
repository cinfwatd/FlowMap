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
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

/**
 * Presents the map to the user.
 *
 * @author Dogak Cinfwat.
 */
public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private static final int REQUEST_CODE_LOCATION = 1;
    private GoogleMap mMap;
    private Marker mMarker = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        initialiseMap();
    }

    /**
     * Initialises map the current user location when location permission is granted.
     */
    private void initialiseMap() {
        if (checkLocationPermission()) {

            // Get reference to system location manager
            final LocationManager locationManager = (LocationManager)
                    getSystemService(Context.LOCATION_SERVICE);
            Location location = null;

            if (locationManager != null) {
                // Define listener that responds to location updates
                final LocationListener locationListener = new LocationListener() {
                    @Override
                    public void onLocationChanged(Location location) {
                        updateLocation(location);
                    }

                    @Override
                    public void onStatusChanged(String s, int i, Bundle bundle) {}

                    @Override
                    public void onProviderEnabled(String s) {
//                      TODO: check provider type and user it, maybe?
                    }

                    @Override
                    public void onProviderDisabled(String s) {
//                      TODO: check provider type and respond, maybe a message to the user.
                    }
                };

                final long minTime = 0;  // minimum time interval between notifications.
                final float minDistance = 0;  // minimum distance between notifications.
                final String networkLocationProvider = LocationManager.NETWORK_PROVIDER;
                final String gpsLocationProvider = LocationManager.GPS_PROVIDER;

//                Register location listener with location manager to receive updates.
                locationManager.requestLocationUpdates(
                        networkLocationProvider, minTime, minDistance, locationListener);
                locationManager.requestLocationUpdates(
                        gpsLocationProvider, minTime, minDistance, locationListener);

//                Get quick fix location with the network provider.
                location = locationManager
                        .getLastKnownLocation(networkLocationProvider);
            }
            updateLocationAnimate(location);
        }
    }

    /**
     * Zooms-in on current user location with some animation.
     *
     * @param location provided user location.
     */
    private void updateLocationAnimate(Location location) {
        if (location != null) {
            final LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
            final MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(latLng);

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
     * Updates user location with provided location information as frequently as possible.
     *
     * @param location provided user location.
     */
    private void updateLocation(Location location) {
        if (location != null) {
            final LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
            mMarker.setPosition(latLng);
        }
    }

    /**
     * Checks for location permission on devices running Android M or greater and request for
     * permission when not already granted.
     *
     * @return the permission status. If false, it initiates the permission request process.
     */
    private boolean checkLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION) !=
                    PackageManager.PERMISSION_GRANTED) {

                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_FINE_LOCATION)) {
                    showRationaleDialog();
                } else {
                    requestLocationPermission();
                }
                return false;
            }
        }
        return true;
    }

    /**
     * Does the actual permission request.
     */
    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                REQUEST_CODE_LOCATION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_LOCATION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    initialiseMap();
                } else {
//                    TODO: Disable modules depending on this permission
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
                                    ((MapsActivity) getActivity()).doPositiveClick();
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
    private void doPositiveClick() {
        requestLocationPermission();
    }
}
