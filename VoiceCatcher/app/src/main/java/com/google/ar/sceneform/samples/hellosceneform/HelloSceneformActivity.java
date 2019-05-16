/*
 * Copyright 2018 Google LLC. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.ar.sceneform.samples.hellosceneform;

import com.app.androidkt.speechapi.SpeechAPI;
import com.app.androidkt.speechapi.VoiceRecorder;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;

import android.os.AsyncTask;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;

import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;

import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.ar.core.Anchor;
import com.google.ar.core.Frame;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.Camera;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.Sun;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.FootprintSelectionVisualizer;
import com.google.ar.sceneform.ux.TransformableNode;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * This is an example activity that uses the Sceneform UX package to make common AR tasks easier.
 */
public class HelloSceneformActivity extends AppCompatActivity {
  private static final String TAG = HelloSceneformActivity.class.getSimpleName();
  private static final double MIN_OPENGL_VERSION = 3.1;

  private ArFragment arFragment;
  private ArSceneView arSceneView;
  private ViewRenderable textViewRenderable;
  public int bubbleflag;
    private static final int PERMISSION_REQUESTS = 1;
    public Boolean loadingDone = false;
    public static Boolean faceSuccess = false;

    //THREAD
    findface findFace = new findface();
    UI_Task task = new UI_Task();
    public static Boolean LOCK = false;

    public Session mSession;
    public Frame mFrame;
    private ArrayBlockingQueue<MotionEvent> mQueuedSingleTaps = new ArrayBlockingQueue<>(16);
    private SpeechAPI speechAPI;
    private VoiceRecorder mVoiceRecorder;
    private static final int RECORD_REQUEST_CODE = 101;

    private ImageButton button1;
    private ImageButton button2;
    private ImageButton button3;

    private String a = "abc";
    private TextView mTextView;
    private TextView mTextView2;
    private TextView mTextView3;
    float FacePointX;
    float FacePointY;
    float FacePlusX;
    float FacePlusY;

    @BindView(R.id.status)
    TextView status;
    @BindView(R.id.textMessage)
    TextView textMessage;
    @BindView(R.id.listview)
    ListView listView;
    private List<String> stringList;

    private final VoiceRecorder.Callback mVoiceCallback = new VoiceRecorder.Callback() {
        @Override
        public void onVoiceStart() {
            if (speechAPI != null) {
                speechAPI.startRecognizing(mVoiceRecorder.getSampleRate());
            }        }

        @Override
        public void onVoice(byte[] data, int size) {
            if (speechAPI != null) {
                if(LOCK == false){
                    face_thread_runnable.run();
                }
                speechAPI.recognize(data, size);
            }        }

        @Override
        public void onVoiceEnd() {
            if (speechAPI != null) {
                speechAPI.finishRecognizing();
            }
        }
    };

