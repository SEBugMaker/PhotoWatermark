#!/bin/bash

# 测试脚本：验证PhotoWatermarkApp的功能

# 设置变量
PROJECT_DIR="$(pwd)"
LIB_DIR="$PROJECT_DIR/lib"
OUT_DIR="$PROJECT_DIR/out"
MAIN_CLASS="PhotoWatermarkApp"

# 检查编译是否成功
if [ ! -d "$OUT_DIR" ] || [ ! -f "$OUT_DIR/$MAIN_CLASS.class" ]; then
    echo "错误：程序尚未编译成功。请先运行 ./compile_and_run.sh 进行编译。"
    exit 1
fi

# 检查是否存在测试图片目录
TEST_IMAGE_DIR="$PROJECT_DIR/picture"
if [ ! -d "$TEST_IMAGE_DIR" ]; then
    echo "警告：测试图片目录 $TEST_IMAGE_DIR 不存在。"
    echo "请在该目录下放置一些测试图片，或者手动运行程序测试。"
    echo "运行程序的命令：java -cp '$OUT_DIR:$LIB_DIR/metadata-extractor-2.18.0.jar' $MAIN_CLASS"
    exit 1
fi

# 检查测试目录中是否有图片文件
IMAGE_COUNT=$(ls -l "$TEST_IMAGE_DIR"/*.{jpg,jpeg,png,gif,bmp} 2>/dev/null | wc -l)
if [ $IMAGE_COUNT -eq 0 ]; then
    echo "警告：测试图片目录 $TEST_IMAGE_DIR 中没有找到支持的图片文件。"
    echo "请在该目录下放置一些测试图片，或者手动运行程序测试。"
    echo "运行程序的命令：java -cp '$OUT_DIR:$LIB_DIR/metadata-extractor-2.18.0.jar' $MAIN_CLASS"
    exit 1
fi

# 提供测试选项
cat << EOF
==================================
PhotoWatermarkApp 测试脚本
==================================

测试图片目录: $TEST_IMAGE_DIR
找到的图片数量: $IMAGE_COUNT

请选择测试类型:
1. 测试单个图片文件
2. 测试整个目录的图片
3. 显示运行程序的命令
4. 退出
EOF

read -p "请输入选择 (1-4): " choice

case $choice in
    1)
        # 选择一个测试图片文件
        TEST_IMAGE=$(ls -1 "$TEST_IMAGE_DIR"/*.{jpg,jpeg,png,gif,bmp} 2>/dev/null | head -n 1)
        echo "\n选择测试文件: $TEST_IMAGE"
        echo "运行程序并处理该文件...\n"
        # 创建一个期望的输入序列
        cat > input.txt << EOF
$TEST_IMAGE
36
WHITE
BOTTOM_RIGHT
EOF
        # 运行程序并提供输入
        java -cp "$OUT_DIR:$LIB_DIR/metadata-extractor-2.18.0.jar:$LIB_DIR/xmpcore-6.1.11.jar" $MAIN_CLASS < input.txt
        # 清理临时文件
        rm input.txt
        ;;
    2)
        echo "\n运行程序并处理整个目录的图片...\n"
        # 创建一个期望的输入序列
        cat > input.txt << EOF
$TEST_IMAGE_DIR
36
WHITE
BOTTOM_RIGHT
EOF
        # 运行程序并提供输入
        java -cp "$OUT_DIR:$LIB_DIR/metadata-extractor-2.18.0.jar:$LIB_DIR/xmpcore-6.1.11.jar" $MAIN_CLASS < input.txt
        # 清理临时文件
        rm input.txt
        ;;
    3)
        echo "运行程序的命令："
        echo "java -cp '$OUT_DIR:$LIB_DIR/metadata-extractor-2.18.0.jar:$LIB_DIR/xmpcore-6.1.11.jar' $MAIN_CLASS"
        echo "\n或者使用编译脚本："
        echo "chmod +x compile_and_run.sh"
        echo "./compile_and_run.sh"
        ;;
    4)
        echo "\n退出测试脚本。"
        exit 0
        ;;
    *)
        echo "\n无效的选择，请重新运行脚本并选择 1-4。"
        exit 1
        ;;
esac

# 显示结果提示
echo "\n=================================="
if [ "$choice" = "1" ] || [ "$choice" = "2" ]; then
    OUTPUT_DIR="$(dirname "$TEST_IMAGE_DIR")/$(basename "$TEST_IMAGE_DIR")_watermark"
    if [ -d "$OUTPUT_DIR" ]; then
        echo "测试完成！已处理的图片保存在: $OUTPUT_DIR"
        echo "可以使用 ls 命令查看结果：ls -l $OUTPUT_DIR"
    else
        echo "测试可能失败，请检查程序输出的错误信息。"
    fi
fi

# 提供进一步的测试建议
echo "\n进一步测试建议："
echo "1. 尝试不同的字体大小（如 24, 48）"
echo "2. 尝试不同的字体颜色（如 BLACK, RED, GREEN）"
echo "3. 尝试不同的水印位置（如 TOP_LEFT, CENTER）"
echo "4. 使用不同类型的图片文件进行测试"
echo "=================================="