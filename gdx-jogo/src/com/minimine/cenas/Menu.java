package com.minimine.cenas;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import com.minimine.Inicio;
import com.minimine.ui.UI;
import com.minimine.ui.InterUtil;
import com.minimine.servidor.Net;
import com.minimine.utils.ArquivosUtil;
import com.minimine.mundo.Mundo;

import com.micro.componentes.Botao;
import com.micro.janelas.Painel;
import com.micro.componentes.Rotulo;
import com.micro.util.Ancora;
import com.micro.componentes.CaixaDialogo;
import com.micro.janelas.PainelFatiado;
import com.micro.util.GerenciadorUI;
import com.minimine.audio.Musicas;
import com.minimine.graficos.Render;

public class Menu implements Screen {
    public SpriteBatch pincel;
    public BitmapFont fonte;
    
    public GerenciadorUI gerenciadorUI;
    public PainelFatiado visualJanela;
    public PainelFatiado visualBotao;
    public float escalaPixel = 4.0f;

    public Painel painelMenu;
    public CaixaDialogo dialogoSair;

    public Preferences prefs;
	
	public Texture texturaUi;

    @Override
    public void show() {
        pincel = new SpriteBatch();
        fonte = InterUtil.carregarFonte("fontes/pixel-16.fnt");

        gerenciadorUI = new GerenciadorUI();

        prefs = Gdx.app.getPreferences("MiniConfig");

        try {
            texturaUi = new Texture(Gdx.files.internal("texturas/ui/base.png"));
            visualJanela = new PainelFatiado(texturaUi);
            visualBotao = new PainelFatiado(texturaUi);
            criarInterface();
        } catch(Exception e) {
            Gdx.app.log("ERRO", "Recursos nao encontrados: " + e.getMessage());
        }
        Gdx.input.setInputProcessor(gerenciadorUI);
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glCullFace(GL20.GL_BACK);
        Gdx.gl.glEnable(GL20.GL_BLEND);

        Mundo.RAIO_CHUNKS = prefs.getInteger("raioChunks", Mundo.RAIO_CHUNKS);
        UI.pov = prefs.getInteger("pov", UI.pov);
        UI.sensi = prefs.getFloat("sensi", UI.sensi);
        UI.distancia = prefs.getFloat("distancia", UI.distancia);
        Jogo.musicas = prefs.getBoolean("musicas", Jogo.musicas);
		UI.debug = prefs.getBoolean("debug", UI.debug);
		UI.botoesTam = prefs.getInteger("botoesTam", UI.botoesTam);
		Jogo.nome = prefs.getString("nome", Jogo.nome);
        Gdx.input.setCursorCatched(false);

		Musicas.pausar();
    }

    public void criarInterface() {
        criarPainelMenu();
        criarDialogos();
        gerenciadorUI.add(painelMenu);
    }

    public void criarPainelMenu() {
        painelMenu = new Painel(visualJanela, -300, -300, 600, 580, escalaPixel);
        painelMenu.defEspaco(20, 30);
        painelMenu.corFundo = new Color(0.1f, 0.15f, 0.2f, 1f);

        Rotulo titulo = new Rotulo("MiniMine", fonte, escalaPixel * 1.2f);
        titulo.largura = 560;
        titulo.altura = 80;
        painelMenu.addAncorado(titulo, Ancora.SUPERIOR_CENTRO, 0, 0);

        // versão no canto inferior esquerdo do painel
        Rotulo rotuloVersao = new Rotulo(ArquivosUtil.versao, fonte, escalaPixel * 0.5f);
        rotuloVersao.largura = 120;
        rotuloVersao.altura = 30;
        painelMenu.addAncorado(rotuloVersao, Ancora.INFERIOR_ESQUERDO, 5, 5);

        float larguraBotao = 400;
        float alturaBotao = 70;

        Runnable acaoJogar = new Runnable() {
            public void run() {
                Inicio.tela.setScreen(new MundoMenu());
            }
        };
        Botao botaoJogar = new Botao("Um Jogador", visualBotao, fonte, 0, 0, larguraBotao, alturaBotao, escalaPixel, acaoJogar);
        painelMenu.addAncorado(botaoJogar, Ancora.CENTRO, 0, 100);

        Runnable acaoMulti = new Runnable() {
            public void run() {
                Inicio.tela.setScreen(new MultiMenu());
            }
        };
        Botao botaoMulti = new Botao("Multijogador", visualBotao, fonte, 0, 0, larguraBotao, alturaBotao, escalaPixel, acaoMulti);
        painelMenu.addAncorado(botaoMulti, Ancora.CENTRO, 0, 0);

        Runnable acaoConfig = new Runnable() {
            public void run() {
                Inicio.tela.setScreen(new Config());
            }
        };
        Botao botaoConfig = new Botao("Configurações", visualBotao, fonte, 0, 0, larguraBotao, alturaBotao, escalaPixel, acaoConfig);
        painelMenu.addAncorado(botaoConfig, Ancora.CENTRO, 0, -100);

        Runnable acaoSair = new Runnable() {
            public void run() {
                dialogoSair.mostrar("Sair", "Deseja sair do jogo?", new CaixaDialogo.Fechar() {
						public void confirmou(boolean confirmou) {
							if(confirmou) Gdx.app.exit();
						}
					});
            }
        };
        Botao botaoSair = new Botao("Sair", visualBotao, fonte, 0, 0, 200, 60, escalaPixel, acaoSair);
        painelMenu.addAncorado(botaoSair, Ancora.INFERIOR_CENTRO, 0, 0);
    }

    public void criarDialogos() {
        dialogoSair = new CaixaDialogo(visualJanela, fonte, escalaPixel);
        dialogoSair.addOk(visualBotao);
        dialogoSair.addCancelar(visualBotao);
        gerenciadorUI.add(dialogoSair);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        Gdx.gl.glDisable(GL20.GL_CULL_FACE);

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
        pincel.dispose();
        gerenciadorUI.liberar();
		texturaUi.dispose();
    }

    @Override
    public void hide() {
        dispose();
    }

    @Override public void pause() {}
    @Override public void resume() {}
}
