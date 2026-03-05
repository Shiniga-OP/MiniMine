package com.minimine.mundo.geracao;

import com.minimine.utils.ruidos.Simplex2D;
/*
 * cria formas continentais organicas
 * usa multiplas camadas de distorção pra evitar padrões repetitivos
 */
public class DominioDeformacao {
    public final Simplex2D ruido1;
    public final Simplex2D ruido2;
    public final Simplex2D ruido3;

    public DominioDeformacao(long semente) {
        this.ruido1 = new Simplex2D(semente);
        this.ruido2 = new Simplex2D(semente ^ 0x9E3779B97F4A7C15L);
        this.ruido3 = new Simplex2D(semente ^ 0x3C6EF372FE94F82AL);
    }

    public double obterElevacaoContinental(double x, double z) {
        // === camada 1: forma continental de baixissima frequencia ===
        // escala ~0.00005 -> periodo ~20000 blocos -> continentes verdadeiros, não ilhas
        // duas oitavas, persistencia baixa -> formas suaves e alongadas
        double continente = ruido1.ruidoFractal(x * 0.00005, z * 0.00005, 2, 0.4, 2.2);

        // === camada 2: distorção de dominio em escala media ===
        // ruido2 distorce antes de amostrar o detalhe regional;
        // posição grande(3000/5000) evita correlação entre eixos
        double warpX = ruido2.ruidoFractal(x * 0.00018, z * 0.00018, 2, 0.5, 2.0) * 600.0;
        double warpZ = ruido2.ruidoFractal(x * 0.00018 + 3000, z * 0.00018 + 5000, 2, 0.5, 2.0) * 600.0;

        double x1 = x + warpX;
        double z1 = z + warpZ;

        // === camada 3: detalhe regional(bordas de costas, golfos) ===
        double posX2 = ruido3.ruidoFractal(x1 * 0.00028, z1 * 0.00028, 2, 0.45, 2.0) * 280.0;
        double posZ2 = ruido3.ruidoFractal(x1 * 0.00028 + 800, z1 * 0.00028 + 1200, 2, 0.45, 2.0) * 280.0;

        double regional = ruido1.ruidoFractal((x1 + posX2) * 0.00038, (z1 + posZ2) * 0.00038, 3, 0.5, 2.0);

        // mistura: continental dita a forma geral, regional adiciona detalhe costeiro
        // peso 0.65/0.35 -> continentes dominam sobre ilhas locais
        return continente * 0.65 + regional * 0.35;
    }

    // sem alocação, passa o buffer, reutilizado a cada chamada
    public void obterGradiente(double x, double z, double delta, double[] saida) {
        double altura0 = obterElevacaoContinental(x, z);
        saida[0] = (obterElevacaoContinental(x + delta, z) - altura0) / delta;
        saida[1] = (obterElevacaoContinental(x, z + delta) - altura0) / delta;
    }
}


