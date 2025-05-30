package com.minimine;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.os.Bundle;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;
import org.luaj.vm2.lib.jse.JsePlatform;
import java.util.HashSet;
import java.util.Set;
import java.nio.ShortBuffer;
import org.json.JSONObject;
import org.json.JSONException;
import org.json.JSONArray;
import java.io.IOException;
import android.os.Environment;
import java.io.File;
import java.io.FileInputStream;
import android.media.MediaPlayer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Random;
import java.io.BufferedReader;
import java.io.FileReader;
import android.content.res.AssetManager;
import java.util.PriorityQueue;

public class MundoActivity extends Activity {
    private GLSurfaceView tela;
    public GLRender render;

    private TextView coordenadas;
    public EditText chat;

    public EditText console;
    public Logs log;

	public Comandos comandos;

	public DPadView dpad;
	private Handler responsavel2 = new Handler();
	private Runnable movimentoTarefa;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_jogo);
        coordenadas = findViewById(R.id.coordenadas);
        chat = findViewById(R.id.chat);

        console = findViewById(R.id.logs);

        log = new Logs();
        log.capturar();
		
        tela = findViewById(R.id.tela);
		
		Intent dados = getIntent();
		int seed = dados.getIntExtra("seed", 3);
		String tipoMundo = dados.getStringExtra("tipoMundo");
		String pacoteTex = dados.getStringExtra("pacoteTex");
		
		console.setText(String.valueOf(seed));
        tela.setEGLContextClientVersion(3);

        render = new GLRender(this, tela, seed, tipoMundo, "texturas/"+pacoteTex+"/");
        tela.setRenderer(render);
        tela.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        render.camera.mover(0.5f);
		
		comandos =  new Comandos(render, chat);

		dpad = findViewById(R.id.dpad);
		
		dpad.definirDPadAgenda(new DPadView.DPadAgenda() {
				@Override
				public void quandoDirecaoPressio(int direcao) {
					responsavel2.removeCallbacks(movimentoTarefa);
					movimentoTarefa = new MovimentoTarefa(direcao);
					responsavel2.post(movimentoTarefa);
				}

				@Override
				public void quandoDirecaoAtivada(int direcao) {
					responsavel2.removeCallbacks(movimentoTarefa);
				}
			});
	}

	private class MovimentoTarefa implements Runnable {
		private final int direcao;

		public MovimentoTarefa(int direcao) {
			this.direcao = direcao;
		}

		@Override
		public void run() {
			coordenadas.setText("X: "+Math.round(render.camera.posicao[0])+" Y: "+Math.round(render.camera.posicao[1])+" Z: "+Math.round(render.camera.posicao[2]));
			Globals globals = JsePlatform.standardGlobals();
			
			LuaValue luaComandos = CoerceJavaToLua.coerce(comandos);
			LuaValue luaRender = CoerceJavaToLua.coerce(render);
			
			globals.set("render", luaRender);
			globals.set("comandos", luaComandos);
			
			try {
				if(chat.getText().toString().startsWith("/")) comandos.executar(chat.getText().toString());
				else globals.load(chat.getText().toString(), "script").call();
				chat.setText("");
				console.setText(log.exibir());
			} catch(Exception e) {
				console.setText("erro: "+e);
			}
			
			// movimento puro sem diagonal
			if((direcao & DPadView.DIR_CIMA) != 0) {
				render.moverFrente();
			}
			if((direcao & DPadView.DIR_BAIXO) != 0) {
				render.moverTras();
			}
			if((direcao & DPadView.DIR_ESQUERDA) != 0) {
				render.moverEsquerda();
			}
			if((direcao & DPadView.DIR_DIREITA) != 0) {
				render.moverDireita();
			}

			responsavel2.postDelayed(this, 100); // intervalo aumentado para 100ms
		}
	}

	public void pular(View view) {
		if(render.mundo.noChao(render.camera) || render.gravidade==false || render.camera.peso==0) {
			for(int i=0; i<500; i++) {
				render.camera.pesoTotal = 0;

				render.camera.posicao[1] += render.camera.salto; // forca de pulo
				float[] posAjustada = render.mundo.verificarColisao(render.camera, render.camera.posicao[0], render.camera.posicao[1], render.camera.posicao[2]);
				render.camera.posicao[1] = posAjustada[1];
			}
		}
	}

	public void colocarBloco(View view) {
		float[] pos = render.camera.posicao;

		float yaw = render.camera.yaw;
		float tom = render.camera.tom;

		float dx = (float) (Math.cos(Math.toRadians(tom)) * Math.cos(Math.toRadians(yaw)));
		float dy = (float) Math.sin(Math.toRadians(tom));
		float dz = (float) (Math.cos(Math.toRadians(tom)) * Math.sin(Math.toRadians(yaw)));

		float maxDist = render.camera.alcance;

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

			if(render.mundo.eBlocoSolido(mapaX, mapaY, mapaZ)) {
				if(!render.camera.itemMao.equals("AR")) {
					int blocoX = mapaX + (hitAxis == 0 ? -passoX : 0);
					int blocoY = mapaY + (hitAxis == 1 ? -passoY : 0);
					int blocoZ = mapaZ + (hitAxis == 2 ? -passoZ : 0);
					render.mundo.colocarBloco(blocoX, blocoY, blocoZ, render.camera);
				} else {
					render.mundo.destruirBloco(mapaX, mapaY, mapaZ, render.camera);
				}
				return;
			}
		}
	}

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        return render.eventoToque(e);
    }

    @Override
    protected void onResume() {
        super.onResume();
        tela.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        render.limparTexturas();
        tela.onPause();
    }
}

class Logs {

    private ByteArrayOutputStream saida;

    public void capturar() {
        saida = new ByteArrayOutputStream();
        PrintStream console = new PrintStream(saida);

        System.setOut(console);
        System.setErr(console);
    }

    public String exibir() {
        return saida.toString();
    }
}

class ArmUtil {
	public static String lerArquivo(File arquivo) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(arquivo));
		StringBuilder sb = new StringBuilder();
		String linha;
		while((linha = br.readLine()) != null) {
			sb.append(linha);
		}
		br.close();
		return sb.toString();
	}
}

class Mundo {
	public ExecutorService executor = Executors.newFixedThreadPool(1);
	public GLSurfaceView tela;

    public static int CHUNK_TAMANHO = 16; // padrao: 16, testes: 8
    public static int MUNDO_LATERAL = 120; // padrao: 60, testes: 16
    public static int RAIO_CARREGAMENTO = 2; // padrao: 3, testes: 2, inicial: 15

    public static final int FACES_POR_BLOCO = 6;
	
	public int ceuPosicao;
	
	public int atlasTexturaId = -1;
	public Map<String, float[]> atlasUVMapa = new HashMap<>();

	public ConcurrentHashMap<String, Bloco[][][]> chunksAtivos = new ConcurrentHashMap<>();
	public ConcurrentHashMap<String, List<VBOGrupo>> chunkVBOs = new ConcurrentHashMap<>();
	
    public HashMap<String, Bloco[][][]> chunksCarregados = new HashMap<>();

    public long ultimoCarr = 0;
    public final long intervaloCarr = 0;

	public String tipo;
	public int seed;
	public String pacoteTex;

	public HashMap<String, Boolean> chunksAlterados = new HashMap<>();
	
	public List<String> estruturas = new ArrayList<>();
	
	public static final float[][] NORMAIS = new float[][] {
		{0f, 0f, 1f},
		{0f, 0f, -1f},
		{0f, 1f, 0f},
		{0f, -1f, 0f},
		{-1f, 0f, 0f},
		{1f, 0f, 0f}
	};

	public static FloatBuffer hitboxBuffer;
	public static int hitboxShader = 0;

	public Mundo(GLSurfaceView tela, int seed, String tipo, String pacoteTex) {
		this.tela = tela;
		this.seed = seed;
	    this.tipo = tipo;
		this.pacoteTex = pacoteTex;
		this.definirEstruturas();
	}
	
