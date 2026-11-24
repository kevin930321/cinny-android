export type RoomCategory = 'DirectMessage' | 'SpaceChannel' | 'OrphanRoom';

export interface SpaceNode {
  roomId: string;
  name: string;
  shortCode: string;
  topic: string;
  avatarUrl?: string;
  childRoomIds: string[];
}

export interface DiscoverableSpaceRoom {
  roomId: string;
  spaceId: string;
  name: string;
  topic: string;
  memberCount: number;
  isEncrypted: boolean;
  joined: boolean;
}

export interface ReactionBadge {
  key: string;
  count: number;
  reactedByMe: boolean;
}

export interface ReplyPreview {
  eventId: string;
  senderName: string;
  body: string;
}

export interface MediaAttachmentPreview {
  mxcUrl: string;
  filename: string;
  mimeType: string;
  sizeLabel: string;
  isEncrypted: boolean;
  decryptedPreviewUrl?: string;
  isDecrypted: boolean;
}

export interface TimelineMessage {
  eventId: string;
  roomId: string;
  senderId: string;
  senderName: string;
  senderAvatarUrl?: string;
  timestampLabel: string;
  timestampMs: number;
  body: string;
  isEdited: boolean;
  isRedacted: boolean;
  isEncrypted: boolean;
  isUnableToDecrypt?: boolean;
  isOwn: boolean;
  replyTo?: ReplyPreview;
  attachment?: MediaAttachmentPreview;
  threadReplies: TimelineMessage[];
  reactions: ReactionBadge[];
}

export interface RoomSummary {
  roomId: string;
  name: string;
  topic: string;
  isEncrypted: boolean;
  isDirect: boolean;
  category: RoomCategory;
  unreadCount: number;
  highlightCount: number;
  parentSpaceIds: string[];
}

export type VerificationStep =
  | { kind: 'idle' }
  | { kind: 'sas'; emojis: Array<{ symbol: string; label: string }> }
  | { kind: 'confirmed'; deviceId: string };

export interface PushNotificationBanner {
  id: string;
  roomId: string;
  roomName: string;
  senderName: string;
  bodyPreview: string;
  isEncrypted: boolean;
}

export interface UniFfiTraceEntry {
  id: string;
  fnName: string;
  argsSummary: string;
  rustModule: string;
  timestamp: string;
}

export interface CinnyAppState {
  isAuthenticated: boolean;
  authMethod: 'password' | 'sso';
  homeserverUrl: string;
  currentUserId: string;
  darkTheme: boolean;
  recoveryUnlocked: boolean;
  pushEnabled: boolean;
  pushEndpoint: string;
  activePushBanner: PushNotificationBanner | null;
  selectedSpaceFilter: 'all' | 'dm' | string;
  activeRoomId: string | null;
  activeThreadEventId: string | null;
  spaces: SpaceNode[];
  discoverableRooms: DiscoverableSpaceRoom[];
  rooms: RoomSummary[];
  messagesByRoom: Record<string, TimelineMessage[]>;
  verification: VerificationStep;
  traces: UniFfiTraceEntry[];
}

export const SAS_DEMO_EMOJIS = [
  { symbol: '🔐', label: 'Lock' },
  { symbol: '🦀', label: 'Crab' },
  { symbol: '🧭', label: 'Compass' },
  { symbol: '⚡', label: 'Bolt' },
  { symbol: '🛰️', label: 'Satellite' },
  { symbol: '🔑', label: 'Key' },
  { symbol: '🛡️', label: 'Shield' },
];

