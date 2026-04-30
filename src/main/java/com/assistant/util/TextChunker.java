package com.assistant.util;

import java.util.ArrayList;
import java.util.List;

/**
 * 文本分块工具类
 *
 * 每块约 500 字符，块之间重叠 50 字符，保证 start 每次至少前进 chunkSize/2 防止死循环
 */
public class TextChunker {

    private static final int DEFAULT_CHUNK_SIZE = 500;
    private static final int DEFAULT_OVERLAP_SIZE = 50;

    /**
     * 按固定大小分块（带重叠）
     */
    public static List<String> chunk(String text) {
        return chunk(text, DEFAULT_CHUNK_SIZE, DEFAULT_OVERLAP_SIZE);
    }

    public static List<String> chunk(String text, int chunkSize, int overlap) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        String cleanedText = text.replaceAll("\\s+", " ").trim();
        int textLen = cleanedText.length();

        // 限制最大处理长度，防止 OOM
        int maxLen = 500_000;
        if (textLen > maxLen) {
            cleanedText = cleanedText.substring(0, maxLen);
            textLen = maxLen;
        }

        List<String> chunks = new ArrayList<>();
        int start = 0;

        while (start < textLen) {
            int end = Math.min(start + chunkSize, textLen);

            String chunk = cleanedText.substring(start, end).trim();
            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }

            // 保证每次至少前进 chunkSize - overlap，防止死循环
            int step = chunkSize - overlap;
            start += Math.max(step, 1);
        }

        return chunks;
    }
}