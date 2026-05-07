package com.minimine.utils;

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

public class Net {
    public static final String NOME = "[MiniMine]: ";
    public static final int TCP_PORTA = 9001;
    public static final int UDP_PORTA = 9002;
    public static final String CLIENTE_MODO = "CLIENTE";
    public static final String SERVIDOR_MODO = "SERVIDOR";
    public static final int[] versao = ArquivosUtil.VERSAO;

    public String modoAtual = SERVIDOR_MODO;
    // estruturas do servidor
    public ServerSocket servidorSocket;
    public Array<Cliente> clientes = new Array<Cliente>();
    public DatagramSocket attSocket;
    // estruturas do cliente
    public Socket clienteSocket;
    public PrintWriter clienteDados;
    public BufferedReader clienteEntrada;
    public boolean conectado = false;
    public volatile String IP = null;
	public static String ultimoIP = null;

    public Net(String modoAtual) {
        Gdx.app.log(NOME, "Iniciando como: " + modoAtual);

        if(modoAtual.equals(SERVIDOR_MODO)) {
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
						procurarE_Conectar();
					}
				}).start();
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
								Cliente cliente = new Cliente(socket);
								clientes.add(cliente);
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
        Gdx.app.log(NOME, "Ouvindo por pedidos de descoberta UDP na porta " + UDP_PORTA);
        try {
            attSocket = new DatagramSocket(UDP_PORTA);
            byte[] buffer = new byte[1024];
            DatagramPacket pacote = new DatagramPacket(buffer, buffer.length);

            while(attSocket != null && !attSocket.isClosed()) {
                attSocket.receive(pacote);
                String msg = new String(pacote.getData(), 0, pacote.getLength());

                if(msg.startsWith("[MINIMINE]: descobrindo servidor")) {
                    Gdx.app.log(NOME + "-descoberta", "Pedido de descoberta de " + pacote.getAddress().getHostAddress());
                    byte[] respostaDados = "[MiniMine]: servidor encontrado".getBytes();
                    DatagramPacket respostaPacote = new DatagramPacket(respostaDados, respostaDados.length, pacote.getAddress(), pacote.getPort());
                    attSocket.send(respostaPacote);
                    Gdx.app.log(NOME + "-descoberta", "Resposta de confirmação enviada.");
                }
            }
        } catch(Exception e) {
            Gdx.app.error(NOME, "Erro no listener UDP: " + e.getMessage());
        } finally {
            if(attSocket != null) attSocket.close();
        }
    }

    public class Cliente implements Runnable {
        public final Socket socket;
        public final BufferedReader entrada;
        public final PrintWriter dados;
        public boolean rodando = true;

        public Cliente(Socket socket) throws IOException {
            this.socket = socket;
            this.entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.dados = new PrintWriter(socket.getOutputStream(), true);
        }

        public void run() {
            try {
                String linhaEntrada;
                while(rodando && (linhaEntrada = entrada.readLine()) != null) {
                    Gdx.app.log(NOME + "-Servidor", "Recebido do Cliente: " + linhaEntrada);
                    String resposta = "Servidor recebeu: " + linhaEntrada + "Olá devolta do servidor";
                    dados.println(resposta);
                    Gdx.app.log(NOME + "-Servidor", "Enviando de volta: " + resposta);
                }
            } catch(IOException e) {
                Gdx.app.error(NOME + "-Servidor", "Conexão perdida com o cliente: " + socket.getRemoteAddress());
            } finally {
                clientes.removeValue(this, true);
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
            DatagramPacket envioPacote = new DatagramPacket(dadosEnvio, dadosEnvio.length, InetAddress.getByName("255.255.255.255"), UDP_PORTA);
            socket.send(envioPacote);
            Gdx.app.log(NOME + "-descoberta", "Pacote de descoberta enviado...");
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
            clienteEntrada = new BufferedReader(new InputStreamReader(clienteSocket.getInputStream()));
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
        if(conectado) {
            Gdx.app.log(NOME + "-Cliente", "Enviando mensagem: " + msg);
            clienteDados.println(msg);
        } else {
            Gdx.app.error(NOME + "-Cliente", "Não conectado. Não pode enviar mensagem.");
        }
    }

    public void receberMsgServidor() {
        try {
            String servidorMsg;
            while(conectado && clienteEntrada != null && (servidorMsg = clienteEntrada.readLine()) != null) {
                Gdx.app.log("Mensagem Recebida", servidorMsg);
            }
        } catch(IOException e) {
            Gdx.app.error(NOME + "-Cliente", "Conexão com o servidor perdida: " + e.getMessage());
        } finally {
            conectado = false;
            Gdx.app.log(NOME + "-Cliente", "Cliente desconectado.");
        }
    }

    public static final String URL_VERSAO = "https://focadoestudios.netlify.app/pacotes/minimine/versao.txt";
    public static final String URL_APK    = "https://focadoestudios.netlify.app/pacotes/minimine/MiniMine.apk";
    public static final String URL_JAR    = "https://focadoestudios.netlify.app/pacotes/minimine/minimine.jar";

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
            for(Cliente cliente : clientes) {
                cliente.rodando = false;
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

