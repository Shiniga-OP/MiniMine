package com.minimine.mundo;
// java
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.nio.charset.StandardCharsets;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
// utils
import com.minimine.utils.arrays.FloatArrayUtil;
import com.minimine.utils.arrays.ShortArrayUtil;
import com.minimine.utils.arrays.ArrayReuso;
import com.minimine.utils.Mat;
import com.minimine.graficos.Texturas;
import com.minimine.graficos.Render;
import com.minimine.graficos.Animacoes2D;
// libgdx
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.graphics.GL20;
// blocos
import com.minimine.mundo.blocos.Bloco;
import com.minimine.mundo.blocos.BlocoModelo;
// entidades
import com.minimine.entidades.Jogador;
import com.minimine.entidades.Entidade;
import com.minimine.entidades.RegistroCriaturas;
// geração
import com.minimine.mundo.geracao.MotorGeracao;
import com.minimine.mundo.geracao.RegistroBiomas;
import com.minimine.mundo.geracao.ContextoGeracao;
import com.minimine.mundo.geracao.EstruturaPendente;
import com.minimine.utils.MemNativa;
import com.minimine.inventario.ReceitaRegistro;
import com.minimine.utils.TarefasUtil;
import com.minimine.mundo.chunks.Chunk;
import com.minimine.mundo.chunks.ChunkMalha;
import com.minimine.mundo.chunks.ChunkProcesso;

public class Mundo {
    public static String nome = "novo mundo";

    public static final List<Entidade> entidades = new ArrayList<>();
    public static RegistroCriaturas registroCriaturas;

    public static float GRAVIDADE = -30f;

    public static final List<Chunk> praLiberar = new ArrayList<>();
    public static final List<Long> praRemover = new ArrayList<>();

    public static final Map<Long, Chunk> chunks = new ConcurrentHashMap<>();
    public static final Map<Long, Chunk> chunksMod = new ConcurrentHashMap<>();

	// Em Mundo.java
	public static final Chunk[] chunkCache = {
		null, null, null,
		null, null, null,
		null, null, null
	};
	public static final java.util.concurrent.atomic.AtomicInteger proximoCache = 
    new java.util.concurrent.atomic.AtomicInteger(0);
    /*
     * estados:
     *   0 = vazia, sem dados
     *   1 = dados prontos(terreno + vegetação)
     *   11 = transitório: processando estruturas(evita disparo duplo)
     *   2 = estruturas prontas
     *   3 = luz pronta
     *   4 = malha pronta
	 */
    public static final Map<Long, Integer> estados = new ConcurrentHashMap<>();
    /*
     * filaEstrutura: estruturas que extrapolaram os limites de uma chunk durante a
     * geração e precisam ser aplicadas quando a chunk alvo atingir estado 1
     * chave = chunk alvo, valor = lista de blocos pendentes para escrever nela
	 */
    // fila compacta: 5 ints por entrada (lx, ly, lz, id, meta), sem objetos EstruturaPendente
    public static final Map<Long, int[]> filaEstrutura = new ConcurrentHashMap<>();
    // tamanho atual(entradas, não ints) de cada fila, separado do array pra crescimento sem realocar o mapa
    public static final Map<Long, int[]> filaTam = new ConcurrentHashMap<>();
    public static final int FILA_CAMPOS = 5;
    public static final int FILA_CAP_INICIAL = 32; // entradas

    public static final int TAM_CHUNK = 16, Y_CHUNK = 256;
    public static final int CHUNK_AREA = TAM_CHUNK * TAM_CHUNK;
    public static long semente = 0;
    public static int RAIO_CHUNKS = 5;

    public static boolean carregado = false, ciclo = true, nuvens = true;

    public static MotorGeracao motor;
    public static RegistroBiomas registroBiomas;

    public static float contaFluxo = 0f;
    public static final float INTERVALO_FLUXO = 0.5f;

