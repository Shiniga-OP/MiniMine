package com.minimine.mundo;

// java
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
// utils
import com.minimine.utils.arrays.FloatArrayUtil;
import com.minimine.utils.arrays.ShortArrayUtil;
import com.minimine.utils.Mat;
import com.minimine.utils.NuvensUtil;
import com.minimine.utils.DiaNoiteUtil;
import com.minimine.graficos.Texturas;
import com.minimine.utils.CorposCelestes;
// libgdx
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Pool;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
// ruidos
import com.minimine.utils.ruidos.Simplex3D;
import com.minimine.utils.ruidos.Simplex2D;
// blocos
import com.minimine.mundo.blocos.Bloco;
// graficos
import com.minimine.graficos.Render;
import com.minimine.graficos.Animacoes2D;
import com.minimine.entidades.Jogador;
import java.util.Random;
import java.nio.charset.StandardCharsets;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import com.minimine.utils.arrays.ArrayReuso;
import com.minimine.entidades.Entidade;
import com.minimine.mundo.blocos.BlocoModelo;
import com.minimine.mundo.geracao.MotorGeracao;
import com.minimine.mundo.geracao.RegistroBiomas;
import com.minimine.entidades.RegistroCriaturas;

public class Mundo {
    public static String nome = "novo mundo";

	public static List<Entidade> entidades = new ArrayList<>();
	public static RegistroCriaturas registroCriaturas;

	public static float GRAVIDADE = -30f;

	public static final List<Chunk> praLiberar = new ArrayList<>();
	public static final List<Long> praRemover = new ArrayList<>();

    public static Map<Long, Chunk> chunks = new ConcurrentHashMap<>();
    public static Map<Long, Chunk> chunksMod = new ConcurrentHashMap<>();
	// estados: 0 = vazia, 1 = dados Prontos, 2 = malha Pronta
	public static final Map<Long, Integer> estados = new ConcurrentHashMap<>();

    public static final int TAM_CHUNK = 16, Y_CHUNK = 256;
    public static final int CHUNK_AREA = TAM_CHUNK * TAM_CHUNK;
    public static long semente = 0;
	public static int RAIO_CHUNKS = 5;

    public static boolean carregado = false, ciclo = true, nuvens = true;
    
    public static ExecutorService exec;
	
	public static MotorGeracao motor;
	public static RegistroBiomas registroBiomas;

    public void iniciar() {
        semente = semente == 0 ? (System.currentTimeMillis() * MathUtils.random(2, 10)) : semente;
       
		registroCriaturas = new RegistroCriaturas();
		registroCriaturas.carregar(Gdx.files.internal("criaturas/"));
		
		registroBiomas = new RegistroBiomas();
		registroBiomas.carregarBiomas(Gdx.files.internal("biomas/"));
		
		motor = new MotorGeracao(semente, registroBiomas);

        if(exec == null || exec.isShutdown()) exec = Executors.newFixedThreadPool(8);
    }

    // chamado em render:
    public void att(float delta, Jogador jg) {
		if(exec.isShutdown()) return;

		attChunks((int)jg.posicao.x, (int)jg.posicao.z);

		if(!carregado && estados.size() >= 1) {
			Integer x = estados.get(Chave.calcularChave((int)jg.posicao.x >> 4, (int)jg.posicao.z >> 4));
			if(x != null && x == 2) carregado = true;
		}
		if(carregado) {
			GerenciadorEntidades.att(delta, this, jg);
			if(ciclo) CorposCelestes.att(jg.camera.combined, jg.posicao);
		}
    }

    // chamado em dispose:
    public void liberar() {
        for(Chunk chunk : chunks.values()) {
            if(chunk.malha != null) {
                chunk.malha.dispose();
				chunk.malha = null;
            }
        }
		for(Chunk chunk : chunksMod.values()) {
            if(chunk.malha != null) {
				chunk.malha = null;
            }
		}
        for(Entidade e : entidades) {
			e.liberar();
			e = null;
		}
		chunksMod.clear();
        chunks.clear();
		estados.clear();
		entidades.clear();
        exec.shutdown();
		if(com.minimine.ui.UI.debug) Gdx.app.log("ArrayReuso", ArrayReuso.estatisticas());
		ArrayReuso.limparPools();
    }

