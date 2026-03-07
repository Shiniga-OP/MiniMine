package com.minimine.mundo.geracao;

import com.minimine.utils.ruidos.OpenSimplex2;

/*
 * cria formas continentais orgânicas via domain warping em 3 camadas
 * usa sementes distintas por campo para evitar correlação entre camadas
 */
public class DominioDeformacao {

    // sementes dos tres campos independentes
    public final long semente1;
    public final long semente2;
    public final long semente3;

    public DominioDeformacao(final long semente) {
        this.semente1 = semente;
        this.semente2 = semente ^ 0x9E3779B97F4A7C15L;
        this.semente3 = semente ^ 0x3C6EF372FE94F82AL;
    }

    public double obterElevacaoContinental(double x, double z) {
        // === camada 1: forma continental de baixissima frequencia ===
        // escala ~0.00005 -> período ~20000 blocos -> continentes verdadeiros
        double continente = OpenSimplex2.ruido2Fractal(semente1, x * 0.00005, z * 0.00005, 2, 0.4, 2.2);

        // === camada 2: distorção de domínio em escala média ===
        // posição grande(3000/5000) evita correlação entre eixos X e Z
        double tamX = OpenSimplex2.ruido2Fractal(semente2, x * 0.00018, z * 0.00018, 2, 0.5, 2.0);
        double tamZ = OpenSimplex2.ruido2Fractal(semente2, x * 0.00018 + 3000, z * 0.00018 + 5000, 2, 0.5, 2.0);

        double x1 = x + tamX;
        double z1 = z + tamZ;

        // === camada 3: detalhe regional(bordas de costas, golfos) ===
        double posX2 = OpenSimplex2.ruido2Fractal(semente3, x1 * 0.00028, z1 * 0.00028, 2, 0.45, 2.0) * 280.0;
        double posZ2 = OpenSimplex2.ruido2Fractal(semente3, x1 * 0.00028 + 800, z1 * 0.00028 + 1200, 2, 0.45, 2.0) * 280.0;

        double regional = OpenSimplex2.ruido2Fractal(semente1, (x1 + posX2) * 0.00038, (z1 + posZ2) * 0.00038, 3, 0.5, 2.0);

        // continental dita a forma geral, regional adiciona detalhe costeiro
        return continente * 0.65 + regional * 0.35;
    }

    // buffer reutilizado pelo chamador
    public void obterGradiente(double x, double z, double delta, double[] saida) {
        double altura0 = obterElevacaoContinental(x, z);
        saida[0] = (obterElevacaoContinental(x + delta, z) - altura0) / delta;
        saida[1] = (obterElevacaoContinental(x, z + delta) - altura0) / delta;
    }
}

