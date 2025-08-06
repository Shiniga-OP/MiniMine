package com.engine;

import java.util.ArrayList;
import org.json.JSONObject;
import org.json.JSONArray;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

public class Modelador {
	public static float[] criarAgua(float x, float y, float z, int nivel, int dir) {
		float alturaTopoBase = y + 1.0f - nivel * 0.125f;
		// inclinacao topo conforme dir bitflag(n=1, s=2, l=4, o=8)
		float ajusteN = ((dir & 1) != 0) ? -0.2f : 0f;
		float ajusteS = ((dir & 2) != 0) ? -0.2f : 0f;
		float ajusteL = ((dir & 4) != 0) ? -0.2f : 0f;
		float ajusteO = ((dir & 8) != 0) ? -0.2f : 0f;
		float hN = alturaTopoBase + ajusteN;
		float hS = alturaTopoBase + ajusteS;
		float hL = alturaTopoBase + ajusteL;
		float hO = alturaTopoBase + ajusteO;
		// garante >= y(não desce abaixo da base)
		hN = Math.max(y, hN);
		hS = Math.max(y, hS);
		hL = Math.max(y, hL);
		hO = Math.max(y, hO);
		return new float[] {
			// face de frente(z+1)
			x, y,  z+1,
			x+1, y,  z+1,
			x+1, hN, z+1,
			x, y,  z+1,
			x+1, hN, z+1,
			x,  hS, z+1,
			// face de tras(z)
			x, y,  z,
			x+1, y,  z,
			x+1, hL, z,
			x, y,  z,
			x+1, hL, z,
			x, hO, z,
			// face de cima(topo)
			x, hO, z,
			x+1, hL, z,
			x+1, hN, z+1,
			x, hO, z,
			x+1, hN, z+1,
			x, hS, z+1,
			// face de baixo
			x,    y,  z,
			x+1,  y,  z,
			x+1,  y,  z+1,
			x, y,  z,
			x+1,  y,  z+1,
			x, y,  z+1,
			// face esquerda(x)
			x, y,  z,
			x, hO, z,
			x, hS, z+1,
			x, y,  z,
			x, hS, z+1,
			x, y,  z+1,
			// face direita(x+1)
			x+1, y,  z,
			x+1, hL, z,
			x+1, hN, z+1,
			x+1, y,  z,
			x+1, hN, z+1,
			x+1, y, z+1
		};
	}
	public static float[] criarBloco(float x, float y, float z) {
        return new float[] {
            // face de frente
            x, y, z+1, x+1, y, z+1, x+1, y+1, z+1,
            x, y, z+1, x+1, y+1, z+1, x, y+1, z+1,
            // face de tras
            x, y, z, x+1, y, z, x+1, y+1, z,
            x, y, z, x+1, y+1, z, x, y+1, z,
            // face de cima
            x, y+1, z, x+1, y+1, z, x+1, y+1, z+1,
            x, y+1, z, x+1, y+1, z+1, x, y+1, z+1,
            // face de baixo
            x, y, z, x+1, y, z, x+1, y, z+1,
            x, y, z, x+1, y, z+1, x, y, z+1,
            // face esquerda
            x, y, z, x, y+1, z, x, y+1, z+1,
            x, y, z, x, y+1, z+1, x, y, z+1,
            // face direita
            x+1, y, z, x+1, y+1, z, x+1, y+1, z+1,
            x+1, y, z, x+1, y+1, z+1, x+1, y, z+1
        };
    }
	
	public static float[] juntar(float[]... arrs) {
		int total = 0;
		for(float[] a : arrs) total += a.length;
		float[] m = new float[total];
		int i = 0;
		for(float[] a : arrs) {
			System.arraycopy(a, 0, m, i, a.length);
			i += a.length;
		}
		return m;
	}
	
