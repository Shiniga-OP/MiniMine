package com.microinterface;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Array;

public class RotuloMultilinha extends Componente {
    public String texto;
    public BitmapFont fonte;
    public GlyphLayout medidor;
    public float escala;
    public Array<String> linhas;
    public float alturaLinha;

    public RotuloMultilinha(String texto, BitmapFont fonte, float escala) {
        super(0, 0, 0, 0);
        this.texto = texto;
        this.fonte = fonte;
        this.escala = escala;
        this.medidor = new GlyphLayout();
        this.linhas = new Array<String>();
    }

    public void quebrarTexto() {
        linhas.clear();

        if(largura <= 0) {
            linhas.add(texto);
            return;
        }
        fonte.getData().setScale(escala);

        String[] palavras = texto.split(" ");
        String linhaAtual = "";

        for(int i = 0; i < palavras.length; i++) {
            String teste = linhaAtual.isEmpty() ? palavras[i] : linhaAtual + " " + palavras[i];
            medidor.setText(fonte, teste);

            if(medidor.width <= largura - 20) {
                linhaAtual = teste;
            } else {
                if(!linhaAtual.isEmpty()) {
                    linhas.add(linhaAtual);
                }
                linhaAtual = palavras[i];
            }
        }
        if(!linhaAtual.isEmpty()) {
            linhas.add(linhaAtual);
        }
        medidor.setText(fonte, "A");
        alturaLinha = medidor.height;

        fonte.getData().setScale(1.0f);
    }

    public void desenhar(SpriteBatch pincel, float delta, float paiX, float paiY) {
        quebrarTexto();

        fonte.getData().setScale(escala);

        float totalAltura = linhas.size * alturaLinha * 1.2f;
        float posY = paiY + (altura / 2) + (totalAltura / 2);

        for(int i = 0; i < linhas.size; i++) {
            medidor.setText(fonte, linhas.get(i));
            float posX = paiX + (largura / 2) - (medidor.width / 2);
            fonte.draw(pincel, linhas.get(i), posX, posY);
            posY -= alturaLinha * 1.2f;
        }
        fonte.getData().setScale(1.0f);
    }
	
	@Override
	public void liberar() {
		super.liberar();
		linhas.clear();
		fonte.dispose();
	}
}
