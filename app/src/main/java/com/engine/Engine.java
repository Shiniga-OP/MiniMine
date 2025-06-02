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

class Cor {
	public static final float[] VERMELHO = new float[]{1f, 0f, 0f, 1f};
	public static final float[] VERDE   = new float[]{0f, 1f, 0f, 1f};
	public static final float[] AZUL    = new float[]{0f, 0f, 1f, 1f};
}

class Texto2D {
    public Objeto2D objeto;
    public int textura = -1;
    public String texto;
    public float x, y;
    public float tamanhoFonte;
    public float[] cor;

    public Texto2D(String texto, float x, float y, float tamanhoFonte, float... cor) {
        this.texto = (texto == "") ? "." : texto;
        this.x = x;
        this.y = y;
        this.tamanhoFonte = tamanhoFonte;
        this.cor = cor.clone();
        gerarRecursos();
    }

    private void gerarRecursos() {
        if(textura != -1) {
            GLES30.glDeleteTextures(1, new int[]{textura}, 0);
        }

        textura = criarTexturaBitmap();
        criarObjeto2D();
    }

    private int criarTexturaBitmap() {
        Paint paint = new Paint();
        paint.setTextSize(tamanhoFonte);
        paint.setColor(Color.WHITE);
        paint.setAntiAlias(true);
        paint.setTextAlign(Paint.Align.LEFT);

        Rect bounds = new Rect();
        paint.getTextBounds(texto, 0, texto.length(), bounds);
        float largura = paint.measureText(texto);
        float altura = bounds.height();

        Bitmap bitmap = Bitmap.createBitmap(
            (int) Math.ceil(largura), 
            (int) Math.ceil(altura), 
            Bitmap.Config.ARGB_8888
        );

        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        canvas.drawText(texto, 0, -bounds.top, paint);

        // Aplicar cor
        int color = Color.argb(
            (int)(cor[3] * 255), 
            (int)(cor[0] * 255), 
            (int)(cor[1] * 255), 
            (int)(cor[2] * 255)
        );
        bitmap = aplicarCor(bitmap, color);

        int[] texId = new int[1];
        GLES30.glGenTextures(1, texId, 0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texId[0]);
        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);

        bitmap.recycle();
        return texId[0];
    }

    private Bitmap aplicarCor(Bitmap original, int color) {
        Bitmap result = Bitmap.createBitmap(
            original.getWidth(), 
            original.getHeight(), 
            Bitmap.Config.ARGB_8888
        );
        Canvas canvas = new Canvas(result);
        Paint paint = new Paint();
        paint.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(original, 0, 0, paint);
        return result;
    }

    private void criarObjeto2D() {
        Paint paint = new Paint();
        paint.setTextSize(tamanhoFonte);
        Rect bounds = new Rect();
        paint.getTextBounds(texto, 0, texto.length(), bounds);

        objeto = new Objeto2D(
            x, y, 
            paint.measureText(texto), 
            bounds.height(), 
            textura
        );
    }
	
    public void definirTex(String novoTexto) {
        if(!texto.equals(novoTexto)) {
            texto = novoTexto;
            gerarRecursos();
        }
    }

    public void definirCor(float[] novaCor) {
        if(!Arrays.equals(cor, novaCor)) {
            cor = novaCor.clone();
            gerarRecursos();
        }
    }

    public void definirTam(float novoTamanho) {
        if(tamanhoFonte != novoTamanho) {
            tamanhoFonte = novoTamanho;
            gerarRecursos();
        }
    }

    public void definirPosicao(float x, float y) {
        this.x = x;
        this.y = y;
        if(objeto != null) {
            objeto.x = x;
            objeto.y = y;
        }
    }

    public void destruir() {
        if(textura != -1) {
            GLES30.glDeleteTextures(1, new int[]{textura}, 0);
            textura = -1;
        }
    }
}

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

class Camera3D {
	public float[] posicao = new float[]{0f, 0f, 0f};
	public float[] foco = new float[]{0f, 0f, 0f};
	public float[] up = new float[]{0f, 1f, 0f};

	public float yaw = -90f;
    public float tom = 0f;
	
	public Camera3D() {
		rotacionar(0, 0);
	}

	public void rotacionar(float dx, float dy) {
        yaw += dx;
        tom -= dy;

        if(tom > 89f) tom = 89f;
        if(tom < -89f) tom = -89f;

        foco[0] = (float) (Math.cos(Math.toRadians(yaw)) * (float) Math.cos(Math.toRadians(tom)));
        foco[1] = (float) Math.sin(Math.toRadians(tom));
        foco[2] = (float) (Math.sin(Math.toRadians(yaw)) * (float) Math.cos(Math.toRadians(tom)));
        normalize(foco);
    }

    public void mover(float velocidade) {
        posicao[0] += foco[0] * velocidade;
        posicao[1] += foco[1] * velocidade;
        posicao[2] += foco[2] * velocidade;
    }

    public void strafe(float velocidade) {
        float[] direita = {
            foco[2], 0f, -foco[0]
        };
        normalize(direita);

        posicao[0] -= direita[0] * velocidade;
        posicao[2] -= direita[2] * velocidade;
    }

    public void normalize(float[] vec) {
        float tamanho = (float) Math.sqrt(vec[0]*vec[0] + vec[1]*vec[1] + vec[2] *vec[2]);
        if(tamanho == 0f) return;
        vec[0] /= tamanho;
        vec[1] /= tamanho;
        vec[2] /= tamanho;
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
