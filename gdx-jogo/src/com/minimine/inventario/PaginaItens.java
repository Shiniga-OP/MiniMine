package com.minimine.inventario;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.minimine.entidades.Jogador;
import com.minimine.mundo.blocos.Bloco;

import java.util.ArrayList;
import java.util.List;
import com.minimine.graficos.Texturas;
/*
 * catalogo de blocos para o modo criativo
 * abre ao clicar no botão de catalogo no inventario(criativo e espectador)
 * grade 6x5 = 30 itens por pagina
 * campo de pesquisa filtra em todos os blocos e remonta as paginas com os resultados
 * setas < > navegam entre páginas
 * clicar num bloco adiciona 64 unidades ao inventario do jogador
 */
public class PaginaItens {
    public static final int COLUNAS = 6;
    public static final int LINHAS = 5;
    public static final int POR_PAGINA = COLUNAS * LINHAS; // 30
    public static final int TAM_SLOT = 56;
    public static final int ESPACO = 4;
    public static final int PASSO = TAM_SLOT + ESPACO;

    public boolean aberta = false;

    // pesquisa
    public String filtro = "";
    public boolean digitando = false; // true enquanto o campo estiver focado

    // paginação
    public int pagina = 0; // indice da página atual
    public int totalPaginas = 1;

    // lista filtrada de blocos(remontada sempre que filtro ou blocos mudam)
    public final List<Item> filtrados = new ArrayList<>();

    // rects dos 30 slots visíveis
    public final Rectangle[] rects = new Rectangle[POR_PAGINA];

    // rects dos botões de navegação e do campo de pesquisa
    public Rectangle rectAnterior = new Rectangle();
    public Rectangle rectProximo = new Rectangle();
    public Rectangle rectCampo = new Rectangle();

    // posição da grade na tela
    public int origemX, origemY;

    // largura total da grade
    public static final int LARGURA_GRADE = COLUNAS * PASSO - ESPACO;
    // altura total da grade
    public static final int ALTURA_GRADE = LINHAS  * PASSO - ESPACO;
    // altura total do painel(campo + grade + nav)
    public static final int ALTURA_PAINEL = 44 + 8 + ALTURA_GRADE + 8 + 36;

    public PaginaItens() {
        for(int i = 0; i < POR_PAGINA; i++) rects[i] = new Rectangle();
    }

    // chama quando a tela é redimensionada ou ao abrir pela primeira vez
    public void aoAjustar(int telaV, int telaH) {
        origemX = telaV / 2 - LARGURA_GRADE / 2;
        // campo de pesquisa em cima, grade no meio, navegação em baixo
        final int topoY = telaH / 2 + ALTURA_PAINEL / 2;

        // campo de pesquisa
        rectCampo.set(origemX, topoY - 44, LARGURA_GRADE, 40);

        // grade de slots
        origemY = (int)(rectCampo.y - 8 - ALTURA_GRADE);
        for(int linha = 0; linha < LINHAS; linha++) {
            for(int col = 0; col < COLUNAS; col++) {
                int i = linha * COLUNAS + col;
                rects[i].set(
                    origemX + col * PASSO,
                    origemY + (LINHAS - 1 - linha) * PASSO,
                    TAM_SLOT, TAM_SLOT);
            }
        }
        // botões de navegação abaixo da grade
        float navY = origemY - 8 - 32;
        rectAnterior.set(origemX, navY, 60, 32);
        rectProximo .set(origemX + LARGURA_GRADE - 60, navY, 60, 32);
    }

    // abre a tela
    public void abrir(int telaV, int telaH, Inventario inv) {
        if(aberta) return;
        aberta = true;
        filtro = "";
        pagina = 0;
        digitando = false;
        inv.aberto = false;
        aoAjustar(telaV, telaH);
        reconstruirFiltro();
    }

    public void fechar() {
        aberta = false;
        digitando = false;
        filtro = "";
        Gdx.input.setOnscreenKeyboardVisible(false);
        Gdx.input.setCursorCatched(true);
    }

