package com.minimine.graficos;

import com.minimine.ui.UI;
import com.minimine.entidades.Jogador;
import com.minimine.mundo.Mundo;
import com.minimine.mundo.Chave;
import com.minimine.mundo.blocos.BlocoModelo;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.minimine.utils.DiaNoiteUtil;
import com.minimine.mundo.chunks.Chunk;
import com.minimine.utils.NuvensUtil;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.Texture;
import com.minimine.utils.DiaNoiteUtil;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.math.Vector2;
import com.minimine.entidades.Entidade;
import com.minimine.mundo.blocos.BlocoEstrutura;
import com.badlogic.gdx.graphics.g3d.utils.DefaultShaderProvider;
import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.minimine.graficos.shaders.ShaderBranco;
import com.minimine.utils.Mat;
import java.util.List;
import com.minimine.entidades.GerenciadorEntidades;
import com.minimine.Logs;
import com.badlogic.gdx.graphics.profiling.GLProfiler;
import com.minimine.cenas.Jogo;

public class Render extends Renderizador {
    public static ShaderProgram shader;
    public static ShapeRenderer debugCaixas;
	
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
    "uniform vec3 u_corCeu;\n" +
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
    "   vec3 corNevoa = u_corCeu;\n" + 
    "   gl_FragColor = vec4(mix(texCor.rgb * iluminacaoFinal, corNevoa, fator), texCor.a);\n" +
    "}";
	public static String logs = "";
	public static Runtime rt = Runtime.getRuntime();
	public static GLProfiler gpu;

    public Render(List<Jogador> jogadores, Mundo mundo) {
        super(jogadores, mundo);
	}
	
	@Override
	public void iniciar() {
        super.iniciar();

        shader = new ShaderProgram(vert, frag);
		ShaderProgram.pedantic = false;

        if(!shader.isCompiled()) Gdx.app.log("shader", "[ERRO]: "+shader.getLog());
        debugCaixas = new ShapeRenderer();

        // animação da água
        Animacoes2D.add("agua", new TextureRegion[]{
			Texturas.atlas.get("agua_a1"), Texturas.atlas.get("agua_a2"),
			Texturas.atlas.get("agua_a3"), Texturas.atlas.get("agua_a4")
		}, 2.5f);  // 2.5 quadros por segundo

        // carrega as particulas
        gp = new GerenciadorParticulas(ui.jg);

        if(mundo.nuvens) NuvensUtil.iniciar(ui.jg.posicao);
        
		mb = new ModelBatch(new DefaultShaderProvider() {
				@Override
				protected Shader createShader(Renderable r) {
					return new ShaderBranco();
				}
			});
		gpu = new GLProfiler(Gdx.graphics);
		if(UI.debug) gpu.enable();
		else gpu.disable();
    }
	
