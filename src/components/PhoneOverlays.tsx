import { useState, FormEvent } from 'react';
import {
  ArrowLeft,
  Bell,
  Check,
  Code2,
  Compass,
  Hash,
  Inbox,
  Lock,
  ShieldCheck,
  User,
  Users,
  X,
} from 'lucide-react';
import {
  CinnyAppState,
  CinnyMessageLayout,
  CinnySettingsState,
  CinnyThemeMode,
  RoomNotificationLevel,
  RoomSummary,
  TimelineMessage,
} from '../lib/cinnyEngine';
import { PhoneThemeClasses } from './phoneThemeClasses';

const THEME_OPTIONS: Array<{ mode: CinnyThemeMode; label: string; swatchBg: string; swatchAccent: string }> = [
  { mode: 'Light', label: 'Light', swatchBg: 'bg-[#F5F5F5]', swatchAccent: 'bg-[#1245A8]' },
  { mode: 'Silver', label: 'Silver', swatchBg: 'bg-[#DEDEDE]', swatchAccent: 'bg-[#1245A8]' },
  { mode: 'Dark', label: 'Dark', swatchBg: 'bg-[#1A1A1A]', swatchAccent: 'bg-[#BDB6EC]' },
  { mode: 'Butter', label: 'Butter', swatchBg: 'bg-[#1A1916]', swatchAccent: 'bg-[#FFFBDE]' },
];

const LAYOUT_OPTIONS: Array<{ value: CinnyMessageLayout; label: string; desc: string }> = [
  { value: 'Modern', label: 'Modern', desc: '頭像分組排版' },
  { value: 'Compact', label: 'Compact', desc: 'IRC 高密度' },
  { value: 'Bubble', label: 'Bubble', desc: '對話氣泡' },
];

