package com.minimine.entidades;

import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.collision.Ray;
import com.minimine.utils.Mat;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import net.mgsx.gltf.loaders.gltf.GLTFLoader;
import net.mgsx.gltf.scene3d.scene.SceneAsset;
import com.minimine.mundo.blocos.Bloco;
import com.minimine.audio.Audio;
import com.minimine.mundo.Mundo;

public class Jogador {
	public ModelInstance modelo;
	public int modo = 2; // 0 = espectador, 1 = criativo, 2 = sobrevivencia
	public PerspectiveCamera camera;
	public Vector3 posicao = new Vector3(1, 80, 1), velocidade = new Vector3();
	public final Vector3 frenteV = new Vector3(0, 0, 0), direitaV = new Vector3(0, 0, 0);

	public float largura = 0.6f, altura = 1.8f, profundidade = 0.6f;
	public boolean noChao = true, naAgua = false, agachado = false, nasceu = false;
	public static boolean esquerda = false, frente = false, tras = false, direita = false, cima = false, baixo = false, acao = false;
	public BoundingBox hitbox = new BoundingBox();
	public static final BoundingBox blocoBox = new BoundingBox();
	public static final Vector3 minVec = new Vector3(), maxVec = new Vector3();

	public static float GRAVIDADE = -30f, VELO_MAX_QUEDA = -50f, velo = 8f, pulo = 10f;

	public CharSequence item = "ar";
	public static int ALCANCE = 7;
	public Inventario inv = new Inventario(this);

	public float yaw = 180f, tom = -20f;

	public void criarModelo3D() {
		SceneAsset asset = new GLTFLoader().load(Gdx.files.internal("modelos/jogador.gltf"));
		this.modelo = new ModelInstance(asset.scene.model);
	}

	public void interagirBloco() {
		Ray raio = camera.getPickRay(
			Gdx.graphics.getWidth() / 2f,
			Gdx.graphics.getHeight() / 2f);
		float olhoX = raio.origin.x;
		float olhoY = raio.origin.y;
		float olhoZ = raio.origin.z;
		float dirX = raio.direction.x;
		float dirY = raio.direction.y;
		float dirZ = raio.direction.z;

		for(float t = 0; t < ALCANCE; t += 0.15f) { // passo menor = mais preciso
			int x = Mat.floor(olhoX + dirX * t);
			int y = Mat.floor(olhoY + dirY * t);
			int z = Mat.floor(olhoZ + dirZ * t);

			Bloco bloco = Bloco.numIds.get(Mundo.obterBlocoMundo(x, y, z));

			if(bloco != null) {
				if(item.equals("ar")) {
					if(modo == 2) inv.addItem(bloco.nome, 1);
					Mundo.defBlocoMundo(x, y, z, "ar");
					Bloco.tocarSom(bloco.nome);
				} else {
					int xAnt = Mat.floor(olhoX + dirX * (t - 0.25f));
					int yAnt = Mat.floor(olhoY + dirY * (t - 0.25f));
					int zAnt = Mat.floor(olhoZ + dirZ * (t - 0.25f));

					if(Mundo.obterBlocoMundo(xAnt, yAnt, zAnt) == 0) {
						blocoBox.set(minVec.set(xAnt, yAnt, zAnt), maxVec.set(xAnt + 1, yAnt + 1, zAnt + 1));
						attHitbox();
						if(blocoBox.intersects(hitbox)) return;
						Mundo.defBlocoMundo(xAnt, yAnt, zAnt, item);
						Bloco.tocarSom(item);

						if(modo == 2) inv.rmItem(inv.slotSelecionado, 1);
					}
				}
				return;
			}
		}
	}

	public void attHitbox() {
		float x = posicao.x;
		float y = posicao.y;
		float z = posicao.z;

		hitbox.set(minVec.set(x - largura / 2, y, z - profundidade / 2), maxVec.set(x + largura / 2, y + altura, z + profundidade / 2));
	}

	public boolean colideComMundo() {
		int minX = Mat.floor(hitbox.min.x);
		int maxX = Mat.floor(hitbox.max.x);
		int minY = Mat.floor(hitbox.min.y);
		int maxY = Mat.floor(hitbox.max.y);
		int minZ = Mat.floor(hitbox.min.z);
		int maxZ = Mat.floor(hitbox.max.z);

		naAgua = false;

		for(int x = minX; x <= maxX; x++) {
			for(int y = minY; y <= maxY; y++) {
				for(int z = minZ; z <= maxZ; z++) {

					int id = Mundo.obterBlocoMundo(x, y, z);
					if(id == 0) continue;

					Bloco b = Bloco.numIds.get(id);

					blocoBox.set(
						minVec.set(x, y, z),
						maxVec.set(x + 1, y + 1, z + 1)
					);
					if(!b.solido) {
						naAgua = true;
						continue;
					} else {
						if(hitbox.intersects(blocoBox)) return true;
					}
				}
			}
		}
		return false;
	}

