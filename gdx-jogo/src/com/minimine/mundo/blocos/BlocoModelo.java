package com.minimine.mundo.blocos;

import com.minimine.utils.arrays.FloatArrayUtil;
import com.minimine.utils.arrays.ShortArrayUtil;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.ObjectIntMap;
import com.minimine.graficos.Texturas;

public class BlocoModelo {
    public static final float TAM = 1f;

    // pos(1) + U, V(2) + texID(1) + cor(1)
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
            return (float)mapaTexturas.get(nome, 0);
        }
        TextureRegion regiao = Texturas.atlas.get(nome);
        if(regiao == null) return 0f; // textura faltando, usa ID 0 ou trata erro

        final int id = contaTexturas;
        if(id >= 256) return 0f; // limite de segurança do array/shader

        mapaTexturas.put(nome, id);

        // preenche os dados que o shader vai ler(uMin, vMin, uMax, vMax)
        final int idc = id << 2;
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

    public void addFace(int faceId, String texturaNome, float x, float y, float z, 
	float h, float v, float luzBloco, float luzSol, FloatArrayUtil verts, ShortArrayUtil idc) {}
}
