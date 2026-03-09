package com.minimine.mundo;

import com.minimine.mundo.blocos.Bloco;

public class ChunkUtil {
	public static final int LOG2(final int i) {
		if(i == 4) return 2;
		else if(i == 8) return 3;
		else if(i == 16) return 4;
		else if(i == 32) return 5;
		return 0;
	}

	public static byte obterLuzCompleta(int x, int y, int z, Chunk chunk) {
		final int idc = x + (z << 4) + (y << 8);
		return chunk.luz[idc];
	}

	public static byte obterLuzBloco(int x, int y, int z, Chunk chunk) {
		final int idc = x + (z << 4) + (y << 8);
		return (byte)(chunk.luz[idc] & 15);
	}

	public static void defLuzBloco(int x, int y, int z, byte valor, Chunk chunk) {
		final int idc = x + (z << 4) + (y << 8);
		chunk.luz[idc] = (byte)((chunk.luz[idc] & 0xF0) | (valor & 15));
	}

	public static byte obterMeta(int x, int y, int z, Chunk chunk) {
		return chunk.meta[x + (z << 4) + (y << 8)];
	}

	public static void defMeta(int x, int y, int z, byte valor, Chunk chunk) {
		chunk.meta[x + (z << 4) + (y << 8)] = valor;
	}

	public static boolean ehSolido(int x, int y, int z, Chunk chunk) {
		if(x < 0 || x >= 16 || y < 0 || y >= 256 || z < 0 || z >= 16) {
			return false;
		}
		int bloco;
		if(chunk.usaPaleta) {
			bloco = chunk.paleta[
			lerPacote(x + (z << 4) + (y << 8),
			chunk.paletaBits, chunk.blocos,
			chunk.blocosPorInt, LOG2(chunk.blocosPorInt))
			];
		} else {
			bloco = lerPacote(x + (z << 4) + (y << 8),
			chunk.bitsPorBloco, chunk.blocos, chunk.blocosPorInt,
			LOG2(chunk.blocosPorInt));
		}
		return bloco != 0;
	}

	public static int obterBloco(int x, int y, int z, Chunk chunk) {
		int total = x + (z << 4) + (y << 8); 

		if(chunk.blocos == null) return 0;

		if(chunk.usaPaleta) {
			int idc = lerPacote(total, chunk.paletaBits, chunk.blocos, chunk.blocosPorInt, LOG2(chunk.blocosPorInt));
			if(idc < 0 || idc >= chunk.paletaTam) return 0;
			return chunk.paleta[idc];
		} else {
			return lerPacote(total, chunk.bitsPorBloco, chunk.blocos, chunk.blocosPorInt, LOG2(chunk.blocosPorInt));
		}
	}

	public static void defBloco(int x, int y, int z, CharSequence nome, Chunk chunk) {
		final int bloco = nome.equals("ar") ? 0 : Bloco.texIds.get(nome).tipo;
		final int total = x + (z << 4) + (y << 8);
		// se ta em modo paleta, tenta usar/expandir paleta
		if(chunk.usaPaleta) {
			// procura na paleta
			int idc = -1;
			for(int i = 0; i < chunk.paletaTam; i++) {
				if(chunk.paleta[i] == bloco) {
					idc = i;
					break;
				}
			}
			if(idc == -1) {
				// não existe ainda na paleta -> tentar inserir
				int capacidade = 1 << chunk.paletaBits;
				if(chunk.paletaTam < capacidade) {
					// cabe na paleta atual
					if(chunk.paletaTam >= chunk.paleta.length) {
						// aumenta array se necessario
						int[] novo = new int[Math.max(chunk.paleta.length << 1, capacidade)];
						System.arraycopy(chunk.paleta, 0, novo, 0, chunk.paleta.length);
						chunk.paleta = novo;
					}
					chunk.paleta[chunk.paletaTam] = bloco;
					idc = chunk.paletaTam++;
				} else {
					// paleta cheia para paletaBits atual
					if(chunk.paletaBits < 8) {
						// aumenta paletaBits(repacota indices)
						refazerPaleta(chunk.paletaBits + 1, chunk);
						// inserir agora(deve caber)
						int[] pal = chunk.paleta;
						for(int i = 0; i < chunk.paletaTam; i++) {
							if(pal[i] == bloco) {
								idc = i;
								break;
							}
						}
						if(idc == -1) {
							if(chunk.paletaTam >= chunk.paleta.length) {
								int[] novo = new int[Math.max(chunk.paleta.length << 1, 1 << chunk.paletaBits)];
								System.arraycopy(chunk.paleta, 0, novo, 0, chunk.paleta.length);
								chunk.paleta = novo;
							}
							chunk.paleta[chunk.paletaTam] = bloco;
							idc = chunk.paletaTam++;
						}
					} else {
						// paleta ja com 8 bits e cheia -> converte para modo direto
						convertPaletaDireto(chunk, bloco);
						// agora no modo direto
					}
				}
			}
			// se ainda estamos em paleta(idc valido), grava indice
			if(chunk.usaPaleta) {
				gravarPacote(total, idc, chunk.paletaBits, chunk.blocos, chunk.blocosPorInt, LOG2(chunk.blocosPorInt));
				return;
			}
			// caso contrario, conversão ocorreu e prossegue pra modo direto
		}
		// modo direto: garantias de bits e compactação se necessario
		if(!chunk.usaPaleta) {
			if(bloco > chunk.maxIds) {
				int novosBits = ChunkUtil.bitsPraMaxId(bloco);
				if(chunk.bitsPorBloco != novosBits) {
					ChunkUtil.compactar(novosBits, chunk);
				}
			}
			// escrever valor direto
			gravarPacote(total, bloco, chunk.bitsPorBloco, chunk.blocos, chunk.blocosPorInt, LOG2(chunk.blocosPorInt));
		}
	}

