package com.minimine.servidor;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net.Protocol;
import com.badlogic.gdx.net.ServerSocket;
import com.badlogic.gdx.net.ServerSocketHints;
import com.badlogic.gdx.net.Socket;
import com.badlogic.gdx.net.SocketHints;
import com.badlogic.gdx.utils.Array;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.FileOutputStream;
import java.io.InputStream;
import com.badlogic.gdx.Application;
import com.minimine.Instalador;
import com.minimine.mundo.Mundo;
import com.minimine.mundo.chunks.Chunk;
import com.minimine.mundo.chunks.ChunkProcesso;
import com.minimine.mundo.blocos.Bloco;
import com.minimine.mundo.Chave;
import java.util.Map;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import com.minimine.utils.ArquivosUtil;
import java.util.List;
import java.util.ArrayList;
import java.util.zip.DeflaterOutputStream;
import java.util.Set;
import java.util.HashSet;
import java.util.Enumeration;
import java.net.NetworkInterface;
import java.net.InterfaceAddress;
import java.util.zip.InflaterInputStream;

public class Net {
    public static final String NOME = "[MiniMine]: ";
    public static final int TCP_PORTA = 9001;
    public static final int UDP_PORTA = 9002;
    public static final int UDP_POS_PORTA = 9003;
    public static final String CLIENTE_MODO = "CLIENTE";
    public static final String SERVIDOR_MODO = "SERVIDOR";
    public static final int[] versao = ArquivosUtil.VERSAO;

    public String modoAtual = SERVIDOR_MODO;
    public ServerSocket servidorSocket;
    public Array<Cliente> clientes = new Array<Cliente>();
    public DatagramSocket attSocket;
    public DatagramSocket udpCliente;
    public InetAddress udpServidorEndereco;
    public Socket clienteSocket;
    public PrintWriter clienteDados;
    public BufferedReader clienteEntrada;
    public boolean conectado = false;
    public volatile String IP = null;
    public static String ultimoIP = null;

    public volatile int idLocal = 0;
    public static int proximoId = 1;

    public interface OuvinteMensagem {
        void aoReceber(String msg);
    }
    public OuvinteMensagem ouvinte = null;

    public Net(String modoAtual) {
        Gdx.app.log(NOME, "Iniciando como: " + modoAtual);
        this.modoAtual = modoAtual;

        if(modoAtual.equals(SERVIDOR_MODO)) {
            idLocal = 0;
            Gdx.app.postRunnable(new Runnable() {
					public void run() {
						iniciarTcpServidor();
					}
				});
            new Thread(new Runnable() {
					public void run() {
						iniciarReceptor();
					}
				}).start();
        } else if(modoAtual.equals(CLIENTE_MODO)) {
            new Thread(new Runnable() {
					public void run() {
                        try {
                            udpCliente = new DatagramSocket();
                        } catch(Exception e) {
                            Gdx.app.error(NOME, "Falha ao criar socket UDP do cliente: " + e.getMessage());
                        }
						procurarE_Conectar();
					}
				}).start();
        }
    }

    // construtor para conexão direta por IP(sem descoberta UDP, funciona via VPN/internet)
    public Net(String modoAtual, final String ipFixo) {
        Gdx.app.log(NOME, "Iniciando como: " + modoAtual + " (IP direto: " + ipFixo + ")");
        this.modoAtual = modoAtual;
        this.IP = ipFixo;
        ultimoIP = ipFixo;

        if(modoAtual.equals(CLIENTE_MODO)) {
            new Thread(new Runnable() {
                    public void run() {
                        try {
                            udpCliente = new DatagramSocket();
                            udpServidorEndereco = InetAddress.getByName(ipFixo);
                        } catch(Exception e) {
                            Gdx.app.error(NOME, "Falha ao criar socket UDP do cliente: " + e.getMessage());
                        }
                    }
                }).start();
            Gdx.app.postRunnable(new Runnable() {
					public void run() {
						conectarServidorTcp();
					}
				});
        }
    }

