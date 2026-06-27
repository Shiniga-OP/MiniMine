package com.minimine.mundo.chunks;

import com.minimine.mundo.blocos.Bloco;
import com.minimine.graficos.TipoRender;
import java.util.Arrays;
import com.minimine.mundo.Mundo;

public class ChunkLuz {
    public static final int Y_MAX = Mundo.Y_CHUNK - 1;
    public static final int[] POS_X = {1, -1, 0, 0, 0, 0};
    public static final int[] POS_Y = {0, 0, 1, -1, 0, 0};
    public static final int[] POS_Z = {0, 0, 0, 0, 1, -1};

    public static final int TOTAL_BLOCOS = 16 * Mundo.Y_CHUNK * 16;

    public static final ThreadLocal<byte[]> LUZ_TEMP_REUSO = new ThreadLocal<byte[]>() {
        @Override protected byte[] initialValue() { return new byte[TOTAL_BLOCOS]; }
    };
    public static final ThreadLocal<int[]> FILA_LUZ_REUSO = new ThreadLocal<int[]>() {
        @Override protected int[] initialValue() { return new int[TOTAL_BLOCOS * 6]; }
    };

    public void calcularLuz(Chunk chunk) {
        execProcesso(chunk, true);
    }
    
    public void attLuz(Chunk chunk) {
        if(!chunk.luzSuja) return;
        chunk.luzSuja = false;
        execProcesso(chunk, false);
    }
	
    public void recalcularLuz(Chunk chunk) {
        zerarLuzBlocoChunk(chunk);

        final Chunk chunkNorte = Mundo.obterChunk(chunk.x, chunk.z - 1);
        final Chunk chunkSul = Mundo.obterChunk(chunk.x, chunk.z + 1);
        final Chunk chunkLeste = Mundo.obterChunk(chunk.x + 1, chunk.z);
        final Chunk chunkOeste = Mundo.obterChunk(chunk.x - 1, chunk.z);
        final Chunk chunkNE = Mundo.obterChunk(chunk.x + 1, chunk.z - 1);
        final Chunk chunkNO = Mundo.obterChunk(chunk.x - 1, chunk.z - 1);
        final Chunk chunkSE = Mundo.obterChunk(chunk.x + 1, chunk.z + 1);
        final Chunk chunkSO = Mundo.obterChunk(chunk.x - 1, chunk.z + 1);

        final Chunk[] vizinhas = {
			chunkNorte, chunkSul, chunkLeste, chunkOeste,
			chunkNE, chunkNO, chunkSE, chunkSO
		};
        for(Chunk v : vizinhas) {
            if(v != null) zerarLuzBlocoChunk(v);
        }
        chunk.luzSuja = true;
        chunk.att = true;
        for(Chunk v : vizinhas) {
            if(v != null) {
				v.luzSuja = true;
				v.att = true;
			}
        }
    }

    // buffers ThreadLocal para pré-computar propriedades dos blocos, evitando
    // pesquisas de HashMap e chamadas a ChunkUtil dentro dos loops quentes
    public static final ThreadLocal<boolean[]> OPACO_REUSO = new ThreadLocal<boolean[]>() {
        @Override protected boolean[] initialValue() { return new boolean[TOTAL_BLOCOS]; }
    };
    public static final ThreadLocal<byte[]> EMISSAO_REUSO = new ThreadLocal<byte[]>() {
        @Override protected byte[] initialValue() { return new byte[TOTAL_BLOCOS]; }
    };
    // blocos decodificados da chunk: evita 65536 chamadas a lerPacote no pré-compute
    public static final ThreadLocal<int[]> BLOCOS_REUSO = new ThreadLocal<int[]>() {
        @Override protected int[] initialValue() { return new int[TOTAL_BLOCOS]; }
    };

