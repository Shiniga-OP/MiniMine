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
        
        initialize(new Inicio(getExternalMediaDirs()[0].getAbsolutePath()), new AndroidApplicationConfiguration());
    }
}
