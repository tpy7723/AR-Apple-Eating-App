package com.vuforia.engine.CoreSamples.app.VirtualButtons;

import android.annotation.SuppressLint;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;
import android.widget.TextView;
import com.vuforia.Device;
import com.vuforia.ImageTargetResult;
import com.vuforia.Rectangle;
import com.vuforia.Renderer;
import com.vuforia.State;
import com.vuforia.Tool;
import com.vuforia.TrackableResult;
import com.vuforia.VirtualButton;
import com.vuforia.VirtualButtonResult;
import com.vuforia.Vuforia;
import com.vuforia.engine.CoreSamples.R;
import com.vuforia.engine.CoreSamples.app.ImageTargets.OBJLoader;
import com.vuforia.engine.CoreSamples.app.ImageTargets.OBJParser.ObjParser;
import com.vuforia.engine.SampleApplication.SampleAppRenderer;
import com.vuforia.engine.SampleApplication.SampleAppRendererControl;
import com.vuforia.engine.SampleApplication.SampleApplicationSession;
import com.vuforia.engine.SampleApplication.SampleRendererBase;
import com.vuforia.engine.SampleApplication.utils.CubeObject;
import com.vuforia.engine.SampleApplication.utils.CubeShaders;
import com.vuforia.engine.SampleApplication.utils.LineShaders;
import com.vuforia.engine.SampleApplication.utils.SampleUtils;
import com.vuforia.engine.SampleApplication.utils.Teapot;
import com.vuforia.engine.SampleApplication.utils.Texture;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.Vector;

public class VirtualButtonRenderer extends SampleRendererBase implements SampleAppRendererControl
{
    private static final String LOGTAG = "VirtualButtonRenderer";

    private final VirtualButtons mActivity;
    private int select = 0;

    private static TextView textView2, textView4;

    static int score = 0;
    static int combo = 0;
    private OBJLoader tempobj; // 주인공
    private int tempindex = 0;
    private OBJLoader mObj,mObj2; // cat , dog
    private OBJLoader dogObj,elefanteObj; // dog, elefante
    private OBJLoader gressObj, treeObj; // gress, tree
    private final WeakReference<VirtualButtons> mActivityRef;

    private int apple_rot = 0 ;

    private int temptime = 0;
    private int temptime2 = 1000;
    public static float rot = 0.0f;

    private Random random = new Random();
    private double randomValue = Math.random();
    private Boolean appleposition_z_bool = true;

    public static float x_move = -0.001f;
    public static float y_move = -0.001f;
    public static float z_move = -0.001f;

    private float appleposition_x = (float) (randomValue * 0.2f) -0.1f;
    private float appleposition_y = (float) (randomValue * 0.2f) -0.1f;
    private float appleposition_z = 0.06f;

    // OpenGL ES 2.0 specific (3D model):
    private int shaderProgramID = 0;
    private int vertexHandle = 0;
    private int textureCoordHandle = 0;
    private int mvpMatrixHandle = 0;
    private int texSampler2DHandle = 0;

    private int lineOpacityHandle = 0;
    private int lineColorHandle = 0;
    private int mvpMatrixButtonsHandle = 0;

    // OpenGL ES 2.0 specific (Virtual Buttons):
    private int vbShaderProgramID = 0;
    private int vbVertexHandle = 0;

//    private static final float kTeapotScale = 0.005f;

    // Define the coordinates of the virtual buttons to render the area of action,
    // These values are the same as those in Wood.xml

    static private final float[] RED_VB_BUTTON =  {-0.10868f, -0.05352f, -0.07575f, -0.06587f};
    static private final float[] BLUE_VB_BUTTON =  {-0.04528f, -0.05352f, -0.01235f, -0.06587f};
    static private final float[] YELLOW_VB_BUTTON =  {0.01482f, -0.05352f, 0.04775f, -0.06587f};
    static private final float[] GREEN_VB_BUTTON =  {0.07657f, -0.05352f, 0.10950f, -0.06587f};
    static private float ROT_VB_BUTTON[] = {0.07657f, 0.05352f, 0.10950f, 0.06587f};

    // Constants:
    static private float kTeapotScale = 0.005f;

