package com.minimine.graficos;

import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.math.Vector3;
import com.minimine.mundo.Mundo;
import com.minimine.entidades.Jogador;

public class EmissorParticulas {
    public static final int MAX_FRAGMENTOS = 2000;
    public static float[] dados = new float[MAX_FRAGMENTOS * 24]; 
    public static Mesh malha;
    public static float[] posX = new float[MAX_FRAGMENTOS], posY = new float[MAX_FRAGMENTOS], posZ = new float[MAX_FRAGMENTOS];
    public static float[] velX = new float[MAX_FRAGMENTOS], velY = new float[MAX_FRAGMENTOS], velZ = new float[MAX_FRAGMENTOS];
    public static float[] vida = new float[MAX_FRAGMENTOS], vidaMax = new float[MAX_FRAGMENTOS];
    public static float[] tamanhos = new float[MAX_FRAGMENTOS];
    public static float[] cores = new float[MAX_FRAGMENTOS]; 
    public static int ativos = 0;

    public static Vector3 vDir = new Vector3(), vCima = new Vector3(), vLado = new Vector3();

    public static void iniciar() {
        malha = new Mesh(false, MAX_FRAGMENTOS * 4, MAX_FRAGMENTOS * 6, 
		new VertexAttribute(VertexAttributes.Usage.Position, 3, "a_pos"),
		new VertexAttribute(VertexAttributes.Usage.ColorPacked, 4, "a_cor"));

        short[] indices = new short[MAX_FRAGMENTOS * 6];
        for(int i = 0; i < MAX_FRAGMENTOS; i++) {
            int v = i * 4;
            int idc = i * 6;
            indices[idc] = (short)v;
			indices[idc+1] = (short)(v+1);
			indices[idc+2] = (short)(v+2);
            indices[idc+3] = (short)v;
			indices[idc+4] = (short)(v+2);
			indices[idc+5] = (short)(v+3);
        }
        malha.setIndices(indices);
    }

    public static void criar(int x, int y, int z, float cor) {
        int quantidade = MathUtils.random(24, 48);
        for(int i = 0; i < quantidade; i++) {
            if(ativos >= MAX_FRAGMENTOS) return;
            posX[ativos] = x + MathUtils.random(0.2f, 0.8f); 
            posY[ativos] = y + MathUtils.random(0.2f, 0.8f); 
            posZ[ativos] = z + MathUtils.random(0.2f, 0.8f);

            velX[ativos] = MathUtils.random(-0.15f, 0.15f);
            velY[ativos] = MathUtils.random(0.1f, 0.3f);
            velZ[ativos] = MathUtils.random(-0.15f, 0.15f);

            float tempoVida = MathUtils.random(0.5f, 1.2f);
            vida[ativos] = tempoVida;
            vidaMax[ativos] = tempoVida;
            tamanhos[ativos] = MathUtils.random(0.05f, 0.12f);
            cores[ativos] = cor;
            ativos++;
        }
    }

    public static void att(ShaderProgram shader, float delta, Jogador jogador) {
        if(ativos == 0) return;
        // pega a orientação da camera para girar as particulas
        vDir.set(jogador.camera.direction).nor();
        vLado.set(jogador.camera.direction).crs(jogador.camera.up).nor();
        vCima.set(vLado).crs(jogador.camera.direction).nor();

        int idc = 0;
        for(int i = 0; i < ativos; i++) {
            vida[i] -= delta;
            if(vida[i] <= 0) {
                posX[i] = posX[ativos-1]; posY[i] = posY[ativos-1]; posZ[i] = posZ[ativos-1];
                velX[i] = velX[ativos-1]; velY[i] = velY[ativos-1]; velZ[i] = velZ[ativos-1];
                vida[i] = vida[ativos-1]; vidaMax[i] = vidaMax[ativos-1];
                cores[i] = cores[ativos-1]; tamanhos[i] = tamanhos[ativos-1];
                ativos--; i--; continue;
            }
            velY[i] -= 0.6f * delta; // gravidade um pouco mais forte
            posX[i] += velX[i]; posY[i] += velY[i]; posZ[i] += velZ[i];

            // efeito de encolher: quanto menos vida, menor a particula
            float escala = (vida[i] / vidaMax[i]) * tamanhos[i];

            // calcula os 4 vertices virados para a camera
            float x0 = -escala, y0 = -escala;
            float x1 =  escala, y1 = -escala;
            float x2 =  escala, y2 =  escala;
            float x3 = -escala, y3 =  escala;

            renderVertice(idc++, i, x0, y0);
            renderVertice(idc++, i, x1, y1);
            renderVertice(idc++, i, x2, y2);
            renderVertice(idc++, i, x3, y3);
        }
        malha.setVertices(dados, 0, idc);
        malha.render(shader, GL20.GL_TRIANGLES, 0, ativos * 6);
    }

    public static void renderVertice(int idc, int i, float vx, float vy) {
        // aplica a rotação baseada na camera
        dados[idc * 4] = posX[i] + (vx * vLado.x) + (vy * vCima.x);
        dados[idc * 4 + 1] = posY[i] + (vx * vLado.y) + (vy * vCima.y);
        dados[idc * 4 + 2] = posZ[i] + (vx * vLado.z) + (vy * vCima.z);
        dados[idc * 4 + 3] = cores[i];
    }
}
