package com.minimine.entidades;

import java.util.Iterator;
import java.util.Random;
import com.minimine.mundo.Mundo;
import com.minimine.mundo.Chave;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import com.minimine.cenas.Jogo;

public class GerenciadorEntidades {
	public static final Random aleatorio = new Random();
    public static float tempo = 0f;
    public static final float INTERVALO = 5f; // segundos
    public static final int MAX_ENTIDADES = 20;
    public static final float DIST_MIN_NASCER = 10f;

	public static void att(float delta, Mundo mundo, Jogador jg) {
		// remove entidades se saiu da area visivel
		final Iterator<Entidade> it = mundo.entidades.iterator();
		while(it.hasNext()) {
			final Entidade e = it.next();
			
			boolean ehVisivel = false;
			
			for(Jogador jgR :Jogo.jogadores) {
				if(mundo.noRaioVisivel(jgR, e.posicao.x, e.posicao.z)) {
					ehVisivel = true;
					break;
				}
			}
			if(!ehVisivel) {
				e.liberar();
				it.remove();
			}
		}
		if(mundo.carregado) {
			tempo += delta;
			if(tempo >= INTERVALO && mundo.entidades.size() < MAX_ENTIDADES) {
				tempo = 0f;
				tentarNascerEntidade(jg, mundo);
			}
		}
		for(Entidade e : mundo.entidades) {
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
		for(Map.Entry<Long, Integer> e : mundo.estados.entrySet()) {
			if(e.getValue() == 2) disponiveis.add(e.getKey());
		}
		if(disponiveis.isEmpty()) return;

		// embaralha tentando até 5 chunks candidatos
		for(int t = 0; t < 5; t++) {
			final long chave = disponiveis.get(aleatorio.nextInt(disponiveis.size()));
			final int cx = Chave.x(chave);
			final int cz = Chave.z(chave);

			// posição aleatória dentro da chunk
			final int mx = cx * mundo.TAM_CHUNK + aleatorio.nextInt(mundo.TAM_CHUNK);
			final int mz = cz * mundo.TAM_CHUNK + aleatorio.nextInt(mundo.TAM_CHUNK);

			// distancia minima do jogador
			final float dx = mx - jogador.posicao.x;
			final float dz = mz - jogador.posicao.z;
			if(dx * dx + dz * dz < DIST_MIN_NASCER * DIST_MIN_NASCER) continue;

			final int vy = mundo.obterAlturaChao(mx, mz);
			if(vy <= 1) continue;

			final String bioma = Mundo.motor.obterBioma(mx, mz);

			final List<DadosCriatura> candidatos = Mundo.registroCriaturas.paraOBioma(bioma);
			if(candidatos.isEmpty()) return;

			final DadosCriatura escolhido = sortearPorRaridade(candidatos);
			if(escolhido == null) return;

			mundo.entidades.add(new Criatura(escolhido, mx, vy, mz));
			return;
		}
	}
	public static DadosCriatura sortearPorRaridade(List<DadosCriatura> lista) {
		float total = 0f;
		for(DadosCriatura m : lista) total += m.raridade;
		float sorteio = aleatorio.nextFloat() * total;
		float acum = 0f;
		for(DadosCriatura m : lista) {
			acum += m.raridade;
			if(sorteio <= acum) return m;
		}
		return lista.get(lista.size() - 1);
	}
}