export function createInitialCinnyState(): CinnyAppState {
  const spaces: SpaceNode[] = [
    {
      roomId: '!cinny-space:matrix.org',
      name: 'Cinny Official',
      shortCode: 'CN',
      topic: 'Minimalist Matrix client community & design system',
      avatarUrl: '/src/assets/images/cinny_space_icon_1795491899191.jpg',
      childRoomIds: ['!cinny-dev:matrix.org', '!cinny-design:matrix.org'],
    },
    {
      roomId: '!rust-matrix:matrix.org',
      name: 'Rust Matrix Core',
      shortCode: 'RS',
      topic: 'matrix-sdk, UniFFI & cargo-ndk native Android engineering',
      avatarUrl: '/src/assets/images/rust_space_icon_1795491900171.jpg',
      childRoomIds: ['!uniffi-bindings:matrix.org', '!crypto-megolm:matrix.org'],
    },
  ];

  const rooms: RoomSummary[] = [
    {
      roomId: '!cinny-dev:matrix.org',
      name: 'cinny-android-dev',
      topic: 'Rust matrix-sdk + Jetpack Compose Material 3 原生移植討論',
      isEncrypted: true,
      isDirect: false,
      category: 'SpaceChannel',
      unreadCount: 0,
      highlightCount: 0,
      parentSpaceIds: ['!cinny-space:matrix.org'],
    },
    {
      roomId: '!cinny-design:matrix.org',
      name: 'folds-ui-tokens',
      topic: 'Cinny 設計系統色票、Space Rail 與行動端單手操作動線',
      isEncrypted: false,
      isDirect: false,
      category: 'SpaceChannel',
      unreadCount: 3,
      highlightCount: 0,
      parentSpaceIds: ['!cinny-space:matrix.org'],
    },
    {
      roomId: '!uniffi-bindings:matrix.org',
      name: 'uniffi-kotlin-bridge',
      topic: 'Tokio async runtime 與 Kotlin StateFlow 零拷貝橋接',
      isEncrypted: true,
      isDirect: false,
      category: 'SpaceChannel',
      unreadCount: 2,
      highlightCount: 1,
      parentSpaceIds: ['!rust-matrix:matrix.org'],
    },
    {
      roomId: '!crypto-megolm:matrix.org',
      name: 'e2ee-megolm-store',
      topic: 'Olm/Megolm SQLite 狀態持久化、4S Recovery 與 SAS 驗證',
      isEncrypted: true,
      isDirect: false,
      category: 'SpaceChannel',
      unreadCount: 0,
      highlightCount: 0,
      parentSpaceIds: ['!rust-matrix:matrix.org'],
    },
    {
      roomId: '!dm-ajbura:matrix.org',
      name: 'Ajay Bura (Cinny)',
      topic: 'Direct encrypted conversation',
      isEncrypted: true,
      isDirect: true,
      category: 'DirectMessage',
      unreadCount: 1,
      highlightCount: 1,
      parentSpaceIds: [],
    },
  ];

  const discoverableRooms: DiscoverableSpaceRoom[] = [
    {
      roomId: '!cinny-releases:matrix.org',
      spaceId: '!cinny-space:matrix.org',
      name: 'cinny-releases',
      topic: 'Cinny Web 與 Android 原生版本更新公告',
      memberCount: 1420,
      isEncrypted: false,
      joined: false,
    },
    {
      roomId: '!rust-unifiedpush:matrix.org',
      spaceId: '!rust-matrix:matrix.org',
      name: 'unifiedpush-android',
      topic: 'WorkManager 與 Rust 背景解密推播實作交流',
      memberCount: 318,
      isEncrypted: true,
      joined: false,
    },
  ];

  const messagesByRoom: Record<string, TimelineMessage[]> = {
    '!cinny-dev:matrix.org': [
      {
        eventId: '$evt_101',
        roomId: '!cinny-dev:matrix.org',
        senderId: '@ajbura:matrix.org',
        senderName: 'Ajay',
        senderAvatarUrl: '/src/assets/images/avatar_developer_1795491900681.jpg',
        timestampLabel: '19:24',
        timestampMs: 1795490640000,
        body: '歡迎來到 Cinny Native Android！現在已完整整合 SSO/OIDC、4S 金鑰備份還原、加密媒體與 UnifiedPush 背景推播。',
        isEdited: false,
        isRedacted: false,
        isEncrypted: true,
        isOwn: false,
        threadReplies: [
          {
            eventId: '$evt_101_t1',
            roomId: '!cinny-dev:matrix.org',
            senderId: '@lin:matrix.org',
            senderName: 'Lin (Rust Core)',
            timestampLabel: '19:26',
            timestampMs: 1795490760000,
            body: 'Rust `TimelineFocus::Thread` 與 `media.rs` AES-CTR 加密附件已全面接通。',
            isEdited: false,
            isRedacted: false,
            isEncrypted: true,
            isOwn: false,
            threadReplies: [],
            reactions: [{ key: '🦀', count: 2, reactedByMe: false }],
          },
        ],
        reactions: [
          { key: '🦀', count: 4, reactedByMe: true },
          { key: '🚀', count: 2, reactedByMe: false },
        ],
      },
      {
        eventId: '$evt_102',
        roomId: '!cinny-dev:matrix.org',
        senderId: '@lin:matrix.org',
        senderName: 'Lin (Rust Core)',
        timestampLabel: '19:31',
        timestampMs: 1795491060000,
        body: '[Image] compose-architecture.png',
        isEdited: false,
        isRedacted: false,
        isEncrypted: true,
        isOwn: false,
        attachment: {
          mxcUrl: 'mxc://matrix.org/cinnyEncryptedImg01',
          filename: 'compose-architecture.png',
          mimeType: 'image/png',
          sizeLabel: '428 KB',
          isEncrypted: true,
          decryptedPreviewUrl: '/src/assets/images/rust_space_icon_1795491900171.jpg',
          isDecrypted: false,
        },
        reactions: [{ key: '👍', count: 3, reactedByMe: false }],
        threadReplies: [],
      },
      {
        eventId: '$evt_103_utd',
        roomId: '!cinny-dev:matrix.org',
        senderId: '@sec:matrix.org',
        senderName: 'Ajay (Old Session)',
        timestampLabel: '19:35',
        timestampMs: 1795491300000,
        body: '等待 Megolm 加密金鑰中…（請於安全中心輸入 4S Recovery Key 解鎖歷史訊息）',
        isEdited: false,
        isRedacted: false,
        isEncrypted: true,
        isUnableToDecrypt: true,
        isOwn: false,
        threadReplies: [],
        reactions: [],
      },
    ],
    '!cinny-design:matrix.org': [
      {
        eventId: '$evt_201',
        roomId: '!cinny-design:matrix.org',
        senderId: '@mira:matrix.org',
        senderName: 'Mira',
        timestampLabel: '18:50',
        timestampMs: 1795488600000,
        body: 'Jetpack Compose Material 3 的色彩表已對齊 Cinny 的高對比淺色與暗黑 Onyx 模式。',
        isEdited: false,
        isRedacted: false,
        isEncrypted: false,
        isOwn: false,
        threadReplies: [],
        reactions: [{ key: '✨', count: 2, reactedByMe: false }],
      },
    ],
    '!uniffi-bindings:matrix.org': [
      {
        eventId: '$evt_301',
        roomId: '!uniffi-bindings:matrix.org',
        senderId: '@lin:matrix.org',
        senderName: 'Lin (Rust Core)',
        timestampLabel: '19:10',
        timestampMs: 1795489800000,
        body: '已啟用 `uniffi = { version = "0.29", features = ["tokio"] }`，涵蓋 SSO Token、4S 備份、Thread 與 Media 下載快取。',
        isEdited: true,
        isRedacted: false,
        isEncrypted: true,
        isOwn: false,
        threadReplies: [],
        reactions: [{ key: '⚡', count: 5, reactedByMe: true }],
      },
    ],
    '!crypto-megolm:matrix.org': [
      {
        eventId: '$evt_401',
        roomId: '!crypto-megolm:matrix.org',
        senderId: '@sec:matrix.org',
        senderName: 'Security Bot',
        timestampLabel: '17:45',
        timestampMs: 1795484700000,
        body: 'Cross-signing 與 4S 金鑰備份已整合至 Rust CryptoStore。開啟安全中心可測試 SAS 驗證與 4S 金鑰解鎖。',
        isEdited: false,
        isRedacted: false,
        isEncrypted: true,
        isOwn: false,
        threadReplies: [],
        reactions: [],
      },
    ],
    '!dm-ajbura:matrix.org': [
      {
        eventId: '$evt_501',
        roomId: '!dm-ajbura:matrix.org',
        senderId: '@ajbura:matrix.org',
        senderName: 'Ajay',
        senderAvatarUrl: '/src/assets/images/avatar_developer_1795491900681.jpg',
        timestampLabel: '19:40',
        timestampMs: 1795491600000,
        body: '你可以直接點右上角的「下載專案 ZIP」取得完整 v0.3.0 Cargo + Gradle 原生專案。',
        isEdited: false,
        isRedacted: false,
        isEncrypted: true,
        isOwn: false,
        threadReplies: [],
        reactions: [{ key: '🎉', count: 1, reactedByMe: false }],
      },
    ],
  };

  return {
    isAuthenticated: true,
    authMethod: 'password',
    homeserverUrl: 'https://matrix.org',
    currentUserId: '@you:matrix.org',
    darkTheme: false,
    recoveryUnlocked: false,
    pushEnabled: true,
    pushEndpoint: 'https://ntfy.sh/cinny-up-demo-984c',
    activePushBanner: null,
    selectedSpaceFilter: '!cinny-space:matrix.org',
    activeRoomId: '!cinny-dev:matrix.org',
    activeThreadEventId: null,
    spaces,
    discoverableRooms,
    rooms,
    messagesByRoom,
    verification: { kind: 'idle' },
    traces: [
      {
        id: 'tr-init-1',
        fnName: 'CinnyClient::start_sync',
        argsSummary: 'homeserver="https://matrix.org"',
        rustModule: 'client.rs',
        timestamp: '19:44:01',
      },
      {
        id: 'tr-init-2',
        fnName: 'build_space_hierarchy_from_items',
        argsSummary: 'spaces=2, rooms=5',
        rustModule: 'spaces.rs',
        timestamp: '19:44:02',
      },
      {
        id: 'tr-init-3',
        fnName: 'subscribe_room_timeline',
        argsSummary: 'room_id="!cinny-dev:matrix.org"',
        rustModule: 'timeline.rs',
        timestamp: '19:44:02',
      },
    ],
  };
}

