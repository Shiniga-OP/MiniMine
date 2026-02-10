package com.minimine.mundo.blocos;

import com.minimine.utils.arrays.FloatArrayUtil;
import com.minimine.utils.arrays.ShortArrayUtil;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.ObjectIntMap;
import com.minimine.mundo.ChunkLuz;
import com.minimine.graficos.Texturas;

public class BlocoModelo {
    public static final float TAM = 1f;

    // Reduzido de 10 para 7 floats por vértice!
    // Layout: X, Y, Z (3) + U, V (2) + TexID (1) + Cor (1)
    public static final int FLOATS_POR_VERTICE = 7; 

    // Cache para mapear nomes -> IDs numéricos para o Shader
    private static final ObjectIntMap<String> mapaTexturas = new ObjectIntMap<>();
    // Array plano para enviar como Uniform (4 floats por textura: u, v, u2, v2)
    // Suporta até 256 texturas únicas por enquanto.
    public static final float[] dadosAtlas = new float[256 * 4]; 
    private static int contagemTexturas = 0;

    public static final float[][][] FACE_VERTICES = {
        {{TAM, TAM, 0}, {0, TAM, 0}, {0, TAM, TAM}, {TAM, TAM, TAM}}, // topo
        {{TAM, 0, TAM}, {0, 0, TAM}, {0, 0, 0}, {TAM, 0, 0}},         // baixo
        {{TAM, 0, TAM}, {TAM, 0, 0}, {TAM, TAM, 0}, {TAM, TAM, TAM}}, // +X
        {{0, 0, 0}, {0, 0, TAM}, {0, TAM, TAM}, {0, TAM, 0}},         // -X
        {{0, TAM, TAM}, {0, 0, TAM}, {TAM, 0, TAM}, {TAM, TAM, TAM}}, // +Z
        {{0, 0, 0}, {0, TAM, 0}, {TAM, TAM, 0}, {TAM, 0, 0}}          // -Z
    };

    public static final float[][][] FACE_UVS = {
        {{1,1}, {0,1}, {0,0}, {1,0}}, 
        {{1,0}, {0,0}, {0,1}, {1,1}}, 
        {{1,1}, {0,1}, {0,0}, {1,0}}, 
        {{1,1}, {0,1}, {0,0}, {1,0}}, 
        {{0,0}, {0,1}, {1,1}, {1,0}}, 
        {{0,1}, {0,0}, {1,0}, {1,1}}  
    };

    /**
     * Obtém ou cria um ID para a textura e preenche o buffer de dados do atlas.
     */
    public static float obterIdTextura(String nome) {
        if(mapaTexturas.containsKey(nome)) {
            return (float) mapaTexturas.get(nome, 0);
        }

        TextureRegion region = Texturas.atlas.get(nome);
        if(region == null) return 0f; // Textura missing, usa ID 0 ou trata erro

        int id = contagemTexturas;
        if(id >= 256) return 0f; // Limite de segurança do array/shader

        mapaTexturas.put(nome, id);

        // Preenche os dados que o Shader vai ler (uMin, vMin, uMax, vMax)
        // OBS: Usamos region.getU() etc diretamente do libGDX
        int idx = id * 4;
        dadosAtlas[idx]     = region.getU();
        dadosAtlas[idx + 1] = region.getV();
        dadosAtlas[idx + 2] = region.getU2();
        dadosAtlas[idx + 3] = region.getV2();

        contagemTexturas++;
        return (float) id;
    }

    public static void addFace(int faceId, String texturaNome, float x, float y, float z, 
							   float h, float v, float luzBloco, float luzSol, FloatArrayUtil verts, ShortArrayUtil idc) {

        // Pega o ID numérico (0 a 255) em vez de passar coordenadas brutas
        float texId = obterIdTextura(texturaNome);

        float multFace = ChunkLuz.FACE_LUZ[faceId];
        int r = (int)(luzBloco * multFace * 255);
        int g = (int)(luzSol * multFace * 255);
        int b = (int)(multFace * 255); 
        float corFinal = Color.toFloatBits(r, g, b, 255);

        // Atualizado para usar a constante correta
        short indiceBase = (short)(verts.tam / FLOATS_POR_VERTICE); 

        float sx = 1f, sy = 1f, sz = 1f; 
        float uh = 1f, vv = 1f; 

        switch(faceId) {
            case 0: case 1: sx = h; sz = v; uh = h; vv = v; break;
            case 2: case 3: sz = h; sy = v; uh = h; vv = v; break;
            case 4: case 5: sx = h; sy = v; uh = h; vv = v; break;
        }

        for(int i = 0; i < 4; i++) {
            float[] vert = FACE_VERTICES[faceId][i];
            float[] uv = FACE_UVS[faceId][i];

            // Posicao (3 floats)
            verts.add(x + vert[0] * sx);
            verts.add(y + vert[1] * sy);
            verts.add(z + vert[2] * sz);

            // UV Local com Tiling do Guloso (2 floats)
            // O shader vai usar fract() nisso aqui
            verts.add(uv[0] * uh); 
            verts.add(uv[1] * vv);

            // Texture ID (1 float) - SUBSTITUI OS 4 FLOATS DE ANTES
            verts.add(texId);

            // Cor (1 float)
            verts.add(corFinal);
        }

        idc.add(indiceBase);
        idc.add((short)(indiceBase + 1));
        idc.add((short)(indiceBase + 2));
        idc.add((short)(indiceBase + 2));
        idc.add((short)(indiceBase + 3));
        idc.add(indiceBase);
    }
}