    public static int obterBlocoMundo(int x, int y, int z) {
        if(y < 0 || y >= Y_CHUNK) return 0; // ar(fora dos limites)

        Chunk chunk = chunks.get(Chave.calcularChave(x >> 4, z >> 4));

        if(chunk == null) return 0;

        int localX = x & 0xF;
        int localZ = z & 0xF;

        return ChunkUtil.obterBloco(localX, y, localZ, chunk);
    }

    public static void defBlocoMundo(int x, int y, int z, CharSequence bloco) {
        if(y < 0 || y >= Y_CHUNK) return; // fora dos limites

        final int chunkX = x >> 4;
        final int chunkZ = z >> 4;

		final long chave = Chave.calcularChave(chunkX, chunkZ);

        Chunk chunk = chunks.get(chave);
        if(chunk == null) {
			Gdx.app.log("Mundo", "chunk null na posição X: "+chunkX+", Z: "+chunkZ);
			return;
		}
        int localX = x & 0xF;
        int localZ = z & 0xF;

		int blocoAntigoId = ChunkUtil.obterBloco(localX, y, localZ, chunk);

		// detecta se o bloco antigo era um emissor de luz
		boolean eraEmissor = false;
		if(blocoAntigoId != 0) {
			if(Bloco.numIds.get(blocoAntigoId).luz > 0) {
				eraEmissor = true;
			}
		}
		if(blocoAntigoId != 0 && bloco != null) {
			Render.gp.criar(x, y, z, Texturas.atlas.get(Bloco.numIds.get(blocoAntigoId).lados));
		}
		// se era emissor, zera luz antes de remover o bloco
		// isso evita que chunks importem luz antiga durante recalculo
		if(eraEmissor) ChunkLuz.zerarLuz(chunk);

        ChunkUtil.defBloco(localX, y, localZ, bloco, chunk);

		// se não era emissor, marca chunk e vizinhas normalmente
		if(!eraEmissor) chunk.luzSuja = true;
        chunk.att = true;

		Chunk chunkAdj;

        // marca chunks vizinhas pra atualizar malha se o bloco ta na borda
        if(localX == 0) {
            chunkAdj = chunks.get(Chave.calcularChave(chunkX - 1, chunkZ));
            if(chunkAdj != null) chunkAdj.att = true;
        }
        if(localX == TAM_CHUNK - 1) {
            chunkAdj = chunks.get(Chave.calcularChave(chunkX + 1, chunkZ));
            if(chunkAdj != null) chunkAdj.att = true;
        }
        if(localZ == 0) {
            chunkAdj = chunks.get(Chave.calcularChave(chunkX, chunkZ - 1));
            if(chunkAdj != null) chunkAdj.att = true;
        }
        if(localZ == TAM_CHUNK - 1) {
            chunkAdj = chunks.get(Chave.calcularChave(chunkX, chunkZ + 1));
            if(chunkAdj != null) chunkAdj.att = true;
        }
        // marca as 4 vizinhas diretas como luzSuja
        // porque a luz pode viajar até elas independente de onde o bloco ta
        if(!eraEmissor) {
            chunkAdj = chunks.get(Chave.calcularChave(chunkX - 1, chunkZ));
            if(chunkAdj != null) chunkAdj.luzSuja = true;

			chunkAdj = chunks.get(Chave.calcularChave(chunkX + 1, chunkZ));
            if(chunkAdj != null) chunkAdj.luzSuja = true;

            chunkAdj = chunks.get(Chave.calcularChave(chunkX, chunkZ - 1));
            if(chunkAdj != null) chunkAdj.luzSuja = true;

            chunkAdj = chunks.get(Chave.calcularChave(chunkX, chunkZ + 1));
            if(chunkAdj != null) chunkAdj.luzSuja = true;
        }
        chunksMod.put(chave, chunk);
    }