export function getSpaceUnreadSummary(
  space: SpaceNode,
  rooms: RoomSummary[]
): { unread: number; highlights: number } {
  let unread = 0;
  let highlights = 0;
  for (const room of rooms) {
    if (space.childRoomIds.includes(room.roomId)) {
      unread += room.unreadCount;
      highlights += room.highlightCount;
    }
  }
  return { unread, highlights };
}

export function filterRooms(
  rooms: RoomSummary[],
  spaceFilter: 'all' | 'dm' | string,
  searchQuery: string
): RoomSummary[] {
  const q = searchQuery.trim().toLowerCase();
  return rooms.filter((room) => {
    const matchesSpace =
      spaceFilter === 'all'
        ? true
        : spaceFilter === 'dm'
          ? room.category === 'DirectMessage'
          : room.parentSpaceIds.includes(spaceFilter);
    const matchesQuery =
      q.length === 0 ||
      room.name.toLowerCase().includes(q) ||
      room.topic.toLowerCase().includes(q);
    return matchesSpace && matchesQuery;
  });
}

function appendTrace(
  traces: UniFfiTraceEntry[],
  fnName: string,
  argsSummary: string,
  rustModule: string,
  timestamp: string
): UniFfiTraceEntry[] {
  const next: UniFfiTraceEntry = {
    id: `tr-${Date.now()}-${traces.length + 1}`,
    fnName,
    argsSummary,
    rustModule,
    timestamp,
  };
  return [next, ...traces].slice(0, 14);
}

