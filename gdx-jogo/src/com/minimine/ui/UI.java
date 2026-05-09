package com.minimine.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Input;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

import com.minimine.graficos.Texturas;
import com.minimine.Debugador;
import com.minimine.Logs;
import com.minimine.mundo.Mundo;
import com.minimine.mundo.blocos.Bloco;
import com.minimine.inventario.Inventario;
import com.minimine.entidades.Jogador;
import com.minimine.utils.DiaNoiteUtil;
import com.minimine.mundo.blocos.InterfaceBloco;
import com.minimine.inventario.PaginaItens;

import com.micro.util.GerenciadorUI;
import com.micro.componentes.CaixaDialogo;
import com.micro.componentes.CampoTexto;
import com.micro.componentes.Rotulo;
import com.micro.janelas.PainelFatiado;
import com.micro.util.Acao;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.graphics.profiling.GLProfiler;
import com.minimine.cenas.Jogo;
import com.badlogic.gdx.utils.TimeUtils;
import com.minimine.inventario.ItemRegistro;
import com.minimine.inventario.Item;
import com.micro.util.Ancora;

public class UI implements InputProcessor {
    // camera 3D e renderização
    public static PerspectiveCamera camera;
    public static SpriteBatch sb;
    public static BitmapFont fonte;

    public static float sensi = 0.25f;
    public static float aprox = 0.01f;
    public static float distancia = 400f;
    public static int pov = 90;
	
	public static int telaV, telaH;

    // botões do DPad mobile
    public static final HashMap<String, BotaoDpad> botoesDpad = new HashMap<>();
    public static final HashMap<Integer, String> toquesDpad = new HashMap<>();

    public static int botoesTam = 48; // 48dp tamanho fisico minimo confortavel pro dedo

    public Sprite spriteMira;
    public int pontoDir = -1;
    public final Vector2 ultimaDir = new Vector2();

    public static GerenciadorUI gerenciador;
    public static PainelFatiado visualBase;

    // dialogo de chat/alertas genericos
    public static CaixaDialogo dialogoChat;
    public static CampoTexto campoChatTexto;

    // rotulos dinamicos acessiveis externamente
    public static final HashMap<String, Rotulo> rotulos = new HashMap<>();

    // estado
    public static Jogador jg;
    public static boolean debug = false;
    public static boolean modoTexto = false;
    public static int fps = 0;
    public static Debugador debugador;

    public boolean chatAberto = false;
    public String ultimaMensagem = "";
    public List<String> msgs = new ArrayList<>();

    public PaginaItens paginaItens = new PaginaItens();

    public static Runtime rt = Runtime.getRuntime();
	public static GLProfiler gpu;

    public static boolean gui = true;

