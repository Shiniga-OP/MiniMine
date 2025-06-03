package com.minimine.engine;

import android.widget.EditText;
import com.engine.Logs;

public class Comandos {
	public GLRender render;
	public EditText chat;

	public Comandos(GLRender render, EditText chat) {
		this.render = render;
		this.chat = chat;
	}

    public void executar(String comando) {
		try {
			// tp:
			if(comando.startsWith("/tp ")) {
				comando = comando.replace("/tp ", "");
				String[] tokens = comando.split(" ");

				float x = Float.parseFloat(tokens[0].replace("~", String.valueOf(render.camera.posicao[0])));
				float y = Float.parseFloat(tokens[1].replace("~", String.valueOf(render.camera.posicao[1])));
				float z = Float.parseFloat(tokens[2].replace("~", String.valueOf(render.camera.posicao[2])));
				render.camera.posicao[0] = x;
				render.camera.posicao[1] = y;
				render.camera.posicao[2] = z;

				comando = "jogador teleportado para X: "+x+", Y: "+y+", Z: "+z;
				// chunk:
			} else if(comando.startsWith("/chunk raio ")) {
				comando = comando.replace("/chunk raio ", "");

				render.mundo.RAIO_CARREGAMENTO = Integer.parseInt(comando);

				comando = "chunks ao redor do jogador: "+render.mundo.CHUNK_TAMANHO*render.mundo.RAIO_CARREGAMENTO;
			} else if(comando.startsWith("/chunk tamanho")) {
				comando = comando.replace("/chunk tamanho ", "");

				render.mundo.CHUNK_TAMANHO = Integer.parseInt(comando);

				comando = "tamanho de chunk definido para "+render.mundo.CHUNK_TAMANHO+" blocos de largura";
				// bloco:
			} else if(comando.equals("/bloco ids")) {
				comando = "";
				for(String bloco : TipoBloco.tipos.keySet()) {
					comando += bloco + "\n";
				}
				// player:
			} else if(comando.startsWith("/player mao")) {
				comando = comando.replace("/player mao ", "");

				if(TipoBloco.tipos.containsKey(comando)) render.camera.itemMao = comando;
				else {
					comando = "esse tipo de bloco não existe, tipos existentes:\n\n";
					for(String bloco : TipoBloco.tipos.keySet()) {
						comando += bloco + "\n";
					}
				}
			} else if(comando.startsWith("/player passo ")) {
				comando = comando.replace("/player passo ", "");
				render.camera.velocidadeX = Float.parseFloat(comando);
			} else if(comando.startsWith("/player salto ")) {
				comando = comando.replace("/player salto ", "");
				render.camera.salto = Float.parseFloat(comando);
			}  else if(comando.startsWith("/player peso ")) {
				comando = comando.replace("/player peso ", "");

				render.camera.peso = Float.parseFloat(comando);
			} else if(comando.startsWith("/player hitbox[0] ")) {
				comando = comando.replace("/player hitbox[0] ", "");

				render.camera.hitbox[0] = Float.parseFloat(comando);
			} else if(comando.startsWith("/player hitbox[1] ")) {
				comando = comando.replace("/player hitbox[1] ", "");

				render.camera.hitbox[1] = Float.parseFloat(comando);
				// debug:
			} else if(comando.startsWith("/debug ")) {
				comando = comando.replace("/debug ", "");

				if(comando.equals("0")) {
					render.debug = false;
					comando = "debug desativado";
				} else if(comando.equals("1")) {
					render.debug = true;
					comando = "debug ativado";
				}
			} else if(comando.startsWith("/seed")) {
				comando = "seed atual: "+render.mundo.seed;
			} else if(comando.startsWith("/log")) {
				if(comando.startsWith("/log att")) Logs.capturar();
				else if(comando.startsWith("/log 0")) Logs.parar();
				// memoria
			} else if(comando.startsWith("/gc")) {
				if(comando.equals("/gc 1")) render.gc = true;
				if(comando.equals("/gc 0")) render.gc = false;
			} else if(comando.startsWith("/mundo ")) {
				if(comando.startsWith("/mundo salvar")) render.svMundo(render.mundo);
				if(comando.startsWith("/mundo carregar")) render.crMundo(render.mundo);
				// tempo
			} else if(comando.startsWith("/tempo velo ")) {
				comando = comando.replace("/tempo velo ", "");
				render.tempoVelo = Float.parseFloat(comando);
			}  else if(comando.startsWith("/tempo ")) {
				comando = comando.replace("/tempo ", "");
				render.tempo = Float.parseFloat(comando);
			} 
			System.out.println(comando);
		} catch(Exception e) {
			System.out.println("erro: " + e.getMessage());
			e.printStackTrace();
		}
    }
}
