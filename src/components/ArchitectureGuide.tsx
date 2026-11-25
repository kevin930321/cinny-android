import { useState } from 'react';
import { Check, Copy } from 'lucide-react';

interface MappingRow {
  layer: string;
  cinnyWeb: string;
  rustAndroid: string;
  fileRef: string;
}

const ARCHITECTURE_MAPPING: MappingRow[] = [
  {
    layer: 'Matrix 協定、SSO 與同步引擎',
    cinnyWeb: 'matrix-js-sdk (TypeScript REST + /sync + SSO)',
    rustAndroid: 'matrix-sdk 0.16 + SyncService + Custom Tabs SSO (cinny://sso)',
    fileRef: 'rust-core/src/client.rs',
  },
  {
    layer: 'E2EE、SAS 與 4S 金鑰備份',
    cinnyWeb: 'matrix-sdk-crypto-wasm (IndexedDB + 4S)',
    rustAndroid: 'Olm/Megolm + 4S Recovery Key + Keystore 加密 SQLite',
    fileRef: 'rust-core/src/crypto.rs',
  },
  {
    layer: '加密圖片與檔案 mxc:// 快取',
    cinnyWeb: 'Browser Blob Decrypt + ObjectURL',
    rustAndroid: 'AES-CTR 附件加密上傳 + media_cache/ FileProvider',
    fileRef: 'rust-core/src/media.rs',
  },
  {
    layer: 'UnifiedPush 背景推播解密',
    cinnyWeb: 'Service Worker WebPush',
    rustAndroid: 'UnifiedPush Receiver + WorkManager + process_push_event',
    fileRef: 'rust-core/src/push.rs',
  },
  {
    layer: 'Spaces、公開目錄 Explore 與房間設定',
    cinnyWeb: 'Space Sidebar + Explore + RoomSettings + Inbox',
    rustAndroid: 'create_space / search_public_rooms / update_room_* / notification_settings',
    fileRef: 'rust-core/src/spaces.rs',
  },
  {
    layer: '時間軸、討論串、輸入提示與事件原始碼',
    cinnyWeb: 'Modern/Compact/Bubble + Typing + View Source',
    rustAndroid: 'TimelineFocus::Thread + subscribe_room_typing + original_json',
    fileRef: 'rust-core/src/timeline.rs',
  },
  {
    layer: 'Cinny 主題系統與原生設定中心',
    cinnyWeb: 'Light/Silver/Dark/Butter + colorMXID + Settings',
    rustAndroid: 'CinnyTheme + CinnyPreferences + SettingsScreen + RoomSettingsScreen',
    fileRef: 'android/.../ui/screens/SettingsScreen.kt',
  },
];

const BUILD_COMMANDS = [
  {
    step: '01. 安裝 Rust Android 交叉編譯目標與 cargo-ndk（並執行 Rust 單元測試）',
    cmd: 'rustup target add aarch64-linux-android armv7-linux-androideabi x86_64-linux-android\ncargo install cargo-ndk\ncd rust-core && cargo test',
  },
  {
    step: '02. 產出 UniFFI Kotlin 綁定並編譯 libcinny_core.so（僅剝除除錯符號）',
    cmd: 'export ANDROID_NDK_HOME=$HOME/Android/Sdk/ndk/26.3.11579264\n./scripts/build-android-ndk.sh',
  },
  {
    step: '03. 執行 Kotlin 單元測試並使用 Gradle 建置原生 Android APK',
    cmd: 'cd android\n./gradlew testDebugUnitTest assembleDebug',
  },
];

