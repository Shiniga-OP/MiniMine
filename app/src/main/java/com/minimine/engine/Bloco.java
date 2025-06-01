package com.minimine.engine;

import java.util.Map;
import java.util.HashMap;

public class Bloco {
    public int x, y, z;
	public String id;
    public String[] tipo;
	public boolean solido=true, liquido=false, gasoso=false;

    public Bloco(int x, int y, int z, String tipo) {
        this.x = x;
        this.y = y;
        this.z = z;
		if(tipo.equals("AR")) this.solido=false;
		this.tipo = TipoBloco.tipos.get(tipo);
		this.id = tipo;
    }

    public float[] obterVertices() {
        return new float[] {
            // face de frente
            x, y, z+1, x+1, y, z+1, x+1, y+1, z+1,
            x, y, z+1, x+1, y+1, z+1, x, y+1, z+1,

            // face de tras
            x, y, z, x+1, y, z, x+1, y+1, z,
            x, y, z, x+1, y+1, z, x, y+1, z,

            // face de cima
            x, y+1, z, x+1, y+1, z, x+1, y+1, z+1,
            x, y+1, z, x+1, y+1, z+1, x, y+1, z+1,

            // face de baixo
            x, y, z, x+1, y, z, x+1, y, z+1,
            x, y, z, x+1, y, z+1, x, y, z+1,

            // face esquerda
            x, y, z, x, y+1, z, x, y+1, z+1,
            x, y, z, x, y+1, z+1, x, y, z+1,

            // face direita
            x+1, y, z, x+1, y+1, z, x+1, y+1, z+1,
            x+1, y, z, x+1, y+1, z+1, x+1, y, z+1
        };
    }

    public float[] obterCoordenadas() {
        float[] coordenadas = new float[6 * 6 * 2];
        int faceIndice = 0;

        for(int i = 0; i < coordenadas.length; i += 12) {
            String texturaIndice = tipo[faceIndice];
            float u1 = 0, u2 = 1, v1 = 0, v2 = 1;

            if(texturaIndice.equals("-1") || texturaIndice.equals("0")) {
                u1 = v1 = 0;
                u2 = v2 = 1;
            } else {
				System.out.println("sei lá");
			}

            coordenadas[i] = u1; coordenadas[i+1] = v2;
            coordenadas[i+2] = u2; coordenadas[i+3] = v2;
            coordenadas[i+4] = u2; coordenadas[i+5] = v1;

            coordenadas[i+6] = u1; coordenadas[i+7] = v2;
            coordenadas[i+8] = u2; coordenadas[i+9] = v1;
            coordenadas[i+10] = u1; coordenadas[i+11] = v1;

            faceIndice++;
        }
        return coordenadas;
    }
}

class TipoBloco {
    public static Map<String, String[]> tipos = new HashMap<>();

    static {
        definirTipo("AR",
					"0", "0", "0",
					"0", "0", "0"
					);
        definirTipo("GRAMA",
					"texturas/grama_lado.png",
					"texturas/grama_lado.png",
					"texturas/grama_cima.png",
					"texturas/terra.png",
					"texturas/grama_lado2.png",
					"texturas/grama_lado2.png"
					);
		definirTipo("AREIA",
					"texturas/areia.png",
					"texturas/areia.png",
					"texturas/areia.png",
					"texturas/areia.png",
					"texturas/areia.png",
					"texturas/areia.png"
					);
		definirTipo("LAMA",
					"texturas/lama.png",
					"texturas/lama.png",
					"texturas/lama.png",
					"texturas/lama.png",
					"texturas/lama.png",
					"texturas/lama.png"
					);
        definirTipo("TERRA",
					"texturas/terra.png",
					"texturas/terra.png",
					"texturas/terra.png",
					"texturas/terra.png",
					"texturas/terra.png",
					"texturas/terra.png"
					);
        definirTipo("PEDRA",
					"texturas/pedra.png",
					"texturas/pedra.png",
					"texturas/pedra.png",
					"texturas/pedra.png",
					"texturas/pedra.png",
					"texturas/pedra.png"
					);
        definirTipo("PEDREGULHO",
					"texturas/pedregulho.png",
					"texturas/pedregulho.png",
					"texturas/pedregulho.png",
					"texturas/pedregulho.png",
					"texturas/pedregulho.png",
					"texturas/pedregulho.png"
					);
        definirTipo("TABUAS_CARVALHO",
					"texturas/tabuas_carvalho.png",
					"texturas/tabuas_carvalho.png",
					"texturas/tabuas_carvalho.png",
					"texturas/tabuas_carvalho.png",
					"texturas/tabuas_carvalho.png",
					"texturas/tabuas_carvalho.png"
					);
		definirTipo("TRONCO_CARVALHO",
					"texturas/tronco_carvalho.png",
					"texturas/tronco_carvalho.png",
					"texturas/tronco_carvalho_cima.png",
					"texturas/tronco_carvalho_cima.png",
					"texturas/tronco_carvalho.png",
					"texturas/tronco_carvalho.png"
					);
		definirTipo("FOLHAS",
					"texturas/folhas.png",
					"texturas/folhas.png",
					"texturas/folhas.png",
					"texturas/folhas.png",
					"texturas/folhas.png",
					"texturas/folhas.png"
					);
        definirTipo("BEDROCK",
					"texturas/bedrock.png",
					"texturas/bedrock.png",
					"texturas/bedrock.png",
					"texturas/bedrock.png",
					"texturas/bedrock.png",
					"texturas/bedrock.png"
					);
    }

    public static void definirTipo(String id, String... tipo) {
        tipos.put(id, tipo);
    }
}
