package com.minimine.graficos;

import java.util.HashMap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class Texturas {
	public static TexLista<CharSequence, Texture> texs = new TexLista<CharSequence, Texture>();
	public static TexLista<CharSequence, TextureRegion> atlas = new TexLista<CharSequence, TextureRegion>();
	public static Texture blocos;
	public static Texture agua;
	
	static {
		try {
			// atlas:
			blocos = new Texture(Gdx.files.internal("blocos/blocos.png"));
			agua = new Texture(Gdx.files.internal("blocos/anims/agua.png"));
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
			
			// animações:
			atlas.put("agua_a1", new TextureRegion(agua, 0, 0, 16, 16));
			atlas.put("agua_a2", new TextureRegion(agua, 0, 16, 16, 16));
			// interface:
			texs.put("botao_f", new Texture(Gdx.files.internal("ui/botao_f.png")));
			texs.put("botao_t", new Texture(Gdx.files.internal("ui/botao_t.png")));
			texs.put("botao_d", new Texture(Gdx.files.internal("ui/botao_d.png")));
			texs.put("botao_e", new Texture(Gdx.files.internal("ui/botao_e.png")));
			texs.put("mira", new Texture(Gdx.files.internal("ui/mira.png")));
			texs.put("clique", new Texture(Gdx.files.internal("ui/clique.png")));
			texs.put("ataque", new Texture(Gdx.files.internal("ui/ataque.png")));
			texs.put("slot", new Texture(Gdx.files.internal("ui/slot.png")));
			texs.put("receita", new Texture(Gdx.files.internal("ui/receita.png")));
			texs.put("botao_opcao", new Texture(Gdx.files.internal("ui/botao_opcao.png")));
			texs.put("botao_ld", new Texture(Gdx.files.internal("ui/botao_ld.png")));
			texs.put("botao_le", new Texture(Gdx.files.internal("ui/botao_le.png")));
			texs.put("salvar", new Texture(Gdx.files.internal("ui/salvar.png")));
			texs.put("botao_ativado", new Texture(Gdx.files.internal("ui/botao_ativado.png")));
			texs.put("botao_desativado", new Texture(Gdx.files.internal("ui/botao_desativado.png")));
			texs.put("botao_aviso", new Texture(Gdx.files.internal("ui/botao_aviso.png")));
			texs.put("botao_servidor", new Texture(Gdx.files.internal("ui/servidor.png")));
			texs.put("botao_cliente", new Texture(Gdx.files.internal("ui/cliente.png")));
		} catch(Exception e) {
			Gdx.app.log("Texturas", "[ERRO]: " + e);
		}
	}

	public static class TexLista<K, V> extends HashMap<K, V> {
		public V obter(Object chave) {
			V o = super.get(chave);
			if(o == null) {
				Gdx.app.log("Texturas", "[ERRO] em: " + chave);
				if(containsKey("slot")) return super.get("slot");
			}
			return o;
		}
	}
	
	public static void liberar() {
		for(Texture tex : texs.values()) {
			tex.dispose();
		}
		blocos.dispose();
	}
}
