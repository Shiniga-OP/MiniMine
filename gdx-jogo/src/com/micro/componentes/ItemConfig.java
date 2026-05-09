package com.micro.componentes;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.micro.janelas.PainelFatiado;
import com.micro.util.Acao;
/*
 * linha de configuração para uso dentro de PainelRolavel

 * modo NUMERICO: exibe [titulo][ valor ][ - ][ + ]
 *   - usa acaoMenos e acaoMais para alterar o valor
 *   - o rotulo de valor é atualizado externamente via rotuloValor.texto

 * Modo ALTERNAR: exibe [titulo][  estado ][ alterar ]
 *   - usa acaoToggle para alternar o valor
 *   - o rotulo de valor é atualizado externamente via rotuloValor.texto

 * uso:
 *   // numerico
 *   ItemConfig item = ItemConfig.numerico(x, y, largura, altura,
 *       "Raio Chunks:", "4", fonte, escala, pixelBranco, visualBotao,
 *       acaoMenos, acaoMais);

 *   // alternar
 *   ItemConfig item = ItemConfig.alternar(x, y, largura, altura,
 *       "Musicas:", "Ligado", fonte, escala, pixelBranco, visualBotao,
 *       acaoAlternar);
 */
public class ItemConfig extends Componente {
    public Rotulo rotuloLabel;
    public Rotulo rotuloValor;
    public Texture pixelBranco;
    public Color corFundo = new Color(0.18f, 0.18f, 0.22f, 1f);
    public Color corBorda = new Color(0.3f, 0.3f, 0.35f, 1f);

    // filhos interativos (botoes)
    public ItemBotao botaoA; // botao menos ou alterar
    public ItemBotao botaoB; // botao mais (so no modo numerico)

    public static ItemConfig numerico(
        float x, float y, float largura, float altura,
        String label, String valorInicial,
        BitmapFont fonte, float escala,
        Texture pixelBranco, PainelFatiado visual,
        Acao acaoMenos, Acao acaoMais
    ) {
        ItemConfig item = new ItemConfig(x, y, largura, altura, pixelBranco);

        float larguraBotao = 60;
        float larguraValor = 110;
        float margemV = 10;
        float alturaInterna = altura - margemV * 2;
        float larguraLabel = largura - larguraValor - larguraBotao * 2 - 30;

        item.rotuloLabel = new Rotulo(label, fonte, escala);
        item.rotuloLabel.x = 12;
        item.rotuloLabel.y = margemV;
        item.rotuloLabel.largura = larguraLabel;
        item.rotuloLabel.altura = alturaInterna;

        item.rotuloValor = new Rotulo(valorInicial, fonte, escala);
        item.rotuloValor.x = larguraLabel + 10;
        item.rotuloValor.y = margemV;
        item.rotuloValor.largura = larguraValor;
        item.rotuloValor.altura = alturaInterna;

        float xMenos = larguraLabel + larguraValor + 15;
        item.botaoA = new ItemBotao(xMenos, margemV, larguraBotao, alturaInterna,
									"-", fonte, escala, pixelBranco, acaoMenos);

        float xMais = xMenos + larguraBotao + 5;
        item.botaoB = new ItemBotao(xMais, margemV, larguraBotao, alturaInterna,
									"+", fonte, escala, pixelBranco, acaoMais);

        return item;
    }

    public static ItemConfig alternar(
        float x, float y, float largura, float altura,
        String label, String valorInicial,
        BitmapFont fonte, float escala,
        Texture pixelBranco, PainelFatiado visual,
        Acao acaoToggle
    ) {
        ItemConfig item = new ItemConfig(x, y, largura, altura, pixelBranco);

        float larguraBotao = 130;
        float larguraValor = 130;
        float margemV = 10;
        float alturaInterna = altura - margemV * 2;
        float larguraLabel = largura - larguraValor - larguraBotao - 30;

        item.rotuloLabel = new Rotulo(label, fonte, escala);
        item.rotuloLabel.x = 12;
        item.rotuloLabel.y = margemV;
        item.rotuloLabel.largura = larguraLabel;
        item.rotuloLabel.altura = alturaInterna;

        item.rotuloValor = new Rotulo(valorInicial, fonte, escala);
        item.rotuloValor.x = larguraLabel + 10;
        item.rotuloValor.y = margemV;
        item.rotuloValor.largura = larguraValor;
        item.rotuloValor.altura = alturaInterna;

        float xAlterar = larguraLabel + larguraValor + 15;
        item.botaoA = new ItemBotao(xAlterar, margemV, larguraBotao, alturaInterna,
									"Alterar", fonte, escala * 0.8f, pixelBranco, acaoToggle);
        item.botaoB = null;

        return item;
    }

    public ItemConfig(float x, float y, float largura, float altura, Texture pixelBranco) {
        super(x, y, largura, altura);
        this.pixelBranco = pixelBranco;
    }

    @Override
    public boolean aoTocar(float toqueX, float toqueY, boolean pressionado) {
        float relX = toqueX - x;
        float relY = toqueY - y;

        if(botaoB != null && botaoB.aoTocar(relX, relY, pressionado)) return true;
        if(botaoA != null && botaoA.aoTocar(relX, relY, pressionado)) return true;

        if(!pressionado) {
            // garante reinicio de estado mesmo clicando fora dos botoes
            if(botaoA != null) botaoA.aoTocar(relX, relY, false);
            if(botaoB != null) botaoB.aoTocar(relX, relY, false);
        }
        return contem(toqueX, toqueY);
    }

    @Override
    public void desenhar(SpriteBatch pincel, float delta, float paiX, float paiY) {
        float desenharX = paiX + x;
        float desenharY = paiY + y;

        // fundo
        pincel.setColor(corFundo);
        pincel.draw(pixelBranco, desenharX, desenharY, largura, altura);
        // separador inferior
        pincel.setColor(corBorda);
        pincel.draw(pixelBranco, desenharX, desenharY, largura, 1);

        pincel.setColor(Color.WHITE);
        rotuloLabel.desenhar(pincel, delta, desenharX, desenharY);
        rotuloValor.desenhar(pincel, delta, desenharX, desenharY);
        if(botaoA != null) botaoA.desenhar(pincel, delta, desenharX, desenharY);
        if(botaoB != null) botaoB.desenhar(pincel, delta, desenharX, desenharY);
    }
}

