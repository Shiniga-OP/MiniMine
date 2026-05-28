package com.minimine.inventario;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Rectangle;
import com.minimine.graficos.Texturas;
import com.badlogic.gdx.math.Vector2;
import com.minimine.mundo.blocos.Bloco;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.minimine.entidades.Jogador;

public class Inventario {
    public Jogador jogador;
    public int quantSlots = 25;
    public static final int SLOTS_GRADE = 9;
    public int slotsV = 5, slotsH = 5;
    public int tamSlot = 64+16;
    public Texture texSlot;
    public Rectangle[] rects;
    public int invX, invY;

    public Item itemSendoArrastado = null;
    public int slotOrigem = -1;
    public int ponteiroArrastando = -1;

    // itens[0..quantSlots-1] = inventário normal, itens[quantSlots..quantSlots+SLOTS_GRADE-1] = grade de receita
    public final Item[] itens = new Item[quantSlots + SLOTS_GRADE];
	public final String[] nomesReceita = new String[SLOTS_GRADE];
    public int slotSelecionado = 0;
    public boolean aberto = false;

    public int barraSlots = 5;
    public Rectangle[] barraRects;
    public int barraY = 20;

    public Item itemFlutuante = null;
    public int slotOrigemFlutuante = -1;
    public Vector2 posFlutuante = new Vector2();

    // === modo divisão por arrastar ===
    public boolean modoDivisao = false;
    public int quantidadeOriginalDivisao = 0;
    public int[] slotsDivisao = new int[30];
    public int qtdSlotsDivisao = 0;

    // === receita ===
    // rectsGrade[i] aponta pro mesmo rect que rects[quantSlots + i]
    public Rectangle[] rectsGrade = new Rectangle[SLOTS_GRADE];
    public Item resultadoReceita = null;
    public Rectangle rectResultado;

