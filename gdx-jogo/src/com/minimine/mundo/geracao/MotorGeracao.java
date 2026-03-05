package com.minimine.mundo.geracao;

import com.minimine.mundo.Chunk;
import com.minimine.mundo.ChunkUtil;
import com.minimine.mundo.Mundo;

/*
 * motor de geração central, substitui Biomas.java
 * toda a logica de geração de coluna por bioma ta aqui
 * a TabelaBiomas tem so dados; o motor decide o que fazer com eles
 */
public final class MotorGeracao {
    public static final int NIVEL_MAR = 62;
    public final ContextoGeracao ctx;
	
	public static final ThreadLocal<int[][]> ALTURAS_CACHE = new ThreadLocal<int[][]>() {
        @Override protected int[][] initialValue() { return new int[16][16]; }
    };
	public static final ThreadLocal<int[][]> BIOMAS_CACHE = new ThreadLocal<int[][]>() {
        @Override protected int[][] initialValue() { return new int[16][16]; }
    };
	public static final ThreadLocal<double[]> GRAD_CACHE = new ThreadLocal<double[]>() {
        @Override protected double[] initialValue() { return new double[2]; }
    };

    public MotorGeracao(long semente) {
        this.ctx = new ContextoGeracao(semente);
    }

    public void gerarChunk(Chunk chunk) {
        final int chunkX = chunk.x << 4;
        final int chunkZ = chunk.z << 4;

        final int[][] alturas  = ALTURAS_CACHE.get();
        final int[][] biomaIds = BIOMAS_CACHE.get();

        for(int x = 0; x < 16; x++) {
            for(int z = 0; z < 16; z++) {
                int mx = chunkX + x, mz = chunkZ + z;
                double elev = ctx.dominio.obterElevacaoContinental(mx, mz);
                double tipo = identificarTipo(elev);
                int alt = calcularAltura(mx, mz, elev, tipo);
                int bioma = determinarBioma(mx, mz, alt, elev, tipo);
                alturas [x][z] = alt;
                biomaIds[x][z] = bioma;
                gerarColuna(chunk, x, z, mx, mz, alt, bioma, tipo);
            }
        }
        addArvores(chunk, chunkX, chunkZ, alturas, biomaIds);
        addVegetacao(chunk, chunkX, chunkZ, alturas, biomaIds);
        chunk.dadosProntos = true;
    }

    // calculo de alturas
    public int calcularAltura(int x, int z, double elev, double tipo) {
        double altura = elev;
        double suavizacao = ctx.ruido.ruido(x * 0.0002, z * 0.0002) * 0.5 + 0.5;

        if(tipo > 0.45) {
            double montanhas = ctx.crista.cristaFractal(x * 0.0008, z * 0.0008, 2, 2.2, 0.5);
            double cordilheiras = ctx.crista.cristaBilateral(x * 0.0004, z * 0.0004, 2, 2.0, 0.5);
            double fator = (tipo - 0.45) / 0.55;
            altura += montanhas    * fator * 0.32;
            altura += cordilheiras * fator * 0.14;
            altura  = altura * (0.75 + suavizacao * 0.25);
            if(fator > 0.6) {
                double rochoso = ctx.crista.swiss(x * 0.002, z * 0.002, 2, 2.0, 0.4, 0.5);
                altura += rochoso * (fator - 0.6) * 0.10;
            }
        } else if(tipo > 0.0) {
            double trans = ctx.ruido.ruidoFractal(x * 0.0008, z * 0.0008, 2, 0.5, 2.0);
            altura += trans * 0.04 * tipo;
        } else {
            double fundo = ctx.ruido.ruidoFractal(x * 0.001, z * 0.001, 2, 0.5, 2.0);
            altura += fundo * 0.02;
        }
        double turbulencia = ctx.crista.jordan(x * 0.001, z * 0.001, 2, 2.1, 1.0, 0.5);
        altura += turbulencia * 0.04;
        altura += ctx.erosao.obterErosaoInterpolada(x, z) * 0.1;
        altura += ctx.ruido.ruidoFractal(x * 0.01,  z * 0.01,  2, 0.5, 2.0) * 0.035;
        altura += ctx.ruido.ruidoFractal(x * 0.03,  z * 0.03,  2, 0.5, 2.0) * 0.018;

        if(tipo > -0.05 && tipo < 0.42) {
            double rio = calcularFatorRio(x, z, tipo);
            if(rio > 0) altura -= rio * 0.12;
        }
        int blocos;
        if(altura < 0) blocos = NIVEL_MAR + (int)(altura * 60.0);
        else blocos = NIVEL_MAR + (int)(altura * 97.0);

        if(blocos > NIVEL_MAR + 40) {
            double v3d = ctx.ruido3d.ruidoFractal(x * 0.04, blocos * 0.08, z * 0.04, 1, 0.5, 2.0);
            if(v3d > 0.45) blocos += (int)((v3d - 0.45) * 10.0);
        }
        return Math.max(1, Math.min(240, blocos));
    }