	public static void defLuzMundo(int x, int y, int z, byte novaLuz) {
		if(y < 0 || y >= Y_CHUNK) return;

		// localiza a chunk alvo pelas coordenadas globais
		Chunk alvo = chunks.get(Chave.calcularChave(x >> 4, z >> 4));

		if(alvo != null) {
			int lx = x & 0xF;
			int lz = z & 0xF;
			int idc = lx + (lz << 4) + (y << 8);
			alvo.luz[idc] = novaLuz;
			alvo.luzSuja = true; // marca pra reconstruir a malha
			alvo.att = true;
		}
	}

	public static byte obterLuzMundo(int x, int y, int z) {
		if(y < 0 || y >= Y_CHUNK) return 0;

		Chunk chunk = chunks.get(Chave.calcularChave(x >> 4, z >> 4));

		if(chunk == null) return 0;

		int localX = x & 0xF;
		int localZ = z & 0xF;
		int idc = localX + (localZ << 4) + (y << 8);

		return chunk.luz[idc];
	}

	public static int obterAlturaChao(int x, int z) {
		// começa do topo do mundo(Y_CHUNK - 1) e desce
		for(int y = Y_CHUNK - 1; y > 0; y--) {
			int blocoId = obterBlocoMundo(x, y, z);
			if(blocoId != 0) {
				return y + 1; // retorna a posição logo acima do bloco encontrado
			}
		}
		return 80; // caso de segurança: se o mundo estiver vazio, nasce no 80
	}

    // GERAÇÃO DE DADOS:
    public boolean deveAttChunk(int chunkX, int chunkZ, int x, int z) {
        int distX = Mat.abs(chunkX - x);
        int distZ = Mat.abs(chunkZ - z);
        // prioriza chunks mais proximos
        if(distX <= 1 && distZ <= 1) return true; // alta prioridade
        if(distX <= RAIO_CHUNKS/2 && distZ <= RAIO_CHUNKS/2) return true; // media prioridade
        return distX <= RAIO_CHUNKS && distZ <= RAIO_CHUNKS; // baixa prioridade
    }

    public void attChunks(int x, int z) {
		final int cx = x >> 4;
		final int cz = z >> 4;
		limparChunks(cx, cz);

		// gera em ordem de distancia crescente(centro primeiro)
		for(int dx = -RAIO_CHUNKS; dx <= RAIO_CHUNKS; dx++) {
			for(int dz = -RAIO_CHUNKS; dz <= RAIO_CHUNKS; dz++) {
				tentarGerarChunk(cx + dx, cz + dz);
			}
		}
	}

    public void limparChunks(int chunkX, int chunkZ) {
		praLiberar.clear();
		praRemover.clear();

		for(Map.Entry<Long, Chunk> e : chunks.entrySet()) {
			long chave = e.getKey();
			int distX = Mat.abs(Chave.x(chave) - chunkX);
			int distZ = Mat.abs(Chave.z(chave) - chunkZ);
			Chunk chunk = e.getValue();

			if(distX > RAIO_CHUNKS || distZ > RAIO_CHUNKS) {
				if(!chunksMod.containsKey(chave)) praLiberar.add(chunk);
				if(chunk != null) {
					if(chunk.malha != null) praLiberar.add(chunk);
				}
				praRemover.add(chave);
			} else if(chunk.att && !chunk.fazendo && estados.getOrDefault(chave, 0) >= 1) {
				// Só atualiza se os dados já estão prontos — evita gerar malha de chunk ainda em geração
				if(chunk.luzSuja) {
					ChunkLuz.attLuz(chunk);
				}
				if(vizinhosProntos(chunk.x, chunk.z)) {
					gerarMalha(chave);
				}
			}
		}
		if(!praLiberar.isEmpty() || !praRemover.isEmpty()) {
			Gdx.app.postRunnable(new Runnable() {
					@Override
					public void run() {
						for(Chunk c : praLiberar) {
							if(c.malha != null) {
								c.malha.dispose();
								c.malha = null;
							}
						}
						for(long k : praRemover) chunks.remove(k);
					}
				});
		}
	}

