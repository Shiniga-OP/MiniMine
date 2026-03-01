package com.minimine.graficos;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.decals.CameraGroupStrategy;
import com.badlogic.gdx.graphics.g3d.decals.Decal;
import com.badlogic.gdx.graphics.g3d.decals.DecalBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.minimine.entidades.Jogador;

public class GerenciadorParticulas {
    public DecalBatch lote;
    public Array<Decal> particulas = new Array<>();
    public Array<DadosParticula> dados = new Array<>();
    public Jogador jogadorRef; // referencia da camera

    static class DadosParticula {
        Vector3 velo = new Vector3();
        float vida;
    }

    public GerenciadorParticulas(Jogador jogador) {
        jogadorRef = jogador;
        lote = new DecalBatch(new CameraGroupStrategy(jogador.camera));
    }

    /*
     * posição do bloco quebrou
     * regiao A textura completa do bloco
     */
    public void criar(float x, float y, float z, TextureRegion regiao) {
        int quantidade = MathUtils.random(5, 15); // fragmentos

        // tamanho da textura original
        int texV = regiao.getRegionWidth();
        int texH = regiao.getRegionHeight();

        for(int i = 0; i < quantidade; i++) {
            // 1. CORTE DA TEXTURA (A SOLUÇÃO DAS "MINI MESAS")
            // Escolhe um tamanho para o fragmento (ex: 4 pixels de largura)
            int tamanhoFrag = 4; 

            // sorteia onde começa o corte dentro da textura original
            int corteX = MathUtils.random(0, texV - tamanhoFrag);
            int corteY = MathUtils.random(0, texH - tamanhoFrag);

            // cria uma niva região que é so aquele pedaço
            TextureRegion pedaco = new TextureRegion(regiao, corteX, corteY, tamanhoFrag, tamanhoFrag);

            // 2. criação do decalque
            // tamanho fisico no mundo 3D(0.1 unidades)
            Decal d = Decal.newDecal(0.12f, 0.12f, pedaco, true);

            // espalha um pouco a posição inicial pra não saírem todos do mesmo ponto exato
            d.setPosition(
                x + MathUtils.random(0.2f, 0.8f), 
                y + MathUtils.random(0.2f, 0.8f), 
                z + MathUtils.random(0.2f, 0.8f)
            );

            DadosParticula dp = new DadosParticula();
            // explosão radial
            dp.velo.set(
                MathUtils.random(-0.1f, 0.1f),
                MathUtils.random(0.05f, 0.3f), 
                MathUtils.random(-0.1f, 0.1f)
            );
            dp.vida = MathUtils.random(0.5f, 8.0f);

            particulas.add(d);
            dados.add(dp);
        }
    }

    public void att(float delta) {
        for(int i = 0; i < particulas.size; i++) {
            Decal d = particulas.get(i);
            DadosParticula dp = dados.get(i);

            dp.vida -= delta;
            if(dp.vida <= 0) {
                particulas.removeIndex(i);
                dados.removeIndex(i);
                i--;
                continue;
            }
            dp.velo.y -= 1.2f * delta; // gravidade

            // atualiza posição
            d.translate(dp.velo.x, dp.velo.y, dp.velo.z);

            // 3. faz olhar pra camera
            d.lookAt(jogadorRef.camera.position, jogadorRef.camera.up);

            // efeito de encolher antes de sumir
            if(dp.vida < 0.3f) {
                d.setScale(dp.vida / 0.4f);
            }
            lote.add(d);
        }
        lote.flush();
    }

    public void liberar() {
        if(lote != null) lote.dispose();
    }
}
