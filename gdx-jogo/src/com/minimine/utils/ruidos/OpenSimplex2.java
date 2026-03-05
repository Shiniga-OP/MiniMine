package com.minimine.utils.ruidos;
/*
 * OpenSimplex2S(SuperSimplex), variante borrada
 * implementação estatica, semente passada em cada chamada, sem estado, sem alocação
 
 * por que OpenSimplex2S e não OpenSimplex2F:
 *   2F é mais rapido mas produz bordas mais duras, bom pra texturas tecnicas
 *   2S usa kernel maior(raio 3/4 vs 2/3), resultado mais suave e isotropico
 *   pra terreno, 2S é sempre preferivel
 
 * 2D: Simplex modificado com kernel maior e tabela de 24 gradientes
 *       resolve o problema de repetição direcional do Simplex original
 
 * 3D, re-orientado 8-ponto BCC ruido
 *       constroi um lattice BCC como união de dois lattices cubicos rotacionados
 *       muito mais isotropico que Simplex 3D em qualquer orientação de corte
 *       dois sabores:
 *         ruido3XY: planos XY são primarios(bom para altura + tempo)
 *         ruido3XZ: planos XZ são primarios(bom para terreno: X/Z horizontal, Y vertical)
 
 * normalização:
 *   ruido2 retorna aproximadamente [-1, 1]
 *   ruido3 retorna aproximadamente [-1, 1]
 *   "aproximadamente" porque simplex não tem envelope exato
 */
public final class OpenSimplex2 {
    // === CONSTANTES ===

    // 2D: fator de skew e de-skew do triangulo para o simplex
    public static final double SKEW2 = 0.366025403784438646; // (sqrt(3) - 1) / 2
    public static final double UNSKEW2 = 0.211324865405187117; // (3 - sqrt(3)) / 6

    // 2D: raio do kernel ao quadrado, OpenSimplex2S usa 9/4(maior que Simplex padrão 1/2)
    // raio maior -> transição mais suave entre contribuições de vertice
    public static final double KERNEL2 = 0.5625; // (3/4)^2

    // normalização 2D, escala o resultado para ficar proximo de [-1, 1]
    public static final double NORM2   = 47.0;

    // 3D: rotação BCC(1/sqrt(3) - 1) / 3 e 1/sqrt(3) * 2/3
    public static final double ROTATE3 = -0.16666666666666666; // (1/sqrt(3) - 1) / 3
    public static final double FALLBACK3 = 0.6666666666666666; // 2/3

    // 3D: raio do kernel ao quadrado
    public static final double KERNEL3 = 0.6;

    // normalização 3D
    public static final double NORM3   = 32.0;

    // ===TABELAS DE GRADIENTE ===
    // 2D: 24 gradientes unitarios distribuidos simetricamente no circulo
    // 24 direções em vez de 12 -> reduz repetição visual em ruido com limite
    // cada par(gx, gy) representa uma direção unitaria
    public static final float[] GRAD2 = {
		0.130526192220052f,  0.99144486137381f,
		0.38268343236509f,   0.923879532511287f,
		0.608761429008721f,  0.793353340291235f,
		0.793353340291235f,  0.608761429008721f,
		0.923879532511287f,  0.38268343236509f,
		0.99144486137381f,   0.130526192220051f,
		0.99144486137381f,  -0.130526192220051f,
		0.923879532511287f, -0.38268343236509f,
		0.793353340291235f, -0.608761429008721f,
		0.608761429008721f, -0.793353340291235f,
		0.38268343236509f,  -0.923879532511287f,
		0.130526192220052f, -0.99144486137381f,
        -0.130526192220052f, -0.99144486137381f,
        -0.38268343236509f,  -0.923879532511287f,
        -0.608761429008721f, -0.793353340291235f,
        -0.793353340291235f, -0.608761429008721f,
        -0.923879532511287f, -0.38268343236509f,
        -0.99144486137381f,  -0.130526192220052f,
        -0.99144486137381f,   0.130526192220051f,
        -0.923879532511287f,  0.38268343236509f,
        -0.793353340291235f,  0.608761429008721f,
        -0.608761429008721f,  0.793353340291235f,
        -0.38268343236509f,   0.923879532511287f,
        -0.130526192220052f,  0.99144486137381f,
    };
    // 3D: 24 gradientes nas arestas do cubo com comprimento uniforme
    // dois tipos de vetor: arestas longas(±1, ±1, 0) e curtas(0, ±1, ±1)
    // a correção de comprimento ta embutida na normalização
    public static final float[] GRAD3 = {
		0f,  1f,  1f,   0f,  1f, -1f,   0f, -1f,  1f,   0f, -1f, -1f,
		1f,  0f,  1f,   1f,  0f, -1f,  -1f,  0f,  1f,  -1f,  0f, -1f,
		1f,  1f,  0f,   1f, -1f,  0f,  -1f,  1f,  0f,  -1f, -1f,  0f,
		0f,  1f,  1f,   0f,  1f, -1f,   0f, -1f,  1f,   0f, -1f, -1f,
		1f,  0f,  1f,   1f,  0f, -1f,  -1f,  0f,  1f,  -1f,  0f, -1f,
		1f,  1f,  0f,   1f, -1f,  0f,  -1f,  1f,  0f,  -1f, -1f,  0f,
    };
	
