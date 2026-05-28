package com.minimine.entidades;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.minimine.mundo.Mundo;
import com.minimine.utils.Mat;
import com.minimine.mundo.blocos.Bloco;
import com.minimine.graficos.TipoRender;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.minimine.mundo.chunks.Chunk;
import com.minimine.mundo.Chave;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.DataInputStream;

public class Entidade {
	public String nome;
	public int vida;
	public int vidaMax;
	public float velo = 8f; // velocidade maxima no chão
	public float peso = 65f; // 65 kg

	// constantes de movimento
	public float aceleracaoChao = 0.45f; // impulso por frame no chão(0.0~1.0)
	public float atritoChao = 0.55f; // fator multiplicativo ao frear no chão(0.0~1.0)
	public float aceleracaoAr = 0.04f; // controle aéreo reduzido
	public float atritoAr = 0.98f; // quase sem frenagem no ar
	public boolean esquerda = false, frente = false, tras = false,
	direita = false, cima = false, baixo = false, acao = false;
	public boolean movendo = false, noChao = true, naAgua = false,
	agachado = false, nasceu = false, voando = false, correndo = false;

	public Vector3 posicao = new Vector3(1, 80, 1), velocidade = new Vector3();
	public final Vector3 frenteV = new Vector3(0, 0, 0), direitaV = new Vector3(0, 0, 0);
	public float pulo = 10f;

	public float largura = 0.6f, altura = 1.9f, profundidade = 0.6f;

	public BoundingBox hitbox = new BoundingBox();
	public final BoundingBox blocoHitbox = new BoundingBox(); // colisão dos blocos

	public final Vector3 minVec = new Vector3(), maxVec = new Vector3();

	public float yaw = 180f, tom = -20f; // pra ver onde ta olhando
	public float VELO_MAX_QUEDA = -50f;

	public String bioma = "";

	// === sistema de dano ===
	public static final int BLOCOS_QUEDA_SEGURA = 3;   // blocos sem dano
	public static final float TICK_REGEN = 5f;  // regenera 1 de vida a cada 5 segundos

	public float alturaMaxQueda = 0f; // Y mais alto registrado enquanto estava no ar
	public boolean rastreandoQueda = false;

	public float tempoInvulneravel = 0f; // segundos restantes de invulnerabilidade pós-dano
	public static final float DURACAO_INVUL = 0.5f;

	public float tempoRegen = 0f;

	// === combate ===
	public int danoAtaque = 1;
	public float intervaloAtaque = 0.5f; // segundos entre ataques
	public float tempoAtaque = 0f; // cooldown restante

	public float tempoPiscando = 0f; // efeito visual de dano
	public static final float DURACAO_PISCAR = 0.4f;

	public float[] dadosLuz = {15f, 0f};

	public Chunk chunkCache = null;

	public void attHitbox() {
		final float x = posicao.x;
		final float y = posicao.y;
		final float z = posicao.z;

		hitbox.set(minVec.set(x - largura / 2, y, z - profundidade / 2), maxVec.set(x + largura / 2, y + altura, z + profundidade / 2));
	}

	public boolean colideMundo() {
		final int minX = Mat.floor(hitbox.min.x);
		final int maxX = Mat.floor(hitbox.max.x);
		final int minY = Mat.floor(hitbox.min.y);
		final int maxY = Mat.floor(hitbox.max.y);
		final int minZ = Mat.floor(hitbox.min.z);
		final int maxZ = Mat.floor(hitbox.max.z);

		naAgua = false;

		for(int x = minX; x <= maxX; x++) {
			for(int y = minY; y <= maxY; y++) {
				for(int z = minZ; z <= maxZ; z++) {

					final int id = Mundo.obterBlocoMundo(x, y, z);
					if(id == 0) continue;

					final Bloco b = Bloco.numIds.get(id);

					blocoHitbox.set(
						minVec.set(x, y, z),
						maxVec.set(x + 1, y + 1, z + 1)
					);
					if(b.render == TipoRender.LIQUIDO) {
						naAgua = true;
						continue;
					} else if(!b.colisao) {
						continue;
					} else {
						if(hitbox.intersects(blocoHitbox)) return true;
					}
				}
			}
		}
		return false;
	}

