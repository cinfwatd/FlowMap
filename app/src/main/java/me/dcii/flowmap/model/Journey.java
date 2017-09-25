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

import com.google.android.gms.maps.model.LatLng;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Represents the user Journey.
 *
 * @author Dogak Cinfwat.
 */

public class Journey extends RealmObject {

    @PrimaryKey
    private String id;  // Provide unique identifier.

    /**
     * Represents the name of the Id field. This is used in querying the Realm store.
     */
    public static String FIELD_ID = "id";

    /**
     * RealmList of intermediate {@link } positions during user journey.
     */
    private RealmList<Location> locations;

    /**
     * Transportation type string name. Used since custom types are not supported in Realm.
     */
    private String transportTypeName;

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
    }

    /**
     * Empty constructor.
     */
    public Journey() {
        this(UUID.randomUUID().toString(),new RealmList<Location>(), TransportType.WALKING.name());
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
}
