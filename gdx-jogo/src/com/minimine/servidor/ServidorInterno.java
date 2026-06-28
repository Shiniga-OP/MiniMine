package com.minimine.servidor;

import com.badlogic.gdx.Gdx;
import com.minimine.mundo.Mundo;
import com.minimine.entidades.Jogador;
import com.minimine.utils.ArquivosUtil;
import com.minimine.cenas.Jogo;
import java.util.List;
import com.minimine.Inicio;
import java.util.Map;
import java.util.HashMap;
import com.minimine.mundo.chunks.Chunk;
import com.minimine.mundo.chunks.ChunkProcesso;
import com.minimine.mundo.Chave;
import com.badlogic.gdx.math.Vector3;
import com.minimine.entidades.ItemMundo;
import com.minimine.mundo.blocos.Bloco;
import com.minimine.utils.DiaNoiteUtil;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.URLEncoder;
import com.minimine.entidades.Entidade;
import com.minimine.entidades.ItemMundo;
import java.util.Iterator;
import com.badlogic.gdx.math.MathUtils;
import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;
import com.badlogic.gdx.graphics.profiling.GLProfiler;
import com.minimine.ui.UI;
import com.minimine.Logs;
import com.minimine.utils.TarefasUtil;
import com.minimine.entidades.GerenciadorEntidades;
import com.minimine.mundo.fluidos.FluxoFluido;
/*
 * em solo, sobe um Net(SERVIDOR_MODO) local e conecta o cliente
 * via Net(CLIENTE_MODO, "127.0.0.1")
 * o Jogo passa a usar sempre o
 * caminho de rede, eliminando a bifurcação solo/multi

 * o servidor interno é o unico responsavel por:
 *   - carregar e salvar o mundo(ArquivosUtil.crMundo/svMundo)
 *   - ser a fonte de verdade do Mundo(estado, chunks, blocos)

 * O cliente local recebe tudo via protocolo, igual a um cliente remoto
 */
public class ServidorInterno {
	public Net netServidor = null;
	public Net netCliente = null;
	public boolean rodando = false;
	public List<Jogador> jogadores;
	public final Map<Integer, Jogador> jogadoresRede = new HashMap<>();
	public Mundo mundo;
	public Thread threadTick;
	public int tick = 0;
	public static final long MS_POR_TICK = 50; // 20 TPS
	public static float delta;
	public final Map<Long, Chunk> chunksMod = new ConcurrentHashMap<>();
	public final List<TarefaTick> tarefasLoop = new ArrayList<>();
	public Jogador jgUi;

	// buffers reutilizaveis para broadcast de posição no tick
	public final ByteArrayOutputStream baosPosicao = new ByteArrayOutputStream(64);
	public final DataOutputStream dosPosicao = new DataOutputStream(baosPosicao);

	public Runnable attMundo = new Runnable() {
		@Override
		public void run() {
			mundo.att(jgUi);
		}
	};
	public Runnable attEntidade = new Runnable() {
		@Override
		public void run() {
			if(mundo.carregado) GerenciadorEntidades.att(delta, mundo, jogadores);
		}
	};
	public Runnable attFluxo = new Runnable() {
		@Override
		public void run() {
			FluxoFluido.tick(tick);
		}
	};

