package com.engine;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import java.nio.FloatBuffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class GL {
	public static GLSurfaceView tela;

	public static void definirTela(GLSurfaceView opengl) {
		tela = opengl;
	}

	public static void limpar() {
		GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);
	}

	public static void corFundo(float... cor) {
		GLES30.glClearColor(cor[0], cor[1], cor[2], cor[3]);
	}

	public static void definirRender(GLSurfaceView tela, GLSurfaceView.Renderer render) {
        tela.setEGLContextClientVersion(3);
        tela.setRenderer(render);
        tela.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

	public static void ativar3D(boolean ativo) {
		if(ativo == true) {
			GLES30.glEnable(GLES30.GL_DEPTH_TEST);
			GLES30.glEnable(GLES30.GL_CULL_FACE);
		} else {
			GLES30.glDisable(GLES30.GL_DEPTH_TEST);
			GLES30.glDisable(GLES30.GL_CULL_FACE);
		}
	}

	public static void ativarTransparente(boolean ativo) {
		if(ativo== true) GLES30.glEnable(GLES30.GL_BLEND);
		else GLES30.glDisable(GLES30.GL_BLEND);
	}

	public static void ajustarTela(int largura, int altura) {
		GLES30.glViewport(0, 0, largura, altura);
	}

	// memoria:
	public static int gerarVBO(float[] vertices) {
		FloatBuffer buffer = ByteBuffer.allocateDirect(vertices.length * 4)
			.order(ByteOrder.nativeOrder())
			.asFloatBuffer();
		buffer.put(vertices).position(0);

		int[] vbo = new int[1];
		GLES30.glGenBuffers(1, vbo, 0);
		GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo[0]);
		GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, buffer.capacity() * 4, buffer, GLES30.GL_STATIC_DRAW);

		return vbo[0];
	}

	public static int gerarVBO(float[] vertices, FloatBuffer buffer) {
		buffer.put(vertices).position(0);

		int[] vbo = new int[1];
		GLES30.glGenBuffers(1, vbo, 0);
		GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo[0]);
		GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, buffer.capacity() * 4, buffer, GLES30.GL_STATIC_DRAW);

		return vbo[0];
	}

	public static int gerarVAO(int vbo, int stride) {
		int[] vao = new int[1];
		GLES30.glGenVertexArrays(1, vao, 0);

		GLES30.glBindVertexArray(vao[0]);

		GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo);
		GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, stride, 0);
		GLES30.glEnableVertexAttribArray(0);

		GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, stride, 12);
		GLES30.glEnableVertexAttribArray(1);

		GLES30.glBindVertexArray(0);

		return vao[0];
	}

	public static int gerarVAO(int vbo) {
		int[] vao = new int[1];
		GLES30.glGenVertexArrays(1, vao, 0);

		GLES30.glBindVertexArray(vao[0]);

		GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo);
		GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, 5 * 4, 0);
		GLES30.glEnableVertexAttribArray(0);

		GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, 5 * 4, 12);
		GLES30.glEnableVertexAttribArray(1);

		GLES30.glBindVertexArray(0);

		return vao[0];
	}

	public static FloatBuffer criarBuffer(int tamanho) {
		FloatBuffer buffer = ByteBuffer.allocateDirect(tamanho * 4)
			.order(ByteOrder.nativeOrder())
			.asFloatBuffer();

		return buffer;
	}
}
