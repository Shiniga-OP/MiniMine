package com.engine;

import android.os.Handler;
import android.os.Looper;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.content.Intent;
import android.net.Uri;
import android.Manifest;
import android.content.pm.PackageManager;
import android.app.Activity;
import android.view.Choreographer;
import android.widget.Toast;

public class Sistema{ 
	public static int fps = 0;
	public static int frames = 0;
	public static long tempo = System.currentTimeMillis();
	public static Activity ativ;
	
	public static final Choreographer.FrameCallback fpsTarefa = new Choreographer.FrameCallback() {
		public void doFrame(long frameTimeNanos) {
			frames++;
			long agora = System.currentTimeMillis();
			if(agora - tempo >= 1000) {
				fps = frames;
				frames = 0;
				tempo = agora;
			}
			Choreographer.getInstance().postFrameCallback(this);
		}
	};
	
	public static void defActivity(Activity ctx) {
		ativ = ctx;
	}

	public static void msg(String msg) {
		if(ativ != null) Toast.makeText(ativ, msg, Toast.LENGTH_SHORT).show();
		else System.out.println("erro: você não definiu a activity com Sistema.defActivity(contexto);");
	}

	public static void capturarFPS() {
		Choreographer.getInstance().postFrameCallback(fpsTarefa);
	}
	
	public static void pedirArmazTotal(Activity ctx) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if(!Environment.isExternalStorageManager()) {
                Intent cache = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                cache.setData(Uri.parse("package:" + ctx.getPackageName()));
                ctx.startActivityForResult(cache, 1);
            }
        } else {
            if(ctx.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ctx.requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            }
        }
    }
}
