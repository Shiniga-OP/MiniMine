package com.minimine.graficos;

import java.util.HashMap;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class Texturas {
	public static TexLista<String, Texture> texs = new TexLista<String, Texture>();
	public static TexLista<String, TextureRegion> atlas = new TexLista<String, TextureRegion>();
	public static Texture blocos, agua, icones, base, ceu, ferramentas,
	palito;
	
	public static void iniciar() {
		try {
			// atlas:
			blocos = new Texture(Gdx.files.internal("texturas/blocos/blocos.png"));
			agua = new Texture(Gdx.files.internal("texturas/blocos/anims/agua.png"));
			icones = new Texture(Gdx.files.internal("texturas/ui/icones_16x16.png"));
			base = new Texture(Gdx.files.internal("texturas/ui/base_botao.png"));
			ceu = new Texture(Gdx.files.internal("texturas/sol_lua.png"));
			ferramentas = new Texture(Gdx.files.internal("texturas/itens/ferramentas.png"));
			palito = new Texture(Gdx.files.internal("texturas/itens/madeira/palito.png"));
			// blocos:
			atlas.put("grama_topo", new TextureRegion(blocos, 0, 0, 16, 16));
			atlas.put("grama_lado", new TextureRegion(blocos, 16, 0, 16, 16));
			atlas.put("terra", new TextureRegion(blocos, 32, 0, 16, 16));
			atlas.put("pedregulho", new TextureRegion(blocos, 48, 0, 16, 16));
			atlas.put("agua", new TextureRegion(blocos, 64, 0, 16, 16));
			atlas.put("areia", new TextureRegion(blocos, 80, 0, 16, 16));
			atlas.put("tronco_topo", new TextureRegion(blocos, 96, 0, 16, 16));
			atlas.put("tronco_lado", new TextureRegion(blocos, 112, 0, 16, 16));
			atlas.put("folha", new TextureRegion(blocos, 0, 16, 16, 16));
			atlas.put("tabua_madeira", new TextureRegion(blocos, 16, 16, 16, 16));
			atlas.put("cacto_topo", new TextureRegion(blocos, 32, 16, 16, 16));
			atlas.put("cacto_lado", new TextureRegion(blocos, 48, 16, 16, 16));
			atlas.put("vidro", new TextureRegion(blocos, 64, 16, 16, 16));
			atlas.put("tocha", new TextureRegion(blocos, 80, 16, 16, 16));
			atlas.put("pedra", new TextureRegion(blocos, 96, 16, 16, 16));
			atlas.put("cascalho", new TextureRegion(blocos, 112, 16, 16, 16));
			atlas.put("gelo", new TextureRegion(blocos, 0, 32, 16, 16));
			atlas.put("neve", new TextureRegion(blocos, 16, 32, 16, 16));
			atlas.put("coral_rosa", new TextureRegion(blocos, 32, 32, 16, 16));
			atlas.put("coral_azul", new TextureRegion(blocos, 48, 32, 16, 16));
			atlas.put("coral_amarelo", new TextureRegion(blocos, 64, 32, 16, 16));
			atlas.put("capim", new TextureRegion(blocos, 80, 32, 16, 16));
			atlas.put("tulipa", new TextureRegion(blocos, 96, 32, 16, 16));
			atlas.put("iris_azul", new TextureRegion(blocos, 112, 32, 16, 16));
			atlas.put("arenito", new TextureRegion(blocos, 0, 48, 16, 16));
			atlas.put("pilar_arenito_lado", new TextureRegion(blocos, 16, 48, 16, 16));
			atlas.put("pilar_arenito_topo", new TextureRegion(blocos, 32, 48, 16, 16));
			atlas.put("bloco_estrutura", new TextureRegion(blocos, 48, 48, 16, 16));
			atlas.put("nulo", new TextureRegion(blocos, 64, 48, 16, 16));
			// animações:
			atlas.put("agua_a1", new TextureRegion(agua, 0, 0, 16, 16));
			atlas.put("agua_a2", new TextureRegion(agua, 0, 16, 16, 16));
			atlas.put("agua_a3", new TextureRegion(agua, 0, 32, 16, 16));
			atlas.put("agua_a4", new TextureRegion(agua, 0, 48, 16, 16));
			// interface:
			atlas.put("mira", new TextureRegion(icones, 0, 0, 16, 16));
			atlas.put("clique", new TextureRegion(icones, 16, 0, 16, 16));
			atlas.put("ataque", new TextureRegion(icones, 32, 0, 16, 16));
			atlas.put("receita", new TextureRegion(icones, 48, 0, 16, 16));
			atlas.put("coracao_completo", new TextureRegion(icones, 64, 0, 16, 16));
			atlas.put("coracao_metade", new TextureRegion(icones, 80, 0, 16, 16));
			atlas.put("coracao_vazio", new TextureRegion(icones, 96, 0, 16, 16));
			
			atlas.put("botao_f", new TextureRegion(icones, 0, 16, 16, 16));
			atlas.put("botao_t", new TextureRegion(icones, 16, 16, 16, 16));
			atlas.put("botao_e", new TextureRegion(icones, 32, 16, 16, 16));
			atlas.put("botao_d", new TextureRegion(icones, 48, 16, 16, 16));
			atlas.put("botao_le", new TextureRegion(icones, 64, 16, 16, 16));
			atlas.put("botao_ld", new TextureRegion(icones, 80, 16, 16, 16));
			// ambiente:
			atlas.put("sol", new TextureRegion(ceu, 0, 0, 16, 16));
			atlas.put("lua_completa", new TextureRegion(ceu, 0, 16, 16, 16));
			// itens:
			atlas.put("espada_madeira", new TextureRegion(ferramentas, 0, 0, 16, 16));
			atlas.put("picareta_madeira", new TextureRegion(ferramentas, 16, 0, 16, 16));
			atlas.put("machado_madeira", new TextureRegion(ferramentas, 32, 0, 16, 16));
			atlas.put("pa_madeira", new TextureRegion(ferramentas, 48, 0, 16, 16));
			atlas.put("enxada_madeira", new TextureRegion(ferramentas, 64, 0, 16, 16));
			
			atlas.put("palito", new TextureRegion(palito, 0, 0, 16, 16));
		} catch(Exception e) {
			Gdx.app.log("Texturas", "[ERRO]: " + e);
			throw new RuntimeException("[Texturas]: [ERRO]: ao carregar as texturas: "+e);
		}
	}

	public static class TexLista<K, V> extends HashMap<K, V> {
		public V obter(Object chave) {
			V o = super.get(chave);
			if(o == null) {
				throw new RuntimeException("Texturas: [ERRO] null na textura: " + chave);
			}
			TextureRegion tr = (TextureRegion)o;
			if(tr.getTexture() == null) {
				throw new RuntimeException("Texturas: [ERRO] null na textura: " + chave);
			}
			return o;
		}
		
		public void liberar(Object chave) {
			V o = super.get(chave);
			if(o instanceof Texture) {
				Texture tex = (Texture)o;
				if(tex != null) tex.dispose();
				tex = null;
			}
			super.remove(chave);
		}
	}
	
	public static void liberar() {
		for(Texture tex : texs.values()) {
			if(tex != null) tex.dispose();
			tex = null;
		}
		texs.clear();
		atlas.clear();
		blocos.dispose();
		agua.dispose();
		icones.dispose();
		base.dispose();
		ceu.dispose();
		palito.dispose();
	}
}
