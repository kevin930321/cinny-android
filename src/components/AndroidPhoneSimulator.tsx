import { useState, FormEvent } from 'react';
import {
  ArrowLeft,
  Bell,
  Compass,
  CornerUpLeft,
  Download,
  ExternalLink,
  Hash,
  KeyRound,
  Lock,
  MessageSquare,
  Moon,
  Paperclip,
  Plus,
  Search,
  Send,
  ShieldCheck,
  SmilePlus,
  Sun,
  Trash2,
  X,
} from 'lucide-react';
import {
  CinnyAppState,
  RoomSummary,
  TimelineMessage,
  filterRooms,
  getSpaceUnreadSummary,
} from '../lib/cinnyEngine';

interface AndroidPhoneSimulatorProps {
  state: CinnyAppState;
  onLogin: (homeserver: string, username: string) => void;
  onSsoLogin: (homeserver: string) => void;
  onSelectSpace: (spaceFilter: 'all' | 'dm' | string) => void;
  onSelectRoom: (roomId: string) => void;
  onCreateRoom: (name: string, topic: string, encrypted: boolean) => void;
  onJoinDiscoverableRoom: (roomId: string) => void;
  onBackToSpaces: () => void;
  onToggleTheme: () => void;
  onSendMessage: (markdown: string, replyToId?: string | null) => void;
  onSendMediaAttachment: (filename: string, mimeType: string) => void;
  onDecryptMedia: (eventId: string) => void;
  onToggleReaction: (eventId: string, emoji: string) => void;
  onRedactMessage: (eventId: string) => void;
  onOpenThread: (eventId: string | null) => void;
  onSendThreadReply: (rootEventId: string, markdown: string) => void;
  onStartVerification: () => void;
  onConfirmVerification: () => void;
  onRecoverSecretBackup: (key: string) => void;
  onCloseVerification: () => void;
  onDismissPushBanner: () => void;
}

const QUICK_REACTIONS = ['👍', '🦀', '🚀', '🔥'];

function SafeAvatar({
  src,
  alt,
  fallback,
  dark,
}: {
  src?: string;
  alt: string;
  fallback: string;
  dark: boolean;
}) {
  const [failed, setFailed] = useState(false);

  if (!src || failed) {
    return (
      <span
        className={`flex h-full w-full items-center justify-center text-xs font-semibold ${
          dark ? 'bg-slate-800 text-slate-200' : 'bg-slate-200 text-slate-700'
        }`}
      >
        {fallback}
      </span>
    );
  }

  return (
    <img
      src={src}
      alt={alt}
      referrerPolicy="no-referrer"
      onError={() => setFailed(true)}
      className="h-full w-full object-cover"
    />
  );
}

