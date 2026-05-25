package com.minimine.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.minimine.graficos.Texturas;

public class DiaNoiteUtil {
    // cor do horizonte lida pelo Render para nevoa dos blocos
    public float corCeuR, corCeuG, corCeuB;

    // angulo do ciclo(0 = amanhecer, PI/2 = meio-dia, PI = entardecer, 3PI/2 = meia-noite)
    public float tempo = MathUtils.PI * 0.75f;
    public float tempo_velo = MathUtils.PI2 / 1200f; // dia/noite de 20 minutos
    public static float luz = 1f;

    public final Vector3 dirSol = new Vector3();
    public final Vector3 dirLua = new Vector3();

    // === DOMO(céu gradiente) ===
    public Mesh malhaDomo;
    public ShaderProgram shaderDomo;

    public static final String DOMO_VERT =
	"attribute vec3 a_pos;\n" +
	"uniform mat4 u_proj;\n" +
	"uniform vec3 u_posCamera;\n" +
	"varying float v_alt;\n" +
	
	"void main() {\n" +
	"  v_alt = a_pos.y;\n" +
	"  gl_Position = u_proj * vec4(a_pos + u_posCamera, 1.0);\n" +
	"}";

    // so interpola 2 cores por altitude + tint de por do sol no horizonte inteiro
    public static final String DOMO_FRAG =
	"#ifdef GL_ES\n" +
	"precision mediump float;\n" +
	"#endif\n" +
	"varying float v_alt;\n" +
	"uniform vec3 u_corTopo;\n" +
	"uniform vec3 u_corHorizonte;\n" +
	
	"void main() {\n" +
	"  float t = clamp(v_alt / 0.5, 0.0, 1.0);\n" +
	"  t = t * t;\n" +
	"  gl_FragColor = vec4(mix(u_corHorizonte, u_corTopo, t), 1.0);\n" +
	"}";

    // === ESTRELAS(GL_POINTS, quadrados nativos) ===
    public Mesh malhaEstrelas;
    public ShaderProgram shaderEstrelas;
    public static final int NUM_ESTRELAS = 800;

    public static final String ESTRELAS_VERT =
	"attribute vec3 a_pos;\n" +
	"attribute float a_brilho;\n" +
	"uniform mat4 u_proj;\n" +
	"uniform vec3 u_posCamera;\n" +
	"uniform float u_alfa;\n" +
	"varying float v_brilho;\n" +
	
	"void main() {\n" +
	"  v_brilho = a_brilho;\n" +
	"  gl_Position = u_proj * vec4(a_pos + u_posCamera, 1.0);\n" +
	"  gl_PointSize = 2.0;\n" +
	"}";

    public static final String ESTRELAS_FRAG =
	"#ifdef GL_ES\n" +
	"precision mediump float;\n" +
	"#endif\n" +
	"varying float v_brilho;\n" +
	"uniform float u_alfa;\n" +
	
	"void main() {\n" +
	"  gl_FragColor = vec4(1.0, 1.0, 1.0, v_brilho * u_alfa);\n" +
	"}";

    // === HALO sol/lua (GL_POINTS, gl_PointCoord garante círculo) ===
    public Mesh malhaHalo;
    public ShaderProgram shaderHalo;

    public static final String HALO_VERT =
	"attribute vec3 a_pos;\n" +
	"attribute vec2 a_uv;\n" +
	"uniform mat4 u_proj;\n" +
	"uniform vec3 u_centro;\n" +
	"uniform float u_tam;\n" +
	"uniform vec3 u_camD;\n" +
	"varying vec2 v_uv;\n" +
	
	"void main() {\n" +
	"  v_uv = a_uv;\n" +
	"  vec3 mundoCima = vec3(0.0, 1.0, 0.0);\n" +
	"  vec3 r = normalize(cross(u_camD, mundoCima));\n" +
	"  vec3 u = cross(r, u_camD);\n" +
	"  vec3 p = u_centro + r * a_pos.x * u_tam + u * a_pos.y * u_tam;\n" +
	"  gl_Position = u_proj * vec4(p, 1.0);\n" +
	"}";

