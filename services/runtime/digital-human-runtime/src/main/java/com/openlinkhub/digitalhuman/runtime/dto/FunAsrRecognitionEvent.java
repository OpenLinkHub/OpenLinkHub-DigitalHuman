package com.openlinkhub.digitalhuman.runtime.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "FunASR 实时识别事件")
public record FunAsrRecognitionEvent(
        @Schema(description = "DashScope 请求 ID")
        String requestId,
        @Schema(description = "当前句子或中间识别文本")
        String text,
        @Schema(description = "句子开始时间，单位毫秒")
        Long beginTime,
        @Schema(description = "句子结束时间，单位毫秒")
        Long endTime,
        @Schema(description = "是否句子开始事件")
        boolean sentenceBegin,
        @Schema(description = "是否句子结束事件")
        boolean sentenceEnd,
        @Schema(description = "是否完整结果")
        boolean completeResult
) {
}