	public static int bitsPraMaxId(int maxId) {
		// lerPacote/gravarPacote usam bit-shift assumindo que blocosPorInt é potencia de 2
		// isso so é verdade quando bits e {1, 2, 4, 8} -> blocosPorInt e {32, 16, 8, 4}.
		// valores como 3, 5, 6, 7 resultam em blocosPorInt ímpar e corrompem o indice
		if(maxId <= 1)  return 1; // 32 blocos/int
		if(maxId <= 3)  return 2; // 16 blocos/int
		if(maxId <= 15) return 4; //  8 blocos/int
		return 8; //  4 blocos/int
	}

	// usa bit-shift
	// blocosPorInt é sempre potencia de 2(32/1=32, 32/2=16, 32/4=8, 32/8=4),
	// então: x/n == x>>log2(n) e x%n == x&(n-1)
	public static int lerPacote(int indiceGlobal, int bits, int[] arr, int blocosPorInt, int log2) {
		final int idc = indiceGlobal >> log2;
		final int bitPos = (indiceGlobal & (blocosPorInt - 1)) * bits;
		final int mascara = (1 << bits) - 1;
		return (arr[idc] >>> bitPos) & mascara;
	}

	public static void gravarPacote(int indiceGlobal, int valor, int bits, int[] arr, int blocosPorInt, int log2) {
		final int idc = indiceGlobal >> log2;
		final int bitPos = (indiceGlobal & (blocosPorInt - 1)) * bits;
		final int mascara = ((1 << bits) - 1) << bitPos;
		arr[idc] = (arr[idc] & ~mascara) | ((valor & ((1 << bits) - 1)) << bitPos);
	}

	public static void refazerPaleta(int novosBits, Chunk chunk) {
		if(!chunk.usaPaleta) return;
		if(novosBits <= 0 || novosBits > 8) novosBits = 8;

		int bitsAntigo = chunk.paletaBits;
		int blocosPorIntAntigo = chunk.blocosPorInt;

		// nem precisa repacotar
		if(bitsAntigo == novosBits) {
			return; // nada muda
		}
		int[] antigos = chunk.blocos;

		chunk.paletaBits = novosBits;
		chunk.blocosPorInt = 32 / chunk.paletaBits;

		final int totalBlocos = 1 << 16;
		int tamNovo = (totalBlocos + chunk.blocosPorInt - 1) / chunk.blocosPorInt;
		int[] novos = new int[tamNovo];

		final int log2Antigo = LOG2(blocosPorIntAntigo);
		final int log2Novo = LOG2(chunk.blocosPorInt);

		if(antigos != null && blocosPorIntAntigo > 0 && bitsAntigo > 0) {
			// lerPacote/gravarPacote usam bit-shift internamente (blocosPorInt é sempre potência de 2)
			// evita divisão inteira em loop de 65280 iterações
			for(int i = 0; i < totalBlocos; i++) {
				int idAntigo = lerPacote(i, bitsAntigo, antigos, blocosPorIntAntigo, log2Antigo);
				gravarPacote(i, idAntigo, chunk.paletaBits, novos, chunk.blocosPorInt, log2Novo);
			}
		}
		chunk.blocos = novos;
	}

