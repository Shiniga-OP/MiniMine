package com.minimine.mundo.geracao;

/*
 * tabela estatica de dados puros por bioma
 * sem logica, sem classes de gerador
 * o motor le esses dados e decide o que fazer
 * adicionar bioma novo: declarar ID + adicionar linha na tabela
 */
public final class TabelaBiomas {
    // ids
    public static final int OCEANO = 0;
    public static final int OCEANO_COSTEIRO = 1;
    public static final int OCEANO_QUENTE = 2;
    public static final int OCEANO_ABISSAL = 3;
    public static final int MAR_CONGELADO = 4;
    public static final int PLANICIES = 5;
    public static final int PLANICIES_MONTANHOSAS = 6;
    public static final int FLORESTA = 7;
    public static final int FLORESTA_COSTEIRA = 8;
    public static final int FLORESTA_MONTANHOSA = 9;
    public static final int DESERTO = 10;
    public static final int COLINAS_DESERTO = 11;
    public static final int TUNDRA = 12;
    public static final int PICOS_GELADOS = 13;
    public static final int RIO = 14;
    public static final int LAGO = 15;
    public static final int COLINAS_FLORESTAIS = 16;
    public static final int FLORESTA_LEITO = 17;

    public static final int TOTAL = 18;

    public static final DadosBioma[] TABELA = new DadosBioma[TOTAL];
	
    static {
        // topo subTopo lrofSub interior aquatico nivelAgua  tipoArvore  limArvore vegetacao                              limVeg          limFlor
        TABELA[OCEANO] = new DadosBioma("areia", "areia", 3, "pedra", true,  62, null,     Float.MAX_VALUE, null,                                         Float.MAX_VALUE, Float.MAX_VALUE);
        TABELA[OCEANO_QUENTE] = new DadosBioma("areia", "areia", 3, "pedra", true,  62, null,     Float.MAX_VALUE, null,                                         Float.MAX_VALUE, Float.MAX_VALUE);
        TABELA[OCEANO_ABISSAL] = new DadosBioma("pedra", "pedra", 4, "pedra", true,  62, null,     Float.MAX_VALUE, null,                                         Float.MAX_VALUE, Float.MAX_VALUE);
        TABELA[OCEANO_COSTEIRO] = new DadosBioma("areia", "areia", 3, "pedra", true,  62, null,     Float.MAX_VALUE, null,                                         Float.MAX_VALUE, Float.MAX_VALUE);
        TABELA[MAR_CONGELADO] = new DadosBioma("gelo",  "pedra", 3, "pedra", true,  62, null,     Float.MAX_VALUE, null,                                         Float.MAX_VALUE, Float.MAX_VALUE);
        TABELA[PLANICIES] = new DadosBioma("grama", "terra", 4, "pedra", false, 62, null,     Float.MAX_VALUE, new String[]{"capim", "tulipa", "iris_azul"}, 0.45f,           0.80f);
        TABELA[PLANICIES_MONTANHOSAS] = new DadosBioma("grama", "terra", 4, "pedra", false, 62, null,     Float.MAX_VALUE, new String[]{"capim", "tulipa", "iris_azul"}, 0.45f,           0.80f);
        TABELA[FLORESTA] = new DadosBioma("grama", "terra", 4, "pedra", false, 62, "normal",  0.65f,          new String[]{"capim", "iris_azul", "tulipa"}, 0.55f,           0.85f);
        TABELA[FLORESTA_COSTEIRA] = new DadosBioma("grama", "terra", 4, "pedra", false, 62, "normal",  0.65f,          new String[]{"capim", "iris_azul", "tulipa"}, 0.55f,           0.85f);
        TABELA[FLORESTA_MONTANHOSA] = new DadosBioma("grama", "terra", 3, "pedra", false, 62, "conica",  0.75f,          new String[]{"capim", "iris_azul"},           0.65f,           0.88f);
        TABELA[DESERTO] = new DadosBioma("areia", "areia", 5, "pedra", false, 62, null,     Float.MAX_VALUE, null,                                         Float.MAX_VALUE, Float.MAX_VALUE);
        TABELA[COLINAS_DESERTO] = new DadosBioma("areia", "areia", 5, "pedra", false, 62, null,     Float.MAX_VALUE, null,                                         Float.MAX_VALUE, Float.MAX_VALUE);
        TABELA[TUNDRA] = new DadosBioma("neve",  "terra", 4, "pedra", false, 62, null,     Float.MAX_VALUE, new String[]{"capim"},                        0.78f,           Float.MAX_VALUE);
        TABELA[PICOS_GELADOS] = new DadosBioma("neve",  "gelo",  2, "pedra", false, 62, null,     Float.MAX_VALUE, null,                                         Float.MAX_VALUE, Float.MAX_VALUE);

        TABELA[RIO] = TABELA[OCEANO_COSTEIRO];
        TABELA[LAGO] = TABELA[OCEANO];
        TABELA[COLINAS_FLORESTAIS] = TABELA[FLORESTA];
        TABELA[FLORESTA_LEITO] = TABELA[FLORESTA_COSTEIRA];
    }

    public static final String[] NOMES = {
        "Oceano", "Costa", "Mar Quente", "Oceano Abissal", "Mar Congelado",
        "Planície", "Planície Alta",
        "Floresta", "Mata Costeira", "Serrania",
        "Deserto", "Dunas",
        "Tundra", "Picos Gelados",
        "Rio", "Lago", "Colinas Florestais", "Floresta de Leito"
    };

    public static String nome(int id) {
        return (id >= 0 && id < NOMES.length) ? NOMES[id] : "Desconhecido";
    }
}

