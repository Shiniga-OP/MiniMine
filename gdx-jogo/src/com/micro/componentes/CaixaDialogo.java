package com.micro.componentes;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import java.util.ArrayList;
import java.util.List;
import com.micro.janelas.PainelFatiado;
import com.micro.janelas.Painel;
import com.micro.util.Acao;
import com.micro.util.Ancora;

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

    public Botao botaoFechar;

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
        // titulo flutua acima do painel principal
        painelTitulo = new Painel(visual, 0, altura*1.25f, largura, 50, escala);
        painelTitulo.corFundo = new Color(0.3f, 0.5f, 0.8f, 1f);

        rotuloTitulo = new Rotulo("", fonte, escala);
        rotuloTitulo.largura = largura - 50; // deixa espaço pro X
        rotuloTitulo.altura = 50;
        painelTitulo.add(rotuloTitulo);

        // botão X no canto direito da barra de título
        Acao acaoFechar = new Acao() {
            public void exec() { fechar(false); }
        };
        botaoFechar = new Botao("X", visual, fonte, largura - 46, 4, 42, 42, escala * 0.7f, acaoFechar);

        // painel de botões na base
        painelBotoes = new Painel(0, 0, largura, altura);
        painelBotoes.defEspaco(10);

        // calcula quanto sobra pra mensagem
        float espacoOcupado = 50 + 60 + 20; // titulo + botoes + margens

        // reduz a escala pra 0.6f pra garantir que o texto não fique gigante
        rotulomsg = new RotuloMultilinha("", fonte, escala * 0.6f);
        rotulomsg.x = 20;
        rotulomsg.largura = largura - 40;

        // posiciona o texto logo abaixo do titulo
        rotulomsg.y = 70;
        rotulomsg.altura = altura - espacoOcupado;
    }

    public void mostrar(String titulo, String msg, Fechar aoFechar) {
        this.titulo = titulo;
        this.msg = msg;
        this.aoFechar = aoFechar;
        this.ativa = true;

        rotuloTitulo.texto = titulo;
        rotulomsg.texto = msg;

        // calcula o topo do componente mais alto para posicionar o rotulomsg acima
        float topoMaximo = 0;
        for(Componente c : componentes) {
            float topo = c.y + c.altura;
            if(topo > topoMaximo) topoMaximo = topo;
        }
        if(topoMaximo > 0) {
            rotulomsg.y = topoMaximo + 10;
            rotulomsg.altura = altura - rotulomsg.y - 10;
        } else {
            rotulomsg.y = painelBotoes.altura + 10;
            rotulomsg.altura = altura - painelBotoes.altura - 20;
        }
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

    // botão posicionado por ancora(uso geral)
    public Botao addBotao(String texto, PainelFatiado visualBotao, Ancora ancoragem, float margemX, Acao acao) {
        Botao botao = new Botao(texto, visualBotao, fonte, 0, 0, 120, 40, escala, acao);
        painelBotoes.addAncorado(botao, ancoragem, margemX, 0);
		return botao;
    }

    // botão posicionado manualmente dentro do painelBotoes(x/y explicitos)
    public Botao addBotaoManual(String texto, PainelFatiado visualBotao, float x, float y, float larg, float alt, Acao acao) {
        Botao botao = new Botao(texto, visualBotao, fonte, x, y, larg, alt, escala, acao);
        painelBotoes.add(botao);
		return botao;
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

    // define largura/altura e recalcula todas as posições internas
    public void definirTamanho(float larg, float alt) {
        this.largura = larg;
        this.altura = alt;

        painelTitulo.largura = larg;
        painelTitulo.y = alt + 10; // flutua 10px acima do painel

        rotuloTitulo.largura = larg - 50;
        rotuloTitulo.altura = 50;

        botaoFechar.x = larg - 46;
        botaoFechar.y = 4;

        painelBotoes.largura = larg;

        rotulomsg.largura = larg - 40;
    }

    public void centralizar(float larguraTela, float alturaTela) {
        this.x = (larguraTela - this.largura) / 2;
        this.y = (alturaTela - this.altura) / 2;
    }

    public boolean aoTocar(float toqueX, float toqueY, boolean pressionado) {
		if(!ativa) return false;

		// calculamos a posição real da barra de titulo no mundo
		float tituloXGlobal = x;
		float tituloYGlobal = y + altura - 50;

		boolean noTitulo = toqueX >= tituloXGlobal && toqueX <= tituloXGlobal + largura &&
			toqueY >= tituloYGlobal && toqueY <= tituloYGlobal + 50;

		// toque no X tem prioridade sobre o arraste
		float tituloLocalX = toqueX - tituloXGlobal;
		float tituloLocalY = toqueY - tituloYGlobal;
		if(noTitulo && botaoFechar.aoTocar(tituloLocalX, tituloLocalY, pressionado)) {
			return true;
		}
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
        // o X é desenhado no espaço do titulo(Y relativo ao topo do dialogo)
        float tituloY = desenharY + altura - 50;
        botaoFechar.desenhar(pincel, delta, desenharX, tituloY);
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
		rotuloTitulo.liberar();
		rotulomsg.liberar();
	}
}
