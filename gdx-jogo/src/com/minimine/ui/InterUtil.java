package com.minimine.ui;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.Gdx;
import com.minimine.Inicio;
import java.util.Map;
import java.util.HashMap;
import com.badlogic.gdx.graphics.Color;

public class InterUtil {
	public static Map<String, BitmapFont> fontes = new HashMap<>();
	
	public static BitmapFont carregarFonte(String caminho, float escala, boolean externo) {
		if(fontes.containsKey(caminho)) {
			final BitmapFont fonte = fontes.get(caminho);
			if(escala != 0f) fonte.getData().setScale(escala);
			fonte.setColor(Color.WHITE);
			return fonte;
		}
		final BitmapFont fonte = new BitmapFont(externo ? Gdx.files.absolute(Inicio.externo+"/MiniMine/mods/"+caminho) : Gdx.files.internal(caminho));
		if(escala != 0f) fonte.getData().setScale(escala);
		fonte.setUseIntegerPositions(true);
		fonte.setColor(Color.WHITE);
		fontes.put(caminho, fonte);
		
		return fonte;
	}
	
	public static BitmapFont carregarFonte(String caminho) {
		return carregarFonte(caminho, 0f, false);
	}
	
	public static BitmapFont carregarFonte(String caminho, float escala) {
		return carregarFonte(caminho, escala, false);
	}
	
	public static void liberarFonte(BitmapFont fonte) {
		fontes.remove(fonte).dispose();
	}
	
	public static void liberar() {
		for(BitmapFont b : fontes.values()) {
			if(b != null) b.dispose();
			b = null;
		}
		fontes.clear();
	}
}
