package com.minimine.entidades;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import net.mgsx.gltf.loaders.gltf.GLTFLoader;
import net.mgsx.gltf.scene3d.scene.SceneAsset;
import com.badlogic.gdx.graphics.GL20;

public class ModeloJogador {
    public Model modelo;
    public ModelInstance instancia;
    public SceneAsset ativoCena;

    public ModeloJogador() {
        try {
            // carrega o GLTF
            ativoCena = new GLTFLoader().load(Gdx.files.internal("modelos/jogador.gltf"));
            modelo = ativoCena.scene.model;

            instancia = new ModelInstance(modelo);
        } catch(Exception e) {
            Gdx.app.error("[ModeloJogador]", "Erro no GLTF: " + e.getMessage());
        }
    }

    public void render(com.badlogic.gdx.graphics.g3d.ModelBatch lote) {
        if(instancia != null) {
            lote.render(instancia);
        }
    }
    public void liberar() {
        if(modelo != null) modelo.dispose();
        if(ativoCena != null) ativoCena.dispose();
    }

    public void animar(float delta) {
        
    }
}
