package com.minimine.entidades;

import java.util.Iterator;
import java.util.Random;
import com.minimine.mundo.Mundo;
import com.minimine.mundo.Chave;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import com.minimine.cenas.Jogo;
import com.minimine.mundo.chunks.Chunk;

public class GerenciadorEntidades {
	public static final Random aleatorio = new Random();
    public static float tempo = 0f;
    public static final float INTERVALO = 5f; // segundos
    public static final int MAX_ENTIDADES = 20;
    public static final float DIST_MIN_NASCER = 10f;

	public static void att(float delta, Mundo mundo, List<Jogador> jogadores) {
		// tick de nascimdnto: independente de ter entidades visiveis
		if(mundo.carregado) {
			tempo += delta;
			if(tempo >= INTERVALO && mundo.entidades.size() < MAX_ENTIDADES) {
				tempo = 0f;
				for(int k = 0; k < jogadores.size(); k++) {
					tentarNascerEntidade(jogadores.get(k), mundo);
				}
			}
		}

		// remove entidades se saiu da area visivel
		for(int i = 0; i < mundo.entidades.size(); i++) {
			final Entidade e = mundo.entidades.get(i);

			boolean ehVisivel = false;

			for(int k = 0; k < jogadores.size(); k++) {
				final Jogador jg = jogadores.get(k);
				if(mundo.noRaioVisivel(jg, e.posicao.x, e.posicao.z)) {
					ehVisivel = true;
					break;
				}
			}
			if(!ehVisivel) {
				e.liberar();
				mundo.entidades.remove(i);
				i--;
				continue;
			}
			e.att(delta);

			if(e.naAgua) {
				// empuxo quase cancela a gravidade; foca fica levemente suspensa
				final float empuxo = -mundo.GRAVIDADE * 0.92f; // ~27.6, quase neutraliza os -30
				e.velocidade.y += (mundo.GRAVIDADE + empuxo) * delta;
				// amortece velocidade vertical na água pra dar sensação de resistencia do fluido
				e.velocidade.y *= (float)Math.pow(0.85, 1);
			} else if(!e.voando) {
				e.velocidade.y += mundo.GRAVIDADE * delta;
			}
			if(e.velocidade.y < e.VELO_MAX_QUEDA) e.velocidade.y = e.VELO_MAX_QUEDA;
		}
	}

	public static void tentarNascerEntidade(Jogador jogador, Mundo mundo) {
		// pega um chunk carregado aleatório(estado 2 = malha pronta)
		final List<Long> disponiveis = new ArrayList<>();
		for(Chunk e : mundo.chunks.values()) {
			if(e.estado == 4) disponiveis.add(e.chave);
		}
		if(disponiveis.isEmpty()) return;

		// embaralha tentando até 5 chunks candidatos
		for(int t = 0; t < 10; t++) {
			final long chave = disponiveis.get(aleatorio.nextInt(disponiveis.size()));
			final int cx = Chave.x(chave);
			final int cz = Chave.z(chave);

			// posição aleatoria dentro da chunk
			final int mx = cx * mundo.TAM_CHUNK + aleatorio.nextInt(mundo.TAM_CHUNK);
			final int mz = cz * mundo.TAM_CHUNK + aleatorio.nextInt(mundo.TAM_CHUNK);

			// distancia minima do jogador
			final float dx = mx - jogador.posicao.x;
			final float dz = mz - jogador.posicao.z;
			if(dx * dx + dz * dz < DIST_MIN_NASCER * DIST_MIN_NASCER) continue;

			final int vy = mundo.obterAlturaChao(mx, mz);
			if(vy <= 1) continue;

			final String bioma = mundo.motor.obterBioma(mx, mz);

			final List<DadosCriatura> candidatos = mundo.registroCriaturas.paraOBioma(bioma);
			if(candidatos.isEmpty()) continue;

			final DadosCriatura escolhido = sortearPorRaridade(candidatos);
			if(escolhido == null) continue;

			// aplica chanceNascimento se definida no JSON
			if(escolhido.chanceNascimento > 0f && aleatorio.nextFloat() > escolhido.chanceNascimento) continue;

			mundo.entidades.add(new Criatura(escolhido, mx, vy, mz));
			return;
		}
	}

	public static DadosCriatura sortearPorRaridade(List<DadosCriatura> lista) {
		float total = 0f;
		for(int i = 0; i < lista.size(); i++) {
			final DadosCriatura m = lista.get(i);
			total += m.raridade;
		}
		float sorteio = aleatorio.nextFloat() * total;
		float acum = 0f;
		for(int i = 0; i < lista.size(); i++) {
			final DadosCriatura m = lista.get(i);
			acum += m.raridade;
			if(sorteio <= acum) return m;
		}
		return lista.get(lista.size() - 1);
	}
}
