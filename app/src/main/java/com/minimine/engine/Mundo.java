package com.minimine.engine;

import com.engine.Camera3D;
import java.util.Random;
import java.util.Map;
import java.util.List;
import com.engine.PerlinNoise3D;
import com.engine.PerlinNoise2D;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import org.json.JSONObject;
import org.json.JSONArray;
import com.engine.Modelador;
import com.engine.VBOGrupo;

public class Mundo {
    public int CHUNK_TAMANHO = 16; // padrao: 16, testes: 8
    public int MUNDO_LATERAL = 64; // padrao: 64, testes: 32
    public int RAIO_CARREGAMENTO = 7; // padrao: 8, testes: 7

	public int atlasTexturaId = -1;
	public Map<String, float[]> atlasUVMapa = new ConcurrentHashMap<>();

	public Map<ChunkCoord, Bloco[][][]> chunksAtivos = new ConcurrentHashMap<>();
	public Map<ChunkCoord, List<VBOGrupo>> chunkVBOs = new ConcurrentHashMap<>();
	public Map<ChunkCoord, Boolean> chunksAlterados = new ConcurrentHashMap<>();
	public Map<ChunkCoord, Bloco[][][]> chunksModificados = new ConcurrentHashMap<>();
    public Map<ChunkCoord, Bloco[][][]> chunksCarregados = new ConcurrentHashMap<>();
	public String nome = "novo mundo", tipo = "normal";
	public int seed;
	public String pacoteTex;
	
	public List<String> estruturas = new ArrayList<>();
	public List<Mob> entidades = new ArrayList<>();
	
	public static final int PLANICIE = 0;
    public static final int DESERTO = 1;
    public static final int MONTANHA = 2;
    public static final int FLORESTA = 3;
	public static final int FLORESTA_LAGOAS = 5;
    public static final int LAGO = 4;

	List<Bioma> BIOMAS = new ArrayList<>(); 
	
    public class Bioma {
		public final float altBase;
		public final float variacao;
		public final float escala2D;
		public final float escala3D;
		public final String blocoSup;
		public final String blocoSub;
		public final String blocoCaver;
		public final float caverna;
		public Bioma(float altBase, float variacao, float escala2D, float escala3D, String blocoSup, String blocoSub, String blocoCaver, float caverna) {
			this.altBase = altBase;
			this.variacao = variacao;
			this.escala2D = escala2D;
			this.escala3D = escala3D;
			this.blocoSup = blocoSup;
			this.blocoSub = blocoSub;
			this.blocoCaver = blocoCaver;
			this.caverna = caverna;
		}
	}

	public final float[][] NORMAIS = {
		{0f, 0f, 1f},
		{0f, 0f, -1f},
		{0f, 1f, 0f},
		{0f, -1f, 0f},
		{-1f, 0f, 0f},
		{1f, 0f, 0f}
	};
	
	public final int[] DX = {0, 0, 0, 0, -1, 1};
	public final int[] DY = {0, 0, 1, -1, 0, 0};
	public final int[] DZ = {1, -1, 0, 0, 0, 0};
	
	public final Map<String, float[]> uvCache = new ConcurrentHashMap<String, float[]>(64);
	
	public final float[] UV_PADRAO = {0f, 0f, 1f, 1f};
	
	public void defBloco(Bloco bloco, int x, int y, int z) {
		if(y < 0 || y >= MUNDO_LATERAL) return;

		int chunkX = (int) Math.floor(x / (float) CHUNK_TAMANHO);
		int chunkZ = (int) Math.floor(z / (float) CHUNK_TAMANHO);
		ChunkCoord chave =  new ChunkCoord(chunkX, chunkZ);

		Bloco[][][] chunk = chunksAtivos.get(chave);
		if(chunk == null) return;

		int localX = x - chunkX * CHUNK_TAMANHO;
		int localZ = z - chunkZ * CHUNK_TAMANHO;

		if(localX < 0 || localX >= CHUNK_TAMANHO || localZ < 0 || localZ >= CHUNK_TAMANHO) return;
		chunk[localX][y][localZ] = bloco;
	}
	
