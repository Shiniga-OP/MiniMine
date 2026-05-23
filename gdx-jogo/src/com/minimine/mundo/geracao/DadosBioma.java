package com.minimine.mundo.geracao;

import com.minimine.utils.MJson;
import com.minimine.utils.ArquivosUtil;
import com.minimine.mundo.blocos.Bloco;
import java.util.List;
import java.util.Map;
import com.badlogic.gdx.Gdx;

public final class DadosBioma {
    public final String chave;
    public final String nome;
    public final float peso;
    public final int altMin;
    public final int altMax;

    public final Clima clima;
    public final Superficie superficie;
    // estruturas naturais pré-compiladas(arvores, casas, etc)
    public final EntradaEstrutura[] estruturas;
    // vegetação de 1 bloco pré-compilada(grama, flores, etc)
    public final EntradaVegetacao[] vegetacao;

    // === classes internas ===
    public static final class Clima {
        public final float calor;
        public final float umidade;

        public Clima(float calor, float umidade) {
            this.calor = calor;
            this.umidade = umidade;
        }
    }

    public static final class Superficie {
        public final String topo;
        public final String subtopo;
        public final int profTopo;
        public final int profSubtopo;
        public final String interior;
        public final String fundoRioBloco;
        public final int profFundoPedra;

        public Superficie(String topo, String subtopo, int profTopo, int profSubtopo, String interior,
		String fundoRioBloco, int profFundoPedra) {
            this.topo = topo;
            this.subtopo = subtopo;
            this.profTopo = profTopo;
            this.profSubtopo = profSubtopo;
            this.interior = interior;
            this.fundoRioBloco = fundoRioBloco;
            this.profFundoPedra = profFundoPedra;
        }
    }
    /*
     * estrutura natural pré-compilada
     * blocoIds[i]: id numerico ja resolvido via Bloco.texIds
     * lx[i], ly[i], lz[i]: coordenadas locais relativas a ancora
     * ancX, ancY, ancZ: deslocamento da ancora
     * chance: probabilidade por coluna elegivel [0..1]
     */
    public static final class EntradaEstrutura {
        public final String nome;
        public final float chance;
        public final int[] blocoIds;
        public final short[] blocoMeta;
        public final int[] lx, ly, lz;
        public final int ancX, ancY, ancZ;
		public final int larg, prof;
		public final int blocoBaixo;

        public EntradaEstrutura(String nome, float chance,
		int[] blocoIds, short[] blocoMeta,
		int[] lx, int[] ly, int[] lz,
		int ancX, int ancY, int ancZ, int larg, int prof, int blocoBaixo) {
            this.nome = nome;
            this.chance = chance;
            this.blocoIds = blocoIds;
            this.blocoMeta = blocoMeta;
            this.lx = lx;
            this.ly = ly;
            this.lz = lz;
            this.ancX = ancX;
            this.ancY = ancY;
            this.ancZ = ancZ;
			this.larg = larg;
			this.prof = prof;
			this.blocoBaixo = blocoBaixo;
        }
    }
    /*
     * vegetação de 1 bloco pré-compilada
     * id: id numerico ja resolvido: direto para ChunkUtil.defBloco
     * chance: probabilidade por coluna elegivel [0..1]
     */
    public static final class EntradaVegetacao {
        public final int id;
        public final float chance;

        public EntradaVegetacao(int id, float chance) {
            this.id = id;
            this.chance = chance;
        }
    }

    public DadosBioma(String chave, String nome, float peso, int altMin, int altMax,
	Clima clima, Superficie superficie,
	EntradaEstrutura[] estruturas, EntradaVegetacao[] vegetacao) {
        this.chave = chave;
        this.nome = nome;
        this.peso = peso;
        this.altMin = altMin;
        this.altMax = altMax;
        this.clima = clima;
        this.superficie = superficie;
        this.estruturas = estruturas;
        this.vegetacao = vegetacao;
    }

    public static DadosBioma compilar(String chave, String json) {
        Map<String, Object> raiz = MJson.praObjeto(MJson.analisar(json));
        String nome = MJson.obterString(raiz, "nome", chave);
        float peso = MJson.obterFloat(raiz, "peso", 1.0f);
        int altMin = MJson.obterInt(raiz, "alt_min", 0);
        int altMax = MJson.obterInt(raiz, "alt_max", 255);
        Clima clima = compilarClima(MJson.praObjeto(raiz.get("clima")));
        Superficie superficie = compilarSuperficie(MJson.praObjeto(raiz.get("superficie")));
        EntradaEstrutura[] estruturas = compilarEstruturas(raiz.get("estruturas"));
        EntradaVegetacao[] vegetacao = compilarVegetacao(raiz.get("vegetacao"));
        return new DadosBioma(chave, nome, peso, altMin, altMax,
		clima, superficie, estruturas, vegetacao);
    }

    public static Clima compilarClima(Map<String, Object> obj) {
        return new Clima(
            MJson.obterFloat(obj, "calor", 0.5f),
            MJson.obterFloat(obj, "umidade", 0.5f)
        );
    }

