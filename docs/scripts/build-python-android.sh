#!/usr/bin/env bash
#
# build-python-android.sh — Build the Python runtime distribution for Android ARM64.
#
# Downloads the official CPython Android embeddable package, cross-compiles a
# minimal launcher binary using the Android NDK, cleans up unnecessary files,
# and packages everything as a .tar.gz archive ready for on-device extraction.
#
# Requirements:
#   - Android NDK r28+ installed (ANDROID_NDK_HOME or auto-detected)
#   - Internet access (to download CPython source package)
#   - tar, xz, shasum
#
# Usage:
#   ./build-python-android.sh [--python-version 3.14.3] [--output-dir ./out]
#
set -euo pipefail

# ---------------------------------------------------------------------------
# Configuration
# ---------------------------------------------------------------------------

PYTHON_VERSION="${PYTHON_VERSION:-3.14.3}"
PYTHON_MAJOR_MINOR="${PYTHON_VERSION%.*}"  # e.g., 3.14
API_LEVEL=24
ARCH="aarch64"
TARGET="${ARCH}-linux-android"
OUTPUT_DIR="${OUTPUT_DIR:-$(pwd)/out}"
WORK_DIR="$(mktemp -d)"

# Detect NDK
if [ -z "${ANDROID_NDK_HOME:-}" ]; then
    # Try common locations
    for candidate in \
        "$HOME/Library/Android/sdk/ndk/"* \
        "$HOME/Android/Sdk/ndk/"* \
        "/opt/android-ndk-"*; do
        if [ -d "$candidate" ]; then
            ANDROID_NDK_HOME="$candidate"
            break
        fi
    done
fi

if [ -z "${ANDROID_NDK_HOME:-}" ] || [ ! -d "$ANDROID_NDK_HOME" ]; then
    echo "ERROR: Android NDK not found. Set ANDROID_NDK_HOME." >&2
    exit 1
fi

echo "=== Python Android ARM64 Build Script ==="
echo "Python version:  $PYTHON_VERSION"
echo "NDK:             $ANDROID_NDK_HOME"
echo "API level:       $API_LEVEL"
echo "Work dir:        $WORK_DIR"
echo "Output dir:      $OUTPUT_DIR"
echo ""

# ---------------------------------------------------------------------------
# Step 1: Download the official CPython Android embeddable package
# ---------------------------------------------------------------------------

PACKAGE_NAME="cpython-${PYTHON_VERSION}+20250614-${ARCH}-linux-android-install_only_stripped.tar.gz"
DOWNLOAD_URL="https://github.com/indygreg/python-build-standalone/releases/download/20250614/${PACKAGE_NAME}"

# First try the official python.org Android package
OFFICIAL_PACKAGE="Python-${PYTHON_VERSION}-${ARCH}-linux-android${API_LEVEL}-debug.tar.gz"
OFFICIAL_URL="https://www.python.org/ftp/python/${PYTHON_VERSION}/${OFFICIAL_PACKAGE}"

echo ">>> Step 1: Downloading CPython ${PYTHON_VERSION} Android package..."

cd "$WORK_DIR"

# Try official python.org first (available since Python 3.13)
if curl -fsSL --head "$OFFICIAL_URL" >/dev/null 2>&1; then
    echo "    Using official python.org Android package"
    curl -fSL -o source-package.tar.gz "$OFFICIAL_URL"
elif curl -fsSL --head "$DOWNLOAD_URL" >/dev/null 2>&1; then
    echo "    Using python-build-standalone package"
    curl -fSL -o source-package.tar.gz "$DOWNLOAD_URL"
else
    echo "ERROR: Could not find a pre-built Python ${PYTHON_VERSION} for Android ARM64." >&2
    echo "Check: https://www.python.org/ftp/python/${PYTHON_VERSION}/" >&2
    exit 1
fi

echo "    Downloaded: $(du -h source-package.tar.gz | cut -f1)"

# ---------------------------------------------------------------------------
# Step 2: Extract source package
# ---------------------------------------------------------------------------

echo ">>> Step 2: Extracting source package..."

mkdir -p extracted
tar xzf source-package.tar.gz -C extracted

# Find the Python prefix directory (may be nested)
PYTHON_PREFIX=""
for candidate in \
    "extracted/python/install" \
    "extracted/python" \
    "extracted/Python-${PYTHON_VERSION}" \
    extracted/*/; do
    if [ -d "$candidate/lib" ]; then
        PYTHON_PREFIX="$candidate"
        break
    fi
done

if [ -z "$PYTHON_PREFIX" ]; then
    echo "ERROR: Could not find Python prefix in extracted package." >&2
    echo "Contents:" >&2
    ls -la extracted/ >&2
    exit 1
fi

echo "    Python prefix: $PYTHON_PREFIX"

# ---------------------------------------------------------------------------
# Step 3: Cross-compile the launcher binary
# ---------------------------------------------------------------------------

echo ">>> Step 3: Cross-compiling Python launcher..."

# Find the NDK toolchain
TOOLCHAIN="$ANDROID_NDK_HOME/toolchains/llvm/prebuilt"
if [ "$(uname)" = "Darwin" ]; then
    TOOLCHAIN="$TOOLCHAIN/darwin-x86_64"
elif [ "$(uname)" = "Linux" ]; then
    TOOLCHAIN="$TOOLCHAIN/linux-x86_64"
fi

CC="${TOOLCHAIN}/bin/${TARGET}${API_LEVEL}-clang"

if [ ! -x "$CC" ]; then
    echo "ERROR: Compiler not found at $CC" >&2
    exit 1
fi

# Write the minimal launcher
cat > launcher.c << 'LAUNCHER_EOF'
/* Minimal Python launcher for Android.
 * Links against libpython and calls Py_BytesMain.
 */