export function completeSsoLoginFlow(
  state: CinnyAppState,
  homeserverUrl: string,
  username: string,
  nowLabel = '19:45'
): CinnyAppState {
  return {
    ...state,
    isAuthenticated: true,
    authMethod: 'sso',
    homeserverUrl,
    currentUserId: username,
    traces: appendTrace(
      state.traces,
      'CinnyClient::login_with_sso_token',
      `homeserver="${homeserverUrl}", callback="cinny://sso"`,
      'client.rs',
      nowLabel
    ),
  };
}

export function selectRoom(state: CinnyAppState, roomId: string, nowLabel = '19:45'): CinnyAppState {
  const rooms = state.rooms.map((r) =>
    r.roomId === roomId ? { ...r, unreadCount: 0, highlightCount: 0 } : r
  );
  return {
    ...state,
    activeRoomId: roomId,
    activeThreadEventId: null,
    activePushBanner:
      state.activePushBanner?.roomId === roomId ? null : state.activePushBanner,
    rooms,
    traces: appendTrace(
      state.traces,
      'subscribe_room_timeline',
      `room_id="${roomId}"`,
      'timeline.rs',
      nowLabel
    ),
  };
}

export function createRoomFlow(
  state: CinnyAppState,
  name: string,
  topic: string,
  isEncrypted: boolean,
  spaceId: string | null,
  nowLabel = '19:45'
): CinnyAppState {
  const trimmed = name.trim();
  if (!trimmed) return state;
  const newRoomId = `!room_${Date.now()}:matrix.org`;
  const parentSpaceIds =
    spaceId && spaceId !== 'all' && spaceId !== 'dm' ? [spaceId] : [];
  const created: RoomSummary = {
    roomId: newRoomId,
    name: trimmed,
    topic: topic.trim() || '新建 Matrix 討論室',
    isEncrypted,
    isDirect: false,
    category: parentSpaceIds.length > 0 ? 'SpaceChannel' : 'OrphanRoom',
    unreadCount: 0,
    highlightCount: 0,
    parentSpaceIds,
  };
  const updatedSpaces = state.spaces.map((sp) =>
    parentSpaceIds.includes(sp.roomId)
      ? { ...sp, childRoomIds: [...sp.childRoomIds, newRoomId] }
      : sp
  );

  return {
    ...state,
    spaces: updatedSpaces,
    rooms: [created, ...state.rooms],
    messagesByRoom: {
      ...state.messagesByRoom,
      [newRoomId]: [],
    },
    activeRoomId: newRoomId,
    traces: appendTrace(
      state.traces,
      'CinnyClient::create_room',
      `name="${trimmed}", e2ee=${isEncrypted}`,
      'spaces.rs',
      nowLabel
    ),
  };
}

