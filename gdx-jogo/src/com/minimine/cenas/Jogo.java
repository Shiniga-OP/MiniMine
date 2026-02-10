package com.minimine.cenas;

import com.badlogic.gdx.Screen;
import com.minimine.utils.Net;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import java.util.List;
import com.minimine.utils.ArquivosUtil;
import java.util.ArrayList;
import com.minimine.mods.LuaAPI;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.minimine.mundo.ChunkUtil;
import com.minimine.utils.NuvensUtil;
import com.minimine.utils.DiaNoiteUtil;
import com.minimine.graficos.Texturas;
import com.minimine.utils.CorposCelestes;
import com.minimine.Inicio;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.minimine.mods.Util;
import com.minimine.Logs;
import com.minimine.audio.Audio;
import com.minimine.mundo.Chunk;
import com.minimine.mundo.Mundo;
import com.minimine.ui.UI;
import com.minimine.graficos.Render;
import com.minimine.mundo.Biomas;
import com.minimine.entidades.Jogador;

public class Jogo implements Screen {
	public static Mundo mundo = new Mundo();
	public static Jogador jogador = new Jogador();
	public static Render render;
	
	public Net net;
	
    @Override
	public void show() {
		mundo.ciclo = true;
		
		// net = new Net(Net.SERVIDOR_MODO);
		
		render = new Render(jogador, mundo);
		if(ArquivosUtil.existe(Inicio.externo+"/MiniMine/mundos/"+mundo.nome+".mini")) ArquivosUtil.crMundo(mundo, jogador);
		render.mundo.iniciar();
		
		LuaAPI.iniciar();
		
		Audio.sons.put("grama_1", Gdx.audio.newMusic(Gdx.files.internal("audio/blocos/grama_1.mp3")));
		Audio.sons.put("terra_1", Gdx.audio.newMusic(Gdx.files.internal("audio/blocos/terra_1.mp3")));
		Audio.sons.put("terra_2", Gdx.audio.newMusic(Gdx.files.internal("audio/blocos/terra_2.mp3")));
		Audio.sons.put("terra_3", Gdx.audio.newMusic(Gdx.files.internal("audio/blocos/terra_3.mp3")));
		Audio.sons.put("pedra_1", Gdx.audio.newMusic(Gdx.files.internal("audio/blocos/pedra_1.mp3")));
		Audio.sons.put("pedra_2", Gdx.audio.newMusic(Gdx.files.internal("audio/blocos/pedra_2.mp3")));
		Audio.sons.put("madeira_1", Gdx.audio.newMusic(Gdx.files.internal("audio/blocos/madeira_1.mp3")));
		Audio.sons.put("madeira_2", Gdx.audio.newMusic(Gdx.files.internal("audio/blocos/madeira_2.mp3")));
		Audio.sons.put("madeira_3", Gdx.audio.newMusic(Gdx.files.internal("audio/blocos/madeira_3.mp3")));
		
		new java.util.Timer().schedule(
			new java.util.TimerTask() {
				@Override
				public void run() {
					if(mundo.ciclo) DiaNoiteUtil.att();
				}
			},
			0, 120
		);
	}

    @Override
	public void render(float delta) {
		render.att(delta);
		if(mundo.carregado) LuaAPI.att(delta);
    }

    @Override
    public void dispose() {
		mundo.carregado = false;
		render.liberar();
		net.liberar();
		CorposCelestes.liberar();
    }
	
	@Override
	public void resize(int v, int h) {
		render.ui.ajustar(v, h);
		Gdx.gl.glViewport(0, 0, v, h);
		LuaAPI.ajustar(v, h);
	}

	@Override
	public void hide() {
		mundo.carregado = false;
		for(Chunk c : mundo.chunks.values()) {
			if(c.malha != null) c.malha.dispose();
			c.malha = null;
		}
		mundo.chunks.clear();
		ArquivosUtil.svMundo(mundo, jogador);
		mundo.carregado = true;
	}
	@Override
	public void pause() {
		LuaAPI.iniciar();	
		mundo.carregado = false;
		for(Chunk c : mundo.chunks.values()) {
			if(c.malha != null) c.malha.dispose();
			c.malha = null;
		}
		mundo.chunks.clear();
		ArquivosUtil.svMundo(mundo, jogador);
		mundo.carregado = true;
	}
	@Override public void resume() {}
}
