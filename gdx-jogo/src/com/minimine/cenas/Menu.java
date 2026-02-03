package com.minimine.cenas;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import java.util.List;
import com.minimine.ui.Botao;
import java.util.ArrayList;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Gdx;
import com.minimine.ui.Texto;
import com.minimine.Cenas;
import com.minimine.Inicio;
import com.badlogic.gdx.graphics.GL20;
import com.minimine.graficos.Texturas;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.minimine.utils.ArquivosUtil;
import com.minimine.mundo.ChunkUtil;
import com.minimine.utils.DiaNoiteUtil;
import com.minimine.ui.InterUtil;
import com.minimine.utils.CorposCelestes;
import com.minimine.mundo.Chunk;
import com.badlogic.gdx.utils.Pool;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.Preferences;
import com.minimine.mundo.Mundo;
import com.minimine.ui.UI;
import com.minimine.mundo.Biomas;

public class Menu implements Screen, InputProcessor {
	public static SpriteBatch sb;
    public static BitmapFont fonte;
	public static List<Texto> textos;
	public static List<Botao> botoes;
	public static float botaoTam = 130;
	public static Preferences prefs;
	
	@Override
	public void show() {	
		prefs = Gdx.app.getPreferences("MiniConfig");
		textos = new ArrayList<>();
		botoes = new ArrayList<>();
		sb = new SpriteBatch();
		
		fonte = InterUtil.carregarFonte("ui/fontes/pixel.ttf", 30);
		Gdx.input.setInputProcessor(this);
		
		Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());  
		Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
		Gdx.gl.glCullFace(GL20.GL_BACK);
		Gdx.gl.glEnable(GL20.GL_BLEND);
		
		attInterface();
		
		Mundo.RAIO_CHUNKS = prefs.getInteger("raioChunks", Mundo.RAIO_CHUNKS);
		UI.pov = prefs.getInteger("pov", UI.pov);
		UI.sensi = prefs.getFloat("sensi", UI.sensi);
		UI.distancia = prefs.getFloat("distancia", UI.distancia);
		UI.aprox = prefs.getFloat("aprox", UI.aprox);
	}
	
	public void attInterface() {
		textos.clear();
		botoes.clear();

		float dpi = Gdx.graphics.getDensity();
		float escala = dpi * 0.75f;

		float larguraBotao = 260 * escala;
		float alturaBotao = 130 * escala;

		botoes.add(new Botao(Texturas.texs.get("botao_opcao"), 0, 0, larguraBotao * 2, alturaBotao, "irJogo") {
				@Override
				public void aoTocar(int tx, int ty, int p) {
					Inicio.defTela(Cenas.selecao);
				}
				@Override
				public void aoAjustar(int v, int h) {
					float dpiAtual = Gdx.graphics.getDensity();
					float escalaAtual = dpiAtual * 0.75f;
					float novaLargura = 260 * escalaAtual * 2;
					float novaAltura = 130 * escalaAtual;
					tamX = novaLargura;
					tamY = novaAltura;
					defPos((v - novaLargura) / 2, (h - novaAltura) / 2);
				}
			});
		textos.add(new Texto("Um Jogador", 0, 0) {
				@Override
				public void aoAjustar(int v, int h) {
					GlyphLayout l = new GlyphLayout(fonte, texto);
					x = (v - l.width) / 2f;
					y = h / 2f;
				}
			});
		botoes.add(new Botao(Texturas.texs.get("botao_opcao"), 0, 0, larguraBotao * 2, alturaBotao, "irConfig") {
				@Override
				public void aoTocar(int tx, int ty, int p) {
					Inicio.defTela(Cenas.configuracoes);
				}
				@Override
				public void aoAjustar(int v, int h) {
					float dpiAtual = Gdx.graphics.getDensity();
					float escalaAtual = dpiAtual * 0.75f;
					float novaLargura = 260 * escalaAtual * 2;
					float novaAltura = 130 * escalaAtual;
					tamX = novaLargura;
					tamY = novaAltura;
					defPos((v - novaLargura) / 2f, (h - novaAltura) / 3f);
				}
			});
		textos.add(new Texto("Configurações", 0, 0) {
				@Override
				public void aoAjustar(int v, int h) {
					GlyphLayout l = new GlyphLayout(fonte, texto);
					x = (v - l.width) / 2f;
					y = h / 2.95f;
				}
			});
		textos.add(new Texto("MiniMine", 0, 0) {
				@Override
				public void aoAjustar(int v, int h) {
					GlyphLayout l = new GlyphLayout(fonte, texto);
					x = (v - l.width) / 2f;
					y = h - (75 * Gdx.graphics.getDensity());
				}
			});
	}
	
	@Override
	public void render(float delta) {
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
		Gdx.gl.glDisable(GL20.GL_CULL_FACE);
		
		sb.begin();
		for(Botao bt : botoes) {
			bt.porFrame(delta, sb, fonte);
		}
		for(Texto t : textos) {
			t.porFrame(delta, sb, fonte);
		}
		sb.end();
	}
	
	@Override
	public void resize(int v, int h) {
		Gdx.gl.glViewport(0, 0, v, h);

		sb.getProjectionMatrix().setToOrtho2D(0, 0, v, h);

		for(Botao b : botoes) {
			if(b != null) b.aoAjustar(v, h);
		}
		for(Texto t : textos) {
			if(t != null) t.aoAjustar(v, h);
		}
	}
	@Override
	public void dispose() {
		try {
			sb.dispose();
			fonte.dispose();	
		} catch(Exception e) {}
	}
	@Override
	public boolean touchDown(int telaX, int telaY, int p, int b) {
		int y = Gdx.graphics.getHeight() - telaY;
		for(Botao bt : botoes) {
			if(bt.hitbox.contains(telaX, y)) {
				bt.aoTocar(telaX, y, p);
			}
		}
		return false;
	}
	@Override public void hide() {}
	@Override public void pause() {}
	@Override public void resume() {}
	@Override public boolean touchDragged(int p, int p1, int p2) {return false;}
	@Override public boolean touchUp(int p, int p1, int p2, int p3) {return false;}
	@Override public boolean keyDown(int p){return false;}
	@Override public boolean keyTyped(char p){return false;}
	@Override public boolean keyUp(int p){return false;}
	@Override public boolean mouseMoved(int p, int p1){return false;}
	@Override public boolean scrolled(float p, float p1){return false;}
}
