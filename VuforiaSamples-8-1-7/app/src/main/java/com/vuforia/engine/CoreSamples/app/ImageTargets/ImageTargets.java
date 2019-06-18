/*===============================================================================
Copyright (c) 2019 PTC Inc. All Rights Reserved.

Copyright (c) 2012-2014 Qualcomm Connected Experiences, Inc. All Rights Reserved.

Vuforia is a trademark of PTC Inc., registered in the United States and other 
countries.
===============================================================================*/

package com.vuforia.engine.CoreSamples.app.ImageTargets;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.vuforia.CameraDevice;
import com.vuforia.DataSet;
import com.vuforia.DeviceTracker;
import com.vuforia.ImageTarget;
import com.vuforia.ObjectTracker;
import com.vuforia.PositionalDeviceTracker;
import com.vuforia.Rectangle;
import com.vuforia.STORAGE_TYPE;
import com.vuforia.State;
import com.vuforia.Trackable;
import com.vuforia.TrackableList;
import com.vuforia.TrackableResult;
import com.vuforia.Tracker;
import com.vuforia.TrackerManager;
import com.vuforia.VirtualButton;
import com.vuforia.Vuforia;
import com.vuforia.engine.CoreSamples.app.VirtualButtons.VirtualButtons;
import com.vuforia.engine.CoreSamples.ui.SampleAppMessage;
import com.vuforia.engine.SampleApplication.SampleActivityBase;
import com.vuforia.engine.SampleApplication.utils.SampleAppTimer;
import com.vuforia.engine.SampleApplication.SampleApplicationControl;
import com.vuforia.engine.SampleApplication.SampleApplicationException;
import com.vuforia.engine.SampleApplication.SampleApplicationSession;
import com.vuforia.engine.SampleApplication.utils.LoadingDialogHandler;
import com.vuforia.engine.SampleApplication.utils.SampleApplicationGLView;
import com.vuforia.engine.SampleApplication.utils.Texture;
import com.vuforia.engine.CoreSamples.R;
import com.vuforia.engine.CoreSamples.ui.SampleAppMenu.SampleAppMenu;
import com.vuforia.engine.CoreSamples.ui.SampleAppMenu.SampleAppMenuGroup;
import com.vuforia.engine.CoreSamples.ui.SampleAppMenu.SampleAppMenuInterface;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Vector;

/**
 * The main activity for the ImageTargets sample.
 * Image Targets allows users to create 2D targets for detection and tracking
 *
 * This class does high-level handling of the Vuforia lifecycle and any UI updates
 *
 * For ImageTarget-specific rendering, check out ImageTargetRenderer.java
 * For the low-level Vuforia lifecycle code, check out SampleApplicationSession.java
 */
public class ImageTargets extends SampleActivityBase implements SampleApplicationControl,
    SampleAppMenuInterface
{
    private VirtualButtons myvirtual;
    private int seperate = 30;
    private boolean updateBtns = false;
    private DataSet mDataSet = null;
    private byte buttonMask = 0;
    public final String[] virtualButtonColors = { "red", "blue", "yellow", "green" };
    private ImageTargetRenderer mRenderer;

    // Enumeration for masking button indices into single integer:
    private static final int BUTTON_1 = 1;
    private static final int BUTTON_2 = 2;
    private static final int BUTTON_3 = 4;
    private static final int BUTTON_4 = 8;

    static final int NUM_BUTTONS = 4;

    private static final String LOGTAG = "ImageTargets";
    
    private SampleApplicationSession vuforiaAppSession;

    private DataSet mCurrentDataset;
    private int mCurrentDatasetSelectionIndex = 0;
    private int mStartDatasetsIndex = 0;
    private int mDatasetsNumber = 0;
    private final ArrayList<String> mDatasetStrings = new ArrayList<>();

    private SampleApplicationGLView mGlView;


    
    private GestureDetector mGestureDetector;
    
    // The textures we will use for rendering:
    private Vector<Texture> mTextures;

    // Menu option flags
    private boolean mSwitchDatasetAsap = false;
    private boolean mFlash = false;
    private boolean mContAutofocus = true;
    private boolean mDeviceTracker = false;

    private View mFocusOptionView;
    private View mFlashOptionView;
    
    private RelativeLayout mUILayout;
    
    private SampleAppMenu mSampleAppMenu;
    ArrayList<View> mSettingsAdditionalViews = new ArrayList<>();

    private SampleAppMessage mSampleAppMessage;
    private SampleAppTimer mRelocalizationTimer;
    private SampleAppTimer mStatusDelayTimer;

    private int mCurrentStatusInfo;

    final LoadingDialogHandler loadingDialogHandler = new LoadingDialogHandler(this);
    
    // Alert Dialog used to display SDK errors
    private AlertDialog mErrorDialog;
    
    private boolean mIsDroidDevice = false;

    public void setVirtualButtons(){
        mDataSet = null;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        Log.d(LOGTAG, "onCreate");
        super.onCreate(savedInstanceState);
        
        vuforiaAppSession = new SampleApplicationSession(this);
        
        startLoadingAnimation();
        mDatasetStrings.add("MobileAR.xml");
//        mDatasetStrings.add("MobileAR.xml");
//        mDatasetStrings.add("MobileAR.xml");

        vuforiaAppSession
            .initAR(this, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        
        mGestureDetector = new GestureDetector(getApplicationContext(), new GestureListener(this));
        
        // Load any sample specific textures:
        mTextures = new Vector<>();
        loadTextures();

        mIsDroidDevice = android.os.Build.MODEL.toLowerCase().startsWith("droid");

        // Relocalization timer and message
        mSampleAppMessage = new SampleAppMessage(this, mUILayout, mUILayout.findViewById(R.id.topbar_layout), false);
        mRelocalizationTimer = new SampleAppTimer(10000, 1000)
        {
            @Override
            public void onFinish()
            {
                if (vuforiaAppSession != null)
                {
                    vuforiaAppSession.resetDeviceTracker();
                }

                super.onFinish();
            }
        };

        mStatusDelayTimer = new SampleAppTimer(1000, 1000)
        {
            @Override
            public void onFinish()
            {
                if (mRenderer.isTargetCurrentlyTracked())
                {
                    super.onFinish();
                    return;
                }

                if (!mRelocalizationTimer.isRunning())
                {
                    mRelocalizationTimer.startTimer();
                }

                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        mSampleAppMessage.show(getString(R.string.instruct_relocalize));
                    }
                });

                super.onFinish();
            }
        };

