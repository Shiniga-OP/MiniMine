package com.minimine.mundo.geracao;

import com.minimine.utils.ruidos.Simplex2D;
import com.minimine.utils.ruidos.Simplex3D;
import com.minimine.utils.ruidos.RidgeNoise2D;

public class GeradorTerreno {
    public final DominioDeformacao dominio;
    public final RidgeNoise2D ridge;
    public final Simplex2D ruido;
    public final Simplex3D ruido3d;
    public final Simplex3D cavernas;
    public final Simplex3D cavernasProfundas;
    public final ErosaoHidraulica erosao;

    public final long semente;
    public final int nivelMar = 62;

    public GeradorTerreno(long semente) {
        this.semente = semente;
        this.dominio = new DominioDeformacao(semente);
        this.ridge = new RidgeNoise2D(semente ^ 0x5DEECE66DL);
        this.ruido = new Simplex2D(semente ^ 0x9E3779B9L);
        this.ruido3d = new Simplex3D(semente ^ 0x61C88647L);

        // novos ruidos para cavernas em diferentes camadas
        this.cavernas = new Simplex3D(semente ^ 0x1A2B3C4DL);
        this.cavernasProfundas = new Simplex3D(semente ^ 0x9F8E7D6CL);

        // pré computa erosão pra uma região
        this.erosao = new ErosaoHidraulica(semente, 512, 8.0);
        this.erosao.simularGotas(3000, dominio);
    }

    public int[] calcularDadosColuna(int x, int z) {
        // 1. base continental com dominio distorção
        double base = dominio.obterElevacaoContinental(x, z);

        // 2. identifica tipo de terreno(oceano, planicie, montanha)
        double tipoTerreno = identificarTipo(base);
        double altura = base;

        // 3. suavizaçãp: reduz a intensidade das montanhas para criar mais areas planas
        double suavizacao = ruido.ruido(x * 0.0002, z * 0.0002) * 0.5 + 0.5;

        // se for terreno elevado, adiciona detalhes de montanhas
        if(tipoTerreno > 0.3) {
            double montanhas = ridge.ridgeFractal(x * 0.0008, z * 0.0008, 2, 2.2, 0.5);
            double cordilheiras = ridge.ridgeBilateral(x * 0.0004, z * 0.0004, 2, 2.0, 0.5);

            double fatorMontanha = (tipoTerreno - 0.3) / 0.7;

            // reduz a contribuição das montanhas em 40%
            altura += montanhas * fatorMontanha * 0.48;
            altura += cordilheiras * fatorMontanha * 0.24;

            // aplica suavização progressiva
            altura = altura * (0.7 + suavizacao * 0.3);

            if(fatorMontanha > 0.5) {
                double rochoso = ridge.swiss(x * 0.002, z * 0.002, 2, 2.0, 0.4, 0.5);
                altura += rochoso * (fatorMontanha - 0.5) * 0.12;
            }
        } else {
            // areas baixas pra ter transições mais suaves
            double transicao = ruido.ruidoFractal(x * 0.001, z * 0.001, 2, 0.5, 2.0);
            altura += transicao * 0.05 * (1.0 - tipoTerreno);
        }
        // turbulencia jordan pra micro variações de solo
        double turbulencia = ridge.jordan(x * 0.001, z * 0.001, 3, 2.1, 1.0, 0.5);
        altura += turbulencia * 0.08;

        // aplica erosão pré calculada
        double valorErosao = erosao.obterErosaoInterpolada(x, z);
        altura += valorErosao * 0.1;

        // detalhes finais de superficie
        double detalhe1 = ruido.ruidoFractal(x * 0.01, z * 0.01, 3, 0.5, 2.0) * 0.05;
        double detalhe2 = ruido.ruidoFractal(x * 0.03, z * 0.03, 2, 0.5, 2.0) * 0.03;
        altura += detalhe1 + detalhe2;

        // converte escala -1,1 para blocos reais
        int blocos;
        if(altura < 0) {
            blocos = nivelMar + (int)(altura * 25.0);
        } else {
            // reduz a escala vertical em 25% para terrenos menos ingrimes
            blocos = nivelMar + (int)(altura * 97.0);
        }
        // variações 3D pra criar saliencias e pequenos tuneis superficiais
        if(blocos > nivelMar + 25) {
            double var3d = ruido3d.ruidoFractal(x * 0.04, blocos * 0.08, z * 0.04, 1, 0.5, 2.0);
            if(var3d > 0.45) {
                blocos += (int)((var3d - 0.45) * 10.0);
            }
        }
        int alturaFinal = Math.max(1, Math.min(240, blocos));
        // passa a base continental ja calculada pra evitar reprocessamento no bioma
        TipoBioma bioma = determinarBioma(x, z, alturaFinal, base);

        return new int[] { alturaFinal, bioma.ordinal() };
    }

