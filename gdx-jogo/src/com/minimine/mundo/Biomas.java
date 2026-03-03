package com.minimine.mundo;

import com.minimine.mundo.geracao.GeradorTerreno;
import com.minimine.mundo.geracao.GeradorTerreno.TipoBioma;
import com.minimine.mundo.geracao.Arvores;

// interface entre o gerador de terreno e o sistema de chunks
public class Biomas {
    public static GeradorTerreno gerador;
	public static final TipoBioma[][] biomasCache = new TipoBioma[16][16];
	public static final int[] bufferDados = new int[2];
	/*
	 calcula e armazena alturas/biomas em cache pra evitar
	 que addArvores chame calcularDadosColuna de novo pra cada coluna(eliminava
	 até 144 chamadas extras por chunk de floresta, duplicando o tempo de geração)
	 */
    public static void iniciar() {
        gerador = new GeradorTerreno(Mundo.semente);
    }

    public static void escolher(Chunk chunk) {
        int chunkX = chunk.x << 4;
        int chunkZ = chunk.z << 4;
		
		final int[][] alturas = new int[16][16];
		
        for(int x = 0; x < 16; x++) {
            for(int z = 0; z < 16; z++) {
                int mundoX = chunkX + x;
                int mundoZ = chunkZ + z;
				
				final int[] dados = new int[2];

                gerador.calcularDadosColuna(mundoX, mundoZ, dados);
                alturas[x][z] = dados[0];
                biomasCache[x][z] = TipoBioma.values()[dados[1]];

                gerarColuna(chunk, x, z, alturas[x][z], biomasCache[x][z], mundoX, mundoZ);
            }
        }
        // adiciona arvores depois de gerar todas as colunas, usando o cache
        addArvores(chunk, chunkX, chunkZ, biomasCache, alturas);
        chunk.dadosProntos = true;
    }

    public static void gerarColuna(Chunk chunk, int x, int z, int altura, TipoBioma bioma, int mundoX, int mundoZ) {
        // fim do mundo
        ChunkUtil.defBloco(x, 0, z, "pedra", chunk);

        // uma chamada por coluna, o gerador resolve internamente quais Y são vazios
		final boolean[] vazios = new boolean[altura];
        gerador.calcularVaziosColuna(mundoX, mundoZ, altura, vazios);

        // blocos de pedra/cascalho do interior
        for(int y = 1; y < altura - 4; y++) {
            if(!vazios[y]) {
                if(gerador.temCascalho(mundoX, y, mundoZ, altura, bioma)) {
                    ChunkUtil.defBloco(x, y, z, "cascalho", chunk);
                } else {
                    ChunkUtil.defBloco(x, y, z, "pedra", chunk);
                }
            }
        }
        // camadas superiores por bioma, sem nenhuma chamada a temRavina/temArco/temCaverna
        switch(bioma) {
            case OCEANO:
            case OCEANO_QUENTE:
            case OCEANO_ABISSAL:
                int profundidade = 62 - altura;
                for(int y = altura - 4; y < altura; y++) {
                    if(!vazios[y]) {
                        if(profundidade > 20) {
                            ChunkUtil.defBloco(x, y, z, "pedra", chunk);
                        } else {
                            if(gerador.temCascalho(mundoX, y, mundoZ, altura, bioma)) {
                                ChunkUtil.defBloco(x, y, z, "cascalho", chunk);
                            } else {
                                ChunkUtil.defBloco(x, y, z, "areia", chunk);
                            }
                        }
                    }
                }
                for(int y = altura; y <= 62; y++) {
                    ChunkUtil.defBloco(x, y, z, "agua", chunk);
                }
				break;
            case OCEANO_COSTEIRO:
                for(int y = altura - 3; y < altura; y++) {
                    if(!vazios[y]) ChunkUtil.defBloco(x, y, z, "areia", chunk);
                }
                for(int y = altura; y <= 62; y++) {
                    ChunkUtil.defBloco(x, y, z, "agua", chunk);
                }
				break;
            case DESERTO:
            case COLINAS_DESERTO:
                for(int y = altura - 5; y < altura; y++) {
                    if(!vazios[y]) ChunkUtil.defBloco(x, y, z, "areia", chunk);
                }
                double cactoChance = Mundo.s2D.ruido(mundoX * 0.1, mundoZ * 0.1);
                if(cactoChance > 0.85 && altura > 62) {
                    int alturaCacto = (int)((Mundo.s2D.ruido(mundoX * 0.3, mundoZ * 0.3) * 0.5 + 0.5) * 3) + 2;
                    for(int cy = 0; cy < alturaCacto; cy++) {
                        ChunkUtil.defBloco(x, altura + cy, z, "cacto", chunk);
                    }
                }
				break;
            case PLANICIES:
            case PLANICIES_MONTANHOSAS:
                for(int y = altura - 4; y < altura - 1; y++) {
                    if(!vazios[y]) {
                        if(bioma == TipoBioma.PLANICIES_MONTANHOSAS &&
                           gerador.temCascalho(mundoX, y, mundoZ, altura, bioma)) {
                            ChunkUtil.defBloco(x, y, z, "cascalho", chunk);
                        } else {
                            ChunkUtil.defBloco(x, y, z, "terra", chunk);
                        }
                    }
                }
                if(!vazios[altura - 1]) ChunkUtil.defBloco(x, altura - 1, z, "grama", chunk);
				break;
            case FLORESTA:
            case FLORESTA_COSTEIRA:
            case FLORESTA_MONTANHOSA:
                int profTerra = bioma == TipoBioma.FLORESTA_MONTANHOSA ? 3 : 4;
                for(int y = altura - profTerra; y < altura - 1; y++) {
                    if(!vazios[y]) ChunkUtil.defBloco(x, y, z, "terra", chunk);
                }
                if(!vazios[altura - 1]) ChunkUtil.defBloco(x, altura - 1, z, "grama", chunk);
				break;
			case MAR_CONGELADO:
				// camada de gelo na superficie e água abaixo
				for(int y = altura - 3; y < altura; y++) {
					if(!vazios[y]) ChunkUtil.defBloco(x, y, z, "pedra", chunk);
				}
				// gelo no nivel do mar ou no topo da coluna
				if(!vazios[altura - 1]) ChunkUtil.defBloco(x, altura - 1, z, "gelo", chunk);

				for(int y = altura; y <= 62; y++) {
					// no topo do mar, coloca gelo, abaixo coloca água
					if(y == 62) ChunkUtil.defBloco(x, y, z, "gelo", chunk);
					else ChunkUtil.defBloco(x, y, z, "agua", chunk);
				}
				break;
			case TUNDRA:
				// chão de terra coberto com uma camada de neve
				for(int y = altura - 4; y < altura - 1; y++) {
					if(!vazios[y]) ChunkUtil.defBloco(x, y, z, "terra", chunk);
				}
				if(!vazios[altura - 1]) ChunkUtil.defBloco(x, altura - 1, z, "neve", chunk);
				break;
			case PICOS_GELADOS:
				// montanhas de pedra pura cobertas de neve grossa e gelo
				for(int y = altura - 5; y < altura - 2; y++) {
					if(!vazios[y]) ChunkUtil.defBloco(x, y, z, "pedra", chunk);
				}
				if(!vazios[altura - 2]) ChunkUtil.defBloco(x, altura - 2, z, "gelo", chunk);
				if(!vazios[altura - 1]) ChunkUtil.defBloco(x, altura - 1, z, "neve", chunk);
				break;
        }
    }