	public Bloco obterBloco(int x, int y, int z) {
		if(y < 0 || y >= MUNDO_LATERAL) return null;

		int chunkX = (int) Math.floor(x / (float) CHUNK_TAMANHO);
		int chunkZ = (int) Math.floor(z / (float) CHUNK_TAMANHO);
		ChunkCoord chave =  new ChunkCoord(chunkX, chunkZ);

		Bloco[][][] chunk = chunksAtivos.get(chave);
		if(chunk == null) return null;

		int localX = x - chunkX * CHUNK_TAMANHO;
		int localZ = z - chunkZ * CHUNK_TAMANHO;

		if(localX < 0 || localX >= CHUNK_TAMANHO || localZ < 0 || localZ >= CHUNK_TAMANHO) return null;
		return chunk[localX][y][localZ];
	}

	public Mundo(int seed, String nome, String tipo, String pacoteTex) {
		this.seed = seed;
		this.nome = nome;
	    this.tipo = tipo;
		this.pacoteTex = pacoteTex;
		this.definirBiomas();
	}
	
	public Bloco criarBloco(int x, int y, int z, String tipo) {
		if(tipo.equals("AGUA")) return new Agua(x, y, z, tipo);
		else return new Bloco(x, y, z, tipo);
	}
	
	public Bloco[][][] gerarChunk(final int chunkX, final int chunkZ) {
		final int baseX = chunkX * CHUNK_TAMANHO;
		final int baseZ = chunkZ * CHUNK_TAMANHO;
		final Bloco[][][] chunk = new Bloco[CHUNK_TAMANHO][MUNDO_LATERAL][CHUNK_TAMANHO];
		if(tipo.equals("plano")) {
			for(int x = 0; x < CHUNK_TAMANHO; x++) {
				for(int z = 0; z < CHUNK_TAMANHO; z++) {
					chunk[x][0][z] = new Bloco(baseX + x, 0, baseZ + z, "BEDROCK");
					chunk[x][1][z] = new Bloco(baseX + x, 1, baseZ + z, "TERRA");
					chunk[x][2][z] = new Bloco(baseX + x, 2, baseZ + z, "GRAMA");
				}
			}
			return chunk;
		}
		int[][] alturas = new int[CHUNK_TAMANHO][CHUNK_TAMANHO];
		int[][] biomas = new int[CHUNK_TAMANHO][CHUNK_TAMANHO];
		long sementeChunk = ((long) chunkX * 341873128712L) ^ ((long) chunkZ * 132897987541L) ^ seed;
		Random rnd = new Random(sementeChunk);
		// determina alturas e biomas
		for(int x = 0; x < CHUNK_TAMANHO; x++) {
			int globalX = baseX + x;
			for(int z = 0; z < CHUNK_TAMANHO; z++) {
				int globalZ = baseZ + z;
				float ruidoBioma = PerlinNoise2D.ruido(globalX * 0.007f, globalZ * 0.007f, seed);
				int biomaAtual;
				if(ruidoBioma < -0.5f) biomaAtual = DESERTO;
				else if(ruidoBioma < 0.01f) biomaAtual = FLORESTA_LAGOAS;
				else if(ruidoBioma < 0.15f) biomaAtual = PLANICIE;
				else if(ruidoBioma < 0.3f) biomaAtual = FLORESTA;
				else if(ruidoBioma < 0.6f) biomaAtual = MONTANHA;
				else biomaAtual = LAGO;

				biomas[x][z] = biomaAtual;
				Bioma b = BIOMAS.get(biomaAtual);
				float noise2D = PerlinNoise2D.ruido(globalX * b.escala2D, globalZ * b.escala2D, seed);
				alturas[x][z] = (int) (b.altBase + noise2D * b.variacao);
			}
		}
		// gera os blocos
		for(int x = 0; x < CHUNK_TAMANHO; x++) {
			int globalX = baseX + x;
			for(int z = 0; z < CHUNK_TAMANHO; z++) {
				int biomaAtual = biomas[x][z];
				Bioma b = BIOMAS.get(biomaAtual);
				int altura = alturas[x][z];
				int globalZ = baseZ + z;
				for(int y = 0; y < MUNDO_LATERAL; y++) {
					String tipoBloco = "AR";
					if(y == 0) tipoBloco = "BEDROCK";
					else if(y < altura - 4) {
						tipoBloco = b.blocoCaver;
						float noise3D = PerlinNoise3D.ruido(globalX * b.escala3D, y * b.escala3D, globalZ * b.escala3D, seed + 100);
						if(noise3D > b.caverna) tipoBloco = "AR";
					} else if(y < altura)  tipoBloco = b.blocoSub;
					else if(y == altura) tipoBloco = b.blocoSup;
					if(!tipoBloco.equals("AR")) chunk[x][y][z] = criarBloco(globalX, y, globalZ, tipoBloco);
				}
			}
		}
		// estruturas
		for(int i = 0; i < 4; i++) {
			int xAle = rnd.nextInt(CHUNK_TAMANHO);
			int zAle = rnd.nextInt(CHUNK_TAMANHO);
			int biomaEscolhido = biomas[xAle][zAle];

			if(biomaEscolhido == FLORESTA) {
				int altura = alturas[xAle][zAle];
				adicionarEstrutura(baseX + xAle, altura, baseZ + zAle, estruturas.get(0), chunk);
			} else if(biomaEscolhido == DESERTO && i < 1) {
				int altura = alturas[xAle][zAle];
				adicionarEstrutura(baseX + xAle, altura, baseZ + zAle, estruturas.get(3), chunk);
			}
		}
		return chunk;
	}

