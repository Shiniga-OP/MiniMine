package com.engine;

public class Camera3D {
    public float[] posicao = {8f, 70f, 8f};
    public float[] foco = {0f, 0f, -1f};
    public float[] up = {0f, 1f, 0f};

	public float[] hitbox = {0.5f, 0.5f};

    public float yaw = -90f;
    public float tom = 0f;

	public Camera3D() {
		this.mover(0f);
		this.rotacionar(0f, 0f);
	}

    public void rotacionar(float dx, float dy) {
        // rotacao invertida propositalmente para rotacao certa:
        yaw += dx;
        tom -= dy;

        if(tom > 89f) tom = 89f;
        if(tom < -89f) tom = -89f;

        foco[0] = (float)(Math.cos(Math.toRadians(yaw)) * (float)Math.cos(Math.toRadians(tom)));
        foco[1] = (float)Math.sin(Math.toRadians(tom));
        foco[2] = (float)(Math.sin(Math.toRadians(yaw)) * (float)Math.cos(Math.toRadians(tom)));
        normalize(foco);
    }

    public void mover(float velocidade) {
        posicao[0] += foco[0] * velocidade;
        posicao[1] += foco[1] * velocidade;
        posicao[2] += foco[2] * velocidade;
    }

    public void normalize(float[] vec) {
        float tamanho = (float)Math.sqrt(vec[0]*vec[0] + vec[1]*vec[1] + vec[2]*vec[2]);
        if(tamanho==0f) return;
        vec[0] /= tamanho;
        vec[1] /= tamanho;
        vec[2] /= tamanho;
    }
}
