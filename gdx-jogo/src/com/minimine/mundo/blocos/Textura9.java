package com.minimine.mundo.blocos;

public class Textura9 {
	public String topo, sul, norte, leste, oeste, base;
	
	public Textura9 def(String textura) {
		topo(textura);
		lados(textura);
		return this;
	}
	
	public Textura9 topo(String topo) {
		this.topo = topo;
		this.base = topo;
		return this;
	}
	
	public Textura9 base(String base) {
		this.base = base;
		return this;
	}
	
	public Textura9 frente(String frente) {
		this.sul = frente;
		return this;
	}
	
	public Textura9 tras(String tras) {
		this.norte = tras;
		return this;
	}
	
	public Textura9 lados(String lados) {
		this.sul = lados;
		this.norte = lados;
		this.leste = lados;
		this.oeste = lados;
		return this;
	}
}