    VirtualButtonRenderer(VirtualButtons activity,
                          SampleApplicationSession session)
    {
        mActivity = activity;
        mActivityRef = new WeakReference<>(activity);
        vuforiaAppSession = session;

        // SampleAppRenderer used to encapsulate the use of RenderingPrimitives setting
        // the device mode AR/VR and stereo mode
        mSampleAppRenderer = new SampleAppRenderer(this, mActivity, Device.MODE.MODE_AR,
                vuforiaAppSession.getVideoMode(), false, 0.01f, 5f);
    }


    public void setActive(boolean active)
    {
        mSampleAppRenderer.setActive(active);
    }


    @Override
    public void initRendering()
    {
        Log.d(LOGTAG, "initRendering");

        textView2 = (TextView) mActivity.findViewById(R.id.textView2);
        textView4 = (TextView) mActivity.findViewById(R.id.textView4);


        // Define clear color
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, Vuforia.requiresAlpha() ? 0.0f
                : 1.0f);


        // Now generate the OpenGL texture objects and add settings
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



        //cat
        ObjParser objParser = new ObjParser(mActivityRef.get());
        try{
            objParser.parse(R.raw.cat);
        }
        catch(IOException e){
            e.printStackTrace();
        }
        mObj = new OBJLoader(objParser);
        tempobj = mObj; // 기본 캐릭터

        //apple
        ObjParser objParser2 = new ObjParser(mActivityRef.get());
        try{
            objParser2.parse(R.raw.dog);
        }
        catch(IOException e){
            e.printStackTrace();
        }
        mObj2 = new OBJLoader(objParser2);

        //dog
        ObjParser objParser3 = new ObjParser(mActivityRef.get());
        try{
            objParser3.parse(R.raw.dog2);
        }
        catch(IOException e){
            e.printStackTrace();
        }
        dogObj = new OBJLoader(objParser3);

        //elefante
        ObjParser objParser4 = new ObjParser(mActivityRef.get());
        try{
            objParser4.parse(R.raw.elefante);
        }
        catch(IOException e){
            e.printStackTrace();
        }
        elefanteObj = new OBJLoader(objParser4);

        //gress
        ObjParser objParser5 = new ObjParser(mActivityRef.get());
        try{
            objParser5.parse(R.raw.gress);
        }
        catch(IOException e){
            e.printStackTrace();
        }
        gressObj = new OBJLoader(objParser5);

