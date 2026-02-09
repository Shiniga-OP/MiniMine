package com.minimine.mundo;

import com.minimine.mundo.geracao.GeradorTerreno;
import com.minimine.mundo.geracao.GeradorTerreno.TipoBioma;
import com.minimine.mundo.geracao.Arvores;

// interface entre o gerador de terreno e o sistema de chunks
public class Biomas {
    public static GeradorTerreno gerador;

    public static void iniciar() {
        gerador = new GeradorTerreno(Mundo.semente);
    }

    public static void escolher(Chunk chunk) {
        int chunkX = chunk.x << 4;
        int chunkZ = chunk.z << 4;

        for(int x = 0; x < 16; x++) {
            for(int z = 0; z < 16; z++) {
                int mundoX = chunkX + x;
                int mundoZ = chunkZ + z;

                // recebe altura e bioma em uma unica chamada otimizada
                int[] dados = gerador.calcularDadosColuna(mundoX, mundoZ);
                int altura = dados[0];
                TipoBioma bioma = TipoBioma.values()[dados[1]];

                gerarColuna(chunk, x, z, altura, bioma, mundoX, mundoZ);
            }
        }
        // adiciona arvores depois gerar todas as colunas
        addArvores(chunk, chunkX, chunkZ);

        chunk.dadosProntos = true;
    }

    public static void gerarColuna(Chunk chunk, int x, int z, int altura, TipoBioma bioma, int mundoX, int mundoZ) {
        // fim do mundo
        ChunkUtil.defBloco(x, 0, z, "pedra", chunk);

        // verifica cavernas para cada camada
        for(int y = 1; y < altura - 4; y++) {
            if(!gerador.temCaverna(mundoX, y, mundoZ)) {
                ChunkUtil.defBloco(x, y, z, "pedra", chunk);
            }
        }
        // camadas superiores por bioma
        switch(bioma) {
            case OCEANO:
            case OCEANO_QUENTE:
            case OCEANO_PROFUNDO:
                // areia ou pedra
                int profundidade = 62 - altura;
                for(int y = altura - 4; y < altura; y++) {
                    if(!gerador.temCaverna(mundoX, y, mundoZ)) {
                        if(profundidade > 20) {
                            ChunkUtil.defBloco(x, y, z, "pedra", chunk);
                        } else {
                            ChunkUtil.defBloco(x, y, z, "areia", chunk);
                        }
                    }
                }
                // agua
                for(int y = altura; y <= 62; y++) {
                    ChunkUtil.defBloco(x, y, z, "agua", chunk);
                }
				break;
            case OCEANO_COSTEIRO:
                // areia
                for(int y = altura - 3; y < altura; y++) {
                    if(!gerador.temCaverna(mundoX, y, mundoZ)) {
                        ChunkUtil.defBloco(x, y, z, "areia", chunk);
                    }
                }
                // agua rasa
                for(int y = altura; y <= 62; y++) {
                    ChunkUtil.defBloco(x, y, z, "agua", chunk);
                }
				break;
            case DESERTO:
            case COLINAS_DESERTO:
                // areia profunda
                for(int y = altura - 5; y < altura; y++) {
                    if(!gerador.temCaverna(mundoX, y, mundoZ)) {
                        ChunkUtil.defBloco(x, y, z, "areia", chunk);
                    }
                }
                // cactos esparsos no deserto
                double cactoChance = Mundo.s2D.ruido(mundoX * 0.1, mundoZ * 0.1);
                if(cactoChance > 0.85 && altura > 62) {
                    int alturaCacto = (int)((Mundo.s2D.ruido(mundoX * 0.3, mundoZ * 0.3) * 0.5 + 0.5) * 2) + 2;
                    for(int cy = 0; cy < alturaCacto; cy++) {
                        ChunkUtil.defBloco(x, altura + cy, z, "cacto", chunk);
                    }
                }
				break;
            case PLANICIES:
            case PLANICIES_MONTANHOSAS:
                // terra
                for(int y = altura - 4; y < altura - 1; y++) {
                    if(!gerador.temCaverna(mundoX, y, mundoZ)) {
                        ChunkUtil.defBloco(x, y, z, "terra", chunk);
                    }
                }
                // grama
                if(!gerador.temCaverna(mundoX, altura - 1, mundoZ)) {
                    ChunkUtil.defBloco(x, altura - 1, z, "grama", chunk);
                }
				break;
            case PLANICIES_AGUADAS:
                // terra
                for(int y = altura - 4; y < altura - 1; y++) {
                    if(!gerador.temCaverna(mundoX, y, mundoZ)) {
                        ChunkUtil.defBloco(x, y, z, "terra", chunk);
                    }
                }
                // verifica se é lago
                double lago = Mundo.s2D.ruido(mundoX * 0.015, mundoZ * 0.015);
                if(lago < -0.5) {
                    ChunkUtil.defBloco(x, altura - 1, z, "areia", chunk);
                    ChunkUtil.defBloco(x, altura, z, "agua", chunk);
                    ChunkUtil.defBloco(x, altura + 1, z, "agua", chunk);
                } else {
                    if(!gerador.temCaverna(mundoX, altura - 1, mundoZ)) {
                        ChunkUtil.defBloco(x, altura - 1, z, "grama", chunk);
                    }
                }
				break;
            case FLORESTA:
            case FLORESTA_COSTEIRA:
            case FLORESTA_MONTANHOSA:
                // terra
                int profTerra = bioma == TipoBioma.FLORESTA_MONTANHOSA ? 3 : 4;
                for(int y = altura - profTerra; y < altura - 1; y++) {
                    if(!gerador.temCaverna(mundoX, y, mundoZ)) {
                        ChunkUtil.defBloco(x, y, z, "terra", chunk);
                    }
                }
                // grama
                if(!gerador.temCaverna(mundoX, altura - 1, mundoZ)) {
                    ChunkUtil.defBloco(x, altura - 1, z, "grama", chunk);
                }
				break;
            case FLORESTA_COM_RIOS:
                // terra
                for(int y = altura - 4; y < altura - 1; y++) {
                    if(!gerador.temCaverna(mundoX, y, mundoZ)) {
                        ChunkUtil.defBloco(x, y, z, "terra", chunk);
                    }
                }
                // verifica se é rio
                double rio = Math.abs(Mundo.s2D.ruido(mundoX * 0.008, mundoZ * 0.008));
                if(rio < 0.08) {
                    ChunkUtil.defBloco(x, altura - 1, z, "areia", chunk);
                    ChunkUtil.defBloco(x, altura, z, "agua", chunk);
                } else {
                    if(!gerador.temCaverna(mundoX, altura - 1, mundoZ)) {
                        ChunkUtil.defBloco(x, altura - 1, z, "grama", chunk);
                    }
                }
				break;
        }
    }

