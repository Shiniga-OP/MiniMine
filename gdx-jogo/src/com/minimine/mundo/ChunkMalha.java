package com.minimine.mundo;

import com.minimine.utils.arrays.FloatArrayUtil;
import com.minimine.utils.arrays.ShortArrayUtil;
import com.minimine.mundo.blocos.Bloco;
import com.minimine.mundo.blocos.BlocoModelo;

public class ChunkMalha {
    // tamanho maximo de mascara necessaria(eixo X/Z: 16 * Y_CHUNK)
    public static final int MASCARA_MAX = 16 * Mundo.Y_CHUNK;

    // reutiliza array de mascara por thread
    public static final ThreadLocal<int[]> MASCARA_CACHE = new ThreadLocal<int[]>() {
        @Override protected int[] initialValue() { return new int[MASCARA_MAX]; }
    };

    public static void attMalha(Chunk chunk, FloatArrayUtil verts, ShortArrayUtil idcSolidos, ShortArrayUtil idcTransp) {
		Chunk cXP, cXN, cZP, cZN;
        cXP = Mundo.chunks.get(Chave.calcularChave(chunk.x + 1, chunk.z));
        cXN = Mundo.chunks.get(Chave.calcularChave(chunk.x - 1, chunk.z));
        cZP = Mundo.chunks.get(Chave.calcularChave(chunk.x, chunk.z + 1));
        cZN = Mundo.chunks.get(Chave.calcularChave(chunk.x, chunk.z - 1));

        final int[] mascara = MASCARA_CACHE.get();

        // === gera malha de renderização(O Guloso) ===
        // blocos com modeloX(capim, flores) são ignorados aqui e tratados separado no final

        // 1. eixo Y(faces cima/baixo)
        for(boolean cima : new boolean[]{true, false}) {
            for(int y = 0; y < Mundo.Y_CHUNK; y++) {
                int n = 0;
                for(int z = 0; z < 16; z++) {
                    for(int x = 0; x < 16; x++) {
                        int id = ChunkUtil.obterBloco(x, y, z, chunk);
                        int val = 0;
                        if(id != 0) {
                            Bloco b = Bloco.numIds.get(id);
                            if(b != null && !b.modeloX) {
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
        for(boolean leste : new boolean[]{true, false}) {
            for(int x = 0; x < 16; x++) {
                int n = 0;
                for(int y = 0; y < Mundo.Y_CHUNK; y++) {
                    for(int z = 0; z < 16; z++) {
                        int id = ChunkUtil.obterBloco(x, y, z, chunk);
                        int val = 0;
                        if(id != 0) {
                            Bloco b = Bloco.numIds.get(id);
                            if(b != null && !b.modeloX) {
                                int nx = leste ? x + 1 : x - 1;
                                int vizId = 0;
                                Chunk tC = chunk;
                                int tx = nx;

                                if(nx >= 16) { tC = cXP; tx = 0; }
                                else if(nx < 0) { tC = cXN; tx = 15; }

                                if(tC != null) vizId = ChunkUtil.obterBloco(tx, y, z, tC);

                                Bloco bViz = (vizId == 0) ? null : Bloco.numIds.get(vizId);
                                // se chunk vizinho de borda não existe, não renderiza a face agora:
                                // quando ele carregar, marcará este chunk com att=true e a malha será refeita
                                if(deveRenderFace(b, bViz) && !(tC == null && (nx < 0 || nx >= 16))) {
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
                    for(int x = 0; x < 16; x++) {
                        int id = ChunkUtil.obterBloco(x, y, z, chunk);
                        int val = 0;
                        if(id != 0) {
                            Bloco b = Bloco.numIds.get(id);
                            if(b != null && !b.modeloX) {
                                int nz = sul ? z + 1 : z - 1;
                                int vizId = 0;
                                Chunk tC = chunk;
                                int tz = nz;

                                if(nz >= 16) { tC = cZP; tz = 0; }
                                else if(nz < 0) { tC = cZN; tz = 15; }

                                if(tC != null) vizId = ChunkUtil.obterBloco(x, y, tz, tC);

                                Bloco bViz = (vizId == 0) ? null : Bloco.numIds.get(vizId);
                                // se chunk vizinho de borda não existe, não renderiza a face agora:
                                // quando ele carregar, marcará este chunk com att=true e a malha será refeita
                                if(deveRenderFace(b, bViz) && !(tC == null && (nz < 0 || nz >= 16))) {
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

        // === passo separado para modelos em X(capim, flores, etc) ===
        // cada bloco é tratado individualmente, O Guloso não se aplica a eles
        for(int y = 0; y < Mundo.Y_CHUNK; y++) {
            for(int z = 0; z < 16; z++) {
                for(int x = 0; x < 16; x++) {
                    int id = ChunkUtil.obterBloco(x, y, z, chunk);
                    if(id == 0) continue;
                    Bloco b = Bloco.numIds.get(id);
                    if(b == null || !b.modeloX) continue;

                    // pega a luz de cima pro X
                    int ly = Math.min(y + 1, Mundo.Y_CHUNK - 1);
                    byte luz = ChunkUtil.obterLuzCompleta(x, ly, z, chunk);
                    float lb = (luz & 0x0F) / 15f;
                    float ls = ((luz >> 4) & 0x0F) / 15f;

                    BlocoModelo.addModeloX(b.topo, x, y, z, lb, ls, verts, idcTransp);
                }
            }
        }
    }

    public static void malhaPlana(int[] mascara, int largura, int altura,
	int profundidade, int faceId, Chunk chunk, FloatArrayUtil verts, ShortArrayUtil idcSolidos, ShortArrayUtil idcTransp) {
        int n = 0;
        for(int j = 0; j < altura; j++) {
            for(int i = 0; i < largura; ) {
                int val = mascara[n];
                if(val != 0) {
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
                    for(int l = 0; l < h; l++) {
                        for(int k = 0; k < v; k++) {
                            mascara[n + k + l * largura] = 0;
                        }
                    }
                    int id = val >> 8;
                    int luzTotal = val & 0xFF;
                    float lb = (luzTotal & 0x0F) / 15f;
                    float ls = ((luzTotal >> 4) & 0x0F) / 15f;

                    Bloco b = Bloco.numIds.get(id);
                    float x = 0, y = 0, z = 0;
                    float fv = 0, fh = 0;

                    switch(faceId) {
                        case 0: case 1:
                            x = i; z = j; y = profundidade;
                            fv = v; fh = h;
							break;
                        case 2: case 3:
                            z = i; y = j; x = profundidade;
                            fv = v; fh = h;
							break;
                        case 4: case 5:
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


