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

public class MainActivity extends Activity {
	public int PERMISSAO;
	public GLSurfaceView tela;
	public GLRender render;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
		
		pedirPermissao();
		
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
		Audio.pararMusicas();
		render.limparTexturas();
		render.destruir();
	}
}
