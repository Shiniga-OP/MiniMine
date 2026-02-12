package com.minimine.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.InputProcessor;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import com.minimine.graficos.Texturas;
import com.minimine.Debugador;
import org.luaj.vm2.LuaFunction;
import java.util.Map;
import org.luaj.vm2.LuaValue;
import com.minimine.utils.DiaNoiteUtil;
import com.minimine.ui.InterUtil;
import com.badlogic.gdx.Input;
import com.minimine.utils.ArquivosUtil;
import com.minimine.Inicio;
import com.minimine.Logs;
import com.minimine.mundo.Mundo;
import com.minimine.entidades.Inventario;
import com.minimine.cenas.Jogo;
import com.minimine.mods.LuaAPI;
import com.minimine.entidades.Jogador;

public class UI implements InputProcessor {
	public static PerspectiveCamera camera;
	public static Map<CharSequence, Botao> botoes = new HashMap<>();
	public static Map<CharSequence, Texto> textos = new HashMap<>();
	public static Dialogo dialogo = new Dialogo();
    public static SpriteBatch sb;
    public static BitmapFont fonte;
    public static CharSequence otimizadorC = "desligado";

	public Sprite spriteMira;
	public int pontoEsq = -1;
    public int pontoDir = -1;
    public final Vector2 esqCentro = new Vector2();
    public final Vector2 esqPos = new Vector2();
    public final Vector2 ultimaDir = new Vector2();

	public static float sensi = 0.25f;
	public static float aprox = 0.01f;
	public static float distancia = 400f;
	public static int pov = 90;
	public static final HashMap<Integer, CharSequence> toques = new HashMap<>();
	public static Runtime rt = Runtime.getRuntime();

	public static float botaoTam = 70f;
	public static float espaco = 60f;

	public static Jogador jg;
	public static boolean debug = true;
	public static boolean modoTexto = false;
	public static int fps = 0;
	public static Debugador debugador;
	
	public static EstanteVertical menuOpcoes;
	public static boolean menuAberto = false;
	
	public boolean chatAberto = false;
	public String ultimaMensagem = "";
	public List<String> msgs = new ArrayList<String>();

