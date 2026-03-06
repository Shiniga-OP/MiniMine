package com.minimine.mundo.geracao;

import com.badlogic.gdx.files.FileHandle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RegistroBiomas {
    public final Map<String, DadosBioma> biomas = new LinkedHashMap<String, DadosBioma>();

    // cache separado por tipo de coluna para selecao rapida
    public final List<DadosBioma> terrestres = new ArrayList<DadosBioma>();
    public final List<DadosBioma> aquaticos  = new ArrayList<DadosBioma>();
    public DadosBioma padrao; // sempre o primeiro registrado
	
    // Carregamento
    public void carregarBiomas(FileHandle pasta) {
        FileHandle[] arquivos = pasta.list(".json");

        if(arquivos == null || arquivos.length == 0)
            throw new RuntimeException("nenhum bioma encontrado em: " + pasta.path());

        for(FileHandle a : arquivos) {
            String chave = a.nameWithoutExtension();
            String json = a.readString("UTF-8");
            DadosBioma bioma = DadosBioma.compilar(chave, json);
            biomas.put(chave, bioma);

            if(padrao == null) padrao = bioma;

            if(bioma.aquatico) aquaticos.add(bioma);
            else terrestres.add(bioma);
        }
    }
	
    public DadosBioma selecionar(float temp, float umidade, int altura, int nivelMar, boolean aquatico) {
        List<DadosBioma> reuso = aquatico ? aquaticos : terrestres;

        // passa 1: candidatos que encaixam em temp E umidade
        DadosBioma melhor = null;
        float melhorPon = -1f;

        for(int i = 0; i < reuso.size(); i++) {
            DadosBioma b = reuso.get(i);
            if(b.clima.aceitaTemp(temp) && b.clima.aceitaUmid(umidade)) {
                // dentro do range: pontua por raridade(maior raridade = mais comum = preferido)
                float pont = b.raridade;
                if(pont > melhorPon) {
                    melhorPon = pont;
                    melhor    = b;
                }
            }
        }
        if(melhor != null) return melhor;

        // passa 2: nenhum encaixou perfeitamente, pega o de menor distancia climatica
        float menorDist = Float.MAX_VALUE;
        for(int i = 0; i < reuso.size(); i++) {
            DadosBioma b = reuso.get(i);
            float dist = distClima(b, temp, umidade);
            if(dist < menorDist) {
                menorDist = dist;
                melhor = b;
            }
        }
        if(melhor != null) return melhor;

        return padrao;
    }

    public static float distClima(DadosBioma b, float temp, float umidade) {
        float dt = temp < b.clima.tempMin ? b.clima.tempMin - temp
			: temp > b.clima.tempMax ? temp - b.clima.tempMax : 0f;
        float du = umidade < b.clima.umidMin ? b.clima.umidMin - umidade
			: umidade > b.clima.umidMax ? umidade - b.clima.umidMax : 0f;
        return dt * dt + du * du;
    }

    public DadosBioma obter(String chave) {
        DadosBioma b = biomas.get(chave);
        if(b == null) throw new RuntimeException("bioma desconhecido: " + chave);
        return b;
    }

    public boolean existe(String chave) {
        return biomas.containsKey(chave);
    }

    public Collection<DadosBioma> todos() {
        return biomas.values();
    }

    public int total() {
        return biomas.size();
    }
}