    public static final String HALO_FRAG =
	"#ifdef GL_ES\n" +
	"precision mediump float;\n" +
	"#endif\n" +
	"varying vec2 v_uv;\n" +
	"uniform vec3 u_corHalo;\n" +
	
	"void main() {\n" +
	"  vec2 uv = v_uv * 2.0 - 1.0;\n" +
	"  float d = length(uv);\n" +
	"  float brilho = 0.06 / (d * d + 0.04);\n" +
	"  float borda = 1.0 - smoothstep(0.5, 1.0, d);\n" +
	"  float a = clamp(brilho * borda, 0.0, 1.0);\n" +
	"  if(a < 0.005) discard;\n" +
	"  gl_FragColor = vec4(u_corHalo, a * 0.5);\n" +
	"}";

    // === BILLBOARD sol/lua ===
    public Mesh malhaBillboard;
    public ShaderProgram shaderBillboard;

    public static final String BILL_VERT =
	"attribute vec3 a_pos;\n" +
	"attribute vec2 a_uv;\n" +
	"uniform mat4 u_proj;\n" +
	"uniform vec3 u_centro;\n" +
	"uniform float u_tam;\n" +
	"uniform vec3 u_camD;\n" +
	"uniform vec4 u_uvRegiao;\n" +
	"varying vec2 v_uv;\n" +
	
	"void main() {\n" +
	"  v_uv = mix(u_uvRegiao.xy, u_uvRegiao.zw, a_uv);\n" +
	"  vec3 mundoCima = vec3(0.0, 1.0, 0.0);\n" +
	"  vec3 r = normalize(cross(u_camD, mundoCima));\n" +
	"  vec3 u = cross(r, u_camD);\n" +
	"  vec3 p = u_centro + r * a_pos.x * u_tam + u * a_pos.y * u_tam;\n" +
	"  gl_Position = u_proj * vec4(p, 1.0);\n" +
	"}";

    public static final String BILL_FRAG =
	"#ifdef GL_ES\n" +
	"precision mediump float;\n" +
	"#endif\n" +
	"varying vec2 v_uv;\n" +
	"uniform sampler2D u_tex;\n" +
	
	"void main() {\n" +
	"  vec4 c = texture2D(u_tex, v_uv);\n" +
	"  if(c.a < 0.05) discard;\n" +
	"  gl_FragColor = vec4(c.rgb, c.a);\n" +
	"}";

    public final float RAIO_VISUAL = 100f;
    public final float TAM_SOL = 8f;
    public final float TAM_LUA = 6f;

    // cores do céu calculadas na CPU
    // dia
    public static final float[] COR_TOPO_DIA = {0.26f, 0.50f, 0.95f};
    public static final float[] COR_HOR_DIA = {0.52f, 0.74f, 1.00f};
    // noite
    public static final float[] COR_TOPO_NOITE = {0.02f, 0.03f, 0.10f};
    public static final float[] COR_HOR_NOITE = {0.03f, 0.05f, 0.14f};
    // por/nascer do sol, tinge o horizonte inteiro
    public static final float[] COR_POENTE = {1.00f, 0.35f, 0.05f};

    public void iniciar() {
        shaderDomo = new ShaderProgram(DOMO_VERT, DOMO_FRAG);
        if(!shaderDomo.isCompiled())
            Gdx.app.log("DiaNoite", "[ERRO] domo: " + shaderDomo.getLog());

        shaderEstrelas = new ShaderProgram(ESTRELAS_VERT, ESTRELAS_FRAG);
        if(!shaderEstrelas.isCompiled())
            Gdx.app.log("DiaNoite", "[ERRO] estrelas: " + shaderEstrelas.getLog());

        shaderHalo = new ShaderProgram(HALO_VERT, HALO_FRAG);
        if(!shaderHalo.isCompiled())
            Gdx.app.log("DiaNoite", "[ERRO] halo: " + shaderHalo.getLog());

        shaderBillboard = new ShaderProgram(BILL_VERT, BILL_FRAG);
        if(!shaderBillboard.isCompiled())
            Gdx.app.log("DiaNoite", "[ERRO] billboard: " + shaderBillboard.getLog());

        malhaDomo = criarDomo(20, 8);
        malhaEstrelas = criarEstrelas();

        malhaHalo = new Mesh(true, 4, 6,
		new VertexAttribute(VertexAttributes.Usage.Position, 3, "a_pos"),
		new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, "a_uv"));
		