    // tipo de terreno
    public static double identificarTipo(double base) {
        if(base < -0.15) return -0.5;
        double t = (base + 0.15) / 1.15;
        t = Math.min(1.0, t);
        t = t * t * (3.0 - 2.0 * t);
        return -0.15 + t * 0.65;
    }

    public double calcularFatorRio(int x, int z, double tipo) {
        double desvX = ctx.rioDesvio.ruidoFractal(x * 0.0006,       z * 0.0006,       2, 0.5, 2.0) * 180.0;
        double desvZ = ctx.rioDesvio.ruidoFractal(x * 0.0006 + 700, z * 0.0006 + 700, 2, 0.5, 2.0) * 180.0;
        double rv = ctx.rioRuido.ruidoFractal((x + desvX) * 0.0003, (z + desvZ) * 0.0003, 2, 0.5, 2.0);

        double dist = Math.abs(rv);
        double largura = 0.06 + tipo * 0.04;
        if(dist > largura) return 0.0;

        double perfil = 1.0 - (dist / largura);
        perfil = perfil * perfil;
        return perfil * (0.4 + Math.max(0, -tipo) * 0.6);
    }
	
	// calculo de biomas:
    public int determinarBioma(int x, int z, int altura, double elev, double tipo) {
        double celularVal  = ctx.celular.ruido(x * 0.0008, z * 0.0008);
        double distEquador = Math.abs(z * 0.00015);
        double temp = 1.0 - distEquador * 0.7;
        temp -= Math.max(0, (altura - NIVEL_MAR) * 0.004);
        temp += (celularVal - 0.3) * 0.2;

        double umidade = ctx.ruido.ruidoFractal(x * 0.0005, z * 0.0005, 2, 0.5, 2.0) * 0.5 + 0.5;
        double varClima = ctx.ruido.ruidoFractal(x * 0.0003, z * 0.0003, 2, 0.6, 2.0);
        umidade += varClima * 0.3;
        umidade += (1.0 - celularVal) * 0.15;

        if(altura <= NIVEL_MAR) {
            if(temp < 0.25) return TabelaBiomas.MAR_CONGELADO;
            if(altura < NIVEL_MAR - 35 || elev < -0.55) return TabelaBiomas.OCEANO_ABISSAL;
            if(temp > 0.7) return TabelaBiomas.OCEANO_QUENTE;
            return TabelaBiomas.OCEANO;
        }
        if(temp < 0.3) return altura > NIVEL_MAR + 40 ? TabelaBiomas.PICOS_GELADOS : TabelaBiomas.TUNDRA;

        if(altura < NIVEL_MAR + 7) {
            double[] grad = GRAD_CACHE.get();
            ctx.dominio.obterGradiente(x, z, 8, grad);
            double inclinacao = Math.sqrt(grad[0] * grad[0] + grad[1] * grad[1]);
            if(inclinacao < 0.003) return TabelaBiomas.OCEANO_COSTEIRO;
        }
        if(tipo > -0.05 && tipo < 0.42) {
            if(calcularFatorRio(x, z, tipo) > 0.4 && altura <= NIVEL_MAR + 3)
                return TabelaBiomas.OCEANO_COSTEIRO;
        }
        if(umidade < 0.25 - celularVal * 0.1) return altura > NIVEL_MAR + 15 ? TabelaBiomas.COLINAS_DESERTO : TabelaBiomas.DESERTO;

        if(umidade > 0.55 + celularVal * 0.1) {
            if(altura > NIVEL_MAR + 35) return TabelaBiomas.FLORESTA_MONTANHOSA;
            if(elev < 0.05) return TabelaBiomas.FLORESTA_COSTEIRA;
            return TabelaBiomas.FLORESTA;
        }
        return altura > NIVEL_MAR + 20 ? TabelaBiomas.PLANICIES_MONTANHOSAS : TabelaBiomas.PLANICIES;
    }
	