    public static Superficie compilarSuperficie(Map<String, Object> obj) {
        return new Superficie(
            MJson.obterString(obj, "topo", "pedra"),
            MJson.obterString(obj, "subtopo", "pedra"),
            MJson.obterInt(obj, "prof_topo", 1),
            MJson.obterInt(obj, "prof_subtopo", 3),
            MJson.obterString(obj, "interior", "pedra"),
            MJson.obterString(obj, "fundo_rio_bloco", null),
            MJson.obterInt(obj, "prof_fundo_pedra",  0)
        );
    }
    /*
     * le o array "estruturas" do JSON:
     * [
     *   { "nome": "natural/arvore_1", "chance": 0.05 },
     *   { "nome": "natural/arvore_2", "chance": 0.03 }
     * ]
     * carrega o .minies via ArquivosUtil (internal assets) e resolve os IDs de bloco uma única vez
     */
    public static EntradaEstrutura[] compilarEstruturas(Object val) {
        if(val == null) return new EntradaEstrutura[0];
        List<Object> arr = MJson.praArray(val);
        if(arr == null || arr.isEmpty()) return new EntradaEstrutura[0];

        EntradaEstrutura[] resultado = new EntradaEstrutura[arr.size()];
        int conta = 0;

        for(int i = 0; i < arr.size(); i++) {
            Map<String, Object> obj = MJson.praObjeto(arr.get(i));
            String nomeArq = MJson.obterString(obj, "nome", null);
            float chance = MJson.obterFloat(obj, "chance", 0f);
			CharSequence blocoBaixo = MJson.obterString(obj, "blocoBaixo", null);

            if(nomeArq == null || chance <= 0f) continue;

            ArquivosUtil.DadosEstrutura d = ArquivosUtil.crEstrutura(nomeArq, false);
            if(d == null) {
                Gdx.app.log("[DadosBioma]", "[AVISO] estrutura não encontrada: " + nomeArq);
                continue;
            }
            int total = d.lx.length;
            int[] blocoIds  = new int[total];
            short[] blocoMeta = new short[total];

            for(int j = 0; j < total; j++) {
                String idStr = d.ids[j];
                if(idStr == null || "ar".equals(idStr)) {
                    blocoIds[j] = -1; // marcador: ignorar na colocação
                } else {
                    Bloco b = Bloco.texIds.get(idStr);
                    if(b == null) {
                        Gdx.app.log("[DadosBioma]", "[AVISO] bloco desconhecido em '" + nomeArq + "': " + idStr);
                        blocoIds[j] = -1;
                    } else {
                        blocoIds[j] = b.tipo;
                    }
                }
                blocoMeta[j] = d.meta[j];
            }
			int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
			int minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
			for(int j = 0; j < total; j++) {
				if(d.lx[j] < minX) minX = d.lx[j];
				if(d.lx[j] > maxX) maxX = d.lx[j];
				if(d.lz[j] < minZ) minZ = d.lz[j];
				if(d.lz[j] > maxZ) maxZ = d.lz[j];
			}
			int larg = maxX - minX + 1;
			int prof = maxZ - minZ + 1;
            resultado[conta++] = new EntradaEstrutura(
                nomeArq, chance,
                blocoIds, blocoMeta,
                d.lx, d.ly, d.lz,
                d.ancX, d.ancY, d.ancZ,
				larg, prof,
                blocoBaixo != null && Bloco.texIds.get(blocoBaixo) != null ? Bloco.texIds.get(blocoBaixo).tipo : -1
            );
        }
        if(conta == resultado.length) return resultado;
        EntradaEstrutura[] compactado = new EntradaEstrutura[conta];
        System.arraycopy(resultado, 0, compactado, 0, conta);
        return compactado;
    }
    /*
     * le o array "vegetacao" do JSON:
     * [
     *   { "nome": "grama_alta", "chance": 0.4 },
     *   { "nome": "flor_vermelha", "chance": 0.05 }
     * ]
     * resolve o ID de bloco uma unica vez
     */
    public static EntradaVegetacao[] compilarVegetacao(Object val) {
        if(val == null) return new EntradaVegetacao[0];
        List<Object> arr = MJson.praArray(val);
        if(arr == null || arr.isEmpty()) return new EntradaVegetacao[0];

        EntradaVegetacao[] resultado = new EntradaVegetacao[arr.size()];
        int conta = 0;

        for(int i = 0; i < arr.size(); i++) {
            Map<String, Object> obj = MJson.praObjeto(arr.get(i));
            String nomeBloco = MJson.obterString(obj, "nome", null);
            float chance = MJson.obterFloat(obj, "chance", 0f);
            if(nomeBloco == null || chance <= 0f) continue;

            Bloco b = Bloco.texIds.get(nomeBloco);
            if(b == null) {
                Gdx.app.log("[DadosBioma]", "[AVISO] bloco de vegetação desconhecido: " + nomeBloco);
                continue;
            }
            resultado[conta++] = new EntradaVegetacao(b.tipo, chance);
        }
        if(conta == resultado.length) return resultado;
        EntradaVegetacao[] compactado = new EntradaVegetacao[conta];
        System.arraycopy(resultado, 0, compactado, 0, conta);
        return compactado;
    }
}
