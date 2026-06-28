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
 
 * meta do bloco(short, 16 bits):
 *  bits 0-3 = nivel(1–8; 8 = fonte, 1 = minimo fluindo, 0 = vazio/ar)
 *  bit 8 = marca fonte(1 = fonte permanente, não some)

 * propagação:
 *  - prioridade pra baixo(desce antes de se espalhar)
 *  - espalha pros 4 lados com nivel - 1
 *  - fonte regenera nivel 15 todo tick
 *  - nivel 0 remove o bloco(vira ar)
*/
public class FluxoFluido {
	// codificação do meta
	public static final int MASCARA_NIVEL = 0xF;
	public static final int MARCA_FONTE = 0x100;

	public static final int NIVEL_FONTE = 8;
	public static final int NIVEL_MIN = 1;

	// fila de posições mundiais pendentes pra processar
	// usa long empacotando x,y,z igual ao sistema de chaves de chunk
	public static final ArrayDeque<Long> fila = new ArrayDeque<>();

	// dx/dz dos 4 vizinhos horizontais
	public static final int[] DX = {1, -1, 0, 0};
	public static final int[] DZ = {0, 0, 1, -1};

	public static void tick(int numTick) {
		if(fila.isEmpty()) return;

		// processa todos os blocos enfileirados neste tick
		// novos blocos gerados pela propagação entram na fila do proximo tick
		final int tam = fila.size();
		for(int i = 0; i < tam; i++) {
			final long pos = fila.poll();
			processar(Chave.x3d(pos), Chave.y3d(pos), Chave.z3d(pos), numTick);
		}
	}

	public static void processar(int x, int y, int z, int numTick) {
		final int meta = Mundo.obterMetaMundo(x, y, z) & 0xFFFF;
		final int nivel = meta & MASCARA_NIVEL;
		final boolean fonte = (meta & MARCA_FONTE) != 0;

		// bloco sumiu(outro bloco foi colocado por cima)
		final int blocoAtual = Mundo.obterBlocoMundo(x, y, z);
		if(blocoAtual == 0 || Bloco.numIds.get(blocoAtual) == null) return;
		final Bloco tipo = Bloco.numIds.get(blocoAtual);
		if(tipo.render != TipoRender.LIQUIDO) return;

		// respeita viscosidade: so propaga nos ticks certos
		if(numTick % tipo.viscosidade != 0) {
			fila.add(Chave.gerar3d(x, y, z));
			return;
		}
		// fonte regenera nivel maximo
		final int nivelAtual = fonte ? NIVEL_FONTE : nivel;
		if(fonte) {
			Mundo.defMetaMundo(x, y, z, (short)(MARCA_FONTE | NIVEL_FONTE));
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
				colocar(x, yAbaixo, z, blocoAtual, nivelAtual, false);
				if(fonte) fila.add(Chave.gerar3d(x, y, z));
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
					colocar(nx, y, nz, blocoAtual, nivelVizinho, false);
				}
			}
		}
		// reenfileira pra manter propagação viva
		if(fonte || nivelAtual > NIVEL_MIN) fila.add(Chave.gerar3d(x, y, z));
	}

	// verifica se o fluido pode entrar num bloco
	// blocoAlvo = 0(ar) ou outro fluido de densidade menor
	public static boolean podeEntrar(int blocoAlvo, int blocoFluido, float densidadeAtual) {
		if(blocoAlvo == 0) return true;
		final Bloco b = Bloco.numIds.get(blocoAlvo);
		if(b == null) return false;
		if(b.render != TipoRender.LIQUIDO) return false;
		// fluido mais denso desloca o menos denso
		return densidadeAtual > b.densidade;
	}

	// coloca um bloco fluido com nivel e tipo no mundo
	// evita loop(defBlocoMundo detecta liquido e chama colocarFonte)
	public static void colocar(int x, int y, int z, int blocoId, int nivel, boolean fonte) {
		final Chunk chunk = Mundo.obterChunk(x >> 4, z >> 4);
		if(chunk == null) return;
		
		final int nivelAtual = ChunkProcesso.util.obterMeta(x & 0xF, y, z & 0xF, chunk) & MASCARA_NIVEL;

		// so atualiza se o novo nivel for maior que o atual
		if(nivelAtual >= nivel) return;

		ChunkProcesso.util.defBloco(x & 0xF, y, z & 0xF, blocoId, chunk);
		ChunkProcesso.util.defMeta(x & 0xF, y, z & 0xF, (short)nivel, chunk);
		
		chunk.att = true;
		
		fila.add(Chave.gerar3d(x, y, z));
	}

	public static void colocarChunk(int x, int y, int z, int blocoId, int nivel, Chunk chunk) {
		if(chunk == null) return;
		final int nivelAtual = ChunkProcesso.util.obterMeta(x & 0xF, y, z & 0xF, chunk) & MASCARA_NIVEL;

		// so atualiza se o novo nivel for maior que o atual
		if(nivelAtual >= nivel) return;

		ChunkProcesso.util.defBloco(x & 0xF, y, z & 0xF, blocoId, chunk);
		ChunkProcesso.util.defMeta(x & 0xF, y, z & 0xF, (short)nivel, chunk);
		
		fila.add(Chave.gerar3d(x, y, z));
	}

	// coloca uma fonte permanente no mundo(chamado ao colocar balde d'água, etc)
	public static void colocarFonte(int x, int y, int z, String nomeFluido) {
		final Bloco b = Bloco.texIds.get(nomeFluido);
		if(b == null || b.render != TipoRender.LIQUIDO) return;
		colocar(x, y, z, b.tipo, NIVEL_FONTE, true);
	}

	// remove um fluido do mundo(chamado ao quebrar fonte com balde vazio, etc)
	public static void remover(int x, int y, int z) {
		Mundo.defBlocoMundo(x, y, z, 0);
		Mundo.defMetaMundo(x, y, z, (short)0);
	}
}
