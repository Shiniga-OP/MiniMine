package com.minimine.mundo.chunks;

import java.util.concurrent.atomic.AtomicReferenceArray;

/*
 * grade circular 2D de chunks, tamanho fixo por raio.
 * índice = floorMod(chunkX, diametro) * diametro + floorMod(chunkZ, diametro)
 * ao ler um slot, sempre verificar chunk.x == x && chunk.z == z
 * pois dois coords distintos podem mapear pro mesmo slot.
 *
 * thread-safety: AtomicReferenceArray garante visibilidade sem locks.
 * single-writer (TarefasUtil.mundo) / multi-reader (entidades, fisica, GL).
 */
public class GradeChunk {
	public volatile AtomicReferenceArray<Chunk> grade;
	public volatile int diametro;

	public GradeChunk(int raio) {
		this.diametro = (raio * 2) + 1;
		this.grade = new AtomicReferenceArray<Chunk>(diametro * diametro);
	}

	public static int floorMod(int a, int b) {
		return ((a % b) + b) % b;
	}

	public int idc(int chunkX, int chunkZ) {
		return floorMod(chunkX, diametro) * diametro + floorMod(chunkZ, diametro);
	}

	public Chunk obter(int chunkX, int chunkZ) {
		final AtomicReferenceArray<Chunk> g = grade;
		final int d = diametro;
		final Chunk c = g.get(floorMod(chunkX, d) * d + floorMod(chunkZ, d));
		if(c != null && c.x == chunkX && c.z == chunkZ) return c;
		return null;
	}

	public void def(int chunkX, int chunkZ, Chunk chunk) {
		grade.set(idc(chunkX, chunkZ), chunk);
	}

	public void rm(int chunkX, int chunkZ) {
		final int i = idc(chunkX, chunkZ);
		final Chunk atual = grade.get(i);
		if(atual != null && atual.x == chunkX && atual.z == chunkZ) {
			grade.set(i, null);
		}
	}
	/*
	 * realoca se o raio mudou. copia chunks existentes pro novo indice
	 * deve ser chamado só pela thread escritora(TarefasUtil.mundo)
	 */
	public void verificarRaio(int raio) {
		final int novoDiametro = (raio * 2) + 1;
		if(novoDiametro == diametro) return;

		final AtomicReferenceArray<Chunk> antiga = grade;
		final AtomicReferenceArray<Chunk> nova = new AtomicReferenceArray<Chunk>(novoDiametro * novoDiametro);

		for(int i = 0; i < antiga.length(); i++) {
			final Chunk c = antiga.get(i);
			if(c == null) continue;
			final int ni = floorMod(c.x, novoDiametro) * novoDiametro + floorMod(c.z, novoDiametro);
			nova.set(ni, c);
		}
		diametro = novoDiametro;
		grade = nova;
	}
	
	public int tam() {
		return grade.length();
	}

	public Chunk obterIdc(int i) {
		return grade.get(i);
	}
}
