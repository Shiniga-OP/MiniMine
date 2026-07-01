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
import com.minimine.Inicio;
import com.micro.util.GerenciadorUI;
import com.micro.janelas.Painel;
import com.micro.janelas.PainelFatiado;
import com.micro.componentes.CampoTexto;
import com.micro.componentes.Botao;
import com.micro.componentes.Rotulo;
import com.micro.util.Ancora;
import com.minimine.ui.InterUtil;
import com.micro.janelas.Lista;
import com.micro.util.FabricaUtil;

public class Config implements Screen {
    public SpriteBatch pincel;
    public BitmapFont fonteTitulo;
    public BitmapFont fonteTexto;
    
    public Preferences prefs;

    public GerenciadorUI gerenciadorUI;
    public PainelFatiado visualJanela;
    public PainelFatiado visualBotao;
    public Texture pixelBranco;
    public float escalaPixel;

    public Painel painelPrincipal;
	
    // referencias aos itens para atualizar valores no render
    public Rotulo itemRaio, itemSensi, itemDistancia, itemPOV, itemBotoesTam;
	public Botao itemMusicas, itemDebug, itemInterface;
    public CampoTexto campoNome;
	
	public Texture texturaUi;
	
    @Override
    public void show() {
        pincel = new SpriteBatch();
        fonteTitulo = InterUtil.carregarFonte("fontes/pixel-16.fnt", 2f);
        fonteTexto = InterUtil.carregarFonte("fontes/pixel-16.fnt", 1.5f);
        
        escalaPixel = 4.0f;

        final Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(1, 1, 1, 1);
        pixmap.fill();
        pixelBranco = new Texture(pixmap);
        pixmap.dispose();

        prefs = Gdx.app.getPreferences("MiniConfig");
        gerenciadorUI = new GerenciadorUI();

        try {
            texturaUi = new Texture(Gdx.files.internal("texturas/ui/base.png"));
            
            visualJanela = new PainelFatiado(texturaUi);
            visualBotao = new PainelFatiado(texturaUi);
            criarInterface();
        } catch(Exception e) {
            Gdx.app.log("ERRO", "Recursos não encontrados: " + e.getMessage());
        }
        Gdx.input.setInputProcessor(gerenciadorUI);
    }
	
