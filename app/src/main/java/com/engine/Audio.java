package com.engine;
import android.media.MediaPlayer;
import android.content.res.AssetFileDescriptor;
import android.content.Context;

public class Audio{
	public static MediaPlayer mp;
	
	public static void tocarMusica(Context ctx, String caminho) {
        try {
            AssetFileDescriptor afd = ctx.getAssets().openFd(caminho);
            mp = new MediaPlayer();
            mp.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            mp.setLooping(true);
            mp.prepare();
            mp.start();
        } catch(Exception e) {
            System.out.println("erro: "+e);
        }
    }

	public static void tocarMusica(Context ctx, String caminho, boolean loop) {
        try {
            AssetFileDescriptor afd = ctx.getAssets().openFd(caminho);
            mp = new MediaPlayer();
            mp.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            mp.setLooping(loop);
            mp.prepare();
            mp.start();
        } catch(Exception e) {
            System.out.println("erro: "+e);
        }
    }

    public  static void pararMusica() {
        if(mp != null) {
            mp.stop();
            mp.release();
            mp = null;
        }
    }
}
