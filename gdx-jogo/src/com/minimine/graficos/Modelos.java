package com.minimine.graficos;

import com.badlogic.gdx.graphics.g3d.Model;
import java.util.HashMap;
import net.mgsx.gltf.loaders.gltf.GLTFLoader;
import com.badlogic.gdx.Gdx;
import net.mgsx.gltf.scene3d.scene.SceneAsset;

public class Modelos {
	public static final HashMap<String, SceneAsset> modelos = new HashMap<>();
	
	public static Model obterModelo(String caminho, boolean interno) {
		if(modelos.containsKey(caminho)) return modelos.get(caminho).scene.model;
		
		SceneAsset ativoCena = new GLTFLoader().load(interno ? Gdx.files.internal(caminho) : Gdx.files.absolute(caminho));
		 modelos.put(caminho, ativoCena);
		 
		 return modelos.get(caminho).scene.model;
	}
	
	public static Model obterModelo(String caminho) {
		return obterModelo(caminho, true);
	}
	
	public static void liberar() {
		for(SceneAsset m : modelos.values()) {
			m.scene.model.dispose();
			m.dispose();
		}
		modelos.clear();
	}
}