export function joinDiscoverableSpaceRoom(
  state: CinnyAppState,
  roomId: string,
  nowLabel = '19:45'
): CinnyAppState {
  const target = state.discoverableRooms.find((d) => d.roomId === roomId);
  if (!target || target.joined) return state;

  const newSummary: RoomSummary = {
    roomId: target.roomId,
    name: target.name,
    topic: target.topic,
    isEncrypted: target.isEncrypted,
    isDirect: false,
    category: 'SpaceChannel',
    unreadCount: 0,
    highlightCount: 0,
    parentSpaceIds: [target.spaceId],
  };

  return {
    ...state,
    discoverableRooms: state.discoverableRooms.map((d) =>
      d.roomId === roomId ? { ...d, joined: true } : d
    ),
    spaces: state.spaces.map((sp) =>
      sp.roomId === target.spaceId
        ? { ...sp, childRoomIds: [...sp.childRoomIds, target.roomId] }
        : sp
    ),
    rooms: [...state.rooms, newSummary],
    messagesByRoom: {
      ...state.messagesByRoom,
      [target.roomId]: [
        {
          eventId: `$evt_join_${Date.now()}`,
          roomId: target.roomId,
          senderId: '@bot:matrix.org',
          senderName: 'Space Directory',
          timestampLabel: nowLabel,
          timestampMs: Date.now(),
          body: `已加入 #${target.name}（${target.memberCount} 位成員）`,
          isEdited: false,
          isRedacted: false,
          isEncrypted: target.isEncrypted,
          isOwn: false,
          threadReplies: [],
          reactions: [],
        },
      ],
    },
    traces: appendTrace(
      state.traces,
      'CinnyClient::join_room',
      `room_id="${roomId}"`,
      'spaces.rs',
      nowLabel
    ),
  };
}