    public UI(Jogador jogador) {
        camera = new PerspectiveCamera(pov, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.position.set(10f, 18f, 10f);
        camera.lookAt(0, 0, 0);
        camera.near = aprox;
        camera.far = distancia;
        camera.update();

        sb = new SpriteBatch();
        fonte = InterUtil.carregarFonte("fontes/pixel.ttf");

        this.jg = jogador;
        this.jg.camera = camera;

        gerenciador = new GerenciadorUI();
        visualBase = new PainelFatiado(Texturas.base);

        criarDialogos();
        configDpad(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

		MenuPause.iniciar();
        MenuPause.sr = new ShapeRenderer();

        // inicia as interfaces dos blocos agora que visualBase e fonte estão prontos
        Bloco.iniciarInterfaces(visualBase, fonte);

        Gdx.input.setInputProcessor(this);
        Gdx.input.setCursorCatched(true);
		gpu = new GLProfiler(Gdx.graphics);
		if(debug) gpu.enable();
		else gpu.disable();
    }

    // === criação de componentes Micro ===

    // cria o diálogo de chat e os dialogos de alerta simples
    public void criarDialogos() {
        // dialogo de chat
        dialogoChat = new CaixaDialogo(visualBase, fonte, 3f, new ShapeRenderer()); // ShapeRenderer proprio do dialogo
        dialogoChat.largura = 500;
        dialogoChat.altura  = 200;

        campoChatTexto = new CampoTexto(visualBase, fonte, 30, 80, 440, 50, 3f);
        campoChatTexto.padrao = "Digite sua mensagem...";
        campoChatTexto.limiteCaracteres = 128;
        dialogoChat.add(campoChatTexto);

        Acao acaoEnviar = new Acao() {
            @Override public void exec() {
                String msg = campoChatTexto.texto.trim();
                if(!msg.isEmpty()) {
                    ultimaMensagem = msg;
                    msgs.add("> " + msg);
                    Gdx.app.log("CHAT", msg);
                }
                campoChatTexto.texto = "";
                dialogoChat.fechar(true);
                chatAberto = false;
                modoTexto  = false;
                Gdx.input.setCursorCatched(true);
            }
        };
        Acao acaoCancelar = new Acao() {
            @Override public void exec() {
                campoChatTexto.texto = "";
                dialogoChat.fechar(false);
                chatAberto = false;
                modoTexto  = false;
                Gdx.input.setCursorCatched(true);
            }
        };
        dialogoChat.addBotao("Enviar",   visualBase, Ancora.CENTRO_DIREITO,  -10, acaoEnviar);
        dialogoChat.addBotao("Cancelar", visualBase, Ancora.CENTRO_ESQUERDO,  10, acaoCancelar);

        gerenciador.addDialogo(dialogoChat);
    }

    // chat
    public void abrirChat() {
        if(chatAberto) return;
        chatAberto = true;
        modoTexto = true;
        Gdx.input.setCursorCatched(false);

        float cx = Gdx.graphics.getWidth()  / 2f - dialogoChat.largura / 2f;
        float cy = Gdx.graphics.getHeight() / 2f - dialogoChat.altura  / 2f;
        dialogoChat.x = cx;
        dialogoChat.y = cy;

        dialogoChat.mostrar("Chat", "", new CaixaDialogo.Fechar() {
				@Override
				public void aoFechar(boolean confirmou) {
					chatAberto = false;
					modoTexto = false;
					Gdx.input.setCursorCatched(true);
				}
			});
    }

    // abre um dialogo de aviso com padrão ao fechar
    public static void abrirDialogo(String titulo, final CaixaDialogo.Fechar fechar) {
        final CaixaDialogo alerta = new CaixaDialogo(visualBase, fonte, 3f, new ShapeRenderer());
        alerta.largura = 400;
        alerta.altura = 160;
        alerta.x = Gdx.graphics.getWidth() / 2f - alerta.largura / 2f;
        alerta.y = Gdx.graphics.getHeight() / 2f - alerta.altura  / 2f;
        alerta.addOk(visualBase);
        gerenciador.addDialogo(alerta);
		Gdx.input.setCursorCatched(false);
        alerta.mostrar(titulo, "", fechar != null ? fechar : new CaixaDialogo.Fechar(){@Override public void aoFechar(boolean c){Gdx.input.setCursorCatched(true);}});
    }

    // dpad(sprites, texturas direcionais não fazem sentido na Micro)
    public void criarBotoesDpad() {
        if(!botoesDpad.isEmpty()) return;

        TextureRegion mira = Texturas.atlas.get("mira");
        spriteMira = mira != null ? new Sprite(mira) : null;
        if(spriteMira != null) spriteMira.setSize(40f, 40f);
        else Gdx.app.log("[UI]", "[ERRO]: A textura mira é null");

        if(Gdx.app.getType() == com.badlogic.gdx.Application.ApplicationType.Desktop) return;

        botoesDpad.put("direita", new BotaoDpad(Texturas.atlas.get("botao_d"),  0) {
				public void aoTocar(){jg.direita = true;}
				public void aoSoltar(){jg.direita = false;}
			});
        botoesDpad.put("esquerda", new BotaoDpad(Texturas.atlas.get("botao_e"), 0) {
				public void aoTocar(){jg.esquerda = true;}
				public void aoSoltar(){jg.esquerda = false;}
			});
        botoesDpad.put("frente", new BotaoDpad(Texturas.atlas.get("botao_f"), 0) {
				public void aoTocar(){jg.frente = true;}
				public void aoSoltar(){jg.frente = false;}
			});
        botoesDpad.put("tras", new BotaoDpad(Texturas.atlas.get("botao_t"), 0) {
				public void aoTocar(){jg.tras = true;}
				public void aoSoltar(){jg.tras = false;}
			});
        botoesDpad.put("cima", new BotaoDpad(Texturas.atlas.get("botao_f"), 0) {
				public void aoTocar() {
					jg.cima = true;
					if(jg.modo == 1) { // so conta pulo se estava no chão
						if(jg.tempoDuploPulo > 0f) {
							jg.voando = !jg.voando;
							if(jg.voando) jg.velocidade.y = 0;
							jg.tempoDuploPulo = 0f;
						} else {
							jg.tempoDuploPulo = Jogador.JANELA_DUPLO_PULO;
						}
					}
				}
				public void aoSoltar(){jg.cima = false;}
			});
        botoesDpad.put("diagDireita", new BotaoDpad(Texturas.atlas.get("botao_ld"), 0) {
				public void aoTocar(){jg.frente = jg.direita = true;}
				public void aoSoltar(){jg.frente = jg.direita = false;}
			});
        botoesDpad.put("diagEsquerda", new BotaoDpad(Texturas.atlas.get("botao_le"), 0) { public void aoTocar(){ jg.frente = jg.esquerda = true;  } public void aoSoltar(){ jg.frente = jg.esquerda = false; } });

        botoesDpad.put("baixo", new BotaoDpad(Texturas.atlas.get("botao_t"), 0) {
				public void aoTocar() {
					jg.baixo = true;
					if(jg.agachado) {
						jg.velo *= 2f;
						jg.altura *= 1.2f;
						jg.agachado = false;
					} else {
						jg.velo /= 2f; 
						jg.altura /= 1.2f;
						jg.agachado = true;
					}
				}
				public void aoSoltar() {
					jg.baixo = false;
					if(jg.agachado) {
						jg.velo *= 2f;
						jg.altura *= 1.2f;
						jg.agachado = false;
					} else {
						jg.velo /= 2f; 
						jg.altura /= 1.2f;
						jg.agachado = true;
					}
				}
			});
        botoesDpad.put("acao", new BotaoDpad(Texturas.atlas.get("clique"), 0) {
				public void aoTocar() {
					if(jg.inv.itens[jg.inv.slotSelecionado] != null) jg.item = jg.inv.itens[jg.inv.slotSelecionado].nome;
					else jg.item = "ar";
					jg.acao = true;
					jg.interagirBloco();
				}
				public void aoSoltar() { jg.acao = false; }
			});
        botoesDpad.put("ataque", new BotaoDpad(Texturas.atlas.get("ataque"), 0) {
				public void aoTocar() { jg.item = "ar"; jg.interagirBloco(); }
				public void aoSoltar() { jg.acao = false; }
			});
        botoesDpad.put("inv", new BotaoDpad(Texturas.atlas.get("clique"), jg.inv.tamSlot) {
				public void aoTocar() {
					if(jg.modo == 1 && jg.inv.aberto) jg.inv.alternar();
					else if(paginaItens.aberta) paginaItens.fechar();
					else jg.inv.alternar();
				}
				public void aoSoltar() {}
			});
        botoesDpad.put("menu_principal", new BotaoDpad(Texturas.atlas.get("receita"), 0) {
				public void aoTocar() { MenuPause.alternarMenu(); }
				public void aoSoltar() {}
			});
    }

    public void configDpad(int v, int h) {
        criarBotoesDpad();
        if(Gdx.app.getType() == com.badlogic.gdx.Application.ApplicationType.Desktop) {
            defPosMira(v, h);
            return;
        }
        final float dp = Gdx.graphics.getDensity();
        final float tam = botoesTam * dp;
        final float gap = 4 * dp; // 4dp de espaço entre botões
        final float passo = tam + gap;
        final float marg = (botoesTam - 16) * dp; // 16dp de margem das bordas

        // redimensiona todos os botões pro tamanho dp correto
        redimensionarBotoes(tam);

        // === DPad de movimento canto inferior esquerdo ===
        // ancora: borda esquerda + margem, borda inferior + margem
        final float dX = marg;
        final float dY = marg;

        defPosDpad("diagEsquerda", dX, dY + passo * 2);
        defPosDpad("frente", dX + passo, dY + passo * 2);
        defPosDpad("diagDireita",  dX + passo * 2, dY + passo * 2);
        defPosDpad("esquerda", dX, dY + passo);
        defPosDpad("direita", dX + passo * 2, dY + passo);
        defPosDpad("tras", dX + passo, dY);

        // === DPad de ações canto inferior direito ===
        final float aX = v - marg - passo * 2 - tam;
        final float aY = marg;

        defPosDpad("cima", aX + passo, aY + passo * 2);
        defPosDpad("ataque", aX, aY + passo);
        defPosDpad("acao", aX + passo * 2, aY + passo);
        defPosDpad("baixo", aX + passo, aY);

        // === inv e receita colados a hotbar ===
        final int hotbarX = (v >> 1) - ((jg.inv.hotbarSlots * jg.inv.tamSlot) >> 1);
        defPosDpad("inv", hotbarX + jg.inv.hotbarSlots * jg.inv.tamSlot, jg.inv.hotbarY);
        
        // === menu pause canto superior direito ===
        defPosDpad("menu_principal", v - tam - marg, h - tam - marg);

        defPosMira(v, h);
    }

    // redimensiona todos os BotaoDpad pro tamanho dp calculado
    public void redimensionarBotoes(float tam) {
        for(BotaoDpad b : botoesDpad.values()) {
            b.sprite.setSize(tam, tam);
            b.hitbox.width = tam;
            b.hitbox.height = tam;
        }
        // inv e receita usam tamSlot: não redimensiona
        final BotaoDpad bInv = botoesDpad.get("inv");
        final float ts = jg.inv.tamSlot;

        if(bInv != null) {
			bInv.sprite.setSize(ts, ts);
			bInv.hitbox.width = ts;
			bInv.hitbox.height = ts;
		}
    }

    public void defPosDpad(String nome, float x, float y) {
        final BotaoDpad b = botoesDpad.get(nome);
        if(b == null) return;
        b.sprite.setPosition(x, y);
        b.hitbox.setPosition(x, y);
    }

    public void defPosMira(int v, int h) {
        if(spriteMira == null) {
            final TextureRegion r = Texturas.atlas.get("mira");
            if(r != null) {
				spriteMira = new Sprite(r);
				spriteMira.setSize(40f, 40f);
			}
        }
        if(spriteMira != null) {
            spriteMira.setPosition(v / 2f - spriteMira.getWidth() / 2f,
								   h / 2f - spriteMira.getHeight() / 2f);
            spriteMira.setAlpha(0.9f);
        }
    }

    public static Rotulo addRotulo(String nome, String texto, float x, float y) {
        final Rotulo r = new Rotulo(texto, fonte, 1f);
        r.x = x;
        r.y = y;
        rotulos.put(nome, r);
        return r;
    }

    // loop principal
    public void att(float delta, Mundo mundo) {
        attCamera(camera.direction, jg.yaw, jg.tom);
        camera.up.set(0, 1, 0);

        if(!gui) return;

        Gdx.gl.glActiveTexture(GL20.GL_TEXTURE1);
        Gdx.gl.glBindTexture(GL20.GL_TEXTURE_2D, 0);
        Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);

        sb.begin();

        // mira
        if(spriteMira != null) spriteMira.draw(sb);

        // dpad
        for(BotaoDpad b : botoesDpad.values()) b.desenhar(sb);

        // rotulos estaticos
        for(Rotulo r : rotulos.values()) r.desenhar(sb, delta, 0f, 0f);

        // inventario
        renderizarInventario(sb, fonte, jg.inv);

        // página de itens criativos
        paginaItens.renderizar(sb, fonte);

        // barra de vida
        renderizarVida(sb);

        // menu pause
        MenuPause.renderizar(sb, fonte);

        // dialogos da Micro(chat, alertas)
        gerenciador.desenhar(sb, delta);

        // interfaces de blocos abertas
		if(Bloco.ABERTO) {
			for(Bloco b : Bloco.blocos) {
				if(b != null && b.ui != null && b.ui.aberta()) {
					b.ui.renderizar(sb, fonte, delta);
				}
			}
		}
        // debug
        if(debug) renderDebug(mundo);
        sb.end();
    }

    public void renderizarInventario(SpriteBatch sb, BitmapFont fonte, Inventario inv) {
        if(inv.rectsHotbar == null || inv.rectsHotbar.length == 0 ||
           inv.itens == null || inv.itens.length == 0) return;

        // hotbar
        for(int i = 0; i < inv.rectsHotbar.length; i++) {
            if(inv.rectsHotbar[i] == null) continue;
            final float rx = inv.rectsHotbar[i].x, ry = inv.rectsHotbar[i].y;
            final float rv = inv.rectsHotbar[i].width, rh = inv.rectsHotbar[i].height;

			sb.draw(inv.texSlot, rx, ry, rv, rh);
            if(i == inv.slotSelecionado) {
                sb.setColor(1, 1, 1, 0.5f);
                sb.draw(inv.texSlot, rx, ry, rv, rh);
                sb.setColor(1, 1, 1, 1);
            }
            if(inv.itens[i] != null) {		
				sb.draw(inv.itens[i].textura, rx + 5, ry + 5, inv.tamSlot - 10, inv.tamSlot - 10);
                if(inv.itens[i].quantidade > 1) {
                    fonte.draw(sb, String.valueOf(inv.itens[i].quantidade),
							   rx + inv.tamSlot - 15, ry + 15);
                }
            }
        }
        // inventario completo
        if(inv.aberto) {
            for(int i = 0; i < inv.rects.length; i++) {
                final float rx = inv.rects[i].x, ry = inv.rects[i].y;
                final float rv = inv.rects[i].width, rh = inv.rects[i].height;

				sb.draw(inv.texSlot, rx, ry, rv, rh);

                if(inv.itens[i] != null) {
					sb.draw(inv.itens[i].textura, rx + 5, ry + 5, inv.tamSlot - 5, inv.tamSlot - 5);

                    if(inv.itens[i].quantidade > 1) {
                        fonte.draw(sb, String.valueOf(inv.itens[i].quantidade),
								   rx + inv.tamSlot - 15, ry + 15);
                    }
                }
            }
        }
        // grade de receita(so quando inventário aberto)
        if(inv.aberto && inv.rectsGrade != null) {
            for(int i = 0; i < 9; i++) {
                if(inv.rectsGrade[i] == null) continue;
                final float rx = inv.rectsGrade[i].x, ry = inv.rectsGrade[i].y;
                final float rv = inv.rectsGrade[i].width, rh = inv.rectsGrade[i].height;
                sb.draw(inv.texSlot, rx, ry, rv, rh);
				if(inv.gradeReceita[i] == null) continue;
                final Item item = inv.gradeReceita[i];
                if(item != null && item.nome.length() > 0) {
                    sb.draw(item.textura, rx + 4, ry + 4, rv - 8, rh - 8);
                }
            }
            // slot de resultado
            if(inv.rectResultado != null) {
                final float rx = inv.rectResultado.x, ry = inv.rectResultado.y;
                final float rv = inv.rectResultado.width, rh = inv.rectResultado.height;
                sb.draw(inv.texSlot, rx, ry, rv, rh);
                if(inv.resultadoReceita != null) {
                    sb.draw(inv.resultadoReceita.textura, rx + 4, ry + 4, rv - 8, rh - 8);
                    if(inv.resultadoReceita.quantidade > 1)
                        fonte.draw(sb, String.valueOf(inv.resultadoReceita.quantidade), rx + 4, ry + 14);
                }
            }
        }
        // item flutuante
        if(inv.itemFlutuante != null) {
			final float posX = inv.posFlutuante.x - inv.itemFlutuante.textura.getRegionWidth() / 2;
			final float posY = inv.posFlutuante.y - inv.itemFlutuante.textura.getRegionHeight() / 2;

			sb.draw(inv.itemFlutuante.textura, posX, posY, inv.tamSlot - 10, inv.tamSlot - 10);
            if(inv.itemFlutuante.quantidade > 1) {
                fonte.draw(sb, String.valueOf(inv.itemFlutuante.quantidade),
						   posX + inv.tamSlot - 15, posY + 15);
            }
        }
        // botão de catalogo criativo(acima do ultimo slot do inventario)
        if(jg.modo == 1 && !paginaItens.aberta && inv.aberto) {
            final TextureRegion texAcao = Texturas.atlas.get("clique");
            if(texAcao != null && inv.rects != null && inv.rects.length > 0) {
                final Rectangle ultimoSlot = inv.rects[inv.rects.length - 1];
                sb.draw(texAcao,
						ultimoSlot.x,
						ultimoSlot.y + inv.tamSlot + 4,
						inv.tamSlot, inv.tamSlot
					);
            }
        }
    }

    public void renderizarVida(SpriteBatch sb) {
        final TextureRegion coracaoCompleto = Texturas.atlas.get("coracao_completo");
        final TextureRegion coracaoMetade = Texturas.atlas.get("coracao_metade");
        final TextureRegion coracaoVazio = Texturas.atlas.get("coracao_vazio");
        if(coracaoCompleto == null || coracaoMetade == null || coracaoVazio == null) return;

        final int totalCoracoes = jg.vidaMax >> 1; // 20 vida = 10 corações
        final float tamCoracao  = 30f;
        final float espCoracao  = 2f;

        final float hotbarY = (jg.inv.rectsHotbar != null && jg.inv.rectsHotbar.length > 0 && jg.inv.rectsHotbar[0] != null)
            ? jg.inv.rectsHotbar[0].y + jg.inv.tamSlot + 4f
            : 30f;
        final float inicioX = (jg.inv.rectsHotbar != null && jg.inv.rectsHotbar.length > 0 && jg.inv.rectsHotbar[0] != null)
            ? jg.inv.rectsHotbar[0].x
            : 10f;

        for(int i = 0; i < totalCoracoes; i++) {
            final float x = inicioX + i * (tamCoracao + espCoracao);
            final float y = hotbarY;

            final int vidaEsseCoracao = jg.vida - i * 2;

            final TextureRegion tex;
            if(vidaEsseCoracao >= 2) tex = coracaoCompleto;
            else if(vidaEsseCoracao == 1) tex = coracaoMetade;
            else tex = coracaoVazio;

            sb.draw(tex, x, y, tamCoracao, tamCoracao);
        }
    }

    public void renderDebug(final Mundo mundo) {
        final float livre = rt.freeMemory() >> 20;
        final float total = rt.totalMemory() >> 20;
        final float nativaLivre = debugador.obterHeapLivre() >> 20;
        final float nativaTotal = debugador.obterHeapTotal() >> 20;
        fps = Gdx.graphics.getFramesPerSecond();

        final String[] logsArr = Logs.logs.split("\n");
		Logs.logs = "";
        final int inicio = Math.max(0, logsArr.length - 15);
        for(int i = inicio; i < logsArr.length; i++) Logs.logs += logsArr[i] + '\n';

        final float yawNorm = ((jg.yaw % 360) + 360) % 360;
        final String direcao;
        if(yawNorm >= 337.5f || yawNorm < 22.5f)   direcao = "Norte";
        else if(yawNorm < 67.5f) direcao = "Nordeste";
        else if(yawNorm < 112.5f) direcao = "Leste";
        else if(yawNorm < 157.5f) direcao = "Sudeste";
        else if(yawNorm < 202.5f) direcao = "Sul";
        else if(yawNorm < 247.5f) direcao = "Sudoeste";
        else if(yawNorm < 292.5f) direcao = "Oeste";
        else direcao = "Noroeste";

        fonte.draw(sb, String.format(
					   "Jogador:\nX: %.1f, Y: %.1f, Z: %.1f\nDireção: %s (%.1f°)\nModo: %s\nSlot: %d\nItem: %s\n" +
					   "No chão: %b\nNa água: %b\nAgachado: %b\nVoando: %b\n\nStatus:\nVelocidade: %.2f\nAltura: %.2f\n\n" +
					   "Controles:\nDireita: %b, Esquerda: %b\nFrente: %b, Trás: %b\nCima: %b\nBaixo: %b\nAção: %b\n\n" +
					   "Mundo:\nNome: %s\nBioma atual: %s\nRaio Chunks: %d\nChunks: %d\n" +
					   "Chunks Alteradas: %d\nSemente: %d\nTempo: %.2f\nVelocidade do tempo: %.5f",
					   jg.posicao.x, jg.posicao.y, jg.posicao.z, direcao, yawNorm,
					   (jg.modo == 0 ? "espectador" : jg.modo == 1 ? "criativo" : "sobrevivencia"),
					   jg.inv.slotSelecionado, jg.item, jg.noChao, jg.naAgua, jg.agachado, jg.voando,
					   jg.velo, jg.altura,
					   jg.direita, jg.esquerda, jg.frente, jg.tras, jg.cima, jg.baixo, jg.acao,
					   mundo.nome, jg.bioma, mundo.RAIO_CHUNKS, mundo.chunks.size(),
					   mundo.chunksMod.size(), mundo.semente, Jogo.render.diaNoite.tempo, Jogo.render.diaNoite.tempo_velo),
				   50, telaH - 100);
        fonte.draw(sb, String.format(
					   "FPS: %d\nGPU:\nDesenhos: %d\nVértices: %.0f\nTrocas de Shader: %d\nLinks de textura: %d\n" +
					   "Threads ativas: %d\nMemória livre: %.1f MB\nMemória total: %.1f MB\n" +
					   "Memória usada: %.1f MB\nMemória nativa livre: %.1f MB\nMemória nativa total: %.1f MB\n" +
					   "Memória nativa usada: %.1f MB\n\nLogs:\n%s",
					   fps,
					   gpu.getDrawCalls(),
					   gpu.getVertexCount().total,
					   gpu.getShaderSwitches(),
					   gpu.getTextureBindings(),
					   Thread.activeCount(), livre, total, total - livre,
					   nativaLivre, nativaTotal, nativaTotal - nativaLivre,
					   Logs.logs),
				   telaV - 300, telaH - 100);
		gpu.reset();
    }

    // camera
    public static void attCamera(Vector3 vetor, float yaw, float tom) {
        final float yawRad = yaw * MathUtils.degRad;
        final float tomRad = tom * MathUtils.degRad;
        vetor.set(
            MathUtils.cos(tomRad) * MathUtils.sin(yawRad),
            MathUtils.sin(tomRad),
            MathUtils.cos(tomRad) * MathUtils.cos(yawRad)
        ).nor();
    }

    public void ajustar(int v, int h) {
		telaV = v;
		telaH = h;
        Gdx.gl.glViewport(0, 0, v, h);
        camera.viewportWidth  = v;
        camera.viewportHeight = h;
        camera.update();
        sb.getProjectionMatrix().setToOrtho2D(0, 0, v, h);
        jg.inv.aoAjustar(v, h);
        if(paginaItens.aberta) paginaItens.aoAjustar(v, h);
        configDpad(v, h);
    }

    public void liberar() {
        sb.dispose();
        gerenciador.liberar();
        MenuPause.liberar();
		botoesDpad.clear();
		rotulos.clear();
		toquesDpad.clear();
    }

    @Override
    public boolean touchDown(int telaX, int telaY, int p, int b) {
        int y = telaH - telaY;

        // interfaces de blocos tem prioridade sobre tudo exceto o gerenciador
        if(gerenciador.processarToque(telaX, y, true)) return true;

        // se tem interface de bloco aberta, ela consome o toque
        if(modoTexto) {
            for(Bloco bloco : Bloco.blocos) {
                if(bloco != null && bloco.ui != null && bloco.ui.aberta()) {
                    bloco.ui.processarToque(telaX, y, true);
                    return true;
                }
            }
            return true;
        }
        // menu pause
        if(MenuPause.menuAberto) {
            boolean consumido = MenuPause.processarToque(telaX, y, true);
            if(!consumido) MenuPause.fecharMenu();
            return true;
        }
        // pagina de itens criativos tem prioridade se estiver aberta
        if(paginaItens.aberta) {
            paginaItens.aoTocar(telaX, y, jg);
            return true;
        }
        // cliques PC no mundo
        if(Gdx.app.getType() == com.badlogic.gdx.Application.ApplicationType.Desktop && !jg.inv.aberto) {
            if(b == Input.Buttons.LEFT) {
                jg.item = "ar";
                jg.interagirBloco();
                jg.item = jg.inv.itens[jg.inv.slotSelecionado] != null
					? jg.inv.itens[jg.inv.slotSelecionado].nome : "ar";
                return true;
            }
            if(b == Input.Buttons.RIGHT) {
                jg.item = jg.inv.itens[jg.inv.slotSelecionado] != null ? jg.inv.itens[jg.inv.slotSelecionado].nome : "ar";
                jg.acao = true;
                jg.interagirBloco();
                return true;
            }
        }
        // dpad
        for(Map.Entry<String, BotaoDpad> e : botoesDpad.entrySet()) {
            if(e.getValue().hitbox.contains(telaX, y)) {
                e.getValue().aoTocar();
                e.getValue().sprite.setAlpha(0.5f);
                toquesDpad.put(p, e.getKey());
                return true;
            }
        }
        // inventario
        jg.inv.aoTocar(telaX, y, p);

        // botão catalogo de itens
        if(jg.inv.rects != null && jg.inv.rects.length > 0 && jg.inv.aberto && jg.modo == 1) {
            Rectangle ultimoSlot = jg.inv.rects[jg.inv.rects.length - 1];
            float bx = ultimoSlot.x;
            float by = ultimoSlot.y + jg.inv.tamSlot + 4;
            if(telaX >= bx && telaX <= bx + jg.inv.tamSlot && y >= by && y <= by + jg.inv.tamSlot) {
                paginaItens.abrir(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), jg.inv);
                return true;
            }
        }
        if(pontoDir == -1) {
			pontoDir = p;
			ultimaDir.set(telaX, y);
		}
        return true;
    }

