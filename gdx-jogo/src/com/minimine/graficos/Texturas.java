package com.minimine.graficos;

import java.util.HashMap;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class Texturas {
	public static TexLista<CharSequence, Texture> texs = new TexLista<CharSequence, Texture>();
	public static TexLista<CharSequence, TextureRegion> atlas = new TexLista<CharSequence, TextureRegion>();
	public static Texture blocos;
	public static Texture agua;
	public static Texture icones;
	public static Texture base;
	
	public static void iniciar() {
		try {
			// atlas:
			blocos = new Texture(Gdx.files.internal("blocos/blocos.png"));
			agua = new Texture(Gdx.files.internal("blocos/anims/agua.png"));
			icones = new Texture(Gdx.files.internal("ui/icones_16x16.png"));
			base = new Texture(Gdx.files.internal("ui/base_botao.png"));
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
			// animações:
			atlas.put("agua_a1", new TextureRegion(agua, 0, 0, 16, 16));
			atlas.put("agua_a2", new TextureRegion(agua, 0, 16, 16, 16));
			// interface:
			atlas.put("mira", new TextureRegion(icones, 0, 0, 16, 16));
			atlas.put("clique", new TextureRegion(icones, 16, 0, 16, 16));
			atlas.put("ataque", new TextureRegion(icones, 32, 0, 16, 16));
			atlas.put("receita", new TextureRegion(icones, 48, 0, 16, 16));
			atlas.put("coracao_completo", new TextureRegion(icones, 64, 0, 16, 16));
			atlas.put("coracao_metade", new TextureRegion(icones, 80, 0, 16, 16));
			atlas.put("coracao_vazio", new TextureRegion(icones, 96, 0, 16, 16));
			
			texs.put("botao_f", new Texture(Gdx.files.internal("ui/botao_f.png")));
			texs.put("botao_t", new Texture(Gdx.files.internal("ui/botao_t.png")));
			texs.put("botao_d", new Texture(Gdx.files.internal("ui/botao_d.png")));
			texs.put("botao_e", new Texture(Gdx.files.internal("ui/botao_e.png")));
			texs.put("botao_ld", new Texture(Gdx.files.internal("ui/botao_ld.png")));
			texs.put("botao_le", new Texture(Gdx.files.internal("ui/botao_le.png")));
		} catch(Exception e) {
			Gdx.app.log("Texturas", "[ERRO]: " + e);
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
	}
	
	public static void liberar() {
		for(Texture tex : texs.values()) tex.dispose();
		texs.clear();
		atlas.clear();
		blocos.dispose();
		agua.dispose();
		icones.dispose();
		base.dispose();
	}
}
