import { consola } from 'consola';
import { getAllDevices, getDevice } from '../db/index.js';
import {
  getConnectedDevices,
  isDeviceOnline,
  sendCommand,
} from '../websocket/index.js';
import { parseNaturalLanguageQuery } from '../util/queryParser.js';
import {
  AnalyzeStorageSchema,
  ClickByIdSchema,
  ClickByTextSchema,
  DeleteFileSchema,
  DeleteAlarmSchema,
  DismissAlarmSchema,
  ExecuteShellSchema,
  FindElementSchema,
  FindLargeFilesSchema,
  GetAlarmsSchema,
  GetBatterySchema,
  GetClipboardSchema,
  GetDeviceInfoSchema,
  GetLocationSchema,
  GetScreenHierarchySchema,
  GetVolumeSchema,
  GlobalActionSchema,
  IndexMediaMetadataSchema,
  InputGestureSchema,
  InputTextSchema,
  LaunchIntentSchema,
  ListFilesSchema,
  ListPackagesSchema,
  MakeCallSchema,
  MakeCallWithVoiceSchema,
  PlayAudioSchema,
  PostNotificationSchema,
  ReadFileSchema,
  ReadNotificationsSchema,
  ReadSmsSchema,
  RecordVideoSchema,
  SearchContactsSchema,
  SearchMediaSchema,
  SendSmsSchema,
  SetAlarmSchema,
  SetClipboardSchema,
  SetVolumeSchema,
  ShowOverlaySchema,
  ShowToastSchema,
  SpeakTtsSchema,
  StopAudioSchema,
  TakePhotoSchema,
  TakeScreenshotSchema,
  VibrateSchema,
  WriteFileSchema,
} from './tools.js';

export interface ToolResult {
  content: Array<{
    type: 'text' | 'image';
    text?: string;
    data?: string;
    mimeType?: string;
  }>;
  isError?: boolean;
}

function textResult(text: string, isError = false): ToolResult {
  return {
    content: [{ type: 'text', text }],
    isError,
  };
}

function jsonResult(data: unknown, isError = false): ToolResult {
  return {
    content: [{ type: 'text', text: JSON.stringify(data, null, 2) }],
    isError,
  };
}

function imageResult(base64: string, mimeType = 'image/png'): ToolResult {
  return {
    content: [{ type: 'image', data: base64, mimeType }],
  };
}

function errorResult(message: string): ToolResult {
  return textResult(`Error: ${message}`, true);
}

export async function handleToolCall(
  name: string,
  args: Record<string, unknown>
): Promise<ToolResult> {
  try {
    switch (name) {
      case 'aster_list_devices':
        return handleListDevices();

      case 'aster_get_device_info':
        return handleGetDeviceInfo(args);

      case 'aster_read_notifications':
        return handleReadNotifications(args);

      case 'aster_read_sms':
        return handleReadSms(args);

      case 'aster_send_sms':
        return handleSendSms(args);

      case 'aster_get_location':
        return handleGetLocation(args);

      case 'aster_execute_shell':
        return handleExecuteShell(args);

      case 'aster_list_packages':
        return handleListPackages(args);

      case 'aster_list_files':
        return handleListFiles(args);

      case 'aster_read_file':
        return handleReadFile(args);

      case 'aster_write_file':
        return handleWriteFile(args);

      case 'aster_delete_file':
        return handleDeleteFile(args);

      case 'aster_take_screenshot':
        return handleTakeScreenshot(args);

      case 'aster_get_screen_hierarchy':
        return handleGetScreenHierarchy(args);

      case 'aster_input_gesture':
        return handleInputGesture(args);

      case 'aster_input_text':
        return handleInputText(args);

      case 'aster_global_action':
        return handleGlobalAction(args);

      case 'aster_speak_tts':
        return handleSpeakTts(args);

      case 'aster_get_battery':
        return handleGetBattery(args);

      case 'aster_show_overlay':
        return handleShowOverlay(args);

      case 'aster_vibrate':
        return handleVibrate(args);

      case 'aster_play_audio':
        return handlePlayAudio(args);

      case 'aster_post_notification':
        return handlePostNotification(args);

      case 'aster_make_call':
        return handleMakeCall(args);

      case 'aster_make_call_with_voice':
        return handleMakeCallWithVoice(args);

      case 'aster_set_clipboard':
        return handleSetClipboard(args);

      case 'aster_get_clipboard':
        return handleGetClipboard(args);

      case 'aster_show_toast':
        return handleShowToast(args);

      case 'aster_launch_intent':
        return handleLaunchIntent(args);

      case 'aster_find_element':
        return handleFindElement(args);

      case 'aster_click_by_text':
        return handleClickByText(args);

      case 'aster_click_by_id':
        return handleClickById(args);

      case 'aster_analyze_storage':
        return handleAnalyzeStorage(args);

      case 'aster_find_large_files':
        return handleFindLargeFiles(args);

      case 'aster_index_media_metadata':
        return handleIndexMediaMetadata(args);

      case 'aster_search_media':
        return handleSearchMedia(args);

      case 'aster_stop_audio':
        return handleStopAudio(args);

      case 'aster_get_volume':
        return handleGetVolume(args);

      case 'aster_set_volume':
        return handleSetVolume(args);

      case 'aster_search_contacts':
        return handleSearchContacts(args);

      case 'aster_get_alarms':
        return handleGetAlarms(args);

      case 'aster_set_alarm':
        return handleSetAlarm(args);

      case 'aster_dismiss_alarm':
        return handleDismissAlarm(args);

      case 'aster_delete_alarm':
        return handleDeleteAlarm(args);

      case 'aster_take_photo':
        return handleTakePhoto(args);

      case 'aster_record_video':
        return handleRecordVideo(args);

      default:
        return errorResult(`Unknown tool: ${name}`);
    }
  } catch (error) {
    consola.error(`Tool ${name} error:`, error);
    return errorResult(error instanceof Error ? error.message : String(error));
  }
}

