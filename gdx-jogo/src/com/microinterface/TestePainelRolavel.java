package com.microinterface;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

// mostra tres exemplos: lista simples, lista com botoes, e lista de selecao
public class TestePainelRolavel implements Screen, InputProcessor {
    public SpriteBatch pincel;
    public GerenciadorUI gerenciadorUI;
    public BitmapFont fonte;

    // paineis de exemplo
    public PainelRolavel painelSimples;
    public PainelRolavel painelBotoes;
    public PainelRolavel painelSelecao;

    // recursos compartilhados
    public Texture pixelBranco;
    
    // controle de selecao
    public int itemSelecionado = -1;
    public String msgSelecao = "";

    public void show() {
        pincel = new SpriteBatch();
        gerenciadorUI = new GerenciadorUI();
        fonte = new BitmapFont();

        // cria pixel branco pra desenhar retangulos
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        pixelBranco = new Texture(pixmap);
        pixmap.dispose();

        // cria os tres paineis de exemplo
        criarPainelSimples();
        criarPainelBotoes();
        criarPainelSelecao();

        Gdx.input.setInputProcessor(this);

        System.out.println("=== TESTE PAINEL ROLAVEL ===");
        System.out.println("Esquerda: Lista simples de rotulos");
        System.out.println("Centro: Lista com botoes clicaveis");
        System.out.println("Direita: Lista de selecao");
    }

    // cria um painel simples com rotulos
    public void criarPainelSimples() {
        painelSimples = new PainelRolavel(20, 50, 220, 500);
        painelSimples.defEspaco(5);
        painelSimples.corBarra = new Color(0.3f, 0.6f, 0.9f, 0.8f);

        // adiciona 30 rotulos
        for(int i = 0; i < 30; i++) {
            Rotulo rotulo = new Rotulo("Item " + (i + 1), fonte, 1.0f);
            rotulo.x = 5;
            rotulo.y = 5 + (i * 40);
            rotulo.largura = 210;
            rotulo.altura = 35;
            painelSimples.add(rotulo);
        }
        painelSimples.calcularAlturaConteudo();
        gerenciadorUI.add(painelSimples);
    }

    // cria um painel com botoes clicaveis
    public void criarPainelBotoes() {
        painelBotoes = new PainelRolavel(260, 50, 220, 500);
        painelBotoes.defEspaco(5);
        painelBotoes.corBarra = new Color(0.6f, 0.3f, 0.9f, 0.8f);

        // adiciona 20 botoes
        for(int i = 0; i < 20; i++) {
            final int indice = i;

            ItemBotao botao = new ItemBotao(
                5,
                5 + (i * 55),
                210,
                50,
                "Botao " + (i + 1),
                fonte,
                1.0f,
                pixelBranco,
                new Acao() {
                    public void exec() {
                        msgSelecao = "Clicou no botao " + (indice + 1);
                        System.out.println(msgSelecao);
                    }
                }
            );
            painelBotoes.add(botao);
        }
        painelBotoes.calcularAlturaConteudo();
        gerenciadorUI.add(painelBotoes);
    }

    // cria um painel de selecao
    public void criarPainelSelecao() {
        painelSelecao = new PainelRolavel(500, 50, 220, 500);
        painelSelecao.defEspaco(5);
        painelSelecao.corBarra = new Color(0.9f, 0.6f, 0.3f, 0.8f);

        final String[] opcoes = {
            "Opcao A", "Opcao B", "Opcao C", "Opcao D", "Opcao E",
            "Opcao F", "Opcao G", "Opcao H", "Opcao I", "Opcao J",
            "Opcao K", "Opcao L", "Opcao M", "Opcao N", "Opcao O"
        };

        for(int i = 0; i < opcoes.length; i++) {
            final int indice = i;

            ItemSelecao item = new ItemSelecao(
                5,
                5 + (i * 55),
                210,
                50,
                opcoes[i],
                fonte,
                1.0f,
                pixelBranco,
                indice == itemSelecionado,
                new Acao() {
                    public void exec() {
                        itemSelecionado = indice;
                        msgSelecao = "Selecionado: " + opcoes[indice];
                        System.out.println(msgSelecao);
                        attSelecao();
                    }
                }
            );
            painelSelecao.add(item);
        }
        painelSelecao.calcularAlturaConteudo();
        gerenciadorUI.add(painelSelecao);
    }

