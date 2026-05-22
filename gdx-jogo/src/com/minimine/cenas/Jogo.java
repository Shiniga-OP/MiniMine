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
import com.minimine.mundo.chunks.ChunkProcesso;
import com.minimine.mundo.chunks.ChunkLuz;
import com.minimine.mundo.chunks.ChunkMalha;
import com.minimine.servidor.Net;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import com.badlogic.gdx.math.MathUtils;
import com.minimine.servidor.ServidorInterno;
import java.util.Timer;
import com.minimine.utils.DiaNoiteUtil;
import com.minimine.utils.TarefasUtil;

public class Jogo implements Screen {
    public static Mundo mundo;
    public static List<Jogador> jogadores;
    public static int modo = 2;
    public static Renderizador render;
    public static boolean musicas = true, graficosTeste = false;

	public static ServidorInterno servidor;

    public static final float INTERVALO_POS = 0.05f;
    public float tempoPosicao = 0f;

    @Override
    public void show() {
		servidor = new ServidorInterno();

        mundo = new Mundo();
        jogadores = new ArrayList<>();
		TarefasUtil.iniciar();

        Bloco.iniciar();

        // solo/multi servidor: sobe servidor interno(carrega mundo, abre socket)
        // cliente remoto: pula servidor, mundo vem do servidor via protocolo
        if(!Net.CLIENTE_MODO.equals(MultiMenu.modoRede)) {
            servidor.iniciar(mundo, jogadores);
        } else {
            servidor.mundo = mundo;
            servidor.relogio = new Timer();
			mundo.diaNoite = new DiaNoiteUtil();
			if(mundo.ciclo) mundo.diaNoite.iniciar();
			mundo.iniciar(false);
            servidor.rodando = true;
        }
        Jogador jogador = new Jogador();
        jogador.modo = modo;
        jogadores.add(jogador);

        mundo.chunksMod.clear();

        // solo: sobe servidor interno e conecta cliente local em 127.0.0.1
        // multi servidor: idem, mas outros clientes também podem conectar
        // multi cliente: pula o servidor interno, conecta direto ao remoto
        if(!Net.CLIENTE_MODO.equals(MultiMenu.modoRede)) {
            // cliente local conecta via solo, mesmo caminho de qualquer cliente remoto
            servidor.netCliente = new Net(Net.CLIENTE_MODO, "127.0.0.1");
        } else {
            // cliente remoto: net ja pode ter vindo pronto do MultiMenu(conexão por IP)
            if(MultiMenu.netClientePronto != null) {
                servidor.netCliente = MultiMenu.netClientePronto;
                MultiMenu.netClientePronto = null;
            } else {
                servidor.netCliente = new Net(Net.CLIENTE_MODO);
            }
        }
		servidor.netCliente.ouvinte = new Net.OuvinteMensagem() {
            public void aoReceber(String msg) {
                servidor.processarMsg(msg);
            }
        };
		ChunkProcesso.luz = new ChunkLuz();
		ChunkProcesso.malha = new ChunkMalha();
		render = new Render(jogadores, mundo);

        render.iniciar();
		if(!Net.CLIENTE_MODO.equals(MultiMenu.modoRede)) {
			mundo.iniciar(true);
		}
        servidor.relogio.schedule(
            new java.util.TimerTask() {
                @Override
                public void run() {
                    if(musicas) Musicas.tocarAleatorio();
                }
            }, 0, 1000);
        try {
			LuaAPI.iniciar();
		} catch(Exception e) {}
    }

    @Override
    public void render(float delta) {
        render.att(delta);
        if(mundo.carregado) LuaAPI.att(delta);

        if(servidor.netCliente != null && jogadores.size() > 0) {
            tempoPosicao += delta;
            if(tempoPosicao >= INTERVALO_POS) {
                tempoPosicao = 0f;
                Jogador jg = jogadores.get(0);
                
                servidor.enviarPosicao(
                    jg.posicao.x, jg.posicao.y, jg.posicao.z,
                    jg.yaw, jg.tom
                );
            }
        }
    }

    @Override
    public void dispose() {
        mundo.carregado = false;
        render.liberar();
        Bloco.liberar();
		TarefasUtil.liberar();
        // servidor interno salva o mundo e fecha, clientes remotos ja foram desconectados
        servidor.parar(mundo, jogadores);
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
        if(servidor.rodando) {
            ArquivosUtil.svMundo(mundo, jogadores);
        }
    }
    @Override public void resume() {}
}
