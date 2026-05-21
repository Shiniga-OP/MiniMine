package com.minimine.cenas;

import com.badlogic.gdx.Screen;
import com.minimine.entidades.Jogador;
import com.minimine.audio.Musicas;
import com.minimine.mundo.Mundo;
import com.minimine.graficos.Render;
import com.minimine.utils.ArquivosUtil;
import com.minimine.mods.LuaAPI;
import com.minimine.mundo.blocos.Bloco;
import com.badlogic.gdx.Gdx;
import com.minimine.graficos.Renderizador;
import com.minimine.graficos.teste.GraficosTeste;
import com.minimine.mundo.chunks.ChunkProcesso;
import com.minimine.mundo.chunks.ChunkLuz;
import com.minimine.mundo.chunks.ChunkMalha;
import com.minimine.servidor.Net;
import com.minimine.servidor.ServidorInterno;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import com.badlogic.gdx.math.MathUtils;

public class Jogo implements Screen {
    public static Mundo mundo;
    public static List<Jogador> jogadores;
    public static int modo = 2;
    public static Renderizador render;
    public static boolean musicas = true, graficosTeste = false;
    public static java.util.Timer relogio;
    public static Net net;

    public static final float INTERVALO_POS = 0.05f;
    public float tempoPosicao = 0f;
    public Map<Integer, Jogador> jogadoresRede = new HashMap<Integer, Jogador>();

    @Override
    public void show() {
        relogio = new java.util.Timer();
        mundo = new Mundo();
        jogadores = new ArrayList<Jogador>();
        Jogador jogador = new Jogador();
        jogador.modo = modo;
        jogadores.add(jogador);
        jogadoresRede.clear();

        mundo.chunksMod.clear();

        Bloco.iniciar();

        // solo: sobe servidor interno e conecta cliente local em 127.0.0.1
        // multi servidor: idem, mas outros clientes também podem conectar
        // multi cliente: pula o servidor interno, conecta direto ao remoto
        if(!Net.CLIENTE_MODO.equals(MultiMenu.modoRede)) {
            // sobe servidor interno(carrega o mundo, abre socket TCP/UDP)
            ServidorInterno.iniciar(mundo, jogadores);
            // cliente local conecta via solo, mesmo caminho de qualquer cliente remoto
            net = new Net(Net.CLIENTE_MODO, "127.0.0.1");
        } else {
            // cliente remoto: net ja pode ter vindo pronto do MultiMenu(conexão por IP)
            if(net == null) net = new Net(Net.CLIENTE_MODO);
        }
        net.ouvinte = new Net.OuvinteMensagem() {
            public void aoReceber(String msg) {
                processarMsgRede(msg);
            }
        };
        if(graficosTeste) {
            ChunkProcesso.luz = new ChunkLuz();
            ChunkProcesso.malha = new ChunkMalha();
            render = new GraficosTeste(jogadores, mundo);
        } else {
            ChunkProcesso.luz = new ChunkLuz();
            ChunkProcesso.malha = new ChunkMalha();
            render = new Render(jogadores, mundo);
        }
        render.iniciar();

        relogio.schedule(
            new java.util.TimerTask() {
                @Override
                public void run() {
                    if(musicas) Musicas.tocarAleatorio();
                }
            }, 0, 1000);
        try {
			LuaAPI.iniciar();
		} catch(Exception e) {
			Gdx.app.log("[Jogo]", "[ERRO]: "+e);
		}
    }

