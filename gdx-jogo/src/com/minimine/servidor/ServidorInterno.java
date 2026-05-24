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
import java.util.Timer;
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
/*
 * em solo, sobe um Net(SERVIDOR_MODO) local e conecta o cliente
 * via Net(CLIENTE_MODO, "127.0.0.1")
 * o Jogo passa a usar sempre o
 * caminho de rede, eliminando a bifurcação solo/multi

 * o servidor interno é o unico responsavel por:
 *   - carregar e salvar o mundo(ArquivosUtil.crMundo/svMundo)
 *   - ser a fonte de verdade do Mundo(estado, chunks, blocos)

 * O cliente local recebe tudo via protocolo, igual a um cliente remoto
 * Protocolo inteiramente binário: sem String, sem CSV, sem split
 */
public class ServidorInterno {
	public Net netServidor = null;
	public Net netCliente = null;
	public boolean rodando = false;
	public Map<Integer, Jogador> jogadoresRede = new HashMap<Integer, Jogador>();
	public Mundo mundo;
	public Timer relogio;
	public Map<Long, Chunk> chunksMod = new HashMap<Long, Chunk>();

	public void iniciar(final Mundo mundo, final List<Jogador> jogadores) {
		if(rodando) return;
		rodando = true;

		if(ArquivosUtil.existe(Inicio.externo + "/MiniMine/mundos/" + URLEncoder.encode(mundo.nome) + ".mini")) {
			ArquivosUtil.crMundo(mundo, jogadores.isEmpty() ? null : jogadores.get(0));
		} else {
			Gdx.app.log("[Servidor]", "mundo "+mundo.nome+" não encontrado, criando novo");
		}
		mundo.diaNoite = new DiaNoiteUtil();
		if(mundo.ciclo) mundo.diaNoite.iniciar();

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
								byte[] pktMundo = montarMundo(mundo);
								synchronized(cliente) {
									cliente.saida.write(pktMundo);
									cliente.saida.flush();
								}
								// envia cada chunk modificada
								synchronized(mundo.chunksMod) {
									for(Chunk chunk : mundo.chunksMod.values()) {
										byte[] pkt = montarChunk(chunk);
										synchronized(cliente) {
											cliente.saida.write(pkt);
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
			try { Thread.sleep(100); } catch(InterruptedException e) { Thread.currentThread().interrupt(); }
			tentativas++;
		}
		if(netServidor.servidorSocket == null) {
			Gdx.app.error("[ServidorInterno]", "Timeout ao aguardar socket do servidor interno.");
		} else {
			Gdx.app.log("[ServidorInterno]", "Servidor interno pronto.");
		}
		this.mundo = mundo;
		this.relogio = new Timer();
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
					mundo.chunks.clear();
					mundo.iniciar(true);
				break;
			}
			case Net.PACOTE_BLOCO: {
					int x = dis.readInt(); int y = dis.readInt(); int z = dis.readInt(); int id = dis.readInt();
					String item = Net.lerUTF(dis);
					mundo.defBlocoMundo(x, y, z, id);
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
						Jogo.jogadores.add(jgRede);
						Gdx.app.log("[Jogo]", "jogador " + nome + " entrou, id: " + id);
					}
				break;
			}
			case Net.PACOTE_SAIU: {
					int id = dis.readInt();
					Jogador jgRede = jogadoresRede.remove(id);
					if(jgRede != null) {
						Jogo.jogadores.remove(jgRede);
						Gdx.app.log("[Jogo]", "jogador " + jgRede.nome + " saiu");
					}
				break;
			}
		}
	}

	public void enviarPosicao(float x, float y, float z, float yaw, float tom, String item) {
		if(!netCliente.conectado || netCliente.clienteSaida == null) return;
		try {
			Jogador jg = Jogo.jogadores.get(0);
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
		dos.writeInt(chunk.x);
		dos.writeInt(chunk.z);
		dos.writeByte(chunk.usaPaleta ? 1 : 0);
		dos.writeInt(chunk.paletaBits);
		dos.writeInt(chunk.paletaTam);
		int paletaTam = (chunk.usaPaleta && chunk.paleta != null) ? chunk.paletaTam : 0;
		for(int i = 0; i < paletaTam; i++) dos.writeInt(chunk.paleta[i]);
		dos.writeInt(chunk.bitsPorBloco);
		dos.writeInt(chunk.blocosPorInt);
		int tamBlocos = (chunk.blocos != null) ? chunk.blocos.length : 0;
		dos.writeInt(tamBlocos);
		for(int i = 0; i < tamBlocos; i++) dos.writeInt(chunk.blocos[i]);
		dos.writeInt(chunk.luz.length);
		dos.write(chunk.luz);
		dos.writeInt(chunk.meta.length);
		for(int i = 0; i < chunk.meta.length; i++) dos.writeShort(chunk.meta[i]);
		dos.flush();
		return baos.toByteArray();
	}

	// le PACOTE_CHUNK do stream e insere em chunksMod
	public void lerChunk(DataInputStream dis) throws IOException {
		int cx = dis.readInt(); int cz = dis.readInt();
		boolean usaPaleta = dis.readByte() == 1;
		int paletaBits = dis.readInt(); int paletaTam = dis.readInt();
		Chunk chunk = new Chunk();
		chunk.x = cx; chunk.z = cz;
		chunk.usaPaleta = usaPaleta;
		chunk.paletaBits = paletaBits;
		chunk.paletaTam = paletaTam;
		if(paletaTam > 0) {
			chunk.paleta = new int[Math.max(1 << paletaBits, paletaTam)];
			for(int i = 0; i < paletaTam; i++) chunk.paleta[i] = dis.readInt();
		}
		chunk.bitsPorBloco = dis.readInt();
		chunk.blocosPorInt = dis.readInt();
		int tamBlocos = dis.readInt();
		if(tamBlocos > 0) {
			chunk.blocos = new int[tamBlocos];
			for(int i = 0; i < tamBlocos; i++) chunk.blocos[i] = dis.readInt();
		}
		int luzLen = dis.readInt();
		chunk.luz = new byte[luzLen];
		dis.readFully(chunk.luz);
		int metaLen = dis.readInt();
		chunk.meta = new short[metaLen];
		for(int i = 0; i < metaLen; i++) chunk.meta[i] = dis.readShort();
		chunk.chave = Chave.calcularChave(cx, cz);
		chunk.dadosProntos = true;
		chunk.att = true;
		synchronized(chunksMod) {
			chunksMod.put(chunk.chave, chunk);
		}
	}

	public void parar(Mundo mundo, List<Jogador> jogadores) {
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
		relogio.cancel();
		for(Jogador jg : Jogo.jogadores) jg.liberar();
		mundo.liberar();
		Gdx.app.log("[Servidor]", "Servidor interno encerrado.");
	}
}
