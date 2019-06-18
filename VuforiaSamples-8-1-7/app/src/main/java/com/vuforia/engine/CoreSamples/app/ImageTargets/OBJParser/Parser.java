package com.vuforia.engine.CoreSamples.app.ImageTargets.OBJParser;

import android.content.Context;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public abstract class Parser {
    /* 각 object ID 별 vertices, texture vertices, normal vertices, indices들을 따로 저장해주기 위해 Map 변수 사용 */
	private Map<String, ArrayList<Float>> vertices;
    private Map<String, ArrayList<Float>> textures;
    private Map<String, ArrayList<Float>> normals;
    private Map<String, ArrayList<Short>> indices;

    /* 각 object ID들을 저장할 ArrayList */
	private ArrayList<String> objIds;

	/* 각 object ID 별 Texture 파일들을 저장할 변수 */
	private Map<String, ArrayList<String>> textureFiles;

	/* 현재 object ID를 저장할 String형 변수 */
	private String currObjId;

	/* Buffer 변수 */
	protected BufferedReader file;

	protected Context context;

	/* 생성자, 모든 변수들 초기화 */
	public Parser(Context context) {
		this.context = context;
		this.objIds = new ArrayList<>();
		this.vertices = new HashMap<>();
		this.normals = new HashMap<>();
		this.textures = new HashMap<>();
        this.indices = new HashMap<>();
        this.textureFiles = new HashMap<>();
	}

	/* Resource의 OBJ 파일을 받아 buffer 준비 */
	public void parse(int resId) throws IOException {
		InputStream in = context.getResources().openRawResource(resId);
		InputStreamReader is = new InputStreamReader(in);
		this.file = new BufferedReader(is);
	}

	/* Vertices ArrayList에 추가 */
	protected void addVertice(Float[] vertice) {
		ArrayList<Float> verts = this.vertices.get(currObjId);

		for(int i = 0; i < vertice.length; i++) {
			verts.add(vertice[i]);
		}
	}

	/* Normal ArrayList에 추가 */
    protected void addNormal(Float[] normal) {
		ArrayList<Float> Norms = this.normals.get(currObjId);

		for(int i = 0; i < normal.length; i++) {
			Norms.add(normal[i]);
		}
	}

	/* Texture vertices ArrayList에 추가 */
    protected void addTexture(Float[] texture) {
		ArrayList<Float> Texts = this.textures.get(currObjId);

		for(int i = 0; i < texture.length; i++) {
			Texts.add(texture[i]);
		}
	}

    /* Indices ArrayList에 추가 */
    protected void addIndice(Short[] indice) {
        ArrayList<Short> indis = this.indices.get(currObjId);

        for(int i = 0; i < indice.length; i++) {
            indis.add(indice[i]);
        }
    }

	/* Texture file 추가 (currently unused) */
	protected void addTexturefile(String textureFile) {
		this.textureFiles.get(currObjId).add(textureFile);
	}

	/* 현재 object ID에 따라 모든 ArrayList들 추가 */
    protected void addObjId(String objId) {
		this.currObjId = objId;		
		this.objIds.add(objId);
		
		this.vertices.put(currObjId, new ArrayList<Float>());
		this.textures.put(currObjId, new ArrayList<Float>());
		this.normals.put(currObjId, new ArrayList<Float>());
        this.indices.put(currObjId, new ArrayList<Short>());
		this.textureFiles.put(currObjId, new ArrayList<String>());
	}

	/* Object ID 반환 */
	public ArrayList<String> getObjectIds() {
	    return  this.objIds;
	}

	/* 현재 object ID의 vertices 반환 */
	public float[] getObjectVertices(String objId) {
		ArrayList<Float> verts = this.vertices.get(objId);
		float[] retArray = new float[verts.size()];
		int i = 0;

		for(Float f : verts) {
			retArray[i++] = (f != null ? f.floatValue() : 0);
        }

		return retArray;
	}

    /* 현재 object ID의 normal 반환 */
	public float[] getObjectNormals(String objId) {
		ArrayList<Float> norms = this.normals.get(objId);
		float[] retArray = new float[norms.size()];
		int i = 0;

		for(Float f : norms) {
			retArray[i++] = (f != null ? f.floatValue() : 0);
		}

		return retArray;
	}

    /* 현재 object ID의 texture vertices 반환 */
	public float[] getObjectTextures(String objId) {
		ArrayList<Float> texts = this.textures.get(objId);
		float[] retArray = new float[texts.size()];
		int i = 0;

		for(Float f : texts) {
			retArray[i++] = (f != null ? f.floatValue() : 0);
		}

		return retArray;
	}

    /* 현재 object ID의 indices 반환 */
    public short[] getObjectIndices(String objId) {
        ArrayList<Short> indis = this.indices.get(objId);
        short[] retArray = new short[indis.size()];
        int i = 0;

        for(Short index : indis) {
            retArray[i++] = (index != null ? index.shortValue() : 0);
        }

        return retArray;
    }

    /* 현재 object ID의 texture file 반환 (currently unused) */
	public String[] getObjectTextureFiles(String objId) {
		ArrayList<String> textFiles = this.textureFiles.get(objId);
		String[] retArray = new String[textFiles.size()];
		int i = 0;

		for(String s : textFiles) {
			retArray[i++] = s;
		}

		return retArray;
	}
}