    public Inventario(Jogador jogador) {
        texSlot = Texturas.base;
        if(texSlot != null) aoAjustar(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        else Gdx.app.log("[Inventario]", "[ERRO]: Textura de slot nula");
        this.jogador = jogador;
    }

    public void aoAjustar(int v, int h) {
        invX = (v >> 1) - ((slotsH * tamSlot) >> 1);
        invY = (h >> 1) - ((slotsV * tamSlot) >> 1);

        rects = new Rectangle[quantSlots + SLOTS_GRADE];
        int i = 0;
        for(int y = 0; y < slotsV; y++) {
            for(int x = 0; x < slotsH; x++) {
                if(i >= quantSlots) break;
                rects[i] = new Rectangle(invX + x * tamSlot, invY + y * tamSlot, tamSlot, tamSlot);
                i++;
            }
        }
        barraRects = new Rectangle[barraSlots];
        final int hotbarX = (v >> 1) - ((barraSlots * tamSlot) >> 1);
        for(int x = 0; x < barraSlots; x++) {
            barraRects[x] = new Rectangle(hotbarX + x * tamSlot, barraY, tamSlot, tamSlot);
        }
        // grade de receita: a direita do inventario, centralizada verticalmente
        final int gradeX = invX + slotsH * tamSlot + 40;
        final int gradeY = invY + ((slotsV * tamSlot) >> 1) - ((3 * tamSlot) >> 1);
        for(int l = 0; l < 3; l++) {
            for(int c = 0; c < 3; c++) {
                final int idx = quantSlots + l * 3 + c;
                rects[idx] = new Rectangle(gradeX + c * tamSlot, gradeY + (2 - l) * tamSlot, tamSlot, tamSlot);
                rectsGrade[l * 3 + c] = rects[idx];
            }
        }
        // slot de resultado: a direita da grade, centralizado
        final int resX = gradeX + 3 * tamSlot + 24;
        final int resY = gradeY + tamSlot;
        rectResultado = new Rectangle(resX, resY, tamSlot, tamSlot);
    }
    public void aoSoltar(int telaX, int telaY, int p) {
		if(modoDivisao) {
			modoDivisao = false;
			qtdSlotsDivisao = 0;
			if(itemFlutuante != null && itemFlutuante.quantidade <= 0) {
				itemFlutuante = null;
				slotOrigemFlutuante = -1;
			}
			return;
		}
		if(p != ponteiroArrastando || itemSendoArrastado == null) return;

		int slotDestino = -1;
		for(int i = 0; i < barraRects.length; i++) {
			if(barraRects[i].contains(telaX, telaY)) {
				slotDestino = i;
				break;
			}
		}
		if(slotDestino == -1 && aberto) {
			for(int i = 0; i < rects.length; i++) {
				if(rects[i].contains(telaX, telaY)) {
					slotDestino = i;
					break;
				}
			}
		}
		if(slotDestino != -1) {
			final Item itemNoDestino = itens[slotDestino];
			if(itemNoDestino != null && itemNoDestino.nome.equals(itemSendoArrastado.nome)) {
				itemNoDestino.quantidade += itemSendoArrastado.quantidade;
				itens[slotOrigem] = null;
			} else {
				itens[slotDestino] = itemSendoArrastado;
				itens[slotOrigem] = itemNoDestino;
			}
			if(slotDestino >= quantSlots) attReceita();
		} else {
			itens[slotOrigem] = itemSendoArrastado;
		}
		itemSendoArrastado = null;
		slotOrigem = -1;
		ponteiroArrastando = -1;
	}

    public void selecionarSlot(int slot) {
        slotSelecionado = slot;
        if(itens[slot] != null) jogador.item = itens[slot].nome;
        else jogador.item = "ar";
    }

    public void addItem(String nome, int quantidade) {
        if(itens[slotSelecionado] != null && itens[slotSelecionado].nome.equals(nome)) {
            itens[slotSelecionado].quantidade += quantidade;
            return;
        }
        for(int i = 0; i < itens.length; i++) {
            if(itens[i] != null && itens[i].nome.equals(nome)) {
                itens[i].quantidade += quantidade;
                return;
            }
        }
        for(int i = 0; i < itens.length; i++) {
            if(itens[i] == null) {
                final TextureRegion textura;
                final Item b = ItemRegistro.obter(nome);

				if(b != null) textura = b.textura;
                else {
                    Gdx.app.log("[Inventario]", "textura não encontrada para: " + nome);
                    textura = Texturas.atlas.obter("terra");
                }
                itens[i] = new Item(nome, textura, quantidade);
                return;
            }
        }
    }

    // igual a addItem mas so usa slots do inventario normal(não a grade de receita)
    public void addItemInv(String nome, int quantidade) {
        if(slotSelecionado < quantSlots && itens[slotSelecionado] != null && itens[slotSelecionado].nome.equals(nome)) {
            itens[slotSelecionado].quantidade += quantidade;
            return;
        }
        for(int i = 0; i < quantSlots; i++) {
            if(itens[i] != null && itens[i].nome.equals(nome)) {
                itens[i].quantidade += quantidade;
                return;
            }
        }
        for(int i = 0; i < quantSlots; i++) {
            if(itens[i] == null) {
                final TextureRegion textura;
                final Item b = ItemRegistro.obter(nome);
                if(b != null) textura = b.textura;
                else {
                    Gdx.app.log("[Inventario]", "textura não encontrada para: " + nome);
                    textura = Texturas.atlas.obter("terra");
                }
                itens[i] = new Item(nome, textura, quantidade);
                return;
            }
        }
    }

    public void rmItem(int slot, int quantidade) {
        if(itens[slot] != null) {
            itens[slot].quantidade -= quantidade;
            if(itens[slot].quantidade <= 0) {
                itens[slot] = null;
            }
        }
    }

    public void attReceita() {
        for(int i = 0; i < SLOTS_GRADE; i++) {
            final Item it = itens[quantSlots + i];
            nomesReceita[i] = (it != null) ? it.nome : null;
        }
        final ReceitaRegistro.Receita r = ReceitaRegistro.combinar(nomesReceita);
        if(r == null) {
            resultadoReceita = null;
            return;
        }
		final TextureRegion tex;
		final Item b = ItemRegistro.obter(r.resultado);
		if(b != null) tex = b.textura;
		else tex = Texturas.atlas.obter("terra");

        resultadoReceita = new Item(r.resultado, tex, r.quantidade);
    }

    public void aoTocar(int telaX, int telaY, int p) {
        if(!aberto) {
            for(int i = 0; i < barraRects.length; i++) {
                if(barraRects[i].contains(telaX, telaY)) {
                    selecionarSlot(i);
                    return;
                }
            }
            return;
        }
        posFlutuante.set(telaX, telaY);

        // === slot de resultado ===
        if(rectResultado.contains(telaX, telaY)) {
			if(resultadoReceita == null) return;
			if(itemFlutuante == null) {
				itemFlutuante = resultadoReceita;
				slotOrigemFlutuante = -1;
				// consome 1 de cada ingrediente usado
				for(int i = 0; i < SLOTS_GRADE; i++) {
					final Item it = itens[quantSlots + i];
					if(it != null) {
						it.quantidade--;
						if(it.quantidade <= 0) itens[quantSlots + i] = null;
					}
				}
				attReceita();
				resultadoReceita = null;
			}
			return;
		}
        // === todos os slots (inventario, hotbar e grade) tratados igual ===
        int slotClicado = -1;
        for(int i = 0; i < barraRects.length; i++) {
            if(barraRects[i].contains(telaX, telaY)) {
                slotClicado = i;
                break;
            }
        }
        if(slotClicado == -1) {
            for(int i = 0; i < rects.length; i++) {
                if(rects[i].contains(telaX, telaY)) {
                    slotClicado = i;
                    break;
                }
            }
        }
        if(slotClicado == -1) return;

        final boolean ehGrade = slotClicado >= quantSlots;

        if(itemFlutuante == null) {
            if(itens[slotClicado] != null) {
                itemFlutuante = itens[slotClicado];
                slotOrigemFlutuante = slotClicado;
                itens[slotClicado] = null;
                modoDivisao = false;
                qtdSlotsDivisao = 0;
                if(ehGrade) attReceita();
            }
        } else if(modoDivisao) {
			modoDivisao = false;
			qtdSlotsDivisao = 0;
			if(itemFlutuante.quantidade > 0) {
				final Item destino = itens[slotClicado];
				if(destino == null) itens[slotClicado] = itemFlutuante;
				else if(destino.nome.equals(itemFlutuante.nome)) destino.quantidade += itemFlutuante.quantidade;
			}
			itemFlutuante = null;
			slotOrigemFlutuante = -1;
			if(ehGrade) attReceita();
			return;
		} else {
			final Item destino = itens[slotClicado];
			if(destino != null && destino.nome.equals(itemFlutuante.nome)) {
				destino.quantidade += itemFlutuante.quantidade;
				itemFlutuante = null;
				slotOrigemFlutuante = -1;
			} else if(destino != null) {
				itens[slotClicado] = itemFlutuante;
				if(slotOrigemFlutuante >= 0) itens[slotOrigemFlutuante] = destino;
				itemFlutuante = null;
				slotOrigemFlutuante = -1;
			} else {
				modoDivisao = true;
				quantidadeOriginalDivisao = itemFlutuante.quantidade;
				qtdSlotsDivisao = 0;
			}
			if(ehGrade) attReceita();
		}
		attReceita();
    }

    public final void moverFlutuante(final int telaX, final int telaY) {
        if(itemFlutuante == null) return;
        posFlutuante.set(telaX, telaY);
    }

    public final void aoArrastar(final int telaX, final int telaY) {
		if(itemFlutuante == null) return;
		posFlutuante.set(telaX, telaY);

		if(!modoDivisao) return;

		int slotAtual = -1;
		for(int i = 0; i < barraRects.length; i++) {
			if(barraRects[i].contains(telaX, telaY)) {
				slotAtual = i;
				break;
			}
		}
		if(slotAtual == -1 && aberto) {
			for(int i = 0; i < rects.length; i++) {
				if(rects[i].contains(telaX, telaY)) {
					slotAtual = i;
					break;
				}
			}
		}
		if(slotAtual == -1) return;

		if(slotAtual == slotOrigemFlutuante) return;
		for(int i = 0; i < qtdSlotsDivisao; i++) {
			if(slotsDivisao[i] == slotAtual) return;
		}
		final Item itemNoSlot = itens[slotAtual];
		if(itemNoSlot != null && !itemNoSlot.nome.equals(itemFlutuante.nome)) return;

		slotsDivisao[qtdSlotsDivisao++] = slotAtual;

		final int porcao = quantidadeOriginalDivisao / qtdSlotsDivisao;
		if(porcao < 1) {
			qtdSlotsDivisao--;
			return;
		}
		boolean tocouGrade = false;
		for(int i = 0; i < qtdSlotsDivisao; i++) {
			final int s = slotsDivisao[i];
			if(itens[s] == null)
				itens[s] = new Item(itemFlutuante.nome, itemFlutuante.textura, porcao);
			else
				itens[s].quantidade = porcao;
			if(s >= quantSlots) tocouGrade = true;
		}
		itemFlutuante.quantidade = quantidadeOriginalDivisao - porcao * qtdSlotsDivisao;
		if(tocouGrade) attReceita();
	}

    public void alternar() {
        if(aberto) {
            aberto = false;
            if(itemFlutuante != null) {
                // se origem era a grade, não devolve pra grade (vai ser limpa logo abaixo)
                if(slotOrigemFlutuante >= 0 && slotOrigemFlutuante < quantSlots && itens[slotOrigemFlutuante] == null) {
                    itens[slotOrigemFlutuante] = itemFlutuante;
                } else {
                    addItemInv(itemFlutuante.nome, itemFlutuante.quantidade);
                }
                itemFlutuante = null;
                slotOrigemFlutuante = -1;
            }
            // devolve ingredientes da grade pro inventario
            for(int i = 0; i < SLOTS_GRADE; i++) {
                final Item it = itens[quantSlots + i];
                if(it != null && it.nome.length() > 0) {
                    addItemInv(it.nome, it.quantidade);
                    itens[quantSlots + i] = null;
                }
            }
            resultadoReceita = null;
            Gdx.input.setCursorCatched(true);
        } else {
            aberto = true;
            Gdx.input.setCursorCatched(false);
        }
    }
    // renderiza inventario + grade de receita
    public void renderizar(SpriteBatch sb, BitmapFont fonte) {
        if(!aberto) return;

        // slots do inventario
        for(int i = 0; i < rects.length; i++) {
            final Rectangle r = rects[i];
            sb.draw(texSlot, r.x, r.y, r.width, r.height);
            if(itens[i] != null) {
                sb.draw(itens[i].textura, r.x + 4, r.y + 4, r.width - 8, r.height - 8);
                if(itens[i].quantidade > 1) {
                    fonte.draw(sb, String.valueOf(itens[i].quantidade), r.x + 4, r.y + 16);
				}
            }
        }
        // slot de resultado
        sb.draw(texSlot, rectResultado.x, rectResultado.y, rectResultado.width, rectResultado.height);
        if(resultadoReceita != null) {
            sb.draw(resultadoReceita.textura, rectResultado.x + 4, rectResultado.y + 4,
					rectResultado.width - 8, rectResultado.height - 8);
            if(resultadoReceita.quantidade > 1)
                fonte.draw(sb, String.valueOf(resultadoReceita.quantidade),
						   rectResultado.x + 4, rectResultado.y + 16);
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
}
