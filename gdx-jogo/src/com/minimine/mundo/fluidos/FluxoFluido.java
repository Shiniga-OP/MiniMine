package com.minimine.mundo.fluidos;

import com.minimine.mundo.Mundo;
import com.minimine.mundo.Chave;
import com.minimine.mundo.chunks.Chunk;
import com.minimine.mundo.chunks.ChunkProcesso;
import com.minimine.mundo.blocos.Bloco;
import com.minimine.graficos.TipoRender;
import java.util.ArrayDeque;

/*
 * motor de propagação de fluídos por BFS, rodando no tick do servidor
 *
 * meta do bloco (short, 16 bits):
 *   bits 0-3  = nível (1–15; 15 = fonte, 1 = mínimo fluindo, 0 = vazio/ar)
 *   bits 4-7  = id do tipo de fluído (índice em Bloco.numIds, até 16 tipos)
 *   bit  8    = flag fonte (1 = fonte permanente, não some)
 *
 * propagação:
 *   - prioridade pra baixo (desce antes de se espalhar)
 *   - espalha pros 4 lados com nível - 1
 *   - fonte regenera nível 15 todo tick
 *   - nível 0 remove o bloco (vira ar)
 */
public class FluxoFluido {
	// codificação do meta
	public static final int MASCARA_NIVEL = 0xF;
	public static final int MASCARA_TIPO = 0xF0;
	public static final int FLAG_FONTE = 0x100;
	public static final int SHIFT_TIPO = 4;

	public static final int NIVEL_FONTE = 15;
	public static final int NIVEL_MIN = 1;

	// fila de posições mundiais pendentes pra processar
	// usa long empacotando x,y,z igual ao sistema de chaves de chunk
	public static final ArrayDeque<long[]> fila = new ArrayDeque<>();

	// dx/dz dos 4 vizinhos horizontais
	public static final int[] DX = {1, -1, 0, 0};
	public static final int[] DZ = {0, 0, 1, -1};

	// enfileira um bloco fluido pra ser processado no proximo tick
	// chamado quando um bloco fluido é colocado no mundo
	public static void enfileirar(int x, int y, int z) {
		fila.add(new long[]{x, y, z});
	}

	public static void tick(int numTick) {
		if(fila.isEmpty()) return;

		// processa todos os blocos enfileirados neste tick
		// novos blocos gerados pela propagação entram na fila do proximo tick
		final int tam = fila.size();
		for(int i = 0; i < tam; i++) {
			final long[] pos = fila.poll();
			if(pos == null) break;
			processar((int)pos[0], (int)pos[1], (int)pos[2], numTick);
		}
	}

	public static void processar(int x, int y, int z, int numTick) {
		final int meta = Mundo.obterMetaMundo(x, y, z) & 0xFFFF;
		final int nivel = meta & MASCARA_NIVEL;
		final int tipoId = (meta & MASCARA_TIPO) >> SHIFT_TIPO;
		final boolean fonte = (meta & FLAG_FONTE) != 0;

		// bloco sumiu(outro bloco foi colocado por cima)
		final int blocoAtual = Mundo.obterBlocoMundo(x, y, z);
		if(blocoAtual == 0 || Bloco.numIds.get(blocoAtual) == null) return;
		final Bloco tipo = Bloco.numIds.get(blocoAtual);
		if(tipo.render != TipoRender.LIQUIDO) return;

		// respeita viscosidade: so propaga nos ticks certos
		if(numTick % tipo.viscosidade != 0) {
			fila.add(new long[]{x, y, z});
			return;
		}
		// fonte regenera nivel maximo
		final int nivelAtual = fonte ? NIVEL_FONTE : nivel;
		if(fonte) {
			Mundo.defMetaMundo(x, y, z, (short)(FLAG_FONTE | (tipoId << SHIFT_TIPO) | NIVEL_FONTE));
		}
		if(nivelAtual <= 0) {
			// sem nivel: remove o bloco
			Mundo.defBlocoMundo(x, y, z, 0);
			return;
		}
		// tenta descer primeiro
		final int yAbaixo = y - 1;
		if(yAbaixo >= 0) {
			final int blocoAbaixo = Mundo.obterBlocoMundo(x, yAbaixo, z);
			if(podeEntrar(blocoAbaixo, blocoAtual, tipo.densidade)) {
				colocar(x, yAbaixo, z, blocoAtual, tipoId, NIVEL_FONTE, false);
				// reenfileira este bloco pra continuar espalhando
				fila.add(new long[]{x, y, z});
				return;
			}
		}
		// espalha pros 4 lados se tiver nivel suficiente
		if(nivelAtual > NIVEL_MIN) {
			final int nivelVizinho = nivelAtual - 1;
			for(int d = 0; d < 4; d++) {
				final int nx = x + DX[d];
				final int nz = z + DZ[d];
				final int blocoViz = Mundo.obterBlocoMundo(nx, y, nz);
				if(podeEntrar(blocoViz, blocoAtual, tipo.densidade)) {
					colocar(nx, y, nz, blocoAtual, tipoId, nivelVizinho, false);
				}
			}
		}
		// reenfileira fonte pra manter propagação viva
		if(fonte) fila.add(new long[]{x, y, z});
	}

	// verifica se o fluido pode entrar num bloco
	// blocoAlvo = 0(ar) ou outro fluido de densidade menor
	private static boolean podeEntrar(int blocoAlvo, int blocoFluido, float densidadeAtual) {
		if(blocoAlvo == 0) return true;
		final Bloco b = Bloco.numIds.get(blocoAlvo);
		if(b == null) return false;
		if(b.render != TipoRender.LIQUIDO) return false;
		// fluido mais denso desloca o menos denso
		return densidadeAtual > b.densidade;
	}

	// coloca um bloco fluído com nivel e tipo no mundo
	// evita loop(defBlocoMundo detecta liquido e chama colocarFonte)
	public static void colocar(int x, int y, int z, int blocoId, int tipoId, int nivel, boolean fonte) {
		final int metaAtual = Mundo.obterMetaMundo(x, y, z) & 0xFFFF;
		final int nivelAtual = metaAtual & MASCARA_NIVEL;

		// so atualiza se o novo nivel for maior que o atual
		if(nivelAtual >= nivel) return;

		final Chunk chunk = Mundo.obterChunk(x >> 4, z >> 4);
		if(chunk == null) return;
		ChunkProcesso.util.defBloco(x & 0xF, y, z & 0xF, blocoId, chunk);
		final int flagFonte = fonte ? FLAG_FONTE : 0;
		Mundo.defMetaMundo(x, y, z, (short)(flagFonte | (tipoId << SHIFT_TIPO) | nivel));
		chunk.att = true;
		fila.add(new long[]{x, y, z});
	}

	// coloca uma fonte permanente no mundo (chamado ao colocar balde d'água, etc.)
	public static void colocarFonte(int x, int y, int z, String nomeFluido) {
		final Bloco b = Bloco.texIds.get(nomeFluido);
		if(b == null || b.render != TipoRender.LIQUIDO) return;
		colocar(x, y, z, b.tipo, (b.tipo & MASCARA_TIPO) >> SHIFT_TIPO, NIVEL_FONTE, true);
		enfileirar(x, y, z);
	}

	// remove um fluido do mundo(chamado ao quebrar fonte com balde vazio, etc)
	public static void remover(int x, int y, int z) {
		Mundo.defBlocoMundo(x, y, z, 0);
		Mundo.defMetaMundo(x, y, z, (short)0);
	}
}