    // reconstroi a lista filtrada e recalcula totalPaginas
    public void reconstruirFiltro() {
        filtrados.clear();
        final String q = filtro.toLowerCase().trim();
        for(Item b : ItemRegistro.todos()) {
            if(b == null) continue;
            if(q.isEmpty() || ("" + b.nome).toLowerCase().contains(q)) {
                filtrados.add(b);
            }
        }
        totalPaginas = Math.max(1, (filtrados.size() + POR_PAGINA - 1) / POR_PAGINA);
        if(pagina >= totalPaginas) pagina = totalPaginas - 1;
    }

    // digitar caractere no campo de pesquisa
    public void digitarCaractere(char c) {
        if(!digitando) return;
        if(c == '\b') {
            if(filtro.length() > 0) filtro = filtro.substring(0, filtro.length() - 1);
        } else if(c >= 32 && filtro.length() < 32) {
            filtro += c;
        }
        pagina = 0;
        reconstruirFiltro();
    }

    // toque na tela, retorna true se consumiu
    public boolean aoTocar(int telaX, int telaY, Jogador jogador) {
        if(!aberta) return false;

        // campo de pesquisa
        if(rectCampo.contains(telaX, telaY)) {
            digitando = true;
            Gdx.input.setOnscreenKeyboardVisible(true);
            return true;
        }
        digitando = false;
        Gdx.input.setOnscreenKeyboardVisible(false);

        // navegação
        if(rectAnterior.contains(telaX, telaY)) {
            if(pagina > 0) pagina--;
            return true;
        }
        if(rectProximo.contains(telaX, telaY)) {
            if(pagina < totalPaginas - 1) pagina++;
            return true;
        }
        // slots
        final int base = pagina * POR_PAGINA;
        for(int i = 0; i < POR_PAGINA; i++) {
            if(rects[i].contains(telaX, telaY)) {
                final int idc = base + i;
                if(idc < filtrados.size()) {
                    Item b = filtrados.get(idc);
                    jogador.inv.addItem(b.nome, 64);
                }
                return true;
            }
        }
        return true; // consome tudo enquanto aberta
    }
    // renderização, chamado dentro do sb.begin()/end() de UI
    public void renderizar(SpriteBatch sb, BitmapFont fonte) {
        if(!aberta) return;

        final int base = pagina * POR_PAGINA;

        // fundo semitransparente dos slots
        for(int i = 0; i < POR_PAGINA; i++) {
            final int idc = base + i;
            sb.draw(Texturas.base, rects[i].x, rects[i].y, rects[i].width, rects[i].height);
            if(idc < filtrados.size()) {
                final Item b = filtrados.get(idc);
				
				sb.draw(b.textura,
					rects[i].x + 4, rects[i].y + 4,
					TAM_SLOT - 8,   TAM_SLOT - 8
				);
            }
        }
        // campo de pesquisa: fundo + texto
        sb.draw(Texturas.base, rectCampo.x, rectCampo.y, rectCampo.width, rectCampo.height);
        final String textoExibido = filtro.isEmpty() && !digitando ? "Pesquisar..." : filtro + (digitando ? "|" : "");
        fonte.draw(sb, textoExibido, rectCampo.x + 8, rectCampo.y + rectCampo.height - 10);

        // botões de navegação: fundo + label
        sb.draw(Texturas.base, rectAnterior.x, rectAnterior.y, rectAnterior.width, rectAnterior.height);
        fonte.draw(sb, "<", rectAnterior.x + 22, rectAnterior.y + rectAnterior.height - 8);

        sb.draw(Texturas.base, rectProximo.x, rectProximo.y, rectProximo.width, rectProximo.height);
        fonte.draw(sb, ">", rectProximo.x + 22, rectProximo.y + rectProximo.height - 8);

        // indicador de pagina centralizado
        final String indicador = (pagina + 1) + "/" + totalPaginas;
        fonte.draw(sb, indicador,
				   rectAnterior.x + LARGURA_GRADE / 2f - 10,
				   rectAnterior.y + rectAnterior.height - 8);
    }
}
