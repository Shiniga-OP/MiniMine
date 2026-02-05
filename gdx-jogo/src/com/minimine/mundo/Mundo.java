package com.minimine.mundo;

// java
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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
import com.minimine.graficos.EmissorParticulas;
import com.minimine.cenas.Jogador;
import java.nio.charset.StandardCharsets;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

public class Mundo {
    public static String nome = "novo mundo";

    public static final List<Object> texturas = new ArrayList<>();
	public static final List<Chunk> praLiberar = new ArrayList<>();
	public static final List<Long> praRemover = new ArrayList<>();

    public static Map<Long, Chunk> chunks = new ConcurrentHashMap<>();
    public static Map<Long, Chunk> chunksMod = new ConcurrentHashMap<>();
	// Estados: 0 = Vazia, 1 = Dados Prontos, 2 = Malha Pronta
	public static final Map<Long, Integer> estados = new ConcurrentHashMap<>();

    public static final int TAM_CHUNK = 16, Y_CHUNK = 255;
    public static final int CHUNK_AREA = TAM_CHUNK * TAM_CHUNK;
    public static long semente = 0L;
	public static int RAIO_CHUNKS = 5;

    public static Simplex2D s2D;
	public static Simplex3D s3D;

    public static boolean carregado = false, ciclo = true, nuvens = true;
    public static boolean debugColisao = false;
    public static float tick = 0.2f;

    public static ExecutorService exec;

    public static Matrix4 matrizTmp = new Matrix4();

    static {
        texturas.add(Texturas.texs.get("grama_topo"));
        texturas.add(Texturas.texs.get("grama_lado"));
        texturas.add(Texturas.texs.get("terra"));
        texturas.add(Texturas.texs.get("pedra"));
        texturas.add(Texturas.texs.get("agua"));
        texturas.add(Texturas.texs.get("areia"));
        texturas.add(Texturas.texs.get("tronco_topo"));
        texturas.add(Texturas.texs.get("tronco_lado"));
        texturas.add(Texturas.texs.get("folha"));
        texturas.add(Texturas.texs.get("tabua_madeira"));
        texturas.add(Texturas.texs.get("cacto_topo"));
        texturas.add(Texturas.texs.get("cacto_lado"));
        texturas.add(Texturas.texs.get("vidro"));
        texturas.add(Texturas.texs.get("tocha"));

        Bloco.blocos.add(null);
        Bloco.blocos.add(new Bloco("grama", 0, 1, 2));
        Bloco.blocos.add(new Bloco("terra", 2));
        Bloco.blocos.add(new Bloco("pedra", 3));
        Bloco.blocos.add(new Bloco("agua", 4, true, false, false));
        Bloco.blocos.add(new Bloco("areia", 5));
        Bloco.blocos.add(new Bloco("tronco", 6, 7));
        Bloco.blocos.add(new Bloco("folha", 8, true, true, false));
        Bloco.blocos.add(new Bloco("tabua_madeira", 9));
        Bloco.blocos.add(new Bloco("cacto", 10, 11));
        Bloco.blocos.add(new Bloco("vidro", 12, true, true, false));
        Bloco.blocos.add(new Bloco("tocha", 13, 13, 13, false, true, true, 15));

		Bloco.addSom("grama", "grama_1", "terra_1", "terra_2", "terra_3");
		Bloco.addSom("terra", "terra_1", "terra_2", "terra_3");
		Bloco.addSom("pedra", "pedra_1", "pedra_2");
		Bloco.addSom("folha", "terra_1", "terra_2", "terra_3");
		Bloco.addSom("tabua_madeira", "madeira_1", "madeira_2", "madeira_3");
		Bloco.addSom("tocha", "madeira_1", "madeira_2", "madeira_3");
    }

    public void iniciar() {
        semente = semente == 0 ? (System.currentTimeMillis() >> 1) : semente;
        s2D = new Simplex2D(semente);
		s3D = new Simplex3D(semente >> 1);

		Biomas.iniciar();

        if(exec == null || exec.isShutdown()) {
			exec = Executors.newFixedThreadPool(8);
		}
    }

