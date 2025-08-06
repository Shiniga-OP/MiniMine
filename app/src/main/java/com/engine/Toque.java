package com.engine;
import android.view.MotionEvent;

public class Toque {
	public static int pontoAtivo = -1;

	public static float ultimoX = 0, ultimoY = 0;

	public static boolean cameraOlhar(Camera3D camera, MotionEvent e) {
		int acao = e.getActionMasked();
		int total = e.getPointerCount();

		switch (acao) {
			case MotionEvent.ACTION_DOWN:
			case MotionEvent.ACTION_POINTER_DOWN:
				if(pontoAtivo == -1) {
					for(int i = 0; i < total && i < 2; i++) {
						pontoAtivo = e.getPointerId(i);
						ultimoX = e.getX(i);
						ultimoY = e.getY(i);
						break;
					}
				}
				break;

			case MotionEvent.ACTION_MOVE:
				for(int i = 0; i < total; i++) {
					if(e.getPointerId(i) == pontoAtivo) {
						float x = e.getX(i);
						float y = e.getY(i);
						float dx = x - ultimoX;
						float dy = y - ultimoY;
						camera.rotacionar(dx * 0.15f, dy * 0.15f);
						ultimoX = x;
						ultimoY = y;
						break;
					}
				}
				break;

			case MotionEvent.ACTION_POINTER_UP:
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_CANCEL:
				int idSolto = e.getPointerId(e.getActionIndex());
				if(idSolto == pontoAtivo) {
					pontoAtivo = -1;
				}
				break;
		}
		return true;
	}
}
