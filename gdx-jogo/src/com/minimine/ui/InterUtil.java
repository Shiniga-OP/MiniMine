package com.minimine.ui;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.Gdx;
import com.minimine.Inicio;
import java.util.Map;
import java.util.HashMap;

public class InterUtil {
	public static Map<String, BitmapFont> fontes = new HashMap<>();
	
	public static BitmapFont carregarFonte(String caminho, float escala, boolean externo) {
		if(fontes.containsKey(caminho)) {
			BitmapFont fonte = fontes.get(caminho);
			if(escala != 0f) fonte.getData().setScale(escala);
			return fonte;
		}
		BitmapFont fonte = new BitmapFont(externo ? Gdx.files.absolute(Inicio.externo+"/MiniMine/mods/"+caminho) : Gdx.files.internal(caminho));
		if(escala != 0f) fonte.getData().setScale(escala);
		fonte.setUseIntegerPositions(true);
		fontes.put(caminho, fonte);
		
		return fonte;
	}
	
	public static BitmapFont carregarFonte(String caminho) {
		return carregarFonte(caminho, 0f, false);
	}
	
	public static BitmapFont carregarFonte(String caminho, float escala) {
		return carregarFonte(caminho, escala, false);
	}
	
	public static void liberarFonte(BitmapFont... fonte) {
		for(int i = 0; i < fonte.length; i++) {
			fontes.remove(fonte[i]);
			fonte[i].dispose();
		}
	}
	
	public static void liberar() {
		for(BitmapFont b : fontes.values()) {
			if(b != null) b.dispose();
			b = null;
		}
		fontes.clear();
	}
}
