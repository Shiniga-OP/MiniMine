package com.minimine;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.MathUtils;

public class Controles implements InputProcessor {
	public PerspectiveCamera camera;

	public int pontoEsq = -1;
	public int pontoDir = -1;
	public Vector2 esqCentro = new Vector2();
	public Vector2 esqPos = new Vector2();
	public Vector2 ultimaDir = new Vector2();

	public Vector3 velo = new Vector3();
	public float veloM = 9f;      // m/s
	public float sensi = 0.25f;
	public float grav = -30f;      // m/s^2

	public boolean noChao = false;
	// camera
	public float yaw = 180f;
	public float tom = -20f;

	public int telaV;
	public int telaH;

	public Controles() {
		telaV = Gdx.graphics.getWidth();
		telaH = Gdx.graphics.getHeight();

		camera = new PerspectiveCamera(67, telaV, telaH);
		camera.position.set(0, 70f, 0);
		attCamera();

		camera.near = 0.1f;
		camera.far = 300f;
		camera.update();
	}

	public void att(float delta) {
		if(!noChao) velo.y += grav * delta;

		Vector2 joy = obterEsquerda(); 
		if(joy.len2() > 0.0001f) {
			Vector3 propagar = new Vector3(camera.direction.x, 0f, camera.direction.z);
			propagar.nor();
			Vector3 direita = propagar.cpy().crs(camera.up).nor();

			Vector3 move = new Vector3();
			move.add(propagar.scl(joy.y));
			move.add(direita.scl(joy.x));
			if(move.len2() > 0.00001f) {
				move.nor().scl(veloM);
				velo.x = move.x;
				velo.z = move.z;
			} else {
				velo.x = 0f;
				velo.z = 0f;
			}
		} else {
			velo.x = 0f;
			velo.z = 0f;
		}
		Vector3 deltaPos = new Vector3(velo).scl(delta);
		camera.position.add(deltaPos);

		float chao = 20f;
		if(camera.position.y <= chao + 1.7f) {
			camera.position.y = chao + 1.7f;
			velo.y = 0f;
			noChao = true;
		} else {
			noChao = false;
		}
		attCamera();
		camera.update();
	}

	public void ajustar(int v, int h) {
		telaV = v;
		telaH = h;
		camera.viewportWidth = v;
		camera.viewportHeight = h;
		camera.update();
	}

	@Override
	public boolean touchDown(int telaX, int telaY, int ponto, int botao) {
		int yInv = telaH - telaY;
		if(telaX < telaV / 2) {
			if(pontoEsq == -1) {
				pontoEsq = ponto;
				esqCentro.set(telaX, yInv);
				esqPos.set(telaX, yInv);
			}
		} else {
			if(pontoDir == -1) {
				pontoDir = ponto;
				ultimaDir.set(telaX, yInv);
			}
		}
		return true;
	}

	@Override
	public boolean touchUp(int telaX, int telaY, int ponto, int botao) {
		if(ponto == pontoEsq) {
			pontoEsq = -1;
			esqPos.set(esqCentro);
		}
		if(ponto == pontoDir) pontoDir = -1;
		return true;
	}

	@Override
	public boolean touchDragged(int telaX, int telaY, int ponto) {
		int yInv = telaH - telaY;
		if(ponto == pontoEsq) {
			esqPos.set(telaX, yInv);
		} else if(ponto == pontoDir) {
			float dx = telaX - ultimaDir.x;
			float dy = yInv - ultimaDir.y;
			yaw -= dx * sensi;
			tom += dy * sensi;
			if(tom > 89f) tom = 89f;
			if(tom < -89f) tom = -89f;
			ultimaDir.set(telaX, yInv);
		}
		return true;
	}

	public Vector2 obterEsquerda() {
		Vector2 s = new Vector2();
		if(pontoEsq == -1) return s;
		s.set(esqPos).sub(esqCentro);
		float maxRadianos = Math.min(telaV, telaH) * 0.20f;
		if(s.len() > maxRadianos) s.nor().scl(maxRadianos);
		s.scl(1f / maxRadianos); // -1..1
		s.y = s.y;
		return s;
	}

	public void attCamera() {
		float yawRad = yaw * MathUtils.degRad;
		float tomRad = tom * MathUtils.degRad;
		float cx = MathUtils.cos(tomRad) * MathUtils.sin(yawRad);
		float cy = MathUtils.sin(tomRad);
		float cz = MathUtils.cos(tomRad) * MathUtils.cos(yawRad);
		camera.direction.set(cx, cy, cz).nor();
		camera.up.set(0f, 1f, 0f);
		camera.lookAt(camera.position.x + camera.direction.x,
				camera.position.y + camera.direction.y,
				camera.position.z + camera.direction.z);
	}

	@Override 
	public boolean keyDown(int c) { 
		if(c == com.badlogic.gdx.Input.Keys.P) {
			com.minimine.mundo.Mundo.debugColisao = !com.minimine.mundo.Mundo.debugColisao;
			return true;
		}
		return false; 
	}
	@Override public boolean keyUp(int c) { return false; }
	@Override public boolean keyTyped(char c) { return false; }
	@Override public boolean mouseMoved(int tx, int ty) { return false; }
	@Override public boolean scrolled(float p, float p2) { return false; }
	}
