package com.minimine.utils;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.Gdx;
import com.minimine.mundo.Mundo;

public class NuvensUtil {
    public static Mesh malha;
    public static ShaderProgram shader;
    public static float tempo = 0f;
    public static float[] nuvensPos; // [x, y, z, tam] pra cada nuvem
    public static final int NUM_NUVENS = 100;
    public static final float RAIO_VISIVEL = 200f;
	public static float ALTURA_NUVENS = 150;
    public static final float VELO = 0.2f;

    public static final int VERT_NUVEM = 8;
    public static final int TOTAL_VERTICES = NUM_NUVENS * VERT_NUVEM;
    public static final int TOTAL_INDICES = NUM_NUVENS * 36;

    public static float[] verticesCache = new float[TOTAL_VERTICES * 3];
    public static short[] indicesCache = new short[TOTAL_INDICES];
	
    public static Vector3 centro= new Vector3();
    
    public static String vert = 
    "attribute vec3 a_pos;\n" +
    "uniform mat4 u_projPos;\n" +
    "void main() {\n" +
    "  gl_Position = u_projPos * vec4(a_pos, 1.0);\n" +
    "}";

    public static String frag =
    "#ifdef GL_ES\n" +
    "precision mediump float;\n" +
    "#endif\n" +
    "void main() {\n" +
    "  vec3 cor = vec3(1.0, 1.0, 1.0);\n" +
    "  gl_FragColor = vec4(cor, 0.8);\n" +
    "}";
	
	public static VertexAttribute atribus = new VertexAttribute(VertexAttributes.Usage.Position, 3, "a_pos");

    public static void gerarNuvem(int idc, Vector3 centro) {
        int base = idc * 4;
        // gera ao redor do centro(360 graus)
        float angulo = (float)Math.random() * 360f;
        float distancia = (float)Math.random() * RAIO_VISIVEL * 0.8f; // 80% do raio

        nuvensPos[base] = centro.x + (float)Math.cos(Math.toRadians(angulo)) * distancia;
        nuvensPos[base + 1] = ALTURA_NUVENS + (float)Math.random() * 20f; // altura
        nuvensPos[base + 2] = centro.z + (float)Math.sin(Math.toRadians(angulo)) * distancia;
        nuvensPos[base + 3] = 6f + (float)Math.random() * 10f; // tamanho
    }

    public static void iniciar(Vector3 pos) {
        nuvensPos = new float[NUM_NUVENS * 4];
        shader = new ShaderProgram(vert, frag);

        for(int i = 0; i < NUM_NUVENS; i++) {
            gerarNuvem(i, pos);
        }

        // aloca a malha
        malha = new Mesh(true, TOTAL_VERTICES, TOTAL_INDICES, atribus);
        
        // gera os indices so uma vez(eles nunca mudam, são sempre cubos)
        preencherIndicesFixos();
        
        attMesh(); 
        centro.set(pos);
    }

    private static void preencherIndicesFixos() {
        int indicesIdc = 0;
        short vertPos = 0;
        for(int i = 0; i < NUM_NUVENS; i++) {
            short baseIdc = vertPos;
            // baixo
            indicesCache[indicesIdc++] = (short)(baseIdc + 2); indicesCache[indicesIdc++] = (short)(baseIdc + 3); indicesCache[indicesIdc++] = (short)(baseIdc + 0);
            indicesCache[indicesIdc++] = (short)(baseIdc + 0); indicesCache[indicesIdc++] = (short)(baseIdc + 1); indicesCache[indicesIdc++] = (short)(baseIdc + 2);
            // topo
            indicesCache[indicesIdc++] = (short)(baseIdc + 4); indicesCache[indicesIdc++] = (short)(baseIdc + 7); indicesCache[indicesIdc++] = (short)(baseIdc + 6);
            indicesCache[indicesIdc++] = (short)(baseIdc + 6); indicesCache[indicesIdc++] = (short)(baseIdc + 5); indicesCache[indicesIdc++] = (short)(baseIdc + 4);
            // lados(frente, direita, tras, esquerda)
            indicesCache[indicesIdc++] = (short)(baseIdc + 0); indicesCache[indicesIdc++] = (short)(baseIdc + 4); indicesCache[indicesIdc++] = (short)(baseIdc + 5);
            indicesCache[indicesIdc++] = (short)(baseIdc + 5); indicesCache[indicesIdc++] = (short)(baseIdc + 1); indicesCache[indicesIdc++] = (short)(baseIdc + 0);
            indicesCache[indicesIdc++] = (short)(baseIdc + 1); indicesCache[indicesIdc++] = (short)(baseIdc + 5); indicesCache[indicesIdc++] = (short)(baseIdc + 6);
            indicesCache[indicesIdc++] = (short)(baseIdc + 6); indicesCache[indicesIdc++] = (short)(baseIdc + 2); indicesCache[indicesIdc++] = (short)(baseIdc + 1);
            indicesCache[indicesIdc++] = (short)(baseIdc + 2); indicesCache[indicesIdc++] = (short)(baseIdc + 6); indicesCache[indicesIdc++] = (short)(baseIdc + 7);
            indicesCache[indicesIdc++] = (short)(baseIdc + 7); indicesCache[indicesIdc++] = (short)(baseIdc + 3); indicesCache[indicesIdc++] = (short)(baseIdc + 2);
            indicesCache[indicesIdc++] = (short)(baseIdc + 3); indicesCache[indicesIdc++] = (short)(baseIdc + 7); indicesCache[indicesIdc++] = (short)(baseIdc + 4);
            indicesCache[indicesIdc++] = (short)(baseIdc + 4); indicesCache[indicesIdc++] = (short)(baseIdc + 0); indicesCache[indicesIdc++] = (short)(baseIdc + 3);
            vertPos += 8;
        }
        malha.setIndices(indicesCache);
    }

