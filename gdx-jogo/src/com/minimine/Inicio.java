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

public class Inicio extends Game {
	public static boolean ehArm64;
	public static String externo;
	public static boolean telaNova = false;
	public static Screen telaAtual;
	public static Logs log = new Logs();
	
	public Inicio(String externo, Debugador debugador) {
		Inicio.externo = externo;
		UI.debugador = debugador;
		ehArm64 = debugador.ehArm64();
		ehArm64 = false;
	}

	@Override
	public void create() {
		Gdx.app.setApplicationLogger(log);
        Gdx.graphics.setVSync(false);
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
		if(telaAtual != null) telaAtual.render(Gdx.graphics.getDeltaTime());
	}

	@Override
	public void dispose() {
		super.dispose();
		try {
			for(Texture tex : Texturas.texs.values()) {
				tex.dispose();
			}
			Mundo.liberar();
			UI.liberar();
			NuvensUtil.liberar();
			CorposCelestes.liberar();
			Audio.liberar();
		} catch(Exception e) {
			Gdx.app.log("Inicio", "[ERRO] ao liberar: "+e);
		}
	}
}
