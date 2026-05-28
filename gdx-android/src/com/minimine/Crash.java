package com.minimine;

import android.app.Activity;
import android.app.Application;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import dalvik.system.DexFile;
import java.lang.Thread.UncaughtExceptionHandler;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import android.os.Environment;
import androidx.multidex.MultiDex;
import androidx.multidex.MultiDexApplication;

public class Crash extends MultiDexApplication {
    public static Handler loop = new Handler(Looper.getMainLooper());
    public static File LOG_ARQUIVO;
	public static Context ctx;

    public static void log(String tag, String msg) {
        android.util.Log.d(tag, msg);
        gravarLog("[" + tag + "] " + msg);
    }
    public static void log(String tag, Throwable e) {
        e.printStackTrace();
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        gravarLog("[" + tag + "] " + sw.toString());
    }
    private static void gravarLog(String linha) {
        if(LOG_ARQUIVO == null) return;
        try {
            File p = LOG_ARQUIVO.getParentFile();
            if(p != null && !p.exists()) p.mkdirs();
            FileWriter ae = new FileWriter(LOG_ARQUIVO, true);
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                ae.write(sdf.format(new Date()) + " " + linha + "\n");
            } finally {
                ae.close();
            }
        } catch(Throwable e) {}
    }

    @Override
	protected void attachBaseContext(Context base) {
		LOG_ARQUIVO = new File(base.getExternalFilesDir(null), "logs/logs.txt");
		ctx = base;
		try {
			super.attachBaseContext(base);
			if(Build.VERSION.SDK_INT < 21) {
				MultiDex.install(base);
			}
		} catch(Throwable e) {
			log("attachBaseContext", e);
		}
	}
    public static void instalarDexExtras(Context ctx) throws Exception {
        StringBuilder log = new StringBuilder();
        
        try {
            File apk = new File(ctx.getApplicationInfo().sourceDir);
            log.append("APK: " + apk.getAbsolutePath() + "\n");
            File dexDir = ctx.getDir("dex_extra", Context.MODE_PRIVATE);
            log.append("dexDir: " + dexDir.getAbsolutePath() + "\n");
            ZipFile zip = new ZipFile(apk);
            List<DexFile> dexArquivos = new ArrayList<DexFile>();
            try {
                int n = 2;
                while(true) {
                    ZipEntry e = zip.getEntry("classes" + n + ".dex");
                    if(e == null) { log.append("Parou em classes" + n + ".dex\n"); break; }
                    log.append("Encontrou classes" + n + ".dex\n");
                    File saida = new File(dexDir, "classes" + n + ".dex");
                    if(!saida.exists() || saida.length() != e.getSize()) {
                        log.append("Extraindo classes" + n + ".dex\n");
                        InputStream in = zip.getInputStream(e);
                        FileOutputStream fos = new FileOutputStream(saida);
                        try {
                            gravar(in, fos);
                        } finally {
                            fecharES(in, fos);
                        }
                    }
                    File opt = new File(dexDir, "classes" + n + ".odex");
                    log.append("Carregando DEX: " + saida.getAbsolutePath() + "\n");
                    dexArquivos.add(DexFile.loadDex(saida.getAbsolutePath(), opt.getAbsolutePath(), 0));
                    log.append("DEX carregado com sucesso\n");
                    n++;
                }
            } finally {
                zip.close();
            }
            log.append("Total DEX arquivos: " + dexArquivos.size() + "\n");
            if(dexArquivos.isEmpty()) {
				log("InstalarDexExtras", log.toString());
				return;
			}
            ClassLoader cl = ctx.getClassLoader();
            log.append("ClassLoader: " + cl.getClass().getName() + "\n");
            Field lista_caminhoCampo = encontrarCampo(cl.getClass(), "pathList");
            lista_caminhoCampo.setAccessible(true);
            Object lista_caminho = lista_caminhoCampo.get(cl);
            log.append("caminho lista: " + lista_caminho.getClass().getName() + "\n");
            Field dexElementosCampos = encontrarCampo(lista_caminho.getClass(), "dexElements");
            dexElementosCampos.setAccessible(true);
            Object[] dexElementos = (Object[]) dexElementosCampos.get(lista_caminho);
            log.append("dexElementos.length: " + dexElementos.length + "\n");
            Class<?> elementoClasse = dexElementos.getClass().getComponentType();
            log.append("elementoClasse: " + elementoClasse.getName() + "\n");
            Object[] extras = (Object[]) Array.newInstance(elementoClasse, dexArquivos.size());
            for(int i = 0; i < dexArquivos.size(); i++) {
                try {
                    extras[i] = elementoClasse.getConstructor(File.class, String.class, File.class, DexFile.class)
                        .newInstance(apk, null, dexDir, dexArquivos.get(i));
                    log.append("Construtor 4-args OK\n");
                } catch(Exception e1) {
                    log.append("Construtor 4-args falhou: " + e1 + "\n");
                    try {
                        extras[i] = elementoClasse.getConstructor(File.class, DexFile.class)
                            .newInstance(apk, dexArquivos.get(i));
                        log.append("Construtor 2-args OK\n");
                    } catch(Exception e2) {
                        log.append("Construtor 2-args falhou: " + e2 + "\n");
                    }
                }
            }
            Object[] combinado = (Object[]) Array.newInstance(elementoClasse, dexElementos.length + extras.length);
            System.arraycopy(dexElementos, 0, combinado, 0, dexElementos.length);
            System.arraycopy(extras, 0, combinado, dexElementos.length, extras.length);
            dexElementosCampos.set(lista_caminho, combinado);
            log.append("Injecao concluida\n");
        } catch(Exception e) {
            log.append("EXCEÇÃO: " + e + "\n");
            throw e;
        } finally {
            try {
				log("instalarDexExtras", log.toString());
			} catch(Exception e) {}
        }
    }

    public static Field encontrarCampo(Class<?> cls, String nome) throws NoSuchFieldException {
        while(cls != null) {
            try {
                return cls.getDeclaredField(nome);
            } catch(NoSuchFieldException e) {
                cls = cls.getSuperclass();
            }
        }
        throw new NoSuchFieldException(nome);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        CrashUtil.getInstance().logGlobal(this);
        CrashUtil.getInstance().logParte(this);
    }
    public static void gravar(InputStream e, OutputStream s) throws IOException {
        byte[] buf = new byte[1024 * 8];
        int tam;
        while((tam = e.read(buf)) != -1) {
            s.write(buf, 0, tam);
        }
    }
    public static void gravar(File a, byte[] dados) throws IOException {
        File p = a.getParentFile();
        if(p != null && !p.exists()) p.mkdirs();

        ByteArrayInputStream e = new ByteArrayInputStream(dados);
        FileOutputStream s = new FileOutputStream(a);
        try {
            gravar(e, s);
        } finally {
            fecharES(e, s);
        }
    }
    public static String praString(InputStream e) throws IOException {
        ByteArrayOutputStream s = new ByteArrayOutputStream();
        gravar(e, s);
        try {
            return s.toString("UTF-8");
        } finally {
            fecharES(e, s);
        }
    }
    public static void fecharES(Closeable... cs) {
        for(Closeable c : cs) {
            try {
                if(c != null) c.close();
            } catch(IOException e) {}
        }
    }
    public static class CrashUtil {
        public static final UncaughtExceptionHandler DEFAULT_UNCAUGHT_EXCEPTION_HANDLER = Thread.getDefaultUncaughtExceptionHandler();
        public static CrashUtil copia;
        public Util util;
        public static CrashUtil getInstance() {
            if(copia == null) {
                copia = new CrashUtil();
            }
            return copia;
        }
        public void logGlobal(Context ctx) {
            logGlobal(ctx, null);
        }
        public void logGlobal(Context ctx, String crashDir) {
            Thread.setDefaultUncaughtExceptionHandler(new Excecao(ctx.getApplicationContext(), crashDir));
        }
        public void deslogar() {
            Thread.setDefaultUncaughtExceptionHandler(DEFAULT_UNCAUGHT_EXCEPTION_HANDLER);
        }
        public void logParte(Context ctx) {
            deslogarParte(ctx);
            util = new Util(ctx.getApplicationContext());
            loop.postAtFrontOfQueue(util);
        }
        public void deslogarParte(Context ctx) {
            if(util != null) {
                util.rodando.set(false);
                util = null;
            }
        }
        public static class Util implements Runnable {
            public final Context mCtx;
            public final AtomicBoolean rodando = new AtomicBoolean(true);

            public Util(Context context) {
                this.mCtx = context;
            }
            @Override
            public void run() {
                while(rodando.get()) {
                    try {
                        Looper.loop();
                    } catch(final Throwable e) {
                        log("CrashUtil.Util", e);
                        if(rodando.get()) {
                            loop.post(new Runnable(){
                                    @Override
                                    public void run() {
                                        Toast.makeText(mCtx, e.toString(), Toast.LENGTH_LONG).show();
                                    }
                                });
                        } else {
                            if(e instanceof RuntimeException) {
                                throw (RuntimeException)e;
                            } else {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                }
            }
        }
        public static class Excecao implements UncaughtExceptionHandler {
            public static DateFormat dados = new SimpleDateFormat("yyyy_MM_dd-HH_mm_ss");
            public final Context mCtx;
            public final File mCrashDir;

            public Excecao(Context ctx, String crashDir) {
                this.mCtx= ctx;
                this.mCrashDir = TextUtils.isEmpty(crashDir) ? new File(mCtx.getExternalCacheDir(), "crash") : new File(crashDir);
            }
            @Override
            public void uncaughtException(Thread thread, Throwable ex) {
                try {
                    String log = informar(ex);
                    log(log);
                    try {
                        Intent t = new Intent(mCtx, CrashActivity.class);
                        t.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        t.putExtra(Intent.EXTRA_TEXT, log);
                        mCtx.startActivity(t);
                    } catch(Throwable e) {
                        Crash.log("CrashUtil.Excecao", e);
                        Crash.log("CrashUtil.Excecao", e.toString());
                    }
                    Crash.log("CrashUtil.Excecao", ex);
                    android.os.Process.killProcess(android.os.Process.myPid());
                    System.exit(0);
                } catch (Throwable e) {
                    if(DEFAULT_UNCAUGHT_EXCEPTION_HANDLER != null) DEFAULT_UNCAUGHT_EXCEPTION_HANDLER.uncaughtException(thread, ex);
                }
            }
            public String informar(Throwable ex) {
                String tempo = dados.format(new Date());
                String nomeV = "desconhecida";
                long codigoV = 0;
                try {
                    PackageInfo pi = mCtx.getPackageManager().getPackageInfo(mCtx.getPackageName(), 0);
                    nomeV = pi.versionName;
                    codigoV = Build.VERSION.SDK_INT >= 28 ? pi.getLongVersionCode() : pi.versionCode;
                } catch(Throwable ignored) {}

                LinkedHashMap<String, String> c = new LinkedHashMap<String, String>();
                c.put("Data do Crash", tempo);
                c.put("Dispositivo", String.format("%s, %s", Build.MANUFACTURER, Build.MODEL));
                c.put("Versão do Android", String.format("%s (%d)", Build.VERSION.RELEASE, Build.VERSION.SDK_INT));
                c.put("Versão do App", String.format("%s (%d)", nomeV, codigoV));
                c.put("Kernel", kernel());
                c.put("Suporte de Abis", Build.VERSION.SDK_INT >= 21 && Build.SUPPORTED_ABIS != null ? Arrays.toString(Build.SUPPORTED_ABIS): "desconhecido");
                c.put("Impressão Digital", Build.FINGERPRINT);

                StringBuilder sb = new StringBuilder();

                for(String chave : c.keySet()) {
                    if(sb.length() != 0) sb.append("\n");
                    sb.append(chave);
                    sb.append(" :    ");
                    sb.append(c.get(chave));
                }
                sb.append("\n\n");
                sb.append(Log.getStackTraceString(ex));
                return sb.toString(); 
            }
            public void log(String msg) {
                String time = dados.format(new Date());
                File file = new File(mCrashDir, "crash_" + time + ".txt");
                try {
                    gravar(file, msg.getBytes("UTF-8"));
                } catch(Throwable e) {
                    e.printStackTrace();
                } 
            }
            public static String kernel() {
                try {
                    return Crash.praString(new FileInputStream("/proc/version")).trim();
                } catch (Throwable e) {
                    return e.getMessage();
                }
            }
        }
    }
    public static final class CrashActivity extends Activity {
        public String mLog;
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            setTheme(android.R.style.Theme_DeviceDefault);
            setTitle("O App Crashou");

            mLog = getIntent().getStringExtra(Intent.EXTRA_TEXT);

            ScrollView cv = new ScrollView(this);
            cv.setFillViewport(true);

            HorizontalScrollView hsc = new HorizontalScrollView(this);

            TextView tv = new TextView(this);
            int es = dp2px(16);
            tv.setPadding(es, es, es, es);
            tv.setText(mLog);
            tv.setTextIsSelectable(true);
            tv.setTypeface(Typeface.DEFAULT);
            tv.setLinksClickable(true);

            hsc.addView(tv);
            cv.addView(hsc, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

            setContentView(cv);
        }
        public void reiniciar() {
            Intent t = getPackageManager().getLaunchIntentForPackage(getPackageName());
            if(t != null) {
                t.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(t);
            }
            finish();
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(0);
        }
        public static int dp2px(float dpValor) {
            final float tam = Resources.getSystem().getDisplayMetrics().density;
            return (int)(dpValor * tam + 0.5f);
        }
        @Override
        public boolean onCreateOptionsMenu(Menu menu) {
            menu.add(0, android.R.id.copy, 0, android.R.string.copy)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
            return super.onCreateOptionsMenu(menu);
        }
        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            switch(item.getItemId()) {
                case android.R.id.copy:
                    ClipboardManager cm = (ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
                    cm.setPrimaryClip(ClipData.newPlainText(getPackageName(), mLog));
                    return true;
            }
            return super.onOptionsItemSelected(item);
        }
        @Override
        public void onBackPressed() {
            reiniciar();
        }
    }
}

