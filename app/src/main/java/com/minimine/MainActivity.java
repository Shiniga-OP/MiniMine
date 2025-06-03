package com.minimine;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import com.minimine.engine.Player;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.os.Bundle;
import android.graphics.Paint;
import android.view.View;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.graphics.Color;
import android.graphics.Canvas;
import android.util.DisplayMetrics;
import android.Manifest;
import android.os.Build;
import android.os.Environment;
import android.content.pm.PackageManager;
import android.widget.Toast;
import android.provider.Settings;
import android.net.Uri;
import com.minimine.engine.Mundo;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.nio.FloatBuffer;
import android.opengl.GLES30;
import java.util.Map;
import com.minimine.engine.Bloco;
import java.util.List;
import com.minimine.engine.VBOGrupo;
import com.minimine.engine.ShaderUtils;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.nio.ShortBuffer;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import android.opengl.Matrix;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.Comparator;
import java.util.HashSet;
import android.graphics.Bitmap;
import android.content.res.AssetManager;
import java.io.InputStream;
import android.graphics.BitmapFactory;
import java.io.IOException;
import android.opengl.GLUtils;
import com.minimine.engine.TipoBloco;
import android.os.Looper;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import com.engine.Audio;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.BufferedInputStream;

public class MainActivity extends Activity {
    public GLSurfaceView tela;
    public GLRender render;

	public static double livre = 10.0, total, usado;
	public static String gc;

	public Runtime rt;
	public Handler atualizadorMemoria = new Handler();
	public Runnable tarefaFPS;

	public long tempoAnterior = System.nanoTime();
	public int frames = 0;
	public int fps = 0;

	private int PERMISSAO;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
		
		pedirPermissao();
		
		tela = findViewById(R.id.tela);

		rt = Runtime.getRuntime();
		
		Audio.tocarMusica(this, "musicas/igor.m4a", false);
/*
		tarefaFPS = new Runnable() {
			public void run() {
				livre = rt.freeMemory() / 1048576.0;
				total = rt.totalMemory() / 1048576.0;
				usado = total - livre;
				frames++;
				long agora = System.nanoTime();
				if(agora - tempoAnterior >= 1_000_000_000L) {
					fps = frames;
					frames = 0;
					tempoAnterior = agora;
				}
				atualizadorMemoria.postDelayed(this, 16); // 60 vezes por segundo
			}
		};
		atualizadorMemoria.post(tarefaFPS);
		*/
		iniciar();
	}
	
	public void iniciar() {
        tela.setEGLContextClientVersion(3);

        render = new GLRender(this, tela, 77734, "novo mundo", "normal", "texturas/"+"evolva"+"/");
        tela.setRenderer(render);
        tela.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
	}
	
	public void paraMundo(View v) {
		Intent intent = new Intent(this, InicioActivity.class);
		startActivity(intent);
	}

	public void paraLogs(View v) {

	}
	
	public void pedirPermissao() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if(!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + this.getPackageName()));
                this.startActivityForResult(intent, PERMISSAO);
            }
        } else {
            if(this.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                this.requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSAO);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==PERMISSAO) {
            if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode==PERMISSAO) {
            if(grantResults.length>0&&grantResults[0]==PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "permissão concedida", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "permissão negada", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        tela.onResume();
    }

	@Override
	protected void onDestroy() {
		super.onDestroy();
		atualizadorMemoria.removeCallbacks(tarefaFPS);
		render.limparTexturas();
		render.destruir();
	}
}

class GLRender implements GLSurfaceView.Renderer {
    public Context contexto;
    public final GLSurfaceView tela;
    public float[] projMatriz = new float[16];
    public float[] viewMatriz = new float[16];
    public float[] vpMatriz = new float[16];

    public Player camera = new Player();

    public ExecutorService executor = Executors.newFixedThreadPool(4);
	
	public boolean ativo = true;

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
	public int chunksPorVez = 1;

	// céu:
	public float tempo = 0.40f;
	public float tempoVelo = 0.0008f;

