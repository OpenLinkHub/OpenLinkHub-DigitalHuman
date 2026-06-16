package com.openlinkhub.digitalhuman.runtime.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "FunASR 实时识别响应")
public record FunAsrRecognitionResponse(
        @Schema(description = "DashScope 请求 ID")
        String requestId,
        @Schema(description = "最终识别文本")
        String text,
        @Schema(description = "识别过程中的事件列表")
        List<FunAsrRecognitionEvent> events,
        @Schema(description = "首包延迟，单位毫秒")
        long firstPackageDelayMs,
        @Schema(description = "末包延迟，单位毫秒")
        long lastPackageDelayMs,
        @Schema(description = "上传音频字节数")
        int audioBytes,
        @Schema(description = "发送到 DashScope 的音频帧数量")
        int frameCount
) {
}
