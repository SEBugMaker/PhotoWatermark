#!/bin/bash

# GitHub Release Upload Script
# 使用方法: ./upload_to_github_release.sh <GITHUB_TOKEN> <TAG_NAME> <RELEASE_NAME>

set -e

# 检查参数
if [ $# -ne 3 ]; then
    echo "Usage: $0 <GITHUB_TOKEN> <TAG_NAME> <RELEASE_NAME>"
    echo "Example: $0 your_token v1.0.0 'PhotoWatermark v1.0.0'"
    exit 1
fi

GITHUB_TOKEN=$1
TAG_NAME=$2
RELEASE_NAME=$3
REPO_OWNER="your_username"  # 需要修改为实际的GitHub用户名
REPO_NAME="PhotoWatermark"

# 文件路径
DMG_FILE="dist/macos/PhotoWatermark-1.0.0.dmg"
APP_FILE="dist/macos/PhotoWatermark.app"

# 检查文件是否存在
if [ ! -f "$DMG_FILE" ]; then
    echo "Error: DMG file not found: $DMG_FILE"
    exit 1
fi

if [ ! -d "$APP_FILE" ]; then
    echo "Error: App bundle not found: $APP_FILE"
    exit 1
fi

# 创建应用的zip文件（因为.app是目录，需要压缩）
echo "Creating zip archive for PhotoWatermark.app..."
zip -r "dist/macos/PhotoWatermark-1.0.0.app.zip" "$APP_FILE"

APP_ZIP_FILE="dist/macos/PhotoWatermark-1.0.0.app.zip"

# 创建release的API调用
echo "Creating release $TAG_NAME..."
RELEASE_RESPONSE=$(curl -s -X POST \
    -H "Authorization: token $GITHUB_TOKEN" \
    -H "Accept: application/vnd.github.v3+json" \
    -d "{
        \"tag_name\": \"$TAG_NAME\",
        \"name\": \"$RELEASE_NAME\",
        \"body\": \"PhotoWatermark macOS application\\n\\n包含文件:\\n- PhotoWatermark-1.0.0.dmg: 安装包\\n- PhotoWatermark-1.0.0.app.zip: 应用压缩包\",
        \"draft\": false,
        \"prerelease\": false
    }" \
    "https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/releases")

# 获取release的upload_url
UPLOAD_URL=$(echo "$RELEASE_RESPONSE" | grep -o '"upload_url": "[^"]*' | cut -d'"' -f4 | sed 's/{.*}//')
RELEASE_ID=$(echo "$RELEASE_RESPONSE" | grep -o '"id": [0-9]*' | head -1 | cut -d' ' -f2)

if [ -z "$UPLOAD_URL" ]; then
    echo "Error creating release. Response:"
    echo "$RELEASE_RESPONSE"
    exit 1
fi

echo "Release created successfully. ID: $RELEASE_ID"

# 上传DMG文件
echo "Uploading DMG file..."
DMG_UPLOAD_RESPONSE=$(curl -s -X POST \
    -H "Authorization: token $GITHUB_TOKEN" \
    -H "Accept: application/vnd.github.v3+json" \
    -H "Content-Type: application/x-apple-diskimage" \
    --data-binary "@$DMG_FILE" \
    "${UPLOAD_URL}?name=PhotoWatermark-1.0.0.dmg&label=PhotoWatermark%20Installer")

# 上传APP zip文件
echo "Uploading APP zip file..."
APP_UPLOAD_RESPONSE=$(curl -s -X POST \
    -H "Authorization: token $GITHUB_TOKEN" \
    -H "Accept: application/vnd.github.v3+json" \
    -H "Content-Type: application/zip" \
    --data-binary "@$APP_ZIP_FILE" \
    "${UPLOAD_URL}?name=PhotoWatermark-1.0.0.app.zip&label=PhotoWatermark%20App")

echo "Upload completed successfully!"
echo "Release URL: https://github.com/$REPO_OWNER/$REPO_NAME/releases/tag/$TAG_NAME"