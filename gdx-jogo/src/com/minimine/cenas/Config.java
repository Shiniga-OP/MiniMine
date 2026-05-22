package com.minimine.cenas;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.Preferences;
import com.minimine.mundo.Mundo;
import com.minimine.ui.UI;
import com.minimine.Cenas;
import com.minimine.Inicio;
import com.micro.util.GerenciadorUI;
import com.micro.janelas.Painel;
import com.micro.janelas.PainelFatiado;
import com.micro.janelas.PainelRolavel;
import com.micro.componentes.ItemConfig;
import com.micro.componentes.CampoTexto;
import com.micro.componentes.Botao;
import com.micro.componentes.Rotulo;
import com.micro.util.Ancora;
import com.micro.util.Acao;

public class Config implements Screen, InputProcessor {
    public SpriteBatch pincel;
    public ShapeRenderer pincelFormas;
    public BitmapFont fonteTitulo;
    public BitmapFont fonteTexto;
    public OrthographicCamera camera;
    public Viewport vista;
    public Vector3 toqueAuxiliar;
    public Preferences prefs;

    public GerenciadorUI gerenciadorUI;
    public PainelFatiado visualJanela;
    public PainelFatiado visualBotao;
    public Texture pixelBranco;
    public float escalaPixel;

    public Painel painelPrincipal;

    // referencias aos itens para atualizar valores no render
    public ItemConfig itemRaio, itemSensi,
	itemMusicas, itemDistancia, itemPOV,
	itemDebug, itemBotoesTam, itemInterface;
    public CampoTexto campoNome;

    @Override
    public void show() {
        pincel = new SpriteBatch();
        pincelFormas = new ShapeRenderer();

        fonteTitulo = new BitmapFont();
        fonteTitulo.getData().setScale(2.0f);
        fonteTitulo.getRegion().getTexture().setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        fonteTexto = new BitmapFont();
        fonteTexto.getData().setScale(1.5f);
        fonteTexto.getRegion().getTexture().setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        camera = new OrthographicCamera();
        vista = new ScreenViewport(camera);
        vista.apply(true);

        toqueAuxiliar = new Vector3();
        escalaPixel = 4.0f;

        final Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(1, 1, 1, 1);
        pixmap.fill();
        pixelBranco = new Texture(pixmap);
        pixmap.dispose();

        prefs = Gdx.app.getPreferences("MiniConfig");
        gerenciadorUI = new GerenciadorUI();

        try {
            Texture textura = new Texture(Gdx.files.internal("texturas/ui/base.png"));
            textura.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            visualJanela = new PainelFatiado(textura);
            visualBotao = new PainelFatiado(textura);
            criarInterface();
        } catch(Exception e) {
            Gdx.app.log("ERRO", "Recursos nao encontrados: " + e.getMessage());
        }
        Gdx.input.setInputProcessor(this);
    }