export function PhoneSettingsOverlay({
  state,
  theme,
  onClose,
  onUpdateSettings,
  onUpdateDisplayName,
  onOpenSecurity,
  onSignOut,
}: {
  state: CinnyAppState;
  theme: PhoneThemeClasses;
  onClose: () => void;
  onUpdateSettings: (patch: Partial<CinnySettingsState>) => void;
  onUpdateDisplayName: (name: string) => void;
  onOpenSecurity: () => void;
  onSignOut: () => void;
}) {
  const [nameDraft, setNameDraft] = useState(state.displayName);

  const handleSaveName = (e: FormEvent) => {
    e.preventDefault();
    if (!nameDraft.trim()) return;
    onUpdateDisplayName(nameDraft.trim());
  };

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-label="Cinny Settings"
      className={`absolute inset-0 z-40 flex flex-col ${theme.bg} ${theme.text}`}
    >
      <div className={`flex items-center justify-between border-b px-3 py-2.5 ${theme.line} ${theme.surface}`}>
        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={onClose}
            aria-label="關閉設定"
            className={`flex h-9 w-9 items-center justify-center rounded-xl ${theme.hoverSurface}`}
          >
            <ArrowLeft className="h-4 w-4" />
          </button>
          <div>
            <h3 className="text-sm font-semibold">設定 Settings</h3>
            <p className={`text-[10px] ${theme.muted}`}>{state.currentUserId}</p>
          </div>
        </div>
      </div>

      <div className="flex-1 space-y-4 overflow-y-auto p-3.5 text-xs">
        {/* Appearance: Themes */}
        <section className={`rounded-2xl border p-3 ${theme.line} ${theme.surface}`}>
          <h4 className={`mb-2 text-[11px] font-semibold uppercase tracking-wider ${theme.muted}`}>
            外觀主題 (Cinny Themes)
          </h4>
          <div className="grid grid-cols-2 gap-2">
            {THEME_OPTIONS.map((opt) => {
              const active = state.settings.themeMode === opt.mode;
              return (
                <button
                  key={opt.mode}
                  type="button"
                  onClick={() => onUpdateSettings({ themeMode: opt.mode })}
                  className={`flex items-center justify-between rounded-xl border px-2.5 py-2 text-left transition-colors ${
                    active
                      ? `${theme.primaryContainer} ${theme.onPrimaryContainer} border-transparent font-semibold`
                      : `${theme.line} ${theme.hoverSurface}`
                  }`}
                >
                  <div className="flex items-center gap-2">
                    <span className={`flex h-5 w-5 items-center justify-center rounded-md border ${theme.line} ${opt.swatchBg}`}>
                      <span className={`h-2 w-2 rounded-full ${opt.swatchAccent}`} />
                    </span>
                    <span>{opt.label}</span>
                  </div>
                  {active && <Check className="h-3.5 w-3.5" />}
                </button>
              );
            })}
          </div>
        </section>

        {/* Message Layout */}
        <section className={`rounded-2xl border p-3 ${theme.line} ${theme.surface}`}>
          <h4 className={`mb-2 text-[11px] font-semibold uppercase tracking-wider ${theme.muted}`}>
            訊息排版 (Message Layout)
          </h4>
          <div className="grid grid-cols-3 gap-1.5">
            {LAYOUT_OPTIONS.map((opt) => {
              const active = state.settings.messageLayout === opt.value;
              return (
                <button
                  key={opt.value}
                  type="button"
                  onClick={() => onUpdateSettings({ messageLayout: opt.value })}
                  className={`rounded-xl border px-2 py-2 text-center transition-colors ${
                    active
                      ? `${theme.primaryBg} ${theme.onPrimary} border-transparent font-semibold`
                      : `${theme.line} ${theme.hoverSurface}`
                  }`}
                >
                  <div>{opt.label}</div>
                  <div className={`mt-0.5 text-[9px] ${active ? 'opacity-85' : theme.muted}`}>
                    {opt.desc}
                  </div>
                </button>
              );
            })}
          </div>

          <div className="mt-3 space-y-2 pt-2 border-t border-dashed border-current/15">
            <label className="flex cursor-pointer items-center justify-between gap-2">
              <span>24 小時制時間</span>
              <input
                type="checkbox"
                checked={state.settings.use24HourClock}
                onChange={(e) => onUpdateSettings({ use24HourClock: e.target.checked })}
                className={theme.checkboxAccent}
              />
            </label>
            <label className="flex cursor-pointer items-center justify-between gap-2">
              <span>依最新活動排序房間</span>
              <input
                type="checkbox"
                checked={state.settings.sortRoomsByActivity}
                onChange={(e) => onUpdateSettings({ sortRoomsByActivity: e.target.checked })}
                className={theme.checkboxAccent}
              />
            </label>
            <label className="flex cursor-pointer items-center justify-between gap-2">
              <span>Developer Tools (事件 JSON 檢視)</span>
              <input
                type="checkbox"
                checked={state.settings.developerTools}
                onChange={(e) => onUpdateSettings({ developerTools: e.target.checked })}
                className={theme.checkboxAccent}
              />
            </label>
          </div>
        </section>

        {/* Account Profile */}
        <form onSubmit={handleSaveName} className={`rounded-2xl border p-3 ${theme.line} ${theme.surface}`}>
          <h4 className={`mb-2 flex items-center gap-1.5 text-[11px] font-semibold uppercase tracking-wider ${theme.muted}`}>
            <User className="h-3.5 w-3.5" />
            <span>帳號資料 (Profile)</span>
          </h4>
          <div className="flex gap-2">
            <input
              type="text"
              value={nameDraft}
              onChange={(e) => setNameDraft(e.target.value)}
              aria-label="顯示名稱"
              className={`min-h-[36px] flex-1 rounded-xl border px-2.5 text-xs outline-none ${theme.line} ${theme.surfaceVar}`}
            />
            <button
              type="submit"
              className={`rounded-xl px-3 py-1.5 font-semibold ${theme.primaryBg} ${theme.onPrimary}`}
            >
              儲存
            </button>
          </div>
        </form>

        {/* Security & Sign out */}
        <section className={`rounded-2xl border p-3 space-y-2 ${theme.line} ${theme.surface}`}>
          <button
            type="button"
            onClick={onOpenSecurity}
            className={`flex w-full items-center justify-between rounded-xl px-2.5 py-2 ${theme.hoverSurface}`}
          >
            <span className="flex items-center gap-2 font-medium">
              <ShieldCheck className={`h-4 w-4 ${theme.success}`} />
              <span>裝置驗證與 4S 金鑰備份</span>
            </span>
            <span className={`text-[10px] ${theme.muted}`}>
              {state.recoveryUnlocked ? '已解鎖' : '未解鎖'}
            </span>
          </button>
          <button
            type="button"
            onClick={onSignOut}
            className={`w-full rounded-xl border px-3 py-2 font-semibold ${theme.line} ${theme.critical} ${theme.hoverSurface}`}
          >
            登出 Matrix 帳號
          </button>
        </section>
      </div>
    </div>
  );
}

