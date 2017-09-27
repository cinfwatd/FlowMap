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

package me.dcii.flowmap.model;

import android.content.Context;
import android.content.res.Resources;
import android.text.TextUtils;

import com.google.android.gms.maps.model.LatLng;

import java.util.Date;
import java.util.Locale;
import java.util.UUID;

import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.Ignore;
import io.realm.annotations.PrimaryKey;
import me.dcii.flowmap.R;

/**
 * Represents the user Journey.
 *
 * @author Dogak Cinfwat.
 */

public class Journey extends RealmObject {

    @Ignore
    private long mSecondsInMilliseconds = 1000;

    @Ignore
    private long mMinutesInMilliseconds = mSecondsInMilliseconds * 60;

    @Ignore
    private long mHoursInMilliseconds = mMinutesInMilliseconds * 60;

    @Ignore
    private long mDaysInMilliseconds = mHoursInMilliseconds * 24;

    @PrimaryKey
    private String id;  // Provide unique identifier.

    /**
     * Represents the names of the fields. These are used in querying the Realm store.
     */
    public static String FIELD_ID = "id";
    public static String FIELD_IS_DELETED = "isDeleted";


    /**
     * RealmList of intermediate {@link } positions during user journey.
     */
    private RealmList<Location> locations;

    /**
     * Transportation type string name. Used since custom types are not supported in Realm.
     */
    private String transportTypeName;

    /**
     * Flag used to implement soft deletion.
     */
    private boolean isDeleted;

    /**
     * Date the journey was deleted.
     */
    private Date dateDeleted;

    private String startAddress;
    private String endAddress;

    /**
     * Constructor.
     *
     * @param id primary key identifier.
     * @param locations journey intermediate {@link Location} positions.
     * @param transportTypeName journey transport type.
     */
    public Journey(String id, RealmList<Location> locations, String transportTypeName) {
        this.id = id;
        this.locations = locations;
        this.transportTypeName = transportTypeName;
        this.isDeleted = false;
        this.dateDeleted = null;
        this.startAddress = "";
        this.endAddress = "";
    }

    /**
     * Empty constructor.
     */
    public Journey() {
        this(UUID.randomUUID().toString(),new RealmList<Location>(), TransportType.OTHERS.name());
    }

    public Location getStartLocation() {
        // get start location from locations.
        if (locations.size() != 0) {
            return locations.get(0);  // First location is the start location.
        }
        return null;
    }

    public Location getEndLocation() {
        // If end is null and locations RealmList is not empty, get last location from locations.
        if (locations.size() != 0) {
            return locations.get(locations.size() - 1);  // last location is the end location.
        }
        return null;
    }

    public RealmList<Location> getLocations() {
        return locations;
    }

    public void setLocations(RealmList<Location> locations) {
        this.locations = locations;
    }

    public TransportType getTransportType() {
        return TransportType.valueOf(transportTypeName);
    }

    public void setTransportType(TransportType transportType) {
        this.transportTypeName = transportType.name();
    }

    public String getId() {
        return this.id;
    }

    public void addLocation(Location location) {
        locations.add(location);
    }

    public void addLocation(LatLng location) {
        locations.add(new Location(location.latitude, location.longitude));
    }

    public void setIsDeleted(boolean isDeleted) {
        this.isDeleted = isDeleted;
    }

    public void setDateDeleted(Date dateDeleted) {
        this.dateDeleted = dateDeleted;
    }

    /**
     * Gets the total travel time from the first and last locations recorded in {@link #locations}.
     *
     * @param context context used to access application resources.
     * @return
     */
    public String getTravelTime(Context context) {

        final Location start = getStartLocation();
        final Location end = getEndLocation();

        // time in milliseconds.
        long timeDifference = end.getTravelTime().getTime() - start.getTravelTime().getTime();

        // Get days elapsed take that out from the total timeDifference.
        final long daysElapsed = timeDifference / mDaysInMilliseconds;
        timeDifference = timeDifference % mDaysInMilliseconds;

        // Get hours elapsed since days elapsed and take that out from the timeDifference.
        final long hoursElapsed = timeDifference / mHoursInMilliseconds;
        timeDifference = timeDifference % mHoursInMilliseconds;

        // Get minutes elapsed since hours elapsed and take that out from the timeDifference.
        final long minutesElapsed = timeDifference / mMinutesInMilliseconds;
        timeDifference = timeDifference % mMinutesInMilliseconds;

        // Get seconds elapsed since minutes elapsed.
        final long secondsElapsed = timeDifference / mSecondsInMilliseconds;

        String timeString = "";
        final Resources res = context.getResources();

        // Stitch together the travel time string.
        if (daysElapsed > 0) {
            timeString = res.getQuantityString(R.plurals.days, (int) daysElapsed, daysElapsed);
        }
        if (hoursElapsed > 0) {
            timeString += res.getQuantityString(R.plurals.hours, (int) hoursElapsed, hoursElapsed);
        }
        if (minutesElapsed > 0) {
            timeString += res.getQuantityString(R.plurals.minutes, (int) minutesElapsed, minutesElapsed);
        }
        if (secondsElapsed > 0) {
            timeString += res.getQuantityString(R.plurals.seconds, (int) secondsElapsed, secondsElapsed);
        }
        return timeString;
    }

    /**
     * Provides helper method to delete {@link Journey} from {@link Realm} store.
     *
     * @param realm the realm instance.
     * @param id the {@link Journey#id} identifier.
     * @param soft the flag representing soft or hard (real) deletion.
     */
    public static void delete(Realm realm, String id, boolean soft) {

        final Journey journey = realm.where(Journey.class).equalTo(FIELD_ID, id).findFirst();
        if (journey != null) {
            if (soft) {

                journey.setIsDeleted(true);
                journey.setDateDeleted(new Date());
                realm.insertOrUpdate(journey);
            } else {
                journey.deleteFromRealm();
            }
        }
    }

    /**
     * Provides helper method to restore a soft deleted {@link Journey} from {@link Realm} store.
     *
     * @param realm the realm instance.
     * @param id the {@link Journey#id} identifier.
     */
    public static void restore(Realm realm, String id) {
        final Journey journey = realm.where(Journey.class).equalTo(FIELD_ID, id).findFirst();
        if (journey != null) {
            journey.setIsDeleted(false);
            journey.setDateDeleted(null);
            realm.insertOrUpdate(journey);
        }
    }

    /**
     * Returns the start {@link android.location.Address} string address.
     *
     * @return the start address.
     */
    public String getStartAddress() {
        if (TextUtils.isEmpty(startAddress)) {
            // return the location co-ordinates toString if startAddress is empty.
            return getStartLocation().toString();
        }
        return startAddress;
    }

    /**
     * Sets the start {@link android.location.Address} string address.
     *
     * @param address the {@link android.location.Address} string representation.
     */
    public void setStartAddress(String address) {
        this.startAddress = address;
    }

    /**
     * Returns the end {@link android.location.Address} string address.
     *
     * @return the end address.
     */
    public String getEndAddress() {
        if (TextUtils.isEmpty(endAddress)) {
            // return the location co-ordinates toString if endAddress is empty.
            return getEndLocation().toString();
        }
        return endAddress;
    }

    /**
     * Sets the end {@link android.location.Address} string address.
     *
     * @param address the {@link android.location.Address} string representation.
     */
    public void setEndAddress(String address) {
        this.endAddress = address;
    }
}
