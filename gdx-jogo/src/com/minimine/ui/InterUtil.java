package com.minimine.ui;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.Gdx;
import com.minimine.Inicio;
import java.util.Map;
import java.util.HashMap;

public class InterUtil {
	public SpriteBatch sb;
	public BitmapFont fonte;
	public Map<CharSequence, Objeto> objetos = new HashMap<>();

	public InterUtil(int tamTexto) {
		sb = new SpriteBatch();
		fonte = carregarFonte("ui/fontes/pixel.ttf", tamTexto);
	}

	public void att(float delta) {
		for(Objeto b : objetos.values()) {
			if(b != null) b.porFrame(delta, sb, fonte);
		}
	}

	public void ajustar(int v, int h) {
		for(Objeto b : objetos.values()) {
			if(b != null) b.aoAjustar(v, h);
		}
	}

	public void liberar() {
		sb.dispose();
		fonte.dispose();
		for(Objeto b : objetos.values()) {
			b.aoFim();
		}
		objetos.clear();
	}

	public static BitmapFont carregarFonte(String caminho, int tamanho) {
		FreeTypeFontGenerator gerador = new FreeTypeFontGenerator(caminho.equals("ui/fontes/pixel.ttf") ? Gdx.files.internal(caminho) : Gdx.files.absolute(Inicio.externo+"/MiniMine/mods/"+caminho));
		FreeTypeFontGenerator.FreeTypeFontParameter args = new FreeTypeFontGenerator.FreeTypeFontParameter();
		args.size = tamanho;
		args.characters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_?!π√×|§°←↑↓→€¥¢£+-/×%^*#$&|=,.:;!?(){}[]<>@\"'~áãâçéêèēóõôò";
		BitmapFont fonte = gerador.generateFont(args);
		gerador.dispose();
		return fonte;
	}
	
	public static BitmapFont carregarFonte(String caminho) {
		FreeTypeFontGenerator gerador = new FreeTypeFontGenerator(caminho.equals("ui/fontes/pixel.ttf") ? Gdx.files.internal(caminho) : Gdx.files.absolute(Inicio.externo+"/MiniMine/mods/"+caminho));
		FreeTypeFontGenerator.FreeTypeFontParameter args = new FreeTypeFontGenerator.FreeTypeFontParameter();
		args.characters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_?!π√×|§°←↑↓→€¥¢£+-/×%^*#$&|=,.:;!?(){}[]<>@\"'~áãâçéêèēóõôò";
		BitmapFont fonte = gerador.generateFont(args);
		gerador.dispose();
		return fonte;
	}

	public static class Objeto {
		public String nome;

		public Objeto(String nome) {
			this.nome = nome;
		}
		public void porFrame(float delta, SpriteBatch sb, BitmapFont fonte) {}
		public void aoAjustar(int v, int h) {}
		public void aoTocar(int telaX, int telaY, int p) {}
		public void aoSoltar(int telaX, int telaY, int p) {}
		public void aoArrastar(int telaX, int telaY, int p) {}
		public void aoFim() {}
	}
}