    public void processarMsgRede(String msg) {
        if(msg.startsWith("POS:")) {
            String[] p = msg.split(":");
            if(p.length < 7) return;
            try {
                int id = Integer.parseInt(p[1]);
                if(net != null && id == net.idLocal) return;
                float x = Float.parseFloat(p[2]);
                float y = Float.parseFloat(p[3]);
                float z = Float.parseFloat(p[4]);
                float yaw = Float.parseFloat(p[5]);
                float tom = Float.parseFloat(p[6]);
                Jogador jgRede = jogadoresRede.get(id);
                if(jgRede == null) {
                    jgRede = new Jogador();
                    jgRede.modo = modo;
                    jgRede.trocarPessoa();
                    jogadoresRede.put(id, jgRede);
                    jogadores.add(jgRede);
                    Gdx.app.log("[Jogo]", "jogador remoto " + id + " adicionado");
                }
                jgRede.posicao.set(x, y, z);
                jgRede.camera.direction.set(0, 0, -1);
                jgRede.camera.rotate(com.badlogic.gdx.math.Vector3.Y, yaw);
            } catch(NumberFormatException e) {}
        } else if(msg.startsWith("SEMENTE:")) {
            try {
                Mundo.semente = Long.parseLong(msg.substring(8).trim());
            } catch(NumberFormatException e) {}
        } else if(msg.startsWith("CHUNKS_FIM:")) {
            Gdx.app.log("[Jogo]", "Mundo recebido do servidor");
        } else if(msg.startsWith("BLOCO:")) {
            String[] p = msg.split(":");
            if(p.length < 5) return;
            try {
                int x = Integer.parseInt(p[1]);
                int y = Integer.parseInt(p[2]);
                int z = Integer.parseInt(p[3]);
                int id = Integer.parseInt(p[4]);
                mundo.defBlocoMundo(x, y, z, id);
            } catch(NumberFormatException e) {}
        } else if(msg.startsWith("ENTROU:")) {
            String[] p = msg.split(":");
            if(p.length < 2) return;
            try {
                int id = Integer.parseInt(p[1]);
                if(net != null && id == net.idLocal) return;
                if(!jogadoresRede.containsKey(id)) {
                    Jogador jgRede = new Jogador();
                    jgRede.modo = modo;
                    jgRede.trocarPessoa();
                    jogadoresRede.put(id, jgRede);
                    jogadores.add(jgRede);
                    Gdx.app.log("[Jogo]", "jogador remoto " + id + " entrou");
                }
            } catch(NumberFormatException e) {}
        } else if(msg.startsWith("SAIU:")) {
            String[] p = msg.split(":");
            if(p.length < 2) return;
            try {
                int id = Integer.parseInt(p[1]);
                Jogador jgRede = jogadoresRede.remove(id);
                if(jgRede != null) {
                    jogadores.remove(jgRede);
                    Gdx.app.log("[Jogo]", "jogador remoto " + id + " removido");
                }
            } catch(NumberFormatException e) {}
        }
    }

    @Override
    public void render(float delta) {
        render.att(delta);
        if(mundo.carregado) LuaAPI.att(delta);

        if(net != null && jogadores.size() > 0) {
            tempoPosicao += delta;
            if(tempoPosicao >= INTERVALO_POS) {
                tempoPosicao = 0f;
                Jogador jg = jogadores.get(0);
                float yaw = MathUtils.atan2(
                    jg.camera.direction.x, jg.camera.direction.z
                ) * MathUtils.radiansToDegrees;
                net.enviarPosicao(
                    jg.posicao.x, jg.posicao.y, jg.posicao.z,
                    yaw, 0
                );
            }
        }
    }

    @Override
    public void dispose() {
        mundo.carregado = false;
        relogio.cancel();
        render.liberar();
        Bloco.liberar();
        jogadoresRede.clear();
        if(net != null) {
            net.liberar();
            net = null;
        }
        // servidor interno salva o mundo e fecha, clientes remotos ja foram desconectados
        ServidorInterno.parar(mundo, jogadores);
    }

    @Override
    public void resize(int v, int h) {
        render.ui.ajustar(v, h);
        LuaAPI.ajustar(v, h);
    }

    @Override
    public void hide() {
        dispose();
    }
    @Override
    public void pause() {
        // salva via servidor interno, ele é a fonte de verdade
        if(ServidorInterno.rodando) {
            ArquivosUtil.svMundo(mundo, jogadores);
        }
    }
    @Override public void resume() {}
}
