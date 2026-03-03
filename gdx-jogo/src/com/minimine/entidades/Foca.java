package com.minimine.entidades;

import com.badlogic.gdx.math.MathUtils;
import com.minimine.mundo.Mundo;
import com.minimine.utils.Mat;
import com.minimine.mundo.blocos.Bloco;
import net.mgsx.gltf.loaders.gltf.GLTFLoader;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import net.mgsx.gltf.scene3d.scene.SceneAsset;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.utils.AnimationController;
import com.badlogic.gdx.math.Vector3;
import com.minimine.graficos.Modelos;
/*
 * foca com IA de aprendizado por reforço
 
 * estados internos:
 *   VAGANDO: anda aleatoriamente(baixa necessidade de água)
 *   SEDENTA: procura água ativamente
 *   NADANDO: dentro da água, satisfeita
 *   PRESA: colidiu várias vezes seguidas; muta a rede e tenta nova direção
 
 * a rede neural recebe observações do ambiente a cada tick de decisão
 * e devolve um vetor de ação, após executar a ação, uma recompensa é calculada
 * e retroalimentada pra rede aprender
 */
public class Foca extends Entidade {
    // estados da maquina de estados finita
    public enum Estado { VAGANDO, SEDENTA, NADANDO, PRESA }
    public Estado estado = Estado.VAGANDO;
    public IA ia;
    // temporizadores e contadores
    public float cronometroDecisao = 0f; // quando chega a 0, toma nova decisão
    public float tempoForaDaAgua = 0f; // acumula tempo fora da água
    public float tempoNaAgua = 0f; // acumula tempo dentro da água
    public float tempoPresoTotal = 0f; // pra detectar travamento

    public int colisoesSeguidas = 0; // detecta quando ta presa em canto
    public static final int LIMITE_COLISOES = 6;

    // necessidade e memória de água
    public float necessidadeAgua = 0f; // 0 = hidratada, >100 = critica
    public boolean estavaNaAgua = false; // quadro anterior

    // ultima posição de água encontrada no escaner(pode ser(-1,-1) se não encontrou)
    public float aguaRelX = 0f;
    public float aguaRelZ = 0f;
    public boolean aguaConhecida = false;

    // recompensa acumulada no passo atual(calculada ao longo do quadro, aplicada
    // quando a IA tomar a proxima decisão)
    public float recompensaPendente  = 0f;
	
    // direção corrente calculada pela IA
    public Vector3 direcao = new Vector3();
	public Vector3 direcaoSuave = new Vector3();

    public static SceneAsset ativoCena;
    public ModelInstance instancia;
    public AnimationController animCtr;
	
    // constantes de recompensa
    public static final float REC_ENTROU_NA_AGUA = 15f;
    public static final float REC_NADANDO_SEDENTA = 1f; // por tick na água com sede
    public static final float REC_VAGANDO_NA_AGUA = 0.2f;
    public static final float PEN_FORA_AGUA_SEDENTA = -0.5f; // por tick fora com sede
    public static final float PEN_SAIU_DA_AGUA = -0.5f;
    public static final float PEN_COLISAO = -1f;
    public static final float REC_SOBREVIVEU = 0.05f; // recompensa base por existir

    public Foca(float x, float y, float z) {
        super();
		vida = 20;
		vidaMax = 20;
		bioma = "Tundra";
		
        this.posicao.set(x, y, z);
        this.largura = 0.8f;
        this.altura = 0.6f;
        this.profundidade = 1.2f;
        this.velo = 5f;

        ia = new IA();

        try {
            instancia = new ModelInstance(Modelos.obterModelo("modelos/foca.gltf"));
            animCtr = new AnimationController(instancia);
            animCtr.setAnimation("nadando", -1);
        } catch(Exception e) {
            Gdx.app.error("[Foca]", "Erro no GLTF: " + e.getMessage());
        }
    }

    @Override
    public void att(float delta) {
        super.att(delta);
        if(animCtr != null) animCtr.update(delta);

        // === 1. acumula tempo e necessidades continuas ===
        attNecessidades(delta);

        // === 2. recompensas continuas(a cada frame) ===
        acumularRecompensas();

        // === 3. toma decisão quando o cronometro chega a zero ===
        cronometroDecisao -= delta;
        if(cronometroDecisao <= 0f) tomarDecisao();
        
        // === 4. aplica a direção escolhida nos vetores de movimento ===
        aplicarMovimento();

        // === 5. fisica e colisão(identico ao original) ===
        processarFisicaColisao(delta);

        // === 6. atualiza animação conforme estado ===
        attAnimacao();

        // === 7. salva estado do frame para o proximo ===
        estavaNaAgua = naAgua;
    }
	
