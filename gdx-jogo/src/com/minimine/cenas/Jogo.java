package com.minimine.cenas;

import com.badlogic.gdx.Screen;
import com.minimine.entidades.Jogador;
import com.minimine.audio.Musicas;
import com.minimine.mundo.Mundo;
import com.minimine.graficos.Render;
import com.minimine.utils.ArquivosUtil;
import com.minimine.mods.LuaAPI;
import com.minimine.Inicio;
import com.minimine.utils.DiaNoiteUtil;
import com.minimine.mundo.blocos.Bloco;
import com.minimine.mundo.geracao.MotorGeracao;
import com.badlogic.gdx.Gdx;
import com.minimine.mundo.geracao.RegistroBiomas;
import com.minimine.graficos.Renderizador;
import com.minimine.graficos.teste.GraficosTeste;
import com.minimine.mundo.chunks.ChunkProcesso;
import com.minimine.mundo.chunks.ChunkLuz;
import com.minimine.mundo.chunks.ChunkMalha;
import com.minimine.utils.Net;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.util.zip.InflaterInputStream;

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

        if(MultiMenu.modoRede != null) {
            if(net == null) {  // so cria se não veio pronto do MultiMenu(conexão por IP)
                net = new Net(MultiMenu.modoRede);
            }
            net.ouvinte = new Net.OuvinteMensagem() {
                public void aoReceber(String msg) {
                    processarMsgRede(msg);
                }
            };
            net.ouvinteChunk = new Net.OuvinteChunk() {
                public void aoReceberChunk(com.minimine.mundo.chunks.Chunk chunk, long chave) {
                    Mundo.chunks.put(chave, chunk);
                    Mundo.estados.put(chave, 2);
                }
            };
        }

        if(graficosTeste) {
            ChunkProcesso.luz = new ChunkLuz();
            ChunkProcesso.malha = new ChunkMalha();
            render = new GraficosTeste(jogadores, mundo);
        } else {
            ChunkProcesso.luz = new ChunkLuz();
            ChunkProcesso.malha = new ChunkMalha();
            render = new Render(jogadores, mundo);
        }
        if(!Net.CLIENTE_MODO.equals(MultiMenu.modoRede)) {
			if(ArquivosUtil.existe(Inicio.externo+"/MiniMine/mundos/"+mundo.nome+".mini")) {
				ArquivosUtil.crMundo(mundo, jogador);
			}
		}
        render.iniciar();

        relogio.schedule(
            new java.util.TimerTask() {
                @Override
                public void run() {
                    if(musicas) Musicas.tocarAleatorio();
                }
            }, 0, 1000);
        LuaAPI.iniciar();
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
        } else if(msg.startsWith("CHUNK:")) {
            final String hex = msg.substring(6);
            new Thread(new Runnable() {
					public void run() {
						try {
							byte[] comprimido = new byte[hex.length() / 2];
							for(int i = 0; i < comprimido.length; i++)
								comprimido[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
							java.util.zip.InflaterInputStream inflater = new java.util.zip.InflaterInputStream(
								new ByteArrayInputStream(comprimido));
							final DataInputStream dis = new DataInputStream(inflater);
							final int cx = dis.readInt();
							final int cz = dis.readInt();
							final long chave = com.minimine.mundo.Chave.calcularChave(cx, cz);
							final com.minimine.mundo.chunks.Chunk chunk = new com.minimine.mundo.chunks.Chunk();
							chunk.x = cx;
							chunk.z = cz;
							chunk.chave = chave;
							chunk.meta = new short[Mundo.TAM_CHUNK * Mundo.Y_CHUNK * Mundo.TAM_CHUNK];
							com.minimine.mundo.chunks.ChunkProcesso.util.compactar(
								com.minimine.mundo.chunks.ChunkProcesso.util.bitsPraMaxId(chunk.maxIds), chunk);
							int total = dis.readInt();
							for(int k = 0; k < total; k++) {
								int x = dis.readInt();
								int y = dis.readInt();
								int z = dis.readInt();
								String bid = dis.readUTF();
								com.minimine.mundo.chunks.ChunkProcesso.util.defBloco(x, y, z, bid, chunk);
							}
							int metaTam = Mundo.TAM_CHUNK * Mundo.Y_CHUNK * Mundo.TAM_CHUNK;
							for(int i = 0; i < metaTam; i++) chunk.meta[i] = dis.readShort();
							chunk.dadosProntos = true;
							chunk.att = true;
							Mundo.chunksMod.put(chave, chunk);
						} catch(Exception e) {
							Gdx.app.error("[Jogo]", "Erro ao carregar chunk da rede: " + e.getMessage());
						}
					}
				}).start();
        } else if(msg.startsWith("CHUNKS_FIM:")) {
            Gdx.app.log("[Jogo]", "Todos os chunks recebidos do servidor");
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
                float yaw = com.badlogic.gdx.math.MathUtils.atan2(
                    jg.camera.direction.x, jg.camera.direction.z
                ) * com.badlogic.gdx.math.MathUtils.radiansToDegrees;
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
        ArquivosUtil.svMundo(mundo, jogadores);
        relogio.cancel();
        render.liberar();
        Bloco.liberar();
        jogadoresRede.clear();
        if(net != null) {
            net.liberar();
            net = null;
        }
    }

    @Override
    public void resize(int v, int h) {
        ArquivosUtil.svMundo(mundo, jogadores);
        render.ui.ajustar(v, h);
        LuaAPI.ajustar(v, h);
    }

    @Override
    public void hide() {
        ArquivosUtil.svMundo(mundo, jogadores);
        dispose();
    }
    @Override
    public void pause() {
        ArquivosUtil.svMundo(mundo, jogadores);
    }
    @Override public void resume() {}
}
