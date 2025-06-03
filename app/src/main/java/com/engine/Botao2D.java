package com.engine;
import android.view.MotionEvent;
import android.os.Looper;
import android.os.Handler;

public class Botao2D {
    public Objeto2D objeto;
    public boolean pressionado = false;
    public Handler repetidor;
    public Runnable acao;

    public Botao2D(Objeto2D objeto2D) {
        this.objeto = objeto2D;
        this.repetidor = new Handler(Looper.getMainLooper());
    }

    public void definirAcao(Runnable acao) {
        this.acao = acao;
    }

	public int pontoAtivo = -1;

	public boolean verificarToque(MotionEvent e) {
		int tipo = e.getActionMasked();
		int quant = e.getPointerCount();

		switch(tipo) {
			case MotionEvent.ACTION_DOWN:
			case MotionEvent.ACTION_POINTER_DOWN:
				if(pontoAtivo == -1) {
					for(int i = 0; i < quant && i < 2; i++) {
						float x = e.getX(i), y = e.getY(i);
						if(objeto.tocado(x, y)) {
							pontoAtivo = e.getPointerId(i);
							pressionado = true;
							iniciarRepeticao();
							return true;
						}
					}
				}
				break;

			case MotionEvent.ACTION_MOVE:
				for(int i = 0; i < quant; i++) {
					if(e.getPointerId(i) == pontoAtivo) {
						float x = e.getX(i), y = e.getY(i);
						if(!objeto.tocado(x, y)) {
							pressionado = false;
							pontoAtivo = -1;
							pararRepeticao();
						}
						return true;
					}
				}
				break;

			case MotionEvent.ACTION_POINTER_UP:
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_CANCEL:
				int id = e.getPointerId(e.getActionIndex());
				if(id == pontoAtivo) {
					pressionado = false;
					pontoAtivo = -1;
					pararRepeticao();
				}
				break;
		}
		return pressionado;
	}

    public void iniciarRepeticao() {
        repetidor.postDelayed(new Runnable() {
				@Override
				public void run() {
					if(pressionado && acao != null) {
						acao.run();
						repetidor.postDelayed(this, 100);
					}
				}
			}, 0);
    }

    public void pararRepeticao() {
        repetidor.removeCallbacksAndMessages(null);
    }
}