//        VirtualButtons virtualButtons;
//        virtualButtons.setVirtualButtons();
//        mRenderer = new VirtualButtonRenderer(virtualButtons, vuforiaAppSession);

        Button b1 = (Button) findViewById(R.id.button1);
        b1.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_MOVE:
                        onHandler1();
                        break;
                    case MotionEvent.ACTION_DOWN:
                        onHandler1();
                        break;
                    case MotionEvent.ACTION_UP:
                        mHandler.removeCallbacks(r);
                        break;
                }
                return true;
            }
        });

        Button b2 = (Button) findViewById(R.id.button2);
        b2.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_MOVE:
                        onHandler2();
                        break;
                    case MotionEvent.ACTION_DOWN:
                        onHandler2();
                        break;
                    case MotionEvent.ACTION_UP:
                        mHandler.removeCallbacks(r);
                        break;
                }
                return true;
            }
        });

        Button b3 = (Button) findViewById(R.id.button3);
        b3.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_MOVE:
                        onHandler3();
                        break;
                    case MotionEvent.ACTION_DOWN:
                        onHandler3();
                        break;
                    case MotionEvent.ACTION_UP:
                        mHandler.removeCallbacks(r);
                        break;
                }
                return true;
            }
        });

        Button b4 = (Button) findViewById(R.id.button4);
        b4.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_MOVE:
                        onHandler4();
                        break;
                    case MotionEvent.ACTION_DOWN:
                        onHandler4();
                        break;
                    case MotionEvent.ACTION_UP:
                        mHandler.removeCallbacks(r);
                        break;
                }
                return true;
            }
        });

        Button b5 = (Button) findViewById(R.id.button5);
        b5.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
