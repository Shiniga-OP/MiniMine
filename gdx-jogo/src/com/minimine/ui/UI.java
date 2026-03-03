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
import com.minimine.entidades.Inventario;
import com.minimine.entidades.Jogador;
import com.minimine.utils.DiaNoiteUtil;
import com.minimine.utils.Receitas;

import com.micro.GerenciadorUI;
import com.micro.CaixaDialogo;
import com.micro.CampoTexto;
import com.micro.Rotulo;
import com.micro.PainelFatiado;
import com.micro.Acao;

public class UI implements InputProcessor {
    // camera 3D e renderização
    public static PerspectiveCamera camera;
    public static SpriteBatch sb;
    public static BitmapFont fonte;

    public static float sensi = 0.25f;
    public static float aprox = 0.01f;
    public static float distancia = 400f;
    public static int pov = 90;

    // botões do DPad mobile
    public static final HashMap<String, BotaoDpad> botoesDpad = new HashMap<>();
    public static final HashMap<Integer, String> toquesDpad = new HashMap<>();

    public static float botaoTam = 70f;
    public static float espaco = 60f;

    public Sprite spriteMira;
    public int pontoDir = -1;
    public final Vector2 ultimaDir = new Vector2();

    public static GerenciadorUI gerenciador;
    public static PainelFatiado visualBase;

    // dialogo de chat/alertas genericos
    public static CaixaDialogo dialogoChat;
    public static CampoTexto   campoChatTexto;

    // rotulos dinamicos acessiveis externamente
    public static final HashMap<String, Rotulo> rotulos = new HashMap<>();

    // estado
    public static Jogador jg;
    public static boolean debug     = true;
    public static boolean modoTexto = false;
    public static int fps = 0;
    public static Debugador debugador;

    public boolean chatAberto = false;
    public String  ultimaMensagem = "";
    public List<String> msgs = new ArrayList<>();

    public static Runtime rt = Runtime.getRuntime();

