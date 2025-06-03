package com.minimine.engine;

import java.util.List;
import java.util.ArrayList;
import com.engine.Camera3D;

public class Player extends Camera3D {
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
