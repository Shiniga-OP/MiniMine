package com.minimine;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;
import org.luaj.vm2.lib.jse.JsePlatform;
import com.minimine.engine.Comandos;
import com.minimine.engine.Logs;
import com.minimine.engine.GLRender;
import com.minimine.engine.Player;
import com.engine.Camera3D;
import android.opengl.GLSurfaceView;
import android.widget.TextView;
import android.widget.EditText;
import android.os.Handler;
import android.os.Bundle;
import android.view.MotionEvent;
import android.graphics.Paint;
import android.view.View;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.graphics.Color;
import android.graphics.Canvas;
import android.util.DisplayMetrics;

public class MundoActivity extends Activity {
    public GLSurfaceView tela;
    public GLRender render;

    public TextView coordenadas;
    public EditText chat, console;

	public Comandos comandos;

	// public DPadView dpad;
	public Handler responsavel2 = new Handler();
	public Runnable movimentoTarefa;
	public Runtime rt;
	public Handler atualizadorMemoria = new Handler();
	public Runnable tarefaDebug;
	public Runnable tarefaFPS;

	public Globals globals = JsePlatform.standardGlobals(); 

	public int pontoAtivo = -1;
    public float ultimoX, ultimoY;
	
	public long tempoAnterior = System.nanoTime();
	public int frames = 0;
	public int fps = 0;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_jogo);
		
        coordenadas = findViewById(R.id.coordenadas);
		coordenadas.setTextSize(20);
        chat = findViewById(R.id.chat);

        console = findViewById(R.id.logs);

        Logs.capturar();

        tela = findViewById(R.id.tela);

		Intent dados = getIntent();
		int seed = dados.getIntExtra("seed", 3);
		String nome = dados.getStringExtra("nomeMundo");
		String tipoMundo = dados.getStringExtra("tipoMundo");
		String pacoteTex = dados.getStringExtra("pacoteTex");

		console.setText(String.valueOf(seed));
        tela.setEGLContextClientVersion(3);

        render = new GLRender(this, tela, seed, nome, tipoMundo, "texturas/"+pacoteTex+"/");
        tela.setRenderer(render);
        tela.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        render.camera.mover(0.5f);

		comandos =  new Comandos(render, chat);
		
		tarefaFPS = new Runnable() {
			public void run() {
				frames++;
				long agora = System.nanoTime();
				if(agora - tempoAnterior >= 1_000_000_000L) {
					fps = frames;
					frames = 0;
					tempoAnterior = agora;
				}
				atualizadorMemoria.postDelayed(this, 1); // 60 vezes por segundo
			}
		};
		
		tarefaDebug = new Runnable() {
			public void run() {
				String posicao = "X: "+render.camera.posicao[0]+
					", Y: "+render.camera.posicao[1]+
					", Z: "+render.camera.posicao[2];

				if(render.debug) {
					String debug =
						"memória livre: " + String.format("%.2f", render.livre) + " MB" +
						"\nmemória total: " + String.format("%.2f", render.total) + " MB" +
						"\nmemória usada: " + String.format("%.2f", render.usado) + " MB" +
						"\n\nFPS: "+fps+
						"\n\nchunks ativas: " + render.mundo.chunksAtivos.size() +
						"\nchunks modificados: " + render.mundo.chunksModificados.size() +
						"\nchunks por vez: " + render.chunksPorVez +
						"\nraio de carregamento: "+render.mundo.RAIO_CARREGAMENTO+
						"\n\nnome do mundo: " + render.mundo.nome +
						"\nseed atual: " + render.seed +
						"\n\ntempo: " + render.tempo +
						"\nciclo diario: " + render.ciclo +
						"\n\nplayer atual:\nposicao: " + posicao +
						"\nmão: "+render.camera.itemMao+
						"\nhitbox: vertical: " + render.camera.hitbox[0] +
						", horizontal: " + render.camera.hitbox[1] +
						"\nslots: " + render.camera.inventario.size();

					coordenadas.setText(debug);
				} else {
					coordenadas.setText(posicao);
				}
				atualizadorMemoria.postDelayed(this, 200);
			}
		};
		atualizadorMemoria.post(tarefaFPS);
		atualizadorMemoria.post(tarefaDebug);
		
		LuaValue luaComandos = CoerceJavaToLua.coerce(comandos);
		LuaValue luaRender = CoerceJavaToLua.coerce(render);

		globals.set("render", luaRender);
		globals.set("comandos", luaComandos);
	}
/*
	private class MovimentoTarefa implements Runnable {
		private final int direcao;

		public MovimentoTarefa(int direcao) {
			this.direcao = direcao;
		}

		@Override
		public void run() {
			try {
				if(chat.getText().toString().startsWith("/")) {
					comandos.executar(chat.getText().toString());
					chat.setText("");
				} else if(!chat.getText().toString().equals("")) {
					globals.load(chat.getText().toString(), "script").call();
					chat.setText("");
				}
				
				console.setText(Logs.exibir());
			} catch(Exception e) {
				System.out.println("erro: "+e);
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
*/
	public boolean eventoToque(MotionEvent e) {
		int acao = e.getActionMasked();
		int indice = e.getActionIndex();

		switch (acao) {
			case MotionEvent.ACTION_DOWN:
			case MotionEvent.ACTION_POINTER_DOWN:
				if (pontoAtivo == -1) {
					pontoAtivo = e.getPointerId(indice);
					ultimoX = e.getX(indice);
					ultimoY = e.getY(indice);
				}
				break;

			case MotionEvent.ACTION_MOVE:
				int i = e.findPointerIndex(pontoAtivo);
				if (i != -1) {
					float x = e.getX(i);
					float y = e.getY(i);
					float dx = x - ultimoX;
					float dy = y - ultimoY;
					render.camera.rotacionar(dx * 0.15f, dy * 0.15f);
					ultimoX = x;
					ultimoY = y;
				}
				break;

			case MotionEvent.ACTION_POINTER_UP:
				if (e.getPointerId(indice) == pontoAtivo) {
					for (int j = 0; j < e.getPointerCount(); j++) {
						if (j != indice) {
							pontoAtivo = e.getPointerId(j);
							ultimoX = e.getX(j);
							ultimoY = e.getY(j);
							break;
						}
					}
				}
				break;

			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_CANCEL:
				pontoAtivo = -1;
				break;
		}

		return pontoAtivo != -1;
	}

	public void pular(View v) {
		try {
			if(render.camera != null) {
				render.pular();
			} else {
				System.out.println("erro: a camera é null");
			}
		} catch(Exception e) {
			System.out.println("erro: "+e);
		}
	}

	public void colocarBloco(View v) {
		render.colocarBloco();
	}

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        eventoToque(e);
		for(int i = 0; i < render.ui.botoes.size(); i++) {
			render.ui.botoes.get(i).verificarToque(e);
		}
		return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        tela.onResume();
    }

	@Override
	protected void onDestroy() {
		super.onDestroy();
		render.svMundo(render.mundo);
		atualizadorMemoria.removeCallbacks(tarefaDebug);
		atualizadorMemoria.removeCallbacks(tarefaFPS);
		responsavel2.removeCallbacks(movimentoTarefa);
		render.limparTexturas();
		render.destruir();
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
