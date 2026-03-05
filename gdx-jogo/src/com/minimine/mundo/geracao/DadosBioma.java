package com.minimine.mundo.geracao;

// dados puros de um bioma
public final class DadosBioma {
    public final String blocoTopo;
    public final String blocoSubTopo;
    public final int profSubTopo;
    public final String blocoInterior;
    public final boolean aquatico;
    public final int nivelAgua;
    public final String tipoArvore; // "normal" | "larga" | "conica" | null
    public final float limiteArvore;
    public final String[] vegetacao;
    public final float limiteVegetacao;
    public final float limiteFlor;

    public DadosBioma(
		String blocoTopo, String blocoSubTopo, int profSubTopo, String blocoInterior,
		boolean aquatico, int nivelAgua,
		String tipoArvore, float limiteArvore,
		String[] vegetacao, float limiteVegetacao, float limiteFlor) {
        this.blocoTopo = blocoTopo;
        this.blocoSubTopo = blocoSubTopo;
        this.profSubTopo = profSubTopo;
        this.blocoInterior = blocoInterior;
        this.aquatico = aquatico;
        this.nivelAgua = nivelAgua;
        this.tipoArvore = tipoArvore;
        this.limiteArvore = limiteArvore;
        this.vegetacao = vegetacao;
        this.limiteVegetacao = limiteVegetacao;
        this.limiteFlor = limiteFlor;
    }
}

