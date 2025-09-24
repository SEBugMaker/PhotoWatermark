import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

/**
 * 负责保存 / 加载上次关闭时的界面设置（快照）。
 * 使用 WatermarkTemplate 结构保存当前所有参数，另外保存当时选中的模板ID（如果有）。
 */
public class LastSessionManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public static File getLastSessionFile() {
        // 复用 TemplateManager 的目录逻辑
        File templatesFile = TemplateManager.getTemplatesFile();
        return new File(templatesFile.getParentFile(), "last_session.json");
    }

    public static LastSession load() {
        File f = getLastSessionFile();
        if (!f.exists()) return null;
        try (Reader r = new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8)) {
            return GSON.fromJson(r, LastSession.class);
        } catch (Exception e) {
            // 备份损坏
            try { Files.copy(f.toPath(), new File(f.getParentFile(), f.getName()+".corrupt-"+System.currentTimeMillis()).toPath(), StandardCopyOption.REPLACE_EXISTING); } catch (Exception ignore) {}
            return null;
        }
    }

    public static boolean save(LastSession session) {
        if (session == null) return false;
        File f = getLastSessionFile();
        File tmp = new File(f.getParentFile(), f.getName()+".tmp");
        try (Writer w = new OutputStreamWriter(new FileOutputStream(tmp), StandardCharsets.UTF_8)) {
            GSON.toJson(session, w);
        } catch (IOException e) {
            return false;
        }
        try {
            Files.move(tmp.toPath(), f.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            try { Files.copy(tmp.toPath(), f.toPath(), StandardCopyOption.REPLACE_EXISTING); tmp.delete(); } catch (IOException ex) { return false; }
        }
        return true;
    }
}

class LastSession {
    String lastTemplateId; // 用户当时选中的模板（如果选择了）
    WatermarkTemplate lastSettings; // 完整快照
    long savedAt;
}