    // === HASH ===
    // hash de bits rapido, avalanche completo
    // combina semente com coordenadas do lattice de forma independente por eixo
    public static int hash(long semente, int xp, int yp) {
        long h = semente ^ ((long)xp * 0x5205402B9270C86FL) ^ ((long)yp * 0x598CD327003817B5L);
        h = (h ^ (h >>> 33)) * 0xff51afd7ed558ccdL;
        h = (h ^ (h >>> 33)) * 0xc4ceb9fe1a85ec53L;
        return (int)(h ^ (h >>> 33));
    }

    public static int hash(long semente, int xp, int yp, int zp) {
        long h = semente ^ ((long)xp * 0x5205402B9270C86FL)
			^ ((long)yp * 0x598CD327003817B5L)
			^ ((long)zp * 0x4A8C4E8E1E3F6B29L);
        h = (h ^ (h >>> 33)) * 0xff51afd7ed558ccdL;
        h = (h ^ (h >>> 33)) * 0xc4ceb9fe1a85ec53L;
        return (int)(h ^ (h >>> 33));
    }
    /*
     * ruido 2D OpenSimplex2S
     * retorna valor aproximadamente em [-1, 1]
     * kernel estendido(raio 3/4) para suavidade maior
     */
    public static double ruido2(long semente, double x, double y) {
        // transforma para coordenadas do lattice simplex
        double skew = (x + y) * SKEW2;
        int i = floorRapido(x + skew);
        int j = floorRapido(y + skew);

        // coordenadas dentro da célula skewed
        double unskew = (i + j) * UNSKEW2;
        double x0 = x - (i - unskew);
        double y0 = y - (j - unskew);

        double valor = 0.0;

        // contribuição dos vertices do triangulo
        // OpenSimplex2S avalia 4 vertices(não 3 como Simplex) pro kernel maior
        // vertice 0: sempre(i, j)
        valor += contribuicao2(semente, i, j, x0, y0);

        // vertice 1 e 2: os dois da segunda metade do losango
        double x1 = x0 - 1.0 + UNSKEW2;
        double y1 = y0 + UNSKEW2;
        double x2 = x0 + UNSKEW2;
        double y2 = y0 - 1.0 + UNSKEW2;

        valor += contribuicao2(semente, i + 1, j,     x1, y1);
        valor += contribuicao2(semente, i,     j + 1, x2, y2);

        // vertice 3: canto oposto do losango
        double x3 = x0 - 1.0 + 2.0 * UNSKEW2;
        double y3 = y0 - 1.0 + 2.0 * UNSKEW2;
        valor += contribuicao2(semente, i + 1, j + 1, x3, y3);

        return valor * NORM2;
    }

