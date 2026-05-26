package com.minimine.cenas;

import com.badlogic.gdx.Screen;
import com.minimine.entidades.Jogador;
import com.minimine.audio.Musicas;
import com.minimine.mundo.Mundo;
import com.minimine.graficos.Render;
import com.minimine.utils.ArquivosUtil;
import com.minimine.mods.LuaAPI;
import com.minimine.mundo.blocos.Bloco;
import com.badlogic.gdx.Gdx;
import com.minimine.graficos.Renderizador;
import com.minimine.mundo.chunks.ChunkProcesso;
import com.minimine.mundo.chunks.ChunkLuz;
import com.minimine.mundo.chunks.ChunkMalha;
import com.minimine.servidor.Net;
import java.util.List;
import java.util.ArrayList;
import com.badlogic.gdx.math.MathUtils;
import com.minimine.servidor.ServidorInterno;
import com.minimine.utils.DiaNoiteUtil;
import com.minimine.Inicio;
import java.io.DataInputStream;
import java.io.IOException;
import com.minimine.servidor.TarefaTick;
import com.minimine.utils.TarefasUtil;

public class Jogo implements Screen {
	public static Mundo mundo;
	public static List<Jogador> jogadores;
	public static int modo = 2;
	public static Renderizador render;
	public static boolean musicas = true, graficosTeste = false;
	public static String identidade;
	public static String nome = "Breno";
	public static ServidorInterno servidor;
	public static String debug1 = "", debug2 = "";

	public static final float INTERVALO_POS = 0.05f;
	public float tempoPosicao = 0f;

	@Override
	public void show() {
		TarefasUtil.iniciar();
		
		servidor = new ServidorInterno();
		mundo = new Mundo();
		jogadores = new ArrayList<Jogador>();
		Jogador jogador = new Jogador(identidade);
		jogador.modo = modo;
		jogadores.add(jogador);

		Bloco.iniciar();

		if(!Net.CLIENTE_MODO.equals(MultiMenu.modoRede)) {
			servidor.iniciar(mundo, jogadores);
		} else {
			servidor.jgUi = jogador;
			servidor.jogadores = jogadores;
			servidor.mundo = mundo;
			if(mundo.ciclo) mundo.diaNoite.iniciar();
			mundo.iniciar(false);
			servidor.rodando = true;
			servidor.threadTick = new Thread(new Runnable() {
					public void run() {
						int numTick = 0;
						while(servidor.rodando) {
							final long inicio = System.currentTimeMillis();
							servidor.tick(numTick++);
							final long gasto = System.currentTimeMillis() - inicio;
							final long espera = servidor.MS_POR_TICK - gasto;
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
			servidor.threadTick.setName("servidor-tick");
			servidor.threadTick.setDaemon(true);
			servidor.threadTick.start();
		}
		if(!Net.CLIENTE_MODO.equals(MultiMenu.modoRede)) {
			servidor.netCliente = new Net(Net.CLIENTE_MODO, "127.0.0.1");
		} else {
			if(MultiMenu.netClientePronto != null) {
				servidor.netCliente = MultiMenu.netClientePronto;
				MultiMenu.netClientePronto = null;
			} else {
				servidor.netCliente = new Net(Net.CLIENTE_MODO);
			}
		}
		servidor.netCliente.ouvinte = new Net.OuvintePacote() {
			public void aoReceber(byte tipo, DataInputStream dis) throws IOException {
				if(tipo == Net.PACOTE_ID) {
					servidor.enviarEntrou(servidor.netCliente.idLocal, identidade, nome);
				}
				servidor.processarPacote(tipo, dis);
			}
		};
		ChunkProcesso.luz = new ChunkLuz();
		ChunkProcesso.malha = new ChunkMalha();
		render = new Render(jogadores, mundo);

		render.iniciar();
		if(!Net.CLIENTE_MODO.equals(MultiMenu.modoRede)) {
			mundo.iniciar(true);
		}
		servidor.tarefasLoop.add(new TarefaTick() {
				public void executar(int tick) {
					if(musicas && tick % 20 == 0) Musicas.tocarAleatorio();
				}
			});
		try {
			LuaAPI.iniciar();
		} catch(Exception e) {}
	}

	@Override
	public void render(float delta) {
		render.att(delta);
		if(mundo.carregado) LuaAPI.att(delta);

		if(servidor.netCliente != null && jogadores.size() > 0) {
			tempoPosicao += delta;
			if(tempoPosicao >= INTERVALO_POS) {
				tempoPosicao = 0f;
				Jogador jg = jogadores.get(0);
				servidor.enviarPosicao(
					jg.posicao.x, jg.posicao.y, jg.posicao.z,
					jg.yaw, jg.tom, jg.item
				);
			}
		}
	}

	@Override
	public void dispose() {
		mundo.carregado = false;
		render.liberar();
		Bloco.liberar();
		servidor.parar();
		TarefasUtil.liberar();
	}

	@Override
	public void resize(int v, int h) {
		render.ui.ajustar(v, h);
		LuaAPI.ajustar(v, h);
	}

	@Override
	public void hide() {
		dispose();
	}

	@Override
	public void pause() {
		if(servidor.rodando) {
			ArquivosUtil.svMundo(mundo, jogadores);
		}
	}

	@Override public void resume() {}
}
