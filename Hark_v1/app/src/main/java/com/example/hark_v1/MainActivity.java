package com.example.hark_v1;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import android.app.Activity;

import android.app.ActivityManager;
import android.content.Context;
import android.net.Uri;
import android.os.Build;

import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
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

import java.util.Arrays;
import java.util.EnumSet;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    private ArFragment arFragment;

    Pose camera_loc;
    public String pose;
    public int counter;
    public float degree;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);
//        session = arFragment.getArSceneView().getSession();
        counter = 0;



        Scene.OnTouchListener touchListener = (onTouch, whereTouch) -> {
            Frame frame = arFragment.getArSceneView().getArFrame();
            if(frame == null || counter < 10) {
                return true;
            }
            camera_loc = frame.getCamera().getDisplayOrientedPose();
            placeText(camera_loc, degree+" degrees", degree);
            degree+=5;
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
                Log.i("my_hark", "Timer expired. camera translation & rotation: "+ pose);
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

                });


        arFragment.getArSceneView().getScene().addChild(anchorNode);
        counter = 0;

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


}
