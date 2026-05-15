package com.khiconjk.rootadbenabler;

import android.content.Context;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public final class RootShell {
    private RootShell() {}

    public static final class Result {
        public final int exitCode;
        public final String output;

        public Result(int exitCode, String output) {
            this.exitCode = exitCode;
            this.output = output == null ? "" : output;
        }

        public boolean success() {
            return exitCode == 0;
        }
    }

    private static final String[] SU_CANDIDATES = new String[] {
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/su/bin/su",
            "/data/adb/ksu/bin/su",
            "/data/adb/magisk/su",
            "/debug_ramdisk/su"
    };

    public static Result run(Context context, String script) {
        StringBuilder log = new StringBuilder();

        try {
            String suPath = findWorkingSu(log);

            if (suPath == null) {
                log.append("\nERROR: Không tìm thấy su hoạt động.\n");
                log.append("Hãy kiểm tra KernelSU / KernelSU Next / Magisk đã bật quyền root cho app chưa.\n");
                log.append("Package app: com.khiconjk.rootadbenabler\n");
                return new Result(-1, log.toString());
            }

            log.append("\nUSING_SU=").append(suPath).append("\n");

            File scriptFile = new File(context.getCacheDir(), "root_cmd.sh");
            FileWriter writer = new FileWriter(scriptFile, false);
            writer.write("#!/system/bin/sh\n");
            writer.write("export PATH=/system/bin:/system/xbin:/vendor/bin:/odm/bin:/product/bin:/data/adb/ksu/bin:/data/adb/magisk:/debug_ramdisk:$PATH\n");
            writer.write(script == null ? "" : script);
            writer.write("\n");
            writer.flush();
            writer.close();

            scriptFile.setReadable(true, false);
            scriptFile.setWritable(true, true);
            scriptFile.setExecutable(true, false);

            List<String> cmd = new ArrayList<>();
            cmd.add(suPath);
            cmd.add("-c");
            cmd.add("sh '" + scriptFile.getAbsolutePath() + "'");

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            pb.environment().put("PATH", "/system/bin:/system/xbin:/vendor/bin:/odm/bin:/product/bin:/data/adb/ksu/bin:/data/adb/magisk:/debug_ramdisk");

            Process p = pb.start();
            String out = readAll(p.getInputStream());

            boolean finished = p.waitFor(90, TimeUnit.SECONDS);
            if (!finished) {
                p.destroyForcibly();
                log.append(out);
                log.append("\nERROR: Root command timeout\n");
                return new Result(-2, log.toString());
            }

            int code = p.exitValue();

            log.append(out);
            log.append("\nExit code: ").append(code).append("\n");

            return new Result(code, log.toString());

        } catch (Exception e) {
            log.append("\nEXCEPTION: ")
                    .append(e.getClass().getSimpleName())
                    .append(": ")
                    .append(e.getMessage())
                    .append("\n");
            return new Result(-1, log.toString());
        }
    }

    public static Result run(String script) {
        StringBuilder log = new StringBuilder();

        try {
            String suPath = findWorkingSu(log);

            if (suPath == null) {
                log.append("\nERROR: Không tìm thấy su hoạt động.\n");
                return new Result(-1, log.toString());
            }

            log.append("\nUSING_SU=").append(suPath).append("\n");

            ProcessBuilder pb = new ProcessBuilder(
                    suPath,
                    "-c",
                    "export PATH=/system/bin:/system/xbin:/vendor/bin:/odm/bin:/product/bin:/data/adb/ksu/bin:/data/adb/magisk:/debug_ramdisk:$PATH; " + script
            );

            pb.redirectErrorStream(true);
            pb.environment().put("PATH", "/system/bin:/system/xbin:/vendor/bin:/odm/bin:/product/bin:/data/adb/ksu/bin:/data/adb/magisk:/debug_ramdisk");

            Process p = pb.start();
            String out = readAll(p.getInputStream());

            boolean finished = p.waitFor(90, TimeUnit.SECONDS);
            if (!finished) {
                p.destroyForcibly();
                log.append(out);
                log.append("\nERROR: Root command timeout\n");
                return new Result(-2, log.toString());
            }

            int code = p.exitValue();

            log.append(out);
            log.append("\nExit code: ").append(code).append("\n");

            return new Result(code, log.toString());

        } catch (Exception e) {
            log.append("\nEXCEPTION: ")
                    .append(e.getClass().getSimpleName())
                    .append(": ")
                    .append(e.getMessage())
                    .append("\n");
            return new Result(-1, log.toString());
        }
    }

    private static String findWorkingSu(StringBuilder log) {
        log.append("===== FIND SU =====\n");

        for (String path : SU_CANDIDATES) {
            File f = new File(path);
            log.append("check ").append(path)
                    .append(" exists=").append(f.exists())
                    .append(" canExecute=").append(f.canExecute())
                    .append("\n");

            if (f.exists()) {
                if (testSu(path, log)) {
                    return path;
                }
            }
        }

        log.append("check su from PATH\n");
        if (testSu("su", log)) {
            return "su";
        }

        return null;
    }

    private static boolean testSu(String suPath, StringBuilder log) {
        try {
            ProcessBuilder pb = new ProcessBuilder(suPath, "-c", "id");
            pb.redirectErrorStream(true);
            pb.environment().put("PATH", "/system/bin:/system/xbin:/vendor/bin:/odm/bin:/product/bin:/data/adb/ksu/bin:/data/adb/magisk:/debug_ramdisk");

            Process p = pb.start();
            String out = readAll(p.getInputStream());

            boolean finished = p.waitFor(12, TimeUnit.SECONDS);
            if (!finished) {
                p.destroyForcibly();
                log.append("test ").append(suPath).append(" TIMEOUT\n");
                return false;
            }

            int code = p.exitValue();

            log.append("test ").append(suPath)
                    .append(" exit=").append(code)
                    .append(" output=").append(out.replace("\n", " "))
                    .append("\n");

            return code == 0 && out.contains("uid=0");

        } catch (Exception e) {
            log.append("test ").append(suPath)
                    .append(" exception=")
                    .append(e.getClass().getSimpleName())
                    .append(": ")
                    .append(e.getMessage())
                    .append("\n");
            return false;
        }
    }

    private static String readAll(InputStream inputStream) throws Exception {
        StringBuilder sb = new StringBuilder();
        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));

        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line).append('\n');
        }

        return sb.toString();
    }
}
