package com.minimine.mundo;

import com.minimine.utils.arrays.FloatArrayUtil;
import com.minimine.utils.arrays.ShortArrayUtil;
import com.minimine.mundo.blocos.Bloco;
import com.minimine.mundo.blocos.BlocoModelo;

public class ChunkMalha {
    public static void attMalha(Chunk chunk, FloatArrayUtil verts, ShortArrayUtil idcSolidos, ShortArrayUtil idcTransp) {
        ChunkLuz.attLuz(chunk);

        Chunk cXP, cXN, cZP, cZN;
        cXP = Mundo.chunks.get(Chave.calcularChave(chunk.x + 1, chunk.z));
        cXN = Mundo.chunks.get(Chave.calcularChave(chunk.x - 1, chunk.z));
        cZP = Mundo.chunks.get(Chave.calcularChave(chunk.x, chunk.z + 1));
        cZN = Mundo.chunks.get(Chave.calcularChave(chunk.x, chunk.z - 1));

        // === gera malha de renderização(O Guloso) ===
        
        // 1. eixo Y(faces cima/baixo)
        int[] mascara = new int[16 * 16];
        for(boolean cima : new boolean[]{true, false}) { // passada pra cima, depois baixo
            for(int y = 0; y < Mundo.Y_CHUNK; y++) {
                int n = 0;
                for(int z = 0; z < 16; z++) {
                    for(int x = 0; x < 16; x++) {
                        int id = ChunkUtil.obterBloco(x, y, z, chunk);
                        int val = 0;
                        if(id != 0) {
                            Bloco b = Bloco.numIds.get(id);
                            if(b != null) {
                                int ny = cima ? y + 1 : y - 1;
                                int vizId = 0;
                                if(ny >= 0 && ny < Mundo.Y_CHUNK) {
                                    vizId = ChunkUtil.obterBloco(x, ny, z, chunk);
                                } else { 
                                    vizId = 0;
                                }
                                Bloco bViz = (vizId == 0) ? null : Bloco.numIds.get(vizId);
                                
                                if(deveRenderFace(b, bViz)) {
                                    byte luz = ChunkUtil.obterLuzCompleta(x, (ny < 0 || ny >= Mundo.Y_CHUNK) ? y : ny, z, chunk);
                                    val = (id << 8) | (luz & 0xFF);
                                }
                            }
                        }
                        mascara[n++] = val;
                    }
                }
                malhaPlana(mascara, 16, 16, y, cima ? 0 : 1, chunk, verts, idcSolidos, idcTransp);
            }
        }
        // 2. eixo X(faces leste/oeste)
        mascara = new int[16 * Mundo.Y_CHUNK];
        for(boolean leste : new boolean[]{true, false}) {
            for(int x = 0; x < 16; x++) {
                int n = 0;
                for(int y = 0; y < Mundo.Y_CHUNK; y++) {
                    for(int z = 0; z < 16; z++) {
                        int id = ChunkUtil.obterBloco(x, y, z, chunk);
                        int val = 0;
                        if(id != 0) {
                            Bloco b = Bloco.numIds.get(id);
                            if(b != null) {
                                int nx = leste ? x + 1 : x - 1;
                                int vizId = 0;
                                Chunk tC = chunk;
                                int tx = nx;
                                
                                if(nx >= 16) { tC = cXP; tx = 0; }
                                else if(nx < 0) { tC = cXN; tx = 15; }
                                
                                if(tC != null) vizId = ChunkUtil.obterBloco(tx, y, z, tC);
                                
                                Bloco bViz = (vizId == 0) ? null : Bloco.numIds.get(vizId);
                                if(deveRenderFace(b, bViz)) {
                                    byte luz = (tC != null) ? ChunkUtil.obterLuzCompleta(tx, y, z, tC) : 15;
                                    val = (id << 8) | (luz & 0xFF);
                                }
                            }
                        }
                        mascara[n++] = val;
                    }
                }
                malhaPlana(mascara, 16, Mundo.Y_CHUNK, x, leste ? 2 : 3, chunk, verts, idcSolidos, idcTransp);
            }
        }
        // 3. eixo Z(faces sul/norte)
        for(boolean sul : new boolean[]{true, false}) {
            for(int z = 0; z < 16; z++) {
                int n = 0;
                for(int y = 0; y < Mundo.Y_CHUNK; y++) {
                    for (int x = 0; x < 16; x++) {
                        int id = ChunkUtil.obterBloco(x, y, z, chunk);
                        int val = 0;
                        if(id != 0) {
                            Bloco b = Bloco.numIds.get(id);
                            if(b != null) {
                                int nz = sul ? z + 1 : z - 1;
                                int vizId = 0;
                                Chunk tC = chunk;
                                int tz = nz;
                                
                                if(nz >= 16) { tC = cZP; tz = 0; }
                                else if(nz < 0) { tC = cZN; tz = 15; }
                                
                                if(tC != null) vizId = ChunkUtil.obterBloco(x, y, tz, tC);
                                
                                Bloco bViz = (vizId == 0) ? null : Bloco.numIds.get(vizId);
                                if(deveRenderFace(b, bViz)) {
                                    byte luz = (tC != null) ? ChunkUtil.obterLuzCompleta(x, y, tz, tC) : 15;
                                    val = (id << 8) | (luz & 0xFF);
                                }
                            }
                        }
                        mascara[n++] = val;
                    }
                }
                malhaPlana(mascara, 16, Mundo.Y_CHUNK, z, sul ? 4 : 5, chunk, verts, idcSolidos, idcTransp);
            }
        }
    }