	// geração por coluna
    public void gerarColuna(Chunk chunk, int x, int z, int mx, int mz, int altura, int biomaId, double tipo) {
        DadosBioma d = TabelaBiomas.TABELA[biomaId];

        ChunkUtil.defBloco(x, 0, z, "pedra", chunk);

        boolean[] vazios = calcularVazios(mx, mz, altura);

        for(int y = 1; y < altura - d.profSubTopo; y++) {
            if(!vazios[y]) ChunkUtil.defBloco(x, y, z, temCascalho(mx, y, mz, altura) ? "cascalho" : d.blocoInterior, chunk);
        }

        switch(biomaId) {
            case TabelaBiomas.OCEANO:
            case TabelaBiomas.OCEANO_QUENTE: {
					String fundo = (62 - altura) > 20 ? "pedra" : "areia";
					for(int y = altura - 3; y < altura; y++)
						if(!vazios[y]) ChunkUtil.defBloco(x, y, z, fundo, chunk);
					for(int y = altura; y <= 62; y++)
						ChunkUtil.defBloco(x, y, z, "agua", chunk);
					break;
				}
            case TabelaBiomas.OCEANO_ABISSAL: {
					for(int y = altura - 4; y < altura; y++)
						if(!vazios[y]) ChunkUtil.defBloco(x, y, z, "pedra", chunk);
					for(int y = altura; y <= 62; y++)
						ChunkUtil.defBloco(x, y, z, "agua", chunk);
					break;
				}
            case TabelaBiomas.OCEANO_COSTEIRO: {
					double fatorRio = calcularFatorRio(mx, mz, tipo);
					String base = (fatorRio > 0.4 && altura <= 62) ? "cascalho" : "areia";
					for(int y = altura - 3; y < altura; y++)
						if(!vazios[y]) ChunkUtil.defBloco(x, y, z, base, chunk);
					for(int y = altura; y <= 62; y++)
						ChunkUtil.defBloco(x, y, z, "agua", chunk);
					break;
				}
            case TabelaBiomas.MAR_CONGELADO: {
					for(int y = altura - 3; y < altura; y++)
						if(!vazios[y]) ChunkUtil.defBloco(x, y, z, "pedra", chunk);
					if(!vazios[altura - 1]) ChunkUtil.defBloco(x, altura - 1, z, "gelo", chunk);
					for(int y = altura; y <= 62; y++)
						ChunkUtil.defBloco(x, y, z, y == 62 ? "gelo" : "agua", chunk);
					double iv = ctx.celular.ruido(mx * 0.009, mz * 0.009);
					if(iv > 0.72) {
						int topo = (int)((iv - 0.72) / 0.28 * 18) + 2;
						for(int y = 63; y < 63 + topo; y++) {
							double dist = Math.abs(ctx.ruido.ruido(mx * 0.04 + y, mz * 0.04));
							ChunkUtil.defBloco(x, y, z, dist < 0.25 ? "pedra" : "gelo", chunk);
						}
					} else if(iv > 0.62) {
						int borda = (int)((iv - 0.62) / 0.10 * 4) + 1;
						for(int y = 63; y < 63 + borda; y++)
							ChunkUtil.defBloco(x, y, z, "gelo", chunk);
					}
					break;
				}
            case TabelaBiomas.DESERTO:
            case TabelaBiomas.COLINAS_DESERTO: {
					for(int y = altura - 5; y < altura; y++)
						if(!vazios[y]) ChunkUtil.defBloco(x, y, z, "areia", chunk);
					double chance = Mundo.s2D.ruido(mx * 0.1, mz * 0.1);
					if(chance > 0.85 && altura > 62) {
						int alt = (int)((Mundo.s2D.ruido(mx * 0.3, mz * 0.3) * 0.5 + 0.5) * 3) + 2;
						for(int cy = 0; cy < alt; cy++)
							ChunkUtil.defBloco(x, altura + cy, z, "cacto", chunk);
					}
					break;
				}
            case TabelaBiomas.PICOS_GELADOS: {
					for(int y = altura - 5; y < altura - 2; y++)
						if(!vazios[y]) ChunkUtil.defBloco(x, y, z, "pedra", chunk);
					if(!vazios[altura - 2]) ChunkUtil.defBloco(x, altura - 2, z, "gelo", chunk);
					if(!vazios[altura - 1]) ChunkUtil.defBloco(x, altura - 1, z, "neve", chunk);
					break;
				}
            default: {
					for(int y = altura - d.profSubTopo; y < altura - 1; y++)
						if(!vazios[y]) ChunkUtil.defBloco(x, y, z, d.blocoSubTopo, chunk);
					if(!vazios[altura - 1]) ChunkUtil.defBloco(x, altura - 1, z, d.blocoTopo, chunk);
					break;
				}
        }
    }