	public Map<Integer, List<float[]>> calculoVBO(Bloco[][][] chunk) {
		Map<Integer, List<float[]>> dadosPorTextura = new HashMap<>(8);
		for(int x = 0; x < CHUNK_TAMANHO; x++) {
			for(int y = 0; y < MUNDO_LATERAL; y++) {
				for(int z = 0; z < CHUNK_TAMANHO; z++) {
					Bloco bloco = chunk[x][y][z];
					if(bloco == null) continue;

					float[] vertices = null;
					if(bloco instanceof Agua) {
						Agua b = (Agua) bloco;
						vertices = Modelador.criarAgua(b.x, b.y, b.z, b.nivel, b.dir);
					} else vertices = Modelador.criarBloco(bloco.x, bloco.y, bloco.z);

					for(int face = 0; face < 6; face++) {
						if(!faceVisivel(bloco.x, bloco.y, bloco.z, face)) continue;

						float[] dadosFace = new float[48];
						float[] normal = NORMAIS[face];
						int antes = face * 18;
						// copia vertices + normais otimizado
						for(int v = 0; v < 6; v++) {
							int src = antes + v * 3;
							int dst = v * 8;
							System.arraycopy(vertices, src, dadosFace, dst, 3);
							System.arraycopy(normal, 0, dadosFace, dst + 3, 3);
						}
						String recurso = TipoBloco.tipos.get(bloco.id)[face];
						float[] uv = uvCache.get(recurso);
						if(uv == null) {
							uv = atlasUVMapa.get(recurso);
							if(uv == null) uv = UV_PADRAO;
							uvCache.putIfAbsent(recurso, uv);
						}
						dadosFace[6] = uv[0]; dadosFace[7] = uv[3];
						dadosFace[14] = uv[2]; dadosFace[15] = uv[3];
						dadosFace[22] = uv[2]; dadosFace[23] = uv[1];
						dadosFace[30] = uv[0]; dadosFace[31] = uv[3];
						dadosFace[38] = uv[2]; dadosFace[39] = uv[1];
						dadosFace[46] = uv[0]; dadosFace[47] = uv[1];
						List<float[]> lista = dadosPorTextura.get(atlasTexturaId);
						if(lista == null) {
							lista = new ArrayList<>(256);
							dadosPorTextura.put(atlasTexturaId, lista);
						}
						lista.add(dadosFace);
					}
				}
			}
		}
		return dadosPorTextura;
	}