    private ArrayAdapter adapter;
    private final SpeechAPI.Listener mSpeechServiceListener = new SpeechAPI.Listener() {
        @Override
        public void onSpeechRecognized(final String text, final boolean isFinal) {
            if (isFinal) {
                mVoiceRecorder.dismiss();
            }
            if (a != null && !TextUtils.isEmpty(text)){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (isFinal){
                            //a = null;
                            if(faceSuccess == true) {
                                onClear();
                                rend(a, bubbleflag);
                            }
                            textMessage.setText(null);
                            stringList.add(0,text);
                            adapter.notifyDataSetChanged();

                        }
                        else{
                            //onClear();
                            a = text;

                            textMessage.setText(text);
                        }
                    }
                });
            }
        }
    };

    @Override
  @SuppressWarnings({"AndroidApiChecker", "FutureReturnValueIgnored"})
  // CompletableFuture requires api level 24
  // FutureReturnValueIgnored is not valid
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    onStart();
    if (!checkIsSupportedDeviceOrFinish(this)) {
      return;
    }

        mTextView = (TextView) findViewById(R.id.bubble1);

    setContentView(R.layout.activity_ux);
    arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);
    arSceneView = arFragment.getArSceneView();

      arFragment.getPlaneDiscoveryController().hide();
      arFragment.getPlaneDiscoveryController().setInstructionView(null);
      arSceneView.getPlaneRenderer().setEnabled(false);

        //Sppech
        ButterKnife.bind(this);
        speechAPI = new SpeechAPI(HelloSceneformActivity.this);
        stringList = new ArrayList<>();
        adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, stringList);
        listView.setAdapter(adapter);

        button1 = (ImageButton)findViewById(R.id.bbutton1);
        button2 = (ImageButton)findViewById(R.id.bbutton2);
        button3 = (ImageButton)findViewById(R.id.bbutton3);

        button1.setOnClickListener(new View.OnClickListener(){
          public void onClick(View v){
              bubbleflag = 0;
              button1.setBackgroundColor(Color.parseColor("#FFFFFF"));
              button2.setBackgroundColor(Color.parseColor("#00000000"));
              button3.setBackgroundColor(Color.parseColor("#00000000"));
          }
      });
        button2.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                bubbleflag = 1;
                button2.setBackgroundColor(Color.parseColor("#FFFFFF"));
                button1.setBackgroundColor(Color.parseColor("#00000000"));
                button3.setBackgroundColor(Color.parseColor("#00000000"));
            }
        });
        button3.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                bubbleflag = 2;
                button3.setBackgroundColor(Color.parseColor("#FFFFFF"));
                button2.setBackgroundColor(Color.parseColor("#00000000"));
                button1.setBackgroundColor(Color.parseColor("#00000000"));
            }
        });
      }
    @Override
    protected void onStart() {
        super.onStart();
        if (isGrantedPermission(android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startVoiceRecorder();
        }
        else {
            makeRequest(android.Manifest.permission.RECORD_AUDIO);
        }
        if (isGrantedPermission(Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED){

        }
        else {
            makeRequest(Manifest.permission.CAMERA);
        }
        speechAPI.addListener(mSpeechServiceListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onStop() {
        stopVoiceRecorder();
        // Stop Cloud Speech API
        speechAPI.removeListener(mSpeechServiceListener);
        speechAPI.destroy();
        speechAPI = null;
        face_thread_runnable.destroy();
        super.onStop();
    }

    class UI_Task extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            };
            return null;
        }

        @Override
        protected void onPreExecute() {
            loadingDone = false;
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            loadingDone = true;
            super.onPostExecute(aVoid);
        }
    }

    public void onClearButtonClicked(View v){
      onClear();
    }

    public void rend(String a, int bubbleflag){
        if(bubbleflag == 0) {
            ViewRenderable.builder()
                    .setView(this, R.layout.newone_1)
                    .build()
                    .thenAccept(renderable -> {
                                mTextView = renderable.getView().findViewById(R.id.bubble1);
                                mTextView.setText(a);

                                textViewRenderable = renderable;

                                if (textViewRenderable != null) {
                                    FacePlusY = findFace.FacePlusY;
                                    FacePointX = (float) (findFace.FaceCenterX*2.25); // + FacePlusX;
                                    FacePointY = (float) (findFace.FaceY*2960/480) -4*FacePlusY;
                                    float[] touchArray = new float[]{FacePointX, FacePointY};
                                    float objectDistanceFromCamera = -1.0f;
                                    float[] normalizedMetersArr = getNormalizedScreenCoordinates(touchArray, arSceneView.getWidth(), arSceneView.getHeight(), objectDistanceFromCamera);

                                    Pose inAirPose = mFrame.getAndroidSensorPose().compose(Pose.makeTranslation(normalizedMetersArr[0], normalizedMetersArr[1], objectDistanceFromCamera)).extractTranslation();
                                    Anchor anchor = mSession.createAnchor(inAirPose);

                                    // Create the Anchor.
                                    //com.google.ar.core.Pose.makeTranslation(0, 0.5f, 0).compose(hitResult.getHitPose());
                                    //Anchor anchor = hitResult.createAnchor();
                                    AnchorNode anchorNode = new AnchorNode(anchor);
                                    anchorNode.setParent(arSceneView.getScene());

                                    // Create the transformable andy and add it to the anchor.
                                    FootprintSelectionVisualizer footprintSelectionVisualizer = (FootprintSelectionVisualizer)arFragment.getTransformationSystem().getSelectionVisualizer();
                                    TransformableNode andy = new TransformableNode(arFragment.getTransformationSystem());
                                    footprintSelectionVisualizer.removeSelectionVisual(andy);
                                    andy.setParent(anchorNode);
                                    andy.setRenderable(textViewRenderable);
                                    //andy.setLookDirection(direction);
                                    //andy.select();
                                }
                            }
                    );
        }
        if(bubbleflag == 1) {
            ViewRenderable.builder()
                    .setView(this, R.layout.newone_2)
                    .build()
                    .thenAccept(renderable -> {
                                mTextView2 = renderable.getView().findViewById(R.id.bubble2);
                                mTextView2.setText(a);
                                textViewRenderable = renderable;
                                mSession = arSceneView.getSession();
                                Frame mFrame = arSceneView.getArFrame();
                        findface findFace = new findface();
                        findFace.findFace(mFrame);

                        if (textViewRenderable != null) {
                            Log.d(TAG, "얼굴 인식 후 얼굴의 좌표 : (" + findFace.FaceX + ", " + findFace.FaceX + ")");
                            FacePlusY = findFace.FacePlusY;
                            FacePointX = (float) (findFace.FaceCenterX*2.25); // + FacePlusX;
                            FacePointY = (float) (findFace.FaceY*2960/480) -4*FacePlusY;
                            float[] touchArray = new float[]{FacePointX, FacePointY};
                            float objectDistanceFromCamera = -1.0f;
                            float[] normalizedMetersArr = getNormalizedScreenCoordinates(touchArray, arSceneView.getWidth(), arSceneView.getHeight(), objectDistanceFromCamera);

                            Pose inAirPose = mFrame.getAndroidSensorPose().compose(Pose.makeTranslation(normalizedMetersArr[0], normalizedMetersArr[1], objectDistanceFromCamera)).extractTranslation();
                            Anchor anchor = mSession.createAnchor(inAirPose);

                                    AnchorNode anchorNode = new AnchorNode(anchor);
                                    anchorNode.setParent(arSceneView.getScene());

                                    // Create the transformable andy and add it to the anchor.
                                    FootprintSelectionVisualizer footprintSelectionVisualizer = (FootprintSelectionVisualizer)arFragment.getTransformationSystem().getSelectionVisualizer();
                                    TransformableNode andy = new TransformableNode(arFragment.getTransformationSystem());
                                    footprintSelectionVisualizer.removeSelectionVisual(andy);
                                    andy.setParent(anchorNode);
                                    andy.setRenderable(textViewRenderable);
                                    //andy.setLookDirection(direction);
                                    //andy.select();
                                }
                            }
                    );
        }
        if(bubbleflag == 2) {
            ViewRenderable.builder()
                    .setView(this, R.layout.newone_3)
                    .build()
                    .thenAccept(renderable -> {
                                mTextView3 = renderable.getView().findViewById(R.id.bubble3);
                                mTextView3.setText(a);
                                textViewRenderable = renderable;
                                mSession = arSceneView.getSession();
                                Frame mFrame = arSceneView.getArFrame();
                        findface findFace = new findface();
                        findFace.findFace(mFrame);

                        if (textViewRenderable != null) {
                            Log.d(TAG, "얼굴 인식 후 얼굴의 좌표 : (" + findFace.FaceX + ", " + findFace.FaceX + ")");
                            FacePlusY = findFace.FacePlusY;
                            FacePointX = (float) (findFace.FaceCenterX*2.25); // + FacePlusX;
                            FacePointY = (float) (findFace.FaceY*2960/480) -4*FacePlusY;
                            float[] touchArray = new float[]{FacePointX, FacePointY};
                            float objectDistanceFromCamera = -1.0f;
                            float[] normalizedMetersArr = getNormalizedScreenCoordinates(touchArray, arSceneView.getWidth(), arSceneView.getHeight(), objectDistanceFromCamera);

                            Pose inAirPose = mFrame.getAndroidSensorPose().compose(Pose.makeTranslation(normalizedMetersArr[0], normalizedMetersArr[1], objectDistanceFromCamera)).extractTranslation();
                            Anchor anchor = mSession.createAnchor(inAirPose);

                                    AnchorNode anchorNode = new AnchorNode(anchor);
                                    anchorNode.setParent(arSceneView.getScene());

                                    // Create the transformable andy and add it to the anchor.
                                    FootprintSelectionVisualizer footprintSelectionVisualizer = (FootprintSelectionVisualizer)arFragment.getTransformationSystem().getSelectionVisualizer();
                                    TransformableNode andy = new TransformableNode(arFragment.getTransformationSystem());
                                    footprintSelectionVisualizer.removeSelectionVisual(andy);
                                    andy.setParent(anchorNode);
                                    andy.setRenderable(textViewRenderable);
                                    //andy.setLookDirection(direction);
                                   // andy.select();
                                }
                            }
                    );
        }
    }



  /**
   * Returns false and displays an error message if Sceneform can not run, true if Sceneform can run
   * on this device.
   *
   * <p>Sceneform requires Android N on the device as well as OpenGL 3.1 capabilities.
   *
   * <p>Finishes the activity if Sceneform can not run
   */

    public static boolean checkIsSupportedDeviceOrFinish(final Activity activity) {
        if (Build.VERSION.SDK_INT < VERSION_CODES.N) {
            Log.e(TAG, "Sceneform requires Android N or later");
            Toast.makeText(activity, "Sceneform requires Android N or later", Toast.LENGTH_LONG).show();
            activity.finish();
            return false;
        }
        String openGlVersionString =
                ((ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE))
                        .getDeviceConfigurationInfo()
                        .getGlEsVersion();
        if (Double.parseDouble(openGlVersionString) < MIN_OPENGL_VERSION) {
            Log.e(TAG, "Sceneform requires OpenGL ES 3.1 later");
            Toast.makeText(activity, "Sceneform requires OpenGL ES 3.1 or later", Toast.LENGTH_LONG)
                    .show();
            activity.finish();
            return false;
        }
        return true;
    }
    private int isGrantedPermission(String permission) {
        return ContextCompat.checkSelfPermission(this, permission);
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] results) {
        if(requestCode == RECORD_REQUEST_CODE){
            if(results.length == 0 && results[0] == PackageManager.PERMISSION_DENIED
                    && results[0] == PackageManager.PERMISSION_DENIED){
                finish();
            }
            else{
                startVoiceRecorder();
            }
        }
    }

    //SPEECH PART
    private void makeRequest(String permission) {
        ActivityCompat.requestPermissions(this, new String[]{permission}, RECORD_REQUEST_CODE);
    }

    private void startVoiceRecorder() {
        if (mVoiceRecorder != null) {
            mVoiceRecorder.stop();
        }
        mVoiceRecorder = new VoiceRecorder(mVoiceCallback);
        mVoiceRecorder.start();
        task.execute();
    }

    private void stopVoiceRecorder() {
        if (mVoiceRecorder != null) {
            mVoiceRecorder.stop();
            mVoiceRecorder = null;
        }
    }
    //SPEECH PART END
    private void onClear(){
        List<Node> childeren = new ArrayList<>(arSceneView.getScene().getChildren());
        for(Node node : childeren) {
            if (node instanceof AnchorNode) {
                if (((AnchorNode) node).getAnchor() != null) {
                    ((AnchorNode) node).getAnchor().detach();
                }
            }
            if (!(node instanceof Camera) && !(node instanceof Sun)) {
                node.setParent(null);
            }
        }
    }
    private float [] getNormalizedScreenCoordinates(float[] vec2, int screenWidth, int screenHeight, float metersAway){
        if(vec2 == null || vec2.length != 2) return null;

        float [] normalizedTouch = new float[]{vec2[0]/screenWidth, vec2[1]/screenHeight};
        float screenWidthInTranslationMeters = 0.37f; //range is [-0.37,0.37]
        float screenHeightInTranslationMeters = 0.675f; //range is [-0.615, 0.615]

        float normalizedAwayFromCenterX;
        float normalizedMetersAwayFromCenterX;
        if(normalizedTouch[0] >= 0.5f){
            normalizedAwayFromCenterX = (float) ((1.0 - 0.0) / (1.0 - 0.5f) * (normalizedTouch[0] - 1.0f) + 1.0f);
        }else{
            normalizedAwayFromCenterX = (float) ((0.0 + 1.0) / (0.5f - 0f) * (normalizedTouch[0] - 1.0f) + 1.0f);
        }
        normalizedMetersAwayFromCenterX = normalizedAwayFromCenterX * screenWidthInTranslationMeters * Math.abs(metersAway);


        float normalizedAwayFromCenterY ;
        float normalizedMetersAwayFromCenterY;
        if((1-normalizedTouch[1]) < 0.5f){
            normalizedAwayFromCenterY = (float) ((1.0 - 0.0) / (1.0 - 0.5f) * ((1 - normalizedTouch[1]) - 1.0f) + 1.0f);
        }else{
            normalizedAwayFromCenterY = (float) ((0.0 + 1.0) / (0.5f - 0f) * ((1 - normalizedTouch[1]) - 1.0f) + 1.0f);
        }
        normalizedMetersAwayFromCenterY = normalizedAwayFromCenterY * screenHeightInTranslationMeters * Math.abs(metersAway);

        return new float[]{normalizedMetersAwayFromCenterX, normalizedMetersAwayFromCenterY};
    }

    //==============얼굴인식 쓰레드===================
    class Face_Thread_Runnable extends Thread {
        @Override
        public void run() {

        }
    }

    private final Face_Thread_Runnable face_thread_runnable = new Face_Thread_Runnable(){
        @Override
        public void run() {
            arSceneView = arFragment.getArSceneView();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(loadingDone == true)
                    {
                        arSceneView = arFragment.getArSceneView();
                        mSession = arSceneView.getSession();
                        mFrame = arSceneView.getArFrame();
                        findFace.findFace(mFrame);
                    }
                }
            });
        }
    };

    private static boolean isPermissionGranted(Context context, String permission) {
        if (ContextCompat.checkSelfPermission(context, permission)
                == PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "Permission granted: " + permission);
            return true;
        }
        Log.i(TAG, "Permission NOT granted: " + permission);
        return false;
    }

}
