import { useState } from 'react';
import { AndroidPhoneSimulator } from './components/AndroidPhoneSimulator';
import { ArchitectureView, BuildGuideView } from './components/ArchitectureGuide';
import { RustBridgeInspector } from './components/RustBridgeInspector';
import { SourceExplorer } from './components/SourceExplorer';
import { StudioTab, TopNav } from './components/TopNav';
import {
  CinnyAppState,
  completeSsoLoginFlow,
  confirmSasVerificationFlow,
  createInitialCinnyState,
  createRoomFlow,
  decryptMediaPreviewFlow,
  joinDiscoverableSpaceRoom,
  recoverSecretBackupFlow,
  redactMessage,
  selectRoom,
  sendMarkdownMessage,
  sendMediaAttachmentFlow,
  sendThreadReply,
  simulateIncomingSyncMessage,
  simulatePushNotificationFlow,
  startSasVerificationFlow,
  toggleReactionOnMessage,
} from './lib/cinnyEngine';

function currentTimeLabel(): string {
  const now = new Date();
  const hh = String(now.getHours()).padStart(2, '0');
  const mm = String(now.getMinutes()).padStart(2, '0');
  return `${hh}:${mm}`;
}

function currentClockSec(): string {
  const now = new Date();
  const hh = String(now.getHours()).padStart(2, '0');
  const mm = String(now.getMinutes()).padStart(2, '0');
  const ss = String(now.getSeconds()).padStart(2, '0');
  return `${hh}:${mm}:${ss}`;
}