	public boolean faceVisivel(int x, int y, int z, int face) {
		int ny = y + DY[face];
		if(ny < 0 || ny >= MUNDO_LATERAL) return true;

		int nx = x + DX[face];
		int nz = z + DZ[face];
		// calculo otimizado de chunk
		int chunkX = nx >> 4; // divisao por 16(CHUNK_TAMANHO) usando shift
		int chunkZ = nz >> 4;

		ChunkCoord chaveChunk = new ChunkCoord(chunkX, chunkZ);
		Bloco[][][] chunkVizinho = chunksAtivos.get(chaveChunk);
		if(chunkVizinho == null) return false;
		
		int localX = nx & 0xF; // modulo 16 usando AND
		int localZ = nz & 0xF;

		Bloco vizinho = chunkVizinho[localX][ny][localZ];
		return vizinho == null || !(vizinho instanceof Agua) && !(vizinho instanceof Bloco);
	}
	
	// gera ou carrega chunks ja existentes
    public Bloco[][][] carregarChunk(int chunkX, int chunkY) {
        final ChunkCoord chave = new ChunkCoord(chunkX, chunkY);
        if(chunksCarregados.containsKey(chave)) return chunksCarregados.get(chave);
        else {
            final Bloco[][][] chunk = gerarChunk(chunkX, chunkY);
            chunksCarregados.put(chave, chunk);
            return chunk;
        }
    }

	public void adicionarEstrutura(int x, int y, int z, String json, Bloco[][][] chunk) {
		String jsonString = json;

		if(!json.equals("")) jsonString = json;
		try {
			JSONObject estrutura = new JSONObject(jsonString);

			JSONArray blocos = estrutura.getJSONArray("blocos");

			for(int i = 0; i < blocos.length(); i++) {
				JSONObject bloco = blocos.getJSONObject(i);

				int bx = bloco.getInt("x") + x;
				int by = bloco.getInt("y") + y + 1;
				int bz = bloco.getInt("z") + z;

				addBloco(bx, by, bz, bloco.getString("tipo"), chunk);
			}
		} catch(Exception e) {
			System.out.println("erro ao carregar o json estrutura: "+e);
		}
	}

	public boolean spawnEstrutura(float chance, int x, int z, int seed) {
		float noise = PerlinNoise2D.ruido(x * 0.1f, z * 0.1f, seed + 1000);
		float normalizedo = (noise + 1f) / 2f;
		return normalizedo < chance;
	}
	
	public void definirBiomas() {
		// planicie
		BIOMAS.add(new Bioma(22f, 4f, // altura base e variação
				  0.03f, 0.14f, // escalas 2D e 3D
				  "GRAMA", "TERRA", "PEDRA", // camadas
				  0.12f)); // limite de cavernas
		// deserto
		BIOMAS.add(new Bioma(20f, 5f,
				  0.04f, 0.1f,
				  "AREIA", "AREIA", "PEDRA",
				  0.01f));
		// montanha
		BIOMAS.add(new Bioma(24f, 10f,
				  0.02f, 0.16f,
				  "PEDRA", "PEDRA", "PEDRA",
				  0.15f));
		// floresta
		BIOMAS.add(new Bioma(22f, 3f,
				  0.05f, 0.1f,
				  "GRAMA", "TERRA", "PEDRA",
				  0.15f));
		// lago
		BIOMAS.add(new Bioma(20f, 0.1f,
				0.001f, 0.12f,
			    "AGUA", "AREIA", "PEDRA",
				 0.1f));
		// floresta de lagos
		BIOMAS.add(new Bioma(22f, 3f,
				  0.05f, 0.1f,
				  "GRAMA", "TERRA", "PEDRA",
				  0.15f));
	}

