package com.openlinkhub.digitalhuman.runtime.api.rest;

import com.openlinkhub.digitalhuman.runtime.dto.FunAsrRecognitionResponse;
import com.openlinkhub.digitalhuman.runtime.asr.FunAsrRealtimeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/runtime/asr")
@Tag(name = "FunASR 实时识别", description = "DashScope FunASR realtime speech recognition APIs")
public class FunAsrController {

    private final FunAsrRealtimeService funAsrRealtimeService;

    public FunAsrController(FunAsrRealtimeService funAsrRealtimeService) {
        this.funAsrRealtimeService = funAsrRealtimeService;
    }

    @GetMapping("/health")
    @Operation(summary = "健康检查", description = "检查 digital-human-runtime 服务是否可用。")
    public Map<String, String> health() {
        return Map.of("status", "ok", "service", "digital-human-runtime");
    }

    @PostMapping(value = "/realtime/recognize", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "上传音频并执行 FunASR 实时识别",
            description = "将上传的音频文件按实时流式方式切片发送到 DashScope FunASR，并返回识别事件、最终文本和延迟指标。"
    )
    public FunAsrRecognitionResponse recognize(
            @Parameter(description = "音频文件。PCM 推荐使用 16kHz、单声道、16-bit little-endian。", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "音频格式，默认读取 dashscope.funasr.format，例如 pcm。")
            @RequestParam(value = "format", required = false) String format,
            @Parameter(description = "采样率，默认读取 dashscope.funasr.sample-rate，例如 16000。")
            @RequestParam(value = "sampleRate", required = false) Integer sampleRate
    ) throws IOException {
        return funAsrRealtimeService.recognize(
                file.getBytes(),
                file.getOriginalFilename(),
                file.getContentType(),
                format,
                sampleRate
        );
    }
}