    public double identificarTipo(double base) {
        if(base < -0.2) return 0.0; // oceano profundo
        if(base < 0.0) return (base + 0.2) / 0.2 * 0.2; // transição mar
        // aumento o raio de planícies para ter mais áreas planas
        if(base < 0.35) return 0.2 + (base / 0.35) * 0.2; // planicies expandidas
        return 0.4 + (Math.min(base - 0.35, 0.65) / 0.65) * 0.6; // montanhas
    }

    public TipoBioma determinarBioma(int x, int z, int altura, double base) {
        // temperatura baseada na latitude(Z) e altura
        double distEquador = Math.abs(z * 0.00015);
        double temp = 1.0 - distEquador * 0.7;
        temp -= Math.max(0, (altura - nivelMar) * 0.004);

        // umidade baseada na proximidade com oceano (base continental baixa)
        double umidade = Math.exp(-Math.max(0, base) * 2.0);
        double varClima = ruido.ruidoFractal(x * 0.0003, z * 0.0003, 4, 0.6, 2.0);
        umidade += varClima * 0.3;

        // define bioma
        if(altura <= nivelMar) {
            if(altura < nivelMar - 20) return TipoBioma.OCEANO_PROFUNDO;
            if(temp > 0.7) return TipoBioma.OCEANO_QUENTE;
            return TipoBioma.OCEANO;
        }
        // mais blocos de praia para transição suave
        if(altura < nivelMar + 5) return TipoBioma.OCEANO_COSTEIRO;

        if(umidade < 0.25) {
            return altura > nivelMar + 15 ? TipoBioma.COLINAS_DESERTO : TipoBioma.DESERTO;
        }
        if(umidade > 0.55) {
            if(altura > nivelMar + 35) return TipoBioma.FLORESTA_MONTANHOSA;

            double rio = Math.abs(ruido.ruido(x * 0.008, z * 0.008));
            if(rio < 0.08) return TipoBioma.FLORESTA_COM_RIOS;

            if(base < 0.05) return TipoBioma.FLORESTA_COSTEIRA;
            return TipoBioma.FLORESTA;
        }
        double lago = ruido.ruido(x * 0.015, z * 0.015);
        if(lago < -0.5 && altura < nivelMar + 8) return TipoBioma.PLANICIES_AGUADAS;

        return altura > nivelMar + 20 ? TipoBioma.PLANICIES_MONTANHOSAS : TipoBioma.PLANICIES;
    }

    // verifica se deve haver caverna nesta posição
    public boolean temCaverna(int x, int y, int z) {
        // cavernas em camadas diferentes com ruídos distintos
        if(y < 10) return false; // não gera cavernas muito perto do bedrock
        if(y > 180) return false; // não gera cavernas muito alto

        // cavernas principais(camada media)
        if(y >= 20 && y <= 80) {
            double valor = cavernas.ruidoFractal(x * 0.02, y * 0.03, z * 0.02, 3, 0.5, 2.0);
            if(valor > 0.6) return true;
        }
        // cavernas profundas(mais raras e maiores)
        if(y >= 10 && y <= 50) {
            double valor = cavernasProfundas.ruidoFractal(x * 0.015, y * 0.025, z * 0.015, 2, 0.5, 2.0);
            if(valor > 0.65) return true;
        }
        // cavernas superficiais(pequenas)
        if(y >= 60 && y <= 120) {
            double valor = ruido3d.ruidoFractal(x * 0.025, y * 0.035, z * 0.025, 2, 0.5, 2.0);
            if(valor > 0.7) return true;
        }
        // tuneis horizontais usando ruido verme
        double tunel = cavernas.ruido(x * 0.01, y * 0.5, z * 0.01);
        double espessura = Math.abs(ruido3d.ruido(x * 0.03, y * 0.1, z * 0.03));
        if(tunel > 0.5 && espessura < 0.15) return true;

        return false;
    }

    public enum TipoBioma {
        OCEANO, OCEANO_COSTEIRO, OCEANO_QUENTE, OCEANO_PROFUNDO,
        PLANICIES, PLANICIES_MONTANHOSAS, PLANICIES_AGUADAS,
        FLORESTA, FLORESTA_COSTEIRA, FLORESTA_COM_RIOS, FLORESTA_MONTANHOSA,
        DESERTO, COLINAS_DESERTO
		}
}

