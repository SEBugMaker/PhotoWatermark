#!/bin/bash

# GitHub Release 一键上传脚本
# 使用方法: ./release_upload.sh

set -e

echo "🚀 开始创建GitHub Release..."

# 配置信息
REPO_OWNER="SEBugMaker"
REPO_NAME="PhotoWatermark"
TAG_NAME="release1.0"
RELEASE_NAME="PhotoWatermark v1.0.0"

# 文件路径
DMG_FILE="dist/macos/PhotoWatermark-1.0.0.dmg"
APP_ZIP_FILE="dist/macos/PhotoWatermark-1.0.0.app.zip"

# 检查文件是否存在
echo "📁 检查文件..."
if [ ! -f "$DMG_FILE" ]; then
    echo "❌ 错误: DMG文件不存在: $DMG_FILE"
    exit 1
fi

if [ ! -f "$APP_ZIP_FILE" ]; then
    echo "❌ 错误: APP压缩文件不存在: $APP_ZIP_FILE"
    echo "请先运行: cd dist/macos && zip -r PhotoWatermark-1.0.0.app.zip PhotoWatermark.app"
    exit 1
fi

echo "✅ 文件检查完成"

# 检查是否已经存在该tag
echo "🔍 检查tag是否存在..."
if git rev-parse "$TAG_NAME" >/dev/null 2>&1; then
    echo "⚠️  Tag $TAG_NAME 已存在，是否删除并重新创建？(y/n)"
    read -r response
    if [[ "$response" =~ ^[Yy]$ ]]; then
        git tag -d "$TAG_NAME"
        git push origin ":refs/tags/$TAG_NAME"
        echo "✅ 已删除旧tag"
    else
        echo "❌ 取消操作"
        exit 1
    fi
fi

# 创建并推送tag
echo "🏷️  创建tag: $TAG_NAME"
git tag -a "$TAG_NAME" -m "Release $RELEASE_NAME"
git push origin "$TAG_NAME"

# 创建release（使用GitHub API）
echo "📦 创建GitHub Release..."

# 由于我们没有安装gh CLI，使用curl通过GitHub API创建release
# 注意：这需要GITHUB_TOKEN环境变量

if [ -z "$GITHUB_TOKEN" ]; then
    echo "⚠️  未设置GITHUB_TOKEN环境变量"
    echo "请设置你的GitHub Personal Access Token:"
    echo "export GITHUB_TOKEN=你的_token_here"
    echo ""
    echo "或者使用GitHub网页界面手动创建release:"
    echo "1. 访问: https://github.com/$REPO_OWNER/$REPO_NAME/releases"
    echo "2. 点击 'Create a new release'"
    echo "3. 选择 tag: $TAG_NAME"
    echo "4. 上传文件:"
    echo "   - $DMG_FILE"
    echo "   - $APP_ZIP_FILE"
    exit 1
fi

# 创建release
curl -X POST \
  -H "Authorization: token $GITHUB_TOKEN" \
  -H "Accept: application/vnd.github.v3+json" \
  -d '{
    "tag_name": "'$TAG_NAME'",
    "name": "'$RELEASE_NAME'",
    "body": "PhotoWatermark macOS 应用程序\n\n## 包含文件\n- 📦 PhotoWatermark-1.0.0.dmg: macOS安装包\n- 📱 PhotoWatermark-1.0.0.app.zip: 应用程序压缩包\n\n## 使用说明\n1. 下载 .dmg 文件进行标准安装\n2. 或下载 .app.zip 解压后直接运行\n\n## 系统要求\n- macOS 10.10 或更高版本\n- Java 11 或更高版本（已包含在应用中）",
    "draft": false,
    "prerelease": false
  }' \
  "https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/releases"

echo ""
echo "✅ Release创建完成！"
echo "📍 访问: https://github.com/$REPO_OWNER/$REPO_NAME/releases/tag/$TAG_NAME"
echo ""
echo "注意：由于API限制，文件上传可能需要手动在GitHub网页界面完成。"
echo "请访问上面的链接，点击 'Edit release'，然后拖放文件到上传区域。"