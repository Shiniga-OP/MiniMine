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

public class Mundo {
    public int CHUNK_TAMANHO = 16; // padrao: 16, testes: 8
    public int MUNDO_LATERAL = 64; // padrao: 64, testes: 32
    public int RAIO_CARREGAMENTO = 5; // padrao: 3, testes: 2, inicial: 15

    public final int FACES_POR_BLOCO = 6;

	public int atlasTexturaId = -1;
	public Map<String, float[]> atlasUVMapa = new HashMap<>();

	public Map<String, Bloco[][][]> chunksAtivos = new ConcurrentHashMap<>();
	public Map<String, List<VBOGrupo>> chunkVBOs = new ConcurrentHashMap<>();
	public Map<String, Boolean> chunksAlterados = new HashMap<>();
	public Map<String, Bloco[][][]> chunksModificados = new HashMap<>();
    public Map<String, Bloco[][][]> chunksCarregados = new HashMap<>();
	public String nome = "novo mundo", tipo = "plano";
	public int seed;
	public String pacoteTex;
	
	public List<String> estruturas = new ArrayList<>();
	public List<Mob> mobs = new ArrayList<Mob>();
	
	public static final int PLANICIE = 0;
    public static final int DESERTO = 1;
    public static final int MONTANHA = 2;
    public static final int FLORESTA = 3;
	public static final int FLORESTA_LAGOAS = 5;
    public static final int LAGO = 4;

	Bioma[] BIOMAS = new Bioma[] {
		// planicie
		new Bioma(22f, 4f, // altura base e variação
		0.03f, 0.14f, // escalas 2D e 3D
		"GRAMA", "TERRA", "PEDRA", // camadas
		0.12f), // limite de cavernas
		// deserto
		new Bioma(20f, 5f,
		0.04f, 0.1f,
		"AREIA", "AREIA", "PEDRA",
		0.01f),
		// montanha
		new Bioma(24f, 10f,
		0.02f, 0.16f,
		"PEDRA", "PEDRA", "PEDRA",
		0.15f),
		// floresta
		new Bioma(22f, 3f,
		0.05f, 0.1f,
		"GRAMA", "TERRA", "PEDRA",
		0.15f),
		// floresta de lagos
		new Bioma(22f, 3f,
		0.05f, 0.1f,
		"GRAMA", "TERRA", "PEDRA",
		0.15f),
		// lago
		new Bioma(20f, 1f,
		0.01f, 0.12f,
		"AGUA", "AREIA", "PEDRA",
		0.1f)
	};
	
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
	
	public final Map<String, float[]> uvCache = new HashMap<String, float[]>(64);
	
	public final float[] UV_PADRAO = {0f, 0f, 1f, 1f};

	public Mundo(int seed, String nome, String tipo, String pacoteTex) {
		this.seed = seed;
		this.nome = nome;
	    this.tipo = tipo;
		this.pacoteTex = pacoteTex;
		this.definirEstruturas();
	}
	