#include "Python.h"

int main(int argc, char *argv[]) {
    return Py_BytesMain(argc, argv);
}
LAUNCHER_EOF

# Find include and lib directories
INCLUDE_DIR=""
for candidate in \
    "$PYTHON_PREFIX/include/python${PYTHON_MAJOR_MINOR}" \
    "$PYTHON_PREFIX/include/python${PYTHON_VERSION}"; do
    if [ -d "$candidate" ]; then
        INCLUDE_DIR="$candidate"
        break
    fi
done

LIB_DIR="$PYTHON_PREFIX/lib"

if [ -z "$INCLUDE_DIR" ]; then
    echo "ERROR: Python include directory not found." >&2
    exit 1
fi

echo "    Include dir: $INCLUDE_DIR"
echo "    Lib dir:     $LIB_DIR"

# Compile and link
"$CC" \
    -O2 \
    -I"$INCLUDE_DIR" \
    -L"$LIB_DIR" \
    -Wl,-rpath,'$ORIGIN/../lib' \
    -o python3 \
    launcher.c \
    -lpython${PYTHON_MAJOR_MINOR}

# Strip the binary
"${TOOLCHAIN}/bin/llvm-strip" python3

echo "    Launcher compiled: $(du -h python3 | cut -f1)"

# ---------------------------------------------------------------------------
# Step 4: Assemble the distribution
# ---------------------------------------------------------------------------

echo ">>> Step 4: Assembling distribution..."

DIST_NAME="python-${PYTHON_VERSION}"
DIST_DIR="$WORK_DIR/$DIST_NAME"
mkdir -p "$DIST_DIR/bin"

# Copy the launcher binary
cp python3 "$DIST_DIR/bin/python3"
chmod 755 "$DIST_DIR/bin/python3"

# Create symlinks
(cd "$DIST_DIR/bin" && ln -sf python3 python)
(cd "$DIST_DIR/bin" && ln -sf python3 "python${PYTHON_MAJOR_MINOR}")

# Copy lib directory (shared libs + stdlib)
cp -a "$PYTHON_PREFIX/lib" "$DIST_DIR/lib"

# Create pip3 bootstrap script
cat > "$DIST_DIR/bin/pip3" << 'PIP_EOF'
#!/bin/sh
# Bootstrap pip3 — installs pip on first run if not present.
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PYTHON="$SCRIPT_DIR/python3"
PYTHON_HOME="$(dirname "$SCRIPT_DIR")"

export PYTHONHOME="$PYTHON_HOME"

# Check if pip is already installed
if "$PYTHON" -m pip --version >/dev/null 2>&1; then
    exec "$PYTHON" -m pip "$@"
fi

echo "pip not found. Installing via ensurepip..." >&2
"$PYTHON" -m ensurepip --default-pip 2>/dev/null || {
    echo "ensurepip failed. Downloading get-pip.py..." >&2
    if command -v curl >/dev/null 2>&1; then
        curl -fsSL https://bootstrap.pypa.io/get-pip.py | "$PYTHON" -
    elif command -v wget >/dev/null 2>&1; then
        wget -qO- https://bootstrap.pypa.io/get-pip.py | "$PYTHON" -
    else
        echo "ERROR: Neither curl nor wget available to download get-pip.py" >&2
        exit 1
    fi
}
exec "$PYTHON" -m pip "$@"
PIP_EOF
chmod 755 "$DIST_DIR/bin/pip3"
(cd "$DIST_DIR/bin" && ln -sf pip3 pip)

echo "    Distribution assembled at $DIST_DIR"

# ---------------------------------------------------------------------------
# Step 5: Clean up unnecessary files
# ---------------------------------------------------------------------------

echo ">>> Step 5: Cleaning up distribution..."

SIZE_BEFORE=$(du -sm "$DIST_DIR" | cut -f1)

# Remove test suite (huge)
rm -rf "$DIST_DIR/lib/python${PYTHON_MAJOR_MINOR}/test"
rm -rf "$DIST_DIR/lib/python${PYTHON_MAJOR_MINOR}/tests"

# Remove header files
rm -rf "$DIST_DIR/include"
rm -rf "$DIST_DIR/lib/python${PYTHON_MAJOR_MINOR}/config-${PYTHON_MAJOR_MINOR}-${TARGET}"

