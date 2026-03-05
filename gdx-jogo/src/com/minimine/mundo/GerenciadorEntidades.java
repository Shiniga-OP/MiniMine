package com.minimine.mundo;

import java.util.Random;
import java.util.List;
import java.util.ArrayList;
import com.minimine.entidades.Jogador;
import java.util.Map;
import com.minimine.entidades.Foca;
import com.minimine.entidades.Entidade;
import java.util.Iterator;

public class GerenciadorEntidades {
	public static final Random aleatorio = new Random();
    public static float tempo = 0f;
    public static final float INTERVALO = 5f; // segundos
    public static final int MAX_ENTIDADES = 20;
    public static final float DIST_MIN_NASCER = 10f;
	
	public static void att(float delta, Mundo mundo, Jogador jg) {
		// remove entidades se saiu da area visivel
		Iterator<Entidade> it = mundo.entidades.iterator();
		while(it.hasNext()) {
			Entidade e = it.next();
			long chaveE = Chave.calcularChave((int)e.posicao.x >> 4, (int)e.posicao.z >> 4);
			if(!mundo.chunks.containsKey(chaveE)) {
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
				float empuxo = -mundo.GRAVIDADE * 0.92f; // ~27.6, quase neutraliza os -30
				e.velocidade.y += (mundo.GRAVIDADE + empuxo) * delta;
				// amortece velocidade vertical na água pra dar sensação de resistencia do fluido
				e.velocidade.y *= (float)Math.pow(0.85, e instanceof Foca ? 1.5 : 1);
			} else {
				e.velocidade.y += mundo.GRAVIDADE * delta;
			}
			if(e.velocidade.y < e.VELO_MAX_QUEDA) e.velocidade.y = e.VELO_MAX_QUEDA;
		}
	}
	
	public static void tentarNascerEntidade(Jogador jogador, Mundo mundo) {
		// pega um chunk carregado aleatório(estado 2 = malha pronta)
		List<Long> disponiveis = new ArrayList<>();
		for(Map.Entry<Long, Integer> e : mundo.estados.entrySet()) {
			if(e.getValue() == 2) disponiveis.add(e.getKey());
		}
		if(disponiveis.isEmpty()) return;

		// embaralha tentando até 5 chunks candidatos
		for(int t = 0; t < 5; t++) {
			long chave = disponiveis.get(aleatorio.nextInt(disponiveis.size()));
			int cx = Chave.x(chave);
			int cz = Chave.z(chave);

			// posição aleatória dentro da chunk
			int mx = cx * mundo.TAM_CHUNK + aleatorio.nextInt(mundo.TAM_CHUNK);
			int mz = cz * mundo.TAM_CHUNK + aleatorio.nextInt(mundo.TAM_CHUNK);

			// distancia minima do jogador
			float dx = mx - jogador.posicao.x;
			float dz = mz - jogador.posicao.z;
			if(dx * dx + dz * dz < DIST_MIN_NASCER * DIST_MIN_NASCER) continue;

			int wy = mundo.obterAlturaChao(mx, mz);
			if(wy <= 1) continue;
			
			Entidade entidade = null;
			
			String bioma = Mundo.motor.obterBioma(mx, mz);
			
			if(bioma.equals("Mar Congelado")) entidade = new Foca(mx, wy, mz);
			
			if(entidade != null) mundo.entidades.add(entidade);
			return;
		}
	}
}
