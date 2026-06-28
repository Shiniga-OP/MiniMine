package com.minimine.cenas;

import com.badlogic.gdx.Screen;
import com.minimine.entidades.Jogador;
import com.minimine.audio.Musicas;
import com.minimine.mundo.Mundo;
import com.minimine.graficos.Render;
import com.minimine.utils.ArquivosUtil;
import com.minimine.mundo.blocos.Bloco;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
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
import com.minimine.mundo.geracao.RegistroBiomas;
import com.minimine.entidades.RegistroCriaturas;
import com.minimine.inventario.ReceitaRegistro;
import com.minimine.inventario.Item;
import com.minimine.ui.InterUtil;

public class Jogo implements Screen {
	public static Mundo mundo;
	public static List<Jogador> jogadores;
	public static Jogador jogador;
	public static int modo = 2;
	public static Render render;
	public static boolean musicas = true;
	public static String identidade;
	public static String nome = "Breno";
	public static ServidorInterno servidor;
	public static String debug1 = "", debug2 = "";

	public static final float INTERVALO_POS = 0.05f;
	public float tempoPosicao = 0f;

	// carregamento
	public ShapeRenderer srCarregamento;
	public SpriteBatch sbCarregamento;
	public BitmapFont fonteCarregamento;
	public volatile String msgBarra = "";
	public volatile float progresso = 0f;
	
	@Override
	public void show() {
		TarefasUtil.iniciar();
		
		msgBarra = "Carregando graficos...";
		Mundo.carregado = false;
		srCarregamento = new ShapeRenderer();
		sbCarregamento = new SpriteBatch();
		fonteCarregamento = InterUtil.carregarFonte("fontes/pixel-16.fnt", 1.5f);
		
		progresso = 0.1f;
		msgBarra = "Criando recursos...";

		servidor = new ServidorInterno();
		mundo = new Mundo();
		jogadores = new ArrayList<Jogador>();
		jogador = new Jogador(identidade);
		jogador.modo = modo;
		jogadores.add(jogador);
		
		TarefasUtil.fisica.execute(new Runnable() {
			@Override
			public void run() {
				Bloco.iniciar();
				Item.iniciar();

				progresso = 0.2f;
				msgBarra = "Carregando criaturas...";
				
				mundo.registroCriaturas = new RegistroCriaturas();
				mundo.registroCriaturas.carregar(Gdx.files.internal("criaturas/"));

				progresso = 0.3f;
				msgBarra = "Carregando biomas...";

				mundo.registroBiomas = new RegistroBiomas();
				mundo.registroBiomas.carregarBiomas(Gdx.files.internal("biomas/"));

				ReceitaRegistro.iniciar();

				progresso = 0.5f;
				msgBarra = "Configurando ambiente...";
				
				if(!Net.CLIENTE_MODO.equals(MultiMenu.modoRede)) {
					servidor.iniciar(mundo, jogadores);
				} else {
					servidor.jgUi = jogador;
					servidor.jogadores = jogadores;
					servidor.mundo = mundo;
					
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
				render = new Render(jogadores, mundo);
				
				Gdx.app.postRunnable(new Runnable() {
					@Override
					public void run() {
						if(mundo.ciclo) mundo.diaNoite.iniciar();
						render.iniciar();
						if(!Net.CLIENTE_MODO.equals(MultiMenu.modoRede)) {
							mundo.iniciar(true);
						}
						render.ui.ajustar(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
						progresso = 0.7f;
						msgBarra = "Configurando músicas...";
						servidor.tarefasLoop.add(new TarefaTick() {
								public void executar(int tick) {
									if(musicas && tick % 20 == 0) Musicas.tocarAleatorio();
								}
							});
						progresso = 0.9f;
						msgBarra = "Gerando mundo...";
					}
				});
			}
		});
	}

	@Override
	public void render(float delta) {
		if(!mundo.carregado) {
			renderCarregamento();
			return;
		}
		if(srCarregamento != null) {
			srCarregamento.dispose();
			sbCarregamento.dispose();
			srCarregamento = null;
			sbCarregamento = null;
			fonteCarregamento.setColor(1f, 1f, 1f, 1f);
			fonteCarregamento = null;
		}
		render.att(delta);

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

	public void renderCarregamento() {
		final int v = Gdx.graphics.getWidth();
		final int h = Gdx.graphics.getHeight();

		Gdx.gl.glClearColor(0.08f, 0.08f, 0.1f, 1f);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		final float largBarra = v * 0.5f;
		final float altBarra = 18f;
		final float xBarra = (v - largBarra) / 2f;
		final float yBarra = h * 0.42f;

		srCarregamento.begin(ShapeRenderer.ShapeType.Filled);
		// fundo
		srCarregamento.setColor(0.2f, 0.2f, 0.25f, 1f);
		srCarregamento.rect(xBarra, yBarra, largBarra, altBarra);
		// preenchimento
		srCarregamento.setColor(0.3f, 0.7f, 0.4f, 1f);
		srCarregamento.rect(xBarra, yBarra, largBarra * progresso, altBarra);
		srCarregamento.end();

		srCarregamento.begin(ShapeRenderer.ShapeType.Line);
		srCarregamento.setColor(0.5f, 0.5f, 0.55f, 1f);
		srCarregamento.rect(xBarra, yBarra, largBarra, altBarra);
		srCarregamento.end();
		
		sbCarregamento.begin();
		fonteCarregamento.getData().setScale(2.0f);
		fonteCarregamento.setColor(1f, 1f, 1f, 1f);
		fonteCarregamento.draw(sbCarregamento, "Carregando mundo...", xBarra, yBarra + altBarra + 48f);
		fonteCarregamento.getData().setScale(1.5f);
		fonteCarregamento.setColor(0.7f, 0.7f, 0.75f, 1f);
		fonteCarregamento.draw(sbCarregamento, msgBarra, xBarra, yBarra - 10f);
		sbCarregamento.end();
	}

	@Override
	public void dispose() {
		if(srCarregamento != null) {
			srCarregamento.dispose();
			sbCarregamento.dispose();
			srCarregamento = null;
			sbCarregamento = null;
			fonteCarregamento = null;
		}
		if(mundo == null) return;
		mundo.carregado = false;
		render.liberar();
		Bloco.liberar();
		servidor.parar();
		TarefasUtil.liberar();
	}

	@Override
	public void resize(int v, int h) {
		if(render != null) render.ui.ajustar(v, h);
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