    @Override
    public boolean touchUp(int telaX, int telaY, int p, int b) {
        if(b == Input.Buttons.RIGHT) jg.acao = false;
        final int y = telaH - telaY;

        gerenciador.processarToque(telaX, y, false);

        if(MenuPause.menuAberto) {
            MenuPause.processarToque(telaX, y, false);
            return true;
        }
        final String nomeBotao = toquesDpad.remove(p);
        if(nomeBotao != null) {
            final BotaoDpad bd = botoesDpad.get(nomeBotao);
            if(bd != null) {
				bd.aoSoltar();
				bd.sprite.setAlpha(0.9f);
			}
        }
        if(p == pontoDir) pontoDir = -1;
        return true;
    }

    @Override
    public boolean touchDragged(int telaX, int telaY, int p) {
        if(modoTexto) return true;
        final int y = telaH - telaY;

        jg.inv.aoArrastar(telaX, y);

        if(MenuPause.menuAberto) {
            MenuPause.processarArraste(telaX, y);
            return true;
        }
        if(p == pontoDir && !jg.inv.aberto) {
            final float dx = telaX - ultimaDir.x;
            final float dy = y - ultimaDir.y;
            jg.yaw -= dx * sensi;
            jg.tom += dy * sensi;
            jg.tom = MathUtils.clamp(jg.tom, -89f, 89f);
            ultimaDir.set(telaX, y);
        }
        // transição entre botões do dpad ao arrastar
        final String nomeAtual = toquesDpad.get(p);
        boolean sobreBotao = false;
        for(Map.Entry<String, BotaoDpad> e : botoesDpad.entrySet()) {
            if(e.getValue().hitbox.contains(telaX, y)) {
                sobreBotao = true;
                if(!e.getKey().equals(nomeAtual)) {
                    if(nomeAtual != null) {
                        final BotaoDpad antigo = botoesDpad.get(nomeAtual);
                        if(antigo != null) {
							antigo.aoSoltar();
							antigo.sprite.setAlpha(0.9f);
						}
                    }
                    e.getValue().aoTocar();
                    e.getValue().sprite.setAlpha(0.5f);
                    toquesDpad.put(p, e.getKey());
                }
                break;
            }
        }
        if(!sobreBotao && nomeAtual != null) {
            final BotaoDpad antigo = botoesDpad.get(nomeAtual);
            if(antigo != null) { antigo.aoSoltar(); antigo.sprite.setAlpha(0.9f); }
            toquesDpad.put(p, null);
        }
		camera.update();
        return true;
    }

