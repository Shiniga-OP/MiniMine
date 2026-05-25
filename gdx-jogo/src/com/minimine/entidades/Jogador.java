package com.minimine.entidades;

import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.collision.Ray;
import com.minimine.utils.Mat;
import com.minimine.mundo.blocos.Bloco;
import com.minimine.graficos.TipoRender;
import com.minimine.audio.Audio;
import com.minimine.mundo.Mundo;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.minimine.cenas.Jogo;
import com.minimine.graficos.Modelos;
import com.minimine.mundo.blocos.InterfaceBloco;
import com.minimine.inventario.Inventario;
import com.badlogic.gdx.math.MathUtils;
import com.minimine.graficos.Texturas;
import com.badlogic.gdx.graphics.g3d.utils.RenderContext;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Matrix4;
import com.minimine.mundo.Chave;
import com.minimine.entidades.ItemMundo;
import com.minimine.inventario.Item;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.DataInputStream;
import com.badlogic.gdx.graphics.g3d.utils.AnimationController;
import com.minimine.ui.UI;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.minimine.ui.InterUtil;

public class Jogador extends Entidade {
	public String id;
	public int modo = 2;
	public int pessoa = 0;
	public PerspectiveCamera camera;
	public float forcaMov = 0;

	public String item = "ar";
	public String itemCache = "ar";
	public int ALCANCE = 7;
	public Inventario inv;

	public float tam = 1.30f;

	public ModelInstance instancia;
	public AnimationController animCtr;

	public float tempoAnimacao = 0;
	public float tempoDuploPulo = 0f;
	public static final float JANELA_DUPLO_PULO = 0.3f;

	// yaw do corpo, separado da câmera — atualiza com delay pra não ficar rígido
	public float yawTronco = 180f;

	public final Quaternion rotTemp = new Quaternion();
	public final Vector3 eulerTemp = new Vector3();

	public Node cabeca, tronco, bracoDir, bracoEsq, pernaDir, pernaEsq, itemPos;

	public final Quaternion rotCabeca = new Quaternion();
	public final Quaternion rotTronco = new Quaternion();
	public float troncoTransY = 0f;
	public final Quaternion rotBracoDir = new Quaternion();
	public final Quaternion rotBracoEsq = new Quaternion();
	public final Quaternion rotPernaDir = new Quaternion();
	public final Quaternion rotPernaEsq = new Quaternion();

	public ModelInstance modeloItem;

	public static SpriteBatch sbNome;
	private static final Vector3 posNome = new Vector3();

	public Jogador(String id) {
		super();
		this.id = id;
		nome = "Bruno";
		vida = 20;
		vidaMax = 20;
		camera = com.minimine.ui.UI.criarCamera();
		this.inv = new Inventario(this);
		attModelo();
	}

	@Override
	public void morreu() {
		posicao = new Vector3(0f, 0f, 0f);
		Mundo.limparChunks(0, 0);
		final long chave = Chave.calcularChave(0, 0);
		final com.minimine.mundo.chunks.Chunk mod = Mundo.chunksMod.get(chave);
		if(mod != null) Mundo.chunks.put(chave, mod);
		posicao.y = Mundo.obterAlturaChao((int)posicao.x, (int)posicao.z);
		Mundo.carregado = false;
		velocidade.set(0, 0, 0);
		vida = vidaMax;
		tempoInvulneravel = 3f;
	}