    // adiciona arvores depois toda a chunk ta gerada
    public static void addArvores(Chunk chunk, int chunkX, int chunkZ) {
        for(int x = 2; x < 14; x++) {
            for(int z = 2; z < 14; z++) {
                int mundoX = chunkX + x;
                int mundoZ = chunkZ + z;

                // usa pra decidir se coloca arvore
                double arvoreRuido = Mundo.s2D.ruido(mundoX * 0.1, mundoZ * 0.1);

                // encontra a altura do terreno nesta posição
                int altura = -1;
                for(int y = Mundo.Y_CHUNK - 1; y >= 0; y--) {
                    int bloco = ChunkUtil.obterBloco(x, y, z, chunk);
                    if(bloco != 0) {
                        altura = y;
                        break;
                    }
                }
                if(altura <= 0 || altura <= 62) continue; // não gera arvores na agua

                int blocoBase = ChunkUtil.obterBloco(x, altura, z, chunk);

                // florestas: arvores densas
                if(blocoBase == 1) { // grama
                    // verifica bioma original para densidade
                    int[] dados = gerador.calcularDadosColuna(mundoX, mundoZ);
                    TipoBioma bioma = TipoBioma.values()[dados[1]];

                    double limite = 0.7; // padrão

                    if(bioma == TipoBioma.FLORESTA || bioma == TipoBioma.FLORESTA_COSTEIRA) {
                        limite = 0.65; // mais densa
                    } else if(bioma == TipoBioma.FLORESTA_MONTANHOSA) {
                        limite = 0.75; // menos densa
                    } else if(bioma == TipoBioma.FLORESTA_COM_RIOS) {
                        limite = 0.68;
                    } else {
                        continue; // outros biomas não tem arvores neste sistema
                    }
                    if(arvoreRuido > limite) {
                        gerarArvore(chunk, x, altura + 1, z, mundoX, mundoZ, bioma);
                    }
                }
            }
        }
    }

    // gera uma arvore com variações
    public static void gerarArvore(Chunk chunk, int x, int y, int z, int mundoX, int mundoZ, TipoBioma bioma) {
        // usa ruído para determinar tipo e tamanho da árvore
        double tipoRuido = Mundo.s2D.ruido(mundoX * 0.2, mundoZ * 0.2);
        double tamRuido = Mundo.s2D.ruido(mundoX * 0.25, mundoZ * 0.25);

        int alturaTronco = 4 + (int)((tamRuido * 0.5 + 0.5) * 3); // 4-7 blocos

        // determina o tipo de arvore
        if(bioma == TipoBioma.FLORESTA_MONTANHOSA) {
            // arvores conicas(pinheiros)
            Arvores.gerarArvoreConica(chunk, x, y, z, alturaTronco);
        } else if(tipoRuido > 0.5) {
            // arvore normal com copa arredondada
            Arvores.gerarArvoreNormal(chunk, x, y, z, alturaTronco);
        } else {
            // arvore com copa larga
            Arvores.gerarArvoreLarga(chunk, x, y, z, alturaTronco);
        }
    }
}

