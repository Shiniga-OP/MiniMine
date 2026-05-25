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

public class Inicio extends Game {
	public static String externo;
	public static boolean telaNova = false;
	public static Screen telaAtual;
	public static Logs log = new Logs();
	
	public Inicio(String externo) {
		Inicio.externo = externo;
	}

	@Override
	public void create() {
		Gdx.app.setApplicationLogger(log);
		
        Gdx.graphics.setVSync(false);
		Gdx.graphics.setForegroundFPS(0); // fps ilimitado
		
		Audio.iniciar();
		Musicas.iniciar();
		Texturas.iniciar();
		
		defTela(Cenas.intro);
	}

	public static void defTela(Screen tela) {
		telaAtual = tela;
		telaNova = true;
	}

	@Override
	public void render() {
		super.render();
		if(telaNova) {
			setScreen(telaAtual);
			telaNova = false;
		}
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