	public void definirEstruturas() {
		String arvore1 = 
		    "{ "+
			"\"nome\": \"arvore\","+
			"\"blocos\": ["+
			"{\"x\":0, \"y\":0, \"z\":0, \"tipo\": \"TRONCO_CARVALHO\"},"+
			"{\"x\":0, \"y\":1, \"z\":0, \"tipo\": \"TRONCO_CARVALHO\"},"+
			"{\"x\":0, \"y\":2, \"z\":0, \"tipo\": \"TRONCO_CARVALHO\"},"+
			"{\"x\":0, \"y\":3, \"z\":0, \"tipo\": \"TRONCO_CARVALHO\"},"+
			"{\"x\":0, \"y\":4, \"z\":0, \"tipo\": \"TRONCO_CARVALHO\"},"+
			"{\"x\":-1, \"y\":4, \"z\":0, \"tipo\": \"FOLHAS\"},"+
			"{\"x\":1, \"y\":4, \"z\":0, \"tipo\": \"FOLHAS\"},"+
			"{\"x\":1, \"y\":4, \"z\":1, \"tipo\": \"FOLHAS\"},"+
			"{\"x\":0, \"y\":4, \"z\":1, \"tipo\": \"FOLHAS\"},"+
			"{\"x\":-1, \"y\":4, \"z\":-1, \"tipo\": \"FOLHAS\"},"+
			"{\"x\":0, \"y\":4, \"z\":-1, \"tipo\": \"FOLHAS\"},"+
			"{\"x\":-2, \"y\":4, \"z\":0, \"tipo\": \"FOLHAS\"},"+
			"{\"x\":2, \"y\":4, \"z\":0, \"tipo\": \"FOLHAS\"},"+
			"{\"x\":-1, \"y\":5, \"z\":0, \"tipo\": \"FOLHAS\"},"+
			"{\"x\":1, \"y\":5, \"z\":0, \"tipo\": \"FOLHAS\"},"+
			"{\"x\":0, \"y\":5, \"z\":1, \"tipo\": \"FOLHAS\"},"+
			"{\"x\":0, \"y\":5, \"z\":-1, \"tipo\": \"FOLHAS\"},"+
			"{\"x\":0, \"y\":5, \"z\":0, \"tipo\": \"FOLHAS\"}"+
			"]"+
			"}";
			
		String arvore2 = 
		    "{ "+
			"\"nome\": \"arvore\","+
			"\"blocos\": ["+
			"{\"x\":0, \"y\":0, \"z\":0, \"tipo\": \"TRONCO_CARVALHO\"},"+
			"{\"x\":0, \"y\":1, \"z\":0, \"tipo\": \"TRONCO_CARVALHO\"},"+
			"{\"x\":0, \"y\":2, \"z\":0, \"tipo\": \"TRONCO_CARVALHO\"},"+
			"{\"x\":0, \"y\":3, \"z\":0, \"tipo\": \"TRONCO_CARVALHO\"},"+
			"{\"x\":0, \"y\":4, \"z\":0, \"tipo\": \"TRONCO_CARVALHO\"},"+
			"{\"x\":0, \"y\":5, \"z\":0, \"tipo\": \"TRONCO_CARVALHO\"},"+
			"{\"x\":-1, \"y\":5, \"z\":0, \"tipo\": \"FOLHAS\"},"+
			"{\"x\":1, \"y\":5, \"z\":0, \"tipo\": \"FOLHAS\"},"+
			"{\"x\":1, \"y\":5, \"z\":1, \"tipo\": \"FOLHAS\"},"+
			"{\"x\":0, \"y\":5, \"z\":1, \"tipo\": \"FOLHAS\"},"+
			"{\"x\":-1, \"y\":5, \"z\":-1, \"tipo\": \"FOLHAS\"},"+
			"{\"x\":0, \"y\":5, \"z\":-1, \"tipo\": \"FOLHAS\"},"+
			"{\"x\":-2, \"y\":5, \"z\":0, \"tipo\": \"FOLHAS\"},"+
			"{\"x\":2, \"y\":5, \"z\":0, \"tipo\": \"FOLHAS\"},"+
			"{\"x\":-1, \"y\":6, \"z\":0, \"tipo\": \"FOLHAS\"},"+
			"{\"x\":1, \"y\":6, \"z\":0, \"tipo\": \"FOLHAS\"},"+
			"{\"x\":0, \"y\":6, \"z\":1, \"tipo\": \"FOLHAS\"},"+
			"{\"x\":0, \"y\":6, \"z\":-1, \"tipo\": \"FOLHAS\"},"+
			"{\"x\":0, \"y\":6, \"z\":0, \"tipo\": \"FOLHAS\"}"+
			"]"+
			"}";
			
			String pedra1 =
			"{"+
			"\"blocos\": ["+
			"{\"x\": 0, \"y\": 1, \"z\": 0, \"tipo\": \"PEDRA\"},"+
			"{\"x\": 1, \"y\": 1, \"z\": 0, \"tipo\": \"PEDREGULHO\"},"+
			"{\"x\": 1, \"y\": 1, \"z\": 1, \"tipo\": \"PEDRA\"},"+
			"{\"x\": -1, \"y\": 1, \"z\": 0, \"tipo\": \"PEDREGULHO\"},"+
			"{\"x\": 1, \"y\": 1, \"z\": 1, \"tipo\": \"PEDREGULHO\"},"+
			"{\"x\": 0, \"y\": 1, \"z\": 1, \"tipo\": \"PEDREGULHO\"},"+
			"{\"x\": 0, \"y\": 2, \"z\": 0, \"tipo\": \"PEDRA\"},"+
			"{\"x\": 0, \"y\": 2, \"z\": 1, \"tipo\": \"PEDREGULHO\"}"+
			"]"+
			"}";
		estruturas.add(arvore1);
		estruturas.add(arvore2);
		estruturas.add(pedra1);
	}

	public void destruirBloco(final float globalX, final float y, final float globalZ, final Player player) {
		this.executor.execute(new Runnable() {
				@Override
				public void run() {
					int chunkX = (int) Math.floor(globalX / (float) CHUNK_TAMANHO);
					int chunkZ = (int) Math.floor(globalZ / (float) CHUNK_TAMANHO);
					String chaveChunk = chunkX + "," + chunkZ;

					Bloco[][][] chunk = carregarChunk(chunkX, chunkZ);

					int intY = (int) y;
					int localX = (int) (globalX - (chunkX * CHUNK_TAMANHO));
					int localZ = (int) (globalZ - (chunkZ * CHUNK_TAMANHO));

					if(y < 0 || y >= MUNDO_LATERAL || localX < 0 || localX >= CHUNK_TAMANHO || localZ < 0 || localZ >= CHUNK_TAMANHO) return;

					Bloco blocoExistente = chunk[localX][intY][localZ];

					if(blocoExistente == null || blocoExistente.tipo[0].equals("0")) return;
					
					player.inventario.get(0).tipo = blocoExistente.id;
					player.inventario.get(0).quant += 1;
					chunk[localX][intY][localZ] = new Bloco((int) globalX, (int) y, (int) globalZ, "AR");

					if(chunksAtivos.containsKey(chaveChunk)) chunksAlterados.put(chaveChunk, true);
				}
			});
	}

	public void colocarBloco(final float globalX, final float y, final float globalZ,  final Player player) {
		this.executor.execute(new Runnable() {
				@Override
				public void run() {
					int chunkX = (int) Math.floor(globalX / (float) CHUNK_TAMANHO);
					int chunkZ = (int) Math.floor(globalZ / (float) CHUNK_TAMANHO);
					String chaveChunk = chunkX + "," + chunkZ;

					// carrega ou gera o chunk correspondente
					Bloco[][][] chunk = carregarChunk(chunkX, chunkZ);

					int intY = (int) y;
					int localX = (int) (globalX - (chunkX * CHUNK_TAMANHO));
					int localZ = (int) (globalZ - (chunkZ * CHUNK_TAMANHO));

					if(y < 0 || y >= MUNDO_LATERAL || localX < 0 || localX >= CHUNK_TAMANHO || localZ < 0 || localZ >= CHUNK_TAMANHO) return;

					Bloco blocoExistente = chunk[localX][intY][localZ];
					if(blocoExistente != null && !blocoExistente.tipo[0].equals("0")) return;

					// define o bloco
					if(player.inventario.get(0).quant >= 0)chunk[localX][intY][localZ] = new Bloco((int) globalX, (int) y, (int) globalZ, player.itemMao);

					// se o chunk estiver ativo marca como alterado para atualizacao da VBO
					if(chunksAtivos.containsKey(chaveChunk)) chunksAlterados.put(chaveChunk, true);
				}
			});
	}
	
	public void addBloco(final float globalX, final float y, final float globalZ,  final String tipo, final Bloco[][][] chunk) {
		int chunkX = (int) Math.floor(globalX / (float) CHUNK_TAMANHO);
		int chunkZ = (int) Math.floor(globalZ / (float) CHUNK_TAMANHO);

		int intY = (int) y;
		int localX = (int) (globalX - (chunkX * CHUNK_TAMANHO));
		int localZ = (int) (globalZ - (chunkZ * CHUNK_TAMANHO));

		if(y < 0 || y >= MUNDO_LATERAL || localX < 0 || localX >= CHUNK_TAMANHO || localZ < 0 || localZ >= CHUNK_TAMANHO) {
			return;
		}

		Bloco blocoExistente = chunk[localX][intY][localZ];
		if(blocoExistente != null && !blocoExistente.tipo[0].equals("0")) {
			return;
		}

		chunk[localX][intY][localZ] = new Bloco((int) globalX, (int) y, (int) globalZ, tipo);
	}