export default function App() {
  const [activeTab, setActiveTab] = useState<StudioTab>('simulator');
  const [cinnyState, setCinnyState] = useState<CinnyAppState>(() =>
    createInitialCinnyState()
  );

  const handleLogin = (homeserver: string, username: string) => {
    setCinnyState((prev) => ({
      ...prev,
      isAuthenticated: true,
      authMethod: 'password',
      homeserverUrl: homeserver,
      currentUserId: username,
      traces: [
        {
          id: `tr-login-${Date.now()}`,
          fnName: 'CinnyClient::login_with_password',
          argsSummary: `homeserver="${homeserver}", user="${username}"`,
          rustModule: 'client.rs',
          timestamp: currentClockSec(),
        },
        ...prev.traces,
      ].slice(0, 14),
    }));
  };

  const handleSsoLogin = (homeserver: string) => {
    setCinnyState((prev) =>
      completeSsoLoginFlow(prev, homeserver, '@sso_android:matrix.org', currentClockSec())
    );
  };

  const handleSelectSpace = (spaceFilter: 'all' | 'dm' | string) => {
    setCinnyState((prev) => ({
      ...prev,
      selectedSpaceFilter: spaceFilter,
      activeRoomId: null,
      activeThreadEventId: null,
    }));
  };

  const handleSelectRoom = (roomId: string) => {
    setCinnyState((prev) => selectRoom(prev, roomId, currentClockSec()));
  };

  const handleCreateRoom = (name: string, topic: string, encrypted: boolean) => {
    setCinnyState((prev) =>
      createRoomFlow(prev, name, topic, encrypted, prev.selectedSpaceFilter, currentClockSec())
    );
  };

  const handleJoinDiscoverableRoom = (roomId: string) => {
    setCinnyState((prev) => joinDiscoverableSpaceRoom(prev, roomId, currentClockSec()));
  };

  const handleBackToSpaces = () => {
    setCinnyState((prev) => ({
      ...prev,
      activeRoomId: null,
      activeThreadEventId: null,
    }));
  };

  const handleToggleTheme = () => {
    setCinnyState((prev) => ({ ...prev, darkTheme: !prev.darkTheme }));
  };

  const handleSendMessage = (markdown: string, replyToId?: string | null) => {
    setCinnyState((prev) =>
      prev.activeRoomId
        ? sendMarkdownMessage(
            prev,
            prev.activeRoomId,
            markdown,
            replyToId,
            currentTimeLabel()
          )
        : prev
    );
  };

  const handleSendMediaAttachment = (filename: string, mimeType: string) => {
    setCinnyState((prev) =>
      prev.activeRoomId
        ? sendMediaAttachmentFlow(
            prev,
            prev.activeRoomId,
            filename,
            mimeType,
            currentTimeLabel()
          )
        : prev
    );
  };

  const handleDecryptMedia = (eventId: string) => {
    setCinnyState((prev) =>
      prev.activeRoomId
        ? decryptMediaPreviewFlow(prev, prev.activeRoomId, eventId, currentClockSec())
        : prev
    );
  };

  const handleToggleReaction = (eventId: string, emoji: string) => {
    setCinnyState((prev) =>
      prev.activeRoomId
        ? toggleReactionOnMessage(
            prev,
            prev.activeRoomId,
            eventId,
            emoji,
            currentClockSec()
          )
        : prev
    );
  };

  const handleRedactMessage = (eventId: string) => {
    setCinnyState((prev) =>
      prev.activeRoomId
        ? redactMessage(prev, prev.activeRoomId, eventId, currentClockSec())
        : prev
    );
  };

  const handleOpenThread = (eventId: string | null) => {
    setCinnyState((prev) => ({ ...prev, activeThreadEventId: eventId }));
  };

  const handleSendThreadReply = (rootEventId: string, markdown: string) => {
    setCinnyState((prev) =>
      prev.activeRoomId
        ? sendThreadReply(
            prev,
            prev.activeRoomId,
            rootEventId,
            markdown,
            currentTimeLabel()
          )
        : prev
    );
  };

  const handleSimulateIncoming = () => {
    setCinnyState((prev) => {
      const targetRoom = prev.activeRoomId ?? '!cinny-dev:matrix.org';
      return simulateIncomingSyncMessage(
        prev,
        targetRoom,
        'Ajay',
        '已推送最新 cargo-ndk 與 UniFFI 0.29 async 綁定測試結果！',
        currentTimeLabel()
      );
    });
  };

  const handleSimulatePush = () => {
    setCinnyState((prev) =>
      simulatePushNotificationFlow(
        prev,
        '!uniffi-bindings:matrix.org',
        'Lin (Rust Core)',
        'WorkManager 已成功在背景解密 E2EE 推播訊息！',
        currentTimeLabel()
      )
    );
  };

  return (
    <div className="min-h-screen bg-slate-50 text-slate-900">
      <TopNav activeTab={activeTab} onSelectTab={setActiveTab} />

      <main className="mx-auto max-w-7xl px-4 py-5 sm:px-6">
        <section className="mb-5 flex flex-col justify-between gap-4 border-b border-slate-200 pb-4 md:flex-row md:items-end">
          <div>
            <h1 className="text-2xl font-bold tracking-tight text-slate-900 sm:text-3xl text-balance">
              以 Rust 驅動的 Cinny 原生安卓 Matrix 客戶端
            </h1>
            <p className="mt-1.5 max-w-3xl text-sm text-slate-600">
              參考 cinnyapp/cinny 設計語言，核心採用 matrix-sdk 0.16、SSO / 4S 金鑰備份、AES-CTR 加密媒體與 UnifiedPush，原生 Android 介面採用 Jetpack Compose Material 3。
            </p>
          </div>
          <div className="flex shrink-0 items-center gap-2 text-xs text-slate-500 tabular-nums">
            <span>matrix-sdk 0.16</span>
            <span aria-hidden="true">·</span>
            <span>UniFFI 0.29</span>
            <span aria-hidden="true">·</span>
            <span>Jetpack Compose</span>
          </div>
        </section>

        {activeTab === 'simulator' && (
          <div className="grid grid-cols-1 items-start gap-8 lg:grid-cols-12">
            <div className="lg:col-span-5 flex justify-center">
              <AndroidPhoneSimulator
                state={cinnyState}
                onLogin={handleLogin}
                onSsoLogin={handleSsoLogin}
                onSelectSpace={handleSelectSpace}
                onSelectRoom={handleSelectRoom}
                onCreateRoom={handleCreateRoom}
                onJoinDiscoverableRoom={handleJoinDiscoverableRoom}
                onBackToSpaces={handleBackToSpaces}
                onToggleTheme={handleToggleTheme}
                onSendMessage={handleSendMessage}
                onSendMediaAttachment={handleSendMediaAttachment}
                onDecryptMedia={handleDecryptMedia}
                onToggleReaction={handleToggleReaction}
                onRedactMessage={handleRedactMessage}
                onOpenThread={handleOpenThread}
                onSendThreadReply={handleSendThreadReply}
                onStartVerification={() =>
                  setCinnyState((prev) =>
                    startSasVerificationFlow(prev, currentClockSec())
                  )
                }
                onConfirmVerification={() =>
                  setCinnyState((prev) =>
                    confirmSasVerificationFlow(
                      prev,
                      'CINNY_PIXEL_9_PRO',
                      currentClockSec()
                    )
                  )
                }
                onRecoverSecretBackup={(key) =>
                  setCinnyState((prev) =>
                    recoverSecretBackupFlow(prev, key, currentClockSec())
                  )
                }
                onCloseVerification={() =>
                  setCinnyState((prev) => ({
                    ...prev,
                    verification: { kind: 'idle' },
                  }))
                }
                onDismissPushBanner={() =>
                  setCinnyState((prev) => ({
                    ...prev,
                    activePushBanner: null,
                  }))
                }
              />
            </div>

            <div className="lg:col-span-7">
              <RustBridgeInspector
                state={cinnyState}
                onSimulateIncoming={handleSimulateIncoming}
                onSimulatePush={handleSimulatePush}
                onShowLoginScreen={() =>
                  setCinnyState((prev) => ({ ...prev, isAuthenticated: false }))
                }
                onOpenSpacesList={handleBackToSpaces}
                onTriggerVerify={() =>
                  setCinnyState((prev) =>
                    startSasVerificationFlow(prev, currentClockSec())
                  )
                }
              />
            </div>
          </div>
        )}

        {activeTab === 'source' && <SourceExplorer />}
        {activeTab === 'architecture' && <ArchitectureView />}
        {activeTab === 'build' && <BuildGuideView />}
      </main>
    </div>
  );
}
