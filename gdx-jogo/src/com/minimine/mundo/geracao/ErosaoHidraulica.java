package com.minimine.mundo.geracao;

import com.minimine.utils.ruidos.Simplex2D;
/*
 * simulação de erosão hidraulica usando particulas de água
 * baseia em papeis de geração procedural(olsen, mei, etc)
 */
public class ErosaoHidraulica {
    public final Simplex2D ruido;
    public final double[][] campoErosao;
    public final int tamMapa;
    public final double escala;
	
    // parametros fisicos
    public static final double CAPACIDADE_SEDIMENTO = 4.0;
    public static final double TAXA_DEPOSICAO = 0.3;
    public static final double TAXA_EROSAO = 0.3;
    public static final double EVAPORACAO = 0.01;
    public static final double GRAVIDADE = 4.0;
    public static final double INERCIA = 0.05;

    public ErosaoHidraulica(long semente, int tamMapa, double escala) {
        this.ruido = new Simplex2D(semente);
        this.tamMapa = tamMapa;
        this.escala = escala;
        this.campoErosao = new double[tamMapa][tamMapa];
    }
    // simula gotas de chuva que erodem o terreno
    public void simularGotas(int numGotas, DominioDeformacao dominio) {
        for(int i = 0; i < numGotas; i++) {
            // posição inicial aleatoria
            double x = ruido.ruido(i * 123.456, i * 789.012) * tamMapa * escala;
            double z = ruido.ruido(i * 456.789, i * 234.567) * tamMapa * escala;

            simularGota(x, z, dominio);
        }
    }

    public void simularGota(double x, double z, DominioDeformacao dominio) {
        double dirX = 0;
        double dirZ = 0;
        double velocidade = 1.0;
        double agua = 1.0;
        double sedimento = 0.0;

        for(int vida = 0; vida < 20; vida++) {
            // posição na grade
            int xi = (int)(x / escala);
            int zi = (int)(z / escala);

            if(xi < 0 || xi >= tamMapa - 1 || zi < 0 || zi >= tamMapa - 1) {
                break;
            }
            // altura atual
            double alturaAtual = dominio.obterElevacaoContinental(x, z);

            // calcula gradiente
			final double[] gradBuffer = new double[2];
            dominio.obterGradiente(x, z, escala, gradBuffer);
			double gradX = gradBuffer[0];
			double gradZ = gradBuffer[1];

            // atualiza direção com inercia
            dirX = dirX * INERCIA - gradX * (1.0 - INERCIA);
            dirZ = dirZ * INERCIA - gradZ * (1.0 - INERCIA);

            // normaliza direção
            double tam = Math.sqrt(dirX * dirX + dirZ * dirZ);
            if(tam > 0) {
                dirX /= tam;
                dirZ /= tam;
            }
            // move particula
            double xNovo = x + dirX;
            double zNovo = z + dirZ;

            // altura nova
            double alturaNova = dominio.obterElevacaoContinental(xNovo, zNovo);

            // diferença de altura
            double deltaA = alturaNova - alturaAtual;

            // capacidade de carregar sedimento
            double capacidade = Math.max(-deltaA, 0.01) * velocidade * agua * CAPACIDADE_SEDIMENTO;

            // erosão ou deposição
            if(sedimento > capacidade) {
                // deposita excesso
                double deposito = (sedimento - capacidade) * TAXA_DEPOSICAO;
                sedimento -= deposito;
                campoErosao[xi][zi] += deposito;
            } else {
                // erode
                double erosao = Math.min((capacidade - sedimento) * TAXA_EROSAO, -deltaA);
                sedimento += erosao;
                campoErosao[xi][zi] -= erosao;
            }
            // atualiza velocidade
            velocidade = Math.sqrt(velocidade * velocidade + deltaA * GRAVIDADE);
            agua *= (1.0 - EVAPORACAO);

            // para se ficou sem água
            if(agua < 0.01) {
                break;
            }
            x = xNovo;
            z = zNovo;
        }
    }
    // obtem o valor de erosão acumulada em uma posição
    public double obterErosao(double x, double z) {
        int xi = (int)(x / escala);
        int zi = (int)(z / escala);

        if(xi < 0 || xi >= tamMapa || zi < 0 || zi >= tamMapa) {
            return 0;
        }
        return campoErosao[xi][zi];
    }

    // obtem erosão interpolada bilinear
    public double obterErosaoInterpolada(double x, double z) {
        double fx = x / escala;
        double fz = z / escala;

        int x0 = (int)Math.floor(fx);
        int z0 = (int)Math.floor(fz);
        int x1 = x0 + 1;
        int z1 = z0 + 1;

        if(x0 < 0 || x1 >= tamMapa || z0 < 0 || z1 >= tamMapa) {
            return 0;
        }
        double sx = fx - x0;
        double sz = fz - z0;

        double e00 = campoErosao[x0][z0];
        double e10 = campoErosao[x1][z0];
        double e01 = campoErosao[x0][z1];
        double e11 = campoErosao[x1][z1];

        double e0 = e00 * (1.0 - sx) + e10 * sx;
        double e1 = e01 * (1.0 - sx) + e11 * sx;

        return e0 * (1.0 - sz) + e1 * sz;
    }
}