    public void criarInterface() {
        painelPrincipal = new Painel(visualJanela, -350, -350, 700, 700, escalaPixel);
        painelPrincipal.defEspaco(20, 30);

        Rotulo titulo = new Rotulo("CONFIGURAÇÕES", fonteTitulo, escalaPixel);
        titulo.largura = 660;
        titulo.altura = 60;
        painelPrincipal.addAncorado(titulo, Ancora.SUPERIOR_CENTRO, 0, 0);

        // painel rolavel ocupa o espaco entre o titulo e o botao voltar
        Lista painelOpcoes = new Lista(visualJanela, 20, 80, 660, 530, escalaPixel, pixelBranco);
        painelOpcoes.defEspaco(0.5f);

        float larguraItem = 650;
        float alturaItem = 75;
        float espacamento = 6;
        float escalaItem = escalaPixel * 0.75f;

        // raio de chunks
        itemRaio = FabricaUtil.criarConfigNum(painelOpcoes,
            5, posItem(0, alturaItem, espacamento), larguraItem, alturaItem,
            "Raio Chunks:", String.valueOf(Mundo.RAIO_CHUNKS),
            fonteTexto, escalaItem, pixelBranco, visualBotao,
            new Runnable() {
                public void run() {
                    if(Mundo.RAIO_CHUNKS > 1) {
                        Mundo.RAIO_CHUNKS--;
                        itemRaio.defTexto(Mundo.RAIO_CHUNKS);
                    }
                }
            },
            new Runnable() {
                public void run() {
                    if(Mundo.RAIO_CHUNKS < 20) {
                        Mundo.RAIO_CHUNKS++;
                        itemRaio.defTexto(Mundo.RAIO_CHUNKS);
                    }
                }
            }
        );
        
        // sensibilidade
        itemSensi = FabricaUtil.criarConfigNum(painelOpcoes,
            5, posItem(1, alturaItem, espacamento), larguraItem, alturaItem,
            "Sensibilidade:", String.format("%.2f", UI.sensi),
            fonteTexto, escalaItem, pixelBranco, visualBotao,
            new Runnable() {
                public void run() {
                    if(UI.sensi > 0f) {
                        UI.sensi -= 0.05f;
                        itemSensi.defTexto(String.format("%.2f", UI.sensi));
                    }
                }
            },
            new Runnable() {
                public void run() {
                    if(UI.sensi < 5.0f) {
                        UI.sensi += 0.05f;
                        itemSensi.defTexto(String.format("%.2f", UI.sensi));
                    }
                }
            }
        );
        painelOpcoes.addItem(itemSensi);

        // musicas
        itemMusicas = FabricaUtil.criarSelecao(
            5, posItem(2, alturaItem, espacamento), larguraItem, alturaItem,
            "Musicas:", Jogo.musicas,
            fonteTexto, escalaItem, pixelBranco, visualBotao,
            new Runnable() {
                public void run() {
                    Jogo.musicas = !Jogo.musicas;
                    com.minimine.audio.Musicas.pausar();
                }
            }
        );
        painelOpcoes.addItem(itemMusicas);

        // distancia de renderizacao
        itemDistancia = FabricaUtil.criarConfigNum(painelOpcoes,
            5, posItem(3, alturaItem, espacamento), larguraItem, alturaItem,
            "Distancia:", String.format("%.0f", UI.distancia),
            fonteTexto, escalaItem, pixelBranco, visualBotao,
            new Runnable() {
                public void run() {
                    if(UI.distancia > 200f) {
                        UI.distancia -= 50f;
                        itemDistancia.defTexto(String.format("%.0f", UI.distancia));
                    }
                }
            },
            new Runnable() {
                public void run() {
                    if(UI.distancia < 1000f) {
                        UI.distancia += 50f;
                        itemDistancia.defTexto(String.format("%.0f", UI.distancia));
                    }
                }
            }
        );
        // campo de visao
        itemPOV = FabricaUtil.criarConfigNum(painelOpcoes,
            5, posItem(4, alturaItem, espacamento), larguraItem, alturaItem,
            "Campo Visão:", String.valueOf(UI.pov),
            fonteTexto, escalaItem, pixelBranco, visualBotao,
            new Runnable() {
                public void run() {
                    if(UI.pov > 0) {
                        UI.pov -= 5;
                        itemPOV.defTexto(String.valueOf(UI.pov));
                    }
                }
            },
            new Runnable() {
                public void run() {
                    if(UI.pov < 300) {
                        UI.pov += 5;
                        itemPOV.defTexto(String.valueOf(UI.pov));
                    }
                }
            }
        );
		// debug:
        itemDebug = FabricaUtil.criarSelecao(
            5, posItem(5, alturaItem, espacamento), larguraItem, alturaItem,
            "Modo Debug:", UI.debug,
            fonteTexto, escalaItem, pixelBranco, visualBotao,
            new Runnable() {
                public void run() {
                    UI.debug = !UI.debug;
                }
            }
        );
        painelOpcoes.addItem(itemDebug);

		// botões:
		itemBotoesTam = FabricaUtil.criarConfigNum(painelOpcoes,
            5, posItem(4, alturaItem, espacamento), larguraItem, alturaItem,
            "Tamanho dos Botões:", String.valueOf(UI.botoesTam),
            fonteTexto, escalaItem, pixelBranco, visualBotao,
            new Runnable() {
                public void run() {
                    if(UI.pov > 0) {
                        UI.botoesTam -= 8;
                        itemBotoesTam.defTexto(String.valueOf(UI.botoesTam));
                    }
                }
            },
            new Runnable() {
                public void run() {
                    if(UI.botoesTam < 64) {
                        UI.botoesTam += 8;
                        itemBotoesTam.defTexto(String.valueOf(UI.botoesTam));
                    }
                }
            }
        );
        // nome do jogador
        Rotulo rotuloNome = new Rotulo("Nome:", fonteTexto, escalaItem);
        rotuloNome.x = 5;
        rotuloNome.y = posItem(7, alturaItem, espacamento);
        rotuloNome.largura = 200;
        rotuloNome.altura = alturaItem;
        painelOpcoes.addItem(rotuloNome);

        campoNome = new CampoTexto(visualJanela, fonteTexto, 210, posItem(7, alturaItem, espacamento), 435, alturaItem, escalaItem);
        campoNome.defTexto(Jogo.nome);
        campoNome.limiteCaracteres = 16;
        
        campoNome.mudanca = new CampoTexto.Texto() {
            public void aoMudar(String novoTexto) {
                Jogo.nome = novoTexto;
            }
        };
        painelOpcoes.addItem(campoNome);

		// GUI:
		itemInterface = FabricaUtil.criarSelecao(
			5, posItem(8, alturaItem, espacamento), larguraItem, alturaItem,
			"Interface de jogo:", UI.gui,
			fonteTexto, escalaItem, pixelBranco, visualBotao,
			new Runnable() {
				public void run() {
					UI.gui = !UI.gui;
				}
			}
		);
        painelOpcoes.addItem(itemInterface);
        painelPrincipal.add(painelOpcoes);

        Runnable acaoVoltar = new Runnable() {
            public void run() {
                prefs.putInteger("raioChunks", Mundo.RAIO_CHUNKS);
                prefs.putInteger("pov", UI.pov);
                prefs.putFloat("sensi", UI.sensi);
                prefs.putFloat("distancia", UI.distancia);
                prefs.putBoolean("musicas", Jogo.musicas);
				prefs.putBoolean("debug", UI.debug);
				prefs.putInteger("botoesTam", UI.botoesTam);
                prefs.putString("nome", campoNome.texto);
                prefs.flush();
                Inicio.tela.setScreen(new Menu());
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

        pincel.begin();
        gerenciadorUI.desenhar(pincel, delta);
        pincel.end();
    }

    @Override
    public void resize(int v, int h) {
        gerenciadorUI.ajustar(v, h);
    }

    @Override
    public void dispose() {
        if(pincel != null) pincel.dispose();
        if(pixelBranco != null) pixelBranco.dispose();
		texturaUi.dispose();
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
}
