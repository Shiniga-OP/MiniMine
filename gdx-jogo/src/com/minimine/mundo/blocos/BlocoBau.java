package com.minimine.mundo.blocos;

import java.util.HashMap;
import com.minimine.inventario.Item;
import com.minimine.inventario.ItemRegistro;
import com.minimine.mundo.Mundo;
import com.minimine.entidades.ItemMundo;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.micro.janelas.PainelFatiado;
import com.minimine.mundo.Chave;

public class BlocoBau {
	public static final int SLOTS = 27;
	public static final HashMap<Long, Item[]> dados = new HashMap<>();

	public static Item[] obterOuCriar(int x, int y, int z) {
		final long ch = Chave.gerar3d(x, y, z);
		if(!dados.containsKey(ch)) dados.put(ch, new Item[SLOTS]);
		return dados.get(ch);
	}

	public static void aoDestruir(int x, int y, int z) {
		final long ch = Chave.gerar3d(x, y, z);
		final Item[] inv = dados.remove(ch);
		if(inv == null) return;
		for(int i = 0; i < inv.length; i++) {
			if(inv[i] != null && inv[i].quantidade > 0) {
				final ItemMundo deixado = new ItemMundo(inv[i].nome, inv[i].quantidade, x, y, z);
				deixado.posicao.set(x + 0.5f, y + 0.5f, z + 0.5f);
				Mundo.entidades.add(deixado);
			}
		}
	}
	// === interface ===
	public static BlocoBauUI ui;

	public static void iniciar(Bloco bloco, BitmapFont fonte) {
		ui = new BlocoBauUI(fonte);
		bloco.ui = ui;
		bloco.evento = new EventoBloco() {
			@Override public void aoColocar(int x, int y, int z) {
				obterOuCriar(x, y, z);
			}
			@Override public void aoDestruir(int x, int y, int z) {
				BlocoBau.aoDestruir(x, y, z);
			}
		};
	}
}
