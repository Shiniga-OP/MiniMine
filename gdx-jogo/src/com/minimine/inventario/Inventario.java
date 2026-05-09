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
    public int slotsV = 5, slotsH = 5;
    public int tamSlot = 64+16;
    public Texture texSlot;
    public Rectangle[] rects;
    public int invX, invY;

    public Item itemSendoArrastado = null;
    public int slotOrigem = -1;
    public int ponteiroArrastando = -1;

    public Item[] itens = new Item[quantSlots];
    public int slotSelecionado = 0;
    public boolean aberto = false;

    public int hotbarSlots = 5;
    public Rectangle[] rectsHotbar;
    public int hotbarY = 20;

    public Item itemFlutuante = null;
    public boolean itemFlutuanteVeioDaGrade = false;
    public int slotOrigemFlutuante = -1; // -1 se veio da grade
    public int slotGradeOrigem = -1; // slot da grade de origem, se veio dela
    public Vector2 posFlutuante = new Vector2();

    // === modo divisão por arrastar ===
    public boolean modoDivisao = false;
    public int quantidadeOriginalDivisao = 0;
    public int[] slotsDivisao = new int[30]; // slots visitados durante o arrastar
    public int qtdSlotsDivisao = 0;

    // === receita ===
    public int tamReceita = 56;
    public Item[] gradeReceita = new Item[9];
    public Item resultadoReceita = null;
    public Rectangle[] rectsGrade = new Rectangle[9];
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

        rects = new Rectangle[quantSlots];
        int i = 0;
        for(int y = 0; y < slotsV; y++) {
            for(int x = 0; x < slotsH; x++) {
                if(i >= quantSlots) break;
                final float sx = invX + (x * tamSlot);
                final float sy = invY + (y * tamSlot);
                rects[i] = new Rectangle(sx, sy, tamSlot, tamSlot);
                i++;
            }
        }
        rectsHotbar = new Rectangle[hotbarSlots];
        final int hotbarX = (v >> 1) - ((hotbarSlots * tamSlot) >> 1);
        for(int x = 0; x < hotbarSlots; x++) {
            final float sx = hotbarX + (x * tamSlot);
            rectsHotbar[x] = new Rectangle(sx, hotbarY, tamSlot, tamSlot);
        }
        // grade de receita: a direita do inventario, centralizada verticalmente
        final int gradeX = invX + slotsH * tamSlot + 40;
        final int gradeY = invY + ((slotsV * tamSlot) >> 1) - ((3 * tamReceita) >> 1);
        for(int l = 0; l < 3; l++) {
            for(int c = 0; c < 3; c++) {
                final float sx = gradeX + c * tamReceita;
                final float sy = gradeY + (2 - l) * tamReceita;
                rectsGrade[l * 3 + c] = new Rectangle(sx, sy, tamReceita, tamReceita);
            }
        }
        // slot de resultado: a direita da grade, centralizado
        final int resX = gradeX + 3 * tamReceita + 24;
        final int resY = gradeY + tamReceita;
        rectResultado = new Rectangle(resX, resY, tamReceita, tamReceita);
    }
    public void aoSoltar(int telaX, int telaY, int p) {
		if(modoDivisao) {
			modoDivisao = false;
			qtdSlotsDivisao = 0;
			if(itemFlutuante != null && itemFlutuante.quantidade <= 0) {
				itemFlutuante = null;
				slotOrigemFlutuante = -1;
				itemFlutuanteVeioDaGrade = false;
				slotGradeOrigem = -1;
			}
			return;
		}
		if(p != ponteiroArrastando || itemSendoArrastado == null) return;

		int slotDestino = -1;
		for(int i = 0; i < rectsHotbar.length; i++) {
			if(rectsHotbar[i].contains(telaX, telaY)) {
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
		} else {
			itens[slotOrigem] = itemSendoArrastado;
		}
		itemSendoArrastado = null;
		slotOrigem = -1;
		ponteiroArrastando = -1;
	}

    public void selecionarSlot(int slot, Jogador jogador) {
        slotSelecionado = slot;
        if(itens[slot] != null) jogador.item = itens[slot].nome;
        else jogador.item = "ar";
    }

    public void addItem(CharSequence nome, int quantidade) {
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

    public void rmItem(int slot, int quantidade) {
        if(itens[slot] != null) {
            itens[slot].quantidade -= quantidade;
            if(itens[slot].quantidade <= 0) {
                itens[slot] = null;
            }
        }
    }

    public void attReceita() {
        CharSequence[] nomes = new CharSequence[9];
        for(int i = 0; i < 9; i++) {
            nomes[i] = (gradeReceita[i] != null) ? gradeReceita[i].nome : null;
        }
        final ReceitaRegistro.Receita r = ReceitaRegistro.combinar(nomes);
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
            for(int i = 0; i < rectsHotbar.length; i++) {
                if(rectsHotbar[i].contains(telaX, telaY)) {
                    selecionarSlot(i, jogador);
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
                itemFlutuanteVeioDaGrade = false;
                slotOrigemFlutuante = -1;
                slotGradeOrigem = -1;
                // consome ingredientes
                for(int i = 0; i < 9; i++) gradeReceita[i] = null;
                resultadoReceita = null;
            }
            return;
        }
        // === slots da grade de receita ===
        for(int i = 0; i < 9; i++) {
            if(rectsGrade[i].contains(telaX, telaY)) {
                if(itemFlutuante == null) {
                    if(gradeReceita[i] != null && gradeReceita[i].nome.length() > 0) {
                        itemFlutuante = new Item(gradeReceita[i].nome, gradeReceita[i].textura, gradeReceita[i].quantidade);
                        itemFlutuanteVeioDaGrade = true;
                        slotGradeOrigem = i;
                        slotOrigemFlutuante = -1;
                        gradeReceita[i] = null;
                        attReceita();
                    }
                } else {
                    // o que tava na grade volta pro flutuante
                    final Item anteriorNaGrade = gradeReceita[i];
					if(anteriorNaGrade != null && anteriorNaGrade.nome.length() > 0) {
						gradeReceita[i] = new Item(itemFlutuante.nome, itemFlutuante.textura, itemFlutuante.quantidade);
						itemFlutuante = new Item(anteriorNaGrade.nome, anteriorNaGrade.textura, anteriorNaGrade.quantidade);
						itemFlutuanteVeioDaGrade = true;
						slotGradeOrigem = i;
						slotOrigemFlutuante = -1;
					} else {
						gradeReceita[i] = new Item(itemFlutuante.nome, itemFlutuante.textura, itemFlutuante.quantidade);
						itemFlutuante = null;
						itemFlutuanteVeioDaGrade = false;
						slotGradeOrigem = -1;
					}
					attReceita();
                }
                return;
            }
        }
        // === slots normais do inventario e hotbar ===
        int slotClicado = -1;
        for(int i = 0; i < rectsHotbar.length; i++) {
            if(rectsHotbar[i].contains(telaX, telaY)) {
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

        if(itemFlutuante == null) {
            if(itens[slotClicado] != null) {
                itemFlutuante = itens[slotClicado];
                itemFlutuanteVeioDaGrade = false;
                slotOrigemFlutuante = slotClicado;
                slotGradeOrigem = -1;
                itens[slotClicado] = null;
                modoDivisao = false;
                qtdSlotsDivisao = 0;
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
			itemFlutuanteVeioDaGrade = false;
			slotGradeOrigem = -1;
			return;
		} else {
			final Item destino = itens[slotClicado];
			if(destino != null && destino.nome.equals(itemFlutuante.nome)) {
				// agrupa
				destino.quantidade += itemFlutuante.quantidade;
				itemFlutuante = null;
				slotOrigemFlutuante = -1;
				return;
			}
			// so ativa divisão se não agrupou
			modoDivisao = true;
			quantidadeOriginalDivisao = itemFlutuante.quantidade;
			qtdSlotsDivisao = 0;
		}
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
		for(int i = 0; i < rectsHotbar.length; i++) {
			if(rectsHotbar[i].contains(telaX, telaY)) { slotAtual = i; break; }
		}
		if(slotAtual == -1 && aberto) {
			for(int i = 0; i < rects.length; i++) {
				if(rects[i].contains(telaX, telaY)) { slotAtual = i; break; }
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
		for(int i = 0; i < qtdSlotsDivisao; i++) {
			final int s = slotsDivisao[i];
			if(itens[s] == null)
				itens[s] = new Item(itemFlutuante.nome, itemFlutuante.textura, porcao);
			else
				itens[s].quantidade = porcao;
		}
		itemFlutuante.quantidade = quantidadeOriginalDivisao - porcao * qtdSlotsDivisao;
	}

    public void alternar() {
        if(aberto) {
            aberto = false;
            // devolve item flutuante
            if(itemFlutuante != null) {
                if(itemFlutuanteVeioDaGrade && slotGradeOrigem >= 0) {
                    gradeReceita[slotGradeOrigem].nome = itemFlutuante.nome;
                } else if(slotOrigemFlutuante >= 0 && itens[slotOrigemFlutuante] == null) {
                    itens[slotOrigemFlutuante] = itemFlutuante;
                } else {
                    addItem(itemFlutuante.nome, itemFlutuante.quantidade);
                }
                itemFlutuante = null;
                itemFlutuanteVeioDaGrade = false;
                slotOrigemFlutuante = -1;
                slotGradeOrigem = -1;
            }
            // devolve ingredientes da grade pro inventario
            for(int i = 0; i < 9; i++) {
                if(gradeReceita[i] != null && gradeReceita[i].nome.length() > 0) {
                    addItem(gradeReceita[i].nome, gradeReceita[i].quantidade);
                    gradeReceita[i] = null;
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
        // grade de receita
        for(int i = 0; i < 9; i++) {
            final Rectangle r = rectsGrade[i];
            sb.draw(texSlot, r.x, r.y, r.width, r.height);
            if(gradeReceita[i] != null && gradeReceita[i].nome.length() > 0) {
				final TextureRegion tex;
				final Item b = ItemRegistro.obter(gradeReceita[i].nome);
				if(b != null) tex = b.textura;
				else tex = Texturas.atlas.obter("terra");
				
				if(tex != null) sb.draw(tex, r.x + 4, r.y + 4, r.width - 8, r.height - 8);
				if(gradeReceita[i].quantidade > 1) {
					fonte.draw(sb, String.valueOf(gradeReceita[i].quantidade), r.x + 4, r.y + 16);
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
            final float px = posFlutuante.x - tamReceita / 2f;
            final float py = posFlutuante.y - tamReceita / 2f;
            sb.draw(itemFlutuante.textura, px, py, tamReceita, tamReceita);
            if(itemFlutuante.quantidade > 1) {
                fonte.draw(sb, String.valueOf(itemFlutuante.quantidade), px + 4, py + 16);
			}
        }
    }
}