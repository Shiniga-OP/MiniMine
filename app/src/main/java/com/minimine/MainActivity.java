package com.minimine;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.content.Intent;
import android.content.res.AssetManager;
import android.content.Context;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.Manifest;
import android.net.Uri;
import android.content.pm.PackageManager;
import android.widget.Toast;

public class MainActivity extends Activity {

	private int PERMISSAO;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
		pedirPermissao();
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
}