    // necessidades
    public void attNecessidades(float delta) {
        if(naAgua) {
            tempoNaAgua += delta;
            tempoForaDaAgua = 0f;
            necessidadeAgua = Math.max(0f, necessidadeAgua - 10f * delta);
        } else {
            tempoForaDaAgua += delta;
            tempoNaAgua = 0f;
            necessidadeAgua += 4f * delta; // cresce 4 unidades/segundo fora da água
        }
        necessidadeAgua = Math.min(necessidadeAgua, 150f);
    }

    // recompensas contínuas (acumuladas frame a frame, entregues na decisão)
    public void acumularRecompensas() {
        recompensaPendente += REC_SOBREVIVEU;

        if(naAgua) {
            if(necessidadeAgua > 20f) recompensaPendente += REC_NADANDO_SEDENTA;
            else recompensaPendente += REC_VAGANDO_NA_AGUA;
        } else {
            if(necessidadeAgua > 20f) recompensaPendente += PEN_FORA_AGUA_SEDENTA;
        }
        // eventos pontuais baseados em transição de estado água
        if(naAgua && !estavaNaAgua && necessidadeAgua > 20f) {
            recompensaPendente += REC_ENTROU_NA_AGUA;
		}
        if(!naAgua && estavaNaAgua) {
            recompensaPendente += PEN_SAIU_DA_AGUA;
		}
    }
	
    // tomada de decisão via rede neural
    public void tomarDecisao() {
        // entrega recompensa acumulada a rede antes de decidir
        ia.aprender(recompensaPendente);
        recompensaPendente = 0f;

        // verifica se ta presa
        attEstado();

        // monta vetor de observação
        float[] obs = montarObservacao();

        // consulta a rede
        float[] acao = ia.pensar(obs);

        // interpreta saidas
        //  acao[0] = moveX(-1 a 1)
        //  acao[1] = moveZ(-1 a 1)
        //  acao[2] = pular(>0.5 = pula)
        //  acao[3] = mergulhar(<-0.5 = submerge; so na água)
        direcao.x = acao[0];
        direcao.z = acao[1];

        // pulo: so fora da água, quando não ta ja no ar
        cima  = (acao[2] > 0.5f) && noChao && !naAgua;
        baixo = (acao[3] < -0.5f) && naAgua;

        // ajusta cronometro de decisão com base no estado
        switch(estado) {
            case SEDENTA:
                cronometroDecisao = MathUtils.random(0.3f, 0.8f); // decide rapido
            break;
            case PRESA:
                cronometroDecisao = 0.2f; // decide muito rapido pra escapar
            break;
            default:
                cronometroDecisao = MathUtils.random(0.5f, 2.0f);
			break;
        }
    }
    
    // maquina de estados
    public void attEstado() {
        if(colisoesSeguidas >= LIMITE_COLISOES) {
            estado = Estado.PRESA;
            tempoPresoTotal += 1f;
            // muta a rede para sair do local minimo
            ia.mutar(0.3f + Math.min(tempoPresoTotal * 0.05f, 0.8f));
            colisoesSeguidas = 0;
        } else if(naAgua) {
            estado = Estado.NADANDO;
            tempoPresoTotal = 0f;
        } else if(necessidadeAgua > 20f) {
            estado = Estado.SEDENTA;
            tempoPresoTotal = 0f;
        } else {
            estado = Estado.VAGANDO;
            tempoPresoTotal = 0f;
        }
    }
    
    // observação do ambiente
    public float[] montarObservacao() {
        // atualiza mapa de água mais proxima
        escanearAgua(estado == Estado.SEDENTA || estado == Estado.PRESA ? 20 : 8);

        float[] obs = new float[6];
        obs[0] = naAgua ? 1f : 0f;
        obs[1] = Math.min(necessidadeAgua / 100f, 1f);
        obs[2] = aguaConhecida ? MathUtils.clamp(aguaRelX / 20f, -1f, 1f) : 0f;
        obs[3] = aguaConhecida ? MathUtils.clamp(aguaRelZ / 20f, -1f, 1f) : 0f;
        obs[4] = noChao ? 1f : 0f;
        obs[5] = MathUtils.clamp(velocidade.y / 10f, -1f, 1f);
        return obs;
    }

