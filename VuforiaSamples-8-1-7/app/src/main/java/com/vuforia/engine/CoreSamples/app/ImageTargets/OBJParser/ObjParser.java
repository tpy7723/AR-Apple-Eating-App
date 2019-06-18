package com.vuforia.engine.CoreSamples.app.ImageTargets.OBJParser;

import android.content.Context;

import java.io.IOException;
import java.util.ArrayList;

public class ObjParser extends Parser {
	/* Vertices, texture vertices, normal vertices를 저장할 ArrayList들 */
	private ArrayList<Float[]> verticePoints, texturePoints, normalPoints;
	private ArrayList<Integer[]> verticeIndices;

	/* Normal & texture vertices가 있는지 확인하기 위함 */
	public boolean normal_flag = false;
	public boolean texture_flag = true;

	/* 생성자, ArrayList들 초기화 */
	public ObjParser(Context context) {
		super(context);
		this.verticePoints = new ArrayList<>();
		this.texturePoints = new ArrayList<>();
		this.normalPoints = new ArrayList<>();
		this.verticeIndices = new ArrayList<>();
	}

	@Override
	public void parse(int resId) throws IOException {
		super.parse(resId);
		String line;
		
		    while((line = this.file.readLine()) != null) {
		    	if(line.matches("^#.*")) {
		    		continue;
		    	}

		    	String[] chunks = line.split(" ");
		    	String cmd = chunks[0].toLowerCase();

		    	/* Object */
		    	if(cmd.equals("o")){
		    		this.addObjId(chunks[1]);
		    	}
		    	/* Vertices */
		    	else if (cmd.equals("v")) {
		    		Float[] vPoint = {
	    				Float.parseFloat(chunks[1]),
	    				Float.parseFloat(chunks[2]),
	    				Float.parseFloat(chunks[3])
					};
		    		this.verticePoints.add(vPoint);
		    	}
		    	/* Normal vertices */
		    	else if (cmd.equals("vn")) {
		    		Float[] nPoint = {
	    				Float.parseFloat(chunks[1]),
	    				Float.parseFloat(chunks[2]),
	    				Float.parseFloat(chunks[3])
					};
		    		this.normalPoints.add(nPoint);
		    	}
		    	/* Texture vertices */
		    	else if (cmd.equals("vt")) {
		    		Float[] tPoint = {
	    				Float.parseFloat(chunks[1]),
	    				Float.parseFloat(chunks[2])
					};
		    		this.texturePoints.add(tPoint);
		    	}
		    	/* Face */
		    	else if (cmd.equals("f")) {
					short[] vps = new short[3];

		    		for(int i = 1; i < chunks.length; i++) {
						String[] bits = chunks[i].split("/");

						switch(bits.length) {
							case 3:
								int np;

								try {
									np = Integer.parseInt(bits[2]) - 1;
								}
								catch(NumberFormatException e) {
									np = 0;
								}

								this.addNormal(this.normalPoints.get(np));
								normal_flag = true;

							case 2:
								int tp;

								try {
										tp = Integer.parseInt(bits[1]) - 1;
								}
								catch(NumberFormatException e) {
										tp = 0;
								}
								try {
										this.addTexture(this.texturePoints.get(tp));
								}
								catch(IndexOutOfBoundsException e) {
										texture_flag = false;
								}

							case 1:
								int vp;

								try {
									vps[i - 1] = (short) Integer.parseInt(bits[0]);
									vp = Integer.parseInt(bits[0]) - 1;
								}
								catch(NumberFormatException e) {
									vp = 0;
								}

								this.addVertice(this.verticePoints.get(vp));
						}
					}

					Short[] indices = {vps[0], vps[1], vps[2]};
					this.addIndice(indices);
				}
		    	/* MTL stuffs (currently not used) */
		    	else if(cmd.equals("usemtl")) {
		    		/*String file = chunks[1];
		    		//sometimes file names lead with a _ for no good reason, trim it
		    		if(file.startsWith("_"))
		    		{
		    			file = file.substring(1);
		    		}
		    		
		    		this.addTexturefile(file);*/
		    	}
		    	else {
		    		continue;
		    	}
			}
	}
}
