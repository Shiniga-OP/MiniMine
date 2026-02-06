package com.microinterface;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class PainelFatiado {
    public TextureRegion[][] fatias;
    public int tamBase = 8; // o tamanho original no arquivo(8x8)

    public PainelFatiado(Texture textura) {
        fatias = TextureRegion.split(textura, tamBase, tamBase);
    }
	
	public PainelFatiado(Texture textura, int tamanhoBase) {
        fatias = TextureRegion.split(textura, tamBase, tamBase);
		tamBase = tamanhoBase;
    }

    public void desenhar(SpriteBatch pincel, float x, float y, float largura, float altura, float escala) {
        // tf = tamanho da fatia ajustado pela escala
        float tf = tamBase * escala; 

        // 1. cantos(proporcionais a escala)
        pincel.draw(fatias[0][0], x, y + altura - tf, tf, tf); // superior esquerdo
        pincel.draw(fatias[0][2], x + largura - tf, y + altura - tf, tf, tf); // superior direito
        pincel.draw(fatias[2][0], x, y, tf, tf); // inferior esquerdo
        pincel.draw(fatias[2][2], x + largura - tf, y, tf, tf); // inferior direito

        // 2. bordas(esticadas entre os cantos)
        pincel.draw(fatias[0][1], x + tf, y + altura - tf, largura - 2 * tf, tf); // cima
        pincel.draw(fatias[2][1], x + tf, y, largura - 2 * tf, tf); // baixo
        pincel.draw(fatias[1][0], x, y + tf, tf, altura - 2 * tf); // esquerda
        pincel.draw(fatias[1][2], x + largura - tf, y + tf, tf, altura - 2 * tf); // direita

        // 3. centro
        pincel.draw(fatias[1][1], x + tf, y + tf, largura - 2 * tf, altura - 2 * tf);
    }
}
