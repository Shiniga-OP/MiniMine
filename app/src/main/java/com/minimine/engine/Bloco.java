package com.minimine.engine;

public class Bloco {
    public int x, y, z;
	public String id;
    public Bloco(int x, int y, int z, String tipo) {
        this.x = x;
        this.y = y;
        this.z = z;
		this.id = tipo;
    }
}
class Agua extends Bloco {
	public int nivel = 0; // 0 mais alto(0-7) o padrão não debaixo deve zer 1
	public int dir = 0; // 0 sem inclinacao(0-8) direcoez
    public Agua(int x, int y, int z, String tipo) {
		super(x, y, z, tipo);
    }
}
