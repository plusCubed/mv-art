package com.pluscubed.mvart.ui;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.pluscubed.mvart.R;
import com.pluscubed.mvart.model.ArtLocation;
import com.pluscubed.mvart.model.ArtLocationList;

import java.util.ArrayList;
import java.util.List;

/**
 * List
 */
public class ArtLocationListFragment extends Fragment {

    private RecyclerView mRecyclerView;
    private ArtLocationAdapter mAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public void updateDataAndSetSearch(String search) {
        if (mAdapter != null) {
            mAdapter.updateDataAndSetSearch(search);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mRecyclerView = (RecyclerView) inflater.inflate(R.layout.fragment_art_list, container, false);
        mAdapter = new ArtLocationAdapter();
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        return mRecyclerView;
    }

    private class ArtLocationAdapter extends RecyclerView.Adapter<ArtLocationAdapter.ArtViewHolder> {

        private String mSearchQuery;
        private List<ArtLocation> mArtLocations;

        public ArtLocationAdapter() {
            mArtLocations = ArtLocationList.getInstance().getList();
            mSearchQuery = "";
        }

        @Override
        public int getItemViewType(int position) {
            ArtLocation artLocation = mArtLocations.get(position);
            return artLocation.artist.isEmpty() && artLocation.address.isEmpty() ? 1 : 0;
        }

        @Override
        public ArtViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
            View view = LayoutInflater.from(getActivity()).inflate(R.layout.list_item_artlocation, viewGroup, false);
            ArtViewHolder holder = new ArtViewHolder(view);
            holder.title = (TextView) view.findViewById(R.id.list_item_artlocation_title_textview);
            holder.desc = (TextView) view.findViewById(R.id.list_item_artlocation_desc_textview);

            ViewGroup.LayoutParams params = view.getLayoutParams();
            if (viewType == 1) {
                params.height = MainActivity.convertDpToPx(getActivity(), 48);
                view.setLayoutParams(params);
                holder.desc.setVisibility(View.GONE);
            } else {
                params.height = MainActivity.convertDpToPx(getActivity(), 72);
                view.setLayoutParams(params);
                holder.desc.setVisibility(View.VISIBLE);
            }
            return holder;
        }

        @Override
        public void onBindViewHolder(ArtViewHolder holder, int i) {
            ArtLocation location = mArtLocations.get(i);
            holder.title.setText(location.title);
            String text = location.artist + "\n" + location.address;
            holder.desc.setText(text);
        }

        public void updateDataAndSetSearch(String string) {
            mSearchQuery = string;

            if (!mSearchQuery.isEmpty()) {
                mArtLocations = new ArrayList<>();
                for (ArtLocation location : ArtLocationList.getInstance().getList()) {
                    if (location.matchFilter(string)) {
                        mArtLocations.add(location);
                    }
                }
            } else {
                mArtLocations = ArtLocationList.getInstance().getList();
            }
            notifyDataSetChanged();
        }

        @Override
        public int getItemCount() {
            return mArtLocations.size();
        }

        class ArtViewHolder extends RecyclerView.ViewHolder {
            TextView title;
            TextView desc;

            public ArtViewHolder(View itemView) {
                super(itemView);
                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent i = new Intent(getActivity(), ArtLocationDetailsActivity.class);
                        i.putExtra(ArtLocationDetailsActivity.ART_LOCATION_INDEX,
                                getLayoutPosition());
                        startActivity(i);
                    }
                });
                itemView.findViewById(R.id.list_item_artlocation_map_imageview).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ((MainActivity) getActivity()).showMarkerInfoWindow(getLayoutPosition());
                    }
                });
            }
        }
    }
}
