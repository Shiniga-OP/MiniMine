package com.minimine.entidades;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Rectangle;
import com.minimine.graficos.Texturas;
import com.badlogic.gdx.math.Vector2;
import com.minimine.mundo.ChunkUtil;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.minimine.mundo.blocos.Bloco;
import com.minimine.ui.UI;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class Inventario {
	public Jogador jogador;
    public int quantSlots = 25;
    public int slotsV = 5, slotsH = 5;
    public int tamSlot = 64+16;
    public Texture texSlot;
    public Sprite[] sprites;
    public Rectangle[] rects;
    public int invX, invY;

	public Item itemSendoArrastado = null;
    public int slotOrigem = -1;
    public int ponteiroArrastando = -1; // ID do toque que ta arrastando

    public Item[] itens = new Item[quantSlots];
    public int slotSelecionado = 0;
    public boolean aberto = false;

    public int hotbarSlots = 5;
    public Rectangle[] rectsHotbar;
    public Sprite[] spritesHotbar;
    public int hotbarY = 20;

	public Item itemFlutuante = null;
    public int slotOrigemFlutuante = -1;
    public Vector2 posFlutuante = new Vector2(); // posicao visual do item flutuante

    public Inventario(Jogador jogador) {
        texSlot = Texturas.texs.get("slot");
        aoAjustar(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		this.jogador = jogador;
    }

    public void aoAjustar(int v, int h) {
        invX = v / 2 - (slotsH * tamSlot) / 2;
        invY = h / 2 - (slotsV * tamSlot) / 2;
        // slots do inventario completo:
        sprites = new Sprite[quantSlots];
        rects = new Rectangle[quantSlots];

        int i = 0;
        for(int y = 0; y < slotsV; y++) {
            for(int x = 0; x < slotsH; x++) {
                if(i >= quantSlots) break;
                Sprite s = new Sprite(texSlot);
                s.setSize(tamSlot, tamSlot);
                s.setPosition(invX + (x * tamSlot), invY + (y * tamSlot));
                Rectangle rect = new Rectangle(s.getX(), s.getY(), s.getWidth(), s.getHeight());
                sprites[i] = s;
                rects[i] = rect;
                i++;
            }
        }
        //  slots da hotbar:
        spritesHotbar = new Sprite[hotbarSlots];
        rectsHotbar = new Rectangle[hotbarSlots];

        int hotbarX = v / 2 - (hotbarSlots * tamSlot) / 2;
        for(int x = 0; x < hotbarSlots; x++) {
            Sprite s = new Sprite(texSlot);
            s.setSize(tamSlot, tamSlot);
            s.setPosition(hotbarX + (x * tamSlot), hotbarY);
            Rectangle rect = new Rectangle(s.getX(), s.getY(), s.getWidth(), s.getHeight());
            spritesHotbar[x] = s;
            rectsHotbar[x] = rect;
        }
    }

    public void aoSoltar(int telaX, int telaY, int p) {
        // se o ponteiro que foi solto nao é o que tava arrastando,  da gelo no insta K-K-K-K
        if(p != ponteiroArrastando || itemSendoArrastado == null) return;

        int slotDestino = -1;
        // procura o slot de destino na hotbar
        for(int i = 0; i < rectsHotbar.length; i++) {
            if(rectsHotbar[i].contains(telaX, telaY)) {
                slotDestino = i;
                break;
            }
        }
        // se não achou, procura no inv
        if(slotDestino == -1 && aberto) { // segurança
            for(int i = 0; i < rects.length; i++) {
                if(rects[i].contains(telaX, telaY)) {
                    slotDestino = i;
                    break;
                }
            }
        }
        // logica de troca
        if(slotDestino != -1) {
            // troca os itens(itemNoDestino pode ser null se o slot estiver vazio)
            Item itemNoDestino = itens[slotDestino];
            itens[slotDestino] = itemSendoArrastado; // coloca o item arrastado no destino
            itens[slotOrigem] = itemNoDestino;      // coloca o item do destino na origem
        } else {
            // destino invalido(soltou fora de um slot): retorna o item para a origem
            itens[slotOrigem] = itemSendoArrastado;
        }
        // retorna ao estado de arrasto
        itemSendoArrastado = null;
        slotOrigem = -1;
        ponteiroArrastando = -1;
    }

    public void selecionarSlot(int slot, Jogador jogador) {
        slotSelecionado = slot;
        if(itens[slot] != null) {
            jogador.item = itens[slot].nome;
        } else {
            jogador.item = "ar";
        }
    }

    public void addItem(CharSequence nome, int quantidade) {
		// se o slot tem o mesmo item:
		if(itens[slotSelecionado] != null && itens[slotSelecionado].nome.equals(nome)) {
			itens[slotSelecionado].quantidade += quantidade;
			return;
		}
		// se o slot atual é diferente, coloca o item onde tiver espaço:
		for(int i = 0; i < itens.length; i++) {
			if(itens[i] != null && itens[i].nome.equals(nome)) {
				itens[i].quantidade += quantidade;
				return;
			}
		}
		// senao, colocar no primeiro slot vazio:
		for(int i = 0; i < itens.length; i++) {
			if(itens[i] == null) {
				TextureRegion textura = null;

				for(Bloco b : Bloco.blocos) {
					if(b == null) continue;
					if(b.nome.equals(nome)) {
						// pega a textura do bloco
						textura = Texturas.atlas.obter(b.lados);
						break;
					}
				}
				if(textura == null) {
					Gdx.app.log("[Inventario]", "textura não encontrada para: " + nome);
					textura = Texturas.atlas.obter("terra"); // padrão
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

    public void att() {
		if(spritesHotbar == null || spritesHotbar.length == 0 ||
		   itens == null || itens.length == 0) return;
		// hotbar:
		for(int i = 0; i < spritesHotbar.length; i++) {
			if(spritesHotbar[i] == null) continue;
			spritesHotbar[i].draw(UI.sb);

			if(i == slotSelecionado) {
				UI.sb.setColor(1, 1, 1, 0.5f);
				spritesHotbar[i].draw(UI.sb);
				UI.sb.setColor(1, 1, 1, 1);
			}
			// apenas checa se temum item no slot
			// se o item foi pego, itens[i] é null e o desenho é ignorado
			if(itens[i] != null) {
				Sprite itemSprite = new Sprite(itens[i].textura);
				itemSprite.setSize(tamSlot - 10, tamSlot - 10);
				itemSprite.setPosition(spritesHotbar[i].getX() + 5, spritesHotbar[i].getY() + 5);
				itemSprite.draw(UI.sb);

				if(itens[i].quantidade > 1) {
					UI.fonte.draw(UI.sb, String.valueOf(itens[i].quantidade), 
					spritesHotbar[i].getX() + tamSlot - 15, 
					spritesHotbar[i].getY() + 15);
				}
			}
		}
		// inventario:
		if(aberto) {
			for(int i = 0; i < sprites.length; i++) {
				sprites[i].draw(UI.sb);
				// checa se tem um item no slot
				if(itens[i] != null) {
					Sprite itemSprite = new Sprite(itens[i].textura);

					itemSprite.setSize(tamSlot - 5, tamSlot - 5); 
					itemSprite.setPosition(sprites[i].getX() + 5, sprites[i].getY() + 5);
					itemSprite.draw(UI.sb);

					if(itens[i].quantidade > 1) {
						UI.fonte.draw(UI.sb, String.valueOf(itens[i].quantidade), 
									  sprites[i].getX() + tamSlot - 15, 
									  sprites[i].getY() + 15);
					}
				}
			}
		}
		if(itemFlutuante != null) {
			Sprite itemSprite = new Sprite(itemFlutuante.textura);
			itemSprite.setSize(tamSlot - 10, tamSlot - 10); 

			// centraliza o sprite na posição do ultimo toque/arrasto posFlutuante)
			itemSprite.setPosition(posFlutuante.x - itemSprite.getWidth() / 2, 
								   posFlutuante.y - itemSprite.getHeight() / 2);
			itemSprite.draw(UI.sb);
			// renderiza a quantidade
			if(itemFlutuante.quantidade > 1) {
				UI.fonte.draw(UI.sb, String.valueOf(itemFlutuante.quantidade), 
							  itemSprite.getX() + tamSlot - 15, 
							  itemSprite.getY() + 15);
			}
		}
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
        // encontrar qual slot foi clicado
        int slotClicado = -1;
        // lrocura na hotbar
        for(int i = 0; i < rectsHotbar.length; i++) {
            if(rectsHotbar[i].contains(telaX, telaY)) {
                slotClicado = i;
                break;
            }
        }
        // procura no inventario se não achou na hotbar)
        if(slotClicado == -1) {
            for(int i = 0; i < rects.length; i++) {
                if(rects[i].contains(telaX, telaY)) {
                    slotClicado = i;
                    break;
                }
            }
        }
        // se não clicou em nenhum slot, não faz nada
        if(slotClicado == -1)  return;
        // logica de pegar/soltar/trocar
        posFlutuante.set(telaX, telaY); // atualiza a posicao

        if(itemFlutuante == null) {
            // nal segurando nada
            // se o slot clicado tem um item, pega ele, ih, la ele rapaiz
            if(itens[slotClicado] != null) {
                itemFlutuante = itens[slotClicado];
                slotOrigemFlutuante = slotClicado;
                itens[slotClicado] = null;
            }
        } else {
            // segurando um item
            // trocamos o item flutuante com o item do slot clicado
            Item itemNoSlot = itens[slotClicado]; // guarda o item do slot(pode ser null)
            itens[slotClicado] = itemFlutuante; // coloca o item flutuante no slot
            itemFlutuante = itemNoSlot; // o item do slot(ou null) agora é o flutuante
            // se o novo item flutuante for null (trocamos com slot vazio), resetamos a origem.
            if(itemFlutuante == null) {
                slotOrigemFlutuante = -1;
            } else {
                // se pegamos um novo item, a nova origem"é este slot
                slotOrigemFlutuante = slotClicado;
            }
        }
    }

    public void aoArrastar(int telaX, int telaY, int p) {
        if(itemFlutuante != null) posFlutuante.set(telaX, telaY);
    }

    public void alternar() {
		if(aberto) {
			aberto = false;
			if(itemFlutuante != null) {
				// tenta mansar pro slot de origem
				if(itens[slotOrigemFlutuante] == null) {
					itens[slotOrigemFlutuante] = itemFlutuante;
				} else {
					// se o slot de origem foi ocupado procura o primeiro vazio
					addItem(itemFlutuante.nome, itemFlutuante.quantidade);
				}
				itemFlutuante = null;
				slotOrigemFlutuante = -1;
			}
		} else {
			aberto = true;
		}
    }

    public static class Item {
        public CharSequence nome;
        public TextureRegion textura;
        public int quantidade;

        public Item(CharSequence nome, TextureRegion textura, int quantidade) {
            this.nome = nome;
            this.textura = textura;
            this.quantidade = quantidade;
        }
    }
}