	public void interagirBloco() {
		final Ray raio = camera.getPickRay(
			Gdx.graphics.getWidth() >> 1,
			Gdx.graphics.getHeight() >> 1
		);
		final float olhoX = raio.origin.x;
		final float olhoY = raio.origin.y;
		final float olhoZ = raio.origin.z;
		final float dirX = raio.direction.x;
		final float dirY = raio.direction.y;
		final float dirZ = raio.direction.z;

		for(float t = 0; t < ALCANCE; t += 0.10f) {
			final int x = Mat.floor(olhoX + dirX * t);
			final int y = Mat.floor(olhoY + dirY * t);
			final int z = Mat.floor(olhoZ + dirZ * t);

			final Bloco bloco = Bloco.numIds.get(Mundo.obterBlocoMundo(x, y, z));

			if(bloco != null) {
				if(item.equals("ar") || bloco.render == TipoRender.LIQUIDO) {
					// servidor aplica e faz echo de volta, não aplica local
					if(Jogo.servidor.netCliente != null) Jogo.servidor.enviarBloco(x, y, z, 0, bloco.nome);
					Bloco.tocarSom(bloco.nome);
					if(bloco.evento != null) bloco.evento.aoDestruir(x, y, z);
				} else {
					if(bloco.ui != null) {
						bloco.ui.abrir(x, y, z);
						return;
					}
					final int xAnt = Mat.floor(olhoX + dirX * (t - 0.25f));
					final int yAnt = Mat.floor(olhoY + dirY * (t - 0.25f));
					final int zAnt = Mat.floor(olhoZ + dirZ * (t - 0.25f));

					if(Mundo.obterBlocoMundo(xAnt, yAnt, zAnt) == 0) {
						blocoHitbox.set(minVec.set(xAnt, yAnt, zAnt), maxVec.set(xAnt + 1, yAnt + 1, zAnt + 1));
						attHitbox();
						if(blocoHitbox.intersects(hitbox)) return;

						final Bloco blocoColocar = Bloco.texIds.get(item);
						final int idColocar = blocoColocar != null ? blocoColocar.tipo : 0;
						// servidor aplica e faz echo de volta, não aplica local
						if(Jogo.servidor.netCliente != null) Jogo.servidor.enviarBloco(xAnt, yAnt, zAnt, idColocar, "ar");
						Bloco.tocarSom(item);
						if(blocoColocar != null && blocoColocar.evento != null) {
							blocoColocar.evento.aoColocar(xAnt, yAnt, zAnt);
						}
						if(modo == 2) inv.rmItem(inv.slotSelecionado, 1);
					}
				}
				return;
			}
		}
	}

	@Override
	public boolean tomarDano(int dano) {
		if(modo == 2) return super.tomarDano(dano);
		return false;
	}

