package com.example.application;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class WindowsToastScheduler {
    private static final String SCRIPT_RESOURCE_PATH = "/windows/toast.ps1";

    public void syncScheduledToasts(List<PlannedToast> toasts) throws IOException, InterruptedException {
        if (!isWindows()) {
            return;
        }
        List<PlannedToast> safe = toasts != null ? toasts : List.of();
        Path scriptPath = ensureScriptOnDisk();
        Path jsonPath = writeToastsJson(safe);
        String exePath = resolveCurrentCommand();
        runPowerShell(scriptPath, jsonPath, exePath);
    }

    private void runPowerShell(Path scriptPath, Path jsonPath, String exePath) throws IOException, InterruptedException {
        Objects.requireNonNull(scriptPath, "scriptPath");
        Objects.requireNonNull(jsonPath, "jsonPath");

        try {
            runShell("powershell.exe", scriptPath, jsonPath, exePath);
        } catch (IOException exception) {
            if (looksLikeMissingExecutable(exception)) {
                runShell("pwsh.exe", scriptPath, jsonPath, exePath);
            } else {
                throw exception;
            }
        }
    }

    private void runShell(String shellExe, Path scriptPath, Path jsonPath, String exePath) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        command.add(shellExe);
        command.add("-NoProfile");
        command.add("-NonInteractive");
        command.add("-ExecutionPolicy");
        command.add("Bypass");
        command.add("-File");
        command.add(scriptPath.toAbsolutePath().toString());
        command.add("-InputPath");
        command.add(jsonPath.toAbsolutePath().toString());
        command.add("-AppId");
        command.add(WindowsToastIds.TODO_AUMID);
        command.add("-AppName");
        command.add(WindowsToastIds.TODO_APP_NAME);
        command.add("-ExePath");
        command.add(exePath != null ? exePath : "");
        command.add("-ShortcutName");
        command.add(WindowsToastIds.TODO_SHORTCUT_NAME);

        ProcessBuilder builder = new ProcessBuilder(command);
        Process process = builder.start();

        String stdout = readAll(process.getInputStream());
        String stderr = readAll(process.getErrorStream());
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("Toast sync failed (exit " + exitCode + "): " + (stderr.isBlank() ? stdout : stderr));
        }
    }

    private boolean looksLikeMissingExecutable(IOException exception) {
        String message = exception.getMessage();
        if (message == null) {
            return false;
        }
        return message.contains("CreateProcess error=2") || message.contains("The system cannot find the file specified");
    }

    private Path writeToastsJson(List<PlannedToast> toasts) throws IOException {
        Path path = Files.createTempFile("todo-toasts-", "-" + Instant.now().toEpochMilli() + ".json");
        path.toFile().deleteOnExit();

        StringBuilder json = new StringBuilder();
        json.append('[');
        boolean first = true;
        for (PlannedToast toast : toasts) {
            if (toast == null || toast.id() == null || toast.id().isBlank()) {
                continue;
            }
            if (!first) {
                json.append(',');
            }
            first = false;
            json.append('{');
            appendJsonString(json, "id", toast.id());
            json.append(',');
            appendJsonNumber(json, "dueEpochMillis", toast.dueEpochMillis());
            json.append(',');
            appendJsonString(json, "title", toast.title());
            json.append(',');
            appendJsonString(json, "body", toast.body());
            json.append('}');
        }
        json.append(']');

        Files.writeString(path, json.toString(), StandardCharsets.UTF_8);
        return path;
    }

    private void appendJsonString(StringBuilder sb, String key, String value) {
        sb.append('"').append(escapeJson(key)).append('"').append(':');
        sb.append('"').append(escapeJson(value)).append('"');
    }

    private void appendJsonNumber(StringBuilder sb, String key, long value) {
        sb.append('"').append(escapeJson(key)).append('"').append(':');
        sb.append(value);
    }

    private String escapeJson(String raw) {
        if (raw == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(raw.length() + 8);
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            switch (c) {
                case '\\' -> out.append("\\\\");
                case '"' -> out.append("\\\"");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.toString();
    }

    private Path ensureScriptOnDisk() throws IOException {
        try (InputStream in = WindowsToastScheduler.class.getResourceAsStream(SCRIPT_RESOURCE_PATH)) {
            if (in == null) {
                throw new IOException("Missing toast script resource: " + SCRIPT_RESOURCE_PATH);
            }
            Path script = Files.createTempFile("todo-toast-", ".ps1");
            script.toFile().deleteOnExit();
            Files.write(script, readAllBytes(in));
            return script;
        }
    }

    private static byte[] readAllBytes(InputStream in) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int read;
        while ((read = in.read(chunk)) != -1) {
            buffer.write(chunk, 0, read);
        }
        return buffer.toByteArray();
    }

    private static String readAll(InputStream in) throws IOException {
        if (in == null) {
            return "";
        }
        return new String(readAllBytes(in), StandardCharsets.UTF_8);
    }

    private boolean isWindows() {
        String name = System.getProperty("os.name", "");
        return name.toLowerCase().contains("windows");
    }

    private String resolveCurrentCommand() {
        try {
            return ProcessHandle.current().info().command().orElse("");
        } catch (Exception ignored) {
            return "";
        }
    }
}