export function AndroidPhoneSimulator({
  state,
  onLogin,
  onSsoLogin,
  onSelectSpace,
  onSelectRoom,
  onCreateRoom,
  onJoinDiscoverableRoom,
  onBackToSpaces,
  onToggleTheme,
  onSendMessage,
  onSendMediaAttachment,
  onDecryptMedia,
  onToggleReaction,
  onRedactMessage,
  onOpenThread,
  onSendThreadReply,
  onStartVerification,
  onConfirmVerification,
  onRecoverSecretBackup,
  onCloseVerification,
  onDismissPushBanner,
}: AndroidPhoneSimulatorProps) {
  const [roomQuery, setRoomQuery] = useState('');
  const [composerText, setComposerText] = useState('');
  const [replyingTo, setReplyingTo] = useState<TimelineMessage | null>(null);
  const [reactionMenuForId, setReactionMenuForId] = useState<string | null>(null);
  const [threadDraft, setThreadDraft] = useState('');
  const [loginHomeserver, setLoginHomeserver] = useState(state.homeserverUrl);
  const [loginUser, setLoginUser] = useState('@you:matrix.org');
  const [loginPassword, setLoginPassword] = useState('demo-matrix-pass');
  const [loginError, setLoginError] = useState('');

  // Room search, Create Room & Space Discovery state
  const [showMsgSearch, setShowMsgSearch] = useState(false);
  const [msgSearchQuery, setMsgSearchQuery] = useState('');
  const [showCreateRoomModal, setShowCreateRoomModal] = useState(false);
  const [newRoomName, setNewRoomName] = useState('');
  const [newRoomTopic, setNewRoomTopic] = useState('');
  const [newRoomEncrypted, setNewRoomEncrypted] = useState(true);
  const [showSpaceDiscoverModal, setShowSpaceDiscoverModal] = useState(false);

  // Security modal tabs: SAS vs 4S Recovery
  const [securityTab, setSecurityTab] = useState<'sas' | 'recovery'>('sas');
  const [recoveryKeyInput, setRecoveryKeyInput] = useState('EsTX-984C-Rust-Backup');

  const dark = state.darkTheme;
  const activeRoom: RoomSummary | undefined = state.rooms.find(
    (r) => r.roomId === state.activeRoomId
  );
  const allMessages: TimelineMessage[] = state.activeRoomId
    ? state.messagesByRoom[state.activeRoomId] ?? []
    : [];
  const activeMessages =
    showMsgSearch && msgSearchQuery.trim()
      ? allMessages.filter(
          (m) =>
            !m.isRedacted &&
            (m.body.toLowerCase().includes(msgSearchQuery.toLowerCase()) ||
              m.senderName.toLowerCase().includes(msgSearchQuery.toLowerCase()))
        )
      : allMessages;
  const activeThreadRoot = state.activeThreadEventId
    ? allMessages.find((m) => m.eventId === state.activeThreadEventId)
    : undefined;

  const filteredRooms = filterRooms(state.rooms, state.selectedSpaceFilter, roomQuery);
  const activeSpaceName =
    state.selectedSpaceFilter === 'all'
      ? '全部聊天室'
      : state.selectedSpaceFilter === 'dm'
        ? '私訊 Direct Messages'
        : state.spaces.find((s) => s.roomId === state.selectedSpaceFilter)?.name ?? 'Spaces';

  const handleSend = (e: FormEvent) => {
    e.preventDefault();
    if (!composerText.trim()) return;
    onSendMessage(composerText, replyingTo?.eventId ?? null);
    setComposerText('');
    setReplyingTo(null);
  };

  const handleThreadSubmit = (e: FormEvent) => {
    e.preventDefault();
    if (!activeThreadRoot || !threadDraft.trim()) return;
    onSendThreadReply(activeThreadRoot.eventId, threadDraft);
    setThreadDraft('');
  };

  const handleLoginSubmit = (e: FormEvent) => {
    e.preventDefault();
    if (!loginHomeserver.trim() || !loginUser.trim() || !loginPassword.trim()) {
      setLoginError('請輸入 Homeserver、Matrix ID 與密碼');
      return;
    }
    setLoginError('');
    onLogin(loginHomeserver.trim(), loginUser.trim());
  };

  const handleCreateSubmit = (e: FormEvent) => {
    e.preventDefault();
    if (!newRoomName.trim()) return;
    onCreateRoom(newRoomName, newRoomTopic, newRoomEncrypted);
    setNewRoomName('');
    setNewRoomTopic('');
    setShowCreateRoomModal(false);
  };

  return (
    <div className="flex flex-col items-center">
      {/* Android Device Frame */}
      <div
        data-testid="android-device-frame"
        className={`relative w-full max-w-[390px] overflow-hidden rounded-[34px] border-[6px] shadow-xl transition-colors ${
          dark
            ? 'border-slate-800 bg-[#0B0F17] text-slate-100'
            : 'border-slate-900 bg-[#F8FAFC] text-slate-900'
        }`}
      >
        {/* Android Status Bar */}
        <div
          className={`flex items-center justify-between px-6 pt-3 pb-2 text-[11px] font-medium tabular-nums ${
            dark ? 'bg-[#111827] text-slate-300' : 'bg-white text-slate-600'
          }`}
        >
          <span>19:47</span>
          <span
            aria-hidden="true"
            className="h-3.5 w-16 rounded-full bg-slate-950/90"
          />
          <span>5G · {state.recoveryUnlocked ? '4S' : 'E2EE'}</span>
        </div>

        {/* Viewport Body */}
        <div className="relative flex h-[545px] flex-col overflow-hidden">
          {/* UnifiedPush Android Heads-Up Notification Banner */}
          {state.activePushBanner && (
            <div
              role="region"
              aria-label="UnifiedPush 推播通知"
              className={`absolute top-2 right-2.5 left-2.5 z-30 flex items-center justify-between gap-2 rounded-2xl border p-3 shadow-lg ${
                dark
                  ? 'border-indigo-500/40 bg-slate-900 text-slate-100'
                  : 'border-indigo-200 bg-white text-slate-900'
              }`}
            >
              <button
                type="button"
                onClick={() => onSelectRoom(state.activePushBanner!.roomId)}
                className="flex min-w-0 flex-1 items-start gap-2.5 text-left"
              >
                <Bell className="mt-0.5 h-4 w-4 shrink-0 text-indigo-500" aria-hidden="true" />
                <div className="min-w-0 flex-1">
                  <div className="truncate text-xs font-semibold">
                    {state.activePushBanner.roomName} · E2EE 已解密
                  </div>
                  <div
                    className={`truncate text-[11px] ${
                      dark ? 'text-slate-300' : 'text-slate-600'
                    }`}
                  >
                    {state.activePushBanner.senderName}: {state.activePushBanner.bodyPreview}
                  </div>
                </div>
              </button>
              <button
                type="button"
                onClick={onDismissPushBanner}
                aria-label="關閉推播通知"
                className="p-1 text-slate-400 hover:text-slate-600"
              >
                <X className="h-3.5 w-3.5" />
              </button>
            </div>
          )}

          {!state.isAuthenticated ? (
            /* Android Screen 1: Matrix Login Screen (Password + SSO/OIDC) */
            <form
              onSubmit={handleLoginSubmit}
              className="flex flex-1 flex-col justify-center px-6 py-6"
            >
              <div className="mb-5">
                <h2 className="text-2xl font-bold tracking-tight">Cinny</h2>
                <p
                  className={`mt-1 text-xs leading-relaxed ${
                    dark ? 'text-slate-400' : 'text-slate-600'
                  }`}
                >
                  支援密碼與 SSO / OIDC 瀏覽器驗證，並以 Rust Keystore 加密保存 SQLite 狀態。
                </p>
              </div>

              <div className="space-y-2.5">
                <div>
                  <label
                    htmlFor="android-homeserver"
                    className={`block text-xs font-medium mb-1 ${
                      dark ? 'text-slate-300' : 'text-slate-700'
                    }`}
                  >
                    Homeserver URL
                  </label>
                  <input
                    id="android-homeserver"
                    type="url"
                    value={loginHomeserver}
                    onChange={(e) => setLoginHomeserver(e.target.value)}
                    className={`w-full rounded-xl border px-3.5 py-2 text-sm outline-none focus:ring-2 focus:ring-indigo-500 ${
                      dark
                        ? 'border-slate-700 bg-slate-900 text-slate-100'
                        : 'border-slate-200 bg-white text-slate-900'
                    }`}
                  />
                </div>
                <div>
                  <label
                    htmlFor="android-username"
                    className={`block text-xs font-medium mb-1 ${
                      dark ? 'text-slate-300' : 'text-slate-700'
                    }`}
                  >
                    Matrix ID
                  </label>
                  <input
                    id="android-username"
                    type="text"
                    value={loginUser}
                    onChange={(e) => setLoginUser(e.target.value)}
                    className={`w-full rounded-xl border px-3.5 py-2 text-sm outline-none focus:ring-2 focus:ring-indigo-500 ${
                      dark
                        ? 'border-slate-700 bg-slate-900 text-slate-100'
                        : 'border-slate-200 bg-white text-slate-900'
                    }`}
                  />
                </div>
                <div>
                  <label
                    htmlFor="android-password"
                    className={`block text-xs font-medium mb-1 ${
                      dark ? 'text-slate-300' : 'text-slate-700'
                    }`}
                  >
                    Password
                  </label>
                  <input
                    id="android-password"
                    type="password"
                    value={loginPassword}
                    onChange={(e) => setLoginPassword(e.target.value)}
                    className={`w-full rounded-xl border px-3.5 py-2 text-sm outline-none focus:ring-2 focus:ring-indigo-500 ${
                      dark
                        ? 'border-slate-700 bg-slate-900 text-slate-100'
                        : 'border-slate-200 bg-white text-slate-900'
                    }`}
                  />
                </div>
              </div>

              {loginError && (
                <p className="mt-2.5 text-xs text-red-600">{loginError}</p>
              )}

              <button
                type="submit"
                className="mt-5 min-h-[44px] w-full rounded-xl bg-indigo-600 px-4 py-2 text-sm font-semibold text-white transition-colors hover:bg-indigo-500"
              >
                以密碼登入 Matrix
              </button>
              <button
                type="button"
                onClick={() => onSsoLogin(loginHomeserver.trim() || 'https://matrix.org')}
                className={`mt-2.5 inline-flex min-h-[42px] w-full items-center justify-center gap-2 rounded-xl border px-4 py-2 text-xs font-semibold transition-colors ${
                  dark
                    ? 'border-slate-700 bg-slate-900 text-slate-200 hover:bg-slate-800'
                    : 'border-slate-200 bg-white text-slate-700 hover:bg-slate-100'
                }`}
              >
                <ExternalLink className="h-3.5 w-3.5" aria-hidden="true" />
                <span>透過 SSO / OIDC 瀏覽器登入</span>
              </button>
            </form>
          ) : activeRoom ? (
            /* Android Screen 3: Room Timeline Screen */
            <div className="flex flex-1 flex-col overflow-hidden">
              {/* Top App Bar */}
              <div
                className={`flex items-center justify-between border-b px-3 py-2.5 ${
                  dark
                    ? 'border-slate-800 bg-[#111827]'
                    : 'border-slate-200 bg-white'
                }`}
              >
                <div className="flex min-w-0 items-center gap-1.5">
                  <button
                    type="button"
                    onClick={onBackToSpaces}
                    aria-label="返回 Space 與聊天室列表"
                    className={`flex h-9 w-9 shrink-0 items-center justify-center rounded-xl transition-colors ${
                      dark ? 'hover:bg-slate-800' : 'hover:bg-slate-100'
                    }`}
                  >
                    <ArrowLeft className="h-4 w-4" />
                  </button>
                  <div className="min-w-0">
                    <div className="flex items-center gap-1.5">
                      {activeRoom.isEncrypted ? (
                        <Lock
                          className="h-3.5 w-3.5 shrink-0 text-emerald-600"
                          aria-hidden="true"
                        />
                      ) : (
                        <Hash
                          className="h-3.5 w-3.5 shrink-0 text-slate-400"
                          aria-hidden="true"
                        />
                      )}
                      <span className="truncate text-sm font-semibold">
                        {activeRoom.name}
                      </span>
                    </div>
                    <p
                      className={`truncate text-[11px] ${
                        dark ? 'text-slate-400' : 'text-slate-500'
                      }`}
                    >
                      {activeRoom.isEncrypted ? 'Megolm E2EE' : '公開頻道'} ·{' '}
                      {allMessages.length} 則訊息
                    </p>
                  </div>
                </div>

                <div className="flex shrink-0 items-center gap-0.5">
                  <button
                    type="button"
                    onClick={() => {
                      setShowMsgSearch((v) => !v);
                      setMsgSearchQuery('');
                    }}
                    aria-label="搜尋房間歷史訊息"
                    className={`flex h-9 w-9 items-center justify-center rounded-xl transition-colors ${
                      showMsgSearch
                        ? 'text-indigo-500'
                        : dark
                          ? 'text-slate-300 hover:bg-slate-800'
                          : 'text-slate-600 hover:bg-slate-100'
                    }`}
                  >
                    <Search className="h-4 w-4" />
                  </button>
                  <button
                    type="button"
                    onClick={() => {
                      setSecurityTab('sas');
                      onStartVerification();
                    }}
                    aria-label="開啟裝置 E2EE 安全中心"
                    className={`flex h-9 w-9 items-center justify-center rounded-xl transition-colors ${
                      state.verification.kind === 'confirmed' || state.recoveryUnlocked
                        ? 'text-emerald-600'
                        : dark
                          ? 'text-slate-300 hover:bg-slate-800'
                          : 'text-slate-600 hover:bg-slate-100'
                    }`}
                  >
                    <ShieldCheck className="h-4 w-4" />
                  </button>
                  <button
                    type="button"
                    onClick={onToggleTheme}
                    aria-label="切換 Cinny 淺色或暗黑主題"
                    className={`flex h-9 w-9 items-center justify-center rounded-xl transition-colors ${
                      dark
                        ? 'text-slate-300 hover:bg-slate-800'
                        : 'text-slate-600 hover:bg-slate-100'
                    }`}
                  >
                    {dark ? <Sun className="h-4 w-4" /> : <Moon className="h-4 w-4" />}
                  </button>
                </div>
              </div>

              {showMsgSearch && (
                <div
                  className={`border-b px-3 py-2 ${
                    dark ? 'border-slate-800 bg-[#111827]' : 'border-slate-200 bg-white'
                  }`}
                >
                  <input
                    type="text"
                    value={msgSearchQuery}
                    onChange={(e) => setMsgSearchQuery(e.target.value)}
                    placeholder="搜尋本地已解密訊息..."
                    aria-label="搜尋房間訊息"
                    className={`min-h-[34px] w-full rounded-lg border px-2.5 text-xs outline-none focus:ring-2 focus:ring-indigo-500 ${
                      dark
                        ? 'border-slate-700 bg-slate-900 text-slate-100'
                        : 'border-slate-200 bg-slate-50 text-slate-900'
                    }`}
                  />
                </div>
              )}

              {/* Message Timeline */}
              <div
                data-testid="android-timeline-list"
                className="flex-1 space-y-3 overflow-y-auto px-3.5 py-3"
              >
                {activeMessages.map((msg) => (
                  <article
                    key={msg.eventId}
                    className={`group rounded-2xl p-2.5 transition-colors ${
                      dark ? 'hover:bg-slate-900/70' : 'hover:bg-white'
                    }`}
                  >
                    {msg.replyTo && (
                      <div
                        className={`mb-1.5 flex items-center gap-1.5 rounded-lg px-2.5 py-1 text-[11px] ${
                          dark
                            ? 'bg-slate-800/80 text-slate-300'
                            : 'bg-slate-100 text-slate-600'
                        }`}
                      >
                        <CornerUpLeft className="h-3 w-3 shrink-0" />
                        <span className="font-medium">{msg.replyTo.senderName}:</span>
                        <span className="truncate">{msg.replyTo.body}</span>
                      </div>
                    )}

                    <div className="flex items-start gap-2.5">
                      <div className="h-8 w-8 shrink-0 overflow-hidden rounded-full">
                        <SafeAvatar
                          src={msg.senderAvatarUrl}
                          alt={msg.senderName}
                          fallback={msg.senderName.slice(0, 2).toUpperCase()}
                          dark={dark}
                        />
                      </div>

                      <div className="min-w-0 flex-1">
                        <div className="flex items-baseline justify-between gap-2">
                          <div className="flex items-baseline gap-1.5 min-w-0">
                            <span
                              className={`truncate text-xs font-semibold ${
                                msg.isOwn
                                  ? 'text-indigo-500'
                                  : dark
                                    ? 'text-slate-100'
                                    : 'text-slate-900'
                              }`}
                            >
                              {msg.senderName}
                            </span>
                            <span
                              className={`text-[10px] tabular-nums ${
                                dark ? 'text-slate-500' : 'text-slate-400'
                              }`}
                            >
                              {msg.timestampLabel}
                            </span>
                            {msg.isEdited && !msg.isRedacted && (
                              <span className="text-[10px] text-slate-400">· 已編輯</span>
                            )}
                          </div>

                          {!msg.isRedacted && (
                            <div className="flex items-center gap-0.5">
                              <button
                                type="button"
                                onClick={() =>
                                  setReactionMenuForId(
                                    reactionMenuForId === msg.eventId
                                      ? null
                                      : msg.eventId
                                  )
                                }
                                aria-label={`對 ${msg.senderName} 的訊息加上表情反應`}
                                className={`flex h-7 w-7 items-center justify-center rounded-lg ${
                                  dark
                                    ? 'text-slate-400 hover:bg-slate-800 hover:text-slate-200'
                                    : 'text-slate-400 hover:bg-slate-100 hover:text-slate-700'
                                }`}
                              >
                                <SmilePlus className="h-3.5 w-3.5" />
                              </button>
                              <button
                                type="button"
                                onClick={() => setReplyingTo(msg)}
                                aria-label={`回覆 ${msg.senderName} 的訊息`}
                                className={`flex h-7 w-7 items-center justify-center rounded-lg ${
                                  dark
                                    ? 'text-slate-400 hover:bg-slate-800 hover:text-slate-200'
                                    : 'text-slate-400 hover:bg-slate-100 hover:text-slate-700'
                                }`}
                              >
                                <CornerUpLeft className="h-3.5 w-3.5" />
                              </button>
                              <button
                                type="button"
                                onClick={() => onOpenThread(msg.eventId)}
                                aria-label={`開啟 ${msg.senderName} 訊息討論串`}
                                className={`flex h-7 w-7 items-center justify-center rounded-lg ${
                                  dark
                                    ? 'text-slate-400 hover:bg-slate-800 hover:text-slate-200'
                                    : 'text-slate-400 hover:bg-slate-100 hover:text-slate-700'
                                }`}
                              >
                                <MessageSquare className="h-3.5 w-3.5" />
                              </button>
                              {msg.isOwn && (
                                <button
                                  type="button"
                                  onClick={() => onRedactMessage(msg.eventId)}
                                  aria-label={`撤回我的訊息`}
                                  className={`flex h-7 w-7 items-center justify-center rounded-lg ${
                                    dark
                                      ? 'text-slate-400 hover:bg-slate-800 hover:text-red-400'
                                      : 'text-slate-400 hover:bg-slate-100 hover:text-red-600'
                                  }`}
                                >
                                  <Trash2 className="h-3.5 w-3.5" />
                                </button>
                              )}
                            </div>
                          )}
                        </div>

                        <p
                          className={`mt-1 text-[13px] leading-relaxed break-words ${
                            msg.isRedacted || msg.isUnableToDecrypt
                              ? 'italic text-amber-600'
                              : dark
                                ? 'text-slate-200'
                                : 'text-slate-700'
                          }`}
                        >
                          {msg.body}
                        </p>

                        {/* Encrypted Media Attachment Card */}
                        {msg.attachment && !msg.isRedacted && (
                          <div
                            className={`mt-2 overflow-hidden rounded-xl border p-2 ${
                              dark
                                ? 'border-slate-700 bg-slate-900'
                                : 'border-slate-200 bg-slate-50'
                            }`}
                          >
                            {msg.attachment.isDecrypted && msg.attachment.decryptedPreviewUrl ? (
                              <img
                                src={msg.attachment.decryptedPreviewUrl}
                                alt={msg.attachment.filename}
                                className="mb-2 h-24 w-full rounded-lg object-cover"
                              />
                            ) : null}
                            <div className="flex items-center justify-between gap-2">
                              <div className="min-w-0">
                                <div className="truncate text-xs font-medium">
                                  {msg.attachment.filename}
                                </div>
                                <div className="text-[10px] text-slate-400">
                                  {msg.attachment.isEncrypted ? 'AES-CTR 加密媒體' : 'Plain'} ·{' '}
                                  {msg.attachment.sizeLabel}
                                </div>
                              </div>
                              {!msg.attachment.isDecrypted ? (
                                <button
                                  type="button"
                                  onClick={() => onDecryptMedia(msg.eventId)}
                                  className="inline-flex shrink-0 items-center gap-1 rounded-lg bg-indigo-600 px-2.5 py-1 text-[11px] font-medium text-white hover:bg-indigo-500"
                                >
                                  <Download className="h-3 w-3" />
                                  <span>解密檢視</span>
                                </button>
                              ) : (
                                <span className="text-[10px] font-medium text-emerald-600">
                                  已快取
                                </span>
                              )}
                            </div>
                          </div>
                        )}

                        {reactionMenuForId === msg.eventId && (
                          <div
                            className={`mt-2 inline-flex items-center gap-1 rounded-xl border p-1 ${
                              dark
                                ? 'border-slate-700 bg-slate-900'
                                : 'border-slate-200 bg-white'
                            }`}
                          >
                            {QUICK_REACTIONS.map((emoji) => (
                              <button
                                key={emoji}
                                type="button"
                                onClick={() => {
                                  onToggleReaction(msg.eventId, emoji);
                                  setReactionMenuForId(null);
                                }}
                                aria-label={`反應 ${emoji}`}
                                className={`flex h-8 w-8 items-center justify-center rounded-lg text-sm transition-colors ${
                                  dark ? 'hover:bg-slate-800' : 'hover:bg-slate-100'
                                }`}
                              >
                                {emoji}
                              </button>
                            ))}
                          </div>
                        )}

                        {(msg.reactions.length > 0 || msg.threadReplies.length > 0) && (
                          <div className="mt-2 flex flex-wrap items-center gap-1.5">
                            {msg.reactions.map((rx) => (
                              <button
                                key={rx.key}
                                type="button"
                                onClick={() => onToggleReaction(msg.eventId, rx.key)}
                                aria-label={`切換表情 ${rx.key} 目前 ${rx.count}`}
                                className={`inline-flex items-center gap-1 rounded-lg border px-2 py-0.5 text-xs tabular-nums transition-colors ${
                                  rx.reactedByMe
                                    ? 'border-indigo-500/50 bg-indigo-500/10 text-indigo-500 font-medium'
                                    : dark
                                      ? 'border-slate-700 bg-slate-800/80 text-slate-300'
                                      : 'border-slate-200 bg-slate-100 text-slate-700'
                                }`}
                              >
                                <span>{rx.key}</span>
                                <span>{rx.count}</span>
                              </button>
                            ))}
                            {msg.threadReplies.length > 0 && (
                              <button
                                type="button"
                                onClick={() => onOpenThread(msg.eventId)}
                                className="inline-flex items-center gap-1 text-xs font-medium text-indigo-500 hover:underline px-1.5 py-0.5 tabular-nums"
                              >
                                <MessageSquare className="h-3 w-3" />
                                <span>{msg.threadReplies.length} 則討論串回覆</span>
                              </button>
                            )}
                          </div>
                        )}
                      </div>
                    </div>
                  </article>
                ))}
              </div>

              {/* Composer */}
              <form
                onSubmit={handleSend}
                className={`border-t px-2.5 py-2.5 ${
                  dark
                    ? 'border-slate-800 bg-[#111827]'
                    : 'border-slate-200 bg-white'
                }`}
              >
                {replyingTo && (
                  <div className="mb-2 flex items-center justify-between rounded-lg bg-indigo-500/10 px-2.5 py-1 text-xs text-indigo-500">
                    <span className="truncate">
                      回覆 {replyingTo.senderName}：{replyingTo.body}
                    </span>
                    <button
                      type="button"
                      onClick={() => setReplyingTo(null)}
                      aria-label="取消回覆"
                      className="ml-2 p-0.5 hover:opacity-75"
                    >
                      <X className="h-3.5 w-3.5" />
                    </button>
                  </div>
                )}

                <div className="flex items-center gap-1.5">
                  <button
                    type="button"
                    onClick={() =>
                      onSendMediaAttachment('android-capture.png', 'image/png')
                    }
                    aria-label="傳送加密圖片附件"
                    className={`flex h-10 w-10 shrink-0 items-center justify-center rounded-xl transition-colors ${
                      dark
                        ? 'text-slate-300 hover:bg-slate-800'
                        : 'text-slate-600 hover:bg-slate-100'
                    }`}
                  >
                    <Paperclip className="h-4 w-4" />
                  </button>
                  <input
                    type="text"
                    value={composerText}
                    onChange={(e) => setComposerText(e.target.value)}
                    placeholder={`傳送訊息至 #${activeRoom.name}`}
                    aria-label="輸入聊天訊息"
                    className={`min-h-[40px] flex-1 rounded-xl border px-3 py-1.5 text-sm outline-none focus:ring-2 focus:ring-indigo-500 ${
                      dark
                        ? 'border-slate-700 bg-slate-900 text-slate-100 placeholder:text-slate-500'
                        : 'border-slate-200 bg-slate-50 text-slate-900 placeholder:text-slate-400'
                    }`}
                  />
                  <button
                    type="submit"
                    disabled={!composerText.trim()}
                    aria-label="傳送訊息"
                    className="flex h-[40px] w-[40px] shrink-0 items-center justify-center rounded-xl bg-indigo-600 text-white transition-colors hover:bg-indigo-500 disabled:opacity-40"
                  >
                    <Send className="h-4 w-4" />
                  </button>
                </div>
              </form>

              {/* Thread Sheet Overlay */}
              {activeThreadRoot && (
                <div
                  className={`absolute inset-0 z-20 flex flex-col ${
                    dark ? 'bg-[#0B0F17]' : 'bg-[#F8FAFC]'
                  }`}
                >
                  <div
                    className={`flex items-center justify-between border-b px-3 py-2.5 ${
                      dark
                        ? 'border-slate-800 bg-[#111827]'
                        : 'border-slate-200 bg-white'
                    }`}
                  >
                    <div className="flex items-center gap-2">
                      <button
                        type="button"
                        onClick={() => onOpenThread(null)}
                        aria-label="關閉討論串"
                        className="flex h-10 w-10 items-center justify-center rounded-xl hover:bg-slate-500/10"
                      >
                        <ArrowLeft className="h-5 w-5" />
                      </button>
                      <span className="text-sm font-semibold">討論串回覆</span>
                    </div>
                    <span className="text-xs text-slate-500 tabular-nums">
                      {activeThreadRoot.threadReplies.length} 則回覆
                    </span>
                  </div>

                  <div className="flex-1 space-y-3 overflow-y-auto p-4">
                    <div
                      className={`rounded-xl border p-3 ${
                        dark
                          ? 'border-slate-800 bg-[#111827]'
                          : 'border-slate-200 bg-white'
                      }`}
                    >
                      <div className="text-xs font-semibold text-indigo-500">
                        {activeThreadRoot.senderName} · {activeThreadRoot.timestampLabel}
                      </div>
                      <p className="mt-1 text-sm">{activeThreadRoot.body}</p>
                    </div>

                    {activeThreadRoot.threadReplies.map((reply) => (
                      <div key={reply.eventId} className="pl-3">
                        <div className="text-xs font-semibold">
                          {reply.senderName}{' '}
                          <span className="font-normal text-slate-400 tabular-nums">
                            {reply.timestampLabel}
                          </span>
                        </div>
                        <p
                          className={`mt-0.5 text-sm ${
                            dark ? 'text-slate-300' : 'text-slate-700'
                          }`}
                        >
                          {reply.body}
                        </p>
                      </div>
                    ))}
                  </div>

                  <form
                    onSubmit={handleThreadSubmit}
                    className={`flex items-center gap-2 border-t p-3 ${
                      dark
                        ? 'border-slate-800 bg-[#111827]'
                        : 'border-slate-200 bg-white'
                    }`}
                  >
                    <input
                      type="text"
                      value={threadDraft}
                      onChange={(e) => setThreadDraft(e.target.value)}
                      placeholder="回覆此討論串..."
                      aria-label="討論串輸入框"
                      className={`min-h-[42px] flex-1 rounded-xl border px-3 text-sm outline-none focus:ring-2 focus:ring-indigo-500 ${
                        dark
                          ? 'border-slate-700 bg-slate-900 text-slate-100'
                          : 'border-slate-200 bg-slate-50 text-slate-900'
                      }`}
                    />
                    <button
                      type="submit"
                      disabled={!threadDraft.trim()}
                      aria-label="傳送討論串回覆"
                      className="flex h-[42px] w-[42px] items-center justify-center rounded-xl bg-indigo-600 text-white disabled:opacity-40"
                    >
                      <Send className="h-4 w-4" />
                    </button>
                  </form>
                </div>
              )}
            </div>
          ) : (
            /* Android Screen 2: Cinny Space Rail + Room List Screen */
            <div className="flex flex-1 flex-col overflow-hidden">
              <div
                className={`flex items-center justify-between border-b px-3 py-2.5 ${
                  dark
                    ? 'border-slate-800 bg-[#111827]'
                    : 'border-slate-200 bg-white'
                }`}
              >
                <h2 className="truncate text-sm font-semibold">{activeSpaceName}</h2>
                <div className="flex items-center gap-0.5">
                  <button
                    type="button"
                    onClick={() => setShowSpaceDiscoverModal(true)}
                    aria-label="探索 Space 公開頻道"
                    className="flex h-9 w-9 items-center justify-center rounded-xl hover:bg-slate-500/10"
                  >
                    <Compass className="h-4 w-4" />
                  </button>
                  <button
                    type="button"
                    onClick={() => setShowCreateRoomModal(true)}
                    aria-label="建立或加入聊天室"
                    className="flex h-9 w-9 items-center justify-center rounded-xl hover:bg-slate-500/10"
                  >
                    <Plus className="h-4 w-4" />
                  </button>
                  <button
                    type="button"
                    onClick={onStartVerification}
                    aria-label="裝置安全驗證"
                    className="flex h-9 w-9 items-center justify-center rounded-xl hover:bg-slate-500/10"
                  >
                    <ShieldCheck className="h-4 w-4" />
                  </button>
                  <button
                    type="button"
                    onClick={onToggleTheme}
                    aria-label="切換主題"
                    className="flex h-9 w-9 items-center justify-center rounded-xl hover:bg-slate-500/10"
                  >
                    {dark ? <Sun className="h-4 w-4" /> : <Moon className="h-4 w-4" />}
                  </button>
                </div>
              </div>

              <div className="flex flex-1 overflow-hidden">
                {/* Left Cinny Space Rail */}
                <aside
                  aria-label="Matrix Spaces 導覽列"
                  className={`flex w-16 shrink-0 flex-col items-center gap-2.5 border-r py-3 ${
                    dark
                      ? 'border-slate-800 bg-[#111827]'
                      : 'border-slate-200 bg-slate-100/80'
                  }`}
                >
                  <button
                    type="button"
                    onClick={() => onSelectSpace('all')}
                    aria-label="顯示全部聊天室"
                    className={`flex h-11 w-11 items-center justify-center rounded-xl text-xs font-bold transition-colors ${
                      state.selectedSpaceFilter === 'all'
                        ? 'bg-indigo-600 text-white'
                        : dark
                          ? 'bg-slate-800 text-slate-300 hover:bg-slate-700'
                          : 'bg-white text-slate-700 hover:bg-slate-200'
                    }`}
                  >
                    All
                  </button>
                  <button
                    type="button"
                    onClick={() => onSelectSpace('dm')}
                    aria-label="顯示私訊 Direct Messages"
                    className={`flex h-11 w-11 items-center justify-center rounded-xl text-xs font-bold transition-colors ${
                      state.selectedSpaceFilter === 'dm'
                        ? 'bg-indigo-600 text-white'
                        : dark
                          ? 'bg-slate-800 text-slate-300 hover:bg-slate-700'
                          : 'bg-white text-slate-700 hover:bg-slate-200'
                    }`}
                  >
                    DM
                  </button>

                  <div
                    className={`my-1 h-px w-8 ${
                      dark ? 'bg-slate-800' : 'bg-slate-300'
                    }`}
                  />

                  {state.spaces.map((space) => {
                    const selected = state.selectedSpaceFilter === space.roomId;
                    const { unread } = getSpaceUnreadSummary(space, state.rooms);
                    return (
                      <button
                        key={space.roomId}
                        type="button"
                        onClick={() => onSelectSpace(space.roomId)}
                        aria-label={`切換 Space ${space.name}`}
                        className={`relative h-11 w-11 overflow-hidden rounded-xl border-2 transition-all ${
                          selected
                            ? 'border-indigo-500 rounded-xl'
                            : 'border-transparent rounded-2xl opacity-85 hover:opacity-100'
                        }`}
                      >
                        <SafeAvatar
                          src={space.avatarUrl}
                          alt={space.name}
                          fallback={space.shortCode}
                          dark={dark}
                        />
                        {unread > 0 && (
                          <span className="absolute -right-0.5 -bottom-0.5 flex h-4 min-w-[16px] items-center justify-center rounded-full bg-indigo-600 px-1 text-[10px] font-bold text-white tabular-nums">
                            {unread}
                          </span>
                        )}
                      </button>
                    );
                  })}
                </aside>

                {/* Room List Pane */}
                <div className="flex flex-1 flex-col overflow-hidden px-3 py-2.5">
                  <div className="relative mb-2.5">
                    <Search
                      className="pointer-events-none absolute top-1/2 left-3 h-4 w-4 -translate-y-1/2 text-slate-400"
                      aria-hidden="true"
                    />
                    <input
                      type="text"
                      value={roomQuery}
                      onChange={(e) => setRoomQuery(e.target.value)}
                      placeholder="搜尋房間或聯絡人..."
                      aria-label="搜尋聊天室"
                      className={`min-h-[40px] w-full rounded-xl border pr-3 pl-9 text-xs outline-none focus:ring-2 focus:ring-indigo-500 ${
                        dark
                          ? 'border-slate-800 bg-slate-900 text-slate-100 placeholder:text-slate-500'
                          : 'border-slate-200 bg-white text-slate-900 placeholder:text-slate-400'
                      }`}
                    />
                  </div>

                  <div className="flex-1 space-y-1 overflow-y-auto">
                    {filteredRooms.length === 0 ? (
                      <div className="py-10 text-center text-xs text-slate-400">
                        找不到符合條件的聊天室
                      </div>
                    ) : (
                      filteredRooms.map((room) => (
                        <button
                          key={room.roomId}
                          type="button"
                          onClick={() => onSelectRoom(room.roomId)}
                          className={`flex w-full items-center gap-2.5 rounded-xl px-3 py-2.5 text-left transition-colors ${
                            dark ? 'hover:bg-slate-800/80' : 'hover:bg-white'
                          }`}
                        >
                          {room.isEncrypted ? (
                            <Lock
                              className="h-4 w-4 shrink-0 text-emerald-600"
                              aria-hidden="true"
                            />
                          ) : (
                            <Hash
                              className="h-4 w-4 shrink-0 text-slate-400"
                              aria-hidden="true"
                            />
                          )}
                          <div className="min-w-0 flex-1">
                            <div className="truncate text-sm font-medium">{room.name}</div>
                            <div
                              className={`truncate text-[11px] ${
                                dark ? 'text-slate-400' : 'text-slate-500'
                              }`}
                            >
                              {room.topic}
                            </div>
                          </div>
                          {room.unreadCount > 0 && (
                            <span className="text-xs font-bold text-indigo-500 tabular-nums">
                              {room.unreadCount}
                            </span>
                          )}
                        </button>
                      ))
                    )}
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* Create Room Modal inside Phone */}
          {showCreateRoomModal && (
            <div
              role="dialog"
              aria-modal="true"
              aria-label="建立 Matrix 聊天室"
              className="absolute inset-0 z-50 flex items-center justify-center bg-slate-950/60 p-4 backdrop-blur-xs"
            >
              <form
                onSubmit={handleCreateSubmit}
                className={`w-full space-y-3 rounded-2xl border p-4 shadow-xl ${
                  dark
                    ? 'border-slate-700 bg-[#111827] text-slate-100'
                    : 'border-slate-200 bg-white text-slate-900'
                }`}
              >
                <h3 className="text-sm font-semibold">建立新 Matrix 聊天室</h3>
                <input
                  type="text"
                  value={newRoomName}
                  onChange={(e) => setNewRoomName(e.target.value)}
                  placeholder="頻道名稱 (例：rust-async)"
                  className={`w-full rounded-xl border px-3 py-2 text-xs outline-none ${
                    dark ? 'border-slate-700 bg-slate-900' : 'border-slate-200 bg-slate-50'
                  }`}
                />
                <input
                  type="text"
                  value={newRoomTopic}
                  onChange={(e) => setNewRoomTopic(e.target.value)}
                  placeholder="主題說明 (選填)"
                  className={`w-full rounded-xl border px-3 py-2 text-xs outline-none ${
                    dark ? 'border-slate-700 bg-slate-900' : 'border-slate-200 bg-slate-50'
                  }`}
                />
                <label className="flex items-center justify-between text-xs">
                  <span>啟用 Megolm 端對端加密</span>
                  <input
                    type="checkbox"
                    checked={newRoomEncrypted}
                    onChange={(e) => setNewRoomEncrypted(e.target.checked)}
                  />
                </label>
                <div className="flex justify-end gap-2 pt-1">
                  <button
                    type="button"
                    onClick={() => setShowCreateRoomModal(false)}
                    className="rounded-lg px-3 py-1.5 text-xs text-slate-500"
                  >
                    取消
                  </button>
                  <button
                    type="submit"
                    disabled={!newRoomName.trim()}
                    className="rounded-lg bg-indigo-600 px-3.5 py-1.5 text-xs font-semibold text-white disabled:opacity-40"
                  >
                    建立並進入
                  </button>
                </div>
              </form>
            </div>
          )}

          {/* Space Hierarchy Discovery Modal */}
          {showSpaceDiscoverModal && (
            <div
              role="dialog"
              aria-modal="true"
              aria-label="探索 Space 未加入聊天室"
              className="absolute inset-0 z-50 flex items-center justify-center bg-slate-950/60 p-4 backdrop-blur-xs"
            >
              <div
                className={`w-full space-y-3 rounded-2xl border p-4 shadow-xl ${
                  dark
                    ? 'border-slate-700 bg-[#111827] text-slate-100'
                    : 'border-slate-200 bg-white text-slate-900'
                }`}
              >
                <h3 className="text-sm font-semibold">探索 Space 內的頻道</h3>
                <div className="space-y-2">
                  {state.discoverableRooms.map((item) => (
                    <div
                      key={item.roomId}
                      className={`flex items-center justify-between gap-2 rounded-xl border p-2.5 ${
                        dark ? 'border-slate-800 bg-slate-900' : 'border-slate-200 bg-slate-50'
                      }`}
                    >
                      <div className="min-w-0 flex-1">
                        <div className="truncate text-xs font-semibold">#{item.name}</div>
                        <div className="truncate text-[10px] text-slate-400">
                          {item.memberCount} 位成員 · {item.topic}
                        </div>
                      </div>
                      {item.joined ? (
                        <span className="text-[11px] text-emerald-600 font-medium">已加入</span>
                      ) : (
                        <button
                          type="button"
                          onClick={() => onJoinDiscoverableRoom(item.roomId)}
                          className="rounded-lg bg-indigo-600 px-2.5 py-1 text-[11px] font-semibold text-white"
                        >
                          加入
                        </button>
                      )}
                    </div>
                  ))}
                </div>
                <div className="flex justify-end">
                  <button
                    type="button"
                    onClick={() => setShowSpaceDiscoverModal(false)}
                    className="rounded-lg px-3 py-1.5 text-xs font-medium text-indigo-500"
                  >
                    完成
                  </button>
                </div>
              </div>
            </div>
          )}

          {/* E2EE Security Modal inside Phone: SAS Emoji + 4S Recovery Key */}
          {state.verification.kind !== 'idle' && (
            <div
              role="dialog"
              aria-modal="true"
              aria-label="E2EE 安全驗證與金鑰備份中心"
              className="absolute inset-0 z-50 flex items-center justify-center bg-slate-950/60 p-4 backdrop-blur-xs"
            >
              <div
                className={`w-full rounded-2xl border p-4 shadow-xl ${
                  dark
                    ? 'border-slate-700 bg-[#111827] text-slate-100'
                    : 'border-slate-200 bg-white text-slate-900'
                }`}
              >
                <h3 className="text-sm font-semibold">E2EE 安全驗證與 4S 金鑰備份</h3>
                <div className="mt-2.5 grid grid-cols-2 gap-1 rounded-lg bg-slate-500/10 p-1 text-xs">
                  <button
                    type="button"
                    onClick={() => setSecurityTab('sas')}
                    className={`rounded-md py-1.5 font-medium ${
                      securityTab === 'sas' ? 'bg-indigo-600 text-white' : 'text-slate-500'
                    }`}
                  >
                    SAS 裝置驗證
                  </button>
                  <button
                    type="button"
                    onClick={() => setSecurityTab('recovery')}
                    className={`rounded-md py-1.5 font-medium ${
                      securityTab === 'recovery' ? 'bg-indigo-600 text-white' : 'text-slate-500'
                    }`}
                  >
                    4S 金鑰還原
                  </button>
                </div>

                {securityTab === 'recovery' ? (
                  <div className="mt-3 space-y-2.5">
                    <p
                      className={`text-xs ${
                        dark ? 'text-slate-400' : 'text-slate-600'
                      }`}
                    >
                      輸入 4S Recovery Key 可還原 Cross-Signing 並解密過去封存的 Megolm 房間金鑰。
                    </p>
                    <input
                      type="text"
                      value={recoveryKeyInput}
                      onChange={(e) => setRecoveryKeyInput(e.target.value)}
                      aria-label="4S Recovery Key 輸入框"
                      className={`w-full rounded-xl border px-3 py-2 font-mono text-xs outline-none ${
                        dark ? 'border-slate-700 bg-slate-900' : 'border-slate-200 bg-slate-50'
                      }`}
                    />
                    {state.recoveryUnlocked && (
                      <p className="text-xs font-medium text-emerald-600">
                        4S 金鑰備份已解鎖，時間軸歷史訊息已解密。
                      </p>
                    )}
                    <div className="flex justify-end gap-2 pt-1">
                      <button
                        type="button"
                        onClick={onCloseVerification}
                        className="rounded-lg px-3 py-1.5 text-xs text-slate-500"
                      >
                        關閉
                      </button>
                      <button
                        type="button"
                        onClick={() => onRecoverSecretBackup(recoveryKeyInput)}
                        className="inline-flex items-center gap-1 rounded-lg bg-indigo-600 px-3 py-1.5 text-xs font-semibold text-white"
                      >
                        <KeyRound className="h-3.5 w-3.5" />
                        <span>解鎖 4S 備份</span>
                      </button>
                    </div>
                  </div>
                ) : state.verification.kind === 'sas' ? (
                  <>
                    <p
                      className={`mt-2.5 text-xs ${
                        dark ? 'text-slate-400' : 'text-slate-600'
                      }`}
                    >
                      請確認對方裝置顯示的 7 組表情符號與以下完全相符：
                    </p>
                    <div className="mt-3 grid grid-cols-4 gap-2 text-center">
                      {state.verification.emojis.map((item) => (
                        <div
                          key={item.label}
                          className={`rounded-xl p-2 ${
                            dark ? 'bg-slate-800' : 'bg-slate-100'
                          }`}
                        >
                          <div className="text-lg">{item.symbol}</div>
                          <div className="mt-0.5 text-[10px] font-medium">{item.label}</div>
                        </div>
                      ))}
                    </div>
                    <div className="mt-4 flex justify-end gap-2">
                      <button
                        type="button"
                        onClick={onCloseVerification}
                        className="min-h-[38px] rounded-xl px-3 py-1.5 text-xs font-medium text-slate-500 hover:bg-slate-500/10"
                      >
                        取消
                      </button>
                      <button
                        type="button"
                        onClick={onConfirmVerification}
                        className="min-h-[38px] rounded-xl bg-indigo-600 px-4 py-1.5 text-xs font-semibold text-white hover:bg-indigo-500"
                      >
                        表情完全相符
                      </button>
                    </div>
                  </>
                ) : (
                  <>
                    <p className="mt-3 text-xs text-emerald-600 font-medium">
                      裝置 {state.verification.deviceId} 已透過 Rust matrix-sdk-crypto 完成交叉簽章驗證。
                    </p>
                    <div className="mt-4 flex justify-end">
                      <button
                        type="button"
                        onClick={onCloseVerification}
                        className="min-h-[38px] rounded-xl bg-indigo-600 px-4 py-1.5 text-xs font-semibold text-white hover:bg-indigo-500"
                      >
                        完成
                      </button>
                    </div>
                  </>
                )}
              </div>
            </div>
          )}
        </div>

        {/* Android Gesture Home Bar */}
        <div
          className={`flex justify-center py-2.5 ${
            dark ? 'bg-[#111827]' : 'bg-white'
          }`}
        >
          <span
            aria-hidden="true"
            className={`h-1 w-28 rounded-full ${
              dark ? 'bg-slate-700' : 'bg-slate-300'
            }`}
          />
        </div>
      </div>
    </div>
  );
}