    // buffer nativo reutilizavel pra glGenBuffers, alocado uma vez, usado na thread GL
    public static final java.nio.IntBuffer GL_BUFFER =
	java.nio.ByteBuffer.allocateDirect(12).order(java.nio.ByteOrder.nativeOrder()).asIntBuffer();

    public void iniciar() {
	    semente = semente == 0 ? (System.currentTimeMillis() ^ MathUtils.random(2, 10)) : semente;

	    registroCriaturas = new RegistroCriaturas();
	    registroCriaturas.carregar("criaturas/", 
		    "capivara.json", 
		    "foca.json"
	    );

	    registroBiomas = new RegistroBiomas();
	    registroBiomas.carregarBiomas("biomas/",
		"colinas_deserto.json",
		"costa.json",
		"deserto.json",
		"floresta.json",
		"montanha.json",
		"oceano.json",
		"planicies.json",
		"rio.json",
		"selva.json",
		"taiga.json",
		"tundra.json"
	    );

	    ReceitaRegistro.iniciar();

	    motor = new MotorGeracao(semente, registroBiomas);

	    TarefasUtil.iniciar();
    }

    // chamado em render
    public void att(float delta, Jogador jg) {
        attChunks((int)jg.posicao.x, (int)jg.posicao.z);

        if(!carregado && estados.size() >= 1) {
            Integer est = estados.get(Chave.calcularChave((int)jg.posicao.x >> 4, (int)jg.posicao.z >> 4));
            if(est != null && est == 4) {
				carregado = true;
			}
        }
        if(carregado) {
            GerenciadorEntidades.att(delta, this, jg);

            contaFluxo += delta;
            if(contaFluxo >= INTERVALO_FLUXO) {
                contaFluxo -= INTERVALO_FLUXO;
                for(Chunk chunk : chunks.values()) {
                    if(chunk.fluxoSujo && chunk.dadosProntos) {
                        FluxoAgua.attFluxo(chunk);
                    }
                }
            }
        }
    }

    // libera VBO+IBO de uma chunk da GPU(deve ser chamado na thread GL)
    public static void liberarGpu(Chunk chunk) {
        if(!chunk.gpuPronta) return;
        Gdx.gl.glDeleteBuffer(chunk.vboId);
        Gdx.gl.glDeleteBuffer(chunk.iboId);
        Gdx.gl.glDeleteBuffer(chunk.iboTranspId);
        chunk.vboId = 0;
        chunk.iboId = 0;
        chunk.iboTranspId = 0;
        chunk.gpuPronta = false;
    }

    // chamado em dispose
    public void liberar() {
        for(Chunk chunk : chunks.values()) {
            liberarGpu(chunk);
		}
        for(Entidade e : entidades) e.liberar();
        chunksMod.clear();
        chunks.clear();
        estados.clear();
        filaEstrutura.clear();
        filaTam.clear();
        entidades.clear();
        TarefasUtil.liberar();
        if(com.minimine.ui.UI.debug) Gdx.app.log("ArrayReuso", ArrayReuso.estatisticas());
        ArrayReuso.limparPools();
    }

    // === ACESSO ===
    public static final int obterBlocoMundo(int x, int y, int z) {
        if(y < 0 || y >= Y_CHUNK) return 0;
		final int posX = x >> 4;
		final int posZ = z >> 4;

		final Chunk chunk = obterChunk(posX, posZ);
        if(chunk == null) return Bloco.texIds.get("pedra").tipo;
        return ChunkProcesso.util.obterBloco(x & 0xF, y, z & 0xF, chunk);
    }

