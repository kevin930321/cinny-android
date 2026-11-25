import { useRef, useState, FormEvent } from 'react';
import {
  ArrowDown,
  ArrowLeft,
  Bell,
  Code2,
  Compass,
  CornerUpLeft,
  Download,
  ExternalLink,
  Hash,
  Inbox,
  Info,
  KeyRound,
  Lock,
  MessageSquare,
  Paperclip,
  Pencil,
  Plus,
  Search,
  Send,
  Settings,
  Settings2,
  ShieldCheck,
  SmilePlus,
  Trash2,
  Users,
  X,
} from 'lucide-react';
import {
  CinnyAppState,
  CinnySettingsState,
  RoomNotificationLevel,
  RoomSummary,
  TimelineMessage,
  filterRooms,
  getSpaceUnreadSummary,
  parseInlineMarkdownTokens,
} from '../lib/cinnyEngine';
import {
  PhoneCreateHubModal,
  PhoneEventSourceModal,
  PhoneExploreOverlay,
  PhoneInboxOverlay,
  PhoneMembersOverlay,
  PhoneRoomSettingsOverlay,
  PhoneSettingsOverlay,
  PhoneTopicModal,
} from './PhoneOverlays';
import { getMxidTextClass, getPhoneThemeClasses, PhoneThemeClasses } from './phoneThemeClasses';

