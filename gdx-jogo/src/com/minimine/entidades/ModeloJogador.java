package com.minimine.entidades;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import net.mgsx.gltf.loaders.gltf.GLTFLoader;
import net.mgsx.gltf.scene3d.scene.SceneAsset;

public class ModeloJogador {
    public Model modelo;
    public ModelInstance instancia;
    public SceneAsset ativoCena;

    public float tempoAnimacao = 0;
    public boolean andando = false;

    public Quaternion rotTemp = new Quaternion();
    public Vector3 eulerTemp = new Vector3();

    // referencias dos nos
    public Node cabeca, tronco, bracoDir, bracoEsq, pernaDir, pernaEsq;

    // rotações originais
    public Quaternion rotCabeca = new Quaternion();
    public Quaternion rotTronco = new Quaternion();
    public Quaternion rotBracoDir = new Quaternion();
    public Quaternion rotBracoEsq = new Quaternion();
    public Quaternion rotPernaDir = new Quaternion();
    public Quaternion rotPernaEsq = new Quaternion();

    public ModeloJogador() {
        try {
            ativoCena = new GLTFLoader().load(Gdx.files.internal("modelos/jogador.gltf"));
            modelo = ativoCena.scene.model;
            instancia = new ModelInstance(modelo);

            pegarNos();
            salvarRotacoes();
        } catch(Exception e) {
            Gdx.app.error("[ModeloJogador]", "Erro no GLTF: " + e.getMessage());
        }
    }

    public void pegarNos() {
        cabeca = instancia.getNode("cabeca", true);
        tronco = instancia.getNode("tronco", true);
        bracoDir = instancia.getNode("braco_dir", true);
        bracoEsq = instancia.getNode("braco_esq", true);
        pernaDir = instancia.getNode("perna_dir", true);
        pernaEsq = instancia.getNode("perna_esq", true);
		
		instancia.nodes.clear();
		instancia.nodes.add(bracoDir);
    }

    public void salvarRotacoes() {
        if(cabeca != null) rotCabeca.set(cabeca.rotation);
        if(tronco != null) rotTronco.set(tronco.rotation);
        if(bracoDir != null) rotBracoDir.set(bracoDir.rotation);
        if(bracoEsq != null) rotBracoEsq.set(bracoEsq.rotation);
        if(pernaDir != null) rotPernaDir.set(pernaDir.rotation);
        if(pernaEsq != null) rotPernaEsq.set(pernaEsq.rotation);
    }

    public void animar(float delta, boolean estaAndando, boolean noChao) {
        tempoAnimacao += delta * (estaAndando ? 10f : 3f);
        andando = estaAndando && noChao;

        float anguloBraco = (float)Math.sin(tempoAnimacao) * (andando ? 40f : 3f);
        float anguloPerna = (float)Math.sin(tempoAnimacao) * (andando ? 35f : 2f);

        // leve balanço quando parado
        if(cabeca != null) {
            cabeca.rotation.set(rotCabeca);
            if(!andando) {
                eulerTemp.set((float)Math.sin(tempoAnimacao * 0.5f) * 2f, 0, 0);
                rotTemp.setEulerAngles(eulerTemp.y, eulerTemp.x, eulerTemp.z);
                cabeca.rotation.mulLeft(rotTemp);
            }
        }
        // inclinação suave
        if(tronco != null) {
            tronco.rotation.set(rotTronco);
            if(andando) {
                eulerTemp.set((float)Math.sin(tempoAnimacao * 0.8f) * 2f, 0, 0);
                rotTemp.setEulerAngles(eulerTemp.y, eulerTemp.x, eulerTemp.z);
                tronco.rotation.mulLeft(rotTemp);
            }
        }
        if(bracoDir != null) {
            bracoDir.rotation.set(rotBracoDir);
            eulerTemp.set(0, 0, anguloBraco);
            rotTemp.setEulerAngles(eulerTemp.y, eulerTemp.x, eulerTemp.z);
            bracoDir.rotation.mulLeft(rotTemp);
        }
        if(bracoEsq != null) {
            bracoEsq.rotation.set(rotBracoEsq);
            eulerTemp.set(0, 0, -anguloBraco);
            rotTemp.setEulerAngles(eulerTemp.y, eulerTemp.x, eulerTemp.z);
            bracoEsq.rotation.mulLeft(rotTemp);
        }
        if(pernaDir != null) {
            pernaDir.rotation.set(rotPernaDir);
            eulerTemp.set(-anguloPerna, 0, 0);
            rotTemp.setEulerAngles(eulerTemp.y, eulerTemp.x, eulerTemp.z);
            pernaDir.rotation.mulLeft(rotTemp);
        }
        if(pernaEsq != null) {
            pernaEsq.rotation.set(rotPernaEsq);
            eulerTemp.set(anguloPerna, 0, 0);
            rotTemp.setEulerAngles(eulerTemp.y, eulerTemp.x, eulerTemp.z);
            pernaEsq.rotation.mulLeft(rotTemp);
        }
        instancia.calculateTransforms();
    }

    public void render(com.badlogic.gdx.graphics.g3d.ModelBatch lote) {
        if(instancia != null) lote.render(instancia);
    }

    public void liberar() {
        if(modelo != null) modelo.dispose();
        if(ativoCena != null) ativoCena.dispose();
    }
}
