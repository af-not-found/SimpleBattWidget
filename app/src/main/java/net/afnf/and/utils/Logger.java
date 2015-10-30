package net.afnf.and.utils;

import android.annotation.SuppressLint;
import android.os.Environment;
import android.util.Log;

import net.afnf.and.simplebattwidget.BuildConfig;
import net.afnf.and.simplebattwidget.Const;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Logger {

    private static final int BUF_SIZE = 1000;
    private static final long OLD_LOG_DEL_THRESHOLD_MS = 7 * 24 * 60 * 60 * 1000; // 7日
    
    private static boolean enableLogging = true;
    private static int index = 0;
    private static byte[] levels = new byte[BUF_SIZE];
    private static long[] times = new long[BUF_SIZE];
    private static String[] contents = new String[BUF_SIZE];
    private static byte[] FILE_SEPARATER = "\n-----------------------------------------------------------------\n".getBytes();

    private static enum LEVEL {
        VERBOSE, DEBUG, INFO, WARN, ERROR;
    }
    
    private static final String[] LEVEL_STR = {"V", "D", "I", "W", "E"};
    
    public static void v(String s) {
        log(LEVEL.VERBOSE, s, null);
    }

    public static void i(String s) {
        log(LEVEL.INFO, s, null);
    }

    public static void w(String s) {
        log(LEVEL.WARN, s, null);
    }

    public static void w(String s, Throwable t) {
        log(LEVEL.WARN, s, t);
    }

    public static void e(String s) {
        log(LEVEL.ERROR, s, null);
    }

    public static void e(String s, Throwable t) {
        log(LEVEL.ERROR, s, t);
    }

    private static void log(LEVEL level, String s, Throwable t) {
        if (enableLogging == false) {
            return;
        }

        if (BuildConfig.DEBUG) {
            switch (level) {
                case VERBOSE:
                    Log.v(Const.LOGTAG, s, t);
                    break;
                case DEBUG:
                    Log.d(Const.LOGTAG, s, t);
                    break;
                case INFO:
                    Log.i(Const.LOGTAG, s, t);
                    break;
                case WARN:
                    Log.w(Const.LOGTAG, s, t);
                    break;
                case ERROR:
                    Log.e(Const.LOGTAG, s, t);
                    break;
            }

            synchronized (Logger.class) {
                if (t != null) {
                    s += "\n" + Log.getStackTraceString(t);
                }
                times[index] = System.currentTimeMillis();
                contents[index] = s;
                levels[index] = (byte) level.ordinal();

                index++;
                if (index >= BUF_SIZE) {
                    startFlushThread(false);
                }
            }
        }
    }

    public static void startFlushThread(boolean sync) {
        if (enableLogging == false) {
            return;
        }

        int len = index;
        byte[] old_levels = levels;
        long[] old_times = times;
        String[] old_contents = contents;

        // 初期化
        index = 0;
        levels = new byte[BUF_SIZE];
        times = new long[BUF_SIZE];
        contents = new String[BUF_SIZE];

        // スレッドインスタンス化
        FlushThread thread = new FlushThread(len, old_levels, old_times, old_contents);

        // 同期実行ならrunを呼ぶ
        if (sync) {
            thread.run();
        }
        // 非同期ならスレッド開始
        else {
            thread.start();
        }
    }

    private static List<File> listLogFiles(File outdir) {

        List<File> logfiles = new ArrayList<File>();
        try {
            if (outdir.exists()) {
                File[] files = outdir.listFiles();
                if (files != null) {
                    for (int i = 0; i < files.length; i++) {
                        String name = files[i].getName();
                        if (name.startsWith("log.") && name.endsWith(".txt")) {
                            logfiles.add(files[i]);
                        }
                    }
                }
            }
        }
        catch (Throwable e) {
            Log.w(Const.LOGTAG, "listing failed", e);
        }

        return logfiles;
    }

    @SuppressLint("SimpleDateFormat")
    static public File archive() throws Throwable {
        if (enableLogging == false) {
            return null;
        }

        File archived = null;
        File outdir = new File(Environment.getExternalStorageDirectory(), Const.LOGDIR);

        // リスティング
        List<File> logfiles = listLogFiles(outdir);
        if (logfiles.size() == 0) {
            return null;
        }

        // 並び替え
        Collections.sort(logfiles, new Comparator<File>() {
            @Override
            public int compare(File f1, File f2) {
                return f1.getName().compareTo(f2.getName());
            }
        });

        // 圧縮
        InputStream is = null;
        ZipOutputStream zos = null;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmss.SSS");
            String filename = sdf.format(new Date());
            archived = new File(outdir, "log." + filename + ".zip");
            zos = new ZipOutputStream(new FileOutputStream(archived));
            is = null;

            // ZipEntry追加
            ZipEntry ze = new ZipEntry("log." + filename + ".txt");
            zos.putNextEntry(ze);

            byte[] buf = new byte[1024];
            for (File file : logfiles) {
                is = new FileInputStream(file);
                int len = 0;
                while ((len = is.read(buf)) != -1) {
                    zos.write(buf, 0, len);
                }
                zos.write(FILE_SEPARATER);
                is.close();
            }
            zos.closeEntry();
            zos.close();
        }
        catch (Throwable e) {
            Log.w(Const.LOGTAG, "archiving failed", e);
            throw e;
        }
        finally {
            if (is != null) {
                try {
                    is.close();
                }
                catch (IOException e) {
                }
            }
            if (zos != null) {
                try {
                    zos.close();
                }
                catch (IOException e) {
                }
            }
        }

        // 圧縮に成功したら削除
        try {
            for (File file : logfiles) {
                file.delete();
            }
        }
        catch (Exception e) {
            Log.w(Const.LOGTAG, "delete failed 1, " + e.toString());
            // 続行
        }

        return archived;
    }

    public static void startDeleteOldFileThread() {

        new Thread(new Runnable() {

            @Override
            public void run() {

                try {
                    File outdir = new File(Environment.getExternalStorageDirectory(), Const.LOGDIR);

                    // リスティング
                    List<File> logfiles = listLogFiles(outdir);
                    if (logfiles.size() == 0) {
                        return;
                    }

                    long now = System.currentTimeMillis();
                    for (File file : logfiles) {
                        if (now >= file.lastModified() + OLD_LOG_DEL_THRESHOLD_MS) {
                            file.delete();
                        }
                    }
                }
                catch (Exception e) {
                    Log.w(Const.LOGTAG, "delete failed 2, " + e.toString());
                    // 続行
                }
            }
        }).start();
    }

    public static void setEnableLogging(boolean enabled) {
        Logger.enableLogging = enabled;
    }

    static class FlushThread extends Thread {
        int len = index;
        byte[] old_levels = levels;
        long[] old_times = times;
        String[] old_contents = contents;
        
        public FlushThread(int len, byte[] old_levels, long[] old_times, String[] old_contents) {
            this.len = len;
            this.old_levels = old_levels;
            this.old_times = old_times;
            this.old_contents = old_contents;
        }

        @Override
        @SuppressLint("SimpleDateFormat")
        public void run() {

            long start = System.currentTimeMillis();

            StringBuilder sb = new StringBuilder();
            SimpleDateFormat sdf = new SimpleDateFormat("MM-dd HH:mm:ss.SSS");
            for (int i = 0; i < len; i++) {
                sb.append(sdf.format(new Date(old_times[i])));
                sb.append(" ");
                sb.append(LEVEL_STR[old_levels[i]]);
                sb.append(" ");
                sb.append(old_contents[i]);
                sb.append("\n");
            }

            BufferedOutputStream bos = null;
            try {
                String state = Environment.getExternalStorageState();
                if (Environment.MEDIA_MOUNTED.equals(state)) {
                    File outdir = new File(Environment.getExternalStorageDirectory(), Const.LOGDIR);
                    if (outdir.exists() == false) {
                        if (outdir.mkdirs() == false) {
                            Log.e(Const.LOGTAG, "mkdirs failed : " + outdir.getAbsolutePath());
                        }
                    }
                    sdf = new SimpleDateFormat("yyyyMMdd-HHmmss.SSS");
                    String filename = sdf.format(new Date());
                    File outfile = new File(outdir, "log." + filename + ".txt");
                    Log.v(Const.LOGTAG, "logging outfile : " + outfile.getAbsolutePath());

                    outfile.createNewFile();
                    bos = new BufferedOutputStream(new FileOutputStream(outfile), 8192);
                    bos.write(sb.toString().getBytes("utf-8"));
                    bos.flush();
                }
                else {
                    Log.w(Const.LOGTAG, "logger error, external storage not found");
                }
            }
            catch (Throwable e) {
                Log.e(Const.LOGTAG, "logger writing error", e);
            }
            finally {
                if (bos != null) {
                    try {
                        bos.close();
                    }
                    catch (IOException e) {
                        Log.i(Const.LOGTAG, "logger close error");
                    }
                }
            }

            Log.i(Const.LOGTAG, "logging time : " + (System.currentTimeMillis() - start) + "ms");
        }
    }
}
