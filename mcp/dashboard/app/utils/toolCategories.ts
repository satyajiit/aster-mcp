/**
 * Explicit tool -> category map, mirroring the Android app's ToolsSection.kt so
 * web and phone group the catalogue identically.
 *
 * The old dashboard categorised with a substring chain over the tool name,
 * which silently dumped roughly fifteen tools (alarms, contacts, storage,
 * media, volume, camera) into a catch-all "Other" bucket and would mis-bucket
 * any tool added later.
 */
export interface ToolCategory {
  key: string;
  label: string;
  icon: string;
  accent: string;
}

export const TOOL_CATEGORIES: ToolCategory[] = [
  { key: 'screen', label: 'Screen Control', icon: 'ph:monitor', accent: 'var(--color-mode-remote)' },
  { key: 'device', label: 'Device', icon: 'ph:device-mobile', accent: 'var(--color-primary)' },
  { key: 'files', label: 'Files', icon: 'ph:folder', accent: 'var(--color-warning)' },
  { key: 'camera', label: 'Camera', icon: 'ph:camera', accent: 'var(--color-info)' },
  { key: 'communication', label: 'Communication', icon: 'ph:chat-circle', accent: 'var(--color-accent)' },
  { key: 'notifications', label: 'Notifications', icon: 'ph:bell', accent: 'var(--color-warning)' },
  { key: 'media', label: 'Media', icon: 'ph:speaker-high', accent: 'var(--color-mode-mcp)' },
  { key: 'storage', label: 'Storage', icon: 'ph:hard-drive', accent: 'var(--color-info)' },
  { key: 'apps', label: 'Apps', icon: 'ph:package', accent: 'var(--color-mode-ipc)' },
  { key: 'system', label: 'System', icon: 'ph:terminal', accent: 'var(--color-fg-subtle)' },
  { key: 'overlays', label: 'Overlays', icon: 'ph:stack', accent: 'var(--color-mode-mcp)' },
  { key: 'alarms', label: 'Alarms', icon: 'ph:alarm', accent: 'var(--color-error)' },
];

const TOOL_CATEGORY: Record<string, string> = {
  aster_list_devices: 'device',
  aster_get_device_info: 'device',
  aster_get_battery: 'device',
  aster_get_location: 'device',

  aster_take_screenshot: 'screen',
  aster_get_screen_hierarchy: 'screen',
  aster_input_gesture: 'screen',
  aster_input_text: 'screen',
  aster_global_action: 'screen',
  aster_find_element: 'screen',
  aster_click_by_text: 'screen',
  aster_click_by_id: 'screen',

  aster_list_files: 'files',
  aster_read_file: 'files',
  aster_write_file: 'files',
  aster_delete_file: 'files',

  aster_take_photo: 'camera',
  aster_record_video: 'camera',

  aster_read_sms: 'communication',
  aster_send_sms: 'communication',
  aster_make_call: 'communication',
  aster_make_call_with_voice: 'communication',
  aster_search_contacts: 'communication',
  aster_list_contacts_full: 'communication',
  aster_delete_contacts: 'communication',

  aster_read_notifications: 'notifications',
  aster_post_notification: 'notifications',

  aster_speak_tts: 'media',
  aster_play_audio: 'media',
  aster_stop_audio: 'media',
  aster_get_volume: 'media',
  aster_set_volume: 'media',

  aster_analyze_storage: 'storage',
  aster_find_large_files: 'storage',
  aster_index_media_metadata: 'storage',
  aster_search_media: 'storage',

  aster_list_packages: 'apps',
  aster_list_installed_apps: 'apps',
  aster_launch_intent: 'apps',

  aster_execute_shell: 'system',
  aster_vibrate: 'system',
  aster_set_clipboard: 'system',
  aster_get_clipboard: 'system',

  aster_show_overlay: 'overlays',
  aster_show_toast: 'overlays',

  aster_get_alarms: 'alarms',
  aster_set_alarm: 'alarms',
  aster_dismiss_alarm: 'alarms',
  aster_delete_alarm: 'alarms',
};

/** Unmapped tools land in System rather than a nameless bucket. */
export function categoryFor(toolName: string): string {
  return TOOL_CATEGORY[toolName] ?? 'system';
}