	public void tentarGerarChunk(int x, int z) {
		final long chave = Chave.calcularChave(x, z);
		// 1. verifica se ja ta carregada no mapa ativo
		if(chunks.containsKey(chave)) {
			// se ja existe e precisa de malha, gera
			int estado = estados.getOrDefault(chave, 0);
			if(estado == 1 && !chunks.get(chave).fazendo) {
				if(vizinhosProntos(x, z)) gerarMalha(chave);
			}
			return;
		}
		// 2. se não ta no ativo, verifica se ela existe no chunksMod de modificadas
		Chunk modificado = chunksMod.get(chave);
		if(modificado != null) {
			chunks.put(chave, modificado);
			ChunkLuz.calcularLuz(modificado);
			estados.put(chave, 1); // marca como pronta para malha
			return;
		}
		// 3. so se for realmente nova, gera do zero
		Chunk novo = new Chunk();
		novo.x = x; novo.z = z;
		ChunkUtil.compactar(ChunkUtil.bitsPraMaxId(novo.maxIds), novo);
		chunks.put(chave, novo);
		estados.put(chave, 0);
		gerarDados(chave);
	}
	// verifica se as 8 vizinhas ao redor ja tem dados de blocos
	public boolean vizinhosProntos(int cx, int cz) {
		for(int x = cx - 1; x <= cx + 1; x++) {
			for(int z = cz - 1; z <= cz + 1; z++) {
				if(x == cx && z == cz) continue;
				long vizinha = Chave.calcularChave(x, z);
				// se a vizinha não tem estado ou ainda ta no estado 0(sem dados)
				if(estados.getOrDefault(vizinha, 0) < 1) return false;
			}
		}
		return true;
	}

	public static void gerarDados(final long chave) {
		final Chunk chunk = chunks.get(chave);
		
		exec.submit(new Runnable() {
				@Override
				public void run() {
					try {
						// pré-processa tudo antes de publicar o estado
						motor.gerarChunk(chunk);
						chunk.dadosProntos = true;
						// sinal atomico: so agora o chunk é elegivel pra malha
						estados.put(chave, 1);
					} catch(final Exception e) {
						throw new RuntimeException("[Mundo]: [ERRO]: ao gerar dados: "+e);
					}
				}
			});
	}

	public static void gerarMalha(final long chave) {
		final Chunk chunk = chunks.get(chave);
		if(chunk == null) return;
		chunk.fazendo = true;

		exec.submit(new Runnable() {
				@Override
				public void run() {
					ChunkLuz.calcularLuz(chunk);
					
					final FloatArrayUtil vertsGeral = ArrayReuso.obterFloatArray();
					final ShortArrayUtil idcSolidos = ArrayReuso.obterShortArray();
					final ShortArrayUtil idcTransp = ArrayReuso.obterShortArray();

					ChunkMalha.attMalha(chunk, vertsGeral, idcSolidos, idcTransp);

					final short[] idcFinal = new short[idcSolidos.tam + idcTransp.tam];
					System.arraycopy(idcSolidos.praArray(), 0, idcFinal, 0, idcSolidos.tam);
					System.arraycopy(idcTransp.praArray(), 0, idcFinal, idcSolidos.tam, idcTransp.tam);

					// armazena o tamanho real do buffer de indices
					final int totalIndices = idcFinal.length;

					Gdx.app.postRunnable(new Runnable() {
							@Override
							public void run() {
								try {
									if(chunk.malha != null) {
										chunk.malha.dispose();
										chunk.malha = null;
									}
									final int numVerts = vertsGeral.tam / 5;

									// usa totalIndices pra evitar crash
									chunk.malha = new Mesh(true, numVerts, totalIndices, Render.atriburs);
									chunk.malha.setVertices(vertsGeral.praArray());
									chunk.malha.setIndices(idcFinal);

									// verifica se ta consistente
									if(chunk.malha.getNumIndices() != totalIndices) {
										Gdx.app.error("Mundo", "INCONSISTÊNCIA CRÍTICA: mesh tem " + 
													  chunk.malha.getNumIndices() + " índices, mas deveria ter " + totalIndices);
									}
									// atualiza os contadores
									chunk.contaSolida = idcSolidos.tam;
									chunk.contaTransp = idcTransp.tam;
									chunk.fazendo = false;
									chunk.att = false;
									estados.put(chave, 2);
								} catch(Exception e) {
									Gdx.app.error("Mundo", "Erro ao gerar malha do chunk", e);
								} finally {
									ArrayReuso.devolver(vertsGeral);
									ArrayReuso.devolver(idcSolidos);
									ArrayReuso.devolver(idcTransp);
								}
							}
						});
				}
			});
	}