export function ArchitectureView() {
  return (
    <div className="space-y-6">
      <section className="rounded-2xl border border-slate-200 bg-white p-6">
        <h2 className="text-lg font-semibold text-slate-900">
          Cinny Web 至 Rust 原生安卓架構對照 (v0.4.0 Cinny UI 深度對齊版)
        </h2>
        <p className="mt-1 text-sm text-slate-600">
          全面參考 cinnyapp/cinny WebUI 設計系統與功能結構：支援 Light / Silver / Dark / Butter 四款主題、Modern / Compact / Bubble 排版、完整設定頁面、房間設定、Explore 探索、Inbox 收件匣與即時 Typing 狀態。
        </p>

        <div className="mt-5 overflow-x-auto">
          <table className="w-full border-collapse text-left text-sm">
            <thead>
              <tr className="border-b border-slate-200 text-xs text-slate-500">
                <th className="py-3 pr-4 font-semibold">架構層級</th>
                <th className="py-3 pr-4 font-semibold">Cinny Web (cinnyapp/cinny)</th>
                <th className="py-3 pr-4 font-semibold">Cinny Native Android (Rust)</th>
                <th className="py-3 font-semibold">專案模組路徑</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {ARCHITECTURE_MAPPING.map((row) => (
                <tr key={row.layer} className="hover:bg-slate-50/70">
                  <td className="py-3.5 pr-4 font-medium text-slate-900">
                    {row.layer}
                  </td>
                  <td className="py-3.5 pr-4 text-slate-600">{row.cinnyWeb}</td>
                  <td className="py-3.5 pr-4 text-slate-800 font-medium">
                    {row.rustAndroid}
                  </td>
                  <td className="py-3.5 font-mono text-xs text-indigo-600">
                    {row.fileRef}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>

      <section className="grid grid-cols-1 gap-6 md:grid-cols-3">
        <div className="rounded-2xl border border-slate-200 bg-white p-5">
          <h3 className="text-sm font-semibold text-slate-900">
            01. Cinny 主題、排版與設定中心
          </h3>
          <p className="mt-2 text-xs leading-relaxed text-slate-600">
            移植 <code className="font-mono text-slate-900">Light / Silver / Dark / Butter</code> 色票與 <code className="font-mono text-slate-900">colorMXID</code>，並在 <code className="font-mono text-slate-900">SettingsScreen</code> 集中管理外觀、個人資料、推播與開發者工具。
          </p>
        </div>
        <div className="rounded-2xl border border-slate-200 bg-white p-5">
          <h3 className="text-sm font-semibold text-slate-900">
            02. Space Rail、Explore 與 Inbox
          </h3>
          <p className="mt-2 text-xs leading-relaxed text-slate-600">
            比照 Cinny WebUI 打造 64dp Space 導覽條與分類清單，並於底部固定提供建房 / 建立 Space、公開房間目錄探索與邀請通知收件匣。
          </p>
        </div>
        <div className="rounded-2xl border border-slate-200 bg-white p-5">
          <h3 className="text-sm font-semibold text-slate-900">
            03. 房間設定、Typing 與事件原始碼
          </h3>
          <p className="mt-2 text-xs leading-relaxed text-slate-600">
            可調整房間名稱、主題、通知靜音等級與成員權限；時間軸支援即時 Typing 指示器與開發者模式 <code className="font-mono text-slate-900">original_json</code> 事件檢視。
          </p>
        </div>
      </section>
    </div>
  );
}

export function BuildGuideView() {
  const [copiedIndex, setCopiedIndex] = useState<number | null>(null);

  const handleCopy = async (text: string, idx: number) => {
    try {
      await navigator.clipboard.writeText(text);
      setCopiedIndex(idx);
      setTimeout(() => setCopiedIndex(null), 1500);
    } catch {
      // ignore clipboard errors
    }
  };

  return (
    <div className="space-y-6">
      <section className="rounded-2xl border border-slate-200 bg-white p-6">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <div>
            <h2 className="text-lg font-semibold text-slate-900">
              本地編譯與建置 APK 指南
            </h2>
            <p className="mt-1 text-sm text-slate-600">
              點擊右上角「下載專案 ZIP」解壓縮後，依照下列三個步驟即可於本機編譯 Rust 動態庫並產出原生 Android Debug APK。
            </p>
          </div>
          <a
            href="/api/project/download.zip"
            download="cinny-rust-android.zip"
            className="inline-flex min-h-[40px] items-center rounded-lg bg-indigo-600 px-4 py-2 text-xs font-semibold text-white hover:bg-indigo-500"
          >
            下載 cinny-rust-android.zip
          </a>
        </div>

        <div className="mt-6 space-y-4">
          {BUILD_COMMANDS.map((item, idx) => (
            <div
              key={item.step}
              className="overflow-hidden rounded-xl border border-slate-200 bg-slate-50"
            >
              <div className="flex items-center justify-between border-b border-slate-200 bg-white px-4 py-2.5">
                <span className="text-xs font-semibold text-slate-900">
                  {item.step}
                </span>
                <button
                  type="button"
                  onClick={() => handleCopy(item.cmd, idx)}
                  className="inline-flex min-h-[32px] items-center gap-1.5 rounded-md border border-slate-200 px-2.5 py-1 text-xs font-medium text-slate-700 hover:bg-slate-50"
                >
                  {copiedIndex === idx ? (
                    <>
                      <Check className="h-3.5 w-3.5 text-emerald-600" aria-hidden="true" />
                      <span>已複製</span>
                    </>
                  ) : (
                    <>
                      <Copy className="h-3.5 w-3.5" aria-hidden="true" />
                      <span>複製指令</span>
                    </>
                  )}
                </button>
              </div>
              <pre className="overflow-x-auto bg-slate-950 p-4 font-mono text-xs leading-relaxed text-slate-100">
                <code>{item.cmd}</code>
              </pre>
            </div>
          ))}
        </div>
      </section>
    </div>
  );
}