    public static void defBlocoMundo(int x, int y, int z, CharSequence bloco) {
		final Bloco b = Bloco.texIds.get(bloco);
		defBlocoMundo(x, y, z, b != null ? b.tipo : 0);
	}
	public static void defBlocoMundo(int x, int y, int z, int bloco) {
        if(y < 0 || y >= Y_CHUNK) return;

        final int chunkX = x >> 4;
        final int chunkZ = z >> 4;
        final long chave = Chave.calcularChave(chunkX, chunkZ);

		final Chunk chunk = obterChunk(chave);

        final int localX = x & 0xF;
        final int localZ = z & 0xF;

        final int blocoAntigoId = ChunkProcesso.util.obterBloco(localX, y, localZ, chunk);

        final boolean eraEmissor = blocoAntigoId != 0 && Bloco.numIds.get(blocoAntigoId).luz > 0;

        if(blocoAntigoId != 0) {
            Render.gp.criar(x, y, z, Texturas.atlas.get(Bloco.numIds.get(blocoAntigoId).lados));
        }
        ChunkProcesso.util.defBloco(localX, y, localZ, bloco, chunk);
        ChunkProcesso.util.defMeta(localX, y, localZ, (short)0, chunk);

        final boolean novoEhEmissor = bloco != 0 && Bloco.numIds.get(bloco) != null && Bloco.numIds.get(bloco).luz > 0;

        if(eraEmissor || novoEhEmissor) ChunkProcesso.luz.recalcularLuz(chunk);

        final boolean novoEhAgua = bloco != 0 && bloco == Bloco.AGUA;
		final boolean antigoEraAgua = FluxoAgua.eAgua(blocoAntigoId);

		if(novoEhAgua) {
			ChunkProcesso.util.defMeta(localX, y, localZ, (short)FluxoAgua.NIVEL_FONTE, chunk);
			chunk.fluxoSujo = true;
		} else if(antigoEraAgua) {
			// bloco que era agua foi removido: recalcula
			FluxoAgua.recalcularFluxo(chunk);
			FluxoAgua.marcarSujo(chunkX, chunkZ);
		} else if(chunk.fluxoSujo) {
			// so verifica vizinhos se ja sabe que tem agua por perto nesse chunk
			if(FluxoAgua.eAgua(obterBlocoMundo(x + 1, y, z)) ||
			   FluxoAgua.eAgua(obterBlocoMundo(x - 1, y, z)) ||
			   FluxoAgua.eAgua(obterBlocoMundo(x, y, z + 1)) ||
			   FluxoAgua.eAgua(obterBlocoMundo(x, y, z - 1)) ||
			   FluxoAgua.eAgua(obterBlocoMundo(x, y + 1, z)) ||
			   FluxoAgua.eAgua(obterBlocoMundo(x, y - 1, z))) {
				FluxoAgua.recalcularFluxo(chunk);
				FluxoAgua.marcarSujo(chunkX, chunkZ);
			}
		}
        // marca luz suja de forma assincrona, o recalculo acontece no ciclo normal
        // do limparChunks, evitando rodar 9x attLuzCompleta na thread GL
        chunk.luzSuja = true;
        chunk.att = true;
        Chunk luzAdj;
        luzAdj = obterChunk(chunkX + 1, chunkZ);
		if(luzAdj != null) {
			luzAdj.luzSuja = true;
			luzAdj.att = true;
		}
        luzAdj = obterChunk(chunkX - 1, chunkZ);
		if(luzAdj != null) {
			luzAdj.luzSuja = true;
			luzAdj.att = true;
		}
        luzAdj = obterChunk(chunkX, chunkZ + 1);
		if(luzAdj != null) {
			luzAdj.luzSuja = true;
			luzAdj.att = true;
		}
        luzAdj = obterChunk(chunkX, chunkZ - 1);
		if(luzAdj != null) {
			luzAdj.luzSuja = true;
			luzAdj.att = true;
		}
        // marca vizinhas de borda pra reconstruir malha
        Chunk chunkAdj;
        if(localX == 0) {
            chunkAdj = obterChunk(chunkX - 1, chunkZ);
            if(chunkAdj != null) chunkAdj.att = true;
        }
        if(localX == TAM_CHUNK - 1) {
            chunkAdj = obterChunk(chunkX + 1, chunkZ);
            if(chunkAdj != null) chunkAdj.att = true;
        }
        if(localZ == 0) {
            chunkAdj = obterChunk(chunkX, chunkZ - 1);
            if(chunkAdj != null) chunkAdj.att = true;
        }
        if(localZ == TAM_CHUNK - 1) {
            chunkAdj = obterChunk(chunkX, chunkZ + 1);
            if(chunkAdj != null) chunkAdj.att = true;
        }
        chunksMod.put(chave, chunk);
    }

