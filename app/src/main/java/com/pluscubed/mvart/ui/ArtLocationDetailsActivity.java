package com.pluscubed.mvart.ui;

import android.app.ActivityManager;
import android.app.Fragment;
import android.content.Context;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.target.Target;
import com.pluscubed.mvart.R;
import com.pluscubed.mvart.model.ArtLocation;
import com.pluscubed.mvart.model.ArtLocationList;

import uk.co.senab.photoview.PhotoView;

public class ArtLocationDetailsActivity extends AppCompatActivity {
    public static final String ART_LOCATION_INDEX = "com.pluscubed.mvart.ART_LOCATION_INDEX";
    private ArtLocation mArtLocation;
    private int mIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_art_images);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_generic);
        setSupportActionBar(toolbar);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setTaskDescription(
                    new ActivityManager.TaskDescription(null, null, getResources().getColor(R.color.task_bar)));
            getWindow().setStatusBarColor(Color.BLACK);
        }

        mIndex = getIntent().getIntExtra(ART_LOCATION_INDEX, 0);
        mArtLocation = ArtLocationList.getInstance().get(mIndex);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setTitle(mArtLocation.title);

        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        boolean isWiFi = networkInfo.getType() == ConnectivityManager.TYPE_WIFI;
        if (savedInstanceState == null && !isWiFi) {
            MaterialDialog dialog = new MaterialDialog.Builder(this)
                    .content("You are using mobile data. Displaying these images will use a lot of data. Are you sure you want to view them?")
                    .positiveText("Continue")
                    .negativeText("Go back")
                    .callback(new MaterialDialog.ButtonCallback() {
                        @Override
                        public void onPositive(MaterialDialog dialog) {
                            init();
                        }

                        @Override
                        public void onNegative(MaterialDialog dialog) {
                            finish();
                        }
                    })
                    .build();
            dialog.setCanceledOnTouchOutside(false);
            dialog.show();
        } else {
            init();
        }
    }

    private void init() {
        ViewPager pager = (ViewPager) findViewById(R.id.activity_art_images_viewpager);
        FragmentStatePagerAdapter adapter = new FragmentStatePagerAdapter(getFragmentManager()) {
            @Override
            public Fragment getItem(int position) {
                return ArtLocationDetailsFragment.newInstance(mIndex, position);
            }

            @Override
            public int getCount() {
                return mArtLocation.picUrls.size();
            }
        };
        pager.setAdapter(adapter);
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
        public static final String INIT_ART_LOCATION_PIC_INDEX = "art_location_pic_index";
        private ArtLocation mArtLocation;
        private int mPicIndex;

        public static ArtLocationDetailsFragment newInstance(int index, int picIndex) {
            ArtLocationDetailsFragment fragment = new ArtLocationDetailsFragment();
            Bundle args = new Bundle();
            args.putInt(INIT_ART_LOCATION_INDEX, index);
            args.putInt(INIT_ART_LOCATION_PIC_INDEX, picIndex);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mArtLocation = ArtLocationList.getInstance().get(getArguments().getInt(INIT_ART_LOCATION_INDEX));
            mPicIndex = getArguments().getInt(INIT_ART_LOCATION_PIC_INDEX);
        }

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.fragment_art_image, container, false);
            final ProgressBar progressBar = (ProgressBar) v.findViewById(R.id.fragment_art_location_details_image_progressbar);
            final PhotoView imageView = (PhotoView) v.findViewById(R.id.fragment_art_location_details_pic_imageview);

            progressBar.setVisibility(View.VISIBLE);

            Glide.with(this)
                    .load(mArtLocation.picUrls.get(mPicIndex))
                    .listener(new RequestListener<String, GlideDrawable>() {
                        @Override
                        public boolean onException(Exception e, String model, Target<GlideDrawable> target, boolean isFirstResource) {
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(GlideDrawable resource, String model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
                            progressBar.setVisibility(View.GONE);
                            return false;
                        }
                    })
                    .crossFade()
                    .into(new SimpleTarget<GlideDrawable>() {
                        @Override
                        public void onResourceReady(GlideDrawable resource, GlideAnimation<? super GlideDrawable> glideAnimation) {
                            imageView.setImageDrawable(resource);
                        }
                    });

            return v;
        }
    }
}
