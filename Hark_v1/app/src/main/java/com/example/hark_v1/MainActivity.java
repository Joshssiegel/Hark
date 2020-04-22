package com.example.hark_v1;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;

import android.app.Activity;

import android.app.ActivityManager;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.content.Intent;
import edu.cmu.pocketsphinx.RecognitionListener;

import android.os.Handler;
import android.speech.RecognizerIntent;
import android.content.ActivityNotFoundException;

import android.view.View;



import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.ar.core.Anchor;
import com.google.ar.core.Camera;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.SharedCamera;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;


import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.net.ssl.HttpsURLConnection;

import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.SpeechRecognizer;
import edu.cmu.pocketsphinx.SpeechRecognizerSetup;

import static com.google.ar.sceneform.utilities.SceneformBufferUtils.readStream;


public class MainActivity extends AppCompatActivity implements RecognitionListener {
    private static final String TAG = "my_hark";
    private ArFragment arFragment;

    Pose camera_loc;
    public String pose;
    public int counter;
    public float degree;



    private SpeechRecognizer recognizer;
    private MediaRecorder recorder = null;


    private static final String KWS_SEARCH = "wakeup";
    private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 1;
    private static final int SPEECH_TIMEOUT = 10000;
    private static final double VAD_THRESH = 2.75;

    // URL of the Server running the ML and degree detection algorithms
    private static final String SERVER_URL = "https://9e895d07.ngrok.io";

    public static boolean updateSoundText = false;
    public static String soundText = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);
//        session = arFragment.getArSceneView().getSession();
        counter = 0;
        degree = 85;







        Scene.OnUpdateListener updateListener = frameTime -> {
            // update counter so we don't put multiple text boxes
            counter +=1;
            if(updateSoundText){
                Log.i(TAG, "Found Envronmental Sound Text to place: "+soundText);
                Frame frame = arFragment.getArSceneView().getArFrame();
                camera_loc = frame.getCamera().getDisplayOrientedPose();
                placeText(camera_loc, soundText, degree, true);
                updateSoundText = false;
                soundText = "";
            }

        };




