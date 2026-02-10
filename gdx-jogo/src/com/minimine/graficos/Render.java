package com.minimine.graficos;

import com.minimine.ui.UI;
import com.minimine.entidades.Jogador;
import com.minimine.mundo.Mundo;
import com.minimine.mundo.Chave;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.minimine.utils.DiaNoiteUtil;
import com.minimine.mundo.Chunk;
import com.minimine.utils.NuvensUtil;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.Texture;
import com.minimine.utils.CorposCelestes;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class Render {
	public UI ui;
	public Mundo mundo;

	public static ShaderProgram shader;

	public static int maxFaces = Mundo.TAM_CHUNK * Mundo.Y_CHUNK * Mundo.TAM_CHUNK * 6 / 6;
    public static int maxVerts = maxFaces * 4;
    public static int maxIndices = maxFaces * 6;
    public static final VertexAttribute[] atriburs = new VertexAttribute[] {
        new VertexAttribute(VertexAttributes.Usage.Position, 3, "a_pos"),
        new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, "a_texCoord"),
        new VertexAttribute(VertexAttributes.Usage.Generic, 4, "a_atlasCoords"),
        new VertexAttribute(VertexAttributes.Usage.ColorPacked, 4, "a_cor")
    };

	public static String vert = 
    "attribute vec3 a_pos;\n" +
    "attribute vec2 a_texCoord;\n" +
    "attribute vec4 a_atlasCoords;\n" + // limites do atlas(uMin, vMin, uMax, vMax)
    "attribute vec4 a_cor;\n" +
    "uniform mat4 u_projPos;\n" +
    "varying vec2 v_texCoord;\n" +
    "varying vec4 v_atlasCoords;\n" +
    "varying vec4 v_cor;\n" +
    "void main() {\n" +
	"   v_texCoord = a_texCoord;\n" +
    "   v_atlasCoords = a_atlasCoords;\n" +
	"   v_cor = a_cor;\n" +
	"   gl_Position = u_projPos * vec4(a_pos, 1.0);\n" +
    "}";

	public static String frag =
    "#ifdef GL_ES\n" +
    "precision mediump float;\n" +
    "#endif\n" +
    "varying vec2 v_texCoord;\n" +
    "varying vec4 v_atlasCoords;\n" + 
    "varying vec4 v_cor;\n" + 
    "uniform sampler2D u_textura;\n" +
    "uniform float u_luzCeu;\n" + 
    "void main() {\n" +
    // v_cor.r = Luz da Tocha (0.0 a 1.0)\n" +
    // v_cor.g = Luz do Sol (0.0 a 1.0)\n" +
    // v_cor.b = multiplicador de face(falso AO pra profundidade)
	"   float solDinamico = v_cor.g * u_luzCeu;\n" + 
	"   float brilhoBruto = max(v_cor.r, solDinamico);\n" +
	"   float iluminacaoFinal = brilhoBruto * v_cor.b;\n" +
    // Calculo de tiling dentro do atlas
    "   vec2 uvTam = v_atlasCoords.zw - v_atlasCoords.xy;\n" + // tamanho da regiao no atlas
    "   vec2 localUV = fract(v_texCoord);\n" + // repete 0..1
    "   vec2 finalUV = v_atlasCoords.xy + localUV * uvTam;\n" +

	"   vec4 texCor = texture2D(u_textura, finalUV);\n" +
    "   if(texCor.a < 0.5) discard;\n" +
	// neblina baseada na distancia
	"   float dist = length(gl_FragCoord.z / gl_FragCoord.w);\n" +
	"   float inicio = 16.0;\n" + 
	"   float fim = 64.0;\n" + 
	"   float fator = clamp((dist - inicio) / (fim - inicio), 0.0, 1.0);\n" +
	// cor da neblina mudando com o céu
	"   vec3 corNevoa = vec3(0.4, 0.6, 0.9) * u_luzCeu;\n" + 
	"   gl_FragColor = vec4(mix(texCor.rgb * iluminacaoFinal, corNevoa, fator), texCor.a);\n" +
	"}";

	public Render(Jogador jogador, Mundo mundo) {
		this.ui = new UI(jogador);
		this.mundo = mundo;

		Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());  
		Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
		Gdx.gl.glCullFace(GL20.GL_BACK);
		Gdx.gl.glEnable(GL20.GL_BLEND);
		Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

		shader = new ShaderProgram(vert, frag);
        if(!shader.isCompiled()) Gdx.app.log("shader", "[ERRO]: "+shader.getLog());

		// Atualizado: usa nome da textura ao invés de ID
		TextureRegion[] framesAgua = {
			Texturas.atlas.get("agua"),
			Texturas.atlas.get("agua_a1"),
			Texturas.atlas.get("agua_a2")
		};
		Animacoes2D.add("agua", framesAgua, 3f); // 3 fps

		EmissorParticulas.iniciar();

        ShaderProgram.pedantic = false;

        if(mundo.nuvens && NuvensUtil.primeiraVez) NuvensUtil.iniciar();
        if(mundo.ciclo) CorposCelestes.iniciar();
	}

	public static com.badlogic.gdx.graphics.glutils.ShapeRenderer debugCaixas;

	public void att(float delta) {
		float fator = DiaNoiteUtil.obterFatorTransicao();
		float[] corNoite = {0.05f, 0.05f, 0.15f};
		float[] corDia = {0.5f * DiaNoiteUtil.luz, 0.7f * DiaNoiteUtil.luz, 1.0f * DiaNoiteUtil.luz};

		float r = corNoite[0] * (1f - fator) + corDia[0] * fator;
		float g = corNoite[1] * (1f - fator) + corDia[1] * fator;
		float b = corNoite[2] * (1f - fator) + corDia[2] * fator;

		Gdx.gl.glClearColor(r, g, b, 1f);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
		Gdx.gl.glEnable(GL20.GL_CULL_FACE);

		if(mundo.nuvens) NuvensUtil.att(delta, ui.jg.posicao);

        shader.begin();

        shader.setUniformMatrix("u_projPos", ui.jg.camera.combined);

        shader.setUniformf("u_luzCeu", DiaNoiteUtil.luz); 
		shader.setUniformf("u_alturaSol", DiaNoiteUtil.obterFatorTransicao());

		DiaNoiteUtil.aplicarShader(shader);

		Texturas.blocos.bind(0);
        shader.setUniformi("u_textura", 0);
		Gdx.gl.glDisable(GL20.GL_BLEND);

		// 1. solidos:
        for(final Chunk chunk : mundo.chunks.values()) {
			if(mundo.frustrum(chunk, ui.jg) && chunk.malha != null && chunk.contaSolida > 0) {
				// renderiza apenas do indice 0 até o final dos solidos
				chunk.malha.render(shader, GL20.GL_TRIANGLES, 0, chunk.contaSolida);
			}
		}
		// 2. transparentes:
		Gdx.gl.glEnable(GL20.GL_BLEND);

		for(final Chunk chunk : mundo.chunks.values()) {
			if(mundo.frustrum(chunk, ui.jg) && chunk.malha != null && chunk.contaTransp > 0) {
				// renderiza começando de onde o solido parou
				chunk.malha.render(shader, GL20.GL_TRIANGLES, chunk.contaSolida, chunk.contaTransp);
			}
		}
		Animacoes2D.att(delta);
		EmissorParticulas.att(shader, delta, ui.jg);

		shader.end();
        if(mundo.nuvens) NuvensUtil.att(ui.jg.camera.combined);

		mundo.att(delta, ui.jg);

		if(mundo.carregado) {
			if(!ui.jg.nasceu) {
				// tenta encontrar o chão, se o obterBlocoMundo retornar algo diferente de 0, 
				// significa que os dados daquela parte do mapa ja chegaram
				int yTeste = Mundo.obterAlturaChao((int)ui.jg.posicao.x, (int)ui.jg.posicao.z);
				if(yTeste > 1) { // se encontrou algo acima do fundo do mundo
					ui.jg.posicao.y = yTeste;
					ui.jg.nasceu = true;
					Gdx.app.log("[Jogo]", "jogador nasceu a "+yTeste+" blocos de altura");
				} else Gdx.app.log("[Jogo]", "não nasceu, altura recebida: "+yTeste);
			}
			ui.jg.att(delta);
		}
		Gdx.gl.glDisable(GL20.GL_CULL_FACE);

		ui.att(delta, mundo);

        // DEBUG DE COLISAO
        if(Mundo.debugColisao) {
            if(debugCaixas == null) debugCaixas = new com.badlogic.gdx.graphics.glutils.ShapeRenderer();
            debugCaixas.setProjectionMatrix(ui.jg.camera.combined);
            debugCaixas.begin(com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType.Line);

            // blocos proximos(O Guloso)
            debugCaixas.setColor(1, 0, 0, 1);
            int px = (int)ui.jg.posicao.x;
            int py = (int)ui.jg.posicao.y;
            int pz = (int)ui.jg.posicao.z;

            // itera chunks ao redor pra desenhar as caixas de debug
            int chunkX = px >> 4;
            int chunkZ = pz >> 4;

            for(int cx = chunkX - 1; cx <= chunkX + 1; cx++) {
                for(int cz = chunkZ - 1; cz <= chunkZ + 1; cz++) {
                    long ch = Chave.calcularChave(cx, cz);
                    Chunk c = mundo.chunks.get(ch);
                    if(c != null && c.debugRects != null) {
                        float offX = c.x << 4;
                        float offZ = c.z << 4;

                        synchronized(c.debugRects) {
                            for(com.badlogic.gdx.math.collision.BoundingBox bb : c.debugRects) {
                                // verifica distancia simples pra nao desenhar tudo
                                float globalX = offX + bb.min.x;
                                float globalY = bb.min.y;
                                float globalZ = offZ + bb.min.z;

                                if(Math.abs(globalX - px) > 20 || Math.abs(globalY - py) > 20 || Math.abs(globalZ - pz) > 20) continue;

                                float w = bb.max.x - bb.min.x;
                                float h = bb.max.y - bb.min.y;
                                float d = bb.max.z - bb.min.z;
                                debugCaixas.box(globalX, globalY, globalZ, w, h, d);
                            }
                        }
                    }
                }
            }

            // jogador(caixa simples de exemplo)
            debugCaixas.setColor(0, 1, 0, 1);
            float jw = 0.6f;
            float jh = 1.8f;
            debugCaixas.box(ui.jg.posicao.x - jw/2, ui.jg.posicao.y, ui.jg.posicao.z + jw/2, jw, jh, jw);

            debugCaixas.end();
        }
	}

	public void liberar() {
		shader.dispose();
		ui.liberar();
		mundo.liberar();
	}
}

