package com.minimine.mundo.blocos;
import com.minimine.utils.arrays.FloatArrayUtil;
import com.minimine.utils.arrays.ShortArrayUtil;
import com.badlogic.gdx.graphics.Color;

public class BlocoX extends BlocoModelo {
	/*
     * gera modelo em X para vegetação(capim, flores)
     * dois quads diagonais, cada um renderizado dos dois lados
     * usa os vertices inteiros das quinas do bloco, o empacotamento atual não suporta frações
     * registra na lista transparente
     */
	 @Override
    public void addFace(int faceId, String texturaNome, float x, float y, float z,
	float h, float v2, float luzBloco, float luzSol, FloatArrayUtil verts, ShortArrayUtil idc) {
        float texId = obterIdTextura(texturaNome);

        // modeloX
        int r = (int)(luzBloco * 255);
        int g = (int)(luzSol  * 255);
        int b = 255;
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
