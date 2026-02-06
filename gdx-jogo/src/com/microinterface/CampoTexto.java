package com.microinterface;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class CampoTexto extends Componente {
    public PainelFatiado visual;
    public BitmapFont fonte;
    public float escala;
    public String texto = "";
    public String padrao = "";
    public boolean emFoco = false;
    public float tempoCursor = 0;
    public boolean mostrarCursor = true;
    public int limiteCaracteres = 50;
    public GlyphLayout medidor;
    public float margemInterna = 10;

    // pra notificar o gerenciador quando ganhar foco
    public GerenciadorUI gerenciador = null;

    public interface Texto {
        void aoMudar(String novoTexto);
    }

    public Texto mudanca;

    public CampoTexto(PainelFatiado visual, BitmapFont fonte, float x, float y, float largura, float altura, float escala) {
        super(x, y, largura, altura);
        this.visual = visual;
        this.fonte = fonte;
        this.escala = escala;
        this.medidor = new GlyphLayout();
    }

    public void defTexto(String texto) {
        this.texto = texto;
        if(texto.length() > limiteCaracteres) {
            this.texto = texto.substring(0, limiteCaracteres);
        }
    }

    public void defFoco(boolean foco) {
        this.emFoco = foco;
        if(foco) {
            // notifica o gerenciador que esse campo ta em foco
            if(gerenciador != null) {
                gerenciador.defFocoTexto(this);
            }
            // força o teclado a aparecer
            Gdx.input.setOnscreenKeyboardVisible(true);
        } else {
            Gdx.input.setOnscreenKeyboardVisible(false);
        }
    }

    public boolean aoTocar(float toqueX, float toqueY, boolean pressionado) {
        if(contem(toqueX, toqueY) && !pressionado) {
            emFoco = true;
            defFoco(true);
            return true;
        }
        return false;
    }

    public boolean processarTecla(int c) {
        if(!emFoco) return false;

        if(c == Input.Keys.BACKSPACE && texto.length() > 0) {
            String antigoTexto = texto;
            texto = texto.substring(0, texto.length() - 1);
            if(mudanca != null && !antigoTexto.equals(texto)) {
                mudanca.aoMudar(texto);
            }
            return true;
        }
        if(c == Input.Keys.ENTER) {
            emFoco = false;
            Gdx.input.setOnscreenKeyboardVisible(false);
            return true;
        }
        return false;
    }

    public boolean processarCaractere(char caractere) {
        if(!emFoco) return false;
		
        if(caractere >= 32 && caractere <= 126 && texto.length() < limiteCaracteres) {
            String antigoTexto = texto;
            texto += caractere;
            if(mudanca != null && !antigoTexto.equals(texto)) {
                mudanca.aoMudar(texto);
            }
            return true;
        }
        return false;
    }

    public void desenhar(SpriteBatch pincel, float delta, float paiX, float paiY) {
        float desenharX = paiX + x;
        float desenharY = paiY + y;

        tempoCursor += delta;
        if(tempoCursor > 0.5f) {
            mostrarCursor = !mostrarCursor;
            tempoCursor = 0;
        }
        Color corOriginal = pincel.getColor();
        if(emFoco) {
            pincel.setColor(1f, 1f, 1f, 1f);
        } else {
            pincel.setColor(0.9f, 0.9f, 0.9f, 1f);
        }
        visual.desenhar(pincel, desenharX, desenharY, largura, altura, escala);
        pincel.setColor(corOriginal);

        fonte.getData().setScale(escala);

        String textoExibir = texto.isEmpty() ? padrao : texto;
        if(!textoExibir.isEmpty()) {
            medidor.setText(fonte, textoExibir);

            float larguraDisponivel = largura - (margemInterna * 2);
            if(medidor.width > larguraDisponivel) {
                int tam = textoExibir.length();
                while(medidor.width > larguraDisponivel && tam > 0) {
                    tam--;
                    String textoReduzido = textoExibir.substring(textoExibir.length() - tam);
                    medidor.setText(fonte, textoReduzido);
                }
                textoExibir = textoExibir.substring(textoExibir.length() - tam);
                medidor.setText(fonte, textoExibir);
            }
            float posX = desenharX + margemInterna;
            float posY = desenharY + (altura / 2) + (medidor.height / 2);

            if(texto.isEmpty()) {
                fonte.setColor(0.5f, 0.5f, 0.5f, 1f);
            } else {
                fonte.setColor(Color.BLACK);
            }
            fonte.draw(pincel, textoExibir, posX, posY);
            fonte.setColor(Color.WHITE);
        }
        if(emFoco && mostrarCursor) {
            float cursorX = desenharX + margemInterna;
            if(!texto.isEmpty()) {
                medidor.setText(fonte, texto);
                cursorX += medidor.width;
            }
            float cursorY = desenharY + altura / 2;

            medidor.setText(fonte, "|");
            fonte.setColor(Color.BLACK);
            fonte.draw(pincel, "|", cursorX, cursorY + medidor.height / 2);
            fonte.setColor(Color.WHITE);
        }
        fonte.getData().setScale(1.0f);
    }
	
	@Override
	public void liberar() {
		super.liberar();
		fonte.dispose();
	}
}

