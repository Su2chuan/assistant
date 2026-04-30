package com.assistant.util;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 文件解析工具类
 *
 * 支持解析以下格式的文件：
 * - PDF (.pdf) — 使用 Apache PDFBox
 * - Word (.docx) — 使用 Apache POI
 * - Markdown (.md) / 纯文本 (.txt) — 直接读取
 *
 * 返回文件的纯文本内容，供后续分块和向量化使用
 */
public class FileParserUtil {

    /**
     * 根据文件扩展名自动选择解析器
     *
     * @param inputStream 文件输入流
     * @param filename 文件名（用于判断扩展名）
     * @return 解析后的纯文本内容
     */
    public static String parse(InputStream inputStream, String filename) throws IOException {
        String extension = getFileExtension(filename).toLowerCase();

        return switch (extension) {
            case "pdf" -> parsePdf(inputStream);
            case "docx" -> parseDocx(inputStream);
            case "md", "txt", "markdown" -> parseText(inputStream);
            default -> throw new IllegalArgumentException("不支持的文件格式: " + extension);
        };
    }

    /**
     * 解析 PDF 文件
     *
     * PDFBox 会提取每一页的文本，自动处理换行和空格
     */
    private static String parsePdf(InputStream inputStream) throws IOException {
        try (PDDocument document = Loader.loadPDF(inputStream.readAllBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);  // 按页面位置排序
            return stripper.getText(document);
        }
    }

    /**
     * 解析 Word 文档 (.docx)
     *
     * POI 会遍历所有段落，拼接为纯文本
     */
    private static String parseDocx(InputStream inputStream) throws IOException {
        try (XWPFDocument document = new XWPFDocument(inputStream)) {
            StringBuilder text = new StringBuilder();
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                String paraText = paragraph.getText();
                if (paraText != null && !paraText.isBlank()) {
                    text.append(paraText).append("\n");
                }
            }
            return text.toString();
        }
    }

    /**
     * 解析纯文本文件（Markdown、TXT 等）
     */
    private static String parseText(InputStream inputStream) throws IOException {
        return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    }

    /**
     * 获取文件扩展名
     */
    private static String getFileExtension(String filename) {
        if (filename == null) return "";
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(lastDot + 1) : "";
    }
}