    // chamado em render:
    public void att(float delta, Jogador jogador) {
		attChunks((int) jogador.posicao.x, (int) jogador.posicao.z);

		if(!carregado && chunks.size() >= 1) {
			carregado = true;
		}
        if(ciclo) {
			CorposCelestes.att(jogador.camera.combined, jogador.posicao);
			// ciclo de dia e noite:
			if(tick > 0.03f) {
				tick = 0;
			}
		}
    }

    public boolean frustrum(Chunk chunk, Jogador jogador) {
		float globalX = chunk.x << 4;
		float globalZ = chunk.z << 4;

		// dist2(distancia ao quadrado)
		float distAoQuadrado = Vector2.dst2(globalX, globalZ, jogador.posicao.x, jogador.posicao.z);

		// o raio precisa sendo convertido pra "ao quadrado" pra comparação funcionar
		// (RAIO * 16) * (RAIO * 16)
		float raioEmPixels = RAIO_CHUNKS << 4;
		float raioLimite = raioEmPixels * raioEmPixels;

		if(!(distAoQuadrado < raioLimite)) return false;

		return jogador.camera.frustum.boundsInFrustum(globalX, 0, globalZ, TAM_CHUNK, Y_CHUNK, TAM_CHUNK);
	}
    // chamado em dispose:
    public static void liberar() {
        for(Chunk chunk : chunks.values()) {
            if(chunk.malha != null) {
                chunk.malha.dispose();
				chunk.malha = null;
            }
        }
		chunks.clear();
        chunks.clear();
		estados.clear();
        exec.shutdown();
		Animacoes2D.liberar();
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
		if(blocoAntigoId != 0 && (bloco == null || bloco.equals("ar"))) {
			// pega a luz atual do mundo pro fragmento não ficar preto
			byte luz = obterLuzMundo(x, y, z);
			float lb = (luz & 0x0F) / 15f;
			float ls = ((luz >> 4) & 0x0F) / 15f;
			// usa a cor compactada compativel com seu shader
			float corFragmento = com.badlogic.gdx.graphics.Color.toFloatBits(lb, ls, 1.0f, 1.0f);
			EmissorParticulas.criar(x, y, z, corFragmento);
		}
        ChunkUtil.defBloco(localX, y, localZ, bloco, chunk);
		chunk.luzSuja = true;
        chunk.att = true;
        // chunks perto pra atualizar se o bloco for na borda
        if(localX == 0) {
            Chunk chunkAdj = chunks.get(Chave.calcularChave(chunkX - 1, chunkZ));
            if(chunkAdj != null) chunkAdj.att = true;
        }
        if(localX == TAM_CHUNK - 1) {
            Chunk chunkAdj = chunks.get(Chave.calcularChave(chunkX + 1, chunkZ));
            if(chunkAdj != null) chunkAdj.att = true;
        }
        if(localZ == 0) {
            Chunk chunkAdj = chunks.get(Chave.calcularChave(chunkX, chunkZ - 1));
            if(chunkAdj != null) chunkAdj.att = true;
        }
        if(localZ == TAM_CHUNK - 1) {
            Chunk chunkAdj = chunks.get(Chave.calcularChave(chunkX, chunkZ + 1));
            if(chunkAdj != null) chunkAdj.att = true;
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

		int dx = 0;
		int dz = 0;
		int passo = 1;

		tentarGerarChunk(cx, cz);

		while(passo <= RAIO_CHUNKS << 1) {
			int i;
			for(i = 0; i < passo; i++) {
				dx++;
				int px = cx + dx;
				int pz = cz + dz;
				if(deveAttChunk(px, pz, cx, cz)) tentarGerarChunk(px, pz);
			}
			for(i = 0; i < passo; i++) {
				dz++;
				int px = cx + dx;
				int pz = cz + dz;
				if(deveAttChunk(px, pz, cx, cz)) tentarGerarChunk(px, pz);
			}
			passo++;

			for(i = 0; i < passo; i++) {
				dx--;
				int px = cx + dx;
				int pz = cz + dz;
				if(deveAttChunk(px, pz, cx, cz)) tentarGerarChunk(px, pz);
			}
			for(i = 0; i < passo; i++) {
				dz--;
				int px = cx + dx;
				int pz = cz + dz;
				if(deveAttChunk(px, pz, cx, cz)) tentarGerarChunk(px, pz);
			}
			passo++;
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
			} else if(chunk.att && !chunk.fazendo) {
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
					Biomas.escolher(chunk);
					estados.put(chave, 1); // agora ta pronta pra que as vizinhas gerem malha
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
					final FloatArrayUtil vertsGeral = new FloatArrayUtil();
					final ShortArrayUtil idcSolidos = new ShortArrayUtil();
					final ShortArrayUtil idcTransp = new ShortArrayUtil();

					ChunkMalha.attMalha(chunk, vertsGeral, idcSolidos, idcTransp);

					// junta os indices: primeiro solidos depois transparentes
					final short[] idcFinal = new short[idcSolidos.tam + idcTransp.tam];
					System.arraycopy(idcSolidos.praArray(), 0, idcFinal, 0, idcSolidos.tam);
					System.arraycopy(idcTransp.praArray(), 0, idcFinal, idcSolidos.tam, idcTransp.tam);

					chunk.contaSolida = idcSolidos.tam;
					chunk.contaTransp = idcTransp.tam;

					Gdx.app.postRunnable(new Runnable() {
							@Override
							public void run() {
								if(chunk.malha == null) {
									chunk.malha = new Mesh(true, Render.maxVerts, Render.maxIndices, Render.atriburs);
								}
								try {
									chunk.malha.setVertices(vertsGeral.praArray());
									chunk.malha.setIndices(idcFinal);

									matrizTmp.setToTranslation(chunk.x << 4, 0, chunk.z << 4);
									chunk.malha.transform(matrizTmp);

									chunk.fazendo = false;
									chunk.att = false;
									estados.put(chave, 2);
								} catch(Exception e) {}
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
					// 1: gera apenas os dados dos blocos(se ainda não existirem)
					// garante que a chunk central e suas 4 vizinhas diretas tenham dados
					prepararDadosVizinhos(chunk.x, chunk.z);

					// 2: gera a malha
					final FloatArrayUtil vertsGeral = new FloatArrayUtil();
					final ShortArrayUtil idcSolido = new ShortArrayUtil();
					final ShortArrayUtil idcTransp = new ShortArrayUtil();

					// agora a attmalha pode checar os vizinhos com segurança
					ChunkMalha.attMalha(chunk, vertsGeral, idcSolido, idcTransp);

					Gdx.app.postRunnable(new Runnable() {
							@Override
							public void run() {
								if(chunk.malha != null) {
									chunk.malha.dispose();
								}
								chunk.malha = new Mesh(true, Render.maxVerts, Render.maxIndices, Render.atriburs);
								chunk.malha.setVertices(vertsGeral.praArray());
								chunk.malha.setIndices(idcSolido.praArray());

								matrizTmp.setToTranslation(chunk.x << 4, 0, chunk.z << 4);
								chunk.malha.transform(matrizTmp);

								chunk.fazendo = false;
								chunk.att = false;
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
					Biomas.escolher(v);
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
    // API:
    public static Bloco addBloco(String nome, int topo) {
        return addBloco(nome, topo, topo, topo, false, true);
    }

    public static Bloco addBloco(String nome, int topo, int lados) {
        return addBloco(nome, topo, lados, topo, false, true);
    }

    public static Bloco addBloco(String nome, int topo, int lados, int baixo) {
        return addBloco(nome, topo, lados, baixo, false, true);
    }

    public static Bloco addBloco(String nome, int topo, int lados, int baixo, boolean alfa, boolean solido) {
        return addBloco(nome, topo, lados, baixo, alfa, solido, 0);
    }

	public static Bloco addBloco(String nome, int topo, int lados, int baixo, boolean alfa, boolean solido, int luz) {
        Bloco.blocos.add(new Bloco(nome, topo, lados, baixo, alfa, solido, true, luz));
        return Bloco.blocos.get(Bloco.blocos.size()-1);
    }
}
