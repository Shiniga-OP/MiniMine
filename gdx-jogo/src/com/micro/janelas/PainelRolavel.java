package com.micro.janelas;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector3;
import com.micro.componentes.Componente;

/*
 * PainelRolavel, container com rolagem vertical quando o conteudo excede a altura visivel
 * possui barra de rolagem visual e suporta rolagem via toque/arraste
 */
public class PainelRolavel extends Painel {
    public float deslocamentoY = 0; // quanto o conteudo foi rolado
    public float alturaConteudo = 0; // altura total do conteudo
    public float veloRolagem = 20f;
    // barra de rolagem
    public boolean mostrarBarra = true;
    public float larguraBarra = 24f; // aumentado pra ser mais facil de tocar
    public float margemBarra = 4f;
    public Color corBarra = new Color(0.6f, 0.6f, 0.6f, 0.9f); // mais visivel
    public Color corBarraArrastando = new Color(0.8f, 0.8f, 0.8f, 1f); // indicativo visual
    public Color corFundoBarra = new Color(0.2f, 0.2f, 0.2f, 0.5f);
    // arrastar conteudo e barra
    public boolean arrastandoBarra = false;
    public boolean arrastandoConteudo = false;
    public float ultimoToqueY = 0;
    // textura para desenhar retangulos
    public Texture pixelBranco;

	public static final Vector3 auxiliar1 = new Vector3();
	public static final Vector3 auxiliar2 = new Vector3();

    // cria painel rolavel com fundo visual
    public PainelRolavel(PainelFatiado visual, float x, float y, float largura, float altura, float escala) {
        super(visual, x, y, largura, altura, escala);
        iniciar();
    }
    // cria painel rolavel transparente
    public PainelRolavel(float x, float y, float largura, float altura) {
        super(x, y, largura, altura);
        iniciar();
    }

    public void iniciar() {
        // cria textura de 1x1 pixel branco
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        pixelBranco = new Texture(pixmap);
        pixmap.dispose();
    }

    @Override
    public boolean capturaArraste() {
        return true; // PainelRolavel precisa receber eventos de arraste
    }

    // define a altura total do conteudo, isso depois de adicionar todos os filhos
    public void defAlturaConteudo(float alturaConteudo) {
        this.alturaConteudo = alturaConteudo;
        limitarDeslocamento();
    }

    // calcula automaticamente a altura do conteudo baseado nos filhos
    public void calcularAlturaConteudo() {
        float alturaMax = 0;
        for(int i = 0; i < filhos.size(); i++) {
            Componente filho = filhos.get(i);
            float alturaFilho = filho.y + filho.altura;
            if(alturaFilho > alturaMax) {
                alturaMax = alturaFilho;
            }
        }
        defAlturaConteudo(alturaMax);
    }

    // rola o conteudo(valores positivos rolam para baixo, negativos para cima)
    public void rolar(float delta) {
        deslocamentoY += delta;
        limitarDeslocamento();
    }

    // processa rolagem(roda do mouse ou gesto)
    public void processarRolagem(float quantidade) {
        rolar(-quantidade * veloRolagem);
    }

    public void limitarDeslocamento() {
        float alturaVisivel = altura - espacoSuperior - espacoInferior;
        float maxDeslocamento = Math.max(0, alturaConteudo - alturaVisivel);

        if(deslocamentoY < 0) {
            deslocamentoY = 0;
        } else if(deslocamentoY > maxDeslocamento) {
            deslocamentoY = maxDeslocamento;
        }
    }

    // verifica se o conteudo precisa de rolagem
    public boolean precisaRolagem() {
        float alturaVisivel = altura - espacoSuperior - espacoInferior;
        return alturaConteudo > alturaVisivel;
    }

    // obtem a posicao atual da barra(0 a 1)
    public float obterPosicaoBarra() {
        if(!precisaRolagem()) return 0;

        float alturaVisivel = altura - espacoSuperior - espacoInferior;
        float maxDeslocamento = alturaConteudo - alturaVisivel;
        return maxDeslocamento > 0 ? deslocamentoY / maxDeslocamento : 0;
    }

    // obtem a altura da barra em relacao a area total
    public float obterAlturaBarra() {
        if(!precisaRolagem()) return 0;

        float alturaVisivel = altura - espacoSuperior - espacoInferior;
        float proporcaoVisivel = alturaVisivel / alturaConteudo;
        float alturaAreaBarra = altura - margemBarra * 2;
        return Math.max(20, alturaAreaBarra * proporcaoVisivel);
    }

