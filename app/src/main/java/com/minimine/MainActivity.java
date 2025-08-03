package com.minimine;

import com.minimine.engine.GLRender;
import android.opengl.GLSurfaceView;
import android.app.Activity;
import android.os.Bundle;
import com.engine.Audio;
import android.view.View;
import android.content.Intent;
import com.engine.Sistema;
import android.os.Handler;

public class MainActivity extends Activity {
	public GLSurfaceView tela;
	public GLRender render;
	public boolean dev = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
		
		Sistema.pedirArmazTotal(this);
		if(dev) {
			new Handler().postDelayed(new Runnable() {
				public void run() {
					paraMundo(null);
				}
			}, 1);
		} else {
			tela = findViewById(R.id.tela);

			if(Math.random() < 0.5) Audio.tocarMusica(this, "musicas/igor.m4a", false);
			else Audio.tocarMusica(this, "musicas/igor-2.m4a", false);

			tela.setEGLContextClientVersion(3);
			render = new GLRender(this, tela, 77734, "novo mundo", "normal", "texturas/evolva/");
			render.UI = false;
			render.trava = false;
			tela.setRenderer(render);
			tela.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
		}
	}
	
	public void paraMundo(View v) {
		if(dev) {
			Intent cache = new Intent(this, MundoActivity.class);
			cache.putExtra("dev", dev);
			startActivity(cache);
		} else {
			Intent cache = new Intent(this, InicioActivity.class);
			cache.putExtra("dev", dev);
			startActivity(cache);
		}
	}

    @Override
	protected void onResume() {
		super.onResume();
		if(tela != null) tela.onResume();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		Audio.pararMusicas();
		if(render != null) {
			render.destruir();
		}
	}
}