	public Bloco[][][] gerarChunk(final int chunkX, final int chunkZ) {
		final int baseX = chunkX * CHUNK_TAMANHO;
		final int baseZ = chunkZ * CHUNK_TAMANHO;
		final Bloco[][][] chunk = new Bloco[CHUNK_TAMANHO][MUNDO_LATERAL][CHUNK_TAMANHO];

		if(tipo.equals("plano")) {
			for(int x = 0; x < CHUNK_TAMANHO; x++) {
				int globalX = baseX + x;
				for(int z = 0; z < CHUNK_TAMANHO; z++) {
					int globalZ = baseZ + z;
					for(int y = 0; y < 3; y++) {
						String tipoBloco = (y == 0) ? "BEDROCK" : (y < 2) ? "TERRA" : "GRAMA";
						try {
						chunk[x][y][z] = new Bloco(globalX, y, globalZ, tipoBloco);
						} catch(Exception e) {
							System.out.println("erro: "+e);
						}
					}
				}
			}
			return chunk;
		}

		int[][] alturas = new int[CHUNK_TAMANHO][CHUNK_TAMANHO];
		int[][] biomas = new int[CHUNK_TAMANHO][CHUNK_TAMANHO];

		for(int x = 0; x < CHUNK_TAMANHO; x++) {
			int globalX = baseX + x;
			for(int z = 0; z < CHUNK_TAMANHO; z++) {
				int globalZ = baseZ + z;
				float ruidoBioma = PerlinNoise2D.ruido(globalX * 0.01f, globalZ * 0.01f, seed);
				int biomaAtual;
				if(ruidoBioma < -0.5f) biomaAtual = DESERTO;
				else if(ruidoBioma < 0.01f) biomaAtual = FLORESTA_LAGOAS;
				else if(ruidoBioma < 0.1f) biomaAtual = PLANICIE;
				else if(ruidoBioma < 0.3f) biomaAtual = FLORESTA;
				else if(ruidoBioma < 0.6f) biomaAtual = MONTANHA;
				else biomaAtual = LAGO;
				biomas[x][z] = biomaAtual;

				Bioma b = BIOMAS[biomaAtual];
				float noise2D = PerlinNoise2D.ruido(globalX * b.escala2D, globalZ * b.escala2D, seed);
				alturas[x][z] = (int) (b.altBase + noise2D * b.variacao);
			
				int altura = alturas[x][z];
				biomaAtual = biomas[x][z];
				
				for(int y = 0; y < MUNDO_LATERAL; y++) {
					String tipoBloco = "AR";
					if (y == 0) tipoBloco = "BEDROCK";
					else if(y < altura - 4) tipoBloco = b.blocoCaver;
					else if(y < altura - 1) tipoBloco = b.blocoSub;
					else if(y < altura) tipoBloco = b.blocoSub;
					else if(y == altura) tipoBloco = b.blocoSup;
					if(y < altura - 5) {
						float noise3D = PerlinNoise3D.ruido(globalX * b.escala3D, y * b.escala3D, globalZ * b.escala3D, seed + 100);
						if (noise3D > BIOMAS[biomaAtual].caverna) tipoBloco = "AR";
					}
					if(!tipoBloco.equals("AR")) {
						try {
							chunk[x][y][z] = new Bloco(globalX, y, globalZ, tipoBloco);
						} catch(Exception e) {
							System.out.println("erro: "+e);
						}
					}
				}
			}
		}

		long sementeChunk = ((long) chunkX * 341873128712L) ^ ((long) chunkZ * 132897987541L) ^ seed;
		Random rnd = new Random(sementeChunk);

		int tentativasArvore = 3;
		int tentativasDeserto = 1;

		for(int i = 0; i < tentativasArvore; i++) {
			int xAle = rnd.nextInt(CHUNK_TAMANHO);
			int zAle = rnd.nextInt(CHUNK_TAMANHO);
			int biomaEscolhido = biomas[xAle][zAle];
			if(biomaEscolhido == FLORESTA) {
				int altura = alturas[xAle][zAle];
				int globalX = baseX + xAle;
				int globalZ = baseZ + zAle;
				adicionarEstrutura(globalX, altura, globalZ, estruturas.get(0), chunk);
			}
		}

		for(int i = 0; i < tentativasDeserto; i++) {
			int xAle = rnd.nextInt(CHUNK_TAMANHO);
			int zAle = rnd.nextInt(CHUNK_TAMANHO);
			int biomaEscolhido = biomas[xAle][zAle];
			if(biomaEscolhido == DESERTO) {
				int altura = alturas[xAle][zAle];
				int globalX = baseX + xAle;
				int globalZ = baseZ + zAle;
				adicionarEstrutura(globalX, altura, globalZ, estruturas.get(3), chunk);
			}
		}
		return chunk;
	}