	public void destruirBloco(final float globalX, final float y, final float globalZ, final Player player) {
		final int chunkX = (int) Math.floor(globalX / (float) CHUNK_TAMANHO);
		final int chunkZ = (int) Math.floor(globalZ / (float) CHUNK_TAMANHO);
		final ChunkCoord chaveChunk = new ChunkCoord(chunkX,  chunkZ);

		final Bloco[][][] chunk = carregarChunk(chunkX, chunkZ);

		final int intY = (int) y;
		final int localX = (int) (globalX - (chunkX * CHUNK_TAMANHO));
		final int localZ = (int) (globalZ - (chunkZ * CHUNK_TAMANHO));

		if(y < 0 || y >= MUNDO_LATERAL || localX < 0 || localX >= CHUNK_TAMANHO || localZ < 0 || localZ >= CHUNK_TAMANHO) return;

		final Bloco blocoExistente = chunk[localX][intY][localZ];

		if(blocoExistente == null) return;

		player.inventario.get(0).tipo = blocoExistente.id;
		player.inventario.get(0).quant += 1;
		chunk[localX][intY][localZ] = null;

		if(chunksAtivos.containsKey(chaveChunk)) {
			chunksAlterados.put(chaveChunk, true);
			if(chunksModificados.containsKey(chaveChunk)) chunksModificados.remove(chaveChunk);
			chunksModificados.put(chaveChunk, chunk);
		}
	}

	public void colocarBloco(final float globalX, final float y, final float globalZ,  final Player player) {
		int chunkX = (int) Math.floor(globalX / (float) CHUNK_TAMANHO);
		int chunkZ = (int) Math.floor(globalZ / (float) CHUNK_TAMANHO);
		final ChunkCoord chaveChunk = new ChunkCoord(chunkX, chunkZ);
		// carrega ou gera o chunk correspondente
		Bloco[][][] chunk = carregarChunk(chunkX, chunkZ);

		int intY = (int) y;
		int localX = (int) (globalX - (chunkX * CHUNK_TAMANHO));
		int localZ = (int) (globalZ - (chunkZ * CHUNK_TAMANHO));

		if(y < 0 || y >= MUNDO_LATERAL || localX < 0 || localX >= CHUNK_TAMANHO || localZ < 0 || localZ >= CHUNK_TAMANHO) return;

		Bloco blocoExistente = chunk[localX][intY][localZ];
		if(blocoExistente != null) return;
		// define o bloco
		if(player.inventario.get(0).quant >= 0)chunk[localX][intY][localZ] = criarBloco((int) globalX, (int) y, (int) globalZ, player.itemMao);
		// se o chunk estiver ativo marca como alterado para atualizacao da VBO
		if(chunksAtivos.containsKey(chaveChunk)) {
			chunksAlterados.put(chaveChunk, true);
			if(chunksModificados.containsKey(chaveChunk)) chunksModificados.remove(chaveChunk);
			chunksModificados.put(chaveChunk, chunk);
		}
	}

	public void addBloco(final float globalX, final float y, final float globalZ,  final String tipo, final Bloco[][][] chunk) {
		int chunkX = (int) Math.floor(globalX / (float) CHUNK_TAMANHO);
		int chunkZ = (int) Math.floor(globalZ / (float) CHUNK_TAMANHO);

		int intY = (int) y;
		int localX = (int) (globalX - (chunkX * CHUNK_TAMANHO));
		int localZ = (int) (globalZ - (chunkZ * CHUNK_TAMANHO));

		if(y < 0 || y >= MUNDO_LATERAL || localX < 0 || localX >= CHUNK_TAMANHO || localZ < 0 || localZ >= CHUNK_TAMANHO) return;

		Bloco blocoExistente = chunk[localX][intY][localZ];
		if(blocoExistente != null) return;

		chunk[localX][intY][localZ] = criarBloco((int) globalX, (int) y, (int) globalZ, tipo);
	}

