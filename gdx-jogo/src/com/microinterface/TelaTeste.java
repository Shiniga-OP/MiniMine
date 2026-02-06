package com.microinterface;

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

public class TelaTeste implements Screen, InputProcessor {
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
    public Painel painelOpcoes;
    public Painel painelCreditos;
    public Painel painelAtual;

    public CaixaDialogo dialogoSair;
    public CaixaDialogo dialogoNovoJogo;

    public void show() {
        pincel = new SpriteBatch();
        pincelFormas = new ShapeRenderer();
        fonte = new BitmapFont();
        fonte.getRegion().getTexture().setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        camera = new OrthographicCamera();
        vista = new ScreenViewport(camera);
        vista.apply(true);

        gerenciadorUI = new GerenciadorUI();

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
    }

    public void criarInterface() {
        criarPainelMenu();
        criarPainelOpcoes();
        criarPainelCreditos();
        criarDialogos();

        painelAtual = painelMenu;
        gerenciadorUI.add(painelMenu);
    }

    public void criarPainelMenu() {
        painelMenu = new Painel(visualJanela, -250, -200, 500, 400, escalaPixel);
        painelMenu.defEspaco(20, 30);

        float larguraBotao = 300;
        float alturaBotao = 60;
		
        Acao acaoNovoJogo = new Acao() {
            public void exec() {
                CaixaDialogo.Fechar fechar = new CaixaDialogo.Fechar() {
                    public void aoFechar(boolean confirmou) {
                        if(confirmou) {
                            Gdx.app.log("JOGO", "Novo jogo");
                        }
                    }
                };
                dialogoNovoJogo.mostrar("Novo", "Iniciar?", fechar);
            }
        };
        Botao botaoNovoJogo = new Botao("Novo", visualBotao, fonte, 0, 0, larguraBotao, alturaBotao, escalaPixel, acaoNovoJogo);
        painelMenu.addAncorado(botaoNovoJogo, Ancoragem.CENTRO, 0, 60);

        Acao acaoContinuar = new Acao() {
            public void exec() {
                Gdx.app.log("JOGO", "Continuar");
            }
        };
        Botao botaoContinuar = new Botao("Continuar", visualBotao, fonte, 0, 0, larguraBotao, alturaBotao, escalaPixel, acaoContinuar);
        painelMenu.addAncorado(botaoContinuar, Ancoragem.CENTRO, 0, -15);

        Acao acaoOpcoes = new Acao() {
            public void exec() {
                trocarPainel(painelOpcoes);
            }
        };
        Botao botaoOpcoes = new Botao("Opcoes", visualBotao, fonte, 0, 0, larguraBotao, alturaBotao, escalaPixel, acaoOpcoes);
        painelMenu.addAncorado(botaoOpcoes, Ancoragem.CENTRO, 0, -90);

        Acao acaoCreditos = new Acao() {
            public void exec() {
                trocarPainel(painelCreditos);
            }
        };
        Botao botaoCreditos = new Botao("Creditos", visualBotao, fonte, 0, 0, larguraBotao, alturaBotao, escalaPixel, acaoCreditos);
        painelMenu.addAncorado(botaoCreditos, Ancoragem.CENTRO, 0, -165);

        Acao acaoSair = new Acao() {
            public void exec() {
                dialogoSair.mostrar("Sair", "Sair?", new CaixaDialogo.Fechar() {
						public void aoFechar(boolean confirmou) {
							if(confirmou) {
								Gdx.app.exit();
							}
						}
					});
            }
        };
        Botao botaoSair = new Botao("Sair", visualBotao, fonte, 0, 0, 150, 50, escalaPixel, acaoSair);
        painelMenu.addAncorado(botaoSair, Ancoragem.INFERIOR_DIREITO, 0, 0);
    }

