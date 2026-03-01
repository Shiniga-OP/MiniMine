package com.minimine.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.minimine.graficos.Texturas;
import com.minimine.graficos.Render;
import com.minimine.Inicio;
import com.minimine.utils.ArquivosUtil;
import com.minimine.cenas.Jogo;
import com.minimine.Cenas;

import com.micro.GerenciadorUI;
import com.micro.Painel;
import com.micro.PainelFatiado;
import com.micro.Botao;
import com.micro.Ancora;
import com.micro.Acao;

public class MenuPause {
    public static GerenciadorUI gerenciador;
    public static Painel painelMenu;
    public static PainelFatiado visualBotao;

    public static boolean menuAberto = false;
    public static ShapeRenderer sr;

    // dimensões do painel
    public static float LARGURA_PAINEL = 260f;
    public static float ALTURA_PAINEL = 260f;
    public static float LARGURA_BOTAO = 200f;
    public static float ALTURA_BOTAO = 55f;
    public static float ESCALA_PIXEL = 3f;

    public static void iniciar() {
        gerenciador = new GerenciadorUI();

        // base dos botões
        visualBotao = new PainelFatiado(Texturas.base);

        float cx = -LARGURA_PAINEL / 2f;
        float cy = -ALTURA_PAINEL  / 2f;

        painelMenu = new Painel(visualBotao, cx, cy, LARGURA_PAINEL, ALTURA_PAINEL, ESCALA_PIXEL);
        painelMenu.defEspaco(16, 20);

        // === botão voltar ===
        Botao botaoVoltar = new Botao("Voltar", visualBotao, UI.fonte,
			0, 0, LARGURA_BOTAO, ALTURA_BOTAO, ESCALA_PIXEL,
			new Acao() {
				@Override
				public void exec() {
					fecharMenu();
				}
			});
        painelMenu.addAncorado(botaoVoltar, Ancora.SUPERIOR_CENTRO, 0, 0);

        // === botão salvar ===
        Botao botaoSalvar = new Botao("Salvar", visualBotao, UI.fonte,
			0, 0, LARGURA_BOTAO, ALTURA_BOTAO, ESCALA_PIXEL,
			new Acao() {
				@Override
				public void exec() {
					salvarJogo();
					fecharMenu();
					UI.abrirDialogo("Jogo salvo!", null);
				}
			});
        painelMenu.addAncorado(botaoSalvar, Ancora.CENTRO, 0, 0);

        // === botão sair ===
        Botao botaoSair = new Botao("Sair", visualBotao, UI.fonte,
			0, 0, LARGURA_BOTAO, ALTURA_BOTAO, ESCALA_PIXEL,
			new Acao() {
				@Override
				public void exec() {
					salvarJogo();
					fecharMenu();
					Inicio.defTela(Cenas.menu);
				}
			});
        painelMenu.addAncorado(botaoSair, Ancora.INFERIOR_CENTRO, 0, 0);

        gerenciador.add(painelMenu);
    }

    public static void alternarMenu() {
        if(menuAberto) fecharMenu();
        else abrirMenu();
    }

    public static void abrirMenu() {
        if(gerenciador == null) iniciar();
        Render.pause = true;
        menuAberto = true;
        Gdx.input.setCursorCatched(false);
        Gdx.app.log("MenuPause", "===== MENU ABERTO =====");
    }

    public static void fecharMenu() {
        Render.pause = false;
        menuAberto = false;
        Gdx.input.setCursorCatched(true);
        Gdx.app.log("MenuPause", "===== MENU FECHADO =====");
    }

    public static void salvarJogo() {
        new Thread(new Runnable() {
				@Override
				public void run() {
					ArquivosUtil.svMundo(Jogo.mundo, Jogo.render.ui.jg);
				}
			}).start();
        Gdx.app.log("MenuPause", "Salvando o jogo...");
    }

    public static void renderizar(SpriteBatch sb, BitmapFont fonte) {
        if(!menuAberto || gerenciador == null) return;

        // encerra o sb pra desenhar o fundo escuro com ShapeRenderer
        sb.end();

        sr.setProjectionMatrix(sb.getProjectionMatrix());
        Gdx.gl.glEnable(Gdx.gl.GL_BLEND);
        Gdx.gl.glBlendFunc(Gdx.gl.GL_SRC_ALPHA, Gdx.gl.GL_ONE_MINUS_SRC_ALPHA);

        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0f, 0f, 0f, 0.65f);
        sr.rect(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        sr.end();

        Gdx.gl.glDisable(Gdx.gl.GL_BLEND);

        sb.begin();

        // centraliza o painel no meio da tela antes de desenhar
        painelMenu.x = Gdx.graphics.getWidth()  / 2f - LARGURA_PAINEL / 2f;
        painelMenu.y = Gdx.graphics.getHeight() / 2f - ALTURA_PAINEL  / 2f;

        gerenciador.desenhar(sb, Gdx.graphics.getDeltaTime());
    }
	
	// processadores de toque
    public static boolean processarToque(float x, float y, boolean pressionado) {
        if(!menuAberto || gerenciador == null) return false;
        return gerenciador.processarToque(x, y, pressionado);
    }

    public static void processarArraste(float x, float y) {
        if(!menuAberto || gerenciador == null) return;
        gerenciador.processarArraste(x, y);
    }
	
    public static void liberar() {
        if(sr != null) sr.dispose();
        if(gerenciador != null) gerenciador.liberar();
    }
}