    // escaneamento de água ao redor
    public void escanearAgua(int raio) {
        float melhorDist = Float.MAX_VALUE;
        aguaConhecida = false;

        for(int dx = -raio; dx <= raio; dx++) {
            for(int dz = -raio; dz <= raio; dz++) {
                int bx = (int) posicao.x + dx;
                int by = (int) posicao.y;
                int bz = (int) posicao.z + dz;

                // checa a camada atual e uma abaixo
                for(int dy = -1; dy <= 0; dy++) {
                    Bloco bloco = Bloco.numIds.get(Mundo.obterBlocoMundo(bx, by + dy, bz));
                    if(bloco != null && !bloco.solido && bloco.nome.equals("agua")) {
                        float dist = dx * dx + dz * dz;
                        if(dist < melhorDist) {
                            melhorDist = dist;
                            aguaRelX = dx;
                            aguaRelZ = dz;
                            aguaConhecida = true;
                        }
                    }
                }
            }
        }
    }
	
    // aplica a direção calculada aos vetores de frente/direita
    public void aplicarMovimento() {
		frente = (direcao.x != 0 || direcao.z != 0);
		tras = false; esquerda = false; direita = false;

		// suaviza direção para movimento mais fluido(lerp entre direção atual e nova)
		float fatorSuavizacao = naAgua ? 0.08f : 0.15f; // mais lento na água
		direcaoSuave.x = MathUtils.lerp(direcaoSuave.x, direcao.x, fatorSuavizacao);
		direcaoSuave.z = MathUtils.lerp(direcaoSuave.z, direcao.z, fatorSuavizacao);

		frenteV.x = direcaoSuave.x;
		frenteV.z = direcaoSuave.z;
		if(frenteV.len2() > 0.001f) frenteV.nor();

		direitaV.x =  frenteV.z;
		direitaV.z = -frenteV.x;
	}
    
    // fisica e colisão
    public void processarFisicaColisao(float delta) {
        // gravidade na água
        if(naAgua) {
			velocidade.y = MathUtils.lerp(velocidade.y, baixo ? -3f : 0.5f, 0.12f);
			// resistencia lateral da água(movimento horizontal mais arrastado)
			velocidade.x *= 0.88f;
			velocidade.z *= 0.88f;
		}
        
        float dx = velocidade.x * delta;
        float dy = velocidade.y * delta;
        float dz = velocidade.z * delta;

        boolean colidiu = false;

        // vertical
        posicao.y += dy;
        attHitbox();
        if(colideMundo()) {
            posicao.y -= dy;
            attHitbox();
            noChao = (dy < 0);
            velocidade.y = 0;
            colidiu = true;
        } else {
            noChao = ehChao();
        }
        // horizontal X
        if(agachado && noChao && dx != 0 && !temSuporte(posicao.x + dx, posicao.z)) dx = 0;
        posicao.x += dx;
        attHitbox();
        if(colideMundo()) {
            posicao.x -= dx;
            velocidade.x = 0;
            attHitbox();
            colidiu = true;
        }
        // horizontal Z
        if(agachado && noChao && dz != 0 && !temSuporte(posicao.x, posicao.z + dz)) dz = 0;
        posicao.z += dz;
        attHitbox();
        if(colideMundo()) {
            posicao.z -= dz;
            velocidade.z = 0;
            attHitbox();
            colidiu = true;
        }
        // contagem de colisões pra detectar travamento
        if(colidiu) {
            colisoesSeguidas++;
            recompensaPendente += PEN_COLISAO;
        } else {
            colisoesSeguidas = Math.max(0, colisoesSeguidas - 1);
        }
        // limite de mundo
        if(posicao.y < 0) posicao.y = 200;
    }
    
    // animações
    public void attAnimacao() {
        if(animCtr == null) return;
        switch(estado) {
            case NADANDO:
                if(direcao.len2() < 0.01f) animCtr.setAnimation("naAgua_parada", -1);
                else animCtr.setAnimation("nadando", -1);
                break;
            case SEDENTA:
            case PRESA:
                animCtr.setAnimation("nadando", -1);
            break;
            default: // VAGANDO
                if(direcao.len2() < 0.01f) animCtr.setAnimation("naAgua_parada", -1);
                else animCtr.setAnimation("nadando", -1);
            break;
        }
    }

    // render e liberação
    @Override
    public void render(ModelBatch mb) {
        if(instancia == null) return;

        // rotaciona o modelo pra direção de movimento
        if(direcaoSuave.len2() > 0.01f) {
            instancia.transform.setToRotation(Vector3.Y, (float)Math.toDegrees(Math.atan2(-direcaoSuave.x, -direcaoSuave.z)));
        }
        instancia.transform.setTranslation(posicao);
        mb.render(instancia);
    }
}

