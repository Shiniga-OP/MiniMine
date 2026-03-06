package com.minimine.mundo.geracao;

import com.minimine.utils.MJson;
import java.util.List;
import java.util.Map;
import com.badlogic.gdx.Gdx;

public final class DadosBioma {

    public final String  chave;
    public final String  nome;
    public final float   raridade;
    // true = bioma aquatico(oceano, mar gelado, costeiro...)
    // usado pelo RegistroBiomas para separar os reusos terrestres/aquaticos
    public final boolean aquatico;

    public final Clima clima;
    public final Superficie superficie;
    public final Subsolo subsolo;
    public final Decoracoes decoracoes; // null se não tem decoracoes especiais
    public final Arvores arvores; // null se não tem arvores
    public final Plantas plantas; // null se não tem plantas

    public static final class Clima {
        public final float tempMin, tempMax;
        public final float umidMin, umidMax;

        public Clima(float tempMin, float tempMax, float umidMin, float umidMax) {
            this.tempMin = tempMin; this.tempMax = tempMax;
            this.umidMin = umidMin; this.umidMax = umidMax;
        }

        public boolean aceitaTemp(float t) { return t >= tempMin && t <= tempMax; }
        public boolean aceitaUmid(float u) { return u >= umidMin && u <= umidMax; }
    }

    public static final class Superficie {
        public final String topo;
        public final String subtopo;
        public final int prof;
        public final String interior;
        public final String fundoRioBloco;
        public final int profFundoPedra;

        public Superficie(String topo, String subtopo, int prof, String interior,
		String fundoRioBloco, int profFundoPedra) {
            this.topo = topo;
            this.subtopo = subtopo;
            this.prof = prof;
            this.interior = interior;
            this.fundoRioBloco = fundoRioBloco;
            this.profFundoPedra = profFundoPedra;
        }
    }

    public static final class Subsolo {
        public final float taxaRasa, escalaRasa;
        public final float taxaMedia, escalaMedia;
        public final float taxaProfunda, escalaProfunda;

        public Subsolo(
            float taxaRasa, float escalaRasa,
            float taxaMedia, float escalaMedia,
            float taxaProfunda, float escalaProfunda) {
            this.taxaRasa = taxaRasa;
			this.escalaRasa = escalaRasa;
            this.taxaMedia = taxaMedia;
			this.escalaMedia = escalaMedia;
            this.taxaProfunda = taxaProfunda;
			this.escalaProfunda = escalaProfunda;
        }
    }

    public static final class Decoracoes {
        public final float cactoChance;
        public final int cactoAltMax;
        public final boolean geloSuperficie;
        public final float icebergLimiar;
        public final int icebergAltMax;
        public final String icebergBlocoTopo;
        public final String icebergBlocoNucleo;

        public Decoracoes(float cactoChance, int cactoAltMax, boolean geloSuperficie,
		float icebergLimiar, int icebergAltMax, String icebergBlocoTopo, String icebergBlocoNucleo) {
            this.cactoChance = cactoChance;
            this.cactoAltMax = cactoAltMax;
            this.geloSuperficie = geloSuperficie;
            this.icebergLimiar = icebergLimiar;
            this.icebergAltMax = icebergAltMax;
            this.icebergBlocoTopo = icebergBlocoTopo;
            this.icebergBlocoNucleo = icebergBlocoNucleo;
        }

        public boolean temCacto() { return cactoChance > 0f && cactoAltMax > 0; }
        public boolean temIceberg() { return icebergLimiar > 0f && icebergAltMax > 0; }
    }

    public static final class Arvores {
        public final String tipo;  // "normal" | "conica"
        public final float  limite;

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

    public DadosBioma(
        String chave, String nome, float raridade, boolean aquatico,
        Clima clima, Superficie superficie, Subsolo subsolo,
        Decoracoes decoracoes, Arvores arvores, Plantas plantas) {
        this.chave = chave;
        this.nome = nome;
        this.raridade = raridade;
        this.aquatico = aquatico;
        this.clima = clima;
        this.superficie = superficie;
        this.subsolo = subsolo;
        this.decoracoes = decoracoes;
        this.arvores = arvores;
        this.plantas = plantas;
    }