    public static void execProcesso(Chunk chunk, boolean soVizinhasProntas) {
        chunk.luzFazendo = true;

        final byte[] luzTemp = LUZ_TEMP_REUSO.get();
        final int[] filaLuz = FILA_LUZ_REUSO.get();
        final boolean[] opaco = OPACO_REUSO.get();
        final byte[] emissao = EMISSAO_REUSO.get();
        final int[] blocosDecod = BLOCOS_REUSO.get();
        Arrays.fill(luzTemp, (byte) 0);
        int inicioFila = 0;
        int fimFila = 0;

        // decodifica todos os blocos de uma vez, em ordem linear do array compactado,
        // aproveitando acesso sequencial ao int[] chunk.blocos em vez de 65536 lerPacote individuais
        decodificarBlocos(chunk, blocosDecod);

        // pré-computa opacidade e emissão na mesma ordem do loop de luz solar (x->z->y),
        // agora só faz Bloco.numIds.get de um int[] local, sem lerPacote
        for(int x = 0; x < 16; x++) {
            for(int z = 0; z < 16; z++) {
                final int posXZ = x + (z << 4);
                for(int y = Y_MAX; y >= 0; y--) {
                    final int idc = posXZ + (y << 8);
                    final Bloco b = Bloco.numIds.get(blocosDecod[idc]);
                    opaco[idc] = b != null && b.render == TipoRender.OPACO;
                    emissao[idc] = (b != null && b.luz > 0) ? (byte)(b.luz & 0x0F) : 0;
                }
            }
        }
        for(int x = 0; x < 16; x++) {
            for(int z = 0; z < 16; z++) {
                int luzSolarAtual = 15;
                final int posXZ = x + (z << 4);

                for(int y = Y_MAX; y >= 0; y--) {
                    final int idc = posXZ + (y << 8);

                    // aplica a luz solar atual no bloco antes de verificar se ele é opaco,
                    // assim o próprio bloco sólido recebe a luz que vem de cima,
                    // e apenas os blocos abaixo dele ficam sem luz solar
                    luzTemp[idc] = (byte)(luzSolarAtual << 4);

                    if(luzSolarAtual > 0) filaLuz[fimFila++] = idc;

                    // bloco opaco interrompe a descida da luz solar para os proximos
                    if(opaco[idc]) luzSolarAtual = 0;

                    if(emissao[idc] > 0) {
                        luzTemp[idc] |= emissao[idc];
                        filaLuz[fimFila++] = idc;
                    }
                }
            }
        }
        fimFila = importarLuzVizinhas(chunk, luzTemp, filaLuz, fimFila, soVizinhasProntas, opaco);
        fimFila = propagarBFS(chunk, luzTemp, filaLuz, inicioFila, fimFila, opaco);

        // detecta mudança de borda e propaga sujo para vizinhas
        boolean mudouNorte = false, mudouSul = false, mudouLeste = false, mudouOeste = false;
        for(int a = 0; a < 16; a++) {
            for(int y = 0; y < Mundo.Y_CHUNK; y++) {
                if(!mudouNorte) {
                    final int idc = a + (0 << 4) + (y << 8);
                    if((luzTemp[idc] & 0xFF) != (chunk.luz[idc] & 0xFF)) mudouNorte = true;
                }
                if(!mudouSul) {
                    final int idc = a + (15 << 4) + (y << 8);
                    if((luzTemp[idc] & 0xFF) != (chunk.luz[idc] & 0xFF)) mudouSul = true;
                }
                if(!mudouLeste) {
                    final int idc = 15 + (a << 4) + (y << 8);
                    if((luzTemp[idc] & 0xFF) != (chunk.luz[idc] & 0xFF)) mudouLeste = true;
                }
                if(!mudouOeste) {
                    final int idc = 0 + (a << 4) + (y << 8);
                    if((luzTemp[idc] & 0xFF) != (chunk.luz[idc] & 0xFF)) mudouOeste = true;
                }
                if(mudouNorte && mudouSul && mudouLeste && mudouOeste) break;
            }
            if(mudouNorte && mudouSul && mudouLeste && mudouOeste) break;
        }
        System.arraycopy(luzTemp, 0, chunk.luz, 0, TOTAL_BLOCOS);

        if(mudouNorte) {
            final Chunk v = Mundo.obterChunk(chunk.x, chunk.z - 1);
            if(v != null && !v.luzFazendo) {
				v.luzSuja = true;
				v.att = true;
			}
        }
        if(mudouSul) {
            final Chunk v = Mundo.obterChunk(chunk.x, chunk.z + 1);
            if(v != null && !v.luzFazendo) {
				v.luzSuja = true;
				v.att = true;
			}
        }
        if(mudouLeste) {
            final Chunk v = Mundo.obterChunk(chunk.x + 1, chunk.z);
            if(v != null && !v.luzFazendo) {
				v.luzSuja = true;
				v.att = true;
			}
        }
        if(mudouOeste) {
            final Chunk v = Mundo.obterChunk(chunk.x - 1, chunk.z);
            if(v != null && !v.luzFazendo) {
				v.luzSuja = true;
				v.att = true;
			}
        }
        chunk.luzSuja = false;
        chunk.luzFazendo = false;
    }

