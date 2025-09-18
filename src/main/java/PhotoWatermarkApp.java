import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Scanner;

public class PhotoWatermarkApp {

    // 水印位置枚举
    public enum WatermarkPosition {
        TOP_LEFT, TOP_CENTER, TOP_RIGHT,
        MIDDLE_LEFT, CENTER, MIDDLE_RIGHT,
        BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // 获取用户输入的图片路径
        System.out.print("请输入图片文件路径或目录路径: ");
        String path = scanner.nextLine();
        File inputPath = new File(path);

        // 检查路径是否存在
        if (!inputPath.exists()) {
            System.out.println("错误: 路径不存在。");
            scanner.close();
            return;
        }

        // 获取用户设置的字体大小
        System.out.print("请输入水印字体大小 (默认: 36): ");
        String fontSizeStr = scanner.nextLine();
        int fontSize = fontSizeStr.isEmpty() ? 36 : Integer.parseInt(fontSizeStr);

        // 获取用户设置的字体颜色
        System.out.print("请输入水印字体颜色 (默认: WHITE，可选: BLACK, RED, GREEN, BLUE, YELLOW): ");
        String colorStr = scanner.nextLine();
        Color color = getColorFromString(colorStr.isEmpty() ? "WHITE" : colorStr);

        // 获取用户设置的水印位置
        System.out.print("请输入水印位置 (默认: BOTTOM_RIGHT，可选: TOP_LEFT, TOP_CENTER, TOP_RIGHT, MIDDLE_LEFT, CENTER, MIDDLE_RIGHT, BOTTOM_LEFT, BOTTOM_CENTER): ");
        String positionStr = scanner.nextLine();
        WatermarkPosition position = getPositionFromString(positionStr.isEmpty() ? "BOTTOM_RIGHT" : positionStr);

        try {
            if (inputPath.isFile()) {
                // 处理单个图片文件
                processImage(inputPath, fontSize, color, position);
                System.out.println("水印添加成功！");
            } else if (inputPath.isDirectory()) {
                // 处理目录中的所有图片文件
                File[] files = inputPath.listFiles((dir, name) -> {
                    String lowerName = name.toLowerCase();
                    return lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") ||
                           lowerName.endsWith(".png") || lowerName.endsWith(".gif") ||
                           lowerName.endsWith(".bmp");
                });
                
                if (files == null || files.length == 0) {
                    System.out.println("错误: 目录中没有找到支持的图片文件。");
                } else {
                    System.out.println("找到 " + files.length + " 个图片文件，开始处理...");
                    int successCount = 0;
                    for (File file : files) {
                        try {
                            processImage(file, fontSize, color, position);
                            successCount++;
                        } catch (Exception e) {
                            System.out.println("处理文件 " + file.getName() + " 时出错: " + e.getMessage());
                        }
                    }
                    System.out.println("处理完成，成功添加水印的图片数量: " + successCount);
                }
            }
        } catch (Exception e) {
            System.out.println("处理时出错: " + e.getMessage());
            e.printStackTrace();
        }

        scanner.close();
    }

    // 从字符串获取颜色对象
    private static Color getColorFromString(String colorStr) {
        switch (colorStr.toUpperCase()) {
            case "BLACK": return Color.BLACK;
            case "RED": return Color.RED;
            case "GREEN": return Color.GREEN;
            case "BLUE": return Color.BLUE;
            case "YELLOW": return Color.YELLOW;
            default: return Color.WHITE;
        }
    }

