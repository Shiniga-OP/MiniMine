package com.minimine.engine;

import java.util.Map;
import java.util.HashMap;

public class TipoBloco {
    public static Map<String, String[]> tipos = new HashMap<>();

    static {
        definirTipo("AR",
					"0", "0", "0",
					"0", "0", "0"
					);
        definirTipo("GRAMA",
					"blocos/grama_lado.png",
					"blocos/grama_lado.png",
					"blocos/grama_cima.png",
					"blocos/terra.png",
					"blocos/grama_lado2.png",
					"blocos/grama_lado2.png"
					);
		definirTipo("AREIA",
					"blocos/areia.png",
					"blocos/areia.png",
					"blocos/areia.png",
					"blocos/areia.png",
					"blocos/areia.png",
					"blocos/areia.png"
					);
		definirTipo("AGUA",
					"blocos/agua.png",
					"blocos/agua.png",
					"blocos/agua.png",
					"blocos/agua.png",
					"blocos/agua.png",
					"blocos/agua.png"
					);
		definirTipo("LAMA",
					"blocos/lama.png",
					"blocos/lama.png",
					"blocos/lama.png",
					"blocos/lama.png",
					"blocos/lama.png",
					"blocos/lama.png"
					);
        definirTipo("TERRA",
					"blocos/terra.png",
					"blocos/terra.png",
					"blocos/terra.png",
					"blocos/terra.png",
					"blocos/terra.png",
					"blocos/terra.png"
					);
        definirTipo("PEDRA",
					"blocos/pedra.png",
					"blocos/pedra.png",
					"blocos/pedra.png",
					"blocos/pedra.png",
					"blocos/pedra.png",
					"blocos/pedra.png"
					);
        definirTipo("PEDREGULHO",
					"blocos/pedregulho.png",
					"blocos/pedregulho.png",
					"blocos/pedregulho.png",
					"blocos/pedregulho.png",
					"blocos/pedregulho.png",
					"blocos/pedregulho.png"
					);
        definirTipo("TABUAS_CARVALHO",
					"blocos/tabuas_carvalho.png",
					"blocos/tabuas_carvalho.png",
					"blocos/tabuas_carvalho.png",
					"blocos/tabuas_carvalho.png",
					"blocos/tabuas_carvalho.png",
					"blocos/tabuas_carvalho.png"
					);
		definirTipo("TRONCO_CARVALHO",
					"blocos/tronco_carvalho.png",
					"blocos/tronco_carvalho.png",
					"blocos/tronco_carvalho_cima.png",
					"blocos/tronco_carvalho_cima.png",
					"blocos/tronco_carvalho.png",
					"blocos/tronco_carvalho.png"
					);
		definirTipo("FOLHAS",
					"blocos/folhas.png",
					"blocos/folhas.png",
					"blocos/folhas.png",
					"blocos/folhas.png",
					"blocos/folhas.png",
					"blocos/folhas.png"
					);
		definirTipo("CACTO",
					"blocos/cacto.png",
					"blocos/cacto.png",
					"blocos/cacto_cima.png",
					"blocos/cacto_cima.png",
					"blocos/cacto.png",
					"blocos/cacto.png"
					);
        definirTipo("BEDROCK",
					"blocos/bedrock.png",
					"blocos/bedrock.png",
					"blocos/bedrock.png",
					"blocos/bedrock.png",
					"blocos/bedrock.png",
					"blocos/bedrock.png"
					);
    }

    public static void definirTipo(String id, String... tipo) {
        tipos.put(id, tipo);
    }
}
