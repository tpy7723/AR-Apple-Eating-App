/*===============================================================================
Copyright (c) 2019 PTC Inc. All Rights Reserved.

Copyright (c) 2012-2014 Qualcomm Connected Experiences, Inc. All Rights Reserved.

Vuforia is a trademark of PTC Inc., registered in the United States and other 
countries.
===============================================================================*/

package com.vuforia.engine.CoreSamples.app.ImageTargets;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.Vector;

import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import com.vuforia.Device;
import com.vuforia.DeviceTrackableResult;
import com.vuforia.ImageTargetResult;
import com.vuforia.Matrix44F;
import com.vuforia.Rectangle;
import com.vuforia.Renderer;
import com.vuforia.State;
import com.vuforia.Tool;
import com.vuforia.Trackable;
import com.vuforia.TrackableResult;
import com.vuforia.TrackableResultList;
import com.vuforia.VirtualButton;
import com.vuforia.VirtualButtonResult;
import com.vuforia.VirtualButtonResultList;
import com.vuforia.Vuforia;
import com.vuforia.engine.CoreSamples.R;
import com.vuforia.engine.CoreSamples.app.ImageTargets.OBJParser.ObjParser;
import com.vuforia.engine.SampleApplication.SampleAppRenderer;
import com.vuforia.engine.SampleApplication.SampleAppRendererControl;
import com.vuforia.engine.SampleApplication.SampleRendererBase;
import com.vuforia.engine.SampleApplication.SampleApplicationSession;
import com.vuforia.engine.SampleApplication.utils.CubeObject;
import com.vuforia.engine.SampleApplication.utils.CubeShaders;
import com.vuforia.engine.SampleApplication.utils.LineShaders;
import com.vuforia.engine.SampleApplication.utils.LoadingDialogHandler;
import com.vuforia.engine.SampleApplication.utils.MeshObject;
import com.vuforia.engine.SampleApplication.utils.SampleApplication3DModel;
import com.vuforia.engine.SampleApplication.utils.SampleMath;
import com.vuforia.engine.SampleApplication.utils.SampleUtils;
import com.vuforia.engine.SampleApplication.utils.Teapot;
import com.vuforia.engine.SampleApplication.utils.Texture;

import static com.vuforia.HINT.HINT_MAX_SIMULTANEOUS_IMAGE_TARGETS;


/**
 * The renderer class for the Image Targets sample.
 *
 * In the renderFrame() function you can render augmentations to display over the Target
 */
public class ImageTargetRenderer extends SampleRendererBase implements SampleAppRendererControl
{
    private static final String LOGTAG = "ImageTargetRenderer";
    public static float rot = 0.0f;
    private float angle = 0.0f;

    private Random random = new Random();
    private double randomValue = Math.random();
    private Boolean dogposition_z_bool = true;

    public float dogposition_x = (float) (randomValue * 0.2f) -0.1f;
    public float dogposition_y = (float) (randomValue * 0.2f) -0.1f;
    public float dogposition_z = 0.06f;

    public boolean flag_dog = true;
    // These values are the same as those in Wood.xml
    static private final float[] RED_VB_BUTTON =  {-0.10868f, -0.05352f, -0.07575f, -0.06587f};
    static private final float[] BLUE_VB_BUTTON =  {-0.04528f, -0.05352f, -0.01235f, -0.06587f};
    static private final float[] YELLOW_VB_BUTTON =  {0.01482f, -0.05352f, 0.04775f, -0.06587f};
    static private final float[] GREEN_VB_BUTTON =  {0.07657f, -0.05352f, 0.10950f, -0.06587f};

    // OpenGL ES 2.0 specific (Virtual Buttons):
    private int vbShaderProgramID = 0;
    private int vbVertexHandle = 0;

    private int NUM = 30;
    private ArrayList[] verticeBuffers = new ArrayList[NUM];
    private ArrayList[] textureBuffers = new ArrayList[NUM];

    private static final float kTeapotScale = 0.003f;


    private int lineOpacityHandle = 0;
    private int lineColorHandle = 0;
    private int mvpMatrixButtonsHandle = 0;

    private final ImageTargets mActivity;

    private final WeakReference<ImageTargets> mActivityRef;
    
    private int shaderProgramID;
    private int vertexHandle;
    private int textureCoordHandle;
    private int mvpMatrixHandle;
    private int texSampler2DHandle;

