package com.minimine.mundo.geracao;
/*
 * arrays de trabalho por chamada de gerarChunk()
 
 * MotorGeracao, TerranoBase e GeradorRios são compartilhados entre threads
 * contem apenas parametros e objetos MapaRuido imutaveis apos construção
 * os arrays resultado[] do MapaRuido são o unico estado mutavel que causava
 * condição de corrida quando multiplas threads chamavam gerarChunk() simultaneamente
 
 * ContextoGeracao é alocado uma vez por thread via ThreadLocal em MotorGeracao
 * e passado explicitamente por todos os metodos de geração. Cada thread tem
 * seus proprios arrays, eliminando a concorrencia
 */
public final class ContextoGeracao {
    // arrays de resultado dos MapaRuido um por ruido usado na geração
    // TerranoBase
    public final float[] persistMapa = new float[16 * 16];
    public final float[] baseMapa = new float[16 * 16];
    public final float[] altMapa = new float[16 * 16];
    public final float[] seletorMapa = new float[16 * 16];
    public final float[] superficeMapa = new float[16 * 16];

    // geradorRios
    public final float[] subaquatMapa = new float[16 * 16];
    // cristaMapa: 3D 16 * Y_CHUNK * 16
    public final float[] cristaMapa;

    // MotorGeracao, bioma e fileira
    public final float[] calorMapa = new float[16 * 16];
    public final float[] umidadeMapa = new float[16 * 16];
    public final float[] preenProfMapa = new float[16 * 16];

    // biomaMapa exposto pra uso futuro por GeradorDecoracoes
    public final DadosBioma[] biomaMapa = new DadosBioma[16 * 16];

    public ContextoGeracao(int yChunk) {
        cristaMapa = new float[16 * yChunk * 16];
    }
}