	public boolean eBlocoSolido(int bx, int by, int bz) {
		if(by < 0 || by >= MUNDO_LATERAL) {
			return false;
		}

		int chunkX = (int) Math.floor(bx / (float) CHUNK_TAMANHO);
		int chunkZ = (int) Math.floor(bz / (float) CHUNK_TAMANHO);
		String chaveChunk = chunkX + "," + chunkZ;

		if(!chunksAtivos.containsKey(chaveChunk)) return false;

		Bloco[][][] chunk = chunksAtivos.get(chaveChunk);

		int localX = bx - chunkX * CHUNK_TAMANHO;
		int localZ = bz - chunkZ * CHUNK_TAMANHO;

		if(localX < 0 || localX >= CHUNK_TAMANHO || localZ < 0 || localZ >= CHUNK_TAMANHO) {
			return false;
		}

		Bloco bloco = chunk[localX][by][localZ];
		return bloco != null && bloco.solido==true;
	}

	public float[] veerificarColisao(Camera camera, float novoTx, float novoTy, float novoTz) {
		final float altura = camera.hitbox[0];
		final float largura = camera.hitbox[1];
		final float metadeLargura = largura / 2f;
		float novoX = novoTx;
		float novoY = novoTy - 1.5f;
		float novoZ = novoTz;
		final int maxIter = 5;
		int iter = 0;
		boolean colidiu;

		do {
			colidiu = false;
			final float minX = novoX - metadeLargura;
			final float maxX = novoX + metadeLargura;
			final float minY = novoY;
			final float maxY = novoY + altura;
			final float minZ = novoZ - metadeLargura;
			final float maxZ = novoZ + metadeLargura;

			final int startX = (int) Math.floor(minX);
			final int endX = (int) Math.floor(maxX);
			final int startY = (int) Math.floor(minY);
			final int endY = (int) Math.floor(maxY);
			final int startZ = (int) Math.floor(minZ);
			final int endZ = (int) Math.floor(maxZ);

			for (int bx = startX; bx <= endX; bx++) {
				for (int by = startY; by <= endY; by++) {
					for (int bz = startZ; bz <= endZ; bz++) {
						if (eBlocoSolido(bx, by, bz)) {
							final float blocoMinX = bx;
							final float blocoMaxX = bx + 1f;
							final float blocoMinY = by;
							final float blocoMaxY = by + 1f;
							final float blocoMinZ = bz;
							final float blocoMaxZ = bz + 1f;

							final float sobreposX = Math.min(maxX, blocoMaxX) - Math.max(minX, blocoMinX);
							final float sobreposY = Math.min(maxY, blocoMaxY) - Math.max(minY, blocoMinY);
							final float sobreposZ = Math.min(maxZ, blocoMaxZ) - Math.max(minZ, blocoMinZ);

							if (sobreposX < sobreposY && sobreposX < sobreposZ) {
								novoX += (novoX < bx + 0.5f) ? -sobreposX : sobreposX;
							} else if (sobreposY < sobreposX && sobreposY < sobreposZ) {
								novoY += (novoY < by + 0.5f) ? -sobreposY : sobreposY;
							} else {
								novoZ += (novoZ < bz + 0.5f) ? -sobreposZ : sobreposZ;
							}
							colidiu = true;
						}
					}
				}
			}
			iter++;
		} while (colidiu && iter < maxIter);

		return new float[] { novoX, novoY + 1.5f, novoZ };
	}

	public boolean noChao(Camera camera) {
		float posPés = camera.posicao[1]-1 - (camera.hitbox[0] / 2);

		float yTeste = posPés - 0.1f;
		int by = (int) Math.floor(yTeste);

		float halfLargura = camera.hitbox[1] / 2f;
		int bx1 = (int) Math.floor(camera.posicao[0] - halfLargura);
		int bx2 = (int) Math.floor(camera.posicao[0] + halfLargura);
		int bz1 = (int) Math.floor(camera.posicao[2] - halfLargura);
		int bz2 = (int) Math.floor(camera.posicao[2] + halfLargura);

		for (int bx = bx1; bx <= bx2; bx++) {
			for (int bz = bz1; bz <= bz2; bz++) {
				if (eBlocoSolido(bx, by, bz)) {
					return true;
				}
			}
		}
		return false;
	}

    public boolean faceVisivel(int x, int y, int z, int face) {
        int dx = 0, dy = 0, dz = 0;
        switch(face) {
            case 0: dz = 1; break;
            case 1: dz = -1; break;
            case 2: dy = 1; break;
            case 3: dy = -1; break;
            case 4: dx = -1; break;
            case 5: dx = 1; break;
        }

        int nx = x + dx;
        int ny = y + dy;
        int nz = z + dz;

        // verificacao de limites verticais
        if(ny < 0 || ny >= MUNDO_LATERAL) return true;

        // calculo de coordenadas de chunk
        int chunkX = (int) Math.floor(nx / (float)CHUNK_TAMANHO);
        int chunkZ = (int) Math.floor(nz / (float)CHUNK_TAMANHO);
        String chaveChunk = chunkX + "," + chunkZ;

        // verificacao de chunk carregado
        if(!chunksAtivos.containsKey(chaveChunk)) return true;

        Bloco[][][] chunkVizinho = chunksAtivos.get(chaveChunk);
        if(chunkVizinho==null) return true;

        // conversao para coordenadas locais
        int localX = nx - (chunkX * CHUNK_TAMANHO);
        int localZ = nz - (chunkZ * CHUNK_TAMANHO);

        // ajuste para valores negativos
        if(localX < 0) localX += CHUNK_TAMANHO;
        if(localZ < 0) localZ += CHUNK_TAMANHO;

        // verificacao de indices
        if(localX >= CHUNK_TAMANHO || localZ >= CHUNK_TAMANHO) return true;

        try {
            Bloco vizinho = chunkVizinho[localX][ny][localZ];
            return vizinho==null || vizinho.tipo[0].equals("0") || !vizinho.solido==true;
        } catch(ArrayIndexOutOfBoundsException e) {
            return true;
        }
    }

    // gera ou carrega chunks ja existentes
    public Bloco[][][] carregarChunk(int chunkX, int chunkY) {
        String chave = chunkX + "," + chunkY;
        if(chunksCarregados.containsKey(chave)) {
            return chunksCarregados.get(chave);
        }
        else {
            Bloco[][][] chunk = gerarChunk(chunkX, chunkY);
            chunksCarregados.put(chave, chunk);
            return chunk;
        }
    }
	
	public void adicionarEstrutura(int x, int y, int z, String json, Bloco[][][] chunk) {
		String jsonString = json;

		if(!json.equals("")) jsonString = json;
		try {
			JSONObject estrutura = new JSONObject(jsonString);

			JSONArray blocos = estrutura.getJSONArray("blocos");

			for(int i = 0; i < blocos.length(); i++) {
				JSONObject bloco = blocos.getJSONObject(i);

				int bx = bloco.getInt("x") + x;
				int by = bloco.getInt("y") + y;
				int bz = bloco.getInt("z") + z;

				addBloco(bx, by, bz, bloco.getString("tipo"), chunk);
			}
		} catch(JSONException e) {
			System.out.println("erro ao carregar o json estrutura: "+e);
		}
	}
	
	public boolean spawnEstrutura(float chanceSpawn, int x, int z, int seed ) {
		float TAMANHO_RUIDO = 0.99f;
		
		float LIMITE = 1.5f;
		
		float ruido = PerlinNoise.ruido(x * TAMANHO_RUIDO, z * TAMANHO_RUIDO, seed);
		float normalizado = (ruido + 2f) / 4f;
		
		if(normalizado > LIMITE && Math.random() < chanceSpawn) {
			return true;
		}
		return false;
	}

	public float[] obterNormalParaFace(int face) {
		return NORMAIS[face];
	}

	public void deesenharHitbox(Camera camera, float[] vpMatriz) {
		float altura = camera.hitbox[0];
		float largura = camera.hitbox[1];
		float metadeLargura = largura / 2f;
		float minX = camera.posicao[0] - metadeLargura;
		float maxX = camera.posicao[0] + metadeLargura;
		float minY = camera.posicao[1] - 1.5f;
		float maxY = camera.posicao[1] + altura;
		float minZ = camera.posicao[2] - metadeLargura;
		float maxZ = camera.posicao[2] + metadeLargura;
		float[] vertices = new float[] {
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
			hitboxBuffer = ByteBuffer.allocateDirect(vertices.length * 4)
				.order(ByteOrder.nativeOrder())
				.asFloatBuffer();
		}
		hitboxBuffer.clear();
		hitboxBuffer.put(vertices);
		hitboxBuffer.position(0);
		String vertC = "uniform mat4 uMVPMatriz;" +
			"attribute vec3 aPosicao;" +
			"void main() { gl_Position = uMVPMatriz * vec4(aPosicao, 1.0); }";
		String fragC = "precision mediump float;" +
			"uniform vec4 uCor;" +
			"void main() { gl_FragColor = uCor; }";
		int vertShader = ShaderUtils.carregarShader(GLES30.GL_VERTEX_SHADER, vertC);
		int fragShader = ShaderUtils.carregarShader(GLES30.GL_FRAGMENT_SHADER, fragC);
		int shaderPrograma2 = GLES30.glCreateProgram();
		GLES30.glAttachShader(shaderPrograma2, vertShader);
		GLES30.glAttachShader(shaderPrograma2, fragShader);
		GLES30.glLinkProgram(shaderPrograma2);
		GLES30.glUseProgram(shaderPrograma2);
		int mvpLidar = GLES30.glGetUniformLocation(shaderPrograma2, "uMVPMatriz");
		int corLidar = GLES30.glGetUniformLocation(shaderPrograma2, "uCor");
		int posLidar = GLES30.glGetAttribLocation(shaderPrograma2, "aPosicao");
		GLES30.glUniformMatrix4fv(mvpLidar, 1, false, vpMatriz, 0);
		GLES30.glUniform4f(corLidar, 1f, 0f, 0f, 1f);
		GLES30.glEnableVertexAttribArray(posLidar);
		GLES30.glVertexAttribPointer(posLidar, 3, GLES30.GL_FLOAT, false, 3 * 4, hitboxBuffer);
		GLES30.glDrawArrays(GLES30.GL_LINES, 0, vertices.length / 3);
		GLES30.glDisableVertexAttribArray(posLidar);
		GLES30.glUseProgram(0);
	}

