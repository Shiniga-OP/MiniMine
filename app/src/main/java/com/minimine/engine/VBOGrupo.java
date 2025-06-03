package com.minimine.engine;

public class VBOGrupo {
    public int texturaId;
    public int vboId;
	public int iboId;
    public int vertices;

    public VBOGrupo(int texturaId, int vboId, int iboId, int vertices) {
        this.texturaId = texturaId;
        this.vboId = vboId;
		this.iboId = iboId;
        this.vertices = vertices;
    }
}
