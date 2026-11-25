import test from 'node:test';
import assert from 'node:assert/strict';
import {
  acceptRoomInviteFlow,
  colorMXID,
  completeSsoLoginFlow,
  confirmSasVerificationFlow,
  createInitialCinnyState,
  createRoomFlow,
  createSpaceFlow,
  decryptMediaPreviewFlow,
  editMarkdownMessageFlow,
  filterRooms,
  getSpaceUnreadSummary,
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
  updateCinnySettingsFlow,
  updateOwnDisplayNameFlow,
  updateRoomSettingsFlow,
} from './cinnyEngine';

test('getSpaceUnreadSummary aggregates child room unread and highlight counts', () => {
  const state = createInitialCinnyState();
  const cinnySpace = state.spaces[0];
  const rustSpace = state.spaces[1];

  const cinnySummary = getSpaceUnreadSummary(cinnySpace, state.rooms);
  assert.equal(cinnySummary.unread, 3);
  assert.equal(cinnySummary.highlights, 0);

  const rustSummary = getSpaceUnreadSummary(rustSpace, state.rooms);
  assert.equal(rustSummary.unread, 2);
  assert.equal(rustSummary.highlights, 1);
});

test('filterRooms handles Space, DirectMessage, and search queries', () => {
  const state = createInitialCinnyState();
  assert.equal(filterRooms(state.rooms, 'all', '').length, 6);
  assert.equal(filterRooms(state.rooms, 'dm', '').length, 1);
  assert.equal(filterRooms(state.rooms, '!rust-matrix:matrix.org', '').length, 2);
  assert.equal(filterRooms(state.rooms, 'all', 'megolm').length, 1);
});

test('selectRoom clears unread counts and appends UniFFI trace', () => {
  const state = createInitialCinnyState();
  const next = selectRoom(state, '!uniffi-bindings:matrix.org');
  const room = next.rooms.find((r) => r.roomId === '!uniffi-bindings:matrix.org');
  assert.equal(room?.unreadCount, 0);
  assert.equal(room?.highlightCount, 0);
  assert.equal(next.activeRoomId, '!uniffi-bindings:matrix.org');
  assert.equal(next.traces[0].fnName, 'subscribe_room_timeline');
});

test('sendMarkdownMessage and editMarkdownMessageFlow update message timeline', () => {
  const state = createInitialCinnyState();
  const unchanged = sendMarkdownMessage(state, '!cinny-dev:matrix.org', '   ');
  assert.equal(unchanged, state);

  const next = sendMarkdownMessage(
    state,
    '!cinny-dev:matrix.org',
    'Testing Rust UniFFI message',
    '$evt_101'
  );
  const msgs = next.messagesByRoom['!cinny-dev:matrix.org'];
  const last = msgs[msgs.length - 1];
  assert.equal(last.body, 'Testing Rust UniFFI message');
  assert.equal(last.isOwn, true);
  assert.equal(last.replyTo?.eventId, '$evt_101');
  assert.equal(next.traces[0].fnName, 'send_markdown_message');

  const edited = editMarkdownMessageFlow(next, '!cinny-dev:matrix.org', last.eventId, 'Edited message');
  const editedMsg = edited.messagesByRoom['!cinny-dev:matrix.org'].find((m) => m.eventId === last.eventId);
  assert.equal(editedMsg?.body, 'Edited message');
  assert.equal(editedMsg?.isEdited, true);
});

test('toggleReactionOnMessage adds, increments, and removes own reaction', () => {
  const state = createInitialCinnyState();
  const added = toggleReactionOnMessage(state, '!cinny-dev:matrix.org', '$evt_101', '🔥');
  let target = added.messagesByRoom['!cinny-dev:matrix.org'].find((m) => m.eventId === '$evt_101');
  assert.equal(target?.reactions.find((r) => r.key === '🔥')?.count, 1);

  const removed = toggleReactionOnMessage(added, '!cinny-dev:matrix.org', '$evt_101', '🔥');
  target = removed.messagesByRoom['!cinny-dev:matrix.org'].find((m) => m.eventId === '$evt_101');
  assert.equal(target?.reactions.find((r) => r.key === '🔥'), undefined);
});

test('sendThreadReply and redactMessage update timeline state accurately', () => {
  const state = createInitialCinnyState();
  const threaded = sendThreadReply(state, '!cinny-dev:matrix.org', '$evt_101', 'Thread test');
  const root = threaded.messagesByRoom['!cinny-dev:matrix.org'].find((m) => m.eventId === '$evt_101');
  assert.equal(root?.threadReplies.length, 2);

  const redacted = redactMessage(threaded, '!cinny-dev:matrix.org', '$evt_102');
  const evt102 = redacted.messagesByRoom['!cinny-dev:matrix.org'].find((m) => m.eventId === '$evt_102');
  assert.equal(evt102?.isRedacted, true);
});