    // compilador: JSON -> DadosBioma
    public static DadosBioma compilar(String chave, String json) {
        Map<String, Object> raiz = MJson.praObjeto(MJson.analisar(json));

        String nome = MJson.obterString(raiz, "nome", chave);
		Gdx.app.log("[DadosBioma]", "carregado: "+nome);
        float  raridade = MJson.obterFloat (raiz, "raridade", 0.5f);

        // "aquatico" pode vir direto no JSON ou inferido do legado "coluna.tipo"
        boolean aquatico = MJson.obterBool(raiz, "aquatico", false);
        if(!aquatico) {
            Object colunaLeg = raiz.get("coluna");
            if(colunaLeg != null) {
                Map<String, Object> col = MJson.praObjeto(colunaLeg);
                String tipoLeg = MJson.obterString(col, "tipo", "padrao");
                aquatico = "aquatico".equals(tipoLeg) || "gelado".equals(tipoLeg);
            }
        }
        Clima clima = compilarClima(MJson.praObjeto(raiz.get("clima")));
        Superficie superficie = compilarSuperficie(MJson.praObjeto(raiz.get("superficie")));
        Subsolo subsolo = compilarSubsolo(MJson.praObjeto(raiz.get("subsolo")));
        Decoracoes decoracoes = compilarDecoracoes(raiz);
        Arvores arvores = compilarArvores(raiz.get("arvores"));
        Plantas plantas = compilarPlantas(raiz.get("plantas"));

        return new DadosBioma(chave, nome, raridade, aquatico, clima, superficie, subsolo, decoracoes, arvores, plantas);
    }

    public static Clima compilarClima(Map<String, Object> obj) {
        Map<String, Object> t = MJson.praObjeto(obj.get("temperatura"));
        Map<String, Object> u = MJson.praObjeto(obj.get("umidade"));
        return new Clima(
            MJson.obterFloat(t, "min", 0f), MJson.obterFloat(t, "max", 1f),
            MJson.obterFloat(u, "min", 0f), MJson.obterFloat(u, "max", 1f)
        );
    }

    public static Superficie compilarSuperficie(Map<String, Object> obj) {
        return new Superficie(
            MJson.obterString(obj, "topo", "pedra"),
            MJson.obterString(obj, "subtopo", "pedra"),
            MJson.obterInt(obj, "prof", 3),
            MJson.obterString(obj, "interior", "pedra"),
            MJson.obterString(obj, "fundo_rio_bloco",  null),
            MJson.obterInt(obj, "prof_fundo_pedra", 0)
        );
    }

    public static Subsolo compilarSubsolo(Map<String, Object> obj) {
        Map<String, Object> cavs = MJson.praObjeto(obj.get("cavernas"));
        Map<String, Object> r = MJson.praObjeto(cavs.get("rasa"));
        Map<String, Object> m = MJson.praObjeto(cavs.get("media"));
        Map<String, Object> p = MJson.praObjeto(cavs.get("profunda"));
        return new Subsolo(
            MJson.obterFloat(r, "taxa", 0.08f), MJson.obterFloat(r, "escala", 0.025f),
            MJson.obterFloat(m, "taxa", 0.08f), MJson.obterFloat(m, "escala", 0.020f),
            MJson.obterFloat(p, "taxa", 0.06f), MJson.obterFloat(p, "escala", 0.015f)
        );
    }

