package com.minimine.engine;

import android.view.MotionEvent;
import android.content.Context;
import android.opengl.GLSurfaceView;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.opengl.GLES30;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.ArrayList;
import android.graphics.Bitmap;
import android.content.res.AssetManager;
import java.io.IOException;
import java.io.InputStream;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import java.nio.FloatBuffer;
import java.nio.ByteOrder;
import java.nio.ByteBuffer;
import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.egl.EGLConfig;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.Comparator;
import com.minimine.MundoActivity;
import android.opengl.Matrix;
import android.opengl.GLUtils;
import java.nio.ShortBuffer;
import java.io.File;
import android.os.Environment;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.io.DataOutputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.BufferedInputStream;
import android.os.Handler;
import android.os.Looper;
import com.engine.Cena2D;
import com.engine.Botao2D;
import com.engine.Texturas;
import com.engine.Objeto2D;
import android.util.DisplayMetrics;
import android.app.Activity;
import com.engine.GL;
import com.engine.Sistema;
import com.engine.ShaderUtils;
import org.json.JSONObject;
import org.json.JSONArray;
import com.engine.ArmUtils;
import com.engine.Modelador;
import com.engine.VBOGrupo;

public class GLRender implements GLSurfaceView.Renderer {
    public Context ctx;
    public final GLSurfaceView tela;
    public float[] projMatriz = new float[16];
    public float[] viewMatriz = new float[16];
    public float[] vpMatriz = new float[16];
	
    public Player player = new Player();
    public ExecutorService executor = Executors.newFixedThreadPool(2);
	public float pesoConta = 0f;
	// interface
	public Cena2D ui;
	public boolean debug = false, gc = false, gravidade = true, trava = true, UI = true;
	// chunks:
	public Mundo mundo;
	public int seed;
	public String nome, tipo, ciclo;
	public String pacoteTex;
	public int shaderPrograma;
    public int lidarvPMatriz;
    public int lidarPosicao;
    public int lidarTexCoord;
    public int lidarTexturas;
    public int lidarLuzDirecao;
	public int lidarLuzIntensidade;
    public int lidarNormal;
    public float[] luzDirecao = {0.5f, 1.0f, 0.5f};
	public float luz = 0.93f;
	public int chunksPorVez = 2;
	// céu:
	public float tempo = 0.40f;
	public float tempoVelo = 0.00001f;
	public int shaderProgramaCeu;
	public int viewLocCeu;
	public int projLocCeu;
	public int tempoLocCeu;
	public int vboCeu;
	public int vaoCeu;
	public FloatBuffer ceuBuffer;
	// hitbox
	public FloatBuffer hitboxBuffer;
	public int programaHitbox;
	
