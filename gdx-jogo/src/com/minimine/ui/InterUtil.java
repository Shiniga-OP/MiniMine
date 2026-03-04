package com.minimine.ui;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.Gdx;
import com.minimine.Inicio;
import java.util.Map;
import java.util.HashMap;

public class InterUtil {
	public static Map<String, BitmapFont> fontes = new HashMap<>();
	
	public static BitmapFont carregarFonte(String caminho, int tamanho) {
		if(fontes.containsKey(caminho)) fontes.get(caminho).dispose();
		
		FreeTypeFontGenerator gerador = new FreeTypeFontGenerator(caminho.equals("ui/fontes/pixel.ttf") ? Gdx.files.internal(caminho) : Gdx.files.absolute(Inicio.externo+"/MiniMine/mods/"+caminho));
		FreeTypeFontGenerator.FreeTypeFontParameter args = new FreeTypeFontGenerator.FreeTypeFontParameter();
		args.characters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_?!π√×|§°←↑↓→€¥¢£+-/×%^*#$&|=,.:;!?(){}[]<>@\"'~áãâçéêèēóõôò";
		if(tamanho != 0) args.size = tamanho;
		BitmapFont fonte = gerador.generateFont(args);
		gerador.dispose();
		
		fontes.put(caminho, fonte);
		
		return fonte;
	}
	
	public static BitmapFont carregarFonte(String caminho) {
		return carregarFonte(caminho, 0);
	}
	
	public static void liberar() {
		for(BitmapFont b : fontes.values()) b.dispose();
		fontes.clear();
	}
}
