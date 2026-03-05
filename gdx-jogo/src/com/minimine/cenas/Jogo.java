package com.minimine.cenas;

import com.badlogic.gdx.Screen;
import com.minimine.entidades.Jogador;
import com.minimine.audio.Musicas;
import com.minimine.mundo.Mundo;
import com.minimine.graficos.Render;
import com.minimine.utils.ArquivosUtil;
import com.minimine.mods.LuaAPI;
import com.minimine.Inicio;
import com.minimine.utils.DiaNoiteUtil;
import com.minimine.mundo.blocos.Bloco;
import com.minimine.mundo.geracao.MotorGeracao;

public class Jogo implements Screen {
	public static Mundo mundo;
	public static Jogador jogador;
	public static Render render;
	public static boolean musicas = true;
	public static java.util.Timer relogio;
	
    @Override
	public void show() {
		relogio = new java.util.Timer();
		mundo = new Mundo();
		jogador = new Jogador();
		
		render = new Render(jogador, mundo);
		
		mundo.chunksMod.clear();
		
		Bloco.iniciar();
		
		if(ArquivosUtil.existe(Inicio.externo+"/MiniMine/mundos/"+mundo.nome+".mini")) ArquivosUtil.crMundo(mundo, jogador);
		
		render.mundo.iniciar();
		
		LuaAPI.iniciar();
		
		relogio.schedule(
			new java.util.TimerTask() {
				@Override
				public void run() {
					if(mundo.ciclo) DiaNoiteUtil.att();
				}
			}, 0, 120);
		relogio.schedule(
			new java.util.TimerTask() {
				@Override
				public void run() {
					if(musicas) Musicas.tocarAleatorio();
				}
			}, 0, 1000);
	}

    @Override
	public void render(float delta) {
		render.att(delta);
		if(mundo.carregado) LuaAPI.att(delta);
    }

    @Override
    public void dispose() {
		mundo.carregado = false;
		relogio.cancel();
		render.liberar();
		Bloco.liberar();
    }
	
	@Override
	public void resize(int v, int h) {
		render.ui.ajustar(v, h);
		LuaAPI.ajustar(v, h);
	}

	@Override
	public void hide() {
		ArquivosUtil.svMundo(mundo, jogador);
		dispose();
	}
	@Override public void pause() {}
	@Override public void resume() {}
}
