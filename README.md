# PhotoWatermark

这是一个Java命令行程序，用于给图片添加基于EXIF拍摄时间的水印。

## 功能特点
- 读取图片的EXIF信息，提取拍摄时间作为水印文本
- 允许用户自定义字体大小、颜色和水印位置
- 将处理后的图片保存在原目录下的专用子目录中

## 环境要求
- JDK 17或更高版本
- Maven 3.6或更高版本

## 依赖项
- [metadata-extractor](https://github.com/drewnoakes/metadata-extractor) - 用于读取图片的EXIF信息

## 编译和运行

您可以通过以下两种方式之一来编译和运行程序：

### 方法一：使用Maven编译（推荐）

1. 确保已安装Maven 3.6或更高版本
2. 在项目根目录下执行以下命令：
   ```bash
   mvn clean package
   ```
3. 编译成功后，可执行的JAR文件将位于`target`目录下
4. 执行以下命令运行程序：
   ```bash
   java -jar target/PhotoWatermark-1.0-SNAPSHOT-jar-with-dependencies.jar
   ```

### 方法二：使用编译脚本

如果您的系统上没有安装Maven，可以使用提供的编译脚本来构建和运行项目：

1. 确保脚本有执行权限：
   ```bash
   chmod +x compile_and_run.sh
   ```
2. 运行脚本：
   ```bash
   ./compile_and_run.sh
   ```
3. 脚本将自动下载必要的依赖、编译代码并提供运行选项

## 使用说明
1. 运行程序后，输入图片文件的完整路径
2. 设置水印字体大小（默认36）
3. 设置水印字体颜色（默认WHITE，可选：BLACK, RED, GREEN, BLUE, YELLOW）
4. 设置水印位置（默认BOTTOM_RIGHT，可选：TOP_LEFT, TOP_CENTER, TOP_RIGHT, MIDDLE_LEFT, CENTER, MIDDLE_RIGHT, BOTTOM_LEFT, BOTTOM_CENTER）
5. 程序将处理图片并将带水印的图片保存在原目录下的`[原文件名]_watermark`子目录中

## 示例

假设图片路径为`/path/to/photo.jpg`，程序将：
1. 读取该图片的EXIF信息，提取拍摄日期
2. 按照用户设置的参数添加水印
3. 将处理后的图片保存为`/path/to/photo.jpg_watermark/watermarked_photo.jpg`

## 注意事项
- 如果图片没有EXIF拍摄时间信息，程序将使用当前日期作为水印
- 程序支持常见的图片格式，如JPG、PNG等
- 确保程序有足够的权限读取源文件和写入目标目录

## License
MIT License