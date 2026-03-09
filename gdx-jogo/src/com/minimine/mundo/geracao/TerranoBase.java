package com.minimine.mundo.geracao;

import com.minimine.utils.ruidos.OpenSimplex2;
/*
 * calcula a altura base do terreno para uma chunk inteira.
 * thread-segura: sem estado mutavel, todos os arrays de trabalho vem de ContextoGeracao
 
 *   1. persist(espalhar=2000): rugosidade regional, calculado primeiro
 *   2. terreno_base(espalhar=600, escala=70): com persist variavel por ponto
 *   3. terreno_alt(espalhar=600, escala=25): com persist variavel por ponto
 *   4. altura_sele(espalhar=500): rangelim(0,1), interpola base/alt
 *   5. alt > base -> retorna alt; senão -> base*asel + alt*(1-asel)
 
 *   terreno_base: pos=4, escala=70, espalhar=600, oct=5, persist=0.6, lac=2.0
 *   terreno_alt: pos=4, escala=25, espalhar=600, oct=5, persist=0.6, lac=2.0
 *   terreno_persis: pos=0.6, escala=0.1, espalhar=2000, oct=3, persist=0.6, lac=2.0
 *   altura_sele: pos=-8, escala=16, espalhar=500, oct=6, persist=0.7, lac=2.0
 
 * resultado em ctx.superficeMapa[z*16+x]: Y absoluto em blocos
 * nivelMar ajusta o pos(agua_nivel=1) para nosso sistema(agua_nivel=62)
 */
public final class TerranoBase {
    public final int nivelMar;

    // parametros imutáveis dos ruidos, compartilhaveis entre threads
    public final long semPersist, semBase, semAlt, semSeletor;
    public final float posPersist, escalaPersist, espalharPersist;
    public final float posBase, escalaBase, espalharBase;
    public final float posAlt, escalaAlt, espalharAlt;
    public final float posSel, escalaSel, espalharSel;
    public final int octPersist, octBase, octAlt, octSel;
    public final float lacPersist, lacBase, lacAlt, lacSel;
    public final float perSel;

    public TerranoBase(long semente, int nivelMar) {
        this.nivelMar = nivelMar;

        semPersist = semente ^ 0x61C88647L;
        semBase = semente ^ 0x9E3779B9L;
        semAlt = semente ^ 0x5DEECE66DL;
        semSeletor = semente ^ 0x1A2B3C4DL;

        posPersist = 0.6f;
		escalaPersist = 0.1f;
		espalharPersist = 2000f; octPersist = 3;
		lacPersist = 2.0f;
        posBase = 66.0f; // 62 + 4, altura média do terreno terrestre
		escalaBase = 70.0f;
		espalharBase = 600f;
		octBase = 5;
		lacBase = 2.0f;
        posAlt = 66.0f;  // 62 + 4
		escalaAlt = 25.0f;
		espalharAlt = 600f;
		octAlt = 5;
		lacAlt = 2.0f;
        posSel = 0.0f;
		escalaSel = 16.0f;
		espalharSel = 500f;
		octSel = 6;
		lacSel = 2.0f;
        perSel = 0.7f;
    }
    /*
     * pré-calcula ctx.superficeMapa[] pra chunk em(chunkX, chunkZ)
     * escreve nos arrays do ContextoGeracao da thread, sem acesso a estado compartilhado
	*/
    public void calcularChunk(int chunkX, int chunkZ, ContextoGeracao ctx) {
        // persist
        calcular2D(semPersist, posPersist, escalaPersist, espalharPersist, octPersist, lacPersist,
		0.6f, chunkX, chunkZ, ctx.persistMapa);
        // base com persist variável
        calcular2DComPersist(semBase, posBase, escalaBase, espalharBase, octBase, lacBase,
		chunkX, chunkZ, ctx.baseMapa, ctx.persistMapa);
        // alt com persist variável
        calcular2DComPersist(semAlt, posAlt, escalaAlt, espalharAlt, octAlt, lacAlt,
		chunkX, chunkZ, ctx.altMapa, ctx.persistMapa);
        // seletor
        calcular2D(semSeletor, posSel, escalaSel, espalharSel, octSel, lacSel,
		perSel, chunkX, chunkZ, ctx.seletorMapa);

        for(int i = 0; i < 16 * 16; i++) {
            float base = ctx.baseMapa[i];
            float alt = ctx.altMapa[i];
            float asel = Math.max(0f, Math.min(1f, ctx.seletorMapa[i]));
            float altura = (alt > base) ? alt : (base * asel + alt * (1f - asel));
            ctx.superficeMapa[i] = altura;
        }
    }