	public Bloco[][][] gerarChunk(final int chunkX, final int chunkZ) {
		final Bloco[][][] chunk = new Bloco[CHUNK_TAMANHO][MUNDO_LATERAL][CHUNK_TAMANHO];
		int baseX = chunkX * CHUNK_TAMANHO;
		int baseZ = chunkZ * CHUNK_TAMANHO;
		boolean plano = tipo.equals("plano");

		// gerar blocos básicos com Perlin
		for(int x = 0; x < CHUNK_TAMANHO; x++) {
			for(int z = 0; z < CHUNK_TAMANHO; z++) {
				int globalX = baseX + x;
				int globalZ = baseZ + z;
				float noise = plano ? 0.001f : PerlinNoise.ruido(globalX / 200f, globalZ / 200f, seed);
				int altura = (int) (noise * 16f + 32f);
				for(int y = 0; y < MUNDO_LATERAL; y++) {
					String tipoBloco;
					if(y == 0) tipoBloco = "BEDROCK";
					else if(y < altura - 1) tipoBloco = "PEDRA";
					else if(y < altura) tipoBloco = "TERRA";
					else if(y == altura) tipoBloco = "GRAMA";
					else tipoBloco = "AR";
					chunk[x][y][z] = new Bloco(globalX, y, globalZ, tipoBloco);
				}
			}
		}

		// adicionar estruturas (árvores, pedras)
		for(int x = 0; x < CHUNK_TAMANHO; x++) {
			for(int z = 0; z < CHUNK_TAMANHO; z++) {
				int globalX = baseX + x;
				int globalZ = baseZ + z;
				int altura = (int) (PerlinNoise.ruido(globalX / 200f, globalZ / 200f, seed) * 16f + 32f);

				if(spawnEstrutura(0.1f, globalX, globalZ, seed))
					adicionarEstrutura(globalX, altura, globalZ, estruturas.get(0), chunk);
				if(spawnEstrutura(0.01f, globalX, globalZ, seed))
					adicionarEstrutura(globalX, altura, globalZ, estruturas.get(1), chunk);
				if(spawnEstrutura(0.009f, globalX, globalZ, seed))
					adicionarEstrutura(globalX, altura, globalZ, estruturas.get(2), chunk);
			}
		}

		return chunk;
	}

	public Map<Integer, List<float[]>> calculoVBO(Bloco[][][] chunk) {
		HashMap<Integer, List<float[]>> dadosPorTextura = new HashMap<Integer, List<float[]>>();
		for (int x = 0; x < CHUNK_TAMANHO; x++) {
			for (int y = 0; y < MUNDO_LATERAL; y++) {
				for (int z = 0; z < CHUNK_TAMANHO; z++) {
					Bloco bloco = chunk[x][y][z];
					if (bloco == null || bloco.tipo[0].equals("0")) continue;

					float[] vertices = bloco.obterVertices();

					for (int face = 0; face < 6; face++) {
						if (!faceVisivel(bloco.x, bloco.y, bloco.z, face)) continue;

						float[] dadosFace = new float[48];
						float[] normal = obterNormalParaFace(face);
						int offset = face * 18;

						for (int v = 0; v < 6; v++) {
							int src = offset + v * 3;
							int dst = v * 8;
							dadosFace[dst] = vertices[src];
							dadosFace[dst + 1] = vertices[src + 1];
							dadosFace[dst + 2] = vertices[src + 2];
							dadosFace[dst + 3] = normal[0];
							dadosFace[dst + 4] = normal[1];
							dadosFace[dst + 5] = normal[2];
						}

						String recurso = bloco.tipo[face];
						float[] uv = atlasUVMapa.get(recurso);
						if (uv == null) uv = new float[]{0f, 0f, 1f, 1f};

						float u1 = uv[0], v1 = uv[1], u2 = uv[2], v2 = uv[3];
						float[] uvs = {
							u1, v2, u2, v2, u2, v1,
							u1, v2, u2, v1, u1, v1
						};

						for (int v = 0; v < 6; v++) {
							int dst = v * 8 + 6;
							dadosFace[dst] = uvs[v * 2];
							dadosFace[dst + 1] = uvs[v * 2 + 1];
						}

						int texturaId = atlasTexturaId;
						List<float[]> lista = dadosPorTextura.get(texturaId);
						if (lista == null) {
							lista = new ArrayList<float[]>();
							dadosPorTextura.put(texturaId, lista);
						}
						lista.add(dadosFace);
					}
				}
			}
		}
		return dadosPorTextura;
	}

	public float[] verificarColisao(Camera camera, float novoTx, float novoTy, float novoTz) {
		final float altura = camera.hitbox[0];
		final float largura = camera.hitbox[1];
		final float metadeLargura = largura / 2f;
		float novoX = novoTx;
		float novoY = novoTy - 1.5f;
		float novoZ = novoTz;
		final int maxIter = 5;
		int iter = 0;
		boolean colidiu;
		do {
			colidiu = false;
			float minX = novoX - metadeLargura;
			float maxX = novoX + metadeLargura;
			float minY = novoY;
			float maxY = novoY + altura;
			float minZ = novoZ - metadeLargura;
			float maxZ = novoZ + metadeLargura;
			int startX = (int)Math.floor(minX);
			int endX = (int)Math.floor(maxX);
			int startY = (int)Math.floor(minY);
			int endY = (int)Math.floor(maxY);
			int startZ = (int)Math.floor(minZ);
			int endZ = (int)Math.floor(maxZ);
			for (int bx = startX; bx <= endX; bx++) {
				for (int by = startY; by <= endY; by++) {
					for (int bz = startZ; bz <= endZ; bz++) {
						if (eBlocoSolido(bx, by, bz)) {
							float blocoMinX = bx, blocoMaxX = bx + 1f;
							float blocoMinY = by, blocoMaxY = by + 1f;
							float blocoMinZ = bz, blocoMaxZ = bz + 1f;
							float sobreposX = Math.min(maxX, blocoMaxX) - Math.max(minX, blocoMinX);
							float sobreposY = Math.min(maxY, blocoMaxY) - Math.max(minY, blocoMinY);
							float sobreposZ = Math.min(maxZ, blocoMaxZ) - Math.max(minZ, blocoMinZ);
							if (sobreposX < sobreposY && sobreposX < sobreposZ) {
								novoX += (novoX < bx + 0.5f) ? -sobreposX : sobreposX;
							} else if (sobreposY < sobreposX && sobreposY < sobreposZ) {
								novoY += (novoY < by + 0.5f) ? -sobreposY : sobreposY;
							} else {
								novoZ += (novoZ < bz + 0.5f) ? -sobreposZ : sobreposZ;
							}
							colidiu = true;
						}
					}
				}
			}
			iter++;
		} while (colidiu && iter < maxIter);
		return new float[] { novoX, novoY + 1.5f, novoZ };
	}
	
