package com.minimine.inventario;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class Item {
	public String nome;
	public TextureRegion textura;
	public int quantidade, dano = 1;
	public float mineracao = 0.1f;
	public String categoria = "natural";

	public Item(String nome, TextureRegion textura) {
		this.nome = nome;
		this.textura = textura;
	}
	
	public Item(String nome, TextureRegion textura, int quantidade) {
		this.nome = nome;
		this.textura = textura;
		this.quantidade = quantidade;
	}
	
	public Item dano(int dano) {
		this.dano = dano;
		return this;
	}
	
	public Item mineracao(float mineracao) {
		this.mineracao = mineracao;
		return this;
	}
	
	public Item categoria(String categoria) {
		this.categoria = categoria;
		return this;
	}
	
	public static void iniciar() {
		ItemRegistro.registrar("palito", "palito");
		
		ItemRegistro.registrar("espada_madeira", "espada_madeira")
		.dano(3).categoria("ferramenta");
		
		ItemRegistro.registrar("picareta_madeira", "picareta_madeira")
		.mineracao(0.2f).categoria("ferramenta");
		
		ItemRegistro.registrar("machado_madeira", "machado_madeira")
		.dano(2).mineracao(0.15f).categoria("ferramenta");
	}
}
