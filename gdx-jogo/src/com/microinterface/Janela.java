package com.microinterface;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class Janela extends Componente {
    public PainelFatiado visual;
    public float escala;

    public Janela(PainelFatiado visual, float x, float y, float l, float a, float escala) {
        super(x, y, l, a);
        this.visual = visual;
        this.escala = escala;
    }

    @Override
    public boolean aoTocar(float toqueX, float toqueY, boolean pressionado) {
        // verifica filhos primeiro (de trás para frente para respeitar ordem de desenho)
        for(int i = filhos.size() - 1; i >= 0; i--) {
            Componente filho = filhos.get(i);
            // transforma as coordenadas para o sistema local da janela
            if(filho.aoTocar(toqueX - x, toqueY - y, pressionado)) {
                return true; // filho consumiu o toque
            }
        }
        // se nenhum filho consumiu e o toque está nesta janela
        return contem(toqueX, toqueY);
    }

    @Override
    public void desenhar(SpriteBatch pincel, float delta, float paiX, float paiY) {
        // desenha o fundo usando logica de fatiamento
        visual.desenhar(pincel, x, y, largura, altura, escala);

        // passa o tamanho da janela para os filhos(como texto) saberem onde se alinhar
        for(Componente filho : filhos) {
            filho.largura = this.largura;
            filho.altura = this.altura;
            filho.desenhar(pincel, delta, this.x, this.y);
        }
    }
}