    // Object to be rendered
    private Teapot mTeapot;
    private CubeObject mCube;
    private OBJLoader mObj,mObj2; // cat , dog
    public static int objectSel = 2;

    public static float x_move = -0.001f;
    public static float y_move = -0.001f;
    public static float z_move = -0.001f;

    private static final float BUILDING_SCALE = 0.012f;
    private SampleApplication3DModel mBuildingsModel;

    private boolean mModelIsLoaded = false;
    private boolean mIsTargetCurrentlyTracked = false;
    
    private static final float OBJECT_SCALE_FLOAT = 0.003f;
    
    ImageTargetRenderer(ImageTargets activity, SampleApplicationSession session)
    {
        mActivity = activity;
        mActivityRef = new WeakReference<>(activity);
        vuforiaAppSession = session;

        // SampleAppRenderer used to encapsulate the use of RenderingPrimitives setting
        // the device mode AR/VR and stereo mode
        mSampleAppRenderer = new SampleAppRenderer(this, mActivityRef.get(), Device.MODE.MODE_AR, vuforiaAppSession.getVideoMode(),
                false, 0.01f , 5f);
    }


    public void updateRenderingPrimitives()
    {
        mSampleAppRenderer.updateRenderingPrimitives();
    }


    public void setActive(boolean active)
    {
        mSampleAppRenderer.setActive(active);
    }


