package com.microinterface;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class ItemBotao extends Componente {
    public Rotulo rotulo;
    public Acao acao;
    public Texture pixelBranco;
    public boolean pressionado = false;
    public Color corNormal = new Color(0.3f, 0.4f, 0.5f, 1f);
    public Color corPressionado = new Color(0.4f, 0.5f, 0.6f, 1f);

    public ItemBotao(float x, float y, float largura, float altura,
	String texto, BitmapFont fonte, float escala,
	Texture pixelBranco, Acao acao) {
        super(x, y, largura, altura);
        this.pixelBranco = pixelBranco;
        this.acao = acao;
        this.rotulo = new Rotulo(texto, fonte, escala);
        this.rotulo.largura = largura;
        this.rotulo.altura = altura;
    }

    public boolean aoTocar(float toqueX, float toqueY, boolean pressionadoAgora) {
        if(contem(toqueX, toqueY)) {
            if(pressionadoAgora) {
                pressionado = true;
            } else {
                if(pressionado && acao != null) {
                    acao.exec();
                }
                pressionado = false;
            }
            return true;
        }
        pressionado = false;
        return false;
    }

    public void desenhar(SpriteBatch pincel, float delta, float paiX, float paiY) {
        float desenharX = paiX + x;
        float desenharY = paiY + y;

        // desenha fundo
        Color cor = pressionado ? corPressionado : corNormal;
        pincel.setColor(cor);
        pincel.draw(pixelBranco, desenharX, desenharY, largura, altura);

        // desenha borda
        pincel.setColor(Color.LIGHT_GRAY);
        pincel.draw(pixelBranco, desenharX, desenharY, largura, 2);
        pincel.draw(pixelBranco, desenharX, desenharY + altura - 2, largura, 2);
        pincel.draw(pixelBranco, desenharX, desenharY, 2, altura);
        pincel.draw(pixelBranco, desenharX + largura - 2, desenharY, 2, altura);

        // desenha texto
        pincel.setColor(Color.WHITE);
        rotulo.desenhar(pincel, delta, desenharX, desenharY);
    }
}
