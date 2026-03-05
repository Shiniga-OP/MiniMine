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
    public final Simplex2D rioRuido; // para rede de rios
    public final Simplex2D rioDesvio; // desvio organico dos leitos
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
        this.rioRuido  = new Simplex2D(semente ^ 0xDEADBEEF12345678L);
        this.rioDesvio = new Simplex2D(semente ^ 0xCAFEBABE87654321L);

        this.erosao = new ErosaoHidraulica(semente, 512, 8.0);
        this.erosao.simularGotas(500, dominio);
    }

    public void calcularDadosColuna(int x, int z, int[] buffer) {
        double base = dominio.obterElevacaoContinental(x, z);
        double tipoTerreno = identificarTipo(base);
        double altura = base;

        double suavizacao = ruido.ruido(x * 0.0002, z * 0.0002) * 0.5 + 0.5;

        if(tipoTerreno > 0.45) {
            // zona de montanha, so começa a amplificar acima de 0.45(antes era 0.3)
            double montanhas   = crista.cristaFractal(x * 0.0008, z * 0.0008, 2, 2.2, 0.5);
            double cordilheiras = crista.cristaBilateral(x * 0.0004, z * 0.0004, 2, 2.0, 0.5);
            double fatorMontanha = (tipoTerreno - 0.45) / 0.55; // normaliza [0,1]

            // amplitudes reduzidas: 0.48->0.32, 0.24->0.14, menos "sobe e desce"
            altura += montanhas    * fatorMontanha * 0.32;
            altura += cordilheiras * fatorMontanha * 0.14;
            altura  = altura * (0.75 + suavizacao * 0.25);

            if(fatorMontanha > 0.6) {
                double rochoso = crista.swiss(x * 0.002, z * 0.002, 2, 2.0, 0.4, 0.5);
                altura += rochoso * (fatorMontanha - 0.6) * 0.10;
            }
        } else if(tipoTerreno > 0.0) {
            // zona de colinas suaves/transição, micro-ondulação apenas
            double transicao = ruido.ruidoFractal(x * 0.0008, z * 0.0008, 2, 0.5, 2.0);
            altura += transicao * 0.04 * tipoTerreno; // bem fraco -> planicies quase planas
        } else {
            // zona abaixo de 0 -> oceano/vale; quase nenhuma variação extra
            double fundoVar = ruido.ruidoFractal(x * 0.001, z * 0.001, 2, 0.5, 2.0);
            altura += fundoVar * 0.02;
        }
        // turbulência de detalhe, reduzida pra não criar paredões aleatorios
        double turbulencia = crista.jordan(x * 0.001, z * 0.001, 2, 2.1, 1.0, 0.5);
        altura += turbulencia * 0.04;

        double valorErosao = erosao.obterErosaoInterpolada(x, z);
        altura += valorErosao * 0.1;

        // micro-detalhe de superficie
        double detalhe1 = ruido.ruidoFractal(x * 0.01, z * 0.01, 2, 0.5, 2.0) * 0.035;
        double detalhe2 = ruido.ruidoFractal(x * 0.03, z * 0.03, 2, 0.5, 2.0) * 0.018;
        altura += detalhe1 + detalhe2;

        // === sistema de rios ===
        // rios nascem em zonas de colina/planicie(tipoTerreno 0..0.45) e nunca no oceano
        if(tipoTerreno > -0.05 && tipoTerreno < 0.42) {
            double rioFator = calcularFatorRio(x, z, tipoTerreno);
            if(rioFator > 0) {
                // escava um leito proporcional à largura do rio
                altura -= rioFator * 0.12;
            }
        }
        int blocos;
        if(altura < 0) {
            blocos = nivelMar + (int)(altura * 60.0);
        } else {
            blocos = nivelMar + (int)(altura * 97.0);
        }
        // variação 3D so em alturas realmente elevadas(antes era +25, agora +40)
        if(blocos > nivelMar + 40) {
            double var3d = ruido3d.ruidoFractal(x * 0.04, blocos * 0.08, z * 0.04, 1, 0.5, 2.0);
            if(var3d > 0.45) {
                blocos += (int)((var3d - 0.45) * 10.0);
            }
        }
        int alturaFinal = Math.max(1, Math.min(240, blocos));
        TipoBioma bioma = determinarBioma(x, z, alturaFinal, base, tipoTerreno);

        buffer[0] = alturaFinal;
        buffer[1] = bioma.ordinal();
    }
    /*
     * retorna um valor [0,1] indicando o quanto esta coluna ta dentro de um leito de rio
     * 0 = sem rio; >0 = dentro do leito, proporcional à profundidade do corte
     * usa ruido celular para criar redes ramificadas, não retas
     */
    public double calcularFatorRio(int x, int z, double tipoTerreno) {
        // desvio orgânico do leito, evita rios retos
        double desvX = rioDesvio.ruidoFractal(x * 0.0006, z * 0.0006, 2, 0.5, 2.0) * 180.0;
        double desvZ = rioDesvio.ruidoFractal(x * 0.0006 + 700, z * 0.0006 + 700, 2, 0.5, 2.0) * 180.0;

        // valor do ruido de rios no ponto desviado, frequencia baixa -> rios longos
        double rv = rioRuido.ruidoFractal((x + desvX) * 0.0003, (z + desvZ) * 0.0003, 2, 0.5, 2.0);

        // leito = região onde rv ≈ 0; largura proporcional ao quanto estamos proximos de 0
        double distLeito = Math.abs(rv);
        double largura = 0.06 + tipoTerreno * 0.04; // rios mais largos em vales planos
        if(distLeito > largura) return 0.0;

        // perfil suave de calha: maximo no centro, zero nas bordas
        double perfil = 1.0 - (distLeito / largura);
        perfil = perfil * perfil; // quadrático → bordas suaves

        // rios mais fundos onde o terreno ja desce naturalmente(vales)
        double profBase = 0.4 + Math.max(0, -tipoTerreno) * 0.6;
        return perfil * profBase;
    }

    public double identificarTipo(double base) {
        // distribui as zonas de forma mais natural:
        // base < -0.15 -> oceano/abismo(fixado em -0.5)
        // -0.15..0.20 -> planicie/vale(zona mais larga)
        // 0.20..0.55 -> colinas de transição
        // > 0.55 -> montanha
        // agora a faixa plana ocupa ~35% da distribuição de ruido
        if(base < -0.15) return -0.5;
        double t = (base + 0.15) / 1.15; // mapeia [-0.15,1] -> [0,1]
        t = Math.min(1.0, t);
        // passo borrado suave, mas com a faixa de entrada muito mais ampla pra vale/planicie
        t = t * t * (3.0 - 2.0 * t);
        return -0.15 + t * 0.65; // mapeia [0,1] -> [-0.15, 0.50]
    }

    public TipoBioma determinarBioma(int x, int z, int altura, double base, double tipoTerreno) {
        double celularVal = celular.ruido(x * 0.0008, z * 0.0008);

        double distEquador = Math.abs(z * 0.00015);
        double temp = 1.0 - distEquador * 0.7;
        temp -= Math.max(0, (altura - nivelMar) * 0.004);
        temp += (celularVal - 0.3) * 0.2;

        double umidade = ruido.ruidoFractal(x * 0.0005, z * 0.0005, 2, 0.5, 2.0) * 0.5 + 0.5;
        double varClima = ruido.ruidoFractal(x * 0.0003, z * 0.0003, 2, 0.6, 2.0);
        umidade += varClima * 0.3;
        umidade += (1.0 - celularVal) * 0.15;

        // === oceano/mar ===
        if(altura <= nivelMar) {
            if(temp < 0.25) {
                // mar congelado com icebergs ocasionais(tratado em gerarColuna)
                return TipoBioma.MAR_CONGELADO;
            }
            if(altura < nivelMar - 35 || base < -0.55) return TipoBioma.OCEANO_ABISSAL;
            if(temp > 0.7) return TipoBioma.OCEANO_QUENTE;
            return TipoBioma.OCEANO;
        }

        // === frio acima do mar ===
        if(temp < 0.3) {
            return altura > nivelMar + 40 ? TipoBioma.PICOS_GELADOS : TipoBioma.TUNDRA;
        }
        // === costa: determinada por inclinação, não so por altura ===
        // gradiente alto = penhasco/cliff -> não é praia; gradiente baixo = praia plana
        if(altura < nivelMar + 7) {
            double gradX = dominio.obterElevacaoContinental(x + 8, z) - dominio.obterElevacaoContinental(x - 8, z);
            double gradZ = dominio.obterElevacaoContinental(x, z + 8) - dominio.obterElevacaoContinental(x, z - 8);
            double inclinacao = Math.sqrt(gradX * gradX + gradZ * gradZ);
            // inclinação baixa(<0.003) = praia de areia; alta = rocha costeira(planicie/floresta)
            if(inclinacao < 0.003) return TipoBioma.OCEANO_COSTEIRO; // areia
            // costão rochoso -> cai no critério normal de bioma abaixo
        }
        // === leito de rio: coluna escavada no meio da planicie ===
        if(tipoTerreno > -0.05 && tipoTerreno < 0.42) {
            double rioFator = calcularFatorRio(x, z, tipoTerreno);
            if(rioFator > 0.4 && altura <= nivelMar + 3) {
                return TipoBioma.OCEANO_COSTEIRO; // margem de rio(areia/cascalho)
            }
        }
        // === biomas terrestres ===
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

        int yMin = Math.max(1, Math.min(altura - 40, 10));

        for(int y = yMin; y < altura; y++) {
            boolean ravina = (y >= altura - 40) && temRavina(x, y, z, altura);
            boolean arco   = !ravina && arcoViavel && (y >= altura - 25) && temArcoY(x, y, z, altura, true);
            // passa alturaSuperficie para que temCaverna possa aplicar margem mínima
            boolean caverna = !ravina && !arco && (y >= 10 && y <= 140) && temCaverna(x, y, z, altura);
            buffer[y] = ravina || arco || caverna;
        }
    }

    // versão interna, recebe arcoViavel ja calculado fora do loop
    public boolean temArcoY(int x, int y, int z, int alturaSuperficie, boolean arcoViavel) {
        if(!arcoViavel) return false;
        if(y < alturaSuperficie - 25 || y > alturaSuperficie + 15) return false;

        double vao = Math.abs(ruido3d.ruido(x * 0.03, y * 0.05, z * 0.03));
        if(vao >= 0.2) return false;

        double distTopo = Math.abs(y - (alturaSuperficie - 5));
        double curvatura = 1.0 - (distTopo * distTopo) / 225.0;
        return curvatura > 0.3;
    }

    public boolean temCaverna(int x, int y, int z, int alturaSuperficie) {
        // nunca gera caverna a menos de 8 blocos da superficie, elimina cavernas de 1 bloco
        if(y < 10 || y > 140 || y > alturaSuperficie - 8) return false;
        // 3 zonas sem sobreposição, cada Y avalia exatamente 1 sistema
        if(y <= 40) {
            // profundas: raras e grandes
            return cavernasProfundas.ruidoFractal(x * 0.015, y * 0.025, z * 0.015, 2, 0.5, 2.0) > 0.65;
        }
        if(y <= 90) {
            // principais: camada do meio
            return cavernas.ruidoFractal(x * 0.02, y * 0.03, z * 0.02, 3, 0.5, 2.0) > 0.62;
        }
        // superficiais: ficam mais raras conforme sobem, e so abaixo de alturaSuperficie-8
        double limiar = 0.68 + (y - 90) * 0.002; // 0.68 em y=90 -> 0.78 em y=140
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
		TUNDRA, PICOS_GELADOS,
		// biomas de detalhamento:
		RIO, LAGO,
		COLINAS_FLORESTAIS, FLORESTA_LEITO,
	}
}