    public UI(Jogador jogador) {
        camera = new PerspectiveCamera(pov, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.position.set(10f, 18f, 10f);
        camera.lookAt(0, 0, 0);
        camera.near = aprox;
        camera.far  = distancia;
        camera.update();

        sb = new SpriteBatch();
        fonte = InterUtil.carregarFonte("ui/fontes/pixel.ttf");

        this.jg = jogador;
        this.jg.camera = camera;

        gerenciador = new GerenciadorUI();
        visualBase  = new PainelFatiado(Texturas.base);

        criarDialogos();
        configDpad(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		
		MenuPause.iniciar();
        MenuPause.sr = new ShapeRenderer();

        Gdx.input.setInputProcessor(this);
        Gdx.input.setCursorCatched(true);
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
        dialogoChat.addBotao("Enviar",   visualBase, com.micro.Ancora.CENTRO_DIREITO,  -10, acaoEnviar);
        dialogoChat.addBotao("Cancelar", visualBase, com.micro.Ancora.CENTRO_ESQUERDO,  10, acaoCancelar);

        gerenciador.addDialogo(dialogoChat);
    }

    // chat
    public void abrirChat() {
        if(chatAberto) return;
        chatAberto = true;
        modoTexto  = true;
        Gdx.input.setCursorCatched(false);

        float cx = Gdx.graphics.getWidth()  / 2f - dialogoChat.largura / 2f;
        float cy = Gdx.graphics.getHeight() / 2f - dialogoChat.altura  / 2f;
        dialogoChat.x = cx;
        dialogoChat.y = cy;

        dialogoChat.mostrar("Chat", "", new CaixaDialogo.Fechar() {
				@Override
				public void aoFechar(boolean confirmou) {
					chatAberto = false;
					modoTexto  = false;
					Gdx.input.setCursorCatched(true);
				}
			});
    }

    // abre um dialogo de aviso com padrão ao fechar
    public static void abrirDialogo(String titulo, final CaixaDialogo.Fechar fechar) {
        // cria um dialogo temporario de alerta para não misturar com o de chat
        // reutiliza o gerenciador; o dialogo se auto remove ao fechar
        CaixaDialogo alerta = new CaixaDialogo(visualBase, fonte, 3f, new ShapeRenderer());
        alerta.largura = 400;
        alerta.altura  = 160;
        alerta.x = Gdx.graphics.getWidth()  / 2f - alerta.largura / 2f;
        alerta.y = Gdx.graphics.getHeight() / 2f - alerta.altura  / 2f;
        alerta.addOk(visualBase);
        gerenciador.addDialogo(alerta);
        alerta.mostrar(titulo, "", fechar != null ? fechar : new CaixaDialogo.Fechar(){@Override public void aoFechar(boolean c){}});
    }
    // dpad(mantido como sprites, texturas direcionais não fazem sentido na Micro)
    public void criarBotoesDpad() {
        if(!botoesDpad.isEmpty()) return;

        TextureRegion mira = Texturas.atlas.get("mira");
        spriteMira = mira != null ? new Sprite(mira) : null;
        if(spriteMira != null) spriteMira.setSize(40f, 40f);
        else Gdx.app.log("[UI]", "[ERRO]: A textura mira é null");

        if(Gdx.app.getType() == com.badlogic.gdx.Application.ApplicationType.Desktop) return;

        float tam = MathUtils.clamp(botaoTam * Gdx.graphics.getDensity(), 50f, 150f);

        botoesDpad.put("direita", new BotaoDpad(Texturas.texs.get("botao_d"),  tam) {
				public void aoTocar(){jg.direita = true;}
				public void aoSoltar(){ jg.direita = false;}
			});
        botoesDpad.put("esquerda", new BotaoDpad(Texturas.texs.get("botao_e"), tam) {
				public void aoTocar(){ jg.esquerda = true;}
				public void aoSoltar(){jg.esquerda = false;}
			});
        botoesDpad.put("frente", new BotaoDpad(Texturas.texs.get("botao_f"), tam) {
				public void aoTocar(){ jg.frente = true;}
				public void aoSoltar(){ jg.frente = false;}
			});
        botoesDpad.put("tras", new BotaoDpad(Texturas.texs.get("botao_t"), tam) {
				public void aoTocar(){jg.tras = true;}
				public void aoSoltar(){ jg.tras = false;}
			});
        botoesDpad.put("cima", new BotaoDpad(Texturas.texs.get("botao_f"), tam) {
				public void aoTocar(){ jg.cima = true;}
				public void aoSoltar(){ jg.cima = false;}
			});
        botoesDpad.put("diagDireita", new BotaoDpad(Texturas.texs.get("botao_ld"), tam) {
				public void aoTocar(){ jg.frente = jg.direita  = true;}
				public void aoSoltar(){ jg.frente = jg.direita = false;}
			});
        botoesDpad.put("diagEsquerda", new BotaoDpad(Texturas.texs.get("botao_le"), tam) { public void aoTocar(){ jg.frente = jg.esquerda = true;  } public void aoSoltar(){ jg.frente = jg.esquerda = false; } });

        botoesDpad.put("baixo", new BotaoDpad(Texturas.texs.get("botao_t"), tam) {
				public void aoTocar() {
					jg.baixo = true;
					if(jg.agachado) {
						jg.velo *= 2;  jg.altura *= 1.2f;
						jg.agachado = false;
					} else {
						jg.velo /= 2; 
						jg.altura /= 1.2f;
						jg.agachado = true;
					}
				}
				public void aoSoltar(){jg.baixo = false;}
			});
        botoesDpad.put("acao", new BotaoDpad(Texturas.atlas.get("clique"), tam) {
				public void aoTocar() {
					if(jg.inv.itens[jg.inv.slotSelecionado] != null) jg.item = jg.inv.itens[jg.inv.slotSelecionado].nome;
					else jg.item = "ar";
					jg.acao = true;
					jg.interagirBloco();
				}
				public void aoSoltar() { jg.acao = false; }
			});
        botoesDpad.put("ataque", new BotaoDpad(Texturas.atlas.get("ataque"), tam) {
				public void aoTocar() { jg.item = "ar"; jg.interagirBloco(); }
				public void aoSoltar() { jg.acao = false; }
			});
        botoesDpad.put("inv", new BotaoDpad(Texturas.atlas.get("clique"), jg.inv.tamSlot) {
				public void aoTocar() { jg.inv.alternar(); }
				public void aoSoltar() {}
			});
        botoesDpad.put("receita", new BotaoDpad(Texturas.atlas.get("receita"), jg.inv.tamSlot) {
				public void aoTocar() { Receitas.fabricar(jg.inv); }
				public void aoSoltar() {}
			});
        botoesDpad.put("menu_principal", new BotaoDpad(Texturas.atlas.get("receita"), tam) {
				public void aoTocar()  { MenuPause.alternarMenu(); }
				public void aoSoltar() {}
			});
    }

    public void configDpad(int v, int h) {
        criarBotoesDpad();
        if(Gdx.app.getType() == com.badlogic.gdx.Application.ApplicationType.Desktop) {
            defPosMira(v, h);
            return;
        }
        float tam = MathUtils.clamp(botaoTam * Gdx.graphics.getDensity(), 50f, 150f);
        float centroX = espaco + tam * 1.5f;
        float centroY = espaco + tam * 1.5f;

        defPosDpad("direita", centroX + espaco, centroY - tam / 2);
        defPosDpad("esquerda", centroX - tam - espaco,  centroY - tam / 2);
        defPosDpad("frente", centroX - tam / 2, centroY + espaco);
        defPosDpad("tras", centroX - tam / 2, centroY - tam - espaco);
        defPosDpad("cima", v - tam * 1.5f, centroY + espaco);
        defPosDpad("baixo", v - tam * 1.5f, centroY - tam - espaco);
        defPosDpad("diagDireita",  centroX + espaco, centroY + espaco);
        defPosDpad("diagEsquerda", centroX - tam - espaco, centroY + espaco);
        defPosDpad("acao", v - tam * 1.5f, centroY * 2 + espaco);
        defPosDpad("ataque", v - tam * 2.5f, centroY * 2 + espaco);
        defPosDpad("receita", v - tam, h - tam);

        int hotbarX = v / 2 - (jg.inv.hotbarSlots * jg.inv.tamSlot) / 2;
        defPosDpad("inv", hotbarX + jg.inv.hotbarSlots * jg.inv.tamSlot, jg.inv.hotbarY);

        defPosMira(v, h);
    }

    public void defPosDpad(String nome, float x, float y) {
        BotaoDpad b = botoesDpad.get(nome);
        if(b == null) return;
        b.sprite.setPosition(x, y);
        b.hitbox.setPosition(x, y);
    }

    public void defPosMira(int v, int h) {
        if(spriteMira == null) {
            TextureRegion r = Texturas.atlas.get("mira");
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
        Rotulo r = new Rotulo(texto, fonte, 1f);
        r.x = x;
        r.y = y;
        rotulos.put(nome, r);
        return r;
    }

    // loop principal
    public void att(float delta, Mundo mundo) {
        attCamera(camera.direction, jg.yaw, jg.tom);
        camera.up.set(0, 1, 0);

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

        // barra de vida
        renderizarVida(sb);

        // menu pause
        MenuPause.renderizar(sb, fonte);

        // dialogos da Micro(chat, alertas)
        gerenciador.desenhar(sb, delta);

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
            float rx = inv.rectsHotbar[i].x, ry = inv.rectsHotbar[i].y;
            float rv = inv.rectsHotbar[i].width, rh = inv.rectsHotbar[i].height;

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
                float rx = inv.rects[i].x, ry = inv.rects[i].y;
                float rv = inv.rects[i].width, rh = inv.rects[i].height;
				
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
        // item flutuante
        if(inv.itemFlutuante != null) {
			float posX = inv.posFlutuante.x - inv.itemFlutuante.textura.getRegionWidth() / 2;
			float posY = inv.posFlutuante.y - inv.itemFlutuante.textura.getRegionHeight() / 2;
			
			sb.draw(inv.itemFlutuante.textura, posX, posY, inv.tamSlot - 10, inv.tamSlot - 10);
            if(inv.itemFlutuante.quantidade > 1) {
                fonte.draw(sb, String.valueOf(inv.itemFlutuante.quantidade),
						   posX + inv.tamSlot - 15,
						   posY + 15);
            }
        }
    }

    public void renderizarVida(SpriteBatch sb) {
        TextureRegion coracaoCompleto = Texturas.atlas.get("coracao_completo");
        TextureRegion coracaoMetade   = Texturas.atlas.get("coracao_metade");
        TextureRegion coracaoVazio    = Texturas.atlas.get("coracao_vazio");
        if(coracaoCompleto == null || coracaoMetade == null || coracaoVazio == null) return;

        int totalCoracoes = jg.vidaMax >> 1; // 20 vida = 10 corações
        float tamCoracao  = 30f;
        float espCoracao  = 2f;

        // posiciona acima da hotbar(ou no canto superior esquerdo se a hotbar não existir)
        float hotbarY = (jg.inv.rectsHotbar != null && jg.inv.rectsHotbar.length > 0 && jg.inv.rectsHotbar[0] != null)
            ? jg.inv.rectsHotbar[0].y + jg.inv.tamSlot + 4f
            : 30f;
        float startX = (jg.inv.rectsHotbar != null && jg.inv.rectsHotbar.length > 0 && jg.inv.rectsHotbar[0] != null)
            ? jg.inv.rectsHotbar[0].x
            : 10f;

        for(int i = 0; i < totalCoracoes; i++) {
            float x = startX + i * (tamCoracao + espCoracao);
            float y = hotbarY;

            int vidaEsseCoracao = jg.vida - i * 2; // quanta vida "sobra" pra esse coração

            TextureRegion tex;
            if(vidaEsseCoracao >= 2) tex = coracaoCompleto;
            else if(vidaEsseCoracao == 1) tex = coracaoMetade;
            else tex = coracaoVazio;

            sb.draw(tex, x, y, tamCoracao, tamCoracao);
        }
    }

    public void renderDebug(Mundo mundo) {
        float livre = rt.freeMemory()  >> 20;
        float total = rt.totalMemory() >> 20;
        float nativaLivre = debugador.obterHeapLivre() >> 20;
        float nativaTotal = debugador.obterHeapTotal() >> 20;
        fps = Gdx.graphics.getFramesPerSecond();

        String[] logsArr = Logs.logs.split("\n");
        StringBuilder sb2 = new StringBuilder();
        int inicio = Math.max(0, logsArr.length - 15);
        for(int i = inicio; i < logsArr.length; i++) sb2.append(logsArr[i]).append("\n");

        fonte.draw(sb, String.format(
					   "Jogador:\nX: %.1f, Y: %.1f, Z: %.1f\nModo: %s\nSlot: %d\nItem: %s\n" +
					   "No chão: %b\nNa água: %b\nAgachado: %b\n\nStatus:\nVelocidade: %.2f\nAltura: %.2f\n\n" +
					   "Controles:\nDireita: %b, Esquerda: %b\nFrente: %b, Trás: %b\nCima: %b\nBaixo: %b\nAção: %b\n\n" +
					   "Mundo:\nNome: %s\nBioma atual: %s\nRaio Chunks: %d\nChunks ativos: %d\n" +
					   "Chunks Alteradas: %d\nSemente: %d\nTempo: %.2f\nTick: %.3f\nVelocidade do tempo: %.5f",
					   jg.posicao.x, jg.posicao.y, jg.posicao.z,
					   (jg.modo == 0 ? "espectador" : jg.modo == 1 ? "criativo" : "sobrevivencia"),
					   jg.inv.slotSelecionado, jg.item, jg.noChao, jg.naAgua, jg.agachado, jg.velo, jg.altura,
					   jg.direita, jg.esquerda, jg.frente, jg.tras, jg.cima, jg.baixo, jg.acao,
					   mundo.nome, jg.bioma, mundo.RAIO_CHUNKS, mundo.chunks.size(),
					   mundo.chunksMod.size(), mundo.semente, DiaNoiteUtil.tempo, mundo.tick, DiaNoiteUtil.tempo_velo),
				   50, Gdx.graphics.getHeight() - 100);
        fonte.draw(sb, String.format(
					   "FPS: %d\nThreads ativas: %d\nMemória livre: %.1f MB\nMemória total: %.1f MB\n" +
					   "Memória usada: %.1f MB\nMemória nativa livre: %.1f MB\nMemória nativa total: %.1f MB\n" +
					   "Memória nativa usada: %.1f MB\n\nLogs:\n%s",
					   fps, Thread.activeCount(), livre, total, total - livre,
					   nativaLivre, nativaTotal, nativaTotal - nativaLivre,
					   sb2.toString()),
				   Gdx.graphics.getWidth() - 300, Gdx.graphics.getHeight() - 100);
    }

    // camera
    public static void attCamera(Vector3 vetor, float yaw, float tom) {
        float yawRad = yaw * MathUtils.degRad;
        float tomRad = tom * MathUtils.degRad;
        vetor.set(
            MathUtils.cos(tomRad) * MathUtils.sin(yawRad),
            MathUtils.sin(tomRad),
            MathUtils.cos(tomRad) * MathUtils.cos(yawRad)
        ).nor();
    }

    public void ajustar(int v, int h) {
        Gdx.gl.glViewport(0, 0, v, h);
        camera.viewportWidth  = v;
        camera.viewportHeight = h;
        camera.update();
        sb.getProjectionMatrix().setToOrtho2D(0, 0, v, h);
        jg.inv.aoAjustar(v, h);
        configDpad(v, h);
    }

    public void liberar() {
        sb.dispose();
        fonte.dispose();
        gerenciador.liberar();
        MenuPause.liberar();
		botoesDpad.clear();
		rotulos.clear();
		toquesDpad.clear();
    }

    @Override
    public boolean touchDown(int telaX, int telaY, int p, int b) {
        int y = Gdx.graphics.getHeight() - telaY;

        // dialogos do Micro tem prioridade total
        if(gerenciador.processarToque(telaX, y, true)) return true;
        if(modoTexto) return true;

        // menu pause
        if(MenuPause.menuAberto) {
            boolean consumido = MenuPause.processarToque(telaX, y, true);
            if(!consumido) MenuPause.fecharMenu();
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

        if(pontoDir == -1) { pontoDir = p; ultimaDir.set(telaX, y); }
        return true;
    }

    @Override
    public boolean touchUp(int telaX, int telaY, int p, int b) {
        if(b == Input.Buttons.RIGHT) jg.acao = false;
        int y = Gdx.graphics.getHeight() - telaY;

        gerenciador.processarToque(telaX, y, false);

        if(MenuPause.menuAberto) {
            MenuPause.processarToque(telaX, y, false);
            return true;
        }
        String nomeBotao = toquesDpad.remove(p);
        if(nomeBotao != null) {
            BotaoDpad bd = botoesDpad.get(nomeBotao);
            if(bd != null) { bd.aoSoltar(); bd.sprite.setAlpha(0.9f); }
        }
        if(p == pontoDir) pontoDir = -1;
        return true;
    }

    @Override
    public boolean touchDragged(int telaX, int telaY, int p) {
        if(modoTexto) return true;
        int y = Gdx.graphics.getHeight() - telaY;

        jg.inv.aoArrastar(telaX, y, p);

        if(MenuPause.menuAberto) {
            MenuPause.processarArraste(telaX, y);
            return true;
        }
        if(p == pontoDir && !jg.inv.aberto) {
            float dx = telaX - ultimaDir.x;
            float dy = y - ultimaDir.y;
            jg.yaw -= dx * sensi;
            jg.tom += dy * sensi;
            jg.tom = MathUtils.clamp(jg.tom, -89f, 89f);
            ultimaDir.set(telaX, y);
        }
        // transição entre botões do dpad ao arrastar
        String nomeAtual = toquesDpad.get(p);
        boolean sobreBotao = false;
        for(Map.Entry<String, BotaoDpad> e : botoesDpad.entrySet()) {
            if(e.getValue().hitbox.contains(telaX, y)) {
                sobreBotao = true;
                if(!e.getKey().equals(nomeAtual)) {
                    if(nomeAtual != null) {
                        BotaoDpad antigo = botoesDpad.get(nomeAtual);
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
            BotaoDpad antigo = botoesDpad.get(nomeAtual);
            if(antigo != null) { antigo.aoSoltar(); antigo.sprite.setAlpha(0.9f); }
            toquesDpad.put(p, null);
        }
        return true;
    }

    @Override
    public boolean keyDown(int p) {
        if(modoTexto) {
            gerenciador.processarTecla(p);
            return true;
        }
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
            Gdx.input.setCursorCatched(jg.inv.aberto); // inverte junto com o inventario
            jg.inv.alternar();
        }
        if(p == Input.Keys.F1) debug = !debug;
        if(p == Input.Keys.T) abrirChat();
        if(p == Input.Keys.P) Mundo.debugColisao = !Mundo.debugColisao;
        if(p == Input.Keys.ESCAPE) MenuPause.alternarMenu();
        if(p == Input.Keys.R) Receitas.fabricar(jg.inv);
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
        return false;
    }

    @Override
    public boolean mouseMoved(int x, int y1) {
        if(modoTexto || MenuPause.menuAberto) return true;
        int y = Gdx.graphics.getHeight() - y1;
        jg.inv.aoArrastar(x, y, -1);
        if(!jg.inv.aberto) {
            jg.yaw -= Gdx.input.getDeltaX() * sensi;
            jg.tom -= Gdx.input.getDeltaY() * sensi;
            jg.tom = MathUtils.clamp(jg.tom, -89f, 89f);
        }
        return true;
    }

    @Override
    public boolean scrolled(float x, float y) {
        if(y > 0) jg.inv.slotSelecionado = (jg.inv.slotSelecionado + 1) % jg.inv.hotbarSlots;
        else if (y < 0) jg.inv.slotSelecionado = (jg.inv.slotSelecionado - 1 + jg.inv.hotbarSlots) % jg.inv.hotbarSlots;
        return true;
    }

    public abstract class BotaoDpad {
        public final Sprite sprite;
        public final com.badlogic.gdx.math.Rectangle hitbox;

        public BotaoDpad(TextureRegion textura, float tam) {
            sprite = new Sprite(textura);
            sprite.setSize(tam, tam);
            sprite.setAlpha(0.9f);
            hitbox = new com.badlogic.gdx.math.Rectangle(0, 0, tam, tam);
        }

        // sobrecarga pra Texture direta(botoes do dpad guardados em texs, não atlas)
        public BotaoDpad(com.badlogic.gdx.graphics.Texture textura, float tam) {
            sprite = new Sprite(textura);
            sprite.setSize(tam, tam);
            sprite.setAlpha(0.9f);
            hitbox = new com.badlogic.gdx.math.Rectangle(0, 0, tam, tam);
        }
        public abstract void aoTocar();
        public abstract void aoSoltar();
        public void desenhar(SpriteBatch sb) { sprite.draw(sb); }
    }
}