	public boolean noChao(Player camera) {
		float posPes = camera.pos[1];

		float yTeste = posPes - 0.1f;
		int by = (int) Math.floor(yTeste);

		float metadeLargura = camera.hitbox[1];
		int bx1 = (int) Math.floor(camera.pos[0] - metadeLargura);
		int bx2 = (int) Math.floor(camera.pos[0] + metadeLargura);
		int bz1 = (int) Math.floor(camera.pos[2] - metadeLargura);
		int bz2 = (int) Math.floor(camera.pos[2] + metadeLargura);

		for(int bx = bx1; bx <= bx2; bx++) {
			for(int bz = bz1; bz <= bz2; bz++) if(eBlocoSolido(bx, by, bz)) return true;
		}
		return false;
	}

	public boolean eBlocoSolido(int bx, int by, int bz) {
		if(by < 0 || by >= MUNDO_LATERAL) return false;
		int chunkX = (int) Math.floor(bx / (float) CHUNK_TAMANHO);
		int chunkZ = (int) Math.floor(bz / (float) CHUNK_TAMANHO);
		Bloco[][][] chunk = chunksAtivos.get(new ChunkCoord(chunkX, chunkZ));
		if(chunk == null) return false;
		int localX = bx - chunkX * CHUNK_TAMANHO;
		int localZ = bz - chunkZ * CHUNK_TAMANHO;
		if(localX < 0 || localX >= CHUNK_TAMANHO || localZ < 0 || localZ >= CHUNK_TAMANHO) return false;
		Bloco bloco = chunk[localX][by][localZ];
		return bloco != null && !(bloco instanceof Agua);
	}

	public float[] verificarColisao(Player cam, float tx, float ty, float tz) {
		float[] pos = cam.pos;
		float h = cam.hitbox[0]; //altura
		float r = cam.hitbox[1];  //metade da largura
		// processa cada eixo separadamente na ordem correta
		float[] novo = {tx, pos[1], pos[2]};
		novo = ajustarColisao(cam, novo, r, h, 0); // X primeiro
		novo[2] = tz;
		novo = ajustarColisao(cam, novo, r, h, 2); // Z segundo
		novo[1] = ty;
		novo = ajustarColisao(cam, novo, r, h, 1); // Y por ultimo
		return novo;
	}

	public float[] ajustarColisao(Camera3D cam, float[] pos, float raio, float altura, int eixo) {
		float[] original = cam.pos;
		float[] testePos = pos.clone();

		final int PASSOS = 3;
		float incremento = (pos[eixo] - original[eixo]) / PASSOS;

		for(int i = 1; i <= PASSOS; i++) {
			testePos[eixo] = original[eixo] + incremento * i;
			if(!colidiria(testePos[0], testePos[1], testePos[2], altura, raio)) continue;
			// colisao detectada, volta ao ultimo passo valido
			testePos[eixo] = original[eixo] + incremento * (i - 1);
			break;
		}
		return testePos;
	}

	public boolean colidiria(float cx, float cy, float cz, float altura, float raio) {
		float pe = cy;
		float cabeca = pe + altura;
		for(int x = -1; x <= 1; x++) {
			for(int z = -1; z <= 1; z++) {
				float px = cx + x * raio;
				float pz = cz + z * raio;
				for(float py = pe; py <= cabeca; py += altura / 2f) {
					int bx = (int) px;
					int by = (int) py;
					int bz = (int) pz;
					if(eBlocoSolido(bx, by, bz)) return true;
				}
			}
		}
		return false;
	}
}
