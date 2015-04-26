package com.pluscubed.mvart.ui;

import android.app.ActivityManager;
import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.pluscubed.mvart.R;
import com.pluscubed.mvart.model.ArtLocation;
import com.pluscubed.mvart.model.ArtLocationList;
import com.pluscubed.mvart.network.DownloadImageTask;

public class ArtLocationDetailsActivity extends ActionBarActivity {
    public static final String ART_LOCATION_INDEX = "com.pluscubed.mvart.ART_LOCATION_INDEX";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_generic);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_generic);
        setSupportActionBar(toolbar);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setTaskDescription(
                    new ActivityManager.TaskDescription(null, null, getResources().getColor(R.color.task_bar)));
        }

        FragmentManager fm = getFragmentManager();
        Fragment f = fm.findFragmentById(R.id
                .activity_generic_content_framelayout);
        int index = getIntent().getIntExtra(ART_LOCATION_INDEX, 0);
        ArtLocation artLocation = ArtLocationList.getInstance().get(index);
        if (f == null) {
            fm.beginTransaction()
                    .replace(R.id.activity_generic_content_framelayout,
                            ArtLocationDetailsFragment.newInstance(
                                    index)
                    )
                    .commit();
        }
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setTitle(artLocation.title);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    public static class ArtLocationDetailsFragment extends Fragment {
        public static final String INIT_ART_LOCATION_INDEX = "art_location_index";
        private ArtLocation mArtLocation;

        public static ArtLocationDetailsFragment newInstance(int index) {
            ArtLocationDetailsFragment fragment = new ArtLocationDetailsFragment();
            Bundle args = new Bundle();
            args.putInt(INIT_ART_LOCATION_INDEX, index);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mArtLocation = ArtLocationList.getInstance().get(getArguments().getInt(INIT_ART_LOCATION_INDEX));
        }

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.fragment_art_location_details, container, false);
            ProgressBar progressBar = (ProgressBar) v.findViewById(R.id.fragment_art_location_details_image_progressbar);
            ImageView imageView = (ImageView) v.findViewById(R.id.fragment_art_location_details_pic_imageview);
            TextView desc = (TextView) v.findViewById(R.id.fragment_art_location_details_desc_textview);

            if (mArtLocation.pic == null) {
                DownloadImageTask imageTask = new DownloadImageTask();
                SparseArray<Object> dlArgs = new SparseArray<>();
                dlArgs.put(DownloadImageTask.DL_IMAGE_ARGS_ARTLOCATION, mArtLocation);
                dlArgs.put(DownloadImageTask.DL_IMAGE_ARGS_IMAGEVIEW, imageView);
                dlArgs.put(DownloadImageTask.DL_IMAGE_ARGS_PROGRESSBAR, progressBar);
                dlArgs.put(DownloadImageTask.DL_IMAGE_ARGS_THUMBNAIL_BOOL, false);
                imageTask.execute(dlArgs);
            } else {
                imageView.setImageDrawable(mArtLocation.pic);
                progressBar.setVisibility(View.GONE);
            }
            desc.setLineSpacing(0, 1.3f);
            desc.setText(mArtLocation.getFormattedDesc(getActivity()));
            return v;
        }
    }
}