	public void iniciar(final Mundo mundo, final List<Jogador> jogadores) {
		if(rodando) return;
		rodando = true;

		this.jogadores = jogadores;

		jgUi = jogadores.get(0);

		if(ArquivosUtil.existe(Inicio.externo + "/MiniMine/mundos/" + URLEncoder.encode(mundo.nome) + ".mini")) {
			ArquivosUtil.crMundo(mundo, jogadores.isEmpty() ? null : jogadores.get(0));
		} else {
			Gdx.app.log("[Servidor]", "mundo "+mundo.nome+" não encontrado, criando novo");
		}
		netServidor = new Net(Net.SERVIDOR_MODO);
		netServidor.ouvinte = new Net.OuvintePacote() {
			public void aoReceber(byte tipo, DataInputStream dados) throws IOException {
				// servidor so repassa via broadcast, não processa aqui
			}
		};
		netServidor.ouvinteConexao = new Net.OuvinteConexao() {
			public void aoConectar(final Net.Cliente cliente) {
				new Thread(new Runnable() {
						public void run() {
							try {
								// envia estado do mundo
								byte[] pacoteMundo = montarMundo(mundo);
								synchronized(cliente) {
									cliente.saida.write(pacoteMundo);
									cliente.saida.flush();
								}
								// envia cada chunk modificada
								synchronized(mundo.chunksMod) {
									for(Chunk chunk : mundo.chunksMod.values()) {
										byte[] pacote = montarChunk(chunk);
										synchronized(cliente) {
											cliente.saida.write(pacote);
											cliente.saida.flush();
										}
									}
								}
								// sinaliza fim do mundo
								synchronized(cliente) {
									cliente.saida.writeByte(Net.PACOTE_MUNDO_FIM);
									cliente.saida.flush();
								}
							} catch(Exception e) {
								Gdx.app.error("[ServidorInterno]", "Erro ao enviar mundo para cliente " + cliente.id + ": " + e.getMessage());
							}
						}
					}).start();
			}
		};
		int tentativas = 0;
		while(netServidor.servidorSocket == null && tentativas < 30) {
			try {
				Thread.sleep(100);
			} catch(InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			tentativas++;
		}
		if(netServidor.servidorSocket == null) {
			Gdx.app.error("[ServidorInterno]", "Saida ao aguardar socket do servidor interno.");
		} else {
			Gdx.app.log("[ServidorInterno]", "Servidor interno pronto.");
		}
		this.mundo = mundo;
		threadTick = new Thread(new Runnable() {
				public void run() {
					int numTick = 0;
					while(rodando) {
						final long inicio = System.currentTimeMillis();
						tick(numTick++);
						final long gasto = System.currentTimeMillis() - inicio;
						final long espera = MS_POR_TICK - gasto;
						if(espera > 0) {
							try {
								Thread.sleep(espera);
							} catch(InterruptedException e) {
								Thread.currentThread().interrupt();
							}
						}
					}
				}
			});
		threadTick.setName("servidor-tick");
		threadTick.setDaemon(true);
		threadTick.start();
	}

	// chamado 20x por segundo pela threadTick
	public void tick(int numTick) {
		if(mundo == null) return;
		tick = numTick;
		delta = MS_POR_TICK / 1000f;

		// ciclo dia/noite: DiaNoiteUtil não tem thread propria, avança aqui
		if(mundo.diaNoite != null && mundo.ciclo) {
			mundo.diaNoite.tempo += mundo.diaNoite.tempo_velo * delta;
			if(mundo.diaNoite.tempo > MathUtils.PI2)
				mundo.diaNoite.tempo -= MathUtils.PI2;
		}
		TarefasUtil.mundo.execute(attMundo);
		if(mundo.carregado) {
			TarefasUtil.entidades.execute(attEntidade);
			TarefasUtil.fisica.execute(attFluxo);
		}
		for(int i = 0; i < jogadores.size(); i++) {
			final Jogador jg = jogadores.get(i);
			if(mundo.carregado) {
				if(!jg.nasceu) {
					final int yTeste = Mundo.obterAlturaChao((int)jg.posicao.x, (int)jg.posicao.z);
					if(yTeste > 1) {
						jg.posicao.y = yTeste;
						jg.nasceu = true;
						final long chave = Chave.gerar(0, 0);
						final Chunk chunk = mundo.obterChunk(chave);
						if(chunk != null) mundo.chunksMod.put(chave, chunk);
						Gdx.app.log("[Jogo]", "jogador nasceu a "+yTeste+" blocos de altura");
					} else Gdx.app.log("[Jogo]", "não nasceu, altura recebida: "+yTeste);
				}
				jg.att(delta);
			}
		}
		final com.minimine.inventario.Item itemInv = jgUi.inv.itens[jgUi.inv.slotSelecionado];
		if(itemInv != null && itemInv.nome != jgUi.item) jgUi.item = itemInv.nome;
		else if(itemInv == null) jgUi.item = "ar";
		// broadcast de posição dos jogadores de rede para o cliente local
		if(netServidor != null) {
			for(Map.Entry<Integer, Jogador> e : jogadoresRede.entrySet()) {
				final Jogador jg = e.getValue();
				try {
					baosPosicao.reset();
					dosPosicao.writeByte(Net.PACOTE_POS);
					dosPosicao.writeInt(e.getKey());
					dosPosicao.writeFloat(jg.posicao.x);
					dosPosicao.writeFloat(jg.posicao.y);
					dosPosicao.writeFloat(jg.posicao.z);
					dosPosicao.writeFloat(jg.yaw);
					dosPosicao.writeFloat(jg.tom);
					int marcas = (jg.frente ? 1 : 0) | (jg.tras ? 2 : 0)
						| (jg.esquerda ? 4 : 0) | (jg.direita ? 8 : 0)
						| (jg.voando ? 16 : 0) | (jg.agachado ? 32 : 0);
					dosPosicao.writeInt(marcas);
					Net.escreverUTF(dosPosicao, jg.item);
					dosPosicao.flush();
					netServidor.broadcastTodos(baosPosicao.toByteArray());
				} catch(IOException ex) {
					Gdx.app.error("[ServidorInterno]", "Erro no broadcast de posição: " + ex.getMessage());
				}
			}
		}
		// tarefas externas registradas
		for(int i = 0; i < tarefasLoop.size(); i++) tarefasLoop.get(i).executar(numTick);
	}

	// processa pacotes recebidos pelo cliente local
	public void processarPacote(byte tipo, DataInputStream dis) throws IOException {
		switch(tipo) {
			case Net.PACOTE_POS: {
					int id = dis.readInt();
					if(netCliente == null || id == netCliente.idLocal) break;
					float x = dis.readFloat(); float y = dis.readFloat(); float z = dis.readFloat();
					float yaw = dis.readFloat(); float tom = dis.readFloat();
					int marcas = dis.readInt();
					String item = Net.lerUTF(dis);
					Jogador jgRede = jogadoresRede.get(id);
					if(jgRede == null) break;
					jgRede.posicao.set(x, y, z);
					jgRede.yaw = yaw;
					jgRede.tom = tom;
					jgRede.camera.direction.set(0, 0, -1);
					jgRede.camera.rotate(Vector3.Y, yaw);
					jgRede.frente = (marcas & 1)  != 0;
					jgRede.tras = (marcas & 2)  != 0;
					jgRede.esquerda= (marcas & 4)  != 0;
					jgRede.direita = (marcas & 8)  != 0;
					jgRede.voando  = (marcas & 16) != 0;
					jgRede.agachado= (marcas & 32) != 0;
					jgRede.item = item;
					break;
				}
			case Net.PACOTE_MUNDO: {
					mundo.diaNoite.tempo = dis.readFloat();
					mundo.nome = Net.lerUTF(dis);
					mundo.semente = dis.readLong();
					mundo.plano = dis.readByte() == 1;
					break;
				}
			case Net.PACOTE_CHUNK: {
					lerChunk(dis);
					break;
				}
			case Net.PACOTE_MUNDO_FIM: {
					mundo.chunksMod = chunksMod;
					mundo.chunks = new com.minimine.mundo.chunks.GradeChunk(Mundo.RAIO_CHUNKS);
					mundo.iniciar(true);
					break;
				}
			case Net.PACOTE_BLOCO: {
                    int x = dis.readInt();
                    int y = dis.readInt();
                    int z = dis.readInt();
                    int id = dis.readInt();
                    String item = Net.lerUTF(dis);
                    mundo.defBlocoMundo(x, y, z, id);

                    // ACORDAR OS FLUIDOS ADJACENTES (IGUAL AO LUANTI)
                    com.minimine.mundo.fluidos.FluxoFluido.notificarVizinhos(x, y, z);

                    if(!item.equals("ar")) {
                        mundo.entidades.add(new ItemMundo(item, 1, x + 0.5f, y + 0.5f, z + 0.5f));
                    }
                    break;
                }
			case Net.PACOTE_ENTROU: {
					int id = dis.readInt();
					String identidade = Net.lerUTF(dis);
					String nome = Net.lerUTF(dis);
					if(netCliente == null || id == netCliente.idLocal) break;
					if(!jogadoresRede.containsKey(id)) {
						Jogador jgRede = new Jogador(identidade);
						jgRede.modo = Jogo.modo;
						jgRede.pessoa = 3;
						jgRede.nome = nome;
						jgRede.attModelo();
						jogadoresRede.put(id, jgRede);
						jogadores.add(jgRede);
						Gdx.app.log("[Jogo]", "jogador " + nome + " entrou, id: " + id);
					}
					break;
				}
			case Net.PACOTE_SAIU: {
					int id = dis.readInt();
					Jogador jgRede = jogadoresRede.remove(id);
					if(jgRede != null) {
						jogadores.remove(jgRede);
						Gdx.app.log("[Jogo]", "jogador " + jgRede.nome + " saiu");
					}
					break;
				}
		}
	}

	public void enviarPosicao(float x, float y, float z, float yaw, float tom, String item) {
		if(!netCliente.conectado || netCliente.clienteSaida == null) return;
		try {
			Jogador jg = jgUi;
			int marcas = (jg.frente ? 1 : 0) | (jg.tras ? 2 : 0) | (jg.esquerda ? 4 : 0)
				| (jg.direita ? 8 : 0) | (jg.voando ? 16 : 0) | (jg.agachado ? 32 : 0);
			synchronized(netCliente.clienteSaida) {
				netCliente.clienteSaida.writeByte(Net.PACOTE_POS);
				netCliente.clienteSaida.writeInt(netCliente.idLocal);
				netCliente.clienteSaida.writeFloat(x);
				netCliente.clienteSaida.writeFloat(y);
				netCliente.clienteSaida.writeFloat(z);
				netCliente.clienteSaida.writeFloat(yaw);
				netCliente.clienteSaida.writeFloat(tom);
				netCliente.clienteSaida.writeInt(marcas);
				Net.escreverUTF(netCliente.clienteSaida, item);
				netCliente.clienteSaida.flush();
			}
		} catch(IOException e) {
			Gdx.app.error("[Servidor]", "Erro ao enviar posição: " + e.getMessage());
		}
	}

	public void enviarBloco(int x, int y, int z, int id, String item) {
		if(!netCliente.conectado || netCliente.clienteSaida == null) return;
		try {
			synchronized(netCliente.clienteSaida) {
				netCliente.clienteSaida.writeByte(Net.PACOTE_BLOCO);
				netCliente.clienteSaida.writeInt(x);
				netCliente.clienteSaida.writeInt(y);
				netCliente.clienteSaida.writeInt(z);
				netCliente.clienteSaida.writeInt(id);
				Net.escreverUTF(netCliente.clienteSaida, item);
				netCliente.clienteSaida.flush();
			}
		} catch(IOException e) {
			Gdx.app.error("[Servidor]", "Erro ao enviar bloco: " + e.getMessage());
		}
	}

	public void enviarEntrou(int idLocal, String identidade, String nome) {
		if(!netCliente.conectado || netCliente.clienteSaida == null) return;
		try {
			synchronized(netCliente.clienteSaida) {
				netCliente.clienteSaida.writeByte(Net.PACOTE_ENTROU);
				netCliente.clienteSaida.writeInt(idLocal);
				Net.escreverUTF(netCliente.clienteSaida, identidade);
				Net.escreverUTF(netCliente.clienteSaida, nome);
				netCliente.clienteSaida.flush();
			}
		} catch(IOException e) {
			Gdx.app.error("[Servidor]", "Erro ao enviar entrou: " + e.getMessage());
		}
	}

	// monta PACOTE_MUNDO como byte[]
	public static byte[] montarMundo(Mundo mundo) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream(64);
		DataOutputStream dos = new DataOutputStream(baos);
		dos.writeByte(Net.PACOTE_MUNDO);
		dos.writeFloat(mundo.diaNoite.tempo);
		Net.escreverUTF(dos, mundo.nome);
		dos.writeLong(mundo.semente);
		dos.writeByte(mundo.plano ? 1 : 0);
		dos.flush();
		return baos.toByteArray();
	}

