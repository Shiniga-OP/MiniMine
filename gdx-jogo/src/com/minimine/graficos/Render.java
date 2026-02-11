package com.minimine.graficos;

import com.minimine.ui.UI;
import com.minimine.entidades.Jogador;
import com.minimine.mundo.Mundo;
import com.minimine.mundo.Chave;
import com.minimine.mundo.blocos.BlocoModelo; // Importante para pegar os dados do atlas
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

    public static final VertexAttribute[] atriburs = new VertexAttribute[] {
        new VertexAttribute(VertexAttributes.Usage.Position, 3, "a_pos"),
        new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, "a_texCoord"),
        new VertexAttribute(VertexAttributes.Usage.Generic, 1, "a_texId"),
        new VertexAttribute(VertexAttributes.Usage.ColorPacked, 4, "a_cor")
    };

    public static String vert = 
    "attribute vec3 a_pos;\n" +
    "attribute vec2 a_texCoord;\n" +
    "attribute float a_texId;\n" + // recebe o ID(0, 1, 2...)
    "attribute vec4 a_cor;\n" +
    "uniform mat4 u_projPos;\n" +
    "varying vec2 v_texCoord;\n" +
    "varying float v_texId;\n" + // passa pro frag
    "varying vec4 v_cor;\n" +
    "void main() {\n" +
    "   v_texCoord = a_texCoord;\n" +
    "   v_texId = a_texId;\n" +
    "   v_cor = a_cor;\n" +
    "   gl_Position = u_projPos * vec4(a_pos, 1.0);\n" +
    "}";

    public static String frag =
    "#ifdef GL_ES\n" +
    "precision mediump float;\n" +
    "#endif\n" +
    "varying vec2 v_texCoord;\n" +
    "varying float v_texId;\n" +
    "varying vec4 v_cor;\n" +
    "uniform sampler2D u_textura;\n" +
    "uniform float u_luzCeu;\n" +
    // array de vec4 contendo [uMin, vMin, uMax, vMax] pra cada ID
    "uniform vec4 u_atlasRects[256];\n" + 

    "void main() {\n" +
    "   float solDinamico = v_cor.g * u_luzCeu;\n" + 
    "   float brilhoBruto = max(v_cor.r, solDinamico);\n" +
    "   float iluminacaoFinal = brilhoBruto * v_cor.b;\n" +

    // === logica de mapeamento ===
    // 1. pega os limites do atlas baseados no ID do vertice
    "   vec4 limites = u_atlasRects[int(v_texId)];\n" +

    // 2. calcula o tamanho da textura no atlas(uMax - uMin, vMax - vMin)
    "   vec2 tam = limites.zw - limites.xy;\n" +

    // 3. aplica o fract() pra repetir a textura(GULOSO)
    "   vec2 localUV = fract(v_texCoord);\n" +

    // 4. mapeia para a posicao final no atlas
    "   vec2 finalUV = limites.xy + localUV * tam;\n" +

    "   vec4 texCor = texture2D(u_textura, finalUV);\n" +
    "   if(texCor.a < 0.5) discard;\n" +

    "   float dist = length(gl_FragCoord.z / gl_FragCoord.w);\n" +
    "   float inicio = 16.0;\n" + 
    "   float fim = 64.0;\n" + 
    "   float fator = clamp((dist - inicio) / (fim - inicio), 0.0, 1.0);\n" +
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

        TextureRegion[] framesAgua = {
            Texturas.atlas.get("agua"),
            Texturas.atlas.get("agua_a1"),
            Texturas.atlas.get("agua_a2")
        };
        Animacoes2D.add("agua", framesAgua, 3f); 

		ui.jg.criarModelo3D();

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
		Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);

        if(mundo.nuvens) NuvensUtil.att(delta, ui.jg.posicao);

        shader.begin();

        shader.setUniformMatrix("u_projPos", ui.jg.camera.combined);
        shader.setUniformf("u_luzCeu", DiaNoiteUtil.luz); 
        shader.setUniformf("u_alturaSol", DiaNoiteUtil.obterFatorTransicao());

        // == envia dados do atlas pro shader ===
        // envia a tabela de pesquisa uma vez por frame(ou quando mudar)
        // o 4fv envia vetores de 4 floats
        shader.setUniform4fv("u_atlasRects", BlocoModelo.dadosAtlas, 0, 256 * 4); 

        DiaNoiteUtil.aplicarShader(shader);

        Texturas.blocos.bind(0);
        shader.setUniformi("u_textura", 0);
        Gdx.gl.glDisable(GL20.GL_BLEND);

        // 1. solidos:
        for(final Chunk chunk : mundo.chunks.values()) {
            if(mundo.frustrum(chunk, ui.jg) && chunk.malha != null && chunk.contaSolida > 0) {
                chunk.malha.render(shader, GL20.GL_TRIANGLES, 0, chunk.contaSolida);
            }
        }
        // 2. transparentes:
        Gdx.gl.glEnable(GL20.GL_BLEND);

        for(final Chunk chunk : mundo.chunks.values()) {
            if(mundo.frustrum(chunk, ui.jg) && chunk.malha != null && chunk.contaTransp > 0) {
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
                int yTeste = Mundo.obterAlturaChao((int)ui.jg.posicao.x, (int)ui.jg.posicao.z);
                if(yTeste > 1) {
                    ui.jg.posicao.y = yTeste;
                    ui.jg.nasceu = true;
                    Gdx.app.log("[Jogo]", "jogador nasceu a "+yTeste+" blocos de altura");
                } else Gdx.app.log("[Jogo]", "não nasceu, altura recebida: "+yTeste);
            }
            ui.jg.att(delta);
        }
        Gdx.gl.glDisable(GL20.GL_CULL_FACE);

        ui.att(delta, mundo);

		if(ui.debug) {
			if(debugCaixas == null) {
				debugCaixas = new com.badlogic.gdx.graphics.glutils.ShapeRenderer();
			}
			debugCaixas.setColor(0, 1, 0, 1);
			debugCaixas.setProjectionMatrix(ui.jg.camera.combined);
			debugCaixas.begin(com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType.Line);

            debugCaixas.box(ui.jg.posicao.x - ui.jg.largura/2, ui.jg.posicao.y, ui.jg.posicao.z + ui.jg.largura/2, ui.jg.largura, ui.jg.altura, ui.jg.largura);

            debugCaixas.end();
		}
		ui.jg.render();
    }

    public void liberar() {
        shader.dispose();
        ui.liberar();
        mundo.liberar();
    }
}

