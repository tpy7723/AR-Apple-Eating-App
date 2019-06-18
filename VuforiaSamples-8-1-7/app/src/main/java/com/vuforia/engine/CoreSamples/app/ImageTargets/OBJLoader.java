package com.vuforia.engine.CoreSamples.app.ImageTargets;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;

import com.vuforia.engine.CoreSamples.app.ImageTargets.OBJParser.ObjParser;
import com.vuforia.engine.SampleApplication.utils.MeshObject;

public class OBJLoader extends MeshObject {
    private Buffer mVertBuff;
    private Buffer mTexCoordBuff;
    private Buffer mNormBuff;
    private Buffer mIndBuff;

    private int indicesNumber = 0;
    private int verticesNumber = 0;

    private ArrayList<FloatBuffer> vertexBuffers = new ArrayList<>();
    private ArrayList<FloatBuffer> normalBuffers = new ArrayList<>();
    private ArrayList<FloatBuffer> textureBuffers = new ArrayList<>();
    private ArrayList<ShortBuffer> indicesBuffers = new ArrayList<>();

    boolean texture_flag , normal_flag;
    private ArrayList<Integer> vertices_count = new ArrayList<>();
    private ArrayList<Integer> indices_count = new ArrayList<>();

    float scale = 1.0f;
    int group;
    int[] textureIDs;

    public OBJLoader(ObjParser objParser) {
        group = objParser.getObjectIds().size();
        texture_flag = objParser.texture_flag;
        normal_flag = objParser.normal_flag;
        textureIDs = new int[group];

        for(int i = 0; i < group; i++) {
            String ID = objParser.getObjectIds().get(i);
            float[] vertices = objParser.getObjectVertices(ID);
            float[] normals = objParser.getObjectNormals(ID);
            float[] textures = objParser.getObjectTextures(ID);
            short[] indices = objParser.getObjectIndices(ID);

            mVertBuff = fillBuffer(vertices);
            vertexBuffers.add(((ByteBuffer) mVertBuff).asFloatBuffer());
            vertices_count.add(vertices.length / 3);
            verticesNumber += vertices.length / 3;

            mIndBuff = fillBuffer(indices);
            indicesBuffers.add(((ByteBuffer) mIndBuff).asShortBuffer());
            indices_count.add(indices.length);
            indicesNumber += indices.length;

            if (texture_flag) {
                mTexCoordBuff = fillBuffer(textures);
                textureBuffers.add(((ByteBuffer) mTexCoordBuff).asFloatBuffer());
            }
            if (normal_flag) {
                mNormBuff = fillBuffer(normals);
                normalBuffers.add(((ByteBuffer) mNormBuff).asFloatBuffer());
            }
        }
    }

    @Override
    public int getNumObjectIndex() {
        return indicesNumber;
    }

    public int getNumObjectIndex(int i) {
        return indices_count.get(i);
    }

    @Override
    public int getNumObjectVertex() {
        return verticesNumber;
    }

    public int getNumObjectVertex(int i) {
        return vertices_count.get(i);
    }

    @Override
    public Buffer getBuffer(BUFFER_TYPE bufferType)
    {
        Buffer result = null;
        switch (bufferType)
        {
            case BUFFER_TYPE_VERTEX:
                result = mVertBuff;
                break;
            case BUFFER_TYPE_TEXTURE_COORD:
                result = mTexCoordBuff;
                break;
            case BUFFER_TYPE_NORMALS:
                result = mNormBuff;
                break;
            case BUFFER_TYPE_INDICES:
                result = mIndBuff;
            default:
                break;

        }

        return result;
    }

    public ArrayList getBuffers(int buf_num) {
        ArrayList result = null;

        switch (buf_num) {
            case 0:
                result = vertexBuffers;
                break;
            case 1:
                result = normalBuffers;
                break;
            case 2:
                result = textureBuffers;
                break;
            case 3:
                result = indicesBuffers;
            default:
                break;
        }

        return result;
    }
}