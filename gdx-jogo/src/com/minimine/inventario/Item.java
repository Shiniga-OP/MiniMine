package com.minimine.inventario;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class Item {
	public String nome;
	public TextureRegion textura;
	public int quantidade, dano = 1;
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
		ItemRegistro.registrar("espada_madeira", "espada_madeira").dano = 3;
		ItemRegistro.registrar("picareta_madeira", "picareta_madeira").mineracao = 0.2f;
		Item m = ItemRegistro.registrar("machado_madeira", "machado_madeira");
		m.dano = 2;
		m.mineracao = 0.15f;
	}
}