    // The render function.
    // This function is called from the SampleAppRenderer by using the RenderingPrimitives views.
    // The state is owned by SampleAppRenderer which is controlling its lifecycle.
    // NOTE: State should not be cached outside this method.
    public void renderFrame(State state, float[] projectionMatrix)
    {


        // Renders video background replacing Renderer.DrawVideoBackground()
        mSampleAppRenderer.renderVideoBackground(state);

        // Set the device pose matrix as identity
        Matrix44F devicePoseMatrix = SampleMath.Matrix44FIdentity();
        Matrix44F modelMatrix;

        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        GLES20.glEnable(GLES20.GL_CULL_FACE);
        GLES20.glCullFace(GLES20.GL_BACK);
        GLES20.glFrontFace(GLES20.GL_CCW);   // Back camera

        // Read device pose from the state and create a corresponding view matrix (inverse of the device pose)
        if (state.getDeviceTrackableResult() != null)
        {
            int statusInfo = state.getDeviceTrackableResult().getStatusInfo();
            int trackerStatus = state.getDeviceTrackableResult().getStatus();

            mActivityRef.get().checkForRelocalization(statusInfo);

            if (trackerStatus != TrackableResult.STATUS.NO_POSE)
            {
                modelMatrix = Tool.convertPose2GLMatrix(state.getDeviceTrackableResult().getPose());

                // We transpose here because Matrix44FInverse returns a transposed matrix
                devicePoseMatrix = SampleMath.Matrix44FTranspose(SampleMath.Matrix44FInverse(modelMatrix));
            }
        }

        TrackableResultList trackableResultList = state.getTrackableResults();

        // Determine if target is currently being tracked
        setIsTargetCurrentlyTracked(trackableResultList);

        // Iterate through trackable results and render any augmentations
        for (TrackableResult result : trackableResultList)
        {
            Trackable trackable = result.getTrackable();

            if (result.isOfType(ImageTargetResult.getClassType()))
            {
                int textureIndex;
                modelMatrix = Tool.convertPose2GLMatrix(result.getPose());

//                textureIndex = trackable.getName().equalsIgnoreCase("stones") ? 0
//                    : 1;
//                textureIndex = trackable.getName().equalsIgnoreCase("tarmac") ? 2
//                    : textureIndex;
//
//                textureIndex = mActivityRef.get().isDeviceTrackingActive() ? 3 : textureIndex;
                textureIndex = 0; // 항상 0


                renderModel(projectionMatrix, devicePoseMatrix.getData(), modelMatrix.getData(), textureIndex); // cat
                SampleUtils.checkGLError("Image Targets renderFrame");
            }
        }

        GLES20.glDisable(GLES20.GL_DEPTH_TEST);

        // Renders video background replacing Renderer.DrawVideoBackground()

//        mSampleAppRenderer.renderVideoBackground(state);
//
//        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
//        GLES20.glEnable(GLES20.GL_CULL_FACE);
//        GLES20.glCullFace(GLES20.GL_BACK);
//
//        // Did we find any trackables this frame?
//        if (!state.getTrackableResults().empty())
//        {
//            // Get the trackable:
//            TrackableResult trackableResult = state.getTrackableResults().at(0);
//            float[] modelViewMatrix = Tool.convertPose2GLMatrix(
//                    trackableResult.getPose()).getData();
//
//            // The image target specific result:
//            ImageTargetResult imageTargetResult = (ImageTargetResult) trackableResult;
//            VirtualButtonResultList virtualButtonResultList = imageTargetResult.getVirtualButtonResults();
//
//            // Set transformations:
//            float[] modelViewProjection = new float[16];
//            Matrix.multiplyMM(modelViewProjection, 0, projectionMatrix, 0, modelViewMatrix, 0);
//
//            // Set the texture used for the teapot model:
//            int textureIndex = 0;
//
//            float vbVertices[] = new float[virtualButtonResultList.size() * 24];
//            short vbCounter = 0;
//            // Iterate through this targets virtual buttons:
//            for (VirtualButtonResult buttonResult : virtualButtonResultList){
//                VirtualButton button = buttonResult.getVirtualButton();
//
//                int buttonIndex = 0;
//
//                // Run through button name array to find button index
//                for (int j = 0; j < ImageTargets.NUM_BUTTONS; ++j)
//                {
//                    if (button.getName().compareTo(
//                            mActivity.virtualButtonColors[j]) == 0)
//                    {
//                        buttonIndex = j;
//                        break;
//                    }
//                }
//                Log.d("pkspks", "호출");
//                // If the button is pressed, than use this texture:
//                if (buttonResult.isPressed())
//                {
//                    Log.d("pkspks", "pressed");
//                    textureIndex = buttonIndex + 1;
//                }
//
//                // Define the four virtual buttons as Rectangle using the same values as the dataset
//                Rectangle vbRectangle[] = new Rectangle[4];
//                vbRectangle[0] = new Rectangle(RED_VB_BUTTON[0], RED_VB_BUTTON[1],
//                        RED_VB_BUTTON[2], RED_VB_BUTTON[3]);
//                vbRectangle[1] = new Rectangle(BLUE_VB_BUTTON[0], BLUE_VB_BUTTON[1],
//                        BLUE_VB_BUTTON[2], BLUE_VB_BUTTON[3]);
//                vbRectangle[2] = new Rectangle(YELLOW_VB_BUTTON[0], YELLOW_VB_BUTTON[1],
//                        YELLOW_VB_BUTTON[2], YELLOW_VB_BUTTON[3]);
//                vbRectangle[3] = new Rectangle(GREEN_VB_BUTTON[0], GREEN_VB_BUTTON[1],
//                        GREEN_VB_BUTTON[2], GREEN_VB_BUTTON[3]);
//
//                // We add the vertices to a common array in order to have one
//                // single draw call. This is more efficient than having multiple
//                // glDrawArray calls
//                vbVertices[vbCounter] = vbRectangle[buttonIndex].getLeftTopX();
//                vbVertices[vbCounter + 1] = vbRectangle[buttonIndex]
//                        .getLeftTopY();
//                vbVertices[vbCounter + 2] = 0.0f;
//                vbVertices[vbCounter + 3] = vbRectangle[buttonIndex]
//                        .getRightBottomX();
//                vbVertices[vbCounter + 4] = vbRectangle[buttonIndex]
//                        .getLeftTopY();
//                vbVertices[vbCounter + 5] = 0.0f;
//                vbVertices[vbCounter + 6] = vbRectangle[buttonIndex]
//                        .getRightBottomX();
//                vbVertices[vbCounter + 7] = vbRectangle[buttonIndex]
//                        .getLeftTopY();
//                vbVertices[vbCounter + 8] = 0.0f;
//                vbVertices[vbCounter + 9] = vbRectangle[buttonIndex]
//                        .getRightBottomX();
//                vbVertices[vbCounter + 10] = vbRectangle[buttonIndex]
//                        .getRightBottomY();
//                vbVertices[vbCounter + 11] = 0.0f;
//                vbVertices[vbCounter + 12] = vbRectangle[buttonIndex]
//                        .getRightBottomX();
//                vbVertices[vbCounter + 13] = vbRectangle[buttonIndex]
//                        .getRightBottomY();
//                vbVertices[vbCounter + 14] = 0.0f;
//                vbVertices[vbCounter + 15] = vbRectangle[buttonIndex]
//                        .getLeftTopX();
//                vbVertices[vbCounter + 16] = vbRectangle[buttonIndex]
//                        .getRightBottomY();
//                vbVertices[vbCounter + 17] = 0.0f;
//                vbVertices[vbCounter + 18] = vbRectangle[buttonIndex]
//                        .getLeftTopX();
//                vbVertices[vbCounter + 19] = vbRectangle[buttonIndex]
//                        .getRightBottomY();
//                vbVertices[vbCounter + 20] = 0.0f;
//                vbVertices[vbCounter + 21] = vbRectangle[buttonIndex]
//                        .getLeftTopX();
//                vbVertices[vbCounter + 22] = vbRectangle[buttonIndex]
//                        .getLeftTopY();
//                vbVertices[vbCounter + 23] = 0.0f;
//                vbCounter += 24;
//
//            }
//
//            // We only render if there is something on the array
//            if (vbCounter > 0)
//            {
//                // Render frame around button
//                GLES20.glUseProgram(vbShaderProgramID);
//
//                GLES20.glVertexAttribPointer(vbVertexHandle, 3,
//                        GLES20.GL_FLOAT, false, 0, fillBuffer(vbVertices));
//
//                GLES20.glEnableVertexAttribArray(vbVertexHandle);
//
//                GLES20.glUniform1f(lineOpacityHandle, 1.0f);
//                GLES20.glUniform3f(lineColorHandle, 1.0f, 1.0f, 1.0f);
//
//                GLES20.glUniformMatrix4fv(mvpMatrixButtonsHandle, 1, false,
//                        modelViewProjection, 0);
//
//                // We multiply by 8 because that's the number of vertices per
//                // button
//                // The reason is that GL_LINES considers only pairs. So some
//                // vertices
//                // must be repeated.
//                GLES20.glDrawArrays(GLES20.GL_LINES, 0, virtualButtonResultList.size() * 8);
//
//                SampleUtils.checkGLError("VirtualButtons drawButton");
//
//                GLES20.glDisableVertexAttribArray(vbVertexHandle);
//            }
//
//            Texture thisTexture = mTextures.get(textureIndex);
////
////            // Scale 3D model
////            Matrix.scaleM(modelViewMatrix, 0, kTeapotScale, kTeapotScale,
////                    kTeapotScale);
////
////            float[] modelViewProjectionScaled = new float[16];
////            Matrix.multiplyMM(modelViewProjectionScaled, 0, projectionMatrix, 0, modelViewMatrix, 0);
//
//            float[] rotationMatrix = new float[16];
//            float[] translationMatrix = new float[16];
//            float[] modelViewProjectionScaled = new float[16];
//
//
//            GLES20.glUseProgram(shaderProgramID);
//
//            GLES20.glEnableVertexAttribArray(vertexHandle);
//            GLES20.glEnableVertexAttribArray(textureCoordHandle);
//
//            Matrix.setIdentityM(translationMatrix,0);
//            Matrix.setIdentityM(rotationMatrix,0);
//            Matrix.setRotateM(rotationMatrix, 0, 90, 0.0f, 0.0f, 1.0f);
//            Matrix.multiplyMM(modelViewMatrix, 0, modelViewMatrix, 0, rotationMatrix, 0);
//            Matrix.setIdentityM(rotationMatrix,0);
//            Matrix.setRotateM(rotationMatrix, 0, 90.0f, 1.0f, 0.0f, 0.0f);
//            Matrix.translateM(translationMatrix,0,0,0,0.5f);
//            Matrix.multiplyMM(modelViewMatrix, 0, modelViewMatrix, 0, translationMatrix, 0);
//            Matrix.multiplyMM(modelViewMatrix, 0, modelViewMatrix, 0, rotationMatrix, 0);
//            Matrix.multiplyMM(modelViewProjectionScaled, 0, projectionMatrix, 0, modelViewMatrix, 0);
//
//            //1 testwhite
//            GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT, false, 0, (FloatBuffer) verticeBuffers[0].get(0));
//            GLES20.glVertexAttribPointer(textureCoordHandle, 2, GLES20.GL_FLOAT, false, 0, (FloatBuffer) textureBuffers[0].get(0));
//            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
//            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 1);
//            GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, modelViewProjectionScaled, 0);
//            GLES20.glUniform1i(texSampler2DHandle, 0);
//            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mObj.getNumObjectVertex());
//
//            //2 testred
//            GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT, false, 0, (FloatBuffer) verticeBuffers[0].get(0));
//            GLES20.glVertexAttribPointer(textureCoordHandle, 2, GLES20.GL_FLOAT, false, 0, (FloatBuffer) textureBuffers[0].get(0));
//            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
//            GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, modelViewProjectionScaled, 0);
//            GLES20.glUniform1i(texSampler2DHandle, 0);
//            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mObj.getNumObjectVertex());
//
//            //3 testgreen
//            GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT, false, 0, (FloatBuffer) verticeBuffers[0].get(0));
//            GLES20.glVertexAttribPointer(textureCoordHandle, 2, GLES20.GL_FLOAT, false, 0, (FloatBuffer) textureBuffers[0].get(0));
//            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
//            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 3);
//            GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, modelViewProjectionScaled, 0);
//            GLES20.glUniform1i(texSampler2DHandle, 0);
//            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mObj.getNumObjectVertex());
//
//            //4 testwindow
//            GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT, false, 0, (FloatBuffer) verticeBuffers[0].get(0));
//            GLES20.glVertexAttribPointer(textureCoordHandle, 2, GLES20.GL_FLOAT, false, 0, (FloatBuffer) textureBuffers[0].get(0));
//            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
//            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 4);
//            GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, modelViewProjectionScaled, 0);
//            GLES20.glUniform1i(texSampler2DHandle, 0);
//            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mObj.getNumObjectVertex());
//
//        }
//        GLES20.glDisableVertexAttribArray(vertexHandle);
//        GLES20.glDisableVertexAttribArray(textureCoordHandle);
//
//        SampleUtils.checkGLError("VirtualButtons renderFrame");
//
//        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
//
//        Renderer.getInstance().end();
    }

