package com.minimine.mundo.fluidos;

import com.minimine.mundo.Mundo;
import com.minimine.mundo.Chave;
import com.minimine.mundo.chunks.Chunk;
import com.minimine.mundo.chunks.ChunkProcesso;
import com.minimine.mundo.blocos.Bloco;
import com.minimine.graficos.TipoRender;
/*
 * motor de propagação de fluidos por fluidos ticks agendados

 * meta do bloco(short, 16 bits):
 *  bits 0-3 = nivel(1–8; 8 = fonte, 1 = minimo fluindo, 0 = vazio/ar)
 *  bit 8 = marca fonte(1 = fonte permanente, não some)

 * regra universal de espalhamento:
 *  - tem ar abaixo: so desce
 *  - tem chão: espalha pros lados com nivel - 1
 *  - nivel 0: remove o bloco
 *  - fonte: regenera nivel maximo todo tick e reagenda

 * fila: balde queue circular de arrays de long primitivo
 *  - baldes potencia de 2, maior que qualquer viscosidade
 *  - inserção O(1), tick processa só o balde do tick atual
 */
public class FluxoFluido {
	public static final int MASCARA_NIVEL = 0xF;
	public static final int MARCA_FONTE = 0x100;

	public static final int NIVEL_FONTE = 8;
	public static final int NIVEL_MIN = 1;

	public static final int[] DX = {1, -1, 0, 0};
	public static final int[] DZ = {0, 0, 1, -1};

	static final int BALDES = 128;
	static final int CAP_BALDE = 256;
	static long[][] baldes = new long[BALDES][CAP_BALDE];
	static final int[] contas = new int[BALDES];
	static int tickAtual = 1;

	public static void agendar(int x, int y, int z, int tickAlvo) {
		final int slot = tickAlvo & (BALDES - 1);
		final int n = contas[slot];
		if(n >= baldes[slot].length) {
			final long[] novo = new long[baldes[slot].length * 2];
			System.arraycopy(baldes[slot], 0, novo, 0, n);
			baldes[slot] = novo;
		}
		baldes[slot][n] = Chave.gerar3d(x, y, z);
		contas[slot] = n + 1;
	}

	public static void tick(int numTick) {
		tickAtual = numTick;
		final int slot = numTick & (BALDES - 1);
		final int n = contas[slot];
		if(n == 0) return;
		contas[slot] = 0;
		for(int i = 0; i < n; i++) {
			final long pos = baldes[slot][i];
			processar(Chave.x3d(pos), Chave.y3d(pos), Chave.z3d(pos), numTick);
		}
	}

	public static void processar(int x, int y, int z, int numTick) {
		final int meta = Mundo.obterMetaMundo(x, y, z) & 0xFFFF;
		final boolean fonte = (meta & MARCA_FONTE) != 0;

		final int blocoAtual = Mundo.obterBlocoMundo(x, y, z);
		final Bloco tipo = Bloco.numIds.get(blocoAtual);
		if(tipo == null || tipo.render != TipoRender.LIQUIDO) return;

		final int nivel = fonte ? NIVEL_FONTE : (meta & MASCARA_NIVEL);

		if(!fonte && nivel <= 0) {
			Mundo.defBlocoMundo(x, y, z, 0);
			Mundo.defMetaMundo(x, y, z, (short) 0);
			final Chunk chunk = Mundo.obterChunk(x >> 4, z >> 4);
			if(chunk != null) chunk.att = true;
			for(int d = 0; d < 4; d++) {
				notificar(x + DX[d], y, z + DZ[d], blocoAtual, numTick, tipo.viscosidade);
			}
			notificar(x, y + 1, z, blocoAtual, numTick, tipo.viscosidade);
			return;
		}
		if(fonte) Mundo.defMetaMundo(x, y, z, (short)(MARCA_FONTE | NIVEL_FONTE));

		// regra universal: ar abaixo = so desce; chão = espalha pros lados
		final int yAbaixo = y - 1;
		if(yAbaixo >= 0 && podeEntrar(Mundo.obterBlocoMundo(x, yAbaixo, z), blocoAtual, tipo.densidade)) {
			colocar(x, yAbaixo, z, blocoAtual, NIVEL_FONTE, numTick + tipo.viscosidade);
			if(fonte) agendar(x, y, z, numTick + tipo.viscosidade);
			return;
		}
		// tem chão: espalha pros lados
		if(nivel > NIVEL_MIN) {
			final int nivelViz = nivel - 1;
			for(int d = 0; d < 4; d++) {
				final int nx = x + DX[d];
				final int nz = z + DZ[d];
				if(podeEntrar(Mundo.obterBlocoMundo(nx, y, nz), blocoAtual, tipo.densidade)) {
					colocar(nx, y, nz, blocoAtual, nivelViz, numTick + tipo.viscosidade);
				}
			}
		}
		if(fonte) agendar(x, y, z, numTick + tipo.viscosidade);
	}

