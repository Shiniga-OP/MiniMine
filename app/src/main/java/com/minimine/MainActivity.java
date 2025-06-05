package com.minimine;

import com.minimine.engine.GLRender;
import android.opengl.GLSurfaceView;
import android.app.Activity;
import android.os.Bundle;
import com.engine.Audio;
import android.view.View;
import android.content.Intent;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.net.Uri;
import android.Manifest;
import android.widget.Toast;
import android.content.pm.PackageManager;
import com.engine.Sistema;

public class MainActivity extends Activity {
	public int PERMISSAO;
	public GLSurfaceView tela;
	public GLRender render;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
		
		Sistema.pedirArmazTotal(this);
		
		tela = findViewById(R.id.tela);
		
		if(Math.random() < 0.5) {
			Audio.tocarMusica(this, "musicas/igor.m4a", false);
		} else {
			Audio.tocarMusica(this, "musicas/igor-2.m4a", false);
		}
		
		tela.setEGLContextClientVersion(3);
        render = new GLRender(this, tela, 77734, "novo mundo", "normal", "texturas/"+"evolva"+"/");
		render.UI = false;
		render.trava = false;
        tela.setRenderer(render);
        tela.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
	}
	
	public void iniciar() {
		
	}
	
	public void paraMundo(View v) {
		Intent intent = new Intent(this, InicioActivity.class);
		startActivity(intent);
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
		render.limparTexturas();
		render.destruir();
	}
}