export function sendMarkdownMessage(
  state: CinnyAppState,
  roomId: string,
  markdown: string,
  replyToEventId?: string | null,
  nowLabel = '19:45'
): CinnyAppState {
  const trimmed = markdown.trim();
  if (!trimmed) return state;

  const existing = state.messagesByRoom[roomId] ?? [];
  const targetReply = replyToEventId
    ? existing.find((m) => m.eventId === replyToEventId)
    : undefined;
  const room = state.rooms.find((r) => r.roomId === roomId);

  const newMessage: TimelineMessage = {
    eventId: `$evt_${Date.now()}_${existing.length + 1}`,
    roomId,
    senderId: state.currentUserId,
    senderName: 'You',
    timestampLabel: nowLabel,
    timestampMs: Date.now(),
    body: trimmed,
    isEdited: false,
    isRedacted: false,
    isEncrypted: room?.isEncrypted ?? true,
    isOwn: true,
    replyTo: targetReply
      ? {
          eventId: targetReply.eventId,
          senderName: targetReply.senderName,
          body: targetReply.body,
        }
      : undefined,
    threadReplies: [],
    reactions: [],
  };

  return {
    ...state,
    messagesByRoom: {
      ...state.messagesByRoom,
      [roomId]: [...existing, newMessage],
    },
    traces: appendTrace(
      state.traces,
      'send_markdown_message',
      `room_id="${roomId}", reply=${replyToEventId ? 'Some' : 'None'}`,
      'timeline.rs',
      nowLabel
    ),
  };
}

export function sendMediaAttachmentFlow(
  state: CinnyAppState,
  roomId: string,
  filename: string,
  mimeType: string,
  nowLabel = '19:45'
): CinnyAppState {
  const existing = state.messagesByRoom[roomId] ?? [];
  const room = state.rooms.find((r) => r.roomId === roomId);
  const isEncrypted = room?.isEncrypted ?? true;
  const newMessage: TimelineMessage = {
    eventId: `$evt_media_${Date.now()}_${existing.length + 1}`,
    roomId,
    senderId: state.currentUserId,
    senderName: 'You',
    timestampLabel: nowLabel,
    timestampMs: Date.now(),
    body: `[Image] ${filename}`,
    isEdited: false,
    isRedacted: false,
    isEncrypted,
    isOwn: true,
    attachment: {
      mxcUrl: `mxc://matrix.org/rustUpload_${Date.now()}`,
      filename,
      mimeType,
      sizeLabel: '312 KB',
      isEncrypted,
      decryptedPreviewUrl: '/src/assets/images/cinny_space_icon_1795491899191.jpg',
      isDecrypted: true,
    },
    threadReplies: [],
    reactions: [],
  };

  return {
    ...state,
    messagesByRoom: {
      ...state.messagesByRoom,
      [roomId]: [...existing, newMessage],
    },
    traces: appendTrace(
      state.traces,
      'send_media_attachment',
      `file="${filename}", aes_ctr=${isEncrypted}`,
      'media.rs',
      nowLabel
    ),
  };
}

