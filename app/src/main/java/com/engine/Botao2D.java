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

    public boolean verificarToque(MotionEvent e) {
        float x = e.getX();
        float y = e.getY();
        boolean tocado = objeto.tocado(x, y);

        if(tocado && !pressionado) {
            pressionado = true;
            iniciarRepeticao();
			return true;
        } else if(!tocado && pressionado) {
            pressionado = false;
            pararRepeticao();
			return false;
        } else {
			return false;
		}
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
