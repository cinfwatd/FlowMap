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

package me.dcii.flowmap.adapter;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import io.realm.OrderedRealmCollection;
import io.realm.RealmRecyclerViewAdapter;
import me.dcii.flowmap.R;
import me.dcii.flowmap.model.Journey;

/**
 * Journeys {@link RecyclerView} adapter, extends the {@link RealmRecyclerViewAdapter} to provide
 * support for {@link io.realm.RealmModel} (models).
 *
 * @author Dogak Cinfwat.
 */

public class JourneyRecyclerViewAdapter extends
        RealmRecyclerViewAdapter<Journey, JourneyRecyclerViewAdapter.ViewHolder> {

    /**
     * Recycler view journey item click listener.
     */
    private JourneyClickListener mJourneyClickListener;

    public JourneyRecyclerViewAdapter(OrderedRealmCollection<Journey> data, JourneyClickListener clickListener) {
        super(data, true);
        setHasStableIds(true);
        mJourneyClickListener = clickListener;
    }

    @Override
    public JourneyRecyclerViewAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_journey_row, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(JourneyRecyclerViewAdapter.ViewHolder holder, int position) {
        final Journey journey = getItem(position);
        holder.bind(journey, mJourneyClickListener);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private TextView mStartLocation;
        private TextView mEndLocation;
        private TextView mTransportTravelTime;
        private String journeyId;

        ViewHolder(View view) {
            super(view);
            mStartLocation = view.findViewById(R.id.start_location);
            mEndLocation = view.findViewById(R.id.end_location);
            mTransportTravelTime = view.findViewById(R.id.transport_travel_time);
            journeyId = "";
        }

        /**
         * Binds the journey data to the view.
         *
         * @param journey the journey to bind.
         * @param clickListener the {@link JourneyClickListener}.
         */
        private void bind(final Journey journey, final JourneyClickListener clickListener) {

            journeyId = journey.getId();
            // Tint programmatically since resource drawable tint is supported in API level 23+.
            Drawable startDrawable = ContextCompat.getDrawable(
                    itemView.getContext(), R.drawable.ic_start_place_black_24dp);
            startDrawable = DrawableCompat.wrap(startDrawable);

            // Set tint color to Green for start location.
            DrawableCompat.setTint(startDrawable, Color.GREEN);
            DrawableCompat.setTintMode(startDrawable, PorterDuff.Mode.SRC_IN);
            mStartLocation.setText(journey.getStartLocation().toString());
            mStartLocation.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    startDrawable, null, null, null);

            // Change tint color to red for end location.
            Drawable endDrawable = ContextCompat.getDrawable(
                    itemView.getContext(), R.drawable.ic_end_place_black_24dp);
            endDrawable = DrawableCompat.wrap(endDrawable);
            DrawableCompat.setTint(endDrawable, Color.RED);
            DrawableCompat.setTintMode(endDrawable, PorterDuff.Mode.SRC_IN);
            mEndLocation.setText(journey.getEndLocation().toString());
            mEndLocation.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    endDrawable, null, null, null);

            mTransportTravelTime.setText(journey.getTravelTime(itemView.getContext()));

            // Set left drawable to show transport type.
            switch (journey.getTransportType()) {
                case RUNNING:
                    mTransportTravelTime.setCompoundDrawablesRelativeWithIntrinsicBounds(
                            R.drawable.ic_directions_run_black_24dp, 0, 0, 0);
                    break;
                case CYCLING:
                    mTransportTravelTime.setCompoundDrawablesRelativeWithIntrinsicBounds(
                            R.drawable.ic_directions_bike_black_24dp, 0, 0, 0);
                    break;
                case DRIVING:
                    mTransportTravelTime.setCompoundDrawablesRelativeWithIntrinsicBounds(
                            R.drawable.ic_directions_car_black_24dp, 0, 0, 0);
                    return;
                default:
                    // Walking.
                    mTransportTravelTime.setCompoundDrawablesRelativeWithIntrinsicBounds(
                            R.drawable.ic_directions_walk_black_24dp, 0, 0, 0);
            }

            // itemView from parent ViewHolder class.
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    clickListener.journeyClicked(journey);
                }
            });
        }

        public String getJourneyId() {
            return journeyId;
        }
    }

    /**
     * Journey click listener interface.
     */
    public interface JourneyClickListener {
        void journeyClicked(Journey journey);
    }
}