        //tree
        ObjParser objParser6 = new ObjParser(mActivityRef.get());
        try{
            objParser6.parse(R.raw.tree);
        }
        catch(IOException e){
            e.printStackTrace();
        }
        treeObj = new OBJLoader(objParser6);
    }


    public void updateRenderingPrimitives()
    {
        mSampleAppRenderer.updateRenderingPrimitives();
    }


    @SuppressLint("SetTextI18n")
    public void renderFrame(State state, float[] projectionMatrix) {
        // Renders video background replacing Renderer.DrawVideoBackground()
        mSampleAppRenderer.renderVideoBackground(state);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
//        GLES20.glEnable(GLES20.GL_CULL_FACE);
//        GLES20.glCullFace(GLES20.GL_BACK);

        // Did we find any trackables this frame?
        Log.d("pkspks3", Integer.toString(state.getTrackableResults().size()));
        if (state.getTrackableResults().size() > 0) {
            // Get the trackable:
            TrackableResult trackableResult = state.getTrackableResults().at(0);
            float[] modelViewMatrix = Tool.convertPose2GLMatrix( // character
                    trackableResult.getPose()).getData();

            float[] modelViewMatrix2 = Arrays.copyOf(modelViewMatrix,modelViewMatrix.length); // apple
            float[] modelViewMatrix3 = Arrays.copyOf(modelViewMatrix,modelViewMatrix.length); // gress
            float[] modelViewMatrix4 = Arrays.copyOf(modelViewMatrix,modelViewMatrix.length); // tree
            // The image target specific result:
            ImageTargetResult imageTargetResult = (ImageTargetResult) trackableResult;

            // Set transformations:
            float[] modelViewProjection = new float[16];
            Matrix.multiplyMM(modelViewProjection, 0, projectionMatrix, 0, modelViewMatrix, 0);

            float vbVertices[] = new float[imageTargetResult
                    .getVirtualButtonResults().size() * 24];
            short vbCounter = 0;

            // Iterate through this targets virtual buttons:
            for (int i = 0; i < imageTargetResult.getVirtualButtonResults().size(); ++i) {
                VirtualButtonResult buttonResult = imageTargetResult
                        .getVirtualButtonResults().at(i);
                VirtualButton button = buttonResult.getVirtualButton();
                int buttonIndex = 0;
                //shark_rotate = true;
                // Run through button name array to find button index
                for (int j = 0; j < VirtualButtons.NUM_BUTTONS; ++j) {
                    if (button.getName().compareTo(mActivity.virtualButtonColors[j]) == 0) {
                        buttonIndex = j;
                        break;
                    }
                }

                if (buttonResult.isPressed()) {
                    switch (buttonIndex){
                        case 0: // speed +
                            VirtualButtons.velocity += 0.0001f;
                            Log.d("pks4", "speed up");
                            break;

                        case 1: // speed -
                            VirtualButtons.velocity -= 0.0001f;
                            if (VirtualButtons.velocity <= 0.0001f){
                                VirtualButtons.velocity = 0.0001f;
                            }
                            Log.d("pks4", "speed down");
                            break;

                        case 2: // size +
                            kTeapotScale += 0.0001f;
                            Log.d("pks4", "Size +");
                            break;

                        case 3: //size-
                            kTeapotScale -= 0.0001f;
                            if (kTeapotScale <= 0.0001f){
                                kTeapotScale = 0.0001f;
                            }
                            Log.d("pks4", "Size -");
                            break;

                        case 4: // change
                            x_move = -0.001f;
                            y_move = -0.001f;
                            y_move = -0.001f;
                            rot = 0;

                            switch (select){
                                case 25:
                                    tempobj = elefanteObj;
                                    tempindex = 3;
                                    break;
                                case 50:
                                    tempobj = dogObj;
                                    tempindex = 2;
                                    break;
                                case 75:
                                    tempobj = mObj;
                                    tempindex = 0;
                                    break;
                            }
                            select++;
                            if (select == 100){
                                select = 0;
                            }
                            Log.d("pks4", "change");
                            break;

                    }
                }

                // Define the four virtual buttons as Rectangle using the same values as the dataset
                Rectangle vbRectangle[] = new Rectangle[5];
                vbRectangle[0] = new Rectangle(RED_VB_BUTTON[0], RED_VB_BUTTON[1],
                        RED_VB_BUTTON[2], RED_VB_BUTTON[3]);
                vbRectangle[1] = new Rectangle(BLUE_VB_BUTTON[0], BLUE_VB_BUTTON[1],
                        BLUE_VB_BUTTON[2], BLUE_VB_BUTTON[3]);
                vbRectangle[2] = new Rectangle(YELLOW_VB_BUTTON[0], YELLOW_VB_BUTTON[1],
                        YELLOW_VB_BUTTON[2], YELLOW_VB_BUTTON[3]);
                vbRectangle[3] = new Rectangle(GREEN_VB_BUTTON[0], GREEN_VB_BUTTON[1],
                        GREEN_VB_BUTTON[2], GREEN_VB_BUTTON[3]);
                vbRectangle[4] = new Rectangle(ROT_VB_BUTTON[0], ROT_VB_BUTTON[1],
                        ROT_VB_BUTTON[2], ROT_VB_BUTTON[3]);


                // We add the vertices to a common array in order to have one
                // single draw call. This is more efficient than having multiple
                // glDrawArray calls

                vbVertices[vbCounter] = vbRectangle[buttonIndex].getLeftTopX();
                vbVertices[vbCounter + 1] = vbRectangle[buttonIndex]
                        .getLeftTopY();
                vbVertices[vbCounter + 2] = 0.0f;
                vbVertices[vbCounter + 3] = vbRectangle[buttonIndex]
                        .getRightBottomX();
                vbVertices[vbCounter + 4] = vbRectangle[buttonIndex]
                        .getLeftTopY();
                vbVertices[vbCounter + 5] = 0.0f;
                vbVertices[vbCounter + 6] = vbRectangle[buttonIndex]
                        .getRightBottomX();
                vbVertices[vbCounter + 7] = vbRectangle[buttonIndex]
                        .getLeftTopY();
                vbVertices[vbCounter + 8] = 0.0f;
                vbVertices[vbCounter + 9] = vbRectangle[buttonIndex]
                        .getRightBottomX();
                vbVertices[vbCounter + 10] = vbRectangle[buttonIndex]
                        .getRightBottomY();
                vbVertices[vbCounter + 11] = 0.0f;
                vbVertices[vbCounter + 12] = vbRectangle[buttonIndex]
                        .getRightBottomX();
                vbVertices[vbCounter + 13] = vbRectangle[buttonIndex]
                        .getRightBottomY();
                vbVertices[vbCounter + 14] = 0.0f;
                vbVertices[vbCounter + 15] = vbRectangle[buttonIndex]
                        .getLeftTopX();
                vbVertices[vbCounter + 16] = vbRectangle[buttonIndex]
                        .getRightBottomY();
                vbVertices[vbCounter + 17] = 0.0f;
                vbVertices[vbCounter + 18] = vbRectangle[buttonIndex]
                        .getLeftTopX();
                vbVertices[vbCounter + 19] = vbRectangle[buttonIndex]
                        .getRightBottomY();
                vbVertices[vbCounter + 20] = 0.0f;
                vbVertices[vbCounter + 21] = vbRectangle[buttonIndex]
                        .getLeftTopX();
                vbVertices[vbCounter + 22] = vbRectangle[buttonIndex]
                        .getLeftTopY();
                vbVertices[vbCounter + 23] = 0.0f;
                vbCounter += 24;

            }

            // We only render if there is something on the array
            if (vbCounter > 0)
            {
                // Render frame around button
                GLES20.glUseProgram(vbShaderProgramID);

                GLES20.glVertexAttribPointer(vbVertexHandle, 3,
                        GLES20.GL_FLOAT, false, 0, fillBuffer(vbVertices));

                GLES20.glEnableVertexAttribArray(vbVertexHandle);

                GLES20.glUniform1f(lineOpacityHandle, 1.0f);
                GLES20.glUniform3f(lineColorHandle, 1.0f, 1.0f, 1.0f);

                GLES20.glUniformMatrix4fv(mvpMatrixButtonsHandle, 1, false,
                        modelViewProjection, 0);

                // We multiply by 8 because that's the number of vertices per
                // button
                // The reason is that GL_LINES considers only pairs. So some
                // vertices
                // must be repeated.
                GLES20.glDrawArrays(GLES20.GL_LINES, 0, imageTargetResult.getVirtualButtonResults().size() * 8);


                SampleUtils.checkGLError("VirtualButtons drawButton");

                GLES20.glDisableVertexAttribArray(vbVertexHandle);
            }

            float[] tmp_modelMatrix = new float[16];

            Log.d("x_move", Double.toString(x_move));
            Log.d("y_move", Double.toString(y_move));
            Log.d("z_move", Double.toString(z_move));

            if (x_move < -0.1189999133348465f){
                x_move = (-1f)* 0.1189999133348465f;
            }
            if (x_move > 0.07219992578029633f){
                x_move = 0.07219992578029633f;
            }
            if (y_move < -0.05139996111392975f){
                y_move = (-1f)* 0.05139996111392975f;
            }
            if (y_move >0.07739998400211334f){
                y_move = 0.07739998400211334f;
            }

            Log.d("pks123", Double.toString(x_move));
            Log.d("pks123", Double.toString(y_move));

            Matrix.translateM(modelViewMatrix, 0, x_move, y_move, z_move);
            System.arraycopy(modelViewMatrix, 0, tmp_modelMatrix, 0, modelViewMatrix.length);
            Matrix.rotateM(modelViewMatrix, 0, rot, 0, 0, 1);
            Matrix.scaleM(modelViewMatrix, 0, -1 * kTeapotScale,kTeapotScale,kTeapotScale);

            // 사과 먹기 (충돌)
            if (Math.sqrt(Math.pow(x_move - appleposition_x , 2) + Math.pow(y_move - appleposition_y, 2) +
                    Math.pow(z_move - appleposition_z, 2)) < 0.015){

                VirtualButtons.sound2.play(VirtualButtons.soundId2,1f,1f,0,0,1f); // button sound

                randomValue = Math.random();
                random = new Random();
                appleposition_x = (float) (randomValue * 0.2f) -0.1f;
                appleposition_y = (float) (randomValue * 0.2f) -0.1f;
                appleposition_z_bool = random.nextBoolean();
                if (appleposition_z_bool){
                    appleposition_z = 0.06f;
                }else{
                    appleposition_z = 0.00f;
                }


                if (appleposition_x < -0.1f){
                    appleposition_x = (-1f)* 0.1f;
                }
                if (appleposition_x > 0.07f){
                    appleposition_x = 0.07f;
                }
                if (appleposition_y < -0.05f){
                    appleposition_y = (-1f)* 0.05f;
                }
                if (appleposition_y >0.07f){
                    appleposition_y = 0.07f;
                }



                score += 10;

                if (VirtualButtons.record_time - temptime <= 5){
                    combo++;
                    score += 10; // 추가점수
                    textView4.setText(" Combo " + Integer.toString(combo) + " !!!");
                    temptime2 = VirtualButtons.record_time;
                }
                temptime = VirtualButtons.record_time;

                textView2.setText(" 점수 : " + Integer.toString(score));

                //      mHandler.postDelayed(r, 50);


            }else {
                if(VirtualButtons.record_time - temptime >= 5){
                    combo = 0;
                    textView4.setText("");
                }
                Matrix.translateM(modelViewMatrix2, 0, appleposition_x, appleposition_y, appleposition_z);
                Matrix.rotateM(modelViewMatrix2, 0,apple_rot,0,0,1);
                apple_rot = apple_rot + 3;
                if (apple_rot==360){
                    apple_rot = 0;
                }
                Matrix.scaleM(modelViewMatrix2, 0, -0.01f, 0.01f, 0.01f);
            }

            Matrix.translateM(modelViewMatrix3, 0, -0.030f, 0.052f,0.0f);
            Matrix.scaleM(modelViewMatrix3, 0, -0.012f, 0.009f, 0.01f);

            Matrix.translateM(modelViewMatrix4, 0, -0.050f, 0.00f,0.0f);
            Matrix.scaleM(modelViewMatrix4, 0, -0.01f, 0.01f, 0.01f);

            if (VirtualButtons.gamestate){
                my_draw(tempobj, modelViewMatrix, projectionMatrix, tempindex); // cat
                my_draw(mObj2, modelViewMatrix2, projectionMatrix, 1); // apple
                my_draw(gressObj,modelViewMatrix3, projectionMatrix, 4); // gress
                my_draw(treeObj,modelViewMatrix4, projectionMatrix, 5); // tree
            }

        }

        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        Renderer.getInstance().end();
    }


    private void my_draw(OBJLoader obj, float[] modelViewMatrix, float[] projectionMatrix, int index){
        OBJLoader mObj = obj;
        Texture thisTexture = mTextures.get(index);

        float[] modelViewProjectionScaled = new float[16];
        Matrix.multiplyMM(modelViewProjectionScaled, 0, projectionMatrix, 0, modelViewMatrix, 0);

        // Render 3D model
        GLES20.glUseProgram(shaderProgramID);

        GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT,
                false, 0, mObj.getVertices());
        GLES20.glVertexAttribPointer(textureCoordHandle, 2,
                GLES20.GL_FLOAT, false, 0, mObj.getTexCoords());

        GLES20.glEnableVertexAttribArray(vertexHandle);
        GLES20.glEnableVertexAttribArray(textureCoordHandle);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
                thisTexture.mTextureID[0]);
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false,
                modelViewProjectionScaled, 0);
        GLES20.glUniform1i(texSampler2DHandle, 0);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mObj.getNumObjectVertex());

        GLES20.glDisableVertexAttribArray(vertexHandle);
        GLES20.glDisableVertexAttribArray(textureCoordHandle);

        SampleUtils.checkGLError("VirtualButtons renderFrame");
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


    public void setTextures(Vector<Texture> textures)
    {
        mTextures = textures;
    }
}
