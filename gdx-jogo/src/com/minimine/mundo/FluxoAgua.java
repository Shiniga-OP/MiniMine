package com.minimine.mundo;

import com.minimine.mundo.blocos.Bloco;
import java.util.Arrays;
/*
 * sistema de fluxo de água baseado em BFS por nivel
 
 * o nivel de água é armazenado no byte[] meta da Chunk.java:
 *   0 = fonte(nivel cheio, colocado pelo jogador ou gerado estaticamente)
 *   1 a 7 = fluxo horizontal decrescente(7 = mais fraco, some ao chegar em 8)
 *   0xFF = ausencia de água(bloco seco)
 
 * FLUXO PROGRESSIVO:
 *   cada tick propaga um nivel lateral a mais que o tick anterior
 *   nivelAlvo começa em 1(primeira expansão a partir da fonte) e cresce até
 *   NIVEL_MAX_FLUXO. a fronteira é determinada pelo maior nível ja presente
 *   queda livre(Y-1) sempre propaga sem restrição
 
 * REMOÇÃO:
 *   recalcularFluxo() faz BFS completo semeando só fontes reais(meta=0)
 *   blocos de fluxo não semeados somem se não alcançados
*/
public class FluxoAgua {
    public static final int NIVEL_AUSENTE = 0xFF;
    public static final int NIVEL_FONTE = 0;
    public static final int NIVEL_MAX_FLUXO = 7;

    public static final int TOTAL_BLOCOS = 16 * Mundo.Y_CHUNK * 16;

    public static final ThreadLocal<byte[]> META_TEMP_REUSO = new ThreadLocal<byte[]>() {
        @Override protected byte[] initialValue() { return new byte[TOTAL_BLOCOS]; }
    };
    public static final ThreadLocal<int[]> FILA_REUSO = new ThreadLocal<int[]>() {
        @Override protected int[] initialValue() { return new int[TOTAL_BLOCOS]; }
    };
	
    // TICK NORMAL: avança a fronteira lateral um nivel por tick
    public static void attFluxo(Chunk chunk) {
        if(!chunk.fluxoSujo) return;

        final byte[] metaTemp = META_TEMP_REUSO.get();
        final int[] fila = FILA_REUSO.get();
        Arrays.fill(metaTemp, (byte)NIVEL_AUSENTE);
        int inicioFila = 0;
        int fimFila = 0;

        // semeia fontes reais(meta=0) e descobre o nivel maximo de fluxo presente
        int nivelFronteira = 0; // começa em 0(nivel da propria fonte)
        for(int x = 0; x < 16; x++) {
            for(int z = 0; z < 16; z++) {
                for(int y = 0; y < Mundo.Y_CHUNK; y++) {
                    int id = ChunkUtil.obterBloco(x, y, z, chunk);
                    if(eAgua(id)) {
                        int nivel = ChunkUtil.obterMeta(x, y, z, chunk) & 0xFF;
                        if(nivel == NIVEL_FONTE) {
                            int idc = x + (z << 4) + (y << 8);
                            metaTemp[idc] = (byte)NIVEL_FONTE;
                            fila[fimFila++] = idc;
                        } else if(nivel != NIVEL_AUSENTE && nivel > nivelFronteira) {
                            nivelFronteira = nivel;
                        }
                    }
                }
            }
        }
        // nivelAlvo é o proximo nivel a propagar lateralmente
        // se so ha fontes(nivelFronteira=0), propaga nivel 1
        // se ja ha fluxo até nivel N, propaga nivel N+1
        final int nivelAlvo = nivelFronteira + 1;

        propagar(chunk, metaTemp, fila, inicioFila, fimFila, nivelAlvo);
        boolean mudou = aplicar(chunk, metaTemp);

        // continua sujo se ainda ha niveis a expandir
        chunk.fluxoSujo = mudou && nivelAlvo <= NIVEL_MAX_FLUXO;
        chunk.luzSuja = true;
        chunk.att = true;
    }

