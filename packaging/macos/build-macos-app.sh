#!/usr/bin/env bash
set -euo pipefail

# =============================================
# PhotoWatermark macOS Packager (Enhanced)
# Steps:
#  1. Verify tools
#  2. Clean & build with assembly
#  3. Locate or create fat jar
#  4. Run jpackage -> .app
#  5. Build DMG
#  6. Summary
# =============================================

APP_NAME="PhotoWatermark"
APP_VERSION="1.0.0"
MAIN_CLASS="PhotoWatermarkGUI"   # GUI entry (manifest mainClass overridden by jpackage)
PROJECT_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
TARGET_DIR="$PROJECT_ROOT/target"
OUTPUT_DIR="$PROJECT_ROOT/dist/macos"
ICON_PATH="$PROJECT_ROOT/packaging/macos/PhotoWatermark.icns"
FAT_JAR_PATTERN='*-jar-with-dependencies.jar'
LOG_PREFIX="[PMW]"

mkdir -p "$OUTPUT_DIR"

color() { local c="$1"; shift || true; printf "\033[%sm%s\033[0m" "$c" "$*"; }
log() { echo -e "${LOG_PREFIX} $*"; }
step() { color 36 "\n==> $*"; echo; }
ok() { color 32 "✔ $*"; echo; }
warn() { color 33 "⚠ $*"; echo; }
err() { color 31 "✘ $*" >&2; }

step "1/6 Checking tools & Java version"
if ! command -v java >/dev/null 2>&1; then err "Java not found"; exit 1; fi
JAVA_VER=$(java -version 2>&1 | head -n1)
log "Java: $JAVA_VER"
if ! command -v mvn >/dev/null 2>&1; then err "Maven not found"; exit 1; fi
if ! command -v jpackage >/dev/null 2>&1; then err "jpackage not found (need JDK 17+)."; exit 1; fi
ok "Required tools present"

step "2/6 Building project (clean + assembly)"
( cd "$PROJECT_ROOT" && mvn -q -DskipTests clean package assembly:single || { err "Maven build failed"; exit 1; } )
ok "Maven build finished"

step "3/6 Locating fat jar"
FAT_JAR=""
# Preferred: target/<artifact>-<version>-jar-with-dependencies.jar
if ls "$TARGET_DIR"/$FAT_JAR_PATTERN >/dev/null 2>&1; then
  FAT_JAR=$(ls -1t "$TARGET_DIR"/$FAT_JAR_PATTERN | head -n1)
  ok "Found fat jar: $(basename "$FAT_JAR")"
else
  warn "Fat jar not found. Attempting fallback packaging (plain classes + dependency jars)."
fi

# Fallback input preparation if no fat jar
tmp_input="$OUTPUT_DIR/_input"
rm -rf "$tmp_input"; mkdir -p "$tmp_input"
if [ -n "$FAT_JAR" ]; then
  cp "$FAT_JAR" "$tmp_input/"
  MAIN_JAR_NAME="$(basename "$FAT_JAR")"
else
  # Copy compiled classes into a temp jar
  if [ ! -d "$TARGET_DIR/classes" ]; then err "classes directory missing"; exit 1; fi
  (cd "$TARGET_DIR/classes" && jar cfe "$tmp_input/app-temp.jar" "$MAIN_CLASS" . )
  MAIN_JAR_NAME="app-temp.jar"
  # Copy dependency jars from local repo (simple heuristic): use metadata-extractor and gson already present in local m2 or lib dir
  # Prefer local target/dependency if any; fallback to lib
  # We bundle minimal libs that are already in project lib
  cp "$PROJECT_ROOT"/lib/*.jar "$tmp_input/" 2>/dev/null || true
  # Try to copy gson from local m2 repository
  GSON_JAR=$(find "$HOME/.m2/repository" -type f -name 'gson-*.jar' 2>/dev/null | sort | tail -n1 || true)
  if [ -n "$GSON_JAR" ]; then cp "$GSON_JAR" "$tmp_input/"; else warn "Could not find gson jar in local m2; ensure runtime availability"; fi
  ok "Created fallback temp jar + copied dependencies"
fi

INPUT_DIR="$tmp_input"

# Icon args (use string form to avoid unbound issues under set -u)
ICON_FLAGS=""
if [ -f "$ICON_PATH" ]; then
  ICON_FLAGS="--icon \"$ICON_PATH\""
else
  warn "Icon not found, using generic."
fi

APP_IMAGE_DIR="$OUTPUT_DIR/$APP_NAME.app"
DMG_PATH="$OUTPUT_DIR/$APP_NAME-$APP_VERSION.dmg"

step "4/6 Creating .app (jpackage app-image)"
rm -rf "$APP_IMAGE_DIR"
set +e
# shellcheck disable=SC2086
jpackage \
  --type app-image \
  --name "$APP_NAME" \
  --app-version "$APP_VERSION" \
  --input "$INPUT_DIR" \
  --main-jar "$MAIN_JAR_NAME" \
  --main-class "$MAIN_CLASS" \
  $ICON_FLAGS \
  --dest "$OUTPUT_DIR" \
  --java-options '-Xms256m' \
  --java-options '-Xmx1024m'
JPK_EXIT=$?
set -e
if [ $JPK_EXIT -ne 0 ]; then err "jpackage app-image failed (exit $JPK_EXIT)"; exit 1; fi
[ -d "$APP_IMAGE_DIR" ] || { err ".app not created"; exit 1; }
ok ".app created: $APP_IMAGE_DIR"

step "5/6 Creating DMG image"
rm -f "$DMG_PATH"
set +e
# shellcheck disable=SC2086
jpackage \
  --type dmg \
  --name "$APP_NAME" \
  --app-version "$APP_VERSION" \
  --input "$INPUT_DIR" \
  --main-jar "$MAIN_JAR_NAME" \
  --main-class "$MAIN_CLASS" \
  $ICON_FLAGS \
  --dest "$OUTPUT_DIR" \
  --java-options '-Xms256m' \
  --java-options '-Xmx1024m'
JPK_DMG_EXIT=$?
set -e
if [ $JPK_DMG_EXIT -ne 0 ]; then warn "DMG creation failed (exit $JPK_DMG_EXIT). You can still use the .app."; else ok "DMG created: $DMG_PATH"; fi

step "6/6 Build summary"
ls -1 "$OUTPUT_DIR" | sed 's/^/  - /'

echo
ok "Done. Launch with: open '$APP_IMAGE_DIR'"
if [ -f "$DMG_PATH" ]; then echo "Mount DMG: open '$DMG_PATH'"; fi

# Cleanup temporary input if using fat jar (to keep final artifacts clean)
if [ -n "$FAT_JAR" ]; then rm -rf "$tmp_input"; fi
