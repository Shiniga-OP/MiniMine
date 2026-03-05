package com.minimine.mundo.geracao;

import com.minimine.utils.ruidos.OpenSimplex2;
/*
 * erosão hidraulica sem estado, infinita
 * calcula analiticamente o quanto um ponto (x,z) seria erodido
 * dado o campo de elevação ao redor
 * forças modeladas:
 *   1. curvatura local(laplaciano), concavo erode, convexo deposita
 *   2. inclinação — amplifica o efeito da curvatura, sem sinal próprio
 *   3. resistência da rocha, ruido regional que modula o resultado
 * retorna negativo onde houve erosão, positivo onde houve deposição
 */
public class ErosaoHidraulica {
    // delta proporcional ao período do campo de elevação
    // DominioDeformacao tem detalhe mais fino em escala ~0.00038 -> período ~2600 blocos
    // 100 blocos captura curvatura real sem ser sensivel a erro numerico de ponto flutuante
    public static final double DELTA = 100.0;

    // com delta=100 o laplaciano fica na casa de 1e-8 a 1e-6
    // tanh(x * NORM) mapeia isso para [-1, 1] suavemente
    public static final double NORM_CURVATURA  = 8_000_000.0;

    // gradiente típico do campo está em torno de 1e-4 a 5e-4
    public static final double NORM_INCLINACAO = 8_000.0;

    // inclinação amplifica a curvatura, não tem sinal proprio
    // 1.0 = encosta muito ingreme pode dobrar o efeito
    public static final double PESO_INCLINACAO = 1.0;

    // resistencia da rocha, frequencia baixa -> variações regionais
    public static final double ESCALA_RESISTENCIA = 0.00035;
    public static final double PESO_RESISTENCIA   = 0.30;

    // deposição é fisicamente mais fraca que erosão
    public static final double VALOR_MIN = -1.0;
    public static final double VALOR_MAX =  0.5;

    public final DominioDeformacao dominio;
    public final long seedResistencia;

    public ErosaoHidraulica(long semente, DominioDeformacao dominio) {
        this.dominio = dominio;
        this.seedResistencia = semente ^ 0xB5AD4ECBE2A9A0EEL;
    }

    public double obterErosaoInterpolada(double x, double z) {
        // cinco amostras — reutilizadas para curvatura e gradiente
        double c = dominio.obterElevacaoContinental(x, z);
        double cx1 = dominio.obterElevacaoContinental(x + DELTA, z);
        double cx2 = dominio.obterElevacaoContinental(x - DELTA, z);
        double cz1 = dominio.obterElevacaoContinental(x, z + DELTA);
        double cz2 = dominio.obterElevacaoContinental(x, z - DELTA);

        // === curvatura (laplaciano de segunda ordem) ===
        // negativo = concavo = bacia = erode
        // positivo = convexo = cume  = deposita
        double curvatura = (cx1 + cx2 + cz1 + cz2 - 4.0 * c) / (DELTA * DELTA);
        curvatura = Math.tanh(curvatura * NORM_CURVATURA);

        // === inclinação ===
        // sempre positiva, so amplifica, não define sinal
        double gx = (cx1 - cx2) / (2.0 * DELTA);
        double gz = (cz1 - cz2) / (2.0 * DELTA);
        double inclinacao = Math.sqrt(gx * gx + gz * gz);
        inclinacao = Math.tanh(inclinacao * NORM_INCLINACAO);

        // inclinação amplifica a curvatura
        // concavo íngreme erode muito, convexo íngreme deposita muito, plano quase não muda
        double erosaoGeom = -curvatura * (1.0 + inclinacao * PESO_INCLINACAO);

        // === resistencia da rocha ===
        double rocha = OpenSimplex2.ruido2Fractal(
            seedResistencia,
            x * ESCALA_RESISTENCIA,
            z * ESCALA_RESISTENCIA,
            3, 0.5, 2.0
        );
        double fatorRocha = 1.0 - rocha * PESO_RESISTENCIA;

        double resultado = erosaoGeom * fatorRocha;
        return Math.max(VALOR_MIN, Math.min(VALOR_MAX, resultado));
    }
}

