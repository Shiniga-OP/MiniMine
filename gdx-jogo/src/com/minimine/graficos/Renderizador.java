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
	public static DiaNoiteUtil diaNoite;
	public static UI ui;
	public static boolean pause = false;
	public static GerenciadorParticulas gp;
    public static ModelBatch mb; // gerenciador de modelos 3D de entidades
	public static int PASSO = 20;
	public static List<Jogador> jogadores;
	
	public Renderizador(List<Jogador> jogadores, Mundo mundo) {
        this.ui = new UI(jogadores.get(0));
		this.jogadores = jogadores;
        this.mundo = mundo;
		this.diaNoite = new DiaNoiteUtil();
	}
	
	public void iniciar() {
		Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());  
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glCullFace(GL20.GL_BACK);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
		Gdx.gl.glFrontFace(GL20.GL_CCW);
	}
	public void att(float delta) {}
	
	public void liberar() {
		ui.liberar();
        ui.jg.liberar();
        mundo.liberar();
		mb.dispose();
        gp.liberar();
		if(mundo.ciclo) diaNoite.liberar();
	}
	
	public static void renderChunk(Chunk chunk, int iboId, int posIndices, int contaIndices, ShaderProgram shader) {
        Gdx.gl.glBindBuffer(GL20.GL_ARRAY_BUFFER, chunk.vboId);
        Gdx.gl.glBindBuffer(GL20.GL_ELEMENT_ARRAY_BUFFER, iboId);

        final int posLoc = shader.getAttributeLocation("a_pos");
        final int texCoordLoc = shader.getAttributeLocation("a_texCoord");
        final int texIdLoc = shader.getAttributeLocation("a_texId");
        final int corLoc = shader.getAttributeLocation("a_cor");

        if(posLoc >= 0) {
            Gdx.gl.glEnableVertexAttribArray(posLoc);
            Gdx.gl.glVertexAttribPointer(posLoc, 1, GL20.GL_FLOAT, false, PASSO, 0);
        }
        if(texCoordLoc >= 0) {
            Gdx.gl.glEnableVertexAttribArray(texCoordLoc);
            Gdx.gl.glVertexAttribPointer(texCoordLoc, 2, GL20.GL_FLOAT, false, PASSO, 4);
        }
        if(texIdLoc >= 0) {
            Gdx.gl.glEnableVertexAttribArray(texIdLoc);
            Gdx.gl.glVertexAttribPointer(texIdLoc, 1, GL20.GL_FLOAT, false, PASSO, 12);
        }
        if(corLoc >= 0) {
            Gdx.gl.glEnableVertexAttribArray(corLoc);
            // a_cor é 4 bytes sem sinal normalizados num float
            Gdx.gl.glVertexAttribPointer(corLoc, 4, GL20.GL_UNSIGNED_BYTE, true, PASSO, 16);
        }
        // posição em bytes: cada indice é um short(2 bytes)
        Gdx.gl20.glDrawElements(GL20.GL_TRIANGLES, contaIndices, GL20.GL_UNSIGNED_SHORT, posIndices * 2);

        if(posLoc >= 0) Gdx.gl.glDisableVertexAttribArray(posLoc);
        if(texCoordLoc >= 0) Gdx.gl.glDisableVertexAttribArray(texCoordLoc);
        if(texIdLoc >= 0) Gdx.gl.glDisableVertexAttribArray(texIdLoc);
        if(corLoc >= 0) Gdx.gl.glDisableVertexAttribArray(corLoc);

        Gdx.gl.glBindBuffer(GL20.GL_ARRAY_BUFFER, 0);
        Gdx.gl.glBindBuffer(GL20.GL_ELEMENT_ARRAY_BUFFER, 0);
    }
}
