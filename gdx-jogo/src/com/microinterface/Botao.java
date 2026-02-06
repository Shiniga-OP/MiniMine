package com.microinterface;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class Botao extends Componente {
    public PainelFatiado visual;
    public Rotulo rotulo;
    public float escala;
    public Acao acaoClique;
    public boolean pressionado = false;

    public Botao(String texto, PainelFatiado visual, BitmapFont fonte, float x, float y, float l, float a, float escala, Acao acao) {
        super(x, y, l, a);
        this.visual = visual;
        this.escala = escala;
        this.acaoClique = acao;

        // o rotulo interno não precisa de coordenadas, ele se centraliza no botão
        this.rotulo = new Rotulo(texto, fonte, escala);
        this.rotulo.largura = l;
        this.rotulo.altura = a;
    }

    @Override
    public boolean aoTocar(float toqueX, float toqueY, boolean pressionado) {
        if(contem(toqueX, toqueY)) {
            if(pressionado) {
                this.pressionado = true;
            } else {
                // se soltou o mouse e tava pressionado antes, executa a ação
                if(this.pressionado && acaoClique != null) {
                    acaoClique.exec();
                }
                this.pressionado = false;
            }
            return true; // consome o clique
        }
		// se o mouse saiu de cima do botão, cancela o clique
        pressionado = false;
        return false;
    }

    @Override
    public void desenhar(SpriteBatch pincel, float delta, float paiX, float paiY) {
        // posição absoluta na tela
        float desenharX = paiX + x;
        float desenharY = paiY + y;

        // se pressionado, fica cinza escuro, senão, branco normal
        if(pressionado) {
            pincel.setColor(0.6f, 0.6f, 0.6f, 1f);
        } else {
            pincel.setColor(Color.WHITE);
        }
        visual.desenhar(pincel, desenharX, desenharY, largura, altura, escala);

        // retorna a cor pra branco para não afetar o texto
        pincel.setColor(Color.WHITE);

        // renderiza o texto centralizado no botão
        rotulo.desenhar(pincel, delta, desenharX, desenharY);
    }
	
	@Override
	public void liberar() {
		super.liberar();
		rotulo.liberar();
	}
}
