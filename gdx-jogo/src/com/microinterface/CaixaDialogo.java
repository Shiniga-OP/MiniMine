package com.microinterface;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import java.util.ArrayList;
import java.util.List;

public class CaixaDialogo extends Componente {
    public PainelFatiado visual;
    public BitmapFont fonte;
    public float escala;
    public String titulo;
    public String msg;
    public boolean ativa = false;
    public ShapeRenderer pincelFormas;

    public Painel painelTitulo;
    public Rotulo rotuloTitulo;
    public RotuloMultilinha rotulomsg;
    public Painel painelBotoes;
    public List<Componente> componentes = new ArrayList<Componente>();

    public boolean arrastando = false;
    public float toqueInicialX;
    public float toqueInicialY;

    public interface Fechar {
        void aoFechar(boolean confirmou);
    }
    public Fechar aoFechar;

    public CaixaDialogo(PainelFatiado visual, BitmapFont fonte, float escala, ShapeRenderer pincelFormas) {
        super(0, 0, 400, 250);
        this.visual = visual;
        this.fonte = fonte;
        this.escala = escala;
        this.pincelFormas = pincelFormas;

        criarInterface();
    }

    public void criarInterface() {
        painelTitulo = new Painel(visual, 0, altura - 50, largura, 50, escala);
        painelTitulo.corFundo = new Color(0.3f, 0.5f, 0.8f, 1f);

        rotuloTitulo = new Rotulo("", fonte, escala);
        rotuloTitulo.largura = largura;
        rotuloTitulo.altura = 50;
        painelTitulo.add(rotuloTitulo);

        rotulomsg = new RotuloMultilinha("", fonte, escala * 0.8f);
        rotulomsg.x = 20;
        rotulomsg.y = 80;
        rotulomsg.largura = largura - 40;
        rotulomsg.altura = 100;

        painelBotoes = new Painel(0, 0, largura, 60);
        painelBotoes.defEspaco(10);
    }

    public void mostrar(String titulo, String msg, Fechar aoFechar) {
        this.titulo = titulo;
        this.msg = msg;
        this.aoFechar = aoFechar;
        this.ativa = true;

        rotuloTitulo.texto = titulo;
        rotulomsg.texto = msg;
    }

    public void addOk(PainelFatiado visualBotao) {
        Acao acaoOk = new Acao() {
            public void exec() {
                fechar(true);
            }
        };
        Botao botaoOk = new Botao("OK", visualBotao, fonte, 0, 0, 120, 40, escala, acaoOk);
        painelBotoes.addAncorado(botaoOk, Ancora.CENTRO_DIREITO, -10, 0);
    }

    public void addCancelar(PainelFatiado visualBotao) {
        Acao acaoCancelar = new Acao() {
            public void exec() {
                fechar(false);
            }
        };
        Botao botaoCancelar = new Botao("Cancelar", visualBotao, fonte, 0, 0, 120, 40, escala, acaoCancelar);
        painelBotoes.addAncorado(botaoCancelar, Ancora.CENTRO_ESQUERDO, 10, 0);
    }

    public void addBotao(String texto, PainelFatiado visualBotao, Ancora ancoragem, float margemX, Acao acao) {
        Botao botao = new Botao(texto, visualBotao, fonte, 0, 0, 120, 40, escala, acao);
        painelBotoes.addAncorado(botao, ancoragem, margemX, 0);
    }

    public void add(Componente componente) {
        componentes.add(componente);
    }

    public void fechar(boolean confirmou) {
        this.ativa = false;
        if(aoFechar != null) {
            aoFechar.aoFechar(confirmou);
        }
    }

    public void centralizar(float larguraTela, float alturaTela) {
        this.x = (larguraTela - this.largura) / 2;
        this.y = (alturaTela - this.altura) / 2;
    }

    public boolean aoTocar(float toqueX, float toqueY, boolean pressionado) {
		if(!ativa) return false;

		// calculamos a posição real da barra de título no mundo
		float tituloXGlobal = x;
		float tituloYGlobal = y + altura - 50;

		boolean noTitulo = toqueX >= tituloXGlobal && toqueX <= tituloXGlobal + largura &&
			toqueY >= tituloYGlobal && toqueY <= tituloYGlobal + 50;

		if(pressionado && noTitulo) {
			arrastando = true;
			toqueInicialX = toqueX - x;
			toqueInicialY = toqueY - y;
			return true;
		}
        if(!pressionado) {
            arrastando = false;
        }
        // processa toques nos componentes filhos
        for(Componente comp : componentes) {
            if(comp.aoTocar(toqueX - x, toqueY - y, pressionado)) {
                return true;
            }
        }
        if(painelBotoes.aoTocar(toqueX - x, toqueY - y, pressionado)) {
            return true;
        }
        return contem(toqueX, toqueY);
    }

    public void aoArrastar(float toqueX, float toqueY) {
		if(ativa && arrastando) { 
			this.x = toqueX - toqueInicialX;
			this.y = toqueY - toqueInicialY;
		}
	}

    public void desenhar(SpriteBatch pincel, float delta, float paiX, float paiY) {
        if(!ativa) return;

        float desenharX = paiX + x;
        float desenharY = paiY + y;

        pincel.end();
        if(pincelFormas != null) {
            pincelFormas.begin(ShapeRenderer.ShapeType.Filled);
            pincelFormas.setColor(0, 0, 0, 0.6f);
            pincelFormas.rect(paiX - 2000, paiY - 2000, 4000, 4000);
            pincelFormas.end();
        }
        pincel.begin();

        visual.desenhar(pincel, desenharX, desenharY, largura, altura, escala);
        painelTitulo.desenhar(pincel, delta, desenharX, desenharY);
        rotulomsg.desenhar(pincel, delta, desenharX, desenharY);

        // desenha componentes filhos
        for(Componente comp : componentes) {
            comp.desenhar(pincel, delta, desenharX, desenharY);
        }
        painelBotoes.desenhar(pincel, delta, desenharX, desenharY);
    }
	
	@Override
	public void liberar() {
		super.liberar();
		fonte.dispose();
		rotuloTitulo.liberar();
		rotulomsg.liberar();
	}
}