//                    case MotionEvent.ACTION_MOVE:
//                        onHandler5();
//                        break;
                    case MotionEvent.ACTION_DOWN:
                        onHandler5();
                        break;
                    case MotionEvent.ACTION_UP:
                        mHandler.removeCallbacks(r);
                        break;
                }
                return true;
            }
        });


    }

    private Handler mHandler;
    private Runnable r;

    private void onHandler1() {
        mHandler = new Handler();
        r = new Runnable() {
            @Override
            public void run() {
                if (ImageTargetRenderer.rot < 360.0f && ImageTargetRenderer.rot >= 180.0f){
                    float diff = (360.0f - ImageTargetRenderer.rot)/seperate;
                    for(int i = 0; i<seperate; i ++){
                        ImageTargetRenderer.rot += diff;
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }else if (0 < ImageTargetRenderer.rot && ImageTargetRenderer.rot < 180.0f) {
                    float diff = (ImageTargetRenderer.rot - 0.0f) / seperate;
                    for (int i = 0; i < seperate; i++) {
                        ImageTargetRenderer.rot -= diff;
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }else{
                    ImageTargetRenderer.rot = 0.0f;
                }

                ImageTargetRenderer.y_move -= 0.002;
                Log.d("pks4", "DOWN");
            }
        };
        mHandler.postDelayed(r, 50);
    }
    private void onHandler2() {
        mHandler = new Handler();
        r = new Runnable() {
            @Override
            public void run() {
                if (ImageTargetRenderer.rot < 180.0f){
                    float diff = (180.0f - ImageTargetRenderer.rot)/seperate;
                    for(int i = 0; i<seperate; i ++){
                        ImageTargetRenderer.rot += diff;
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }else if (ImageTargetRenderer.rot > 180.0f) {
                    float diff = (ImageTargetRenderer.rot - 180.0f) / seperate;
                    for (int i = 0; i < seperate; i++) {
                        ImageTargetRenderer.rot -= diff;
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }else{
                    ImageTargetRenderer.rot = 180.0f;
                }
                ImageTargetRenderer.y_move += 0.002;

                Log.d("pks4", "UP");
            }
        };
        mHandler.postDelayed(r, 50);
    }

    private void onHandler3() {
        mHandler = new Handler();
        r = new Runnable() {
            @Override
            public void run() {
                if (ImageTargetRenderer.rot == 0){
                    float diff = (90.0f - ImageTargetRenderer.rot)/seperate;
                    for(int i = 0; i<seperate; i ++){
                        ImageTargetRenderer.rot += diff;
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }else if (ImageTargetRenderer.rot > 90.0f && ImageTargetRenderer.rot <= 270.0f ) {
                    float diff = (ImageTargetRenderer.rot - 90.0f) / seperate;
                    for (int i = 0; i < seperate; i++) {
                        ImageTargetRenderer.rot -= diff;
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }else{
                    ImageTargetRenderer.rot = 90.0f;
                }

                ImageTargetRenderer.x_move += 0.002;
                Log.d("pks4", "RIGHT");
            }
        };
        mHandler.postDelayed(r, 50);
    }

    private void onHandler4() {
        mHandler = new Handler();
        r = new Runnable() {
            @Override
            public void run() {
                if (ImageTargetRenderer.rot >= 90.0f && ImageTargetRenderer.rot < 270.0f){
                    float diff = (270.0f - ImageTargetRenderer.rot)/seperate;
                    for(int i = 0; i<seperate; i ++){
                        ImageTargetRenderer.rot += diff;
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }else if(ImageTargetRenderer.rot == 0.0f){
                    float diff = (360.0f - 270.0f) / seperate;
                    ImageTargetRenderer.rot = 360.0f;
                    for (int i = 0; i < seperate; i++) {

                        ImageTargetRenderer.rot -= diff;
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                else{
                    ImageTargetRenderer.rot = 270.0f;
                }

                ImageTargetRenderer.x_move -= 0.002;
                Log.d("pks4", "LEFT");

            }
        };
        mHandler.postDelayed(r, 50);
    }

    private void onHandler5() {
        mHandler = new Handler();
        r = new Runnable() {
            @Override
            public void run() {
                ImageTargetRenderer.z_move = 0.001f;
                for(int i = 0; i<60; i ++) {
                    ImageTargetRenderer.z_move += 0.001f;
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                for(int i = 0; i<60; i ++) {
                    ImageTargetRenderer.z_move -= 0.001f;
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                ImageTargetRenderer.z_move = 0.001f;
                Log.d("pks4", "JUMP");

            }
        };
        mHandler.postDelayed(r, 50);
    }

    private class GestureListener extends
        GestureDetector.SimpleOnGestureListener
    {
        // Used to set autofocus one second after a manual focus is triggered
        private final Handler autofocusHandler = new Handler();

        private WeakReference<ImageTargets> activityRef;
        

        private GestureListener(ImageTargets activity)
        {
            activityRef = new WeakReference<>(activity);
        }

        @Override
        public boolean onDown(MotionEvent e)
        {
            return true;
        }


        // Process Single Tap event to trigger autofocus
        @Override
        public boolean onSingleTapUp(MotionEvent e)
        {
            boolean result = CameraDevice.getInstance().setFocusMode(
                    CameraDevice.FOCUS_MODE.FOCUS_MODE_TRIGGERAUTO);
            if (!result)
                Log.e("SingleTapUp", "Unable to trigger focus");

            // Generates a Handler to trigger continuous auto-focus
            // after 1 second
            autofocusHandler.postDelayed(new Runnable()
            {
                public void run()
                {
                    if (activityRef.get().mContAutofocus)
                    {
                        final boolean autofocusResult = CameraDevice.getInstance().setFocusMode(
                                CameraDevice.FOCUS_MODE.FOCUS_MODE_CONTINUOUSAUTO);

                        if (!autofocusResult)
                            Log.e("SingleTapUp", "Unable to re-enable continuous auto-focus");
                    }
                }
            }, 1000L);

            return true;
        }
    }


    // Load specific textures from the APK, which we will later use for rendering.
    private void loadTextures()
    {
//        mTextures.add(Texture.loadTextureFromApk("TextureTeapotBrass.png",
//            getAssets()));
//        mTextures.add(Texture.loadTextureFromApk("TextureTeapotBlue.png",
//            getAssets()));
//        mTextures.add(Texture.loadTextureFromApk("TextureTeapotRed.png",
//            getAssets()));
//        mTextures.add(Texture.loadTextureFromApk("ImageTargets/Buildings.png",
//            getAssets()));
//        mTextures.add(Texture.loadTextureFromApk("ImageTargets/box.jpg",
//                getAssets()));
        mTextures.add(Texture.loadTextureFromApk("ImageTargets/cat_texture.jpg",
                getAssets()));
        mTextures.add(Texture.loadTextureFromApk("ImageTargets/apple_texture.png",
                getAssets()));

    }
    

    @Override
    protected void onResume()
    {
        Log.d(LOGTAG, "onResume");
        super.onResume();

        showProgressIndicator(true);
        
        // This is needed for some Droid devices to force portrait
        if (mIsDroidDevice)
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        vuforiaAppSession.onResume();
    }


    // Called whenever the device orientation or screen resolution changes
    @Override
    public void onConfigurationChanged(Configuration config)
    {
        Log.d(LOGTAG, "onConfigurationChanged");
        super.onConfigurationChanged(config);
        
        vuforiaAppSession.onConfigurationChanged();
    }


    @Override
    protected void onPause()
    {
        Log.d(LOGTAG, "onPause");
        super.onPause();
        
        if (mGlView != null)
        {
            mGlView.setVisibility(View.INVISIBLE);
            mGlView.onPause();
        }
        
        // Turn off the flash
        if (mFlashOptionView != null && mFlash)
        {
            // OnCheckedChangeListener is called upon changing the checked state
            setMenuToggle(mFlashOptionView, false);
        }
        
        vuforiaAppSession.onPause();
    }
    

    @Override
    protected void onDestroy()
    {
        Log.d(LOGTAG, "onDestroy");
        super.onDestroy();
        
        try
        {
            vuforiaAppSession.stopAR();
        } catch (SampleApplicationException e)
        {
            Log.e(LOGTAG, e.getString());
        }
        
        // Unload texture:
        mTextures.clear();
        mTextures = null;
        
        System.gc();
    }
    

    private void initApplicationAR()
    {
        // Create OpenGL ES view:
        int depthSize = 16;
        int stencilSize = 0;
        boolean translucent = Vuforia.requiresAlpha();
        
        mGlView = new SampleApplicationGLView(getApplicationContext());
        mGlView.init(translucent, depthSize, stencilSize);

        mRenderer = new ImageTargetRenderer(this, vuforiaAppSession);
        mRenderer.setTextures(mTextures);
        mGlView.setRenderer(mRenderer);
        mGlView.setPreserveEGLContextOnPause(true);

        setRendererReference(mRenderer);
    }
    
    
    private void startLoadingAnimation()
    {
        mUILayout = (RelativeLayout) View.inflate(getApplicationContext(), R.layout.camera_overlay, null);
        
        mUILayout.setVisibility(View.VISIBLE);
        mUILayout.setBackgroundColor(Color.BLACK);

        RelativeLayout topbarLayout = mUILayout.findViewById(R.id.topbar_layout);
        topbarLayout.setVisibility(View.VISIBLE);

        TextView title = mUILayout.findViewById(R.id.topbar_title);
        title.setText(getText(R.string.feature_image_targets));

        mSettingsAdditionalViews.add(topbarLayout);
        
        // Gets a reference to the loading dialog
        loadingDialogHandler.mLoadingDialogContainer = mUILayout
            .findViewById(R.id.loading_indicator);
        
        // Shows the loading indicator at start
        loadingDialogHandler
            .sendEmptyMessage(LoadingDialogHandler.SHOW_LOADING_DIALOG);
        
        // Adds the inflated layout to the view
        addContentView(mUILayout, new LayoutParams(LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT));
        
    }
    

    @Override
    public boolean doLoadTrackersData()
    {
        TrackerManager tManager = TrackerManager.getInstance();
        ObjectTracker objectTracker = (ObjectTracker) tManager
            .getTracker(ObjectTracker.getClassType());
        if (objectTracker == null)
        {
            Log.d(
                    LOGTAG,
                    "Failed to load tracking data set because the ObjectTracker has not been initialized.");
            return false;
        }

        // Create the data set:
        mDataSet = objectTracker.createDataSet();
        if (mDataSet == null)
        {
            Log.d("pkspks", "null");
            Log.d(LOGTAG, "Failed to create a new tracking data.");
            return false;
        }

        // Load the data set:
        if (!mDataSet.load("VirtualButtons/Wood.xml",
                STORAGE_TYPE.STORAGE_APPRESOURCE))
        {
            Log.d("pkspks", "!load");
            Log.d(LOGTAG, "Failed to load data set.");
            return false;
        }

        // Activate the data set:
        if (!objectTracker.activateDataSet(mDataSet))
        {
            Log.d("pkspks", "!active");
            Log.d(LOGTAG, "Failed to activate data set.");
            return false;
        }

        Log.d("pkspks", "Successfully loaded and activated data set.");

        if (mCurrentDataset == null)
        {
            Log.d("pkspks", "null2");
            mCurrentDataset = objectTracker.createDataSet();
        }
        
        if (mCurrentDataset == null)
        {
            Log.d("pkspks", "null3");
            return false;
        }
        
        if (!mCurrentDataset.load(
            mDatasetStrings.get(mCurrentDatasetSelectionIndex),
            STORAGE_TYPE.STORAGE_APPRESOURCE))
        {
            Log.d("pkspks", "!load2");
            return false;
        }
        
        if (!objectTracker.activateDataSet(mCurrentDataset))
        {
            Log.d("pkspks", "!load3");
            return false;
        }
        
        TrackableList trackableList = mCurrentDataset.getTrackables();
        for (Trackable trackable : trackableList)
        {
            String name = "Current Dataset : " + trackable.getName();
            trackable.setUserData(name);
            Log.d("pkspks", "UserData:Set the following user data "
                + trackable.getUserData());
        }
        
        return true;
    }
    
    
    @Override
    public boolean doUnloadTrackersData()
    {
        // Indicate if the trackers were unloaded correctly
        boolean result = true;
        
        TrackerManager tManager = TrackerManager.getInstance();
        ObjectTracker objectTracker = (ObjectTracker) tManager
            .getTracker(ObjectTracker.getClassType());

        if (objectTracker == null)
        {
            Log.d(
                    LOGTAG,
                    "Failed to destroy the tracking data set because the ObjectTracker has not been initialized.");
            return false;
        }

        if (mDataSet != null)
        {
            if (!objectTracker.deactivateDataSet(mDataSet))
            {
                Log.d(
                        LOGTAG,
                        "Failed to destroy the tracking data set because the data set could not be deactivated.");
                result = false;
            } else if (!objectTracker.destroyDataSet(mDataSet))
            {
                Log.d(LOGTAG, "Failed to destroy the tracking data set.");
                result = false;
            }

            if (result)
                Log.d(LOGTAG, "Successfully destroyed the data set.");

            mDataSet = null;
        }

        
        if (mCurrentDataset != null && mCurrentDataset.isActive())
        {
            if (objectTracker.getActiveDataSets().at(0).equals(mCurrentDataset)
                && !objectTracker.deactivateDataSet(mCurrentDataset))
            {
                result = false;
            }
            else if (!objectTracker.destroyDataSet(mCurrentDataset))
            {
                result = false;
            }
            
            mCurrentDataset = null;
        }
        
        return result;
    }


    @Override
    public void onVuforiaResumed()
    {
        if (mGlView != null)
        {
            mGlView.setVisibility(View.VISIBLE);
            mGlView.onResume();
        }
    }


    @Override
    public void onVuforiaStarted()
    {
        mRenderer.updateRenderingPrimitives();

        if (mContAutofocus)
        {
            if(!CameraDevice.getInstance().setFocusMode(CameraDevice.FOCUS_MODE.FOCUS_MODE_CONTINUOUSAUTO))
            {
                // If continuous autofocus mode fails, attempt to set to a different mode
                if(!CameraDevice.getInstance().setFocusMode(CameraDevice.FOCUS_MODE.FOCUS_MODE_TRIGGERAUTO))
                {
                    CameraDevice.getInstance().setFocusMode(CameraDevice.FOCUS_MODE.FOCUS_MODE_NORMAL);
                }

                setMenuToggle(mFocusOptionView, false);
            }
            else
            {
                setMenuToggle(mFocusOptionView, true);
            }
        }
        else
        {
            setMenuToggle(mFocusOptionView, false);
        }

        showProgressIndicator(false);
    }


    private void showProgressIndicator(boolean show)
    {
        if (show)
        {
            loadingDialogHandler.sendEmptyMessage(LoadingDialogHandler.SHOW_LOADING_DIALOG);
        }
        else
        {
            loadingDialogHandler.sendEmptyMessage(LoadingDialogHandler.HIDE_LOADING_DIALOG);
        }
    }


    // Called once Vuforia has been initialized or
    // an error has caused Vuforia initialization to stop
    @Override
    public void onInitARDone(SampleApplicationException exception)
    {
        if (exception == null)
        {
            initApplicationAR();
            
            mRenderer.setActive(true);
            
            // Now add the GL surface view. It is important
            // that the OpenGL ES surface view gets added
            // BEFORE the camera is started and video
            // background is configured.
            addContentView(mGlView, new LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT));
            
            // Sets the UILayout to be drawn in front of the camera
            mUILayout.bringToFront();

            mUILayout.setBackgroundColor(Color.TRANSPARENT);

            mSampleAppMenu = new SampleAppMenu(this, this, "Image Targets",
                    mGlView, mUILayout, mSettingsAdditionalViews);
            setSampleAppMenuSettings();

            vuforiaAppSession.startAR();
        }
        else
        {
            Log.e(LOGTAG, exception.getString());
            showInitializationErrorMessage(exception.getString());
        }
    }
    

    private void showInitializationErrorMessage(String message)
    {
        final String errorMessage = message;
        runOnUiThread(new Runnable()
        {
            public void run()
            {
                if (mErrorDialog != null)
                {
                    mErrorDialog.dismiss();
                }

                // Generates an Alert Dialog to show the error message
                AlertDialog.Builder builder = new AlertDialog.Builder(
                        ImageTargets.this);
                builder
                    .setMessage(errorMessage)
                    .setTitle(getString(R.string.INIT_ERROR))
                    .setCancelable(false)
                    .setIcon(0)
                    .setPositiveButton(getString(R.string.button_OK),
                        new DialogInterface.OnClickListener()
                        {
                            public void onClick(DialogInterface dialog, int id)
                            {
                                finish();
                            }
                        });
                
                mErrorDialog = builder.create();
                mErrorDialog.show();
            }
        });
    }


    // Called every frame
    @Override
    public void onVuforiaUpdate(State state)
    {
        if (updateBtns)
        {
            Log.d("pks7", "hihi");

            // Update() runs in the tracking thread, therefore it is guaranteed
            // that the tracker is not doing anything at this point.
            // Hence reconfiguration is possible.

            ObjectTracker ot = (ObjectTracker) (TrackerManager.getInstance()
                    .getTracker(ObjectTracker.getClassType()));

            // Deactivate the data set prior to reconfiguration:
            ot.deactivateDataSet(mDataSet);
            Log.d("pks7", "hihi2");

            if (mDataSet.getTrackables().size() <= 0)
            {
                Log.d("pks7", "hihi2.11");
                Log.e(LOGTAG, "Could not deactivate dataset!");
                return;
            }
            Log.d("pks7", "hihi2.1");
            Trackable trackable = mDataSet.getTrackables().at(0);
            Log.d("pks7", "hihi2.2");
            ImageTarget imageTarget = (ImageTarget) (trackable);
            Log.d("pks7", "hihi3");

            // Check to see if any button has been enabled/disabled from the menu
            if ((buttonMask & BUTTON_1) != 0)
            {
                Log.d(LOGTAG, "Toggle Button 1");

                if (toggleVirtualButton(imageTarget, virtualButtonColors[0],
                        -0.10868f, -0.05352f, -0.07575f, -0.06587f))
                {
                    Log.d(LOGTAG, "Successfully toggled Button 1");
                }
                else
                {
                    Log.e(LOGTAG, "Failed to toggle Button 1");
                }

            }
            if ((buttonMask & BUTTON_2) != 0)
            {
                Log.d(LOGTAG, "Toggle Button 2");

                if (toggleVirtualButton(imageTarget, virtualButtonColors[1],
                        -0.04528f, -0.05352f, -0.01235f, -0.06587f))
                {
                    Log.d(LOGTAG, "Successfully toggled Button 2");
                }
                else
                {
                    Log.e(LOGTAG, "Failed to toggle Button 2");
                }
            }
            if ((buttonMask & BUTTON_3) != 0)
            {
                Log.d(LOGTAG, "Toggle Button 3");

                if (toggleVirtualButton(imageTarget, virtualButtonColors[2],
                        0.01482f, -0.05352f, 0.04775f, -0.06587f))
                {
                    Log.d(LOGTAG, "Successfully toggled Button 3");
                }
                else
                {
                    Log.e(LOGTAG, "Failed to toggle Button 3");
                }
            }
            if ((buttonMask & BUTTON_4) != 0)
            {
                Log.d(LOGTAG, "Toggle Button 4");

                if (toggleVirtualButton(imageTarget, virtualButtonColors[3],
                        0.07657f, -0.05352f, 0.10950f, -0.06587f))
                {
                    Log.d(LOGTAG, "Successfully toggled Button 4");
                }
                else
                {
                    Log.e(LOGTAG, "Failed to toggle Button 4");
                }
            }

            // Reactivate the data set:
            ot.activateDataSet(mDataSet);

            buttonMask = 0;
            updateBtns = false;
        }
    }


    // Create/destroy a Virtual Button at runtime
    //
    // NOTE: This will NOT work if the tracker is active!
    private boolean toggleVirtualButton(ImageTarget imageTarget, String name,
                                        float left, float top, float right, float bottom)
    {
        Log.d(LOGTAG, "toggleVirtualButton");

        boolean buttonToggleSuccess = false;

        VirtualButton virtualButton = imageTarget.getVirtualButton(name);

        if (virtualButton != null)
        {
            Log.d(LOGTAG, "Destroying Virtual Button> " + name);
            buttonToggleSuccess = imageTarget
                    .destroyVirtualButton(virtualButton);
        }
        else
        {
            Log.d(LOGTAG, "Creating Virtual Button> " + name);
            Rectangle vbRectangle = new Rectangle(left, top, right, bottom);
            VirtualButton virtualButton2 = imageTarget.createVirtualButton(
                    name, vbRectangle);

            if (virtualButton2 != null)
            {
                // This is just a showcase. The values used here a set by
                // default on Virtual Button creation
                virtualButton2.setEnabled(true);
                virtualButton2.setSensitivity(VirtualButton.SENSITIVITY.MEDIUM);
                buttonToggleSuccess = true;
            }
        }

        return buttonToggleSuccess;
    }

    // Toggles the enabled state of the Virtual Button
    private void addButtonToToggle(int virtualButtonIdx)
    {
        Log.d(LOGTAG, "addButtonToToggle");

        if (!(virtualButtonIdx >= 0 && virtualButtonIdx < NUM_BUTTONS))
        {
            Log.e(LOGTAG, "Could not add button");
            return;
        }

        switch (virtualButtonIdx)
        {
            case 0:
                buttonMask |= BUTTON_1;
                Log.d("pks", "hihi");
                break;

            case 1:
                buttonMask |= BUTTON_2;
                break;

            case 2:
                buttonMask |= BUTTON_3;
                break;

            case 3:
                buttonMask |= BUTTON_4;
                break;
        }

        updateBtns = true;
        Log.d("pks", "byebye");
    }

    @Override
    public boolean doInitTrackers()
    {
        // Indicate if the trackers were initialized correctly
        boolean result = true;

        TrackerManager tManager = TrackerManager.getInstance();

        Tracker tracker = tManager.initTracker(ObjectTracker.getClassType());
        if (tracker == null)
        {
            Log.e(
                LOGTAG,
                "Tracker not initialized. Tracker already initialized or the camera is already started");
            result = false;
        } else
        {
            Log.i(LOGTAG, "Tracker successfully initialized");
        }

        // Initialize the Positional Device Tracker
        DeviceTracker deviceTracker = (PositionalDeviceTracker)
                tManager.initTracker(PositionalDeviceTracker.getClassType());

        if (deviceTracker != null)
        {
            Log.i(LOGTAG, "Successfully initialized Device Tracker");
        }
        else
        {
            Log.e(LOGTAG, "Failed to initialize Device Tracker");
        }

        return result;
    }
    
    
    @Override
    public boolean doStartTrackers()
    {
        // Indicate if the trackers were started correctly
        boolean result = true;

        TrackerManager trackerManager = TrackerManager.getInstance();

        Tracker objectTracker = trackerManager.getTracker(ObjectTracker.getClassType());

        if (objectTracker != null && objectTracker.start())
        {
            Log.i(LOGTAG, "Successfully started Object Tracker");
        }
        else
        {
            Log.e(LOGTAG, "Failed to start Object Tracker");
            result = false;
        }

        if (isDeviceTrackingActive())
        {
            PositionalDeviceTracker deviceTracker = (PositionalDeviceTracker) trackerManager
                    .getTracker(PositionalDeviceTracker.getClassType());

            if (deviceTracker != null && deviceTracker.start())
            {
                Log.i(LOGTAG, "Successfully started Device Tracker");
            }
            else
            {
                Log.e(LOGTAG, "Failed to start Device Tracker");
            }
        }
        
        return result;
    }
    
    
    @Override
    public boolean doStopTrackers()
    {
        // Indicate if the trackers were stopped correctly
        boolean result = true;

        TrackerManager trackerManager = TrackerManager.getInstance();

        Tracker objectTracker = trackerManager.getTracker(ObjectTracker.getClassType());
        if (objectTracker != null)
        {
            objectTracker.stop();
            Log.i(LOGTAG, "Successfully stopped object tracker");
        }
        else
        {
            Log.e(LOGTAG, "Failed to stop object tracker");
            result = false;
        }

        // Stop the device tracker
        if(isDeviceTrackingActive())
        {

            Tracker deviceTracker = trackerManager.getTracker(PositionalDeviceTracker.getClassType());

            if (deviceTracker != null)
            {
                deviceTracker.stop();
                Log.i(LOGTAG, "Successfully stopped device tracker");
            }
            else
            {
                Log.e(LOGTAG, "Could not stop device tracker");
            }
        }

        return result;
    }
    
    
    @Override
    public boolean doDeinitTrackers()
    {
        TrackerManager tManager = TrackerManager.getInstance();

        // Indicate if the trackers were deinitialized correctly
        boolean result = tManager.deinitTracker(ObjectTracker.getClassType());
        tManager.deinitTracker(PositionalDeviceTracker.getClassType());
        
        return result;
    }
    
    
    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        // Process the Gestures
        return ((mSampleAppMenu != null && mSampleAppMenu.processEvent(event))
                || mGestureDetector.onTouchEvent(event));
    }
    
    
    boolean isDeviceTrackingActive()
    {
        return mDeviceTracker;
    }

    // Menu options
    private final static int CMD_BACK = -1;
    private final static int CMD_DEVICE_TRACKING = 1;
    private final static int CMD_AUTOFOCUS = 2;
    private final static int CMD_FLASH = 3;
    private final static int CMD_DATASET_START_INDEX = 4;

    // Menu options
    private final static int CMD_BUTTON_RED = 10;
    private final static int CMD_BUTTON_BLUE = 11;
    private final static int CMD_BUTTON_YELLOW = 12;
    private final static int CMD_BUTTON_GREEN = 13;


    private void setSampleAppMenuSettings()
    {
        SampleAppMenuGroup group;

//        group = mSampleAppMenu.addGroup("", false);
//        group.addTextItem(getString(R.string.menu_back), -1);

//        group = mSampleAppMenu.addGroup("", true);
//        group.addSelectionItem(getString(R.string.menu_device_tracker),
//                CMD_DEVICE_TRACKING, false);

        group = mSampleAppMenu.addGroup(getString(R.string.menu_camera), true);
        mFocusOptionView = group.addSelectionItem(getString(R.string.menu_contAutofocus),
            CMD_AUTOFOCUS, mContAutofocus);
        mFlashOptionView = group.addSelectionItem(
            getString(R.string.menu_flash), CMD_FLASH, false);

        group = mSampleAppMenu.addGroup(getString(R.string.menu_virtual_buttons), true);
        group.addSelectionItem(getString(R.string.menu_button_red),
                CMD_BUTTON_RED, true);
        group.addSelectionItem(getString(R.string.menu_button_blue),
                CMD_BUTTON_BLUE, true);
        group.addSelectionItem(getString(R.string.menu_button_yellow),
                CMD_BUTTON_YELLOW, true);
        group.addSelectionItem(getString(R.string.menu_button_green),
                CMD_BUTTON_GREEN, true);

//        group = mSampleAppMenu
//            .addGroup(getString(R.string.menu_datasets), true);
//        mStartDatasetsIndex = CMD_DATASET_START_INDEX;
//        mDatasetsNumber = mDatasetStrings.size();
        
//        group.addRadioItem("Stones & Chips", mStartDatasetsIndex, true);
//        group.addRadioItem("Tarmac", mStartDatasetsIndex + 1, false);
//        group.addRadioItem("My Patterns", mStartDatasetsIndex + 2, false); // 6

//        group = mSampleAppMenu.addGroup("OBJECTS", true);
//        group.addRadioItem("Teapot", 7, true);
//        group.addRadioItem("Cube", 8, false);
//        group.addRadioItem("MyObj", 9, false);

        mSampleAppMenu.attachMenu();
    }


    private void setMenuToggle(View view, boolean value)
    {
        // OnCheckedChangeListener is called upon changing the checked state
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
        {
            ((Switch) view).setChecked(value);
        } else
        {
            ((CheckBox) view).setChecked(value);
        }
    }


    // In this function you can define the desired behavior for each menu option
    // Each case corresponds to a menu option
    @Override
    public boolean menuProcess(int command)
    {
        boolean result = true;

        switch (command)
        {
            case CMD_BACK:
                finish();
                break;

            case CMD_BUTTON_RED:
                addButtonToToggle(0);
                break;

            case CMD_BUTTON_BLUE:
                addButtonToToggle(1);
                break;

            case CMD_BUTTON_YELLOW:
                addButtonToToggle(2);
                break;

            case CMD_BUTTON_GREEN:
                addButtonToToggle(3);
                break;

            case CMD_FLASH:
                result = CameraDevice.getInstance().setFlashTorchMode(!mFlash);

                if (result)
                {
                    mFlash = !mFlash;
                } else
                {
                    showToast(getString(mFlash ? R.string.menu_flash_error_off
                        : R.string.menu_flash_error_on));
                    Log.e(LOGTAG,
                        getString(mFlash ? R.string.menu_flash_error_off
                            : R.string.menu_flash_error_on));
                }
                break;

            case CMD_AUTOFOCUS:

                if (mContAutofocus)
                {
                    result = CameraDevice.getInstance().setFocusMode(
                        CameraDevice.FOCUS_MODE.FOCUS_MODE_NORMAL);

                    if (result)
                    {
                        mContAutofocus = false;
                    } else
                    {
                        showToast(getString(R.string.menu_contAutofocus_error_off));
                        Log.e(LOGTAG,
                            getString(R.string.menu_contAutofocus_error_off));
                    }
                } else
                {
                    result = CameraDevice.getInstance().setFocusMode(
                        CameraDevice.FOCUS_MODE.FOCUS_MODE_CONTINUOUSAUTO);

                    if (result)
                    {
                        mContAutofocus = true;
                    } else
                    {
                        showToast(getString(R.string.menu_contAutofocus_error_on));
                        Log.e(LOGTAG,
                            getString(R.string.menu_contAutofocus_error_on));
                    }
                }

                break;

            case CMD_DEVICE_TRACKING:

                result = toggleDeviceTracker();

                break;

//            case 7:
//                ImageTargetRenderer.objectSel = 0;
//                break;
//            case 8:
//                ImageTargetRenderer.objectSel = 1;
//                break;
//            case 9: // cat
//                ImageTargetRenderer.objectSel = 2;
//                break;
            default: //6
//                ImageTargetRenderer.objectSel = 2;
                if (command >= mStartDatasetsIndex // 4보다 크고
                    && command < mStartDatasetsIndex + mDatasetsNumber) // 4+3 보다 작으면
                {
                    mSwitchDatasetAsap = true;
                    mCurrentDatasetSelectionIndex = command
                        - mStartDatasetsIndex;
                }
                ImageTargetRenderer.objectSel = 2;
                break;
        }
        
        return result;
    }


    private boolean toggleDeviceTracker()
    {
        boolean result = true;
        TrackerManager trackerManager = TrackerManager.getInstance();
        PositionalDeviceTracker deviceTracker = (PositionalDeviceTracker)
                trackerManager.getTracker(PositionalDeviceTracker.getClassType());

        if (deviceTracker != null)
        {
            if (!mDeviceTracker)
            {
                if (!deviceTracker.start())
                {
                    Log.e(LOGTAG,"Failed to start device tracker");
                    result = false;
                }
                else
                {
                    Log.d(LOGTAG,"Successfully started device tracker");
                }
            }
            else
            {
                deviceTracker.stop();
                Log.d(LOGTAG, "Successfully stopped device tracker");

                clearSampleAppMessage();
            }
        }
        else
        {
            Log.e(LOGTAG, "Device tracker is null!");
            result = false;
        }

        if (result)
        {
            mDeviceTracker = !mDeviceTracker;
        }
        else
        {
            clearSampleAppMessage();
        }

        return result;
    }


    public void checkForRelocalization(final int statusInfo)
    {
        if (mCurrentStatusInfo == statusInfo)
        {
            return;
        }

        mCurrentStatusInfo = statusInfo;

        if (mCurrentStatusInfo == TrackableResult.STATUS_INFO.RELOCALIZING)
        {
            // If the status is RELOCALIZING, start the timer
            if (!mStatusDelayTimer.isRunning())
            {
                mStatusDelayTimer.startTimer();
            }
        }
        else
        {
            // If the status is not RELOCALIZING, stop the timers and hide the message
            if (mStatusDelayTimer.isRunning())
            {
                mStatusDelayTimer.stopTimer();
            }

            if (mRelocalizationTimer.isRunning())
            {
                mRelocalizationTimer.stopTimer();
            }

            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    if (mSampleAppMessage != null)
                    {
                        mSampleAppMessage.hide();
                    }
                }
            });
        }
    }


    private void clearSampleAppMessage()
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                if (mSampleAppMessage != null)
                {
                    mSampleAppMessage.hide();
                }
            }
        });
    }
    
    
    private void showToast(String text)
    {
        Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
    }
}
