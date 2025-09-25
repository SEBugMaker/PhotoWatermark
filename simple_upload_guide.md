# GitHub Release 上传指南

## 方法1: 使用GitHub网页界面（最简单）

1. 访问你的GitHub仓库页面
2. 点击 "Releases" 标签
3. 点击 "Create a new release"
4. 输入标签名（如：v1.0.0）
5. 输入发布标题和描述
6. 拖放以下文件到上传区域：
   - `dist/macos/PhotoWatermark-1.0.0.dmg`
   - `dist/macos/PhotoWatermark.app`（需要先压缩成.zip）

## 方法2: 使用命令行（curl）

### 步骤1: 创建应用压缩包
```bash
cd /Users/rain/研究生资料/研一上/大语言模型辅助软件工程/hw/PhotoWatermark
cd dist/macos
zip -r PhotoWatermark-1.0.0.app.zip PhotoWatermark.app
```

### 步骤2: 获取GitHub Token
1. 访问 https://github.com/settings/tokens
2. 创建新的Personal Access Token
3. 勾选 `repo` 权限

### 步骤3: 创建Release并上传文件

```bash
# 设置变量
GITHUB_TOKEN="你的_token_here"
REPO_OWNER="你的GitHub用户名"
REPO_NAME="PhotoWatermark"
TAG_NAME="v1.0.0"

# 创建release
curl -X POST \
  -H "Authorization: token $GITHUB_TOKEN" \
  -H "Accept: application/vnd.github.v3+json" \
  -d '{
    "tag_name": "'$TAG_NAME'",
    "name": "PhotoWatermark v1.0.0",
    "body": "PhotoWatermark macOS application\n\n包含文件:\n- PhotoWatermark-1.0.0.dmg: 安装包\n- PhotoWatermark-1.0.0.app.zip: 应用压缩包",
    "draft": false,
    "prerelease": false
  }' \
  "https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/releases"

# 获取release ID（需要从响应中提取）
RELEASE_ID="从上面响应中获取的release ID"

# 上传DMG文件
curl -X POST \
  -H "Authorization: token $GITHUB_TOKEN" \
  -H "Accept: application/vnd.github.v3+json" \
  -H "Content-Type: application/x-apple-diskimage" \
  --data-binary "@PhotoWatermark-1.0.0.dmg" \
  "https://uploads.github.com/repos/$REPO_OWNER/$REPO_NAME/releases/$RELEASE_ID/assets?name=PhotoWatermark-1.0.0.dmg"

# 上传APP zip文件
curl -X POST \
  -H "Authorization: token $GITHUB_TOKEN" \
  -H "Accept: application/vnd.github.v3+json" \
  -H "Content-Type: application/zip" \
  --data-binary "@PhotoWatermark-1.0.0.app.zip" \
  "https://uploads.github.com/repos/$REPO_OWNER/$REPO_NAME/releases/$RELEASE_ID/assets?name=PhotoWatermark-1.0.0.app.zip"
```

## 方法3: 使用GitHub CLI（推荐安装）

### 安装GitHub CLI
```bash
brew install gh
```

### 使用GitHub CLI上传
```bash
# 登录（只需一次）
gh auth login

# 创建release并上传文件
gh release create v1.0.0 \
  dist/macos/PhotoWatermark-1.0.0.dmg \
  dist/macos/PhotoWatermark-1.0.0.app.zip \
  --title "PhotoWatermark v1.0.0" \
  --notes "PhotoWatermark macOS application"
```

## 文件准备检查清单

- [ ] `dist/macos/PhotoWatermark-1.0.0.dmg` - 安装包
- [ ] `dist/macos/PhotoWatermark.app` - 应用（需要压缩成.zip）
- [ ] GitHub Personal Access Token（如果使用API）
- [ ] GitHub仓库已创建并推送了代码

## 注意事项

1. 确保你的GitHub仓库是公开的，或者你有私有仓库的访问权限
2. 文件大小限制：GitHub release附件最大2GB
3. 建议使用语义化版本号（如v1.0.0）
4. 上传前确保文件没有损坏且可以正常运行