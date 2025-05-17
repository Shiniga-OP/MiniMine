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
import com.minimine.MainActivity;
import android.graphics.BitmapFactory;
import android.content.Context;
import java.io.InputStream;
import android.graphics.Bitmap;
import android.opengl.GLUtils;
import android.widget.Toast;            

public class GLRender implements GLSurfaceView.Renderer {
    public Cena2D ui = new Cena2D();
	public Cena3D jogo = new Cena3D();                
    public boolean pronto = false;
    public Objeto2D o;
    public int texturaBranca;            

    public GLRender(GLSurfaceView tela) {              
        tela.setEGLContextClientVersion(3);              
        tela.setRenderer(this);              
        tela.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);              
    }              

    @Override                
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {                
        GLES30.glClearColor(0f, 0f, 0f, 1f);
        GLES30.glEnable(GLES30.GL_DEPTH_TEST);
        GLES30.glEnable(GLES30.GL_CULL_FACE);                
        GLES30.glCullFace(GLES30.GL_BACK);              
        ui.iniciar(1080, 1920);
		jogo.iniciar();
        pronto = true;              
    }                

    @Override                
    public void onSurfaceChanged(GL10 gl, int largura, int altura) {                
        GLES30.glViewport(0, 0, largura, altura);                
        ui.atualizarProjecao(largura, altura);
		jogo.atualizarProjecao(largura, altura);
    }                

    @Override                
    public void onDrawFrame(GL10 gl) {                
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);                
        if(ui != null && jogo != null && pronto) {
			if(jogo.objetos.size() > 0) jogo.render();
			if(ui.objetos.size() > 0) ui.render();
        }
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
            "#version 300 es\n" +                
            "layout(location = 0) in vec2 aPos;\n" +                
            "layout(location = 1) in vec2 aTex;\n" +                
            "uniform mat4 uProjecao;\n" +                
            "out vec2 vTex;\n" +                
            "void main() {\n" +                
            "  gl_Position = uProjecao * vec4(aPos, 0.0, 1.0);\n" +                
            "  vTex = aTex;\n" +                
            "}";                
    }                

    public static String obterFrag2D() {                
        return                
            "#version 300 es\n" +                
            "precision mediump float;\n" +                
            "in vec2 vTex;\n" +                
            "uniform sampler2D uTextura;\n" +                
            "out vec4 fragCor;\n" +                
            "void main() {\n" +                
            "  fragCor = texture(uTextura, vTex);\n" +                
            "}";                
    }
	
	public static String obterVert3D() {
		return "#version 300 es\n" +
			"layout(location = 0) in vec3 aPos;\n" +
			"layout(location = 1) in vec2 aTex;\n" +
			"uniform mat4 uMVP;\n" +
			"out vec2 vTex;\n" +
			"void main() {\n" +
			"  gl_Position = uMVP * vec4(aPos, 1.0);\n" +
			"  vTex = aTex;\n" +
			"}";
	}

	public static String obterFrag3D() {
		return "#version 300 es\n" +
			"precision mediump float;\n" +
			"in vec2 vTex;\n" +
			"uniform sampler2D uTextura;\n" +
			"out vec4 fragCor;\n" +
			"void main() {\n" +
			"  fragCor = texture(uTextura, vTex);\n" +
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
	.allocateDirect(4 * 4 * 4) // 4 vértices * 4 floats * 4 bytes                
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

    public void add(final Objeto2D obj) {              
        objetos.add(obj);              
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
}

class Texturas {
	public static int carregarTexturaAsset(Context ctx, String nomeArquivo) {
		try {
			InputStream is = ctx.getAssets().open(nomeArquivo);
			Bitmap bmp = BitmapFactory.decodeStream(is);
			int[] texID = new int[1];
			GLES30.glGenTextures(1, texID, 0);
			GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texID[0]);

			GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bmp, 0);

			GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
			GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);

			bmp.recycle();
			return texID[0];
		} catch(Exception e) {
			Debug.log("erro ao carregar textura no caminho: " + nomeArquivo + "\n" + e);
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

class Cena3D {
	public ShaderUtils shader;
	public FloatBuffer bufferVertices;
	public int vao, vbo;
	public float[] matrizProj = new float[16];
	public float[] matrizView = new float[16];
	public float[] matrizModel = new float[16];
	public float[] matrizFinal = new float[16];
	public int locMVP, locTex;
	public List<Objeto3D> objetos = new ArrayList<Objeto3D>();

	public void iniciar() {
		shader = new ShaderUtils(ShaderUtils.obterVert3D(), ShaderUtils.obterFrag3D());

		int[] ids = new int[1];
		GLES30.glGenVertexArrays(1, ids, 0);
		vao = ids[0];
		GLES30.glBindVertexArray(vao);

		GLES30.glGenBuffers(1, ids, 0);
		vbo = ids[0];
		GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo);

		bufferVertices = ByteBuffer.allocateDirect(6 * 6 * 5 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();

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
		Matrix.setLookAtM(matrizView, 0, 0, 0, 0, 0, 0, -5, 0, 1, 0);
	}

	public void render() {
		shader.usar();
		GLES30.glBindVertexArray(vao);

		for(Objeto3D o : objetos) {
			Matrix.setIdentityM(matrizModel, 0);
			Matrix.translateM(matrizModel, 0, o.x, o.y, o.z);
			Matrix.scaleM(matrizModel, 0, o.largura, o.altura, o.profundidade);

			Matrix.multiplyMM(matrizFinal, 0, matrizView, 0, matrizModel, 0);
			Matrix.multiplyMM(matrizFinal, 0, matrizProj, 0, matrizFinal, 0);

			GLES30.glUniformMatrix4fv(locMVP, 1, false, matrizFinal, 0);
			GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, o.textura);

			preencherVerticesCube();
			GLES30.glBufferSubData(GLES30.GL_ARRAY_BUFFER, 0, bufferVertices.capacity() * 4, bufferVertices);
			GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, 36);
		}

		GLES30.glBindVertexArray(0);
	}

	public void preencherVerticesCube() {
		float[] v = {
			// frente
			-0.5f, -0.5f, 0.5f, 0, 0,
			0.5f, -0.5f, 0.5f, 1, 0,
			0.5f,  0.5f, 0.5f, 1, 1,
			-0.5f, -0.5f, 0.5f, 0, 0,
			0.5f,  0.5f, 0.5f, 1, 1,
			-0.5f,  0.5f, 0.5f, 0, 1,
			// trás
			-0.5f, -0.5f, -0.5f, 1, 0,
			-0.5f,  0.5f, -0.5f, 1, 1,
			0.5f,  0.5f, -0.5f, 0, 1,
			-0.5f, -0.5f, -0.5f, 1, 0,
			0.5f,  0.5f, -0.5f, 0, 1,
			0.5f, -0.5f, -0.5f, 0, 0,
			// lados
			-0.5f, -0.5f, -0.5f, 0, 0,
			-0.5f, -0.5f,  0.5f, 1, 0,
			-0.5f,  0.5f,  0.5f, 1, 1,
			-0.5f, -0.5f, -0.5f, 0, 0,
			-0.5f,  0.5f,  0.5f, 1, 1,
			-0.5f,  0.5f, -0.5f, 0, 1,
			0.5f, -0.5f, -0.5f, 1, 0,
			0.5f,  0.5f, -0.5f, 1, 1,
			0.5f,  0.5f,  0.5f, 0, 1,
			0.5f, -0.5f, -0.5f, 1, 0,
			0.5f,  0.5f,  0.5f, 0, 1,
			0.5f, -0.5f,  0.5f, 0, 0,
			// topo
			-0.5f, 0.5f, -0.5f, 0, 1,
			-0.5f, 0.5f,  0.5f, 0, 0,
			0.5f, 0.5f,  0.5f, 1, 0,
			-0.5f, 0.5f, -0.5f, 0, 1,
			0.5f, 0.5f,  0.5f, 1, 0,
			0.5f, 0.5f, -0.5f, 1, 1,
			// fundo
			-0.5f, -0.5f, -0.5f, 0, 0,
			0.5f, -0.5f, -0.5f, 1, 0,
			0.5f, -0.5f,  0.5f, 1, 1,
			-0.5f, -0.5f, -0.5f, 0, 0,
			0.5f, -0.5f,  0.5f, 1, 1,
			-0.5f, -0.5f,  0.5f, 0, 1,
		};
		bufferVertices.clear();
		bufferVertices.put(v);
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

class Objeto3D {
	public float x, y, z, largura, altura, profundidade;
	public int textura;

	public Objeto3D(float x, float y, float z, float largura, float altura, float profundidade, int textura) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.largura = largura;
		this.altura = altura;
		this.profundidade = profundidade;
		this.textura = (textura == -1) ? Texturas.texturaBranca() : textura;
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
