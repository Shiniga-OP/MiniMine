package com.minimine.engine;

import android.opengl.GLES30;
import java.nio.FloatBuffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import android.content.Context;
import java.io.InputStream;

public class ShaderUtils {
    public static int carregarShader(int tipo, String shaderCodigo) {
        int shader = GLES30.glCreateShader(tipo);
        GLES30.glShaderSource(shader, shaderCodigo);
        GLES30.glCompileShader(shader);
        return shader;
    }

    public static int criarPrograma(String shaderVerticesCodigo, String shaderFragmentoCodigo) {
        int verticesShader = carregarShader(GLES30.GL_VERTEX_SHADER, shaderVerticesCodigo);
        int fragmentoShader = carregarShader(GLES30.GL_FRAGMENT_SHADER, shaderFragmentoCodigo);

        int programa = GLES30.glCreateProgram();
        GLES30.glAttachShader(programa, verticesShader);
        GLES30.glAttachShader(programa, fragmentoShader);
        GLES30.glLinkProgram(programa);
        return programa;
    }

    public static FloatBuffer criarBufferFloat(float[] dados) {
        ByteBuffer bb = ByteBuffer.allocateDirect(dados.length * 4);
        bb.order(ByteOrder.nativeOrder());
        FloatBuffer fb = bb.asFloatBuffer();
        fb.put(dados);
        fb.position(0);
        return fb;
    }

    public static String lerShaderDoRaw(Context contexto, int resId) {
        try {
            InputStream is = contexto.getResources().openRawResource(resId);
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            is.close();
            return new String(buffer);
        } catch(Exception e) {
            return null;
        }
    }
}