// Tool handlers

function handleListDevices(): ToolResult {
  const devices = getAllDevices();
  const connected = getConnectedDevices();

  const result = devices.map(device => ({
    id: device.id,
    name: device.name,
    model: device.model,
    platform: device.platform,
    status: device.status,
    online: connected.has(device.id),
    lastSeen: new Date(device.lastSeen).toISOString(),
  }));

  return jsonResult({ devices: result, count: result.length });
}

async function handleGetDeviceInfo(args: Record<string, unknown>): Promise<ToolResult> {
  const { deviceId } = GetDeviceInfoSchema.parse(args);
  const device = getDevice(deviceId);

  if (!device) {
    return errorResult(`Device ${deviceId} not found`);
  }

  if (!isDeviceOnline(deviceId)) {
    return jsonResult({
      ...device,
      online: false,
      message: 'Device is offline. Connect the device to get live info.',
    });
  }

  const response = await sendCommand(deviceId, 'get_device_info');
  return jsonResult({
    ...device,
    online: true,
    liveInfo: response.data,
  });
}

async function handleReadNotifications(args: Record<string, unknown>): Promise<ToolResult> {
  const { deviceId, limit } = ReadNotificationsSchema.parse(args);
  const response = await sendCommand(deviceId, 'read_notifications', { limit });
  return jsonResult(response.data);
}

async function handleReadSms(args: Record<string, unknown>): Promise<ToolResult> {
  const { deviceId, limit, threadId } = ReadSmsSchema.parse(args);
  const response = await sendCommand(deviceId, 'read_sms', { limit, threadId });
  return jsonResult(response.data);
}

async function handleGetLocation(args: Record<string, unknown>): Promise<ToolResult> {
  const { deviceId } = GetLocationSchema.parse(args);
  const response = await sendCommand(deviceId, 'get_location');
  return jsonResult(response.data);
}

async function handleExecuteShell(args: Record<string, unknown>): Promise<ToolResult> {
  const { deviceId, command } = ExecuteShellSchema.parse(args);
  const response = await sendCommand(deviceId, 'execute_shell', { command });
  return jsonResult(response.data);
}

async function handleListPackages(args: Record<string, unknown>): Promise<ToolResult> {
  const { deviceId, includeSystem } = ListPackagesSchema.parse(args);
  const response = await sendCommand(deviceId, 'list_packages', { includeSystem });
  return jsonResult(response.data);
}

async function handleListFiles(args: Record<string, unknown>): Promise<ToolResult> {
  const { deviceId, path } = ListFilesSchema.parse(args);
  const response = await sendCommand(deviceId, 'list_files', { path });
  return jsonResult(response.data);
}

