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

class GL {
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
	
	public static void ativar3D() {
		GLES30.glEnable(GLES30.GL_DEPTH_TEST);
        GLES30.glEnable(GLES30.GL_CULL_FACE);
        GLES30.glCullFace(GLES30.GL_BACK);
	}
	
	public static void ajustarTela(int largura, int altura) {
		GLES30.glViewport(0, 0, largura, altura);
	}
}

class ShaderUtils {
    public int id;

    public ShaderUtils(String vert, String frag) {
        int vs = compilar(GLES30.GL_VERTEX_SHADER, vert);
        int fs = compilar(GLES30.GL_FRAGMENT_SHADER, frag);
        id = GLES30.glCreateProgram();
        GLES30.glAttachShader(id, vs);
        GLES30.glAttachShader(id, fs);
        GLES30.glLinkProgram(id);
        int[] status = new int[1];
        GLES30.glGetProgramiv(id, GLES30.GL_LINK_STATUS, status, 0);
        if(status[0] == 0) {
            String log = GLES30.glGetProgramInfoLog(id);                
            Debug.log("erro ao linkar programa:\n" + log);
        }
        GLES30.glDeleteShader(vs);
        GLES30.glDeleteShader(fs);
    }

    public int compilar(int tipo, String fonte) {
        int shader = GLES30.glCreateShader(tipo);
        GLES30.glShaderSource(shader, fonte);
        GLES30.glCompileShader(shader);
        int[] status = new int[1];
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, status, 0);
        if(status[0] == 0) {
            String log = GLES30.glGetShaderInfoLog(shader);
            Debug.log("erro compilando shader: " + log);
        }
        return shader;
    }

    public void usar() {
        GLES30.glUseProgram(id);
    }

    public static String obterVert2D() {
        return
            "#version 300 es\n"+ 
            "layout(location = 0) in vec2 aPos;\n"+
            "layout(location = 1) in vec2 aTex;\n"+
            "uniform mat4 uProjecao;\n"+
            "out vec2 vTex;\n"+
            "void main() {\n"+
            "gl_Position = uProjecao * vec4(aPos, 0.0, 1.0);\n"+
            "vTex = aTex;\n"+
            "}";
    }

    public static String obterFrag2D() {
        return
            "#version 300 es\n"+
            "precision mediump float;\n"+
            "in vec2 vTex;\n"+
            "uniform sampler2D uTextura;\n"+
            "out vec4 fragCor;\n"+
            "void main() {\n"+
            "fragCor = texture(uTextura, vTex);\n"+
            "}";
    }

	public static String obterVert3D() {
		return
			"#version 300 es\n"+
			"layout(location = 0) in vec3 aPos;\n"+
			"layout(location = 1) in vec2 aTex;\n"+
			"uniform mat4 uMVP;\n"+
			"out vec2 vTex;\n"+
			"void main() {\n"+
			"gl_Position = uMVP * vec4(aPos, 1.0);\n"+
			"vTex = aTex;\n"+
			"}";
	}

	public static String obterFrag3D() {
		return
			"#version 300 es\n"+
			"precision mediump float;\n"+
			"in vec2 vTex;\n"+
			"uniform sampler2D uTextura;\n"+
			"out vec4 fragCor;\n"+
			"void main() {\n"+
			"fragCor = texture(uTextura, vTex);\n"+
			"}";
	}
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

class PerlinNoise2D {
    private static final int[] p = new int[512];
    private static final int[] permutacao = {
        151,160,137,91,90,15,
        131,13,201,95,96,53,194,233,7,225,140,36,103,30,69,142,8,99,37,240,21,10,23,
        190,6,148,247,120,234,75,0,26,197,62,94,252,219,203,117,35,11,32,57,177,33,
        88,237,149,56,87,174,20,125,136,171,168,68,175,74,165,71,134,139,48,27,166,
        77,146,158,231,83,111,229,122,60,211,133,230,220,105,92,41,55,46,245,40,244,
        102,143,54,65,25,63,161,1,216,80,73,209,76,132,187,208,89,18,169,200,196,
        135,130,116,188,159,86,164,100,109,198,173,186,3,64,52,217,226,250,124,123,
        5,202,38,147,118,126,255,82,85,212,207,206,59,227,47,16,58,17,182,189,28,42,
        223,183,170,213,119,248,152,2,44,154,163,70,221,153,101,155,167,43,172,9,
        129,22,39,253,19,98,108,110,79,113,224,232,178,185,112,104,218,246,97,228,
        251,34,242,193,238,210,144,12,191,179,162,241,81,51,145,235,249,14,239,107,
        49,192,214,31,181,199,106,157,184,84,204,176,115,121,50,45,127,4,150,254,
        138,236,205,93,222,114,67,29,24,72,243,141,128,195,78,66,215,61,156,180
    };
    public static final float[][] GRADIENTS = {
        { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 },
        { 1, 1 }, { -1, 1 }, { 1, -1 }, { -1, -1 }
    };

    static {
        for (int i = 0; i < 256; i++) {
            p[i] = permutacao[i];
            p[i + 256] = permutacao[i];
        }
    }

