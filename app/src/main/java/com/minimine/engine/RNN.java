package com.minimine.engine;

public class RNN {
	public float[] softmax(float[] arr) {
		int tam = arr.length;
		float max = Float.NEGATIVE_INFINITY;
		// valor maximo
		for(int i = 0; i < tam; i++) if(arr[i] > max) max = arr[i];
		// calcula exponenciais
		float[] exp = new float[tam];
		float soma = 0f;
		for(int i = 0; i < tam; i++) {
			exp[i] = (float) Math.exp(arr[i] - max);
			soma += exp[i];
		}
		// normaliza:
		for(int i = 0; i < tam; i++) exp[i] /= soma;
		return exp;
	}
	
	public float sigmoid(float x) {
		return (float) (1 / (1 + Math.exp(-x)));
	}
	public float derivadaSigmoid(float y) {
		return y * (1 - y);
	}
	public float tanh(float x) {
		return (float) Math.tanh(x);
	}
	public float derivadaTanh(float y) {
		return 1 - y * y;
	}
	public float ReLU(float x) {
		return Math.max(0, x);
	}
	public float derivadaReLU(float y) {
		return y > 0 ? 1 : 0;
	}
	
	public float[][] matriz(int l, int c, float escala) {
		float[][] m = new float[l][c];
		for(int i = 0; i < m.length; i++) {
			for(int j = 0; j < m[i].length; j++) m[i][j] = (float)(Math.random()*2-1)*escala;
		}
		return m;
	}
	
	public float[] vetor(int n, float escala) {
		float[] v = new float[n];
		for(int i = 0; i < v.length; i++) v[i] = (float)(Math.random()*2-1)*escala;
		return v;
	}
	public float[] zeros(int n) {
		float[] v = new float[n];
		for(int i = 0; i < v.length; i++) v[i] = 0;
		return v;
	}
}