interface AndroidPhoneSimulatorProps {
  state: CinnyAppState;
  onLogin: (homeserver: string, username: string) => void;
  onSsoLogin: (homeserver: string) => void;
  onLogout: () => void;
  onSelectSpace: (spaceFilter: 'all' | 'dm' | string) => void;
  onSelectRoom: (roomId: string) => void;
  onCreateRoom: (name: string, topic: string, encrypted: boolean) => void;
  onCreateSpace: (name: string, topic: string) => void;
  onJoinDiscoverableRoom: (roomId: string) => void;
  onAcceptInvite: (roomId: string) => void;
  onBackToSpaces: () => void;
  onUpdateSettings: (patch: Partial<CinnySettingsState>) => void;
  onUpdateDisplayName: (name: string) => void;
  onUpdateRoomSettings: (
    roomId: string,
    patch: { name?: string; topic?: string; notificationLevel?: RoomNotificationLevel }
  ) => void;
  onInviteUser: (roomId: string, userId: string) => void;
  onSendMessage: (markdown: string, replyToId?: string | null) => void;
  onEditMessage: (eventId: string, body: string) => void;
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
const PRESET_HOMESERVERS = ['https://matrix.org', 'https://mozilla.org', 'https://kde.org'];

function FormattedMessageBody({ body, theme }: { body: string; theme: PhoneThemeClasses }) {
  const tokens = parseInlineMarkdownTokens(body);
  return (
    <>
      {tokens.map((tok, i) => {
        if (tok.kind === 'bold') {
          return (
            <strong key={i} className="font-semibold">
              {tok.text}
            </strong>
          );
        }
        if (tok.kind === 'italic') {
          return (
            <em key={i} className="italic">
              {tok.text}
            </em>
          );
        }
        if (tok.kind === 'code') {
          return (
            <code
              key={i}
              className={`rounded px-1 py-0.5 font-mono text-[11px] ${theme.surfaceVar}`}
            >
              {tok.text}
            </code>
          );
        }
        return <span key={i}>{tok.text}</span>;
      })}
    </>
  );
}

function SafeAvatar({
  src,
  alt,
  fallback,
  theme,
}: {
  src?: string;
  alt: string;
  fallback: string;
  theme: PhoneThemeClasses;
}) {
  const [failed, setFailed] = useState(false);

  if (!src) {
    return (
      <span
        className={`flex h-full w-full items-center justify-center text-xs font-semibold ${theme.surfaceVar} ${theme.text}`}
      >
        {fallback}
      </span>
    );
  }

  if (failed) {
    return (
      <span className={`flex h-full w-full items-center justify-center p-1.5 ${theme.surfaceVar}`}>
        <img
          src="/res/svg/image-broken.svg"
          alt={alt}
          className="h-4 w-4 opacity-70"
        />
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
  onLogout,
  onSelectSpace,
  onSelectRoom,
  onCreateRoom,
  onCreateSpace,
  onJoinDiscoverableRoom,
  onAcceptInvite,
  onBackToSpaces,
  onUpdateSettings,
  onUpdateDisplayName,
  onUpdateRoomSettings,
  onInviteUser,
  onSendMessage,
  onEditMessage,
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
  const timelineRef = useRef<HTMLDivElement | null>(null);
  const [showJumpToLatest, setShowJumpToLatest] = useState(false);
  const [roomQuery, setRoomQuery] = useState('');
  const [composerText, setComposerText] = useState('');
  const [replyingTo, setReplyingTo] = useState<TimelineMessage | null>(null);
  const [editingMsg, setEditingMsg] = useState<TimelineMessage | null>(null);
  const [reactionMenuForId, setReactionMenuForId] = useState<string | null>(null);
  const [sourceModalMsg, setSourceModalMsg] = useState<TimelineMessage | null>(null);
  const [showTopicModal, setShowTopicModal] = useState(false);
  const [threadDraft, setThreadDraft] = useState('');

  const [loginHomeserver, setLoginHomeserver] = useState(state.homeserverUrl);
  const [loginUser, setLoginUser] = useState('@you:matrix.org');
  const [loginPassword, setLoginPassword] = useState('demo-matrix-pass');
  const [loginError, setLoginError] = useState('');

  const [showMsgSearch, setShowMsgSearch] = useState(false);
  const [msgSearchQuery, setMsgSearchQuery] = useState('');
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [activeOverlay, setActiveOverlay] = useState<
    'none' | 'settings' | 'roomSettings' | 'members' | 'explore' | 'inbox'
  >('none');

  const [securityTab, setSecurityTab] = useState<'sas' | 'recovery'>('sas');
  const [recoveryKeyInput, setRecoveryKeyInput] = useState('EsTX-984C-Rust-Backup');

  const theme = getPhoneThemeClasses(state.settings.themeMode);
  const activeRoom: RoomSummary | undefined = state.rooms.find(
    (r) => r.roomId === state.activeRoomId
  );
  const activeRoomMembers = state.activeRoomId
    ? state.membersByRoom[state.activeRoomId] ?? []
    : [];
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

  const hasUtdMessages = allMessages.some((m) => m.isUnableToDecrypt);

  const filteredRooms = filterRooms(
    state.rooms,
    state.selectedSpaceFilter,
    roomQuery,
    state.settings.sortRoomsByActivity
  );
  const invitedRooms = state.rooms.filter((r) => r.category === 'Invited');
  const joinedFilteredRooms = filteredRooms.filter((r) => r.category !== 'Invited');
  const inboxBadgeCount =
    invitedRooms.length +
    state.rooms.filter((r) => r.category !== 'Invited' && r.highlightCount > 0).length;

  const selectedSpaceNode =
    state.selectedSpaceFilter !== 'all' && state.selectedSpaceFilter !== 'dm'
      ? state.spaces.find((s) => s.roomId === state.selectedSpaceFilter)
      : undefined;

  const activeSpaceName =
    state.selectedSpaceFilter === 'all'
      ? '首頁 Home'
      : state.selectedSpaceFilter === 'dm'
        ? '私訊 People'
        : selectedSpaceNode?.name ?? 'Spaces';

  const handleSend = (e: FormEvent) => {
    e.preventDefault();
    if (!composerText.trim()) return;
    if (editingMsg) {
      onEditMessage(editingMsg.eventId, composerText);
      setEditingMsg(null);
      setComposerText('');
      return;
    }
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

  const formatMsgClock = (label: string) => {
    if (state.settings.use24HourClock) return label;
    const [hhRaw, mm = '00'] = label.split(':');
    const hh = Number(hhRaw);
    if (Number.isNaN(hh)) return label;
    const suffix = hh >= 12 ? 'PM' : 'AM';
    const h12 = hh % 12 === 0 ? 12 : hh % 12;
    return `${h12}:${mm} ${suffix}`;
  };

  return (
    <div className="flex flex-col items-center">
      {/* Android Device Frame */}
      <div
        data-testid="android-device-frame"
        data-theme-mode={state.settings.themeMode}
        data-message-layout={state.settings.messageLayout}
        className={`relative w-full max-w-[390px] overflow-hidden rounded-[34px] border-[6px] shadow-xl transition-colors ${theme.frameBorder} ${theme.bg} ${theme.text}`}
      >
        {/* Android Status Bar */}
        <div
          className={`flex items-center justify-between px-6 pt-3 pb-2 text-[11px] font-medium tabular-nums ${theme.surface} ${theme.muted}`}
        >
          <span>19:47</span>
          <span aria-hidden="true" className="h-3.5 w-16 rounded-full bg-black/85" />
          <span>{state.settings.themeMode} · {state.recoveryUnlocked ? '4S' : 'E2EE'}</span>
        </div>

        {/* Viewport Body */}
        <div className="relative flex h-[560px] flex-col overflow-hidden">
          {/* UnifiedPush Android Heads-Up Notification Banner */}
          {state.activePushBanner && (
            <div
              role="region"
              aria-label="UnifiedPush 推播通知"
              className={`absolute top-2 right-2.5 left-2.5 z-30 flex items-center justify-between gap-2 rounded-2xl border p-3 shadow-lg ${theme.line} ${theme.surface} ${theme.text}`}
            >
              <button
                type="button"
                onClick={() => onSelectRoom(state.activePushBanner!.roomId)}
                className="flex min-w-0 flex-1 items-start gap-2.5 text-left"
              >
                <Bell className={`mt-0.5 h-4 w-4 shrink-0 ${theme.primaryText}`} aria-hidden="true" />
                <div className="min-w-0 flex-1">
                  <div className="truncate text-xs font-semibold">
                    {state.activePushBanner.roomName} · E2EE 已解密
                  </div>
                  <div className={`truncate text-[11px] ${theme.muted}`}>
                    {state.activePushBanner.senderName}: {state.activePushBanner.bodyPreview}
                  </div>
                </div>
              </button>
              <button
                type="button"
                onClick={onDismissPushBanner}
                aria-label="關閉推播通知"
                className={`p-1 ${theme.muted}`}
              >
                <X className="h-3.5 w-3.5" />
              </button>
            </div>
          )}

          {!state.isAuthenticated ? (
            /* Android Screen 1: Cinny Sequence Card Login Screen with dot-grid pattern & official AuthHeader */
            <div
              className={`flex flex-1 flex-col justify-center px-4 py-3 ${
                theme.isDark ? 'cinny-dot-bg-dark' : 'cinny-dot-bg-light'
              }`}
            >
              <form
                onSubmit={handleLoginSubmit}
                className={`overflow-hidden rounded-2xl border shadow-sm ${theme.line} ${theme.surface}`}
              >
                <div className={`flex items-center gap-3 border-b px-4 py-3.5 ${theme.line} ${theme.surfaceVar}`}>
                  <img
                    src="/res/svg/cinny.svg"
                    alt="Cinny Logo"
                    className="h-11 w-11 shrink-0 rounded-full border border-current/15 object-cover"
                  />
                  <div className="min-w-0">
                    <h2 className="text-lg font-bold tracking-tight leading-tight">Cinny</h2>
                    <p className={`mt-0.5 text-[11px] leading-snug ${theme.muted}`}>
                      Rust E2EE Matrix Client · 支援密碼與 SSO
                    </p>
                  </div>
                </div>
                <div className="p-4">

                <div className="mb-3 flex flex-wrap gap-1.5">
                  {PRESET_HOMESERVERS.map((hs) => (
                    <button
                      key={hs}
                      type="button"
                      onClick={() => setLoginHomeserver(hs)}
                      className={`rounded-lg border px-2 py-1 text-[10px] font-medium transition-colors ${
                        loginHomeserver === hs
                          ? `${theme.primaryContainer} ${theme.onPrimaryContainer} border-transparent`
                          : `${theme.line} ${theme.muted}`
                      }`}
                    >
                      {hs.replace('https://', '')}
                    </button>
                  ))}
                </div>

                <div className="space-y-2.5">
                  <div>
                    <label htmlFor="android-homeserver" className={`block text-[11px] font-medium mb-1 ${theme.muted}`}>
                      Homeserver URL
                    </label>
                    <input
                      id="android-homeserver"
                      type="url"
                      value={loginHomeserver}
                      onChange={(e) => setLoginHomeserver(e.target.value)}
                      className={`w-full rounded-xl border px-3 py-2 text-xs outline-none ${theme.line} ${theme.surfaceVar}`}
                    />
                  </div>
                  <div>
                    <label htmlFor="android-username" className={`block text-[11px] font-medium mb-1 ${theme.muted}`}>
                      Matrix ID
                    </label>
                    <input
                      id="android-username"
                      type="text"
                      value={loginUser}
                      onChange={(e) => setLoginUser(e.target.value)}
                      className={`w-full rounded-xl border px-3 py-2 text-xs outline-none ${theme.line} ${theme.surfaceVar}`}
                    />
                  </div>
                  <div>
                    <label htmlFor="android-password" className={`block text-[11px] font-medium mb-1 ${theme.muted}`}>
                      Password
                    </label>
                    <input
                      id="android-password"
                      type="password"
                      value={loginPassword}
                      onChange={(e) => setLoginPassword(e.target.value)}
                      className={`w-full rounded-xl border px-3 py-2 text-xs outline-none ${theme.line} ${theme.surfaceVar}`}
                    />
                  </div>
                </div>

                {loginError && <p className={`mt-2 text-xs ${theme.critical}`}>{loginError}</p>}

                <button
                  type="submit"
                  className={`mt-4 min-h-[40px] w-full rounded-xl px-4 py-2 text-xs font-semibold ${theme.primaryBg} ${theme.onPrimary}`}
                >
                  以密碼登入 Matrix
                </button>
                <button
                  type="button"
                  onClick={() => onSsoLogin(loginHomeserver.trim() || 'https://matrix.org')}
                  className={`mt-2 inline-flex min-h-[38px] w-full items-center justify-center gap-2 rounded-xl border px-4 py-2 text-xs font-semibold ${theme.line} ${theme.hoverSurface}`}
                >
                  <ExternalLink className="h-3.5 w-3.5" aria-hidden="true" />
                  <span>透過 SSO / OIDC 瀏覽器登入</span>
                </button>
                </div>
              </form>
            </div>
          ) : activeRoom ? (
            /* Android Screen 3: Cinny Room Timeline Screen */
            <div className="flex flex-1 flex-col overflow-hidden">
              {/* Clickable Cinny Room Header */}
              <div className={`flex items-center justify-between border-b px-2.5 py-2 ${theme.line} ${theme.surface}`}>
                <div className="flex min-w-0 flex-1 items-center gap-1">
                  <button
                    type="button"
                    onClick={onBackToSpaces}
                    aria-label="返回 Space 與聊天室列表"
                    className={`flex h-9 w-9 shrink-0 items-center justify-center rounded-xl ${theme.hoverSurface}`}
                  >
                    <ArrowLeft className="h-4 w-4" />
                  </button>
                  <button
                    type="button"
                    onClick={() => setActiveOverlay('roomSettings')}
                    aria-label="開啟聊天室設定"
                    className={`min-w-0 flex-1 rounded-xl px-1.5 py-1 text-left ${theme.hoverSurface}`}
                  >
                    <div className="flex items-center gap-1.5">
                      {activeRoom.isEncrypted ? (
                        <Lock className={`h-3.5 w-3.5 shrink-0 ${theme.success}`} aria-hidden="true" />
                      ) : (
                        <Hash className={`h-3.5 w-3.5 shrink-0 ${theme.muted}`} aria-hidden="true" />
                      )}
                      <span className="truncate text-sm font-semibold">{activeRoom.name}</span>
                    </div>
                    <p className={`truncate text-[10px] ${theme.muted}`}>{activeRoom.topic}</p>
                  </button>
                </div>

                <div className="flex shrink-0 items-center gap-0.5">
                  <button
                    type="button"
                    onClick={() => setShowTopicModal(true)}
                    aria-label="檢視聊天室主題"
                    className={`flex h-8 w-8 items-center justify-center rounded-xl ${theme.muted} ${theme.hoverSurface}`}
                  >
                    <Info className="h-4 w-4" />
                  </button>
                  <button
                    type="button"
                    onClick={() => {
                      setShowMsgSearch((v) => !v);
                      setMsgSearchQuery('');
                    }}
                    aria-label="搜尋房間歷史訊息"
                    className={`flex h-8 w-8 items-center justify-center rounded-xl ${
                      showMsgSearch ? theme.primaryText : `${theme.muted} ${theme.hoverSurface}`
                    }`}
                  >
                    <Search className="h-4 w-4" />
                  </button>
                  <button
                    type="button"
                    onClick={() => setActiveOverlay('members')}
                    aria-label="開啟聊天室成員列表"
                    className={`flex h-8 w-8 items-center justify-center rounded-xl ${theme.muted} ${theme.hoverSurface}`}
                  >
                    <Users className="h-4 w-4" />
                  </button>
                  <button
                    type="button"
                    onClick={() => {
                      setSecurityTab('sas');
                      onStartVerification();
                    }}
                    aria-label="開啟裝置 E2EE 安全中心"
                    className={`flex h-8 w-8 items-center justify-center rounded-xl ${
                      state.verification.kind === 'confirmed' || state.recoveryUnlocked
                        ? theme.success
                        : `${theme.muted} ${theme.hoverSurface}`
                    }`}
                  >
                    <ShieldCheck className="h-4 w-4" />
                  </button>
                  <button
                    type="button"
                    onClick={() => setActiveOverlay('roomSettings')}
                    aria-label="房間詳細設定"
                    className={`flex h-8 w-8 items-center justify-center rounded-xl ${theme.muted} ${theme.hoverSurface}`}
                  >
                    <Settings2 className="h-4 w-4" />
                  </button>
                </div>
              </div>

              {showMsgSearch && (
                <div className={`border-b px-3 py-2 ${theme.line} ${theme.surface}`}>
                  <input
                    type="text"
                    value={msgSearchQuery}
                    onChange={(e) => setMsgSearchQuery(e.target.value)}
                    placeholder="搜尋本地已解密訊息..."
                    aria-label="搜尋房間訊息"
                    className={`min-h-[34px] w-full rounded-xl border px-2.5 text-xs outline-none ${theme.line} ${theme.surfaceVar}`}
                  />
                </div>
              )}

              {hasUtdMessages && (
                <button
                  type="button"
                  onClick={() => {
                    setSecurityTab('recovery');
                    onStartVerification();
                  }}
                  className={`flex items-center justify-between border-b px-3 py-1.5 text-left text-[11px] ${theme.line} ${theme.surfaceVar} ${theme.warning}`}
                >
                  <span className="truncate font-medium">部分歷史訊息等待金鑰 · 點擊使用 4S Key 解鎖</span>
                  <KeyRound className="h-3.5 w-3.5 shrink-0" />
                </button>
              )}

              {/* Message Timeline: Supports Modern / Compact / Bubble */}
              <div className="relative flex-1 overflow-hidden">
                <div
                  ref={timelineRef}
                  onScroll={(e) => {
                    const el = e.currentTarget;
                    const distanceFromBottom = el.scrollHeight - el.scrollTop - el.clientHeight;
                    setShowJumpToLatest(distanceFromBottom > 28);
                  }}
                  data-testid="android-timeline-list"
                  className={`h-full overflow-y-auto px-3 py-2.5 ${
                    state.settings.messageLayout === 'Compact' ? 'space-y-1.5' : 'space-y-3'
                  }`}
                >
                  {activeMessages.map((msg) => {
                    const mxidClass = getMxidTextClass(msg.senderId, theme.isDark);
                    const isBubbleRight =
                      state.settings.messageLayout === 'Bubble' &&
                      state.settings.bubbleOwnRight &&
                      msg.isOwn;

                    if (state.settings.messageLayout === 'Compact') {
                      return (
                        <article
                          key={msg.eventId}
                          className={`rounded-lg px-2 py-1 text-xs ${theme.hoverSurface}`}
                        >
                          <div className="flex items-start justify-between gap-2">
                            <div className="min-w-0 flex-1 leading-relaxed break-words">
                              <span className={`mr-1.5 font-mono text-[10px] ${theme.muted} tabular-nums`}>
                                [{formatMsgClock(msg.timestampLabel)}]
                              </span>
                              <span className={`mr-1.5 font-semibold ${mxidClass}`}>
                                &lt;{msg.senderName}&gt;
                              </span>
                              <span className={msg.isRedacted || msg.isUnableToDecrypt ? `italic ${theme.warning}` : ''}>
                                {msg.isRedacted || msg.isUnableToDecrypt ? (
                                  msg.body
                                ) : (
                                  <FormattedMessageBody body={msg.body} theme={theme} />
                                )}
                              </span>
                            {msg.isEdited && !msg.isRedacted && (
                              <span className={`ml-1 text-[10px] ${theme.muted}`}>(已編輯)</span>
                            )}
                          </div>
                          {!msg.isRedacted && (
                            <div className="flex shrink-0 items-center gap-0.5">
                              {state.settings.developerTools && (
                                <button
                                  type="button"
                                  onClick={() => setSourceModalMsg(msg)}
                                  aria-label={`檢視 ${msg.senderName} 事件 JSON`}
                                  className={`p-1 ${theme.muted}`}
                                >
                                  <Code2 className="h-3 w-3" />
                                </button>
                              )}
                              {msg.isOwn && (
                                <button
                                  type="button"
                                  onClick={() => {
                                    setEditingMsg(msg);
                                    setComposerText(msg.body);
                                  }}
                                  aria-label="編輯訊息"
                                  className={`p-1 ${theme.muted}`}
                                >
                                  <Pencil className="h-3 w-3" />
                                </button>
                              )}
                            </div>
                          )}
                        </div>
                        {msg.attachment && !msg.isRedacted && (
                          <div className={`mt-1 ml-4 flex items-center justify-between gap-2 rounded-lg border px-2 py-1 text-[11px] ${theme.line} ${theme.surfaceVar}`}>
                            <span className="truncate">{msg.attachment.filename}</span>
                            {!msg.attachment.isDecrypted ? (
                              <button
                                type="button"
                                onClick={() => onDecryptMedia(msg.eventId)}
                                className={`shrink-0 font-semibold ${theme.primaryText}`}
                              >
                                解密
                              </button>
                            ) : (
                              <span className={theme.success}>已快取</span>
                            )}
                          </div>
                        )}
                        {msg.reactions.length > 0 && (
                          <div className="mt-1 ml-4 flex flex-wrap gap-1">
                            {msg.reactions.map((rx) => (
                              <button
                                key={rx.key}
                                type="button"
                                onClick={() => onToggleReaction(msg.eventId, rx.key)}
                                className={`rounded border px-1.5 py-0.5 text-[10px] tabular-nums ${
                                  rx.reactedByMe
                                    ? `${theme.primaryContainer} ${theme.onPrimaryContainer} border-transparent`
                                    : `${theme.line} ${theme.surfaceVar}`
                                }`}
                              >
                                {rx.key} {rx.count}
                              </button>
                            ))}
                          </div>
                        )}
                      </article>
                    );
                  }

                  return (
                    <article
                      key={msg.eventId}
                      className={`group rounded-2xl p-2 transition-colors ${
                        state.settings.messageLayout === 'Bubble'
                          ? isBubbleRight
                            ? `ml-8 border ${theme.line} ${theme.primaryContainer} ${theme.onPrimaryContainer}`
                            : `mr-4 border ${theme.line} ${theme.surface}`
                          : theme.hoverSurface
                      }`}
                    >
                      {msg.replyTo && (
                        <div
                          className={`mb-1.5 flex items-center gap-1.5 rounded-lg px-2 py-1 text-[11px] ${theme.surfaceVar} ${theme.muted}`}
                        >
                          <CornerUpLeft className="h-3 w-3 shrink-0" />
                          <span className="font-medium">{msg.replyTo.senderName}:</span>
                          <span className="truncate">{msg.replyTo.body}</span>
                        </div>
                      )}

                      <div className="flex items-start gap-2.5">
                        {!isBubbleRight && (
                          <div className="h-8 w-8 shrink-0 overflow-hidden rounded-full">
                            <SafeAvatar
                              src={msg.senderAvatarUrl}
                              alt={msg.senderName}
                              fallback={msg.senderName.slice(0, 2).toUpperCase()}
                              theme={theme}
                            />
                          </div>
                        )}

                        <div className="min-w-0 flex-1">
                          <div className="flex items-baseline justify-between gap-2">
                            <div className="flex items-baseline gap-1.5 min-w-0">
                              <span className={`truncate text-xs font-semibold ${isBubbleRight ? theme.onPrimaryContainer : mxidClass}`}>
                                {msg.senderName}
                              </span>
                              <span className={`text-[10px] tabular-nums ${isBubbleRight ? 'opacity-75' : theme.muted}`}>
                                {formatMsgClock(msg.timestampLabel)}
                              </span>
                              {msg.isEdited && !msg.isRedacted && (
                                <span className={`text-[10px] ${isBubbleRight ? 'opacity-75' : theme.muted}`}>· 已編輯</span>
                              )}
                            </div>

                            {!msg.isRedacted && (
                              <div className="flex items-center gap-0.5">
                                <button
                                  type="button"
                                  onClick={() =>
                                    setReactionMenuForId(
                                      reactionMenuForId === msg.eventId ? null : msg.eventId
                                    )
                                  }
                                  aria-label={`對 ${msg.senderName} 的訊息加上表情反應`}
                                  className={`flex h-6 w-6 items-center justify-center rounded-lg ${theme.muted} ${theme.hoverSurface}`}
                                >
                                  <SmilePlus className="h-3.5 w-3.5" />
                                </button>
                                <button
                                  type="button"
                                  onClick={() => {
                                    setEditingMsg(null);
                                    setReplyingTo(msg);
                                  }}
                                  aria-label={`回覆 ${msg.senderName} 的訊息`}
                                  className={`flex h-6 w-6 items-center justify-center rounded-lg ${theme.muted} ${theme.hoverSurface}`}
                                >
                                  <CornerUpLeft className="h-3.5 w-3.5" />
                                </button>
                                <button
                                  type="button"
                                  onClick={() => onOpenThread(msg.eventId)}
                                  aria-label={`開啟 ${msg.senderName} 訊息討論串`}
                                  className={`flex h-6 w-6 items-center justify-center rounded-lg ${theme.muted} ${theme.hoverSurface}`}
                                >
                                  <MessageSquare className="h-3.5 w-3.5" />
                                </button>
                                {state.settings.developerTools && (
                                  <button
                                    type="button"
                                    onClick={() => setSourceModalMsg(msg)}
                                    aria-label={`檢視 ${msg.senderName} 事件原始碼`}
                                    className={`flex h-6 w-6 items-center justify-center rounded-lg ${theme.muted} ${theme.hoverSurface}`}
                                  >
                                    <Code2 className="h-3.5 w-3.5" />
                                  </button>
                                )}
                                {msg.isOwn && (
                                  <>
                                    <button
                                      type="button"
                                      onClick={() => {
                                        setReplyingTo(null);
                                        setEditingMsg(msg);
                                        setComposerText(msg.body);
                                      }}
                                      aria-label="編輯我的訊息"
                                      className={`flex h-6 w-6 items-center justify-center rounded-lg ${theme.muted} ${theme.hoverSurface}`}
                                    >
                                      <Pencil className="h-3.5 w-3.5" />
                                    </button>
                                    <button
                                      type="button"
                                      onClick={() => onRedactMessage(msg.eventId)}
                                      aria-label="撤回我的訊息"
                                      className={`flex h-6 w-6 items-center justify-center rounded-lg ${theme.muted} ${theme.hoverCritical}`}
                                    >
                                      <Trash2 className="h-3.5 w-3.5" />
                                    </button>
                                  </>
                                )}
                              </div>
                            )}
                          </div>

                          <p
                            className={`mt-1 text-[13px] leading-relaxed break-words ${
                              msg.isRedacted || msg.isUnableToDecrypt
                                ? `italic ${theme.warning}`
                                : ''
                            }`}
                          >
                            {msg.isRedacted || msg.isUnableToDecrypt ? (
                              msg.body
                            ) : (
                              <FormattedMessageBody body={msg.body} theme={theme} />
                            )}
                          </p>

                          {msg.attachment && !msg.isRedacted && (
                            <div className={`mt-2 overflow-hidden rounded-xl border p-2 ${theme.line} ${theme.surfaceVar}`}>
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
                                  <div className={`text-[10px] ${theme.muted}`}>
                                    {msg.attachment.isEncrypted ? 'AES-CTR 加密媒體' : 'Plain'} · {msg.attachment.sizeLabel}
                                  </div>
                                </div>
                                {!msg.attachment.isDecrypted ? (
                                  <button
                                    type="button"
                                    onClick={() => onDecryptMedia(msg.eventId)}
                                    className={`inline-flex shrink-0 items-center gap-1 rounded-lg px-2.5 py-1 text-[11px] font-medium ${theme.primaryBg} ${theme.onPrimary}`}
                                  >
                                    <Download className="h-3 w-3" />
                                    <span>解密檢視</span>
                                  </button>
                                ) : (
                                  <span className={`text-[10px] font-medium ${theme.success}`}>已快取</span>
                                )}
                              </div>
                            </div>
                          )}

                          {reactionMenuForId === msg.eventId && (
                            <div className={`mt-2 inline-flex items-center gap-1 rounded-xl border p-1 ${theme.line} ${theme.surface}`}>
                              {QUICK_REACTIONS.map((emoji) => (
                                <button
                                  key={emoji}
                                  type="button"
                                  onClick={() => {
                                    onToggleReaction(msg.eventId, emoji);
                                    setReactionMenuForId(null);
                                  }}
                                  aria-label={`反應 ${emoji}`}
                                  className={`flex h-7 w-7 items-center justify-center rounded-lg text-sm ${theme.hoverSurface}`}
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
                                      ? `${theme.primaryContainer} ${theme.onPrimaryContainer} border-transparent font-medium`
                                      : `${theme.line} ${theme.surfaceVar}`
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
                                  className={`inline-flex items-center gap-1 text-xs font-medium hover:underline px-1.5 py-0.5 tabular-nums ${theme.primaryText}`}
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
                  );
                })}
                </div>

                {showJumpToLatest && (
                  <button
                    type="button"
                    onClick={() => {
                      if (timelineRef.current) {
                        timelineRef.current.scrollTo({
                          top: timelineRef.current.scrollHeight,
                          behavior: 'smooth',
                        });
                      }
                    }}
                    className={`absolute bottom-2.5 left-1/2 -translate-x-1/2 inline-flex items-center gap-1.5 rounded-full border px-3 py-1.5 text-[11px] font-semibold shadow-md ${theme.line} ${theme.surface} ${theme.text}`}
                  >
                    <ArrowDown className={`h-3.5 w-3.5 ${theme.primaryText}`} />
                    <span>跳至最新訊息</span>
                  </button>
                )}
              </div>

              {/* Composer */}
              <form
                onSubmit={handleSend}
                className={`border-t px-2.5 py-2 ${theme.line} ${theme.surface}`}
              >
                {(replyingTo || editingMsg) && (
                  <div className={`mb-2 flex items-center justify-between rounded-lg px-2.5 py-1 text-xs ${theme.primaryContainer} ${theme.onPrimaryContainer}`}>
                    <span className="truncate">
                      {editingMsg
                        ? `編輯訊息：${editingMsg.body}`
                        : `回覆 ${replyingTo?.senderName}：${replyingTo?.body}`}
                    </span>
                    <button
                      type="button"
                      onClick={() => {
                        setReplyingTo(null);
                        setEditingMsg(null);
                        setComposerText('');
                      }}
                      aria-label="取消回覆或編輯"
                      className="ml-2 p-0.5 hover:opacity-75"
                    >
                      <X className="h-3.5 w-3.5" />
                    </button>
                  </div>
                )}

                <div className="flex items-center gap-1.5">
                  <button
                    type="button"
                    onClick={() => onSendMediaAttachment('android-capture.png', 'image/png')}
                    aria-label="傳送加密圖片附件"
                    className={`flex h-9 w-9 shrink-0 items-center justify-center rounded-xl ${theme.muted} ${theme.hoverSurface}`}
                  >
                    <Paperclip className="h-4 w-4" />
                  </button>
                  <input
                    type="text"
                    value={composerText}
                    onChange={(e) => setComposerText(e.target.value)}
                    placeholder={
                      editingMsg ? '修改訊息內容...' : `傳送訊息至 #${activeRoom.name}`
                    }
                    aria-label="輸入聊天訊息"
                    className={`min-h-[38px] flex-1 rounded-xl border px-3 py-1.5 text-xs outline-none ${theme.line} ${theme.surfaceVar}`}
                  />
                  <button
                    type="submit"
                    disabled={!composerText.trim()}
                    aria-label={editingMsg ? '儲存編輯訊息' : '傳送訊息'}
                    className={`flex h-[38px] w-[38px] shrink-0 items-center justify-center rounded-xl disabled:opacity-40 ${theme.primaryBg} ${theme.onPrimary}`}
                  >
                    <Send className="h-4 w-4" />
                  </button>
                </div>
              </form>

              {/* Thread Sheet Overlay */}
              {activeThreadRoot && (
                <div className={`absolute inset-0 z-20 flex flex-col ${theme.bg}`}>
                  <div className={`flex items-center justify-between border-b px-3 py-2.5 ${theme.line} ${theme.surface}`}>
                    <div className="flex items-center gap-2">
                      <button
                        type="button"
                        onClick={() => onOpenThread(null)}
                        aria-label="關閉討論串"
                        className={`flex h-9 w-9 items-center justify-center rounded-xl ${theme.hoverSurface}`}
                      >
                        <ArrowLeft className="h-4 w-4" />
                      </button>
                      <span className="text-sm font-semibold">討論串回覆</span>
                    </div>
                    <span className={`text-xs ${theme.muted} tabular-nums`}>
                      {activeThreadRoot.threadReplies.length} 則回覆
                    </span>
                  </div>

                  <div className="flex-1 space-y-3 overflow-y-auto p-4">
                    <div className={`rounded-xl border p-3 ${theme.line} ${theme.surface}`}>
                      <div className={`text-xs font-semibold ${theme.primaryText}`}>
                        {activeThreadRoot.senderName} · {activeThreadRoot.timestampLabel}
                      </div>
                      <p className="mt-1 text-sm">{activeThreadRoot.body}</p>
                    </div>

                    {activeThreadRoot.threadReplies.map((reply) => (
                      <div key={reply.eventId} className="pl-3">
                        <div className="text-xs font-semibold">
                          {reply.senderName}{' '}
                          <span className={`font-normal ${theme.muted} tabular-nums`}>
                            {reply.timestampLabel}
                          </span>
                        </div>
                        <p className={`mt-0.5 text-sm ${theme.muted}`}>{reply.body}</p>
                      </div>
                    ))}
                  </div>

                  <form
                    onSubmit={handleThreadSubmit}
                    className={`flex items-center gap-2 border-t p-3 ${theme.line} ${theme.surface}`}
                  >
                    <input
                      type="text"
                      value={threadDraft}
                      onChange={(e) => setThreadDraft(e.target.value)}
                      placeholder="回覆此討論串..."
                      aria-label="討論串輸入框"
                      className={`min-h-[38px] flex-1 rounded-xl border px-3 text-xs outline-none ${theme.line} ${theme.surfaceVar}`}
                    />
                    <button
                      type="submit"
                      disabled={!threadDraft.trim()}
                      aria-label="傳送討論串回覆"
                      className={`flex h-[38px] w-[38px] items-center justify-center rounded-xl disabled:opacity-40 ${theme.primaryBg} ${theme.onPrimary}`}
                    >
                      <Send className="h-4 w-4" />
                    </button>
                  </form>
                </div>
              )}
            </div>
          ) : (
            /* Android Screen 2: Cinny Space Rail + Categorized Room List Screen */
            <div className="flex flex-1 overflow-hidden">
              {/* Left Cinny 64px Space Rail */}
              <aside
                aria-label="Matrix Spaces 導覽列"
                className={`flex w-16 shrink-0 flex-col items-center justify-between border-r py-2.5 ${theme.line} ${theme.surface}`}
              >
                {/* Top & Middle Spaces */}
                <div className="flex w-full flex-col items-center gap-2 overflow-y-auto px-1">
                  <button
                    type="button"
                    onClick={() => onSelectSpace('all')}
                    aria-label="顯示全部聊天室"
                    className={`relative flex h-11 w-11 items-center justify-center text-xs font-bold transition-all ${
                      state.selectedSpaceFilter === 'all'
                        ? `rounded-xl ${theme.primaryBg} ${theme.onPrimary}`
                        : `rounded-2xl ${theme.surfaceVar} ${theme.text}`
                    }`}
                  >
                    All
                  </button>
                  <button
                    type="button"
                    onClick={() => onSelectSpace('dm')}
                    aria-label="顯示私訊 Direct Messages"
                    className={`relative flex h-11 w-11 items-center justify-center text-xs font-bold transition-all ${
                      state.selectedSpaceFilter === 'dm'
                        ? `rounded-xl ${theme.primaryBg} ${theme.onPrimary}`
                        : `rounded-2xl ${theme.surfaceVar} ${theme.text}`
                    }`}
                  >
                    DM
                  </button>

                  <div className={`my-0.5 h-px w-8 ${theme.divider}`} />

                  {state.spaces.map((space) => {
                    const selected = state.selectedSpaceFilter === space.roomId;
                    const { unread } = getSpaceUnreadSummary(space, state.rooms);
                    return (
                      <div key={space.roomId} className="relative flex w-full items-center justify-center">
                        {selected && (
                          <span
                            aria-hidden="true"
                            className={`absolute left-0 h-5 w-1 rounded-r-full ${theme.primaryBg}`}
                          />
                        )}
                        <button
                          type="button"
                          onClick={() => onSelectSpace(space.roomId)}
                          aria-label={`切換 Space ${space.name}`}
                          className={`relative h-11 w-11 overflow-hidden border-2 transition-all ${
                            selected
                              ? `rounded-xl border-current ${theme.primaryText}`
                              : 'rounded-2xl border-transparent opacity-85 hover:opacity-100'
                          }`}
                        >
                          <SafeAvatar
                            src={space.avatarUrl}
                            alt={space.name}
                            fallback={space.shortCode}
                            theme={theme}
                          />
                          {unread > 0 && (
                            <span
                              className={`absolute -right-0.5 -bottom-0.5 flex h-4 min-w-[16px] items-center justify-center rounded-full px-1 text-[10px] font-bold tabular-nums ${theme.primaryBg} ${theme.onPrimary}`}
                            >
                              {unread}
                            </span>
                          )}
                        </button>
                      </div>
                    );
                  })}
                </div>

                {/* Bottom Sticky Cinny Actions: Create / Explore / Inbox / Settings */}
                <div className="flex flex-col items-center gap-1.5 pt-2 border-t border-current/10">
                  <button
                    type="button"
                    onClick={() => setShowCreateModal(true)}
                    aria-label="建立聊天室或 Space"
                    className={`flex h-9 w-9 items-center justify-center rounded-xl ${theme.surfaceVar} ${theme.text}`}
                  >
                    <Plus className="h-4 w-4" />
                  </button>
                  <button
                    type="button"
                    onClick={() => setActiveOverlay('explore')}
                    aria-label="探索公開頻道 Explore"
                    className={`flex h-9 w-9 items-center justify-center rounded-xl ${theme.muted} ${theme.hoverSurface}`}
                  >
                    <Compass className="h-4 w-4" />
                  </button>
                  <button
                    type="button"
                    onClick={() => setActiveOverlay('inbox')}
                    aria-label="開啟收件匣 Inbox"
                    className={`relative flex h-9 w-9 items-center justify-center rounded-xl ${theme.muted} ${theme.hoverSurface}`}
                  >
                    <Inbox className="h-4 w-4" />
                    {inboxBadgeCount > 0 && (
                      <span className={`absolute top-1 right-1 h-2 w-2 rounded-full ${theme.primaryBg}`} />
                    )}
                  </button>
                  <button
                    type="button"
                    onClick={() => setActiveOverlay('settings')}
                    aria-label="開啟 Cinny 設定"
                    className={`flex h-9 w-9 items-center justify-center rounded-xl ${theme.primaryContainer} ${theme.onPrimaryContainer}`}
                  >
                    <Settings className="h-4 w-4" />
                  </button>
                </div>
              </aside>

              {/* Right Room List Pane */}
              <div className="flex flex-1 flex-col overflow-hidden">
                <div className={`flex items-center justify-between border-b px-3 py-2.5 ${theme.line} ${theme.surface}`}>
                  <div className="flex min-w-0 items-center gap-2">
                    {selectedSpaceNode ? (
                      <div className="h-7 w-7 shrink-0 overflow-hidden rounded-lg">
                        <SafeAvatar
                          src={selectedSpaceNode.avatarUrl}
                          alt={selectedSpaceNode.name}
                          fallback={selectedSpaceNode.shortCode}
                          theme={theme}
                        />
                      </div>
                    ) : (
                      <img
                        src="/res/svg/cinny.svg"
                        alt="Cinny"
                        className="h-7 w-7 shrink-0 rounded-full border border-current/15 object-cover"
                      />
                    )}
                    <div className="min-w-0">
                      <h2 className="truncate text-sm font-semibold">{activeSpaceName}</h2>
                      <p className={`truncate text-[10px] ${theme.muted}`}>
                        {state.displayName} · {state.settings.themeMode}
                      </p>
                    </div>
                  </div>
                  <button
                    type="button"
                    onClick={onStartVerification}
                    aria-label="裝置安全驗證"
                    className={`flex h-8 w-8 items-center justify-center rounded-xl ${
                      state.verification.kind === 'confirmed' || state.recoveryUnlocked
                        ? theme.success
                        : `${theme.muted} ${theme.hoverSurface}`
                    }`}
                  >
                    <ShieldCheck className="h-4 w-4" />
                  </button>
                </div>

                <div className="p-2.5">
                  <div className="relative">
                    <Search
                      className={`pointer-events-none absolute top-1/2 left-2.5 h-3.5 w-3.5 -translate-y-1/2 ${theme.muted}`}
                      aria-hidden="true"
                    />
                    <input
                      type="text"
                      value={roomQuery}
                      onChange={(e) => setRoomQuery(e.target.value)}
                      placeholder="搜尋房間或聯絡人..."
                      aria-label="搜尋聊天室"
                      className={`min-h-[36px] w-full rounded-xl border pr-3 pl-8 text-xs outline-none ${theme.line} ${theme.surface}`}
                    />
                  </div>
                </div>

                <div className="flex-1 space-y-2 overflow-y-auto px-2.5 pb-3">
                  {selectedSpaceNode && (
                    <section
                      aria-label={`Space ${selectedSpaceNode.name} 導覽卡片`}
                      className={`rounded-2xl border p-2.5 ${theme.line} ${theme.surface}`}
                    >
                      <div className="flex items-center gap-2.5">
                        <div className="h-10 w-10 shrink-0 overflow-hidden rounded-xl">
                          <SafeAvatar
                            src={selectedSpaceNode.avatarUrl}
                            alt={selectedSpaceNode.name}
                            fallback={selectedSpaceNode.shortCode}
                            theme={theme}
                          />
                        </div>
                        <div className="min-w-0 flex-1">
                          <div className="truncate text-xs font-bold">{selectedSpaceNode.name}</div>
                          <p className={`line-clamp-2 text-[10px] leading-snug ${theme.muted}`}>
                            {selectedSpaceNode.topic}
                          </p>
                        </div>
                      </div>
                      <div className="mt-2 grid grid-cols-2 gap-1.5">
                        <button
                          type="button"
                          onClick={() => setActiveOverlay('explore')}
                          className={`rounded-lg border px-2 py-1 text-[10px] font-semibold ${theme.line} ${theme.hoverSurface}`}
                        >
                          瀏覽空間頻道
                        </button>
                        <button
                          type="button"
                          onClick={() => setShowCreateModal(true)}
                          className={`rounded-lg px-2 py-1 text-[10px] font-semibold ${theme.primaryContainer} ${theme.onPrimaryContainer}`}
                        >
                          建立頻道
                        </button>
                      </div>
                    </section>
                  )}

                  {invitedRooms.length > 0 && state.selectedSpaceFilter === 'all' && (
                    <div>
                      <div className={`px-1.5 pb-1 text-[10px] font-semibold uppercase tracking-wider ${theme.primaryText}`}>
                        Invites ({invitedRooms.length})
                      </div>
                      {invitedRooms.map((room) => (
                        <div
                          key={room.roomId}
                          className={`flex items-center justify-between gap-2 rounded-xl border px-2.5 py-2 ${theme.line} ${theme.surface}`}
                        >
                          <div className="min-w-0">
                            <div className="truncate text-xs font-semibold">{room.name}</div>
                            <div className={`truncate text-[10px] ${theme.muted}`}>{room.topic}</div>
                          </div>
                          <button
                            type="button"
                            onClick={() => onAcceptInvite(room.roomId)}
                            className={`shrink-0 rounded-lg px-2 py-1 text-[10px] font-semibold ${theme.primaryBg} ${theme.onPrimary}`}
                          >
                            加入
                          </button>
                        </div>
                      ))}
                    </div>
                  )}

                  {joinedFilteredRooms.length === 0 ? (
                    <div className={`py-10 text-center text-xs ${theme.muted}`}>
                      找不到符合條件的聊天室
                    </div>
                  ) : (
                    joinedFilteredRooms.map((room) => (
                      <button
                        key={room.roomId}
                        type="button"
                        onClick={() => onSelectRoom(room.roomId)}
                        className={`flex w-full items-center gap-2.5 rounded-xl px-2.5 py-2 text-left transition-colors ${theme.hoverSurface}`}
                      >
                        <span
                          className={`flex h-9 w-9 shrink-0 items-center justify-center rounded-xl text-xs font-bold ${theme.surfaceVar}`}
                        >
                          {room.isEncrypted ? (
                            <Lock className={`h-3.5 w-3.5 ${theme.success}`} aria-hidden="true" />
                          ) : (
                            <Hash className={`h-3.5 w-3.5 ${theme.muted}`} aria-hidden="true" />
                          )}
                        </span>
                        <div className="min-w-0 flex-1">
                          <div className="truncate text-xs font-semibold">{room.name}</div>
                          <div className={`truncate text-[10px] ${theme.muted}`}>{room.topic}</div>
                        </div>
                        {(room.highlightCount > 0 || room.unreadCount > 0) && (
                          <span
                            className={`rounded-full px-1.5 py-0.5 text-[10px] font-bold tabular-nums ${
                              room.highlightCount > 0
                                ? `${theme.primaryBg} ${theme.onPrimary}`
                                : `${theme.surfaceVar} ${theme.text}`
                            }`}
                          >
                            {room.highlightCount > 0 ? `@${room.highlightCount}` : room.unreadCount}
                          </span>
                        )}
                      </button>
                    ))
                  )}
                </div>
              </div>
            </div>
          )}

          {/* Cinny Full Overlays inside Phone */}
          {activeOverlay === 'settings' && (
            <PhoneSettingsOverlay
              state={state}
              theme={theme}
              onClose={() => setActiveOverlay('none')}
              onUpdateSettings={onUpdateSettings}
              onUpdateDisplayName={onUpdateDisplayName}
              onOpenSecurity={() => {
                setActiveOverlay('none');
                onStartVerification();
              }}
              onSignOut={() => {
                setActiveOverlay('none');
                onLogout();
              }}
            />
          )}

          {activeOverlay === 'roomSettings' && activeRoom && (
            <PhoneRoomSettingsOverlay
              room={activeRoom}
              members={activeRoomMembers}
              theme={theme}
              onClose={() => setActiveOverlay('none')}
              onSave={(patch) => onUpdateRoomSettings(activeRoom.roomId, patch)}
            />
          )}

          {activeOverlay === 'members' && activeRoom && (
            <PhoneMembersOverlay
              room={activeRoom}
              members={activeRoomMembers}
              theme={theme}
              onClose={() => setActiveOverlay('none')}
              onInviteUser={(userId) => onInviteUser(activeRoom.roomId, userId)}
            />
          )}

          {activeOverlay === 'explore' && (
            <PhoneExploreOverlay
              state={state}
              theme={theme}
              onClose={() => setActiveOverlay('none')}
              onJoinRoom={onJoinDiscoverableRoom}
              onOpenJoinedRoom={onSelectRoom}
            />
          )}

          {activeOverlay === 'inbox' && (
            <PhoneInboxOverlay
              state={state}
              theme={theme}
              onClose={() => setActiveOverlay('none')}
              onAcceptInvite={onAcceptInvite}
              onOpenRoom={onSelectRoom}
            />
          )}

          {showCreateModal && (
            <PhoneCreateHubModal
              theme={theme}
              onClose={() => setShowCreateModal(false)}
              onCreateRoom={onCreateRoom}
              onCreateSpace={onCreateSpace}
            />
          )}

          {sourceModalMsg && (
            <PhoneEventSourceModal
              message={sourceModalMsg}
              theme={theme}
              onClose={() => setSourceModalMsg(null)}
            />
          )}

          {showTopicModal && activeRoom && (
            <PhoneTopicModal
              room={activeRoom}
              theme={theme}
              onClose={() => setShowTopicModal(false)}
              onOpenSettings={() => setActiveOverlay('roomSettings')}
            />
          )}

          {/* E2EE Security Modal inside Phone */}
          {state.verification.kind !== 'idle' && (
            <div
              role="dialog"
              aria-modal="true"
              aria-label="E2EE 安全驗證與金鑰備份中心"
              className="absolute inset-0 z-50 flex items-center justify-center bg-black/60 p-4 backdrop-blur-xs"
            >
              <div className={`w-full rounded-2xl border p-4 shadow-xl ${theme.line} ${theme.surface} ${theme.text}`}>
                <h3 className="text-sm font-semibold">E2EE 安全驗證與 4S 金鑰備份</h3>
                <div className={`mt-2.5 grid grid-cols-2 gap-1 rounded-lg p-1 text-xs ${theme.surfaceVar}`}>
                  <button
                    type="button"
                    onClick={() => setSecurityTab('sas')}
                    className={`rounded-md py-1.5 font-medium ${
                      securityTab === 'sas' ? `${theme.primaryBg} ${theme.onPrimary}` : theme.muted
                    }`}
                  >
                    SAS 裝置驗證
                  </button>
                  <button
                    type="button"
                    onClick={() => setSecurityTab('recovery')}
                    className={`rounded-md py-1.5 font-medium ${
                      securityTab === 'recovery' ? `${theme.primaryBg} ${theme.onPrimary}` : theme.muted
                    }`}
                  >
                    4S 金鑰還原
                  </button>
                </div>

                {securityTab === 'recovery' ? (
                  <div className="mt-3 space-y-2.5">
                    <p className={`text-xs ${theme.muted}`}>
                      輸入 4S Recovery Key 可還原 Cross-Signing 並解密過去封存的 Megolm 房間金鑰。
                    </p>
                    <input
                      type="text"
                      value={recoveryKeyInput}
                      onChange={(e) => setRecoveryKeyInput(e.target.value)}
                      aria-label="4S Recovery Key 輸入框"
                      className={`w-full rounded-xl border px-3 py-2 font-mono text-xs outline-none ${theme.line} ${theme.surfaceVar}`}
                    />
                    {state.recoveryUnlocked && (
                      <p className={`text-xs font-medium ${theme.success}`}>
                        4S 金鑰備份已解鎖，時間軸歷史訊息已解密。
                      </p>
                    )}
                    <div className="flex justify-end gap-2 pt-1">
                      <button
                        type="button"
                        onClick={onCloseVerification}
                        className={`rounded-lg px-3 py-1.5 text-xs ${theme.muted}`}
                      >
                        關閉
                      </button>
                      <button
                        type="button"
                        onClick={() => onRecoverSecretBackup(recoveryKeyInput)}
                        className={`inline-flex items-center gap-1 rounded-lg px-3 py-1.5 text-xs font-semibold ${theme.primaryBg} ${theme.onPrimary}`}
                      >
                        <KeyRound className="h-3.5 w-3.5" />
                        <span>解鎖 4S 備份</span>
                      </button>
                    </div>
                  </div>
                ) : state.verification.kind === 'sas' ? (
                  <>
                    <p className={`mt-2.5 text-xs ${theme.muted}`}>
                      請確認對方裝置顯示的 7 組表情符號與以下完全相符：
                    </p>
                    <div className="mt-3 grid grid-cols-4 gap-2 text-center">
                      {state.verification.emojis.map((item) => (
                        <div key={item.label} className={`rounded-xl p-2 ${theme.surfaceVar}`}>
                          <div className="text-lg">{item.symbol}</div>
                          <div className="mt-0.5 text-[10px] font-medium">{item.label}</div>
                        </div>
                      ))}
                    </div>
                    <div className="mt-4 flex justify-end gap-2">
                      <button
                        type="button"
                        onClick={onCloseVerification}
                        className={`min-h-[38px] rounded-xl px-3 py-1.5 text-xs font-medium ${theme.muted}`}
                      >
                        取消
                      </button>
                      <button
                        type="button"
                        onClick={onConfirmVerification}
                        className={`min-h-[38px] rounded-xl px-4 py-1.5 text-xs font-semibold ${theme.primaryBg} ${theme.onPrimary}`}
                      >
                        表情完全相符
                      </button>
                    </div>
                  </>
                ) : (
                  <>
                    <p className={`mt-3 text-xs font-medium ${theme.success}`}>
                      裝置 {state.verification.deviceId} 已透過 Rust matrix-sdk-crypto 完成交叉簽章驗證。
                    </p>
                    <div className="mt-4 flex justify-end">
                      <button
                        type="button"
                        onClick={onCloseVerification}
                        className={`min-h-[38px] rounded-xl px-4 py-1.5 text-xs font-semibold ${theme.primaryBg} ${theme.onPrimary}`}
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
        <div className={`flex justify-center py-2.5 ${theme.surface}`}>
          <span aria-hidden="true" className={`h-1 w-28 rounded-full ${theme.divider}`} />
        </div>
      </div>
    </div>
  );
}
