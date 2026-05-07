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
import com.badlogic.gdx.Gdx;
import com.minimine.mundo.geracao.RegistroBiomas;
import com.minimine.graficos.Renderizador;
import com.minimine.graficos.teste.GraficosTeste;
import com.minimine.mundo.chunks.ChunkProcesso;
import com.minimine.mundo.chunks.ChunkLuz;
import com.minimine.mundo.chunks.ChunkMalha;
import com.minimine.utils.Net;

public class Jogo implements Screen {
	public static Mundo mundo;
	public static Jogador jogador;
	public static int modo = 2;
	public static Renderizador render;
	public static boolean musicas = true, graficosTeste = false;
	public static java.util.Timer relogio;
	public static Net net;
	
    @Override
	public void show() {
		relogio = new java.util.Timer();
		mundo = new Mundo();
		jogador = new Jogador();
		jogador.modo = modo;
		
		mundo.chunksMod.clear();
		
		Bloco.iniciar();
		
		if(MultiMenu.modoRede != null) net = new Net(MultiMenu.modoRede);
		
		if(graficosTeste) {
			ChunkProcesso.luz = new ChunkLuz();
			ChunkProcesso.malha = new ChunkMalha();
			render = new GraficosTeste(jogador, mundo);
		} else {
			ChunkProcesso.luz = new ChunkLuz();
			ChunkProcesso.malha = new ChunkMalha();
			render = new Render(jogador, mundo);
		}
		if(ArquivosUtil.existe(Inicio.externo+"/MiniMine/mundos/"+mundo.nome+".mini")) ArquivosUtil.crMundo(mundo, jogador);
		
		render.iniciar();
		
		relogio.schedule(
			new java.util.TimerTask() {
				@Override
				public void run() {
					if(musicas) Musicas.tocarAleatorio();
				}
			}, 0, 1000);
		LuaAPI.iniciar();
	}

    @Override
	public void render(float delta) {
		render.att(delta);
		if(mundo.carregado) LuaAPI.att(delta);
    }

    @Override
    public void dispose() {
		mundo.carregado = false;
		ArquivosUtil.svMundo(mundo, jogador);
		relogio.cancel();
		render.liberar();
		Bloco.liberar();
		if(net != null) {
			net.liberar();
			net = null;
		}
    }
	
	@Override
	public void resize(int v, int h) {
		ArquivosUtil.svMundo(mundo, jogador);
		render.ui.ajustar(v, h);
		LuaAPI.ajustar(v, h);
	}

	@Override
	public void hide() {
		ArquivosUtil.svMundo(mundo, jogador);
		dispose();
	}
	@Override
	public void pause() {
		ArquivosUtil.svMundo(mundo, jogador);
	}
	@Override public void resume() {}
}
