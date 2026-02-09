package com.minimine.cenas;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
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
import com.minimine.Inicio;
import com.minimine.Cenas;
import com.minimine.mundo.Mundo;
import com.minimine.utils.ArquivosUtil;
import com.microinterface.GerenciadorUI;
import com.microinterface.Painel;
import com.microinterface.PainelFatiado;
import com.microinterface.ItemBotao;
import com.microinterface.Botao;
import com.microinterface.Rotulo;
import com.microinterface.Ancora;
import com.microinterface.Acao;
import com.microinterface.CaixaDialogo;
import com.microinterface.CampoTexto;
import com.microinterface.PainelRolavel;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MundoMenu implements Screen, InputProcessor {
    public SpriteBatch pincel;
    public ShapeRenderer pincelFormas;
    public BitmapFont fonteTitulo;
    public BitmapFont fonteTexto;
    public OrthographicCamera camera;
    public Viewport vista;
    public Vector3 toqueAuxiliar;

    public GerenciadorUI gerenciadorUI;
    public PainelFatiado visualJanela;
    public PainelFatiado visualBotao;
    public Texture pixelBranco;
    public float escalaPixel;

    public Painel painelPrincipal;
    public PainelRolavel painelMundos;
    public CaixaDialogo dialogoNome;
    public CaixaDialogo dialogoSemente;
    public CampoTexto campoNome;
    public CampoTexto campoSemente;

    public List<String> nomesMundos;
    public boolean recarregarInterface;

    @Override
    public void show() {
        ArquivosUtil.debug = true;

        pincel = new SpriteBatch();
        pincelFormas = new ShapeRenderer();

        // cria textura de pixel branco
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(1, 1, 1, 1);
        pixmap.fill();
        pixelBranco = new Texture(pixmap);
        pixmap.dispose();

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
        nomesMundos = new ArrayList<String>();
        recarregarInterface = false;

        gerenciadorUI = new GerenciadorUI();

        try {
            Texture textura = new Texture(Gdx.files.internal("ui/base.png"));
            textura.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            visualJanela = new PainelFatiado(textura);
            visualBotao = new PainelFatiado(textura);

            carregarMundos();
            criarInterface();
        } catch(Exception e) {
            Gdx.app.log("ERRO", "Recursos nao encontrados: " + e.getMessage());
        }
        Gdx.input.setInputProcessor(this);
    }

    public void carregarMundos() {
        nomesMundos.clear();
        File pastaMundos = new File(Inicio.externo + "/MiniMine/mundos");
        if(pastaMundos.exists() && pastaMundos.isDirectory()) {
            File[] arquivos = pastaMundos.listFiles();
            if(arquivos != null) {
                for(int i = 0; i < arquivos.length; i++) {
                    File arquivo = arquivos[i];
                    if(arquivo.isFile() && arquivo.getName().endsWith(".mini")) {
                        String nome = arquivo.getName().replace(".mini", "");
                        nomesMundos.add(nome);
                    }
                }
            }
        }
    }

    public void criarInterface() {
        painelPrincipal = new Painel(visualJanela, -400, -350, 800, 700, escalaPixel);
        painelPrincipal.defEspaco(20, 30);

        // titulo
        Rotulo titulo = new Rotulo("MUNDOS", fonteTitulo, escalaPixel);
        titulo.largura = 760;
        titulo.altura = 60;
        painelPrincipal.addAncorado(titulo, Ancora.SUPERIOR_CENTRO, 0, 0);

        // botao novo mundo
        Acao acaoNovoMundo = new Acao() {
            public void exec() {
                abrirDialogoNovoMundo();
            }
        };
        Botao botaoNovoMundo = new Botao("NOVO MUNDO", visualBotao, fonteTexto, 0, 0, 400, 70, escalaPixel, acaoNovoMundo);
        painelPrincipal.addAncorado(botaoNovoMundo, Ancora.SUPERIOR_CENTRO, 0, -80);

        // painel rolavel para lista de mundos (SEM fundo visual)
        painelMundos = new PainelRolavel(20, 170, 760, 420);
        painelMundos.defEspaco(0); // sem espaçamento interno para aproveitar toda área

        // adiciona mundos ao painel rolavel usando ItemBotao
        if(nomesMundos.isEmpty()) {
            Rotulo mensagemVazia = new Rotulo("Nenhum mundo salvo", fonteTexto, escalaPixel * 0.8f);
            mensagemVazia.x = 5;
            mensagemVazia.y = 5;
            mensagemVazia.largura = 750;
            mensagemVazia.altura = 50;
            painelMundos.add(mensagemVazia);
        } else {
            // altura de cada botão + espaçamento
            float alturaBotao = 80;
            float espacamento = 10;

            for(int i = 0; i < nomesMundos.size(); i++) {
                final String nomeMundo = Mundo.decodificarNome(nomesMundos.get(i));

                Acao acaoJogar = new Acao() {
                    public void exec() {
                        Mundo.nome = nomeMundo;
                        Inicio.defTela(Cenas.jogo);
                    }
                };

                // posicionamento simples: cada botão abaixo do anterior
                float x = 5;
                float y = 5 + (i * (alturaBotao + espacamento));

                ItemBotao itemMundo = new ItemBotao(x, y, 750, alturaBotao, nomeMundo, fonteTexto, escalaPixel, pixelBranco, acaoJogar);
                painelMundos.add(itemMundo);
            }
        }
        painelMundos.calcularAlturaConteudo();
        painelPrincipal.add(painelMundos);

        // botao voltar
        Acao acaoVoltar = new Acao() {
            public void exec() {
                Inicio.defTela(Cenas.menu);
            }
        };
        Botao botaoVoltar = new Botao("VOLTAR", visualBotao, fonteTexto, 0, 0, 200, 60, escalaPixel, acaoVoltar);
        painelPrincipal.addAncorado(botaoVoltar, Ancora.INFERIOR_CENTRO, 0, 0);

        gerenciadorUI.addCamada(painelPrincipal, GerenciadorUI.CAMADA_UI);

        criarDialogos();
    }

    public void criarDialogos() {
        // dialogo para nome do mundo
        dialogoNome = new CaixaDialogo(visualJanela, fonteTexto, escalaPixel, pincelFormas);
        dialogoNome.largura = 500;
        dialogoNome.altura = 300;

        campoNome = new CampoTexto(visualBotao, fonteTexto, 50, 120, 400, 50, escalaPixel);
        campoNome.padrao = "Nome do Mundo";
        campoNome.limiteCaracteres = 30;
        dialogoNome.add(campoNome);

        Acao acaoConfirmarNome = new Acao() {
            public void exec() {
                String nome = campoNome.texto.trim();
                if(!nome.isEmpty()) {
                    Mundo.nome = nome;
                    dialogoNome.fechar(false);
                    abrirDialogoSemente();
					Gdx.input.setOnscreenKeyboardVisible(false);
                }
            }
        };
        dialogoNome.addBotao("OK", visualBotao, Ancora.CENTRO_DIREITO, -10, acaoConfirmarNome);

        Acao acaoCancelarNome = new Acao() {
            public void exec() {
                campoNome.texto = "";
                dialogoNome.fechar(false);
				Gdx.input.setOnscreenKeyboardVisible(false);
            }
        };
        dialogoNome.addBotao("Cancelar", visualBotao, Ancora.CENTRO_ESQUERDO, 10, acaoCancelarNome);

        gerenciadorUI.addDialogo(dialogoNome);

        // dialogo para semente
        dialogoSemente = new CaixaDialogo(visualJanela, fonteTexto, escalaPixel, pincelFormas);
        dialogoSemente.largura = 500;
        dialogoSemente.altura = 300;

        campoSemente = new CampoTexto(visualBotao, fonteTexto, 50, 120, 400, 50, escalaPixel);
        campoSemente.padrao = "Semente";
        campoSemente.limiteCaracteres = 10;
        dialogoSemente.add(campoSemente);

        Acao acaoConfirmarSemente = new Acao() {
            public void exec() {
                String textoSemente = campoSemente.texto.trim();
                int semente = 0;
                try {
                    semente = Integer.parseInt(textoSemente);
                } catch(Exception e) {
                    semente = 0;
                }
                Mundo.semente = semente;
                dialogoSemente.fechar(false);
				Gdx.input.setOnscreenKeyboardVisible(false);
                Inicio.defTela(Cenas.jogo);
            }
        };
        dialogoSemente.addBotao("OK", visualBotao, Ancora.CENTRO_DIREITO, -10, acaoConfirmarSemente);

        Acao acaoCancelarSemente = new Acao() {
            public void exec() {
                campoSemente.texto = "";
                dialogoSemente.fechar(false);
				Gdx.input.setOnscreenKeyboardVisible(false);
            }
        };
        dialogoSemente.addBotao("Cancelar", visualBotao, Ancora.CENTRO_ESQUERDO, 10, acaoCancelarSemente);

        gerenciadorUI.addDialogo(dialogoSemente);
    }

    public void abrirDialogoNovoMundo() {
        campoNome.texto = "";
        dialogoNome.mostrar("Nome do Mundo", "Digite o nome:", null);
    }

    public void abrirDialogoSemente() {
        campoSemente.texto = "";
        dialogoSemente.mostrar("Semente", "Digite a semente:", null);
    }

    @Override
    public void render(float delta) {
        // recarrega a interface se necessario
        if(recarregarInterface) {
            carregarMundos();
            gerenciadorUI.limpar();
            criarInterface();
            recarregarInterface = false;
        }
        Gdx.gl.glClearColor(0.2f, 0.2f, 0.3f, 1);
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
    public boolean keyDown(int c) {
        return gerenciadorUI.processarTecla(c);
    }

    @Override
    public boolean keyTyped(char c) {
        return gerenciadorUI.processarCaractere(c);
    }

    @Override public void pause() {}
    @Override public void resume() {}

    @Override
    public boolean keyUp(int k) { 
        return false; 
    }
    @Override
    public boolean mouseMoved(int x, int y) { 
        return false; 
    }
    @Override
    public boolean scrolled(float a, float b) { 
        return false; 
    }
}