package com.minimine.mundo.chunks;

import com.minimine.utils.arrays.FloatArrayUtil;
import com.minimine.utils.arrays.ShortArrayUtil;
import com.minimine.mundo.blocos.Bloco;
import com.minimine.mundo.blocos.BlocoModelo;
import com.minimine.graficos.TipoRender;
import com.minimine.mundo.Mundo;
import com.minimine.mundo.blocos.BlocoCubo;

public class ChunkMalha {
    // tamanho maximo de mascara necessaria(eixo X/Z: 16 * Y_CHUNK)
    // reutiliza array de mascara por thread
    public static final ThreadLocal<int[]> MASCARA_CACHE = new ThreadLocal<int[]>() {
        @Override protected int[] initialValue() { return new int[16 * 256]; }
    };
	public static final boolean[] mascara2 = {true, false};

    public void attMalha(Chunk chunk, FloatArrayUtil verts, ShortArrayUtil idcSolidos, ShortArrayUtil idcTransp) {
		Chunk cXP, cXN, cZP, cZN;
        cXP = Mundo.obterChunk(chunk.x + 1, chunk.z);
        cXN = Mundo.obterChunk(chunk.x - 1, chunk.z);
        cZP = Mundo.obterChunk(chunk.x, chunk.z + 1);
        cZN = Mundo.obterChunk(chunk.x, chunk.z - 1);
        // descarte usa dadosProntos, garante que os blocos existem para decisão de face
        if(cXP != null && !cXP.dadosProntos) cXP = null;
        if(cXN != null && !cXN.dadosProntos) cXN = null;
        if(cZP != null && !cZP.dadosProntos) cZP = null;
        if(cZN != null && !cZN.dadosProntos) cZN = null;
        // luz usa estado >= 3 garante que calcularLuz ja rodou na vizinha
        // se nao tiver luz pronta, usa padrão 0 e refaz quando estiver pronta
        final int[] mascara = MASCARA_CACHE.get();

        // === gera malha de renderização(O Guloso) ===
        // blocos com modeloX(capim, flores) são ignorados aqui e tratados separado no final

        // 1. eixo Y(faces cima/baixo)
        for(boolean cima : mascara2) {
            for(int y = 0; y < Mundo.Y_CHUNK; y++) {
                int n = 0;
                for(int z = 0; z < 16; z++) {
                    for(int x = 0; x < 16; x++) {
                        final int id = ChunkUtil.obterBloco(x, y, z, chunk);
                        int val = 0;
                        if(id != 0) {
                            final Bloco b = Bloco.numIds.get(id);
                            if(b != null && b.modelo instanceof BlocoCubo) {
                                final int ny = cima ? y + 1 : y - 1;
                                int vizId = 0;
                                if(ny >= 0 && ny < Mundo.Y_CHUNK) {
                                    vizId = ChunkUtil.obterBloco(x, ny, z, chunk);
                                } else {
                                    vizId = 0;
                                }
                                final Bloco bViz = (vizId == 0) ? null : Bloco.numIds.get(vizId);
                                if(deveRenderFace(b, bViz)) {
                                    final byte luz = ChunkUtil.obterLuzCompleta(x, (ny < 0 || ny >= Mundo.Y_CHUNK) ? y : ny, z, chunk);
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
        for(boolean leste : mascara2) {
            for(int x = 0; x < 16; x++) {
                int n = 0;
                for(int y = 0; y < Mundo.Y_CHUNK; y++) {
                    for(int z = 0; z < 16; z++) {
                        final int id = ChunkUtil.obterBloco(x, y, z, chunk);
                        int val = 0;
                        if(id != 0) {
                            final Bloco b = Bloco.numIds.get(id);
                            if(b != null && b.modelo instanceof BlocoCubo) {
                                final int nx = leste ? x + 1 : x - 1;
                                int vizId = 0;
                                Chunk tC = chunk;
                                int tx = nx;

                                if(nx >= 16) {
									tC = cXP;
									tx = 0;
								} else if(nx < 0) {
									tC = cXN;
									tx = 15;
								}
                                if(tC != null) vizId = ChunkUtil.obterBloco(tx, y, z, tC);

                                final Bloco bViz = (vizId == 0) ? null : Bloco.numIds.get(vizId);
                                // se chunk vizinho de borda não existe, não renderiza a face agora:
                                // quando ele carregar, marcará este chunk com att=true e a malha será refeita
                                if(deveRenderFace(b, bViz) && !(tC == null && (nx < 0 || nx >= 16))) {
                                    final byte luz = (tC != null)
                                        ? ChunkUtil.obterLuzCompleta(tx, y, z, tC)
                                        : ChunkUtil.obterLuzCompleta(x, y, z, chunk);
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
        for(boolean sul : mascara2) {
            for(int z = 0; z < 16; z++) {
                int n = 0;
                for(int y = 0; y < Mundo.Y_CHUNK; y++) {
                    for(int x = 0; x < 16; x++) {
                        final int id = ChunkUtil.obterBloco(x, y, z, chunk);
                        int val = 0;
                        if(id != 0) {
                            final Bloco b = Bloco.numIds.get(id);
                            if(b != null && b.modelo instanceof BlocoCubo) {
                                final int nz = sul ? z + 1 : z - 1;
                                int vizId = 0;
                                Chunk tC = chunk;
                                int tz = nz;

                                if(nz >= 16) {
									tC = cZP;
									tz = 0;
								} else if(nz < 0) {
									tC = cZN;
									tz = 15;
								}
                                if(tC != null) vizId = ChunkUtil.obterBloco(x, y, tz, tC);

                                final Bloco bViz = (vizId == 0) ? null : Bloco.numIds.get(vizId);
                                // se chunk vizinho de borda não existe, não renderiza a face agora:
                                // quando ele carregar, marcará este chunk com att=true e a malha será refeita
                                if(deveRenderFace(b, bViz) && !(tC == null && (nz < 0 || nz >= 16))) {
                                    final byte luz = (tC != null)
                                        ? ChunkUtil.obterLuzCompleta(x, y, tz, tC)
                                        : ChunkUtil.obterLuzCompleta(x, y, z, chunk);
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
                    final int id = ChunkUtil.obterBloco(x, y, z, chunk);
                    if(id == 0) continue;
                    final Bloco b = Bloco.numIds.get(id);
                    if(b == null || b.modelo instanceof BlocoCubo) continue;

                    // pega a luz de cima pro X
                    final int ly = Math.min(y + 1, Mundo.Y_CHUNK - 1);
                    final byte luz = ChunkUtil.obterLuzCompleta(x, ly, z, chunk);
                    final float lb = (luz & 0x0F) / 15f;
                    final float ls = ((luz >> 4) & 0x0F) / 15f;

                    b.modelo.addFace(0, b.textura.topo, x, y, z, 0, 0, lb, ls, verts, idcTransp);
                }
            }
        }
    }

    public static void malhaPlana(int[] mascara, int largura, int altura,
	int profundidade, int faceId, Chunk chunk, FloatArrayUtil verts, ShortArrayUtil idcSolidos, ShortArrayUtil idcTransp) {
        int n = 0;
        for(int j = 0; j < altura; j++) {
            for(int i = 0; i < largura; ) {
                final int val = mascara[n];
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
                    final int id = val >> 8;
                    final int luzTotal = val & 0xFF;
                    final float lb = (luzTotal & 0x0F) / 15f;
                    final float ls = ((luzTotal >> 4) & 0x0F) / 15f;

                    final Bloco b = Bloco.numIds.get(id);
                    float x = 0, y = 0, z = 0;
                    float fv = 0, fh = 0;

                    switch(faceId) {
                        case 0: case 1:
                            x = i;
							z = j;
							y = profundidade;
                            fv = v;
							fh = h;
							break;
                        case 2: case 3:
                            z = i;
							y = j;
							x = profundidade;
                            fv = v;
							fh = h;
							break;
                        case 4: case 5:
                            x = i;
							y = j;
							z = profundidade;
                            fv = v;
							fh = h;
							break;
                    }
                    final ShortArrayUtil lista = (b.render == TipoRender.OPACO || b.render == TipoRender.RECORTE) ? idcSolidos : idcTransp;
                    b.modelo.addFace(faceId, b.texturaId(faceId), x, y, z, fv, fh, lb, ls, verts, lista);

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
        if(vizinho.render == TipoRender.OPACO) return false;
        return true;
    }
}