    public void iniciarTcpServidor() {
        ServerSocketHints servidorInfo = new ServerSocketHints();
        servidorInfo.acceptTimeout = 0;
        try {
            servidorSocket = Gdx.net.newServerSocket(Protocol.TCP, TCP_PORTA, servidorInfo);
            Gdx.app.log(NOME, "Servidor TCP iniciado. Porta " + TCP_PORTA);
            new Thread(new Runnable() {
					public void run() {
						while(servidorSocket != null) {
							try {
								Socket socket = servidorSocket.accept(null);
								Gdx.app.log(NOME, "Cliente TCP conectado: " + socket.getRemoteAddress());
								int id = proximoId++;
								Cliente cliente = new Cliente(socket, id);
								synchronized(clientes) {
									clientes.add(cliente);
								}
								cliente.dados.println("ID:" + id);
								broadcast("ENTROU:" + id, cliente);
								// notifica o proprio servidor que um jogador entrou
								if(ouvinte != null) {
									final OuvinteMensagem ov = ouvinte;
									final int idFinal = id;
									Gdx.app.postRunnable(new Runnable() {
											public void run() { ov.aoReceber("ENTROU:" + idFinal); }
										});
								}
								final Cliente clienteFinal = cliente;
								new Thread(new Runnable() {
										public void run() {
											try {
												clienteFinal.dados.println("SEMENTE:" + Mundo.semente);
												clienteFinal.dados.flush();
												// envia chunks modificados; enviarChunkParaCliente controla duplicatas
												for(Map.Entry<Long, Chunk> e : Mundo.chunksMod.entrySet()) {
													enviarChunkParaCliente(clienteFinal, e.getValue());
													Thread.sleep(20);
												}
												Thread.sleep(500);
												clienteFinal.dados.println("CHUNKS_FIM:");
												clienteFinal.dados.flush();
											} catch(Exception e) {
												Gdx.app.error(NOME, "Erro ao enviar chunks para cliente " + clienteFinal.id + ": " + e.getMessage());
											}
										}
									}).start();
								new Thread(cliente).start();
							} catch(Exception e) {
								Gdx.app.error(NOME, "Erro ao aceitar conexão TCP: " + e.getMessage());
								if(servidorSocket == null) break;
							}
						}
					}
				}).start();
        } catch(Exception e) {
            Gdx.app.error(NOME, "Falha ao iniciar Servidor TCP: " + e.getMessage());
        }
    }

    public void iniciarReceptor() {
        Gdx.app.log(NOME, "Ouvindo UDP na porta " + UDP_POS_PORTA);
        try {
            attSocket = new DatagramSocket(UDP_POS_PORTA);
            byte[] buffer = new byte[1024];
            DatagramPacket pacote = new DatagramPacket(buffer, buffer.length);

            while(attSocket != null && !attSocket.isClosed()) {
                attSocket.receive(pacote);
                final String msg = new String(pacote.getData(), 0, pacote.getLength());
                final InetAddress origem = pacote.getAddress();
                final int portaOrigem = pacote.getPort();

                if(msg.startsWith("[MINIMINE]: descobrindo servidor")) {
                    Gdx.app.log(NOME + "-descoberta", "Pedido de descoberta de " + origem.getHostAddress());
                    byte[] respostaDados = "[MiniMine]: servidor encontrado".getBytes();
                    DatagramPacket respostaPacote = new DatagramPacket(respostaDados, respostaDados.length, origem, portaOrigem);
                    attSocket.send(respostaPacote);
                    Gdx.app.log(NOME + "-descoberta", "Resposta de confirmação enviada.");
                } else if(msg.startsWith("POS:")) {
                    // reencaminha para todos os outros clientes via UDP(exceto o remetente)
                    byte[] dados = msg.getBytes();
                    synchronized(clientes) {
                        for(int i = 0; i < clientes.size; i++) {
                            Cliente c = clientes.get(i);
                            try {
                                InetAddress endCliente = InetAddress.getByName(
                                    c.socket.getRemoteAddress().replace("/", "").split(":")[0]);
                                // registra endereco UDP e não devolve o pacote pro remetente
                                if(origem.equals(endCliente)) {
                                    c.enderecoUdp = endCliente;
                                    continue;
                                }
                                DatagramPacket dp = new DatagramPacket(dados, dados.length, endCliente, UDP_POS_PORTA);
                                attSocket.send(dp);
                            } catch(Exception e) {
                                // ignora erro de reencaminhamento pra um cliente especifico
                            }
                        }
                    }
                    if(ouvinte != null) {
                        final OuvinteMensagem ov = ouvinte;
                        Gdx.app.postRunnable(new Runnable() {
                                public void run() { ov.aoReceber(msg); }
                            });
                    }
                }
            }
        } catch(Exception e) {
            Gdx.app.error(NOME, "Erro no listener UDP: " + e.getMessage());
        } finally {
            if(attSocket != null) attSocket.close();
        }
    }