    public void criarInterface() {
        painelPrincipal = new Painel(visualJanela, -350, -350, 700, 700, escalaPixel);
        painelPrincipal.defEspaco(20, 30);

        Rotulo titulo = new Rotulo("CONFIGURACOES", fonteTitulo, escalaPixel);
        titulo.largura = 660;
        titulo.altura = 60;
        painelPrincipal.addAncorado(titulo, Ancora.SUPERIOR_CENTRO, 0, 0);

        // painel rolavel ocupa o espaco entre o titulo e o botao voltar
        PainelRolavel painelOpcoes = new PainelRolavel(20, 80, 660, 530);
        painelOpcoes.defEspaco(0.5f);

        float larguraItem = 650;
        float alturaItem = 75;
        float espacamento = 6;
        float escalaItem = escalaPixel * 0.75f;

        // raio de chunks
        itemRaio = ItemConfig.numerico(
            5, posItem(0, alturaItem, espacamento), larguraItem, alturaItem,
            "Raio Chunks:", String.valueOf(Mundo.RAIO_CHUNKS),
            fonteTexto, escalaItem, pixelBranco, visualBotao,
            new Acao() {
                public void exec() {
                    if(Mundo.RAIO_CHUNKS > 1) {
                        Mundo.RAIO_CHUNKS--;
                        itemRaio.rotuloValor.texto = String.valueOf(Mundo.RAIO_CHUNKS);
                    }
                }
            },
            new Acao() {
                public void exec() {
                    if(Mundo.RAIO_CHUNKS < 20) {
                        Mundo.RAIO_CHUNKS++;
                        itemRaio.rotuloValor.texto = String.valueOf(Mundo.RAIO_CHUNKS);
                    }
                }
            }
        );
        painelOpcoes.add(itemRaio);

        // sensibilidade
        itemSensi = ItemConfig.numerico(
            5, posItem(1, alturaItem, espacamento), larguraItem, alturaItem,
            "Sensibilidade:", String.format("%.2f", UI.sensi),
            fonteTexto, escalaItem, pixelBranco, visualBotao,
            new Acao() {
                public void exec() {
                    if(UI.sensi > 0f) {
                        UI.sensi -= 0.05f;
                        itemSensi.rotuloValor.texto = String.format("%.2f", UI.sensi);
                    }
                }
            },
            new Acao() {
                public void exec() {
                    if(UI.sensi < 5.0f) {
                        UI.sensi += 0.05f;
                        itemSensi.rotuloValor.texto = String.format("%.2f", UI.sensi);
                    }
                }
            }
        );
        painelOpcoes.add(itemSensi);

        // musicas
        itemMusicas = ItemConfig.alternar(
            5, posItem(2, alturaItem, espacamento), larguraItem, alturaItem,
            "Musicas:", Jogo.musicas ? "Ligado" : "Desligado",
            fonteTexto, escalaItem, pixelBranco, visualBotao,
            new Acao() {
                public void exec() {
                    Jogo.musicas = !Jogo.musicas;
                    itemMusicas.rotuloValor.texto = Jogo.musicas ? "Ligado" : "Desligado";
                    com.minimine.audio.Musicas.pausar();
                }
            }
        );
        painelOpcoes.add(itemMusicas);

        // distancia de renderizacao
        itemDistancia = ItemConfig.numerico(
            5, posItem(3, alturaItem, espacamento), larguraItem, alturaItem,
            "Distancia:", String.format("%.0f", UI.distancia),
            fonteTexto, escalaItem, pixelBranco, visualBotao,
            new Acao() {
                public void exec() {
                    if(UI.distancia > 200f) {
                        UI.distancia -= 50f;
                        itemDistancia.rotuloValor.texto = String.format("%.0f", UI.distancia);
                    }
                }
            },
            new Acao() {
                public void exec() {
                    if(UI.distancia < 1000f) {
                        UI.distancia += 50f;
                        itemDistancia.rotuloValor.texto = String.format("%.0f", UI.distancia);
                    }
                }
            }
        );
        painelOpcoes.add(itemDistancia);

        // campo de visao
        itemPOV = ItemConfig.numerico(
            5, posItem(4, alturaItem, espacamento), larguraItem, alturaItem,
            "Campo Visao:", String.valueOf(UI.pov),
            fonteTexto, escalaItem, pixelBranco, visualBotao,
            new Acao() {
                public void exec() {
                    if(UI.pov > 0) {
                        UI.pov -= 5;
                        itemPOV.rotuloValor.texto = String.valueOf(UI.pov);
                    }
                }
            },
            new Acao() {
                public void exec() {
                    if(UI.pov < 300) {
                        UI.pov += 5;
                        itemPOV.rotuloValor.texto = String.valueOf(UI.pov);
                    }
                }
            }
        );
        painelOpcoes.add(itemPOV);

		// debug:
        itemDebug = ItemConfig.alternar(
            5, posItem(5, alturaItem, espacamento), larguraItem, alturaItem,
            "Modo Debug:", UI.debug ? "Ligado" : "Desligado",
            fonteTexto, escalaItem, pixelBranco, visualBotao,
            new Acao() {
                public void exec() {
                    UI.debug = !UI.debug;
                    itemDebug.rotuloValor.texto = UI.debug ? "Ligado" : "Desligado";
                }
            }
        );
        painelOpcoes.add(itemDebug);

		// botões:
		itemBotoesTam = ItemConfig.numerico(
            5, posItem(4, alturaItem, espacamento), larguraItem, alturaItem,
            "Tamanho dos Botões:", String.valueOf(UI.botoesTam),
            fonteTexto, escalaItem, pixelBranco, visualBotao,
            new Acao() {
                public void exec() {
                    if(UI.pov > 0) {
                        UI.botoesTam -= 8;
                        itemBotoesTam.rotuloValor.texto = String.valueOf(UI.botoesTam);
                    }
                }
            },
            new Acao() {
                public void exec() {
                    if(UI.botoesTam < 64) {
                        UI.botoesTam += 8;
                        itemBotoesTam.rotuloValor.texto = String.valueOf(UI.botoesTam);
                    }
                }
            }
        );
        painelOpcoes.add(itemBotoesTam);

        // nome do jogador
        Rotulo rotuloNome = new Rotulo("Nome:", fonteTexto, escalaItem);
        rotuloNome.x = 5;
        rotuloNome.y = posItem(7, alturaItem, espacamento);
        rotuloNome.largura = 200;
        rotuloNome.altura = alturaItem;
        painelOpcoes.add(rotuloNome);

        campoNome = new CampoTexto(visualJanela, fonteTexto, 210, posItem(7, alturaItem, espacamento), 435, alturaItem, escalaItem);
        campoNome.defTexto(Jogo.nome);
        campoNome.limiteCaracteres = 16;
        campoNome.gerenciador = gerenciadorUI;
        campoNome.mudanca = new CampoTexto.Texto() {
            public void aoMudar(String novoTexto) {
                Jogo.nome = novoTexto;
            }
        };
        painelOpcoes.add(campoNome);

		// GUI:
		itemInterface = ItemConfig.alternar(
			5, posItem(8, alturaItem, espacamento), larguraItem, alturaItem,
			"Interface de jogo:", UI.gui ? "Ligado" : "Desligado",
			fonteTexto, escalaItem, pixelBranco, visualBotao,
			new Acao() {
				public void exec() {
					UI.gui = !UI.gui;
					itemInterface.rotuloValor.texto = UI.gui ? "Ligado" : "Desligado";
				}
			}
		);
        painelOpcoes.add(itemInterface);
        painelPrincipal.add(painelOpcoes);

        Acao acaoVoltar = new Acao() {
            public void exec() {
                prefs.putInteger("raioChunks", Mundo.RAIO_CHUNKS);
                prefs.putInteger("pov", UI.pov);
                prefs.putFloat("sensi", UI.sensi);
                prefs.putFloat("distancia", UI.distancia);
                prefs.putBoolean("musicas", Jogo.musicas);
				prefs.putBoolean("debug", UI.debug);
				prefs.putInteger("botoesTam", UI.botoesTam);
                prefs.putString("nome", campoNome.texto);
                prefs.flush();
                Inicio.defTela(Cenas.menu);
            }
        };
        Botao botaoVoltar = new Botao("VOLTAR", visualBotao, fonteTexto, 0, 0, 200, 60, escalaPixel, acaoVoltar);
        painelPrincipal.addAncorado(botaoVoltar, Ancora.INFERIOR_CENTRO, 0, 0);
        gerenciadorUI.add(painelPrincipal);
    }

