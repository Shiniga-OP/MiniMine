package com.minimine;

import android.os.Bundle;
import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import android.app.Activity;
import java.io.File;
import android.content.pm.ActivityInfo;

public class MainActivity extends AndroidApplication {
	public static Activity ISSO;
	
    @Override
    public void onCreate(Bundle s) {
        super.onCreate(s);
		
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		
		try {
			java.lang.reflect.Method m = android.os.StrictMode.class.getMethod("disableDeathOnFileUriExposure");
			m.invoke(null);
		} catch(Exception e) {}
		
		Sistema.pedirArmazTotal(this);
		
		ISSO = this;
		
		File apk = new File(Sistema.externo, "MiniMine/tmp/MiniMine.apk");
		new File(Sistema.externo, "MiniMine/tmp/").mkdirs();
		if(apk.exists()) apk.delete();
        
        AndroidApplicationConfiguration cfg = new AndroidApplicationConfiguration();
        
        initialize(new Inicio(Sistema.externo, new DebugadorDoAndroid(), new InstaladorAndroid(this)), cfg);
    }
}
