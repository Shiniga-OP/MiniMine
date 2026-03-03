package com.minimine.mundo.geracao;

import com.minimine.utils.ruidos.Simplex2D;
import com.minimine.utils.ruidos.Simplex3D;
import com.minimine.utils.ruidos.CristaRuido2D;
import com.minimine.utils.ruidos.CelularRuido2D;

public class GeradorTerreno {
    public final DominioDeformacao dominio;
    public final CristaRuido2D crista;
    public final Simplex2D ruido;
    public final Simplex3D ruido3d;
    public final Simplex3D cavernas;
    public final Simplex3D cavernasProfundas;
    public final CelularRuido2D celular;
    public final ErosaoHidraulica erosao;
    public final long semente;
    public final int nivelMar = 62;

    public GeradorTerreno(long semente) {
        this.semente = semente;
        this.dominio = new DominioDeformacao(semente);
        this.crista = new CristaRuido2D(semente ^ 0x5DEECE66DL);
        this.ruido = new Simplex2D(semente ^ 0x9E3779B9L);
        this.ruido3d = new Simplex3D(semente ^ 0x61C88647L);

        this.cavernas = new Simplex3D(semente ^ 0x1A2B3C4DL);
        this.cavernasProfundas = new Simplex3D(semente ^ 0x9F8E7D6CL);

        this.celular = new CelularRuido2D(semente ^ 0x4F3C2B1AL);

        this.erosao = new ErosaoHidraulica(semente, 512, 8.0);
        this.erosao.simularGotas(500, dominio);
    }

    public void calcularDadosColuna(int x, int z, int[] buffer) {
        double base = dominio.obterElevacaoContinental(x, z);
        double tipoTerreno = identificarTipo(base);
        double altura = base;

        double suavizacao = ruido.ruido(x * 0.0002, z * 0.0002) * 0.5 + 0.5;

        if(tipoTerreno > 0.3) {
            double montanhas = crista.cristaFractal(x * 0.0008, z * 0.0008, 2, 2.2, 0.5);
            double cordilheiras = crista.cristaBilateral(x * 0.0004, z * 0.0004, 2, 2.0, 0.5);
            double fatorMontanha = (tipoTerreno - 0.3) / 0.7;

            altura += montanhas * fatorMontanha * 0.48;
            altura += cordilheiras * fatorMontanha * 0.24;
            altura = altura * (0.7 + suavizacao * 0.3);

            if(fatorMontanha > 0.5) {
                double rochoso = crista.swiss(x * 0.002, z * 0.002, 2, 2.0, 0.4, 0.5);
                altura += rochoso * (fatorMontanha - 0.5) * 0.12;
            }
        } else {
            double transicao = ruido.ruidoFractal(x * 0.001, z * 0.001, 2, 0.5, 2.0);
            altura += transicao * 0.05 * (1.0 - tipoTerreno);
        }
        double turbulencia = crista.jordan(x * 0.001, z * 0.001, 2, 2.1, 1.0, 0.5);
        altura += turbulencia * 0.08;

        double valorErosao = erosao.obterErosaoInterpolada(x, z);
        altura += valorErosao * 0.1;

        double detalhe1 = ruido.ruidoFractal(x * 0.01, z * 0.01, 2, 0.5, 2.0) * 0.05;
        double detalhe2 = ruido.ruidoFractal(x * 0.03, z * 0.03, 2, 0.5, 2.0) * 0.03;
        altura += detalhe1 + detalhe2;

        int blocos;
        if(altura < 0) {
            blocos = nivelMar + (int)(altura * 60.0);
        } else {
            blocos = nivelMar + (int)(altura * 97.0);
        }
        if(blocos > nivelMar + 25) {
            double var3d = ruido3d.ruidoFractal(x * 0.04, blocos * 0.08, z * 0.04, 1, 0.5, 2.0);
            if(var3d > 0.45) {
                blocos += (int)((var3d - 0.45) * 10.0);
            }
        }
        int alturaFinal = Math.max(1, Math.min(240, blocos));
        TipoBioma bioma = determinarBioma(x, z, alturaFinal, base);

        buffer[0] = alturaFinal;
		buffer[1] = bioma.ordinal();
    }