	public static void gerarChunk(final long chave) {
		final Chunk chunk = chunks.get(chave);
		chunk.x = Chave.x(chave);
		chunk.z = Chave.z(chave);
		chunk.fazendo = true;

		exec.submit(new Runnable() {
				@Override
				public void run() {
					prepararDadosVizinhos(chunk.x, chunk.z);

					final FloatArrayUtil vertsGeral = ArrayReuso.obterFloatArray();
					final ShortArrayUtil idcSolidos = ArrayReuso.obterShortArray();
					final ShortArrayUtil idcTransp = ArrayReuso.obterShortArray();

					ChunkMalha.attMalha(chunk, vertsGeral, idcSolidos, idcTransp);

					final short[] idcFinal = new short[idcSolidos.tam + idcTransp.tam];
					System.arraycopy(idcSolidos.praArray(), 0, idcFinal, 0, idcSolidos.tam);
					System.arraycopy(idcTransp.praArray(), 0, idcFinal, idcSolidos.tam, idcTransp.tam);

					// guarda o total de indices pra não perder a referencia
					final int totalIndices = idcFinal.length;

					Gdx.app.postRunnable(new Runnable() {
							@Override
							public void run() {
								try {
									if(chunk.malha != null) {
										chunk.malha.dispose();
										chunk.malha = null;
									}
									final int numVerts = vertsGeral.tam / 5;

									// usa totalIndices
									chunk.malha = new Mesh(true, numVerts, totalIndices, Render.atriburs);
									chunk.malha.setVertices(vertsGeral.praArray());
									chunk.malha.setIndices(idcFinal);

									// atualiza os contadores
									chunk.contaSolida = idcSolidos.tam;
									chunk.contaTransp = idcTransp.tam;
									chunk.fazendo = false;
									chunk.att = false;
								} catch(Exception e) {
									Gdx.app.error("Mundo", "Erro em gerarChunk", e);
								} finally {
									ArrayReuso.devolver(vertsGeral);
									ArrayReuso.devolver(idcSolidos);
									ArrayReuso.devolver(idcTransp);
								}
							}
						});
				}
			});
	}

	public static void prepararDadosVizinhos(int cx, int cz) {
		// define um raio de vizinhos necessarios(pelo menos as 4 direções)
		for(int x = cx - 1; x <= cx + 1; x++) {
			for(int z = cz - 1; z <= cz + 1; z++) {
				long vizinhaChave = Chave.calcularChave(x, z);
				Chunk v = chunks.get(vizinhaChave);

				// se a chunk vizinha não existe, cria ela apenas com dados(sem malha ainda)
				if(v == null) {
					v = new Chunk();
					v.x = x; v.z = z;
					ChunkUtil.compactar(ChunkUtil.bitsPraMaxId(v.maxIds), v);
					chunks.put(vizinhaChave, v);
				}
				// se os blocos ainda não foram gerados(biomas não escolhidos)
				if(!v.dadosProntos) {
					motor.gerarChunk(v);
					ChunkLuz.calcularLuz(v);
					v.dadosProntos = true;
				}
			}
		}
	}

	public static String decodificarNome(String nome){
	    try {
	    	return URLDecoder.decode(nome, StandardCharsets.UTF_8.name());
	    } catch(UnsupportedEncodingException e) {
			Gdx.app.error("[Mundo]", "erro ao decodificar nome: "+e);
			return "Desconhecido";
	    } 
    }
}

