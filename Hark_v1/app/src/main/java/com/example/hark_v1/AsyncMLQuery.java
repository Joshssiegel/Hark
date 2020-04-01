package com.example.hark_v1;

import android.content.Context;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

public class AsyncMLQuery extends AsyncTask<String, String, String> {

    private Context mContext;
    private MediaRecorder recorder = null;

    private static final String TAG = "my_hark";
    private static String SOUND_FILENAME;




    public AsyncMLQuery(Context context) {
        //Relevant Context should be provided to newly created components (whether application context or activity context)
        //getApplicationContext() - Returns the context for all activities running in application
        mContext = context.getApplicationContext();
        SOUND_FILENAME = mContext.getExternalCacheDir().getAbsolutePath();
        SOUND_FILENAME += "/audiorecordtest.3gp";
    }




    //Execute this before the request is made
    @Override
    protected void onPreExecute() {
        // A toast provides simple feedback about an operation as popup.
        // It takes the application Context, the text message, and the duration for the toast as arguments
        Log.i(TAG, "Pre-Execute");
    }

    //Perform the request in background
    @Override
    protected String doInBackground(String... strings) {
        while(true) {
            HttpURLConnection connection;
            try {
                String url = strings[0];
                Log.i(TAG, "CONTACTING ML SERVER AT: " + url);
                //Open a new URL connection
                connection = (HttpURLConnection) new URL(url)
                        .openConnection();

                //Defines a HTTP request type
                connection.setRequestMethod("POST");

                //Sets headers: Content-Type, Authorization
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Accept", "application/json");
                connection.setRequestProperty("Authorization", "Token fab11c9b6bd4215a989c5bf57eb678");

                Log.i(TAG, "Starting Recording for env sounds");
                start_recording();
                //Wait 4 seconds
                Thread.sleep(4000);
                stop_recording();
                Log.i(TAG, "Finished Recording for env sounds");


                final ByteArrayOutputStream out = new ByteArrayOutputStream();
                try (final InputStream in = new FileInputStream(SOUND_FILENAME)) {
                    final byte[] buf = new byte[3 * (int) Math.pow(2, 20)];
                    int n;
                    while ((n = in.read(buf)) >= 0) {
                        out.write(buf, 0, n);
                    }
                }
                final byte[] data = out.toByteArray();
                String data_b64 = Base64.encodeToString(data, Base64.DEFAULT);
                Log.i("base64", data_b64);

                //Add POST data in JSON format
                JSONObject jsonParam = new JSONObject();
                try {
                    jsonParam.put("recording", data_b64);
                } catch (JSONException e) {
                    Log.e(TAG, "ERROR creating JSON: " + e.toString());
                    continue;
                }

                Log.i(TAG, "Sending Recording to Server");

                //Create a writer object and make the request
                OutputStreamWriter outputStream = new OutputStreamWriter(connection.getOutputStream());
                outputStream.write(jsonParam.toString());
                outputStream.flush();
                outputStream.close();

                InputStream inputStream = new BufferedInputStream(connection.getInputStream());
                String ResponseData = convertStreamToString(inputStream);
                //Get the Response code for the request
                //            return connection.getResponseCode();
                Log.i(TAG, "MACHINE LEARNING SERVER RESPONSE: " + ResponseData);
                publishProgress(ResponseData);
            } catch (Exception e) {
                Log.e(TAG, "ERROR with async ML task: " + e.toString());

            }

        }

    }

    public void start_recording(){
        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        recorder.setOutputFile(SOUND_FILENAME);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            recorder.prepare();
        } catch (IOException e) {
            Log.e(TAG, "prepare() failed");
            return;
        }

        recorder.start();
    }
    private void stop_recording() {
        recorder.stop();
        recorder.release();
        recorder = null;
    }

    public String convertStreamToString(InputStream is) {

        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();

        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append((line + "\n"));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }

    //Run this once the background task returns.
    @Override
    protected void onProgressUpdate(String... values)
    {
        super.onProgressUpdate(values);
        String env_sound = values[0];
        Log.i(TAG, "About to give to main thread: " + env_sound);
        MainActivity.soundText = env_sound;
        MainActivity.updateSoundText = true;
    }
}
