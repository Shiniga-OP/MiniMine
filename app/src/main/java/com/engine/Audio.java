package com.engine;

import android.media.MediaPlayer;
import android.content.res.AssetFileDescriptor;
import android.content.Context;
import java.util.List;
import java.util.ArrayList;

public class Audio {
    public static List<MediaPlayer> mps = new ArrayList<MediaPlayer>();

    public static MediaPlayer tocarMusica(Context ctx, String caminho) {
        return tocarMusica(ctx, caminho, true);
    }

    public static MediaPlayer tocarMusica(Context ctx, String caminho, boolean loop) {
        AssetFileDescriptor afd = null;
        try {
            afd = ctx.getAssets().openFd(caminho);
            MediaPlayer mp = new MediaPlayer();
            mp.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            mp.setLooping(loop);
            mp.prepare();
            mp.start();
            mps.add(mp);
            return mp;
        } catch(Exception e) {
            System.out.println("erro: " + e);
            return null;
        } finally {
            if(afd != null) {
                try {
                    afd.close();
                } catch(Exception e) {
                    System.out.println("erro: " + e);
                }
            }
        }
    }

    public static void pararMusicas() {
        for(int i = mps.size() - 1; i >= 0; i--) {
            MediaPlayer mp = mps.get(i);
            if(mp != null) {
                try {
                    mp.stop();
                } catch(Exception e) {
                    System.out.println("erro: " + e);
                }
                mp.release();
                mps.remove(i);
            }
        }
    }

    public static void pararMusica(MediaPlayer mp) {
        if(mp != null) {
            try {
                mp.stop();
            } catch(Exception e) {
                System.out.println("erro: " + e);
            }
            mp.release();
            mps.remove(mp);
        }
    }
	
	public static void attSom3D(Audio3D audio, Camera3D camera) {
		
	}
	
	public static float calcularDistancia(float x1, float y1, float z1, float x2, float y2, float z2) {
		float dx = x1 - x2;
		float dy = y1 - y2;
		float dz = z1 - z2;
		return (float) Math.sqrt(dx*dx + dy*dy + dz*dz);
	}
	
	public class Audio3D {
		public float[] posicao = new float[]{0f, 0f, 0f};
		
		public Audio3D(float... posicao) {
			this.posicao = posicao;
		}
	}
}