	public Map<Integer, List<float[]>> calculoVBO(Bloco[][][] chunk) {
		boolean chunkVazio = true;
		loop:
		for(int x = 0; x < CHUNK_TAMANHO; x++) {
			for(int y = 0; y < MUNDO_LATERAL; y++) {
				for(int z = 0; z < CHUNK_TAMANHO; z++) {
					if(chunk[x][y][z] != null) {
						chunkVazio = false;
						break loop;
					}
				}
			}
		}
		if(chunkVazio) return new HashMap<>();
		Map<Integer, List<float[]>> dadosPorTextura = new HashMap<Integer, List<float[]>>(8);

		for(int x = 0; x < CHUNK_TAMANHO; x++) {
			for(int y = 0; y < MUNDO_LATERAL; y++) {
				for(int z = 0; z < CHUNK_TAMANHO; z++) {
					Bloco bloco = chunk[x][y][z];
					if(bloco == null || "0".equals(bloco.tipo[0])) continue;

					float[] vertices = bloco.obterVertices();
					for(int face = 0; face < 6; face++) {
						if(!faceVisivel(bloco.x, bloco.y, bloco.z, face)) continue;

						float[] dadosFace = new float[48];
						float[] normal = NORMAIS[face];
						int antes = face * 18;

						// Copia vértices + normais
						for(int v = 0; v < 6; v++) {
							int src = antes + v * 3;
							int dst = v * 8;
							System.arraycopy(vertices, src, dadosFace, dst, 3);
							System.arraycopy(normal, 0, dadosFace, dst + 3, 3);
						}

						// Resolve UVs via cache
						String recurso = bloco.tipo[face];
						float[] uv = uvCache.get(recurso);
						if(uv == null) {
							uv = atlasUVMapa.get(recurso);
							if(uv == null) uv = UV_PADRAO;
							uvCache.put(recurso, uv);
						}
						float u1 = uv[0], v1 = uv[1], u2 = uv[2], v2 = uv[3];

						dadosFace[6]  = u1; dadosFace[7]  = v2;
						dadosFace[14] = u2; dadosFace[15] = v2;
						dadosFace[22] = u2; dadosFace[23] = v1;
						dadosFace[30] = u1; dadosFace[31] = v2;
						dadosFace[38] = u2; dadosFace[39] = v1;
						dadosFace[46] = u1; dadosFace[47] = v1;

						int texId = atlasTexturaId;
						List<float[]> lista = dadosPorTextura.get(texId);
						if(lista == null) {
							lista = new ArrayList<float[]>(256);
							dadosPorTextura.put(texId, lista);
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

		// verificação vertical precoce
		if(ny < 0 || ny >= MUNDO_LATERAL) return true;

		int nx = x + DX[face];
		int nz = z + DZ[face];

		// calculo de chunk otimizado
		int chunkX = (nx >= 0) ? nx / CHUNK_TAMANHO : (nx + 1) / CHUNK_TAMANHO - 1;
		int chunkZ = (nz >= 0) ? nz / CHUNK_TAMANHO : (nz + 1) / CHUNK_TAMANHO - 1;

		// formação de chave eficiente
		String chaveChunk = String.valueOf(chunkX).concat(",").concat(String.valueOf(chunkZ));

		// busca direta no mapa
		Bloco[][][] chunkVizinho = chunksAtivos.get(chaveChunk);
		if(chunkVizinho == null) return true;

		// calculo de coordenadas locais
		int localX = nx - chunkX * CHUNK_TAMANHO;
		if(localX < 0) localX += CHUNK_TAMANHO;

		int localZ = nz - chunkZ * CHUNK_TAMANHO;
		if(localZ < 0) localZ += CHUNK_TAMANHO;

		// acesso seguro
		if(localX >= CHUNK_TAMANHO || localZ >= CHUNK_TAMANHO) return true;

		Bloco vizinho = chunkVizinho[localX][ny][localZ];
		return vizinho == null || "0".equals(vizinho.tipo[0]) || !vizinho.solido;
	}
	
	// gera ou carrega chunks ja existentes
    public Bloco[][][] carregarChunk(int chunkX, int chunkY) {
        final String chave = chunkX + "," + chunkY;
        if(chunksCarregados.containsKey(chave)) {
            return chunksCarregados.get(chave);
        }
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

	public boolean spawnEstrutura(float chanceSpawn, int x, int z, int seed) {
		float noise = PerlinNoise2D.ruido(x * 0.1f, z * 0.1f, seed + 1000);
		float normalized = (noise + 1f) / 2f;
		return normalized < chanceSpawn;
	}

	public void definirEstruturas() {
		String arvore1 = 
		    "{ "+
			"\"nome\": \"arvore\","+
			"\"blocos\": ["+
			"{\"x\":0, \"y\":0, \"z\":0, \"tipo\": \"TRONCO_CARVALHO\"},"+
			"{\"x\":0, \"y\":1, \"z\":0, \"tipo\": \"TRONCO_CARVALHO\"},"+
			"{\"x\":0, \"y\":2, \"z\":0, \"tipo\": \"TRONCO_CARVALHO\"},"+
			"{\"x\":0, \"y\":3, \"z\":0, \"tipo\": \"TRONCO_CARVALHO\"},"+
			"{\"x\":0, \"y\":4, \"z\":0, \"tipo\": \"TRONCO_CARVALHO\"},"+
			"{\"x\":-1, \"y\":4, \"z\":0, \"tipo\": \"FOLHAS\"},"+
			"{\"x\":1, \"y\":4, \"z\":0, \"tipo\": \"FOLHAS\"},"+
			"{\"x\":1, \"y\":4, \"z\":1, \"tipo\": \"FOLHAS\"},"+
			"{\"x\":0, \"y\":4, \"z\":1, \"tipo\": \"FOLHAS\"},"+
			"{\"x\":-1, \"y\":4, \"z\":-1, \"tipo\": \"FOLHAS\"},"+
			"{\"x\":0, \"y\":4, \"z\":-1, \"tipo\": \"FOLHAS\"},"+
			"{\"x\":-2, \"y\":4, \"z\":0, \"tipo\": \"FOLHAS\"},"+
			"{\"x\":2, \"y\":4, \"z\":0, \"tipo\": \"FOLHAS\"},"+
			"{\"x\":-1, \"y\":5, \"z\":0, \"tipo\": \"FOLHAS\"},"+
			"{\"x\":1, \"y\":5, \"z\":0, \"tipo\": \"FOLHAS\"},"+
			"{\"x\":0, \"y\":5, \"z\":1, \"tipo\": \"FOLHAS\"},"+
			"{\"x\":0, \"y\":5, \"z\":-1, \"tipo\": \"FOLHAS\"},"+
			"{\"x\":0, \"y\":5, \"z\":0, \"tipo\": \"FOLHAS\"}"+
			"]"+
			"}";

		String arvore2 = 
		    "{ "+
			"\"nome\": \"arvore\","+
			"\"blocos\": ["+
			"{\"x\":0, \"y\":0, \"z\":0, \"tipo\": \"TRONCO_CARVALHO\"},"+
			"{\"x\":0, \"y\":1, \"z\":0, \"tipo\": \"TRONCO_CARVALHO\"},"+
			"{\"x\":0, \"y\":2, \"z\":0, \"tipo\": \"TRONCO_CARVALHO\"},"+
			"{\"x\":0, \"y\":3, \"z\":0, \"tipo\": \"TRONCO_CARVALHO\"},"+
			"{\"x\":0, \"y\":4, \"z\":0, \"tipo\": \"TRONCO_CARVALHO\"},"+
			"{\"x\":0, \"y\":5, \"z\":0, \"tipo\": \"TRONCO_CARVALHO\"},"+
			"{\"x\":-1, \"y\":5, \"z\":0, \"tipo\": \"FOLHAS\"},"+
			"{\"x\":1, \"y\":5, \"z\":0, \"tipo\": \"FOLHAS\"},"+
			"{\"x\":1, \"y\":5, \"z\":1, \"tipo\": \"FOLHAS\"},"+
			"{\"x\":0, \"y\":5, \"z\":1, \"tipo\": \"FOLHAS\"},"+
			"{\"x\":-1, \"y\":5, \"z\":-1, \"tipo\": \"FOLHAS\"},"+
			"{\"x\":0, \"y\":5, \"z\":-1, \"tipo\": \"FOLHAS\"},"+
			"{\"x\":-2, \"y\":5, \"z\":0, \"tipo\": \"FOLHAS\"},"+
			"{\"x\":2, \"y\":5, \"z\":0, \"tipo\": \"FOLHAS\"},"+
			"{\"x\":-1, \"y\":6, \"z\":0, \"tipo\": \"FOLHAS\"},"+
			"{\"x\":1, \"y\":6, \"z\":0, \"tipo\": \"FOLHAS\"},"+
			"{\"x\":0, \"y\":6, \"z\":1, \"tipo\": \"FOLHAS\"},"+
			"{\"x\":0, \"y\":6, \"z\":-1, \"tipo\": \"FOLHAS\"},"+
			"{\"x\":0, \"y\":6, \"z\":0, \"tipo\": \"FOLHAS\"}"+
			"]"+
			"}";

		String pedra1 =
			"{"+
			"\"blocos\": ["+
			"{\"x\": 0, \"y\": 0, \"z\": 0, \"tipo\": \"PEDRA\"},"+
			"{\"x\": 1, \"y\": 0, \"z\": 0, \"tipo\": \"PEDREGULHO\"},"+
			"{\"x\": 1, \"y\": 0, \"z\": 1, \"tipo\": \"PEDRA\"},"+
			"{\"x\": -1, \"y\": 0, \"z\": 0, \"tipo\": \"PEDREGULHO\"},"+
			"{\"x\": 1, \"y\": 0, \"z\": 1, \"tipo\": \"PEDREGULHO\"},"+
			"{\"x\": 0, \"y\": 0, \"z\": 1, \"tipo\": \"PEDREGULHO\"},"+
			"{\"x\": 0, \"y\": 1, \"z\": 0, \"tipo\": \"PEDRA\"},"+
			"{\"x\": 0, \"y\": 1, \"z\": 1, \"tipo\": \"PEDREGULHO\"}"+
			"]"+
			"}";
		String cacto =
			"{"+
			"\"blocos\": ["+
			"{\"x\": 0, \"y\": 0, \"z\": 0, \"tipo\": \"CACTO\"},"+
			"{\"x\": 0, \"y\": 1, \"z\": 0, \"tipo\": \"CACTO\"}"+
			"]"+
			"}";
		estruturas.add(arvore1);
		estruturas.add(arvore2);
		estruturas.add(pedra1);
		estruturas.add(cacto);
	}

	public void destruirBloco(final float globalX, final float y, final float globalZ, final Player player) {
		final int chunkX = (int) Math.floor(globalX / (float) CHUNK_TAMANHO);
		final int chunkZ = (int) Math.floor(globalZ / (float) CHUNK_TAMANHO);
		final String chaveChunk = chunkX + "," + chunkZ;

		final Bloco[][][] chunk = carregarChunk(chunkX, chunkZ);

		final int intY = (int) y;
		final int localX = (int) (globalX - (chunkX * CHUNK_TAMANHO));
		final int localZ = (int) (globalZ - (chunkZ * CHUNK_TAMANHO));

		if(y < 0 || y >= MUNDO_LATERAL || localX < 0 || localX >= CHUNK_TAMANHO || localZ < 0 || localZ >= CHUNK_TAMANHO) return;

		final Bloco blocoExistente = chunk[localX][intY][localZ];

		if(blocoExistente == null || blocoExistente.tipo[0].equals("0")) return;

		player.inventario.get(0).tipo = blocoExistente.id;
		player.inventario.get(0).quant += 1;
		chunk[localX][intY][localZ] = null;

		if(chunksAtivos.containsKey(chaveChunk)) {
			chunksAlterados.put(chaveChunk, true);
			chunksModificados.put(chaveChunk, chunk);
		}
	}

	public void colocarBloco(final float globalX, final float y, final float globalZ,  final Player player) {
		int chunkX = (int) Math.floor(globalX / (float) CHUNK_TAMANHO);
		int chunkZ = (int) Math.floor(globalZ / (float) CHUNK_TAMANHO);
		final String chaveChunk = chunkX + "," + chunkZ;

		// carrega ou gera o chunk correspondente
		Bloco[][][] chunk = carregarChunk(chunkX, chunkZ);

		int intY = (int) y;
		int localX = (int) (globalX - (chunkX * CHUNK_TAMANHO));
		int localZ = (int) (globalZ - (chunkZ * CHUNK_TAMANHO));

		if(y < 0 || y >= MUNDO_LATERAL || localX < 0 || localX >= CHUNK_TAMANHO || localZ < 0 || localZ >= CHUNK_TAMANHO) return;

		Bloco blocoExistente = chunk[localX][intY][localZ];
		if(blocoExistente != null && !blocoExistente.tipo[0].equals("0")) return;

		// define o bloco
		if(player.inventario.get(0).quant >= 0)chunk[localX][intY][localZ] = new Bloco((int) globalX, (int) y, (int) globalZ, player.itemMao);

		// se o chunk estiver ativo marca como alterado para atualizacao da VBO
		if(chunksAtivos.containsKey(chaveChunk)) {
			chunksAlterados.put(chaveChunk, true);
			chunksModificados.put(chaveChunk, chunk);
		}
	}

	public void addBloco(final float globalX, final float y, final float globalZ,  final String tipo, final Bloco[][][] chunk) {
		int chunkX = (int) Math.floor(globalX / (float) CHUNK_TAMANHO);
		int chunkZ = (int) Math.floor(globalZ / (float) CHUNK_TAMANHO);

		int intY = (int) y;
		int localX = (int) (globalX - (chunkX * CHUNK_TAMANHO));
		int localZ = (int) (globalZ - (chunkZ * CHUNK_TAMANHO));

		if(y < 0 || y >= MUNDO_LATERAL || localX < 0 || localX >= CHUNK_TAMANHO || localZ < 0 || localZ >= CHUNK_TAMANHO) {
			return;
		}

		Bloco blocoExistente = chunk[localX][intY][localZ];
		if(blocoExistente != null && !blocoExistente.tipo[0].equals("0")) {
			return;
		}

		try {
			chunk[localX][intY][localZ] = new Bloco((int) globalX, (int) y, (int) globalZ, tipo);
		} catch(Exception e) {
			System.out.println("erro: "+e);
		}
	}

	public boolean noChao(Camera3D camera) {
		float posPes = camera.posicao[1] - 1 - (camera.hitbox[0] / 2f);

		float yTeste = posPes - 0.1f;
		int by = (int) Math.floor(yTeste);

		float metadeLargura = camera.hitbox[1] / 2f;
		int bx1 = (int) Math.floor(camera.posicao[0] - metadeLargura);
		int bx2 = (int) Math.floor(camera.posicao[0] + metadeLargura);
		int bz1 = (int) Math.floor(camera.posicao[2] - metadeLargura);
		int bz2 = (int) Math.floor(camera.posicao[2] + metadeLargura);

		for(int bx = bx1; bx <= bx2; bx++) {
			for(int bz = bz1; bz <= bz2; bz++) {
				if(eBlocoSolido(bx, by, bz)) {
					return true;
				}
			}
		}
		return false;
	}

	public boolean eBlocoSolido(int bx, int by, int bz) {
		if(by < 0 || by >= MUNDO_LATERAL) return false;
		int chunkX = (int) Math.floor(bx / (float) CHUNK_TAMANHO);
		int chunkZ = (int) Math.floor(bz / (float) CHUNK_TAMANHO);
		Bloco[][][] chunk = chunksAtivos.get(chunkX + "," + chunkZ);
		if(chunk == null) return false;
		int localX = bx - chunkX * CHUNK_TAMANHO;
		int localZ = bz - chunkZ * CHUNK_TAMANHO;
		if(localX < 0 || localX >= CHUNK_TAMANHO || localZ < 0 || localZ >= CHUNK_TAMANHO) return false;
		try{
			Bloco bloco = chunk[localX][by][localZ];
			return bloco != null && bloco.solido;
		} catch(Exception e) {
			System.out.println("erro: "+e);
			return false;
		}
	}

	public void verificarColisao(Camera3D cam, float tx, float ty, float tz) {
		float[] pos = cam.posicao;
		float h = cam.hitbox[0];       //altura
		float r = cam.hitbox[1] / 2f;  //metade da largura

		// processa cada eixo separadamente na ordem correta
		float[] novo = {tx, pos[1], pos[2]};
		novo = ajustarColisao(cam, novo, r, h, 0); // X primeiro

		novo[2] = tz;
		novo = ajustarColisao(cam, novo, r, h, 2); // Z segundo

		novo[1] = ty;
		novo = ajustarColisao(cam, novo, r, h, 1); // Y por ultimo

		cam.posicao = novo;
	}

	public float[] ajustarColisao(Camera3D cam, float[] pos, float raio, float altura, int eixo) {
		float[] original = cam.posicao;
		float[] testePos = pos.clone();

		final int PASSOS = 3;
		float incremento = (pos[eixo] - original[eixo]) / PASSOS;

		for(int i = 1; i <= PASSOS; i++) {
			testePos[eixo] = original[eixo] + incremento * i;

			if(!colidiria(testePos[0], testePos[1], testePos[2], altura, raio)) {
				continue;
			}

			// colisao detectada, volta ao ultimo passo valido
			testePos[eixo] = original[eixo] + incremento * (i - 1);
			break;
		}
		return testePos;
	}

	public boolean colidiria(float cx, float cy, float cz, float altura, float raio) {
		float pe = cy - 1.5f;
		float cabeca = pe + altura;

		// verifica 9 pontos critcos na hitbox
		for(int x = -1; x <= 1; x++) {
			for(int z = -1; z <= 1; z++) {
				float px = cx + x * raio;
				float pz = cz + z * raio;

				//do pé à cabeça
				for(float py = pe; py <= cabeca; py += altura / 2f) {
					int bx = (int) Math.floor(px);
					int by = (int) Math.floor(py);
					int bz = (int) Math.floor(pz);

					if(eBlocoSolido(bx, by, bz)) {
						return true;
					}
				}
			}
		}
		return false;
	}
}
