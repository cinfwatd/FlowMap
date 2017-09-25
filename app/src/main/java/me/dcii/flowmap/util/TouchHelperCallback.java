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

import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.widget.Toast;

import io.realm.Realm;
import me.dcii.flowmap.R;
import me.dcii.flowmap.adapter.JourneyRecyclerViewAdapter;
import me.dcii.flowmap.model.DataHelper;

/**
 * Handles swipe touch gestures on the journeys recycler view.
 *
 * @author Dogak Cinfwat.
 */

public class TouchHelperCallback extends ItemTouchHelper.SimpleCallback {

    /**
     * Represents the {@link Realm} store instance.
     */
    private Realm mRealm;

    /**
     * Creates a Callback for the given drag and swipe allowance.
     */
    public TouchHelperCallback(Realm realm) {
        super(ItemTouchHelper.UP | ItemTouchHelper.DOWN,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
        mRealm = realm;
    }

    @Override
    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                          RecyclerView.ViewHolder target) {
        return true;
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {

        // Perform soft deletion on Left or Right swipe.
        if (direction == ItemTouchHelper.LEFT || direction == ItemTouchHelper.RIGHT) {
            final JourneyRecyclerViewAdapter.ViewHolder journeyViewHolder =
                    (JourneyRecyclerViewAdapter.ViewHolder) viewHolder;
            DataHelper.deleteItemAsync(mRealm, journeyViewHolder.getJourneyId(), true);

            Toast.makeText(viewHolder.itemView.getContext(),
                    R.string.deleted, Toast.LENGTH_SHORT).show();
        }
    }
}
