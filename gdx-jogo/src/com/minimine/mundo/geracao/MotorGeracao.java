package com.minimine.mundo.geracao;

import com.minimine.mundo.Chunk;
import com.minimine.mundo.ChunkUtil;
import com.minimine.mundo.Mundo;
import com.minimine.utils.ruidos.OpenSimplex2;

public final class MotorGeracao {
    public static final int NIVEL_MAR = 62;

    public final ContextoGeracao ctx;
    public final RegistroBiomas  registro;

    public static final ThreadLocal<int[][]> ALTURAS_CACHE = new ThreadLocal<int[][]>() {
        @Override protected int[][] initialValue() { return new int[16][16]; }
    };
    public static final ThreadLocal<DadosBioma[][]> BIOMAS_CACHE = new ThreadLocal<DadosBioma[][]>() {
        @Override protected DadosBioma[][] initialValue() { return new DadosBioma[16][16]; }
    };
    public static final ThreadLocal<double[]> GRAD_CACHE = new ThreadLocal<double[]>() {
        @Override protected double[] initialValue() { return new double[2]; }
    };

    public MotorGeracao(long semente, RegistroBiomas registro) {
        this.ctx = new ContextoGeracao(semente);
        this.registro = registro;
    }

    public void gerarChunk(Chunk chunk) {
        final int chunkX = chunk.x << 4;
        final int chunkZ = chunk.z << 4;

        final int[][] alturas = ALTURAS_CACHE.get();
        final DadosBioma[][] biomas  = BIOMAS_CACHE.get();

        // fase 1 + 2: preenche coluna e pinta superficie
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int mx = chunkX + x, mz = chunkZ + z;
                double elev = ctx.dominio.obterElevacaoContinental(mx, mz);
                double tipo = identificarTipo(elev);
                int alt = calcularAltura(mx, mz, elev, tipo);
                DadosBioma bioma = determinarBioma(mx, mz, alt, elev, tipo);

                alturas[x][z] = alt;
                biomas[x][z] = bioma;

                boolean[] vazios = calcularVazios(mx, mz, alt);
                preencherColuna(chunk, x, z, alt, vazios);
                aplicarSuperficie(chunk, x, z, mx, mz, alt, bioma, tipo, vazios);
            }
        }
        // fase 3: decoracoes, arvores, vegetacao
        addDecoracoes(chunk, chunkX, chunkZ, alturas, biomas);
        addArvores   (chunk, chunkX, chunkZ, alturas, biomas);
        addVegetacao (chunk, chunkX, chunkZ, alturas, biomas);
        chunk.dadosProntos = true;
    }
	
    // === ALTURA ===
    public int calcularAltura(int x, int z, double elev, double tipo) {
        double altura = elev;
        double suavizacao = OpenSimplex2.ruido2(ctx.sementeRuido, x * 0.0002, z * 0.0002) * 0.5 + 0.5;

        if(tipo > 0.45) {
            double montanhas = ctx.crista.cristaFractal(x * 0.0008, z * 0.0008, 2, 2.2, 0.5);
            double cordilheiras = ctx.crista.cristaBilateral(x * 0.0004, z * 0.0004, 2, 2.0, 0.5);
            double fator = (tipo - 0.45) / 0.55;
            altura += montanhas * fator * 0.32;
            altura += cordilheiras * fator * 0.14;
            altura = altura * (0.75 + suavizacao * 0.25);
            if(fator > 0.6) {
                double rochoso = ctx.crista.swiss(x * 0.002, z * 0.002, 2, 2.0, 0.4, 0.5);
                altura += rochoso * (fator - 0.6) * 0.10;
            }
        } else if(tipo > 0.0) {
            double trans = OpenSimplex2.ruido2Fractal(ctx.sementeRuido, x * 0.0008, z * 0.0008, 2, 0.5, 2.0);
            altura += trans * 0.04 * tipo;
        } else {
            double fundo = OpenSimplex2.ruido2Fractal(ctx.sementeRuido, x * 0.001, z * 0.001, 2, 0.5, 2.0);
            altura += fundo * 0.02;
        }
        double turbulencia = ctx.crista.jordan(x * 0.001, z * 0.001, 2, 2.1, 1.0, 0.5);
        altura += turbulencia * 0.04;
        altura += ctx.erosao.obterErosaoInterpolada(x, z) * 0.1;
        altura += OpenSimplex2.ruido2Fractal(ctx.sementeRuido, x * 0.01, z * 0.01, 2, 0.5, 2.0) * 0.035;
        altura += OpenSimplex2.ruido2Fractal(ctx.sementeRuido, x * 0.03, z * 0.03, 2, 0.5, 2.0) * 0.018;

        if(tipo > -0.05 && tipo < 0.42) {
            double rio = calcularFatorRio(x, z, tipo);
            if(rio > 0) altura -= rio * 0.12;
        }
        int blocos;
        if(altura < 0) blocos = NIVEL_MAR + (int)(altura * 60.0);
        else blocos = NIVEL_MAR + (int)(altura * 97.0);

        if(blocos > NIVEL_MAR + 40) {
            double v3d = OpenSimplex2.ruido3XZ(ctx.sementeRuido3d, x * 0.04, blocos * 0.08, z * 0.04);
            if(v3d > 0.45) blocos += (int)((v3d - 0.45) * 10.0);
        }
        return Math.max(1, Math.min(240, blocos));
    }

    public static double identificarTipo(double base) {
        if(base < -0.15) return -0.5;
        double t = (base + 0.15) / 1.15;
        t = Math.min(1.0, t);
        t = t * t * (3.0 - 2.0 * t);
        return -0.15 + t * 0.65;
    }

    public double calcularFatorRio(int x, int z, double tipo) {
        double desvX = OpenSimplex2.ruido2Fractal(ctx.sementeRioDesvio, x * 0.0006,       z * 0.0006,       2, 0.5, 2.0) * 180.0;
        double desvZ = OpenSimplex2.ruido2Fractal(ctx.sementeRioDesvio, x * 0.0006 + 700, z * 0.0006 + 700, 2, 0.5, 2.0) * 180.0;
        double rv = OpenSimplex2.ruido2Fractal(ctx.sementeRioRuido, (x + desvX) * 0.0003, (z + desvZ) * 0.0003, 2, 0.5, 2.0);

        double dist = Math.abs(rv);
        double largura = 0.06 + tipo * 0.04;
        if(dist > largura) return 0.0;

        double perfil = 1.0 - (dist / largura);
        perfil = perfil * perfil;
        return perfil * (0.4 + Math.max(0, -tipo) * 0.6);
    }

    // === BIOMA ===
    public DadosBioma determinarBioma(int x, int z, int altura, double elev, double tipo) {
        double celularVal  = ctx.celular.ruido(x * 0.0008, z * 0.0008);
        double distEquador = Math.abs(z * 0.00015);

        double temp = 1.0 - distEquador * 0.7;
        temp -= Math.max(0, (altura - NIVEL_MAR) * 0.004);
        temp += (celularVal - 0.3) * 0.2;

        double umidade = OpenSimplex2.ruido2Fractal(ctx.sementeRuido, x * 0.0005, z * 0.0005, 2, 0.5, 2.0) * 0.5 + 0.5;
        double varClima = OpenSimplex2.ruido2Fractal(ctx.sementeRuido, x * 0.0003, z * 0.0003, 2, 0.6, 2.0);
        umidade += varClima * 0.3;
        umidade += (1.0 - celularVal) * 0.15;

        float t = (float) Math.max(0, Math.min(1, temp));
        float u = (float) Math.max(0, Math.min(1, umidade));

        boolean aquatico = altura <= NIVEL_MAR;
        if(aquatico) u = 1.0f;

        if(!aquatico && altura < NIVEL_MAR + 7) {
            final double[] grad = GRAD_CACHE.get();
            ctx.dominio.obterGradiente(x, z, 8, grad);
            double inclinacao = Math.sqrt(grad[0] * grad[0] + grad[1] * grad[1]);
            if(inclinacao < 0.003) aquatico = true;
        }
        if(!aquatico && tipo > -0.05 && tipo < 0.42) {
            if(calcularFatorRio(x, z, tipo) > 0.4 && altura <= NIVEL_MAR + 3) aquatico = true;
        }
        return registro.selecionar(t, u, altura, NIVEL_MAR, aquatico);
    }
	
    // FASE 1: PREENCHIMENTO UNIVERSAL
    public void preencherColuna(Chunk chunk, int x, int z, int altura, boolean[] vazios) {
        ChunkUtil.defBloco(x, 0, z, "pedra", chunk);

        // pedra ate a superficie(com cavernas e cascalho)
        for(int y = 1; y < altura; y++) {
            if(!vazios[y])
                ChunkUtil.defBloco(x, y, z, temCascalho(x, y, z, altura) ? "cascalho" : "pedra", chunk);
        }
        // agua acima da superficie ate o nivel do mar
        for(int y = altura; y <= NIVEL_MAR; y++)
            ChunkUtil.defBloco(x, y, z, "agua", chunk);
    }
    // FASE 2: PASSAGEM DA SUPERFICIE
    public void aplicarSuperficie(Chunk chunk, int x, int z, int mx, int mz,
	int altura, DadosBioma d, double tipo, boolean[] vazios) {
        final DadosBioma.Superficie s = d.superficie;
        int profAtual = 0;
        int limSup = Math.min(altura + 1, Mundo.Y_CHUNK - 1) - 2;

        for(int y = limSup; y >= 1; y--) {
            // bloco atual: se e vazio(caverna/ar) ou agua, reinicia o contador
            if(vazios[y] || ChunkUtil.obterBloco(x, y, z, chunk) == 0) {
                profAtual = 0;
                continue;
            }
            if(profAtual == 0) {
                // topo da superficie
                ChunkUtil.defBloco(x, y, z, s.topo, chunk);
            } else if(profAtual < s.prof) {
                // camadas de subtopo, respeita profundidade oceanica e rio
                ChunkUtil.defBloco(x, y, z, resolverSubtopo(s, mx, mz, altura, tipo), chunk);
            } else {
                // interior: bloco padrao abaixo da superficie
                ChunkUtil.defBloco(x, y, z, s.interior, chunk);
                break;
            }
            profAtual++;
        }
    }
    
    public String resolverSubtopo(DadosBioma.Superficie s, int mx, int mz, int altura, double tipo) {
        if(s.profFundoPedra > 0 && (NIVEL_MAR - altura) > s.profFundoPedra)
            return "pedra";
        if(s.fundoRioBloco != null) {
            double fatorRio = calcularFatorRio(mx, mz, tipo);
            if(fatorRio > 0.4 && altura <= NIVEL_MAR) return s.fundoRioBloco;
        }
        return s.subtopo;
    }

    // === VAZIOS(cavernas, ravinas, arcos) ===
    public boolean[] calcularVazios(int x, int z, int altura) {
        boolean[] vazios = new boolean[Math.max(altura, 1)];

        boolean podeArco = altura > NIVEL_MAR + 20;
        double  pilarArco = podeArco ? ctx.celular.ruido(x * 0.015, z * 0.015) : 0;
        boolean arcoViavel = podeArco
            && pilarArco > 0.35 && pilarArco < 0.55
            && OpenSimplex2.ruido2(ctx.sementeRuido, x * 0.02, z * 0.02) > 0.4;

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
        if(y < 10 || y > 140 || y > sup - 8) return false;
        if(y <= 40) return OpenSimplex2.ruido3XYFractal(ctx.sementeCavernasProfundas, x * 0.015, y * 0.025, z * 0.015, 2, 0.5, 2.0) > 0.65;
        if(y <= 90) return OpenSimplex2.ruido3XYFractal(ctx.sementeCavernas,          x * 0.02,  y * 0.03,  z * 0.02,  3, 0.5, 2.0) > 0.62;
        double limiar = 0.68 + (y - 90) * 0.002;
        return OpenSimplex2.ruido3XZFractal(ctx.sementeRuido3d, x * 0.025, y * 0.035, z * 0.025, 2, 0.5, 2.0) > limiar;
    }

    public boolean temRavina(int x, int y, int z, int sup) {
        if(y < sup - 40 || y > sup + 5) return false;
        double r1 = Math.abs(OpenSimplex2.ruido3XZ(ctx.sementeRuido3d, x * 0.012, y * 0.008, z * 0.012));
        if(r1 >= 0.08) return false;
        double r2 = OpenSimplex2.ruido3XZ(ctx.sementeRuido3d, x * 0.008, y * 0.015, z * 0.008);
        if(r2 <= 0.3) return false;
        double dist  = Math.abs(y - sup + 10);
        double fator = Math.max(0, 1.0 - dist / 35.0);
        return Math.abs(OpenSimplex2.ruido2(ctx.sementeRuido, x * 0.041 + y * 0.003, z * 0.037)) < fator * 0.8;
    }

    public boolean temArco(int x, int y, int z, int sup) {
        if(y < sup - 25 || y > sup + 15) return false;
        if(Math.abs(OpenSimplex2.ruido3XZ(ctx.sementeRuido3d, x * 0.03, y * 0.05, z * 0.03)) >= 0.2) return false;
        double distTopo = Math.abs(y - (sup - 5));
        return (1.0 - (distTopo * distTopo) / 225.0) > 0.3;
    }

    public boolean temCascalho(int x, int y, int z, int sup) {
        if(sup <= NIVEL_MAR + 30 || y <= sup - 8) return false;
        double rochoso = ctx.crista.swiss(x * 0.003, z * 0.003, 2, 2.0, 0.4, 0.5);
        if(rochoso <= 0.6) return false;
        return Math.abs(OpenSimplex2.ruido2(ctx.sementeRuido, x * 0.017 + y * 0.013, z * 0.019)) < 0.3;
    }

    // === FASE 3: DECORAÇÕES ===
    public void addDecoracoes(Chunk chunk, int chunkX, int chunkZ, int[][] alturas, DadosBioma[][] biomas) {
        for(int x = 0; x < 16; x++) {
            for(int z = 0; z < 16; z++) {
                DadosBioma d = biomas[x][z];
                if(d.decoracoes == null) continue;

                int altura = alturas[x][z];
                int mx = chunkX + x, mz = chunkZ + z;
                DadosBioma.Decoracoes dec = d.decoracoes;

                // cacto
                if(dec.temCacto() && altura > NIVEL_MAR) {
                    double chance = OpenSimplex2.ruido2(ctx.semente, mx * 0.1, mz * 0.1);
                    if(chance > dec.cactoChance) {
                        int alt = (int)((OpenSimplex2.ruido2(ctx.semente, mx * 0.3, mz * 0.3) * 0.5 + 0.5)
                                * (dec.cactoAltMax - 2)) + 2;
                        for(int cy = 0; cy < alt; cy++)
                            ChunkUtil.defBloco(x, altura + cy, z, "cacto", chunk);
                    }
                }
                // gelo na superficie da agua(biomas gelados aquaticos)
                if(dec.geloSuperficie && altura <= NIVEL_MAR) {
                    ChunkUtil.defBloco(x, NIVEL_MAR, z, "gelo", chunk);
                }
                // iceberg
                if(dec.temIceberg() && altura <= NIVEL_MAR) {
                    double iv = ctx.celular.ruido(mx * 0.009, mz * 0.009);
                    if(iv > dec.icebergLimiar) {
                        int topo = (int)((iv - dec.icebergLimiar) / (1.0 - dec.icebergLimiar) * dec.icebergAltMax) + 2;
                        for(int y = NIVEL_MAR + 1; y < NIVEL_MAR + 1 + topo; y++) {
                            double dist = Math.abs(OpenSimplex2.ruido2(ctx.sementeRuido, mx * 0.04 + y, mz * 0.04));
                            ChunkUtil.defBloco(x, y, z, dist < 0.25 ? dec.icebergBlocoNucleo : dec.icebergBlocoTopo, chunk);
                        }
                    } else if(iv > dec.icebergLimiar - 0.10f) {
                        int borda = (int)((iv - (dec.icebergLimiar - 0.10f)) / 0.10f * 4) + 1;
                        for(int y = NIVEL_MAR + 1; y < NIVEL_MAR + 1 + borda; y++)
                            ChunkUtil.defBloco(x, y, z, dec.icebergBlocoTopo, chunk);
                    }
                }
            }
        }
    }

    public void addArvores(Chunk chunk, int chunkX, int chunkZ, int[][] alturas, DadosBioma[][] biomas) {
        for(int x = 2; x < 14; x++) {
            for(int z = 2; z < 14; z++) {
                DadosBioma d = biomas[x][z];
                if(d.arvores == null) continue;
                if(alturas[x][z] <= NIVEL_MAR) continue;

                int topo = -1;
                for(int y = Mundo.Y_CHUNK - 1; y >= 0; y--) {
                    if(ChunkUtil.obterBloco(x, y, z, chunk) != 0) { topo = y; break; }
                }
                if(topo <= 0) continue;
                if(ChunkUtil.obterBloco(x, topo, z, chunk) != 1) continue;

                int mx = chunkX + x, mz = chunkZ + z;
                if(OpenSimplex2.ruido2(ctx.semente, mx * 0.1, mz * 0.1) <= d.arvores.limite) continue;

                int alturaTronco = 4 + (int)((OpenSimplex2.ruido2(ctx.semente, mx * 0.25, mz * 0.25) * 0.5 + 0.5) * 3);

                if("conica".equals(d.arvores.tipo)) {
                    Arvores.gerarArvoreConica(chunk, x, topo + 1, z, alturaTronco);
                } else if(OpenSimplex2.ruido2(ctx.semente, mx * 0.2, mz * 0.2) > 0.5) {
                    Arvores.gerarArvoreNormal(chunk, x, topo + 1, z, alturaTronco);
                } else {
                    Arvores.gerarArvoreLarga(chunk, x, topo + 1, z, alturaTronco);
                }
            }
        }
    }

    public void addVegetacao(Chunk chunk, int chunkX, int chunkZ, int[][] alturas, DadosBioma[][] biomas) {
        for(int x = 0; x < 16; x++) {
            for(int z = 0; z < 16; z++) {
                DadosBioma d = biomas[x][z];
                if(d.plantas == null) continue;

                int altura = alturas[x][z];
                if(altura <= NIVEL_MAR) continue;
                if(ChunkUtil.obterBloco(x, altura - 1, z, chunk) != 1) continue;

                int mx = chunkX + x, mz = chunkZ + z;
                double densRuido = OpenSimplex2.ruido2(ctx.semente, mx * 0.15, mz * 0.15);
                double tipoRuido = OpenSimplex2.ruido2(ctx.semente, mx * 0.35, mz * 0.35);
                double florRuido = OpenSimplex2.ruido2(ctx.semente, mx * 0.55, mz * 0.55);

                if(densRuido > d.plantas.limite)
                    ChunkUtil.defBloco(x, altura, z, d.plantas.lista[0], chunk);

                if(d.plantas.lista.length > 1 && florRuido > d.plantas.limiteFlor) {
                    int idc = 1 + (int)(tipoRuido * (d.plantas.lista.length - 1));
                    ChunkUtil.defBloco(x, altura, z, d.plantas.lista[Math.min(idc, d.plantas.lista.length - 1)], chunk);
                }
            }
        }
    }

    // === UTILS ===
    public String obterBioma(int x, int z) {
        double elev = ctx.dominio.obterElevacaoContinental(x, z);
        double tipo = identificarTipo(elev);
        int alt  = calcularAltura(x, z, elev, tipo);
        return determinarBioma(x, z, alt, elev, tipo).nome;
    }

    public int[] localizarBioma(String chave, int origemX, int origemZ) {
        if(!registro.existe(chave)) return null;

        int passo = 64, raioMax = 100_000;
        for(int raio = passo; raio <= raioMax; raio += passo) {
            for(int dx = -raio; dx <= raio; dx += passo) {
                for(int dz = -raio; dz <= raio; dz += passo) {
                    if(Math.abs(dx) != raio && Math.abs(dz) != raio) continue;
                    int x = origemX + dx, z = origemZ + dz;
                    double elev = ctx.dominio.obterElevacaoContinental(x, z);
                    double tipo = identificarTipo(elev);
                    int alt  = calcularAltura(x, z, elev, tipo);
                    if(determinarBioma(x, z, alt, elev, tipo).chave.equals(chave))
                        return new int[]{ x, z };
                }
            }
        }
        return null;
    }
}