	public void atualizarCeu(float[] viewMatriz, float[] projecaoMatriz, float tempo) {
		String vertC =
			"#version 300 es\n" +
			"layout (location = 0) in vec3 aPos;\n" +
			"uniform mat4 u_view;\n" +
			"uniform mat4 u_projecao;\n" +
			"out vec3 TexCoords;\n" +
			"void main() {\n" +
			"    TexCoords = aPos;\n" +
			"    mat4 view = u_view;\n" +
			"    view[3][0] = 0.0;\n" +
			"    view[3][1] = 0.0;\n" +
			"    view[3][2] = 0.0;\n" +
			"    vec4 pos = u_projecao * view * vec4(aPos, 1.0);\n" +
			"    gl_Position = pos.xyww;\n" +
			"}";

		String fragC =
			"#version 300 es\n" +
			"precision mediump float;\n" +
			"in vec3 TexCoords;\n" +
			"uniform float u_tempo;\n" +
			"out vec4 FragColor;\n" +
			"void main() {\n" +
			"    vec3 diaCor = vec3(0.53, 0.81, 0.98);\n" +
			"    vec3 noiteCor = vec3(0.0, 0.0, 0.1);\n" +
			"    float t = abs(u_tempo - 0.5) * 2.0;\n" +
			"    vec3 ceuCor = mix(diaCor, noiteCor, t);\n" +
			"    FragColor = vec4(ceuCor, 1.0);\n" +
			"}";

		int vertShader = GLES30.glCreateShader(GLES30.GL_VERTEX_SHADER);
		GLES30.glShaderSource(vertShader, vertC);
		GLES30.glCompileShader(vertShader);
		int[] compiled = new int[1];
		GLES30.glGetShaderiv(vertShader, GLES30.GL_COMPILE_STATUS, compiled, 0);
		if (compiled[0] == 0) { 
			GLES30.glDeleteShader(vertShader);
			return;
		}

		int fragShader = GLES30.glCreateShader(GLES30.GL_FRAGMENT_SHADER);
		GLES30.glShaderSource(fragShader, fragC);
		GLES30.glCompileShader(fragShader);
		GLES30.glGetShaderiv(fragShader, GLES30.GL_COMPILE_STATUS, compiled, 0);
		if(compiled[0]==0) { 
			GLES30.glDeleteShader(fragShader);
			return;
		}

		int shaderPrograma = GLES30.glCreateProgram();
		GLES30.glAttachShader(shaderPrograma, vertShader);
		GLES30.glAttachShader(shaderPrograma, fragShader);
		GLES30.glLinkProgram(shaderPrograma);
		GLES30.glUseProgram(shaderPrograma);

		int viewLoc = GLES30.glGetUniformLocation(shaderPrograma, "u_view");
		int projLoc = GLES30.glGetUniformLocation(shaderPrograma, "u_projecao");
		int tempoLoc = GLES30.glGetUniformLocation(shaderPrograma, "u_tempo");
		GLES30.glUniformMatrix4fv(viewLoc, 1, false, viewMatriz, 0);
		GLES30.glUniformMatrix4fv(projLoc, 1, false, projecaoMatriz, 0);
		GLES30.glUniform1f(tempoLoc, tempo);

		float[] vertices = {
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

		FloatBuffer ceuBuffer = ByteBuffer.allocateDirect(vertices.length * 4)
			.order(ByteOrder.nativeOrder())
			.asFloatBuffer();
		ceuBuffer.put(vertices);
		ceuBuffer.position(0);

		int[] vao = new int[1], vbo = new int[1];
		GLES30.glGenVertexArrays(1, vao, 0);
		GLES30.glGenBuffers(1, vbo, 0);

		GLES30.glBindVertexArray(vao[0]);
		GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo[0]);
		GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, vertices.length * 4, ceuBuffer, GLES30.GL_STATIC_DRAW);
		GLES30.glEnableVertexAttribArray(0);
		GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, 3 * 4, 0);
		GLES30.glBindVertexArray(0);

		GLES30.glDepthFunc(GLES30.GL_LEQUAL);
		GLES30.glBindVertexArray(vao[0]);
		GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, 36);
		GLES30.glBindVertexArray(0);
		GLES30.glDepthFunc(GLES30.GL_LESS);
	}
}

class TipoBloco {
    public static HashMap<String, String[]> tipos = new HashMap<>();

    static {
        definirTipo("AR",
					"0", "0", "0",
					"0", "0", "0"
					);
        definirTipo("GRAMA",
					"texturas/grama_lado.png",
					"texturas/grama_lado.png",
					"texturas/grama_cima.png",
					"texturas/terra.png",
					"texturas/grama_lado2.png",
					"texturas/grama_lado2.png"
					);
        definirTipo("TERRA",
					"texturas/terra.png",
					"texturas/terra.png",
					"texturas/terra.png",
					"texturas/terra.png",
					"texturas/terra.png",
					"texturas/terra.png"
					);
        definirTipo("PEDRA",
					"texturas/pedra.png",
					"texturas/pedra.png",
					"texturas/pedra.png",
					"texturas/pedra.png",
					"texturas/pedra.png",
					"texturas/pedra.png"
					);
        definirTipo("PEDREGULHO",
					"texturas/pedregulho.png",
					"texturas/pedregulho.png",
					"texturas/pedregulho.png",
					"texturas/pedregulho.png",
					"texturas/pedregulho.png",
					"texturas/pedregulho.png"
					);
        definirTipo("TABUAS_CARVALHO",
					"texturas/tabuas_carvalho.png",
					"texturas/tabuas_carvalho.png",
					"texturas/tabuas_carvalho.png",
					"texturas/tabuas_carvalho.png",
					"texturas/tabuas_carvalho.png",
					"texturas/tabuas_carvalho.png"
					);
		definirTipo("TRONCO_CARVALHO",
					"texturas/tronco_carvalho.png",
					"texturas/tronco_carvalho.png",
					"texturas/tronco_carvalho_cima.png",
					"texturas/tronco_carvalho_cima.png",
					"texturas/tronco_carvalho.png",
					"texturas/tronco_carvalho.png"
					);
		definirTipo("FOLHAS",
					"texturas/folhas.png",
					"texturas/folhas.png",
					"texturas/folhas.png",
					"texturas/folhas.png",
					"texturas/folhas.png",
					"texturas/folhas.png"
					);
        definirTipo("BEDROCK",
					"texturas/bedrock.png",
					"texturas/bedrock.png",
					"texturas/bedrock.png",
					"texturas/bedrock.png",
					"texturas/bedrock.png",
					"texturas/bedrock.png"
					);
    }
	
    public static void definirTipo(String id, String... tipo) {
        tipos.put(id, tipo);
    }
}

class Bloco {
    public int x, y, z;
	public String id;
    public String[] tipo;
	public boolean solido=true, liquido=false, gasoso=false;

    public Bloco(int x, int y, int z, String tipo) {
        this.x = x;
        this.y = y;
        this.z = z;
		if(tipo.equals("AR")) this.solido=false;
		this.tipo = TipoBloco.tipos.get(tipo);
		this.id = tipo;
    }

    public float[] obterVertices() {
        return new float[] {
            // face de frente
            x, y, z+1, x+1, y, z+1, x+1, y+1, z+1,
            x, y, z+1, x+1, y+1, z+1, x, y+1, z+1,

            // face de tras
            x, y, z, x+1, y, z, x+1, y+1, z,
            x, y, z, x+1, y+1, z, x, y+1, z,

            // face de cima
            x, y+1, z, x+1, y+1, z, x+1, y+1, z+1,
            x, y+1, z, x+1, y+1, z+1, x, y+1, z+1,

            // face de baixo
            x, y, z, x+1, y, z, x+1, y, z+1,
            x, y, z, x+1, y, z+1, x, y, z+1,

            // face esquerda
            x, y, z, x, y+1, z, x, y+1, z+1,
            x, y, z, x, y+1, z+1, x, y, z+1,

            // face direita
            x+1, y, z, x+1, y+1, z, x+1, y+1, z+1,
            x+1, y, z, x+1, y+1, z+1, x+1, y, z+1
        };
    }
    public float[] obterCoordenadas() {
        float[] coordenadas = new float[6 * 6 * 2];
        int faceIndice = 0;

        for(int i = 0; i < coordenadas.length; i += 12) {
            String texturaIndice = tipo[faceIndice];
            float u1 = 0, u2 = 1, v1 = 0, v2 = 1;

            if(texturaIndice.equals("-1") || texturaIndice.equals("0")) {
                u1 = v1 = 0;
                u2 = v2 = 1;
            } else {}

            coordenadas[i] = u1; coordenadas[i+1] = v2;
            coordenadas[i+2] = u2; coordenadas[i+3] = v2;
            coordenadas[i+4] = u2; coordenadas[i+5] = v1;

            coordenadas[i+6] = u1; coordenadas[i+7] = v2;
            coordenadas[i+8] = u2; coordenadas[i+9] = v1;
            coordenadas[i+10] = u1; coordenadas[i+11] = v1;

            faceIndice++;
        }
        return coordenadas;
    }
}

class GLRender implements GLSurfaceView.Renderer {

    public final Context contexto;
    private final GLSurfaceView tela;
    private final float[] projecaoMatriz = new float[16];
    private final float[] viewMatriz = new float[16];
    private final float[] vpMatriz = new float[16];

    public Mundo mundo;
    public Player camera;

    public String modo = "alpha";

    public int chunksPorVez = 4;
    public ExecutorService executor = Executors.newFixedThreadPool(4);

    private int pontoAtivo = -1;
    private float ultimoX, ultimoY;

	private float pesoConta = 0f;

	public boolean debug = false;
	
	public int seed;
	public String tipo;
	public String pacoteTex;
	
	public boolean gravidade = true;
	
	public float tempo = 0.40f;
	
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

