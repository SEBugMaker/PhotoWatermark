import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

/**
 * 管理水印模板的加载与保存（单一 JSON 文件）。
 * 文件结构：{"schemaVersion":1,"templates":[ ... ]}
 */
public class TemplateManager {
    private static final int SCHEMA_VERSION = 1;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public static File getTemplatesFile() {
        String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        File baseDir;
        if (os.contains("mac")) {
            baseDir = new File(System.getProperty("user.home"), "Library/Application Support/PhotoWatermark");
        } else if (os.contains("win")) {
            String appData = System.getenv("APPDATA");
            if (appData != null && !appData.isEmpty()) {
                baseDir = new File(appData, "PhotoWatermark");
            } else {
                baseDir = new File(System.getProperty("user.home"), "AppData/Roaming/PhotoWatermark");
            }
        } else { // linux / unix
            baseDir = new File(System.getProperty("user.home"), ".config/PhotoWatermark");
        }
        if (!baseDir.exists()) baseDir.mkdirs();
        return new File(baseDir, "templates.json");
    }

    public static List<WatermarkTemplate> loadTemplates() {
        File f = getTemplatesFile();
        if (!f.exists()) return new ArrayList<>();
        try (Reader r = new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8)) {
            TemplateStore store = GSON.fromJson(r, TemplateStore.class);
            if (store == null || store.templates == null) return new ArrayList<>();
            // 过滤 schemaVersion 不兼容但暂时仍加载
            return new ArrayList<>(store.templates);
        } catch (IOException | JsonSyntaxException e) {
            // 备份损坏文件
            try {
                File bak = new File(f.getParentFile(), f.getName() + ".corrupt-" + System.currentTimeMillis());
                Files.copy(f.toPath(), bak.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception ignore) {}
            return new ArrayList<>();
        }
    }

    public static boolean saveTemplates(List<WatermarkTemplate> templates) {
        File f = getTemplatesFile();
        TemplateStore store = new TemplateStore();
        store.schemaVersion = SCHEMA_VERSION;
        store.templates = new ArrayList<>(templates);
        File tmp = new File(f.getParentFile(), f.getName() + ".tmp");
        try (Writer w = new OutputStreamWriter(new FileOutputStream(tmp), StandardCharsets.UTF_8)) {
            GSON.toJson(store, w);
        } catch (IOException e) {
            return false;
        }
        try {
            Files.move(tmp.toPath(), f.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            // 回退为普通替换
            try {
                Files.copy(tmp.toPath(), f.toPath(), StandardCopyOption.REPLACE_EXISTING);
                tmp.delete();
            } catch (IOException ex) {
                return false;
            }
        }
        return true;
    }
}

class TemplateStore {
    int schemaVersion = 1;
    List<WatermarkTemplate> templates = new ArrayList<>();
}

class WatermarkTemplate {
    String id; // UUID
    String name;
    String description;
    long createdAt;
    long updatedAt;

    // 模式: TEXT / IMAGE
    String mode;

    // 通用参数
    String position; // 位置或 CUSTOM
    Integer customX; // 仅 CUSTOM
    Integer customY;
    double rotationDegrees;

    int targetWidth; // 目标宽, 0 保持
    int targetHeight; // 目标高, 0 保持
    int scalePercent; // 缩放百分比 10-200 (100 为原始)

    // 导出命名/输出
    String namingRule;
    String prefix;
    String suffix;
    String outputFormat; // JPEG / PNG
    int jpegQuality;

    // 文本水印
    String text;
    String fontName;
    int fontSize;
    boolean bold;
    boolean italic;
    String color; // #RRGGBB
    int textOpacity; // 0-100
    boolean shadow;
    boolean stroke;

    // 图片水印
    String watermarkImagePath; // 可为空
    double watermarkScale; // 0-1
    int watermarkOpacity; // 0-100

    @Override
    public String toString() {
        return name != null ? name : "未命名模板";
    }
}
