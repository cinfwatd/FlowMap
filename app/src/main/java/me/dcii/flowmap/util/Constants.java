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

package me.dcii.flowmap.util;

/**
 * Application constants.
 *
 * @author Dogak Cinfwat.
 */

public final class Constants {
    public static final int SUCCESS_RESULT = 0;
    public static final int FAILURE_RESULT = 1;
    public static final String PACKAGE_NAME =
            "me.dcii.flowmap";
    public static final String ADDRESS_RECEIVER = PACKAGE_NAME + ".ADDRESS_RECEIVER";
    public static final String LOCATION_RECEIVER = PACKAGE_NAME + ".LOCATION_RECEIVER";
    public static final String RESULT_DATA_KEY = PACKAGE_NAME +
            ".RESULT_DATA_KEY";
    public static final String LOCATION_DATA_EXTRA = PACKAGE_NAME +
            ".LOCATION_DATA_EXTRA";

    // Request Codes used for address lookup.
    public static final String ADDRESS_LOOKUP = "ADDRESS_LOOKUP";
    public static final String JOURNEY_ID_ADDRESS_LOOK = "JOURNEY_ID";
    public static final int START_ADDRESS_LOOKUP = 101;
    public static final int END_ADDRESS_LOOK_UP = 102;
}