#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RUST_DIR="$ROOT_DIR/rust-core"
ANDROID_MAIN="$ROOT_DIR/android/app/src/main"
JNI_LIBS="$ANDROID_MAIN/jniLibs"
KOTLIN_OUT="$ANDROID_MAIN/java"

echo "1. Building unstripped host library for UniFFI Kotlin binding generation..."
cd "$RUST_DIR"
cargo build --release

HOST_LIB="$RUST_DIR/target/release/libcinny_core.so"
if [[ "$(uname -s)" == "Darwin" ]]; then
  HOST_LIB="$RUST_DIR/target/release/libcinny_core.dylib"
fi

echo "2. Generating Kotlin UniFFI bindings into android/app/src/main/java..."
cargo run --bin uniffi-bindgen -- generate \
  --library "$HOST_LIB" \
  --language kotlin \
  --out-dir "$KOTLIN_OUT"

echo "3. Compiling Rust matrix-sdk core for Android ABIs via cargo-ndk..."
cargo ndk \
  -t arm64-v8a \
  -t armeabi-v7a \
  -t x86_64 \
  -o "$JNI_LIBS" \
  build --release

echo "4. Stripping DWARF debug info only (preserving .dynsym for JNA/UniFFI)..."
if [[ -n "${ANDROID_NDK_HOME:-}" ]]; then
  LLVM_STRIP="$(find "$ANDROID_NDK_HOME/toolchains/llvm/prebuilt" -name 'llvm-strip' | head -n 1 || true)"
  if [[ -n "$LLVM_STRIP" ]]; then
    find "$JNI_LIBS" -name 'libcinny_core.so' -exec "$LLVM_STRIP" --strip-debug {} +
  fi
fi

echo "Done! Run: cd $ROOT_DIR/android && ./gradlew assembleDebug"
