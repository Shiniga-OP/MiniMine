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
import com.minimine.mundo.chunks.ChunkUtil;
import com.minimine.mundo.Chave;
import com.badlogic.gdx.math.Vector3;
import java.util.Locale;
import com.minimine.entidades.Entidade;
import com.minimine.entidades.ItemMundo;
import com.minimine.mundo.blocos.Bloco;
import com.minimine.utils.DiaNoiteUtil;
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
	// instancia do Net no modo servidor que roda localmente
	public Net netServidor = null;
	public Net netCliente = null;
	public boolean rodando = false;
	public Map<Integer, Jogador> jogadoresRede = new HashMap<Integer, Jogador>();
	public Mundo mundo;
	public Timer relogio;
	public Map<Long, Chunk> chunksMod = new HashMap<>();
	/*
	 * sobe o servidor interno numa thread separada e espera ele estar pronto
	 * para aceitar conexões antes de retornar
	 * chame isso antes de criar o Net do cliente local
	*/
	public void iniciar(final Mundo mundo, final List<Jogador> jogadores) {
		if(rodando) return;
		rodando = true;

		// carrega o mundo antes de abrir o socket, servidor é a fonte de verdade
		if(ArquivosUtil.existe(Inicio.externo + "/MiniMine/mundos/" + mundo.nome + ".mini")) {
			ArquivosUtil.crMundo(mundo, jogadores.isEmpty() ? null : jogadores.get(0));
		}
		mundo.diaNoite = new DiaNoiteUtil();
		if(mundo.ciclo) mundo.diaNoite.iniciar();
		// sobe o Net em modo servidor, vai abrir TCP e UDP nas portas padrão
		// Gdx.app.postRunnable não é usado aqui porque iniciarTcpServidor
		// precisa estar pronto antes do cliente conectar; Net.iniciarTcpServidor
		// ja lança sua propria thread interna
		netServidor = new Net(Net.SERVIDOR_MODO);
		netServidor.ouvinte = new Net.OuvinteMensagem() {
			public void aoReceber(String msg) {
				processarMsg(msg);
			}
		};
		netServidor.ouvinteConexao = new Net.OuvinteConexao() {
			public void aoConectar(final Net.Cliente cliente) {
				new Thread(new Runnable() {
						public void run() {
							try {
								String msg =
									"MUNDO:TEMPO:"+mundo.diaNoite.tempo+
									":NOME:"+mundo.nome+
									":SEMENTE:"+mundo.semente+
									":PLANO:"+(mundo.plano ? "s" : "n");
								cliente.dados.println(msg);
								cliente.dados.flush();
								synchronized(mundo.chunksMod) {
									for(Chunk chunk : mundo.chunksMod.values()) {
										cliente.dados.println(serializarChunk(chunk));
									}
								}
								cliente.dados.flush();
								cliente.dados.println("MUNDO_FIM");
								cliente.dados.flush();
							} catch(Exception e) {
								Gdx.app.error("[ServidorInterno]", "Erro ao enviar mundo para cliente " + cliente.id + ": " + e.getMessage());
							}
						}
					}).start();
			}
		};
		// aguarda o socket estar aberto(max 3s) antes de deixar o cliente conectar
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
			Gdx.app.error("[ServidorInterno]", "Timeout ao aguardar socket do servidor interno.");
		} else {
			Gdx.app.log("[ServidorInterno]", "Servidor interno pronto.");
		}
		this.mundo = mundo;
		this.relogio = new Timer();
	}
	/*
	 * processa cada mensagem do protocolo enviado
	 * posição, camera, blocos, e etc
	*/
	public void processarMsg(String msg) {
        if(msg.startsWith("POS:")) {
            String[] p = msg.split(":");

            try {
                int id = Integer.parseInt(p[1]);
                if(netCliente != null && id == netCliente.idLocal) return;
                float x = Float.parseFloat(p[2]);
                float y = Float.parseFloat(p[3]);
                float z = Float.parseFloat(p[4]);
                float yaw = Float.parseFloat(p[5]);
                float tom = Float.parseFloat(p[6]);
                int marcas = p.length > 7 ? Integer.parseInt(p[7]) : 0;

                Jogador jgRede = jogadoresRede.get(id);
                if(jgRede == null) {
                    jgRede = new Jogador();
                    jgRede.modo = Jogo.modo;
                    jgRede.trocarPessoa();
                    jogadoresRede.put(id, jgRede);
                    Jogo.jogadores.add(jgRede);
                    Gdx.app.log("[Jogo]", "jogador " + id + " adicionado");
                }
                jgRede.posicao.set(x, y, z);
                jgRede.yaw = yaw;
				jgRede.tom = tom;
                jgRede.camera.direction.set(0, 0, -1);
                jgRede.camera.rotate(Vector3.Y, yaw);
                jgRede.frente = (marcas & 1) != 0;
                jgRede.tras = (marcas & 2) != 0;
                jgRede.esquerda = (marcas & 4) != 0;
                jgRede.direita = (marcas & 8) != 0;
                jgRede.voando = (marcas & 16) != 0;
                jgRede.agachado = (marcas & 32) != 0;
            } catch(NumberFormatException e) {}
        } else if(msg.startsWith("MUNDO:")) {
            String[] p = msg.split(":");
            try {
                for(int i = 1; i < p.length; i++) {
                    if(p[i].equals("TEMPO")) mundo.diaNoite.tempo = Float.parseFloat(p[i+1]);
					else if(p[i].equals("NOME")) mundo.nome = p[i+1];
					else if(p[i].equals("SEMENTE")) mundo.semente = Long.parseLong(p[i+1]);
					else if(p[i].equals("PLANO")) mundo.plano = p[i+1].equals("s") ? true : false;
                }
            } catch(NumberFormatException e) {
				Gdx.app.error("[Servidor]", "[ERRO]: mundo inicial "+e);
			}
        } else if(msg.startsWith("CHUNK:")) {
            deserializarChunk(msg);
        } else if(msg.startsWith("MUNDO_FIM")) {
			mundo.chunksMod = chunksMod;
			mundo.chunks.clear();
			mundo.iniciar(true);
        } else if(msg.startsWith("BLOCO:")) {
            String[] p = msg.split(":");

            try {
                int x = Integer.parseInt(p[1]);
                int y = Integer.parseInt(p[2]);
                int z = Integer.parseInt(p[3]);
                int id = Integer.parseInt(p[4]);
                mundo.defBlocoMundo(x, y, z, id);
				if(!p[5].equals("ar")) {
					final ItemMundo deixado = new ItemMundo(
						p[5], 1,
						x + 0.5f, y + 0.5f, z + 0.5f
					);
					mundo.entidades.add(deixado);
				}
            } catch(NumberFormatException e) {}
        } else if(msg.startsWith("ENTROU:")) {
            String[] p = msg.split(":");

            try {
                int id = Integer.parseInt(p[1]);

                if(netCliente == null || netCliente.idLocal == 0 || id == netCliente.idLocal) return;
                if(!jogadoresRede.containsKey(id)) {
                    Jogador jgRede = new Jogador();
                    jgRede.modo = Jogo.modo;
                    jgRede.pessoa = 3;
					jgRede.attModelo();
                    jogadoresRede.put(id, jgRede);
                    Jogo.jogadores.add(jgRede);
                    Gdx.app.log("[Jogo]", "jogador " + id + " entrou");
                }
            } catch(NumberFormatException e) {}
        } else if(msg.startsWith("SAIU:")) {
            String[] p = msg.split(":");

            try {
                int id = Integer.parseInt(p[1]);
                Jogador jgRede = jogadoresRede.remove(id);
                if(jgRede != null) {
                    Jogo.jogadores.remove(jgRede);
                    Gdx.app.log("[Jogo]", "jogador " + id + " saiu");
                }
            } catch(NumberFormatException e) {}
        }
    }

	public void enviarMsg(String msg) {
        if(netCliente.conectado && netCliente.clienteDados != null) {
            netCliente.clienteDados.println(msg);
        }
    }

	public void enviarPosicao(float x, float y, float z, float yaw, float tom) {
        Jogador jg = Jogo.jogadores.get(0);
        int marcas = (jg.frente ? 1 : 0) | (jg.tras ? 2 : 0) | (jg.esquerda ? 4 : 0)
            | (jg.direita ? 8 : 0) | (jg.voando ? 16 : 0) | (jg.agachado ? 32 : 0);
        String msg = String.format(Locale.US, "POS:%d:%f:%f:%f:%f:%f:%d", netCliente.idLocal, x, y, z, yaw, tom, marcas);
        enviarMsg(msg);
    }

    public void enviarBloco(int x, int y, int z, int id, String item) {
        String msg = String.format("BLOCO:%d:%d:%d:%d:%s", x, y, z, id, item);
        enviarMsg(msg);
    }
	/*
	 * serializa uma chunk para uma unica linha do protocolo:
	 * CHUNK:cx:cz:usaPaleta:paletaBits:paletaTam:paleta(csv):bitsPorBloco:blocosPorInt:blocos(csv):luz(csv):meta(csv)
	*/
	public static String serializarChunk(Chunk chunk) {
		StringBuilder sb = new StringBuilder("CHUNK:");
		sb.append(chunk.x).append(':').append(chunk.z).append(':');
		sb.append(chunk.usaPaleta ? 1 : 0).append(':');
		sb.append(chunk.paletaBits).append(':');
		sb.append(chunk.paletaTam).append(':');
		// paleta
		if(chunk.usaPaleta && chunk.paleta != null) {
			for(int i = 0; i < chunk.paletaTam; i++) {
				if(i > 0) sb.append(',');
				sb.append(chunk.paleta[i]);
			}
		}
		sb.append(':');
		sb.append(chunk.bitsPorBloco).append(':');
		sb.append(chunk.blocosPorInt).append(':');
		// blocos
		if(chunk.blocos != null) {
			for(int i = 0; i < chunk.blocos.length; i++) {
				if(i > 0) sb.append(',');
				sb.append(chunk.blocos[i]);
			}
		}
		sb.append(':');
		// luz
		for(int i = 0; i < chunk.luz.length; i++) {
			if(i > 0) sb.append(',');
			sb.append(chunk.luz[i]);
		}
		sb.append(':');
		// meta
		for(int i = 0; i < chunk.meta.length; i++) {
			if(i > 0) sb.append(',');
			sb.append(chunk.meta[i]);
		}
		return sb.toString();
	}
	/*
	 * reconstroi uma chunk a partir da linha do protocolo e insere em chunksMod
	*/
	public void deserializarChunk(String msg) {
		// formato: CHUNK:cx:cz:usaPaleta:paletaBits:paletaTam:paleta(csv):bitsPorBloco:blocosPorInt:blocos(csv):luz(csv):meta(csv)
		// usa indexOf para evitar split que quebraria os csv internos
		try {
			int pos = 6; // pula "CHUNK:"
			int fim;

			fim = msg.indexOf(':', pos);
			int cx = Integer.parseInt(msg.substring(pos, fim));
			pos = fim + 1;
			fim = msg.indexOf(':', pos);
			int cz = Integer.parseInt(msg.substring(pos, fim));
			pos = fim + 1;
			fim = msg.indexOf(':', pos);
			boolean usaPaleta = msg.charAt(pos) == '1'; pos = fim + 1;
			fim = msg.indexOf(':', pos);
			int paletaBits = Integer.parseInt(msg.substring(pos, fim));
			pos = fim + 1;
			fim = msg.indexOf(':', pos);
			int paletaTam = Integer.parseInt(msg.substring(pos, fim));
			pos = fim + 1;

			// paleta csv
			fim = msg.indexOf(':', pos);
			String paletaCsv = msg.substring(pos, fim); pos = fim + 1;
			fim = msg.indexOf(':', pos);
			int bitsPorBloco = Integer.parseInt(msg.substring(pos, fim));
			pos = fim + 1;
			fim = msg.indexOf(':', pos);
			int blocosPorInt = Integer.parseInt(msg.substring(pos, fim));
			pos = fim + 1;

			// blocos csv
			fim = msg.indexOf(':', pos);
			String blocosCsv = msg.substring(pos, fim);
			pos = fim + 1;

			// luz csv
			fim = msg.indexOf(':', pos);
			String luzCsv = msg.substring(pos, fim);
			pos = fim + 1;

			// meta csv(resto da string)
			String metaCsv = msg.substring(pos);

			Chunk chunk = new Chunk();
			chunk.x = cx;
			chunk.z = cz;
			chunk.usaPaleta = usaPaleta;
			chunk.paletaBits = paletaBits;
			chunk.paletaTam = paletaTam;
			chunk.bitsPorBloco = bitsPorBloco;
			chunk.blocosPorInt = blocosPorInt;

			if(usaPaleta && paletaTam > 0 && !paletaCsv.isEmpty()) {
				String[] pv = paletaCsv.split(",");
				chunk.paleta = new int[Math.max(1 << paletaBits, paletaTam)];
				for(int i = 0; i < pv.length; i++) chunk.paleta[i] = Integer.parseInt(pv[i]);
			}
			if(!blocosCsv.isEmpty()) {
				String[] bv = blocosCsv.split(",");
				chunk.blocos = new int[bv.length];
				for(int i = 0; i < bv.length; i++) chunk.blocos[i] = Integer.parseInt(bv[i]);
			}
			if(!luzCsv.isEmpty()) {
				String[] lv = luzCsv.split(",");
				for(int i = 0; i < lv.length && i < chunk.luz.length; i++) chunk.luz[i] = Byte.parseByte(lv[i]);
			}
			if(!metaCsv.isEmpty()) {
				String[] mv = metaCsv.split(",");
				for(int i = 0; i < mv.length && i < chunk.meta.length; i++) chunk.meta[i] = Short.parseShort(mv[i]);
			}
			chunk.chave = Chave.calcularChave(cx, cz);
			chunk.dadosProntos = true;
			chunk.att = true;
			synchronized(chunksMod) {
				chunksMod.put(chunk.chave, chunk);
			}
		} catch(Exception e) {
			Gdx.app.error("[Servidor]", "Erro ao deserializar chunk: " + e.getMessage());
		}
	}
	/*
	 * para o servidor interno e salva o mundo
	 */
	public void parar(Mundo mundo, List<Jogador> jogadores) {
		if(!rodando) return;
		rodando = false;

		if(netCliente != null) {
            netCliente.liberar();
            netCliente = null;
        }
		// so salva e fecha servidor se for servidor
		if(netServidor != null) {
			try {
				ArquivosUtil.svMundo(mundo, jogadores);
			} catch(Throwable t) {
				Gdx.app.error("[Servidor]", "Erro ao salvar mundo no encerramento: " + t.getMessage());
			}
			netServidor.liberar();
			netServidor = null;
		}
		chunksMod.clear();
		jogadoresRede.clear();
		relogio.cancel();
		Gdx.app.log("[Servidor]", "Servidor interno encerrado.");
	}
}