    // 从字符串获取水印位置枚举
    private static WatermarkPosition getPositionFromString(String positionStr) {
        try {
            return WatermarkPosition.valueOf(positionStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return WatermarkPosition.BOTTOM_RIGHT;
        }
    }

    // 处理图片并添加水印
    private static void processImage(File imageFile, int fontSize, Color color, WatermarkPosition position) throws IOException, ParseException {
        // 读取图片
        BufferedImage image = ImageIO.read(imageFile);

        // 读取EXIF信息获取拍摄时间
        String watermarkText = getExifDateTime(imageFile);
        if (watermarkText == null) {
            // 如果没有EXIF时间信息，使用当前日期
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            watermarkText = dateFormat.format(new Date());
        }

        // 创建Graphics2D对象用于绘制水印
        Graphics2D g2d = image.createGraphics();

        // 设置抗锯齿，使文字更平滑
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 设置字体
        Font font = new Font("Arial", Font.PLAIN, fontSize);
        g2d.setFont(font);

        // 设置字体颜色
        g2d.setColor(color);

        // 获取文字边界框
        FontMetrics fontMetrics = g2d.getFontMetrics();
        int textWidth = fontMetrics.stringWidth(watermarkText);
        int textHeight = fontMetrics.getHeight();

        // 计算水印位置
        int x = 0, y = 0;
        int padding = 20; // 边距

        switch (position) {
            case TOP_LEFT:
                x = padding;
                y = padding + textHeight - fontMetrics.getDescent();
                break;
            case TOP_CENTER:
                x = (image.getWidth() - textWidth) / 2;
                y = padding + textHeight - fontMetrics.getDescent();
                break;
            case TOP_RIGHT:
                x = image.getWidth() - textWidth - padding;
                y = padding + textHeight - fontMetrics.getDescent();
                break;
            case MIDDLE_LEFT:
                x = padding;
                y = (image.getHeight() + textHeight) / 2 - fontMetrics.getDescent();
                break;
            case CENTER:
                x = (image.getWidth() - textWidth) / 2;
                y = (image.getHeight() + textHeight) / 2 - fontMetrics.getDescent();
                break;
            case MIDDLE_RIGHT:
                x = image.getWidth() - textWidth - padding;
                y = (image.getHeight() + textHeight) / 2 - fontMetrics.getDescent();
                break;
            case BOTTOM_LEFT:
                x = padding;
                y = image.getHeight() - padding - fontMetrics.getDescent();
                break;
            case BOTTOM_CENTER:
                x = (image.getWidth() - textWidth) / 2;
                y = image.getHeight() - padding - fontMetrics.getDescent();
                break;
            case BOTTOM_RIGHT:
                x = image.getWidth() - textWidth - padding;
                y = image.getHeight() - padding - fontMetrics.getDescent();
                break;
        }

        // 绘制水印
        g2d.drawString(watermarkText, x, y);

        // 释放资源
        g2d.dispose();

        // 创建保存目录 - 现在统一使用原目录名_watermark作为子目录
        String parentDirPath = imageFile.getParent();
        // 如果是根目录，使用当前工作目录
        if (parentDirPath == null) {
            parentDirPath = System.getProperty("user.dir");
        }
        
        // 获取原始目录的名称用于创建水印目录
        String originalDirName = new File(parentDirPath).getName();
        File outputDir = new File(parentDirPath, originalDirName + "_watermark");
        if (!outputDir.exists()) {
            outputDir.mkdir();
        }

        // 保存图片
        String outputFileName = "watermarked_" + imageFile.getName();
        File outputFile = new File(outputDir, outputFileName);
        String formatName = getImageFormat(imageFile.getName());
        ImageIO.write(image, formatName, outputFile);

        System.out.println("图片已保存至: " + outputFile.getAbsolutePath());
    }

    // 从图片文件中读取EXIF信息获取拍摄时间
    private static String getExifDateTime(File imageFile) {
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(imageFile);
            Directory directory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
            if (directory != null) {
                String dateTime = directory.getString(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
                if (dateTime != null) {
                    // EXIF时间格式通常为: yyyy:MM:dd HH:mm:ss
                    // 提取年月日部分
                    SimpleDateFormat exifFormat = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.ENGLISH);
                    Date date = exifFormat.parse(dateTime);
                    SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd");
                    return outputFormat.format(date);
                }
            }
        } catch (Exception e) {
            System.out.println("读取EXIF信息时出错: " + e.getMessage());
        }
        return null;
    }

    // 获取图片格式
    private static String getImageFormat(String fileName) {
        int dotIndex = fileName.lastIndexOf(".");
        if (dotIndex > 0) {
            return fileName.substring(dotIndex + 1).toUpperCase();
        }
        return "JPG";
    }
}