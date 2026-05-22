package com.minimine.servidor;

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
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.FileOutputStream;
import java.io.InputStream;
import com.badlogic.gdx.Application;
import com.minimine.utils.ArquivosUtil;
import java.util.List;
import java.util.ArrayList;
import java.net.NetworkInterface;
import java.net.InterfaceAddress;
import java.util.Enumeration;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;

public class Net {
    public static final String NOME = "[MiniMine]: ";
    public static final int TCP_PORTA = 9001;
    public static final String CLIENTE_MODO = "CLIENTE";
    public static final String SERVIDOR_MODO = "SERVIDOR";
    public static final int[] versao = ArquivosUtil.VERSAO;

    public String modoAtual = SERVIDOR_MODO;
    public ServerSocket servidorSocket;
    public Array<Cliente> clientes = new Array<Cliente>();

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
    public volatile OuvinteMensagem ouvinte = null;

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
								cliente.dados.println("ID:" + id);
								synchronized(clientes) {
									for(int j = 0; j < clientes.size; j++) {
										Cliente c = clientes.get(j);
										if(c != cliente) cliente.dados.println("ENTROU:" + c.id + ":" + c.identidade + ":" + c.nome);
									}
								}
								cliente.dados.flush();
								if(ouvinteConexao != null) {
									final OuvinteConexao oc = ouvinteConexao;
									final Cliente clienteFinal = cliente;
									Gdx.app.postRunnable(new Runnable() {
											public void run() { oc.aoConectar(clienteFinal); }
										});
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

    public class Cliente implements Runnable {
        public final Socket socket;
        public final BufferedReader entrada;
        public final PrintWriter dados;
        public final int id;
        public boolean rodando = true;
        public String identidade = "";
        public String nome = "";

        public Cliente(Socket socket, int id) throws IOException {
            this.socket = socket;
            this.id = id;
            this.entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()), 1024 * 1024);
            this.dados = new PrintWriter(socket.getOutputStream(), false);
        }

        @Override
        public void run() {
            try {
                String linha;
                while(rodando && (linha = entrada.readLine()) != null) {
                    final String msg = linha;
                    if(msg.startsWith("ENTROU:")) {
                        String[] p = msg.split(":");
                        if(p.length >= 4) { identidade = p[2]; nome = p[3]; }
                    }
                    broadcast(msg, this);
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
                        DatagramPacket envioPacote = new DatagramPacket(dadosEnvio, dadosEnvio.length, broadcast, TCP_PORTA);
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

    public void receberMsgServidor() {
        try {
            String servidorMsg;
            while(conectado && clienteEntrada != null && (servidorMsg = clienteEntrada.readLine()) != null) {
                final String msg = servidorMsg;
                if(msg.startsWith("ID:")) {
                    try {
                        idLocal = Integer.parseInt(msg.substring(3).trim());
                    } catch(NumberFormatException e) {}
                }
                while(ouvinte == null) {
                    try { Thread.sleep(10); } catch(InterruptedException e) { break; }
                }
                if(ouvinte != null) {
                    final OuvinteMensagem ov = ouvinte;
                    Gdx.app.postRunnable(new Runnable() {
							public void run() { ov.aoReceber(msg); }
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


