package com.minimine.cenas;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import com.minimine.Inicio;
import com.minimine.Cenas;
import com.minimine.mundo.Mundo;
import com.minimine.utils.ArquivosUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.micro.util.Acao;
import com.micro.janelas.Painel;
import com.micro.componentes.Botao;
import com.micro.componentes.Rotulo;
import com.micro.util.Ancora;
import com.micro.componentes.ItemBotao;
import com.micro.componentes.ItemLinha;
import com.micro.componentes.CampoTexto;
import com.micro.janelas.PainelRolavel;
import com.micro.componentes.CaixaDialogo;
import com.micro.janelas.PainelFatiado;
import com.micro.util.GerenciadorUI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import com.minimine.ui.InterUtil;

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
    public CaixaDialogo dialogoCriar;
    public CaixaDialogo dialogoConfirmarExcluir;
    public CampoTexto campoNome;
    public CampoTexto campoSemente;

    public List<String> nomesMundos;
    public boolean recarregarInterface, mundoEscolhido;
    public static boolean liberado = false;

    // nome pendente de exclusão, preenchido quando o dialogo de confirmação abre
    public String mundoPendenteExcluir = null;

    @Override
    public void show() {
		Mundo.plano = false;
        ArquivosUtil.debug = true;

        pincel = new SpriteBatch();
        pincelFormas = new ShapeRenderer();

        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(1, 1, 1, 1);
        pixmap.fill();
        pixelBranco = new Texture(pixmap);
        pixmap.dispose();

        fonteTitulo = InterUtil.carregarFonte("fontes/pixel-16.fnt", 2f);
        fonteTexto = InterUtil.carregarFonte("fontes/pixel-16.fnt", 1.5f);
        camera = new OrthographicCamera();
        vista = new ScreenViewport(camera);
        vista.apply(true);

        toqueAuxiliar = new Vector3();
        escalaPixel = 4.0f;
        nomesMundos = new ArrayList<String>();
        recarregarInterface = false;

        gerenciadorUI = new GerenciadorUI();

        try {
            Texture textura = new Texture(Gdx.files.internal("texturas/ui/base.png"));
            textura.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            visualJanela = new PainelFatiado(textura);
            visualBotao = new PainelFatiado(textura);

            carregarMundos();
            criarInterface();
        } catch(Exception e) {
            Gdx.app.log("ERRO", "Recursos nao encontrados: " + e.getMessage());
        }
        Gdx.input.setInputProcessor(this);
        mundoEscolhido = false;
        liberado = false;
    }

    public void carregarMundos() {
        nomesMundos.clear();
        File pastaMundos = ArquivosUtil.obter(Inicio.externo + "/MiniMine/mundos");
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

        Rotulo titulo = new Rotulo("MUNDOS", fonteTitulo, escalaPixel);
        titulo.largura = 760;
        titulo.altura = 60;
        painelPrincipal.addAncorado(titulo, Ancora.SUPERIOR_CENTRO, 0, 0);

        Painel painelBotoes = new Painel(null, 20, 80, 760, 80, 0);

        Acao acaoNovoMundo = new Acao() {
            public void exec() {
                mundoEscolhido = false;
                abrirDialogoCriar();
            }
        };
        Botao botaoNovoMundo = new Botao("Novo Mundo", visualBotao, fonteTexto, 0, 0, 400, 70, escalaPixel * 0.8f, acaoNovoMundo);
        painelBotoes.addAncorado(botaoNovoMundo, Ancora.SUPERIOR_CENTRO, 0, 0);
        painelPrincipal.add(painelBotoes);

        // lista de mundos
        painelMundos = new PainelRolavel(20, 170, 760, 420);
        painelMundos.defEspaco(0.5f);

        if(nomesMundos.isEmpty()) {
            Rotulo mensagemVazia = new Rotulo("Nenhum mundo salvo", fonteTexto, escalaPixel * 0.8f);
            mensagemVazia.x = 5;
            mensagemVazia.y = 5;
            mensagemVazia.largura = 750;
            mensagemVazia.altura = 50;
            painelMundos.add(mensagemVazia);
        } else {
            float alturaLinha = 80;
            float espacamento = 6;
            // larguras das colunas dentro da linha (total = 750)
            // [nome: 430][jogar: 100][editar: 100][excluir: 100] + margens internas
            float larguraNome = 430;
            float larguraBotaoAcao = 100;
            float margemV = 10; // margem vertical interna do botao dentro da linha
            float alturaItemInterno = alturaLinha - margemV * 2;

            for(int i = 0; i < nomesMundos.size(); i++) {
                final String nomeArquivo = nomesMundos.get(i);
				final String nomeMundo;
                try {
					nomeMundo = URLDecoder.decode(nomeArquivo, StandardCharsets.UTF_8.name());
				} catch(Exception e) {
                    throw new RuntimeException("[ERRO]: nome de mundo invalido "+e);
                }
                float y = 5 + (i * (alturaLinha + espacamento));
                ItemLinha linha = new ItemLinha(5, y, 750, alturaLinha, pixelBranco);

                // rotulo do nome do mundo, alinhado verticalmente no centro
                final Rotulo rotuloNome = new Rotulo(nomeMundo, fonteTexto, escalaPixel * 0.75f);
                rotuloNome.x = 10;
                rotuloNome.y = margemV;
                rotuloNome.largura = larguraNome - 10;
                rotuloNome.altura = alturaItemInterno;
                linha.addFilho(rotuloNome);

                // botao jogar
                float xJogar = larguraNome;
                Acao acaoJogar = new Acao() {
                    public void exec() {
                        if(mundoEscolhido) return;
                        Mundo.nome = nomeMundo;
                        mundoEscolhido = true;
                        Inicio.defTela(Cenas.jogo);
                    }
                };
                ItemBotao botaoJogar = new ItemBotao(
                    xJogar, margemV, larguraBotaoAcao, alturaItemInterno,
                    "Jogar", fonteTexto, escalaPixel * 0.6f, pixelBranco, acaoJogar
                );
                linha.addFilho(botaoJogar);

                // botao editar
                float xEditar = larguraNome + larguraBotaoAcao + 5;
                Acao acaoEditar = new Acao() {
                    public void exec() {
                        abrirDialogoEditar(nomeMundo, nomeArquivo);
                    }
                };
                ItemBotao botaoEditar = new ItemBotao(
                    xEditar, margemV, larguraBotaoAcao, alturaItemInterno,
                    "Editar", fonteTexto, escalaPixel * 0.6f, pixelBranco, acaoEditar
                );
                botaoEditar.corNormal.set(0.35f, 0.45f, 0.35f, 1f);
                botaoEditar.corPressionado.set(0.45f, 0.6f, 0.45f, 1f);
                linha.addFilho(botaoEditar);

                // botao excluir
                float xExcluir = larguraNome + (larguraBotaoAcao + 5) * 2;
                Acao acaoExcluir = new Acao() {
                    public void exec() {
                        abrirDialogoConfirmarExcluir(nomeMundo, nomeArquivo);
                    }
                };
                ItemBotao botaoExcluir = new ItemBotao(
                    xExcluir, margemV, larguraBotaoAcao, alturaItemInterno,
                    "Excluir", fonteTexto, escalaPixel * 0.6f, pixelBranco, acaoExcluir
                );
                botaoExcluir.corNormal.set(0.5f, 0.25f, 0.25f, 1f);
                botaoExcluir.corPressionado.set(0.7f, 0.3f, 0.3f, 1f);
                linha.addFilho(botaoExcluir);

                painelMundos.add(linha);
            }
        }
        painelMundos.calcularAlturaConteudo();
        painelPrincipal.add(painelMundos);

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
        // dialogo de criação de mundo
        dialogoCriar = new CaixaDialogo(visualJanela, fonteTexto, escalaPixel, pincelFormas);
        dialogoCriar.definirTamanho(500, 440);
        dialogoCriar.centralizar(0, 0);

        campoNome = new CampoTexto(visualBotao, fonteTexto, 50, 240, 400, 50, escalaPixel / 1.5f);
        campoNome.padrao = "Nome do Mundo";
        campoNome.limiteCaracteres = 30;
        dialogoCriar.add(campoNome);

        campoSemente = new CampoTexto(visualBotao, fonteTexto, 50, 160, 400, 50, escalaPixel / 1.5f);
        campoSemente.padrao = "Semente(opcional)";
        campoSemente.limiteCaracteres = 10;
        dialogoCriar.add(campoSemente);

        Acao acaoSobrevivencia = new Acao() {
            public void exec() { entrarNoMundo(2); }
        };
        Acao acaoCriativo = new Acao() {
            public void exec() { entrarNoMundo(1); }
        };
        Acao acaoEspectador = new Acao() {
            public void exec() { entrarNoMundo(0); }
        };
        // 3 botões de modo de jogo em linha única
        float largBotaoModo = 148f;
        float altBotaoModo = 50f;
        float yLinhaModo = 60f;
        float yLinhaMundo = 5f;
        float xModo = 21f; // (500 - (3*148 + 2*7)) / 2 = 21, centraliza os 3 botões
        dialogoCriar.addBotaoManual("Sobrevivencia", visualBotao,
		xModo, yLinhaModo, largBotaoModo, altBotaoModo, acaoSobrevivencia);
        dialogoCriar.addBotaoManual("Criativo", visualBotao,
		xModo + largBotaoModo + 7f, yLinhaModo, largBotaoModo, altBotaoModo, acaoCriativo);
        dialogoCriar.addBotaoManual("Espectador", visualBotao,
		xModo + (largBotaoModo + 7f) * 2, yLinhaModo, largBotaoModo, altBotaoModo, acaoEspectador);

        final Botao[] modoMundo = {null};

        Acao acaoMundo = new Acao() {
            public void exec() {
                Mundo.plano = !Mundo.plano;
                if(Mundo.plano) modoMundo[0].rotulo.texto = "Mundo Plano";
                else modoMundo[0].rotulo.texto = "Mundo Normal";
            }
        };
        modoMundo[0] = dialogoCriar.addBotaoManual("Mundo Normal", visualBotao,
		xModo + (largBotaoModo + 7f) * 2, yLinhaMundo, largBotaoModo, altBotaoModo, acaoMundo);

        gerenciadorUI.addDialogo(dialogoCriar);

        // dialogo de confirmação de exclusão
        dialogoConfirmarExcluir = new CaixaDialogo(visualJanela, fonteTexto, escalaPixel / 1.5f, pincelFormas);
        dialogoConfirmarExcluir.definirTamanho(460, 220);

        Acao acaoConfirmarExcluir = new Acao() {
            public void exec() {
                if(mundoPendenteExcluir != null) {
                    excluirMundo(mundoPendenteExcluir);
                    mundoPendenteExcluir = null;
                }
                dialogoConfirmarExcluir.fechar(false);
            }
        };
        Acao acaoCancelarExcluir = new Acao() {
            public void exec() {
                mundoPendenteExcluir = null;
                dialogoConfirmarExcluir.fechar(false);
            }
        };
        dialogoConfirmarExcluir.addBotao("Excluir", visualBotao, Ancora.INFERIOR_ESQUERDO, 10, acaoConfirmarExcluir);
        dialogoConfirmarExcluir.addBotao("Cancelar", visualBotao, Ancora.INFERIOR_DIREITO, -10, acaoCancelarExcluir);

        gerenciadorUI.addDialogo(dialogoConfirmarExcluir);
    }

    public void abrirDialogoCriar() {
        campoNome.texto = "";
        campoSemente.texto = "";
        dialogoCriar.mostrar("Novo Mundo", "", new CaixaDialogo.Fechar() {
				public void aoFechar(boolean confirmou) {
					if(!confirmou) {
						campoNome.texto = "";
						campoSemente.texto = "";
						Gdx.input.setOnscreenKeyboardVisible(false);
					}
				}
			});
    }

    public void abrirDialogoEditar(String nomeMundo, String nomeArquivo) {
        // por enquanto so reabre o dialogo de criação com o nome preenchido
        // pode ser expandido para renomear o arquivo depois
        campoNome.texto = nomeMundo;
        campoSemente.texto = "";
        dialogoCriar.mostrar("Editar Mundo", "", null);
    }

    public void abrirDialogoConfirmarExcluir(String nomeMundo, String nomeArquivo) {
        mundoPendenteExcluir = nomeArquivo;
        dialogoConfirmarExcluir.mostrar(
            "Excluir \"" + nomeMundo + "\"?",
            "Isso não pode ser desfeito.",
            null
        );
    }

    public void excluirMundo(String nomeArquivo) {
        File arquivo = ArquivosUtil.obter(Inicio.externo + "/MiniMine/mundos/" + nomeArquivo + ".mini");
        if(arquivo.exists()) {
            arquivo.delete();
        }
        recarregarInterface = true;
    }

    public void entrarNoMundo(int modo) {
        String nome = campoNome.texto.trim();
        if(nome.isEmpty()) return;

        String textoSemente = campoSemente.texto.trim();
        long semente = 0;
        try { semente = Integer.parseInt(textoSemente); } catch(Exception e) { semente = 0; }

        Mundo.nome = nome;
        Mundo.semente = semente;
        Jogo.modo = modo;

        dialogoCriar.fechar(false);
        Gdx.input.setOnscreenKeyboardVisible(false);
        mundoEscolhido = true;
        Inicio.defTela(Cenas.jogo);
    }

    @Override
    public void render(float delta) {
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
        if(liberado) return;
        liberado = true;

        if(pincel != null) pincel.dispose();
        if(pincelFormas != null) pincelFormas.dispose();
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