	// para verificar se tem chão embaixo dos pés da entidade
	public boolean temSuporte(float x, float z) {
		// 1. configura uma hitbox temporaria na nova posição(x, posicao.y, z)
		final float yBase = posicao.y;
		// usa blocoBox temporariamente pra a verificação, configurando na nova posição
		blocoHitbox.set(
			minVec.set(x - largura / 2, yBase, z - profundidade / 2), 
			maxVec.set(x + largura / 2, yBase + altura, z + profundidade / 2)
		);
		// 2. define a area de busca: um pouco abaixo da base da hitbox
		final int minX = Mat.floor(blocoHitbox.min.x);
		final int maxX = Mat.floor(blocoHitbox.max.x);
		// checa o bloco imediatamente abaixo da base(yBase - 0.1f)
		final int yCheque = Mat.floor(yBase - 0.1f); 
		final int minZ = Mat.floor(blocoHitbox.min.z);
		final int maxZ = Mat.floor(blocoHitbox.max.z);

		for(int atualX = minX; atualX <= maxX; atualX++) {
			for(int atualZ = minZ; atualZ <= maxZ; atualZ++) {
				final int id = Mundo.obterBlocoMundo(atualX, yCheque, atualZ);
				if(id != 0) {
					final Bloco b = Bloco.numIds.get(id);
					// se encontrar um bloco solido na camada de checagem, ha suporte
					if(b != null && b.colisao) return true;
				}
			}
		}
		// não encontrou suporte solido em nenhuma parte da area debaixo
		return false;
	}

	public boolean ehChao() {
		// verifica se ha blocos solidos logo abaixo dos pes do jogador
		final float epsilon = 0.05f; // margem pra evitar flutuação
		final float yCheque = posicao.y - epsilon;

		final int minX = Mat.floor(posicao.x - largura / 2);
		final int maxX = Mat.floor(posicao.x + largura / 2);
		final int y = Mat.floor(yCheque);
		final int minZ = Mat.floor(posicao.z - profundidade / 2);
		final int maxZ = Mat.floor(posicao.z + profundidade / 2);

		for(int x = minX; x <= maxX; x++) {
			for(int z = minZ; z <= maxZ; z++) {
				final int id = Mundo.obterBlocoMundo(x, y, z);
				if(id != 0) {
					final Bloco b = Bloco.numIds.get(id);
					if(b != null && b.solido) {
						return true;
					}
				}
			}
		}
		return false;
	}

	// aplica dano respeitando invulnerabilidade; retorna true se o dano foi aplicado
	public boolean tomarDano(int dano) {
		if(tempoInvulneravel > 0f) return false;
		vida -= dano;
		if(vida < 0) vida = 0;
		tempoInvulneravel = DURACAO_INVUL;
		tempoPiscando = DURACAO_PISCAR;
		if(vida == 0) morreu();
		return true;
	}

	public void morreu() {}

