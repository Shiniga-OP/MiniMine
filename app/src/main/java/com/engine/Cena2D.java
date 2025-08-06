package com.engine;

import java.util.List;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.nio.ByteOrder;
import java.nio.ByteBuffer;
import android.opengl.GLES30;
import android.opengl.Matrix;

public class Cena2D {
    public int vao, vbo;
    public int shader;
    public float[] matrizProj;
    public int locProjecao, locTexture;

    public List<Objeto2D> objetos;
	public List<Botao2D> botoes;

    public FloatBuffer bufferTemp;

    public void iniciar() {
		matrizProj = new float[16];
		objetos = new ArrayList<>();
		botoes = new ArrayList<>();
		bufferTemp = GL.criarFloatBuffer(4 * 4);
        shader = ShaderUtils.criarPrograma(ShaderUtils.obterVert2D(), ShaderUtils.obterFrag2D());
        int[] ids = new int[1];
        GLES30.glGenVertexArrays(1, ids, 0);
        vao = ids[0];
        GLES30.glBindVertexArray(vao);

        GLES30.glGenBuffers(1, ids, 0);             
        vbo = ids[0];      
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, 4 * 4 *4, null, GLES30.GL_STATIC_DRAW);   

        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, 4 *4, 0);
        GLES30.glEnableVertexAttribArray(0);
        GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, 4 *4, 2 *4);
        GLES30.glEnableVertexAttribArray(1);

        GLES30.glBindVertexArray(0);

        locProjecao = GLES30.glGetUniformLocation(shader, "uProjecao");
        locTexture = GLES30.glGetUniformLocation(shader, "uTextura");
    }

    public void render() {
        ShaderUtils.usar(shader);
        GLES30.glUniformMatrix4fv(locProjecao, 1, false, matrizProj, 0);
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glUniform1i(locTexture, 0);

        GLES30.glBindVertexArray(vao);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo);

        for(Objeto2D o : objetos) {
			float x = o.x / larguraTela;
			float y = o.y / alturaTela;
			float largura = o.largura / larguraTela;
			float altura = o.altura / alturaTela;

			bufferTemp.clear();
			bufferTemp.put(x).put(y).put(0f).put(0f);
			bufferTemp.put(x).put(y + altura).put(0f).put(1f);
			bufferTemp.put(x + largura).put(y).put(1f).put(0f);
			bufferTemp.put(x + largura).put(y + altura).put(1f).put(1f);
			bufferTemp.flip();

			GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, o.textura);
			GLES30.glBufferSubData(GLES30.GL_ARRAY_BUFFER, 0, bufferTemp.remaining() * 4, bufferTemp);
			GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);
		}
        GLES30.glBindVertexArray(0);
    }

	public float larguraTela, alturaTela;

	public void atualizarProjecao(int h, int v) {
		larguraTela = h;
		alturaTela = v;
		Matrix.orthoM(matrizProj, 0, 0, 1f, 1f, 0, -1, 1);
	}

	public void add(Objeto2D... os) {
        for(int i = 0; i < os.length; i++) objetos.add(os[i]);
    }
	
	public void add(Botao2D... os) {
        for(int i = 0; i < os.length; i++) {
			botoes.add(os[i]);
			objetos.add(os[i].objeto);
		}
    }
	
	public void add(Texto2D... os) {
        for(int i = 0; i < os.length; i++) {
			objetos.add(os[i].objeto);
		}
    }

	public void remover(Objeto2D... os) {
		for(int i = 0; i < os.length; i++) objetos.remove(os[i]);
	}
	
	public void remover(Texto2D... os) {
		for(int i = 0; i < os.length; i++) objetos.remove(os[i].objeto);
	}
	
	public void remover(Botao2D... os) {
		for(int i = 0; i < os.length; i++) {
			objetos.remove(os[i].objeto);
			botoes.remove(os[i]);
		}
	}
}
