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
    public ShaderUtils shader;
    public float[] matrizProj;
    public int locProjecao, locTexture;

    public List<Objeto2D> objetos;
	public List<Botao2D> botoes;

    public FloatBuffer bufferTemp;

    public void iniciar() {
		matrizProj = new float[16];
		objetos = new ArrayList<>();
		botoes = new ArrayList<>();
		bufferTemp = ByteBuffer
			.allocateDirect(4 * 4 * 4) // 4 vertices * 4 floats * 4 bytes
			.order(ByteOrder.nativeOrder())
			.asFloatBuffer();
        shader = new ShaderUtils(ShaderUtils.obterVert2D(), ShaderUtils.obterFrag2D());
        int[] ids = new int[1];
        GLES30.glGenVertexArrays(1, ids, 0);
        vao = ids[0];
        GLES30.glBindVertexArray(vao);

        GLES30.glGenBuffers(1, ids, 0);             
        vbo = ids[0];      
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, 4 * 4 *4, null, GLES30.GL_DYNAMIC_DRAW);   

        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, 4 *4, 0);
        GLES30.glEnableVertexAttribArray(0);
        GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, 4 *4, 2 *4);
        GLES30.glEnableVertexAttribArray(1);

        GLES30.glBindVertexArray(0);

        locProjecao = GLES30.glGetUniformLocation(shader.id, "uProjecao");
        locTexture = GLES30.glGetUniformLocation(shader.id, "uTextura");
    }

    public void render() {
        shader.usar();
        GLES30.glUniformMatrix4fv(locProjecao, 1, false, matrizProj, 0);
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glUniform1i(locTexture, 0);

        GLES30.glBindVertexArray(vao);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo);

        for(Objeto2D o : objetos) {
            bufferTemp.clear();
            bufferTemp.put(o.x).put(o.y).put(0f).put(0f);
            bufferTemp.put(o.x).put(o.y + o.altura).put(0f).put(1f);
            bufferTemp.put(o.x + o.largura).put(o.y).put(1f).put(0f);
            bufferTemp.put(o.x + o.largura).put(o.y + o.altura).put(1f).put(1f);
            bufferTemp.flip();

            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, o.textura);
            GLES30.glBufferSubData(GLES30.GL_ARRAY_BUFFER, 0, bufferTemp.remaining() *4, bufferTemp);
            GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);
        }
        GLES30.glBindVertexArray(0);
    }

	public void atualizarProjecao(int h, int v) {
		Matrix.orthoM(matrizProj, 0, 0, h, v, 0, -1, 1);
		for(int i = 0; i < objetos.size(); i++) {
			if(h < v) {
				objetos.get(i).x = (h - objetos.get(i).largura) - objetos.get(i).x;
				objetos.get(i).y = (v - objetos.get(i).altura) - objetos.get(i).y;
			} else {
				objetos.get(i).x = (h - objetos.get(i).largura) - (objetos.get(i).x + h / 2.2f);
				objetos.get(i).y = (v - objetos.get(i).altura) - objetos.get(i).y;
			}
		}
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

	public void remover(Objeto2D... os) {
		for(int i = 0; i < os.length; i++) objetos.remove(os[i]);
	}
	
	public void remover(Botao2D... os) {
		for(int i = 0; i < os.length; i++) objetos.remove(os[i].objeto);
	}
}