    public static int propagarBFS(Chunk chunk, byte[] luzTemp, int[] filaLuz,
	int inicioFila, int fimFila, boolean[] opaco) {
        while (inicioFila < fimFila) {
            final int idcAtual = filaLuz[inicioFila++];
            final int luzTotal = luzTemp[idcAtual] & 0xFF;

            final int cx = idcAtual & 0xF;
            final int cz = (idcAtual >> 4) & 0xF;
            final int cy = idcAtual >> 8;

            final int lb = luzTotal & 0x0F;
            final int ls = luzTotal >> 4;

            for(int i = 0; i < 6; i++) {
                final int nx = cx + POS_X[i];
                final int ny = cy + POS_Y[i];
                final int nz = cz + POS_Z[i];

                if(nx < 0 || nx >= 16 || ny < 0 || ny >= Mundo.Y_CHUNK || nz < 0 || nz >= 16) continue;

                final int idcVizinho = nx + (nz << 4) + (ny << 8);
                final int luzVizinha = luzTemp[idcVizinho] & 0xFF;

                int lbV = luzVizinha & 0x0F;
                int lsV = luzVizinha >> 4;

                boolean mudou = false;
                if(lb > 0 && lbV < lb - 1) {
					lbV = lb - 1;
					mudou = true;
				}
                if(ls > 0 && lsV < ls - 1) {
					lsV = ls - 1;
					mudou = true;
				}
                if(mudou) {
                    luzTemp[idcVizinho] = (byte) ((lsV << 4) | lbV);

                    if(!opaco[idcVizinho] && fimFila < filaLuz.length) {
                        filaLuz[fimFila++] = idcVizinho;
                    }
                }
            }
        }
        return fimFila;
    }

    public static int importarLuzVizinhas(Chunk chunk, byte[] luzTemp, int[] filaLuz,
	int fimFila, boolean apenasVizinhasProntas, boolean[] opaco) {
        final Chunk norte = filtrarVizinha(Mundo.obterChunk(chunk.x, chunk.z - 1), apenasVizinhasProntas);
        final Chunk sul = filtrarVizinha(Mundo.obterChunk(chunk.x, chunk.z + 1), apenasVizinhasProntas);
        final Chunk leste = filtrarVizinha(Mundo.obterChunk(chunk.x + 1, chunk.z), apenasVizinhasProntas);
        final Chunk oeste = filtrarVizinha(Mundo.obterChunk(chunk.x - 1, chunk.z), apenasVizinhasProntas);

        if(norte != null) fimFila = importarBorda(norte, luzTemp, filaLuz, fimFila, true, 15, 0, opaco);
        if(sul != null) fimFila = importarBorda(sul, luzTemp, filaLuz, fimFila, true, 0, 15, opaco);
        if(leste != null) fimFila = importarBorda(leste, luzTemp, filaLuz, fimFila, false, 0, 15, opaco);
        if(oeste != null) fimFila = importarBorda(oeste, luzTemp, filaLuz, fimFila, false, 15, 0, opaco);
        return fimFila;
    }