# Remove __pycache__ directories
find "$DIST_DIR" -type d -name "__pycache__" -exec rm -rf {} + 2>/dev/null || true

# Remove .pyc files
find "$DIST_DIR" -name "*.pyc" -delete 2>/dev/null || true

# Remove unused tools (turtle demo, IDLE, etc.)
rm -rf "$DIST_DIR/lib/python${PYTHON_MAJOR_MINOR}/idlelib"
rm -rf "$DIST_DIR/lib/python${PYTHON_MAJOR_MINOR}/turtledemo"
rm -rf "$DIST_DIR/lib/python${PYTHON_MAJOR_MINOR}/tkinter"
rm -rf "$DIST_DIR/lib/python${PYTHON_MAJOR_MINOR}/ensurepip/_bundled"

# Remove static libraries
find "$DIST_DIR" -name "*.a" -delete 2>/dev/null || true

# Remove pkg-config
rm -rf "$DIST_DIR/lib/pkgconfig"

# Strip shared libraries
find "$DIST_DIR" -name "*.so" -o -name "*.so.*" | while read -r lib; do
    "${TOOLCHAIN}/bin/llvm-strip" --strip-unneeded "$lib" 2>/dev/null || true
done

SIZE_AFTER=$(du -sm "$DIST_DIR" | cut -f1)
echo "    Size: ${SIZE_BEFORE}MB → ${SIZE_AFTER}MB"

# ---------------------------------------------------------------------------
# Step 6: Write manifest.json
# ---------------------------------------------------------------------------

echo ">>> Step 6: Writing manifest..."

NDK_VERSION=$(basename "$ANDROID_NDK_HOME" | sed 's/[^0-9.]//g' || echo "unknown")

cat > "$DIST_DIR/manifest.json" << MANIFEST_EOF
{
  "runtime": "python",
  "version": "${PYTHON_VERSION}",
  "arch": "${ARCH}",
  "platform": "linux-android",
  "source": "python.org official embeddable + NDK-compiled launcher",
  "pythonVersion": "${PYTHON_VERSION}",
  "ndkVersion": "${NDK_VERSION}",
  "apiLevel": ${API_LEVEL},
  "includes": {
    "stdlib": true,
    "ssl": true,
    "sqlite3": true,
    "pip": false,
    "ensurepip": false
  },
  "notes": "Headers and test suite removed for size. pip can be installed via get-pip.py."
}
MANIFEST_EOF

# ---------------------------------------------------------------------------
# Step 7: Package as tar.gz
# ---------------------------------------------------------------------------

echo ">>> Step 7: Creating archive..."

mkdir -p "$OUTPUT_DIR"
ARCHIVE_NAME="python-${PYTHON_VERSION}-android-arm64.tar.gz"
ARCHIVE_PATH="$OUTPUT_DIR/$ARCHIVE_NAME"

(cd "$WORK_DIR" && tar czf "$ARCHIVE_PATH" "$DIST_NAME/")

ARCHIVE_SIZE=$(stat -f%z "$ARCHIVE_PATH" 2>/dev/null || stat -c%s "$ARCHIVE_PATH")
ARCHIVE_SHA256=$(shasum -a 256 "$ARCHIVE_PATH" 2>/dev/null | cut -d' ' -f1 || sha256sum "$ARCHIVE_PATH" | cut -d' ' -f1)
UNPACKED_SIZE=$(du -sb "$DIST_DIR" 2>/dev/null | cut -f1 || echo $((SIZE_AFTER * 1048576)))

echo ""
echo "=== Build Complete ==="
echo "Archive:       $ARCHIVE_PATH"
echo "Size:          $(du -h "$ARCHIVE_PATH" | cut -f1)"
echo "SHA-256:       $ARCHIVE_SHA256"
echo "Unpacked size: ${SIZE_AFTER}MB"
echo ""

# ---------------------------------------------------------------------------
# Step 8: Write index.json
# ---------------------------------------------------------------------------

echo ">>> Step 8: Writing index.json..."

INDEX_PATH="$OUTPUT_DIR/index.json"
cat > "$INDEX_PATH" << INDEX_EOF
{
  "latest": "${PYTHON_VERSION}",
  "versions": [
    {
      "version": "${PYTHON_VERSION}",
      "url": "https://aster.theappstack.in/runtimes/python/${ARCHIVE_NAME}",
      "sha256": "${ARCHIVE_SHA256}",
      "sizeBytes": ${ARCHIVE_SIZE},
      "unpackedSizeBytes": ${UNPACKED_SIZE:-$((SIZE_AFTER * 1048576))}
    }
  ]
}
INDEX_EOF

echo "    index.json written to $INDEX_PATH"

# ---------------------------------------------------------------------------
# Cleanup
# ---------------------------------------------------------------------------

echo ""
echo ">>> Cleaning up work directory..."
rm -rf "$WORK_DIR"

echo ""
echo "Done! Copy the following files to docs/public/runtimes/python/:"
echo "  $ARCHIVE_PATH"
echo "  $INDEX_PATH"
