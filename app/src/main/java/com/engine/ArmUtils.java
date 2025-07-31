package com.engine;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import android.os.Environment;
import java.io.FileOutputStream;
import android.content.Context;
import java.util.List;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.FileWriter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class ArmUtils {
	public static String lerTextoAssets(Context ctx, String nomeArquivo) {
		try {
			InputStream is = ctx.getAssets().open(nomeArquivo);
			int tamanho = is.available();
			byte[] buffer = new byte[tamanho];
			is.read(buffer);
			is.close();
			return new String(buffer, "UTF-8");
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public static Bitmap lerImgAssets(Context ctx, String nomeArquivo) {
		try {
			InputStream is = ctx.getAssets().open(nomeArquivo);
			Bitmap bmp = BitmapFactory.decodeStream(is);
			is.close();
			return bmp;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public static String lerArquivo(File arquivo) throws Exception {
		BufferedReader br = new BufferedReader(new FileReader(arquivo));
		StringBuilder sb = new StringBuilder();
		String linha;
		while((linha = br.readLine()) != null) sb.append(linha);
		br.close();
		return sb.toString();
	}
	
	public static void criarArquivo(String caminho) {
        int ultimoPasso = caminho.lastIndexOf(File.separator);
        if(ultimoPasso > 0) {
            String dirCaminho = caminho.substring(0, ultimoPasso);
            criarDir(dirCaminho);
        }

        File arquivo = new File(caminho);

        try {
            if(!arquivo.exists()) arquivo.createNewFile();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public static String lerCaminho(String caminho) {
        criarArquivo(caminho);

        StringBuilder sb = new StringBuilder();
        FileReader fr = null;

        try {
            fr = new FileReader(new File(caminho));

            char[] buff = new char[1024];
            int tamanho = 0;

            while((tamanho = fr.read(buff)) > 0) sb.append(new String(buff, 0, tamanho));
        } catch(Exception e) {
            e.printStackTrace();
        } finally {
            if(fr != null) {
                try {
                    fr.close();
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return sb.toString();
    }

    public static void escreverArquivo(String caminho, String texto) {
        criarArquivo(caminho);
        FileWriter escritor = null;
        try {
            escritor = new FileWriter(new File(caminho), false);
            escritor.write(texto);
            escritor.flush();
        } catch(Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if(escritor != null) escritor.close();
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

	public static void copiarPastaAssets(Context contexto, String caminhoDestino, String caminhoAssets) {
		try {
			String[] arquivos = contexto.getAssets().list(caminhoAssets);

			if(arquivos != null && arquivos.length > 0) {
				File pastaDestino = new File(caminhoDestino);
				if (!pastaDestino.exists()) pastaDestino.mkdirs();

				for(String arquivo : arquivos) {
					String novoCaminhoAssets = caminhoAssets + "/" + arquivo;
					String novoCaminhoDestino = caminhoDestino + "/" + arquivo;

					if(contexto.getAssets().list(novoCaminhoAssets).length > 0) copiarPastaAssets(contexto, novoCaminhoDestino, novoCaminhoAssets);
					else copiarArquivoAssets(contexto, novoCaminhoDestino, novoCaminhoAssets);
				}
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	public static boolean renomearPasta(String caminhoAntigo, String caminhoNovo) {
		File pastaAntiga = new File(caminhoAntigo);
		File pastaNova = new File(caminhoNovo);

		return pastaAntiga.exists() && pastaAntiga.isDirectory() && pastaAntiga.renameTo(pastaNova);
	}

	public static void copiarArquivoAssets(Context contexto, String caminhoAssets, String caminhoDestino) {
		try {
			InputStream is = contexto.getAssets().open(caminhoAssets);
			FileOutputStream fos = new FileOutputStream(new File(caminhoDestino));

			byte[] buffer = new byte[1024];
			int bytesLidos;

			while((bytesLidos = is.read(buffer)) != -1) {
				fos.write(buffer, 0, bytesLidos);
			}

			fos.close();
			is.close();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	public static void arquivarAssets(String caminhoExterno, InputStream is) {
		try {
			FileOutputStream fos = new FileOutputStream(new File(caminhoExterno));

			byte[] buffer = new byte[1024];
			int bytesLidos;

			while((bytesLidos = is.read(buffer)) != -1) {
				fos.write(buffer, 0, bytesLidos);
			}

			fos.close();
			is.close();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

    public static void copiarArquivo(String caminhoCP, String caminhoCL) {
        if(!existeArquivo(caminhoCP)) return;
        criarArquivo(caminhoCL);

        FileInputStream fis = null;
        FileOutputStream fos = null;

        try {
            fis = new FileInputStream(caminhoCP);
            fos = new FileOutputStream(caminhoCL, false);

            byte[] buff = new byte[1024];
            int tamanho = 0;

            while((tamanho = fis.read(buff)) > 0) {
                fos.write(buff, 0, tamanho);
            }
        } catch(Exception e) {
            e.printStackTrace();
        } finally {
            if(fis != null) {
                try {
                    fis.close();
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
            if(fos != null) {
                try {
                    fos.close();
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void copiarDir(String caminhoCP, String caminhoCL) {
        File arquivoCP = new File(caminhoCP);
        File[] arquivos = arquivoCP.listFiles();
        File arquivoCL = new File(caminhoCL);
        if(!arquivoCL.exists()) {
            arquivoCL.mkdirs();
        }
        for(File arquivo : arquivos) {
            if(arquivo.isFile()) {
                copiarArquivo(arquivo.getPath(), caminhoCL + "/" + arquivo.getName());
            } else if(arquivo.isDirectory()) {
                copiarDir(arquivo.getPath(), caminhoCL + "/" + arquivo.getName());
            }
        }
    }

    public static void moverArquivo(String caminhoCT, String caminhoCL) {
        copiarArquivo(caminhoCT, caminhoCL);
        deleteArquivo(caminhoCT);
    }

    public static void deleteArquivo(String caminho) {
        File arquivo = new File(caminho);

        if(!arquivo.exists()) return;
        if(arquivo.isFile()) {
            arquivo.delete();
            return;
        }

        File[] arquivos = arquivo.listFiles();

        if(arquivos != null) {
            for(File subArquivo : arquivos) {
                if(subArquivo.isDirectory()) {
                    deleteArquivo(subArquivo.getAbsolutePath());
                }
                if(subArquivo.isFile()) {
                    subArquivo.delete();
                }
            }
        }
        arquivo.delete();
    }

    public static boolean existeArquivo(String caminho) {
        File arquivo = new File(caminho);
        return arquivo.exists();
    }

    public static void criarDir(String caminho) {
        if(!existeArquivo(caminho)) {
            File arquivo = new File(caminho);
            arquivo.mkdirs();
        }
    }

    public static void listarArquivos(String caminho, List<String> lista) {
        File dir = new File(caminho);
        if(!dir.exists() || dir.isFile()) return;

        File[] listaArquivos = dir.listFiles();
        if(listaArquivos == null || listaArquivos.length <= 0) return;

        if(lista==null) return;
        lista.clear();
        for(File arquivo : listaArquivos) {
            lista.add(arquivo.getName());
        }
    }

    public static void listarArquivosAbs(String caminho, List<String> lista) {
        File dir = new File(caminho);
        if(!dir.exists() || dir.isFile()) return;

        File[] listaArquivos = dir.listFiles();
        if(listaArquivos==null || listaArquivos.length <= 0) return;

        if(lista==null) return;
        lista.clear();
        for(File arquivo : listaArquivos) {
            lista.add(arquivo.getAbsolutePath());
        }
    }

    public static boolean eDir(String caminho) {
        if(!existeArquivo(caminho)) return false;
        return new File(caminho).isDirectory();
    }

    public static boolean eArquivo(String caminho) {
        if(!existeArquivo(caminho)) return false;
        return new File(caminho).isFile();
    }

    public static long obterArquivoTam(String caminho) {
        if(!existeArquivo(caminho)) return 0;
        return new File(caminho).length();
    }

    public static String obterArmaExterno() {
        return Environment.getExternalStorageDirectory().getAbsolutePath();
    }

    public static String obterPacoteDados(Context contexto) {
        return contexto.getExternalFilesDir(null).getAbsolutePath();
    }

    public static String obterDirPublico(String tipo) {
        return Environment.getExternalStoragePublicDirectory(tipo).getAbsolutePath();
    }
}