    public double identificarTipo(double base) {
		if(base < -0.5) return -0.5;
		// curva suave continua
		double t = (base + 1.0) / 2.0; // mapeia [-1,1] -> [0,1]
		// passo borrado cubico
		t = t * t * (3.0 - 2.0 * t);
		return -0.5 + t;
	}

    public TipoBioma determinarBioma(int x, int z, int altura, double base) {
        double celularVal = celular.ruido(x * 0.0008, z * 0.0008);

        double distEquador = Math.abs(z * 0.00015);
        double temp = 1.0 - distEquador * 0.7;
        temp -= Math.max(0, (altura - nivelMar) * 0.004);
        temp += (celularVal - 0.3) * 0.2;

		double umidade = ruido.ruidoFractal(x * 0.0005, z * 0.0005, 2, 0.5, 2.0) * 0.5 + 0.5;
        double varClima = ruido.ruidoFractal(x * 0.0003, z * 0.0003, 2, 0.6, 2.0);
        umidade += varClima * 0.3;
        umidade += (1.0 - celularVal) * 0.15;
		
		if(temp < 0.3) {
			if(altura <= nivelMar) {
				return TipoBioma.MAR_CONGELADO;
			}
			return altura > nivelMar + 40 ? TipoBioma.PICOS_GELADOS : TipoBioma.TUNDRA;
		}
        if(altura <= nivelMar) {
            if(altura < nivelMar - 35 || base < -0.7) return TipoBioma.OCEANO_ABISSAL;
            if(temp > 0.7) return TipoBioma.OCEANO_QUENTE;
            return TipoBioma.OCEANO;
        }
        if(altura < nivelMar + 5) return TipoBioma.OCEANO_COSTEIRO;

        if(umidade < 0.25 - celularVal * 0.1) {
            return altura > nivelMar + 15 ? TipoBioma.COLINAS_DESERTO : TipoBioma.DESERTO;
        }
        if(umidade > 0.55 + celularVal * 0.1) {
            if(altura > nivelMar + 35) return TipoBioma.FLORESTA_MONTANHOSA;
            if(base < 0.05) return TipoBioma.FLORESTA_COSTEIRA;
            return TipoBioma.FLORESTA;
        }
        return altura > nivelMar + 20 ? TipoBioma.PLANICIES_MONTANHOSAS : TipoBioma.PLANICIES;
    }
    /*
     * calcula de uma vez quais Y da coluna são vazios(caverna, ravina ou arco)
     * sem mais chamadas separadas a temRavina/temArco/temCaverna por bloco
     */
    public void calcularVaziosColuna(int x, int z, int altura, boolean[] buffer) {
		final boolean podeArco = altura > nivelMar + 20;
		double pilarArco = podeArco ? celular.ruido(x * 0.015, z * 0.015) : 0;
		final boolean arcoViavel = podeArco
			&& pilarArco > 0.35 && pilarArco < 0.55
			&& ruido.ruido(x * 0.02, z * 0.02) > 0.4;

		// limites reais de cada sistema:
		// caverna: y = 10..140
		// ravina: y = (altura-40)..(altura)
		// arco:  y = (altura-25)..(altura+15), mas array vai até altura
		int yMin = Math.max(1, Math.min(altura - 40, 10));
		
		for(int y = yMin; y < altura; y++) {
			boolean ravina = (y >= altura - 40) && temRavina(x, y, z, altura);
			boolean arco = !ravina && arcoViavel && (y >= altura - 25) && temArcoY(x, y, z, altura, true);
			boolean caverna = !ravina && !arco && (y >= 10 && y <= 140) && temCaverna(x, y, z);
			buffer[y] = ravina || arco || caverna;
		}
	}