    // altura inteira para posição local(x,z), apos calcularChunk()
    public int obterAltura(int x, int z, ContextoGeracao ctx) {
        return Math.max(1, Math.min(240, (int) ctx.superficeMapa[z * 16 + x]));
    }

    // ponto arbitrário fora do loop de chunk, uso esporadico
    public int calcularAlturaPonto(int mx, int mz) {
        float persist = avaliar2D(semPersist, posPersist, escalaPersist, espalharPersist, octPersist, 0.6f, lacPersist, mx, mz);
        float base = avaliar2DPersist(semBase, posBase, escalaBase, espalharBase, octBase, lacBase, mx, mz, persist);
        float alt = avaliar2DPersist(semAlt,  posAlt,  escalaAlt,  espalharAlt,  octAlt,  lacAlt,  mx, mz, persist);
        float asel = Math.max(0f, Math.min(1f,
		avaliar2D(semSeletor, posSel, escalaSel, espalharSel, octSel, perSel, lacSel, mx, mz)));
        float altura = (alt > base) ? alt : (base * asel + alt * (1f - asel));
        return Math.max(1, Math.min(240, (int)altura));
    }

    // === UTILITARIOS DE CALCULO ===
    public void calcular2D(long sem, float pos, float escala, float espalhar, int oct, float lac,
	float persist, int origemX, int origemZ, float[] saida) {
        float freq = 1.0f / espalhar;
        for(int z = 0; z < 16; z++) {
            for(int x = 0; x < 16; x++) {
                saida[z * 16 + x] = pos + escala * OpenSimplex2.ruido2Fractal(
					sem, (origemX + x) * freq, (origemZ + z) * freq, oct, persist, lac);
			}
		}
    }

    public void calcular2DComPersist(long sem, float pos, float escala, float espalhar, int oct, float lac,
	int origemX, int origemZ, float[] saida, float[] persistMapa) {
        float freq = 1.0f / espalhar;
        for(int z = 0; z < 16; z++) {
            for(int x = 0; x < 16; x++) {
                int idc = z * 16 + x;
                float persist = persistMapa[idc];
                float nx = (origemX + x) * freq, nz = (origemZ + z) * freq;
                float total = 0, amplitude = 1, frequencia = 1, maximo = 0;
                long s = sem;
                for(int i = 0; i < oct; i++) {
                    total += OpenSimplex2.ruido2(s, nx * frequencia, nz * frequencia) * amplitude;
                    maximo += amplitude;
                    s = s * 6364136223846793005L + 1442695040888963407L;
                    amplitude *= persist;
                    frequencia *= lac;
                }
                saida[idc] = pos + escala * (maximo > 0 ? total / maximo : 0);
            }
        }
    }

    public float avaliar2D(long sem, float pos, float escala, float espalhar, int oct, float persist, float lac, int mx, int mz) {
        return pos + escala * OpenSimplex2.ruido2Fractal(sem, mx / espalhar, mz / espalhar, oct, persist, lac);
    }

    public float avaliar2DPersist(long sem, float pos, float escala, float espalhar, int oct, float lac, int mx, int mz, float persist) {
        float freq = 1.0f / espalhar;
        float total = 0, amplitude = 1, frequencia = 1, maximo = 0;
        long s = sem;
        for(int i = 0; i < oct; i++) {
            total += OpenSimplex2.ruido2(s, mx * freq * frequencia, mz * freq * frequencia) * amplitude;
            maximo += amplitude;
            s = s * 6364136223846793005L + 1442695040888963407L;
            amplitude *= persist;
            frequencia *= lac;
        }
        return pos + escala * (maximo > 0 ? total / maximo : 0);
    }
}

