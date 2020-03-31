package com.example.hark_v1;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;

import android.app.Activity;

import android.app.ActivityManager;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.content.Intent;
import edu.cmu.pocketsphinx.RecognitionListener;
import android.speech.RecognizerIntent;
import android.content.ActivityNotFoundException;

import android.view.View;



import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

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


import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.SpeechRecognizer;
import edu.cmu.pocketsphinx.SpeechRecognizerSetup;


public class MainActivity extends AppCompatActivity implements RecognitionListener {
    private static final String TAG = "my_hark";
    private ArFragment arFragment;

    Pose camera_loc;
    public String pose;
    public int counter;
    public float degree;



    private SpeechRecognizer recognizer;
    private static final String KWS_SEARCH = "wakeup";
    private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 1;
    private static final int SPEECH_TIMEOUT = 10000;
    private static final double VAD_THRESH = 3.5;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);
//        session = arFragment.getArSceneView().getSession();
        counter = 0;
        degree = 85;





        Scene.OnTouchListener touchListener = (onTouch, whereTouch) -> {
            Frame frame = arFragment.getArSceneView().getArFrame();
            if(frame == null || counter < 15) {
                return true;
            }
            counter = 0;
            Log.i(TAG, "Screen Touched, calling speech to text, counter is: "+counter);
            speech_to_text();
//            camera_loc = frame.getCamera().getDisplayOrientedPose();
//
//            placeText(camera_loc, degree+" degrees", degree);
//            degree+=5;
            return true;
        };

        Scene.OnUpdateListener updateListener = frameTime -> {
            // update counter so we don't put multiple text boxes
            counter +=1;

            /*Frame frame = arFragment.getArSceneView().getArFrame();
            if(frame == null) {
                return;
            }
            camera_loc = frame.getCamera().getDisplayOrientedPose();
            pose = camera_loc.toString();
            if(counter > 200){
                Log.i(TAG, "Timer expired. camera translation & rotation: "+ pose);
                placeText(camera_loc, "180 degrees", 180);
//                counter = 0;
            }
            counter +=1;
//            float[] x = pose.getXAxis();
//            float[] y = pose.getYAxis();
//            float[] z = pose.getZAxis();

//            Log.i("hark", "gotPose");*/

        };




        arFragment.getArSceneView().getScene().setOnTouchListener(touchListener);
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



    }

    /**
     * Place text on the screen in the direction of deg
     * @param loc  current location of the camera
     * @param text  text to be displayed
     * @param deg  angle you want the text to be placed at
     */
    public void placeText(Pose loc, String text, float deg){
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

        //Display text
        //add text view node
        ViewRenderable.builder().setView(this, R.layout.text).build()
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
                    tv = ((ViewRenderable) textNode.getRenderable()).getView().findViewById(R.id.textView);

                    //set the text in the textbox
                    tv.setText(text);
                    tv.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            textNode.setParent(null);
                        }
                    }, 10000);



                });


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

    // ================ Speech to Text ================
    public void speech_to_text() {

        Log.i(TAG, "Getting speech to text");


        /* Bad method using google's UI
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                "Say Something, I'm giving up on you :(");

        try {
            startActivityForResult(intent, 1);
        }catch (ActivityNotFoundException e){
            Toast.makeText( this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }*/

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

        // Create your custom grammar-based search
//        File menuGrammar = new File(assetsDir, "mymenu.gram");
//        recognizer.addGrammarSearch(MENU_SEARCH, menuGrammar);
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
            placeText(camera_loc, speechText, degree);
//            degree+=5;

        }
        else{
            Log.i(TAG, "couldn't understand");

        }
    }
    @Override
    public void onBeginningOfSpeech() {
        Log.i(TAG, "SPEECH BEGAN");
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