    public static float ruido(float x, float z, int seed) {
        int X = ((int)Math.floor(x) + seed) & 255;
        int Z = ((int)Math.floor(z) + seed) & 255;
        float xf = x - (int)Math.floor(x);
        float zf = z - (int)Math.floor(z);

        float u = fade(xf);
        float v = fade(zf);

        int A = p[X] + Z;
        int B = p[X + 1] + Z;

        float gradAA = grad(p[A], xf, zf);
        float gradBA = grad(p[B], xf - 1, zf);
        float gradAB = grad(p[A + 1], xf, zf - 1);
        float gradBB = grad(p[B + 1], xf - 1, zf - 1);

        float lerpX1 = lerp(u, gradAA, gradBA);
        float lerpX2 = lerp(u, gradAB, gradBB);
        return lerp(v, lerpX1, lerpX2);
    }

    public static float fade(float t) {
        return t * t * t * (t * (t * 6 - 15) + 10);
    }

    public static float lerp(float t, float a, float b) {
        return a + t * (b - a);
    }

    public static float grad(int hash, float x, float y) {
        int h = hash & 7;
        float[] g = GRADIENTS[h];
        return g[0] * x + g[1] * y;
    }
}

class PerlinNoise3D {
    private static final int[] p = new int[512];
    private static final int[] permutacao = {
        151,160,137,91,90,15,
        131,13,201,95,96,53,194,233,7,225,140,36,103,30,69,142,8,99,37,240,21,10,23,
        190,6,148,247,120,234,75,0,26,197,62,94,252,219,203,117,35,11,32,57,177,33,
        88,237,149,56,87,174,20,125,136,171,168,68,175,74,165,71,134,139,48,27,166,
        77,146,158,231,83,111,229,122,60,211,133,230,220,105,92,41,55,46,245,40,244,
        102,143,54,65,25,63,161,1,216,80,73,209,76,132,187,208,89,18,169,200,196,
        135,130,116,188,159,86,164,100,109,198,173,186,3,64,52,217,226,250,124,123,
        5,202,38,147,118,126,255,82,85,212,207,206,59,227,47,16,58,17,182,189,28,42,
        223,183,170,213,119,248,152,2,44,154,163,70,221,153,101,155,167,43,172,9,
        129,22,39,253,19,98,108,110,79,113,224,232,178,185,112,104,218,246,97,228,
        251,34,242,193,238,210,144,12,191,179,162,241,81,51,145,235,249,14,239,107,
        49,192,214,31,181,199,106,157,184,84,204,176,115,121,50,45,127,4,150,254,
        138,236,205,93,222,114,67,29,24,72,243,141,128,195,78,66,215,61,156,180
    };
    private static final int[][] GRADIENTES = {
        {1,1,0},{-1,1,0},{1,-1,0},{-1,-1,0},
        {1,0,1},{-1,0,1},{1,0,-1},{-1,0,-1},
        {0,1,1},{0,-1,1},{0,1,-1},{0,-1,-1}
    };

    static {
        for(int i = 0; i < 256; i++) {
            p[i] = permutacao[i];
            p[i + 256] = permutacao[i];
        }
    }

    public static float ruido(float x, float y, float z, int seed) {
        int X = ((int)Math.floor(x) + seed) & 255;
        int Y = ((int)Math.floor(y) + seed) & 255;
        int Z = ((int)Math.floor(z) + seed) & 255;
        float xf = x - (int)Math.floor(x);
        float yf = y - (int)Math.floor(y);
        float zf = z - (int)Math.floor(z);

        float u = fade(xf);
        float v = fade(yf);
        float w = fade(zf);

        int A  = p[X] + Y;
        int AA = p[A] + Z;
        int AB = p[A + 1] + Z;
        int B  = p[X + 1] + Y;
        int BA = p[B] + Z;
        int BB = p[B + 1] + Z;

        float g000 = grad(p[AA],     xf,     yf,     zf);
        float g001 = grad(p[AA + 1], xf,     yf,     zf - 1);
        float g010 = grad(p[AB],     xf,     yf - 1, zf);
        float g011 = grad(p[AB + 1], xf,     yf - 1, zf - 1);
        float g100 = grad(p[BA],     xf - 1, yf,     zf);
        float g101 = grad(p[BA + 1], xf - 1, yf,     zf - 1);
        float g110 = grad(p[BB],     xf - 1, yf - 1, zf);
        float g111 = grad(p[BB + 1], xf - 1, yf - 1, zf - 1);

        float lerpX00 = lerp(u, g000, g100);
        float lerpX01 = lerp(u, g001, g101);
        float lerpX10 = lerp(u, g010, g110);
        float lerpX11 = lerp(u, g011, g111);

        float lerpY0 = lerp(v, lerpX00, lerpX10);
        float lerpY1 = lerp(v, lerpX01, lerpX11);

        return lerp(w, lerpY0, lerpY1);
    }

    private static float fade(float t) {
        return t * t * t * (t * (t * 6 - 15) + 10);
    }

    private static float lerp(float t, float a, float b) {
        return a + t * (b - a);
    }

    private static float grad(int hash, float x, float y, float z) {
        int h = hash & 15;
        int[] g = GRADIENTES[h % 12];
        return g[0]*x + g[1]*y + g[2]*z;
    }
}

class Debug {
	public static Context ctx;

	public static void definirCtx(Context contexto) {
		ctx = contexto;
	}

	public static void log(String msg) {
		if(ctx != null) Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show();
		else return;
	}
	
	public static void log(String msg, int tempo) {
		if(ctx != null) Toast.makeText(ctx, msg, tempo).show();
		else return;
	}
}