    public boolean[] calcularVazios(int x, int z, int altura) {
        boolean[] vazios = new boolean[altura];

        boolean podeArco   = altura > NIVEL_MAR + 20;
        double  pilarArco  = podeArco ? ctx.celular.ruido(x * 0.015, z * 0.015) : 0;
        boolean arcoViavel = podeArco
            && pilarArco > 0.35 && pilarArco < 0.55
            && ctx.ruido.ruido(x * 0.02, z * 0.02) > 0.4;

        int yMin = Math.max(1, Math.min(altura - 40, 10));

        for(int y = yMin; y < altura; y++) {
            boolean ravina = (y >= altura - 40) && temRavina(x, y, z, altura);
            boolean arco = !ravina && arcoViavel && (y >= altura - 25) && temArco(x, y, z, altura);
            boolean caverna = !ravina && !arco && (y >= 10 && y <= 140) && temCaverna(x, y, z, altura);
            vazios[y] = ravina || arco || caverna;
        }
        return vazios;
    }

    public boolean temCaverna(int x, int y, int z, int sup) {
        if (y < 10 || y > 140 || y > sup - 8) return false;
        if (y <= 40) return ctx.cavernasProfundas.ruidoFractal(x * 0.015, y * 0.025, z * 0.015, 2, 0.5, 2.0) > 0.65;
        if (y <= 90) return ctx.cavernas.ruidoFractal(x * 0.02, y * 0.03, z * 0.02, 3, 0.5, 2.0) > 0.62;
        double limiar = 0.68 + (y - 90) * 0.002;
        return ctx.ruido3d.ruidoFractal(x * 0.025, y * 0.035, z * 0.025, 2, 0.5, 2.0) > limiar;
    }

    public boolean temRavina(int x, int y, int z, int sup) {
        if(y < sup - 40 || y > sup + 5) return false;
        double r1 = Math.abs(ctx.ruido3d.ruido(x * 0.012, y * 0.008, z * 0.012));
        if(r1 >= 0.08) return false;
        double r2 = ctx.ruido3d.ruido(x * 0.008, y * 0.015, z * 0.008);
        if(r2 <= 0.3) return false;
        double dist  = Math.abs(y - sup + 10);
        double fator = Math.max(0, 1.0 - dist / 35.0);
        return Math.abs(ctx.ruido.ruido(x * 0.041 + y * 0.003, z * 0.037)) < fator * 0.8;
    }

    public boolean temArco(int x, int y, int z, int sup) {
        if(y < sup - 25 || y > sup + 15) return false;
        if(Math.abs(ctx.ruido3d.ruido(x * 0.03, y * 0.05, z * 0.03)) >= 0.2) return false;
        double distTopo  = Math.abs(y - (sup - 5));
        return (1.0 - (distTopo * distTopo) / 225.0) > 0.3;
    }

    public boolean temCascalho(int x, int y, int z, int sup) {
        if(sup <= NIVEL_MAR + 30 || y <= sup - 8) return false;
        double rochoso = ctx.crista.swiss(x * 0.003, z * 0.003, 2, 2.0, 0.4, 0.5);
        if(rochoso <= 0.6) return false;
        return Math.abs(ctx.ruido.ruido(x * 0.017 + y * 0.013, z * 0.019)) < 0.3;
    }

