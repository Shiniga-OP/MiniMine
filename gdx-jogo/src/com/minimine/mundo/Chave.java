package com.minimine.mundo;

public class Chave {
	public static final long gerar(final int x, final int z) {
		return ((long)x << 32) | (z & 0xFFFFFFFFL);
	}
	
	public static final long gerar(final int x, final int y, final int z) {
		return ((long)(x & 0xFFFFF)) | (((long)(y & 0xFF)) << 20) | (((long)(z & 0xFFFFF)) << 28);
	}

	public static final int x(final long chave) {
		return (int)(chave >> 32);
	}

	public static final int z(final long chave) {
		return (int)chave;
	}
}
