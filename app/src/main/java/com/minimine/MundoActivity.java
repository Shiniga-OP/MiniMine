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
import com.minimine.engine.Camera;
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

public class MundoActivity extends Activity {
    public GLSurfaceView tela;
    public GLRender render;

    public TextView coordenadas;
    public EditText chat, console;

    public Logs log;

	public static double livre, total, usado;
	public static String gc;

	public Comandos comandos;

	public DPadView dpad;
	public Handler responsavel2 = new Handler();
	public Runnable movimentoTarefa;
	public Runtime rt;
	public Handler atualizadorMemoria = new Handler();
	public Runnable tarefaMemoria;
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

        log = new Logs();
        log.capturar();

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

		comandos =  new Comandos(log, render, chat);

		dpad = findViewById(R.id.dpad);

		rt = Runtime.getRuntime();
		
		tarefaFPS = new Runnable() {
			public void run() {
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

		Runnable tarefaMemoria = new Runnable() {
			public void run() {
				livre = rt.freeMemory() / 1048576.0;
				total = rt.totalMemory() / 1048576.0;
				usado = total - livre;

				String posicao = "X: "+render.camera.posicao[0]+
					", Y: "+render.camera.posicao[1]+
					", Z: "+render.camera.posicao[2];

				if(render.debug) {
					String debug =
						"memória livre: " + String.format("%.2f", livre) + " MB" +
						"\nmemória total: " + String.format("%.2f", total) + " MB" +
						"\nmemória usada: " + String.format("%.2f", usado) + " MB" +
						"\n"+gc+
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
		atualizadorMemoria.post(tarefaMemoria);
		
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
		LuaValue luaComandos = CoerceJavaToLua.coerce(comandos);
		LuaValue luaRender = CoerceJavaToLua.coerce(render);

		globals.set("render", luaRender);
		globals.set("comandos", luaComandos);
	}

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

				if(log.ativo == true) {
					console.setText(log.exibir());
				}
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
		render.slot1.verificarToque(e);
		render.slot2.verificarToque(e);
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
		atualizadorMemoria.removeCallbacks(tarefaMemoria);
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

		boolean tocou = false;
		boolean pressio = (acao == MotionEvent.ACTION_DOWN || acao == MotionEvent.ACTION_MOVE);

		cimaPressio = pressio && btnCima.contains(x, y); if (cimaPressio) tocou = true;
		baixoPressio = pressio && btnBaixo.contains(x, y); if (baixoPressio) tocou = true;
		esquerdaPressio = pressio && btnEsquerda.contains(x, y); if (esquerdaPressio) tocou = true;
		direitaPressio = pressio && btnDireita.contains(x, y); if (direitaPressio) tocou = true;

		int direcao = 0;
		if (cimaPressio) direcao |= DIR_CIMA;
		if (baixoPressio) direcao |= DIR_BAIXO;
		if (esquerdaPressio) direcao |= DIR_ESQUERDA;
		if (direitaPressio) direcao |= DIR_DIREITA;

		if (agenda != null) {
			if (pressio) agenda.quandoDirecaoPressio(direcao);
			else agenda.quandoDirecaoAtivada(direcao);
		}

		invalidate();
		return tocou;
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
}