	public static void notificar(int x, int y, int z, int blocoId, int numTick, int viscosidade) {
		final int b = Mundo.obterBlocoMundo(x, y, z);
		if(b != blocoId) return;
		final int meta = Mundo.obterMetaMundo(x, y, z) & 0xFFFF;
		if((meta & MARCA_FONTE) != 0) return;
		agendar(x, y, z, numTick + viscosidade);
	}

	public static boolean podeEntrar(int blocoAlvo, int blocoFluido, float densidadeAtual) {
		if(blocoAlvo == 0) return true;
		final Bloco b = Bloco.numIds.get(blocoAlvo);
		if(b == null) return false;
		if(b.render != TipoRender.LIQUIDO) return false;
		return densidadeAtual > b.densidade;
	}

	public static void colocar(int x, int y, int z, int blocoId, int nivel, int tickAlvo) {
		final Chunk chunk = Mundo.obterChunk(x >> 4, z >> 4);
		if(chunk == null) return;
		final int metaAtual = ChunkProcesso.util.obterMeta(x & 0xF, y, z & 0xF, chunk) & 0xFFFF;
		if((metaAtual & MARCA_FONTE) != 0 || (metaAtual & MASCARA_NIVEL) >= nivel) return;
		ChunkProcesso.util.defBloco(x & 0xF, y, z & 0xF, blocoId, chunk);
		ChunkProcesso.util.defMeta(x & 0xF, y, z & 0xF, (short) nivel, chunk);
		chunk.att = true;
		agendar(x, y, z, tickAlvo);
	}

	public static void colocarChunk(int x, int y, int z, int blocoId, int nivel, Chunk chunk) {
		if(chunk == null) return;
		final int nivelAtual = ChunkProcesso.util.obterMeta(x & 0xF, y, z & 0xF, chunk) & MASCARA_NIVEL;
		if(nivelAtual >= nivel) return;
		ChunkProcesso.util.defBloco(x & 0xF, y, z & 0xF, blocoId, chunk);
		ChunkProcesso.util.defMeta(x & 0xF, y, z & 0xF, (short) nivel, chunk);
		chunk.att = true;
		agendar(x, y, z, tickAtual + 1);
	}

	public static void colocarFonte(int x, int y, int z, String nomeFluido) {
		final Bloco b = Bloco.texIds.get(nomeFluido);
		if(b == null || b.render != TipoRender.LIQUIDO) return;
		final Chunk chunk = Mundo.obterChunk(x >> 4, z >> 4);
		if(chunk == null) return;
		ChunkProcesso.util.defBloco(x & 0xF, y, z & 0xF, b.tipo, chunk);
		ChunkProcesso.util.defMeta(x & 0xF, y, z & 0xF, (short)(MARCA_FONTE | NIVEL_FONTE), chunk);
		chunk.att = true;
		agendar(x, y, z, tickAtual + b.viscosidade);
	}

	public static void remover(int x, int y, int z) {
		final int blocoId = Mundo.obterBlocoMundo(x, y, z);
		Mundo.defBlocoMundo(x, y, z, 0);
		Mundo.defMetaMundo(x, y, z, (short) 0);
		final Chunk chunk = Mundo.obterChunk(x >> 4, z >> 4);
		if(chunk != null) chunk.att = true;
		final Bloco tipo = Bloco.numIds.get(blocoId);
		if(tipo == null) return;
		for(int d = 0; d < 4; d++) {
			notificar(x + DX[d], y, z + DZ[d], blocoId, tickAtual, tipo.viscosidade);
		}
		notificar(x, y - 1, z, blocoId, tickAtual, tipo.viscosidade);
	}
}
