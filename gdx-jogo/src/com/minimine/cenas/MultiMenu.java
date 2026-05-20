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
import com.minimine.servidor.Net;
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

public class MultiMenu implements Screen, InputProcessor {
    public static String modoRede = null;

    public SpriteBatch pincel;
    public ShapeRenderer pincelFormas;
    public BitmapFont fonte;
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

    public Rotulo rotuloStatus;

    public List<String> nomesMundos;
    public boolean recarregarInterface = false;
    public static boolean liberado = false;

    // busca de servidor
    public Net buscaNet = null;
    public float tempoBusca = 0f;
    public static final float INTERVALO_BUSCA = 6f;

    // conexão por IP manual(VPN/internet)
    public String ipDigitado = "";
    public boolean conectandoPorIP = false;

    // qual sub-tela ta visivel: "inicio", "mundos", "buscando", "conectarip"
    public String tela = "inicio";

    @Override
    public void show() {
        liberado = false;
        modoRede = null;
        tela = "inicio";

        pincel = new SpriteBatch();
        pincelFormas = new ShapeRenderer();

        Pixmap px = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        px.setColor(1, 1, 1, 1);
        px.fill();
        pixelBranco = new Texture(px);
        px.dispose();

        fonte = new BitmapFont();
        fonte.getData().setScale(1.5f);
        fonte.getRegion().getTexture().setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        camera = new OrthographicCamera();
        vista = new ScreenViewport(camera);
        vista.apply(true);

        toqueAuxiliar = new Vector3();
        escalaPixel = 4.0f;
        nomesMundos = new ArrayList<String>();

        gerenciadorUI = new GerenciadorUI();

        try {
            Texture textura = new Texture(Gdx.files.internal("texturas/ui/base.png"));
            textura.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            visualJanela = new PainelFatiado(textura);
            visualBotao = new PainelFatiado(textura);

            criarPainelInicio();
        } catch(Exception e) {
            Gdx.app.log("ERRO", "Recursos nao encontrados: " + e.getMessage());
        }
        Gdx.input.setInputProcessor(this);
    }

    public void criarPainelInicio() {
        tela = "inicio";
        painelPrincipal = new Painel(visualJanela, -300, -300, 600, 600, escalaPixel);
        painelPrincipal.defEspaco(20, 30);

        Rotulo titulo = new Rotulo("MULTIJOGADOR", fonte, escalaPixel * 1.2f);
        titulo.largura = 560;
        titulo.altura = 80;
        painelPrincipal.addAncorado(titulo, Ancora.SUPERIOR_CENTRO, 0, 0);

        float larguraBotao = 400;
        float alturaBotao = 70;

        Acao acaoHostedar = new Acao() {
            public void exec() {
                gerenciadorUI.limpar();
                criarPainelMundos();
                gerenciadorUI.add(painelPrincipal);
            }
        };
        Botao botaoHostedar = new Botao("Hospedar Mundo", visualBotao, fonte, 0, 0, larguraBotao, alturaBotao, escalaPixel, acaoHostedar);
        painelPrincipal.addAncorado(botaoHostedar, Ancora.CENTRO, 0, 50);

        Acao acaoEntrar = new Acao() {
            public void exec() {
                gerenciadorUI.limpar();
                criarPainelBuscando();
                gerenciadorUI.add(painelPrincipal);
                iniciarBuscaServidor();
            }
        };
        Botao botaoEntrar = new Botao("Entrar em Jogo", visualBotao, fonte, 0, 0, larguraBotao, alturaBotao, escalaPixel, acaoEntrar);
        painelPrincipal.addAncorado(botaoEntrar, Ancora.CENTRO, 0, -50);

        Acao acaoConectarIP = new Acao() {
            public void exec() {
                gerenciadorUI.limpar();
                criarPainelConectarIP();
                gerenciadorUI.add(painelPrincipal);
            }
        };
        Botao botaoConectarIP = new Botao("Conectar por IP", visualBotao, fonte, 0, 0, larguraBotao, alturaBotao, escalaPixel, acaoConectarIP);
        painelPrincipal.addAncorado(botaoConectarIP, Ancora.CENTRO, 0, -130);

        Acao acaoVoltar = new Acao() {
            public void exec() {
                Inicio.defTela(Cenas.menu);
            }
        };
        Botao botaoVoltar = new Botao("VOLTAR", visualBotao, fonte, 0, 0, 200, 60, escalaPixel, acaoVoltar);
        painelPrincipal.addAncorado(botaoVoltar, Ancora.INFERIOR_CENTRO, 0, 0);

        gerenciadorUI.add(painelPrincipal);
    }