async function handleReadFile(args: Record<string, unknown>): Promise<ToolResult> {
  const { deviceId, path } = ReadFileSchema.parse(args);
  const response = await sendCommand(deviceId, 'read_file', { path });
  return jsonResult(response.data);
}

async function handleWriteFile(args: Record<string, unknown>): Promise<ToolResult> {
  const { deviceId, path, content } = WriteFileSchema.parse(args);
  const response = await sendCommand(deviceId, 'write_file', { path, content });
  return jsonResult(response.data);
}

async function handleDeleteFile(args: Record<string, unknown>): Promise<ToolResult> {
  const { deviceId, path } = DeleteFileSchema.parse(args);
  const response = await sendCommand(deviceId, 'delete_file', { path });
  return jsonResult(response.data);
}

async function handleTakeScreenshot(args: Record<string, unknown>): Promise<ToolResult> {
  const { deviceId } = TakeScreenshotSchema.parse(args);
  const response = await sendCommand(deviceId, 'take_screenshot');
  const data = response.data as { screenshot?: string; base64?: string; format?: string; mimeType?: string };
  // Android returns 'screenshot' field, but also support 'base64' for backward compatibility
  const base64Data = data.screenshot || data.base64;
  if (!base64Data) {
    return errorResult('Screenshot data not found in response');
  }
  const mimeType = data.mimeType || (data.format ? `image/${data.format}` : 'image/png');
  return imageResult(base64Data, mimeType);
}

async function handleGetScreenHierarchy(args: Record<string, unknown>): Promise<ToolResult> {
  const { deviceId, mode, maxDepth, includeInvisible, searchText } = GetScreenHierarchySchema.parse(args);
  const response = await sendCommand(deviceId, 'get_screen_hierarchy', {
    mode,
    maxDepth,
    includeInvisible,
    searchText,
  });
  return jsonResult(response.data);
}

async function handleInputGesture(args: Record<string, unknown>): Promise<ToolResult> {
  const { deviceId, gestureType, points, duration } = InputGestureSchema.parse(args);
  const response = await sendCommand(deviceId, 'input_gesture', { gestureType, points, duration });
  return jsonResult(response.data);
}

async function handleInputText(args: Record<string, unknown>): Promise<ToolResult> {
  const { deviceId, text } = InputTextSchema.parse(args);
  const response = await sendCommand(deviceId, 'input_text', { text });
  return jsonResult(response.data);
}

async function handleGlobalAction(args: Record<string, unknown>): Promise<ToolResult> {
  const { deviceId, action } = GlobalActionSchema.parse(args);
  const response = await sendCommand(deviceId, 'global_action', { action });
  return jsonResult(response.data);
}

async function handleSpeakTts(args: Record<string, unknown>): Promise<ToolResult> {
  const { deviceId, text } = SpeakTtsSchema.parse(args);
  const response = await sendCommand(deviceId, 'speak_tts', { text });
  return jsonResult(response.data);
}

async function handleGetBattery(args: Record<string, unknown>): Promise<ToolResult> {
  const { deviceId } = GetBatterySchema.parse(args);
  const response = await sendCommand(deviceId, 'get_battery');
  return jsonResult(response.data);
}

async function handleShowOverlay(args: Record<string, unknown>): Promise<ToolResult> {
  const { deviceId, url, html, showCloseButton, timeout } = ShowOverlaySchema.parse(args);
  const response = await sendCommand(deviceId, 'show_overlay', { url, html, showCloseButton, timeout });
  return jsonResult(response.data);
}

async function handleVibrate(args: Record<string, unknown>): Promise<ToolResult> {
  const { deviceId, pattern } = VibrateSchema.parse(args);
  const response = await sendCommand(deviceId, 'vibrate', { pattern });
  return jsonResult(response.data);
}

async function handlePlayAudio(args: Record<string, unknown>): Promise<ToolResult> {
  const { deviceId, source } = PlayAudioSchema.parse(args);
  const response = await sendCommand(deviceId, 'play_audio', { source });
  return jsonResult(response.data);
}

async function handlePostNotification(args: Record<string, unknown>): Promise<ToolResult> {
  const { deviceId, title, body, actions } = PostNotificationSchema.parse(args);
  const response = await sendCommand(deviceId, 'post_notification', { title, body, actions });
  return jsonResult(response.data);
}

