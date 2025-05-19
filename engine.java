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

class GL {
	public static void limpar() {
		GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);
	}
	
	public static void corFundo(float r, float g, float b, float alfa) {
		GLES30.glClearColor(r, g, b, alfa);
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

class Cena2D {

    public int vao, vbo;
    public ShaderUtils shader;
    public float[] matrizProj = new float[16];
    public int locProjecao, locTexture;

    public List<Objeto2D> objetos = new ArrayList<Objeto2D>();

    public FloatBuffer bufferTemp = ByteBuffer
	.allocateDirect(4 * 4 * 4) // 4 vertices * 4 floats * 4 bytes
	.order(ByteOrder.nativeOrder())
	.asFloatBuffer();

    public void iniciar(int largura, int altura) {
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

        Matrix.orthoM(matrizProj, 0, 0, largura, altura, 0, -1, 1);

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

        for(Objeto2D obj : objetos) {
            bufferTemp.clear();
            bufferTemp.put(obj.x).put(obj.y).put(0f).put(0f);
            bufferTemp.put(obj.x).put(obj.y + obj.altura).put(0f).put(1f);
            bufferTemp.put(obj.x + obj.largura).put(obj.y).put(1f).put(0f);
            bufferTemp.put(obj.x + obj.largura).put(obj.y + obj.altura).put(1f).put(1f);
            bufferTemp.flip();

            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, obj.textura);
            GLES30.glBufferSubData(GLES30.GL_ARRAY_BUFFER, 0, bufferTemp.remaining() *4, bufferTemp);
            GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);
        }
        GLES30.glBindVertexArray(0);
    }

    public void atualizarProjecao(int largura, int altura) {
        Matrix.orthoM(matrizProj, 0, 0, largura, altura, 0, -1, 1);
    }

	public void add(final Objeto2D obj) {
        objetos.add(obj);
    }
}

class Cena3D {
	public Camera3D camera = new Camera3D();
	public ShaderUtils shader;
	public int vao, vbo;
	public float[] matrizProj = new float[16];
	public float[] matrizView = new float[16];
	public float[] matrizModelo = new float[16];
	public float[] matrizFinal = new float[16];
	public int locMVP, locTex;
	public List<Objeto3D> objetos = new ArrayList<Objeto3D>();

	// buffers:
	public FloatBuffer bufferVertices = ByteBuffer
	.allocateDirect(6 * 6 * 5 * 4) // 6 triangulos * 6 vertices * 5 floats * 4 bytes
	.order(ByteOrder.nativeOrder())
	.asFloatBuffer();

	public void iniciar() {
		shader = new ShaderUtils(ShaderUtils.obterVert3D(), ShaderUtils.obterFrag3D());

		int[] ids = new int[1];
		GLES30.glGenVertexArrays(1, ids, 0);
		vao = ids[0];
		GLES30.glBindVertexArray(vao);

		GLES30.glGenBuffers(1, ids, 0);
		vbo = ids[0];
		GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo);

		GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, 6 * 6 * 5 * 4, null, GLES30.GL_STATIC_DRAW);

		GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, 5 * 4, 0);
		GLES30.glEnableVertexAttribArray(0);
		GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, 5 * 4, 3 * 4);
		GLES30.glEnableVertexAttribArray(1);

		GLES30.glBindVertexArray(0);

		locMVP = GLES30.glGetUniformLocation(shader.id, "uMVP");
		locTex = GLES30.glGetUniformLocation(shader.id, "uTextura");
	}

	public void atualizarProjecao(int largura, int altura) {
		float ratio = (float) largura / altura;
		Matrix.perspectiveM(matrizProj, 0, 60, ratio, 1f, 100f);
	}

	public void render() {
		shader.usar();
		GLES30.glBindVertexArray(vao);

		for(Objeto3D o : objetos) {
			Matrix.setIdentityM(matrizModelo, 0);
			Matrix.translateM(matrizModelo, 0, o.x, o.y, o.z);
			Matrix.rotateM(matrizModelo, 0, o.rX, o.rX, o.rX, 0);
			Matrix.scaleM(matrizModelo, 0, o.largura, o.altura, o.profundidade);

			Matrix.multiplyMM(matrizFinal, 0, matrizView, 0, matrizModelo, 0);
			Matrix.multiplyMM(matrizFinal, 0, matrizProj, 0, matrizFinal, 0);
			
			o.rX += 1;

			GLES30.glUniformMatrix4fv(locMVP, 1, false, matrizFinal, 0);
			GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, o.textura);
			
			GLES30.glBufferSubData(GLES30.GL_ARRAY_BUFFER, 0, bufferVertices.capacity() * 4, bufferVertices);
			GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, 36);
		}
		GLES30.glBindVertexArray(0);
		atualizarCamera();
	}
	
	public void atualizarCamera() {
		Matrix.setLookAtM(matrizView, 0,
		camera.posicao[0], camera.posicao[1], camera.posicao[2],
		camera.posicao[0] + camera.foco[0],
		camera.posicao[1] + camera.foco[1],
		camera.posicao[2] + camera.foco[2],
		camera.up[0], camera.up[1], camera.up[2]);
	}

	public void add(Objeto3D obj) {
		objetos.add(obj);
		bufferVertices.put(obj.vertices);
		bufferVertices.flip();
	}
}

class Objeto2D {
    public float x, y, largura, altura;
    public int textura;

    public Objeto2D(float x, float y, float largura, float altura, int textura) {
		this.x = x;
        this.y = y;
        this.largura = largura;
        this.altura = altura;
        this.textura = (textura == -1) ? Texturas.texturaBranca() : textura;
	}
}

class Camera3D {
	public float[] posicao = new float[]{0f, 0f, 0f};
	public float[] foco = new float[]{0f, 0f, 0f};
	public float[] up = new float[]{0f, 1f, 0f};

	public float yaw = -90f;
    public float tom = 0f;

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
	public float x, y, z, largura, altura, profundidade;
	public int textura;
	public float u1 = 0f, v1 = 0f, u2 = 1f, v2 = 1f;
	public float rX = 1f;
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
		// topo
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
	
	public void definirFaceUV(int face, float... uvs) {
		int inicio = face * 6 * 5;
		for(int i = 0; i < 6; i++) {
			vertices[inicio + i * 5 + 3] = uvs[i * 2];
			vertices[inicio + i * 5 + 4] = uvs[i * 2 + 1];
		}
	}
}

class Texturas {
	public static int carregarAsset(Context ctx, String nomeArquivo) {
		try {
			InputStream is = ctx.getAssets().open(nomeArquivo);
			Bitmap bmp = BitmapFactory.decodeStream(is);
			int[] texID = new int[1];
			GLES30.glGenTextures(1, texID, 0);
			GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texID[0]);

			GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bmp, 0);

			GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST);
			GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_NEAREST);

			bmp.recycle();
			return texID[0];
		} catch(Exception e) {
			Debug.log("erro ao carregar textura: " + nomeArquivo + e.getMessage());
			return -1;
		}
	}

	public static int texturaBranca() {
        int[] tex = new int[1];
        GLES30.glGenTextures(1, tex, 0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, tex[0]);
        byte[] branco = {(byte)255, (byte)255, (byte)255, (byte)255};
        ByteBuffer buffer = ByteBuffer.allocateDirect(4);
        buffer.put(branco).position(0);
        GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA, 1, 1, 0,  GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, buffer);

        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_NEAREST);

        return tex[0];
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

class Debug {
	public static Context ctx;

	public static void definirCtx(Context contexto) {
		ctx = contexto;
	}

	public static void log(String msg) {
		if(ctx != null) Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show();
		else return;
	}
}