    public void broadcast(String msg, Cliente exceto) {
        synchronized(clientes) {
            for(int i = 0; i < clientes.size; i++) {
                Cliente c = clientes.get(i);
                if(c != exceto) {
                    c.dados.println(msg);
                    c.dados.flush();
                }
            }
        }
    }

    public void broadcastTodos(String msg) {
        broadcast(msg, null);
    }

    // serializa e envia um chunk para um cliente, marcando como enviado para nao repetir
    public void enviarChunkParaCliente(final Cliente cliente, final Chunk chunk) {
        synchronized(cliente.chunksEnviados) {
            if(cliente.chunksEnviados.contains(chunk.chave)) return;
        }
        new Thread(new Runnable() {
				public void run() {
					try {
						ByteArrayOutputStream baos = new ByteArrayOutputStream();
						DeflaterOutputStream deflate = new DeflaterOutputStream(baos);
						DataOutputStream dos = new DataOutputStream(deflate);
						dos.writeInt(chunk.x);
						dos.writeInt(chunk.z);
						List<int[]> blocos = new ArrayList<int[]>();
						for(int x = 0; x < Mundo.TAM_CHUNK; x++)
							for(int y = 0; y < Mundo.Y_CHUNK; y++)
								for(int z = 0; z < Mundo.TAM_CHUNK; z++) {
									int b = ChunkProcesso.util.obterBloco(x, y, z, chunk);
									if(b != 0) blocos.add(new int[]{x, y, z, b});
								}
						dos.writeInt(blocos.size());
						for(int[] bl : blocos) {
							dos.writeInt(bl[0]);
							dos.writeInt(bl[1]);
							dos.writeInt(bl[2]);
							dos.writeUTF("" + Bloco.numIds.get(bl[3]).nome);
						}
						int metaTam = Mundo.TAM_CHUNK * Mundo.Y_CHUNK * Mundo.TAM_CHUNK;
						for(int i = 0; i < metaTam; i++) dos.writeShort(chunk.meta[i]);
						dos.close();
						byte[] raw = baos.toByteArray();
						StringBuilder sb = new StringBuilder(raw.length * 2);
						for(int i = 0; i < raw.length; i++) {
							int v = raw[i] & 0xFF;
							if(v < 16) sb.append('0');
							sb.append(Integer.toHexString(v));
						}
						synchronized(cliente.chunksEnviados) {
							if(cliente.chunksEnviados.contains(chunk.chave)) return;
							cliente.dados.println("CHUNK:" + sb.toString());
							cliente.dados.flush();
							cliente.chunksEnviados.add(chunk.chave);
						}
					} catch(Exception e) {
						Gdx.app.error(NOME, "Erro ao enviar chunk " + chunk.chave + " para cliente " + cliente.id + ": " + e.getMessage());
					}
				}
			}).start();
    }