    // REMOÇÃO/MUDANÇA: BFS completo para convergir imediatamente
    public static void recalcularFluxo(Chunk chunk) {
        final byte[] metaTemp = META_TEMP_REUSO.get();
        final int[] fila = FILA_REUSO.get();
        Arrays.fill(metaTemp, (byte)NIVEL_AUSENTE);
        int inicioFila = 0;
        int fimFila = 0;

        for(int x = 0; x < 16; x++) {
            for(int z = 0; z < 16; z++) {
                for(int y = 0; y < Mundo.Y_CHUNK; y++) {
                    int id = ChunkUtil.obterBloco(x, y, z, chunk);
                    if(eAgua(id)) {
                        int nivel = ChunkUtil.obterMeta(x, y, z, chunk) & 0xFF;
                        if(nivel == NIVEL_FONTE) {
                            int idc = x + (z << 4) + (y << 8);
                            metaTemp[idc] = (byte)NIVEL_FONTE;
                            fila[fimFila++] = idc;
                        }
                    }
                }
            }
        }
        // passa NIVEL_MAX_FLUXO+1 para desabilitar o limite lateral(BFS completo)
        propagar(chunk, metaTemp, fila, inicioFila, fimFila, NIVEL_MAX_FLUXO + 1);
        aplicar(chunk, metaTemp);
        chunk.fluxoSujo = false;
        chunk.luzSuja = true;
        chunk.att = true;
    }
    // BFS com limite de nivel lateral
    // nivelAlvo: nivel máximo permitido na propagação lateral deste tick
    // NIVEL_MAX_FLUXO+1 = sem limite(BFS completo)
    public static void propagar(Chunk chunk, byte[] metaTemp, int[] fila,
	int inicioFila, int fimFila, int nivelAlvo) {
        final int origemX = chunk.x << 4;
        final int origemZ = chunk.z << 4;

        while(inicioFila < fimFila) {
            int idcAtual = fila[inicioFila++];
            int nivelAtual = metaTemp[idcAtual] & 0xFF;
            if(nivelAtual == NIVEL_AUSENTE) continue;

            int cx = idcAtual & 0xF;
            int cz = (idcAtual >> 4) & 0xF;
            int cy = idcAtual >> 8;

            // queda livre: sempre propaga, sem restrição de nivel
            if(cy > 0) {
                int idcAbaixo = cx + (cz << 4) + ((cy - 1) << 8);
                if(podeEntrar(cx, cy - 1, cz, chunk, metaTemp)) {
                    if(melhorar(idcAbaixo, nivelAtual, metaTemp)) {
                        if(fimFila < fila.length) fila[fimFila++] = idcAbaixo;
                    }
                    continue;
                }
            }
            // propagação lateral: so avança se não ultrapassar nivelAlvo
            int nivelVizinho = nivelAtual + 1;
            if(nivelVizinho <= nivelAlvo && nivelAtual < NIVEL_MAX_FLUXO) {
                if(cx + 1 < 16) {
                    if(podeEntrar(cx + 1, cy, cz, chunk, metaTemp)) {
                        int idc = (cx + 1) + (cz << 4) + (cy << 8);
                        if(melhorar(idc, nivelVizinho, metaTemp)) {
                            if(fimFila < fila.length) fila[fimFila++] = idc;
                        }
                    }
                } else {
                    propagarVizinho(origemX + 16, cy, origemZ + cz, nivelVizinho);
                }
                if(cx - 1 >= 0) {
                    if(podeEntrar(cx - 1, cy, cz, chunk, metaTemp)) {
                        int idc = (cx - 1) + (cz << 4) + (cy << 8);
                        if(melhorar(idc, nivelVizinho, metaTemp)) {
                            if(fimFila < fila.length) fila[fimFila++] = idc;
                        }
                    }
                } else {
                    propagarVizinho(origemX - 1, cy, origemZ + cz, nivelVizinho);
                }
                if(cz + 1 < 16) {
                    if(podeEntrar(cx, cy, cz + 1, chunk, metaTemp)) {
                        int idc = cx + ((cz + 1) << 4) + (cy << 8);
                        if(melhorar(idc, nivelVizinho, metaTemp)) {
                            if(fimFila < fila.length) fila[fimFila++] = idc;
                        }
                    }
                } else {
                    propagarVizinho(origemX + cx, cy, origemZ + 16, nivelVizinho);
                }
                if(cz - 1 >= 0) {
                    if(podeEntrar(cx, cy, cz - 1, chunk, metaTemp)) {
                        int idc = cx + ((cz - 1) << 4) + (cy << 8);
                        if(melhorar(idc, nivelVizinho, metaTemp)) {
                            if(fimFila < fila.length) fila[fimFila++] = idc;
                        }
                    }
                } else {
                    propagarVizinho(origemX + cx, cy, origemZ - 1, nivelVizinho);
                }
            }
        }
    }