    public static void defLuzMundo(int x, int y, int z, byte novaLuz) {
        if(y < 0 || y >= Y_CHUNK) return;
        final Chunk alvo = obterChunk(x >> 4, z >> 4);
        if(alvo != null) {
            final int idc = (x & 0xF) + ((z & 0xF) << 4) + (y << 8);
            alvo.luz[idc] = novaLuz;
            alvo.luzSuja = true;
            alvo.att = true;
        }
    }

    public static byte obterLuzMundo(int x, int y, int z) {
        if(y < 0 || y >= Y_CHUNK) return 0;
        final Chunk chunk = obterChunk(x >> 4, z >> 4);
        if(chunk == null) return 0;
        return chunk.luz[(x & 0xF) + ((z & 0xF) << 4) + (y << 8)];
    }

    public static short obterMetaMundo(int x, int y, int z) {
        if(y < 0 || y >= Y_CHUNK) return 0;
        final Chunk chunk = obterChunk(x >> 4, z >> 4);
        if(chunk == null) return 0;
        return ChunkProcesso.util.obterMeta(x & 0xF, y, z & 0xF, chunk);
    }

    public static void defMetaMundo(int x, int y, int z, short valor) {
        if(y < 0 || y >= Y_CHUNK) return;
        final int chunkX = x >> 4;
        final int chunkZ = z >> 4;
        final Chunk chunk = obterChunk(chunkX, chunkZ);
        if(chunk == null) return;
        int localX = x & 0xF;
        int localZ = z & 0xF;
        ChunkProcesso.util.defMeta(localX, y, localZ, valor, chunk);
        chunk.att = true;
		Chunk adj = null;
        if(localX == 0) {
            adj = obterChunk(chunkX - 1, chunkZ);
            if(adj != null) adj.att = true;
        }
        if(localX == TAM_CHUNK - 1) {
            adj = obterChunk(chunkX + 1, chunkZ);
            if(adj != null) adj.att = true;
        }
        if(localZ == 0) {
            adj = obterChunk(chunkX, chunkZ - 1);
            if(adj != null) adj.att = true;
        }
        if(localZ == TAM_CHUNK - 1) {
            adj = obterChunk(chunkX, chunkZ + 1);
            if(adj != null) adj.att = true;
        }
    }

    public static int obterAlturaChao(int x, int z) {
        for(int y = Y_CHUNK - 1; y > 0; y--) {
            if(obterBlocoMundo(x, y, z) != 0) return y + 1;
        }
        return 80;
    }

	public static final Chunk obterChunk(final long chave) {
		final Chunk[] cache = chunkCache; // bota no registrador ao inves de chamar static

		for(int i = 0; i < 9; i++) {
			if(cache[i] != null && cache[i].chave == chave) {
				return cache[i];
			}
		}
		final Chunk chunk = chunks.get(chave);

		final int slot = proximoCache.getAndIncrement() & 8; 
		cache[slot] = chunk;

		return chunk;
	}

	public static final Chunk obterChunk(int x, int z) {
		return obterChunk(Chave.calcularChave(x, z));
	}

