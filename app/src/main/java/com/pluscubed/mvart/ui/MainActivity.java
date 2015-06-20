package com.pluscubed.mvart.ui;

import android.animation.ObjectAnimator;
import android.app.ActivityManager;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.Target;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.pluscubed.mvart.R;
import com.pluscubed.mvart.model.ArtLocation;
import com.pluscubed.mvart.model.ArtLocationList;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 *
 */
public class MainActivity extends AppCompatActivity {

    public static final String STATE_MAP_MODE = "mapMode";
    public static final String FRAGMENT_MAP = "Map";
    public static final String FRAGMENT_LIST = "List";
    private List<Marker> mMarkers;
    private Toolbar mActionBarToolbar;
    private boolean mMapMode;

    public static int convertDpToPx(Context context, float dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density
                + 0.5f);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_MAP_MODE, mMapMode);
    }

    protected Toolbar getActionBarToolbar() {
        if (mActionBarToolbar == null) {
            mActionBarToolbar = (Toolbar) findViewById(R.id.toolbar_generic);
            if (mActionBarToolbar != null) {
                setSupportActionBar(mActionBarToolbar);
            }
        }
        return mActionBarToolbar;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.map_main, menu);
        MenuItem item = menu.findItem(R.id.action_search);
        MenuItemCompat.setOnActionExpandListener(item, new MenuItemCompat.OnActionExpandListener() {
            private boolean mListShownBeforeSearch;

            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                mListShownBeforeSearch = findViewById(R.id.activity_main_list_framelayout).getTranslationY() == 0;
                if (!mListShownBeforeSearch) {
                    showList();
                }
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                if (!mListShownBeforeSearch) {
                    hideList();
                }
                ArtLocationListFragment fragment = (ArtLocationListFragment) getFragmentManager().findFragmentByTag(FRAGMENT_LIST);
                fragment.updateDataAndSetSearch("");
                return true;
            }
        });
        SearchView view = (SearchView) MenuItemCompat.getActionView(item);
        view.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                ArtLocationListFragment fragment = (ArtLocationListFragment) getFragmentManager().findFragmentByTag(FRAGMENT_LIST);
                fragment.updateDataAndSetSearch(newText);
                return false;
            }
        });
        return super.onCreateOptionsMenu(menu);
    }


    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getActionBarToolbar();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setTaskDescription(
                    new ActivityManager.TaskDescription(null, null, getResources().getColor(R.color.task_bar)));
        }

        mMarkers = new ArrayList<>();
        if (savedInstanceState == null) {
            mMapMode = true;
            addOnGlobalLayoutListener(findViewById(android.R.id.content), new Runnable() {
                @Override
                public void run() {
                    addArtLocationMarkersFromList(getMap());
                }
            });
        } else {
            mMapMode = savedInstanceState.getBoolean(STATE_MAP_MODE);
        }

        //ADD FRAGMENTS
        addMapFragment();

        FragmentManager manager = getFragmentManager();
        ArtLocationListFragment listFragment = (ArtLocationListFragment) manager.findFragmentByTag(FRAGMENT_LIST);
        if (listFragment == null) {
            listFragment = new ArtLocationListFragment();
            manager.beginTransaction()
                    .add(R.id.activity_main_list_framelayout, listFragment, FRAGMENT_LIST)
                    .commit();
        }
        //

        if (!mMapMode) {
            showAndUpdateList();
        } else {
            addOnGlobalLayoutListener(findViewById(R.id.activity_main_list_framelayout), new Runnable() {
                @Override
                public void run() {
                    final FrameLayout listFragmentFrame = (FrameLayout) findViewById(R.id.activity_main_list_framelayout);
                    listFragmentFrame.setTranslationY(listFragmentFrame.getMeasuredHeight());
                }
            });
        }

        LinearLayout openListBar = (LinearLayout) findViewById(R.id.activity_main_listbar_linearlayout);
        openListBar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAndUpdateList();
            }
        });

        setTitle("Mountain View Public Art");
    }

    private void initMap(final MapFragment finalMapFragment) {
        addOnGlobalLayoutListener(findViewById(android.R.id.content), new Runnable() {
            @Override
            public void run() {
                finalMapFragment.getMapAsync(new OnMapReadyCallback() {
                    @Override
                    public void onMapReady(final GoogleMap googleMap) {
                        googleMap.setMyLocationEnabled(true);
                        googleMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {

                            private Bitmap mLoadingBitmap;
                            private Marker mCurrentlyShowing;

                            @Override
                            public View getInfoWindow(Marker marker) {
                                return null;
                            }

                            @Override
                            public View getInfoContents(final Marker marker) {
                                final View v = getLayoutInflater().inflate(R.layout.map_info_window, null);
                                final ArtLocation artLocation = ArtLocationList.getInstance().get(mMarkers.indexOf(marker));
                                ImageView imageView = (ImageView) v.findViewById(R.id.info_window_image);
                                final TextView progressBar = (TextView) v.findViewById(R.id.info_window_loading_image);
                                TextView title = (TextView) v.findViewById(R.id.info_window_title);
                                TextView snippet = (TextView) v.findViewById(R.id.info_window_desc);

                                progressBar.setVisibility(View.VISIBLE);

                                title.setText(artLocation.title);
                                snippet.setText(artLocation.getFormattedDesc(MainActivity.this));

                                if (!marker.equals(mCurrentlyShowing) || mLoadingBitmap == null) {
                                    imageView.setVisibility(View.GONE);
                                    new Thread(new Runnable() {
                                        @Override
                                        public void run() {
                                            try {
                                                mLoadingBitmap = Glide.with(MainActivity.this)
                                                        .load(artLocation.thumbnailPicUrl)
                                                        .asBitmap()
                                                        .into(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                                                        .get();
                                                runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        marker.showInfoWindow();
                                                    }
                                                });
                                            } catch (InterruptedException | ExecutionException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    }).start();
                                } else {
                                    imageView.setVisibility(View.VISIBLE);
                                    imageView.setImageBitmap(mLoadingBitmap);
                                    progressBar.setVisibility(View.GONE);
                                }

                                v.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        int infoWindowHeight = v.getMeasuredHeight();
                                        Projection projection = googleMap.getProjection();

                                        Point markerScreenPosition = projection.toScreenLocation(marker.getPosition());
                                        Point pointHalfScreenAbove = new Point(markerScreenPosition.x,
                                                (markerScreenPosition.y - convertDpToPx(MainActivity.this, 40) - (infoWindowHeight / 2)));

                                        LatLng aboveMarkerLatLng = projection.fromScreenLocation(pointHalfScreenAbove);

                                        CameraUpdate center = CameraUpdateFactory.newLatLng(aboveMarkerLatLng);
                                        googleMap.animateCamera(center, 300, null);
                                    }
                                });

                                mCurrentlyShowing = marker;

                                return v;
                            }
                        });
                        setOnInfoWindowClickListener(googleMap);
                        addArtLocationMarkersFromList(googleMap);
                    }
                });
            }
        });
    }

    private void addMapFragment() {
        FragmentManager manager = getFragmentManager();
        MapFragment mapFragment = (MapFragment) manager
                .findFragmentByTag(FRAGMENT_MAP);
        if (mapFragment == null) {
            GoogleMapOptions options = new GoogleMapOptions();
            options.camera(new CameraPosition.Builder().target(new LatLng(37.391818, -122.078349))
                    .zoom(12).build());
            mapFragment = MapFragment.newInstance(options);
            manager.beginTransaction()
                    .add(R.id.activity_main_map_framelayout, mapFragment, FRAGMENT_MAP)
                    .commit();
        }
        initMap(mapFragment);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                hideList();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (mMapMode) {
            super.onBackPressed();
        } else {
            hideList();
        }
    }

    public void showMarkerInfoWindow(int index) {
        if (!mMapMode) {
            hideList();
        }
        mMarkers.get(index).showInfoWindow();
    }

    private void hideList() {
        final FrameLayout layout = (FrameLayout) findViewById(R.id.activity_main_list_framelayout);
        ObjectAnimator animator = new ObjectAnimator();
        animator.setProperty(View.TRANSLATION_Y);
        animator.setTarget(layout);
        animator.setFloatValues(layout.getHeight());
        animator.setDuration(250);
        animator.start();
        mMapMode = true;
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
    }

    private void showAndUpdateList() {
        showList();

        //Update list items
        ((ArtLocationListFragment) getFragmentManager().findFragmentByTag(FRAGMENT_LIST)).updateDataAndSetSearch("");
    }

    private void showList() {
        final FrameLayout listFrame = (FrameLayout) findViewById(R.id.activity_main_list_framelayout);
        ObjectAnimator animator = new ObjectAnimator();
        animator.setProperty(View.TRANSLATION_Y);
        animator.setFloatValues(0f);
        animator.setTarget(listFrame);
        animator.setDuration(250);
        animator.start();
        mMapMode = false;
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void setOnInfoWindowClickListener(GoogleMap googleMap) {
        googleMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker) {
                Intent i = new Intent(MainActivity.this, ArtLocationDetailsActivity.class);
                i.putExtra(ArtLocationDetailsActivity.ART_LOCATION_INDEX, mMarkers.indexOf(marker));
                startActivity(i);
            }
        });
    }


    private void addOnGlobalLayoutListener(final View view, final Runnable r) {
        ViewTreeObserver vto = view.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                new Handler(Looper.getMainLooper()).post(r);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                } else {
                    //noinspection deprecation
                    view.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }
            }
        });
    }

    private void addArtLocationMarkersFromList(GoogleMap map) {
        if (map != null) {
            List<ArtLocation> list = ArtLocationList.getInstance().getList();
            map.clear();
            mMarkers.clear();
            for (ArtLocation art : list) {
                LatLng latLng = new LatLng(art.latitude, art.longitude);
                MarkerOptions position = new MarkerOptions().position(latLng);
                if (art.endDate != art.startDate) {
                    position.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                }
                Marker marker = map.addMarker(position);
                mMarkers.add(marker);
            }
        }
    }

    private GoogleMap getMap() {
        return ((MapFragment) getFragmentManager().findFragmentByTag(FRAGMENT_MAP)).getMap();
    }


}