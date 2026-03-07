package com.minimine.entidades;

import com.minimine.utils.MJson;
import com.badlogic.gdx.Gdx;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

/*
 * dados imutaveis de um tipo de criatura, compilados de JSON
 * sem logica de comportamento, so estrutura de dados
 */
public final class DadosCriatura {
    public final String nome;
    public final float raridade;
    public final String modelo;

    // hitbox
    public final float largura, altura, profundidade;

    // movimento base
    public final float velo, peso, pulo;

    // variáveis internas, nome -> {valor, min, max}
    public final Map<String, float[]> variaveis; // float[]{valorInicial, min, max}

    // nomes das animações declaradas
    public final String[] animacoes;

    // regras de comportamento em ordem de prioridade
    public final List<Regra> comportamento;

    // nascimento
    public final String[] biomасOrigens;
    public final float chanceNascimento;
    public final int maxNascimento;

    public static final class Regra {
        public final List<Object> condicao; // ["naAgua","andando"] ou ["sede",">",85]
        public final Map<String, Object> exec;

        public Regra(List<Object> condicao, Map<String, Object> exec) {
            this.condicao = condicao;
            this.exec = exec;
        }
    }

    public DadosCriatura(String nome, float raridade, String modelo,
	float largura, float altura, float profundidade,
	float velo, float peso, float pulo,
	Map<String, float[]> variaveis, String[] animacoes,
	List<Regra> comportamento, String[] biomasOrigens, float chanceNascimento, int maxNascimento) {
        this.nome = nome;
        this.raridade = raridade;
        this.modelo = modelo;
        this.largura = largura;
        this.altura = altura;
        this.profundidade = profundidade;
        this.velo = velo;
        this.peso = peso;
        this.pulo = pulo;
        this.variaveis = variaveis;
        this.animacoes = animacoes;
        this.comportamento = comportamento;
        this.biomасOrigens = biomasOrigens;
        this.chanceNascimento = chanceNascimento;
        this.maxNascimento = maxNascimento;
    }

    public static DadosCriatura compilar(String json) {
        Map<String, Object> r = MJson.praObjeto(MJson.analisar(json));

        String nome = MJson.obterString(r, "nome", "desconhecido");
        float raridade = MJson.obterFloat(r, "raridade", 0.5f);
        String modelo = MJson.obterString(r, "modelo", "");

        // hitbox
        Map<String, Object> hb = MJson.praObjeto(r.get("hitbox"));
        float largura = MJson.obterFloat(hb, "largura", 0.6f);
        float altura = MJson.obterFloat(hb, "altura", 1.8f);
        float profundidade = MJson.obterFloat(hb, "profundidade", 0.6f);

        // movimento
        Map<String, Object> mv = MJson.praObjeto(r.get("movimento"));
        float velo = MJson.obterFloat(mv, "velo",  5f);
        float peso = MJson.obterFloat(mv, "peso",  65f);
        float pulo = MJson.obterFloat(mv, "pulo",  10f);

        // variaveis
        Map<String, float[]> variaveis = new LinkedHashMap<>();
        Object varObj = r.get("variaveis");
        if(varObj != null) {
            Map<String, Object> vars = MJson.praObjeto(varObj);
            for(Map.Entry<String, Object> e : vars.entrySet()) {
                Map<String, Object> v = MJson.praObjeto(e.getValue());
                float val = MJson.obterFloat(v, "valor", 0f);
                float min = MJson.obterFloat(v, "min", 0f);
                float max = MJson.obterFloat(v, "max", 100f);
                variaveis.put(e.getKey(), new float[]{val, min, max});
            }
        }
        // animacoes
        String[] animacoes = new String[0];
        Object animObj = r.get("animacoes");
        if(animObj != null) {
            List<Object> lista = MJson.praArray(animObj);
            animacoes = new String[lista.size()];
            for(int i = 0; i < lista.size(); i++) animacoes[i] = MJson.praString(lista.get(i));
        }
        // comportamento
        List<Regra> regras = new java.util.ArrayList<>();
        Object compObj = r.get("comportamento");
        if(compObj != null) {
            for(Object item : MJson.praArray(compObj)) {
                Map<String, Object> reg = MJson.praObjeto(item);
                List<Object> cond = MJson.praArray(reg.get("condicao"));
                Map<String, Object> exec = MJson.praObjeto(reg.get("exec"));
                regras.add(new Regra(cond, exec));
            }
        }
        // origem
        String[] biomasOrigens = new String[0];
        float chanceNascimento = 0f;
        int   maxNascimento    = 1;
        Object origObj = r.get("origem");
        if(origObj != null) {
            Map<String, Object> orig = MJson.praObjeto(origObj);
            List<Object> blist = MJson.praArray(orig.get("biomas"));
            biomasOrigens = new String[blist.size()];
            for(int i = 0; i < blist.size(); i++) biomasOrigens[i] = MJson.praString(blist.get(i));
            chanceNascimento = MJson.obterFloat(orig, "chance", 0f);
            maxNascimento = MJson.obterInt(orig, "max", 1);
        }
        Gdx.app.log("[DadosCriatura]", "carregado: " + nome);
        return new DadosCriatura(nome, raridade, modelo,
		largura, altura, profundidade,
		velo, peso, pulo,
		variaveis, animacoes, regras,
		biomasOrigens, chanceNascimento, maxNascimento);
    }
}

