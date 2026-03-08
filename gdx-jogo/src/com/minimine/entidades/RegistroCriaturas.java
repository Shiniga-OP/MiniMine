package com.minimine.entidades;

import com.badlogic.gdx.files.FileHandle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
/*
 * carrega todos os .json de uma pasta e organiza por chave e bioma
 * usado por GerenciadorEntidades para nascer
 */
public final class RegistroCriaturas {
    public final Map<String, DadosCriatura> criaturas = new LinkedHashMap<>();

    public void carregar(FileHandle pasta) {
        FileHandle[] arquivos = pasta.list(".json");
        if(arquivos == null || arquivos.length == 0)
            throw new RuntimeException("nenhum mob encontrado em: " + pasta.path());
        for(FileHandle a : arquivos) {
            DadosCriatura dados = DadosCriatura.compilar(a.readString("UTF-8"));
            criaturas.put(dados.nome, dados);
        }
    }

    // todos os mobs que spawnam no bioma dado
    public List<DadosCriatura> paraOBioma(String bioma) {
        List<DadosCriatura> resultado = new ArrayList<>();
        for(DadosCriatura m : criaturas.values()) {
            for(String b : m.biomасOrigens) {
                if(b.equalsIgnoreCase(bioma)) {
					resultado.add(m);
					break;
				}
            }
        }
        return resultado;
    }

    public DadosCriatura obter(String nome) {
        DadosCriatura m = criaturas.get(nome);
        if(m == null) throw new RuntimeException("mob desconhecido: " + nome);
        return m;
    }

    public Collection<DadosCriatura> todos() { return criaturas.values(); }
    public int total() {return criaturas.size();}
}

