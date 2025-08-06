package com.engine;

import android.graphics.Canvas;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.Paint;
import android.opengl.GLES30;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.opengl.GLUtils;
import android.graphics.PorterDuffColorFilter;
import java.util.Arrays;

public class Texto2D {
    public Objeto2D objeto;
    public int textura = -1;
    public String texto;
    public float x, y;
    public float tamanhoFonte;
    public float[] cor;

    public Texto2D(String texto, float x, float y, float tamanhoFonte, float... cor) {
        this.texto = (texto == "") ? "." : texto;
        this.x = x;
        this.y = y;
        this.tamanhoFonte = tamanhoFonte;
        this.cor = cor.clone();
        gerarRecursos();
    }

    private void gerarRecursos() {
        if(textura != -1) {
            GLES30.glDeleteTextures(1, new int[]{textura}, 0);
        }

        textura = criarTexturaBitmap();
        criarObjeto2D();
    }

    private int criarTexturaBitmap() {
        Paint pincel = new Paint();
        pincel.setTextSize(tamanhoFonte);
        pincel.setColor(Color.WHITE);
        pincel.setAntiAlias(true);
        pincel.setTextAlign(Paint.Align.LEFT);

        Rect limites = new Rect();
        pincel.getTextBounds(texto, 0, texto.length(), limites);
        float largura = pincel.measureText(texto);
        float altura = limites.height();

        Bitmap bitmap = Bitmap.createBitmap(
            (int) Math.ceil(largura), 
            (int) Math.ceil(altura), 
            Bitmap.Config.ARGB_8888
        );

        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        canvas.drawText(texto, 0, -limites.top, pincel);

        // Aplicar cor
        int rgb = Color.argb(
            (int)(cor[3] * 255), 
            (int)(cor[0] * 255), 
            (int)(cor[1] * 255), 
            (int)(cor[2] * 255)
        );
        bitmap = aplicarCor(bitmap, rgb);

        int[] texId = new int[1];
        GLES30.glGenTextures(1, texId, 0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texId[0]);
        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);

        bitmap.recycle();
        return texId[0];
    }

    private Bitmap aplicarCor(Bitmap original, int cor) {
        Bitmap resultado = Bitmap.createBitmap(
            original.getWidth(), 
            original.getHeight(), 
            Bitmap.Config.ARGB_8888
        );
        Canvas canvas = new Canvas(resultado);
        Paint pincel = new Paint();
        pincel.setColorFilter(new PorterDuffColorFilter(cor, PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(original, 0, 0, pincel);
        return resultado;
    }

    private void criarObjeto2D() {
        Paint pincel = new Paint();
        pincel.setTextSize(tamanhoFonte);
        Rect limites = new Rect();
        pincel.getTextBounds(texto, 0, texto.length(), limites);

        objeto = new Objeto2D(
            x, y, 
            pincel.measureText(texto), 
            limites.height(), 
            textura
        );
    }

    public void definirTex(String novoTexto) {
        if(!texto.equals(novoTexto)) {
            texto = novoTexto;
            gerarRecursos();
        }
    }

    public void definirCor(float[] novaCor) {
        if(!Arrays.equals(cor, novaCor)) {
            cor = novaCor.clone();
            gerarRecursos();
        }
    }

    public void definirTam(float novoTamanho) {
        if(tamanhoFonte != novoTamanho) {
            tamanhoFonte = novoTamanho;
            gerarRecursos();
        }
    }

    public void definirPosicao(float x, float y) {
        this.x = x;
        this.y = y;
        if(objeto != null) {
            objeto.x = x;
            objeto.y = y;
        }
    }

    public void destruir() {
        if(textura != -1) {
            GLES30.glDeleteTextures(1, new int[]{textura}, 0);
            textura = -1;
        }
    }
}
