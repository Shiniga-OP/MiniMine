package com.minimine.mundo.geracao;

import com.minimine.mundo.Chunk;
import com.minimine.mundo.ChunkUtil;
import com.minimine.mundo.Mundo;
import com.minimine.mundo.FluxoAgua;
import com.minimine.utils.ruidos.OpenSimplex2;
/*
 * orquestrador de geração de chunk
 * thread-segura: toda a geração opera sobre ContextoGeracao local por thread

 * MotorGeracao, TerranoBase e GeradorRios são imutaveis após construção
 * contem apenas parametros e sementes
 * multiplas threads podem chamar
 * gerarChunk() simultaneamente sem concorrencia porque cada chamada usa seu proprio
 * ContextoGeracao via ThreadLocal

 *  fase 1: pré-calculo de ruido arrays
 *     persist -> base -> alt -> aSele -> subaquat -> crista -> calor -> umidade -> preenchimento
 *     todos os arrays populados antes de qualquer loop de blocos

 *   fase 2:
 *     for(z) for(x) for(y): pedra | escava canal de rio

 *   fase 3:
 *     passagem separada, por coluna: bioma por calor/umidade/altura, topo/subtopo/interior
 *     popula ctx.biomaMapa[] para uso futuro por GeradorDecoracoes

 *   fase 4:
 *     preenche água em blocos vazios abaixo do nível do mar

 * parametros de bioma
 *   aquecer: pos=0, escala=1, espalhar=1000, oct=3, persist=0.5, lac=2.0
 *   umidade: pos=0, escala=1, espalhar=1000, oct=3, persist=0.5, lac=2.0
 *   preenchimento_profundidade: pos=0, escala=1.2, espalhar=150, oct=3, persist=0.7, lac=2.0
 */
public final class MotorGeracao {
    public static final int NIVEL_MAR = 62;

    public final long semente;
    public final TerranoBase terreno;
    public final GeradorRios rios;
    public final RegistroBiomas registro;

    // parametros de ruidos de bioma, imutaveis, compartilhaveis
    public final long semCalor, semUmidade, sempreenchimento;
    public final float espalharCalor, espalharUmidade, espalharPreen;
    public final int octCalor, octUmidade, octPreen;
    public final float perCalor, perUmidade, perPreen;
    public final float escalaPreen;

    // ContextoGeracao por thread, elimina alocação por chunk e race conditions
    public final ThreadLocal<ContextoGeracao> ctxLocal = new ThreadLocal<ContextoGeracao>() {
		@Override protected ContextoGeracao initialValue() {return new ContextoGeracao(Mundo.Y_CHUNK);}
	};

    public MotorGeracao(long semente, RegistroBiomas registro) {
        this.semente  = semente;
        this.registro = registro;
        this.terreno  = new TerranoBase(semente, NIVEL_MAR);
        this.rios = new GeradorRios(semente, NIVEL_MAR);

        semCalor = semente ^ 0xCAFEBABE87654321L;
        semUmidade = semente ^ 0x4F3C2B1A9E8D7C6BL;
        sempreenchimento = semente ^ 0xB3A4C5D6E7F80192L;
        espalharCalor = 1000f;
		octCalor = 3;
		perCalor = 0.5f;
        espalharUmidade = 1000f;
		octUmidade = 3;
		perUmidade = 0.5f;
        espalharPreen = 150f;
		octPreen = 3;
		perPreen = 0.7f;
		escalaPreen = 1.2f;
    }

