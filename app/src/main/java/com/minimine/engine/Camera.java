package com.minimine.engine;

import java.util.ArrayList;
import java.util.List;
import com.engine.AABB;

public class Camera {
    public float[] posicao = new float[]{8f, 70f, 8f};
    public float[] foco = new float[]{0f, 0f, -1f};
    public float[] up = new float[]{0f, 1f, 0f};

	public float[] hitbox = new float[]{0.5f, 0.5f};

    public float yaw = -90f;
    public float tom = 0f;

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

class Player extends Camera {
	public int vida = 20;
	public float velocidadeX = 0.2f;
	public float salto = 0.25f;
	public float peso = 1f;
	public float alcance = 7f;
	
	public float velocidadeY = 0f;
	public float GRAVIDADE = -0.03f;
	public float velocidadeY_limite = -1.5f;
	
	// estados:
	public boolean noAr = true;
	public String itemMao = "AR";

	public final List<Slot> inventario = new ArrayList<>();

	public Player() {
		super();
		hitbox[0] = 1.4f;
		hitbox[1] = 0.5f;
		inventario.add(new Slot());
/*
		for(int i = 0; i<1; i++) {
			this.inventario.add(new Slot());
		} */
	}

	class Slot {
		public String tipo;
		public int quant;
	}
}
