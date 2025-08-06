package com.minimine.engine;

import com.engine.VBOGrupo;

class Mob extends Player {
	public VBOGrupo modelo;
	public int texturaId;

	public Mob(float x, float y, float z, VBOGrupo modelo, int texturaId) {
		this.modelo = modelo;
		this.texturaId = texturaId;
		this.pos[0] = x;
		this.pos[1] = y;
		this.pos[2] = z;
		this.peso = 1f;
		this.velocidadeX = 0.09f;
		this.hitbox[0] = 1.7f;
		this.hitbox[1] = 0.8f;
	}
}
