package com.minimine;

import android.os.Bundle;
import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import android.app.Activity;
import java.io.File;
import android.content.pm.ActivityInfo;
import android.os.Environment;
import android.os.Build;
import android.content.Intent;
import android.provider.Settings;
import android.net.Uri;
import android.Manifest;
import android.content.pm.PackageManager;

public class InicioActivity extends AndroidApplication {
    @Override
    public void onCreate(Bundle s) {
        super.onCreate(s);
		
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if(!Environment.isExternalStorageManager()) {
                Intent cache = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                cache.setData(Uri.parse("package:" + getPackageName()));
                startActivityForResult(cache, 1);
            }
        } else {
            if(checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            }
        }
        AndroidApplicationConfiguration cfg = new AndroidApplicationConfiguration();
        
        initialize(new Inicio(Environment.getExternalStorageDirectory().getAbsolutePath()), cfg);
    }
}