    public void criarPainelMundos() {
        tela = "mundos";
        carregarMundos();

        painelPrincipal = new Painel(visualJanela, -400, -350, 800, 700, escalaPixel);
        painelPrincipal.defEspaco(20, 30);

        Rotulo titulo = new Rotulo("ESCOLHER MUNDO", fonte, escalaPixel * 1.2f);
        titulo.largura = 760;
        titulo.altura = 60;
        painelPrincipal.addAncorado(titulo, Ancora.SUPERIOR_CENTRO, 0, 0);

        painelMundos = new PainelRolavel(20, 170, 760, 420);
        painelMundos.defEspaco(0.5f);

        if(nomesMundos.isEmpty()) {
            Rotulo vazio = new Rotulo("Nenhum mundo salvo", fonte, escalaPixel * 0.8f);
            vazio.x = 5;
            vazio.y = 5;
            vazio.largura = 750;
            vazio.altura = 50;
            painelMundos.add(vazio);
        } else {
            float alturaLinha = 80;
            float espacamento = 6;
            float larguraNome = 580;
            float larguraBotaoAcao = 150;
            float margemV = 10;
            float alturaItemInterno = alturaLinha - margemV * 2;

            for(int i = 0; i < nomesMundos.size(); i++) {
                final String nomeArquivo = nomesMundos.get(i);
                final String nomeMundo;
                try {
                    nomeMundo = URLDecoder.decode(nomeArquivo, StandardCharsets.UTF_8.name());
                } catch(Exception e) {
                    throw new RuntimeException("[ERRO]: nome de mundo invalido " + e);
                }
                float y = 5 + (i * (alturaLinha + espacamento));
                ItemLinha linha = new ItemLinha(5, y, 750, alturaLinha, pixelBranco);

                Rotulo rotuloNome = new Rotulo(nomeMundo, fonte, escalaPixel * 0.75f);
                rotuloNome.x = 10;
                rotuloNome.y = margemV;
                rotuloNome.largura = larguraNome - 10;
                rotuloNome.altura = alturaItemInterno;
                linha.addFilho(rotuloNome);

                Acao acaoHostear = new Acao() {
                    public void exec() {
                        hospedarMundo(nomeMundo);
                    }
                };
                ItemBotao botaoHostear = new ItemBotao(
                    larguraNome, margemV, larguraBotaoAcao, alturaItemInterno,
                    "Hospedar", fonte, escalaPixel * 0.6f, pixelBranco, acaoHostear
                );
                botaoHostear.corNormal.set(0.25f, 0.4f, 0.55f, 1f);
                botaoHostear.corPressionado.set(0.35f, 0.55f, 0.75f, 1f);
                linha.addFilho(botaoHostear);

                painelMundos.add(linha);
            }
        }
        painelMundos.calcularAlturaConteudo();
        painelPrincipal.add(painelMundos);

        Acao acaoVoltar = new Acao() {
            public void exec() {
                gerenciadorUI.limpar();
                criarPainelInicio();
            }
        };
        Botao botaoVoltar = new Botao("VOLTAR", visualBotao, fonte, 0, 0, 200, 60, escalaPixel, acaoVoltar);
        painelPrincipal.addAncorado(botaoVoltar, Ancora.INFERIOR_CENTRO, 0, 0);
    }

    public void criarPainelConectarIP() {
        tela = "conectarip";
        ipDigitado = Net.ultimoIP != null ? Net.ultimoIP : "";

        painelPrincipal = new Painel(visualJanela, -300, -220, 600, 440, escalaPixel);
        painelPrincipal.defEspaco(20, 30);

        Rotulo titulo = new Rotulo("CONECTAR POR IP", fonte, escalaPixel * 1.2f);
        titulo.largura = 560;
        titulo.altura = 80;
        painelPrincipal.addAncorado(titulo, Ancora.SUPERIOR_CENTRO, 0, 0);

        Rotulo rotuloInstrucao = new Rotulo("Digite o IP do servidor (ex: 192.168.0.10)", fonte, escalaPixel * 0.65f);
        rotuloInstrucao.largura = 520;
        rotuloInstrucao.altura = 50;
        painelPrincipal.addAncorado(rotuloInstrucao, Ancora.CENTRO, 0, 60);

        final CampoTexto campoIP = new CampoTexto(visualBotao, fonte, 0, 0, 480, 70, escalaPixel);
        campoIP.padrao = "Ex: 192.168.0.10 ou VPN IP";
        campoIP.limiteCaracteres = 39;
        campoIP.gerenciador = gerenciadorUI;
        campoIP.defTexto(ipDigitado);
        campoIP.mudanca = new CampoTexto.Texto() {
            public void aoMudar(String novoTexto) {
                ipDigitado = novoTexto;
            }
        };
        painelPrincipal.addAncorado(campoIP, Ancora.CENTRO, 0, -10);

        Acao acaoConectar = new Acao() {
            public void exec() {
                String ip = ipDigitado.trim();
                if(ip.isEmpty()) return;
				ip = ip.split("/")[0].trim();
                conectarPorIP(ip);
            }
        };
        Botao botaoConectar = new Botao("CONECTAR", visualBotao, fonte, 0, 0, 250, 65, escalaPixel, acaoConectar);
        painelPrincipal.addAncorado(botaoConectar, Ancora.CENTRO, 0, -90);

        Acao acaoVoltar = new Acao() {
            public void exec() {
                gerenciadorUI.limpar();
                criarPainelInicio();
            }
        };
        Botao botaoVoltar = new Botao("VOLTAR", visualBotao, fonte, 0, 0, 200, 60, escalaPixel, acaoVoltar);
        painelPrincipal.addAncorado(botaoVoltar, Ancora.INFERIOR_CENTRO, 0, 0);

        gerenciadorUI.add(painelPrincipal);
    }

