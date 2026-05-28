package com.minimine.inventario;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class Item {
	public String nome;
	public TextureRegion textura;
	public int quantidade;
	public float mineracao = 0.1f;

	public Item(String nome, TextureRegion textura) {
		this.nome = nome;
		this.textura = textura;
	}
	
	public Item(String nome, TextureRegion textura, int quantidade) {
		this.nome = nome;
		this.textura = textura;
		this.quantidade = quantidade;
	}
	
	public static void iniciar() {
		ItemRegistro.registrar("palito", "palito");
		ItemRegistro.registrar("espada_madeira", "espada_madeira");
		ItemRegistro.registrar("picareta_madeira", "picareta_madeira").mineracao = 0.15f;
		ItemRegistro.registrar("machado_madeira", "machado_madeira").mineracao = 0.1f;
	}
}
