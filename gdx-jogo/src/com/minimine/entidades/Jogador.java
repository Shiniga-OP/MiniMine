package com.minimine.entidades;

import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.collision.Ray;
import com.minimine.utils.Mat;
import com.minimine.mundo.blocos.Bloco;
import com.minimine.audio.Audio;
import com.minimine.mundo.Mundo;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.Model;
import net.mgsx.gltf.scene3d.scene.SceneAsset;
import net.mgsx.gltf.loaders.gltf.GLTFLoader;
import com.minimine.mundo.Biomas;
import com.minimine.cenas.Jogo;
import com.minimine.graficos.Modelos;

public class Jogador extends Entidade {
	public int modo = 2; // 0 = espectador, 1 = criativo, 2 = sobrevivencia
	public PerspectiveCamera camera;
	public float forcaMovimento = 0; // Controla a intensidade do balanço

	public CharSequence item = "ar";
	public int ALCANCE = 7;
	public Inventario inv;
	
    public ModelInstance instancia;
    
    public float tempoAnimacao = 0;

    public Quaternion rotTemp = new Quaternion();
    public Vector3 eulerTemp = new Vector3();

    // referencias dos nos
    public Node cabeca, tronco, bracoDir, bracoEsq, pernaDir, pernaEsq;

    // rotações originais
    public Quaternion rotCabeca = new Quaternion();
    public Quaternion rotTronco = new Quaternion();
    public Quaternion rotBracoDir = new Quaternion();
    public Quaternion rotBracoEsq = new Quaternion();
    public Quaternion rotPernaDir = new Quaternion();
    public Quaternion rotPernaEsq = new Quaternion();

	public Jogador() {
		super();
		vida = 20;
		vidaMax = 20;
		this.inv = new Inventario(this);
		Jogo.relogio.schedule(
			new java.util.TimerTask() {
				@Override
				public void run() {
					bioma = Biomas.obterBioma((int)posicao.x, (int)posicao.z);
				}
			}, 0, 500);
		try {
            instancia = new ModelInstance(Modelos.obterModelo("modelos/jogador.gltf"));
			
            pegarNos();
            salvarRotacoes();
        } catch(Exception e) {
            Gdx.app.error("[Jogador]", "Erro no GLTF: " + e.getMessage());
        }
		// deixa o braço reto pra frente
		bracoDir.rotation.set(rotBracoDir);
		// rotaciona 90 graus no eixo X
		bracoDir.rotation.mul(new Quaternion(Vector3.X, 100f));
		instancia.calculateTransforms();
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

		for(float t = 0; t < ALCANCE; t += 0.10f) { // passo menor = mais preciso
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
						blocoHitbox.set(minVec.set(xAnt, yAnt, zAnt), maxVec.set(xAnt + 1, yAnt + 1, zAnt + 1));
						attHitbox();
						if(blocoHitbox.intersects(hitbox)) return;
						Mundo.defBlocoMundo(xAnt, yAnt, zAnt, inv.itens[inv.slotSelecionado].nome);
						Bloco.tocarSom(item);

						if(modo == 2) inv.rmItem(inv.slotSelecionado, 1);
					}
				}
				return;
			}
		}
	}

	@Override
	public void att(float delta) {
		if(modo != 2) voando = true;
		super.att(delta);

		Inventario.Item itemInv = inv.itens[inv.slotSelecionado];
		if(itemInv != null && itemInv.nome != item) item = itemInv.nome;
		else if(itemInv == null) item = "ar";
		
		frenteV.x = camera.direction.x;
		frenteV.z = camera.direction.z;
		frenteV.nor();  
		direitaV.x = frenteV.z;
		direitaV.z = -frenteV.x;

		// gravidade no sobrevivencia
		if(naAgua) Mundo.GRAVIDADE = -10;
		else Mundo.GRAVIDADE = -30;

		if(modo == 2 && !noChao || naAgua) { 
			this.velocidade.y += Mundo.GRAVIDADE * delta;

			if(this.velocidade.y < VELO_MAX_QUEDA) {
				this.velocidade.y = VELO_MAX_QUEDA;
			}
		}
		if(modo == 0) {
			posicao.add(velocidade.x * delta, velocidade.y * delta, velocidade.z * delta);
			attHitbox();
			camera.position.set(posicao.x, posicao.y + altura * 0.95f, posicao.z);
			camera.update();
			return;
		}
		float dx = velocidade.x * delta;
		float dy = velocidade.y * delta;
		float dz = velocidade.z * delta;
		// primeiro verifica colisão vertical
		posicao.y += dy;
		attHitbox();

		if(colideMundo()) {
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
		if(colideMundo()) {
			posicao.x -= dx;
			velocidade.x = 0;
			attHitbox();
		}
		if(agachado && noChao && dz != 0 && !temSuporte(posicao.x, posicao.z + dz)) {
			dz = 0;
		}
		posicao.z += dz;
		attHitbox();
		if(colideMundo()) {
			posicao.z -= dz; 
			velocidade.z = 0;
			attHitbox();
		}
		if(posicao.y < -100f) posicao.y = Mundo.obterAlturaChao((int)posicao.x, (int)posicao.z);
		
		if(movendo) {
			tempoAnimacao += delta * 8f; // o tempo corre enquanto anda
			forcaMovimento = Math.min(1f, forcaMovimento + delta * 5f); // liga o balanço
		} else {
			forcaMovimento = Math.max(0f, forcaMovimento - delta * 5f); // desliga o balanço
			if(forcaMovimento == 0) tempoAnimacao = 0; // reinicia o ciclo ao parar totalmente
		}
		camera.position.set(posicao.x, posicao.y + altura * 0.9f, posicao.z);
		camera.update();
	}

	public void render(ModelBatch mb) {
		instancia.transform.set(camera.view);
		
		if(Math.abs(instancia.transform.det()) > 1e-6f) {
			instancia.transform.inv();
		} else {
			camera.update();
			return; // camera ainda não ta pronta, pula o render desse frame
		}
		// calculo do balanço(oscilação)
		// seno faz o movimento lateral(X)
		float balancoX = (float)Math.sin(tempoAnimacao * 0.5f) * 0.05f;
		// cosseno absoluto faz o movimento de "pulo" do passo(Y)
		float balancoY = (float)Math.abs(Math.cos(tempoAnimacao)) * 0.05f;

		// deixa o braço na tela
		instancia.transform.translate(0.5f + balancoX, -2.15f + balancoY, -1f);

		instancia.transform.rotate(Vector3.Y, 15); 

		mb.render(instancia);
	}

    public void pegarNos() {
        cabeca = instancia.getNode("cabeca", true);
        tronco = instancia.getNode("tronco", true);
        bracoDir = instancia.getNode("braco_dir", true);
        bracoEsq = instancia.getNode("braco_esq", true);
        pernaDir = instancia.getNode("perna_dir", true);
        pernaEsq = instancia.getNode("perna_esq", true);

		instancia.nodes.clear();
		instancia.nodes.add(bracoDir);
    }

    public void salvarRotacoes() {
        if(cabeca != null) rotCabeca.set(cabeca.rotation);
        if(tronco != null) rotTronco.set(tronco.rotation);
        if(bracoDir != null) rotBracoDir.set(bracoDir.rotation);
        if(bracoEsq != null) rotBracoEsq.set(bracoEsq.rotation);
        if(pernaDir != null) rotPernaDir.set(pernaDir.rotation);
        if(pernaEsq != null) rotPernaEsq.set(pernaEsq.rotation);
    }
}
