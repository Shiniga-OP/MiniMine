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

public class MainActivity extends Activity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
		requestPermissions(new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
    }
	
	public void paraMundo(View v) {
		Intent intent = new Intent(this, InicioActivity.class);
		startActivity(intent);
	}
}
