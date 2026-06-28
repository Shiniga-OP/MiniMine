package com.minimine.mundo.fluidos;

import com.minimine.mundo.Mundo;
import com.minimine.mundo.Chave;
import com.minimine.mundo.chunks.Chunk;
import com.minimine.mundo.chunks.ChunkProcesso;
import com.minimine.mundo.blocos.Bloco;
import com.minimine.graficos.TipoRender;
import java.util.LinkedHashSet;
/*
 * meta do bloco(16 bits):
 * - bits 0-3 = nivel(1–8; 8 = fonte/maximo, 1 = minimo a fluir, 0 = ar/vazio)
 * - bit 8 = marca fonte(1 = fonte estavel/permanente)
 
 * regra universal:
 *   - bloco abaixo: espalha pros 4 lados
 *   - ar abaixo: espalha so pra baixo
*/
public class FluxoFluido {
	public static final int MASCARA_NIVEL = 0xF;
	public static final int MARCA_FONTE = 0x100;
	public static final int NIVEL_FONTE = 8;
	public static final int NIVEL_MIN = 1;

	public static final int LIQUIDO_LOOP_MAX = 2000;

	public static final LinkedHashSet<Long> filaAtual = new LinkedHashSet<Long>();
	public static final LinkedHashSet<Long> filaProximo = new LinkedHashSet<Long>();

	public static final int[] DX4 = {1, -1, 0, 0};
	public static final int[] DZ4 = {0, 0, 1, -1};

	public static final int[] DX6 = {1, -1, 0, 0, 0, 0};
	public static final int[] DY6 = {0, 0, 1, -1, 0, 0};
	public static final int[] DZ6 = {0, 0, 0, 0, 1, -1};

	public static void agendarProximo(int x, int y, int z) {
		filaProximo.add(Chave.gerar3d(x, y, z));
	}

	public static void agendar(int x, int y, int z) {
		filaAtual.add(Chave.gerar3d(x, y, z));
	}

	public static void colocarFonte(int x, int y, int z, String nomeFluido) {
		final Bloco b = Bloco.texIds.get(nomeFluido);
		if(b == null || b.render != TipoRender.LIQUIDO) return;

		final Chunk chunk = Mundo.obterChunk(x >> 4, z >> 4);
		if(chunk == null) return;

		ChunkProcesso.util.defBloco(x & 0xF, y, z & 0xF, b.tipo, chunk);
		ChunkProcesso.util.defMeta(x & 0xF, y, z & 0xF, (short)(MARCA_FONTE | NIVEL_FONTE), chunk);
		chunk.att = true;

		notificarVizinhos(x, y, z);
	}

	public static void tick(int numTick) {
		if(filaAtual.isEmpty()) {
			filaAtual.addAll(filaProximo);
			filaProximo.clear();
			if(filaAtual.isEmpty()) return;
		}
		final Long[] instantanea = filaAtual.toArray(new Long[0]);
		filaAtual.clear();

		int loops = 0;
		for(int i = 0; i < instantanea.length && loops < LIQUIDO_LOOP_MAX; i++, loops++) {
			final Long pos = instantanea[i];
			if(pos == null) continue;
			transformarLiquidoPos(Chave.x3d(pos), Chave.y3d(pos), Chave.z3d(pos));
		}
		for(int i = loops; i < instantanea.length; i++) {
			if(instantanea[i] != null) filaProximo.add(instantanea[i]);
		}
		filaAtual.addAll(filaProximo);
		filaProximo.clear();
	}
	