    public UI(Jogador jg) {
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
		this.jg = jg;
		this.jg.camera = camera;
		this.jg.inv = new Inventario(jg);

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
						msgs.add("> " + dialogo.texto);
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

	@Override
    public boolean touchDown(int telaX, int telaY, int p, int b) {
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
						bo.aoTocar(telaX, y, p);
						return true;
					}
				}
			}
		}
        if(Gdx.app.getType() == com.badlogic.gdx.Application.ApplicationType.Desktop && !jg.inv.aberto) {
            if(b == Input.Buttons.LEFT) {
                jg.item = "ar";
                jg.interagirBloco();
                if(jg.inv.itens[jg.inv.slotSelecionado] != null) jg.item = jg.inv.itens[jg.inv.slotSelecionado].nome;
                else jg.item = "ar";
                return true;
            }
            if(b == Input.Buttons.RIGHT) {
                if(jg.inv.itens[jg.inv.slotSelecionado] != null) jg.item = jg.inv.itens[jg.inv.slotSelecionado].nome;
                else jg.item = "ar";
                jg.acao = true;
                jg.interagirBloco();
                return true;
            }
        }
        for(Botao e : botoes.values()) {
            if(e.hitbox.contains(telaX, y)) {
                e.aoTocar(telaX, y, p);
                toques.put(p, e.nome);
                return true;
            }
        }
        jg.inv.aoTocar(telaX, y, p);
        if(pontoDir == -1) { 
			pontoDir = p; 
			ultimaDir.set(telaX, y); 
		}
        return true;
    }

    @Override
    public boolean touchUp(int telaX, int telaY, int p, int b) {
		if(modoTexto) return true;
        if(b == Input.Buttons.RIGHT) jg.acao = false;

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
        if(p == pontoDir) pontoDir = -1;
        return true;
    }

	@Override
	public boolean touchDragged(int telaX, int telaY, int p) {
		if(modoTexto) return true;
		int y = Gdx.graphics.getHeight() - telaY;

        jg.inv.aoArrastar(telaX, y, p);

		if(p == pontoDir && !jg.inv.aberto) {
			float dx = telaX - ultimaDir.x;
			float dy = y - ultimaDir.y;
			jg.yaw -= dx * sensi;
			jg.tom += dy * sensi;
			if(jg.tom > 89f) jg.tom = 89f;
			if(jg.tom < -89f) jg.tom = -89f;
			ultimaDir.set(telaX, y);
		}
		if(toques.containsKey(p)) {
            CharSequence botaoAntigo = toques.get(p);
			boolean sobreBotao = false;
			for(Botao e : botoes.values()) {
				if(e.hitbox.contains(telaX, y)) {
					sobreBotao = true;
					if(!e.nome.equals(botaoAntigo)) {
						if(botaoAntigo != null) {
							for(Botao b : botoes.values()) {
								if(b.nome.equals(botaoAntigo)) {
									b.aoSoltar(telaX, y, p);
									break;
								}
							}
						}
						e.aoTocar(telaX, y, p);
						toques.put(p, e.nome);
					}
					break;
				}
			}
			if(!sobreBotao && botaoAntigo != null) {
				for(Botao e : botoes.values()) {
					if(e.nome.equals(botaoAntigo)) {
						e.aoSoltar(telaX, y, p);
						break;
					}
				}
				toques.put(p, null);
			}
		}
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
					public void aoTocar(int t, int t2, int p){ jg.direita = true; sprite.setAlpha(0.5f); }
					public void aoSoltar(int t, int t2, int p){ jg.direita = false; sprite.setAlpha(0.9f); }
				});
			botoes.put("esquerda", new Botao(Texturas.texs.get("botao_e"), 0, 0, tam, tam, "esquerda") {
					public void aoTocar(int t, int t2, int p){ jg.esquerda = true; sprite.setAlpha(0.5f); }
					public void aoSoltar(int t, int t2, int p){ jg.esquerda = false; sprite.setAlpha(0.9f); }
				});
			botoes.put("frente", new Botao(Texturas.texs.get("botao_f"), 0, 0, tam, tam, "frente") {
					public void aoTocar(int t, int t2, int p){ jg.frente = true; sprite.setAlpha(0.5f); }
					public void aoSoltar(int t, int t2, int p){ jg.frente = false; sprite.setAlpha(0.9f); }
				});
			botoes.put("tras", new Botao(Texturas.texs.get("botao_t"), 0, 0, tam, tam, "tras") {
					public void aoTocar(int t, int t2, int p){ jg.tras = true; sprite.setAlpha(0.5f); }
					public void aoSoltar(int t, int t2, int p){ jg.tras = false; sprite.setAlpha(0.9f); }
				});
			botoes.put("cima", new Botao(Texturas.texs.get("botao_f"), 0, 0, tam, tam, "cima") {
					public void aoTocar(int t, int t2, int p){ jg.cima = true; sprite.setAlpha(0.5f); }
					public void aoSoltar(int t, int t2, int p){ jg.cima = false; sprite.setAlpha(0.9f); }
				});
			botoes.put("baixo", new Botao(Texturas.texs.get("botao_t"), 0, 0, tam, tam, "baixo") {
					public void aoTocar(int t, int t2, int p){
						jg.baixo = true;
						sprite.setAlpha(0.5f);
						if(jg.agachado) {
							jg.velo *= 2;
							jg.altura *= 1.2f;
							jg.agachado = false;
						} else {
							jg.velo /= 2;
							jg.altura /= 1.2f;
							jg.agachado = true;
						}
					}
					public void aoSoltar(int t, int t2, int p){ jg.baixo = false; sprite.setAlpha(0.9f); }
				});
			botoes.put("diagDireita", new Botao(Texturas.texs.get("botao_ld"), 0, 0, tam, tam, "diagDireita") {
					public void aoTocar(int t, int t2, int p){ jg.frente = true; jg.direita = true; sprite.setAlpha(0.5f); }
					public void aoSoltar(int t, int t2, int p){ jg.frente = false; jg.direita = false; sprite.setAlpha(0.9f); }
				});
			botoes.put("diagEsquerda", new Botao(Texturas.texs.get("botao_le"), 0, 0, tam, tam, "diagEsquerda") {
					public void aoTocar(int t, int t2, int p){ jg.frente = true; jg.esquerda = true; sprite.setAlpha(0.5f); }
					public void aoSoltar(int t, int t2, int p){ jg.frente = false; jg.esquerda = false; sprite.setAlpha(0.9f); }
				});
			botoes.put("acao", new Botao(Texturas.texs.get("clique"), 0, 0, tam, tam, "acao") {
					public void aoTocar(int t, int t2, int p){
						if(jg.inv.itens[jg.inv.slotSelecionado] != null) jg.item = jg.inv.itens[jg.inv.slotSelecionado].nome;
						else jg.item = "ar";
						jg.acao = true;
						jg.interagirBloco();
						if(jg.inv.itens[jg.inv.slotSelecionado] != null) jg.item = jg.inv.itens[jg.inv.slotSelecionado].nome;
						else jg.item = "ar";
						toques.put(p, "acao");
						sprite.setAlpha(0.5f);
					}
					public void aoSoltar(int t, int t2, int p){ jg.acao = false; sprite.setAlpha(0.9f); }
				});
			botoes.put("ataque", new Botao(Texturas.texs.get("ataque"), 0, 0, tam, tam, "ataque") {
					public void aoTocar(int t, int t2, int p){
						jg.item = "ar";
						jg.interagirBloco();
						toques.put(p, "ataque");
						if(jg.inv.itens[jg.inv.slotSelecionado] != null) jg.item = jg.inv.itens[jg.inv.slotSelecionado].nome;
						else jg.item = "ar";
						sprite.setAlpha(0.5f);
					}
					public void aoSoltar(int t, int t2, int p){ jg.acao = false; sprite.setAlpha(0.9f);}
				});
			botoes.put("inv", new Botao(Texturas.texs.get("clique"), 0, 0, jg.inv.tamSlot, jg.inv.tamSlot, "inv") {
					public void aoTocar(int t, int t2, int p){
						if(jg.inv.itens[jg.inv.slotSelecionado] != null) jg.item = jg.inv.itens[jg.inv.slotSelecionado].nome;
						else jg.item = "ar";
						jg.inv.alternar();
						toques.put(p, "inv");
						sprite.setAlpha(0.5f);
					}
					public void aoSoltar(int t, int t2, int p){sprite.setAlpha(0.9f);}
				});
		}
		botoes.put("receita", new Botao(Texturas.texs.get("receita"), 0, 0, jg.inv.tamSlot, jg.inv.tamSlot, "receita") {
				public void aoTocar(int t, int t2, int p){
					if(jg.inv.itens[jg.inv.slotSelecionado] == null) return;
					if(jg.inv.itens[jg.inv.slotSelecionado].nome.equals("tronco")) {
						jg.inv.rmItem(jg.inv.slotSelecionado, 1);
						jg.inv.addItem("tabua_madeira", 4);
						if(jg.inv.itens[jg.inv.slotSelecionado] != null) jg.item = jg.inv.itens[jg.inv.slotSelecionado].nome;
						else jg.item = "ar";
						Logs.log("feito tabua");
					} else if(jg.inv.itens[jg.inv.slotSelecionado].nome.equals("areia")) {
						jg.inv.rmItem(jg.inv.slotSelecionado, 1);
						jg.inv.addItem("vidro", 1);
						if(jg.inv.itens[jg.inv.slotSelecionado] != null) jg.item = jg.inv.itens[jg.inv.slotSelecionado].nome;
						else jg.item = "ar";
						Logs.log("feito vidro");
					} else if(jg.inv.itens[jg.inv.slotSelecionado].nome.equals("folha")) {
						jg.inv.rmItem(jg.inv.slotSelecionado, 1);
						jg.inv.addItem("tocha", 1);
						if(jg.inv.itens[jg.inv.slotSelecionado] != null) jg.item = jg.inv.itens[jg.inv.slotSelecionado].nome;
						else jg.item = "ar";
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
								ArquivosUtil.svMundo(Jogo.mundo, jg);
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
					ArquivosUtil.svMundo(Jogo.mundo, jg);
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
					int hotbarX = v / 2 - (jg.inv.hotbarSlots * jg.inv.tamSlot) / 2;
					int invX = hotbarX + ((jg.inv.hotbarSlots) * jg.inv.tamSlot);
					b.sprite.setPosition(invX, jg.inv.hotbarY);
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
		attCamera(camera, jg.yaw, jg.tom);

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
		
		this.jg.inv.att();
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
						   "Mundo: %s\njg:\nModo: %s\nSlot: %d\nItem: %s\nNo chão: %b\nNa água: %b\nAgachado: %b\n\nStatus:\nVelocidade: %.2f\nAltura: %.2f\n\n" +
						   "Controles:\nDireita: %b, Esquerda: %b\nFrente: %b, Trás: %b\nCima: %b\nBaixo: %b\nAção: %b\n\n" +
						   "Mundo:\nRaio Chunks: %d\nChunks ativos: %d\nChunks Alteradas: %d\nSemente: %d\nTempo: %.2f\nTick: %.3f\nVelocidade do tempo: %.5f",
						   jg.posicao.x, jg.posicao.y, jg.posicao.z,
						   mundo.nome, 
						   (jg.modo == 0 ? "espectador" : jg.modo == 1 ? "criativo" : "sobrevivencia"), 
						   jg.inv.slotSelecionado, jg.item, jg.noChao, jg.naAgua, jg.agachado, jg.velo, jg.altura,
						   jg.direita, jg.esquerda, jg.frente, jg.tras, jg.cima, jg.baixo, jg.acao,
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

	public static void attCamera(PerspectiveCamera camera, float yaw, float tom) {
		float yawRad = yaw * MathUtils.degRad;
		float tomRad = tom * MathUtils.degRad;

		float cx = MathUtils.cos(tomRad) * MathUtils.sin(yawRad);
		float cy = MathUtils.sin(tomRad);
		float cz = MathUtils.cos(tomRad) * MathUtils.cos(yawRad);

		camera.direction.set(cx, cy, cz).nor();
		camera.up.set(0, 1, 0);
	}

    public void ajustar(int v, int h) {
		for(Botao b : botoes.values()) {
			if(b != null) b.aoAjustar(v, h);
		}
		configDpad(v, h);

        camera.viewportWidth = v;
        camera.viewportHeight = h;
        camera.update();

		jg.inv.aoAjustar(v, h);

        sb.getProjectionMatrix().setToOrtho2D(0, 0, v, h);
    }

    public static void liberar() {
		for(Botao e : botoes.values()) {
			e.aoFim();
		}
        sb.dispose();
        fonte.dispose();
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

	@Override 
    public boolean keyDown(int p) {
		if(modoTexto) return true;
        if(p == Input.Keys.W) jg.frente = true;
        if(p == Input.Keys.S) jg.tras = true;
        if(p == Input.Keys.A) jg.esquerda = true;
        if(p == Input.Keys.D) jg.direita = true;
        if(p == Input.Keys.SPACE) jg.cima = true;
        if(p == Input.Keys.SHIFT_LEFT) {
            jg.baixo = true;
            if(jg.agachado) {
                jg.velo *= 2;
                jg.altura *= 1.2f;
                jg.agachado = false;
            } else {
                jg.velo /= 2;
                jg.altura /= 1.2f;
                jg.agachado = true;
            }
        }
        if(p == Input.Keys.E) {
			if(jg.inv.aberto) {
				Gdx.input.setCursorCatched(true); 
			} else {
				Gdx.input.setCursorCatched(false); 
			}
			jg.inv.alternar();
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
        if(p == Input.Keys.W) jg.frente = false;
        if(p == Input.Keys.S) jg.tras = false;
        if(p == Input.Keys.A) jg.esquerda = false;
        if(p == Input.Keys.D) jg.direita = false;
        if(p == Input.Keys.SPACE) jg.cima = false;
        if(p == Input.Keys.SHIFT_LEFT) jg.baixo = false;
        return true;
    }

    @Override 
    public boolean mouseMoved(int p, int p1) {
		if(modoTexto) return true;

		int y = Gdx.graphics.getHeight() - p1;
		jg.inv.aoArrastar(p, y, -1);

		if(!jg.inv.aberto) {
			float dx = Gdx.input.getDeltaX();
			float dy = Gdx.input.getDeltaY();
			jg.yaw -= dx * sensi;
			jg.tom -= dy * sensi;
			if(jg.tom > 89f) jg.tom = 89f;
			if(jg.tom < -89f) jg.tom = -89f;
		}
        return true;
    }

    @Override 
    public boolean scrolled(float p, float p1) {
        if(p1 > 0) jg.inv.slotSelecionado = (jg.inv.slotSelecionado + 1) % jg.inv.hotbarSlots;
        else if(p1 < 0) jg.inv.slotSelecionado = (jg.inv.slotSelecionado - 1 + jg.inv.hotbarSlots) % jg.inv.hotbarSlots;
        return true;
    }
	@Override
	public boolean keyTyped(char p) {
		if(p == '\n' || p == '\r') modoTexto = false;
		dialogo.digitando(p);
		return false;
	}
}