    @Override
    public void initRendering()
    {
        Vuforia.setHint(HINT_MAX_SIMULTANEOUS_IMAGE_TARGETS , 4);
        if (mTextures == null)
        {
            return;
        }

        GLES20.glClearColor(0.0f, 0.0f, 0.0f, Vuforia.requiresAlpha() ? 0.0f
                : 1.0f);

        for (Texture t : mTextures)
        {
            GLES20.glGenTextures(1, t.mTextureID, 0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, t.mTextureID[0]);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA,
                    t.mWidth, t.mHeight, 0, GLES20.GL_RGBA,
                    GLES20.GL_UNSIGNED_BYTE, t.mData);
        }

        shaderProgramID = SampleUtils.createProgramFromShaderSrc(
                CubeShaders.CUBE_MESH_VERTEX_SHADER,
                CubeShaders.CUBE_MESH_FRAGMENT_SHADER);

        vertexHandle = GLES20.glGetAttribLocation(shaderProgramID,
                "vertexPosition");
        textureCoordHandle = GLES20.glGetAttribLocation(shaderProgramID,
                "vertexTexCoord");
        mvpMatrixHandle = GLES20.glGetUniformLocation(shaderProgramID,
                "modelViewProjectionMatrix");
        texSampler2DHandle = GLES20.glGetUniformLocation(shaderProgramID,
                "texSampler2D");

