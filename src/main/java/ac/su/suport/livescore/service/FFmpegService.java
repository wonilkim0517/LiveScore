package ac.su.suport.livescore.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class FFmpegService {

    @Value("${ffmpeg.path}")
    private String ffmpegPath;

    @Value("${rtmp.server.url}")
    private String rtmpServerUrl;

    private final ConcurrentHashMap<String, Process> runningProcesses = new ConcurrentHashMap<>();

    public void startStreaming(String streamId) {
        String outputPath = "/tmp/hls/" + streamId;
        new File(outputPath).mkdirs();
        String[] command = {
                ffmpegPath,
                "-f", "avfoundation",
                "-framerate", "30",
                "-video_size", "1280x720",
                "-i", "0:0",
                "-c:v", "libx264",
                "-preset", "ultrafast",
                "-tune", "zerolatency",
                "-c:a", "aac",
                "-ar", "48000",
                "-b:a", "128k",
                "-f", "hls",
                "-hls_time", "2",
                "-hls_list_size", "5",
                "-hls_flags", "delete_segments+append_list",
                "-hls_segment_filename", outputPath + "/%03d.ts",
                outputPath + "/playlist.m3u8"
        };

        String[] webmCommand = {
                ffmpegPath,
                "-f", "avfoundation",
                "-framerate", "30",
                "-video_size", "1280x720",
                "-i", "0:0",
                "-c:v", "libvpx-vp9",
                "-crf", "30",
                "-b:v", "0",
                "-b:a", "128k",
                "-c:a", "libopus",
                outputPath + "/" + streamId + ".webm"
        };

        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                log.info("FFmpeg: " + line);
            }
            runningProcesses.put(streamId, process);
            log.info("Started streaming for stream ID: {}", streamId);

            // Start WebM recording
            ProcessBuilder webmPb = new ProcessBuilder(webmCommand);
            webmPb.redirectErrorStream(true);
            Process webmProcess = webmPb.start();
            runningProcesses.put(streamId + "_webm", webmProcess);
            log.info("Started WebM recording for stream ID: {}", streamId);
        } catch (IOException e) {
            log.error("Error starting stream for stream ID: {}", streamId, e);
        }
    }

    public void stopStreaming(String streamId) {
        Process process = runningProcesses.remove(streamId);
        if (process != null) {
            process.destroy();
            log.info("Stopped streaming for stream ID: {}", streamId);
        } else {
            log.warn("No running stream found for stream ID: {}", streamId);
        }

        Process webmProcess = runningProcesses.remove(streamId + "_webm");
        if (webmProcess != null) {
            webmProcess.destroy();
            log.info("Stopped WebM recording for stream ID: {}", streamId);
        } else {
            log.warn("No running WebM recording found for stream ID: {}", streamId);
        }
    }
}