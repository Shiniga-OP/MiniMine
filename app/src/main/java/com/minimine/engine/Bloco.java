package com.minimine.engine;

public class Bloco {
    public int x, y, z;
	public String id;
    public String[] tipo;
	public boolean solido=true, liquido=false, gasoso=false;

    public Bloco(int x, int y, int z, String tipo) {
        this.x = x;
        this.y = y;
        this.z = z;
		if(tipo.equals("AR")) this.solido=false;
		if(tipo.equals("AGUA")) {
			this.solido = false;
			this.liquido = true;
		}
		this.tipo = TipoBloco.tipos.get(tipo);
		this.id = tipo;
    }
}