        if(!mModelIsLoaded)
        {
            mTeapot = new Teapot();
            mCube = new CubeObject();

            //cat
            ObjParser objParser = new ObjParser(mActivityRef.get());
            try{
                objParser.parse(R.raw.cat);
            }
            catch(IOException e){
                e.printStackTrace();
            }
            mObj = new OBJLoader(objParser);
            verticeBuffers[0] = mObj.getBuffers(0);
            textureBuffers[0] = mObj.getBuffers(2);
            //dog
            ObjParser objParser2 = new ObjParser(mActivityRef.get());
            try{
                objParser2.parse(R.raw.dog);
            }
            catch(IOException e){
                e.printStackTrace();
            }
            mObj2 = new OBJLoader(objParser2);


            try {
                mBuildingsModel = new SampleApplication3DModel();
                mBuildingsModel.loadModel(mActivityRef.get().getResources().getAssets(),
                        "ImageTargets/Buildings.txt");
                mModelIsLoaded = true;
            } catch (IOException e)
            {
                Log.e(LOGTAG, "Unable to load buildings");
            }

            // Hide the Loading Dialog
            mActivityRef.get().loadingDialogHandler
                    .sendEmptyMessage(LoadingDialogHandler.HIDE_LOADING_DIALOG);
        }

        vertexHandle = GLES20.glGetAttribLocation(shaderProgramID,
                "vertexPosition");
        textureCoordHandle = GLES20.glGetAttribLocation(shaderProgramID,
                "vertexTexCoord");
        mvpMatrixHandle = GLES20.glGetUniformLocation(shaderProgramID,
                "modelViewProjectionMatrix");
        texSampler2DHandle = GLES20.glGetUniformLocation(shaderProgramID,
                "texSampler2D");