    // === GERAÇÃO DE CHUNKS ===
    public void attChunks(int x, int z) {
        final int cx = x >> 4;
        final int cz = z >> 4;
        limparChunks(cx, cz);

        for(int dx = -RAIO_CHUNKS; dx <= RAIO_CHUNKS; dx++) {
            for(int dz = -RAIO_CHUNKS; dz <= RAIO_CHUNKS; dz++) {
                tentarGerarChunk(cx + dx, cz + dz);
            }
        }
    }
	/*
	NOTA:
	NÃO MEXA NISSO, BFS É MULTIFRAME, SE VOCÊ MEXER NISSO
	PRA FAZER EM 1 LOOP SO VOCÊ VAI QUEBRAR TUDO E MATAR 
	TODO MUNDO E FICAR UM MÊS ACHANDO QUE ERA ChunkLuz.java
	*/
    public static void limparChunks(int chunkX, int chunkZ) {
		praLiberar.clear();
		praRemover.clear();

		for(Map.Entry<Long, Chunk> e : chunks.entrySet()) {
			final long chave = e.getKey();
			final int distX = Mat.abs(Chave.x(chave) - chunkX);
			final int distZ = Mat.abs(Chave.z(chave) - chunkZ);
			final Chunk chunk = e.getValue();
			final int estado = estados.getOrDefault(chave, 0);

			if(distX > RAIO_CHUNKS || distZ > RAIO_CHUNKS) {
				if(!chunksMod.containsKey(chave)) praLiberar.add(chunk);
				if(chunk != null && chunk.gpuPronta) praLiberar.add(chunk);
				praRemover.add(chave);
			} else if(estado == 1 && vizinhosComDados(chunk.x, chunk.z)) {
				processarEstruturas(chave);
			} else if(estado == 2 && vizinhosComEstruturas(chunk.x, chunk.z)) {
				calcularLuz(chave);
			}
		}
		// propaga luz em passes até estabilizar(max 16 = alcance maximo de luz)
		for(int passe = 0; passe < 16; passe++) {
			boolean algumaSuja = false;
			for(Map.Entry<Long, Chunk> e : chunks.entrySet()) {
				final long chave = e.getKey();
				final Chunk chunk = e.getValue();
				final int estado = estados.getOrDefault(chave, 0);
				if(chunk.att && !chunk.fazendo && estado >= 3) {
					ChunkProcesso.luz.attLuz(chunk);
					algumaSuja = true;
				}
			}
			if(!algumaSuja) break;
		}
		// gera malha so depois que a luz estabilizou
		for(Map.Entry<Long, Chunk> e : chunks.entrySet()) {
			final long chave = e.getKey();
			final Chunk chunk = e.getValue();
			final int estado = estados.getOrDefault(chave, 0);
			if(chunk.att && !chunk.fazendo && estado >= 3) {
				if(vizinhosProntos(chunk.x, chunk.z)) gerarMalha(chave);
			}
		}
		if(!praLiberar.isEmpty() || !praRemover.isEmpty()) {
			for(Chunk c : praLiberar) liberarGpu(c);
			for(long chave : praRemover) {
				chunks.remove(chave);
				filaEstrutura.remove(chave);
				filaTam.remove(chave);
			}
		}
	}

    public void tentarGerarChunk(int x, int z) {
        final long chave = Chave.calcularChave(x, z);

        if(chunks.containsKey(chave)) {
            final int estado = estados.getOrDefault(chave, 0);
            if(estado == 1 && vizinhosComDados(x, z)) {
                processarEstruturas(chave);
            } else if(estado == 2 && vizinhosComEstruturas(x, z)) {
                calcularLuz(chave);
            } else if(estado == 3 && !obterChunk(chave).fazendo) {
                if(vizinhosProntos(x, z)) gerarMalha(chave);
            }
            return;
        }
        // chunk modificada pelo jogador: recarrega com luz recalculada
        final Chunk modificado = chunksMod.get(chave);
        if(modificado != null) {
            chunks.put(chave, modificado);
            ChunkProcesso.luz.calcularLuz(modificado);
            estados.put(chave, 3); // dados + estruturas + luz prontos
            return;
        }
        // nova chunk: gera do zero
        final Chunk novo = new Chunk();
        novo.x = x;
		novo.z = z;
		novo.chave = Chave.calcularChave(x, z);
        novo.meta = new short[Mundo.TAM_CHUNK * Mundo.Y_CHUNK * Mundo.TAM_CHUNK];
        ChunkProcesso.util.compactar(ChunkProcesso.util.bitsPraMaxId(novo.maxIds), novo);
        chunks.put(chave, novo);
        estados.put(chave, 0);
        gerarDados(chave);
    }