	public void renderizar() {
		GLES30.glUseProgram(shaderPrograma);
		GLES30.glUniformMatrix4fv(lidarvPMatriz, 1, false, vpMatriz, 0);
		GLES30.glUniform1f(lidarLuzIntensidade, luz);
		GLES30.glUniform3fv(lidarLuzDirecao, 1, luzDirecao, 0);
		
		for(Map.Entry<String, Bloco[][][]> entry : mundo.chunksAtivos.entrySet()) {
			final String chave = entry.getKey();
			if(mundo.chunksAlterados.containsKey(chave) && mundo.chunksAlterados.get(chave)) {
				final Bloco[][][] chunk = entry.getValue();
				executor.submit(new Runnable() {
						public void run() {
							final Map<Integer, List<float[]>> dados = mundo.calculoVBO(chunk);
							tela.queueEvent(new Runnable() {
									public void run() {
										final List<VBOGrupo> grupos = gerarVBO(dados);
										mundo.chunkVBOs.put(chave, grupos);
									}
								});
						}
					});
				mundo.chunksAlterados.put(chave, false);
			}
			List<VBOGrupo> grupos = mundo.chunkVBOs.get(chave);
			if(grupos == null) continue;

			for(VBOGrupo grupo : grupos) {
				// textura:
				GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, grupo.texturaId);
				// VBO:
				GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, grupo.vboId);
				GLES30.glVertexAttribPointer(lidarPosicao, 3, GLES30.GL_FLOAT, false, 8 * 4, 0);
				GLES30.glEnableVertexAttribArray(lidarPosicao);

				GLES30.glVertexAttribPointer(lidarNormal, 3, GLES30.GL_FLOAT, false, 8 * 4, 3 * 4);
				GLES30.glEnableVertexAttribArray(lidarNormal);

				GLES30.glVertexAttribPointer(lidarTexCoord, 2, GLES30.GL_FLOAT, false, 8 * 4, 6 * 4);
				GLES30.glEnableVertexAttribArray(lidarTexCoord);
				// IBO:
				GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, grupo.iboId);
				// renderiza:
				GLES30.glDrawElements(GLES30.GL_TRIANGLES, grupo.vertices, GLES30.GL_UNSIGNED_SHORT, 0);
				// desvincula buffers:
				GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, 0);
				GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0);
			}
		}
	}

	public void carregarShaders(Context contexto) {
		String shaderVertice =
			"uniform mat4 u_VPMatriz;" +
			"uniform vec3 u_direcaoLuz;" +
			"uniform float u_intensidadeLuz;" +
			"attribute vec4 a_Posicao;" +
			"attribute vec3 a_Normal;" +
			"attribute vec2 a_TexCoord;" +
			"varying vec2 v_TexCoord;" +
			"varying float v_intencidadeLuz;" +
			"void main() {" +
			"gl_Position = u_VPMatriz * a_Posicao;" +
			"v_TexCoord = a_TexCoord;" +
			"v_intencidadeLuz = max(dot(normalize(a_Normal), normalize(u_direcaoLuz)), 0.70) * u_intensidadeLuz;" +
			"}";
		String shaderFragmento =
            "precision mediump float;" +
            "varying vec2 v_TexCoord;" +
            "varying float v_intencidadeLuz;" +
            "uniform sampler2D u_Textura;" +
            "void main() {" +
            "vec4 corTextura = texture2D(u_Textura, v_TexCoord);" +
			"if(corTextura.a<0.5) discard;"+
            "gl_FragColor = vec4(corTextura.rgb * v_intencidadeLuz, corTextura.a);" +
            "}";
		shaderPrograma = ShaderUtils.criarPrograma(shaderVertice, shaderFragmento);
		lidarvPMatriz = GLES30.glGetUniformLocation(shaderPrograma, "u_VPMatriz");
		lidarLuzDirecao = GLES30.glGetUniformLocation(shaderPrograma, "u_direcaoLuz");
		lidarPosicao = GLES30.glGetAttribLocation(shaderPrograma, "a_Posicao");
		lidarNormal = GLES30.glGetAttribLocation(shaderPrograma, "a_Normal");
		lidarTexCoord = GLES30.glGetAttribLocation(shaderPrograma, "a_TexCoord");
		lidarTexturas = GLES30.glGetUniformLocation(shaderPrograma, "u_Textura");
		lidarLuzIntensidade = GLES30.glGetUniformLocation(shaderPrograma, "u_intensidadeLuz");
		
		String vertC =
			"#version 300 es\n" +
			"layout (location = 0) in vec3 aPos;\n" +
			"uniform mat4 u_view;\n" +
			"uniform mat4 u_projecao;\n" +
			"out vec3 TexCoords;\n" +
			"void main() {\n" +
			"TexCoords = aPos;\n" +
			"mat4 view = u_view;\n" +
			"view[3][0] = 0.0;\n" +
			"view[3][1] = 0.0;\n" +
			" view[3][2] = 0.0;\n" +
			"vec4 pos = u_projecao * view * vec4(aPos, 1.0);\n" +
			"gl_Position = pos.xyww;\n" +
			"}";
		String fragC =
			"#version 300 es\n" +
			"precision mediump float;\n" +
			"in vec3 TexCoords;\n" +
			"uniform float u_tempo;\n" +
			"out vec4 FragColor;\n" +
			"void main() {\n" +
			"vec3 corDia = vec3(0.53, 0.81, 0.98);\n" +    // dia
			"vec3 corLaranja = vec3(1.0, 0.5, 0.0);\n" +  // por do sol
			"vec3 corNoite = vec3(0.0, 0.0, 0.1);\n" +    // noite
			"float t = mod(u_tempo, 1.0);\n" +
			"vec3 corFinal;\n" +
			"if(t < 0.25){\n" +
			"float f = t / 0.25;\n" +
			"corFinal = mix(corNoite, corLaranja, f);\n" + // noite por do sol
			"} else if(t < 0.5){\n" +
			"float f = (t - 0.25) / 0.25;\n" +
			"corFinal = mix(corLaranja, corDia, f);\n" +   // nascer do sol dia
			"} else if(t < 0.75){\n" +
			"float f = (t - 0.5) / 0.25;\n" +
			"corFinal = mix(corDia, corLaranja, f);\n" +
			"} else {\n" +
			"float f = (t - 0.75) / 0.25;\n" +
			"corFinal = mix(corLaranja, corNoite, f);\n" +
			"}\n" +
			"FragColor = vec4(corFinal, 1.0);\n" +
			"}";
		shaderProgramaCeu = ShaderUtils.criarPrograma(vertC, fragC);
		viewLocCeu = GLES30.glGetUniformLocation(shaderProgramaCeu, "u_view");
		projLocCeu = GLES30.glGetUniformLocation(shaderProgramaCeu, "u_projecao");
		tempoLocCeu = GLES30.glGetUniformLocation(shaderProgramaCeu, "u_tempo");
		
		float[] verticesCeu = {
			// face tras
			-1.0f,  1.0f, -1.0f,
			-1.0f, -1.0f, -1.0f,
			1.0f, -1.0f, -1.0f,
			1.0f, -1.0f, -1.0f,
			1.0f,  1.0f, -1.0f,
			-1.0f,  1.0f, -1.0f,
			// face esquerda
			-1.0f, -1.0f,  1.0f,
			-1.0f, -1.0f, -1.0f,
			-1.0f,  1.0f, -1.0f,
			-1.0f,  1.0f, -1.0f,
			-1.0f,  1.0f,  1.0f,
			-1.0f, -1.0f,  1.0f,
			// face direita
			1.0f, -1.0f, -1.0f,
			1.0f, -1.0f,  1.0f,
			1.0f,  1.0f,  1.0f,
			1.0f,  1.0f,  1.0f,
			1.0f,  1.0f, -1.0f,
			1.0f, -1.0f, -1.0f,
			// face frente
			-1.0f, -1.0f,  1.0f,
			-1.0f,  1.0f,  1.0f,
			1.0f,  1.0f,  1.0f,
			1.0f,  1.0f,  1.0f,
			1.0f, -1.0f,  1.0f,
			-1.0f, -1.0f,  1.0f,
			// face cima
			-1.0f,  1.0f, -1.0f,
			1.0f,  1.0f, -1.0f,
			1.0f,  1.0f,  1.0f,
			1.0f,  1.0f,  1.0f,
			-1.0f,  1.0f,  1.0f,
			-1.0f,  1.0f, -1.0f,
			// face baixo
			-1.0f, -1.0f, -1.0f,
			-1.0f, -1.0f,  1.0f,
			1.0f, -1.0f, -1.0f,
			1.0f, -1.0f, -1.0f,
			-1.0f, -1.0f,  1.0f,
			1.0f, -1.0f,  1.0f
		};
		ceuBuffer = GL.criarFloatBuffer(verticesCeu.length);
		ceuBuffer.put(verticesCeu);
		ceuBuffer.position(0);
		vboCeu = GL.gerarVBO(ceuBuffer);
		vaoCeu = GL.gerarVAO(vboCeu, 3 * 4);
		
		String vertHitboxC =
			"uniform mat4 uMVPMatriz;" +
			"attribute vec3 aPosicao;" +
			"void main() {"+
			"gl_Position = uMVPMatriz * vec4(aPosicao, 1.0);"+
			"}";
		String fragHitboxC =
			"precision mediump float;" +
			"uniform vec4 uCor;" +
			"void main() {"+
			"gl_FragColor = uCor;"+
			"}";
		programaHitbox = ShaderUtils.criarPrograma(vertHitboxC, fragHitboxC);
		GLES30.glLineWidth(10f);
	}
	
	public void renderHitbox() {
		float altura = player.hitbox[0];
		float largura = player.hitbox[1];
		float metadeLargura = largura / 2f;
		float minX = player.pos[0] - metadeLargura;
		float maxX = player.pos[0] + metadeLargura;
		float minY = player.pos[1] - 1.5f;
		float maxY = player.pos[1] + altura;
		float minZ = player.pos[2] - metadeLargura;
		float maxZ = player.pos[2] + metadeLargura;
		
		float[] vertices = {
			minX, minY, minZ,  maxX, minY, minZ,
			maxX, minY, minZ,  maxX, minY, maxZ,
			maxX, minY, maxZ,  minX, minY, maxZ,
			minX, minY, maxZ,  minX, minY, minZ,
			minX, maxY, minZ,  maxX, maxY, minZ,
			maxX, maxY, minZ,  maxX, maxY, maxZ,
			maxX, maxY, maxZ,  minX, maxY, maxZ,
			minX, maxY, maxZ,  minX, maxY, minZ,
			minX, minY, minZ,  minX, maxY, minZ,
			maxX, minY, minZ,  maxX, maxY, minZ,
			maxX, minY, maxZ,  maxX, maxY, maxZ,
			minX, minY, maxZ,  minX, maxY, maxZ
		};
		if(hitboxBuffer==null || hitboxBuffer.capacity() < vertices.length) {
			hitboxBuffer = ByteBuffer.allocateDirect(72 * 4)
				.order(ByteOrder.nativeOrder())
				.asFloatBuffer();
		}
		hitboxBuffer.clear();
		hitboxBuffer.put(vertices);
		hitboxBuffer.position(0);

		GLES30.glUseProgram(programaHitbox);
		int mvpLidar = GLES30.glGetUniformLocation(programaHitbox, "uMVPMatriz");
		int corLidar = GLES30.glGetUniformLocation(programaHitbox, "uCor");
		int posLidar = GLES30.glGetAttribLocation(programaHitbox, "aPosicao");
		GLES30.glUniformMatrix4fv(mvpLidar, 1, false, vpMatriz, 0);
		GLES30.glUniform4f(corLidar, 1f, 0f, 0f, 1f);
		GLES30.glEnableVertexAttribArray(posLidar);
		GLES30.glVertexAttribPointer(posLidar, 3, GLES30.GL_FLOAT, false, 3 * 4, hitboxBuffer);
		GLES30.glDrawArrays(GLES30.GL_LINES, 0, vertices.length / 3);
		GLES30.glDisableVertexAttribArray(posLidar);
	}
	
	public void renderizarMobs() {
		GLES30.glUseProgram(shaderPrograma);
		GLES30.glUniform1f(lidarLuzIntensidade, luz);
		GLES30.glUniform3fv(lidarLuzDirecao, 1, luzDirecao, 0);

		for(Mob mob : mundo.entidades) {
			if(mob == player) continue;
			// matriz modelo * view * projeção
			float[] modeloMatriz = new float[16];
			float[] mvpMatriz = new float[16];

			Matrix.setIdentityM(modeloMatriz, 0);
			Matrix.translateM(modeloMatriz, 0, mob.pos[0], mob.pos[1], mob.pos[2]);
			Matrix.scaleM(modeloMatriz, 0, 0.06f, 0.06f, 0.06f);

			Matrix.multiplyMM(mvpMatriz, 0, vpMatriz, 0, modeloMatriz, 0);
			GLES30.glUniformMatrix4fv(lidarvPMatriz, 1, false, mvpMatriz, 0);

			GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, mob.texturaId);
			GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, mob.modelo.vboId);
			GLES30.glVertexAttribPointer(lidarPosicao, 3, GLES30.GL_FLOAT, false, 8 * 4, 0);
			GLES30.glEnableVertexAttribArray(lidarPosicao);

			GLES30.glVertexAttribPointer(lidarNormal, 3, GLES30.GL_FLOAT, false, 8 * 4, 3 * 4);
			GLES30.glEnableVertexAttribArray(lidarNormal);

			GLES30.glVertexAttribPointer(lidarTexCoord, 2, GLES30.GL_FLOAT, false, 8 * 4, 6 * 4);
			GLES30.glEnableVertexAttribArray(lidarTexCoord);

			GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, mob.modelo.iboId);
			GLES30.glDrawElements(GLES30.GL_TRIANGLES, mob.modelo.vertices, GLES30.GL_UNSIGNED_SHORT, 0);

			GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, 0);
			GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0);
		}
	}
	
	public void atualizarTempo() {
		if(tempo < 1.1f) tempo += tempoVelo;
		else tempo = 0.1f;
		luz = 1.0f - Math.abs(tempo - 0.5f) * 1.0f;
		
		if(tempo >= 0.5f && tempo <= 0.7f) ciclo = "tarde";
		else if(tempo >= 0.8f) ciclo = "noite";
		else if(tempo <= 0.5f) ciclo = "dia";
		
		GLES30.glUseProgram(shaderProgramaCeu);
		GLES30.glUniformMatrix4fv(viewLocCeu, 1, false, viewMatriz, 0);
		GLES30.glUniformMatrix4fv(projLocCeu, 1, false, projMatriz, 0);
		GLES30.glUniform1f(tempoLocCeu, tempo);
		
		GLES30.glDepthFunc(GLES30.GL_LEQUAL);
		GLES30.glBindVertexArray(vaoCeu);
		GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, 36);
		GLES30.glBindVertexArray(0);
		GLES30.glDepthFunc(GLES30.GL_LESS);
	}

	public void limparTexturas() {
		if(mundo.atlasTexturaId != -1) {
			GLES30.glDeleteTextures(1, new int[] { mundo.atlasTexturaId }, 0);
			mundo.atlasTexturaId = -1;
		}
		mundo.atlasUVMapa.clear();
	}
	
	public List<VBOGrupo> gerarVBO(Map<Integer, List<float[]>> dadosPorTextura) {
		List<VBOGrupo> grupos = new ArrayList<>();
		for(Map.Entry<Integer, List<float[]>> entry : dadosPorTextura.entrySet()) {
			int texturaId = entry.getKey();
			//  calcula total de vertices em todas as listas float[]
			int totalVertices = 0;
			for(float[] arr : entry.getValue()) totalVertices += arr.length / 8;
			// cria buffer de vertices
			FloatBuffer vertBuffer = GL.criarFloatBuffer(totalVertices*8);

			for(float[] dados : entry.getValue()) vertBuffer.put(dados);
			vertBuffer.position(0);
			// gera array de indices de sequencia
			ShortBuffer indiceBuffer = GL.criarShortBuffer(totalVertices);
			for(short i = 0; i < totalVertices; i++) indiceBuffer.put(i);
			indiceBuffer.position(0);
			// gera IDs de buffer(VBO e IBO)
			int vboId = GL.gerarVBO(vertBuffer);
			int iboId = GL.gerarIBO(indiceBuffer);
			// desvincula VBO(IBO fica associado ao VAO)
			GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0);
			GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, 0);
			grupos.add(new VBOGrupo(texturaId, vboId, iboId, totalVertices));
		}
		return grupos;
	}
	
	public boolean pronto = false;
	
    public GLRender(Context ctx, GLSurfaceView tela, int seed, String nome, String tipo, String pacoteTex) {
        this.ctx = ctx;
        this.tela = tela;
		this.seed = seed;
		this.nome = nome;
		this.tipo = tipo;
		this.pacoteTex = pacoteTex;
    }
	
	public Botao2D[] botoes = new Botao2D[10];
	
	public void defBotao(int i, float x, float y) {
		try {
			botoes[i].objeto.x = x;
			botoes[i].objeto.y = y;
		} catch(Exception e) {
			System.out.println("erro: "+e);
		}
	}
	public void defMobPos(int i, float x, float y, float z) {
		try {
			mundo.entidades.get(i).pos[0] = x;
			mundo.entidades.get(i).pos[1] = y;
			mundo.entidades.get(i).pos[2] = z;
		} catch(Exception e) {
			System.out.println("erro: "+e);
		}
	}
	public void carregarUI(Context ctx) {      
		// mira:      
		mira = new Objeto2D(0, 0, 50, 50, Texturas.carregarAsset(ctx, "texturas/evolva/ui/mira.png"));      
		// movimentacao      
		botoes[0] = new Botao2D(new Objeto2D(0, 0, botoesTam, botoesTam, Texturas.carregarAsset(ctx, "texturas/evolva/ui/botao_d.png")));
		botoes[0].definirAcao(new Runnable() {
			public void run() { moverDireita(); }
		});
		botoes[1] = new Botao2D(new Objeto2D(0, 0, botoesTam, botoesTam, Texturas.carregarAsset(ctx, "texturas/evolva/ui/botao_e.png")));
		botoes[1].definirAcao(new Runnable() {
			public void run() { moverEsquerda(); }
		});
		botoes[2] = new Botao2D(new Objeto2D(0, 0, botoesTam, botoesTam, Texturas.carregarAsset(ctx, "texturas/evolva/ui/botao_t.png")));
		botoes[2].definirAcao(new Runnable() {
			public void run() { moverTras(); }
		});
		botoes[3] = new Botao2D(new Objeto2D(0, 0, botoesTam, botoesTam, Texturas.carregarAsset(ctx, "texturas/evolva/ui/botao_f.png")));
		botoes[3].definirAcao(new Runnable() {
			public void run() { moverFrente(); }
		});
		botoes[4] = new Botao2D(new Objeto2D(0, 0, botoesTam, botoesTam, Texturas.carregarAsset(ctx, "texturas/evolva/ui/botao_f.png")));      
		botoes[4].definirAcao(new Runnable() {        
			public void run() { pular(); }        
		});        
		botoes[5] = new Botao2D(new Objeto2D(0, 0, botoesTam, botoesTam, Texturas.carregarAsset(ctx, "texturas/evolva/ui/clique.png")));      
		botoes[5].definirAcao(new Runnable() {
			public void run() { colocarBloco(); }        
		});        
		// slots      
		botoes[6] = new Botao2D(new Objeto2D(0, 0, 100, 100, Texturas.texturaBranca()));
		botoes[6].definirAcao(new Runnable() {        
			public void run() { player.itemMao = "AR"; }        
		});      
		botoes[7] = new Botao2D(new Objeto2D(0, 0, 100, 100, Texturas.texturaCor(0.5f, 1f, 0.9f, 1f)));
		botoes[7].definirAcao(new Runnable() {        
			public void run() { player.itemMao = "PEDREGULHO"; }        
		});      
		botoes[8] = new Botao2D(new Objeto2D(0, 0, 100, 100, Texturas.texturaCor(1f, 0.5f, 0.2f, 1f)));        
		botoes[8].definirAcao(new Runnable() {        
			public void run() { player.itemMao = "TABUAS_CARVALHO"; }        
		});      
		botoes[9] = new Botao2D(new Objeto2D(0, 0, 100, 100, Texturas.texturaCor(0.8f, 0.3f, 1f, 1f)));        
		botoes[9].definirAcao(new Runnable() {        
			public void run() { player.itemMao = "TRONCO_CARVALHO"; }        
		});
	}
	
	public Objeto2D mira;
	public int botoesTam = 150;

    @Override  
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {  
        GLES30.glClearColor(0.40f, 0.65f, 0.85f, 1.0f);  
        GLES30.glEnable(GLES30.GL_DEPTH_TEST);  
		GLES30.glEnable(GLES30.GL_BLEND);

        this.mundo = new Mundo(this.seed, this.nome, this.tipo, this.pacoteTex);  
		crMundo(this.mundo);  
		
        this.carregarShaders(ctx);  
        this.carregarTexturas(ctx);
		int texId = Texturas.carregarAsset(ctx, "texturas/teste.png");
		VBOGrupo modelo = Modelador.carregarModelo(ArmUtils.lerTextoAssets(ctx, "modelos/teste.json"), texId);
		mundo.entidades.add(new Mob(5, 120, 5, modelo, texId));
		if(UI) {
			this.carregarUI(ctx);
			ui = new Cena2D();
			ui.iniciar();
			ui.add(mira);
			ui.add(botoes);
		}	
		rt = Runtime.getRuntime();
    } 
	
	public Runtime rt;
	public double livre, total, usado;
	
	@Override  
    public void onDrawFrame(GL10 gl) {  
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);
		
		livre = rt.freeMemory() / 1048576.0;
		total = rt.totalMemory() / 1048576.0;
		usado = total - livre;
		
		atualizarTempo();  
		atualizarChunks();

		if(pronto==true) {
			atualizarGravidade(player);
			player.atualizar();
			for(int i = 0; i < mundo.entidades.size(); i++) {
				Player mob = mundo.entidades.get(i);
				mob.atualizar();
				atualizarGravidade(mob);
			}
			Matrix.multiplyMM(vpMatriz, 0, projMatriz, 0, viewMatriz, 0);  
			if(UI) ui.render();
			renderizar();  
			renderizarMobs();
			atualizarViewMatriz();  
			if(mundo.noChao(player) || mundo.chunksAtivos.size() < 4) {  
				player.noAr = false;  
				pesoConta = 0.1f;  
			} else player.noAr = true;  
		}  
		if(gc == true)  ativarGC();  
		if(debug == true) renderHitbox();  
    }  

	@Override  
    public void onSurfaceChanged(GL10 gl, int h, int v) {  
        GLES30.glViewport(0, 0, h, v);  
        float ratio = (float) h / v;  
        Matrix.perspectiveM(projMatriz, 0, 90, ratio, 0.1f, 400f);  
		if(UI) {
			ui.atualizarProjecao(h, v);  
			mira.y = v / 2 - mira.altura / 2;
			mira.x = h / 2 - mira.largura / 2;
		}
		if(v > h) {
			// movimento:
			defBotao(0, 350, 1200);
			defBotao(1, 50, 1200);
			defBotao(2, 200, 1350);
			defBotao(3, 200, 1050);
			// acoes:
			defBotao(4, 900, 1200);
			defBotao(5, 900, 900);
			// slots:
			defBotao(6, 500, 1400);
			defBotao(7, 620, 1400);
			defBotao(8, 740, 1400);
			defBotao(9, 855, 1400);
		} else {
			
		}
    }
	
	public void atualizarGravidade(Player camera) {
		if(camera.noAr == false) return;
		if(!gravidade || camera.peso == 0) return;
		// aplica gravidade
		camera.velocidadeY += camera.GRAVIDADE;
		if(camera.velocidadeY < camera.velocidadeY_limite)
			camera.velocidadeY = camera.velocidadeY_limite;
		float novaY = camera.pos[1] + camera.velocidadeY;
		// verifica colisão
		camera.pos = mundo.verificarColisao(camera, camera.pos[0], novaY, camera.pos[2]);
		boolean bateuChao =camera. pos[1] > novaY;
		// atualiza o esytado do jogador
		if(bateuChao) {
			camera.velocidadeY = 0;
			camera.noAr = false;
		} else camera.noAr = true;
	}

    public void atualizarChunks() {
		int chunkJogadorX = (int)(player.pos[0] / mundo.CHUNK_TAMANHO);
		int chunkJogadorZ = (int)(player.pos[2] / mundo.CHUNK_TAMANHO);

		Iterator<Map.Entry<String, Bloco[][][]>> it = mundo.chunksAtivos.entrySet().iterator();
		while(it.hasNext()) {
			String chave = it.next().getKey();
			int sep = chave.indexOf(',');
			int cx = Integer.parseInt(chave.substring(0, sep));
			int cz = Integer.parseInt(chave.substring(sep + 1));
			if(Math.abs(cx - chunkJogadorX) > mundo.RAIO_CARREGAMENTO || Math.abs(cz - chunkJogadorZ) > mundo.RAIO_CARREGAMENTO) {
				it.remove();
				if(!mundo.chunksModificados.containsKey(chave)) mundo.chunksCarregados.remove(chave);
				List<VBOGrupo> grupos = mundo.chunkVBOs.remove(chave);
				if(grupos != null) for(VBOGrupo g : grupos) {
						GLES30.glDeleteBuffers(1, new int[]{ g.vboId }, 0);
						GLES30.glDeleteBuffers(1, new int[]{ g.iboId }, 0);
						g = null;
					}
			}
		}
		PriorityQueue<ChunkCandidato> fila = new PriorityQueue<ChunkCandidato>(10, new Comparator<ChunkCandidato>() {
				public int compare(ChunkCandidato a, ChunkCandidato b) {
					return Double.compare(a.distancia, b.distancia);
				}
			});

		for(int x = chunkJogadorX - mundo.RAIO_CARREGAMENTO; x <= chunkJogadorX + mundo.RAIO_CARREGAMENTO; x++) {
			for(int z = chunkJogadorZ - mundo.RAIO_CARREGAMENTO; z <= chunkJogadorZ + mundo.RAIO_CARREGAMENTO; z++) {
				String chave = x + "," + z;
				if(!mundo.chunksAtivos.containsKey(chave)) {
					double dist = Math.hypot(x - chunkJogadorX, z - chunkJogadorZ);
					fila.offer(new ChunkCandidato(chave, x, z, dist));
				}
			}
		}
		int carregados = 0;
		if(livre >= 10.0 || !trava || total <= 30.0) {
			while(!fila.isEmpty() && carregados < chunksPorVez) {
				final ChunkCandidato c = fila.poll();
				final String chave = c.chave;
				final Bloco[][][] chunk = mundo.carregarChunk(c.x, c.z);
				mundo.chunksAtivos.put(chave, chunk);
				executor.submit(new Runnable() {
						public void run() {
							final Map<Integer, List<float[]>> dados = mundo.calculoVBO(chunk);
							tela.queueEvent(new Runnable() {
									public void run() {
										mundo.chunkVBOs.put(chave, gerarVBO(dados));
									}
								});
						}
					});
				carregados++;
			}
			if(!pronto) pronto = true;
		} else ativarGC();
	}
	
	public static void ativarGC() {
		System.gc();
		Runtime.getRuntime().runFinalization();
	}

	private static class ChunkCandidato {
		String chave;
		int x, z;
		double distancia;
		ChunkCandidato(String chave, int x, int z, double distancia) {
			this.chave = chave;
			this.x = x;
			this.z = z;
			this.distancia = distancia;
		}
	}
	
	public void destruir() {
		for(List<VBOGrupo> grupos : mundo.chunkVBOs.values()) {
			for(VBOGrupo grupo : grupos) {
				GLES30.glDeleteBuffers(1, new int[]{grupo.vboId}, 0);
				GLES30.glDeleteBuffers(1, new int[]{grupo.iboId}, 0);
				GLES30.glDeleteTextures(1, new int[]{grupo.texturaId}, 0);
			}
		}
		executor.shutdown();
	}
	
	public void carregarTexturas(Context contexto) {
		HashSet<String> recursosUnicos = new HashSet<>();
		for(String[] tipo : TipoBloco.tipos.values()) {
			// "0" = textura vazia
			if(tipo[0].equals("0"))
				continue;
			for(int face = 0; face < 6; face++) {
				String recurso = tipo[face];
				if(!recurso.equals("-1"))
					recursosUnicos.add(recurso);
			}
		}
		ArrayList<String> listaRecursos = new ArrayList<>(recursosUnicos);
		int numTexturas = listaRecursos.size();
		int atlasCols = (int) Math.ceil(Math.sqrt(numTexturas));
		int atlasLinhas = (int) Math.ceil((double) numTexturas / atlasCols);

		Bitmap tempBitmap = null;
		try {
			AssetManager ctxAssets = contexto.getAssets();
			InputStream tex = ctxAssets.open(pacoteTex+listaRecursos.get(0));
			tempBitmap = BitmapFactory.decodeStream(tex);
			tex.close();
		} catch(IOException e) {
			e.printStackTrace();
		}

		int texV = tempBitmap.getWidth();
		int texH = tempBitmap.getHeight();
		tempBitmap.recycle();

		int atlasV = atlasCols * texV;
		int atlasH = atlasLinhas * texH;
		Bitmap atlasBitmap = Bitmap.createBitmap(atlasV, atlasH, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(atlasBitmap);
		Paint pincel = new Paint();
		pincel.setFilterBitmap(false);

		for(int i = 0; i < listaRecursos.size(); i++) {
			String recurso = listaRecursos.get(i);
			int col = i % atlasCols;
			int linha = i / atlasCols;
			int x = col * texV;
			int y = linha * texH;
			Bitmap bitmap = ArmUtils.lerImgAssets(ctx, pacoteTex+recurso);
			canvas.drawBitmap(bitmap, x, y, pincel);
			bitmap.recycle();

			float u_min = (float) x / atlasV;
			float v_min = (float) y / atlasH;
			float u_max = (float) (x + texV) / atlasV;
			float v_max = (float) (y + texH) / atlasH;
			mundo.atlasUVMapa.put(recurso, new float[]{u_min, v_min, u_max, v_max});
		}
		int[] texturaIds = new int[1];
		GLES30.glGenTextures(1, texturaIds, 0);
		mundo.atlasTexturaId = texturaIds[0];
		GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, mundo.atlasTexturaId);
		GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST);
		GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_NEAREST);
		GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, atlasBitmap, 0);
		atlasBitmap.recycle();
		GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0);
	}
	
	public void colocarBloco() {
		float[] pos = player.camera.pos;

		float yaw = player.camera.yaw;
		float tom = player.camera.tom;

		float dx = (float) (Math.cos(Math.toRadians(tom)) * Math.cos(Math.toRadians(yaw)));
		float dy = (float) Math.sin(Math.toRadians(tom));
		float dz = (float) (Math.cos(Math.toRadians(tom)) * Math.sin(Math.toRadians(yaw)));

		float maxDist = player.alcance;

		int mapaX = (int) Math.floor(pos[0]);
		int mapaY = (int) Math.floor(pos[1]);
		int mapaZ = (int) Math.floor(pos[2]);

		float deltaDistX = (dx==0) ? Float.MAX_VALUE : Math.abs(1 / dx);
		float deltaDistY = (dy==0) ? Float.MAX_VALUE : Math.abs(1 / dy);
		float deltaDistZ = (dz==0) ? Float.MAX_VALUE : Math.abs(1 / dz);

		int passoX = dx < 0 ? -1 : 1;
		int passoY = dy < 0 ? -1 : 1;
		int passoZ = dz < 0 ? -1 : 1;

		float ladoDistX = (dx < 0) ? (pos[0] - mapaX) * deltaDistX : (mapaX + 1.0f - pos[0]) * deltaDistX;
		float ladoDistY = (dy < 0) ? (pos[1] - mapaY) * deltaDistY : (mapaY + 1.0f - pos[1]) * deltaDistY;
		float ladoDistZ = (dz < 0) ? (pos[2] - mapaZ) * deltaDistZ : (mapaZ + 1.0f - pos[2]) * deltaDistZ;

		float distanciaPercorrida = 0.0f;
		int hitAxis = -1;

		while(distanciaPercorrida < maxDist) {
			if(ladoDistX <= ladoDistY && ladoDistX <= ladoDistZ) {
				mapaX += passoX;
				distanciaPercorrida = ladoDistX;
				ladoDistX += deltaDistX;
				hitAxis = 0;
			} else if(ladoDistY <= ladoDistZ) {
				mapaY += passoY;
				distanciaPercorrida = ladoDistY;
				ladoDistY += deltaDistY;
				hitAxis = 1;
			} else {
				mapaZ += passoZ;
				distanciaPercorrida = ladoDistZ;
				ladoDistZ += deltaDistZ;
				hitAxis = 2;
			}
			if(mundo.eBlocoSolido(mapaX, mapaY, mapaZ)) {
				if(!player.itemMao.equals("AR")) {
					int blocoX = mapaX + (hitAxis == 0 ? -passoX : 0);
					int blocoY = mapaY + (hitAxis == 1 ? -passoY : 0);
					int blocoZ = mapaZ + (hitAxis == 2 ? -passoZ : 0);
					mundo.colocarBloco(blocoX, blocoY, blocoZ, player);
				} else mundo.destruirBloco(mapaX, mapaY, mapaZ, player);
				return;
			}
		}
	}
	
	public void moverFrente() { mover(player.camera.foco[0], player.camera.foco[2]); }
	public void moverTras() { mover(-player.camera.foco[0], -player.camera.foco[2]); }
	public void moverDireita() { mover(-player.camera.foco[2], player.camera.foco[0]); }
	public void moverEsquerda() { mover(player.camera.foco[2], -player.camera.foco[0]); }

	public void mover(float dirX, float dirZ) {
		float magSq = dirX * dirX + dirZ * dirZ;
		if(magSq <= 0.0001f) return;

		float invMag = (float) (1.0 / Math.sqrt(magSq));
		dirX *= invMag;
		dirZ *= invMag;

		float velocidade = player.velocidadeX;
		float[] pos = player.pos;

		float altura = player.hitbox[0];
		float raio = player.hitbox[1];
		float[] novaPos = {pos[0], pos[1], pos[2]};

		for(int iteracao = 0; iteracao < 3; iteracao++) {
			float[] movimento = {
				dirX * velocidade/2,
				0,
				dirZ * velocidade/2
			};

			for(int eixo = 0; eixo < 3; eixo++) {
				if(movimento[eixo] == 0) continue;

				float[] testePos = novaPos.clone();
				testePos[eixo] += movimento[eixo];

				if(!mundo.colidiria(testePos[0], testePos[1], testePos[2], altura, raio)) novaPos[eixo] = testePos[eixo];
			}
		}
		player.pos = novaPos;
	}
	
	public void pular() {
		if(!player.noAr && gravidade && player.peso > 0) {
			player.velocidadeY = player.salto;
			player.noAr = true;
		}
	}

    public void atualizarViewMatriz() {
        Matrix.setLookAtM(viewMatriz, 0,
		player.camera.pos[0], player.camera.pos[1], player.camera.pos[2],
		player.camera.pos[0] + player.camera.foco[0],
		player.camera.pos[1] + player.camera.foco[1],
		player.camera.pos[2] + player.camera.foco[2],
		player.camera.up[0], player.camera.up[1], player.camera.up[2]);
    }
	
	public void svMundo(Mundo mundo) {
		try {
			File pasta = new File(Environment.getExternalStorageDirectory(), "MiniMine");
			if(!pasta.exists()) pasta.mkdirs();

			File arquivo = new File(pasta, mundo.nome+".mini");
			FileOutputStream fos = new FileOutputStream(arquivo);
			salvarMundo(fos, mundo.seed, mundo.chunksModificados);
			fos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void crMundo(Mundo mundo) {
		try {
			File arquivo = new File(Environment.getExternalStorageDirectory() + "/MiniMine/"+mundo.nome+".mini");
			if(!arquivo.exists()) {
				pronto = true;
				return;
			}
			FileInputStream fis = new FileInputStream(arquivo);
			carregarMundo(fis, mundo);
			fis.close();
		} catch(IOException e) {
			e.printStackTrace();
		}
	}

	public void salvarMundo(OutputStream out, int seed, Map<String, Bloco[][][]> chunksCarregados) throws IOException {
		DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(out));
		// seed
		dos.writeInt(seed);
		// quantos chunks salvos
		dos.writeInt(chunksCarregados.size());

		for(Map.Entry<String, Bloco[][][]> entry : chunksCarregados.entrySet()) {
			String chave = entry.getKey(); // chave da chunk
			Bloco[][][] chunk = entry.getValue();
			int cx = chunk.length;
			int cy = chunk[0].length;
			int cz = chunk[0][0].length;
			// salva a chave da chunk
			dos.writeUTF(chave);
			// escreve o tamanho da chunk
			dos.writeInt(cx);
			dos.writeInt(cy);
			dos.writeInt(cz);
			// conta quantos bloxos tem na chunk
			int totalNaoAr = 0;
			for(int x = 0; x < cx; x++) {
				for(int y = 0; y < cy; y++) {
					for(int z = 0; z < cz; z++) {
						Bloco b = chunk[x][y][z];
						if(b != null && !b.id.equals("AR")) totalNaoAr++;
					}
				}
			}
			dos.writeInt(totalNaoAr);
			// salva os dados dos blocos
			for(int x = 0; x < cx; x++) {
				for(int y = 0; y < cy; y++) {
					for(int z = 0; z < cz; z++) {
						Bloco b = chunk[x][y][z];
						if(b != null && !b.id.equals("AR")) {
							dos.writeInt(x);
							dos.writeInt(y);
							dos.writeInt(z);
							dos.writeUTF(b.id);
						}
					}
				}
			}
		}
		dos.writeFloat(player.camera.pos[0]);
		dos.writeFloat(player.camera.pos[1]);
		dos.writeFloat(player.camera.pos[2]);
		
		dos.writeFloat(player.camera.yaw);
		dos.writeFloat(player.camera.tom);
		
		dos.writeFloat(tempo);
		dos.writeUTF(mundo.tipo);
		
		dos.flush();
		dos.close();
	}

	public void carregarMundo(InputStream in, Mundo mundo) throws IOException {
		DataInputStream dis = new DataInputStream(new BufferedInputStream(in));
		// bota a seed
		mundo.seed = dis.readInt();
		// quantidade ds chumks
		int totalChunks = dis.readInt();

		for(int i = 0; i < totalChunks; i++) {
			String chave = dis.readUTF();
			int cx = dis.readInt();
			int cy = dis.readInt();
			int cz = dis.readInt();
			Bloco[][][] chunk = new Bloco[cx][cy][cz];
			// eztraindo a chave da xhunk
			String[] partesChave = chave.split(",");
			int chunkX = Integer.parseInt(partesChave[0]);
			int chunkZ = Integer.parseInt(partesChave[1]);
			// constroi a chunk
			for(int x = 0; x < cx; x++) {
				for(int y = 0; y < cy; y++) {
					for(int z = 0; z < cz; z++) {
						int globalX = chunkX * mundo.CHUNK_TAMANHO + x;
						int globalZ = chunkZ * mundo.CHUNK_TAMANHO + z;
						chunk[x][y][z] = new Bloco(globalX, y, globalZ, "AR");
					}
				}
			}
			int totalNaoAr = dis.readInt();
			for(int k = 0; k < totalNaoAr; k++) {
				int x = dis.readInt();
				int y = dis.readInt();
				int z = dis.readInt();
				String id = dis.readUTF();
				// convertendo pra coordenadas globais
				int globalX = chunkX * mundo.CHUNK_TAMANHO + x;
				int globalZ = chunkZ * mundo.CHUNK_TAMANHO + z;
				Bloco b = new Bloco(globalX, y, globalZ, id);
				chunk[x][y][z] = b;
			}
			mundo.chunksModificados.put(chave, chunk);
			mundo.chunksCarregados.put(chave, chunk);
		}
		player.pos[0] = dis.readFloat();
		player.pos[1] = dis.readFloat();
		player.pos[2] = dis.readFloat();
		
		player.camera.yaw = dis.readFloat();
		player.camera.tom = dis.readFloat();
		
		player.camera.rotacionar(0f, 0f);
		tempo = dis.readFloat();	
		mundo.tipo = dis.readUTF();
		
		dis.close();
	}
}
class Mob extends Player {
		public VBOGrupo modelo;
		public int texturaId;

		public Mob(float x, float y, float z, VBOGrupo modelo, int texturaId) {
			this.modelo = modelo;
			this.texturaId = texturaId;
			this.pos[0] = x;
			this.pos[1] = y;
			this.pos[2] = z;
			this.peso = 1f;
			this.velocidadeX = 0.09f;
			this.hitbox[0] = 1.7f;
			this.hitbox[1] = 0.8f;
		}
	}
