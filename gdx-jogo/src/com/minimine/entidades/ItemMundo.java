package com.minimine.entidades;

import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.minimine.graficos.Modelos;
import com.minimine.mundo.Mundo;

public class ItemMundo extends Entidade {
    public int quantidade;
    public float tempoVida = 300f;

    public float tempoFlutuacao = 0f;
    public float anguloRotacao = 0f;

    // raio de coleta pelo jogador
    public static final float RAIO_COLETA = 1.5f;
    public static final float RAIO_ATRACAO = 2.5f;

    public static final float VELO_ROTACAO = 120f;
    public static final float AMPLITUDE_BOB = 0.12f;
    public static final float FREQUENCIA_BOB = 2.0f;
    public static final float IMPULSO_HORIZONTAL = 2.5f;

    public ModelInstance modelo;

    public ItemMundo(String nome, int quantidade, float x, float y, float z) {
        this.nome = nome;
        this.quantidade = quantidade;
        this.posicao.set(x, y, z);

        this.largura = 0.25f;
        this.altura = 0.25f;
        this.profundidade = 0.25f;

        // item nasce no ar, fisica vai resolver o pouso
        this.noChao = false;

        // impulso leve so horizontal + salto pequeno
        float angulo = MathUtils.random(0f, MathUtils.PI2);
        this.velocidade.set(
            MathUtils.cos(angulo) * IMPULSO_HORIZONTAL,
            2.0f,
            MathUtils.sin(angulo) * IMPULSO_HORIZONTAL
        );
        modelo = Modelos.modeloItem(nome);
    }

    @Override
    public void att(float delta) {
		super.att(delta);
        tempoVida -= delta;

        // === movimento horizontal com atrito ===
        velocidade.x *= 0.75f;
        velocidade.z *= 0.75f;

        // === move e colide no Y ===
        final float dy = velocidade.y * delta;
        posicao.y += dy;
        attHitbox();
        if(colideMundo()) {
            posicao.y -= dy;
            attHitbox();
            if(dy < 0) noChao = true;
            velocidade.y = 0f;
        } else {
            noChao = ehChao();
        }
        // === move e colide no X ===
        final float dx = velocidade.x * delta;
        posicao.x += dx;
        attHitbox();
        if(colideMundo()) {
            posicao.x -= dx;
            velocidade.x = 0f;
            attHitbox();
        }
        // === move e colide no Z ===
        final float dz = velocidade.z * delta;
        posicao.z += dz;
        attHitbox();
        if(colideMundo()) {
            posicao.z -= dz;
            velocidade.z = 0f;
            attHitbox();
        }
        // === animação ===
        anguloRotacao = (anguloRotacao + VELO_ROTACAO * delta) % 360f;
        if(noChao) tempoFlutuacao += delta;

        if(modelo != null) {
            modelo.transform.setToTranslation(posicao.x, posicao.y, posicao.z);
            modelo.transform.rotate(Vector3.Y, anguloRotacao);
            modelo.transform.scale(1.5f, 1.5f, 1.5f);
            modelo.calculateTransforms();
            modelo.userData = dadosLuz;
        }
    }

    @Override
    public void render(ModelBatch mb) {
        if(modelo != null) mb.render(modelo);
    }
}

