package com.minimine.cenas;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.Preferences;
import com.minimine.Inicio;
import com.minimine.Cenas;
import com.minimine.mundo.Mundo;
import com.minimine.ui.UI;
import com.minimine.ui.InterUtil;
import com.microinterface.GerenciadorUI;
import com.microinterface.PainelFatiado;
import com.microinterface.Painel;
import com.microinterface.CaixaDialogo;
import com.microinterface.Rotulo;
import com.microinterface.Ancora;
import com.microinterface.Acao;
import com.microinterface.Botao;

public class Menu implements Screen, InputProcessor {
    public SpriteBatch pincel;
    public ShapeRenderer pincelFormas;
    public BitmapFont fonte;
    public OrthographicCamera camera;
    public Viewport vista;
    public Vector3 toqueAuxiliar = new Vector3();

    public GerenciadorUI gerenciadorUI;
    public PainelFatiado visualJanela;
    public PainelFatiado visualBotao;
    public float escalaPixel = 4.0f;

    public Painel painelMenu;
    public CaixaDialogo dialogoSair;
    
    public static Preferences prefs;

    @Override
    public void show() {
        pincel = new SpriteBatch();
        pincelFormas = new ShapeRenderer();
        fonte = new BitmapFont();
        fonte.getRegion().getTexture().setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        camera = new OrthographicCamera();
        vista = new ScreenViewport(camera);
        vista.apply(true);

        gerenciadorUI = new GerenciadorUI();
        
        prefs = Gdx.app.getPreferences("MiniConfig");

        try {
            Texture textura = new Texture(Gdx.files.internal("ui/base.png"));
            textura.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            visualJanela = new PainelFatiado(textura);
            visualBotao = new PainelFatiado(textura);

            criarInterface();
        } catch(Exception e) {
            Gdx.app.log("ERRO", "Recursos nao encontrados: " + e.getMessage());
        } 
        Gdx.input.setInputProcessor(this);
        
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());  
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glCullFace(GL20.GL_BACK);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        
        // carregar preferencias
        Mundo.RAIO_CHUNKS = prefs.getInteger("raioChunks", Mundo.RAIO_CHUNKS);
        UI.pov = prefs.getInteger("pov", UI.pov);
        UI.sensi = prefs.getFloat("sensi", UI.sensi);
        UI.distancia = prefs.getFloat("distancia", UI.distancia);
        UI.aprox = prefs.getFloat("aprox", UI.aprox);
    }

    public void criarInterface() {
        criarPainelMenu();
        criarDialogos();

        gerenciadorUI.add(painelMenu);
    }

    public void criarPainelMenu() {
        painelMenu = new Painel(visualJanela, -300, -250, 600, 500, escalaPixel);
        painelMenu.defEspaco(20, 30);
        painelMenu.corFundo = new Color(0.1f, 0.15f, 0.2f, 1f);

        // titulo
        Rotulo titulo = new Rotulo("MiniMine", fonte, escalaPixel * 1.2f);
        titulo.largura = 560;
        titulo.altura = 80;
        painelMenu.addAncorado(titulo, Ancora.SUPERIOR_CENTRO, 0, 0);

        float larguraBotao = 400;
        float alturaBotao = 70;

        // botão um jogador
        Acao acaoJogar = new Acao() {
            public void exec() {
                Inicio.defTela(Cenas.selecao);
            }
        };
        Botao botaoJogar = new Botao("Um Jogador", visualBotao, fonte, 0, 0, larguraBotao, alturaBotao, escalaPixel, acaoJogar);
        painelMenu.addAncorado(botaoJogar, Ancora.CENTRO, 0, 50);

        // botão Configurações
        Acao acaoConfig = new Acao() {
            public void exec() {
                Inicio.defTela(Cenas.configuracoes);
            }
        };
        Botao botaoConfig = new Botao("Configuracoes", visualBotao, fonte, 0, 0, larguraBotao, alturaBotao, escalaPixel, acaoConfig);
        painelMenu.addAncorado(botaoConfig, Ancora.CENTRO, 0, -50);

        // botão sair
        Acao acaoSair = new Acao() {
            public void exec() {
                dialogoSair.mostrar("Sair", "Deseja sair do jogo?", new CaixaDialogo.Fechar() {
                    public void aoFechar(boolean confirmou) {
                        if(confirmou) {
                            Gdx.app.exit();
                        }
                    }
                });
            }
        };
        Botao botaoSair = new Botao("Sair", visualBotao, fonte, 0, 0, 200, 60, escalaPixel, acaoSair);
        painelMenu.addAncorado(botaoSair, Ancora.INFERIOR_CENTRO, 0, 0);
    }

    public void criarDialogos() {
        dialogoSair = new CaixaDialogo(visualJanela, fonte, escalaPixel, pincelFormas);
        dialogoSair.addOk(visualBotao);
        dialogoSair.addCancelar(visualBotao);
        gerenciadorUI.addDialogo(dialogoSair);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        Gdx.gl.glDisable(GL20.GL_CULL_FACE);

        camera.update();
        pincel.setProjectionMatrix(camera.combined);
        pincelFormas.setProjectionMatrix(camera.combined);

        pincel.begin();
        gerenciadorUI.desenhar(pincel, delta);
        pincel.end();
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

    @Override
    public void resize(int v, int h) {
        vista.update(v, h);
    }

    @Override
    public void dispose() {
        pincel.dispose();
        pincelFormas.dispose();
        fonte.dispose();
		gerenciadorUI.liberar();
    }
    @Override
    public void pause() {}
    public void resume() {}
    public void hide() {
		dispose();
	}
    public boolean keyDown(int k) { return false; }
    public boolean keyUp(int k) { return false; }
    public boolean keyTyped(char c) { return false; }
    public boolean mouseMoved(int x, int y) { return false; }
    public boolean scrolled(float a, float b) { return false; }
}
