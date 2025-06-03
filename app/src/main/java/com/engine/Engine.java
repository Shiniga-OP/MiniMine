package com.engine;            

import android.opengl.GLES30;            
import android.opengl.GLSurfaceView;            
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import java.nio.FloatBuffer;
import android.opengl.Matrix;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.ArrayList;
import android.graphics.BitmapFactory;
import android.content.Context;
import java.io.InputStream;
import android.graphics.Bitmap;
import android.opengl.GLUtils;
import android.widget.Toast;
import android.view.MotionEvent;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Color;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import java.util.Arrays;

class Cena3D {
    public Camera3D camera = new Camera3D();
    public ShaderUtils shader;
    public float[] matrizProj = new float[16];
    public float[] matrizView = new float[16];
    public float[] matrizModelo = new float[16];
    public float[] matrizFinal = new float[16];
    public int locMVP, locTex;
    public List<Objeto3D> objetos = new ArrayList<Objeto3D>();

    public void iniciar(String vert, String frag) {
        this.shader = new ShaderUtils(vert, frag);
        locMVP = GLES30.glGetUniformLocation(this.shader.id, "uMVP");
        locTex = GLES30.glGetUniformLocation(this.shader.id, "uTextura");
    }

    public void iniciar() {
        iniciar(ShaderUtils.obterVert3D(), ShaderUtils.obterFrag3D());
    }

    public void atualizarProjecao(int largura, int altura) {
        float ratio = (float) largura / altura;
        Matrix.perspectiveM(matrizProj, 0, 60, ratio, 1f, 100f);
    }

    public void render() {
        atualizarCamera();
        shader.usar();

        for (Objeto3D o : objetos) {
            GLES30.glBindVertexArray(o.vao);

            Matrix.setIdentityM(matrizModelo, 0);
            Matrix.translateM(matrizModelo, 0, o.x, o.y, o.z);
            Matrix.rotateM(matrizModelo, 0, o.rX, 1, 0, 0);
            Matrix.rotateM(matrizModelo, 0, o.rY, 0, 1, 0);
            Matrix.rotateM(matrizModelo, 0, o.rZ, 0, 0, 1);
            Matrix.scaleM(matrizModelo, 0, o.largura, o.altura, o.profundidade);

            Matrix.multiplyMM(matrizFinal, 0, matrizView, 0, matrizModelo, 0);
            Matrix.multiplyMM(matrizFinal, 0, matrizProj, 0, matrizFinal, 0);

            GLES30.glUniformMatrix4fv(locMVP, 1, false, matrizFinal, 0);
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, o.textura);

            GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, 36);
        }
        GLES30.glBindVertexArray(0);
    }

    public void atualizarVertices(Objeto3D o) {
        FloatBuffer buffer = ByteBuffer
            .allocateDirect(o.vertices.length * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer();
        buffer.put(o.vertices).flip();

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, o.vbo);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, o.vertices.length * 4, buffer, GLES30.GL_STATIC_DRAW);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0);
    }

    public void atualizarCamera() {
        Matrix.setLookAtM(matrizView, 0,
						  camera.posicao[0], camera.posicao[1], camera.posicao[2],
						  camera.posicao[0] + camera.foco[0],
						  camera.posicao[1] + camera.foco[1],
						  camera.posicao[2] + camera.foco[2],
						  camera.up[0], camera.up[1], camera.up[2]);
    }

    public void add(Objeto3D... os) {
        for (Objeto3D o : os) {
            FloatBuffer buffer = ByteBuffer
                .allocateDirect(o.vertices.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
            buffer.put(o.vertices).flip();

            int[] vaoId = new int[1];
            GLES30.glGenVertexArrays(1, vaoId, 0);
            o.vao = vaoId[0];

            int[] vboId = new int[1];
            GLES30.glGenBuffers(1, vboId, 0);
            o.vbo = vboId[0];

            GLES30.glBindVertexArray(o.vao);
            GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, o.vbo);
            GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, o.vertices.length * 4, buffer, GLES30.GL_STATIC_DRAW);

            GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, 5 * 4, 0);
            GLES30.glEnableVertexAttribArray(0);
            GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, 5 * 4, 3 * 4);
            GLES30.glEnableVertexAttribArray(1);

            GLES30.glBindVertexArray(0);
            objetos.add(o);
        }
    }

    public void remover(Objeto3D o) {
        objetos.remove(o);
    }
}

