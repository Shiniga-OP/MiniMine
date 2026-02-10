package com.minimine.utils.ruidos;

public class RuidoRios {
    private final Simplex2D ruido;
    private final Simplex2D ruidoLagos;
    private final long semente;

    public RuidoRios(long semente) {
        this.semente = semente;
        this.ruido = new Simplex2D(semente ^ 0xCAFEBABEL);
        this.ruidoLagos = new Simplex2D(semente ^ 0xDEADBEEFL);
    }

    /**
     * Calcula informações sobre rios usando ruído de linhas
     */
    public InfoRio calcularRio(int x, int z) {
        InfoRio info = new InfoRio();

        // Múltiplas redes de rios em escalas diferentes
        double rio1 = calcularRedeRio(x, z, 0.0004, 2.5);  // Rios principais grandes
        double rio2 = calcularRedeRio(x, z, 0.0008, 1.8);  // Rios médios
        double rio3 = calcularRedeRio(x, z, 0.0016, 1.2);  // Afluentes pequenos

        // Combina as redes
        double intensidadeRio = Math.max(Math.max(rio1, rio2), rio3);

        if(intensidadeRio > 0.0) {
            info.ehRio = true;
            info.intensidade = intensidadeRio;

            // Profundidade baseada na intensidade
            // Rios principais: 8-15 blocos
            // Rios médios: 5-10 blocos  
            // Afluentes: 3-6 blocos
            if(rio1 > 0.0) {
                info.profundidade = 8 + (int)(intensidadeRio * 7.0);
                info.largura = 6 + (int)(intensidadeRio * 8.0);
            } else if(rio2 > 0.0) {
                info.profundidade = 5 + (int)(intensidadeRio * 5.0);
                info.largura = 4 + (int)(intensidadeRio * 6.0);
            } else {
                info.profundidade = 3 + (int)(intensidadeRio * 3.0);
                info.largura = 2 + (int)(intensidadeRio * 4.0);
            }
        }

        return info;
    }

    /**
     * Calcula uma rede de rios usando técnica de cellular/voronoi modificada
     */
    private double calcularRedeRio(int x, int z, double escala, double espessura) {
        double fx = x * escala;
        double fz = z * escala;

        // Cria "linhas de fluxo" usando ruído direcional
        double fluxoX = ruido.ruidoFractal(fx, fz, 3, 0.5, 2.0);
        double fluxoZ = ruido.ruidoFractal(fx + 1000, fz + 1000, 3, 0.5, 2.0);

        // Segue o fluxo para encontrar "vales"
        double distanciaFluxo = calcularDistanciaFluxo(fx, fz, fluxoX, fluxoZ);

        // Converte distância em intensidade de rio
        double limiar = espessura;
        if(distanciaFluxo < limiar) {
            // Cria perfil suave nas margens
            double fator = 1.0 - (distanciaFluxo / limiar);
            return Math.pow(fator, 2.0); // Curva quadrática para margens suaves
        }

        return 0.0;
    }

    /**
     * Calcula a distância ao "vale" mais próximo seguindo o fluxo
     */
    private double calcularDistanciaFluxo(double x, double z, double fluxoX, double fluxoZ) {
        // Usa função de ruído para criar linhas de vale
        double linha = Math.abs(ruido.ruido(x + fluxoX * 2, z + fluxoZ * 2));

        // Adiciona ramificações
        double ramificacao = Math.abs(ruido.ruido(x * 2 - fluxoX, z * 2 - fluxoZ));

        return Math.min(linha, ramificacao * 1.5);
    }

    /**
     * Calcula informações sobre lagos
     */
    public InfoLago calcularLago(int x, int z) {
        InfoLago info = new InfoLago();

        // Lagos em múltiplas escalas
        double lago1 = calcularLago(x, z, 0.0003, 25.0);  // Lagos grandes
        double lago2 = calcularLago(x, z, 0.0008, 15.0);  // Lagos médios
        double lago3 = calcularLago(x, z, 0.0015, 8.0);   // Lagos pequenos

        double intensidade = Math.max(Math.max(lago1, lago2), lago3);

        if(intensidade > 0.0) {
            info.ehLago = true;
            info.intensidade = intensidade;

            // Profundidade baseada no tamanho
            if(lago1 > 0.0) {
                info.profundidade = 6 + (int)(intensidade * 10.0);  // 6-16 blocos
                info.raio = 25.0;
            } else if(lago2 > 0.0) {
                info.profundidade = 4 + (int)(intensidade * 8.0);   // 4-12 blocos
                info.raio = 15.0;
            } else {
                info.profundidade = 3 + (int)(intensidade * 5.0);   // 3-8 blocos
                info.raio = 8.0;
            }
        }

        return info;
    }

    /**
     * Calcula um lago individual usando ruído circular
     */
    private double calcularLago(int x, int z, double escala, double raio) {
        double fx = x * escala;
        double fz = z * escala;

        // Encontra "centros" de lago usando ruído celular
        double centro = ruidoLagos.ruidoFractal(fx, fz, 2, 0.5, 2.0);

        // Filtra apenas pontos baixos (depressões)
        if(centro < -0.3) {
            // Calcula distância ao centro mais próximo
            int cx = (int)Math.floor(fx);
            int cz = (int)Math.floor(fz);

            double menorDist = Double.MAX_VALUE;

            // Verifica células vizinhas
            for(int dx = -1; dx <= 1; dx++) {
                for(int dz = -1; dz <= 1; dz++) {
                    double testX = (cx + dx) / escala;
                    double testZ = (cz + dz) / escala;

                    double testCentro = ruidoLagos.ruidoFractal(testX * escala, testZ * escala, 2, 0.5, 2.0);

                    if(testCentro < -0.3) {
                        double dist = Math.sqrt((x - testX) * (x - testX) + (z - testZ) * (z - testZ));
                        menorDist = Math.min(menorDist, dist);
                    }
                }
            }

            // Cria perfil circular suave
            if(menorDist < raio) {
                double fator = 1.0 - (menorDist / raio);
                return Math.pow(fator, 1.5); // Curva para margens naturais
            }
        }

        return 0.0;
    }

    /**
     * Calcula profundidade extra para oceanos (trincheiras)
     */
    public int calcularProfundidadeOceano(int x, int z, int profundidadeBase) {
        double fx = x * 0.0001;
        double fz = z * 0.0001;

        // Trincheiras oceânicas profundas
        double trincheira = ruido.ruidoFractal(fx, fz, 4, 0.6, 2.0);

        if(trincheira < -0.5) {
            return (int)((Math.abs(trincheira) - 0.5) * 40.0); // Até 20 blocos extras
        }

        return 0;
    }

    /**
     * Informações sobre um rio em uma posição
     */
    public static class InfoRio {
        public boolean ehRio = false;
        public double intensidade = 0.0;
        public int profundidade = 0;
        public int largura = 0;
    }

    /**
     * Informações sobre um lago em uma posição
     */
    public static class InfoLago {
        public boolean ehLago = false;
        public double intensidade = 0.0;
        public int profundidade = 0;
        public double raio = 0.0;
    }
}