    // === VERIFICAÇÕES DE ESTADO DE VIZINHOS ===
    // 4 vizinhos cardinais com estado >= 1(dados prontos)
    public static boolean vizinhosComDados(int cx, int cz) {
        return estados.getOrDefault(Chave.calcularChave(cx + 1, cz), 0) >= 1 &&
			estados.getOrDefault(Chave.calcularChave(cx - 1, cz), 0) >= 1 &&
			estados.getOrDefault(Chave.calcularChave(cx, cz + 1), 0) >= 1 &&
			estados.getOrDefault(Chave.calcularChave(cx, cz - 1), 0) >= 1;
    }

    // 4 vizinhos cardinais com estado >= 2(estruturas prontas)
    public static boolean vizinhosComEstruturas(int cx, int cz) {
        return estados.getOrDefault(Chave.calcularChave(cx + 1, cz), 0) >= 2 &&
			estados.getOrDefault(Chave.calcularChave(cx - 1, cz), 0) >= 2 &&
			estados.getOrDefault(Chave.calcularChave(cx, cz + 1), 0) >= 2 &&
			estados.getOrDefault(Chave.calcularChave(cx, cz - 1), 0) >= 2;
    }

    // 4 vizinhos cardinais com estado >= 3(luz pronta) necessario pra malha correta
    public static boolean vizinhosProntos(int cx, int cz) {
        return estados.getOrDefault(Chave.calcularChave(cx + 1, cz), 0) >= 3 &&
			estados.getOrDefault(Chave.calcularChave(cx - 1, cz), 0) >= 3 &&
			estados.getOrDefault(Chave.calcularChave(cx, cz + 1), 0) >= 3 &&
			estados.getOrDefault(Chave.calcularChave(cx, cz - 1), 0) >= 3;
    }