    @Override
	public boolean aoTocar(float toqueX, float toqueY, boolean pressionado) {
		float cliqueRelativoX = toqueX - x;
		float cliqueRelativoY = toqueY - y;

		// define area da barra, maior que a barra visual pra facilitar o toque
		float areaBarra = 40f; // area de toque generosa
		float xInicioBarra = largura - areaBarra;

		boolean toqueNaAreaBarra = precisaRolagem() && cliqueRelativoX >= xInicioBarra;

		// se soltou o dedo
		if(!pressionado) {
			// se estava arrastando a barra, apenas reseta
			if(arrastandoBarra) {
				arrastandoBarra = false;
				return true;
			}
			// se soltou fora da area da barra, avisa os filhos(botões)
			if(!toqueNaAreaBarra) {
				float cliqueRelativoYConteudo = cliqueRelativoY + deslocamentoY;
				for (Componente filho : filhos) {
					filho.aoTocar(cliqueRelativoX, cliqueRelativoYConteudo, false);
				}
			}
			return contem(toqueX, toqueY);
		}
		// se ta pressionado E arrastando a barra
		if(pressionado && arrastandoBarra) {
			// calcula nova posição baseado no toque
			float yBarra = margemBarra;
			float alturaAreaBarra = altura - margemBarra * 2;
			float posicaoRelativa = (cliqueRelativoY - yBarra) / alturaAreaBarra;
			posicaoRelativa = Math.max(0, Math.min(1, posicaoRelativa));

			float alturaVisivel = altura - espacoSuperior - espacoInferior;
			float maxDeslocamento = Math.max(0, alturaConteudo - alturaVisivel);
			deslocamentoY = posicaoRelativa * maxDeslocamento;

			return true;
		}
		// se acabou de pressionar
		if(pressionado && !arrastandoBarra) {
			// verifica se tocou na ÁREA da barra (não precisa acertar a barrinha exata)
			if(toqueNaAreaBarra) {
				arrastandoBarra = true;

				// ja posiciona no lugar do toque
				float yBarra = margemBarra;
				float alturaAreaBarra = altura - margemBarra * 2;
				float posicaoRelativa = (cliqueRelativoY - yBarra) / alturaAreaBarra;
				posicaoRelativa = Math.max(0, Math.min(1, posicaoRelativa));

				float alturaVisivel = altura - espacoSuperior - espacoInferior;
				float maxDeslocamento = Math.max(0, alturaConteudo - alturaVisivel);
				deslocamentoY = posicaoRelativa * maxDeslocamento;

				return true;
			}
			// se não tocou na área da barra, processa clique nos botões
			float cliqueRelativoYConteudo = cliqueRelativoY + deslocamentoY;
			for(int i = filhos.size() - 1; i >= 0; i--) {
				Componente f = filhos.get(i);
				// só clica se o botão estiver visível
				float fYTela = f.y - deslocamentoY;
				if(fYTela + f.altura > 0 && fYTela < altura) {
					if(f.aoTocar(cliqueRelativoX, cliqueRelativoYConteudo, pressionado)) {
						return true;
					}
				}
			}
		}

		return contem(toqueX, toqueY);
	}

	@Override
	public void add(Componente filho) {
		filhos.add(filho);
		// atualiza o tamanho total toda vez que entra algo novo
		float baseDoFilho = filho.y + filho.altura;
		if(baseDoFilho > alturaConteudo) {
			alturaConteudo = baseDoFilho;
		}
	}

    @Override
	public void desenhar(SpriteBatch pincel, float delta, float paiX, float paiY) {
		float desenharX = paiX + x;
		float desenharY = paiY + y;

		// 1. desenha o fundo do painel(se tiver)
		if(temFundo && visual != null) {
			Color corOriginal = pincel.getColor();
			pincel.setColor(corFundo);
			visual.desenhar(pincel, desenharX, desenharY, largura, altura, escala);
			pincel.setColor(corOriginal);
		}
		// 2. ativa o recorte
		// convertendo as coordenadas do mundo para pixels da tela
        auxiliar1.set(desenharX, desenharY, 0);
        auxiliar1.mul(pincel.getProjectionMatrix()); // aplica a camera/escala

        // pega a posição real na janela
        float sX = ((auxiliar1.x + 1) / 2) * Gdx.graphics.getWidth();
        float sY = ((auxiliar1.y + 1) / 2) * Gdx.graphics.getHeight();

        auxiliar2.set(desenharX + largura, desenharY + altura, 0);
        auxiliar2.mul(pincel.getProjectionMatrix());

        float sLargura = (((auxiliar2.x + 1) / 2) * Gdx.graphics.getWidth()) - sX;
        float sAltura = (((auxiliar2.y + 1) / 2) * Gdx.graphics.getHeight()) - sY;

        // flush pra garantir que conteudo anterior seja renderizado antes do scissor
        pincel.flush();
        Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST);
        Gdx.gl.glScissor((int)sX, (int)sY, (int)sLargura, (int)sAltura);

		// 3. desenha os itens internos com o deslocamento da rolagem
		for(int i = 0; i < filhos.size(); i++) {
			Componente item = filhos.get(i);
			// aplica o deslocamento apenas no desenho para o item "subir"
			item.desenhar(pincel, delta, desenharX, desenharY - deslocamentoY);
		}
		pincel.flush(); // garante que os itens sejam cortados agora
		Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST); // desativa o corte pra não afetar o resto

		// 4. desenha a barra de rolagem(sempre por cima de tudo)
		if(precisaRolagem() && mostrarBarra) {
			float xBarra = desenharX + largura - larguraBarra - margemBarra;
			float yBarra = desenharY + margemBarra;
			float alturaAreaBarra = altura - margemBarra * 2;

			pincel.setColor(corFundoBarra);
			pincel.draw(pixelBranco, xBarra, yBarra, larguraBarra, alturaAreaBarra);

			float alturaPuxador = obterAlturaBarra();
			float posicao = obterPosicaoBarra();
			float yPuxador = yBarra + (alturaAreaBarra - alturaPuxador) * posicao;

			// usa cor diferente quando ta arrastando pra indicativo visual
			pincel.setColor(arrastandoBarra ? corBarraArrastando : corBarra);
			pincel.draw(pixelBranco, xBarra, yPuxador, larguraBarra, alturaPuxador);
			pincel.setColor(Color.WHITE);
		}
	}
}

