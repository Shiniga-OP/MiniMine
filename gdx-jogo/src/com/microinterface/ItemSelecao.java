package com.microinterface;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

// item de lista com indicador de selecao
public class ItemSelecao extends Componente {
    public Rotulo rotulo;
    public Acao acao;
    public Texture pixelBranco;
    public boolean pressionado = false;
    public boolean selecionado = false;
    public Color corNormal = new Color(0.25f, 0.25f, 0.3f, 1f);
    public Color corSelecionado = new Color(0.3f, 0.5f, 0.3f, 1f);
    public Color corPressionado = new Color(0.35f, 0.35f, 0.4f, 1f);

    public ItemSelecao(float x, float y, float largura, float altura,
	String texto, BitmapFont fonte, float escala,
	Texture pixelBranco, boolean selecionado, Acao acao) {
        super(x, y, largura, altura);
        this.pixelBranco = pixelBranco;
        this.selecionado = selecionado;
        this.acao = acao;
        this.rotulo = new Rotulo(texto, fonte, escala);
        this.rotulo.largura = largura - 30;
        this.rotulo.altura = altura;
        this.rotulo.x = 25;
    }

    public void defSelecionado(boolean selecionado) {
        this.selecionado = selecionado;
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
        Color cor = selecionado ? corSelecionado : (pressionado ? corPressionado : corNormal);
        pincel.setColor(cor);
        pincel.draw(pixelBranco, desenharX, desenharY, largura, altura);

        // desenha indicador(circulo)
        float circX = desenharX + 10;
        float circY = desenharY + altura / 2 - 6;

        // borda do circulo
        pincel.setColor(Color.LIGHT_GRAY);
        pincel.draw(pixelBranco, circX, circY, 12, 12);

        // centro do circulo(preenchido se selecionado)
        if(selecionado) {
            pincel.setColor(Color.WHITE);
            pincel.draw(pixelBranco, circX + 3, circY + 3, 6, 6);
        } else {
            pincel.setColor(cor);
            pincel.draw(pixelBranco, circX + 2, circY + 2, 8, 8);
        }
        // desenha borda do item
        pincel.setColor(new Color(0.4f, 0.4f, 0.4f, 1f));
        pincel.draw(pixelBranco, desenharX, desenharY + altura - 1, largura, 1);

        // desenha texto
        pincel.setColor(Color.WHITE);
        rotulo.desenhar(pincel, delta, desenharX, desenharY);
    }
}

