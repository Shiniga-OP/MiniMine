package com.minimine.servidor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net.Protocol;
import com.badlogic.gdx.net.ServerSocket;
import com.badlogic.gdx.net.ServerSocketHints;
import com.badlogic.gdx.net.Socket;
import com.badlogic.gdx.net.SocketHints;
import com.badlogic.gdx.utils.Array;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import com.minimine.utils.ArquivosUtil;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class Net {
	public static final String NOME = "[MiniMine]: ";
	public static final int TCP_PORTA = 9001;
	public static final String CLIENTE_MODO = "CLIENTE";
	public static final String SERVIDOR_MODO = "SERVIDOR";
	public static final int[] versao = ArquivosUtil.VERSAO;

	// tipos de pacote
	public static final byte PACOTE_POS = 0x01;
	public static final byte PACOTE_MUNDO = 0x02;
	public static final byte PACOTE_CHUNK = 0x03;
	public static final byte PACOTE_MUNDO_FIM = 0x04;
	public static final byte PACOTE_BLOCO = 0x05;
	public static final byte PACOTE_ENTROU = 0x06;
	public static final byte PACOTE_SAIU = 0x07;
	public static final byte PACOTE_ID = 0x08;

	public String modoAtual = SERVIDOR_MODO;
	public ServerSocket servidorSocket;
	public Array<Cliente> clientes = new Array<Cliente>();

	public Socket clienteSocket;
	public DataOutputStream clienteSaida;
	public DataInputStream clienteEntrada;
	public boolean conectado = false;
	public volatile String IP = null;
	public static String ultimoIP = null;

	public volatile int idLocal = 0;
	public static int proximoId = 1;

	public interface OuvintePacote {
		void aoReceber(byte tipo, DataInputStream dados) throws IOException;
	}
	public volatile OuvintePacote ouvinte = null;

	public interface OuvinteConexao {
		void aoConectar(Cliente cliente);
	}
	public OuvinteConexao ouvinteConexao = null;

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
		} else if(modoAtual.equals(CLIENTE_MODO)) {
			new Thread(new Runnable() {
					public void run() {
						procurarE_Conectar();
					}
				}).start();
		}
	}

	// construtor para conexão direta por IP(sem descoberta, funciona via VPN/internet)
	public Net(String modoAtual, final String ipFixo) {
		Gdx.app.log(NOME, "Iniciando como: " + modoAtual + " (IP direto: " + ipFixo + ")");
		this.modoAtual = modoAtual;
		this.IP = ipFixo;
		ultimoIP = ipFixo;

		if(modoAtual.equals(CLIENTE_MODO)) {
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
								// PACOTE_ID
								synchronized(cliente) {
									cliente.saida.writeByte(PACOTE_ID);
									cliente.saida.writeInt(id);
									cliente.saida.flush();
								}
								// notifica o novo cliente dos que ja estão autenticados
								synchronized(clientes) {
									for(int j = 0; j < clientes.size; j++) {
										Cliente c = clientes.get(j);
										if(c == cliente || c.identidade.isEmpty()) continue;
										synchronized(cliente) {
											try {
												cliente.saida.writeByte(PACOTE_ENTROU);
												cliente.saida.writeInt(c.id);
												escreverUTF(cliente.saida, c.identidade);
												escreverUTF(cliente.saida, c.nome);
												cliente.saida.flush();
											} catch(IOException e) {}
										}
									}
								}
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

	// broadcast de pacote bruto para todos "exceto"
	public void broadcast(byte[] pacote, Cliente exceto) {
		synchronized(clientes) {
			for(int i = 0; i < clientes.size; i++) {
				Cliente c = clientes.get(i);
				if(c == exceto) continue;
				try {
					synchronized(c) {
						c.saida.write(pacote);
						c.saida.flush();
					}
				} catch(IOException e) {
					Gdx.app.error(NOME, "Erro ao broadcast para cliente " + c.id + ": " + e.getMessage());
				}
			}
		}
	}

	public void broadcastTodos(byte[] pacote) {
		broadcast(pacote, null);
	}

	// escreve String como short(tamanho) + bytes UTF-8
	public static void escreverUTF(DataOutputStream dos, String s) throws IOException {
		if(s == null) s = "";
		byte[] b = s.getBytes("UTF-8");
		dos.writeShort(b.length);
		dos.write(b);
	}

	// le String como short(tamanho) + bytes UTF-8
	public static String lerUTF(DataInputStream dis) throws IOException {
		int tam = dis.readShort() & 0xFFFF;
		byte[] b = new byte[tam];
		dis.readFully(b);
		return new String(b, "UTF-8");
	}

	public class Cliente implements Runnable {
		public final Socket socket;
		public final DataInputStream entrada;
		public final DataOutputStream saida;
		public final int id;
		public boolean rodando = true;
		public String identidade = "";
		public String nome = "";

		public Cliente(Socket socket, int id) throws IOException {
			this.socket = socket;
			this.id = id;
			this.entrada = new DataInputStream(new BufferedInputStream(socket.getInputStream(), 1024 * 64));
			this.saida = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream(), 1024 * 64));
		}

		@Override
		public void run() {
			try {
				while(rodando) {
					byte tipo = entrada.readByte();
					// monta pacote bruto pra broadcast antes de processar
					// PACOTE_ENTROU e PACOTE_BLOCO fazem broadcast pra todos; o resto só pros outros
					final byte[] pacote = lerPacoteBruto(tipo, entrada);
					if(tipo == PACOTE_ENTROU) {
						DataInputStream tmp = new DataInputStream(
							new ByteArrayInputStream(pacote, 1, pacote.length - 1)
						);
						try {
							tmp.readInt(); // id
							identidade = lerUTF(tmp);
							nome = lerUTF(tmp);
						} catch(IOException e) {}
						broadcast(pacote, null);
						// notifica o recem-autenticado dos clientes que ja estão autenticados
						synchronized(clientes) {
							for(int j = 0; j < clientes.size; j++) {
								Cliente c = clientes.get(j);
								if(c == this || c.identidade.isEmpty()) continue;
								try {
									ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
									DataOutputStream dos2 = new DataOutputStream(baos2);
									dos2.writeByte(PACOTE_ENTROU);
									dos2.writeInt(c.id);
									escreverUTF(dos2, c.identidade);
									escreverUTF(dos2, c.nome);
									dos2.flush();
									synchronized(this) {
										saida.write(baos2.toByteArray());
										saida.flush();
									}
								} catch(IOException e) {}
							}
						}
						// agora que o cliente se identificou, envia o mundo
						if(ouvinteConexao != null) {
							final OuvinteConexao oc = ouvinteConexao;
							final Cliente clienteFinal = this;
							Gdx.app.postRunnable(new Runnable() {
								public void run() { oc.aoConectar(clienteFinal); }
							});
						}
					} else if(tipo == PACOTE_BLOCO) {
						broadcast(pacote, null);
					} else {
						broadcast(pacote, this);
					}
					if(ouvinte != null) {
						final OuvintePacote ov = ouvinte;
						final byte tipofinal = tipo;
						final DataInputStream dis = new DataInputStream(
							new ByteArrayInputStream(pacote, 1, pacote.length - 1)
						);
						Gdx.app.postRunnable(new Runnable() {
								public void run() {
									try { ov.aoReceber(tipofinal, dis); } catch(IOException e) {}
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
				// broadcast PACOTE_SAIU
				try {
					ByteArrayOutputStream baos = new ByteArrayOutputStream(5);
					DataOutputStream tmp = new DataOutputStream(baos);
					tmp.writeByte(PACOTE_SAIU);
					tmp.writeInt(id);
					tmp.flush();
					broadcast(baos.toByteArray(), null);
					if(ouvinte != null) {
						final OuvintePacote ov = ouvinte;
						final byte[] pkt = baos.toByteArray();
						Gdx.app.postRunnable(new Runnable() {
								public void run() {
									try {
										DataInputStream dis = new DataInputStream(
											new ByteArrayInputStream(pkt, 1, pkt.length - 1)
										);
										ov.aoReceber(PACOTE_SAIU, dis);
									} catch(IOException e) {}
								}
							});
					}
				} catch(IOException e) {}
				try { socket.dispose(); } catch(Exception e) {}
			}
		}
	}
	/*
	 * le um pacote completo do stream e retorna como byte[] com o byte de tipo incluido no inicio
	 * cada tipo tem tamanho fixo ou prefixado, sem delimitadores de texto
	*/
	public static byte[] lerPacoteBruto(byte tipo, DataInputStream dis) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream(256);
		DataOutputStream tmp = new DataOutputStream(baos);
		tmp.writeByte(tipo);
		switch(tipo) {
			case PACOTE_POS: {
					int id = dis.readInt();
					float x = dis.readFloat();
					float y = dis.readFloat();
					float z = dis.readFloat();
					float yaw = dis.readFloat();
					float tom = dis.readFloat();
					int marcas = dis.readInt();
					int itemTam = dis.readShort() & 0xFFFF;
					byte[] item = new byte[itemTam];
					dis.readFully(item);
					tmp.writeInt(id);
					tmp.writeFloat(x);
					tmp.writeFloat(y);
					tmp.writeFloat(z);
					tmp.writeFloat(yaw);
					tmp.writeFloat(tom);
					tmp.writeInt(marcas);
					tmp.writeShort(itemTam);
					tmp.write(item);
					break;
				}
			case PACOTE_MUNDO: {
					float tempo = dis.readFloat();
					int nomeTam = dis.readShort() & 0xFFFF;
					byte[] nome = new byte[nomeTam];
					dis.readFully(nome);
					long semente = dis.readLong();
					byte plano = dis.readByte();
					tmp.writeFloat(tempo);
					tmp.writeShort(nomeTam);
					tmp.write(nome);
					tmp.writeLong(semente);
					tmp.writeByte(plano);
					break;
				}
			case PACOTE_CHUNK: {
					int cx = dis.readInt();
					int cz = dis.readInt();
					byte usaPaleta = dis.readByte();
					int paletaBits = dis.readInt();
					int paletaTam = dis.readInt();
					int[] paleta = new int[paletaTam];
					for(int i = 0; i < paletaTam; i++) paleta[i] = dis.readInt();
					int bitsPorBloco = dis.readInt(); int blocosPorInt = dis.readInt();
					int tamBlocos = dis.readInt();
					int[] blocos = new int[tamBlocos];
					for(int i = 0; i < tamBlocos; i++) blocos[i] = dis.readInt();
					byte[] luz = new byte[16*256*16];
					dis.readFully(luz);
					short[] meta = new short[16*256*16];
					for(int i = 0; i < meta.length; i++) meta[i] = dis.readShort();
					tmp.writeInt(cx); tmp.writeInt(cz); tmp.writeByte(usaPaleta);
					tmp.writeInt(paletaBits); tmp.writeInt(paletaTam);
					for(int i = 0; i < paletaTam; i++) tmp.writeInt(paleta[i]);
					tmp.writeInt(bitsPorBloco); tmp.writeInt(blocosPorInt);
					tmp.writeInt(tamBlocos);
					for(int i = 0; i < tamBlocos; i++) tmp.writeInt(blocos[i]);
					tmp.write(luz);
					for(int i = 0; i < meta.length; i++) tmp.writeShort(meta[i]);
				break;
			}
			case PACOTE_MUNDO_FIM:
			break;
			case PACOTE_BLOCO: {
					int x = dis.readInt();
					int y = dis.readInt();
					int z = dis.readInt();
					int id = dis.readInt();
					int itemTam = dis.readShort() & 0xFFFF;
					byte[] item = new byte[itemTam];
					dis.readFully(item);
					tmp.writeInt(x); tmp.writeInt(y); tmp.writeInt(z); tmp.writeInt(id);
					tmp.writeShort(itemTam); tmp.write(item);
				break;
			}
			case PACOTE_ENTROU: {
					int id = dis.readInt();
					int iTam = dis.readShort() & 0xFFFF;
					byte[] identidade = new byte[iTam];
					dis.readFully(identidade);
					int nTam = dis.readShort() & 0xFFFF;
					byte[] nome = new byte[nTam];
					dis.readFully(nome);
					tmp.writeInt(id);
					tmp.writeShort(iTam);
					tmp.write(identidade);
					tmp.writeShort(nTam);
					tmp.write(nome);
				break;
			}
			case PACOTE_SAIU: {
					int id = dis.readInt();
					tmp.writeInt(id);
				break;
			}
			case PACOTE_ID: {
					int id = dis.readInt();
					tmp.writeInt(id);
				break;
			}
			default:
				throw new IOException("tipo de pacote desconhecido: " + tipo);
		}
		tmp.flush();
		return baos.toByteArray();
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

			Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
			while(interfaces != null && interfaces.hasMoreElements()) {
				NetworkInterface iface = interfaces.nextElement();
				try {
					if(!iface.isUp() || iface.isLoopback()) continue;
					for(InterfaceAddress ifAddr : iface.getInterfaceAddresses()) {
						InetAddress broadcast = ifAddr.getBroadcast();
						if(broadcast == null) continue;
						DatagramPacket envioPacote = new DatagramPacket(dadosEnvio, dadosEnvio.length, broadcast, TCP_PORTA);
						socket.send(envioPacote);
						Gdx.app.log(NOME + "-descoberta", "Broadcast enviado para " + broadcast.getHostAddress() + " via " + iface.getDisplayName());
					}
				} catch(Exception e) {
					Gdx.app.log(NOME + "-descoberta", "Ignorando interface " + iface.getDisplayName() + ": " + e.getMessage());
				}
			}
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
			clienteSaida = new DataOutputStream(new BufferedOutputStream(clienteSocket.getOutputStream(), 1024 * 64));
			clienteEntrada = new DataInputStream(new BufferedInputStream(clienteSocket.getInputStream(), 1024 * 64));
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

	public void receberMsgServidor() {
		try {
			while(conectado && clienteEntrada != null) {
				byte tipo = clienteEntrada.readByte();
				if(tipo == PACOTE_ID) {
					idLocal = clienteEntrada.readInt();
					while(ouvinte == null) {
						try { Thread.sleep(10); } catch(InterruptedException e) { break; }
					}
					if(ouvinte != null) {
						final OuvintePacote ov = ouvinte;
						final int idRecebido = idLocal;
						Gdx.app.postRunnable(new Runnable() {
								public void run() {
									try {
										DataInputStream dis = new DataInputStream(
											new ByteArrayInputStream(new byte[]{
												(byte)(idRecebido >> 24), (byte)(idRecebido >> 16),
												(byte)(idRecebido >> 8), (byte)idRecebido
											})
										);
										ov.aoReceber(PACOTE_ID, dis);
									} catch(IOException e) {}
								}
							});
					}
					continue;
				}
				final byte[] pacote = lerPacoteBruto(tipo, clienteEntrada);
				while(ouvinte == null) {
					try { Thread.sleep(10); } catch(InterruptedException e) { break; }
				}
				if(ouvinte != null) {
					final OuvintePacote ov = ouvinte;
					final byte tipofinal = tipo;
					final DataInputStream dis = new DataInputStream(
						new ByteArrayInputStream(pacote, 1, pacote.length - 1)
					);
					Gdx.app.postRunnable(new Runnable() {
							public void run() {
								try { ov.aoReceber(tipofinal, dis); } catch(IOException e) {}
							}
						});
				}
			}
		} catch(IOException e) {
			Gdx.app.error(NOME + "-Cliente", "Conexão com o servidor perdida: " + e.getMessage());
		} finally {
			conectado = false;
			Gdx.app.log(NOME + "-Cliente", "Cliente desconectado.");
		}
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
