package com.minimine.entidades;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.utils.AnimationController;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.minimine.graficos.Modelos;
import com.minimine.mundo.Mundo;
import java.util.LinkedHashMap;
import java.util.Map;
import com.minimine.mundo.blocos.Bloco;

// tudo é dirigido pelo InterpretadorCriatura lendo as regras do DadosCriatura
public class Criatura extends Entidade {
    public final DadosCriatura dados;

    // variáveis internas do JSON(sede, fome, etc)
    public final Map<String, Float> variaveis = new LinkedHashMap<>();
    public final Map<String, float[]> faixasVariaveis = new LinkedHashMap<>(); // {min, max}

    // IA
    public final IA ia;
    public float recompensaPendente = 0f;
    public float cronometroIA = 0f;
    public static final float INTERVALO_IA = 0.1f;

    // destino de busca(definido por procurarBloco)
    public Vector3 destino = null;
    public boolean temDestino = false;

    // modelo e animação
    public ModelInstance instancia;
    public AnimationController animCtr;
    public String animAtual = "";

    public Vector3 direcaoSuave = new Vector3();

    public Criatura(DadosCriatura dados, float x, float y, float z) {
        super();
        this.dados = dados;

        this.largura = dados.largura;
        this.altura = dados.altura;
        this.profundidade = dados.profundidade;
        this.velo = dados.velo;
        this.peso = dados.peso;
        this.pulo = dados.pulo;
        this.posicao.set(x, y, z);

        // inicializa variáveis com valor e faixa do JSON
        for(Map.Entry<String, float[]> e : dados.variaveis.entrySet()) {
            float[] faixa = e.getValue(); // {valorInicial, min, max}
            variaveis.put(e.getKey(), faixa[0]);
            faixasVariaveis.put(e.getKey(), new float[]{faixa[1], faixa[2]});
        }
        ia = new IA();

        try {
            instancia = new ModelInstance(Modelos.obterModelo(dados.modelo));
            animCtr   = new AnimationController(instancia);
        } catch(Exception e) {
            Gdx.app.error("[Criatura]", "Erro no modelo " + dados.modelo + ": " + e.getMessage());
        }
    }

    @Override
    public void att(float delta) {
        // 1. avalia todas as regras do JSON
        InterpretadorCriatura.avaliar(this, delta);

        // 2. move em direção ao destino se tiver um
        if(temDestino && destino != null) moverParaDestino();

        // 3. tick de decisão da IA
        cronometroIA -= delta;
        if(cronometroIA <= 0f) {
            ia.aprender(recompensaPendente);
            recompensaPendente = 0f;
            float[] acao = ia.pensar(montarObservacao());
            aplicarAcaoIA(acao);
            cronometroIA = INTERVALO_IA;
        }
        // 4. física
        super.att(delta);
        processarColisao(delta);

        // 5. animação
        if(animCtr != null) animCtr.update(delta);

        // 6. suaviza direção visual
        float fs = naAgua ? 0.08f : 0.15f;
        direcaoSuave.x = MathUtils.lerp(direcaoSuave.x, frenteV.x, fs);
        direcaoSuave.z = MathUtils.lerp(direcaoSuave.z, frenteV.z, fs);
    }

    public float[] montarObservacao() {
        float[] obs = new float[IA.ENTRADAS];
        obs[0] = naAgua ? 1f : 0f;
        obs[1] = 0f;
        obs[2] = 0f;
        obs[3] = 0f;
        obs[4] = noChao ? 1f : 0f;
        obs[5] = MathUtils.clamp(velocidade.y / 10f, -1f, 1f);
        if(temDestino && destino != null) {
            obs[1] = MathUtils.clamp((destino.x - posicao.x) / 20f, -1f, 1f);
            obs[2] = MathUtils.clamp((destino.z - posicao.z) / 20f, -1f, 1f);
        }
        return obs;
    }

    public void aplicarAcaoIA(float[] acao) {
        frenteV.x = acao[0];
        frenteV.z = acao[1];
        if(frenteV.len2() > 0.001f) frenteV.nor();
        direitaV.x =  frenteV.z;
        direitaV.z = -frenteV.x;

        frente = (acao[0] != 0 || acao[1] != 0);
        tras = false; esquerda = false; direita = false;
        cima = acao[2] > 0.5f && noChao && !naAgua;
        baixo = acao[3] < -0.5f && naAgua;
    }

    public void moverParaDestino() {
        float dx = destino.x - posicao.x;
        float dz = destino.z - posicao.z;
        float dist2 = dx * dx + dz * dz;
        if(dist2 < 1f) { temDestino = false; return; }
        float len = (float)Math.sqrt(dist2);
        frenteV.x = dx / len;
        frenteV.z = dz / len;
        direitaV.x =  frenteV.z;
        direitaV.z = -frenteV.x;
        frente = true;
    }

    // chamado pelo InterpretadorMob quando uma regra tem "procurarBloco"
    public void definirDestino(String[] blocoAlvo) {
        int raio = 20;
        float melhorDist = Float.MAX_VALUE;
        for(int dx = -raio; dx <= raio; dx++) {
            for(int dz = -raio; dz <= raio; dz++) {
                int bx = (int)posicao.x + dx;
                int by = (int)posicao.y;
                int bz = (int)posicao.z + dz;
                for(int dy = -1; dy <= 1; dy++) {
                    int id = Mundo.obterBlocoMundo(bx, by + dy, bz);
                    if(id == 0) continue;
                    Bloco b = Bloco.numIds.get(id);
                    if(b == null) continue;
                    for(String alvo : blocoAlvo) {
                        if(b.nome.equals(alvo)) {
                            float dist = dx * dx + dz * dz;
                            if(dist < melhorDist) {
                                melhorDist = dist;
                                destino = new Vector3(bx, by + dy, bz);
                                temDestino = true;
                            }
                        }
                    }
                }
            }
        }
    }

    public void processarColisao(float delta) {
        if(naAgua) {
            velocidade.y  = MathUtils.lerp(velocidade.y, baixo ? -3f : 0.5f, 0.12f);
            velocidade.x *= 0.88f;
            velocidade.z *= 0.88f;
        }
        float dx = velocidade.x * delta;
        float dy = velocidade.y * delta;
        float dz = velocidade.z * delta;

        posicao.y += dy;
		attHitbox();
        if(colideMundo()) {
			posicao.y -= dy;
			attHitbox();
			noChao = dy < 0;
			velocidade.y = 0;
		} else {
			noChao = ehChao();
		}
        posicao.x += dx;
		attHitbox();
        if(colideMundo()) {
			posicao.x -= dx;
			velocidade.x = 0;
			attHitbox();
		}
        posicao.z += dz; attHitbox();
        if(colideMundo()) {
			posicao.z -= dz;
			velocidade.z = 0;
			attHitbox();
		}
        if(posicao.y < 0) posicao.y = 200;
    }

    @Override
    public void render(ModelBatch mb) {
        if(instancia == null) return;
        if(direcaoSuave.len2() > 0.01f) {
            instancia.transform.setToRotation(
                Vector3.Y,
                (float)Math.toDegrees(Math.atan2(-direcaoSuave.x, -direcaoSuave.z))
            );
        }
        instancia.transform.setTranslation(posicao);
        mb.render(instancia);
    }
}