    // atualiza a selecao visual dos itens
    public void attSelecao() {
        for(int i = 0; i < painelSelecao.filhos.size(); i++) {
            Componente comp = painelSelecao.filhos.get(i);
            if(comp instanceof ItemSelecao) {
                ItemSelecao item = (ItemSelecao) comp;
                item.defSelecionado(i == itemSelecionado);
            }
        }
    }

    public void render(float delta) {
        // limpa tela
        Gdx.gl.glClearColor(0.15f, 0.15f, 0.15f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // desenha interface
        pincel.begin();
        gerenciadorUI.desenhar(pincel, delta);

        // desenha titulos dos paineis
        fonte.setColor(Color.WHITE);
        fonte.draw(pincel, "Lista Simples", 20, 30);
        fonte.draw(pincel, "Botoes Clicaveis", 260, 30);
        fonte.draw(pincel, "Selecao", 500, 30);

        // desenha mensagem de seleção
        if(msgSelecao.length() > 0) {
            fonte.draw(pincel, msgSelecao, 20, Gdx.graphics.getHeight() - 20);
        }
        // desenha instruções
        fonte.draw(pincel, "Arraste para rolar | Clique na barra para pular", 20, Gdx.graphics.getHeight() - 40);

        pincel.end();
    }

    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        float y = Gdx.graphics.getHeight() - screenY;
        return gerenciadorUI.processarToque(screenX, y, true);
    }

    public boolean touchUp(int telaX, int telaY, int p, int b) {
        float y = Gdx.graphics.getHeight() - telaY;
        return gerenciadorUI.processarToque(telaX, y, false);
    }

    public boolean touchDragged(int telaX, int telaY, int p) {
        float y = Gdx.graphics.getHeight() - telaY;
        gerenciadorUI.processarArraste(telaX, y);
        return true;
    }

    public boolean mouseMoved(int telaX, int telaY) {return false;}

    public boolean scrolled(float quantidadeX, float quantidadeY) {
        // processa scroll do mouse wheel se o mouse estiver sobre algum painel
        float mouseX = Gdx.input.getX();
        float mouseY = Gdx.graphics.getHeight() - Gdx.input.getY();

        if(painelSimples.contem(mouseX, mouseY)) {
            painelSimples.processarRolagem(quantidadeY);
            return true;
        } else if(painelBotoes.contem(mouseX, mouseY)) {
            painelBotoes.processarRolagem(quantidadeY);
            return true;
        } else if(painelSelecao.contem(mouseX, mouseY)) {
            painelSelecao.processarRolagem(quantidadeY);
            return true;
        }
        return false;
    }

    public boolean keyDown(int c) {
        return gerenciadorUI.processarTecla(c);
    }

    public boolean keyUp(int c) {return false;}

    public boolean keyTyped(char c) {
        return gerenciadorUI.processarCaractere(c);
    }

    @Override
    public void resize(int v, int h) {}
	public void pause() {}
    public void resume() {}
    public void hide() {}

    public void dispose() {
        pincel.dispose();
        fonte.dispose();
        pixelBranco.dispose();

        painelSimples.liberar();
        painelBotoes.liberar();
        painelSelecao.liberar();

        // libera itens
        for(int i = 0; i < painelBotoes.filhos.size(); i++) {
            Componente comp = painelBotoes.filhos.get(i);
            if(comp instanceof ItemBotao) {
                ((ItemBotao) comp).liberar();
            }
        }

        for(int i = 0; i < painelSelecao.filhos.size(); i++) {
            Componente comp = painelSelecao.filhos.get(i);
            if(comp instanceof ItemSelecao) {
                ((ItemSelecao) comp).liberar();
            }
        }
		gerenciadorUI.liberar();
    }
}