	@Override
	public void att(float delta) {
		UI.attCamera(camera.direction, yaw, tom);
        camera.up.set(0, 1, 0);

		if(modo == 0) voando = true;
		if(tempoDuploPulo > 0f) tempoDuploPulo -= delta;

		super.att(delta);

		// coleta deixados proximos
		final java.util.Iterator<Entidade> deixados = Mundo.entidades.iterator();
		while(deixados.hasNext()) {
			final Entidade e = deixados.next();
			if(!(e instanceof ItemMundo)) continue;
			final ItemMundo deixado = (ItemMundo)e;

			if(deixado.tempoVida <= 0f) {
				deixados.remove();
				continue;
			}
			final float dist = posicao.dst(deixado.posicao);

			// magnetismo: puxa o item quando perto o suficiente
			if(dist < ItemMundo.RAIO_ATRACAO) {
				final float vel = 8f * (1f - dist / ItemMundo.RAIO_ATRACAO);
				deixado.posicao.lerp(posicao, vel * delta);
			}
			// coleta efetiva
			if(dist < ItemMundo.RAIO_COLETA) {
				inv.addItem(deixado.nome, deixado.quantidade);
				deixados.remove();
			}
		}
		frenteV.x = camera.direction.x;
		frenteV.z = camera.direction.z;
		frenteV.nor();
		direitaV.x = frenteV.z;
		direitaV.z = -frenteV.x;

		if(naAgua) Mundo.GRAVIDADE = -10;
		else Mundo.GRAVIDADE = -30;

		if(!voando && !noChao || naAgua) {
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
		final float dy = velocidade.y * delta;
		float dz = velocidade.z * delta;

		posicao.y += dy;
		attHitbox();

		if(colideMundo()) {
			posicao.y -= dy;
			attHitbox();
			if(dy < 0) {
				noChao = true;
			} else if(dy > 0) {
				noChao = false;
			}
			velocidade.y = 0;
		} else {
			noChao = ehChao();
		}
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

		final boolean temMovi = frente || tras || esquerda || direita;
		if(temMovi) {
			final float veloHoriz = (float)Math.sqrt(velocidade.x * velocidade.x + velocidade.z * velocidade.z);
			final float fatorVelo = Math.min(1f, veloHoriz / velo);
			tempoAnimacao += delta * 8f * fatorVelo;
			forcaMov = Math.min(1f, forcaMov + delta * 5f);
		} else {
			forcaMov = Math.max(0f, forcaMov - delta * 5f);
		}
		if(pessoa == 0 || pessoa == 3) {
			// primeira pessoa: camera no olho do jogador, desce ao agachar
			final float alturaOlho = agachado ? altura * 0.72f : altura * 0.9f;
			camera.position.set(posicao.x, posicao.y + alturaOlho, posicao.z);
		} else if(pessoa == 1) {
			// terceira pessoa traseira: recua ao longo da direção completa da camera
			camera.position.set(
				posicao.x - camera.direction.x * DIST_TERCEIRA_PESSOA,
				posicao.y + altura * 0.9f - camera.direction.y * DIST_TERCEIRA_PESSOA,
				posicao.z - camera.direction.z * DIST_TERCEIRA_PESSOA
			);
		}
		if(tempoInvulneravel > 0 && modo == 2) {
			final float intensidade = tempoInvulneravel * 0.3f;
			camera.position.set(
				camera.position.x + (MathUtils.random(-1f, 1f) * intensidade),
				camera.position.y + (MathUtils.random(-1f, 1f) * intensidade * 0.5f),
				camera.position.z + (MathUtils.random(-1f, 1f) * intensidade)
			);
		}
		animCtr.update(delta);
		camera.update();

		instancia.userData = dadosLuz;
		if(modeloItem != null) modeloItem.userData = dadosLuz;

		float diffYaw = yaw - yawTronco;
		while(diffYaw > 180f) diffYaw -= 360f;
		while(diffYaw < -180f) diffYaw += 360f;
		if(temMovi) {
			// interpolação suave
			final float veloGiro = 6f;
			yawTronco += diffYaw * Math.min(1f, veloGiro * delta);
		} else {
			if(diffYaw > 60f) yawTronco = yaw - 60f;
			else if(diffYaw < -60f) yawTronco = yaw + 60f;
		}
	}
	
	@Override
	public void render(ModelBatch mb) {
		if(!item.equals(itemCache)) {
			itemCache = item;
			modeloItem = Modelos.modeloItem(item);
		}
		if(pessoa == 0) {
			// primeira pessoa: renderiza braço no espaço da camera(sem profundidade)
			instancia.transform.set(camera.view);

			if(Math.abs(instancia.transform.det()) > 1e-6f) {
				instancia.transform.inv();
			} else {
				camera.update();
				return;
			}
			final float balancoX = MathUtils.sin(tempoAnimacao * 0.5f) * 0.05f;
			final float balancoY = Math.abs(MathUtils.cos(tempoAnimacao)) * 0.05f;

			instancia.transform.translate(0.5f + balancoX, -2.15f + balancoY, -0.5f);
			instancia.transform.rotate(Vector3.Y, 15);

			instancia.transform.scale(tam, tam, tam);

			instancia.calculateTransforms();

			mb.flush();
			Gdx.gl.glClear(GL20.GL_DEPTH_BUFFER_BIT);
		} else if(pessoa == 1 || pessoa == 3) {
			// renderiza modelo e rotaciona pelo yaw do corpo
			instancia.transform.idt();
			instancia.transform.translate(posicao.x, posicao.y, posicao.z);
			instancia.transform.rotate(Vector3.Y, yawTronco + 180f);

			instancia.transform.scale(tam, tam, tam);
			attAnimacao();
			instancia.calculateTransforms();
		}
		mb.render(instancia);
		if(modeloItem != null) {
			modeloItem.transform.set(instancia.transform).mul(itemPos.globalTransform);
			modeloItem.calculateTransforms();
			mb.render(modeloItem);
		}
	}
	
	public void renderNome(PerspectiveCamera camera) {
		if(pessoa == 3 && nome != null) {
			posNome.set(posicao.x, posicao.y + altura + 0.3f, posicao.z);
			Vector3 tela = camera.project(posNome);
			if(tela.z >= 0f && tela.z <= 1f) {
				if(sbNome == null) {
					sbNome = new SpriteBatch();
				}
				sbNome.getProjectionMatrix().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
				sbNome.begin();
				UI.fonte.draw(sbNome, nome, tela.x - nome.length() * 5, tela.y);
				sbNome.end();
			}
		}
	}

	public void pegarNos() {
		cabeca = instancia.getNode("cabeca", true);
		tronco = instancia.getNode("tronco", true);
		bracoDir = instancia.getNode("braco_dir", true);
		bracoEsq = instancia.getNode("braco_esq", true);
		pernaDir = instancia.getNode("perna_dir", true);
		pernaEsq = instancia.getNode("perna_esq", true);
		itemPos = instancia.getNode("item", true);

		if(pessoa == 0) {
			// primeira pessoa: so mostra braço direito e item
			instancia.nodes.clear();
			instancia.nodes.add(bracoDir);
			instancia.nodes.add(itemPos);
		}
	}

	public static final float DIST_TERCEIRA_PESSOA = 4f;

	public void trocarPessoa() {
		pessoa = (pessoa + 1) % 3;
		attModelo();
	}

	public void attModelo() {
		try {
			instancia = new ModelInstance(Modelos.obterModelo("modelos/jogador.gltf"));
			pegarNos();
			salvarRotacoes();
		} catch(Exception e) {
			Gdx.app.error("[Jogador]", "Erro ao trocar visão: " + e.getMessage());
		}
		if(pessoa == 0) {
			bracoDir.rotation.set(rotBracoDir);
			bracoDir.rotation.mul(new Quaternion(Vector3.X, 100f));
			instancia.calculateTransforms();
		}
		animCtr = new AnimationController(instancia);
	}

	public void attAnimacao() {
		if(cabeca == null || tronco == null) return;

		final float tomPreso = Math.max(-80f, Math.min(80f, tom));

		// diferença horizontal entre onde a cmera aponta e onde o corpo aponta
		// isso faz a cabeça "olhar pro lado" quando o corpo ainda não virou
		float diffYaw = yaw - yawTronco;
		while(diffYaw > 180f) diffYaw -= 360f;
		while(diffYaw < -180f) diffYaw += 360f;
		final float diffYawPreso = Math.max(-80f, Math.min(80f, diffYaw));

		// cabeça: tom no X + yaw relativo ao tronco no Y
		cabeca.rotation.set(rotCabeca);

		cabeca.rotation.mul(new Quaternion(Vector3.Y, diffYawPreso));
		cabeca.rotation.mul(new Quaternion(Vector3.X, tomPreso));

		// braços e pernas: balançar ao andar(escala pela forcaMov, que ja depende de velo)
		final float balanco = MathUtils.sin(tempoAnimacao) * forcaMov;
		final float balancoBraco = balanco * 40f;
		final float balancoPerna = balanco * 35f;

		bracoDir.rotation.set(rotBracoDir);
		bracoDir.rotation.mul(new Quaternion(Vector3.X, balancoBraco));

		bracoEsq.rotation.set(rotBracoEsq);
		bracoEsq.rotation.mul(new Quaternion(Vector3.X, -balancoBraco));

		pernaDir.rotation.set(rotPernaDir);
		pernaDir.rotation.mul(new Quaternion(Vector3.X, -balancoPerna));

		pernaEsq.rotation.set(rotPernaEsq);
		pernaEsq.rotation.mul(new Quaternion(Vector3.X, balancoPerna));

		// agachamento
		if(agachado) {
			animCtr.setAnimation("agachar", -1);
		} else {
			animCtr.setAnimation(null, 0);
		}
	}

	public void salvarRotacoes() {
		if(cabeca != null) rotCabeca.set(cabeca.rotation);
		if(tronco != null) { rotTronco.set(tronco.rotation); troncoTransY = tronco.translation.y; }
		if(bracoDir != null) rotBracoDir.set(bracoDir.rotation);
		if(bracoEsq != null) rotBracoEsq.set(bracoEsq.rotation);
		if(pernaDir != null) rotPernaDir.set(pernaDir.rotation);
		if(pernaEsq != null) rotPernaEsq.set(pernaEsq.rotation);
	}

	@Override
	public void salvar(DataOutputStream dos) throws IOException {
		super.salvar(dos);
		dos.writeInt(modo);
		dos.writeUTF(""+item);
        dos.writeInt(ALCANCE);
		dos.writeInt(inv != null ? inv.slotSelecionado : 0);
	}

	@Override
	public void carregar(DataInputStream dis) throws IOException {
		super.carregar(dis);
        modo = dis.readInt();
        item = dis.readUTF();
        ALCANCE = dis.readInt();
        if(inv == null) inv = new Inventario(this);
        inv.slotSelecionado = dis.readInt();
    }
}

