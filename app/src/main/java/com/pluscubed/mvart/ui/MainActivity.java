package com.pluscubed.mvart.ui;

import android.animation.ObjectAnimator;
import android.app.ActivityManager;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.SparseArray;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
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
import com.pluscubed.mvart.network.ArtLocationXmlParser;
import com.pluscubed.mvart.network.DownloadImageTask;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

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
    private DownloadXmlTask mTask;

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
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getActionBarToolbar();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setTaskDescription(
                    new ActivityManager.TaskDescription(null, null, getResources().getColor(R.color.task_bar)));
        }

        mMarkers = new ArrayList<>();
        mMapMode = true;

        if (savedInstanceState == null) {
            downloadArtLocationsToList();
        } else {
            mMapMode = savedInstanceState.getBoolean(STATE_MAP_MODE);
        }

        addMapFragment();

        if (!mMapMode) {
            showList();
        } else {
            addOnGlobalLayoutListener(new Runnable() {
                @Override
                public void run() {
                    final FrameLayout layout = (FrameLayout) findViewById(R.id.activity_main_list_framelayout);
                    layout.setTranslationY(layout.getHeight());
                }
            });
        }

        LinearLayout layout = (LinearLayout) findViewById(R.id.activity_main_listbar_linearlayout);
        layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showList();
            }
        });

        setTitle("Mountain View Public Art");
    }

    private void initMap(final MapFragment finalMapFragment) {
        addOnGlobalLayoutListener(new Runnable() {
            @Override
            public void run() {
                finalMapFragment.getMapAsync(new OnMapReadyCallback() {
                    @Override
                    public void onMapReady(final GoogleMap googleMap) {
                        googleMap.setMyLocationEnabled(true);
                        googleMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
                            @Override
                            public View getInfoWindow(Marker marker) {
                                return null;
                            }

                            @Override
                            public View getInfoContents(final Marker marker) {
                                final View v = getLayoutInflater().inflate(R.layout.map_info_window, null);
                                ArtLocation artLocation = ArtLocationList.getInstance().get(mMarkers.indexOf(marker));
                                ImageView imageView = (ImageView) v.findViewById(R.id.info_window_image);
                                TextView progressBar = (TextView) v.findViewById(R.id.info_window_loading_image);
                                TextView title = (TextView) v.findViewById(R.id.info_window_title);
                                TextView snippet = (TextView) v.findViewById(R.id.info_window_desc);

                                if (artLocation.thumbnailPic == null) {
                                    progressBar.setVisibility(View.VISIBLE);
                                    imageView.setVisibility(View.GONE);
                                    SparseArray<Object> downloadArgs = new SparseArray<>();
                                    downloadArgs.put(DownloadImageTask.DL_IMAGE_ARGS_ARTLOCATION, artLocation);
                                    downloadArgs.put(DownloadImageTask.DL_IMAGE_ARGS_MARKER, marker);
                                    downloadArgs.put(DownloadImageTask.DL_IMAGE_ARGS_THUMBNAIL_BOOL, true);
                                    downloadArgs.put(DownloadImageTask.DL_IMAGE_ARGS_PIC_INDEX, -1);
                                    DownloadImageTask task = new DownloadImageTask();
                                    task.execute(downloadArgs);
                                } else {
                                    progressBar.setVisibility(View.GONE);
                                    imageView.setVisibility(View.VISIBLE);
                                    imageView.setImageDrawable(artLocation.thumbnailPic);
                                }
                                title.setText(artLocation.title);
                                snippet.setText(artLocation.getFormattedDesc(MainActivity.this));

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
        layout.setTranslationY(0f);
        ObjectAnimator animator = new ObjectAnimator();
        animator.setProperty(View.TRANSLATION_Y);
        animator.setTarget(layout);
        animator.setFloatValues(layout.getHeight());
        animator.setDuration(250);
        animator.start();
        mMapMode = true;
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
    }

    private void showList() {
        FragmentManager manager = getFragmentManager();
        ArtLocationListFragment listFragment = (ArtLocationListFragment) manager.findFragmentByTag(FRAGMENT_LIST);
        if (listFragment == null) {
            listFragment = new ArtLocationListFragment();
            manager.beginTransaction()
                    .add(R.id.activity_main_list_framelayout, listFragment, FRAGMENT_LIST)
                    .commit();
        }
        final FrameLayout layout = (FrameLayout) findViewById(R.id.activity_main_list_framelayout);
        layout.setTranslationY(layout.getHeight());
        ObjectAnimator animator = new ObjectAnimator();
        animator.setProperty(View.TRANSLATION_Y);
        animator.setFloatValues(0f);
        animator.setTarget(layout);
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

    private void downloadArtLocationsToList() {
        if (mTask == null || mTask.getStatus() != AsyncTask.Status.RUNNING) {
            ConnectivityManager connMgr = (ConnectivityManager)
                    getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
            if (networkInfo != null && networkInfo.isConnected()) {
                mTask = new DownloadXmlTask();
                mTask.execute("http://pluscubed.github.io/mvart-testing-data/testing_data.xml");
            } else {
                new MaterialDialog.Builder(this)
                        .content(getString(R.string.cant_connect))
                        .positiveText(getString(R.string.dismiss))
                        .negativeText(getString(R.string.retry))
                        .callback(new MaterialDialog.ButtonCallback() {
                            @Override
                            public void onNegative(MaterialDialog dialog) {
                                super.onNegative(dialog);
                                downloadArtLocationsToList();
                            }
                        })
                        .show();
            }
        }
    }

    private void addOnGlobalLayoutListener(final Runnable r) {
        ViewTreeObserver vto = findViewById(android.R.id.content)
                .getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                new Handler(Looper.getMainLooper()).post(r);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    findViewById(android.R.id.content)
                            .getViewTreeObserver().removeOnGlobalLayoutListener(this);
                } else {
                    //noinspection deprecation
                    findViewById(android.R.id.content)
                            .getViewTreeObserver().removeGlobalOnLayoutListener(this);
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
        return mMapMode ? ((MapFragment) getFragmentManager().findFragmentByTag(FRAGMENT_MAP)).getMap() : null;
    }

    private class DownloadXmlTask extends AsyncTask<String, Void, List<ArtLocation>> {
        @Override
        protected List<ArtLocation> doInBackground(String... urls) {
            try {
                InputStream stream = null;
                // Instantiate the parser
                ArtLocationXmlParser parser = new ArtLocationXmlParser();

                try {
                    URL url = new URL(urls[0]);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setDoInput(true);
                    // Starts the query
                    conn.connect();
                    Log.e("", "Connection opened");
                    stream = conn.getInputStream();
                    // Makes sure that the InputStream is closed after the app is
                    // finished using it.
                    return parser.parse(stream);
                } finally {
                    if (stream != null) {
                        stream.close();
                    }
                }
            } catch (IOException | XmlPullParserException e) {
                e.printStackTrace();
                //return getResources().getString(R.string.connection_error);
            }
            return null;
        }

        @Override
        protected void onPostExecute(List<ArtLocation> artLocations) {
            ArtLocationList instance = ArtLocationList.getInstance();
            instance.clear();
            instance.addAll(artLocations);
            addArtLocationMarkersFromList(getMap());
        }
    }
}