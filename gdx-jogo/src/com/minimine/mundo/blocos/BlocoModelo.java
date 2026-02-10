package com.minimine.mundo.blocos;

import com.minimine.utils.arrays.FloatArrayUtil;
import com.minimine.utils.arrays.ShortArrayUtil;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.minimine.mundo.ChunkLuz;
import com.minimine.graficos.Texturas;

public class BlocoModelo {
    public static final float TAM = 1f; // tamanho
    // definições das faces(vertices + UVs)
    public static final float[][][] FACE_VERTICES = {
        // topo
        {
            {TAM, TAM, 0},
            {0, TAM, 0},
            {0, TAM, TAM},
            {TAM, TAM, TAM}
        },
        // baixo
        {
            {TAM, 0, TAM},
            {0, 0, TAM},
            {0, 0, 0},
            {TAM, 0, 0}
        },
        // +X
        {
            {TAM, 0, TAM},
            {TAM, 0, 0},
            {TAM, TAM, 0},
            {TAM, TAM, TAM}
        },
        // -X
        {
            {0, 0, 0},
            {0, 0, TAM},
            {0, TAM, TAM},
            {0, TAM, 0}
        },
        // +Z
        {
            {0, TAM, TAM},
            {0, 0, TAM},
            {TAM, 0, TAM},
            {TAM, TAM, TAM}
        },
        // -Z
        {
            {0, 0, 0},
            {0, TAM, 0},
            {TAM, TAM, 0},
            {TAM, 0, 0}
        }
    };
    public static final float[][][] FACE_UVS = {
        {{1,1}, {0,1}, {0,0}, {1,0}}, // topo
        {{1,0}, {0,0}, {0,1}, {1,1}}, // baixo
        {{1,1}, {0,1}, {0,0}, {1,0}}, // +X
        {{1,1}, {0,1}, {0,0}, {1,0}}, // -X
        {{0,0}, {0,1}, {1,1}, {1,0}}, // +Z
        {{0,1}, {0,0}, {1,0}, {1,1}}  // -Z
    };

    public static void addFace(int faceId, String texturaNome, float x, float y, float z, 
	float h, float v, float luzBloco, float luzSol, FloatArrayUtil verts, ShortArrayUtil idc) {
        // obtem a TextureRegion do atlas usando o nome
        TextureRegion region = Texturas.atlas.obter(texturaNome);

        if(region == null) return;

        // Calcula as coordenadas UV normalizadas do atlas
        float uMin = region.getU();
        float vMin = region.getV();
        float uMax = region.getU2();
        float vMax = region.getV2();

        // pre-calculo da cor pra evitar chamar Color.toFloatBits
        float multFace = ChunkLuz.FACE_LUZ[faceId];
        int r = (int)(luzBloco * multFace * 255);
        int g = (int)(luzSol * multFace * 255);
        int b = (int)(multFace * 255); 
        float corFinal = Color.toFloatBits(r, g, b, 255);

        short indiceBase = (short)(verts.tam / 10); // 10 floats por vertice

        // define escalas baseadas na face
        float sx = 1f, sy = 1f, sz = 1f; // escalas de posicao
        float uh = 1f, vv = 1f; // escalas de UV(h = horizontal, v = vertical)

        // mapeamento:
        // topo/baixo(faces 0, 1): h -> X, v -> Z
        // lados X(faces 2, 3): h -> Z, v -> Y
        // lados Z(faces 4, 5): h -> X, v -> Y
        switch(faceId) {
            case 0: case 1: sx = h; sz = v; uh = h; vv = v; break;
            case 2: case 3: sz = h; sy = v; uh = h; vv = v; break;
            case 4: case 5: sx = h; sy = v; uh = h; vv = v; break;
        }
        // loop pelos 4 vertices
        for(int i = 0; i < 4; i++) {
            float[] vert = FACE_VERTICES[faceId][i];
            float[] uv = FACE_UVS[faceId][i];

            // posicao
            verts.add(x + vert[0] * sx);
            verts.add(y + vert[1] * sy);
            verts.add(z + vert[2] * sz);

            // UV local(pro guloso)
            verts.add(uv[0] * uh); 
            verts.add(uv[1] * vv);
            // Atlas Limites (uMin, vMin, uMax, vMax)
            verts.add(uMin); verts.add(vMin); verts.add(uMax); verts.add(vMax);
            // cor
            verts.add(corFinal);
        }
        // indices(ordem dos triangulos)
        idc.add(indiceBase);
        idc.add((short)(indiceBase + 1));
        idc.add((short)(indiceBase + 2));
        idc.add((short)(indiceBase + 2));
        idc.add((short)(indiceBase + 3));
        idc.add(indiceBase);
    }
}

