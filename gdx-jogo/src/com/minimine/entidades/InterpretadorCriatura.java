package com.minimine.entidades;

import com.minimine.mundo.Mundo;
import com.minimine.mundo.blocos.Bloco;
import com.badlogic.gdx.graphics.g3d.utils.AnimationController;
import java.util.List;
import java.util.Map;
import com.minimine.utils.MJson;
/*
 * avalia as regras de comportamento definidas no JSON e aplica
 * os efeitos sobre o Criatura
 
 * condições suportadas:
 *   flags booleanas : "naAgua", "noChao", "andando", "parado"
 *   negação: "!naAgua", "!noChao", e etc
 *   comparação var: ["sede", ">", 85]  (>, <, >=, <=, ==, !=)
 *   bloco embaixo: ["bloco", "==", "agua"]
 
 * ações suportadas (dentro de "exec"):
 *   tocarAnim: nome da animação no GLTF
 *   recompensa: valor float adicionado à recompensa pendente da IA
 *   procurarBloco: lista de blocos alvo, define destino no criatura
 *   operacao: {variavel, operador(+,-,*,/), valor}
 */
public final class InterpretadorCriatura {
    public static void avaliar(Criatura criatura, float delta) {
        for(DadosCriatura.Regra regra : criatura.dados.comportamento) {
            if(avaliarCondicao(regra.condicao, criatura)) {
                executar(regra.exec, criatura, delta);
            }
        }
    }

    public static boolean avaliarCondicao(List<Object> cond, Criatura criatura) {
        if(cond == null || cond.isEmpty()) return true;

        // condição composta com variável: ["sede", ">", 85]
        if(cond.size() == 3) {
            String esq = String.valueOf(cond.get(0));
            String op  = String.valueOf(cond.get(1));
            float dir = praFloat(cond.get(2));

            // variável interna
            if(criatura.variaveis.containsKey(esq)) {
                float val = criatura.variaveis.get(esq);
                return comparar(val, op, dir);
            }
            // estado do bloco sob os pes
            if("bloco".equals(esq)) {
                String blocoAtual = obterBlocoSobPes(criatura)+"";
                return avaliarComparacao(blocoAtual, op, String.valueOf(cond.get(2)));
            }
            return false;
        }
        // todas as marcações devem ser verdadeiras
        for(Object item : cond) {
            if(!avaliarFlag(String.valueOf(item), criatura)) return false;
        }
        return true;
    }

    public static boolean avaliarFlag(String marca, Criatura criatura) {
        boolean negado = marca.startsWith("!");
        String  chave = negado ? marca.substring(1) : marca;
        boolean valor;
        switch(chave) {
            case "naAgua":  valor = criatura.naAgua;  break;
            case "noChao":  valor = criatura.noChao;  break;
            case "andando": valor = criatura.movendo; break;
            case "parado":  valor = !criatura.movendo; break;
            default:
                // variavel booleana: >0 = true
                Float v = criatura.variaveis.get(chave);
                valor = v != null && v > 0f;
                break;
        }
        return negado ? !valor : valor;
    }

    public static boolean comparar(float val, String op, float dir) {
        switch(op) {
            case ">":  return val >  dir;
            case "<":  return val <  dir;
            case ">=": return val >= dir;
            case "<=": return val <= dir;
            case "==": return val == dir;
            case "!=": return val != dir;
            default:   return false;
        }
    }

    public static boolean avaliarComparacao(String val, String op, String alvo) {
        if("==".equals(op)) return alvo.equals(val);
        if("!=".equals(op)) return !alvo.equals(val);
        return false;
    }

    public static CharSequence obterBlocoSobPes(Criatura criatura) {
        int x = (int)criatura.posicao.x;
        int y = (int)criatura.posicao.y - 1;
        int z = (int)criatura.posicao.z;
        int id = Mundo.obterBlocoMundo(x, y, z);
        if(id == 0) return "ar";
        Bloco b = Bloco.numIds.get(id);
        return b != null ? b.nome : "desconhecido";
    }

    public static void executar(Map<String, Object> exec, Criatura criatura, float delta) {
        if(exec == null) return;

        // tocarAnim
        Object anicriaturaj = exec.get("tocarAnim");
        if(anicriaturaj != null && criatura.animCtr != null) {
            String anim = String.valueOf(anicriaturaj);
            if(!anim.equals(criatura.animAtual)) {
                criatura.animCtr.setAnimation(anim, -1);
                criatura.animAtual = anim;
            }
        }
        // recompensa
        Object recObj = exec.get("recompensa");
        if(recObj != null) {
            criatura.recompensaPendente += praFloat(recObj) * delta;
        }
        // procurarBloco
        Object procObj = exec.get("procurarBloco");
        if(procObj != null) {
            List<Object> lista = MJson.praArray(procObj);
            String[] alvos = new String[lista.size()];
            for(int i = 0; i < lista.size(); i++) alvos[i] = String.valueOf(lista.get(i));
            criatura.definirDestino(alvos);
        }
        // operacao sobre variavel
        Object opObj = exec.get("operacao");
        if(opObj != null) {
            Map<String, Object> op = MJson.praObjeto(opObj);
            String varNome = String.valueOf(op.get("variavel"));
            String operador = String.valueOf(op.get("operador"));
            float valor = praFloat(op.get("valor"));

            Float atual = criatura.variaveis.get(varNome);
            if(atual != null) {
                float resultado;
                switch(operador) {
                    case "+": resultado = atual + valor * delta; break;
                    case "-": resultado = atual - valor * delta; break;
                    case "*": resultado = atual * valor; break;
                    case "/": resultado = valor != 0 ? atual / valor : atual; break;
                    default:  resultado = atual; break;
                }
                // respeita min/max definidos no JSON
                float[] faixa = criatura.faixasVariaveis.get(varNome);
                if(faixa != null) resultado = Math.max(faixa[0], Math.min(resultado, faixa[1]));
                criatura.variaveis.put(varNome, resultado);
            }
        }
    }

    public static float praFloat(Object obj) {
        if(obj instanceof Number) return ((Number)obj).floatValue();
        try {
			return Float.parseFloat(String.valueOf(obj));
		} catch(Exception e) {
			return 0f;
		}
    }
}