    // === GERAÇÃO ===
    // estado 0 -> 1: gera terreno + vegetação
    public static void gerarDados(final long chave) {
        final Chunk chunk = obterChunk(chave);

        TarefasUtil.addGeração(new Runnable() {
				@Override
				public void run() {
					try {
						motor.gerarChunk(chunk);
						chunk.dadosProntos = true;
						estados.put(chave, 1);
					} catch(final Exception e) {
						throw new RuntimeException("[Mundo] erro ao gerar dados: " + e);
					}
				}
			});
    }
    /*
     * estado 1 -> 2: processa estruturas da chunk e aplica fila de pendentes recebida

     * regra de escrita de estruturas:
     *   vizinha em estado 1 -> escreve diretamente(ainda na fase de dados)
     *   vizinha em estado 0 ou >= 2 -> enfileira em filaEstrutura[vizinha]

     * transitorio 11 evita disparo duplo da mesma chunk por duas threads
	 */
    public static void processarEstruturas(final long chave) {
        final Chunk chunk = obterChunk(chave);
        if(chunk == null) return;
        if(!estados.replace(chave, 1, 11)) return; // 11 = transitorio

        TarefasUtil.addGeração(new Runnable() {
				@Override
				public void run() {
					try {
						final int chunkX = chunk.x << 4;
						final int chunkZ = chunk.z << 4;
						final ContextoGeracao ctx = motor.ctxLocal.get();

						// reconstroi biomas para as estruturas
						motor.calcular2D(motor.semCalor, motor.espalharCalor, motor.octCalor, motor.perCalor, 2.0f, chunkX, chunkZ, ctx.calorMapa);
						motor.calcular2D(motor.semUmidade, motor.espalharUmidade, motor.octUmidade, motor.perUmidade, 2.0f, chunkX, chunkZ, ctx.umidadeMapa);
						for(int i = 0; i < 256; i++) {
							ctx.calorMapa[i] = Math.max(0f, Math.min(1f, ctx.calorMapa[i]   * 0.5f + 0.5f));
							ctx.umidadeMapa[i] = Math.max(0f, Math.min(1f, ctx.umidadeMapa[i] * 0.5f + 0.5f));
						}
						for(int z = 0; z < 16; z++) {
							for(int x = 0; x < 16; x++) {
								final int idc = (z << 4) + x;
								int topo = Y_CHUNK - 1;
								while(topo > 0 && ChunkProcesso.util.obterBloco(x, topo, z, chunk) == 0) topo--;
								ctx.topoMapa[idc] = topo;
								float calor = ctx.calorMapa[idc];
								final float umidade = ctx.umidadeMapa[idc];
								calor = Math.max(0f, Math.min(1f, calor - (float)((topo - MotorGeracao.NIVEL_MAR) * 0.004)));
								ctx.biomaMapa[idc] = registroBiomas.selecionar(calor, umidade, topo);
							}
						}
						// 1. aplica estruturas pendentes que outras chunks enfileiraram pra esta
						final int[] pendentes;
						final int[] tamArr;
						synchronized(filaEstrutura) {
							pendentes = filaEstrutura.remove(chave);
							tamArr = filaTam.remove(chave);
						}
						if(pendentes != null && tamArr != null) {
							final int total = tamArr[0];
							for(int i = 0; i < total; i++) {
								final int base = i * FILA_CAMPOS;
								final int lx = pendentes[base];
								final int ly = pendentes[base + 1];
								final int lz = pendentes[base + 2];
								final int id = pendentes[base + 3];
								final short meta = (short)pendentes[base + 4];
								if(ly >= 0 && ly < 256) {
									ChunkProcesso.util.defBloco(lx, ly, lz, id, chunk);
									if(meta != 0) ChunkProcesso.util.defMeta(lx, ly, lz, meta, chunk);
								}
							}
						}
						// 2. gera as estruturas desta chunk(vegetação ja foi feita em gerarDados)
						motor.colocarEstruturas(chunk, chunkX, chunkZ, ctx);

						estados.put(chave, 2);
					} catch(final Exception e) {
						throw new RuntimeException("[Mundo] erro ao processar estruturas: " + e);
					}
				}
			});
    }
    /*
     * estado 2 -> 3: calcula luz
     * exige vizinhosComEstruturas para que a luz não vaze em buracos de estruturas
     * que ainda não chegaram nas vizinhas
	 */
    public static void calcularLuz(final long chave) {
        final Chunk chunk = obterChunk(chave);
        if(chunk == null) return;
        if(!estados.replace(chave, 2, 12)) return; // 12 = transitorio

        TarefasUtil.addGeração(new Runnable() {
				@Override
				public void run() {
					try {
						ChunkProcesso.luz.calcularLuz(chunk);
						estados.put(chave, 3);
					} catch(final Exception e) {
						throw new RuntimeException("[Mundo] erro ao calcular luz: " + e);
					}
				}
			});
    }
    // estado 3 -> 4: gera malha na thread do executor, envia para GPU na thread GL
    public static void gerarMalha(final long chave) {
        final Chunk chunk = obterChunk(chave);
        if(chunk == null) return;
        chunk.fazendo = true;

        TarefasUtil.addMalha(new Runnable() {
				@Override
				public void run() {
					final FloatArrayUtil vertsGeral = ArrayReuso.obterFloatArray();
					final ShortArrayUtil idcSolidos = ArrayReuso.obterShortArray();
					final ShortArrayUtil idcTransp = ArrayReuso.obterShortArray();

					ChunkProcesso.malha.attMalha(chunk, vertsGeral, idcSolidos, idcTransp);

					Gdx.app.postRunnable(new Runnable() {
							@Override
							public void run() {
								try {
									liberarGpu(chunk);

									GL_BUFFER.clear();
									Gdx.gl20.glGenBuffers(3, GL_BUFFER);
									chunk.vboId = GL_BUFFER.get(0);
									chunk.iboId = GL_BUFFER.get(1);
									chunk.iboTranspId = GL_BUFFER.get(2);

									Gdx.gl.glBindBuffer(GL20.GL_ARRAY_BUFFER, chunk.vboId);
									Gdx.gl.glBufferData(GL20.GL_ARRAY_BUFFER, vertsGeral.tam * 4, vertsGeral.bufPronto(), GL20.GL_STATIC_DRAW);

									Gdx.gl.glBindBuffer(GL20.GL_ELEMENT_ARRAY_BUFFER, chunk.iboId);
									Gdx.gl.glBufferData(GL20.GL_ELEMENT_ARRAY_BUFFER, idcSolidos.tam * 2, idcSolidos.bufPronto(), GL20.GL_STATIC_DRAW);

									Gdx.gl.glBindBuffer(GL20.GL_ELEMENT_ARRAY_BUFFER, chunk.iboTranspId);
									Gdx.gl.glBufferData(GL20.GL_ELEMENT_ARRAY_BUFFER, idcTransp.tam * 2, idcTransp.bufPronto(), GL20.GL_STATIC_DRAW);

									Gdx.gl.glBindBuffer(GL20.GL_ARRAY_BUFFER, 0);
									Gdx.gl.glBindBuffer(GL20.GL_ELEMENT_ARRAY_BUFFER, 0);

									chunk.gpuPronta = true;
									chunk.contaSolida = idcSolidos.tam;
									chunk.contaTransp = idcTransp.tam;
									chunk.fazendo = false;
									chunk.att = false;
									estados.put(chave, 4);
								} catch(final Exception e) {
									Gdx.app.error("Mundo", "erro ao gerar malha", e);
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
    // === FILA DE ESTRUTURAS PENDENTES ===
    /*
     * enfileira ou aplica imediatamente um bloco de estrutura para a chunk alvo
     * chamado por MotorGeracao.colocarEstruturas quando a vizinha não ta em estado 1

     * se a chunk alvo ainda não chegou ao estado 2: enfileira para aplicar em processarEstruturas
     * se a chunk alvo ja passou do estado 1(>= 2): aplica imediatamente e marca para
     *   recalcular luz e malha: o bloco chegou atrasado mas ainda pode ser corrigido
	 */
    public static void enfileirarEstrutura(long chaveAlvo, EstruturaPendente pendente) {
        final int estadoAlvo = estados.getOrDefault(chaveAlvo, 0);
        if(estadoAlvo >= 2) {
            // chunk alvo ja passou de processarEstruturas: aplica agora e marca suja
            final Chunk alvo = obterChunk(chaveAlvo);
            if(alvo != null) {
                pendente.aplicar(alvo);
                alvo.luzSuja = true;
                alvo.att = true;
            }
            return;
        }
        // chunk alvo ainda não processou estruturas: enfileira no buffer compacto
        synchronized(filaEstrutura) {
			filaEstrutura.putIfAbsent(chaveAlvo, new int[FILA_CAP_INICIAL * FILA_CAMPOS]);
			filaTam.putIfAbsent(chaveAlvo, new int[]{0});
			int[] buf = filaEstrutura.get(chaveAlvo);
			final int[] tam = filaTam.get(chaveAlvo);

            final int n = tam[0];
            if(n * FILA_CAMPOS >= buf.length) {
                // cresce 1.5x
                final int novasEntradas = (buf.length / FILA_CAMPOS) + (buf.length / FILA_CAMPOS >> 1);
				final int[] novo = new int[novasEntradas * FILA_CAMPOS];
                System.arraycopy(buf, 0, novo, 0, buf.length);
                buf = novo;
                filaEstrutura.put(chaveAlvo, buf);
            }
            final int base = n * FILA_CAMPOS;
            buf[base] = pendente.lx;
            buf[base + 1] = pendente.ly;
            buf[base + 2] = pendente.lz;
            buf[base + 3] = pendente.id;
            buf[base + 4] = pendente.meta;
            tam[0]++;
        }
    }
}