async function handleSendSms(args: Record<string, unknown>): Promise<ToolResult> {
  const { deviceId, number, message } = SendSmsSchema.parse(args);
  const response = await sendCommand(deviceId, 'send_sms', { number, message });
  return jsonResult(response.data);
}

async function handleMakeCall(args: Record<string, unknown>): Promise<ToolResult> {
  const { deviceId, number } = MakeCallSchema.parse(args);
  const response = await sendCommand(deviceId, 'make_call', { number });
  return jsonResult(response.data);
}

async function handleMakeCallWithVoice(args: Record<string, unknown>): Promise<ToolResult> {
  const { deviceId, number, text, waitSeconds } = MakeCallWithVoiceSchema.parse(args);
  const timeout = ((waitSeconds || 8) + 2 + 60) * 1000; // 2s dialer + wait + 60s for TTS and overhead
  const response = await sendCommand(deviceId, 'make_call_with_voice', { number, text, waitSeconds }, timeout);
  return jsonResult(response.data);
}

async function handleSetClipboard(args: Record<string, unknown>): Promise<ToolResult> {
  const { deviceId, text } = SetClipboardSchema.parse(args);
  const response = await sendCommand(deviceId, 'set_clipboard', { text });
  return jsonResult(response.data);
}

async function handleGetClipboard(args: Record<string, unknown>): Promise<ToolResult> {
  const { deviceId } = GetClipboardSchema.parse(args);
  const response = await sendCommand(deviceId, 'get_clipboard');
  return jsonResult(response.data);
}

async function handleShowToast(args: Record<string, unknown>): Promise<ToolResult> {
  const { deviceId, message, duration } = ShowToastSchema.parse(args);
  const response = await sendCommand(deviceId, 'show_toast', { message, duration });
  return jsonResult(response.data);
}

async function handleLaunchIntent(args: Record<string, unknown>): Promise<ToolResult> {
  const { deviceId, packageName, action, data } = LaunchIntentSchema.parse(args);
  // Android expects 'package' parameter, not 'packageName'
  const response = await sendCommand(deviceId, 'launch_intent', { package: packageName, action, data });
  return jsonResult(response.data);
}

async function handleFindElement(args: Record<string, unknown>): Promise<ToolResult> {
  const { deviceId, text, exact } = FindElementSchema.parse(args);
  const response = await sendCommand(deviceId, 'find_element', { text, exact });
  return jsonResult(response.data);
}

async function handleClickByText(args: Record<string, unknown>): Promise<ToolResult> {
  const { deviceId, text } = ClickByTextSchema.parse(args);
  const response = await sendCommand(deviceId, 'click_by_text', { text });
  return jsonResult(response.data);
}

async function handleClickById(args: Record<string, unknown>): Promise<ToolResult> {
  const { deviceId, viewId } = ClickByIdSchema.parse(args);
  const response = await sendCommand(deviceId, 'click_by_view_id', { viewId });
  return jsonResult(response.data);
}

async function handleAnalyzeStorage(args: Record<string, unknown>): Promise<ToolResult> {
  const { deviceId, path, maxDepth, minSizeMB, includeHidden } = AnalyzeStorageSchema.parse(args);
  const response = await sendCommand(deviceId, 'analyze_storage', {
    path,
    maxDepth,
    minSizeMB,
    includeHidden,
  }, 120000); // 2 minute timeout for large storage analysis
  return jsonResult(response.data);
}

async function handleFindLargeFiles(args: Record<string, unknown>): Promise<ToolResult> {
  const { deviceId, minSizeMB, path, fileTypes, limit } = FindLargeFilesSchema.parse(args);
  const response = await sendCommand(deviceId, 'find_large_files', {
    minSizeMB,
    path,
    fileTypes,
    limit,
  }, 60000); // 1 minute timeout
  return jsonResult(response.data);
}

async function handleIndexMediaMetadata(args: Record<string, unknown>): Promise<ToolResult> {
  const { deviceId, path, includeLocation, includeExif, limit } = IndexMediaMetadataSchema.parse(args);
  const response = await sendCommand(deviceId, 'index_media_metadata', {
    path,
    includeLocation,
    includeExif,
    limit,
  }, 120000); // 2 minute timeout for large media collections
  return jsonResult(response.data);
}