    public void addArvores(Chunk chunk, int chunkX, int chunkZ, int[][] alturas, int[][] biomaIds) {
        for(int x = 2; x < 14; x++) {
            for(int z = 2; z < 14; z++) {
                DadosBioma d = TabelaBiomas.TABELA[biomaIds[x][z]];
                if(d.tipoArvore == null) continue;
                if(alturas[x][z] <= NIVEL_MAR) continue;

                int topo = -1;
                for(int y = Mundo.Y_CHUNK - 1; y >= 0; y--) {
                    if(ChunkUtil.obterBloco(x, y, z, chunk) != 0) { topo = y; break; }
                }
                if(topo <= 0) continue;
                if(ChunkUtil.obterBloco(x, topo, z, chunk) != 1) continue;

                int mx = chunkX + x, mz = chunkZ + z;
                if(Mundo.s2D.ruido(mx * 0.1, mz * 0.1) <= d.limiteArvore) continue;

                int alturaTronco = 4 + (int)((Mundo.s2D.ruido(mx * 0.25, mz * 0.25) * 0.5 + 0.5) * 3);

                if("conica".equals(d.tipoArvore)) {
                    Arvores.gerarArvoreConica(chunk, x, topo + 1, z, alturaTronco);
                } else if(Mundo.s2D.ruido(mx * 0.2, mz * 0.2) > 0.5) {
                    Arvores.gerarArvoreNormal(chunk, x, topo + 1, z, alturaTronco);
                } else {
                    Arvores.gerarArvoreLarga(chunk, x, topo + 1, z, alturaTronco);
                }
            }
        }
    }

    public void addVegetacao(Chunk chunk, int chunkX, int chunkZ, int[][] alturas, int[][] biomaIds) {
        for(int x = 0; x < 16; x++) {
            for(int z = 0; z < 16; z++) {
                DadosBioma d = TabelaBiomas.TABELA[biomaIds[x][z]];
                if(d.vegetacao == null) continue;

                int altura = alturas[x][z];
                if(altura <= NIVEL_MAR) continue;
                if(ChunkUtil.obterBloco(x, altura - 1, z, chunk) != 1) continue;

                int mx = chunkX + x, mz = chunkZ + z;
                double densRuido = Mundo.s2D.ruido(mx * 0.15, mz * 0.15);
                double tipoRuido = Mundo.s2D.ruido(mx * 0.35, mz * 0.35);
                double florRuido = Mundo.s2D.ruido(mx * 0.55, mz * 0.55);

                if(densRuido > d.limiteVegetacao)
                    ChunkUtil.defBloco(x, altura, z, d.vegetacao[0], chunk);

                if(d.vegetacao.length > 1 && florRuido > d.limiteFlor) {
                    int idx = 1 + (int)(tipoRuido * (d.vegetacao.length - 1));
                    ChunkUtil.defBloco(x, altura, z, d.vegetacao[Math.min(idx, d.vegetacao.length - 1)], chunk);
                }
            }
        }
    }
	
	// utilitarios
    public String obterBioma(int x, int z) {
        double elev = ctx.dominio.obterElevacaoContinental(x, z);
        double tipo = identificarTipo(elev);
        int alt  = calcularAltura(x, z, elev, tipo);
        return TabelaBiomas.nome(determinarBioma(x, z, alt, elev, tipo));
    }

    public int[] localizarBioma(String nomeBioma, int origemX, int origemZ) {
        int alvo = -1;
        for(int i = 0; i < TabelaBiomas.NOMES.length; i++) {
            if(TabelaBiomas.NOMES[i].equalsIgnoreCase(nomeBioma)) { alvo = i; break; }
        }
        if(alvo < 0) return null;

        int passo = 64, raioMax = 100_000;
        for(int raio = passo; raio <= raioMax; raio += passo) {
            for(int dx = -raio; dx <= raio; dx += passo) {
                for(int dz = -raio; dz <= raio; dz += passo) {
                    if(Math.abs(dx) != raio && Math.abs(dz) != raio) continue;
                    int x = origemX + dx, z = origemZ + dz;
                    double elev = ctx.dominio.obterElevacaoContinental(x, z);
                    double tipo = identificarTipo(elev);
                    int alt = calcularAltura(x, z, elev, tipo);
                    if(determinarBioma(x, z, alt, elev, tipo) == alvo) return new int[]{ x, z };
                }
            }
        }
        return null;
    }
}

