package com.minimine.mundo.blocos;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.minimine.inventario.Item;
import com.minimine.inventario.ItemRegistro;
import com.minimine.graficos.Texturas;
import com.minimine.entidades.Jogador;
import com.minimine.inventario.Inventario;
import com.micro.janelas.PainelFatiado;
import com.minimine.ui.UI;
import com.badlogic.gdx.math.Vector2;

public class BlocoBauUI implements InterfaceBloco {
	public final BitmapFont fonte;

	// slots do baú na tela
	public Rectangle[] rectsBau;
	public int tamSlot;

	// itens do baú atual(referencia direta ao array de BlocoBau.dados)
	public Item[] itensBau;

	// item sendo arrastado(compartilhado entre bau e inventario do jogador)
	public Item itemFlutuante;
	public int slotOrigem; // >= 0 bau, < 0 = inv jogador(-(slot+1))
	public final Vector2 posFlutuante = new Vector2();

	public boolean aberta;

	public BlocoBauUI(BitmapFont fonte) {
		this.fonte = fonte;
	}

	public int bauX, bauY, bauZ;

	@Override
	public void abrir(int bx, int by, int bz) {
		if(aberta && bx == bauX && by == bauY && bz == bauZ) {
			fechar();
			return;
		}
		bauX = bx; bauY = by; bauZ = bz;
		itensBau = BlocoBau.obterOuCriar(bx, by, bz);
		itemFlutuante = null;
		slotOrigem = Integer.MIN_VALUE;
		aberta = true;
		Bloco.ABERTO = true;
		UI.modoTexto = true;
		UI.jg.inv.aberto = true;
		Gdx.input.setCursorCatched(false);
		calcularRects();
	}

	public void calcularRects() {
		final int v = Gdx.graphics.getWidth();
		final Inventario inv = UI.jg.inv;
		tamSlot = inv.tamSlot;

		// 9 colunas x 3 linhas, centralizado, acima do inventario do jogador
		final int cols = 9, linhas = 3;
		final int largura = cols * tamSlot;
		final int bauX = (v >> 1) - (largura >> 1);
		// fica logo acima do inventario do jogador(invY do Inventario)
		final int bauY = inv.invY + inv.slotsV * tamSlot + 20;

		rectsBau = new Rectangle[BlocoBau.SLOTS];
		int i = 0;
		for(int l = 0; l < linhas; l++) {
			for(int c = 0; c < cols; c++) {
				rectsBau[i++] = new Rectangle(bauX + c * tamSlot, bauY + (linhas - 1 - l) * tamSlot, tamSlot, tamSlot);
			}
		}
	}

	@Override
	public void fechar() {
		// devolve item flutuante
		if(itemFlutuante != null) {
			devolverFlutuante();
		}
		aberta = false;
		Bloco.ABERTO = false;
		UI.modoTexto = false;
		UI.jg.inv.aberto = false;
		Gdx.input.setCursorCatched(true);
		itensBau = null;
	}

	public void devolverFlutuante() {
		if(itemFlutuante == null) return;
		if(slotOrigem >= 0 && slotOrigem < BlocoBau.SLOTS && itensBau[slotOrigem] == null) {
			itensBau[slotOrigem] = itemFlutuante;
		} else {
			UI.jg.inv.addItemInv(itemFlutuante.nome, itemFlutuante.quantidade);
		}
		itemFlutuante = null;
	}

	@Override
	public void renderizar(SpriteBatch sb, BitmapFont fonte, float delta) {
		if(!aberta || rectsBau == null) return;

		// renderiza slots do bau
		for(int i = 0; i < rectsBau.length; i++) {
			final Rectangle r = rectsBau[i];
			sb.draw(UI.jg.inv.texSlot, r.x, r.y, r.width, r.height);
			if(itensBau[i] != null) {
				sb.draw(itensBau[i].textura, r.x + 5, r.y + 5, tamSlot - 10, tamSlot - 10);
				if(itensBau[i].quantidade > 1) {
					fonte.draw(sb, String.valueOf(itensBau[i].quantidade), r.x + 4, r.y + 16);
				}
			}
		}
		// item flutuante
		if(itemFlutuante != null) {
			final float px = posFlutuante.x - tamSlot / 2f;
			final float py = posFlutuante.y - tamSlot / 2f;
			sb.draw(itemFlutuante.textura, px, py, tamSlot, tamSlot);
			if(itemFlutuante.quantidade > 1) {
				fonte.draw(sb, String.valueOf(itemFlutuante.quantidade), px + 4, py + 16);
			}
		}
	}

	@Override
	public boolean processarToque(int telaX, int telaY, boolean pressionado) {
		if(!aberta) return false;
		posFlutuante.set(telaX, telaY);

		if(!pressionado) return true;

		// checa slots do bau
		for(int i = 0; i < rectsBau.length; i++) {
			if(!rectsBau[i].contains(telaX, telaY)) continue;
			clicarSlotBau(i);
			return true;
		}
		// checa inventario do jogador(quando bau aberto)
		final Inventario inv = UI.jg.inv;
		for(int i = 0; i < inv.barraRects.length; i++) {
			if(!inv.barraRects[i].contains(telaX, telaY)) continue;
			clicarSlotJogador(i, inv);
			return true;
		}
		if(inv.rects != null) {
			for(int i = 0; i < inv.quantSlots; i++) {
				if(inv.rects[i] == null || !inv.rects[i].contains(telaX, telaY)) continue;
				clicarSlotJogador(i, inv);
				return true;
			}
		}
		// clicou fora de tudo: fecha
		fechar();
		return false;
	}

	public void clicarSlotBau(int idc) {
		if(itemFlutuante == null) {
			if(itensBau[idc] == null) return;
			itemFlutuante = itensBau[idc];
			itensBau[idc] = null;
			slotOrigem = idc;
		} else {
			final Item dest = itensBau[idc];
			if(dest != null && dest.nome.equals(itemFlutuante.nome)) {
				dest.quantidade += itemFlutuante.quantidade;
				itemFlutuante = null;
			} else {
				itensBau[idc] = itemFlutuante;
				if(slotOrigem >= 0 && slotOrigem < BlocoBau.SLOTS) {
					itensBau[slotOrigem] = dest;
				} else if(dest != null) {
					UI.jg.inv.addItemInv(dest.nome, dest.quantidade);
				}
				itemFlutuante = null;
			}
		}
	}

	public void clicarSlotJogador(int idc, Inventario inv) {
		if(itemFlutuante == null) {
			if(inv.itens[idc] == null) return;
			itemFlutuante = inv.itens[idc];
			inv.itens[idc] = null;
			slotOrigem = -(idc + 1); // negativo = slot do jogador
		} else {
			final Item dest = inv.itens[idc];
			if(dest != null && dest.nome.equals(itemFlutuante.nome)) {
				dest.quantidade += itemFlutuante.quantidade;
				itemFlutuante = null;
			} else {
				inv.itens[idc] = itemFlutuante;
				if(slotOrigem >= 0 && slotOrigem < BlocoBau.SLOTS) {
					itensBau[slotOrigem] = dest;
				} else if(slotOrigem < 0) {
					final int fonteIdc = -(slotOrigem + 1);
					inv.itens[fonteIdc] = dest;
				}
				itemFlutuante = null;
			}
		}
	}

	@Override
	public boolean aberta() { return aberta; }

	@Override
	public void liberar() {
		if(aberta) fechar();
		rectsBau = null;
		itensBau = null;
	}
}
