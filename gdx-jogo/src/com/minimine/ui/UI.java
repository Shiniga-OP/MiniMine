package com.minimine.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaValue;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.minimine.Debugador;
import com.minimine.Inicio;
import com.minimine.Logs;
import com.minimine.cenas.Inventario;
import com.minimine.cenas.Jogador;
import com.minimine.cenas.Jogo;
import com.minimine.cenas.Jogador.Estado;
import com.minimine.graficos.Texturas;
import com.minimine.mods.LuaAPI;
import com.minimine.mundo.Mundo;
import com.minimine.utils.ArquivosUtil;
import com.minimine.utils.DiaNoiteUtil;

public class UI implements InputProcessor {
	public static PerspectiveCamera camera;
	public static Map<CharSequence, Botao> botoes = new HashMap<>();
	public static Map<CharSequence, Texto> textos = new HashMap<>();
	public static Dialogo dialogo = new Dialogo();
	public static SpriteBatch sb;
	public static BitmapFont fonte;
	public static CharSequence otimizadorC = "desligado";

	public static boolean esquerda = false, frente = false, tras = false, direita = false, cima = false, baixo = false, acao = false;
	public static float sensi = 0.25f;
	public static float aprox = 0.07f;
	public static float distancia = 400f;
	public static int pov = 120;
	public static final HashMap<Integer, CharSequence> toques = new HashMap<>();
	public static Runtime rt = Runtime.getRuntime();

	public static float botaoTam = 70f;
	public static float espaco = 60f;
	public static Jogador jogador;
	public static boolean debug = false;
	public static boolean modoTexto = false;
	public static int fps = 0;

	public static Debugador debugador;
	public static EstanteVertical menuOpcoes;

	public static boolean menuAberto = false;
	public static void attCamera(PerspectiveCamera camera, float yaw, float tom) {
		float yawRad = yaw * MathUtils.degRad;
		float tomRad = tom * MathUtils.degRad;

		float cx = MathUtils.cos(tomRad) * MathUtils.sin(yawRad);
		float cy = MathUtils.sin(tomRad);
		float cz = MathUtils.cos(tomRad) * MathUtils.cos(yawRad);

		camera.direction.set(cx, cy, cz).nor();
		camera.up.set(0, 1, 0);
	}
	public static void liberar() {
		for(Botao e : botoes.values()) {
			e.aoFim();
		}
		sb.dispose();
		fonte.dispose();
	}
	public static void abrirDialogo(String titulo) {
		abrirDialogo(titulo, "", "", null, null, null);
	}
	public static void abrirDialogo(String titulo, String padrao, String msg, final LuaFunction func) {
		abrirDialogo(titulo, padrao, msg, func, null, null);
	}

	public static void abrirDialogo(String titulo, String padrao, String msg, final LuaFunction func, final LuaFunction func2) {
		abrirDialogo(titulo, padrao, msg, func, func2, null);
	}
	public static void abrirDialogo(final String titulo, String padrao, String msg, final LuaFunction func, final LuaFunction func2, final LuaFunction func3) {
		modoTexto = true;

		dialogo.abrir(titulo, new Dialogo.Acao() {
			@Override
			public void aoConfirmar() {
				if(func != null) func.call(LuaValue.valueOf(dialogo.texto));
				Gdx.input.setCursorCatched(true);
			}
			@Override
			public void aoFechar() {
				if(func2 != null) func2.call();
				Gdx.input.setCursorCatched(true);
			}
			@Override
			public void aoDigitar(char p) {
				if(func3 != null) func3.call(LuaValue.valueOf(p));
			}
		});
		dialogo.texto = padrao;
	}
	public Sprite spriteMira;

	public int pontoEsq = -1;

	public int pontoDir = -1;
	public final Vector2 esqCentro = new Vector2();
	public final Vector2 esqPos = new Vector2();

	public final Vector2 ultimaDir = new Vector2();

	public final Vector3 frenteV = new Vector3(0, 0, 0), direitaV = new Vector3(0, 0, 0);

	public boolean chatAberto = false;

	public String ultimaMensagem = "";

	public List<String> mensagens = new ArrayList<String>();