    public boolean eventoToque(MotionEvent e) {
        int acao = e.getActionMasked();
        switch(acao) {
            case MotionEvent.ACTION_DOWN:
                pontoAtivo = e.getPointerId(0);
                ultimoX = e.getX(0);
                ultimoY = e.getY(0);
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                if(pontoAtivo==-1) {
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
                // se o dedo ativo foi levantado escolhe outro que esteja tocando
                int ponto = e.getPointerId(e.getActionIndex());
                if(ponto==pontoAtivo) {
                    int novoIndice = (e.getActionIndex()==0 ? 1 : 0);
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
	
	public void renderizar(float[] vpMatriz) {
		GLES30.glUseProgram(shaderPrograma);
		GLES30.glUniformMatrix4fv(lidarvPMatriz, 1, false, vpMatriz, 0);
		GLES30.glUniform1f(lidarLuzIntensidade, luz);
		GLES30.glUniform3fv(lidarLuzDirecao, 1, luzDirecao, 0);

		for(Map.Entry<String, Bloco[][][]> entry : mundo.chunksAtivos.entrySet()) {
			String chave = entry.getKey();
			if(mundo.chunksAlterados.containsKey(chave) && mundo.chunksAlterados.get(chave)) {
				Bloco[][][] chunk = entry.getValue();
				Map<Integer, List<float[]>> dados = mundo.calculoVBO(chunk);
				List<VBOGrupo> grupos = this.gerarVBO(dados);
				mundo.chunkVBOs.put(chave, grupos);
				mundo.chunksAlterados.put(chave, false);
			}
			List<VBOGrupo> grupos = mundo.chunkVBOs.get(chave);

			if(grupos==null) continue;

			for(VBOGrupo grupo : grupos) {
				GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, grupo.texturaId);
				GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, grupo.vboId);
				GLES30.glVertexAttribPointer(lidarPosicao, 3, GLES30.GL_FLOAT, false, 8 * 4, 0);
				GLES30.glEnableVertexAttribArray(lidarPosicao);
				GLES30.glVertexAttribPointer(lidarNormal, 3, GLES30.GL_FLOAT, false, 8 * 4, 3 * 4);
				GLES30.glEnableVertexAttribArray(lidarNormal);
				GLES30.glVertexAttribPointer(lidarTexCoord, 2, GLES30.GL_FLOAT, false, 8 * 4, 6 * 4);
				GLES30.glEnableVertexAttribArray(lidarTexCoord);
				GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, grupo.vertices);
			}
		}
		GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0);
	}

	public void iniciarShaders(Context contexto) {
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
			"   gl_Position = u_VPMatriz * a_Posicao;" +
			"   v_TexCoord = a_TexCoord;" +
			"   v_intencidadeLuz = max(dot(normalize(a_Normal), normalize(u_direcaoLuz)), 0.70) * u_intensidadeLuz;" +
			"}";
		String shaderFragmento =
            "precision mediump float;" +
            "varying vec2 v_TexCoord;" +
            "varying float v_intencidadeLuz;" +
            "uniform sampler2D u_Textura;" +
            "void main() {" +
            "   vec4 corTextura = texture2D(u_Textura, v_TexCoord);" +
            "   gl_FragColor = vec4(corTextura.rgb * v_intencidadeLuz, corTextura.a);" +
            "}";
		shaderPrograma = ShaderUtils.criarPrograma(shaderVertice, shaderFragmento);
		lidarvPMatriz = GLES30.glGetUniformLocation(shaderPrograma, "u_VPMatriz");
		lidarLuzDirecao = GLES30.glGetUniformLocation(shaderPrograma, "u_direcaoLuz");
		lidarPosicao = GLES30.glGetAttribLocation(shaderPrograma, "a_Posicao");
		lidarNormal = GLES30.glGetAttribLocation(shaderPrograma, "a_Normal");
		lidarTexCoord = GLES30.glGetAttribLocation(shaderPrograma, "a_TexCoord");
		lidarTexturas = GLES30.glGetUniformLocation(shaderPrograma, "u_Textura");
		lidarLuzIntensidade = GLES30.glGetUniformLocation(shaderPrograma, "u_intensidadeLuz");
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
			int totalVertices = 0;

			for(float[] arr : entry.getValue()) {
				totalVertices += arr.length / 8;
			}

			FloatBuffer buffer = ByteBuffer.allocateDirect(totalVertices * 8 * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();

			for(float[] dados : entry.getValue()) {
				buffer.put(dados);
			}
			buffer.position(0);

			int[] vboIds = new int[1];
			GLES30.glGenBuffers(1, vboIds, 0);
			GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboIds[0]);
			GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, buffer.capacity() * 4, buffer, GLES30.GL_STATIC_DRAW);