    public void conectarPorIP(String ip) {
        conectandoPorIP = true;
        modoRede = Net.CLIENTE_MODO;

        gerenciadorUI.limpar();
        criarPainelBuscando();
        rotuloStatus.texto = "Conectando em " + ip + "...";
        gerenciadorUI.add(painelPrincipal);

        // usa o construtor direto: pula UDP, vai direto pro TCP(funciona via VPN/internet)
        buscaNet = new Net(Net.CLIENTE_MODO, ip);
    }

    public void criarPainelBuscando() {
        tela = "buscando";
        tempoBusca = 0f;

        painelPrincipal = new Painel(visualJanela, -300, -200, 600, 400, escalaPixel);
        painelPrincipal.defEspaco(20, 30);

        Rotulo titulo = new Rotulo("MULTIJOGADOR", fonte, escalaPixel * 1.2f);
        titulo.largura = 560;
        titulo.altura = 80;
        painelPrincipal.addAncorado(titulo, Ancora.SUPERIOR_CENTRO, 0, 0);

        rotuloStatus = new Rotulo("Buscando servidor na rede...", fonte, escalaPixel * 0.8f);
        rotuloStatus.largura = 540;
        rotuloStatus.altura = 80;
        painelPrincipal.addAncorado(rotuloStatus, Ancora.CENTRO, 0, 20);

        Acao acaoCancelar = new Acao() {
            public void exec() {
                if(buscaNet != null) { buscaNet.liberar(); buscaNet = null; }
                gerenciadorUI.limpar();
                criarPainelInicio();
            }
        };
        Botao botaoCancelar = new Botao("CANCELAR", visualBotao, fonte, 0, 0, 200, 60, escalaPixel, acaoCancelar);
        painelPrincipal.addAncorado(botaoCancelar, Ancora.INFERIOR_CENTRO, 0, 0);
    }

    public void hospedarMundo(String nomeMundo) {
        Mundo.nome = nomeMundo;
        modoRede = Net.SERVIDOR_MODO;
        Inicio.defTela(Cenas.jogo);
    }

    public void iniciarBuscaServidor() {
        modoRede = Net.CLIENTE_MODO;
        buscaNet = new Net(Net.CLIENTE_MODO);
    }

    public void carregarMundos() {
        nomesMundos.clear();
        File pasta = ArquivosUtil.obter(Inicio.externo + "/MiniMine/mundos");
        if(pasta.exists() && pasta.isDirectory()) {
            File[] arquivos = pasta.listFiles();
            if(arquivos != null) {
                for(File a : arquivos) {
                    if(a.isFile() && a.getName().endsWith(".mini")) {
                        nomesMundos.add(a.getName().replace(".mini", ""));
                    }
                }
            }
        }
    }

    @Override
    public void render(float delta) {
        if(recarregarInterface) {
            recarregarInterface = false;
            gerenciadorUI.limpar();
            criarPainelInicio();
        }

        if(tela.equals("buscando") && buscaNet != null) {
            tempoBusca += delta;
            if(buscaNet.IP != null && buscaNet.conectado) {
                rotuloStatus.texto = "Servidor encontrado! Entrando...";
                Net.ultimoIP = buscaNet.IP;
                Jogo.net = buscaNet; // passa a conexão existente para o Jogo
                buscaNet = null; // desvincula sem liberar
                Inicio.defTela(Cenas.jogo);
            } else if(conectandoPorIP && buscaNet.IP != null && !buscaNet.conectado && tempoBusca >= 6f) {
                // conexão direta falhou
                buscaNet.liberar();
                buscaNet = null;
                conectandoPorIP = false;
                rotuloStatus.texto = "Falha ao conectar. Verifique o IP.";
            } else if(!conectandoPorIP && tempoBusca >= INTERVALO_BUSCA) {
                buscaNet.liberar();
                buscaNet = null;
                rotuloStatus.texto = "Nenhum servidor encontrado.";
            }
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
        if(buscaNet != null) {
			buscaNet.liberar();
			buscaNet = null;
		}
        if(pincel != null) pincel.dispose();
        if(pincelFormas != null) pincelFormas.dispose();
        if(fonte != null) fonte.dispose();
        if(pixelBranco != null) pixelBranco.dispose();
        gerenciadorUI.liberar();
    }

    @Override public void hide() { dispose(); }
    @Override public void pause() {}
    @Override public void resume() {}

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
    @Override public boolean keyDown(int c) { return gerenciadorUI.processarTecla(c); }
    @Override public boolean keyTyped(char c) { return gerenciadorUI.processarCaractere(c); }
    @Override public boolean keyUp(int k) { return false; }
    @Override public boolean mouseMoved(int x, int y) { return false; }
    @Override public boolean scrolled(float a, float b) { return false; }
}
