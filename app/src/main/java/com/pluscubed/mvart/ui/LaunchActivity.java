package com.pluscubed.mvart.ui;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.afollestad.materialdialogs.MaterialDialog;
import com.crashlytics.android.Crashlytics;
import com.pluscubed.mvart.BuildConfig;
import com.pluscubed.mvart.R;
import com.pluscubed.mvart.model.ArtLocation;
import com.pluscubed.mvart.model.ArtLocationList;
import com.pluscubed.mvart.network.ArtLocationXmlParser;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import io.fabric.sdk.android.Fabric;

/**
 * Launch Activity
 */
public class LaunchActivity extends AppCompatActivity {

    private DownloadXmlTask mTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launch);
        if (BuildConfig.DEBUG) {
            Fabric.with(this, new Crashlytics());
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setTaskDescription(
                    new ActivityManager.TaskDescription(null, null, getResources().getColor(R.color.task_bar)));
        }

        downloadArtLocationsToList();
    }

    private void downloadArtLocationsToList() {
        if (mTask == null || mTask.getStatus() != AsyncTask.Status.RUNNING) {
            ConnectivityManager connMgr = (ConnectivityManager)
                    getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
            if (networkInfo != null && networkInfo.isConnected()) {
                mTask = new DownloadXmlTask();
                mTask.execute("http://www.mountainview.gov/publicarts/mv_art.xml");
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

    private class DownloadXmlTask extends AsyncTask<String, Void, List<ArtLocation>> {
        @Override
        protected List<ArtLocation> doInBackground(String... urls) {
            InputStream stream = null;
            try {
                // Instantiate the parser
                ArtLocationXmlParser parser = new ArtLocationXmlParser();
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
            } catch (IOException | XmlPullParserException e) {
                e.printStackTrace();
                //return getResources().getString(R.string.connection_error);
            } finally {
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(List<ArtLocation> artLocations) {
            ArtLocationList instance = ArtLocationList.getInstance();
            instance.clear();
            instance.addAll(artLocations);
            Intent intent = new Intent(LaunchActivity.this, MainActivity.class);
            //Bundle options = ActivityOptionsCompat.makeCustomAnimation(LaunchActivity.this, 0, android.R.anim.fade_out).toBundle();
            startActivity(intent/*, options*/);
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }
    }
}