class Objeto3D {
	public float x = 0f, y = 0f, z = 0f;
	public float largura = 1f, altura = 1f, profundidade = 1f;
	public float u1 = 0f, v1 = 0f, u2 = 1f, v2 = 1f;
	public int textura, vbo, vao;
	public float rX = 0f, rY = 0f, rZ = 0f;
	public float[] vertices = new float[]{
		// frente
		-0.5f, -0.5f,  0.5f,  u1, v2,
		0.5f, -0.5f,  0.5f,  u2, v2,
		0.5f,  0.5f,  0.5f,  u2, v1,
		-0.5f, -0.5f,  0.5f,  u1, v2,
		0.5f,  0.5f,  0.5f,  u2, v1,
		-0.5f,  0.5f,  0.5f,  u1, v1,
		// trás
		-0.5f, -0.5f, -0.5f,  u2, v2,
		-0.5f,  0.5f, -0.5f,  u2, v1,
		0.5f,  0.5f, -0.5f,  u1, v1,
		-0.5f, -0.5f, -0.5f,  u2, v2,
		0.5f,  0.5f, -0.5f,  u1, v1,
		0.5f, -0.5f, -0.5f,  u1, v2,
		// lado esquerdo
		-0.5f, -0.5f, -0.5f,  u1, v2,
		-0.5f, -0.5f,  0.5f,  u2, v2,
		-0.5f,  0.5f,  0.5f,  u2, v1,
		-0.5f, -0.5f, -0.5f,  u1, v2,
		-0.5f,  0.5f,  0.5f,  u2, v1,
		-0.5f,  0.5f, -0.5f,  u1, v1,
		// lado direito
		0.5f, -0.5f, -0.5f,  u2, v2,
		0.5f,  0.5f, -0.5f,  u2, v1,
		0.5f,  0.5f,  0.5f,  u1, v1,
		0.5f, -0.5f, -0.5f,  u2, v2,
		0.5f,  0.5f,  0.5f,  u1, v1,
		0.5f, -0.5f,  0.5f,  u1, v2,
		//topo
		-0.5f,  0.5f, -0.5f,  u1, v1,
		-0.5f,  0.5f,  0.5f,  u1, v2,
		0.5f,  0.5f,  0.5f,  u2, v2,
		-0.5f,  0.5f, -0.5f,  u1, v1,
		0.5f,  0.5f,  0.5f,  u2, v2,
		0.5f,  0.5f, -0.5f,  u2, v1,
		// baixo
		-0.5f, -0.5f, -0.5f,  u1, v2,
		0.5f, -0.5f, -0.5f,  u2, v2,
		0.5f, -0.5f,  0.5f,  u2, v1,
		-0.5f, -0.5f, -0.5f,  u1, v2,
		0.5f, -0.5f,  0.5f,  u2, v1,
		-0.5f, -0.5f,  0.5f,  u1, v1,
	};
	
	public Objeto3D(float x, float y, float z, float largura, float altura, float profundidade, int textura) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.largura = largura;
		this.altura = altura;
		this.profundidade = profundidade;
		this.textura = (textura == -1) ? Texturas.texturaBranca() : textura;
	}
	
	public Objeto3D(float x, float y, float z, float[] vertices, int textura) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.vertices = vertices;
		this.textura = (textura == -1) ? Texturas.texturaBranca() : textura;
	}
	
	public void definirFaceUV(int face, float... uvs) {
		int inicio = face * 6 * 5;
		for(int i = 0; i < 6; i++) {
			vertices[inicio + i * 5 + 3] = uvs[i * 2];
			vertices[inicio + i * 5 + 4] = uvs[i * 2 + 1];
		}
	}
}

class Toque {
	public static float ultimoX, ultimoY;
	public static int pontoAtivo = -1;
	
	public static boolean eventoToque(Camera3D camera, MotionEvent e) {
        int acao = e.getActionMasked();
        switch(acao) {
            case MotionEvent.ACTION_DOWN:
                pontoAtivo = e.getPointerId(0);
                ultimoX = e.getX(0);
                ultimoY = e.getY(0);
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                if(pontoAtivo == -1) {
                    int indice = e.getActionIndex();
                    pontoAtivo = e.getPointerId(indice);
                    ultimoX = e.getX(indice);
                    ultimoY = e.getY(indice);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                int indicePonto = e.findPointerIndex(pontoAtivo);
                if(indicePonto != -1) {
                    float x = e.getX(indicePonto);
                    float y = e.getY(indicePonto);
                    float dx = x - ultimoX;
                    float dy = y - ultimoY;
                    camera.rotacionar(dx * 0.15f, dy * 0.15f);
                    ultimoX = x;
                    ultimoY = y;
                }
                break;
            case MotionEvent.ACTION_POINTER_UP:
                int ponto = e.getPointerId(e.getActionIndex());
                if(ponto==pontoAtivo) {
                    int novoIndice = (e.getActionIndex() == 0 ? 1 : 0);
                    pontoAtivo = e.getPointerId(novoIndice);
                    ultimoX = e.getX(novoIndice);
                    ultimoY = e.getY(novoIndice);
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                pontoAtivo = -1;
                break;
        } 
        return true;
    }
}
