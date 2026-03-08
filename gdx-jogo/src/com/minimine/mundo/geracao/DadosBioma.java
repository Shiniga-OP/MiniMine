package com.minimine.mundo.geracao;

import com.minimine.utils.MJson;
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
    public final Arvores arvores;
    public final Plantas plantas;

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

    public static final class Arvores {
        public final String tipo;
        public final float limite;

        public Arvores(String tipo, float limite) {
            this.tipo = tipo;
            this.limite = limite;
        }
    }

    public static final class Plantas {
        public final float limite;
        public final float limiteFlor;
        public final String[] lista;

        public Plantas(float limite, float limiteFlor, String[] lista) {
            this.limite = limite;
            this.limiteFlor = limiteFlor;
            this.lista = lista;
        }
    }

    public DadosBioma(String chave, String nome, float peso, int altMin, int altMax,
					  Clima clima, Superficie superficie, Arvores arvores, Plantas plantas) {
        this.chave = chave;
        this.nome = nome;
        this.peso = peso;
        this.altMin = altMin;
        this.altMax = altMax;
        this.clima = clima;
        this.superficie = superficie;
        this.arvores = arvores;
        this.plantas = plantas;
    }

    public static DadosBioma compilar(String chave, String json) {
        Map<String, Object> raiz = MJson.praObjeto(MJson.analisar(json));
        String nome = MJson.obterString(raiz, "nome", chave);
        Gdx.app.log("[DadosBioma]", "carregado: " + nome);
        float peso = MJson.obterFloat(raiz, "peso", 1.0f);
        int altMin = MJson.obterInt(raiz, "alt_min", 0);
        int altMax = MJson.obterInt(raiz, "alt_max", 255);
        Clima clima = compilarClima(MJson.praObjeto(raiz.get("clima")));
        Superficie superficie = compilarSuperficie(MJson.praObjeto(raiz.get("superficie")));
        Arvores arvores = compilarArvores(raiz.get("arvores"));
        Plantas plantas = compilarPlantas(raiz.get("plantas"));
        return new DadosBioma(chave, nome, peso, altMin, altMax, clima, superficie, arvores, plantas);
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
            MJson.obterInt(obj, "prof_fundo_pedra", 0)
        );
    }

    public static Arvores compilarArvores(Object val) {
        if(val == null) return null;
        Map<String, Object> obj = MJson.praObjeto(val);
        return new Arvores(
            MJson.obterString(obj, "tipo", "normal"),
            MJson.obterFloat(obj, "limite", Float.MAX_VALUE)
        );
    }

    public static Plantas compilarPlantas(Object val) {
        if(val == null) return null;
        Map<String, Object> obj = MJson.praObjeto(val);
        List<Object> cru = MJson.praArray(obj.get("lista"));
        String[] lista = new String[cru.size()];
        for(int i = 0; i < cru.size(); i++) lista[i] = MJson.praString(cru.get(i));
        return new Plantas(
            MJson.obterFloat(obj, "limite", Float.MAX_VALUE),
            MJson.obterFloat(obj, "limite_flor", Float.MAX_VALUE),
            lista
        );
    }
}