    public static double contribuicao2(long semente, int xi, int yi, double dx, double dy) {
        double t = KERNEL2 - dx * dx - dy * dy;
        if(t <= 0.0) return 0.0;
        t *= t;
        // indice no array de gradientes: hash -> [0, 24) -> *2 para pegar par(gx, gy)
        int gi = (hash(semente, xi, yi) & 23) * 2;
        return t * t * (GRAD2[gi] * dx + GRAD2[gi | 1] * dy);
    }
    /*
     * ruido 3D com planos XZ primarios, recomendado para terreno
     * X e Z são as coordenadas horizontais, Y é vertical(ou tempo)
     * cortes horizontais(Y fixo) tem maxima isotropia
     * uso típico: ruido3XZ(semente, x * escala, y * escalaY, z * escala)
     */
    public static double ruido3XZ(long semente, double x, double y, double z) {
        // rotação que alinha o eixo Y com a diagonal principal do BCC
        // e mantém XZ no plano de melhor isotropia
        double xz = x + z;
        double s2 = xz * ROTATE3;
        double yr = y * FALLBACK3;
        double xr = x + s2 + yr;
        double zr = z + s2 + yr;
        double yr2 = xz * (-FALLBACK3) + yr;

        return ruido3Base(semente, xr, yr2, zr);
    }
    /*
     * ruido 3D com planos XY primarios
     * util pra animações onde Z é o tempo, ou cavernas onde Y/Z são primarios
     */
    public static double ruido3XY(long semente, double x, double y, double z) {
        double xy = x + y;
        double s2 = xy * ROTATE3;
        double zr = z * FALLBACK3;
        double xr = x + s2 + zr;
        double yr = y + s2 + zr;
        double zr2 = xy * (-FALLBACK3) + zr;

        return ruido3Base(semente, xr, yr, zr2);
    }
    /*
     * base do ruido 3D, re-orientado 8-ponto BCC
     * recebe coordenadas ja rotacionadas
     * o lattice BCC é construido como dois lattices cubicos posição por(0.5, 0.5, 0.5)
     * pra cada lattice, encontra os 4 vertices mais proximos
     * total: 8 vertices contribuindo mais do que Simplex 3D(4 vertices)
     * resultado: muito mais isotropico, menos artefatos direcionais
     */
    public static double ruido3Base(long semente, double xr, double yr, double zr) {
        // celula base no lattice
        int xrb = floorRapido(xr);
        int yrb = floorRapido(yr);
        int zrb = floorRapido(zr);

        // fração dentro da celula
        double xri = xr - xrb;
        double yri = yr - yrb;
        double zri = zr - zrb;

        // determinamos quais dos 8 vertices do cubo contribuem
        // baseado na proximidade ao longo de cada eixo
        double xNeg = xri - 0.5;
        double yNeg = yri - 0.5;
        double zNeg = zri - 0.5;

        double valor = 0.0;

        // primeiro half-lattice: 4 vertices
        valor += contribuicao3(semente, xrb, yrb, zrb, xri,  yri,  zri );
        valor += contribuicao3(semente, xrb + 1, yrb, zrb, xNeg, yri, zri );
        valor += contribuicao3(semente, xrb, yrb + 1, zrb, xri, yNeg, zri );
        valor += contribuicao3(semente, xrb, yrb, zrb + 1, xri, yri, zNeg);

        // segundo half-lattice: posição por(0.5, 0.5, 0.5) no espaço rotacionado
        // semente diferente para independencia entre os dois lattices
        long semente2 = semente ^ 0x6A09E667F3BCC909L;
        valor += contribuicao3(semente2, xrb + 1, yrb + 1, zrb, xNeg, yNeg, zri );
        valor += contribuicao3(semente2, xrb + 1, yrb, zrb + 1, xNeg, yri, zNeg);
        valor += contribuicao3(semente2, xrb, yrb + 1, zrb + 1, xri,  yNeg, zNeg);
        valor += contribuicao3(semente2, xrb + 1, yrb + 1, zrb + 1, xNeg, yNeg, zNeg);

        return valor * NORM3;
    }

    public static double contribuicao3(long semente, int xi, int yi, int zi, double dx, double dy, double dz) {
        double t = KERNEL3 - dx * dx - dy * dy - dz * dz;
        if(t <= 0.0) return 0.0;
        t *= t;
        int gi = (hash(semente, xi, yi, zi) & 23) * 3;
        return t * t * (GRAD3[gi] * dx + GRAD3[gi + 1] * dy + GRAD3[gi + 2] * dz);
    }
    /*
     * fBm 2D
     * soma oitavas de ruido com frequencia e amplitude crescentes/decrescentes
     * persistencia: quanto cada oitava contribui(tipicamente 0.5)
     * lacunaridade: quanto a frequencia aumenta por oitava(tipicamente 2.0)
     */
    public static double ruido2Fractal(long semente, double x, double y, int oitavas, double persistencia, double lacunaridade) {
        double total = 0.0;
        double amplitude = 1.0;
        double frequencia = 1.0;
        double maximo = 0.0;

        for(int i = 0; i < oitavas; i++) {
            total += ruido2(semente, x * frequencia, y * frequencia) * amplitude;
            maximo += amplitude;
            // sementes diferentes por oitava evitam correlação entre camadas
            semente      = semente * 6364136223846793005L + 1442695040888963407L;
            amplitude *= persistencia;
            frequencia *= lacunaridade;
        }
        return total / maximo;
    }
	
    // fBm 3D XZ para terreno
    public static double ruido3XZFractal(long semente, double x, double y, double z, int oitavas, double persistencia, double lacunaridade) {
        double total = 0.0;
        double amplitude = 1.0;
        double frequencia = 1.0;
        double maximo = 0.0;

        for(int i = 0; i < oitavas; i++) {
            total += ruido3XZ(semente, x * frequencia, y * frequencia, z * frequencia) * amplitude;
            maximo += amplitude;
            semente  = semente * 6364136223846793005L + 1442695040888963407L;
            amplitude *= persistencia;
            frequencia *= lacunaridade;
        }
        return total / maximo;
    }
	
    // fBm 3D XY para cavernas e volumes
    public static double ruido3XYFractal(long semente, double x, double y, double z, int oitavas, double persistencia, double lacunaridade) {
        double total = 0.0;
        double amplitude = 1.0;
        double frequencia = 1.0;
        double maximo = 0.0;

        for(int i = 0; i < oitavas; i++) {
            total += ruido3XY(semente, x * frequencia, y * frequencia, z * frequencia) * amplitude;
            maximo += amplitude;
            semente = semente * 6364136223846793005L + 1442695040888963407L;
            amplitude *= persistencia;
            frequencia *= lacunaridade;
        }
        return total / maximo;
    }

	// util:
    public static int floorRapido(double x) {
        int xi = (int)x;
        return x < xi ? xi - 1 : xi;
    }
}

