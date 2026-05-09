package com.micro.componentes;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import java.util.ArrayList;
import java.util.List;
/*
 * linha composta para uso dentro de PainelRolavel
 * agrupa sub-componentes lado a lado
 * e delega toque/soltura para cada um deles

 * uso:
 *   ItemLinha linha = new ItemLinha(x, y, largura, altura, pixelBranco);
 *   linha.addFilho(rotulo);
 *   linha.addFilho(botaoEditar);
 *   linha.addFilho(botaoExcluir);
 *   painelRolavel.add(linha);
*/
public class ItemLinha extends Componente {
    public List<Componente> filhos;
    public Texture pixelBranco;
    public Color corFundo = new Color(0.2f, 0.2f, 0.25f, 1f);
    public Color corBorda = new Color(0.35f, 0.35f, 0.4f, 1f);
    public boolean temFundo = true;

    public ItemLinha(float x, float y, float largura, float altura, Texture pixelBranco) {
        super(x, y, largura, altura);
        this.pixelBranco = pixelBranco;
        this.filhos = new ArrayList<Componente>();
    }

    public void addFilho(Componente filho) {
        filhos.add(filho);
    }

    @Override
    public boolean aoTocar(float toqueX, float toqueY, boolean pressionado) {
        if(!contem(toqueX, toqueY) && !pressionado) {
            // soltou fora: repassa soltura para todos pra reiniciar estado
            for(int i = 0; i < filhos.size(); i++) {
                filhos.get(i).aoTocar(toqueX - x, toqueY - y, false);
            }
            return false;
        }
        // coordenadas relativas a linha
        float relX = toqueX - x;
        float relY = toqueY - y;
        for(int i = filhos.size() - 1; i >= 0; i--) {
            if(filhos.get(i).aoTocar(relX, relY, pressionado)) {
                return true;
            }
        }
        return contem(toqueX, toqueY);
    }

    @Override
    public void desenhar(SpriteBatch pincel, float delta, float paiX, float paiY) {
        float desenharX = paiX + x;
        float desenharY = paiY + y;

        if(temFundo) {
            // fundo
            pincel.setColor(corFundo);
            pincel.draw(pixelBranco, desenharX, desenharY, largura, altura);
            // borda inferior como separador
            pincel.setColor(corBorda);
            pincel.draw(pixelBranco, desenharX, desenharY, largura, 1);
        }

        pincel.setColor(Color.WHITE);
        for(int i = 0; i < filhos.size(); i++) {
            filhos.get(i).desenhar(pincel, delta, desenharX, desenharY);
        }
    }

    @Override
    public void liberar() {
        super.liberar();
        filhos.clear();
    }
}

