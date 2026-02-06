package com.minimine.cenas;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
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
import com.microinterface.GerenciadorUI;
import com.microinterface.Painel;
import com.microinterface.PainelFatiado;
import com.microinterface.Botao;
import com.microinterface.Rotulo;
import com.microinterface.Ancoragem;
import com.microinterface.Acao;

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
    public float escalaPixel;
    
    public Painel painelPrincipal;
    
    public Rotulo rotuloRaioValor;
    public Rotulo rotuloSensiValor;
    public Rotulo rotuloAproxValor;
    public Rotulo rotuloDistanciaValor;
    public Rotulo rotuloPOVValor;

    @Override
    public void show() {
        pincel = new SpriteBatch();
        pincelFormas = new ShapeRenderer();
        
        // carrega fontes
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
        
        prefs = Gdx.app.getPreferences("MiniConfig");
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
        painelPrincipal = new Painel(visualJanela, -350, -350, 700, 700, escalaPixel);
        painelPrincipal.defEspaco(20, 30);
        
        // titulo
        Rotulo titulo = new Rotulo("CONFIGURACOES", fonteTitulo, escalaPixel);
        titulo.largura = 660;
        titulo.altura = 60;
        painelPrincipal.addAncorado(titulo, Ancoragem.SUPERIOR_CENTRO, 0, 0);
        
        float larguraLabel = 250;
        float larguraValor = 100;
        float larguraBotao = 60;
        float alturaBotao = 50;
        float espacoY = 80;
        float posYInicial = 150;
        
        // === RAIO DE CHUNKS ===
        Rotulo labelRaio = new Rotulo("Raio Chunks:", fonteTexto, escalaPixel * 0.8f);
        labelRaio.largura = larguraLabel;
        labelRaio.altura = alturaBotao;
        painelPrincipal.addAncorado(labelRaio, Ancoragem.CENTRO, -200, posYInicial);
        
        rotuloRaioValor = new Rotulo(String.valueOf(Mundo.RAIO_CHUNKS), fonteTexto, escalaPixel * 0.8f);
        rotuloRaioValor.largura = larguraValor;
        rotuloRaioValor.altura = alturaBotao;
        painelPrincipal.addAncorado(rotuloRaioValor, Ancoragem.CENTRO, 50, posYInicial);
        
        Acao acaoDiminuirRaio = new Acao() {
            public void exec() {
                if(Mundo.RAIO_CHUNKS > 1) {
                    Mundo.RAIO_CHUNKS--;
                    rotuloRaioValor.texto = String.valueOf(Mundo.RAIO_CHUNKS);
                }
            }
        };
        Botao botaoDiminuirRaio = new Botao("-", visualBotao, fonteTexto, 0, 0, larguraBotao, alturaBotao, escalaPixel, acaoDiminuirRaio);
        painelPrincipal.addAncorado(botaoDiminuirRaio, Ancoragem.CENTRO, 160, posYInicial);
        
        Acao acaoAumentarRaio = new Acao() {
            public void exec() {
                if(Mundo.RAIO_CHUNKS < 20) {
                    Mundo.RAIO_CHUNKS++;
                    rotuloRaioValor.texto = String.valueOf(Mundo.RAIO_CHUNKS);
                }
            }
        };
        Botao botaoAumentarRaio = new Botao("+", visualBotao, fonteTexto, 0, 0, larguraBotao, alturaBotao, escalaPixel, acaoAumentarRaio);
        painelPrincipal.addAncorado(botaoAumentarRaio, Ancoragem.CENTRO, 230, posYInicial);
        
        // === SENSIBILIDADE ===
        Rotulo labelSensi = new Rotulo("Sensibilidade:", fonteTexto, escalaPixel * 0.8f);
        labelSensi.largura = larguraLabel;
        labelSensi.altura = alturaBotao;
        painelPrincipal.addAncorado(labelSensi, Ancoragem.CENTRO, -200, posYInicial - espacoY);
        
        rotuloSensiValor = new Rotulo(String.format("%.2f", UI.sensi), fonteTexto, escalaPixel * 0.8f);
        rotuloSensiValor.largura = larguraValor;
        rotuloSensiValor.altura = alturaBotao;
        painelPrincipal.addAncorado(rotuloSensiValor, Ancoragem.CENTRO, 50, posYInicial - espacoY);
        
        Acao acaoDiminuirSensi = new Acao() {
            public void exec() {
                if(UI.sensi > 0f) {
                    UI.sensi -= 0.05f;
                    rotuloSensiValor.texto = String.format("%.2f", UI.sensi);
                }
            }
        };
        Botao botaoDiminuirSensi = new Botao("-", visualBotao, fonteTexto, 0, 0, larguraBotao, alturaBotao, escalaPixel, acaoDiminuirSensi);
        painelPrincipal.addAncorado(botaoDiminuirSensi, Ancoragem.CENTRO, 160, posYInicial - espacoY);
        
        Acao acaoAumentarSensi = new Acao() {
            public void exec() {
                if(UI.sensi < 5.0f) {
                    UI.sensi += 0.05f;
                    rotuloSensiValor.texto = String.format("%.2f", UI.sensi);
                }
            }
        };
        Botao botaoAumentarSensi = new Botao("+", visualBotao, fonteTexto, 0, 0, larguraBotao, alturaBotao, escalaPixel, acaoAumentarSensi);
        painelPrincipal.addAncorado(botaoAumentarSensi, Ancoragem.CENTRO, 230, posYInicial - espacoY);
        
        // === APROXIMACAO ===
        Rotulo labelAprox = new Rotulo("Aproximacao:", fonteTexto, escalaPixel * 0.8f);
        labelAprox.largura = larguraLabel;
        labelAprox.altura = alturaBotao;
        painelPrincipal.addAncorado(labelAprox, Ancoragem.CENTRO, -200, posYInicial - espacoY * 2);
        
        rotuloAproxValor = new Rotulo(String.format("%.1f", UI.aprox), fonteTexto, escalaPixel * 0.8f);
        rotuloAproxValor.largura = larguraValor;
        rotuloAproxValor.altura = alturaBotao;
        painelPrincipal.addAncorado(rotuloAproxValor, Ancoragem.CENTRO, 50, posYInicial - espacoY * 2);
        
        Acao acaoDiminuirAprox = new Acao() {
            public void exec() {
                if(UI.aprox > 0.1f) {
                    UI.aprox -= 0.1f;
                    rotuloAproxValor.texto = String.format("%.1f", UI.aprox);
                }
            }
        };
        Botao botaoDiminuirAprox = new Botao("-", visualBotao, fonteTexto, 0, 0, larguraBotao, alturaBotao, escalaPixel, acaoDiminuirAprox);
        painelPrincipal.addAncorado(botaoDiminuirAprox, Ancoragem.CENTRO, 160, posYInicial - espacoY * 2);
        
        Acao acaoAumentarAprox = new Acao() {
            public void exec() {
                if(UI.aprox < 200f) {
                    UI.aprox += 0.1f;
                    rotuloAproxValor.texto = String.format("%.1f", UI.aprox);
                }
            }
        };
        Botao botaoAumentarAprox = new Botao("+", visualBotao, fonteTexto, 0, 0, larguraBotao, alturaBotao, escalaPixel, acaoAumentarAprox);
        painelPrincipal.addAncorado(botaoAumentarAprox, Ancoragem.CENTRO, 230, posYInicial - espacoY * 2);
        
        // === DISTANCIA ===
        Rotulo labelDistancia = new Rotulo("Distancia:", fonteTexto, escalaPixel * 0.8f);
        labelDistancia.largura = larguraLabel;
        labelDistancia.altura = alturaBotao;
        painelPrincipal.addAncorado(labelDistancia, Ancoragem.CENTRO, -200, posYInicial - espacoY * 3);
        
        rotuloDistanciaValor = new Rotulo(String.format("%.0f", UI.distancia), fonteTexto, escalaPixel * 0.8f);
        rotuloDistanciaValor.largura = larguraValor;
        rotuloDistanciaValor.altura = alturaBotao;
        painelPrincipal.addAncorado(rotuloDistanciaValor, Ancoragem.CENTRO, 50, posYInicial - espacoY * 3);
        
        Acao acaoDiminuirDistancia = new Acao() {
            public void exec() {
                if(UI.distancia > 200f) {
                    UI.distancia -= 50f;
                    rotuloDistanciaValor.texto = String.format("%.0f", UI.distancia);
                }
            }
        };
        Botao botaoDiminuirDistancia = new Botao("-", visualBotao, fonteTexto, 0, 0, larguraBotao, alturaBotao, escalaPixel, acaoDiminuirDistancia);
        painelPrincipal.addAncorado(botaoDiminuirDistancia, Ancoragem.CENTRO, 160, posYInicial - espacoY * 3);
        
        Acao acaoAumentarDistancia = new Acao() {
            public void exec() {
                if(UI.distancia < 1000f) {
                    UI.distancia += 50f;
                    rotuloDistanciaValor.texto = String.format("%.0f", UI.distancia);
                }
            }
        };
        Botao botaoAumentarDistancia = new Botao("+", visualBotao, fonteTexto, 0, 0, larguraBotao, alturaBotao, escalaPixel, acaoAumentarDistancia);
        painelPrincipal.addAncorado(botaoAumentarDistancia, Ancoragem.CENTRO, 230, posYInicial - espacoY * 3);
        
        // === CAMPO DE VISAO(POV) ===
        Rotulo labelPOV = new Rotulo("Campo Visao:", fonteTexto, escalaPixel * 0.8f);
        labelPOV.largura = larguraLabel;
        labelPOV.altura = alturaBotao;
        painelPrincipal.addAncorado(labelPOV, Ancoragem.CENTRO, -200, posYInicial - espacoY * 4);
        
        rotuloPOVValor = new Rotulo(String.valueOf(UI.pov), fonteTexto, escalaPixel * 0.8f);
        rotuloPOVValor.largura = larguraValor;
        rotuloPOVValor.altura = alturaBotao;
        painelPrincipal.addAncorado(rotuloPOVValor, Ancoragem.CENTRO, 50, posYInicial - espacoY * 4);
        
        Acao acaoDiminuirPOV = new Acao() {
            public void exec() {
                if(UI.pov > 0) {
                    UI.pov -= 5;
                    rotuloPOVValor.texto = String.valueOf(UI.pov);
                }
            }
        };
        Botao botaoDiminuirPOV = new Botao("-", visualBotao, fonteTexto, 0, 0, larguraBotao, alturaBotao, escalaPixel, acaoDiminuirPOV);
        painelPrincipal.addAncorado(botaoDiminuirPOV, Ancoragem.CENTRO, 160, posYInicial - espacoY * 4);
        
        Acao acaoAumentarPOV = new Acao() {
            public void exec() {
                if(UI.pov < 300) {
                    UI.pov += 5;
                    rotuloPOVValor.texto = String.valueOf(UI.pov);
                }
            }
        };
        Botao botaoAumentarPOV = new Botao("+", visualBotao, fonteTexto, 0, 0, larguraBotao, alturaBotao, escalaPixel, acaoAumentarPOV);
        painelPrincipal.addAncorado(botaoAumentarPOV, Ancoragem.CENTRO, 230, posYInicial - espacoY * 4);
        
        // === BOTAO VOLTAR ===
        Acao acaoVoltar = new Acao() {
            public void exec() {
                prefs.putInteger("raioChunks", Mundo.RAIO_CHUNKS);
                prefs.putInteger("pov", UI.pov);
                prefs.putFloat("sensi", UI.sensi);
                prefs.putFloat("aprox", UI.aprox);
                prefs.putFloat("distancia", UI.distancia);
                prefs.flush();
                Inicio.defTela(Cenas.menu);
            }
        };
        Botao botaoVoltar = new Botao("VOLTAR", visualBotao, fonteTexto, 0, 0, 200, 60, escalaPixel, acaoVoltar);
        painelPrincipal.addAncorado(botaoVoltar, Ancoragem.INFERIOR_CENTRO, 0, 0);
        
        gerenciadorUI.add(painelPrincipal);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.15f, 0.15f, 0.2f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();
        pincel.setProjectionMatrix(camera.combined);
        pincelFormas.setProjectionMatrix(camera.combined);

        // atualiza valores dos rotulos
        rotuloRaioValor.texto = String.valueOf(Mundo.RAIO_CHUNKS);
        rotuloSensiValor.texto = String.format("%.2f", UI.sensi);
        rotuloAproxValor.texto = String.format("%.1f", UI.aprox);
        rotuloDistanciaValor.texto = String.format("%.0f", UI.distancia);
        rotuloPOVValor.texto = String.valueOf(UI.pov);

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
    }

    @Override
    public void hide() {
        dispose();
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
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public boolean keyDown(int c) { return false; }
    @Override public boolean keyUp(int c) { return false; }
    @Override public boolean keyTyped(char c) { return false; }
    @Override public boolean mouseMoved(int x, int y) { return false; }
    @Override public boolean scrolled(float aX, float aY) { return false; }
}