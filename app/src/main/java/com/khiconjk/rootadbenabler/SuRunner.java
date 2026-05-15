package com.khiconjk.rootadbenabler;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

public final class SuRunner {
    private SuRunner() {}

    public static final class Result {
        public final int exitCode;
        public final String output;

        public Result(int exitCode, String output) {
            this.exitCode = exitCode;
            this.output = output == null ? "" : output;
        }

        public boolean ok() {
            return exitCode == 0;
        }
    }

    private static final String ROOT_PATH =
            "/system/bin:/system/xbin:/vendor/bin:/odm/bin:/product/bin:" +
            "/data/adb/ksu/bin:/data/adb/magisk:/debug_ramdisk:/sbin";

    private static final String[] SU_CANDIDATES = new String[] {
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/su/bin/su",
            "/data/adb/ksu/bin/su",
            "/data/adb/magisk/su",
            "/debug_ramdisk/su",
            "/vendor/bin/su",
            "/product/bin/su",
            "su"
    };

    public static Result run(String script) {
        StringBuilder log = new StringBuilder();

        try {
            log.append("===== FIND SU =====\n");

            String suPath = findWorkingSu(log);

            if (suPath == null) {
                log.append("\nERROR: Không tìm thấy su hoạt động.\n");
                log.append("Nguyên nhân thường gặp:\n");
                log.append("- App chưa được cấp root trong KernelSU / KernelSU Next / Magisk.\n");
                log.append("- ROM/root không expose binary su cho app thường.\n");
                log.append("- App cần mở lại sau khi cấp quyền root.\n");
                log.append("\nPackage: com.khiconjk.rootadbenabler\n");
                return new Result(-1, log.toString());
            }

            log.append("\nUSING_SU=").append(suPath).append("\n");
            log.append("===== RUN ROOT SCRIPT =====\n");

            String safeScript =
                    "export PATH=" + ROOT_PATH + ":$PATH\n" +
                    "id\n" +
                    "\n" +
                    (script == null ? "" : script) +
                    "\n";

            ProcessBuilder pb = new ProcessBuilder(
                    suPath,
                    "-c",
                    safeScript
            );

            pb.redirectErrorStream(true);
            pb.environment().put("PATH", ROOT_PATH);

            Process process = pb.start();
            String output = readAll(process.getInputStream());

            boolean finished = process.waitFor(120, TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                log.append(output);
                log.append("\nERROR: Root command timeout\n");
                return new Result(-2, log.toString());
            }

            int exitCode = process.exitValue();

            log.append(output);

            return new Result(exitCode, log.toString());

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
        for (String path : SU_CANDIDATES) {
            if (!"su".equals(path)) {
                File f = new File(path);

                log.append("check ")
                        .append(path)
                        .append(" exists=")
                        .append(f.exists())
                        .append(" canExecute=")
                        .append(f.canExecute())
                        .append("\n");

                if (!f.exists()) {
                    continue;
                }
            } else {
                log.append("check su from PATH\n");
            }

            if (testSu(path, log)) {
                return path;
            }
        }

        return null;
    }

    private static boolean testSu(String suPath, StringBuilder log) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    suPath,
                    "-c",
                    "export PATH=" + ROOT_PATH + ":$PATH; id"
            );

            pb.redirectErrorStream(true);
            pb.environment().put("PATH", ROOT_PATH);

            Process process = pb.start();
            String output = readAll(process.getInputStream());

            boolean finished = process.waitFor(15, TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();

                log.append("test ")
                        .append(suPath)
                        .append(" TIMEOUT\n");

                return false;
            }

            int exitCode = process.exitValue();
            String oneLine = output.replace("\n", " ").replace("\r", " ");

            log.append("test ")
                    .append(suPath)
                    .append(" exit=")
                    .append(exitCode)
                    .append(" output=")
                    .append(oneLine)
                    .append("\n");

            return exitCode == 0 && output.contains("uid=0");

        } catch (Exception e) {
            log.append("test ")
                    .append(suPath)
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

        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;

            while ((line = br.readLine()) != null) {
                sb.append(line).append('\n');
            }
        }

        return sb.toString();
    }
}
