package com.assistant.util;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * URL 内容抓取工具
 *
 * 支持的场景：
 * - 技术博客（掘金、CSDN、博客园、微信公众号等）→ Jsoup 提取正文
 * - GitHub 仓库 → API 获取 README
 * - arXiv 论文 → Jsoup 提取摘要
 *
 * 不支持：需要登录的页面、SPA 纯前端渲染页面
 */
public class UrlFetcher {

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";

    public record FetchResult(String title, String content) {
    }

    public static FetchResult fetch(String url) {
        if (isGitHubRepo(url)) {
            return fetchGitHubReadme(url);
        }
        return fetchWebPage(url);
    }

    private static boolean isGitHubRepo(String url) {
        return url.contains("github.com") &&
                !url.contains("/blob/") &&
                !url.contains("/tree/") &&
                !url.contains("/issues/") &&
                !url.contains("/pull/");
    }

    /**
     * 抓取 GitHub 仓库 README
     * 使用 GitHub API: GET /repos/{owner}/{repo}/readme
     */
    private static FetchResult fetchGitHubReadme(String url) {
        try {
            // 提取 owner/repo
            URI uri = new URI(url);
            String path = uri.getPath();
            String[] parts = path.split("/");
            if (parts.length < 3) {
                return new FetchResult("GitHub", "无法解析仓库地址");
            }
            String owner = parts[1];
            String repo = parts[2];

            // GitHub API 获取 README 内容
            String apiUrl = "https://api.github.com/repos/" + owner + "/" + repo + "/readme";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Accept", "application/vnd.github.raw+json")
                    .header("User-Agent", "KnowledgeAssistant")
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String title = owner + "/" + repo;
                String content = response.body();
                // 限制长度
                if (content.length() > 100_000) {
                    content = content.substring(0, 100_000);
                }
                return new FetchResult(title, content);
            }

            return new FetchResult(owner + "/" + repo, "GitHub API 返回状态码: " + response.statusCode());
        } catch (Exception e) {
            return new FetchResult("GitHub", "抓取失败: " + e.getMessage());
        }
    }

    /**
     * 抓取普通网页内容
     * 用 Jsoup 提取 <article> 或主要正文内容
     */
    private static FetchResult fetchWebPage(String url) {
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .timeout(15000)
                    .get();

            String title = doc.title();
            if (title == null || title.isBlank()) {
                title = "无标题";
            }

            // 优先提取 <article> 标签内容
            String content = extractContent(doc);

            if (content.isBlank()) {
                return new FetchResult(title, "未能提取到正文内容，该页面可能需要登录或为纯前端渲染");
            }

            return new FetchResult(title, content);
        } catch (Exception e) {
            return new FetchResult("抓取失败", "无法访问该链接: " + e.getMessage());
        }
    }

    private static String extractContent(Document doc) {
        // 移除不需要的元素
        doc.select("script, style, nav, header, footer, aside, .ad, .comment, .sidebar, .nav").remove();

        // 优先级1: <article> 标签
        Elements article = doc.select("article");
        if (!article.isEmpty()) {
            return article.text();
        }

        // 优先级2: 常见内容容器选择器
        String[] contentSelectors = {
                ".post-content", ".article-content", ".entry-content",
                ".content", "#content", ".markdown-body",
                ".topic-content", ".rich_media_content",  // 微信公众号
                "#article-content", ".blog-content"
        };
        for (String selector : contentSelectors) {
            Elements elements = doc.select(selector);
            if (!elements.isEmpty()) {
                return elements.text();
            }
        }

        // 优先级3: 取 <main> 或 <body>
        Elements main = doc.select("main");
        if (!main.isEmpty()) {
            return main.text();
        }

        // 最后兜底：取 body 但限制长度
        String bodyText = doc.body().text();
        if (bodyText.length() > 500) {
            return bodyText;
        }

        return "";
    }
}
