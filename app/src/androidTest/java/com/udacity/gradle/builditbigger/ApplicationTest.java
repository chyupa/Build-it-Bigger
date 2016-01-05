package com.udacity.gradle.builditbigger;

import android.app.Application;
import android.content.Context;
import android.os.AsyncTask;
import android.test.ApplicationTestCase;
import android.view.View;
import android.widget.Toast;

import com.example.chyupa.myapplication.backend.myApi.MyApi;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.googleapis.services.AbstractGoogleClientRequest;
import com.google.api.client.googleapis.services.GoogleClientRequestInitializer;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
public class ApplicationTest extends ApplicationTestCase<Application> {
    private CountDownLatch signal;
    private String joke;
    public ApplicationTest() {
        super(Application.class);
    }

    @Override
    protected void setUp() throws Exception{
        super.setUp();
        signal = new CountDownLatch(1);
    }

    @Override
    protected void tearDown() throws Exception {
        signal.countDown();
    }

    public void testAsyncTask() throws Exception {

        EndpointsAsyncTask endpointsAsyncTask = new EndpointsAsyncTask();
        endpointsAsyncTask.setListener(new EndpointsAsyncTask.JsonGetTaskListener() {
            @Override
            public void onComplete(String jsonString, Exception e) {
                joke = jsonString;
                signal.countDown();
            }
        })
                .execute();
        signal.await();

        assertTrue(!joke.isEmpty());
    }
}

class EndpointsAsyncTask extends AsyncTask<Context, Void, String> {
    private JsonGetTaskListener mListener = null;
    private MyApi myApiService = null;
    private Exception mError = null;

    public EndpointsAsyncTask setListener(JsonGetTaskListener listener) {
        this.mListener = listener;
        return this;
    }

    public static interface JsonGetTaskListener {
        public void onComplete(String jsonString, Exception e);
    }

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
        }
        try {
            return myApiService.getJoke().execute().getData();
        } catch (IOException e) {
            return e.getMessage();
        }
    }

    /**
     * @param result
     */
    @Override
    protected void onPostExecute(String result) {
        if (this.mListener != null)
            this.mListener.onComplete(result, mError);

    }
}