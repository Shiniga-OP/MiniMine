package com.minimine.mundo;

public final class Chunk {
    public volatile int bitsPorBloco = 4; // 1..8(modo direto)
    public volatile int blocosPorInt = 32 / this.bitsPorBloco; // = 32 / bitsPorBloco ou 32 / paletaBits
    public volatile int[] blocos; // buffer com dados empacotados(indices de paleta ou ids diretos)
    public volatile byte[] luz = new byte[Mundo.TAM_CHUNK * Mundo.Y_CHUNK * Mundo.TAM_CHUNK];
	// metadados dos blocos:
	public volatile byte[] meta = new byte[Mundo.TAM_CHUNK * Mundo.Y_CHUNK * Mundo.TAM_CHUNK];
    public volatile com.badlogic.gdx.graphics.Mesh malha;
    public volatile int x, z, maxIds = 8;
    public volatile int paletaTam = 0;    // quantas entradas existem
    public volatile int paletaBits = 1; // bits para indice da paleta(1..8)
	public volatile int[] paleta = new int[1 << this.paletaBits]; // array de valores reais(ids de blocos)
	public volatile boolean usaPaleta = true; // controla se estamos no modo paleta
	public volatile boolean fazendo = false;
	public volatile boolean att = false;
	public volatile boolean luzFazendo = false;
	public volatile boolean luzSuja = true;
	public volatile boolean fluxoSujo = false;
	public volatile boolean dadosProntos = false;
	public volatile int contaSolida = 0;
    public volatile int contaTransp = 0;
}
