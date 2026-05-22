package com.minimine.inventario;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class Item {
	public String nome;
	public TextureRegion textura;
	public int quantidade;

	public Item(String nome, TextureRegion textura) {
		this.nome = nome;
		this.textura = textura;
	}
	
	public Item(String nome, TextureRegion textura, int quantidade) {
		this.nome = nome;
		this.textura = textura;
		this.quantidade = quantidade;
	}
}