	public static VBOGrupo carregarModelo(String json, int textureId) {
		ArrayList<Float> vertices = new ArrayList<>();
		ArrayList<Short> indices = new ArrayList<>();
		short globalIdc = 0;
		try {
			JSONObject root = new JSONObject(json);
			JSONObject geometry = root.getJSONObject("modelo");
			int texWidth = geometry.getInt("texV");
			int texHeight = geometry.getInt("texH");

			JSONArray ossos = geometry.getJSONArray("ossos");
			for(int i = 0; i < ossos.length(); i++) {
				JSONObject osso = ossos.getJSONObject(i);
				if(osso.has("cubos")) {
					JSONArray cubes = osso.getJSONArray("cubos");
					for(int j = 0; j < cubes.length(); j++) {
						JSONObject cubo = cubes.getJSONObject(j);
						JSONArray pos = cubo.getJSONArray("pos");
						float ox = (float) pos.getDouble(0);
						float oy = (float) pos.getDouble(1);
						float oz = (float) pos.getDouble(2);
						JSONArray tam = cubo.getJSONArray("tam");
						float w = (float) tam.getDouble(0);
						float h = (float) tam.getDouble(1);
						float d = (float) tam.getDouble(2);
						JSONArray uv = cubo.getJSONArray("uv");
						float u = (float) uv.getDouble(0);
						float v = (float) uv.getDouble(1);
						float inflar = cubo.has("inflar") ? (float) cubo.getDouble("inflar") : 0f;
						// ajusta cordenadas com inflate
						float x0 = ox - inflar;
						float y0 = oy - inflar;
						float z0 = oz - inflar;
						float x1 = ox + w + inflar;
						float y1 = oy + h + inflar;
						float z1 = oz + d + inflar;
						// cordenadas UV normalizadas
						float u0 = u / texWidth;
						float v0 = 1.0f - (v + h) / texHeight;
						float u1 = (u + w) / texWidth;
						float v1 = 1.0f - v / texHeight;
						// gera vertices e indices para cada face
						String[] faces = {"frente", "tras", "direita", "esquerda", "cima", "baixo"};
						for(String face : faces) {
							float[][] faceVerts = obterFaceVertices(face, x0, y0, z0, x1, y1, z1);
							float[] normal = obterFaceNormal(face);
							float[][] uvs = obterFaceUVs(face, u0, v0, u1, v1);
							// adiciona vertices da face
							for(int k = 0; k < 4; k++) {
								// posicao
								vertices.add(faceVerts[k][0]);
								vertices.add(faceVerts[k][1]);
								vertices.add(faceVerts[k][2]);
								// normal
								vertices.add(normal[0]);
								vertices.add(normal[1]);
								vertices.add(normal[2]);
								// UV
								vertices.add(uvs[k][0]);
								vertices.add(uvs[k][1]);
							}
							// adiciona indices da face(2 triangulos)
							indices.add(globalIdc);
							indices.add((short) (globalIdc + 1));
							indices.add((short) (globalIdc + 2));
							indices.add(globalIdc);
							indices.add((short) (globalIdc+ 2));
							indices.add((short) (globalIdc + 3));
							globalIdc += 4;
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		// converte listas para buffers
		FloatBuffer vertBuffer = GL.criarFloatBuffer(vertices.size());
		for(float f : vertices) vertBuffer.put(f);
		vertBuffer.position(0);

		ShortBuffer idcBuffer = GL.criarShortBuffer(indices.size());
		for(short s : indices) idcBuffer.put(s);
		idcBuffer.position(0);

		int vboId = GL.gerarVBO(vertBuffer);
		int iboId = GL.gerarIBO(idcBuffer);

		return new VBOGrupo(
			textureId,
			vboId,
			iboId,
			indices.size() // numero de vertices
		);
	}

	private static float[][] obterFaceVertices(String face, float x0, float y0, float z0, float x1, float y1, float z1) {
		switch (face) {
			case "frente": // z1
				return new float[][]{
					{x0, y0, z1}, {x0, y1, z1}, {x1, y1, z1}, {x1, y0, z1}
				};
			case "tras": // z0
				return new float[][]{
					{x0, y0, z0}, {x1, y0, z0}, {x1, y1, z0}, {x0, y1, z0}
				};
			case "direita": // x1
				return new float[][]{
					{x1, y0, z0}, {x1, y0, z1}, {x1, y1, z1}, {x1, y1, z0}
				};
			case "esquerda": // x0
				return new float[][]{
					{x0, y0, z0}, {x0, y1, z0}, {x0, y1, z1}, {x0, y0, z1}
				};
			case "cima": // y1
				return new float[][]{
					{x0, y1, z0}, {x0, y1, z1}, {x1, y1, z1}, {x1, y1, z0}
				};
			case "baixo": // y0
				return new float[][]{
					{x0, y0, z0}, {x1, y0, z0}, {x1, y0, z1}, {x0, y0, z1}
				};
			default:
				return new float[4][3];
		}
	}

	private static float[] obterFaceNormal(String face) {
		switch (face) {
			case "frente": return new float[]{0, 0, 1};
			case "tras": return new float[]{0, 0, -1};
			case "direita": return new float[]{1, 0, 0};
			case "esquerda": return new float[]{-1, 0, 0};
			case "cima": return new float[]{0, 1, 0};
			case "baixo": return new float[]{0, -1, 0};
			default: return new float[]{0, 0, 0};
		}
	}

	private static float[][] obterFaceUVs(String face, float u0, float v0, float u1, float v1) {
		// mapeamento UV padrao para todas as faces
		return new float[][]{
			{u0, v1}, // Inferior esquerdo
			{u0, v0}, // Superior esquerdo
			{u1, v0}, // Superior direito
			{u1, v1}  // Inferior direito
		};
	}
}
