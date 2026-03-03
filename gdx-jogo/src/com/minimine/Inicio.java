package com.minimine;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Texture;
import com.minimine.cenas.Menu;
import com.minimine.graficos.Texturas;
import com.badlogic.gdx.Gdx;
import com.minimine.ui.UI;
import com.minimine.utils.NuvensUtil;
import com.minimine.utils.CorposCelestes;
import com.minimine.mundo.Mundo;
import com.minimine.audio.Audio;
import com.minimine.audio.Musicas;
import com.minimine.graficos.Modelos;

public class Inicio extends Game {
	public static boolean ehArm64;
	public static String externo;
	public static boolean telaNova = false;
	public static Screen telaAtual;
	public static Logs log = new Logs();
	public static Instalador instalador;
	
	public Inicio(String externo, Debugador debugador, Instalador instalador) {
		Inicio.externo = externo;
		UI.debugador = debugador;
		Inicio.instalador = instalador;
		ehArm64 = debugador.ehArm64();
		ehArm64 = false;
	}

	@Override
	public void create() {
		Gdx.app.setApplicationLogger(log);
        Gdx.graphics.setVSync(false);
		Gdx.graphics.setForegroundFPS(0); // fps ilimitado
		
		// blocos:
		Audio.addSom("grama_1", "audio/blocos/grama_1.mp3");
		Audio.addSom("terra_1", "audio/blocos/terra_1.mp3");
		Audio.addSom("terra_2", "audio/blocos/terra_2.mp3");
		Audio.addSom("terra_3", "audio/blocos/terra_3.mp3");
		Audio.addSom("pedra_1", "audio/blocos/pedra_1.mp3");
		Audio.addSom("pedra_2", "audio/blocos/pedra_2.mp3");
		Audio.addSom("madeira_1", "audio/blocos/madeira_1.mp3");
		Audio.addSom("madeira_2", "audio/blocos/madeira_2.mp3");
		Audio.addSom("madeira_3", "audio/blocos/madeira_3.mp3");

		// musicas
		Musicas.addMusica("igor", "audio/musicas/igor.ogg");
		Musicas.addMusica("igor-2", "audio/musicas/igor-2.ogg");
		
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
			CorposCelestes.liberar();
			Audio.liberar();
			Musicas.liberar();
			Modelos.liberar();
		} catch(Exception e) {
			Gdx.app.log("Inicio", "[ERRO] ao liberar: "+e);
		}
	}
}
