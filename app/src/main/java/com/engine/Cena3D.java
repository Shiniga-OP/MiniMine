package com.engine;  

import java.util.List;  
import android.opengl.GLES30;  
import android.opengl.Matrix;  
import java.util.ArrayList;  
import java.nio.FloatBuffer;  
import java.nio.ByteOrder;  
import java.nio.ByteBuffer;  

public class Cena3D {  
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
            Matrix.translateM(matrizModelo, 0, o.posicao[0], o.posicao[1], o.posicao[2]);  
            Matrix.rotateM(matrizModelo, 0, o.rotacao[0], 1, 0, 0);  
            Matrix.rotateM(matrizModelo, 0, o.rotacao[1], 0, 1, 0);  
            Matrix.rotateM(matrizModelo, 0, o.rotacao[2], 0, 0, 1);  
            Matrix.scaleM(matrizModelo, 0, o.tamanho[0], o.tamanho[1], o.tamanho[2]);  

            Matrix.multiplyMM(matrizFinal, 0, matrizView, 0, matrizModelo, 0);  
            Matrix.multiplyMM(matrizFinal, 0, matrizProj, 0, matrizFinal, 0);  

            GLES30.glUniformMatrix4fv(locMVP, 1, false, matrizFinal, 0);  
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, o.textura.textura);  

            GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, 36);  
        }  
        GLES30.glBindVertexArray(0);  
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
        for(int i = 0; i < os.length; i++) {  
            os[i].vbo = GL.gerarVBO(os[i].obterVertices());  
			os[i].vbo = GL.gerarVAO(os[i].vbo);  
            objetos.add(os[i]);  
        }  
    }  

    public void remover(Objeto3D... os) {  
        for(int i = 0; i < os.length; i++) objetos.remove(os[i]);  
    }  

	public class Objeto3D {  
		public float[] posicao = {0f, 0f, 0f};  
		public float[] tamanho = {1f, 1f, 1f};  
		public float[] rotacao = {0f, 0f, 0f};  
		public Atlas textura;  
		public int vbo, vao;  
		/*  
		 public Objeto3D(float... posicao, int textura) {  
		 this.posicao = posicao;  
		 this.textura = (textura == -1) ? Texturas.texturaBranca() : textura;  
		 }  
		 */  
		public Objeto3D(float... posicao, Atlas textura) {  
			this.posicao = posicao;  

		}  

		public float[] obterVertices() {
			float[] vertices = new float[6 * 6 * 5]; // 6 faces × 2 triângulos × (3 pos + 2 UV)
			int i = 0;

			// Posição dos 8 vértices
			float[][] v = {
				{-0.5f, -0.5f, -0.5f}, // 0
				{ 0.5f, -0.5f, -0.5f}, // 1
				{ 0.5f,  0.5f, -0.5f}, // 2
				{-0.5f,  0.5f, -0.5f}, // 3
				{-0.5f, -0.5f,  0.5f}, // 4
				{ 0.5f, -0.5f,  0.5f}, // 5
				{ 0.5f,  0.5f,  0.5f}, // 6
				{-0.5f,  0.5f,  0.5f}  // 7
			};

			// Índices para as 6 faces
			int[][] faces = {
				{3, 2, 6, 7}, // cima
				{0, 1, 5, 4}, // baixo
				{4, 5, 6, 7}, // frente
				{1, 0, 3, 2}, // trás
				{0, 4, 7, 3}, // esquerda
				{5, 1, 2, 6}  // direita
			};

			for (int f = 0; f < 6; f++) {
				float[] uv = textura.uvs[f];
				float u0 = uv[0], v0 = uv[1], u1 = uv[2], v1 = uv[3];

				int[] idx = faces[f];

				// Triângulo 1
				i = inserirVertice(vertices, i, v[idx[0]], u0, v1);
				i = inserirVertice(vertices, i, v[idx[1]], u1, v1);
				i = inserirVertice(vertices, i, v[idx[2]], u1, v0);

				// Triângulo 2
				i = inserirVertice(vertices, i, v[idx[0]], u0, v1);
				i = inserirVertice(vertices, i, v[idx[2]], u1, v0);
				i = inserirVertice(vertices, i, v[idx[3]], u0, v0);
			}

			return vertices;
		}

		private int inserirVertice(float[] v, int i, float[] pos, float u, float vtx) {
			v[i++] = pos[0];
			v[i++] = pos[1];
			v[i++] = pos[2];
			v[i++] = u;
			v[i++] = vtx;
			return i;
		}  
	}  

	public class Atlas {
		public int textura;
		public int tamX, tamY;
		public float[][] uvs = new float[6][4]; // 6 faces, cada uma com UVs (u0, v0, u1, v1)

		public Atlas(int textura, int tamX, int tamY,
					 int cimaX, int cimaY,
					 int baixoX, int baixoY,
					 int frenteX, int frenteY,
					 int trasX, int trasY,
					 int esqX, int esqY,
					 int dirX, int dirY) {

			this.textura = textura;
			this.tamX = tamX;
			this.tamY = tamY;

			uvs[0] = calcularUV(cimaX, cimaY);
			uvs[1] = calcularUV(baixoX, baixoY);
			uvs[2] = calcularUV(frenteX, frenteY);
			uvs[3] = calcularUV(trasX, trasY);
			uvs[4] = calcularUV(esqX, esqY);
			uvs[5] = calcularUV(dirX, dirY);
		}

		private float[] calcularUV(int x, int y) {
			float u = (float) x / tamX;
			float v = (float) y / tamY;
			float su = 1f / (tamX / 16f);
			float sv = 1f / (tamY / 16f);
			return new float[]{u, v, u + su, v + sv}; // u0, v0, u1, v1
		}
	}  
}
