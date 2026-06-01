package com.minimine.entidades;

import com.badlogic.gdx.math.MathUtils;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.DataInputStream;

/*
 * rede neural com aprendizado por reforço(Policia de gradiente simplificado)
 
 * arquitetura: 6 entradas → 8 neuronios ocultos → 4 saidas
 
 * entradas:
 *   [0] naAgua(0 ou 1)
 *   [1] necessidadeAgua(normalizado 0-1)
 *   [2] direcaoAguaX(normalizado -1 a 1)
 *   [3] direcaoAguaZ(normalizado -1 a 1)
 *   [4] noChao(0 ou 1)
 *   [5] velocidadeY(normalizado)
 
 * saidas:
 *   [0] moveX(direção X do movimento, -1 a 1)
 *   [1] moveZ(direção Z do movimento, -1 a 1)
 *   [2] pular(> 0.5 = pular)
 *   [3] mergulhar(< -0.5 = submergir, so na agua)
 */
public class IA {
    public int ENTRADAS = 6;
    public int OCULTAS = 8;
    public int SAIDAS = 4;

    // pesos
    public float[][] pesosEntrada; // [6][8]
    public float[][] pesosOculta; // [8][4]
    public float[] viesOculta; // [8]
    public float[] viesSaida; // [4]

    // memoria da ultima passagem(necessaria pra retropropagação leve)
    public float[] ultimasEntradas;
    public float[] ultimaOculta; // valores pós-ativação
    public float[] ultimasSaidas;

    // hiperparametros
    public static final float TAXA_APRENDIZADO = 0.01f;
    public static final float DECAIMENTO = 0.999f; // reduz taxa com o tempo
    public float taxaAtual = TAXA_APRENDIZADO;

    // acumulador de recompensas pra estabilizar o aprendizado
    public float recompensaMedia  = 0f;
    public static final float SUAVIZACAO = 0.05f; // EMA

    public IA(int numVariaveis) {
		this.ENTRADAS = 3 + numVariaveis;
        this.OCULTAS  = 8 + numVariaveis / 2;
		
        pesosEntrada = new float[ENTRADAS][OCULTAS];
        pesosOculta = new float[OCULTAS][SAIDAS];
        viesOculta = new float[OCULTAS];
        viesSaida = new float[SAIDAS];

        ultimasEntradas = new float[ENTRADAS];
        ultimaOculta = new float[OCULTAS];
        ultimasSaidas = new float[SAIDAS];

        iniciar();
    }
    // inicialização de He(boa para tanh): N(0, sqrt(2/n))
    public void iniciar() {
        float escalaE = (float)Math.sqrt(2.0 / ENTRADAS);
        float escalaO = (float)Math.sqrt(2.0 / OCULTAS);

        for(int i = 0; i < ENTRADAS; i++) {
            for(int j = 0; j < OCULTAS; j++) {
                pesosEntrada[i][j] = MathUtils.random(-escalaE, escalaE);
			}
		}
        for(int i = 0; i < OCULTAS; i++) {
            viesOculta[i] = 0f;
            for(int j = 0; j < SAIDAS; j++) {
                pesosOculta[i][j] = MathUtils.random(-escalaO, escalaO);
			}
        }
        for(int i = 0; i < SAIDAS; i++) viesSaida[i] = 0f;
    }
	
    // ativação e sua derivada
    public float tanh(float v) {
        return (float)Math.tanh(v);
    }
    // derivada de tanh apartir do valor ja ativado: 1 - tanh²(x)
    public float dtanh(float ativado) {
        return 1f - ativado * ativado;
    }