    // calcula o y de cada item dentro do painel rolavel(indice 0 = topo)
    public float posItem(int indice, float alturaItem, float espacamento) {
        return 5 + indice * (alturaItem + espacamento);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.15f, 0.15f, 0.2f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();
        pincel.setProjectionMatrix(camera.combined);
        pincelFormas.setProjectionMatrix(camera.combined);

        pincel.begin();
        gerenciadorUI.desenhar(pincel, delta);
        pincel.end();
    }

    @Override
    public void resize(int v, int h) {
        vista.update(v, h);
    }

    @Override
    public void dispose() {
        if(pincel != null) pincel.dispose();
        if(pincelFormas != null) pincelFormas.dispose();
        if(fonteTitulo != null) fonteTitulo.dispose();
        if(fonteTexto != null) fonteTexto.dispose();
        if(pixelBranco != null) pixelBranco.dispose();
        gerenciadorUI.liberar();
    }

    @Override
    public void hide() {
        dispose();
    }
    @Override
    public void pause() {
        dispose();
    }
    @Override
    public void resume() {
        show();
    }

    @Override
    public boolean touchDown(int x, int y, int p, int b) {
        camera.unproject(toqueAuxiliar.set(x, y, 0));
        gerenciadorUI.processarToque(toqueAuxiliar.x, toqueAuxiliar.y, true);
        return true;
    }
    @Override
    public boolean touchUp(int x, int y, int p, int b) {
        camera.unproject(toqueAuxiliar.set(x, y, 0));
        gerenciadorUI.processarToque(toqueAuxiliar.x, toqueAuxiliar.y, false);
        return true;
    }
    @Override
    public boolean touchDragged(int x, int y, int p) {
        camera.unproject(toqueAuxiliar.set(x, y, 0));
        gerenciadorUI.processarArraste(toqueAuxiliar.x, toqueAuxiliar.y);
        return true;
    }
    @Override public boolean keyDown(int c) { return campoNome != null && campoNome.processarTecla(c); }
    @Override public boolean keyUp(int c) { return false; }
    @Override public boolean keyTyped(char c) { return campoNome != null && campoNome.processarCaractere(c); }
    @Override public boolean mouseMoved(int x, int y) { return false; }
    @Override public boolean scrolled(float aX, float aY) { return false; }
}