    public static void attMesh() {
        int vertIdc = 0;
        for(int i = 0; i < NUM_NUVENS; i++) {
            int base = i * 4;
            float x = nuvensPos[base], y = nuvensPos[base + 1], z = nuvensPos[base + 2], tam = nuvensPos[base + 3];

            verticesCache[vertIdc++] = x;
            verticesCache[vertIdc++] = y;
            verticesCache[vertIdc++] = z;
            verticesCache[vertIdc++] = x + tam;
            verticesCache[vertIdc++] = y;
            verticesCache[vertIdc++] = z;
            verticesCache[vertIdc++] = x + tam;
            verticesCache[vertIdc++] = y;
            verticesCache[vertIdc++] = z + tam;
            verticesCache[vertIdc++] = x;
            verticesCache[vertIdc++] = y;
            verticesCache[vertIdc++] = z + tam;
            verticesCache[vertIdc++] = x;
            verticesCache[vertIdc++] = y + tam * 0.3f;
            verticesCache[vertIdc++] = z;
            verticesCache[vertIdc++] = x + tam;
            verticesCache[vertIdc++] = y + tam * 0.3f;
            verticesCache[vertIdc++] = z;
            verticesCache[vertIdc++] = x + tam;
            verticesCache[vertIdc++] = y + tam * 0.3f;
            verticesCache[vertIdc++] = z + tam;
            verticesCache[vertIdc++] = x;
            verticesCache[vertIdc++] = y + tam * 0.3f;
            verticesCache[vertIdc++] = z + tam;
        }
        // so atualiza os vertices na malha existente
        malha.setVertices(verticesCache);
    }

    public static void att(float delta, Vector3 pos) {
        boolean precisaAtt = false;
        for(int i = 0; i < NUM_NUVENS; i++) {
            int base = i * 4;
            nuvensPos[base] -= VELO * delta * 60f;
            
            float dx = nuvensPos[base] - pos.x;
            float dz = nuvensPos[base + 2] - pos.z;
            if((float)Math.sqrt(dx*dx + dz*dz) > RAIO_VISIVEL) {
                attNuvemEspalhada(i, pos);
                precisaAtt = true;
            }
        }
        attMesh();
    }

    public static void attNuvemEspalhada(int idc, Vector3 posJogador) {
        int base = idc * 4;
		
        float anguloBase = 0f; // começa a direita(0 graus)
        float variacaoAngulo = 120f; // varia de -60 a +60 graus em relação a direita
        float angulo = anguloBase + (float)Math.random() * variacaoAngulo - variacaoAngulo/2;

        float dis = RAIO_VISIVEL * 0.7f + (float)Math.random() * RAIO_VISIVEL * 0.3f; // 70-100% do raio

        nuvensPos[base] = posJogador.x + (float)Math.cos(Math.toRadians(angulo)) * dis;
        nuvensPos[base + 1] = ALTURA_NUVENS + (float)Math.random() * 20f; // altura
        nuvensPos[base + 2] = posJogador.z + (float)Math.sin(Math.toRadians(angulo)) * dis;
        nuvensPos[base + 3] = 6f + (float)Math.random() * 10f; // tamanho
    }

    public static void att(Matrix4 matrizCamera) {
        if(!Mundo.nuvens || malha == null) return;

        shader.begin();
        shader.setUniformMatrix("u_projPos", matrizCamera);
        malha.render(shader, GL20.GL_TRIANGLES);
        shader.end();
    }

    public static void liberar() {
		malha.dispose();
        shader.dispose();
		malha = null;
    }
}