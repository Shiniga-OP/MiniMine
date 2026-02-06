package com.microinterface;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import java.util.ArrayList;

public abstract class Componente {
    public float x, y, largura, altura;
    protected ArrayList<Componente> filhos = new ArrayList<>();

    public Componente(float x, float y, float largura, float altura) {
        this.x = x;
        this.y = y;
        this.largura = largura;
        this.altura = altura;
    }
    public void add(Componente filho) {
        filhos.add(filho);
    }
    // verifica se uma coordenada(do mouse) ta dentro desse componente
    public boolean contem(float toqueX, float toqueY) {
        return toqueX >= x && toqueX <= x + largura &&
			toqueY >= y && toqueY <= y + altura;
    }
    // retorna sim se o componente consumiu o clique
    public boolean aoTocar(float toqueX, float toqueY, boolean pressionado) {
        return false; 
    }
    // indica se este componente precisa receber eventos contínuos de arraste
    // sobrescreve esse metodo pra retornar true em componentes como PainelRolavel
    public boolean capturaArraste() {
        return false;
    }

    public abstract void desenhar(SpriteBatch pincel, float delta, float paiX, float paiY);

    protected void desenharFilhos(SpriteBatch pincel, float delta, float atualX, float atualY) {
        for(Componente filho : filhos) {
            filho.desenhar(pincel, delta, atualX, atualY);
        }
    }
	public void liberar() {
		filhos.clear();
	}
}

