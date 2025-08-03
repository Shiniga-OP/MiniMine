package com.minimine;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;
import org.luaj.vm2.lib.jse.JsePlatform;
import com.minimine.engine.Comandos;
import com.engine.Logs;
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
import com.engine.Audio;
import com.engine.Toque;
import android.view.KeyEvent;
import com.engine.GL;
import com.engine.Sistema;

public class MundoActivity extends Activity {
    public GLSurfaceView tela;
    public GLRender render;
    public TextView coordenadas;
    public EditText chat, console;
	public Comandos comandos;
	public Runtime rt;
	public Handler atualizador = new Handler();
	public Runnable tarefaDebug;
	public Globals globals = JsePlatform.standardGlobals(); 
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_jogo);
		
		Logs.capturar();
		Sistema.capturarFPS();
        coordenadas = findViewById(R.id.coordenadas);
        chat = findViewById(R.id.chat);
        console = findViewById(R.id.logs);
        tela = findViewById(R.id.tela);

		Intent cache = getIntent();
		int seed = cache.getIntExtra("seed", 12345);
		String nome = cache.getStringExtra("nomeMundo");
		String tipoMundo = cache.getStringExtra("tipoMundo");
		String pacoteTex = cache.getStringExtra("pacoteTex");

		console.setText(String.valueOf(seed));
        render = new GLRender(this, tela, seed, (nome == null) ? "novo mundo" : nome, (tipoMundo == null) ? "normal" : tipoMundo, "texturas/".concat((pacoteTex == null) ? "evolva" : pacoteTex)+"/");
		GL.definirRender(tela, render);
		comandos =  new Comandos(render, chat);
		
		tarefaDebug = new Runnable() {
			public void run() {
				String posicao = "X: "+render.player.pos[0]+
					", Y: "+render.player.pos[1]+
					", Z: "+render.player.pos[2];
				if(render.debug) {
					String debug =
						"memória livre: " + String.format("%.2f", render.livre) + " MB" +
						"\nmemória total: " + String.format("%.2f", render.total) + " MB" +
						"\nmemória usada: " + String.format("%.2f", render.usado) + " MB" +
						"\n\nFPS: "+Sistema.fps+
						"\n\nchunks ativas: " + render.mundo.chunksAtivos.size() +
						"\nchunks modificados: " + render.mundo.chunksModificados.size() +
						"\nchunks por vez: " + render.chunksPorVez +
						"\nraio de carregamento: "+render.mundo.RAIO_CARREGAMENTO+
						"\n\nnome do mundo: " + render.mundo.nome +
						"\nseed atual: " + render.seed +
						"\n\ntempo: " + render.tempo +
						"\nciclo diario: " + render.ciclo +
						"\n\nplayer atual:\nposicao: " + posicao +
						"\nmão: "+render.player.itemMao+
						"\nhitbox: vertical: " + render.player.hitbox[0] +
						", horizontal: " + render.player.hitbox[1] +
						"\nslots: " + render.player.inventario.size();
					coordenadas.setText(debug);
				} else coordenadas.setText(posicao);
				atualizador.postDelayed(this, 200);
			}
		};
		atualizador.post(tarefaDebug);
		
		LuaValue luaComandos = CoerceJavaToLua.coerce(comandos);
		LuaValue luaRender = CoerceJavaToLua.coerce(render);
		globals.set("render", luaRender);
		globals.set("comandos", luaComandos);
		chat.setOnKeyListener(new View.OnKeyListener() {
				public boolean onKey(View v, int keyCode, KeyEvent event) {
					if((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
						try {
							if(chat.getText().toString().startsWith("/")) {
								comandos.executar(chat.getText().toString());
								chat.setText("");
							} else if(!chat.getText().toString().equals("")) globals.load(chat.getText().toString(), "script").call();
							console.setText(Logs.exibir());
						} catch(Exception ee) {
							System.out.println("erro: "+ee);
							System.out.println(Logs.exibir());
						}
						return true;
					}
					return false;
				}
			});
	}

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        Toque.cameraOlhar(render.player.camera, e);
		for(int i = 0; i < render.ui.botoes.size(); i++) render.ui.botoes.get(i).verificarToque(e);
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
		Audio.pararMusicas();
		render.svMundo(render.mundo);
		render.destruir();
	}
}
/*
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
} */
