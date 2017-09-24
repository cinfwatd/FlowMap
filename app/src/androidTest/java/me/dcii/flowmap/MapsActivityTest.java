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

import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiObjectNotFoundException;
import android.support.test.uiautomator.UiSelector;
import android.support.test.uiautomator.Until;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.matcher.ViewMatchers.withContentDescription;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.*;

/**
 * Instrumented test for {@link MapsActivity}.
 *
 * @author Dogak Cinfwat.
 */
@RunWith(AndroidJUnit4.class)
public class MapsActivityTest {

    private static final String MAPS_ACTIVITY_PACKAGE
            = "me.dcii.flowmap.MapsActivity";
    private static final int LAUNCH_TIMEOUT = 5000;

    private UiDevice mDevice = null;
    private MapsActivity mapsActivity = null;

    @Rule
    public ActivityTestRule<MapsActivity> mapsActivityTestRule =
            new ActivityTestRule<>(MapsActivity.class);


    @Before
    public void setUp() throws Exception {
        mapsActivity = mapsActivityTestRule.getActivity();

        // Initialise device instance.
        mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        // Wait for the app to appear
        mDevice.wait(Until.hasObject(By.pkg(MAPS_ACTIVITY_PACKAGE).depth(0)),
                LAUNCH_TIMEOUT);
    }

    @Test
    public void testLaunch() {
        assertNotNull(mapsActivity);
    }

    @Test
    public void testDevice() {
        assertNotNull(mDevice);
    }

    @Test
    public void testUI() {
        assertNotNull(mapsActivity.findViewById(R.id.map));
        assertNotNull(mapsActivity.findViewById(R.id.fab));
    }

    @Test
    public void testMap() {
        // By default the content description of the map is "Google Map".
        onView(withContentDescription("Google Map")).perform(click());
    }

    @Test
    public void testRequestLocationUpdateStart() {
        onView(withId(R.id.fab)).perform(click());

        final UiObject startMarker = mDevice.findObject(new UiSelector()
                .descriptionContains(mapsActivity.getString(R.string.start_location)));
            try {
                startMarker.click();
            } catch (UiObjectNotFoundException e) {
                e.printStackTrace();
            }
    }

    public void testRequestLocationUpdateStop() {
        // Second click request stop updates.
        onView(withId(R.id.fab)).perform(click());

        final UiObject endMarker = mDevice.findObject(new UiSelector()
                .descriptionContains(mapsActivity.getString(R.string.end_location)));
        try {
            endMarker.click();
        } catch (UiObjectNotFoundException e) {
            e.printStackTrace();
        }
    }

    @After
    public void tearDown() throws Exception {
        mapsActivity = null;
        mDevice = null;
    }

}