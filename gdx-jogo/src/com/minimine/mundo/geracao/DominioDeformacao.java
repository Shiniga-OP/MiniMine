package com.minimine.mundo.geracao;

import com.minimine.utils.ruidos.Simplex2D;

/*
 * criar formas continentais organicas
 * usa multiplas camadas de distorção para evitar padrões repetitivos
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
        // 1. distorção com ruido2, separado do ruido1 final evita correlação
        double posX = ruido2.ruidoFractal(x * 0.0002, z * 0.0002, 2, 0.5, 2.0) * 400.0;
        double posZ = ruido2.ruidoFractal(x * 0.0002 + 1000, z * 0.0002 + 1000, 2, 0.5, 2.0) * 400.0;

        double x1 = x + posX;
        double z1 = z + posZ;

        // 2. distorção com ruido3, mais fina, dá organicidade sem repetição
        double posX2 = ruido3.ruidoFractal(x1 * 0.0003, z1 * 0.0003, 2, 0.5, 2.0) * 200.0;
        double posZ2 = ruido3.ruidoFractal(x1 * 0.0003 + 500, z1 * 0.0003 + 500, 2, 0.5, 2.0) * 200.0;

        return ruido1.ruidoFractal((x1 + posX2) * 0.0004, (z1 + posZ2) * 0.0004, 3, 0.5, 2.0);
    }

    // sem alocação — caller passa o buffer, reutilizado a cada chamada
    public void obterGradiente(double x, double z, double delta, double[] saida) {
        double altura0 = obterElevacaoContinental(x, z);
        saida[0] = (obterElevacaoContinental(x + delta, z) - altura0) / delta;
        saida[1] = (obterElevacaoContinental(x, z + delta) - altura0) / delta;
    }
}