    public static void propagarVizinho(int wx, int wy, int wz, int nivelNovo) {
        if(wy < 0 || wy >= Mundo.Y_CHUNK) return;
        int blocoId = Mundo.obterBlocoMundo(wx, wy, wz);
        boolean ehAr   = blocoId == 0;
        boolean ehAgua = eAgua(blocoId);
        if(!ehAr && !ehAgua) return;
        if(ehAgua) {
            int nivelAtual = Mundo.obterMetaMundo(wx, wy, wz) & 0xFF;
            if(nivelAtual == NIVEL_AUSENTE || nivelNovo >= nivelAtual) return;
            Mundo.defMetaMundo(wx, wy, wz, (byte)nivelNovo);
        } else {
            Mundo.defBlocoMundo(wx, wy, wz, "agua");
            Mundo.defMetaMundo(wx, wy, wz, (byte)nivelNovo);
        }
        marcarChunk(wx >> 4, wz >> 4);
    }

    public static boolean aplicar(Chunk chunk, byte[] metaTemp) {
        boolean mudou = false;
        for(int i = 0; i < TOTAL_BLOCOS; i++) {
            int nivel = metaTemp[i] & 0xFF;
            int x = i & 0xF;
            int z = (i >> 4) & 0xF;
            int y = i >> 8;
            int idAtual = ChunkUtil.obterBloco(x, y, z, chunk);
            if(nivel != NIVEL_AUSENTE) {
                if(idAtual == 0) {
                    ChunkUtil.defBloco(x, y, z, "agua", chunk);
                    mudou = true;
                }
                int metaAtual = ChunkUtil.obterMeta(x, y, z, chunk) & 0xFF;
                if(metaAtual != nivel) {
                    ChunkUtil.defMeta(x, y, z, (byte)nivel, chunk);
                    mudou = true;
                }
            } else {
                if(eAgua(idAtual)) {
                    ChunkUtil.defBloco(x, y, z, "ar", chunk);
                    ChunkUtil.defMeta(x, y, z, (byte)NIVEL_AUSENTE, chunk);
                    mudou = true;
                }
            }
        }
        return mudou;
    }

    public static boolean melhorar(int idc, int nivelNovo, byte[] metaTemp) {
        int nivelAtual = metaTemp[idc] & 0xFF;
        if(nivelAtual == NIVEL_AUSENTE || nivelNovo < nivelAtual) {
            metaTemp[idc] = (byte)nivelNovo;
            return true;
        }
        return false;
    }

    public static boolean podeEntrar(int x, int y, int z, Chunk chunk, byte[] metaTemp) {
        int id = ChunkUtil.obterBloco(x, y, z, chunk);
        if(id == 0) return true;
        if(eAgua(id)) {
            int idc = x + (z << 4) + (y << 8);
            int nivelAtual = metaTemp[idc] & 0xFF;
            return nivelAtual == NIVEL_AUSENTE || nivelAtual > NIVEL_FONTE;
        }
        return false;
    }

    public static boolean eAgua(int blocoId) {
        if(blocoId == 0) return false;
        Bloco b = Bloco.numIds.get(blocoId);
        return b != null && "agua".equals(b.nome.toString());
    }

    public static void marcarSujo(int chunkX, int chunkZ) {
        marcarChunk(chunkX, chunkZ);
        marcarChunk(chunkX + 1, chunkZ);
        marcarChunk(chunkX - 1, chunkZ);
        marcarChunk(chunkX, chunkZ + 1);
        marcarChunk(chunkX, chunkZ - 1);
    }

    public static void marcarChunk(int cx, int cz) {
        Chunk c = Mundo.chunks.get(Chave.calcularChave(cx, cz));
        if(c != null) {
			c.fluxoSujo = true;
			c.att = true;
		}
    }
}
