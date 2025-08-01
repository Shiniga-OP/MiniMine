package com.minimine.engine;

import java.util.List;
import java.util.ArrayList;
import com.engine.Camera3D;

public class Player extends Camera3D {
	public int vida = 20;
	public float velocidadeX = 0.2f;
	public float salto = 0.3f;
	public float peso = 1f;
	public float alcance = 7f;
	
	public Camera3D camera = new Camera3D();
	// hitbox:
	public float[] hitbox = {1.8f, 0.5f};
	public float velocidadeY = 0f;
	public float GRAVIDADE = -0.03f;
	public float velocidadeY_limite = -1.5f;
	// estados:
	public boolean noAr = true;
	public String itemMao = "AR";
	public final List<Slot> inventario = new ArrayList<>();

	public Player() {
		super();
		hitbox[0] = 1.7f;
		hitbox[1] = 0.25f;
		inventario.add(new Slot());
	}

	public void atualizar() {
		camera.pos[0] = pos[0];
		camera.pos[1] = pos[1] + hitbox[0];
		camera.pos[2] = pos[2];
	}
	class Slot {
		public String tipo;
		public int quant;
	}
}