	// monta PACOTE_CHUNK como byte[]
	public static byte[] montarChunk(Chunk chunk) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream(65536);
		DataOutputStream dos = new DataOutputStream(baos);
		dos.writeByte(Net.PACOTE_CHUNK);
		Mundo.salvarChunk(chunk, dos);
		dos.flush();
		return baos.toByteArray();
	}

	// le PACOTE_CHUNK do stream e insere em chunksMod
	public void lerChunk(DataInputStream dis) throws IOException {
		final Chunk chunk = Mundo.carregarChunk(dis);
		chunk.dadosProntos = true;
		chunk.att = true;
		synchronized(chunksMod) {
			chunksMod.put(chunk.chave, chunk);
		}
	}

	public void parar() {
		if(!rodando) return;
		rodando = false;

		if(netServidor != null) {
			try {
				ArquivosUtil.svMundo(mundo, jogadores);
			} catch(Throwable t) {
				Gdx.app.error("[Servidor]", "Erro ao salvar mundo no encerramento: " + t.getMessage());
			}
			netServidor.liberar();
			netServidor = null;
		}
		if(netCliente != null) {
			netCliente.liberar();
			netCliente = null;
		}
		chunksMod.clear();
		jogadoresRede.clear();
		if(threadTick != null) {
			threadTick.interrupt();
			threadTick = null;
		}
		for(Jogador jg : jogadores) jg.liberar();
		mundo.liberar();
		Gdx.app.log("[Servidor]", "Servidor interno encerrado.");
	}
}
