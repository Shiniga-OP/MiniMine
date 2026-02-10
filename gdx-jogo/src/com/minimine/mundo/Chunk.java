package com.minimine.mundo;

public final class Chunk {
    public int bitsPorBloco = 4; // 1..8 (modo direto)
    public int blocosPorInt = 32 / this.bitsPorBloco; // = 32 / bitsPorBloco ou 32 / paletaBits
    public int[] blocos; // buffer com dados empacotados(indices de paleta ou ids diretos)
    public final byte[] luz = new byte[16*255*16];
    public volatile com.badlogic.gdx.graphics.Mesh malha;
    public int x, z, maxIds = 8;
    public int paletaTam = 0;    // quantas entradas existem
    public int paletaBits = 1; // bits para indice da paleta(1..8)
	public int[] paleta = new int[1 << this.paletaBits]; // array de valores reais(ids de blocos)
	public boolean usaPaleta = true; // controla se estamos no modo paleta
	public volatile boolean fazendo = false;
	public volatile boolean att = false;
	public volatile boolean luzSuja = true;
	public volatile boolean dadosProntos = false;
	public int contaSolida = 0;
    public int contaTransp = 0;
}
