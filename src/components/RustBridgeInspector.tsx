import { Bell, KeyRound, LogOut, RefreshCw, ShieldCheck, Smartphone } from 'lucide-react';
import { CinnyAppState } from '../lib/cinnyEngine';

interface RustBridgeInspectorProps {
  state: CinnyAppState;
  onSimulateIncoming: () => void;
  onSimulatePush: () => void;
  onShowLoginScreen: () => void;
  onOpenSpacesList: () => void;
  onTriggerVerify: () => void;
}

export function RustBridgeInspector({
  state,
  onSimulateIncoming,
  onSimulatePush,
  onShowLoginScreen,
  onOpenSpacesList,
  onTriggerVerify,
}: RustBridgeInspectorProps) {
  const activeRoom = state.rooms.find((r) => r.roomId === state.activeRoomId);

  return (
    <div className="space-y-6">
      {/* Quick Simulator Controls */}
      <section className="rounded-2xl border border-slate-200 bg-white p-5">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <h2 className="text-base font-semibold text-slate-900">
              Cinny 原生安卓操作與 Rust 橋接面板
            </h2>
            <p className="mt-0.5 text-xs text-slate-500">
              左側手機模擬器中的每次點擊，都會即時觸發對應的 Rust UniFFI 函式呼叫與狀態轉移。
            </p>
          </div>
          <span className="text-xs font-medium text-slate-500 tabular-nums">
            {state.spaces.length} 個 Spaces · {state.rooms.length} 個聊天室
          </span>
        </div>

        <div className="mt-4 flex flex-wrap gap-2">
          <button
            type="button"
            onClick={onOpenSpacesList}
            className="inline-flex min-h-[40px] items-center gap-1.5 rounded-lg border border-slate-200 bg-slate-50 px-3 py-2 text-xs font-medium text-slate-700 transition-colors hover:bg-slate-100"
          >
            <Smartphone className="h-3.5 w-3.5" aria-hidden="true" />
            <span>切換至 Space 列表</span>
          </button>
          <button
            type="button"
            onClick={onSimulateIncoming}
            className="inline-flex min-h-[40px] items-center gap-1.5 rounded-lg border border-slate-200 bg-slate-50 px-3 py-2 text-xs font-medium text-slate-700 transition-colors hover:bg-slate-100"
          >
            <RefreshCw className="h-3.5 w-3.5" aria-hidden="true" />
            <span>模擬收到遠端 Sync 訊息</span>
          </button>
          <button
            type="button"
            onClick={onSimulatePush}
            className="inline-flex min-h-[40px] items-center gap-1.5 rounded-lg border border-slate-200 bg-slate-50 px-3 py-2 text-xs font-medium text-slate-700 transition-colors hover:bg-slate-100"
          >
            <Bell className="h-3.5 w-3.5" aria-hidden="true" />
            <span>模擬 UnifiedPush 加密推播</span>
          </button>
          <button
            type="button"
            onClick={onTriggerVerify}
            className="inline-flex min-h-[40px] items-center gap-1.5 rounded-lg border border-slate-200 bg-slate-50 px-3 py-2 text-xs font-medium text-slate-700 transition-colors hover:bg-slate-100"
          >
            <ShieldCheck className="h-3.5 w-3.5" aria-hidden="true" />
            <span>SAS / 4S 安全中心</span>
          </button>
          <button
            type="button"
            onClick={onShowLoginScreen}
            className="inline-flex min-h-[40px] items-center gap-1.5 rounded-lg border border-slate-200 bg-slate-50 px-3 py-2 text-xs font-medium text-slate-700 transition-colors hover:bg-slate-100"
          >
            <LogOut className="h-3.5 w-3.5" aria-hidden="true" />
            <span>檢視密碼 / SSO 登入畫面</span>
          </button>
        </div>
      </section>

      {/* Active Rust Struct Snapshot */}
      <section className="rounded-2xl border border-slate-200 bg-white p-5">
        <h3 className="text-sm font-semibold text-slate-900">
          目前 Rust 核心狀態結構 (UniFFI Record)
        </h3>
        <div className="mt-3 grid grid-cols-1 gap-3 sm:grid-cols-3 text-xs">
          <div className="rounded-xl bg-slate-50 p-3">
            <div className="text-slate-500">Auth / Push</div>
            <div className="mt-1 truncate font-mono font-medium text-slate-900">
              {state.authMethod.toUpperCase()} · {state.pushEnabled ? 'UP Active' : 'Off'}
            </div>
          </div>
          <div className="rounded-xl bg-slate-50 p-3">
            <div className="text-slate-500">Active RoomSummary</div>
            <div className="mt-1 truncate font-mono font-medium text-slate-900">
              {activeRoom ? activeRoom.roomId : 'None (Space List)'}
            </div>
          </div>
          <div className="rounded-xl bg-slate-50 p-3">
            <div className="text-slate-500 flex items-center gap-1">
              <KeyRound className="h-3 w-3" aria-hidden="true" />
              <span>CryptoStore / 4S</span>
            </div>
            <div className="mt-1 truncate font-mono font-medium text-slate-900">
              {state.recoveryUnlocked
                ? '4S Backup Unlocked'
                : state.verification.kind === 'confirmed'
                  ? `Verified (${state.verification.deviceId})`
                  : state.verification.kind === 'sas'
                    ? 'SasStarted (7 emojis)'
                    : 'Olm/Megolm Ready'}
            </div>
          </div>
        </div>
      </section>

      {/* Live UniFFI Invocation Trace */}
      <section className="rounded-2xl border border-slate-200 bg-white p-5">
        <div className="flex items-center justify-between">
          <h3 className="text-sm font-semibold text-slate-900">
            即時 UniFFI 呼叫紀錄 (Kotlin → Rust)
          </h3>
          <span className="text-xs text-slate-500 tabular-nums">
            最近 {state.traces.length} 筆
          </span>
        </div>

        <div className="mt-3 divide-y divide-slate-100">
          {state.traces.map((trace) => (
            <div
              key={trace.id}
              className="flex flex-col gap-1 py-2.5 sm:flex-row sm:items-center sm:justify-between"
            >
              <div className="min-w-0">
                <span className="font-mono text-xs font-semibold text-indigo-600">
                  {trace.fnName}
                </span>
                <span className="ml-2 font-mono text-xs text-slate-600 break-words">
                  ({trace.argsSummary})
                </span>
              </div>
              <div className="shrink-0 text-[11px] text-slate-500 tabular-nums">
                {trace.rustModule} · {trace.timestamp}
              </div>
            </div>
          ))}
        </div>
      </section>
    </div>
  );
}