    public void criarPainelOpcoes() {
        painelOpcoes = new Painel(visualJanela, -300, -200, 600, 400, escalaPixel);
        painelOpcoes.defEspaco(20, 30);
        painelOpcoes.corFundo = new Color(0.9f, 0.95f, 1.0f, 1f);

        Rotulo titulo = new Rotulo("OPCOES", fonte, escalaPixel);
        titulo.largura = 560;
        titulo.altura = 60;
        painelOpcoes.addAncorado(titulo, Ancoragem.SUPERIOR_CENTRO, 0, 0);

        Rotulo labelMusica = new Rotulo("Vol Musica:", fonte, escalaPixel * 0.8f);
        labelMusica.largura = 560;
        labelMusica.altura = 40;
        painelOpcoes.addAncorado(labelMusica, Ancoragem.CENTRO, 0, 60);

        Rotulo labelEfeitos = new Rotulo("Vol Efeitos:", fonte, escalaPixel * 0.8f);
        labelEfeitos.largura = 560;
        labelEfeitos.altura = 40;
        painelOpcoes.addAncorado(labelEfeitos, Ancoragem.CENTRO, 0, 0);

        Acao acaoVoltar = new Acao() {
            public void exec() {
                trocarPainel(painelMenu);
            }
        };
        Botao botaoVoltar = new Botao("Voltar", visualBotao, fonte, 0, 0, 200, 50, escalaPixel, acaoVoltar);
        painelOpcoes.addAncorado(botaoVoltar, Ancoragem.INFERIOR_CENTRO, 0, 0);
    }

    public void criarPainelCreditos() {
        painelCreditos = new Painel(visualJanela, -300, -250, 600, 500, escalaPixel);
        painelCreditos.defEspaco(20, 30);
        painelCreditos.corFundo = new Color(1.0f, 0.95f, 0.9f, 1f);

        Rotulo titulo = new Rotulo("CREDITOS", fonte, escalaPixel);
        titulo.largura = 560;
        titulo.altura = 60;
        painelCreditos.addAncorado(titulo, Ancoragem.SUPERIOR_CENTRO, 0, 0);

        String[] creditos = {
            "Dev: Shiniga",
            "Arte: Shiniga",
            "Som: VDLN7",
            "UI: MicroUI"
        };
        float posY = 40;
        for(int i = 0; i < creditos.length; i++) {
            Rotulo linha = new Rotulo(creditos[i], fonte, escalaPixel * 0.7f);
            linha.largura = 560;
            linha.altura = 35;
            painelCreditos.addAncorado(linha, Ancoragem.CENTRO, 0, posY);
            posY -= 50;
        }
        Acao acaoVoltar = new Acao() {
            public void exec() {
                trocarPainel(painelMenu);
            }
        };
        Botao botaoVoltar = new Botao("Voltar", visualBotao, fonte, 0, 0, 200, 50, escalaPixel, acaoVoltar);
        painelCreditos.addAncorado(botaoVoltar, Ancoragem.INFERIOR_CENTRO, 0, 0);
    }

    public void criarDialogos() {
        dialogoSair = new CaixaDialogo(visualJanela, fonte, escalaPixel, pincelFormas);
        dialogoSair.addOk(visualBotao);
        dialogoSair.addCancelar(visualBotao);
        gerenciadorUI.addDialogo(dialogoSair);

        dialogoNovoJogo = new CaixaDialogo(visualJanela, fonte, escalaPixel, pincelFormas);
        dialogoNovoJogo.addOk(visualBotao);
        dialogoNovoJogo.addCancelar(visualBotao);
        gerenciadorUI.addDialogo(dialogoNovoJogo);
    }

    public void trocarPainel(Painel novoPainel) {
        gerenciadorUI.rm(painelAtual);
        painelAtual = novoPainel;
        gerenciadorUI.add(painelAtual);
    }
	@Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.05f, 0.1f, 0.15f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

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
    }
	@Override
    public void pause() {}
    public void resume() {}
    public void hide() {}
    public boolean keyDown(int k) { return false; }
    public boolean keyUp(int k) { return false; }
    public boolean keyTyped(char c) { return false; }
    public boolean mouseMoved(int x, int y) { return false; }
    public boolean scrolled(float a, float b) { return false; }
}

