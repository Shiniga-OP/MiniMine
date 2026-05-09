package com.micro.janelas;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.micro.componentes.Componente;
import com.micro.util.Ancora;
/*
 * painel é uma estante que pode ter visual(PainelFatiado) ou ser transparente
 * e suporta ancoragem de filhos
 */
public class Painel extends Componente {
    public PainelFatiado visual;
    public float escala;
    public boolean temFundo;
    public Color corFundo;

    // espaçamento interno
    public float espacoEsquerda = 0;
    public float espacoDireita = 0;
    public float espacoSuperior = 0;
    public float espacoInferior = 0;

    // rastreia qual filho capturou o toque(para propagar arraste)
    protected Componente filhoCapturado = null;

    // cria painel com fundo visual
    public Painel(PainelFatiado visual, float x, float y, float largura, float altura, float escala) {
        super(x, y, largura, altura);
        this.visual = visual;
        this.escala = escala;
        this.temFundo = true;
        this.corFundo = Color.WHITE;
    }

    // cria painel transparente(sem fundo)
    public Painel(float x, float y, float largura, float altura) {
        super(x, y, largura, altura);
        this.temFundo = false;
    }

    // define espaçamento interno
    public void defEspaco(float todos) {
        this.espacoEsquerda = todos;
        this.espacoDireita = todos;
        this.espacoSuperior = todos;
        this.espacoInferior = todos;
    }

    public void defEspaco(float horizontal, float vertical) {
        this.espacoEsquerda = horizontal;
        this.espacoDireita = horizontal;
        this.espacoSuperior = vertical;
        this.espacoInferior = vertical;
    }

    // adiciona filho com ancoragem
    public void addAncorado(Componente filho, Ancora ancoragem, float margemX, float margemY) {
        // calcula área disponivel considerando espaço
        float larguraDisponivel = largura - espacoEsquerda - espacoDireita;
        float alturaDisponivel = altura - espacoSuperior - espacoInferior;

        // calcula posição baseada na ancoragem
        filho.x = ancoragem.calcularX(larguraDisponivel, filho.largura, margemX) + espacoEsquerda;
        filho.y = ancoragem.calcularY(alturaDisponivel, filho.altura, margemY) + espacoInferior;

        filhos.add(filho);
    }

    @Override
    public boolean aoTocar(float toqueX, float toqueY, boolean pressionado) {
        // se ta soltando, reseta o filho capturado
        if(!pressionado) filhoCapturado = null;
        // verifica filhos primeiro(de tras para frente pra respeitar ordem de desenho)
        for(int i = filhos.size() - 1; i >= 0; i--) {
            Componente filho = filhos.get(i);

            if(filho.aoTocar(toqueX - x, toqueY - y, pressionado)) {
                // se está pressionando e o filho consumiu o toque, rastreia ele
                if(pressionado) {
                    filhoCapturado = filho;
                }
                return true; // filho consumiu o toque
            }
        }
        // se nenhum filho consumiu e o toque ta neste painel
        return contem(toqueX, toqueY);
    }

    @Override
    public boolean capturaArraste() {
        // retorna true se algum filho capturado precisa de arraste
        return filhoCapturado != null && filhoCapturado.capturaArraste();
    }

    @Override
    public void desenhar(SpriteBatch pincel, float delta, float paiX, float paiY) {
        float desenharX = paiX + x;
        float desenharY = paiY + y;

        // desenha fundo se tiver
        if(temFundo && visual != null) {
            Color corOriginal = pincel.getColor();
            pincel.setColor(corFundo);
            visual.desenhar(pincel, desenharX, desenharY, largura, altura, escala);
            pincel.setColor(corOriginal);
        }
        desenharFilhos(pincel, delta, desenharX, desenharY);
    }
}

