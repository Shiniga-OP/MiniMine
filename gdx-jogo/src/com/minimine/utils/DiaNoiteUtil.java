package com.minimine.utils;

import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Vector3;
import com.minimine.mundo.Mundo;

public class DiaNoiteUtil {
    public static float tempo = 0.0f;
	public static float tempo_velo = 0.00028f;
    public static float luz = 1.0f;
    public static long ultimaAtt = 0;
    public static final Vector3 posicaoSol = new Vector3();
    public static final Vector3 posicaoLua = new Vector3();
    public static final float RAIO_ORBITA = 150f;
    public static final float ALTURA_BASE = 120f;
    public static final float[] corSol = {1.0f, 0.9f, 0.1f, 1.0f};
    public static final float[] corLua = {0.9f, 0.95f, 1.0f, 1.0f};
    public static float visibiSol = 1.0f;
    public static float visibiLua = 0.0f;
	public static float[] posTmp = new float[3];

    public static void att() {
        tempo += tempo_velo;
        if(tempo > 1.0f) tempo = 0.0f;
        // calcula iluminação baseada no ciclo
        float ciclo = (float)Math.sin(tempo * Math.PI * 2);
        luz = Math.max(0.1f, (ciclo + 1.0f) * 0.4f + 0.2f);

        calcularPosicoesCorposCelestes();
        // calcula visibilidades baseado na altura do sol
        float alturaNormalizada = (posicaoSol.y + RAIO_ORBITA) / (2f * RAIO_ORBITA);
        // sol mais visivel durante o dia
        visibiSol = calcularvisibiSol(alturaNormalizada);
        // lua mais visivel durante a noite  
        visibiLua = calcularvisibiLua(alturaNormalizada);
        // aplicar visibilidade as cores
        corSol[3] = visibiSol;
        corLua[3] = visibiLua;

        ultimaAtt = System.currentTimeMillis();
    }

    public static float calcularvisibiSol(float alturaNorm) {
        // Sol visivel principalmente durante o dia
        if(alturaNorm > 0.6f) return 1.0f; // dia pleno
        if(alturaNorm > 0.4f) return (alturaNorm - 0.4f) * 5f; // amanhecer
        if(alturaNorm > 0.3f) return 0.0f; // abaixo do horizonte
        return 0.0f;
    }

    public static float calcularvisibiLua(float alturaNorm) {
        // lua visivel principalmente durante a noite
        if(alturaNorm < 0.3f) return 1.0f; // noite plena
        if(alturaNorm < 0.4f) return (0.4f - alturaNorm) * 10f; // anoitecer
        if(alturaNorm < 0.6f) return 0.0f; // durante o dia
        return 0.0f;
    }

    public static void calcularPosicoesCorposCelestes() {
        // angulo baseado no tempo(0-360 graus)
        float angulo = tempo * 360f;
		/* sol: comeca a leste(0°) e move para oeste(180°)
		quando angulo = 0°: sol no leste amanhecer)
		quando angulo = 90°: sol no zênite(meio-dia)  
		quando angulo = 180°: sol no oeste(anoitecer) */
        float anguloSol = angulo;
        posicaoSol.set(
            (float)Math.cos(Math.toRadians(anguloSol)) * RAIO_ORBITA,
            (float)Math.sin(Math.toRadians(anguloSol)) * RAIO_ORBITA,
            -50f // posicionado atras da camera pra melhor visibilidade
        );
        // lua: oposta ao sol(angulo + 180°)
        // quando sol = 0°, lua = 180°(lua se poe)
        // quando sol = 180°, lua = 0°(lua nasce)
        float anguloLua = angulo + 180f;
        if(anguloLua > 360f) anguloLua -= 360f;

        posicaoLua.set(
            (float)Math.cos(Math.toRadians(anguloLua)) * RAIO_ORBITA,
            (float)Math.sin(Math.toRadians(anguloLua)) * RAIO_ORBITA,
            -50f);
    }

    public static void aplicarShader(ShaderProgram shader) {
		posTmp[0] =posicaoSol.x; posTmp[1] = posicaoSol.y; posTmp[2] = posicaoSol.z;
        shader.setUniform3fv("u_posSol", posTmp, 0, 3);
		posTmp[0] =posicaoLua.x; posTmp[1] = posicaoLua.y; posTmp[2] = posicaoLua.z;
        shader.setUniform3fv("u_posLua", posTmp, 0, 3);
        shader.setUniform4fv("u_corSol", corSol, 0, 4);
        shader.setUniform4fv("u_corLua", corLua, 0, 4);
    }

    public static boolean ehNoite() {
        return posicaoSol.y < -20f;
    }

    public static boolean ehDia() {
        return posicaoSol.y > 20f;
    }

    public static float obterFatorTransicao() {
        return Math.min(1.0f, Math.max(0.0f, (posicaoSol.y + RAIO_ORBITA) / (2f * RAIO_ORBITA)));
    }
}