//        arFragment.getArSceneView().getScene().setOnTouchListener(touchListener);
        arFragment.getArSceneView().getScene().addOnUpdateListener(updateListener);

        // Keep the screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Check if user has given permission to record audio
        int permissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSIONS_REQUEST_RECORD_AUDIO);
            return;
        }

        // start speech to text setup
        runRecognizerSetup();


        //Instantiate Async task
        AsyncMLQuery myTask = new AsyncMLQuery(this);

        //Run the task in an asynchronous way
        myTask.execute(SERVER_URL+"/env_classifier");



    }

    /**
     * Query the server for the degree
     * @return degree
     */
    public void getDegreeFromServer(){
        try {
            String url = SERVER_URL+"/getangle";
            RequestQueue queue;
            queue = Volley.newRequestQueue(this);
            StringRequest request = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    Log.i(TAG, "Response from server: "+response);
                    if (response != null) {
                        try {
                            //handle your response
                            Log.i(TAG, "Got response: " +response);
                            degree = Float.parseFloat(response);
                        } catch (Exception e) {
                            Log.e(TAG, e.toString());
                            degree = 90;
                        }
                    }
                    else{
                        degree = 90;
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e("error", error.toString());
                    degree = 90;
                }
            });
            queue.add(request);

        }catch(Exception e){
            Log.i(TAG, "!!!! Server is not up");
//            e.printStackTrace();
            Log.e(TAG, e.toString());
            degree = 90;
        }

    }

    /**
     * Place text on the screen in the direction of deg
     * @param loc  current location of the camera
     * @param text  text to be displayed
     * @param deg  angle you want the text to be placed at
     */
    public void placeText(Pose loc, String text, float deg, boolean env_sound){
        // If ARCore is not tracking yet, then don't process anything.
        if (arFragment.getArSceneView().getArFrame().getCamera().getTrackingState() != TrackingState.TRACKING) {
            Log.i(TAG, "Can't place text because ARCore isn't tracking");
            return;
        }
        if(text == null || text.equals("")){
            Log.i(TAG, "cant place empty text");
            return;
        }
        // get the current session so we can add to it
        Session session = arFragment.getArSceneView().getSession();

        //Get current location and rotation of camera to place an anchor point there
        float[] pos = loc.getTranslation();
        float[] rotation = loc.getRotationQuaternion();



        // Create the Anchor at camera location.
        Anchor anchor = session.createAnchor(new Pose(pos, rotation));
        AnchorNode anchorNode = new AnchorNode(anchor);

        // set the parent of the anchor to be the scene
        anchorNode.setParent(arFragment.getArSceneView().getScene());
        int layout;
        if(!env_sound) {
           layout = R.layout.text;
        }
        else{
            layout = R.layout.env_sound_text;
        }

        //Display text
        //add text view node
        ViewRenderable.builder().setView(this, layout).build()
                .thenAccept(viewRenderable -> {
                    // create a textNode
                    Node textNode = new Node();
                    // set the parent of the textNode to be the scene and the anchor
                    textNode.setParent(arFragment.getArSceneView().getScene());
                    textNode.setParent(anchorNode);
                    // set the display to be the textbox defined in res/layout/text.xml
                    textNode.setRenderable(viewRenderable);
                    // we don't want the textbox to cast a shadow or be under a shadow
                    textNode.getRenderable().setShadowReceiver(false);
                    textNode.getRenderable().setShadowCaster(false);

                    // Position the text in the direction of the sound
                    textNode.setLocalPosition(getVectorFromDegree(deg));

                    // Angle the textbox to face towards the camera
                    textNode.setLocalRotation(Quaternion.axisAngle(new Vector3(0f,1f, 0f), (degree-90) ));

                    // Get the textbox layout so we can edit the text in it
                    TextView tv;
                    if(!env_sound) {
                        tv = ((ViewRenderable) textNode.getRenderable()).getView().findViewById(R.id.textView);
                    }
                    else{
                        tv = ((ViewRenderable) textNode.getRenderable()).getView().findViewById(R.id.envSoundTextView);
                    }

                    //set the text in the textbox
                    tv.setText(text);
                    tv.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            textNode.setParent(null);
                        }
                    }, 5000);



                });





        if(degree > 115) {
            // Create the Anchor at camera location.
            ViewRenderable.builder().setView(this, R.layout.left_arrow).build()
                    .thenAccept(viewRenderable -> {
                        // create a textNode
                        Node arrowNode = new Node();
                        // set the parent of the textNode to be the scene and the anchor
                        arrowNode.setParent(arFragment.getArSceneView().getScene());
                        arrowNode.setParent(anchorNode);
                        // set the display to be the textbox defined in res/layout/text.xml
                        arrowNode.setRenderable(viewRenderable);
                        // we don't want the textbox to cast a shadow or be under a shadow
                        arrowNode.getRenderable().setShadowReceiver(false);
                        arrowNode.getRenderable().setShadowCaster(false);

                        // Position the text in the direction of the sound
                        arrowNode.setLocalPosition(getVectorFromDegree(105));

                        // Angle the textbox to face towards the camera
                        arrowNode.setLocalRotation(Quaternion.axisAngle(new Vector3(0f, 1f, 0f), (110 - 90)));

                        // Get the textbox layout so we can edit the text in it
                        ImageView iv;
                        iv = ((ViewRenderable) arrowNode.getRenderable()).getView().findViewById(R.id.leftArrow);

                        Log.i(TAG, "Placing Arrow");
                        iv.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                arrowNode.setParent(null);
                            }
                        }, 1800);


                    });
        }
        else if(degree < 65) {
            // Create the Anchor at camera location.
            ViewRenderable.builder().setView(this, R.layout.right_arrow).build()
                    .thenAccept(viewRenderable -> {
                        // create a textNode
                        Node arrowNode = new Node();
                        // set the parent of the textNode to be the scene and the anchor
                        arrowNode.setParent(arFragment.getArSceneView().getScene());
                        arrowNode.setParent(anchorNode);
                        // set the display to be the textbox defined in res/layout/text.xml
                        arrowNode.setRenderable(viewRenderable);
                        // we don't want the textbox to cast a shadow or be under a shadow
                        arrowNode.getRenderable().setShadowReceiver(false);
                        arrowNode.getRenderable().setShadowCaster(false);

                        // Position the text in the direction of the sound
                        arrowNode.setLocalPosition(getVectorFromDegree(75));

                        // Angle the textbox to face towards the camera
                        arrowNode.setLocalRotation(Quaternion.axisAngle(new Vector3(0f, 1f, 0f), (70 - 90)));

                        // Get the textbox layout so we can edit the text in it
                        ImageView iv;
                        iv = ((ViewRenderable) arrowNode.getRenderable()).getView().findViewById(R.id.rightArrow);

                        Log.i(TAG, "Placing Right Arrow");
                        iv.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                arrowNode.setParent(null);
                            }
                        }, 1800);


                    });
            }
            arFragment.getArSceneView().getScene().addChild(anchorNode);

        }



    /**
     *
     * @param deg
     * @return Vector3 representing the XYZ unit vector for that angle
     */
    public Vector3 getVectorFromDegree(float deg){
        // x direction is the cos of the angle
        float x = (float) Math.cos(Math.toRadians(deg));
        // z direction is the negative sin of the angle
        float z = -1* (float)Math.sin(Math.toRadians(deg));
        // we want the text to be level with the camera, so y = 0
        float y = 0;
        // create the vector and return it.
        Vector3 vector =new Vector3(x,y,z);
        return vector;
    }








    // =========================== Pocket Sphynx ============================
    private void runRecognizerSetup() {
        // Recognizer initialization is a time-consuming and it involves IO,
        // so we execute it in async task
        new AsyncTask<Void, Void, Exception>() {
            @Override
            protected Exception doInBackground(Void... params) {
                try {
                    Assets assets = new Assets(MainActivity.this);
                    File assetDir = assets.syncAssets();
                    setupRecognizer(assetDir);
                } catch (IOException e) {
                    return e;
                }
                return null;
            }
            @Override
            protected void onPostExecute(Exception result) {
                if (result != null) {
                    System.out.println(result.getMessage());
                    Log.i(TAG, "result from setup: "+result.getMessage());
                } else {
                    switchSearch(KWS_SEARCH);
                    Log.i(TAG, "result from setup is null. is it done?");
                }
            }
        }.execute();
    }

    private void setupRecognizer(File assetsDir) throws IOException {
        recognizer = SpeechRecognizerSetup.defaultSetup()
                .setAcousticModel(new File(assetsDir, "en-us-ptm"))
                .setDictionary(new File(assetsDir, "cmudict-en-us.dict"))
                // Disable this line if you don't want recognizer to save raw
                // audio files to app's storage
                //.setRawLogDir(assetsDir)
                .setBoolean("-allphone_ci", true)
                //
                .setFloat("-vad_threshold", VAD_THRESH)

                .getRecognizer();
        recognizer.addListener(this);


        // Create keyword-activation search.
//        recognizer.addKeyphraseSearch(KWS_SEARCH, KEYPHRASE);

        File languageModel = new File(assetsDir, "common-words.lm");

        recognizer.addNgramSearch("wakeup", languageModel);

        Log.i(TAG, "recognizer is setup");

    }

    @Override
    public void onStop() {
        super.onStop();
        Log.i(TAG, "stopping");
        if (recognizer != null) {
            recognizer.cancel();
            recognizer.shutdown();
        }
    }

    @Override
    public void onPartialResult(Hypothesis hypothesis) {
        if (hypothesis == null){
//            Log.i("my_message", "null hypothesis");
            return;
        }
        String text = hypothesis.getHypstr();
        Log.i(TAG, "partial result: "+text);
        /*if (text.equals(KEYPHRASE)){
            Log.i("my_message", "text is keyphrase");
            switchSearch(MENU_SEARCH);
        } else if (text.equals("hello")) {
            System.out.println("Hello to you too!");
            Log.i("my_message", "HELLOOOO!!!");
        } else if (text.equals("good morning")) {
            System.out.println("Good morning to you too!");
            Log.i("my_message", "ITS NOT MORNING");
        } else {
            System.out.println(hypothesis.getHypstr());
            Log.i("my_message", "Did you say this: "+text);
        }

         */
    }

    @Override
    public void onResult(Hypothesis hypothesis) {
        if (hypothesis != null) {
            String speechText = hypothesis.getHypstr();
            if(speechText == null || speechText.equals("")){
                Log.i(TAG, "empty text");
                return;
            }
            double prob = recognizer.getDecoder().getLogmath().exp(hypothesis.getProb());
            Log.i(TAG, "Read text: "+ speechText+" with prob: "+prob);
            int numWords = speechText.split(" ").length;
            double thresh = 0.2/Math.pow(10,numWords);
            if(prob < thresh){
                Log.i(TAG, "prob: "+prob+" must be more than: "+thresh);
                return;
            }
            Frame frame = arFragment.getArSceneView().getArFrame();
            if(frame == null) {
                Log.i(TAG, "can't process speech, frame is null");
                return;
            }
            camera_loc = frame.getCamera().getDisplayOrientedPose();
            Log.i(TAG, "placing text: "+ speechText + " at "+degree);

//            getDegreeFromServer();
            placeText(camera_loc, speechText, degree, false);

//            degree+=5;

        }
        else{
            Log.i(TAG, "couldn't understand");

        }
    }
    @Override
    public void onBeginningOfSpeech() {
        Log.i(TAG, "SPEECH BEGAN");
        getDegreeFromServer();
    }
    @Override
    public void onEndOfSpeech() {
        Log.i(TAG, "SPEECH ENDED");
        switchSearch(KWS_SEARCH);
//        recognizer.stop();

//        if (!recognizer.getSearchName().equals(KWS_SEARCH)) {
//            switchSearch(KWS_SEARCH);
//        }

    }
    private void switchSearch(String searchName) {
        Log.i(TAG, "Switching search: "+searchName);
        recognizer.stop();
        recognizer.startListening(searchName, SPEECH_TIMEOUT);
        /*if (searchName.equals(KWS_SEARCH))
            recognizer.startListening(searchName);
        else
            recognizer.startListening(searchName, 10000);*/
    }
    @Override
    public void onError(Exception error) {
        System.out.println(error.getMessage());
        Log.e(TAG,"ERROR: "+error.getMessage());
    }
    @Override
    public void onTimeout() {
        Log.i(TAG, "on timeout");
        switchSearch(KWS_SEARCH);
    }

    // =========================== End Pocket Sphynx ========================






    /*@Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode){
            case 1:

                if(resultCode==RESULT_OK && null!=data){

                    ArrayList<String> result =
                            data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

                    String speechText = result.get(0);
                    Log.i(TAG, "Read text: "+ speechText);
                    Frame frame = arFragment.getArSceneView().getArFrame();
                    if(frame == null) {

                        return;
                    }
                    camera_loc = frame.getCamera().getDisplayOrientedPose();
                    Log.i(TAG, "placing text: "+ speechText + " at "+degree);
                    placeText(camera_loc, speechText, degree);
                    degree+=5;
                }

                break;
        }
    }*/




}