	public int shaderProgramaCeu;
	public int viewLocCeu;
	public int projLocCeu;
	public int tempoLocCeu;
	public int[] vboCeu;
	public int[] vaoCeu;
	public FloatBuffer ceuBuffer;

	public void renderizar() {
		GLES30.glUseProgram(shaderPrograma);
		GLES30.glUniformMatrix4fv(lidarvPMatriz, 1, false, vpMatriz, 0);
		GLES30.glUniform1f(lidarLuzIntensidade, luz);
		GLES30.glUniform3fv(lidarLuzDirecao, 1, luzDirecao, 0);
		// if(mundo.chunksAtivos.size() > 1) return;
		for(Map.Entry<String, Bloco[][][]> entry : mundo.chunksAtivos.entrySet()) {
			final String chave = entry.getKey();

			List<VBOGrupo> grupos = mundo.chunkVBOs.get(chave);
			if(grupos == null) continue;

			for(VBOGrupo grupo : grupos) {
				// textura:
				GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, grupo.texturaId);

				// VBO:
				GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, grupo.vboId);
				GLES30.glVertexAttribPointer(
					lidarPosicao,
					3,
					GLES30.GL_FLOAT,
					false,
					8 * 4,
					0
				);
				GLES30.glEnableVertexAttribArray(lidarPosicao);

				GLES30.glVertexAttribPointer(
					lidarNormal,
					3,
					GLES30.GL_FLOAT,
					false,
					8 * 4,
					3 * 4
				);
				GLES30.glEnableVertexAttribArray(lidarNormal);

				GLES30.glVertexAttribPointer(
					lidarTexCoord,
					2,
					GLES30.GL_FLOAT,
					false,
					8 * 4,
					6 * 4
				);
				GLES30.glEnableVertexAttribArray(lidarTexCoord);

				// IBO:
				GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, grupo.iboId);
				// renderiza:
				GLES30.glDrawElements(
					GLES30.GL_TRIANGLES,
					grupo.vertices,
					GLES30.GL_UNSIGNED_SHORT,
					0
				);

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

		ceuBuffer = ByteBuffer.allocateDirect(verticesCeu.length * 4)
			.order(ByteOrder.nativeOrder())
			.asFloatBuffer();
		ceuBuffer.put(verticesCeu);
		ceuBuffer.position(0);

		vaoCeu = new int[1];
		vboCeu = new int[1];
		GLES30.glGenVertexArrays(1, vaoCeu, 0);
		GLES30.glGenBuffers(1, vboCeu, 0);

		GLES30.glBindVertexArray(vaoCeu[0]);

		GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboCeu[0]);

		GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, verticesCeu.length * 4, ceuBuffer, GLES30.GL_STATIC_DRAW);

		GLES30.glEnableVertexAttribArray(0);
		GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, 3 * 4, 0);
		GLES30.glBindVertexArray(0);
	}

	public void atualizarTempo() {
		if(tempo < 1.1f) {
			tempo += tempoVelo;
		} else {
			tempo = 0.1f;
		}
		luz = 1.0f - Math.abs(tempo - 0.5f) * 1.0f;

		if(tempo >= 0.5f && tempo <= 0.7f) ciclo = "tarde";
		else if(tempo >= 0.8f) ciclo = "noite";
		else if(tempo <= 0.5f) ciclo = "dia";

		GLES30.glUseProgram(shaderProgramaCeu);
		GLES30.glUniformMatrix4fv(viewLocCeu, 1, false, viewMatriz, 0);
		GLES30.glUniformMatrix4fv(projLocCeu, 1, false, projMatriz, 0);
		GLES30.glUniform1f(tempoLocCeu, tempo);

		GLES30.glDepthFunc(GLES30.GL_LEQUAL);
		GLES30.glBindVertexArray(vaoCeu[0]);
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
			for(float[] arr : entry.getValue()) {
				totalVertices += arr.length / 8;
			}

			// cria buffer de vertices
			FloatBuffer vertBuffer = ByteBuffer
				.allocateDirect(totalVertices * 8 * 4)
				.order(ByteOrder.nativeOrder())
				.asFloatBuffer();

			for(float[] dados : entry.getValue()) {
				vertBuffer.put(dados);
			}
			vertBuffer.position(0);

			// gera array de indices de sequencia
			ShortBuffer indiceBuffer = ByteBuffer
				.allocateDirect(totalVertices * 2)
				.order(ByteOrder.nativeOrder())
				.asShortBuffer();
			for(short i = 0; i < totalVertices; i++) {
				indiceBuffer.put(i);
			}
			indiceBuffer.position(0);

			// gera IDs de buffer(VBO e IBO)
			int[] bufferIds = new int[2];
			GLES30.glGenBuffers(2, bufferIds, 0);
			int vboId = bufferIds[0];
			int iboId = bufferIds[1];

			// prenche VBO
			GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboId);
			GLES30.glBufferData(
				GLES30.GL_ARRAY_BUFFER,
				vertBuffer.capacity() * 4,
				vertBuffer,
				GLES30.GL_STATIC_DRAW
			);

			// prenche IBO
			GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, iboId);
			GLES30.glBufferData(
				GLES30.GL_ELEMENT_ARRAY_BUFFER,
				indiceBuffer.capacity() * 2,
				indiceBuffer,
				GLES30.GL_STATIC_DRAW
			);

			// desvincula VBO(IBO fica associado ao VAO)
			GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0);
			GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, 0);

			grupos.add(new VBOGrupo(texturaId, vboId, iboId, totalVertices));
		}
		return grupos;
	}

	public boolean pronto = false;

    public GLRender(Context contexto, GLSurfaceView tela, int seed, String nome, String tipo, String pacoteTex) {
        this.contexto = contexto;
        this.tela = tela;
		this.seed = seed;
		this.nome = nome;
		this.tipo = tipo;
		this.pacoteTex = pacoteTex;
    }

    @Override  
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {  
        GLES30.glClearColor(0.40f, 0.65f, 0.85f, 1.0f);  
        GLES30.glEnable(GLES30.GL_DEPTH_TEST);  

        this.mundo = new Mundo(  
			this.tela, this.seed, this.nome,  
			this.tipo, this.pacoteTex  
		);
		
		this.mundo.RAIO_CARREGAMENTO = 8;
		
        this.carregarShaders(contexto);  
        this.carregarTexturas(contexto);  
		
		crMundo(mundo);
    } 

	@Override  
    public void onDrawFrame(GL10 gl) {  
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);  
		if(ativo) {
			atualizarTempo();  
			atualizarChunks();
			
			if(pronto==true) {
				Matrix.multiplyMM(vpMatriz, 0, projMatriz, 0, viewMatriz, 0);  
				renderizar();  
				atualizarViewMatriz();  
			}  
		}
    }  

	@Override  
    public void onSurfaceChanged(GL10 gl, int h, int v) {  
        GLES30.glViewport(0, 0, h, v);  
        float ratio = (float) h / v;  
        Matrix.perspectiveM(projMatriz, 0, 90, ratio, 0.1f, 400f);  
    }

    public void atualizarChunks() {
		int chunkJogadorX = (int)(camera.posicao[0] / mundo.CHUNK_TAMANHO);
		int chunkJogadorZ = (int)(camera.posicao[2] / mundo.CHUNK_TAMANHO);

		PriorityQueue<ChunkCandidato> fila = new PriorityQueue<ChunkCandidato>(10, new Comparator<ChunkCandidato>() {
				public int compare(ChunkCandidato a, ChunkCandidato b) {
					return Double.compare(a.distancia, b.distancia);
				}
			});

		for (int x = chunkJogadorX - mundo.RAIO_CARREGAMENTO; x <= chunkJogadorX + mundo.RAIO_CARREGAMENTO; x++) {
			for (int z = chunkJogadorZ - mundo.RAIO_CARREGAMENTO; z <= chunkJogadorZ + mundo.RAIO_CARREGAMENTO; z++) {
				String chave = x + "," + z;
				if (!mundo.chunksAtivos.containsKey(chave)) {
					double dist = Math.hypot(x - chunkJogadorX, z - chunkJogadorZ);
					fila.offer(new ChunkCandidato(chave, x, z, dist));
				}
			}
		}
		
		int carregados = 0;
		if(MainActivity.livre >= 10.0 || MainActivity.total <= 30.0) {
			while (!fila.isEmpty() && carregados < chunksPorVez) {
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
		} 
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
		int atlasRows = (int) Math.ceil((double) numTexturas / atlasCols);

		Bitmap tempBitmap = null;
		try {
			AssetManager assetManager = contexto.getAssets();
			InputStream inputStream = assetManager.open(pacoteTex+listaRecursos.get(0));
			tempBitmap = BitmapFactory.decodeStream(inputStream);
			inputStream.close();
		} catch(IOException e) {
			e.printStackTrace();
		}

		int texV = tempBitmap.getWidth();
		int texH = tempBitmap.getHeight();
		tempBitmap.recycle();

		int atlasV = atlasCols * texV;
		int atlasH = atlasRows * texH;
		Bitmap atlasBitmap = Bitmap.createBitmap(atlasV, atlasH, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(atlasBitmap);
		Paint paint = new Paint();
		paint.setFilterBitmap(false);

		for(int i = 0; i < listaRecursos.size(); i++) {
			String recurso = listaRecursos.get(i);
			int col = i % atlasCols;
			int row = i / atlasCols;
			int x = col * texV;
			int y = row * texH;
			Bitmap bitmap = null;
			try {
				AssetManager assetManager = contexto.getAssets();
				InputStream inputStream = assetManager.open(pacoteTex+recurso);
				bitmap = BitmapFactory.decodeStream(inputStream);
				inputStream.close();
			} catch(IOException e) {
				e.printStackTrace();
			}
			canvas.drawBitmap(bitmap, x, y, paint);
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

    public void atualizarViewMatriz() {
        Matrix.setLookAtM(viewMatriz, 0,
                          camera.posicao[0], camera.posicao[1], camera.posicao[2],
                          camera.posicao[0] + camera.foco[0],
                          camera.posicao[1] + camera.foco[1],
                          camera.posicao[2] + camera.foco[2],
                          camera.up[0], camera.up[1], camera.up[2]);
    }
	
	public void svMundo(Mundo mundo) {
		try {
			File pasta = new File(Environment.getExternalStorageDirectory(), "MiniMine");
			if(!pasta.exists()) pasta.mkdirs();

			File arquivo = new File(pasta, mundo.nome+".mini");
			FileOutputStream fos = new FileOutputStream(arquivo);
			salvarMundo(fos, mundo.seed, mundo.chunksAtivos);
			fos.close();
		} catch (IOException e) {
			ativo = false;
		}
	}

	public void crMundo(Mundo mundo) {
		try {
			File arquivo = new File(Environment.getExternalStorageDirectory() + "/MiniMine/"+mundo.nome+".mini");
			if(!arquivo.exists()) {
				pronto = true;
				ativo = false;
				return;
			}

			FileInputStream fis = new FileInputStream(arquivo);
			carregarMundo(fis, mundo);
			fis.close();
		} catch(IOException e) {
			ativo = false;
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
						if(b != null && !b.id.equals("AR")) {
							totalNaoAr++;
						}
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
		dos.writeFloat(camera.posicao[0]);
		dos.writeFloat(camera.posicao[1]);
		dos.writeFloat(camera.posicao[2]);

		dos.writeFloat(camera.yaw);
		dos.writeFloat(camera.tom);

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
		camera.posicao[0] = dis.readFloat();
		camera.posicao[1] = dis.readFloat();
		camera.posicao[2] = dis.readFloat();

		camera.yaw = dis.readFloat();
		camera.tom = dis.readFloat();

		camera.rotacionar(0f, 0f);

		tempo = dis.readFloat();

		mundo.tipo = dis.readUTF();

		dis.close();
	}
}
