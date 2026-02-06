package com.microinterface;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/**
 * painel é um container que pode ter visual(PainelFatiado) ou ser transparente
 * e suporta ancoragem de filhos
 */
public class Painel extends Componente {
    public PainelFatiado visual;
    public float escala;
    public boolean temFundo;
    public Color corFundo;

    // espaçamento interno
    public float paddingEsquerda = 0;
    public float paddingDireita = 0;
    public float paddingSuperior = 0;
    public float paddingInferior = 0;

    // cria painel com fundo visual
    public Painel(PainelFatiado visual, float x, float y, float largura, float altura, float escala) {
        super(x, y, largura, altura);
        this.visual = visual;
        this.escala = escala;
        this.temFundo = true;
        this.corFundo = Color.WHITE;
    }

    // cria painel transparente(sem fundo)
    public Painel(float x, float y, float largura, float altura) {
        super(x, y, largura, altura);
        this.temFundo = false;
    }

    // define espaçamento interno
    public void defEspaco(float todos) {
        this.paddingEsquerda = todos;
        this.paddingDireita = todos;
        this.paddingSuperior = todos;
        this.paddingInferior = todos;
    }

    public void defEspaco(float horizontal, float vertical) {
        this.paddingEsquerda = horizontal;
        this.paddingDireita = horizontal;
        this.paddingSuperior = vertical;
        this.paddingInferior = vertical;
    }

    // adiciona filho com ancoragem
    public void addAncorado(Componente filho, Ancoragem ancoragem, float margemX, float margemY) {
        // calcula área disponível considerando padding
        float larguraDisponivel = largura - paddingEsquerda - paddingDireita;
        float alturaDisponivel = altura - paddingSuperior - paddingInferior;

        // calcula posição baseada na ancoragem
        filho.x = ancoragem.calcularX(larguraDisponivel, filho.largura, margemX) + paddingEsquerda;
        filho.y = ancoragem.calcularY(alturaDisponivel, filho.altura, margemY) + paddingInferior;

        filhos.add(filho);
    }

    @Override
    public boolean aoTocar(float toqueX, float toqueY, boolean pressionado) {
        // verifica filhos primeiro(de tras para frente pra respeitar ordem de desenho)
        for(int i = filhos.size() - 1; i >= 0; i--) {
            Componente filho = filhos.get(i);

            if(filho.aoTocar(toqueX - x, toqueY - y, pressionado)) {
                return true; // filho consumiu o toque
            }
        }
        // se nenhum filho consumiu e o toque ta neste painel
        return contem(toqueX, toqueY);
    }

    @Override
    public void desenhar(SpriteBatch pincel, float delta, float paiX, float paiY) {
        float desenharX = paiX + x;
        float desenharY = paiY + y;

        // desenha fundo se tiver
        if(temFundo && visual != null) {
            Color corOriginal = pincel.getColor();
            pincel.setColor(corFundo);
            visual.desenhar(pincel, desenharX, desenharY, largura, altura, escala);
            pincel.setColor(corOriginal);
        }
        desenharFilhos(pincel, delta, desenharX, desenharY);
    }
}

