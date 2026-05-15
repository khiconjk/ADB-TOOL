package com.khiconjk.rootadbenabler;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public final class SuRunner {
    private SuRunner() {}

    public static Result run(String command) {
        StringBuilder out = new StringBuilder();
        int code = -1;
        try {
            Process process = new ProcessBuilder("su", "-c", command)
                    .redirectErrorStream(true)
                    .start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    out.append(line).append('\n');
                }
            }

            code = process.waitFor();
        } catch (Exception e) {
            out.append("EXCEPTION: ").append(e.getClass().getSimpleName()).append(": ").append(e.getMessage()).append('\n');
        }
        return new Result(code, out.toString());
    }

    public static boolean hasRoot() {
        Result r = run("id");
        return r.exitCode == 0 && r.output.contains("uid=0");
    }

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
}