        // OpenGL setup for Virtual Buttons
        vbShaderProgramID = SampleUtils.createProgramFromShaderSrc(
                LineShaders.LINE_VERTEX_SHADER, LineShaders.LINE_FRAGMENT_SHADER);

        mvpMatrixButtonsHandle = GLES20.glGetUniformLocation(vbShaderProgramID,
                "modelViewProjectionMatrix");
        vbVertexHandle = GLES20.glGetAttribLocation(vbShaderProgramID,
                "vertexPosition");
        lineOpacityHandle = GLES20.glGetUniformLocation(vbShaderProgramID,
                "opacity");
        lineColorHandle = GLES20.glGetUniformLocation(vbShaderProgramID,
                "color");

    }


    private void renderModel(float[] projectionMatrix, float[] viewMatrix, float[] modelMatrix, int textureIndex)
    {
        MeshObject model;
        float[] modelMatrix2 = Arrays.copyOf(modelMatrix,modelMatrix.length);


        float[] modelViewProjection = new float[16];
        // Apply local transformation to our model
        if (mActivityRef.get().isDeviceTrackingActive())
        {
            Matrix.translateM(modelMatrix, 0, 0, -0.06f, 0);
            Matrix.rotateM(modelMatrix, 0, 90.0f, 1.0f, 0, 0);
            Matrix.scaleM(modelMatrix, 0, BUILDING_SCALE, BUILDING_SCALE, BUILDING_SCALE);

            model = mBuildingsModel;
        }
        else

        {
            if(objectSel == 0) {
                Matrix.translateM(modelMatrix, 0, 0, 0, OBJECT_SCALE_FLOAT);
                Matrix.scaleM(modelMatrix, 0, OBJECT_SCALE_FLOAT, OBJECT_SCALE_FLOAT, OBJECT_SCALE_FLOAT);
                model = mTeapot;
            }else if(objectSel == 1) {
                Matrix.translateM(modelMatrix, 0, 0, 0, 0.01f);
                Matrix.scaleM(modelMatrix, 0, 0.01f, 0.01f, 0.01f);
                model = mCube;
            } else if(objectSel == 2) { // my obj 개냥이

                float[] tmp_modelMatrix = new float[16];

                Matrix.translateM(modelMatrix, 0, x_move, y_move, z_move);
                System.arraycopy(modelMatrix, 0, tmp_modelMatrix, 0, modelMatrix.length);
                Matrix.rotateM(modelMatrix, 0, rot, 0, 0, 1);
                Matrix.scaleM(modelMatrix, 0, 0.01f, 0.01f, 0.01f);

                //고양이
//                Matrix.rotateM(modelMatrix, 0, 0 ,0.00f, 1.0f, 0.00f);
//                Matrix.translateM(modelMatrix, 0, x_move, y_move, 0.01f);
//                Matrix.scaleM(modelMatrix, 0, 0.01f, 0.01f, 0.01f);

//
//                if (y_move < -0.1f) {
//                    y_move = y_move + 0.001f;
//                } else {
//                    for (int i = 0; i < 100; i++) {
//                        y_move = y_move - 0.001f;
//                    }
//                }

                //강아지
                if (Math.sqrt(Math.pow(x_move - dogposition_x , 2) + Math.pow(y_move - dogposition_y, 2) +
                        Math.pow(z_move - dogposition_z, 2)) < 0.015){
//                    flag_dog = false;
                    randomValue = Math.random();
                    random = new Random();
                    dogposition_x = (float) (randomValue * 0.2f) -0.1f;
                    dogposition_y = (float) (randomValue * 0.2f) -0.1f;
                    dogposition_z_bool = random.nextBoolean();
                    if (dogposition_z_bool == true){
                        dogposition_z = 0.06f;
                    }else{
                        dogposition_z = 0.00f;
                    }
                }else {
//                    flag_dog = true;
                    Matrix.translateM(modelMatrix2, 0, dogposition_x, dogposition_y, dogposition_z);
                    Matrix.scaleM(modelMatrix2, 0, -0.01f, 0.01f, 0.01f);
                }
//
//                if(flag_dog){
//
//
//                    Matrix.translateM(modelMatrix2, 0, dogposition_x, dogposition_y, 0.0f);
//                    Matrix.scaleM(modelMatrix2, 0, -0.01f, 0.01f, 0.01f);
//                }

//                angle++;
//                dogposition_x = 0.1f*(float)Math.cos(angle*Math.PI/180.0f);
//                dogposition_y = 0.1f*(float)Math.sin(angle*Math.PI/180.0f);




            }
        }

        my_draw(mObj,modelMatrix,projectionMatrix,viewMatrix,modelViewProjection, textureIndex); // cat
        my_draw(mObj2,modelMatrix2,projectionMatrix,viewMatrix,modelViewProjection, textureIndex+1); // dog

    }

        private void my_draw(OBJLoader obj, float[] modelMatrix, float[] projectionMatrix, float[] viewMatrix, float[] modelViewProjection, int textureIndex){

        OBJLoader model = obj;
        // Combine device pose (view matrix) with model matrix
        Matrix.multiplyMM(modelMatrix, 0, viewMatrix, 0, modelMatrix, 0);

        // Do the final combination with the projection matrix
        Matrix.multiplyMM(modelViewProjection, 0, projectionMatrix, 0, modelMatrix, 0);

        // Activate the shader program and bind the vertex and tex coords
        GLES20.glUseProgram(shaderProgramID);

        GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT, false, 0, model.getVertices());
        GLES20.glVertexAttribPointer(textureCoordHandle, 2, GLES20.GL_FLOAT, false, 0, model.getTexCoords());

        GLES20.glEnableVertexAttribArray(vertexHandle);
        GLES20.glEnableVertexAttribArray(textureCoordHandle);

        // Activate texture 0, bind it, pass to shader
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures.get(textureIndex).mTextureID[0]);
        GLES20.glUniform1i(texSampler2DHandle, 0);

        // Pass the model view matrix to the shader
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, modelViewProjection, 0);

        // Finally draw the model
        if (mActivityRef.get().isDeviceTrackingActive())
        {
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, model.getNumObjectVertex());
        }
        else
        {
//            GLES20.glDrawElements(GLES20.GL_TRIANGLES, model.getNumObjectIndex(), GLES20.GL_UNSIGNED_SHORT, model.getIndices());
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, model.getNumObjectVertex());

        }

        // Disable the enabled arrays
        GLES20.glDisableVertexAttribArray(vertexHandle);
        GLES20.glDisableVertexAttribArray(textureCoordHandle);
    }

    public void setTextures(Vector<Texture> textures)
    {
        mTextures = textures;
    }


    private void setIsTargetCurrentlyTracked(TrackableResultList trackableResultList)
    {
        for(TrackableResult result : trackableResultList)
        {
            // Check the tracking status for result types
            // other than DeviceTrackableResult. ie: ImageTargetResult
            if (!result.isOfType(DeviceTrackableResult.getClassType()))
            {
                int currentStatus = result.getStatus();
                int currentStatusInfo = result.getStatusInfo();

                // The target is currently being tracked if the status is TRACKED|NORMAL
                if (currentStatus == TrackableResult.STATUS.TRACKED
                        || currentStatusInfo == TrackableResult.STATUS_INFO.NORMAL)
                {
                    mIsTargetCurrentlyTracked = true;
                    return;
                }
            }
        }

        mIsTargetCurrentlyTracked = false;
    }

    private Buffer fillBuffer(float[] array)
    {
        // Convert to floats because OpenGL doesnt work on doubles, and manually
        // casting each input value would take too much time.
        // Each float takes four bytes
        ByteBuffer bb = ByteBuffer.allocateDirect(4 * array.length);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        for (float d : array)
            bb.putFloat(d);
        bb.rewind();

        return bb;

    }

    boolean isTargetCurrentlyTracked()
    {
        return mIsTargetCurrentlyTracked;
    }
}
