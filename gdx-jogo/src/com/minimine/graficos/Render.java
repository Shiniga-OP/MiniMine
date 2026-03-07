package com.minimine.graficos;

import com.minimine.ui.UI;
import com.minimine.entidades.Jogador;
import com.minimine.mundo.Mundo;
import com.minimine.mundo.Chave;
import com.minimine.mundo.blocos.BlocoModelo;
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
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.math.Vector2;
import com.minimine.entidades.Entidade;

public class Render {
    public UI ui;
    public Mundo mundo;
	public static boolean pause = false;
    public static ShaderProgram shader;
    public static ShapeRenderer debugCaixas;
    public static GerenciadorParticulas gp;
    public static ModelBatch mb; // gerenciador de modelos 3D de entidades
    
    public static final VertexAttribute[] atriburs = new VertexAttribute[] {
        new VertexAttribute(VertexAttributes.Usage.Position, 1, "a_pos"),
        new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, "a_texCoord"),
        new VertexAttribute(VertexAttributes.Usage.Generic, 1, "a_texId"),
        new VertexAttribute(VertexAttributes.Usage.ColorPacked, 4, "a_cor")
    };

    public static String vert = 
    "attribute float a_pos;\n" +
    "attribute vec2 a_texCoord;\n" +
    "attribute float a_texId;\n" +
    "attribute vec4 a_cor;\n" +
    "uniform mat4 u_projPos;\n" +
    "varying vec2 v_texCoord;\n" +
    "varying float v_texId;\n" +
    "varying vec4 v_cor;\n" +
	"uniform vec3 u_chunkPos;\n"+
   
    // descompacta posição usando operações matematicas
    "vec3 descompactarPos(float compactada) {\n" +
    // arredonda pro int mais proximo
    "    float pacote = floor(compactada + 0.5);\n" +
    // extrai X(5 bits inferiores: 0-31)
    "    float x = mod(pacote, 32.0);\n" +
    // extrai Y(proximos 9 bits: 0-511)
    "    float temp = floor(pacote / 32.0);\n" +
    "    float y = mod(temp, 512.0);\n" +
    // extrai Z(proximos 5 bits: 0-31)
    "    float z = floor(temp / 512.0);\n" +
    "    return vec3(x, y, z);\n" +
    "}\n" +
	
    "void main() {\n" +
    "   vec3 posLocal = descompactarPos(a_pos);\n" +
	"   vec3 posGlobal = posLocal + u_chunkPos;\n"+
    "   v_texCoord = a_texCoord;\n" +
    "   v_texId = a_texId;\n" +
    "   v_cor = a_cor;\n" +
    "   gl_Position = u_projPos * vec4(posGlobal, 1.0);\n" +
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
    "   vec4 limites = u_atlasRects[int(v_texId + 0.5)];\n" +

    // 2. calcula o tamanho da textura no atlas(uMax - uMin, vMax - vMin)
    "   vec2 tam = limites.zw - limites.xy;\n" +

    // 3. aplica o fract() pra repetir a textura(GULOSO)
    "   vec2 localUV = fract(v_texCoord);\n" +

    // 4. mapeia para a posicao final no atlas
    "   const float extra = 0.0005;\n" + // pra não ter cantos invisiveis
    "   vec2 finalUV = limites.xy + extra + localUV * (tam - 2.0 * extra);\n" +

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

        debugCaixas = new ShapeRenderer();

        // animação da água
        Animacoes2D.add("agua", new TextureRegion[]{
			Texturas.atlas.get("agua"),
			Texturas.atlas.get("agua_a1"),
			Texturas.atlas.get("agua_a2")
		}, 2.5f);  // 2.5 quadros por segundo
		
        // carrega as particulas
        gp = new GerenciadorParticulas(ui.jg);

        ShaderProgram.pedantic = false;

        if(mundo.nuvens) NuvensUtil.iniciar(ui.jg.posicao);
        if(mundo.ciclo) CorposCelestes.iniciar();

        mb = new ModelBatch(); // carrega o gerenciador de modelos das entidades
    }

    public void att(float delta) {
		if(!pause) {
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
				if(frustrum(chunk, ui.jg) && chunk.malha != null && chunk.contaSolida > 0) {
					shader.setUniformf("u_chunkPos", chunk.x << 4, 0, chunk.z << 4);
					chunk.malha.render(shader, GL20.GL_TRIANGLES, 0, chunk.contaSolida);
				}
			}
			// 2. transparentes:
			Gdx.gl.glEnable(GL20.GL_BLEND);
			Gdx.gl.glDisable(GL20.GL_CULL_FACE);
			
			for(final Chunk chunk : mundo.chunks.values()) {
				if(frustrum(chunk, ui.jg) && chunk.malha != null && chunk.contaTransp > 0) {
					shader.setUniformf("u_chunkPos", chunk.x << 4, 0, chunk.z << 4);
					chunk.malha.render(shader, GL20.GL_TRIANGLES, chunk.contaSolida, chunk.contaTransp);
				}
			}
			Animacoes2D.att(delta);
			shader.end();

			if(mundo.nuvens) NuvensUtil.att(ui.jg.camera.combined);
			gp.att(delta);
			
			// renderiza os modelos 3D
			mb.begin(ui.jg.camera);

			ui.jg.render(mb);
			for(Entidade e : mundo.entidades) {
				e.render(mb);
			}
			mb.end();
			
			// renderiza o debug:
			if(ui.debug) {
				debugCaixas.setColor(1, 0, 0, 1); // vermelho pro jogador
				debugCaixas.setProjectionMatrix(ui.jg.camera.combined);
				debugCaixas.begin(ShapeRenderer.ShapeType.Line);

				debugCaixas.box(ui.jg.posicao.x - ui.jg.largura/2, ui.jg.posicao.y, ui.jg.posicao.z + ui.jg.largura/2, ui.jg.largura, ui.jg.altura, ui.jg.largura);

				debugCaixas.setColor(0, 1, 0, 1); // verde para as entidades
				for(Entidade e : mundo.entidades) {
					debugCaixas.box(
						e.posicao.x - e.largura / 2, 
						e.posicao.y, 
						e.posicao.z + e.profundidade / 2, 
						e.largura, 
						e.altura, 
						e.profundidade
					);
				}
				debugCaixas.end();
			}
		}
		// renderiza a interface de usuario:
		ui.att(delta, mundo);
    }
	
	public final static boolean frustrum(Chunk chunk, Jogador jogador) {
		final float globalX = chunk.x << 4;
		final float globalZ = chunk.z << 4;
		
		// o raio precisa sendo convertido pra "ao quadrado" pra comparação funcionar
		// (RAIO * 16) * (RAIO * 16)
		final float raioEmPixels = Mundo.RAIO_CHUNKS << 4;
		final float raioLimite = raioEmPixels * raioEmPixels;
		
		// dist2(distancia ao quadrado)
		if(!(Vector2.dst2(globalX, globalZ, jogador.posicao.x, jogador.posicao.z) < raioLimite)) return false;

		return jogador.camera.frustum.boundsInFrustum(globalX, 0, globalZ, 16, 255, 16);
	}
    public void liberar() {
        shader.dispose();
        debugCaixas.dispose();
        ui.liberar();
        ui.jg.liberar();
        mundo.liberar();
        mb.dispose();
        gp.liberar();
		if(mundo.nuvens) NuvensUtil.liberar();
		Animacoes2D.liberar();
    }
}

