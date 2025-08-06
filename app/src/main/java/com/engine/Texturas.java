package com.engine;

import android.opengl.GLES30;
import java.nio.ByteBuffer;
import android.content.Context;
import java.io.InputStream;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap;
import android.opengl.GLUtils;

public class Texturas {
	public static int carregarAsset(Context ctx, String nomeArquivo) {
		try {
			Bitmap bmp = ArmUtils.lerImgAssets(ctx, nomeArquivo);
			int[] texID = new int[1];
			GLES30.glGenTextures(1, texID, 0);
			GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texID[0]);

			GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bmp, 0);

			GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST);
			GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_NEAREST);
			
			bmp.recycle();
			return texID[0];
		} catch(Exception e) {
			System.out.println("erro ao carregar textura: " + nomeArquivo + e.getMessage());
			return -1;
		}
	}

	public static int texturaBranca() {
        int[] tex = new int[1];
        GLES30.glGenTextures(1, tex, 0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, tex[0]);
        byte[] branco = {(byte)255, (byte)255, (byte)255, (byte)255};
        ByteBuffer buffer = ByteBuffer.allocateDirect(4);
        buffer.put(branco).position(0);
        GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA, 1, 1, 0,  GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, buffer);

        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_NEAREST);

        return tex[0];
    }

	public static int texturaCor(float... c) {
		int[] tex = new int[1];
		GLES30.glGenTextures(1, tex, 0);
		GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, tex[0]);

		byte[] cor = {
			(byte)(c[0] * 255), (byte)(c[1] * 255), (byte)(c[2] * 255), (byte)(c[3] * 255)
		};

		ByteBuffer buffer = ByteBuffer.allocateDirect(4);
		buffer.put(cor).position(0);

		GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA, 1, 1, 0,  
							GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, buffer);

		GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST);
		GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_NEAREST);
		return tex[0];
	}
}