    public class Cliente implements Runnable {
        public final Socket socket;
        public final BufferedReader entrada;
        public final PrintWriter dados;
        public final int id;
        public boolean rodando = true;
        // chunks já enviados para este cliente, evita reenvio redundante
        public final Set<Long> chunksEnviados = new HashSet<Long>();
        // endereço IP do cliente para filtragem UDP
        public volatile InetAddress enderecoUdp = null;

        public Cliente(Socket socket, int id) throws IOException {
            this.socket = socket;
            this.id = id;
            this.entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()), 1024 * 1024);
            this.dados = new PrintWriter(socket.getOutputStream(), false);
        }

        public void run() {
            try {
                String linha;
                while(rodando && (linha = entrada.readLine()) != null) {
                    final String msg = linha;
                    if(msg.startsWith("BLOCO:")) broadcast(msg, null);
                    else broadcast(msg, this);
                    if(ouvinte != null) {
                        final OuvinteMensagem ov = ouvinte;
                        Gdx.app.postRunnable(new Runnable() {
								public void run() {
									ov.aoReceber(msg);
								}
							});
                    }
                }
            } catch(IOException e) {
                Gdx.app.error(NOME + "-Servidor", "Conexão perdida com cliente " + id + ": " + e.getMessage());
            } finally {
                rodando = false;
                synchronized(clientes) {
                    clientes.removeValue(this, true);
                }
                broadcast("SAIU:" + id, null);
                if(ouvinte != null) {
                    final OuvinteMensagem ov = ouvinte;
                    final int cid = id;
                    Gdx.app.postRunnable(new Runnable() {
							public void run() {
								ov.aoReceber("SAIU:" + cid);
							}
						});
                }
                try {
                    socket.dispose();
                } catch(Exception e) {
                    Gdx.app.error(NOME, "Erro ao fechar socket do cliente.", e);
                }
            }
        }
    }

    public void procurarE_Conectar() {
        Gdx.app.log(NOME, "Iniciando descoberta de Servidor...");
        int tentativas = 0;
        final int MAX_TENTATIVAS = 5;
        while(IP == null && tentativas < MAX_TENTATIVAS) {
            procurarServidor();
            try {
                Thread.sleep(1000);
            } catch(InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            tentativas++;
        }
        if(IP != null) {
            Gdx.app.log(NOME, "Servidor encontrado em: " + IP);
            Gdx.app.postRunnable(new Runnable() {
					public void run() {
						conectarServidorTcp();
					}
				});
        } else {
            Gdx.app.error(NOME, "Não foi possível encontrar o Servidor após " + MAX_TENTATIVAS + " tentativas.");
        }
    }

    public void procurarServidor() {
        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket();
            socket.setBroadcast(true);
            socket.setSoTimeout(500);
            byte[] dadosEnvio = "[MINIMINE]: descobrindo servidor".getBytes();

            // manda broadcast em todas as interfaces de rede(alcança ZeroTier, Hamachi, etc)
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while(interfaces != null && interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                try {
                    if(!iface.isUp() || iface.isLoopback()) continue;
                    for(InterfaceAddress ifAddr : iface.getInterfaceAddresses()) {
                        InetAddress broadcast = ifAddr.getBroadcast();
                        if(broadcast == null) continue;
                        DatagramPacket envioPacote = new DatagramPacket(dadosEnvio, dadosEnvio.length, broadcast, UDP_PORTA);
                        socket.send(envioPacote);
                        Gdx.app.log(NOME + "-descoberta", "Broadcast enviado para " + broadcast.getHostAddress() + " via " + iface.getDisplayName());
                    }
                } catch(Exception e) {
                    Gdx.app.log(NOME + "-descoberta", "Ignorando interface " + iface.getDisplayName() + ": " + e.getMessage());
                }
            }
            Gdx.app.log(NOME + "-descoberta", "Broadcasts enviados em todas as interfaces...");
            byte[] receBuffer = new byte[1024];
            DatagramPacket pacoteRecebido = new DatagramPacket(receBuffer, receBuffer.length);
            socket.receive(pacoteRecebido);
            String resposta = new String(pacoteRecebido.getData(), 0, pacoteRecebido.getLength());
            if(resposta.contains("servidor encontrado")) {
                IP = pacoteRecebido.getAddress().getHostAddress();
                Gdx.app.log(NOME + "-descoberta", "Servidor encontrado: " + IP);
            }
        } catch(Exception e) {
            Gdx.app.log(NOME + "-descoberta", "Tentativa falhou: " + e.getMessage());
        } finally {
            if(socket != null) socket.close();
        }
    }

    public void conectarServidorTcp() {
        if(IP == null) IP = ultimoIP;
        try {
            SocketHints hints = new SocketHints();
            hints.connectTimeout = 5000;
            clienteSocket = Gdx.net.newClientSocket(Protocol.TCP, IP, TCP_PORTA, hints);
            clienteDados = new PrintWriter(clienteSocket.getOutputStream(), true);
            clienteEntrada = new BufferedReader(new InputStreamReader(clienteSocket.getInputStream()), 1024 * 1024);
            conectado = true;
            Gdx.app.log(NOME, "Conectado ao servidor TCP em: " + IP);
            new Thread(new Runnable() {
					public void run() {
						receberMsgServidor();
					}
				}).start();
        } catch(Exception e) {
            Gdx.app.error(NOME, "Falha ao conectar ao Servidor TCP: " + e.getMessage());
            conectado = false;
        }
    }

    public void enviarMsg(String msg) {
        if(conectado && clienteDados != null) {
            clienteDados.println(msg);
        }
    }

    public interface OuvinteChunk {
        void aoReceberChunk(Chunk chunk, long chave);
    }
    public OuvinteChunk ouvinteChunk = null;

    public void receberMsgServidor() {
        try {
            String servidorMsg;
            while(conectado && clienteEntrada != null && (servidorMsg = clienteEntrada.readLine()) != null) {
                final String msg = servidorMsg;
                if(msg.startsWith("ID:")) {
                    try {
                        idLocal = Integer.parseInt(msg.substring(3).trim());
                    } catch(NumberFormatException e) {}
                    if(ouvinte != null) {
                        final OuvinteMensagem ov = ouvinte;
                        Gdx.app.postRunnable(new Runnable() {
								public void run() { ov.aoReceber(msg); }
							});
                    }
                } else if(msg.startsWith("CHUNK:")) {
                    // descomprime na thread de rede, não bloqueia a thread GL
                    try {
                        String hex = msg.substring(6);
                        byte[] comprimido = new byte[hex.length() / 2];
                        for(int i = 0; i < comprimido.length; i++)
                            comprimido[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
                       InflaterInputStream inf = new InflaterInputStream(new ByteArrayInputStream(comprimido));
                        DataInputStream dis = new DataInputStream(inf);
                        int cx = dis.readInt();
                        int cz = dis.readInt();
                        final long chave = Chave.calcularChave(cx, cz);
                        final Chunk chunk = new Chunk();
                        chunk.x = cx;
                        chunk.z = cz;
                        chunk.chave = chave;
                        chunk.meta = new short[Mundo.TAM_CHUNK * Mundo.Y_CHUNK * Mundo.TAM_CHUNK];
                        ChunkProcesso.util.compactar(ChunkProcesso.util.bitsPraMaxId(chunk.maxIds), chunk);
                        int total = dis.readInt();
                        for(int k = 0; k < total; k++) {
                            int x = dis.readInt();
                            int y = dis.readInt();
                            int z = dis.readInt();
                            String id = dis.readUTF();
                            ChunkProcesso.util.defBloco(x, y, z, id, chunk);
                        }
                        int metaTam = Mundo.TAM_CHUNK * Mundo.Y_CHUNK * Mundo.TAM_CHUNK;
                        for(int i = 0; i < metaTam; i++) chunk.meta[i] = dis.readShort();
                        chunk.dadosProntos = true;
                        chunk.att = true;
                        if(ouvinteChunk != null) {
                            final OuvinteChunk oc = ouvinteChunk;
                            Gdx.app.postRunnable(new Runnable() {
									public void run() { oc.aoReceberChunk(chunk, chave); }
								});
                        }
                    } catch(Exception e) {
                        Gdx.app.error(NOME + "-Cliente", "Erro ao processar CHUNK: " + e.getMessage());
                    }
                } else {
                    if(ouvinte != null) {
                        final OuvinteMensagem ov = ouvinte;
                        Gdx.app.postRunnable(new Runnable() {
								public void run() { ov.aoReceber(msg); }
							});
                    }
                }
            }
        } catch(IOException e) {
            Gdx.app.error(NOME + "-Cliente", "Conexão com o servidor perdida: " + e.getMessage());
        } finally {
            conectado = false;
            Gdx.app.log(NOME + "-Cliente", "Cliente desconectado.");
        }
    }

    public void enviarPosicao(float x, float y, float z, float yaw, float tom) {
        String msg = "POS:" + idLocal + ":" + x + ":" + y + ":" + z + ":" + yaw + ":" + tom;
        if(modoAtual.equals(SERVIDOR_MODO)) {
            broadcastTodos(msg);
        } else {
            enviarMsg(msg);
        }
    }

    public void enviarBloco(int x, int y, int z, int id) {
        String msg = "BLOCO:" + x + ":" + y + ":" + z + ":" + id;
        if(modoAtual.equals(SERVIDOR_MODO)) {
            broadcastTodos(msg);
        } else {
            enviarMsg(msg);
        }
    }
    public static final String URL_VERSAO = "https://focadoestudios.netlify.app/pacotes/minimine/versao.txt";
    public static final String URL_APK = "https://focadoestudios.netlify.app/pacotes/minimine/MiniMine.apk";
    public static final String URL_JAR = "https://focadoestudios.netlify.app/pacotes/minimine/minimine.jar";

    // padrão chamado na thread principal depois da verificação
    public interface ResultadoAtualizacao {
        /*
         * temAtualizacao: true se encontrou versão nova
         * novaVersao: "0.1.2", ou null se sem internet/erro
         * tipo: "OFICIAL", "BETA" ou "ALFA"
		 */
        void aoVerificar(boolean temAtualizacao, String novaVersao, String tipo);
    }

	public interface ResultadoDownload {
		void aoBaixar(String caminho);
	}
    /*
     * verifica em segundo plano se ha uma versão nova disponivel
     * comparação por hierarquia:
     *   [0] oficial -> mudança mais importante
     *   [1] beta -> mudança intermediária
     *   [2] alfa -> mudança mais frequente/menos polida
	 */
    public static void verificarAtualizacao(final ResultadoAtualizacao padrao) {
        new Thread(new Runnable() {
				public void run() {
					try {
						HttpURLConnection con = (HttpURLConnection) new URL(URL_VERSAO).openConnection();
						con.setConnectTimeout(5000);
						con.setReadTimeout(5000);
						con.setRequestMethod("GET");

						int status = con.getResponseCode();
						if(status != 200) {
							Gdx.app.log(NOME, "verificarAtualizacao: servidor retornou " + status);
							notificar(padrao, false, null, null);
							return;
						}
						BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));
						String linha = br.readLine();
						br.close();
						con.disconnect();

						if(linha == null || linha.trim().isEmpty()) {
							notificar(padrao, false, null, null);
							return;
						}
						String[] partes = linha.trim().split("\\.");
						if(partes.length < 3) {
							Gdx.app.log(NOME, "verificarAtualizacao: formato inválido -> " + linha);
							notificar(padrao, false, null, null);
							return;
						}
						final int[] i = {
							Integer.parseInt(partes[0].trim()),
							Integer.parseInt(partes[1].trim()),
							Integer.parseInt(partes[2].trim())
						};
						final int[] v = ArquivosUtil.VERSAO;

						Gdx.app.log(NOME, "Versão local:    " + v[0]+"."+v[1]+"."+v[2]);
						Gdx.app.log(NOME, "Versão internet: " + i[0]+"."+i[1]+"."+i[2]);

						boolean temAtu = false;
						String tipo = null;
						String novaVersao = i[0]+"."+i[1]+"."+i[2];

						if(i[0] > v[0]) {
							temAtu = true;
							tipo = "OFICIAL";
						} else if(i[0] == v[0] && i[1] > v[1]) {
							temAtu = true;
							tipo = "BETA";
						} else if(i[0] == v[0] && i[1] == v[1] && i[2] > v[2]) {
							temAtu = true;
							tipo = "ALFA";
						}
						if(temAtu) {
							Gdx.app.log(NOME, "Nova versão disponível! (" + tipo + ") " + novaVersao);
						} else {
							Gdx.app.log(NOME, "Jogo já está na versão mais recente.");
						}
						notificar(padrao, temAtu, temAtu ? novaVersao : null, tipo);
					} catch(Exception e) {
						Gdx.app.log(NOME, "verificarAtualizacao: sem internet ou erro -> " + e.getMessage());
						notificar(padrao, false, null, null);
					}
				}
			}).start();
    }

    public static void notificar(final ResultadoAtualizacao cb, final boolean tem, final String versao, final String tipo) {
        if(cb == null) return;
        Gdx.app.postRunnable(new Runnable() {
				public void run() {
					cb.aoVerificar(tem, versao, tipo);
				}
			});
    }
    /*
     * baixa a atualização e salva no caminho indicado
     * ao terminar(ou falhar), chama padrao.aoBaixar(caminho) na thread principal
     *   caminho != null -> sucesso
     *   caminho == null -> falha
	 */
    public static void baixarAtualizacao(final String destino, final ResultadoDownload padrao) {
        final String urlDownload = (Gdx.app.getType() == Application.ApplicationType.Android)
            ? URL_APK : URL_JAR;

        new Thread(new Runnable() {
				public void run() {
					try {
						Gdx.app.log(NOME, "Baixando atualização de: " + urlDownload);

						HttpURLConnection con = (HttpURLConnection) new URL(urlDownload).openConnection();
						con.setConnectTimeout(10000);
						con.setReadTimeout(0);
						con.setRequestMethod("GET");

						if(con.getResponseCode() != 200) {
							Gdx.app.log(NOME, "Erro ao baixar: HTTP " + con.getResponseCode());
							notificarDownload(padrao, null);
							return;
						}
						InputStream is = con.getInputStream();
						FileOutputStream fos = new FileOutputStream(destino);
						byte[] buf = new byte[8192];
						int lido;
						while((lido = is.read(buf)) > 0) fos.write(buf, 0, lido);
						fos.flush();
						fos.close();
						is.close();
						con.disconnect();

						Gdx.app.log(NOME, "Download concluído: " + destino);
						notificarDownload(padrao, destino);
					} catch(Exception e) {
						Gdx.app.log(NOME, "Erro no download: " + e.getMessage());
						notificarDownload(padrao, null);
					}
				}
			}).start();
    }

    public static void notificarDownload(final ResultadoDownload cb, final String resultado) {
        if(cb == null) return;
        Gdx.app.postRunnable(new Runnable() {
				public void run() {
					cb.aoBaixar(resultado);
				}
			});
    }

    public void liberar() {
        if(servidorSocket != null) {
            synchronized(clientes) {
                for(int i = 0; i < clientes.size; i++) {
                    clientes.get(i).rodando = false;
                }
            }
            try {
                servidorSocket.dispose();
            } catch(Exception e) {
                Gdx.app.error(NOME, "Erro ao fechar servidor socket.", e);
            }
            servidorSocket = null;
        }
        if(attSocket != null) {
            attSocket.close();
            attSocket = null;
        }
        conectado = false;
        if(clienteSocket != null) {
            try {
                clienteSocket.dispose();
            } catch(Exception e) {
                Gdx.app.error(NOME, "Erro ao fechar cliente socket.", e);
            }
            clienteSocket = null;
        }
    }
}