    public static Decoracoes compilarDecoracoes(Map<String, Object> raiz) {
        float cactoChance = 0f;
        int cactoAltMax = 0;
        boolean geloSuperficie = false;
        float icebergLimiar = 0f;
        int icebergAltMax = 0;
        String icebergBlocoTopo = "gelo";
        String icebergBlocoNucleo = "pedra";

        Object decObj = raiz.get("decoracoes");
        if(decObj != null) {
            Map<String, Object> dec = MJson.praObjeto(decObj);

            Object cactoObj = dec.get("cacto");
            if(cactoObj != null) {
                Map<String, Object> cacto = MJson.praObjeto(cactoObj);
                cactoChance = MJson.obterFloat(cacto, "chance",     0.85f);
                cactoAltMax = MJson.obterInt  (cacto, "altura_max", 5);
            }
            geloSuperficie = MJson.obterBool(dec, "gelo_superficie", false);

            Object geloObj = dec.get("iceberg");
            if(geloObj != null) {
                Map<String, Object> gelo = MJson.praObjeto(geloObj);
                icebergLimiar = MJson.obterFloat(gelo, "limiar", 0.72f);
                icebergAltMax = MJson.obterInt(gelo, "altura_max", 20);
                icebergBlocoTopo = MJson.obterString(gelo, "bloco_topo", "gelo");
                icebergBlocoNucleo = MJson.obterString(gelo, "bloco_nucleo", "pedra");
            }
        }
		
		Object colunaObj = raiz.get("coluna");
        if(colunaObj != null) {
            Map<String, Object> col = MJson.praObjeto(colunaObj);
            String tipoLegado = MJson.obterString(col, "tipo", "padrao");

            // cacto vinha de coluna.cacto
            if("deserto".equals(tipoLegado) && cactoChance == 0f) {
                Object cactoObj = col.get("cacto");
                if(cactoObj != null) {
                    Map<String, Object> cacto = MJson.praObjeto(cactoObj);
                    cactoChance = MJson.obterFloat(cacto, "chance",     0.85f);
                    cactoAltMax = MJson.obterInt  (cacto, "altura_max", 5);
                } else {
                    cactoChance = 0.85f;
                    cactoAltMax = 5;
                }
            }

            // iceberg vinha de coluna.gelo.iceberg
            if ("gelado".equals(tipoLegado) && icebergLimiar == 0f) {
                geloSuperficie = true;
                Object geloObj = col.get("gelo");
                if (geloObj != null) {
                    Map<String, Object> gelo = MJson.praObjeto(geloObj);
                    Object iceObj = gelo.get("iceberg");
                    if (iceObj != null) {
                        Map<String, Object> ice = MJson.praObjeto(iceObj);
                        icebergLimiar      = MJson.obterFloat (ice, "limiar",       0.72f);
                        icebergAltMax      = MJson.obterInt   (ice, "altura_max",   20);
                        icebergBlocoTopo   = MJson.obterString(ice, "bloco_topo",   "gelo");
                        icebergBlocoNucleo = MJson.obterString(ice, "bloco_nucleo", "pedra");
                    }
                }
            }
        }
		
		boolean temAlgo = cactoChance > 0f || geloSuperficie || icebergLimiar > 0f;
        if (!temAlgo) return null;
		
        return new Decoracoes(cactoChance, cactoAltMax, geloSuperficie,
		icebergLimiar, icebergAltMax, icebergBlocoTopo, icebergBlocoNucleo);
    }

    public static Arvores compilarArvores(Object val) {
        if(val == null) return null;
        Map<String, Object> obj = MJson.praObjeto(val);
        return new Arvores(
            MJson.obterString(obj, "tipo", "normal"),
            MJson.obterFloat (obj, "limite", Float.MAX_VALUE)
        );
    }

    public static Plantas compilarPlantas(Object val) {
        if(val == null) return null;
        Map<String, Object> obj = MJson.praObjeto(val);
        List<Object> cru  = MJson.praArray(obj.get("lista"));
        String[] lista = new String[cru.size()];
        for(int i = 0; i < cru.size(); i++) lista[i] = MJson.praString(cru.get(i));
        return new Plantas(
            MJson.obterFloat(obj, "limite", Float.MAX_VALUE),
            MJson.obterFloat(obj, "limite_flor", Float.MAX_VALUE),
            lista
        );
    }
}

