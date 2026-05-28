package com.minimine.graficos;

import com.minimine.mundo.Mundo;
import com.minimine.utils.DiaNoiteUtil;
import com.minimine.ui.UI;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.minimine.entidades.Jogador;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.minimine.mundo.chunks.Chunk;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import java.util.List;

public class Renderizador {
	public static Mundo mundo;
	public static UI ui;
	public static boolean pause = false;
	public static GerenciadorParticulas gp;
    public static ModelBatch mb; // gerenciador de modelos 3D de entidades
	public static int PASSO = 20;
	public static List<Jogador> jogadores;
	public static int fps;
	
	public Renderizador(List<Jogador> jogadores, Mundo mundo) {
        this.ui = new UI(jogadores.get(0));
		this.jogadores = jogadores;
        this.mundo = mundo;
	}
	
	public void iniciar() {
		Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());  
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glCullFace(GL20.GL_BACK);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
		Gdx.gl.glFrontFace(GL20.GL_CCW);
	}
	public void att(float delta) {
		fps = Gdx.graphics.getFramesPerSecond();
	}
	
	public void liberar() {
		ui.liberar();
		mb.dispose();
        gp.liberar();
	}
}
