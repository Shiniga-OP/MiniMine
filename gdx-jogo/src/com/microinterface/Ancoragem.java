package com.microinterface;

public enum Ancoragem {
    SUPERIOR_ESQUERDO,
    SUPERIOR_CENTRO,
    SUPERIOR_DIREITO,
    CENTRO_ESQUERDO,
    CENTRO,
    CENTRO_DIREITO,
    INFERIOR_ESQUERDO,
    INFERIOR_CENTRO,
    INFERIOR_DIREITO;

    // calcula a posição X baseada na ancoragem
    public float calcularX(float larguraPai, float larguraFilho, float margemX) {
        switch(this) {
            case SUPERIOR_ESQUERDO:
            case CENTRO_ESQUERDO:
            case INFERIOR_ESQUERDO:
                return margemX;
            case SUPERIOR_CENTRO:
            case CENTRO:
            case INFERIOR_CENTRO:
                return (larguraPai - larguraFilho) / 2 + margemX;
            case SUPERIOR_DIREITO:
            case CENTRO_DIREITO:
            case INFERIOR_DIREITO:
                return larguraPai - larguraFilho - margemX;
            default:
                return margemX;
        }
    }

    // calcula a posição Y baseada na ancoragem
    public float calcularY(float alturaPai, float alturaFilho, float margemY) {
        switch(this) {
            case SUPERIOR_ESQUERDO:
            case SUPERIOR_CENTRO:
            case SUPERIOR_DIREITO:
                return alturaPai - alturaFilho - margemY;
            case CENTRO_ESQUERDO:
            case CENTRO:
            case CENTRO_DIREITO:
                return (alturaPai - alturaFilho) / 2 + margemY;
            case INFERIOR_ESQUERDO:
            case INFERIOR_CENTRO:
            case INFERIOR_DIREITO:
                return margemY;
            default:
                return margemY;
        }
    }
}

