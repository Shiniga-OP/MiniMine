package com.engine;

public class Camera3D {
    public float[] pos = {1, 1.7f, 1};
    public float[] foco = {0, 0, -1};
    public float[] up = {0, 1, 0};

    public float yaw = -90;
    public float tom = 0;

	public Camera3D() {
		this.rotacionar(0, 0);
	}

    public void rotacionar(float dx, float dy) {
        // rotacao invertida propositalmente para rotacao certa:
        yaw += dx;
        tom -= dy;

        if(tom > 89) tom = 89;
        if(tom < -89) tom = -89;

        foco[0] = (float)(Math.cos(Math.toRadians(yaw)) * (float)Math.cos(Math.toRadians(tom)));
        foco[1] = (float)Math.sin(Math.toRadians(tom));
        foco[2] = (float)(Math.sin(Math.toRadians(yaw)) * (float)Math.cos(Math.toRadians(tom)));
        normalize(foco);
    }

    

    public void normalize(float[] v) {
        float tamanho = (float)Math.sqrt(v[0]*v[0] + v[1]*v[1] + v[2]*v[2]);
        if(tamanho==0) return;
        v[0] /= tamanho;
        v[1] /= tamanho;
        v[2] /= tamanho;
    }
}
