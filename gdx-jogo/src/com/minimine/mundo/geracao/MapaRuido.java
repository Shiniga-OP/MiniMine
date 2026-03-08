package com.minimine.mundo.geracao;

import com.minimine.utils.ruidos.OpenSimplex2;
/*
 * para uma chunk inteira antes do loop de blocos
 
 *   pos: valor medio somado ao resultado(em unidades do dominio, blocos)
 *   escala: amplitude multiplicada sobre o ruído normalizado [-1,1]
 *   espalhamento: comprimento de onda em unidades do domínio (spread)
 *   oitavas: numero de oitavas fBm
 *   persistencia: amplitude relativa de cada oitava
 *   lacunaridade: multiplicador de frequencia por oitava(sempre 2.0)
 
 * resultado em resultado[]:
 *   resultado[i] = pos + escala * fBm(ponto_i)
 *   onde fBm retorna em [-1, 1] aproximadamente
 
 * delegação:
 *   fBm 2D -> OpenSimplex2.ruido2Fractal(avanço LCG correto entre oitavas)
 *   fBm 3D -> OpenSimplex2.ruido3XZFractal(planos XZ primarios, correto para terreno)
 
 * posicionamento:
 *   2D: resultado[z * larguraX + x]
 *   3D: resultado[y * larguraX * larguraZ + z * larguraX + x]
 
 * uso com persistencia variavel:
 *   calcular2DComPersist(origemX, origemZ, larguraX, larguraZ, persistMapa[])
 *   cada ponto usa persistMapa[i]
 *   OpenSimplex2 não expõe fBm com persist por ponto, então iteramos oitavas
 *   manualmente, mas usando o mesmo avanço LCG que ruido2Fractal usa internamente
 */
public final class MapaRuido {
    public final long semente;
    public final float pos;
    public final float escala;
    public final float espalhamento;
    public final int oitavas;
    public final float persistencia;
    public final float lacunaridade;

    // array de resultados pré-calculados, alocado sob demanda, reutilizado entre chunks
    public float[] resultado;

    public MapaRuido(long semente, float pos, float escala, float espalhamento,
	int oitavas, float persistencia, float lacunaridade) {
        this.semente = semente;
        this.pos = pos;
        this.escala = escala;
        this.espalhamento = espalhamento;
        this.oitavas = oitavas;
        this.persistencia = persistencia;
        this.lacunaridade = lacunaridade;
    }

    // === ALOCAÇÃO ===
    public void alocar2D(int larguraX, int larguraZ) {
        int tam = larguraX * larguraZ;
        if(resultado == null || resultado.length < tam) resultado = new float[tam];
    }

    public void alocar3D(int larguraX, int alturaY, int larguraZ) {
        int tam = larguraX * alturaY * larguraZ;
        if(resultado == null || resultado.length < tam) resultado = new float[tam];
    }
	/*
    * === CALCULO 2D ===
	* pré-calcula o mapa 2D para a chunk
	* resultado[z * larguraX + x] = pos + escala * fBm2D(origemX+x, origemZ+z)
    */
    public void calcular2D(int origemX, int origemZ, int larguraX, int larguraZ) {
        alocar2D(larguraX, larguraZ);
        double freqBase = 1.0 / espalhamento;
        for(int z = 0; z < larguraZ; z++) {
            for(int x = 0; x < larguraX; x++) {
                double nx = (origemX + x) * freqBase;
                double nz = (origemZ + z) * freqBase;
                resultado[z * larguraX + x] = pos + escala *
				(float)OpenSimplex2.ruido2Fractal(semente, nx, nz, oitavas, persistencia, lacunaridade);
            }
        }
    }
    /*
     * pré-calcula mapa 2D com persistencia variavel por ponto
     * itera oitavas manualmente usando o mesmo avanço LCG interno do ruido2Fractal,
     * garantindo coerencia entre este metodo e calcular2D()
     */
    public void calcular2DComPersist(int origemX, int origemZ,
	int larguraX, int larguraZ, float[] persistMapa) {
        alocar2D(larguraX, larguraZ);
        double freqBase = 1.0 / espalhamento;

        for(int z = 0; z < larguraZ; z++) {
            for(int x = 0; x < larguraX; x++) {
                int idc = z * larguraX + x;
                double nx = (origemX + x) * freqBase;
                double nz = (origemZ + z) * freqBase;
                float persist = (persistMapa != null) ? persistMapa[idc] : persistencia;

                // oitavas com avanço LCG identico ao ruido2Fractal do OpenSimplex2
                double total = 0.0, amplitude = 1.0, frequencia = 1.0, maximo = 0.0;
                long sem = semente;
                for(int i = 0; i < oitavas; i++) {
                    total += OpenSimplex2.ruido2(sem, nx * frequencia, nz * frequencia) * amplitude;
                    maximo += amplitude;
                    sem = sem * 6364136223846793005L + 1442695040888963407L;
                    amplitude *= persist;
                    frequencia *= lacunaridade;
                }
                resultado[idc] = pos + escala * (float)(maximo > 0 ? total / maximo : 0);
            }
        }
    }

    // === CALCULO 3D ===
    /*
     * pré-calcula o mapa 3D para a chunk
     * indice: y * larguraX * larguraZ + z * larguraX + x
     * usa ruido3XZFractal, planos XZ primários, correto para ruido de terreno vertical
    */
    public void calcular3D(int origemX, int origemY, int origemZ,
	int larguraX, int alturaY, int larguraZ) {
        alocar3D(larguraX, alturaY, larguraZ);
        double freqBase = 1.0 / espalhamento;
        int fatiaXZ = larguraX * larguraZ;
        for(int y = 0; y < alturaY; y++) {
            for(int z = 0; z < larguraZ; z++) {
                for(int x = 0; x < larguraX; x++) {
                    double nx = (origemX + x) * freqBase;
                    double ny = (origemY + y) * freqBase;
                    double nz = (origemZ + z) * freqBase;
                    resultado[y * fatiaXZ + z * larguraX + x] = pos + escala *
					(float)OpenSimplex2.ruido3XZFractal(semente, nx, ny, nz,
					oitavas, persistencia, lacunaridade);
                }
            }
        }
    }

    // === UTILITARIOS ARRAYS ===
    public float obter2D(int x, int z, int larguraX) {
        return resultado[z * larguraX + x];
    }

    public float obter3D(int x, int y, int z, int larguraX, int larguraZ) {
        return resultado[y * larguraX * larguraZ + z * larguraX + x];
    }

    // === AVALIAÇÃO PONTUAL ===
    // avalia o ruido em um unico ponto 2D sem popular resultado[]
    public float avaliar2D(int mx, int mz) {
        double freqBase = 1.0 / espalhamento;
        return pos + escala * (float)OpenSimplex2.ruido2Fractal(
		semente, mx * freqBase, mz * freqBase, oitavas, persistencia, lacunaridade);
    }

    // avalia o ruido em um único ponto 3D sem popular resultado[]
    public float avaliar3D(int mx, int my, int mz) {
        double freqBase = 1.0 / espalhamento;
        return pos + escala * (float)OpenSimplex2.ruido3XZFractal(
		semente, mx * freqBase, my * freqBase, mz * freqBase,
		oitavas, persistencia, lacunaridade);
    }
}