    public static final Chunk filtrarVizinha(final Chunk vizinha, boolean apenasVizinhasProntas) {
		if(vizinha == null) return null;
		if(vizinha.luzFazendo) return null;
		if(apenasVizinhasProntas && !vizinha.dadosProntos) return null;
		return vizinha;
	}

    // vizinha: chunk de onde importamos a luz; opaco[]: da chunk atual(destino)
    public static int importarBorda(Chunk vizinha, byte[] luzTemp, int[] filaLuz,
	int fimFila, boolean iteraX, int bordaViz, int bordaNossa, boolean[] opaco) {
        for(int a = 0; a < 16; a++) {
            for(int y = 0; y < Mundo.Y_CHUNK; y++) {
                final int idcViz = iteraX
					? a + (bordaViz << 4) + (y << 8)
					: bordaViz + (a << 4) + (y << 8);

                final int luzVizinha = vizinha.luz[idcViz] & 0xFF;
                final int lbV = luzVizinha & 0x0F;
                final int lsV = luzVizinha >> 4;

                if(lbV <= 1 && lsV <= 1) continue;

                final int idcNossa = iteraX
					? a + (bordaNossa << 4) + (y << 8)
					: bordaNossa + (a << 4) + (y << 8);

                if(opaco[idcNossa]) continue;

                final int lbNova = lbV - 1;
                final int lsNova = lsV - 1;

                final int luzAtual = luzTemp[idcNossa] & 0xFF;
                final int lbAtual = luzAtual & 0x0F;
                final int lsAtual = luzAtual >> 4;

                if(lbNova > lbAtual || lsNova > lsAtual) {
                    luzTemp[idcNossa] = (byte) ((Math.max(lsNova, lsAtual) << 4)
						| Math.max(lbNova, lbAtual));
                    if(fimFila < filaLuz.length) {
                        filaLuz[fimFila++] = idcNossa;
                    }
                }
            }
        }
        return fimFila;
    }

    // decodifica chunk.blocos inteiro de uma vez em ordem sequencial,
    // muito mais eficiente que 65536 chamadas individuais a lerPacote
    public static void decodificarBlocos(Chunk chunk, int[] dest) {
        if(chunk.blocos == null) {
            Arrays.fill(dest, 0);
            return;
        }
        final int bits = chunk.usaPaleta ? chunk.paletaBits : chunk.bitsPorBloco;
        final int bpi = chunk.blocosPorInt;
        final int log2 = ChunkUtil.LOG2(bpi);
        final int mascara = (1 << bits) - 1;
        final int[] blocos = chunk.blocos;
        if(chunk.usaPaleta) {
            final int[] paleta = chunk.paleta;
            final int palTam = chunk.paletaTam;
            for(int i = 0; i < TOTAL_BLOCOS; i++) {
                final int idc = i >> log2;
                final int bitPos = (i & (bpi - 1)) * bits;
                final int idcPal = (blocos[idc] >>> bitPos) & mascara;
                dest[i] = (idcPal >= 0 && idcPal < palTam) ? paleta[idcPal] : 0;
            }
        } else {
            for(int i = 0; i < TOTAL_BLOCOS; i++) {
                final int idc = i >> log2;
                final int bitPos = (i & (bpi - 1)) * bits;
                dest[i] = (blocos[idc] >>> bitPos) & mascara;
            }
        }
    }

    public static void zerarLuzBlocoChunk(Chunk chunk) {
        for(int i = 0; i < TOTAL_BLOCOS; i++) {
            final int luzSolar = (chunk.luz[i] >> 4) & 0x0F;

            final int x = i & 0xF;
            final int z = (i >> 4) & 0xF;
            final int y = i >> 8;

            final Bloco b = Bloco.numIds.get(ChunkUtil.obterBloco(x, y, z, chunk));

            chunk.luz[i] = (b != null && b.luz > 0)
				? (byte)((luzSolar << 4) | (b.luz & 0x0F))
				: (byte)(luzSolar << 4);
        }
    }
}
