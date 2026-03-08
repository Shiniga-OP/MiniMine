package com.minimine.mundo.geracao;

import com.minimine.utils.ruidos.OpenSimplex2;
/*
 * gera rios escavando o terreno
 * thread-segura: sem estado mutavel
 * todos os arrays de trabalho vem de ContextoGeracao
 
 *  crista_subaquat: pos=0, escala=1, espalhar=1000, oct=5, persist=0.6, lac=2.0
 *  crista: pos=0, escala=1, espalhar=100, oct=4, persist=0.75, lac=2.0
 
 * limite de canal: 0.6f
 
 * fluxo de uso:
 *   1. calcularChunk(chunkX, chunkY, chunkZ, alturaChunk, ctx)
 *      popula ctx.subaquatMapa[](2D) e ctx.cristaMapaa[](3D)
 *   2. para cada bloco: eCanal(x, yLocal, z, yAbs, ctx)
 *      yLocal: indice Y local no cristaMapaa[0, alturaChunk-1]
 *      yAbs: Y mundial absoluto
 */
public final class GeradorRios {
    public static final float LARGURA = 0.2f;
    public final int nivelMar;

    // parametros imutaveis, compartilhaveis entre threads
    public final long semSubaquat, semCrista;
    public final float espalharSubaquat, espalharCrista;
    public final int octSubaquat, octCrista;
    public final float perSubaquat, perCrista, lacSubaquat, lacCrista;

    public GeradorRios(long semente, int nivelMar) {
        this.nivelMar = nivelMar;
        semSubaquat = semente ^ 0xDEADBEEF12345678L;
        semCrista = semente ^ 0x3C6EF372FE94F82AL;
        espalharSubaquat = 1000f;
		octSubaquat = 5;
		perSubaquat = 0.60f;
		lacSubaquat = 2.0f;
        espalharCrista = 100f;
		octCrista = 4;
		perCrista = 0.75f;
		lacCrista = 2.0f;
    }
    /*
     * pré-calcula ctx.subaquatMapa[](2D) e ctx.cristaMapa[](3D).
     * equivalente ao V7:
     *   ruido_crista_subaquat->ruidoMapa2D(node_min.X, node_min.Z)
     *   ruido_crista->ruidoMap3D(node_min.X, node_min.Y-1, node_min.Z)
     */
    public void calcularChunk(int chunkX, int chunkY, int chunkZ, int alturaChunk, ContextoGeracao ctx) {
        double freqU = 1.0 / espalharSubaquat;
        for(int z = 0; z < 16; z++) {
            for(int x = 0; x < 16; x++) {
                float u = (float)OpenSimplex2.ruido2Fractal(semSubaquat,
				(chunkX + x) * freqU, (chunkZ + z) * freqU, octSubaquat, perSubaquat, lacSubaquat);
                ctx.subaquatMapa[z * 16 + x] = Math.abs(u) * 2.0f;
            }
        }
        double freqR = 1.0 / espalharCrista;
        int fatiaXZ = 16 * 16; 
        for(int y = 0; y < alturaChunk; y++) {
            for(int z = 0; z < 16; z++) {
                for(int x = 0; x < 16; x++) {
                    ctx.cristaMapa[y * fatiaXZ + z * 16 + x] = (float)OpenSimplex2.ruido3XZFractal(
						semCrista,
						(chunkX + x) * freqR,
						(chunkY + y) * freqR,
						(chunkZ + z) * freqR,
						octCrista, perCrista, lacCrista);
                }
            }
        }
    }
    // retorna true se o bloco é canal de rio
    public boolean eCanal(int x, int yLocal, int z, int yAbs, int superficieY, ContextoGeracao ctx) {
		float abSubaquat = ctx.subaquatMapa[z * 16 + x];
		if(abSubaquat > LARGURA) return false;
		if(yAbs < superficieY - 4) return false; // só escava perto da superfície

		float altitude = yAbs - nivelMar;
		float horizontalMod = (altitude + 17.0f) / 2.5f;
		float verticalMod = LARGURA - abSubaquat;
		float crista = ctx.cristaMapa[yLocal * 256 + z * 16 + x];
		float ncrista = crista * Math.max(altitude, 0.0f) / 7.0f;

		return ncrista + verticalMod * horizontalMod >= 0.6f;
	}

    // zona de rio para ponto arbitrario, uso esporadico, fora do loop de chunk
    public boolean eZonaRio(int mx, int mz) {
        float u = (float)OpenSimplex2.ruido2Fractal(semSubaquat,
		mx / (double)espalharSubaquat, mz / (double)espalharSubaquat, octSubaquat, perSubaquat, lacSubaquat);
        return Math.abs(u) * 2.0f <= LARGURA;
    }
}