    // versão interna, recebe arcoViavel ja calculado fora do loop
    private boolean temArcoY(int x, int y, int z, int alturaSuperficie, boolean arcoViavel) {
        if(!arcoViavel) return false;
        if(y < alturaSuperficie - 25 || y > alturaSuperficie + 15) return false;

        double vao = Math.abs(ruido3d.ruido(x * 0.03, y * 0.05, z * 0.03));
        if(vao >= 0.2) return false;

        double distTopo = Math.abs(y - (alturaSuperficie - 5));
        double curvatura = 1.0 - (distTopo * distTopo) / 225.0;
        return curvatura > 0.3;
    }

    public boolean temCaverna(int x, int y, int z) {
        if(y < 10 || y > 140) return false;
        // tres zonas sem sobreposição, cada Y avalia exatamente 1 sistema
        if(y <= 40) {
            // profundas: raras e grandes
            return cavernasProfundas.ruidoFractal(x * 0.015, y * 0.025, z * 0.015, 2, 0.5, 2.0) > 0.65;
        }
        if(y <= 90) {
            // principais: camada do meio
            return cavernas.ruidoFractal(x * 0.02, y * 0.03, z * 0.02, 3, 0.5, 2.0) > 0.62;
        }
        // superficiais: ficam mais raras conforme sobem
        double limiar = 0.68 + (y - 90) * 0.002; // 0.68 em y=90 → 0.78 em y=140
        return ruido3d.ruidoFractal(x * 0.025, y * 0.035, z * 0.025, 2, 0.5, 2.0) > limiar;
    }

    public boolean temRavina(int x, int y, int z, int alturaSuperficie) {
        if(y < alturaSuperficie - 40 || y > alturaSuperficie + 5) return false;

        double ravina1 = Math.abs(ruido3d.ruido(x * 0.012, y * 0.008, z * 0.012));
        if(ravina1 >= 0.08) return false; // sai barato

        double ravina2 = ruido3d.ruido(x * 0.008, y * 0.015, z * 0.008);
        if(ravina2 <= 0.3) return false;

        double distSuperficie = Math.abs(y - alturaSuperficie + 10);
        double fatorProf = Math.max(0, 1.0 - distSuperficie / 35.0);
        double chance = Math.abs(ruido.ruido(x * 0.041 + y * 0.003, z * 0.037));
        return chance < fatorProf * 0.8;
    }

    public boolean temArco(int x, int y, int z, int alturaSuperficie) {
        if(y < alturaSuperficie - 25 || y > alturaSuperficie + 15) return false;
        if(alturaSuperficie < nivelMar + 20) return false;

        double pilar = celular.ruido(x * 0.015, z * 0.015);
        if(pilar <= 0.35 || pilar >= 0.55) return false; // sai antes

        double vao = Math.abs(ruido3d.ruido(x * 0.03, y * 0.05, z * 0.03));
        if(vao >= 0.2) return false; // sai antes de calcular formato e curvatura

        double formato = ruido.ruido(x * 0.02, z * 0.02);
        if(formato <= 0.4) return false;

        double distTopo = Math.abs(y - (alturaSuperficie - 5));
        double curvatura = 1.0 - (distTopo * distTopo) / 225.0;
        return curvatura > 0.3;
    }

    // verifica se deve haver cascalho nesta posição
    public boolean temCascalho(int x, int y, int z, int alturaSuperficie, TipoBioma bioma) {
        // so avalia cascalho rochoso perto da superficie de montanhas
        if(alturaSuperficie > nivelMar + 30 && y > alturaSuperficie - 8) {
            double rochoso = crista.swiss(x * 0.003, z * 0.003, 2, 2.0, 0.4, 0.5);
            if(rochoso > 0.6) {
                double chance = Math.abs(ruido.ruido(x * 0.017 + y * 0.013, z * 0.019));
                return chance < 0.3;
            }
        }
        return false;
    }

    public enum TipoBioma {
		OCEANO, OCEANO_COSTEIRO, OCEANO_QUENTE,
		OCEANO_ABISSAL, MAR_CONGELADO,
		PLANICIES, PLANICIES_MONTANHOSAS,
		FLORESTA, FLORESTA_COSTEIRA, FLORESTA_MONTANHOSA,
		DESERTO, COLINAS_DESERTO,
		TUNDRA, PICOS_GELADOS
	}
}