    // === ENTRADA PRINCIPAL ===
    public void gerarChunk(Chunk chunk) {
        final int chunkX = chunk.x << 4;
        final int chunkZ = chunk.z << 4;
        final ContextoGeracao ctx = ctxLocal.get();

        // === FASE 1: pré-calculo de todos os ruidos arrays ===
        // ordem: persist -> base -> alt -> aSele -> subaquat -> crista -> calor -> umidade -> preenchimento
        terreno.calcularChunk(chunkX, chunkZ, ctx);
        rios.calcularChunk(chunkX, 0, chunkZ, Mundo.Y_CHUNK, ctx);
        calcular2D(semCalor, espalharCalor, octCalor, perCalor, 2.0f, chunkX, chunkZ, ctx.calorMapa);
        calcular2D(semUmidade, espalharUmidade, octUmidade, perUmidade, 2.0f, chunkX, chunkZ, ctx.umidadeMapa);
        calcular2Dpreenchimento(chunkX, chunkZ, ctx.preenProfMapa);

        // normaliza calor e umidade para [0,1]
        for(int i = 0; i < 16 * 16; i++) {
            ctx.calorMapa[i] = Math.max(0f, Math.min(1f, ctx.calorMapa[i] * 0.5f + 0.5f));
            ctx.umidadeMapa[i] = Math.max(0f, Math.min(1f, ctx.umidadeMapa[i] * 0.5f + 0.5f));
        }
        // === FASE 2: gerar terreno ===
        // preenche pedra e escava canais de rio
        int pedraSuperficieMaxY = 0;

        for(int z = 0; z < 16; z++) {
            for(int x = 0; x < 16; x++) {
                int superficieY = terreno.obterAltura(x, z, ctx);
                if(superficieY > pedraSuperficieMaxY) pedraSuperficieMaxY = superficieY;

                for(int y = 0; y < Mundo.Y_CHUNK; y++) {
                    if(y <= superficieY && !rios.eCanal(x, y, z, y, superficieY, ctx)) {
                        ChunkUtil.defBloco(x, y, z, "pedra", chunk);
                    }
                }
            }
        }
        // === FASE 3: gerar biomas ===
        // aplica topo/subtopo/interior de bioma
        // topoColuna desce até o primeiro bloco sólido real da coluna
        for(int z = 0; z < 16; z++) {
            for(int x = 0; x < 16; x++) {
                int idc2d = z * 16 + x;

                int topoColuna = terreno.obterAltura(x, z, ctx);
                while(topoColuna > 0 && ChunkUtil.obterBloco(x, topoColuna, z, chunk) == 0) topoColuna--;

                float calor = ctx.calorMapa[idc2d];
                float umidade = ctx.umidadeMapa[idc2d];
                calor = Math.max(0f, Math.min(1f, calor - (float)((topoColuna - NIVEL_MAR) * 0.004)));

                DadosBioma bioma = registro.selecionar(calor, umidade, topoColuna);
                ctx.biomaMapa[idc2d] = bioma;

                final DadosBioma.Superficie s = bioma.superficie;
                float preenchimentoVal = Math.max(0f, ctx.preenProfMapa[idc2d]);
                int profpreenchimento = s.profTopo + s.profSubtopo + (int) preenchimentoVal;

                int profAtual = 0;
                for(int y = topoColuna; y >= 1; y--) {
                    if(ChunkUtil.obterBloco(x, y, z, chunk) == 0) {
						if(profAtual > 0) break;
						continue;
					}
                    if(profAtual < s.profTopo) {
                        ChunkUtil.defBloco(x, y, z, s.topo, chunk);
                    } else if(profAtual < profpreenchimento) {
                        ChunkUtil.defBloco(x, y, z, s.subtopo, chunk);
                    } else {
                        ChunkUtil.defBloco(x, y, z, s.interior, chunk);
                        break;
                    }
                    profAtual++;
                }
            }
        }
        // === FASE 4: preencher água ===
        // preenche blocos vazios abaixo do nível do mar com água estática
        // meta=0 (fonte) mas fluxoSujo permanece false — oceanos não propagam
        for(int z = 0; z < 16; z++) {
            for(int x = 0; x < 16; x++) {
                for(int y = NIVEL_MAR; y >= 0; y--) {
                    if(ChunkUtil.obterBloco(x, y, z, chunk) == 0) {
                        ChunkUtil.defBloco(x, y, z, "agua", chunk);
                        ChunkUtil.defMeta(x, y, z, (byte)FluxoAgua.NIVEL_FONTE, chunk);
                    }
                }
            }
        }
        chunk.dadosProntos = true;
    }

    // === UTILITARIOS ===
    public void calcular2D(long sem, float espalhar, int oct, float persist, float lac,
	int origemX, int origemZ, float[] saida) {
        float freq = 1.0f / espalhar;
        for(int z = 0; z < 16; z++) {
            for(int x = 0; x < 16; x++) {
                saida[z * 16 + x] = OpenSimplex2.ruido2Fractal(
				sem, (origemX + x) * freq, (origemZ + z) * freq, oct, persist, lac);
			}
		}
    }

    public void calcular2Dpreenchimento(int origemX, int origemZ, float[] saida) {
        float freq = 1.0f / espalharPreen;
        for(int z = 0; z < 16; z++) {
            for(int x = 0; x < 16; x++) {
                saida[(z << 4) + x] = escalaPreen * OpenSimplex2.ruido2Fractal(
				sempreenchimento, (origemX + x) * freq, (origemZ + z) * freq, octPreen, perPreen, 2.0f);
			}
		}
    }

    // ponto a ponto, uso esporadico
    public String obterBioma(int mx, int mz) {
        int alt = terreno.calcularAlturaPonto(mx, mz);
        float cal = Math.max(0f, Math.min(1f,
		OpenSimplex2.ruido2Fractal(semCalor, mx / espalharCalor, mz / espalharCalor,
		octCalor, perCalor, 2.0f) * 0.5f + 0.5f - ((alt - NIVEL_MAR) * 0.004f)));
        float umi = Math.max(0f, Math.min(1f,
		OpenSimplex2.ruido2Fractal(semUmidade, mx / espalharUmidade, mz / espalharUmidade,
		octUmidade, perUmidade, 2.0f) * 0.5f + 0.5f));
        return registro.selecionar(cal, umi, alt).nome;
    }

    public int[] localizarBioma(String chave, int origemX, int origemZ) {
        if(!registro.existe(chave)) return new int[]{0, 0};
        int passo = 64, raioMax = 100_000;
        for(int raio = passo; raio <= raioMax; raio += passo) {
            for(int dx = -raio; dx <= raio; dx += passo) {
                for(int dz = -raio; dz <= raio; dz += passo) {
                    if(Math.abs(dx) != raio && Math.abs(dz) != raio) continue;
                    int mx = origemX + dx, mz = origemZ + dz;
                    int alt = terreno.calcularAlturaPonto(mx, mz);
                    float cal = Math.max(0f, Math.min(1f,
					OpenSimplex2.ruido2Fractal(semCalor, mx / espalharCalor, mz / espalharCalor,
					octCalor, perCalor, 2.0f) * 0.5f + 0.5f - ((alt - NIVEL_MAR) * 0.004f)));
                    float umi = Math.max(0f, Math.min(1f,
					OpenSimplex2.ruido2Fractal(semUmidade, mx / espalharUmidade, mz / espalharUmidade,
					octUmidade, perUmidade, 2.0f) * 0.5f + 0.5f));
                    if(registro.selecionar(cal, umi, alt).chave.equals(chave)) return new int[]{mx, mz};
                }
            }
        }
        return new int[]{0, 0};
    }
}


