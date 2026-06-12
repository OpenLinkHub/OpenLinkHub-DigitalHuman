package com.openlinkhub.digitalhuman.funasr.service;

import com.openlinkhub.digitalhuman.funasr.exception.FunAsrException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Component
public class FfmpegAudioTranscoder {

    public byte[] toPcm16kMono(byte[] audio) {
        Path input = null;
        Path output = null;
        try {
            input = Files.createTempFile("funasr-input-", ".m4a");
            output = Files.createTempFile("funasr-output-", ".pcm");
            Files.write(input, audio);

            List<String> command = List.of(
                    "ffmpeg",
                    "-hide_banner",
                    "-loglevel", "error",
                    "-y",
                    "-i", input.toString(),
                    "-ac", "1",
                    "-ar", "16000",
                    "-f", "s16le",
                    "-acodec", "pcm_s16le",
                    output.toString()
            );

            Process process = new ProcessBuilder(command).start();
            String errorOutput = new String(process.getErrorStream().readAllBytes());
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new FunAsrException("Failed to transcode audio with ffmpeg: " + errorOutput.strip());
            }
            return Files.readAllBytes(output);
        } catch (IOException exception) {
            throw new FunAsrException("Failed to transcode audio. Ensure ffmpeg is installed and available in PATH.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new FunAsrException("Interrupted while transcoding audio.", exception);
        } finally {
            deleteIfExists(input);
            deleteIfExists(output);
        }
    }

    private void deleteIfExists(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // Temporary file cleanup failure should not hide the recognition result.
        }
    }
}
