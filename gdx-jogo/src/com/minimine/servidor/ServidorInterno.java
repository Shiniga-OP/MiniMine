package com.minimine.servidor;

import com.badlogic.gdx.Gdx;
import com.minimine.mundo.Mundo;
import com.minimine.entidades.Jogador;
import com.minimine.utils.ArquivosUtil;
import com.minimine.cenas.Jogo;
import java.util.List;
import com.minimine.Inicio;

/*
 * em solo, sobe um Net(SERVIDOR_MODO) local e conecta o cliente
 * via Net(CLIENTE_MODO, "127.0.0.1")
* o Jogo passa a usar sempre o
 * caminho de rede, eliminando a bifurcação solo/multi
 
 * o servidor interno é o unico responsavel por:
 *   - carregar e salvar o mundo(ArquivosUtil.crMundo/svMundo)
 *   - ser a fonte de verdade do Mundo(estado, chunks, blocos)
 
 * O cliente local recebe tudo via protocolo, igual a um cliente remoto
*/
public class ServidorInterno {
	// instância do Net no modo servidor que roda localmente
	public static Net netServidor = null;
	public static boolean rodando = false;
	/*
	 * sobe o servidor interno numa thread separada e espera ele estar pronto
	 * para aceitar conexões antes de retornar
	 * chame isso antes de criar o Net do cliente local
	*/
	public static void iniciar(final Mundo mundo, final List<Jogador> jogadores) {
		if(rodando) return;
		rodando = true;

		// carrega o mundo antes de abrir o socket, servidor é a fonte de verdade
		if(ArquivosUtil.existe(Inicio.externo + "/MiniMine/mundos/" + mundo.nome + ".mini")) {
			ArquivosUtil.crMundo(mundo, jogadores.get(0));
		}
		// sobe o Net em modo servidor, vai abrir TCP e UDP nas portas padrão
		// Gdx.app.postRunnable não é usado aqui porque iniciarTcpServidor
		// precisa estar pronto antes do cliente conectar; Net.iniciarTcpServidor
		// ja lança sua propria thread interna
		netServidor = new Net(Net.SERVIDOR_MODO);

		// aguarda o socket estar aberto(max 3s) antes de deixar o cliente conectar
		int tentativas = 0;
		while(netServidor.servidorSocket == null && tentativas < 30) {
			try {
				Thread.sleep(100);
			} catch(InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			tentativas++;
		}
		if(netServidor.servidorSocket == null) {
			Gdx.app.error("[ServidorInterno]", "Timeout ao aguardar socket do servidor interno.");
		} else {
			Gdx.app.log("[ServidorInterno]", "Servidor interno pronto.");
		}
	}
	/*
	 * para o servidor interno e salva o mundo
	 * chame no dispose() do Jogo, depois de net.liberar()
	*/
	public static void parar(Mundo mundo, List<Jogador> jogadores) {
		if(!rodando) return;
		rodando = false;

		try {
			ArquivosUtil.svMundo(mundo, jogadores);
		} catch(Throwable t) {
			Gdx.app.error("[ServidorInterno]", "Erro ao salvar mundo no encerramento: " + t.getMessage());
		}
		if(netServidor != null) {
			netServidor.liberar();
			netServidor = null;
		}
		Gdx.app.log("[ServidorInterno]", "Servidor interno encerrado.");
	}
}