        malhaHalo.setVertices(new float[]{
			-0.5f, -0.5f, 0, 0, 1,
			0.5f, -0.5f, 0, 1, 1,
			0.5f,  0.5f, 0, 1, 0,
			-0.5f,  0.5f, 0, 0, 0
		});
        malhaHalo.setIndices(new short[]{0, 1, 2, 2, 3, 0});

        malhaBillboard = new Mesh(true, 4, 6,
		new VertexAttribute(VertexAttributes.Usage.Position, 3, "a_pos"),
		new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, "a_uv"));
		
        malhaBillboard.setVertices(new float[]{
			-0.5f, -0.5f, 0, 0, 1,
			0.5f, -0.5f, 0, 1, 1,
			0.5f,  0.5f, 0, 1, 0,
			-0.5f,  0.5f, 0, 0, 0
		});
        malhaBillboard.setIndices(new short[]{0, 1, 2, 2, 3, 0});
    }

    public void att(PerspectiveCamera camera, float delta) {
        tempo += tempo_velo * delta;
        if(tempo > MathUtils.PI2) tempo -= MathUtils.PI2;

        // sol move num arco fixo no plano XY do mundo, independente da camera
        dirSol.set(MathUtils.cos(tempo), MathUtils.sin(tempo), 0f);
        dirLua.set(-dirSol.x, -dirSol.y, 0f);

        // luz ambiente: 0.05(noite) a 1.0(dia)
        luz = Math.max(0.05f, (dirSol.y + 1f) * 0.475f + 0.05f);

        // fator de transição dia/noite(0=noite, 1=dia)
        float dia = passoBorrado(-0.15f, 0.15f, dirSol.y);
        // fator por/nascer do sol: pico quando o sol ta no horizonte
        float poente = 1f - Math.abs(dirSol.y / 0.5f);
        poente = Math.max(0f, Math.min(1f, poente));
        poente = poente * poente;

        // cores do topo e horizonte interpoladas na CPU
        final float topoR = mix(COR_TOPO_NOITE[0], COR_TOPO_DIA[0], dia) * luz;
        final float topoG = mix(COR_TOPO_NOITE[1], COR_TOPO_DIA[1], dia) * luz;
        final float topoB = mix(COR_TOPO_NOITE[2], COR_TOPO_DIA[2], dia) * luz;

        corCeuR = mix(COR_HOR_NOITE[0], COR_HOR_DIA[0], dia) * luz;
        corCeuG = mix(COR_HOR_NOITE[1], COR_HOR_DIA[1], dia) * luz;
        corCeuB = mix(COR_HOR_NOITE[2], COR_HOR_DIA[2], dia) * luz;

        // tinge o horizonte inteiro com a cor do por do sol
        corCeuR = mix(corCeuR, COR_POENTE[0], poente * 0.85f);
        corCeuG = mix(corCeuG, COR_POENTE[1], poente * 0.85f);
        corCeuB = mix(corCeuB, COR_POENTE[2], poente * 0.85f);

        // alfa das estrelas: so a noite, some quando o sol ta subindo
        float alfaEstrelas = 1f - dia;
		
        // === renderiza ===
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glDisable(GL20.GL_CULL_FACE);
        Gdx.gl.glDisable(GL20.GL_BLEND);

        // domo
        shaderDomo.begin();
        shaderDomo.setUniformMatrix("u_proj", camera.combined);
        shaderDomo.setUniformf("u_posCamera", camera.position.x, camera.position.y, camera.position.z);
        shaderDomo.setUniformf("u_corTopo", topoR, topoG, topoB);
        shaderDomo.setUniformf("u_corHorizonte", corCeuR, corCeuG, corCeuB);
        malhaDomo.render(shaderDomo, GL20.GL_TRIANGLES);
        shaderDomo.end();

        // estrelas
        if(alfaEstrelas > 0.01f) {
            Gdx.gl.glEnable(GL20.GL_BLEND);
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            shaderEstrelas.begin();
            shaderEstrelas.setUniformMatrix("u_proj", camera.combined);
            shaderEstrelas.setUniformf("u_posCamera", camera.position.x, camera.position.y, camera.position.z);
            shaderEstrelas.setUniformf("u_alfa", alfaEstrelas);
            malhaEstrelas.render(shaderEstrelas, GL20.GL_POINTS);
            shaderEstrelas.end();
            Gdx.gl.glDisable(GL20.GL_BLEND);
        }
        // sol e lua
        Gdx.gl.glEnable(GL20.GL_BLEND);

        // halos: quad 5x maior que o sprite, brilho 1/d² dissipa nas bordas
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        shaderHalo.begin();
        shaderHalo.setUniformMatrix("u_proj", camera.combined);
        shaderHalo.setUniformf("u_camD", camera.direction);
        // halo do sol
        shaderHalo.setUniformf("u_centro",
		camera.position.x + dirSol.x * RAIO_VISUAL,
		camera.position.y + dirSol.y * RAIO_VISUAL,
		camera.position.z + dirSol.z * RAIO_VISUAL);
		
        shaderHalo.setUniformf("u_tam", TAM_SOL * 5f);
        shaderHalo.setUniformf("u_corHalo", 1.0f, 0.85f, 0.3f);
        malhaHalo.render(shaderHalo, GL20.GL_TRIANGLES);
        // halo da lua
        shaderHalo.setUniformf("u_centro",
		camera.position.x + dirLua.x * RAIO_VISUAL,
		camera.position.y + dirLua.y * RAIO_VISUAL,
		camera.position.z + dirLua.z * RAIO_VISUAL);
		
        shaderHalo.setUniformf("u_tam", TAM_LUA * 5f);
        shaderHalo.setUniformf("u_corHalo", 0.6f, 0.75f, 1.0f);
        malhaHalo.render(shaderHalo, GL20.GL_TRIANGLES);
        shaderHalo.end();

        // sprites: blend normal por cima
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shaderBillboard.begin();
        shaderBillboard.setUniformMatrix("u_proj", camera.combined);
        shaderBillboard.setUniformi("u_tex", 0);
        Texturas.ceu.bind(0);
        renderBillboard(dirSol, camera, TAM_SOL, Texturas.atlas.get("sol"));
        renderBillboard(dirLua, camera, TAM_LUA, Texturas.atlas.get("lua_completa"));
        shaderBillboard.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glEnable(GL20.GL_CULL_FACE);
    }

    public void renderBillboard(Vector3 dir, PerspectiveCamera camera, float tam, TextureRegion regiao) {
        float cx = camera.position.x + dir.x * RAIO_VISUAL;
        float cy = camera.position.y + dir.y * RAIO_VISUAL;
        float cz = camera.position.z + dir.z * RAIO_VISUAL;

        shaderBillboard.setUniformf("u_centro", cx, cy, cz);
        shaderBillboard.setUniformf("u_tam", tam);
        shaderBillboard.setUniformf("u_camD", camera.direction);
        shaderBillboard.setUniformf("u_uvRegiao", regiao.getU(), regiao.getV(), regiao.getU2(), regiao.getV2());
        malhaBillboard.render(shaderBillboard, GL20.GL_TRIANGLES);
    }

    public Mesh criarDomo(int setores, int aneis) {
        int numVerts = (aneis + 1) * (setores + 1);
        int numidc = aneis * setores * 6;
        float[] verts = new float[numVerts * 3];
        short[] idc = new short[numidc];

        int v = 0;
        for(int a = 0; a <= aneis; a++) {
            float phi = MathUtils.PI / 2.0f - a * (MathUtils.PI * 1.1f / aneis);
            for(int s = 0; s <= setores; s++) {
                float theta = 2.0f * MathUtils.PI * s / setores;
                verts[v++] = (float)(Math.cos(phi) * Math.cos(theta));
                verts[v++] = (float)Math.sin(phi);
                verts[v++] = (float)(Math.cos(phi) * Math.sin(theta));
            }
        }
        int i = 0;
        for(int a = 0; a < aneis; a++) {
            for(int s = 0; s < setores; s++) {
                int curr = a * (setores + 1) + s;
                int prox = curr + setores + 1;
                idc[i++] = (short)curr;
                idc[i++] = (short)prox;
                idc[i++] = (short)(prox + 1);
                idc[i++] = (short)(prox + 1);
                idc[i++] = (short)(curr + 1);
                idc[i++] = (short)curr;
            }
        }
        Mesh malha = new Mesh(true, numVerts, numidc,
		new VertexAttribute(VertexAttributes.Usage.Position, 3, "a_pos"));
        malha.setVertices(verts);
        malha.setIndices(idc);
        return malha;
    }

    public Mesh criarEstrelas() {
        // posição(x,y,z) + brilho
        float[] verts = new float[NUM_ESTRELAS * 4];
        // gerador congruencial simples
        long semente = 0x3F2A1B;
        int v = 0;
        int geradas = 0;
        while(geradas < NUM_ESTRELAS) {
            semente = semente * 6364136223846793005L + 1442695040888963407L;
            float u1 = ((semente >> 33) & 0xFFFFFFL) / (float)0xFFFFFFL;
            semente = semente * 6364136223846793005L + 1442695040888963407L;
            float u2 = ((semente >> 33) & 0xFFFFFFL) / (float)0xFFFFFFL;
            semente = semente * 6364136223846793005L + 1442695040888963407L;
            float u3 = ((semente >> 33) & 0xFFFFFFL) / (float)0xFFFFFFL;

            // distribuição uniforme na esfera
            float theta = u1 * MathUtils.PI2;
            float phi = MathUtils.acos(2f * u2 - 1f);
            float x = MathUtils.sin(phi) * MathUtils.cos(theta);
            float y = MathUtils.sin(phi) * MathUtils.sin(theta);
            float z = MathUtils.cos(phi);

            verts[v++] = x;
            verts[v++] = y;
            verts[v++] = z;
            verts[v++] = 0.4f + u3 * 0.6f; // brilho variado
            geradas++;
        }
        Mesh malha = new Mesh(true, NUM_ESTRELAS, 0,
		new VertexAttribute(VertexAttributes.Usage.Position, 3, "a_pos"),
		new VertexAttribute(VertexAttributes.Usage.Generic, 1, "a_brilho"));
		
        malha.setVertices(verts);
        return malha;
    }

    public final boolean ehNoite() { return dirSol.y < -0.15f; }
    public final boolean ehDia() { return dirSol.y > 0.15f; }

    public static final float passoBorrado(float edge0, float edge1, float x) {
        float t = Math.max(0f, Math.min(1f, (x - edge0) / (edge1 - edge0)));
        return t * t * (3f - 2f * t);
    }

    public static final float mix(float a, float b, float t) {
        return a + (b - a) * t;
    }

    public void liberar() {
        if(malhaHalo != null) malhaHalo.dispose();
        if(shaderHalo != null) shaderHalo.dispose();
        if(malhaDomo != null) malhaDomo.dispose();
        if(malhaEstrelas != null) malhaEstrelas.dispose();
        if(malhaBillboard != null) malhaBillboard.dispose();
        if(shaderDomo != null) shaderDomo.dispose();
        if(shaderEstrelas != null) shaderEstrelas.dispose();
        if(shaderBillboard != null) shaderBillboard.dispose();
    }
}