export function decryptMediaPreviewFlow(
  state: CinnyAppState,
  roomId: string,
  eventId: string,
  nowLabel = '19:45'
): CinnyAppState {
  const existing = state.messagesByRoom[roomId] ?? [];
  const updated = existing.map((msg) =>
    msg.eventId === eventId && msg.attachment
      ? { ...msg, attachment: { ...msg.attachment, isDecrypted: true } }
      : msg
  );

  return {
    ...state,
    messagesByRoom: {
      ...state.messagesByRoom,
      [roomId]: updated,
    },
    traces: appendTrace(
      state.traces,
      'download_media_to_cache',
      `event_id="${eventId}", cache="media_cache/"`,
      'media.rs',
      nowLabel
    ),
  };
}

export function sendThreadReply(
  state: CinnyAppState,
  roomId: string,
  rootEventId: string,
  markdown: string,
  nowLabel = '19:45'
): CinnyAppState {
  const trimmed = markdown.trim();
  if (!trimmed) return state;
  const existing = state.messagesByRoom[roomId] ?? [];
  const room = state.rooms.find((r) => r.roomId === roomId);

  const updated = existing.map((msg) => {
    if (msg.eventId !== rootEventId) return msg;
    const reply: TimelineMessage = {
      eventId: `$evt_thread_${Date.now()}_${msg.threadReplies.length + 1}`,
      roomId,
      senderId: state.currentUserId,
      senderName: 'You',
      timestampLabel: nowLabel,
      timestampMs: Date.now(),
      body: trimmed,
      isEdited: false,
      isRedacted: false,
      isEncrypted: room?.isEncrypted ?? true,
      isOwn: true,
      threadReplies: [],
      reactions: [],
    };
    return {
      ...msg,
      threadReplies: [...msg.threadReplies, reply],
    };
  });

  return {
    ...state,
    messagesByRoom: {
      ...state.messagesByRoom,
      [roomId]: updated,
    },
    traces: appendTrace(
      state.traces,
      'send_thread_reply',
      `root="${rootEventId}"`,
      'timeline.rs',
      nowLabel
    ),
  };
}

export function toggleReactionOnMessage(
  state: CinnyAppState,
  roomId: string,
  eventId: string,
  emoji: string,
  nowLabel = '19:45'
): CinnyAppState {
  const existing = state.messagesByRoom[roomId] ?? [];
  const updated = existing.map((msg) => {
    if (msg.eventId !== eventId) return msg;
    const found = msg.reactions.find((r) => r.key === emoji);
    let nextReactions: ReactionBadge[];
    if (found) {
      if (found.reactedByMe) {
        nextReactions = msg.reactions
          .map((r) =>
            r.key === emoji ? { ...r, count: r.count - 1, reactedByMe: false } : r
          )
          .filter((r) => r.count > 0);
      } else {
        nextReactions = msg.reactions.map((r) =>
          r.key === emoji ? { ...r, count: r.count + 1, reactedByMe: true } : r
        );
      }
    } else {
      nextReactions = [...msg.reactions, { key: emoji, count: 1, reactedByMe: true }];
    }
    return { ...msg, reactions: nextReactions };
  });

  return {
    ...state,
    messagesByRoom: {
      ...state.messagesByRoom,
      [roomId]: updated,
    },
    traces: appendTrace(
      state.traces,
      'toggle_reaction',
      `event="${eventId}", key="${emoji}"`,
      'timeline.rs',
      nowLabel
    ),
  };
}

export function redactMessage(
  state: CinnyAppState,
  roomId: string,
  eventId: string,
  nowLabel = '19:45'
): CinnyAppState {
  const existing = state.messagesByRoom[roomId] ?? [];
  const updated = existing.map((msg) =>
    msg.eventId === eventId
      ? { ...msg, body: '此訊息已由發送者撤回', isRedacted: true, reactions: [] }
      : msg
  );
  return {
    ...state,
    messagesByRoom: {
      ...state.messagesByRoom,
      [roomId]: updated,
    },
    traces: appendTrace(
      state.traces,
      'redact_message',
      `event_id="${eventId}"`,
      'timeline.rs',
      nowLabel
    ),
  };
}