async function handleSearchMedia(args: Record<string, unknown>): Promise<ToolResult> {
  const { deviceId, path, dateFrom, dateTo, location, fileTypes, minSizeMB, maxSizeMB, cameraModel, sortBy, limit, query } = SearchMediaSchema.parse(args);

  // If natural language query is provided, parse it
  let searchParams: Record<string, unknown> = {
    path,
    dateFrom,
    dateTo,
    location,
    fileTypes,
    minSizeMB,
    maxSizeMB,
    cameraModel,
    sortBy,
    limit,
  };

  if (query) {
    const parsedQuery = parseNaturalLanguageQuery(query);

    // Merge parsed filters with explicit filters (explicit takes precedence)
    if (!dateFrom && parsedQuery.dateFrom) searchParams.dateFrom = parsedQuery.dateFrom;
    if (!dateTo && parsedQuery.dateTo) searchParams.dateTo = parsedQuery.dateTo;
    if (!location && parsedQuery.location) searchParams.location = parsedQuery.location;
    if (!fileTypes && parsedQuery.fileTypes) searchParams.fileTypes = parsedQuery.fileTypes;
  }

  const response = await sendCommand(deviceId, 'search_media', searchParams, 120000); // 2 minute timeout
  return jsonResult(response.data);
}

async function handleStopAudio(args: Record<string, unknown>): Promise<ToolResult> {
  const { deviceId } = StopAudioSchema.parse(args);
  const response = await sendCommand(deviceId, 'stop_audio');
  return jsonResult(response.data);
}

async function handleGetVolume(args: Record<string, unknown>): Promise<ToolResult> {
  const { deviceId } = GetVolumeSchema.parse(args);
  const response = await sendCommand(deviceId, 'get_volume');
  return jsonResult(response.data);
}

async function handleSetVolume(args: Record<string, unknown>): Promise<ToolResult> {
  const { deviceId, stream, level, mute } = SetVolumeSchema.parse(args);
  const response = await sendCommand(deviceId, 'set_volume', { stream, level, mute });
  return jsonResult(response.data);
}

async function handleSearchContacts(args: Record<string, unknown>): Promise<ToolResult> {
  const { deviceId, name, number, limit } = SearchContactsSchema.parse(args);
  const response = await sendCommand(deviceId, 'search_contacts', { name, number, limit });
  return jsonResult(response.data);
}

async function handleGetAlarms(args: Record<string, unknown>): Promise<ToolResult> {
  const { deviceId } = GetAlarmsSchema.parse(args);
  const response = await sendCommand(deviceId, 'get_alarms');
  return jsonResult(response.data);
}

async function handleSetAlarm(args: Record<string, unknown>): Promise<ToolResult> {
  const { deviceId, hour, minute, message, days, skipUi } = SetAlarmSchema.parse(args);
  const response = await sendCommand(deviceId, 'set_alarm', { hour, minute, message, days, skipUi });
  return jsonResult(response.data);
}

async function handleDismissAlarm(args: Record<string, unknown>): Promise<ToolResult> {
  const { deviceId } = DismissAlarmSchema.parse(args);
  const response = await sendCommand(deviceId, 'dismiss_alarm');
  return jsonResult(response.data);
}

async function handleDeleteAlarm(args: Record<string, unknown>): Promise<ToolResult> {
  const { deviceId, alarmId } = DeleteAlarmSchema.parse(args);
  const response = await sendCommand(deviceId, 'delete_alarm', { alarmId });
  return jsonResult(response.data);
}

async function handleTakePhoto(args: Record<string, unknown>): Promise<ToolResult> {
  const { deviceId, camera, quality } = TakePhotoSchema.parse(args);
  const response = await sendCommand(deviceId, 'take_photo', { camera, quality }, 30000);
  const data = response.data as { photo?: string };
  if (data.photo) {
    return imageResult(data.photo, 'image/jpeg');
  }
  return jsonResult(response.data);
}

async function handleRecordVideo(args: Record<string, unknown>): Promise<ToolResult> {
  const { deviceId, camera, maxDuration } = RecordVideoSchema.parse(args);
  const timeout = ((maxDuration || 8) + 12) * 1000;
  const response = await sendCommand(deviceId, 'record_video', { camera, maxDuration }, timeout);
  return jsonResult(response.data);
}
