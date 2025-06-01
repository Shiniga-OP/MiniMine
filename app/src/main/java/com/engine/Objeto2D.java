package com.engine;
import android.content.Context;

public class Objeto2D {
    public float x, y, largura, altura;
    public int textura;

    public Objeto2D(float x, float y, float largura, float altura, int textura) {
		this.x = x;
        this.y = y;
        this.largura = largura;
        this.altura = altura;
        this.textura = (textura == -1) ? Texturas.texturaBranca() : textura;
	}
	
	public boolean tocado(float x, float y) {
		if(
			x >= this.x &&
			x <= this.x + this.largura &&
			y >= this.y &&
			y <= this.y + this.altura
			) {
			return true;
		} else {
			return false;
		}
	}
}
