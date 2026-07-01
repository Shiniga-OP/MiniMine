package com.minimine;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Texture;
import com.minimine.cenas.Menu;
import com.minimine.graficos.Texturas;
import com.badlogic.gdx.Gdx;
import com.minimine.ui.UI;
import com.minimine.utils.NuvensUtil;
import com.minimine.utils.DiaNoiteUtil;
import com.minimine.mundo.Mundo;
import com.minimine.audio.Audio;
import com.minimine.audio.Musicas;
import com.minimine.graficos.Modelos;
import com.minimine.ui.InterUtil;
import com.minimine.cenas.Jogo;
import com.minimine.cenas.Intro;

public class Inicio extends Game {
	public static String externo;
	public static Game tela;
	public static Logs log = new Logs();
	
	public Inicio(String externo) {
		Inicio.externo = externo;
		tela = this;
	}

	@Override
	public void create() {
		Gdx.app.setApplicationLogger(log);
		
        Gdx.graphics.setVSync(false);
		Gdx.graphics.setForegroundFPS(0); // fps ilimitado
		
		Audio.iniciar();
		Musicas.iniciar();
		Texturas.iniciar();
		
		setScreen(new Intro());
	}

	@Override
	public void dispose() {
		super.dispose();
		try {
			Texturas.liberar();
			Audio.liberar();
			Musicas.liberar();
			Modelos.liberar();
			InterUtil.liberar();
		} catch(Exception e) {
			Gdx.app.log("Inicio", "[ERRO] ao liberar: "+e);
		}
	}
}
