package com.udacity.gradle.builditbigger;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.chyupa.myapplication.backend.myApi.MyApi;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.doubleclick.PublisherAdRequest;
import com.google.android.gms.ads.doubleclick.PublisherInterstitialAd;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.googleapis.services.AbstractGoogleClientRequest;
import com.google.api.client.googleapis.services.GoogleClientRequestInitializer;
import com.udacity.androidjokelibrary.JokeActivity;

import java.io.IOException;


public class MainActivity extends ActionBarActivity {

    private ProgressBar spinner;
    private PublisherInterstitialAd publisherInterstitialAd;
    private Boolean adIsLoading = true;
    private String joke = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /**
         * get the spinner
         * set visibility gone
         */
        spinner = (ProgressBar) findViewById(R.id.spinner);
        spinner.setVisibility(View.GONE);

        /**
         * set add unit for the interstitial ad
         */
        publisherInterstitialAd = new PublisherInterstitialAd(this);
        publisherInterstitialAd.setAdUnitId(getString(R.string.banner_ad_unit_id));

        /**
         * add a listener for the add
         */
        publisherInterstitialAd.setAdListener(new AdListener() {
            /**
             * when the ad is closed request a new interstitial ad and display the new activity with the joke retrieved from GCE module
             */
            @Override
            public void onAdClosed() {
                requestNewInterstitial();
                showJokeActivity(getApplicationContext());
            }

            /**
             * set adIsLoading to false
             */
            @Override
            public void onAdLoaded() {
                adIsLoading = false;
            }

            /**
             * debug add
             * show an errorCode if the ad fails to load
             * @param errorCode
             */
            @Override
            public void onAdFailedToLoad(int errorCode) {
//                Toast.makeText(getApplicationContext(), errorCode, Toast.LENGTH_LONG).show();
            }
        });

        requestNewInterstitial();

        /**
         * removed the onClick from the layout
         * get the tell joke button
         * set click listener to retrieve joke from GCE module
         */
        Button tellJoke = (Button) findViewById(R.id.tell_joke_btn);
        tellJoke.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                spinner.setVisibility(View.VISIBLE);
                new EndpointsAsyncTask().execute(getApplicationContext());
            }
        });
    }

    /**
     * request a new interstitial ad and load it
     */
    private void requestNewInterstitial() {
        PublisherAdRequest adRequest = new PublisherAdRequest.Builder()
                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                .build();

        publisherInterstitialAd.loadAd(adRequest);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * show joke activity
     * check if the interstitial ad is loaded
     * @param context
     */
    public void showJokeActivity(Context context) {
        if (!adIsLoading && !publisherInterstitialAd.isLoaded()) {
            adIsLoading = true;
            PublisherAdRequest adRequest = new PublisherAdRequest.Builder().build();
            publisherInterstitialAd.loadAd(adRequest);
        }
        Intent intent = new Intent(context.getApplicationContext(), JokeActivity.class);
        intent.putExtra("joke", joke);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        context.getApplicationContext().startActivity(intent);
    }

    /**
     * async task to retrieve the joke from the GCE module
     */
    class EndpointsAsyncTask extends AsyncTask<Context, Void, String> {
        private MyApi myApiService = null;
        private Context context;

        @Override
        protected String doInBackground(Context... params) {
            if (myApiService == null) {  // Only do this once
                MyApi.Builder builder = new MyApi.Builder(AndroidHttp.newCompatibleTransport(),
                        new AndroidJsonFactory(), null)
                        // options for running against local devappserver
                        // - 10.0.2.2 is localhost's IP address in Android emulator
                        // - turn off compression when running against local devappserver
                        .setRootUrl("http://10.0.2.2:8080/_ah/api/")
                        .setGoogleClientRequestInitializer(new GoogleClientRequestInitializer() {
                            @Override
                            public void initialize(AbstractGoogleClientRequest<?> abstractGoogleClientRequest) throws IOException {
                                abstractGoogleClientRequest.setDisableGZipContent(true);
                            }
                        });
                // end options for devappserver

                myApiService = builder.build();
                context = params[0];
            }
            try {
                return myApiService.getJoke().execute().getData();
            } catch (IOException e) {
                return e.getMessage();
            }
        }

        /**
         * hide the spinner
         * show the insterstitial add
         * show toast if the interstitial ad is not loaded
         * call showJokeActivity method
         * @see showJokeActivity()
         * @param result
         */
        @Override
        protected void onPostExecute(String result) {
            spinner.setVisibility(View.GONE);
            joke = result;
            if (publisherInterstitialAd != null && publisherInterstitialAd.isLoaded()) {
                publisherInterstitialAd.show();
            } else {
                Toast.makeText(context.getApplicationContext(), "Ad did not load", Toast.LENGTH_SHORT).show();
                showJokeActivity(context.getApplicationContext());
            }

        }
    }

}