			grupos.add(new VBOGrupo(texturaId, vboIds[0], totalVertices));
		}
		GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0);
		return grupos;
	}
	
    public GLRender(Context contexto, GLSurfaceView tela, int seed, String tipo, String pacoteTex) {
        this.contexto = contexto;
        this.tela = tela;
        this.camera = new Player();
		this.seed = seed;
		this.tipo = tipo;
		this.pacoteTex = pacoteTex;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES30.glClearColor(0.40f, 0.65f, 0.85f, 1.0f);
        GLES30.glEnable(GLES30.GL_DEPTH_TEST);
		
        this.mundo = new Mundo(this.tela, this.seed, this.tipo, this.pacoteTex);
        if(modo.equals("alpha")) this.mundo.RAIO_CARREGAMENTO = 2;
        if(modo.equals("teste")) this.mundo.RAIO_CARREGAMENTO = 1;
        this.iniciarShaders(contexto);
        this.carregarTexturas(contexto);
        atualizarViewMatriz();
    }

	@Override
    public void onDrawFrame(GL10 gl) {
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);
		mundo.atualizarCeu(vpMatriz, projecaoMatriz, tempo);
		
		if(!mundo.noChao(camera) && gravidade==true || camera.peso==0) {
			if(pesoConta>=2f) pesoConta = 2f;
			else pesoConta += camera.pesoTotal;
			float novaY = camera.posicao[1] - camera.peso*pesoConta;
			float[] pos = mundo.verificarColisao(camera, camera.posicao[0], novaY, camera.posicao[2]);
			camera.posicao[1] = pos[1];
		}

		atualizarChunks(camera.posicao[0], camera.posicao[2]);

		Matrix.multiplyMM(vpMatriz, 0, projecaoMatriz, 0, viewMatriz, 0);
		renderizar(vpMatriz);

		atualizarViewMatriz();
    }

    public void atualizarChunks(final float posX, final float posZ) {
		int chunkJogadorX = (int) Math.floor(posX / mundo.CHUNK_TAMANHO);
		int chunkJogadorZ = (int) Math.floor(posZ / mundo.CHUNK_TAMANHO);

		// remove chunks fora do raio
		Iterator<Map.Entry<String, Bloco[][][]>> iterator = mundo.chunksAtivos.entrySet().iterator();
		while(iterator.hasNext()) {
			Map.Entry<String, Bloco[][][]> entry = iterator.next();
			String chave = entry.getKey();
			String[] partes = chave.split(",");
			int chunkX = Integer.parseInt(partes[0]);
			int chunkZ = Integer.parseInt(partes[1]);

			if(Math.abs(chunkX - chunkJogadorX) > mundo.RAIO_CARREGAMENTO ||
				Math.abs(chunkZ - chunkJogadorZ) > mundo.RAIO_CARREGAMENTO) {

				iterator.remove();

				List<VBOGrupo> grupos = mundo.chunkVBOs.remove(chave);
				if(grupos != null) {
					for (int i = 0; i < grupos.size(); i++) {
						GLES30.glDeleteBuffers(1, new int[]{ grupos.get(i).vboId }, 0);
					}
				}
			}
		}

		// gerar novos chunks ordenados por distância
		PriorityQueue<ChunkCandidato> fila = new PriorityQueue<ChunkCandidato>(10, new Comparator<ChunkCandidato>() {
				public int compare(ChunkCandidato a, ChunkCandidato b) {
					return Double.compare(a.distancia, b.distancia);
				}
			});

		for(int x = chunkJogadorX - mundo.RAIO_CARREGAMENTO; x <= chunkJogadorX + mundo.RAIO_CARREGAMENTO; x++) {
			for(int z = chunkJogadorZ - mundo.RAIO_CARREGAMENTO; z <= chunkJogadorZ + mundo.RAIO_CARREGAMENTO; z++) {
				final String chave = x + "," + z;
				if(!mundo.chunksAtivos.containsKey(chave)) {
					double dist = Math.sqrt((x - chunkJogadorX)*(x - chunkJogadorX) + (z - chunkJogadorZ)*(z - chunkJogadorZ));
					fila.offer(new ChunkCandidato(chave, x, z, dist));
				}
			}
		}

		int carregados = 0;
		while(!fila.isEmpty() && carregados < this.chunksPorVez) {
			final ChunkCandidato c = fila.poll();
			final String chave = c.chave;
			final Bloco[][][] chunk = mundo.carregarChunk(c.x, c.z);
			mundo.chunksAtivos.put(chave, chunk);

			executor.submit(new Runnable() {
					public void run() {
						final Map<Integer, List<float[]>> dados = mundo.calculoVBO(chunk);
						tela.queueEvent(new Runnable() {
								public void run() {
									List<VBOGrupo> grupos = gerarVBO(dados);
									mundo.chunkVBOs.put(chave, grupos);
								}
							});
					}
				});
			carregados++;
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

    @Override
    public void onSurfaceChanged(GL10 gl, int horizontal, int lateral) {
        GLES30.glViewport(0, 0, horizontal, lateral);
        float ratio = (float) horizontal / lateral;
        Matrix.perspectiveM(projecaoMatriz, 0, 90, ratio, 0.1f, 1000f);
    }

    public void moverFrente() {
		// movimento apenas no X e Z, se a camera ta olhando pra cima
		float[] direcao = {
			camera.foco[0], 
			0, // remove Y
		    camera.foco[2]
		};

		// normaliza o vetor
		float mag = (float)Math.sqrt(direcao[0]*direcao[0] + direcao[2]*direcao[2]);
		if(mag > 0) {
			direcao[0] /= mag;
			direcao[2] /= mag;
		}

		float novaX = camera.posicao[0] + direcao[0] * camera.velocidade;
		float novaZ = camera.posicao[2] + direcao[2] * camera.velocidade;

		// aplica colisao sem alterar Y
		float[] pos = mundo.verificarColisao(camera, novaX, camera.posicao[1], novaZ);
		camera.posicao[0] = pos[0];
		camera.posicao[2] = pos[2];
	}

	public void moverTras() {
		float[] direcao = {
			camera.foco[0], 
			0, // remove Y
			camera.foco[2]
		};

		float mag = (float)Math.sqrt(direcao[0]*direcao[0] + direcao[2]*direcao[2]);
		if(mag > 0) {
			direcao[0] /= mag;
			direcao[2] /= mag;
		}

		float novaX = camera.posicao[0] + direcao[0] * -camera.velocidade;
		float novaZ = camera.posicao[2] + direcao[2] * -camera.velocidade;

		float[] pos = mundo.verificarColisao(camera, novaX, camera.posicao[1], novaZ);
		camera.posicao[0] = pos[0];
		camera.posicao[2] = pos[2];
	}

	public void moverDireita() {
		float[] vetorDireito = {
			camera.foco[2] * camera.up[1] - camera.foco[1] * camera.up[2],
			camera.foco[0] * camera.up[2] - camera.foco[2] * camera.up[0],
			camera.foco[1] * camera.up[0] - camera.foco[0] * camera.up[1]
		};

		float novaX = camera.posicao[0] + vetorDireito[0] * -camera.velocidade;
		float novaY = camera.posicao[1] + vetorDireito[1] * -camera.velocidade;
		float novaZ = camera.posicao[2] + vetorDireito[2] * -camera.velocidade;

		float[] posAjustada = mundo.verificarColisao(camera, novaX, novaY, novaZ);
		camera.posicao[0] = posAjustada[0];
		camera.posicao[1] = posAjustada[1];
		camera.posicao[2] = posAjustada[2];
	}

	public void moverEsquerda() {
		float[] vetorDireito = {
			camera.foco[2] * camera.up[1] - camera.foco[1] * camera.up[2],
			camera.foco[0] * camera.up[2] - camera.foco[2] * camera.up[0],
			camera.foco[1] * camera.up[0] - camera.foco[0] * camera.up[1]
		};

		float novaX = camera.posicao[0] + vetorDireito[0] * camera.velocidade;
		float novaY = camera.posicao[1] + vetorDireito[1] * camera.velocidade;
		float novaZ = camera.posicao[2] + vetorDireito[2] * camera.velocidade;

		float[] posAjustada = mundo.verificarColisao(camera, novaX, novaY, novaZ);
		camera.posicao[0] = posAjustada[0];
		camera.posicao[1] = posAjustada[1];
		camera.posicao[2] = posAjustada[2];
	}

    public void atualizarViewMatriz() {
        Matrix.setLookAtM(viewMatriz, 0,
                          camera.posicao[0], camera.posicao[1], camera.posicao[2],
                          camera.posicao[0] + camera.foco[0],
                          camera.posicao[1] + camera.foco[1],
                          camera.posicao[2] + camera.foco[2],
                          camera.up[0], camera.up[1], camera.up[2]);
    }
}

class ShaderUtils {
    public static int carregarShader(int tipo, String shaderCodigo) {
        int shader = GLES30.glCreateShader(tipo);
        GLES30.glShaderSource(shader, shaderCodigo);
        GLES30.glCompileShader(shader);
        return shader;
    }

    public static int criarPrograma(String shaderVerticesCodigo, String shaderFragmentoCodigo) {
        int verticesShader = carregarShader(GLES30.GL_VERTEX_SHADER, shaderVerticesCodigo);
        int fragmentoShader = carregarShader(GLES30.GL_FRAGMENT_SHADER, shaderFragmentoCodigo);

        int programa = GLES30.glCreateProgram();
        GLES30.glAttachShader(programa, verticesShader);
        GLES30.glAttachShader(programa, fragmentoShader);
        GLES30.glLinkProgram(programa);
        return programa;
    }

    public static FloatBuffer criarBufferFloat(float[] dados) {
        ByteBuffer bb = ByteBuffer.allocateDirect(dados.length * 4);
        bb.order(ByteOrder.nativeOrder());
        FloatBuffer fb = bb.asFloatBuffer();
        fb.put(dados);
        fb.position(0);
        return fb;
    }

    public static String lerShaderDoRaw(Context contexto, int resId) {
        try {
            InputStream is = contexto.getResources().openRawResource(resId);
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            is.close();
            return new String(buffer);
        } catch(Exception e) {
            return null;
        }
    }
}

class VBOGrupo {
    int texturaId;
    int vboId;
    int vertices;

    VBOGrupo(int texturaId, int vboId, int vertices) {
        this.texturaId = texturaId;
        this.vboId = vboId;
        this.vertices = vertices;
    }
}

class PeerlinNoise {
    public static float ruido(final float x, final float z, final int seed) {
        final float[] conta = new float[7];

        conta[0] = z * seed / 100f;
        conta[1] = x * seed / 50f;
        conta[2] = conta[0] * seed * 10f;
        conta[3] = conta[1] * z / 1f;
        conta[4] = x * conta[3] / conta[2];
        conta[5] = conta[4] / conta[1] * conta[0];
		conta[7] = conta[5] / 0.2f;
		
			Math.floor(conta[5]);
			conta[5] /= 2;

        return teste(conta);
    }

    public static float teste(float[] r) {
        float b = r[0] / r[1] * r[3] % r[5];
        b /= r[4] - r[7];
        return b;
    }
}

// gerado por IA
class PerlinNoise {
    private static final int[] p = new int[512];
    private static final int[] permutation = {
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
    // Vetor fixo de gradientes para 2D
    private static final float[][] GRADIENTS = {
        { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 },
        { 1, 1 }, { -1, 1 }, { 1, -1 }, { -1, -1 }
    };

    static {
        for (int i = 0; i < 256; i++) {
            p[i] = permutation[i];
            p[256 + i] = permutation[i];
        }
    }

    // Ruído Perlin 2D
    public static float ruido(float x, float z, int seed) {
        int X = ((int) x + seed) & 255;
        int Z = ((int) z + seed) & 255;
        float xf = x - (int) x;
        float zf = z - (int) z;

        float u = fade(xf);
        float v = fade(zf);

        int A = p[X] + Z, B = p[X + 1] + Z;

        float gradAA = grad(p[A], xf, zf);
        float gradBA = grad(p[B], xf - 1, zf);
        float gradAB = grad(p[A + 1], xf, zf - 1);
        float gradBB = grad(p[B + 1], xf - 1, zf - 1);

        float lerpX1 = lerp(u, gradAA, gradBA);
        float lerpX2 = lerp(u, gradAB, gradBB);
        return lerp(v, lerpX1, lerpX2);
    }

    private static float fade(float t) {
        return t * t * t * (t * (t * 6 - 15) + 10);
    }

    private static float lerp(float t, float a, float b) {
        return a + t * (b - a);
    }

    // Seleciona um vetor de gradiente fixo e retorna o produto escalar
    private static float grad(int hash, float x, float y) {
        int h = hash & 7; // Limita para 8 gradientes
        float[] g = GRADIENTS[h];
        return g[0] * x + g[1] * y;
    }
}

 class Comandos {

	private GLRender render;
	private EditText chat;

	public Comandos(GLRender render, EditText chat) {
		this.render = render;
		this.chat = chat;
	}

    public void executar(String comando) {
		try {
			// tp:
			if(comando.startsWith("/tp ")) {
				comando = comando.replace("/tp ", "");
				String[] tokens = comando.split(" ");

				float x = Float.parseFloat(tokens[0].replace("~", String.valueOf(render.camera.posicao[0])));
				float y = Float.parseFloat(tokens[1].replace("~", String.valueOf(render.camera.posicao[1])));
				float z = Float.parseFloat(tokens[2].replace("~", String.valueOf(render.camera.posicao[2])));
				render.camera.posicao[0] = x;
				render.camera.posicao[1] = y;
				render.camera.posicao[2] = z;

				comando = "jogador teleportado para X: "+x+", Y: "+y+", Z: "+z;
				// chunk:
			} else if(comando.startsWith("/chunk raio ")) {
				comando = comando.replace("/chunk raio ", "");

				render.mundo.RAIO_CARREGAMENTO = Integer.parseInt(comando);

				comando = "chunks ao redor do jogador: "+render.mundo.CHUNK_TAMANHO*render.mundo.RAIO_CARREGAMENTO;
			} else if(comando.startsWith("/chunk tamanho")) {
				comando = comando.replace("/chunk tamanho ", "");

				render.mundo.CHUNK_TAMANHO = Integer.parseInt(comando);

				comando = "tamanho de chunk definido para "+render.mundo.CHUNK_TAMANHO+" blocos de largura";
				// bloco:
			} else if(comando.startsWith("/bloco ")) {
				comando = comando.replace("/bloco ", "");
				String[] tokens = comando.trim().split(" ");

				if(tokens[3] != null) {
					float x = Float.parseFloat(tokens[0].replace("~", String.valueOf(render.camera.posicao[0])));
					float y = Float.parseFloat(tokens[1].replace("~", String.valueOf(render.camera.posicao[1])));
					float z = Float.parseFloat(tokens[2].replace("~", String.valueOf(render.camera.posicao[2])));
					// render.mundo.colocarBloco(x, y, z, tokens[3]);

					comando = "bloco "+tokens[3]+" adicionado na posição X: "+x+", Y: "+y+" Z: "+z;
				} else System.out.println("comando invalido");

				// player:
			} else if(comando.startsWith("/player mao")) {
				comando = comando.replace("/player mao ", "");
				
				render.camera.itemMao = comando;
			} else if(comando.startsWith("/player passo ")) {
				comando = comando.replace("/player passo ", "");
				render.camera.velocidade = Float.parseFloat(comando);
			} else if(comando.startsWith("/player peso ")) {
				comando = comando.replace("/player peso ", "");

				render.camera.peso = Float.parseFloat(comando);
			} else if(comando.startsWith("/player hitbox[0] ")) {
				comando = comando.replace("/player hitbox[0] ", "");

				render.camera.hitbox[0] = Float.parseFloat(comando);
			} else if(comando.startsWith("/player hitbox[1] ")) {
				comando = comando.replace("/player hitbox[1] ", "");

				render.camera.hitbox[1] = Float.parseFloat(comando);
				// debug:
			} else if(comando.startsWith("/debug hitbox ")) {
				comando = comando.replace("/debug hitbox ", "");

				if(comando.equals("0")) {
					render.debug = false;
					comando = "debug desativado";
				} else if(comando.equals("1")) {
					render.debug = true;
					comando = "debug ativado";
				}
			} else if(comando.startsWith("/seed")) {
				comando = "seed atual: "+render.mundo.seed;
			}
			System.out.println(comando);
		} catch(Exception e) {
			System.out.println("erro: " + e.getMessage());
			e.printStackTrace();
		}
    }
}

 class DPadView extends View {

    private Paint pincelBase, pincelPressio;
    private RectF btnCima, btnBaixo, btnEsquerda, btnDireita;
    private boolean cimaPressio, baixoPressio, esquerdaPressio, direitaPressio;
    private DPadAgenda agenda;

    public interface DPadAgenda {
        void quandoDirecaoPressio(int direcao);
        void quandoDirecaoAtivada(int direcao);
    }

    // direcoes usa bitmask para combinacoes
    public static final int DIR_CIMA = 1;
    public static final int DIR_BAIXO = 2;
    public static final int DIR_ESQUERDA = 4;
    public static final int DIR_DIREITA = 8;

    public DPadView(Context contexto, AttributeSet attrs) {
        super(contexto, attrs);
        iniciar();
    }

    private void iniciar() {
        pincelBase = new Paint();
        pincelBase.setColor(Color.argb(150, 255, 255, 255)); // semi transparente
        pincelBase.setStyle(Paint.Style.FILL);
        pincelBase.setAntiAlias(true);

        pincelPressio = new Paint();
        pincelPressio.setColor(Color.argb(200, 200, 200, 0)); // cor quando pressionado
        pincelPressio.setStyle(Paint.Style.FILL);
        pincelPressio.setAntiAlias(true);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        float centroX = h / 2f;
        float centroY = h / 2f;
        float btnTamanho = Math.min(w, h) * 0.16f;

        // posicionando os botoez
        btnCima = new RectF(centroX - btnTamanho, centroY - btnTamanho * 100, 
							centroX + btnTamanho, centroY - btnTamanho);
        btnBaixo = new RectF(centroX - btnTamanho, centroY + btnTamanho, 
							 centroX + btnTamanho, centroY + btnTamanho * 100);
        btnEsquerda = new RectF(centroX - btnTamanho * 100, centroY - btnTamanho, 
								centroX - btnTamanho, centroY + btnTamanho);
        btnDireita = new RectF(centroX + btnTamanho, centroY - btnTamanho, 
							   centroX + btnTamanho * 100, centroY + btnTamanho);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        int acao = e.getActionMasked();
        float x = e.getX();
        float y = e.getY();

        int direcao = 0;
        boolean pressio = (acao==MotionEvent.ACTION_DOWN || acao==MotionEvent.ACTION_MOVE);

        // verifica qual botao foi tocado
        cimaPressio = pressio && btnCima.contains(x, y);
        baixoPressio = pressio && btnBaixo.contains(x, y);
        esquerdaPressio = pressio && btnEsquerda.contains(x, y);
        direitaPressio = pressio && btnDireita.contains(x, y);

        // montar bitmask de direcoes
        if(cimaPressio) direcao |= DIR_CIMA;
        if(baixoPressio) direcao |= DIR_BAIXO;
        if(esquerdaPressio) direcao |= DIR_ESQUERDA;
        if(direitaPressio) direcao |= DIR_DIREITA;

        // notifica listener
        if(agenda != null) {
            if(pressio) {
                agenda.quandoDirecaoPressio(direcao);
            } else {
                agenda.quandoDirecaoAtivada(direcao);
            }
        }

        invalidate();
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // desenhar botões
        renderBotao(canvas, btnCima, cimaPressio);
        renderBotao(canvas, btnBaixo, baixoPressio);
        renderBotao(canvas, btnEsquerda, esquerdaPressio);
        renderBotao(canvas, btnDireita, direitaPressio);
    }

    private void renderBotao(Canvas canvas, RectF rect, boolean pressio) {
        canvas.drawRoundRect(rect, 15, 15, pressio ? pincelPressio : pincelBase);
    }

    public void definirDPadAgenda(DPadAgenda agenda) {
        this.agenda = agenda;
    }

	// controles:

}

class Player extends Camera {
	public int vida = 20;
	public float velocidade = 0.2f;
	public float salto = 0.005f;

	public float peso = 2f;
	public float pesoTotal = 0.0005f;

	public String itemMao = "AR";
	public float alcance = 7f;
	
	public ArrayList<Slot> inventario = new ArrayList<>();

	public Player() {
		super();
		hitbox[0] = 1.4f;
		hitbox[1] = 0.5f;
		
		for(int i = 0; i<1; i++) {
			this.inventario.add(new Slot());
		}
	}
	
	class Slot {
		public String tipo;
		public int quant;
	}
}

class Camera {
    public float[] posicao = new float[3]; // [x, y, z]
    public float[] foco = new float[3];
    public float[] up = new float[3];

	public float[] hitbox = new float[2];

    public float yaw = -90f;
    public float tom = 0f;

    public Camera() {
        // posicao inicial
        posicao[0] = 8f;   // x
        posicao[1] = 40f;  // y
        posicao[2] = 8f;   // z

        // direcao inicial(ponto de foco)
        foco[0] = 0f;
        foco[1] = 0f;
        foco[2] = -1f;

        // vetor up
        up[0] = 0f;
        up[1] = 1f;
        up[2] = 0f;

		hitbox[0] = 0.5f;
		hitbox[1] = 0.5f;
    }

    public void rotacionar(float dx, float dy) {
        // rotacao invertida propositalmente para rotacao certa:
        yaw += dx;
        tom -= dy;

        if(tom > 89f) tom = 89f;
        if(tom < -89f) tom = -89f;

        foco[0] = (float)(Math.cos(Math.toRadians(yaw)) * (float)Math.cos(Math.toRadians(tom)));
        foco[1] = (float)Math.sin(Math.toRadians(tom));
        foco[2] = (float)(Math.sin(Math.toRadians(yaw)) * (float)Math.cos(Math.toRadians(tom)));
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

        // controle invertido para os controles certos
        posicao[0] -= direita[0] * velocidade;
        posicao[2] -= direita[2] * velocidade;
    }

    private void normalize(float[] vec) {
        float tamanho = (float)Math.sqrt(vec[0]*vec[0] + vec[1]*vec[1] + vec[2]*vec[2]);
        if(tamanho==0f) return;
        vec[0] /= tamanho;
        vec[1] /= tamanho;
        vec[2] /= tamanho;
    }
}