	@Override
    public void att(float delta) {
		super.att(delta);
		if(!pause) {
			Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
			Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
			Gdx.gl.glEnable(GL20.GL_CULL_FACE);
			Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);

			if(mundo.ciclo) mundo.diaNoite.att(ui.jg.camera, delta);
			
			if(mundo.carregado) {
				GerenciadorEntidades.att(delta, mundo, ui.jg);
			}
			shader.begin();

			shader.setUniformMatrix("u_projPos", ui.jg.camera.combined);
			shader.setUniformf("u_luzCeu", mundo.diaNoite.luz);
			shader.setUniformf("u_corCeu", mundo.diaNoite.corCeuR, mundo.diaNoite.corCeuG, mundo.diaNoite.corCeuB);

			// == envia dados do atlas pro shader ===
			// envia a tabela de pesquisa uma vez por frame(ou quando mudar)
			// o 4fv envia vetores de 4 floats
			shader.setUniform4fv("u_atlasRects", BlocoModelo.dadosAtlas, 0, 256 * 4); 

			Texturas.blocos.bind(0);
			shader.setUniformi("u_textura", 0);
			Gdx.gl.glDisable(GL20.GL_BLEND);

			// 1. solidos:
			for(final Chunk chunk : mundo.chunks.values()) {
				final boolean renderizar = frustrum(chunk, ui.jg) && chunk.gpuPronta;
				if(renderizar && chunk.contaSolida > 0) {
					shader.setUniformf("u_chunkPos", chunk.x << 4, 0, chunk.z << 4);
					renderChunk(chunk, chunk.iboId, 0, chunk.contaSolida, shader);
				}
			}
			// 2. entidades, antes da agua para nao serem bloqueadas pelo buffer de profundidade dela
			shader.end();
			gp.att(delta);
			mb.begin(ui.jg.camera);
			for(int i = 0; i < mundo.entidades.size(); i++) {
				final Entidade e = mundo.entidades.get(i);
				if(e != ui.jg) e.render(mb, delta);
			}
			if(jogadores.size() != 1) {
				for(int i = 1; i < jogadores.size(); i++) {
					final Jogador jg = jogadores.get(i);
					jg.render(mb, delta);
				}
			}
			if(ui.gui && ui.jg.pessoa != 0) ui.jg.render(mb, delta);
			mb.render(gp);
			mb.end();
			
			// restaura estado GL apos o mb
			Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
			
			if(ui.debug) {
				debugCaixas.setProjectionMatrix(ui.jg.camera.combined);
				debugCaixas.begin(ShapeRenderer.ShapeType.Line);
				debugCaixas.setColor(1, 1, 0, 1);
				final int cxJg = Mat.floor(ui.jg.posicao.x / 16f);
				final int czJg = Mat.floor(ui.jg.posicao.z / 16f);
				final float ox = cxJg * 16f;
				final float oz = czJg * 16f;
				debugCaixas.box(ox, 0f, oz + 16f, 16f, 256f, 16f);
				debugCaixas.end();
			}
			Gdx.gl.glEnable(GL20.GL_CULL_FACE);
			Gdx.gl.glDepthMask(true);
			shader.begin();
			shader.setUniformMatrix("u_projPos", ui.jg.camera.combined);
			shader.setUniformf("u_luzCeu", mundo.diaNoite.luz);
			shader.setUniformf("u_corCeu", mundo.diaNoite.corCeuR, mundo.diaNoite.corCeuG, mundo.diaNoite.corCeuB);
			shader.setUniform4fv("u_atlasRects", BlocoModelo.dadosAtlas, 0, 256 * 4);
			Texturas.blocos.bind(0);
			shader.setUniformi("u_textura", 0);

			// 3. transparentes:
			Gdx.gl.glEnable(GL20.GL_BLEND);
			Gdx.gl.glDisable(GL20.GL_CULL_FACE);

			for(final Chunk chunk : mundo.chunks.values()) {
				final boolean renderizar = frustrum(chunk, ui.jg) && chunk.gpuPronta;
				if(renderizar && chunk.contaTransp > 0) {
					shader.setUniformf("u_chunkPos", chunk.x << 4, 0, chunk.z << 4);
					renderChunk(chunk, chunk.iboTranspId, 0, chunk.contaTransp, shader);
				}
			}
			Animacoes2D.att(delta);
			shader.end();

			if(mundo.nuvens) NuvensUtil.att(delta, ui.jg.camera);

			// 4. jogador principal
			if(ui.jg.pessoa == 0) {
				mb.begin(ui.jg.camera);
				if(ui.gui) ui.jg.render(mb, delta);
				mb.end();
			}
			// renderiza os nomes
			if(jogadores.size() > 1) {
				for(int i = 1; i < jogadores.size(); i++) {
					jogadores.get(i).renderNome(ui.jg.camera);
				}
			}
			// renderiza o debug:
			if(ui.debug) {
				debugCaixas.setColor(1, 0, 0, 1); // vermelho pro jogador
				debugCaixas.setProjectionMatrix(ui.jg.camera.combined);
				debugCaixas.begin(ShapeRenderer.ShapeType.Line);
				
				for(int i = 0; i < jogadores.size(); i++) {
					final Jogador jg = jogadores.get(i);
					debugCaixas.box(jg.posicao.x - jg.largura/2, jg.posicao.y, jg.posicao.z + jg.largura/2, jg.largura, jg.altura, jg.largura);
				}
				debugCaixas.setColor(0, 1, 0, 1); // verde para as entidades
				for(int i = 0; i < mundo.entidades.size(); i++) {
					final Entidade e = mundo.entidades.get(i);
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
			// raio dos bloco_estrutura: itera lista de posições ativas(O(n) onde n = blocos colocados)
			if(!BlocoEstrutura.bcaixas.isEmpty()) {
				debugCaixas.setProjectionMatrix(ui.jg.camera.combined);
				debugCaixas.begin(ShapeRenderer.ShapeType.Line);
				debugCaixas.setColor(1.0f, 0.5f, 0.0f, 1f); // laranja
				for(int[] bl : BlocoEstrutura.bcaixas) {
					// bl = { x global, y global, z global } do bloco_estrutura
					final int bx = bl[0], by = bl[1], bz = bl[2];
					final float larg = BlocoEstrutura.obterLarg(bx, by, bz);
					final float alt  = BlocoEstrutura.obterAlt(bx, by, bz);
					final float prof = BlocoEstrutura.obterProf(bx, by, bz);
					final int cx = BlocoEstrutura.obterCX(bx, by, bz);
					final int cy = BlocoEstrutura.obterCY(bx, by, bz);
					final int cz = BlocoEstrutura.obterCZ(bx, by, bz);
					// z é a face traseira, profundidade negativo vai pra frente
					debugCaixas.box(bx + cx, by + cy, bz + cz, larg, alt, -prof);
				}
				debugCaixas.end();
			}
			// debug:
			final float yawNorm = ((ui.jg.yaw % 360) + 360) % 360;
			final String direcao;
			if(yawNorm >= 337.5f || yawNorm < 22.5f)   direcao = "Norte";
			else if(yawNorm < 67.5f) direcao = "Nordeste";
			else if(yawNorm < 112.5f) direcao = "Leste";
			else if(yawNorm < 157.5f) direcao = "Sudeste";
			else if(yawNorm < 202.5f) direcao = "Sul";
			else if(yawNorm < 247.5f) direcao = "Sudoeste";
			else if(yawNorm < 292.5f) direcao = "Oeste";
			else direcao = "Noroeste";

			final float livre = rt.freeMemory() >> 20;
			final float total = rt.totalMemory() >> 20;
			final float nativa = Gdx.app.getNativeHeap() >> 20;

			final String[] logsArr = Logs.logs.split("\n");
			Logs.logs = "";
			final int inicio = Math.max(0, logsArr.length - 15);
			for(int i = inicio; i < logsArr.length; i++) logs += logsArr[i] + '\n';

			String bioma = "";

			if(mundo.motor != null) {
				bioma = mundo.motor.obterBioma((int)ui.jg.posicao.x, (int)ui.jg.posicao.z);
			}
			if(ui.debug) {
				Jogo.debug1 = String.format(
					"Jogador:\nX: %.1f, Y: %.1f, Z: %.1f\nDireção: %s (%.1f°)\nModo: %s\nSlot: %d\nItem: %s\n" +
					"No chão: %b\nNa água: %b\nAgachado: %b\nVoando: %b\n\nStatus:\nVelocidade: %.2f\nAltura: %.2f\n\n" +
					"Controles:\nDireita: %b, Esquerda: %b\nFrente: %b, Trás: %b\nCima: %b\nBaixo: %b\nAção: %b\n\n" +
					"Mundo:\nNome: %s\nBioma atual: %s\nRaio Chunks: %d\nChunks: %d\n" +
					"Chunks Alteradas: %d\nSemente: %d\nTempo: %.2f\nVelocidade do tempo: %.5f",
					ui.jg.posicao.x, ui.jg.posicao.y, ui.jg.posicao.z, direcao, yawNorm,
					(ui.jg.modo == 0 ? "espectador" : ui.jg.modo == 1 ? "criativo" : "sobrevivencia"),
					ui.jg.inv.slotSelecionado, ui.jg.item, ui.jg.noChao, ui.jg.naAgua, ui.jg.agachado, ui.jg.voando,
					ui.jg.velo, ui.jg.altura,
					ui.jg.direita, ui.jg.esquerda, ui.jg.frente, ui.jg.tras, ui.jg.cima, ui.jg.baixo, ui.jg.acao,
					mundo.nome, bioma, mundo.RAIO_CHUNKS, mundo.chunks.size(),
					mundo.chunksMod.size(), mundo.semente, mundo.diaNoite.tempo, mundo.diaNoite.tempo_velo);

				Jogo.debug2 = String.format(
					"FPS: %d\nGPU:\nDesenhos: %d\nVértices: %.0f\nTrocas de Shader: %d\nLinks de textura: %d\n" +
					"Threads ativas: %d\nMemória livre: %.1f MB\nMemória total: %.1f MB\n" +
					"Memória usada: %.1f MB\nMemória nativa: %.1f MB\n\nLogs:\n%s",
					fps,
					gpu.getDrawCalls(),
					gpu.getVertexCount().total,
					gpu.getShaderSwitches(),
					gpu.getTextureBindings(),
					Thread.activeCount(), livre, total, total - livre,
					nativa,
					logs);
				gpu.reset();
			}
		}
		// renderiza a interface de usuario:
		ui.att(delta, mundo);
    }

	public static final boolean frustrum(Chunk chunk, Jogador jogador) {
		final float cx = (chunk.x << 4) + 8f;
		final float cz = (chunk.z << 4) + 8f;

		final float raioBlocos = (Mundo.RAIO_CHUNKS << 4) + 16f;
		if(Vector2.dst2(cx, cz, jogador.posicao.x, jogador.posicao.z) >= raioBlocos * raioBlocos) return false;

		return jogador.camera.frustum.boundsInFrustum(cx, 128f, cz, 16f, 256f, 16f);
	}
	
	@Override
    public void liberar() {
		super.liberar();
        shader.dispose();
        debugCaixas.dispose();
		if(mundo.nuvens) NuvensUtil.liberar();
		Animacoes2D.liberar();
    }
}