	public void att(float delta) {
		if(voando) velocidade.y = 0;

		// escolhe aceleração e atrito dependendo de onde está
		final boolean noControle = noChao || voando || naAgua;
		final float acel  = noControle ? aceleracaoChao : aceleracaoAr;
		final float atrito = noControle ? atritoChao : atritoAr;

		// direção da entidade
		float desejadoX = 0, desejadoZ = 0;
		if(frente) { desejadoX += frenteV.x; desejadoZ += frenteV.z; }
		if(tras) { desejadoX -= frenteV.x; desejadoZ -= frenteV.z; }
		if(esquerda) { desejadoX += direitaV.x; desejadoZ += direitaV.z; }
		if(direita) { desejadoX -= direitaV.x; desejadoZ -= direitaV.z; }

		// normaliza pra evitar diagonal mais rapida
		final float tam = (float)Math.sqrt(desejadoX * desejadoX + desejadoZ * desejadoZ);
		if(tam > 1f) { desejadoX /= tam; desejadoZ /= tam; }

		final float veloAlvo = (agachado && noChao) ? velo * 0.5f : velo;
		desejadoX *= veloAlvo;
		desejadoZ *= veloAlvo;

		// impulso fixo em direção ao alvo (estilo Minecraft: fração do erro, independente de delta)
		velocidade.x += (desejadoX - velocidade.x) * acel;
		velocidade.z += (desejadoZ - velocidade.z) * acel;

		// atrito multiplicativo por frame quando sem entrada
		if(desejadoX == 0 && desejadoZ == 0) {
			velocidade.x *= atrito;
			velocidade.z *= atrito;
		}
		if(cima) {
			if(voando || noChao || naAgua) {
				velocidade.y = pulo;
				noChao = false;
			}
		}
		if(baixo) velocidade.y = -10;

		movendo = noChao && (Math.abs(velocidade.x) > 1f || Math.abs(velocidade.z) > 1f);

		final int _bx = Mat.floor(posicao.x);
		final int _by = Mat.floor(posicao.y + altura * 0.9f);
		final int _bz = Mat.floor(posicao.z);

		final long chave = Chave.calcularChave(_bx >> 4, _bz >> 4);

		if(chunkCache == null || chave != chunkCache.chave) {
			chunkCache = Mundo.chunks.get(chave);
		}
		if(chunkCache != null && _by >= 0 && _by < Mundo.Y_CHUNK) {
			final int _lx = _bx & 0xF, _lz = _bz & 0xF;
			final int _idc = _lx + (_lz << 4) + (_by << 8);
			final int _luzTotal = chunkCache.luz[_idc] & 0xFF;
			dadosLuz[0] = _luzTotal >> 4;
			dadosLuz[1] = _luzTotal & 0x0F;
		}
		// === ticks de dano ===

		if(tempoAtaque > 0f) {
			tempoAtaque -= delta;
			if(tempoAtaque < 0f) tempoAtaque = 0f;
		}
		if(tempoPiscando > 0f) {
			tempoPiscando -= delta;
			if(tempoPiscando < 0f) tempoPiscando = 0f;
		}
		// invulnerabilidade
		if(tempoInvulneravel > 0f) {
			tempoInvulneravel -= delta;
			if(tempoInvulneravel < 0f) tempoInvulneravel = 0f;
		}
		// rastreamento de queda: registra o Y mais alto enquanto estiver no ar
		if(!noChao && !naAgua && !voando) {
			if(!rastreandoQueda) {
				// começou a cair/pular agora
				alturaMaxQueda = posicao.y;
				rastreandoQueda = true;
			} else if(posicao.y > alturaMaxQueda) {
				// ainda subindo, atualiza o pico
				alturaMaxQueda = posicao.y;
			}
		} else if(noChao && rastreandoQueda) {
			// acabou de pousar: calcula o dano
			final float blocosCaidos = alturaMaxQueda - posicao.y;
			final float blocosAlemSeguro = blocosCaidos - BLOCOS_QUEDA_SEGURA;
			if(blocosAlemSeguro > 0f) {
				final int danoQueda = (int) blocosAlemSeguro; // 1 de dano por bloco acima do limite
				if(danoQueda > 0) tomarDano(danoQueda);
			}
			rastreandoQueda = false;
		} else if(naAgua || voando) {
			// entrou na água ou começou a voar: cancela rastreamento sem punir
			rastreandoQueda = false;
		}
		// regeneração automatica fora da água
		if(!naAgua && vida > 0 && vida < vidaMax) {
			tempoRegen += delta;
			if(tempoRegen >= TICK_REGEN) {
				tempoRegen -= TICK_REGEN;
				vida++;
				if(vida > vidaMax) vida = vidaMax;
			}
		} else if(naAgua) {
			tempoRegen = 0f;
		}
	}

	public void render(ModelBatch mb, float delta) {}
	public void liberar() {}

	public void salvar(DataOutputStream dos) throws IOException {
        dos.writeFloat(posicao.x);
        dos.writeFloat(posicao.y);
        dos.writeFloat(posicao.z);
        dos.writeFloat(yaw);
        dos.writeFloat(tom);
		dos.writeFloat(velo);
		dos.writeBoolean(agachado);
		dos.writeBoolean(nasceu);
        dos.flush();
    }

	public void carregar(DataInputStream dis) throws IOException {
        posicao = new Vector3(dis.readFloat(), dis.readFloat(), dis.readFloat());
        yaw = dis.readFloat();
        tom = dis.readFloat();
		velo = dis.readFloat();
		agachado = dis.readBoolean();
		nasceu = dis.readBoolean();

		if(agachado) {
			velo *= 2;
			altura *= 1.2f;
			agachado = false;
		}
    }
}
