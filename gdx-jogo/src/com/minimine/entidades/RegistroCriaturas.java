package com.minimine.entidades;

import com.badlogic.gdx.files.FileHandle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.minimine.utils.ArquivosUtil;
/*
 * carrega todos os .json de uma pasta e organiza por chave e bioma
 * usado por GerenciadorEntidades para nascer
*/
public final class RegistroCriaturas {
    public final Map<String, DadosCriatura> criaturas = new LinkedHashMap<>();

    public void carregar(FileHandle pasta) {
		final FileHandle[] arquivos = ArquivosUtil.listarAssets(pasta);
		if(arquivos == null || arquivos.length == 0) {
			throw new RuntimeException("nenhuma criatura encontrada em: " + pasta.path());
		}
		for(FileHandle a : arquivos) {
			final DadosCriatura dados = DadosCriatura.compilar(a.readString("UTF-8"));
			criaturas.put(dados.nome, dados);
		}
	}

    // todos os mobs que spawnam no bioma dado
    public List<DadosCriatura> paraOBioma(String bioma) {
        final List<DadosCriatura> resultado = new ArrayList<>();
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

    public final DadosCriatura obter(String nome) {
        final DadosCriatura m = criaturas.get(nome);
        if(m == null) throw new RuntimeException("criatura desconhecida: " + nome);
        return m;
    }

    public Collection<DadosCriatura> todos() { return criaturas.values(); }
    public int total() {return criaturas.size();}
}