	public static void transformarLiquidoPos(int x, int y, int z) {
		final int blocoAtual = Mundo.obterBlocoMundo(x, y, z);

		if(blocoAtual != 0 && !ehLiquido(blocoAtual)) {
			Bloco b = Bloco.blocos.get(blocoAtual);
			if(b != null && b.colisao) return;
		}
		final int metaAtual = Mundo.obterMetaMundo(x, y, z) & 0xFFFF;
		final boolean ehFonte = (metaAtual & MARCA_FONTE) != 0;
		final int nivelAtual = ehFonte ? NIVEL_FONTE : (metaAtual & MASCARA_NIVEL);

		if(ehFonte) {
			espalharPraVizinhos(x, y, z, blocoAtual, NIVEL_FONTE);
			return;
		}
		int maiorNivel = 0;
		int tipoFluido = 0;

		// cima: enche ao máximo
		final int blocoCima = Mundo.obterBlocoMundo(x, y + 1, z);
		if(ehLiquido(blocoCima)) {
			final int metaCima = Mundo.obterMetaMundo(x, y + 1, z) & 0xFFFF;
			final int nivelCima = (metaCima & MARCA_FONTE) != 0 ? NIVEL_FONTE : (metaCima & MASCARA_NIVEL);
			if(nivelCima > 0) {
				maiorNivel = NIVEL_FONTE;
				tipoFluido = blocoCima;
			}
		}
		// lados: aceita propagação de vizinhos que tem bloco solido abaixo deles
		if(maiorNivel < NIVEL_FONTE) {
			for(int i = 0; i < 4; i++) {
				final int vx = x + DX4[i];
				final int vz = z + DZ4[i];
				final int bViz = Mundo.obterBlocoMundo(vx, y, vz);
				if(!ehLiquido(bViz)) continue;
				if(blocoAtual != 0 && blocoAtual != bViz) continue;

				// checa se o vizinho que vai se espalhar tem bloco abaixo
				final int bAbaixoViz = y > 0 ? Mundo.obterBlocoMundo(vx, y - 1, vz) : 1;
				if(y > 0 && !ehBlocoSolido(bAbaixoViz)) continue;

				final int mViz = Mundo.obterMetaMundo(vx, y, vz) & 0xFFFF;
				final int nViz = (mViz & MARCA_FONTE) != 0 ? NIVEL_FONTE : (mViz & MASCARA_NIVEL);
				final int nivelPropagado = nViz - 1;

				if(nivelPropagado > maiorNivel) {
					maiorNivel = nivelPropagado;
					tipoFluido = bViz;
				}
			}
		}
		final int novoNivel = maiorNivel;
		final int novoBloco = (novoNivel >= NIVEL_MIN) ? tipoFluido : 0;

		if(novoBloco != blocoAtual || novoNivel != nivelAtual) {
			attBlocoMundo(x, y, z, novoBloco, novoNivel);
			notificarVizinhosProximo(x, y, z);
			if(novoNivel >= NIVEL_MIN) {
				espalharPraVizinhos(x, y, z, novoBloco, novoNivel);
			}
		} else if (novoNivel >= NIVEL_MIN) {
			espalharPraVizinhos(x, y, z, novoBloco, novoNivel);
		}
	}

	public static void espalharPraVizinhos(int x, int y, int z, int fluidoId, int nivel) {
		if(nivel < NIVEL_MIN) return;

		final int bAbaixo = y > 0 ? Mundo.obterBlocoMundo(x, y - 1, z) : 1;
		final boolean temBloco = y == 0 || ehBlocoSolido(bAbaixo);

		if(temBloco) {
			for(int i = 0; i < 4; i++) {
				int nx = x + DX4[i];
				int nz = z + DZ4[i];
				int bLat = Mundo.obterBlocoMundo(nx, y, nz);
				if(bLat == 0 || podeInundar(bLat) || bLat == fluidoId) {
					agendarProximo(nx, y, nz);
				}
			}
		} else {
			agendarProximo(x, y - 1, z);
		}
	}

	public static void notificarVizinhos(int x, int y, int z) {
		agendar(x, y, z);
		for(int i = 0; i < 6; i++) {
			agendar(x + DX6[i], y + DY6[i], z + DZ6[i]);
		}
	}

	public static void notificarVizinhosProximo(int x, int y, int z) {
		agendarProximo(x, y, z);
		for(int i = 0; i < 6; i++) {
			agendarProximo(x + DX6[i], y + DY6[i], z + DZ6[i]);
		}
	}

	public static void attBlocoMundo(int x, int y, int z, int blocoId, int nivel) {
		final Chunk chunk = Mundo.obterChunk(x >> 4, z >> 4);
		if(chunk == null) return;
		ChunkProcesso.util.defBloco(x & 0xF, y, z & 0xF, blocoId, chunk);
		ChunkProcesso.util.defMeta(x & 0xF, y, z & 0xF, (short) nivel, chunk);
		chunk.att = true;
	}

	public static boolean ehLiquido(int blocoId) {
		if(blocoId == 0) return false;
		final Bloco b = Bloco.blocos.get(blocoId);
		return b != null && b.render == TipoRender.LIQUIDO;
	}

	public static boolean ehBlocoSolido(int blocoId) {
		if(blocoId == 0) return false;
		final Bloco b = Bloco.blocos.get(blocoId);
		return b != null && b.colisao;
	}

	public static boolean podeInundar(int blocoId) {
		if(blocoId == 0) return true;
		final Bloco b = Bloco.blocos.get(blocoId);
		return b != null && b.render == TipoRender.RECORTE;
	}
}
