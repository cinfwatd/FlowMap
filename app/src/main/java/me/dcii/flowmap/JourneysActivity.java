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

import android.content.Intent;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.MenuItem;
import android.widget.Toast;

import io.realm.Realm;
import io.realm.RealmResults;
import me.dcii.flowmap.adapter.JourneyRecyclerViewAdapter;
import me.dcii.flowmap.model.Journey;
import me.dcii.flowmap.util.TouchHelperCallback;

/**
 * Journey list activity.
 *
 * @author Dogak Cinfwat.
 */
public class JourneysActivity extends AppCompatActivity
        implements JourneyRecyclerViewAdapter.JourneyClickListener {

    private RecyclerView mRecyclerView;
    private JourneyRecyclerViewAdapter mJourneyAdapter;
    private Realm mRealm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_journeys);

        // Allow up navigation with the app icon
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        mRealm = Realm.getDefaultInstance();
        mRecyclerView = findViewById(R.id.journey_recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setHasFixedSize(true);

        final RealmResults<Journey> realmResults = mRealm.where(Journey.class)
                .notEqualTo(Journey.FIELD_IS_DELETED, true).findAll();
        mJourneyAdapter = new JourneyRecyclerViewAdapter(realmResults, this);
        mRecyclerView.setAdapter(mJourneyAdapter);

        // Attach touch helper/callback to recycler view.
        final TouchHelperCallback touchHelperCallback = new TouchHelperCallback(mRealm);
        final ItemTouchHelper touchHelper = new ItemTouchHelper(touchHelperCallback);
        touchHelper.attachToRecyclerView(mRecyclerView);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mRecyclerView.setAdapter(null);
        mRealm.close();
    }

    @Override
    public void journeyClicked(Journey journey) {
        Intent intent = new Intent(this, MapsActivity.class);
        intent.putExtra(Journey.FIELD_ID, journey.getId());
        startActivity(intent);
    }
}