	public UI(Jogador jogador) {
		camera = new PerspectiveCamera(pov, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		camera.position.set(10f, 18f, 10f);
		camera.lookAt(0, 0, 0);
		camera.near = aprox;
		camera.far = distancia;
		camera.update();

		sb = new SpriteBatch(); 

		fonte = InterUtil.carregarFonte("ui/fontes/pixel.ttf", 15);

		Gdx.input.setInputProcessor(this);
		Gdx.input.setCursorCatched(true); // prende o mouse no meio da tela
		this.jogador = jogador;
		this.jogador.camera = camera;
		this.jogador.inv = new Inventario();

		configDpad(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		otimizadorC = Inicio.ehArm64 ? "ativo" : "não suportado";
	}

	public void abrirChat() {
		if(chatAberto) return;
		chatAberto = true;
		modoTexto = true;

		dialogo.abrir("chat", new Dialogo.Acao() {
			@Override
			public void aoConfirmar() {
				if(dialogo.texto != null && dialogo.texto.length() > 0) {
					ultimaMensagem = dialogo.texto;
					mensagens.add("> " + dialogo.texto);
					Gdx.app.log("CHAT", dialogo.texto);
				}
				chatAberto = false;
				Gdx.input.setCursorCatched(true);
			}
			@Override
			public void aoDigitar(char p) {}
			@Override
			public void aoFechar() {
				chatAberto = false;
				modoTexto = false;
				Gdx.input.setCursorCatched(true);
			}
		});
	}

	private boolean jogadorInterageComBloco() {
		if(jogador.estado == Jogador.Estado.QUEBRANDO_BLOCO) {
			jogador.item = "ar";
			jogador.interagirBloco();
			if(jogador.inv.itens[jogador.inv.slotSelecionado] != null) jogador.item = jogador.inv.itens[jogador.inv.slotSelecionado].nome;
			else jogador.item = "ar";
			return true;
		}
		if(jogador.estado == Jogador.Estado.COLOCANDO_BLOCO) {
			if(jogador.inv.itens[jogador.inv.slotSelecionado] != null) jogador.item = jogador.inv.itens[jogador.inv.slotSelecionado].nome;
			else jogador.item = "ar";
			acao = true;
			jogador.interagirBloco();
			return true;
		}

		return false;

	}

	private boolean calcVisaoJogador(int telaX, int telaY) {
		int y = Gdx.graphics.getHeight() - telaX;
		jogador.inv.aoArrastar(telaY, y, -1);

		if(!jogador.inv.aberto) {
			float dx = Gdx.input.getDeltaX();
			float dy = Gdx.input.getDeltaY();
			jogador.yaw -= dx * sensi;
			jogador.tom -= dy * sensi;
			if(jogador.tom > 89f) jogador.tom = 89f;
			if(jogador.tom < -89f) jogador.tom = -89f;
		}
		return true;
	}

	@Override
	public boolean touchDown(int telaX, int telaY, int ponteiro, int butao) {
		int y = Gdx.graphics.getHeight() - telaY;

		if(dialogo.visivel) {
			dialogo.verificarToque(telaX, y);
			if(!dialogo.visivel) modoTexto = false; // se fechou ao clicar fora, sai do modo texto
			return true;
		}
		if(modoTexto) return true;

		if(menuAberto) {
			for(InterUtil.Objeto objeto : menuOpcoes.filhos) {
				if(objeto instanceof Botao) {
					Botao bo = (Botao) objeto;
					if(bo.hitbox.contains(telaX, y)) {
						bo.aoTocar(telaX, y, ponteiro);
						return true;
					}
				}
			}
		}
		if(Gdx.app.getType() == com.badlogic.gdx.Application.ApplicationType.Desktop && !jogador.inv.aberto) {
			if(butao == Input.Buttons.LEFT)
				jogador.estado = Jogador.Estado.QUEBRANDO_BLOCO;

			if(butao == Input.Buttons.RIGHT)
				jogador.estado = Jogador.Estado.COLOCANDO_BLOCO;

			
			if(jogador.estado != Jogador.Estado.OLHANDO)
				return true;
		}

		for(Botao e : botoes.values()) {
			if(e.hitbox.contains(telaX, y)) {
				e.aoTocar(telaX, y, ponteiro);
				toques.put(ponteiro, e.nome);
				return true;
			}
		}
		jogador.inv.aoTocar(telaX, y, ponteiro);
		if(pontoDir == -1) { 
			pontoDir = ponteiro; 
			ultimaDir.set(telaX, y); 
		}
		return true;
	}

	@Override
	public boolean touchUp(int telaX, int telaY, int p, int b) {
		if(modoTexto) return true;
		if(b == Input.Buttons.RIGHT) acao = false;

		int y = Gdx.graphics.getHeight() - telaY;
		CharSequence botao = toques.remove(p);
		if(botao != null) {
			for(Botao e : botoes.values()) {
				if(botao.equals(e.nome)) {
					e.aoSoltar(telaX, y, p);
					break;
				}
			}
		}

		jogador.estado = Jogador.Estado.OLHANDO;
		if(p == pontoDir) pontoDir = -1;
		return true;
	}

	@Override
	public boolean touchDragged(int telaX, int telaY, int ponteiro) {
		if(modoTexto) return true;
		int y = Gdx.graphics.getHeight() - telaY;

		jogador.inv.aoArrastar(telaX, y, ponteiro);

		if(ponteiro == pontoDir && !jogador.inv.aberto) {
			float dx = telaX - ultimaDir.x;
			float dy = y - ultimaDir.y;
			jogador.yaw -= dx * sensi;
			jogador.tom += dy * sensi;
			if(jogador.tom > 89f) jogador.tom = 89f;
			if(jogador.tom < -89f) jogador.tom = -89f;
			ultimaDir.set(telaX, y);
		}



		if(toques.containsKey(ponteiro)) {
			CharSequence botaoAntigo = toques.get(ponteiro);
			boolean sobreBotao = false;
			for(Botao e : botoes.values()) {
				if(e.hitbox.contains(telaX, y)) {
					sobreBotao = true;
					if(e.nome.equals(botaoAntigo))continue;

					if(botaoAntigo == null)continue;

					for(Botao b : botoes.values()) 
						if(b.nome.equals(botaoAntigo)) {
							b.aoSoltar(telaX, y, ponteiro);
							break;
						}
					e.aoTocar(telaX, y, ponteiro);
					toques.put(ponteiro, e.nome);
					break;
				}
			}
			if(!sobreBotao && botaoAntigo != null) {
				for(Botao e : botoes.values()) {
					if(e.nome.equals(botaoAntigo)) {
						e.aoSoltar(telaX, y, ponteiro);
						break;
					}
				}
				toques.put(ponteiro, null);
			}
		}
		calcVisaoJogador(telaX, telaY);
	
		return true;
	}

	public void criarBotoesPadrao() {
		if(!botoes.isEmpty()) return;

		spriteMira = new Sprite(Texturas.texs.get("mira"));
		spriteMira.setSize(40f, 40f);

		float densidade = Gdx.graphics.getDensity();
		float tam = botaoTam * densidade;
		tam = MathUtils.clamp(tam, 50f, 150f);

		if(Gdx.app.getType() != com.badlogic.gdx.Application.ApplicationType.Desktop) {
			botoes.put("direita", new Botao(Texturas.texs.get("botao_d"), 0, 0, tam, tam, "direita") {
				public void aoTocar(int t, int t2, int p){ direita = true; sprite.setAlpha(0.5f); }
				public void aoSoltar(int t, int t2, int p){ direita = false; sprite.setAlpha(0.9f); }
			});
			botoes.put("esquerda", new Botao(Texturas.texs.get("botao_e"), 0, 0, tam, tam, "esquerda") {
				public void aoTocar(int t, int t2, int p){ esquerda = true; sprite.setAlpha(0.5f); }
				public void aoSoltar(int t, int t2, int p){ esquerda = false; sprite.setAlpha(0.9f); }
			});
			botoes.put("frente", new Botao(Texturas.texs.get("botao_f"), 0, 0, tam, tam, "frente") {
				public void aoTocar(int t, int t2, int p){ frente = true; sprite.setAlpha(0.5f); }
				public void aoSoltar(int t, int t2, int p){ frente = false; sprite.setAlpha(0.9f); }
			});
			botoes.put("tras", new Botao(Texturas.texs.get("botao_t"), 0, 0, tam, tam, "tras") {
				public void aoTocar(int t, int t2, int p){ tras = true; sprite.setAlpha(0.5f); }
				public void aoSoltar(int t, int t2, int p){ tras = false; sprite.setAlpha(0.9f); }
			});
			botoes.put("cima", new Botao(Texturas.texs.get("botao_f"), 0, 0, tam, tam, "cima") {
				public void aoTocar(int t, int t2, int p){ cima = true; sprite.setAlpha(0.5f); }
				public void aoSoltar(int t, int t2, int p){ cima = false; sprite.setAlpha(0.9f); }
			});
			botoes.put("baixo", new Botao(Texturas.texs.get("botao_t"), 0, 0, tam, tam, "baixo") {
				public void aoTocar(int t, int t2, int p){
					baixo = true; sprite.setAlpha(0.5f);
					if(jogador.agachado) {
						jogador.velo *= 2;
						jogador.altura *= 1.2f;
						jogador.agachado = false;
					} else {
						jogador.velo /= 2;
						jogador.altura /= 1.2f;
						jogador.agachado = true;
					}
				}
				public void aoSoltar(int t, int t2, int p){ baixo = false; sprite.setAlpha(0.9f); }
			});
			botoes.put("diagDireita", new Botao(Texturas.texs.get("botao_ld"), 0, 0, tam, tam, "diagDireita") {
				public void aoTocar(int t, int t2, int p){ frente = true; direita = true; sprite.setAlpha(0.5f); }
				public void aoSoltar(int t, int t2, int p){ frente = false; direita = false; sprite.setAlpha(0.9f); }
			});
			botoes.put("diagEsquerda", new Botao(Texturas.texs.get("botao_le"), 0, 0, tam, tam, "diagEsquerda") {
				public void aoTocar(int t, int t2, int p){ frente = true; esquerda = true; sprite.setAlpha(0.5f); }
				public void aoSoltar(int t, int t2, int p){ frente = false; esquerda = false; sprite.setAlpha(0.9f); }
			});
			botoes.put("acao", new Botao(Texturas.texs.get("clique"), 0, 0, tam, tam, "acao") {
				public void aoTocar(int t, int t2, int p){
					if(jogador.inv.itens[jogador.inv.slotSelecionado] != null) jogador.item = jogador.inv.itens[jogador.inv.slotSelecionado].nome;
					else jogador.item = "ar";
					acao = true;
					jogador.interagirBloco();
					if(jogador.inv.itens[jogador.inv.slotSelecionado] != null) jogador.item = jogador.inv.itens[jogador.inv.slotSelecionado].nome;
					else jogador.item = "ar";
					toques.put(p, "acao");
					sprite.setAlpha(0.5f);
				}
				public void aoSoltar(int t, int t2, int p){ acao = false; sprite.setAlpha(0.9f); }
			});
			botoes.put("ataque", new Botao(Texturas.texs.get("ataque"), 0, 0, tam, tam, "ataque") {
				public void aoTocar(int t, int t2, int p){
					jogador.item = "ar";
					jogador.interagirBloco();
					toques.put(p, "ataque");
					if(jogador.inv.itens[jogador.inv.slotSelecionado] != null) jogador.item = jogador.inv.itens[jogador.inv.slotSelecionado].nome;
					else jogador.item = "ar";
					sprite.setAlpha(0.5f);
				}
				public void aoSoltar(int t, int t2, int p){ acao = false; sprite.setAlpha(0.9f);}
			});
			botoes.put("inv", new Botao(Texturas.texs.get("clique"), 0, 0, jogador.inv.tamSlot, jogador.inv.tamSlot, "inv") {
				public void aoTocar(int t, int t2, int p){
					if(jogador.inv.itens[jogador.inv.slotSelecionado] != null) jogador.item = jogador.inv.itens[jogador.inv.slotSelecionado].nome;
					else jogador.item = "ar";
					jogador.inv.alternar();
					toques.put(p, "inv");
					sprite.setAlpha(0.5f);
				}
				public void aoSoltar(int t, int t2, int p){sprite.setAlpha(0.9f);}
			});
		}
		botoes.put("receita", new Botao(Texturas.texs.get("receita"), 0, 0, jogador.inv.tamSlot, jogador.inv.tamSlot, "receita") {
			public void aoTocar(int t, int t2, int p){
				if(jogador.inv.itens[jogador.inv.slotSelecionado] == null) return;
				if(jogador.inv.itens[jogador.inv.slotSelecionado].nome.equals("tronco")) {
					jogador.inv.rmItem(jogador.inv.slotSelecionado, 1);
					jogador.inv.addItem("tabua_madeira", 4);
					if(jogador.inv.itens[jogador.inv.slotSelecionado] != null) jogador.item = jogador.inv.itens[jogador.inv.slotSelecionado].nome;
					else jogador.item = "ar";
					Logs.log("feito tabua");
				} else if(jogador.inv.itens[jogador.inv.slotSelecionado].nome.equals("areia")) {
					jogador.inv.rmItem(jogador.inv.slotSelecionado, 1);
					jogador.inv.addItem("vidro", 1);
					if(jogador.inv.itens[jogador.inv.slotSelecionado] != null) jogador.item = jogador.inv.itens[jogador.inv.slotSelecionado].nome;
					else jogador.item = "ar";
					Logs.log("feito vidro");
				} else if(jogador.inv.itens[jogador.inv.slotSelecionado].nome.equals("folha")) {
					jogador.inv.rmItem(jogador.inv.slotSelecionado, 1);
					jogador.inv.addItem("tocha", 1);
					if(jogador.inv.itens[jogador.inv.slotSelecionado] != null) jogador.item = jogador.inv.itens[jogador.inv.slotSelecionado].nome;
					else jogador.item = "ar";
					Logs.log("feito tocha");
				}
				toques.put(p, "receita");
				sprite.setAlpha(0.5f);
			}
			public void aoSoltar(int t, int t2, int p){sprite.setAlpha(0.9f);}
		});
		botoes.put("menu_principal", new Botao(Texturas.texs.get("receita"), 0, 0, tam, tam, "menu_principal") {
			@Override
			public void aoTocar(int t, int t2, int p) {
				menuAberto = !menuAberto;
				toques.put(p, "menu_principal");
			}
			@Override
			public void aoSoltar(int t, int t2, int p) {}
		});
		menuOpcoes = new EstanteVertical("menuOpcoes", 0, 0, 15f);

		menuOpcoes.add(new Botao(Texturas.texs.get("salvar"), 0, 0, tam, tam, "botao_salvar") {
			@Override
			public void aoTocar(int t, int t2, int p) {
				Thread threadSalvar = new Thread(new Runnable() {
					@Override
					public void run() {
						ArquivosUtil.svMundo(Jogo.mundo, jogador);
						Gdx.app.postRunnable(new Runnable() {
							@Override
							public void run() {
								abrirDialogo("Jogo salvo com sucesso!");
							}
						});
					}
				});
				threadSalvar.start();
				menuAberto = false;
			}
		});
		menuOpcoes.add(new Botao(Texturas.texs.get("botao_opcao"), 0, 0, tam, tam, "botao_reiniciar_lua") {
			@Override
			public void aoTocar(int t, int t2, int p) {
				Logs.log("Sistema Lua reiniciado");
				LuaAPI.iniciar();
				menuAberto = false;
			}
		});
		menuOpcoes.add(new Botao(Texturas.texs.get("botao_d"), 0, 0, tam, tam, "botao_voltar_menu") {
			@Override
			public void aoTocar(int t, int t2, int p) {
				ArquivosUtil.svMundo(Jogo.mundo, jogador);
				menuAberto = false;
			}
		});
	}

	public void configDpad(int v, int h) {
		criarBotoesPadrao();

		float densidade = Gdx.graphics.getDensity();

		float tam = botaoTam * densidade;

		tam = MathUtils.clamp(tam, 50f, 150f);

		final float centroX = espaco + tam * 1.5f;
		final float centroY = espaco + tam * 1.5f;

		if(Gdx.app.getType() != com.badlogic.gdx.Application.ApplicationType.Desktop) {
			for(Botao b : botoes.values()) {
				if(b.nome.equals("direita")) {
					b.sprite.setAlpha(0.9f);
					b.sprite.setPosition(centroX + espaco, centroY - tam/2);
				} else if(b.nome.equals("esquerda")) {
					b.sprite.setAlpha(0.9f);
					b.sprite.setPosition(centroX - tam - espaco, centroY - tam/2);
				} else if(b.nome.equals("frente")) {
					b.sprite.setAlpha(0.9f);
					b.sprite.setPosition(centroX - tam/2, centroY + espaco);
				} else if(b.nome.equals("tras")) {
					b.sprite.setAlpha(0.9f);
					b.sprite.setPosition(centroX - tam/2, centroY - tam - espaco);
				} else if(b.nome.equals("cima")) {
					b.sprite.setAlpha(0.9f);
					b.sprite.setPosition(v - tam*1.5f, centroY + espaco);
				} else if(b.nome.equals("baixo")) {
					b.sprite.setAlpha(0.9f);
					b.sprite.setPosition(v - tam*1.5f, centroY - tam - espaco);
				} else if(b.nome.equals("diagDireita")) {
					b.sprite.setAlpha(0.9f);
					b.sprite.setPosition(centroX + espaco, centroY + espaco);
				} else if(b.nome.equals("diagEsquerda")) {
					b.sprite.setAlpha(0.9f);
					b.sprite.setPosition(centroX - tam - espaco, centroY + espaco);
				} else if(b.nome.equals("acao")) {
					b.sprite.setAlpha(0.9f);
					b.sprite.setPosition(v - tam*1.5f, centroY*2 + espaco);
				} else if(b.nome.equals("ataque")) {
					b.sprite.setAlpha(0.9f);
					b.sprite.setPosition(v - tam*2.5f, centroY*2 + espaco);
				} else if(b.nome.equals("inv")) {
					b.sprite.setAlpha(0.9f);
					int hotbarX = v / 2 - (jogador.inv.hotbarSlots * jogador.inv.tamSlot) / 2;
					int invX = hotbarX + ((jogador.inv.hotbarSlots) * jogador.inv.tamSlot);
					b.sprite.setPosition(invX, jogador.inv.hotbarY);
				} else if(b.nome.equals("receita")) {
					b.sprite.setAlpha(0.9f);
					b.sprite.setPosition(v - tam, h - tam);
				} else if(b.nome.equals("salvar")) {
					b.sprite.setAlpha(0.9f);
					b.sprite.setPosition((v - tam)-tam, h - tam);
				}
				b.hitbox.setPosition(b.sprite.getX(), b.sprite.getY());
			}
		}
		spriteMira.setPosition(v / 2 - spriteMira.getWidth() / 2, h / 2 - spriteMira.getHeight() / 2);
		spriteMira.setAlpha(0.9f);
	}

	public void att(float delta, Mundo mundo) {
		attCamera(camera, jogador.yaw, jogador.tom);

		frenteV.x = camera.direction.x;
		frenteV.z = camera.direction.z;
		frenteV.nor();  
		direitaV.x = frenteV.z;
		direitaV.z = -frenteV.x;

		jogador.velocidade.x = 0;
		jogador.velocidade.z = 0;
		if(jogador.modo != 2) jogador.velocidade.y = 0;

		if(this.frente) jogador.velocidade.add(frenteV.cpy().scl(jogador.velo));
		if(this.tras)  jogador.velocidade.sub(frenteV.cpy().scl(jogador.velo));
		if(this.esquerda) jogador.velocidade.add(direitaV.cpy().scl(jogador.velo));
		if(this.direita) jogador.velocidade.sub(direitaV.cpy().scl(jogador.velo));
		if(this.cima) {
			if(jogador.modo != 2 || jogador.noChao || jogador.naAgua) {
				jogador.velocidade.y = jogador.pulo; // pulo
				jogador.noChao = false;
			}
		}
		if(this.baixo) jogador.velocidade.y = -10f;

		Gdx.gl.glActiveTexture(GL20.GL_TEXTURE1);
		Gdx.gl.glBindTexture(GL20.GL_TEXTURE_2D, 0); 
		Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0); 

		camera.update();
		sb.begin();

		spriteMira.draw(sb);

		for(Botao e : botoes.values()) {
			e.porFrame(delta, sb, fonte);
		}
		for(Texto e : textos.values()) {
			e.porFrame(delta, sb, fonte);
		}
		dialogo.porFrame(delta, sb, fonte);

		if(menuAberto) menuOpcoes.porFrame(delta, sb, fonte);

		this.jogador.inv.att();


		if(Gdx.app.getType() == com.badlogic.gdx.Application.ApplicationType.Desktop && !jogador.inv.aberto) {
			jogadorInterageComBloco();
		}

		if(debug) {
			float livre = rt.freeMemory() >> 20;
			float total = rt.totalMemory() >> 20;

			float nativaLivre = debugador.obterHeapLivre() >> 20;
			float nativaTotal = debugador.obterHeapTotal() >> 20;

			fps = Gdx.graphics.getFramesPerSecond();

			String[] logsArr = Logs.logs.split("\n");
			StringBuilder construtorLogs = new StringBuilder();

			int inicio = Math.max(0, logsArr.length - 15);
			for(int i = inicio; i < logsArr.length; i++) {
				construtorLogs.append(logsArr[i]).append("\n");
			}
			String logsTexto = construtorLogs.toString();

			fonte.draw(sb, String.format(
						"X: %.1f, Y: %.1f, Z: %.1f\n" +
						"Mundo: %s\nJogador:\nModo: %s\nSlot: %d\nItem: %s\nNo chão: %b\nNa água: %b\nAgachado: %b\n\nStatus:\nVelocidade: %.2f\nAltura: %.2f\n\n" +
						"Controles:\nDireita: %b, Esquerda: %b\nFrente: %b, Trás: %b\nCima: %b\nBaixo: %b\nAção: %b\n\n" +
						"Mundo:\nRaio Chunks: %d\nChunks ativos: %d\nChunks Alteradas: %d\nSemente: %d\nTempo: %.2f\nTick: %.3f\nVelocidade do tempo: %.5f",
						jogador.posicao.x, jogador.posicao.y, jogador.posicao.z,
						mundo.nome, 
						(jogador.modo == 0 ? "espectador" : jogador.modo == 1 ? "criativo" : "sobrevivencia"), 
						jogador.inv.slotSelecionado, jogador.item, jogador.noChao, jogador.naAgua, jogador.agachado, jogador.velo, jogador.altura,
						this.direita, this.esquerda, this.frente, this.tras, this.cima, this.baixo, this.acao,
						mundo.RAIO_CHUNKS, mundo.chunks.size(), mundo.chunksMod.size(), mundo.semente, DiaNoiteUtil.tempo, mundo.tick, DiaNoiteUtil.tempo_velo), 
					50, Gdx.graphics.getHeight() - 100);

			fonte.draw(sb, String.format(
						"FPS: %d\n" +
						"Threads ativas: %d\nMemória livre: %.1f MB\nMemória total: %.1f MB\nMemória usada: %.1f MB\nMemória nativa livre: %.1f MB\nMemória nativa total: %.1f MB\nMemória nativa usada: %.1f MB\n" +
						"Otimizador C: %s\n\n"+
						"Logs:\n%s",
						fps,
						Thread.activeCount(), livre, total, total - livre, nativaLivre, nativaTotal, nativaTotal - nativaLivre,
						otimizadorC,
						logsTexto), 
					Gdx.graphics.getWidth() - 300, Gdx.graphics.getHeight() - 100);
		}
		sb.end();
	}

	public void ajustar(int v, int h) {
		for(Botao b : botoes.values()) {
			if(b != null) b.aoAjustar(v, h);
		}
		configDpad(v, h);

		camera.viewportWidth = v;
		camera.viewportHeight = h;
		camera.update();

		jogador.inv.aoAjustar(v, h);

		sb.getProjectionMatrix().setToOrtho2D(0, 0, v, h);
	}

	public Texto addTexto(String nome, String texto, int x, int y) {
		textos.put(nome, new Texto(texto, x, y) {
			@Override
			public void aoAjustar(int v, int h) {}
		});
		return textos.get(nome);
	}

	public Texto addTexto(String nome, String texto, int x, int y, final LuaFunction func) {
		textos.put(nome, new Texto(texto, x, y) {
			@Override
			public void aoAjustar(int v, int h) {
				func.call(LuaValue.valueOf(v), LuaValue.valueOf(h));
			}
		});
		return textos.get(nome);
	}

	public void defTextoTam(int escalaX, int escalaY) {
		fonte.getData().setScale(escalaX, escalaY);
	}

	public float[] obterTextoTam(int escalaX, int escalaY) {
		BitmapFont.BitmapFontData f = fonte.getData();
		return new float[]{f.scaleX, f.scaleY};
	}

	public Botao addBotao(String textura, float x, float y, float escalaX, float escalaY, String nome, final LuaFunction func) {
		botoes.put(nome, new Botao(Texturas.texs.get(textura), x, y, escalaX, escalaY, nome) {
			@Override
			public void aoTocar(int telaX, int telaY, int p) {
				func.call();
			}
			@Override
			public void aoSoltar(int telaX, int telaY, int p) {}
		});
		return botoes.get(nome);
	}
	public Botao addBotao(String textura, float x, float y, float escalaX, float escalaY, String nome, final LuaFunction func, final LuaFunction func2) {
		botoes.put(nome, new Botao(Texturas.texs.get(textura), x, y, escalaX, escalaY, nome) {
			@Override
			public void aoTocar(int telaX, int telaY, int p) {
				func.call();
			}
			@Override
			public void aoSoltar(int telaX, int telaY, int p) {
				func2.call();
			}
		});
		return botoes.get(nome);
	}
	public Botao addBotao(String textura, float x, float y, float escalaX, float escalaY, String nome, final LuaFunction func, final LuaFunction func2, final LuaFunction func3) {
		botoes.put(nome, new Botao(Texturas.texs.get(textura), x, y, escalaX, escalaY, nome) {
			@Override
			public void aoTocar(int telaX, int telaY, int p) {
				func.call();
			}
			@Override
			public void aoSoltar(int telaX, int telaY, int p) {
				func2.call();
			}
			@Override
			public void aoAjustar(int v, int h) {
				func3.call(LuaValue.valueOf(v), LuaValue.valueOf(h));
			}
		});
		return botoes.get(nome);
	}

	public Botao addBotao(Sprite sprite, float x, float y, float escalaX, float escalaY, String nome, final LuaFunction func, final LuaFunction func2, final LuaFunction func3) {
		botoes.put(nome, new Botao(sprite, x, y, escalaX, escalaY, nome) {
			@Override
			public void aoTocar(int telaX, int telaY, int p) {
				func.call();
			}
			@Override
			public void aoSoltar(int telaX, int telaY, int p) {
				func2.call();
			}
			@Override
			public void aoAjustar(int v, int h) {
				func3.call(LuaValue.valueOf(v), LuaValue.valueOf(h));
			}
		});
		return botoes.get(nome);
	}

	@Override 
	public boolean keyDown(int p) {
		if(modoTexto) return true;
		if(p == Input.Keys.W) frente = true;
		if(p == Input.Keys.S) tras = true;
		if(p == Input.Keys.A) esquerda = true;
		if(p == Input.Keys.D) direita = true;
		if(p == Input.Keys.SPACE) cima = true;
		if(p == Input.Keys.SHIFT_LEFT) {
			baixo = true;
			if(jogador.agachado) {
				jogador.velo *= 2;
				jogador.altura *= 1.2f;
				jogador.agachado = false;
			} else {
				jogador.velo /= 2;
				jogador.altura /= 1.2f;
				jogador.agachado = true;
			}
		}
		if(p == Input.Keys.E) {
			if(jogador.inv.aberto) {
				Gdx.input.setCursorCatched(true); 
			} else {
				Gdx.input.setCursorCatched(false); 
			}
			jogador.inv.alternar();
		}
		if(p == Input.Keys.F1) {
			if(debug) {
				debug = false; 
			} else {
				debug = true;
			}
		}
		if(p == Input.Keys.T) abrirChat();
		if(p == Input.Keys.P) {
			if(Mundo.debugColisao) {
				Mundo.debugColisao = false;
			} else {
				Mundo.debugColisao = true;
			}
		}
		return true;
	}

	@Override 
	public boolean keyUp(int p) {
		if(modoTexto) return true;
		if(p == Input.Keys.W) frente = false;
		if(p == Input.Keys.S) tras = false;
		if(p == Input.Keys.A) esquerda = false;
		if(p == Input.Keys.D) direita = false;
		if(p == Input.Keys.SPACE) cima = false;
		if(p == Input.Keys.SHIFT_LEFT) baixo = false;
		return true;
	}

	@Override 
	public boolean mouseMoved(int telaX, int telaY) {
		if(modoTexto) return true;

		calcVisaoJogador(telaX, telaY);
		return true;
	}

	@Override 
	public boolean scrolled(float p, float p1) {
		if(p1 > 0) jogador.inv.slotSelecionado = (jogador.inv.slotSelecionado + 1) % jogador.inv.hotbarSlots;
		else if(p1 < 0) jogador.inv.slotSelecionado = (jogador.inv.slotSelecionado - 1 + jogador.inv.hotbarSlots) % jogador.inv.hotbarSlots;
		return true;
	}
	@Override
	public boolean keyTyped(char p) {
		if(p == '\n' || p == '\r') modoTexto = false;
		dialogo.digitando(p);
		return false;
	}
}
