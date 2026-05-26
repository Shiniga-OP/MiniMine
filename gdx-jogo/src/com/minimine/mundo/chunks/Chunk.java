package com.minimine.mundo.chunks;

import com.minimine.mundo.Mundo;
import java.util.Arrays;

public final class Chunk {
    public volatile int bitsPorBloco = 4; // 1..8(modo direto)
    public volatile int blocosPorInt = 32 / this.bitsPorBloco; // = 32 / bitsPorBloco ou 32 / paletaBits
    public volatile int[] blocos; // buffer com dados empacotados(indices de paleta ou ids diretos)
    public volatile byte[] luz = new byte[Mundo.TAM_CHUNK * Mundo.Y_CHUNK * Mundo.TAM_CHUNK];
	// metadados dos blocos:
	public volatile short[] meta = new short[Mundo.TAM_CHUNK * Mundo.Y_CHUNK * Mundo.TAM_CHUNK];
    public volatile int vboId = 0; // VBO de vertices na GPU
    public volatile int iboId = 0; // IBO de indices na GPU
	public volatile int iboTranspId = 0;
    public volatile boolean gpuPronta = false; // true quando vboId/iboId são validos
    public volatile int x, z, estado = 0, maxIds = 8;
	public volatile long chave;
    public volatile int paletaTam = 0; // quantas entradas existem
    public volatile int paletaBits = 1; // bits para indice da paleta(1..8)
	public volatile int[] paleta = new int[1 << this.paletaBits]; // array de valores reais(ids de blocos)
	public volatile boolean usaPaleta = true; // controla se estamos no modo paleta
	public volatile boolean fazendo = false;
	public volatile boolean att = false;
	public volatile boolean luzFazendo = false;
	public volatile boolean luzSuja = true;
	public volatile boolean dadosProntos = false;
	public volatile int contaSolida = 0;
    public volatile int contaTransp = 0;

	public static void zerar(Chunk c) {
		c.bitsPorBloco = 4;
		c.blocosPorInt = 32 / c.bitsPorBloco;
		if(c.blocos != null) Arrays.fill(c.blocos, 0);
		Arrays.fill(c.luz, (byte)0);
		Arrays.fill(c.meta, (short)0);
		c.vboId = 0;
		c.iboId = 0;
		c.iboTranspId = 0;
		c.gpuPronta = false;
		c.x = 0;
		c.z = 0;
		c.maxIds = 8;
		c.chave = 0;
		c.paletaTam = 0;
		c.paletaBits = 1;
		if(c.paleta != null) Arrays.fill(c.paleta, 0);
		c.usaPaleta = true;
		c.fazendo = false;
		c.att = false;
		c.luzFazendo = false;
		c.luzSuja = true;
		c.dadosProntos = false;
		c.contaSolida = 0;
		c.contaTransp = 0;
		c.estado = 0;
	}
}
