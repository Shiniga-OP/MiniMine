package com.micro.componentes;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class Rotulo extends Componente {
    public String texto;
    public BitmapFont fonte;
    public GlyphLayout medidor;
    public float escala;

    public Rotulo(String texto, BitmapFont fonte, float escala) {
        super(0, 0, 0, 0);
        this.texto = texto;
        this.fonte = fonte;
        this.escala = escala;
        this.medidor = new GlyphLayout();
    }

	@Override
	public void desenhar(SpriteBatch pincel, float delta, float paiX, float paiY) {
		if(largura <= 0 || altura <= 0) return; // se não tem tamanho, não desenha pra não bugar

		float escalaOriginalX = fonte.getData().scaleX;
		float escalaOriginalY = fonte.getData().scaleY;

		// 1. reinicia a escala pra medir o tamanho "real/bruto" da fonte no arquivo
		fonte.getData().setScale(1.0f);
		medidor.setText(fonte, texto);

		// 2. calculo da escala necessaria(regra de 3)
		//  texto ocupe no máximo, 80% da largura/altura do componente pra não colmar nas bordas
		float margem = 0.8f; 
		float escalaX = (largura * margem) / medidor.width;
		float escalaY = (altura * margem) / medidor.height;

		// usa a menor escala para o texto não ficar deformado(esticado)
		float escalaFinal = Math.min(escalaX, escalaY);

		// se a escala calculada for maior que escalaMaxima, trava na maxima
		if(escalaFinal > escala) escalaFinal = escala;

		// 3. aplica a escala calculada matematicamente
		fonte.getData().setScale(escalaFinal);
		medidor.setText(fonte, texto);

		// 4. posicionamento com base na nova metrica
		float posX = paiX + x + (largura / 2) - (medidor.width / 2);
		float posY = paiY + y + (altura / 2) + (medidor.height / 2);

		fonte.draw(pincel, texto, posX, posY);

		// 5. restaura a zona que estava antes
		fonte.getData().setScale(escalaOriginalX, escalaOriginalY);
	}
}