export function PhoneRoomSettingsOverlay({
  room,
  theme,
  onClose,
  onSave,
}: {
  room: RoomSummary;
  theme: PhoneThemeClasses;
  onClose: () => void;
  onSave: (patch: { name?: string; topic?: string; notificationLevel?: RoomNotificationLevel }) => void;
}) {
  const [nameDraft, setNameDraft] = useState(room.name);
  const [topicDraft, setTopicDraft] = useState(room.topic);
  const currentLevel: RoomNotificationLevel = room.notificationLevel ?? 'AllMessages';

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    onSave({ name: nameDraft, topic: topicDraft });
    onClose();
  };

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-label="房間設定 Room Settings"
      className={`absolute inset-0 z-40 flex flex-col ${theme.bg} ${theme.text}`}
    >
      <div className={`flex items-center justify-between border-b px-3 py-2.5 ${theme.line} ${theme.surface}`}>
        <div className="flex items-center gap-2 min-w-0">
          <button
            type="button"
            onClick={onClose}
            aria-label="關閉房間設定"
            className={`flex h-9 w-9 shrink-0 items-center justify-center rounded-xl ${theme.hoverSurface}`}
          >
            <ArrowLeft className="h-4 w-4" />
          </button>
          <div className="min-w-0">
            <h3 className="truncate text-sm font-semibold">聊天室設定</h3>
            <p className={`truncate text-[10px] ${theme.muted}`}>{room.roomId}</p>
          </div>
        </div>
      </div>

      <div className="flex-1 space-y-3.5 overflow-y-auto p-3.5 text-xs">
        <form onSubmit={handleSubmit} className={`rounded-2xl border p-3 space-y-2.5 ${theme.line} ${theme.surface}`}>
          <div className={`text-[11px] font-semibold uppercase tracking-wider ${theme.muted}`}>
            基本資訊
          </div>
          <div>
            <label htmlFor="room-name-input" className={`mb-1 block text-[11px] ${theme.muted}`}>
              房間名稱
            </label>
            <input
              id="room-name-input"
              type="text"
              value={nameDraft}
              onChange={(e) => setNameDraft(e.target.value)}
              className={`min-h-[36px] w-full rounded-xl border px-2.5 text-xs outline-none ${theme.line} ${theme.surfaceVar}`}
            />
          </div>
          <div>
            <label htmlFor="room-topic-input" className={`mb-1 block text-[11px] ${theme.muted}`}>
              房間主題 (Topic)
            </label>
            <textarea
              id="room-topic-input"
              rows={2}
              value={topicDraft}
              onChange={(e) => setTopicDraft(e.target.value)}
              className={`w-full rounded-xl border p-2.5 text-xs outline-none ${theme.line} ${theme.surfaceVar}`}
            />
          </div>
          <div className="flex items-center justify-between pt-1">
            <span className={`inline-flex items-center gap-1 text-[11px] ${room.isEncrypted ? theme.success : theme.muted}`}>
              {room.isEncrypted ? <Lock className="h-3.5 w-3.5" /> : <Hash className="h-3.5 w-3.5" />}
              {room.isEncrypted ? '已啟用 Megolm 端對端加密' : '未加密公開頻道'}
            </span>
            <button
              type="submit"
              className={`rounded-xl px-3.5 py-1.5 font-semibold ${theme.primaryBg} ${theme.onPrimary}`}
            >
              套用變更
            </button>
          </div>
        </form>

        <section className={`rounded-2xl border p-3 space-y-2 ${theme.line} ${theme.surface}`}>
          <div className={`flex items-center gap-1.5 text-[11px] font-semibold uppercase tracking-wider ${theme.muted}`}>
            <Bell className="h-3.5 w-3.5" />
            <span>通知等級</span>
          </div>
          {(
            [
              ['AllMessages', '所有訊息通知'],
              ['MentionsAndKeywordsOnly', '僅提及 (@) 與關鍵字'],
              ['Mute', '靜音此聊天室'],
            ] as Array<[RoomNotificationLevel, string]>
          ).map(([level, label]) => (
            <button
              key={level}
              type="button"
              onClick={() => onSave({ notificationLevel: level })}
              className={`flex w-full items-center justify-between rounded-xl px-2.5 py-2 text-left ${
                currentLevel === level
                  ? `${theme.primaryContainer} ${theme.onPrimaryContainer} font-semibold`
                  : theme.hoverSurface
              }`}
            >
              <span>{label}</span>
              {currentLevel === level && <Check className="h-3.5 w-3.5" />}
            </button>
          ))}
        </section>

        <section className={`rounded-2xl border p-3 space-y-2 ${theme.line} ${theme.surface}`}>
          <div className={`flex items-center gap-1.5 text-[11px] font-semibold uppercase tracking-wider ${theme.muted}`}>
            <Users className="h-3.5 w-3.5" />
            <span>成員名單 (Members)</span>
          </div>
          <div className="space-y-1.5 text-[11px]">
            <div className="flex items-center justify-between">
              <span>You (@you:matrix.org)</span>
              <span className={`rounded px-1.5 py-0.5 text-[10px] ${theme.primaryContainer} ${theme.onPrimaryContainer}`}>Admin</span>
            </div>
            <div className="flex items-center justify-between">
              <span>Ajay (@ajbura:matrix.org)</span>
              <span className={`rounded px-1.5 py-0.5 text-[10px] ${theme.surfaceVar}`}>Moderator</span>
            </div>
            <div className="flex items-center justify-between">
              <span>Lin (@lin:matrix.org)</span>
              <span className={`rounded px-1.5 py-0.5 text-[10px] ${theme.surfaceVar}`}>Member</span>
            </div>
          </div>
        </section>
      </div>
    </div>
  );
}