    // passagem pra frente, salva ativações pra aprendizado posterior
    public float[] pensar(float[] entradas) {
        System.arraycopy(entradas, 0, ultimasEntradas, 0, ENTRADAS);

        // entrada -> Camada Oculta
        for(int j = 0; j < OCULTAS; j++) {
            float soma = viesOculta[j];
            for(int i = 0; i < ENTRADAS; i++) {
                soma += entradas[i] * pesosEntrada[i][j];
			}
            ultimaOculta[j] = tanh(soma);
        }
        // camada Oculta -> Saída
        for(int j = 0; j < SAIDAS; j++) {
            float soma = viesSaida[j];
            for(int i = 0; i < OCULTAS; i++) {
                soma += ultimaOculta[i] * pesosOculta[i][j];
			}
            ultimasSaidas[j] = tanh(soma);
        }
        return ultimasSaidas.clone();
    }
    // aprendizado por reforço, policia de gradiente com linha de base
    public void aprender(float recompensa) {
        // linha de base = media exponencial das recompensas
        recompensaMedia = (1 - SUAVIZACAO) * recompensaMedia + SUAVIZACAO * recompensa;
        float vantagem = recompensa - recompensaMedia;

        // decaimento da taxa de aprendizado
        taxaAtual *= DECAIMENTO;
        float alfa = Math.max(taxaAtual, 1e-5f); // piso minimo

        // === radiente na camada de saida ===
        float[] gradSaida = new float[SAIDAS];
        for(int j = 0; j < SAIDAS; j++) {
            gradSaida[j] = vantagem * dtanh(ultimasSaidas[j]);
            viesSaida[j] += alfa * gradSaida[j];
            for(int i = 0; i < OCULTAS; i++) {
                pesosOculta[i][j] += alfa * gradSaida[j] * ultimaOculta[i];
			}
        }
        // === retropropaga para camada oculta ===
        float[] gradOculta = new float[OCULTAS];
        for(int i = 0; i < OCULTAS; i++) {
            float soma = 0f;
            for(int j = 0; j < SAIDAS; j++) {
                soma += gradSaida[j] * pesosOculta[i][j];
			}
            gradOculta[i] = soma * dtanh(ultimaOculta[i]);
        }
        // === gradiente na camada de entrada ===
        for(int i = 0; i < OCULTAS; i++) {
            viesOculta[i] += alfa * gradOculta[i];
            for(int k = 0; k < ENTRADAS; k++) {
                pesosEntrada[k][i] += alfa * gradOculta[i] * ultimasEntradas[k];
			}
        }
    }
	
	public static void salvarPesos(DataOutputStream dos, IA ia) throws IOException {
        dos.writeInt(ia.ENTRADAS);
        dos.writeInt(ia.OCULTAS);
        dos.writeInt(ia.SAIDAS);
        for(int i = 0; i < ia.ENTRADAS; i++) {
            for(int j = 0; j < ia.OCULTAS; j++) {
                dos.writeFloat(ia.pesosEntrada[i][j]);
			}
		}
        for(int i = 0; i < ia.OCULTAS; i++) {
            for(int j = 0; j < ia.SAIDAS; j++) {
                dos.writeFloat(ia.pesosOculta[i][j]);
			}
		}
        for(int i = 0; i < ia.OCULTAS; i++) dos.writeFloat(ia.viesOculta[i]);
        for(int i = 0; i < ia.SAIDAS; i++) dos.writeFloat(ia.viesSaida[i]);
        dos.writeFloat(ia.taxaAtual);
        dos.writeFloat(ia.recompensaMedia);
    }

    // ia == null: apenas consome os bytes sem aplicar
    public static void carregarPesos(DataInputStream dis, IA ia) throws IOException {
        int entradas = dis.readInt();
        int ocultas = dis.readInt();
        int saidas = dis.readInt();
        boolean aplicar = ia != null && ia.ENTRADAS == entradas && ia.OCULTAS == ocultas && ia.SAIDAS == saidas;
        for(int i = 0; i < entradas; i++)
            for(int j = 0; j < ocultas; j++) {
                float v = dis.readFloat();
                if(aplicar) ia.pesosEntrada[i][j] = v;
            }
        for(int i = 0; i < ocultas; i++)
            for(int j = 0; j < saidas; j++) {
                float v = dis.readFloat();
                if(aplicar) ia.pesosOculta[i][j] = v;
            }
        for(int i = 0; i < ocultas; i++) {
            float v = dis.readFloat();
            if(aplicar) ia.viesOculta[i] = v;
        }
        for(int i = 0; i < saidas; i++) {
            float v = dis.readFloat();
            if(aplicar) ia.viesSaida[i] = v;
        }
        float taxa = dis.readFloat();
        float media = dis.readFloat();
        if(aplicar) {
			ia.taxaAtual = taxa;
			ia.recompensaMedia = media;
		}
    }
}