	// converte completamente do modo paleta para modo direto
	public static void convertPaletaDireto(Chunk chunk, int blocoExtra) {
		// encontra o maior id real que precisa ser representado
		int maxVal = blocoExtra;
		for(int i = 0; i < chunk.paletaTam; i++) {
			if(chunk.paleta[i] > maxVal) maxVal = chunk.paleta[i];
		}
		int bitsParaReal = bitsPraMaxId(maxVal);
		if(bitsParaReal < 1) bitsParaReal = 1;

		int bitsAntigo = chunk.paletaBits;
		int blocosPorIntAntigo = chunk.blocosPorInt;
		int[] antigos = chunk.blocos;

		chunk.usaPaleta = false;
		chunk.bitsPorBloco = bitsParaReal;
		chunk.blocosPorInt = 32 / chunk.bitsPorBloco;
		chunk.maxIds = (1 << chunk.bitsPorBloco) - 1;

		int totalBlocos = 1 << 16;
		int tamNovo = (totalBlocos + chunk.blocosPorInt - 1) / chunk.blocosPorInt;
		int[] novos = new int[tamNovo];

		final int log2Antigo = LOG2(blocosPorIntAntigo);
		final int log2Novo = LOG2(chunk.blocosPorInt);

		if(antigos != null && blocosPorIntAntigo > 0 && bitsAntigo > 0) {
			// lerPacote/gravarPacote usam bit-shift internamente(blocosPorInt é sempre potencia de 2)
			// evita divisão inteira em loop de 65280 iterações
			for(int i = 0; i < totalBlocos; i++) {
				int idcPal = lerPacote(i, bitsAntigo, antigos, blocosPorIntAntigo, log2Antigo);
				int real = (idcPal >= 0 && idcPal < chunk.paletaTam) ? chunk.paleta[idcPal] : 0;
				gravarPacote(i, real, chunk.bitsPorBloco, novos, chunk.blocosPorInt, log2Novo);
			}
		}
		chunk.blocos = novos;
		// limpa paleta pra liberar memoria
		chunk.paleta = null;
		chunk.paletaTam = 0;
		chunk.paletaBits = 0;
	}

	public static void compactar(int bitsPorBloco, Chunk chunk) {
		// se chunk estava em paleta e compactar é pedido pra modo direto
		if(chunk.usaPaleta && bitsPorBloco > 0) {
			int maxVal = 0;
			if(chunk.paleta != null) {
				for(int i = 0; i < chunk.paletaTam; i++) {
					if(chunk.paleta[i] > maxVal) maxVal = chunk.paleta[i];
				}
			}
			if(bitsPraMaxId(maxVal) > bitsPorBloco) {
				bitsPorBloco = bitsPraMaxId(maxVal);
			}
			convertPaletaDireto(chunk, 0);
			if(chunk.bitsPorBloco < bitsPorBloco) {
				int bitsAntigo = chunk.bitsPorBloco;
				int blocosPorIntAntigo = chunk.blocosPorInt;

				if(bitsAntigo == bitsPorBloco) return; // evita o loop

				int[] antigos = chunk.blocos;

				chunk.bitsPorBloco = bitsPorBloco;
				chunk.blocosPorInt = 32 / chunk.bitsPorBloco;
				chunk.maxIds = (1 << chunk.bitsPorBloco) - 1;

				final int totalBlocos = 1 << 16;
				final int tamNovo = (totalBlocos + chunk.blocosPorInt - 1) / chunk.blocosPorInt;
				int[] novos = new int[tamNovo];

				final int log2Antigo = LOG2(blocosPorIntAntigo);
				final int log2Novo = LOG2(chunk.blocosPorInt);

				if(antigos != null && blocosPorIntAntigo > 0 && bitsAntigo > 0) {
					// lerPacote/gravarPacote usam bit-shift internamente(blocosPorInt é sempre potencia de 2)
					// evita divisão inteira em loop de 65280 iterações
					for(int i = 0; i < totalBlocos; i++) {
						int idAntigo = lerPacote(i, bitsAntigo, antigos, blocosPorIntAntigo, log2Antigo);
						gravarPacote(i, idAntigo, chunk.bitsPorBloco, novos, chunk.blocosPorInt, log2Novo);
					}
				}
				chunk.blocos = novos;
			}
			return;
		}
		// comportamento antigo(modo direto)
		if(bitsPorBloco < 1) bitsPorBloco = 1;

		int bitsAntigo = chunk.bitsPorBloco;
		int blocosPorIntAntigo = chunk.blocosPorInt;
		int[] antigos = chunk.blocos;

		chunk.maxIds = (1 << bitsPorBloco) - 1;
		chunk.bitsPorBloco = bitsPorBloco;
		chunk.blocosPorInt = 32 / chunk.bitsPorBloco;

		final int totalBlocos = 1 << 16;
		int tamNovo = (totalBlocos + chunk.blocosPorInt - 1) / chunk.blocosPorInt;
		int[] novos = new int[tamNovo];

		final int log2Antigo = LOG2(blocosPorIntAntigo);
		final int log2Novo = LOG2(chunk.blocosPorInt);

		if(antigos != null && blocosPorIntAntigo > 0 && bitsAntigo > 0) {
			// lerPacote/gravarPacote usam bit-shift internamente(blocosPorInt é sempre potencia de 2)
			// evita divisão inteira em loop de 65280 iterações
			for(int i = 0; i < totalBlocos; i++) {
				int idAntigo = lerPacote(i, bitsAntigo, antigos, blocosPorIntAntigo, log2Antigo);
				gravarPacote(i, idAntigo, chunk.bitsPorBloco, novos, chunk.blocosPorInt, log2Novo);
			}
		}
		chunk.blocos = novos;
	}
}