	public void att(float delta) {
		frenteV.x = camera.direction.x;
		frenteV.z = camera.direction.z;
		frenteV.nor();  
		direitaV.x = frenteV.z;
		direitaV.z = -frenteV.x;

		velocidade.x = 0;
		velocidade.z = 0;
		if(modo != 2) velocidade.y = 0;

		if(this.frente) velocidade.add(frenteV.cpy().scl(velo));
		if(this.tras)  velocidade.sub(frenteV.cpy().scl(velo));
		if(this.esquerda) velocidade.add(direitaV.cpy().scl(velo));
		if(this.direita) velocidade.sub(direitaV.cpy().scl(velo));
		if(this.cima) {
			if(modo != 2 || noChao || naAgua) {
				velocidade.y = pulo; // pulo
				noChao = false;
			}
        }
        if(this.baixo) velocidade.y = -10f;
		
		// gravidade no sobrevivencia
		if(naAgua) GRAVIDADE = -10;
		else GRAVIDADE = -30;

		if(modo == 2 && !noChao || naAgua) { 
			this.velocidade.y += GRAVIDADE * delta;

			if(this.velocidade.y < VELO_MAX_QUEDA) {
				this.velocidade.y = VELO_MAX_QUEDA;
			}
		}
		if(modo == 0) {
			posicao.add(velocidade.x * delta, velocidade.y * delta, velocidade.z * delta);
			attHitbox();
			camera.position.set(posicao.x, posicao.y + altura * 0.95f, posicao.z);
			return;
		}
		float dx = velocidade.x * delta;
		float dy = velocidade.y * delta;
		float dz = velocidade.z * delta;
		// primeiro verifica colisão vertical
		posicao.y += dy;
		attHitbox();

		if(colideComMundo()) {
			posicao.y -= dy;
			attHitbox(); // atualiza hitbox apos corrigir posição
			// verifica se ta colidindo por baixo(pé no chão)
			if(dy < 0) {
				noChao = true;
			} else if(dy > 0) {
				// colisão por cima(cabeça)
				noChao = false;
			}
			velocidade.y = 0;
		} else {
			// se não ha colisão vertical, verifica se ta no chão usando uma verificação mais precisa
			noChao = ehChao();
		}
		// agora processa movimento horizontal
		if(agachado && noChao && dx != 0 && !temSuporte(posicao.x + dx, posicao.z)) {
			dx = 0;
		}
		posicao.x += dx;
		attHitbox();
		if(colideComMundo()) {
			posicao.x -= dx;
			velocidade.x = 0;
			attHitbox();
		}
		if(agachado && noChao && dz != 0 && !temSuporte(posicao.x, posicao.z + dz)) {
			dz = 0;
		}
		posicao.z += dz;
		attHitbox();
		if(colideComMundo()) {
			posicao.z -= dz; 
			velocidade.z = 0;
			attHitbox();
		}
		camera.position.set(posicao.x, posicao.y + altura * 0.9f, posicao.z);

		if(posicao.y < -100f) {
			posicao.y = 80f;
		}
	}

	public boolean ehChao() {
		// verifica se ha blocos solidos logo abaixo dos pes do jogador
		float epsilon = 0.05f; // margem pra evitar flutuação
		float yCheque = posicao.y - epsilon;

		int minX = Mat.floor(posicao.x - largura / 2);
		int maxX = Mat.floor(posicao.x + largura / 2);
		int y = Mat.floor(yCheque);
		int minZ = Mat.floor(posicao.z - profundidade / 2);
		int maxZ = Mat.floor(posicao.z + profundidade / 2);

		for(int x = minX; x <= maxX; x++) {
			for(int z = minZ; z <= maxZ; z++) {
				int id = Mundo.obterBlocoMundo(x, y, z);
				if(id != 0) {
					Bloco b = Bloco.numIds.get(id);
					if(b != null && b.solido) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public void nascerNoTopo() {
		int chaoY = Mundo.obterAlturaChao((int)posicao.x, (int)posicao.z);
		this.posicao.y = chaoY;
		this.velocidade.y = 0; // zera a queda
		this.nasceu = true;
	}

	public boolean temSuporte(float x, float z) {
		// 1. configura uma hitbox temporaria na nova posição(x, posicao.y, z)
		float yBase = posicao.y;
		// usa blocoBox temporariamente pra a verificação, configurando na nova posição
		blocoBox.set(
			minVec.set(x - largura / 2, yBase, z - profundidade / 2), 
			maxVec.set(x + largura / 2, yBase + altura, z + profundidade / 2)
		);
		// 2. define a area de busca: um pouco abaixo da base da hitbox
		int minX = Mat.floor(blocoBox.min.x);
		int maxX = Mat.floor(blocoBox.max.x);
		// checa o bloco imediatamente abaixo da base(yBase - 0.1f)
		int yCheque = Mat.floor(yBase - 0.1f); 
		int minZ = Mat.floor(blocoBox.min.z);
		int maxZ = Mat.floor(blocoBox.max.z);

		for(int atualX = minX; atualX <= maxX; atualX++) {
			for(int atualZ = minZ; atualZ <= maxZ; atualZ++) {
				int id = Mundo.obterBlocoMundo(atualX, yCheque, atualZ);
				if(id != 0) {
					Bloco b = Bloco.numIds.get(id);
					// se encontrar um bloco solido na camada de checagem, ha suporte
					if(b != null && b.solido) return true;
				}
			}
		}
		// não encontrou suporte solido em nenhuma parte da area debaixo
		return false;
	}
}
