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
    layer: 'Spaces 探索、建立與管理',
    cinnyWeb: 'm.space.child DAG + Hierarchy API',
    rustAndroid: 'browse_space_hierarchy / create_room / invite / members',
    fileRef: 'rust-core/src/spaces.rs',
  },
  {
    layer: '時間軸、討論串與本地搜尋',
    cinnyWeb: 'RoomTimeline + Thread Drawer',
    rustAndroid: 'TimelineFocus::Thread + send_reply + 離線解密搜尋',
    fileRef: 'rust-core/src/timeline.rs',
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
          Cinny Web 至 Rust 原生安卓架構對照 (v0.3.0 完整版)
        </h2>
        <p className="mt-1 text-sm text-slate-600">
          對齊 cinnyapp/cinny 完整功能矩陣：支援密碼與 SSO 登入、4S 恢復金鑰備份、AES-CTR 加密媒體上傳與解密、Thread 討論串、UnifiedPush 背景推播與完整房間管理。
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
            01. SSO、4S 備份與裝置驗證
          </h3>
          <p className="mt-2 text-xs leading-relaxed text-slate-600">
            透過 <code className="font-mono text-slate-900">CustomTabsIntent</code> 與 <code className="font-mono text-slate-900">cinny://sso</code> 承接 SSO Token，並支援 <code className="font-mono text-slate-900">4S Recovery Key</code> 還原歷史 Megolm 金鑰。
          </p>
        </div>
        <div className="rounded-2xl border border-slate-200 bg-white p-5">
          <h3 className="text-sm font-semibold text-slate-900">
            02. AES-CTR 加密媒體與 Threads
          </h3>
          <p className="mt-2 text-xs leading-relaxed text-slate-600">
            整合 <code className="font-mono text-slate-900">media.rs</code> 加解密快取與 <code className="font-mono text-slate-900">FileProvider</code>，並透過 <code className="font-mono text-slate-900">TimelineFocus::Thread</code> 驅動原生討論串。
          </p>
        </div>
        <div className="rounded-2xl border border-slate-200 bg-white p-5">
          <h3 className="text-sm font-semibold text-slate-900">
            03. UnifiedPush 與房間完整控制
          </h3>
          <p className="mt-2 text-xs leading-relaxed text-slate-600">
            <code className="font-mono text-slate-900">PushSyncWorker</code> 於背景恢復 SQLite 並解密通知內容；UI 亦涵蓋建房、邀請、Space 頻道探索與離線搜尋。
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