    // recebe cache de biomas/alturas para evitar recalcular colunas
    public static void addArvores(Chunk chunk, int chunkX, int chunkZ, TipoBioma[][] biomasCache, int[][] alturas) {
        for(int x = 2; x < 14; x++) {
            for(int z = 2; z < 14; z++) {
                int mundoX = chunkX + x;
                int mundoZ = chunkZ + z;

                // usa pra decidir se coloca arvore
                double arvoreRuido = Mundo.s2D.ruido(mundoX * 0.1, mundoZ * 0.1);

                // usa o bioma ja calculado, sem nova chamada a calcularDadosColuna
                TipoBioma bioma = biomasCache[x][z];

                // atalho rapido: outros biomas não têm arvores
                if(bioma != TipoBioma.FLORESTA &&
				bioma != TipoBioma.FLORESTA_COSTEIRA &&
				bioma != TipoBioma.FLORESTA_MONTANHOSA) {
                    continue;
                }
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
                    double limite = 0.7; // padrão

                    if(bioma == TipoBioma.FLORESTA || bioma == TipoBioma.FLORESTA_COSTEIRA) {
                        limite = 0.65; // mais densa
                    } else if(bioma == TipoBioma.FLORESTA_MONTANHOSA) {
                        limite = 0.75; // menos densa
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
        // usa ruido para determinar tipo e tamanho da arvore
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

	public static String bioma_str(TipoBioma bioma) {
		switch(bioma) {
			case OCEANO: return "Oceano";
			case OCEANO_COSTEIRO: return "Costa";
			case OCEANO_QUENTE: return "Mar Quente";
			case OCEANO_ABISSAL: return "Oceano Abissal";
			case PLANICIES: return "Planície";
			case PLANICIES_MONTANHOSAS: return "Planície Alta";
			case FLORESTA: return "Floresta";
			case FLORESTA_COSTEIRA: return "Mata Costeira";
			case FLORESTA_MONTANHOSA: return "Serrania";
			case DESERTO: return "Deserto";
			case COLINAS_DESERTO: return "Dunas";
			case MAR_CONGELADO: return "Mar Congelado";
			case TUNDRA: return "Tundra";
			case PICOS_GELADOS: return "Picos Gelados";
			default: return "Desconhecido";
		}
	}

	public static String obterBioma(int x, int z) {
		if(gerador == null) return "Desconhecido";
		
		gerador.calcularDadosColuna(x, z, bufferDados);
		
		TipoBioma bioma = TipoBioma.values()[bufferDados[1]];

		// retorna o nome formatado
		return bioma_str(bioma);
	}

	public static int[] localizarBioma(String nomeBioma, int origemX, int origemZ) {
		// converte nome pro enum
		TipoBioma alvo = null;
		for(TipoBioma b : TipoBioma.values()) {
			if(bioma_str(b).equalsIgnoreCase(nomeBioma)) {
				alvo = b;
				break;
			}
		}
		if(alvo == null) return null; // bioma não reconhecido

		int passo = 64; // verifica a cada 64 blocos
		int raioMax = 100000; // desiste depois de 10 mil blocos

		for(int raio = passo; raio <= raioMax; raio += passo) {
			// varre o perimetro do raio atual em espiral
			for(int dx = -raio; dx <= raio; dx += passo) {
				for(int dz = -raio; dz <= raio; dz += passo) {
					// so verifica a borda do quadrado atual
					if(Math.abs(dx) != raio && Math.abs(dz) != raio) continue;

					int x = origemX + dx;
					int z = origemZ + dz;

					gerador.calcularDadosColuna(x, z, bufferDados);
					
					TipoBioma bioma = TipoBioma.values()[bufferDados[1]];

					if(bioma == alvo) {
						return new int[]{ x, z };
					}
				}
			}
		}
		return null; // não encontrado no raio maximo
	}
}

