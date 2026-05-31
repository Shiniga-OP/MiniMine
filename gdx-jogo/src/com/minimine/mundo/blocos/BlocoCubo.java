package com.minimine.mundo.blocos;

import com.minimine.utils.arrays.FloatArrayUtil;
import com.minimine.utils.arrays.ShortArrayUtil;
import com.badlogic.gdx.graphics.Color;

public class BlocoCubo extends BlocoModelo {
	@Override
	public void addFace(int faceId, String texturaNome, float x, float y, float z, 
	float h, float v, float luzBloco, float luzSol, FloatArrayUtil verts, ShortArrayUtil idc) {
        // pega o ID numerico(0 a 255) em vez de passar coordenadas brutas
        float texId = obterIdTextura(texturaNome);

        int r = (int)(luzBloco * 255);
        int g = (int)(luzSol * 255);
        int b = 255;
        float corFinal = Color.toFloatBits(r, g, b, 255);

        // atualizado para usar a constante correta
        short idcBase = (short)(verts.tam / FLOATS_VERTICE); 

        float sx = 1f, sy = 1f, sz = 1f; 
        float uh = 1f, vv = 1f; 

        switch(faceId) {
            case 0: case 1: sx = h; sz = v; uh = h; vv = v; break;
            case 2: case 3: sz = h; sy = v; uh = h; vv = v; break;
            case 4: case 5: sx = h; sy = v; uh = h; vv = v; break;
        }
		// face 1
        float[] vert = FACE_VERTICES[faceId][0];
		float[] uv = FACE_UVS[faceId][0];
		// === compacta a posição ===
		int px = (int)(x + vert[0] * sx);
		int py = (int)(y + vert[1] * sy);
		int pz = (int)(z + vert[2] * sz);
		int posCompactada = compactarPosicao(px, py, pz);

        verts.add((float)posCompactada);

		// UV local com tiling do Guloso(2 floats)
		verts.add(uv[0] * uh); 
		verts.add(uv[1] * vv);
		// textura ID(1 float)
        verts.add(texId);
        // cor(1 float)
        verts.add(corFinal);

		// face 2
		vert = FACE_VERTICES[faceId][1];
		uv = FACE_UVS[faceId][1];

		px = (int)(x + vert[0] * sx);
		py = (int)(y + vert[1] * sy);
		pz = (int)(z + vert[2] * sz);
		posCompactada = compactarPosicao(px, py, pz);

        verts.add((float)posCompactada);

		// UV local com tiling do Guloso(2 floats)
		verts.add(uv[0] * uh); 
		verts.add(uv[1] * vv);
		// textura ID(1 float)
        verts.add(texId);
        // cor(1 float)
        verts.add(corFinal);

		// face 3
		vert = FACE_VERTICES[faceId][2];
		uv = FACE_UVS[faceId][2];

		px = (int)(x + vert[0] * sx);
		py = (int)(y + vert[1] * sy);
		pz = (int)(z + vert[2] * sz);
		posCompactada = compactarPosicao(px, py, pz);

        verts.add((float)posCompactada);

		// UV local com tiling do Guloso(2 floats)
		verts.add(uv[0] * uh); 
		verts.add(uv[1] * vv);
		// textura ID(1 float)
        verts.add(texId);
        // cor(1 float)
        verts.add(corFinal);

		// face 4
		vert = FACE_VERTICES[faceId][3];
		uv = FACE_UVS[faceId][3];

		px = (int)(x + vert[0] * sx);
		py = (int)(y + vert[1] * sy);
		pz = (int)(z + vert[2] * sz);
		posCompactada = compactarPosicao(px, py, pz);

        verts.add((float)posCompactada);

		// UV local com tiling do Guloso(2 floats)
		verts.add(uv[0] * uh); 
		verts.add(uv[1] * vv);
		// textura ID(1 float)
        verts.add(texId);
        // cor(1 float)
        verts.add(corFinal);

        idc.add(idcBase);
        idc.add((short)(idcBase + 1));
        idc.add((short)(idcBase + 2));
        idc.add((short)(idcBase + 2));
        idc.add((short)(idcBase + 3));
        idc.add(idcBase);
    }
}