export function PhoneExploreOverlay({
  state,
  theme,
  onClose,
  onJoinRoom,
  onOpenJoinedRoom,
}: {
  state: CinnyAppState;
  theme: PhoneThemeClasses;
  onClose: () => void;
  onJoinRoom: (roomId: string) => void;
  onOpenJoinedRoom: (roomId: string) => void;
}) {
  const [query, setQuery] = useState('');
  const filtered = state.discoverableRooms.filter(
    (r) =>
      !query.trim() ||
      r.name.toLowerCase().includes(query.toLowerCase()) ||
      (r.canonicalAlias ?? '').toLowerCase().includes(query.toLowerCase())
  );

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-label="探索公開頻道 Explore"
      className={`absolute inset-0 z-40 flex flex-col ${theme.bg} ${theme.text}`}
    >
      <div className={`flex items-center gap-2 border-b px-3 py-2.5 ${theme.line} ${theme.surface}`}>
        <button
          type="button"
          onClick={onClose}
          aria-label="關閉探索"
          className={`flex h-9 w-9 items-center justify-center rounded-xl ${theme.hoverSurface}`}
        >
          <ArrowLeft className="h-4 w-4" />
        </button>
        <Compass className={`h-4 w-4 ${theme.primaryText}`} />
        <h3 className="text-sm font-semibold">探索公開頻道 Explore</h3>
      </div>

      <div className="p-3">
        <input
          type="text"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="搜尋 #alias 或公開房間..."
          aria-label="搜尋公開房間"
          className={`min-h-[38px] w-full rounded-xl border px-3 text-xs outline-none ${theme.line} ${theme.surface}`}
        />
      </div>

      <div className="flex-1 space-y-2.5 overflow-y-auto px-3 pb-4">
        {filtered.map((item) => (
          <div
            key={item.roomId}
            className={`rounded-2xl border p-3 ${theme.line} ${theme.surface}`}
          >
            <div className="flex items-start justify-between gap-2">
              <div className="min-w-0 flex-1">
                <div className="truncate text-xs font-semibold">{item.name}</div>
                <div className={`truncate text-[10px] ${theme.primaryText}`}>
                  {item.canonicalAlias ?? item.roomId}
                </div>
                <p className={`mt-1 line-clamp-2 text-[11px] ${theme.muted}`}>{item.topic}</p>
                <div className={`mt-1.5 text-[10px] ${theme.muted} tabular-nums`}>
                  {item.memberCount} 位成員 · {item.isEncrypted ? 'E2EE' : '公開'}
                </div>
              </div>
              {item.joined ? (
                <button
                  type="button"
                  onClick={() => {
                    onOpenJoinedRoom(item.roomId);
                    onClose();
                  }}
                  className={`shrink-0 rounded-xl px-2.5 py-1.5 text-[11px] font-semibold ${theme.primaryContainer} ${theme.onPrimaryContainer}`}
                >
                  開啟
                </button>
              ) : (
                <button
                  type="button"
                  onClick={() => onJoinRoom(item.roomId)}
                  className={`shrink-0 rounded-xl px-3 py-1.5 text-[11px] font-semibold ${theme.primaryBg} ${theme.onPrimary}`}
                >
                  加入
                </button>
              )}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

export function PhoneInboxOverlay({
  state,
  theme,
  onClose,
  onAcceptInvite,
  onOpenRoom,
}: {
  state: CinnyAppState;
  theme: PhoneThemeClasses;
  onClose: () => void;
  onAcceptInvite: (roomId: string) => void;
  onOpenRoom: (roomId: string) => void;
}) {
  const [tab, setTab] = useState<'invites' | 'mentions'>('invites');
  const invites = state.rooms.filter((r) => r.category === 'Invited');
  const highlights = state.rooms.filter((r) => r.category !== 'Invited' && (r.highlightCount > 0 || r.unreadCount > 0));

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-label="收件匣 Inbox"
      className={`absolute inset-0 z-40 flex flex-col ${theme.bg} ${theme.text}`}
    >
      <div className={`flex items-center gap-2 border-b px-3 py-2.5 ${theme.line} ${theme.surface}`}>
        <button
          type="button"
          onClick={onClose}
          aria-label="關閉收件匣"
          className={`flex h-9 w-9 items-center justify-center rounded-xl ${theme.hoverSurface}`}
        >
          <ArrowLeft className="h-4 w-4" />
        </button>
        <Inbox className={`h-4 w-4 ${theme.primaryText}`} />
        <h3 className="text-sm font-semibold">收件匣 Inbox</h3>
      </div>

      <div className={`grid grid-cols-2 border-b ${theme.line} ${theme.surface} text-xs`}>
        <button
          type="button"
          onClick={() => setTab('invites')}
          className={`py-2.5 font-semibold border-b-2 ${
            tab === 'invites' ? `${theme.primaryText} border-current` : `${theme.muted} border-transparent`
          }`}
        >
          待接受邀請 ({invites.length})
        </button>
        <button
          type="button"
          onClick={() => setTab('mentions')}
          className={`py-2.5 font-semibold border-b-2 ${
            tab === 'mentions' ? `${theme.primaryText} border-current` : `${theme.muted} border-transparent`
          }`}
        >
          未讀通知 ({highlights.length})
        </button>
      </div>

      <div className="flex-1 space-y-2.5 overflow-y-auto p-3">
        {tab === 'invites' ? (
          invites.length === 0 ? (
            <div className={`py-10 text-center text-xs ${theme.muted}`}>目前沒有待處理的房間邀請</div>
          ) : (
            invites.map((room) => (
              <div key={room.roomId} className={`rounded-2xl border p-3 ${theme.line} ${theme.surface}`}>
                <div className="text-xs font-semibold">{room.name}</div>
                <p className={`mt-1 text-[11px] ${theme.muted}`}>{room.topic}</p>
                <div className="mt-2.5 flex justify-end gap-2">
                  <button
                    type="button"
                    onClick={() => onAcceptInvite(room.roomId)}
                    className={`rounded-xl px-3 py-1.5 text-[11px] font-semibold ${theme.primaryBg} ${theme.onPrimary}`}
                  >
                    接受邀請並加入
                  </button>
                </div>
              </div>
            ))
          )
        ) : highlights.length === 0 ? (
          <div className={`py-10 text-center text-xs ${theme.muted}`}>目前沒有未讀通知</div>
        ) : (
          highlights.map((room) => (
            <button
              key={room.roomId}
              type="button"
              onClick={() => {
                onOpenRoom(room.roomId);
                onClose();
              }}
              className={`flex w-full items-center justify-between rounded-2xl border p-3 text-left ${theme.line} ${theme.surface} ${theme.hoverSurface}`}
            >
              <div className="min-w-0">
                <div className="truncate text-xs font-semibold">{room.name}</div>
                <div className={`truncate text-[11px] ${theme.muted}`}>{room.topic}</div>
              </div>
              <span className={`ml-2 shrink-0 rounded-full px-2 py-0.5 text-[10px] font-bold ${theme.primaryBg} ${theme.onPrimary} tabular-nums`}>
                {room.highlightCount > 0 ? `@${room.highlightCount}` : room.unreadCount}
              </span>
            </button>
          ))
        )}
      </div>
    </div>
  );
}

export function PhoneCreateHubModal({
  theme,
  onClose,
  onCreateRoom,
  onCreateSpace,
}: {
  theme: PhoneThemeClasses;
  onClose: () => void;
  onCreateRoom: (name: string, topic: string, encrypted: boolean) => void;
  onCreateSpace: (name: string, topic: string) => void;
}) {
  const [mode, setMode] = useState<'room' | 'space'>('room');
  const [name, setName] = useState('');
  const [topic, setTopic] = useState('');
  const [encrypted, setEncrypted] = useState(true);

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    if (!name.trim()) return;
    if (mode === 'room') {
      onCreateRoom(name, topic, encrypted);
    } else {
      onCreateSpace(name, topic);
    }
    onClose();
  };

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-label="建立聊天室或 Space"
      className="absolute inset-0 z-50 flex items-center justify-center bg-black/60 p-4 backdrop-blur-xs"
    >
      <form
        onSubmit={handleSubmit}
        className={`w-full space-y-3 rounded-2xl border p-4 shadow-xl ${theme.line} ${theme.surface} ${theme.text}`}
      >
        <div className="flex items-center justify-between">
          <h3 className="text-sm font-semibold">建立新項目</h3>
          <button type="button" onClick={onClose} aria-label="關閉建立視窗" className={`p-1 ${theme.muted}`}>
            <X className="h-4 w-4" />
          </button>
        </div>

        <div className={`grid grid-cols-2 gap-1 rounded-xl p-1 text-xs ${theme.surfaceVar}`}>
          <button
            type="button"
            onClick={() => setMode('room')}
            className={`rounded-lg py-1.5 font-medium ${mode === 'room' ? `${theme.primaryBg} ${theme.onPrimary}` : theme.muted}`}
          >
            建立 Room
          </button>
          <button
            type="button"
            onClick={() => setMode('space')}
            className={`rounded-lg py-1.5 font-medium ${mode === 'space' ? `${theme.primaryBg} ${theme.onPrimary}` : theme.muted}`}
          >
            建立 Space
          </button>
        </div>

        <input
          type="text"
          value={name}
          onChange={(e) => setName(e.target.value)}
          placeholder={mode === 'room' ? '頻道名稱 (例：rust-async)' : 'Space 名稱 (例：Mobile Team)'}
          className={`w-full rounded-xl border px-3 py-2 text-xs outline-none ${theme.line} ${theme.surfaceVar}`}
        />
        <input
          type="text"
          value={topic}
          onChange={(e) => setTopic(e.target.value)}
          placeholder="主題說明 (選填)"
          className={`w-full rounded-xl border px-3 py-2 text-xs outline-none ${theme.line} ${theme.surfaceVar}`}
        />
        {mode === 'room' && (
          <label className="flex items-center justify-between text-xs">
            <span>啟用 Megolm 端對端加密</span>
            <input
              type="checkbox"
              checked={encrypted}
              onChange={(e) => setEncrypted(e.target.checked)}
            />
          </label>
        )}
        <div className="flex justify-end gap-2 pt-1">
          <button type="button" onClick={onClose} className={`rounded-lg px-3 py-1.5 text-xs ${theme.muted}`}>
            取消
          </button>
          <button
            type="submit"
            disabled={!name.trim()}
            className={`rounded-lg px-3.5 py-1.5 text-xs font-semibold disabled:opacity-40 ${theme.primaryBg} ${theme.onPrimary}`}
          >
            {mode === 'room' ? '建立並進入' : '建立 Space'}
          </button>
        </div>
      </form>
    </div>
  );
}

export function PhoneEventSourceModal({
  message,
  theme,
  onClose,
}: {
  message: TimelineMessage;
  theme: PhoneThemeClasses;
  onClose: () => void;
}) {
  const jsonText = JSON.stringify(
    {
      event_id: message.eventId,
      room_id: message.roomId,
      sender: message.senderId,
      origin_server_ts: message.timestampMs,
      type: 'm.room.message',
      encrypted: message.isEncrypted,
      content: {
        msgtype: message.attachment ? 'm.image' : 'm.text',
        body: message.body,
      },
    },
    null,
    2
  );

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-label="事件原始碼 View Event Source"
      className="absolute inset-0 z-50 flex items-center justify-center bg-black/60 p-4 backdrop-blur-xs"
    >
      <div className={`w-full space-y-3 rounded-2xl border p-4 shadow-xl ${theme.line} ${theme.surface} ${theme.text}`}>
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-1.5 text-xs font-semibold">
            <Code2 className={`h-4 w-4 ${theme.primaryText}`} />
            <span>Matrix Event Source</span>
          </div>
          <button type="button" onClick={onClose} aria-label="關閉事件原始碼" className={`p-1 ${theme.muted}`}>
            <X className="h-4 w-4" />
          </button>
        </div>
        <pre className={`max-h-60 overflow-auto rounded-xl border p-2.5 font-mono text-[10px] leading-relaxed ${theme.line} ${theme.bg}`}>
          <code>{jsonText}</code>
        </pre>
      </div>
    </div>
  );
}