export function recoverSecretBackupFlow(
  state: CinnyAppState,
  recoveryKey: string,
  nowLabel = '19:45'
): CinnyAppState {
  if (!recoveryKey.trim()) return state;
  const nextMessages: Record<string, TimelineMessage[]> = {};
  for (const [roomId, msgs] of Object.entries(state.messagesByRoom)) {
    nextMessages[roomId] = msgs.map((m) =>
      m.isUnableToDecrypt
        ? {
            ...m,
            isUnableToDecrypt: false,
            body: '已透過 4S 金鑰備份成功解密此歷史 Megolm 訊息！',
          }
        : m
    );
  }
  return {
    ...state,
    recoveryUnlocked: true,
    messagesByRoom: nextMessages,
    traces: appendTrace(
      state.traces,
      'recover_with_security_key',
      '4s_backup=unlocked, decrypted_history=true',
      'crypto.rs',
      nowLabel
    ),
  };
}

export function startSasVerificationFlow(
  state: CinnyAppState,
  nowLabel = '19:45'
): CinnyAppState {
  return {
    ...state,
    verification: { kind: 'sas', emojis: SAS_DEMO_EMOJIS },
    traces: appendTrace(
      state.traces,
      'verify_device_sas',
      'user="@ajbura:matrix.org", sas=7_emojis',
      'crypto.rs',
      nowLabel
    ),
  };
}

export function confirmSasVerificationFlow(
  state: CinnyAppState,
  deviceId = 'CINNY_ANDROID_RUST_01',
  nowLabel = '19:45'
): CinnyAppState {
  return {
    ...state,
    verification: { kind: 'confirmed', deviceId },
    traces: appendTrace(
      state.traces,
      'confirm_sas_verification',
      `device_id="${deviceId}", verified=true`,
      'crypto.rs',
      nowLabel
    ),
  };
}

export function simulateIncomingSyncMessage(
  state: CinnyAppState,
  roomId: string,
  senderName: string,
  body: string,
  nowLabel = '19:46'
): CinnyAppState {
  const existing = state.messagesByRoom[roomId] ?? [];
  const incoming: TimelineMessage = {
    eventId: `$evt_sync_${Date.now()}_${existing.length + 1}`,
    roomId,
    senderId: `@${senderName.toLowerCase()}:matrix.org`,
    senderName,
    timestampLabel: nowLabel,
    timestampMs: Date.now(),
    body,
    isEdited: false,
    isRedacted: false,
    isEncrypted: true,
    isOwn: false,
    threadReplies: [],
    reactions: [],
  };

  const rooms = state.rooms.map((r) =>
    r.roomId === roomId && state.activeRoomId !== roomId
      ? { ...r, unreadCount: r.unreadCount + 1 }
      : r
  );

  return {
    ...state,
    rooms,
    messagesByRoom: {
      ...state.messagesByRoom,
      [roomId]: [...existing, incoming],
    },
    traces: appendTrace(
      state.traces,
      'SyncObserver::on_spaces_updated',
      `room_id="${roomId}", delta=+1_event`,
      'client.rs',
      nowLabel
    ),
  };
}

export function simulatePushNotificationFlow(
  state: CinnyAppState,
  roomId = '!uniffi-bindings:matrix.org',
  senderName = 'Lin (Rust Core)',
  bodyPreview = 'PushSyncWorker 已在背景解密新訊息！',
  nowLabel = '19:47'
): CinnyAppState {
  const targetRoom = state.rooms.find((r) => r.roomId === roomId);
  const next = simulateIncomingSyncMessage(state, roomId, senderName, bodyPreview, nowLabel);
  return {
    ...next,
    activePushBanner: {
      id: `push-${Date.now()}`,
      roomId,
      roomName: targetRoom?.name ?? roomId,
      senderName,
      bodyPreview,
      isEncrypted: targetRoom?.isEncrypted ?? true,
    },
    traces: appendTrace(
      next.traces,
      'CinnyClient::process_push_event',
      `room_id="${roomId}", decrypted=true`,
      'push.rs',
      nowLabel
    ),
  };
}