    @Override
    public boolean keyDown(int p) {
        if(modoTexto) {
            gerenciador.processarTecla(p);
            // ESC fecha a interface de bloco aberta
            if(p == Input.Keys.ESCAPE) {
                for(Bloco bloco : Bloco.blocos) {
                    if(bloco != null && bloco.ui != null && bloco.ui.aberta()) {
                        bloco.ui.fechar();
                    }
                }
            }
            return true;
        }
        if(p == Input.Keys.W) jg.frente = true;
        if(p == Input.Keys.S) jg.tras = true;
        if(p == Input.Keys.A) jg.esquerda = true;
        if(p == Input.Keys.D) jg.direita = true;
        if(p == Input.Keys.SPACE) {
			jg.cima = true;
			if(jg.modo == 1) { // so conta pulo se estava no chão
				if(jg.tempoDuploPulo > 0f) {
					jg.voando = !jg.voando;
					if(jg.voando) jg.velocidade.y = 0;
					jg.tempoDuploPulo = 0f;
				} else {
					jg.tempoDuploPulo = Jogador.JANELA_DUPLO_PULO;
				}
			}
		}
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
            if(jg.modo == 1 && jg.inv.aberto) {
                jg.inv.alternar();
            } else if(paginaItens.aberta) paginaItens.fechar();
            else jg.inv.alternar();
            return true;
        }
        if(p == Input.Keys.F1) debug = !debug;
        if(p == Input.Keys.F3) gui = !gui;
        if(p == Input.Keys.T) abrirChat();
        if(p == Input.Keys.ESCAPE) {
            if(paginaItens.aberta) {
				paginaItens.fechar();
				return true;
			}
            MenuPause.alternarMenu();
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
    public boolean keyTyped(char p) {
        if(modoTexto) gerenciador.processarCaractere(p);
        if(paginaItens.aberta) paginaItens.digitarCaractere(p);
        return false;
    }

    @Override
    public boolean mouseMoved(int x, int y1) {
        if(modoTexto || MenuPause.menuAberto) return true;
        final int y = telaH - y1;
        jg.inv.aoArrastar(x, y);
        if(!jg.inv.aberto) {
            jg.yaw -= Gdx.input.getDeltaX() * sensi;
            jg.tom -= Gdx.input.getDeltaY() * sensi;
            jg.tom = MathUtils.clamp(jg.tom, -89f, 89f);
        }
		camera.update();
        return true;
    }

    @Override
    public boolean scrolled(float x, float y) {
        if(y > 0) jg.inv.slotSelecionado = (jg.inv.slotSelecionado + 1) % jg.inv.hotbarSlots;
        else if(y < 0) jg.inv.slotSelecionado = (jg.inv.slotSelecionado - 1 + jg.inv.hotbarSlots) % jg.inv.hotbarSlots;
        return true;
    }

    public abstract class BotaoDpad {
        public final Sprite sprite;
        public final Rectangle hitbox;

        public BotaoDpad(TextureRegion textura, float tam) {
            sprite = new Sprite(textura);
            sprite.setSize(tam, tam);
            sprite.setAlpha(0.9f);
            hitbox = new Rectangle(0, 0, tam, tam);
        }

        // sobrecarga pra Texture direta(botoes do dpad guardados em texs, não atlas)
        public BotaoDpad(com.badlogic.gdx.graphics.Texture textura, float tam) {
            sprite = new Sprite(textura);
            sprite.setSize(tam, tam);
            sprite.setAlpha(0.9f);
            hitbox = new Rectangle(0, 0, tam, tam);
        }
        public abstract void aoTocar();
        public abstract void aoSoltar();
        public void desenhar(SpriteBatch sb) { sprite.draw(sb); }
    }
}