test('SAS verification transitions from idle -> sas -> confirmed', () => {
  const state = createInitialCinnyState();
  const sas = startSasVerificationFlow(state);
  assert.equal(sas.verification.kind, 'sas');
  if (sas.verification.kind === 'sas') {
    assert.equal(sas.verification.emojis.length, 7);
  }
  const confirmed = confirmSasVerificationFlow(sas, 'TEST_DEV');
  assert.equal(confirmed.verification.kind, 'confirmed');
});

test('Cinny settings, Butter theme, colorMXID, RoomSettings, Create Space, and Invite work', () => {
  let state = createInitialCinnyState();
  state = updateCinnySettingsFlow(state, { themeMode: 'Butter', messageLayout: 'Bubble' });
  assert.equal(state.settings.themeMode, 'Butter');
  assert.equal(state.settings.messageLayout, 'Bubble');
  assert.equal(state.darkTheme, true);

  state = updateCinnySettingsFlow(state, { themeMode: 'Silver' });
  assert.equal(state.darkTheme, false);

  const c1 = colorMXID('@ajbura:matrix.org', true);
  const c2 = colorMXID('@ajbura:matrix.org', true);
  assert.equal(c1, c2);

  state = updateOwnDisplayNameFlow(state, 'Cinny Tester');
  assert.equal(state.displayName, 'Cinny Tester');

  state = updateRoomSettingsFlow(state, '!cinny-dev:matrix.org', {
    name: 'cinny-android-v2',
    notificationLevel: 'Mute',
  });
  const updatedRoom = state.rooms.find((r) => r.roomId === '!cinny-dev:matrix.org');
  assert.equal(updatedRoom?.name, 'cinny-android-v2');
  assert.equal(updatedRoom?.notificationLevel, 'Mute');

  state = createSpaceFlow(state, 'Compose UI', 'Design tokens');
  assert.equal(state.spaces.some((s) => s.name === 'Compose UI'), true);

  state = acceptRoomInviteFlow(state, '!invite-compose:matrix.org');
  const accepted = state.rooms.find((r) => r.roomId === '!invite-compose:matrix.org');
  assert.equal(accepted?.category, 'OrphanRoom');
});

test('simulateIncomingSyncMessage increments unread for inactive room', () => {
  const state = createInitialCinnyState();
  const next = simulateIncomingSyncMessage(
    state,
    '!cinny-design:matrix.org',
    'Mira',
    'New Compose tokens pushed'
  );
  const designRoom = next.rooms.find((r) => r.roomId === '!cinny-design:matrix.org');
  assert.equal(designRoom?.unreadCount, 4);
});

test('SSO login, 4S recovery, encrypted media cache, room creation, discovery, and push notification work end-to-end', () => {
  let state = createInitialCinnyState();
  state = completeSsoLoginFlow(state, 'https://matrix.org', '@sso_user:matrix.org');
  assert.equal(state.authMethod, 'sso');
  assert.equal(state.currentUserId, '@sso_user:matrix.org');

  state = recoverSecretBackupFlow(state, 'EsTX-Demo-4S-Key');
  assert.equal(state.recoveryUnlocked, true);
  const recoveredMsg = state.messagesByRoom['!cinny-dev:matrix.org'].find((m) => m.eventId === '$evt_103_utd');
  assert.equal(recoveredMsg?.isUnableToDecrypt, false);

  state = decryptMediaPreviewFlow(state, '!cinny-dev:matrix.org', '$evt_102');
  const mediaMsg = state.messagesByRoom['!cinny-dev:matrix.org'].find((m) => m.eventId === '$evt_102');
  assert.equal(mediaMsg?.attachment?.isDecrypted, true);

  state = sendMediaAttachmentFlow(state, '!cinny-dev:matrix.org', 'diagram.png', 'image/png');
  assert.equal(state.traces[0].fnName, 'send_media_attachment');

  state = createRoomFlow(state, 'security-audit', 'E2EE audit channel', true, '!cinny-space:matrix.org');
  assert.equal(state.rooms.some((r) => r.name === 'security-audit'), true);

  state = joinDiscoverableSpaceRoom(state, '!cinny-releases:matrix.org');
  assert.equal(state.rooms.some((r) => r.roomId === '!cinny-releases:matrix.org'), true);

  state = simulatePushNotificationFlow(state);
  assert.equal(state.activePushBanner?.isEncrypted, true);
  assert.equal(state.traces[0].fnName, 'CinnyClient::process_push_event');
});
