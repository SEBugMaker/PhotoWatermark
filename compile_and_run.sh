#!/bin/bash

# 设置变量
PROJECT_DIR="$(pwd)"
SRC_DIR="$PROJECT_DIR/src/main/java"
LIB_DIR="$PROJECT_DIR/lib"
OUT_DIR="$PROJECT_DIR/out"
MAIN_CLASS="PhotoWatermarkApp"
METADATA_EXTRACTOR_VERSION="2.18.0"
METADATA_EXTRACTOR_JAR="metadata-extractor-$METADATA_EXTRACTOR_VERSION.jar"
XMPCORE_VERSION="6.1.11"
XMPCORE_JAR="xmpcore-$XMPCORE_VERSION.jar"

# 创建目录
mkdir -p "$LIB_DIR" "$OUT_DIR"

# 下载metadata-extractor库
if [ ! -f "$LIB_DIR/$METADATA_EXTRACTOR_JAR" ]; then
    echo "正在下载metadata-extractor库..."
    curl -L -o "$LIB_DIR/$METADATA_EXTRACTOR_JAR" "https://repo1.maven.org/maven2/com/drewnoakes/metadata-extractor/$METADATA_EXTRACTOR_VERSION/$METADATA_EXTRACTOR_JAR"
    if [ $? -ne 0 ]; then
        echo "下载metadata-extractor库失败，请手动下载并放入$LIB_DIR目录"
        exit 1
    fi
fi

# 下载xmpcore库（metadata-extractor的依赖）
if [ ! -f "$LIB_DIR/$XMPCORE_JAR" ]; then
    echo "正在下载xmpcore库..."
    curl -L -o "$LIB_DIR/$XMPCORE_JAR" "https://repo1.maven.org/maven2/com/adobe/xmp/xmpcore/$XMPCORE_VERSION/$XMPCORE_JAR"
    if [ $? -ne 0 ]; then
        echo "下载xmpcore库失败，请手动下载并放入$LIB_DIR目录"
        exit 1
    fi
fi

# 编译Java代码
echo "正在编译Java代码..."
javac -d "$OUT_DIR" -cp "$LIB_DIR/$METADATA_EXTRACTOR_JAR:$LIB_DIR/$XMPCORE_JAR" "$SRC_DIR/$MAIN_CLASS.java"
if [ $? -ne 0 ]; then
    echo "编译失败"
    exit 1
fi

# 运行程序
read -p "编译成功，是否立即运行程序？(y/n): " run_choice
if [ "$run_choice" = "y" ] || [ "$run_choice" = "Y" ]; then
    echo "正在运行程序..."
    java -cp "$OUT_DIR:$LIB_DIR/$METADATA_EXTRACTOR_JAR:$LIB_DIR/$XMPCORE_JAR" $MAIN_CLASS
fi

# 创建可执行JAR文件（可选）
read -p "是否创建可执行JAR文件？(y/n): " jar_choice
if [ "$jar_choice" = "y" ] || [ "$jar_choice" = "Y" ]; then
    echo "正在创建可执行JAR文件..."
    # 先创建Manifest文件
    echo "Main-Class: $MAIN_CLASS" > "$PROJECT_DIR/Manifest.txt"
    # 然后创建JAR文件
    jar cmf "$PROJECT_DIR/Manifest.txt" "$PROJECT_DIR/PhotoWatermark.jar" -C "$OUT_DIR" .
    # 清理临时文件
    rm "$PROJECT_DIR/Manifest.txt"
    echo "可执行JAR文件已创建：$PROJECT_DIR/PhotoWatermark.jar"
    echo "使用命令运行：java -cp '$LIB_DIR/$METADATA_EXTRACTOR_JAR:$LIB_DIR/$XMPCORE_JAR:$PROJECT_DIR/PhotoWatermark.jar' $MAIN_CLASS"
fi

# 更新README文件，添加使用脚本的说明（仅当README不存在相关内容时）
if ! grep -q "使用编译脚本" "$PROJECT_DIR/README.md"; then
    sed -i '' '/## 编译和运行/a\
## 使用编译脚本\
\
如果您的系统上没有安装Maven，可以使用提供的编译脚本来构建和运行项目：\
\
1. 确保脚本有执行权限：\
   ```bash\
   chmod +x compile_and_run.sh\
   ```\
2. 运行脚本：\
   ```bash\
   ./compile_and_run.sh\
   ```\
3. 脚本将自动下载必要的依赖、编译代码并提供运行选项' "$PROJECT_DIR/README.md"
fi

# 提示用户
echo "项目设置完成！"
echo "您可以使用以下方法之一来运行程序："
echo "1. 使用编译脚本：./compile_and_run.sh"
echo "2. 如果您安装了Maven：mvn clean package && java -jar target/PhotoWatermark-1.0-SNAPSHOT-jar-with-dependencies.jar"