    private static void malhaPlana(int[] mascara, int largura, int altura, 
    int profundidade, int faceId, Chunk chunk,
    FloatArrayUtil verts, ShortArrayUtil idcSolidos, ShortArrayUtil idcTransp) {
        int n = 0;
        for(int j = 0; j < altura; j++) {
            for(int i = 0; i < largura; ) {
                int val = mascara[n];
                if(val != 0) {
                    // encontrou inicio de face
                    int v = 1;
                    while(i + v < largura && mascara[n + v] == val) {
                        v++;
                    }
                    int h = 1;
                    boolean continua = true;
                    while(j + h < altura && continua) {
                        for(int k = 0; k < v; k++) {
                            if(mascara[n + k + h * largura] != val) {
                                continua = false;
                                break;
                            }
                        }
                        if(continua) h++;
                    }
                    // limpar mascara
                    for(int l = 0; l < h; l++) {
                        for(int k = 0; k < v; k++) {
                            mascara[n + k + l * largura] = 0;
                        }
                    }
                    // dados da face
                    int id = val >> 8;
                    int luzTotal = val & 0xFF;
                    float lb = (luzTotal & 0x0F) / 15f;
                    float ls = ((luzTotal >> 4) & 0x0F) / 15f;
                    
                    Bloco b = Bloco.numIds.get(id);
                    float x = 0, y = 0, z = 0;
                    float fv = 0, fh = 0;
                    
                    switch(faceId) {
                        case 0: case 1: // Y
                            x = i; z = j; y = profundidade;
                            fv = v; fh = h; 
                            break;
                        case 2: case 3: // X
                            z = i; y = j; x = profundidade;
                            fv = v; fh = h;
                            break;
                        case 4: case 5: // Z
                            x = i; y = j; z = profundidade;
                            fv = v; fh = h;
                            break;
                    }
                    ShortArrayUtil lista = b.transparente ? idcTransp : idcSolidos;
                    BlocoModelo.addFace(faceId, b.texturaId(faceId), x, y, z, fv, fh, lb, ls, verts, lista);

                    i += v;
                    n += v;
                } else {
                    i++;
                    n++;
                }
            }
        }
    }
    
    public static boolean deveRenderFace(Bloco atual, Bloco vizinho) {
        if(vizinho == null) return true;
        if(atual.tipo == vizinho.tipo) return false;
        if(!vizinho.transparente) return false;
        return true;
    }
}
