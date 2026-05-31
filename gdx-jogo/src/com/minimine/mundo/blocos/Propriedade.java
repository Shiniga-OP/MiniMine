package com.minimine.mundo.blocos;

import com.minimine.graficos.TipoRender;

public class Propriedade {
	public TipoRender render = TipoRender.OPACO;
	public int luz, viscosidade = 1;
	public float densidade = 1f, dureza = 0f;
	public boolean colide = true;
	public BlocoModelo modelo = new BlocoCubo();
	
	public Propriedade render(TipoRender render) {
		this.render = render;
		return this;
	}
	
	public Propriedade modeloX() {
		this.modelo = new BlocoX();
		return this;
	}
	
	public Propriedade liquido(float densidade, int viscosidade) {
		this.densidade = densidade;
		this.viscosidade = viscosidade;
		return this;
	}
	
	public Propriedade emiteLuz(int nivel) {
		this.luz = nivel;
		return this;
	}
	
	public Propriedade colisao(boolean colide) {
		this.colide = colide;
		return this;
	}
	
	public Propriedade dureza(float dureza) {
		this.dureza = dureza;
		return this;
	}
}
