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

    // Pos(1) + U, V(2) + TexID(1) + Cor(1)
    public static final int FLOATS_VERTICE = 5;

    // cache pra mapear nomes -> IDs numericos pro shader
    public static final ObjectIntMap<String> mapaTexturas = new ObjectIntMap<>();
    // array plano pra enviar como Uniform(4 floats por textura: u, v, u2, v2)
    // suporta até 256 texturas unicas por enquanto.
    public static final float[] dadosAtlas = new float[256 * 4]; 
    public static int contaTexturas = 0;

    public static final float[][][] FACE_VERTICES = {
        {{TAM, TAM, 0}, {0, TAM, 0}, {0, TAM, TAM}, {TAM, TAM, TAM}}, // topo
        {{TAM, 0, TAM}, {0, 0, TAM}, {0, 0, 0}, {TAM, 0, 0}}, // baixo
        {{TAM, 0, TAM}, {TAM, 0, 0}, {TAM, TAM, 0}, {TAM, TAM, TAM}}, // +X
        {{0, 0, 0}, {0, 0, TAM}, {0, TAM, TAM}, {0, TAM, 0}}, // -X
        {{0, TAM, TAM}, {0, 0, TAM}, {TAM, 0, TAM}, {TAM, TAM, TAM}}, // +Z
        {{0, 0, 0}, {0, TAM, 0}, {TAM, TAM, 0}, {TAM, 0, 0}} // -Z
    };
    public static final float[][][] FACE_UVS = {
        {{1,1}, {0,1}, {0,0}, {1,0}}, 
        {{1,0}, {0,0}, {0,1}, {1,1}}, 
        {{1,1}, {0,1}, {0,0}, {1,0}}, 
        {{1,1}, {0,1}, {0,0}, {1,0}}, 
        {{0,0}, {0,1}, {1,1}, {1,0}}, 
        {{0,1}, {0,0}, {1,0}, {1,1}}  
    };

    // obtem ou cria um ID para a textura e preenche o buffer de dados do atlas
    public static float obterIdTextura(String nome) {
        if(mapaTexturas.containsKey(nome)) {
            return (float) mapaTexturas.get(nome, 0);
        }
        TextureRegion regiao = Texturas.atlas.get(nome);
        if(regiao == null) return 0f; // textura faltando, usa ID 0 ou trata erro

        int id = contaTexturas;
        if(id >= 256) return 0f; // limite de segurança do array/shader

        mapaTexturas.put(nome, id);

        // preenche os dados que o shader vai ler(uMin, vMin, uMax, vMax)
        int idc = id * 4;
        dadosAtlas[idc] = regiao.getU();
        dadosAtlas[idc + 1] = regiao.getV();
        dadosAtlas[idc + 2] = regiao.getU2();
        dadosAtlas[idc + 3] = regiao.getV2();

        contaTexturas++;
        return (float)id;
    }

    /*
     * compacta posição XYZ em um unico int usando bit pacote
     * [5 bits X][9 bits Y][5 bits Z][13 bits livres]
     * 
     * X: 0-31(5 bits, pos 0)
     * Y: 0-511(9 bits, pos 5) 
     * Z: 0-31(5 bits, pos 14)
     */
    public static final int compactarPosicao(int x, int y, int z) {
        return (x & 0x1F) | ((y & 0x1FF) << 5) | ((z & 0x1F) << 14);
    }

    public static void addFace(int faceId, String texturaNome, float x, float y, float z, 
	float h, float v, float luzBloco, float luzSol, FloatArrayUtil verts, ShortArrayUtil idc) {
        // pega o ID numerico(0 a 255) em vez de passar coordenadas brutas
        float texId = obterIdTextura(texturaNome);

        float multFace = ChunkLuz.FACE_LUZ[faceId];
        int r = (int)(luzBloco * multFace * 255);
        int g = (int)(luzSol * multFace * 255);
        int b = (int)(multFace * 255); 
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

    /*
     * gera modelo em X para vegetação(capim, flores)
     * dois quads diagonais, cada um renderizado dos dois lados.
     * usa os vertices inteiros das quinas do bloco, o empacotamento atual não suporta frações
     * registra na lista transparente pois precisa de alpha test/blend no Render.java
     */
    public static void addModeloX(String texturaNome, float x, float y, float z,
	float luzBloco, float luzSol, FloatArrayUtil verts, ShortArrayUtil idc) {
        float texId = obterIdTextura(texturaNome);

        // modeloX usa faceId 2(+X) como referencia de multiplicador de luz lateral
        float multFace = ChunkLuz.FACE_LUZ[2];
        int r = (int)(luzBloco * multFace * 255);
        int g = (int)(luzSol  * multFace * 255);
        int b = (int)(multFace * 255);
        float cor = Color.toFloatBits(r, g, b, 255);

        int ix = (int)x;
        int iy = (int)y;
        int iz = (int)z;

        /*
         * quad A: diagonal / — vai de(x, z+1) até(x+1, z)
         * quad B: diagonal \ — vai de(x, z)   até(x+1, z+1)
         
         * cada quad: 4 vertices, indices gerados duas vezes com orientação invertida
         * atributos de vertice: posCompactada, u, v, texId, cor
         */
        float[][][] quads = {
            {
                // quad A: /
                // px, py, pz, u, v
                {ix,   iy,   iz+1, 0, 1},
                {ix+1, iy,   iz,   1, 1},
                {ix+1, iy+1, iz,   1, 0},
                {ix,   iy+1, iz+1, 0, 0}
            },
            {
                // quad B: \
                {ix,   iy,   iz,   0, 1},
                {ix+1, iy,   iz+1, 1, 1},
                {ix+1, iy+1, iz+1, 1, 0},
                {ix,   iy+1, iz,   0, 0}
            }
        };
        for(float[][] quad : quads) {
            short idcBase = (short)(verts.tam / FLOATS_VERTICE);

            for(float[] v : quad) {
                int posComp = compactarPosicao((int)v[0], (int)v[1], (int)v[2]);
                verts.add((float)posComp);
                verts.add(v[3]); // u
                verts.add(v[4]); // v
                verts.add(texId);
                verts.add(cor);
            }
            // orientação normal(frente)
            idc.add(idcBase);
            idc.add((short)(idcBase + 1));
            idc.add((short)(idcBase + 2));
            idc.add((short)(idcBase + 2));
            idc.add((short)(idcBase + 3));
            idc.add(idcBase);

            // orientação invertido(costas)
            idc.add(idcBase);
            idc.add((short)(idcBase + 3));
            idc.add((short)(idcBase + 2));
            idc.add((short)(idcBase + 2));
            idc.add((short)(idcBase + 1));
            idc.add(idcBase);
        }
    }
}

