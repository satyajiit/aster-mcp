import { z } from 'zod';

export const GetDeviceInfoSchema = z.object({
  deviceId: z.string().describe('The unique identifier of the device'),
});

export const ReadNotificationsSchema = z.object({
  deviceId: z.string().describe('The unique identifier of the device'),
  limit: z.number().optional().default(20).describe('Maximum number of notifications to fetch'),
});

export const ReadSmsSchema = z.object({
  deviceId: z.string().describe('The unique identifier of the device'),
  limit: z.number().optional().default(20).describe('Maximum number of messages to fetch'),
  threadId: z.string().optional().describe('Filter by conversation thread ID'),
});

export const GetLocationSchema = z.object({
  deviceId: z.string().describe('The unique identifier of the device'),
});

export const ExecuteShellSchema = z.object({
  deviceId: z.string().describe('The unique identifier of the device'),
  command: z.string().describe('Shell command to execute in the app sandbox'),
});

export const ListPackagesSchema = z.object({
  deviceId: z.string().describe('The unique identifier of the device'),
  includeSystem: z.boolean().optional().default(false).describe('Include system packages'),
});

export const ListFilesSchema = z.object({
  deviceId: z.string().describe('The unique identifier of the device'),
  path: z.string().describe('Directory path to list'),
});

export const ReadFileSchema = z.object({
  deviceId: z.string().describe('The unique identifier of the device'),
  path: z.string().describe('File path to read'),
});

export const WriteFileSchema = z.object({
  deviceId: z.string().describe('The unique identifier of the device'),
  path: z.string().describe('File path to write'),
  content: z.string().describe('Content to write (text or base64 for binary)'),
});

export const DeleteFileSchema = z.object({
  deviceId: z.string().describe('The unique identifier of the device'),
  path: z.string().describe('File path to delete'),
});

export const TakeScreenshotSchema = z.object({
  deviceId: z.string().describe('The unique identifier of the device'),
});

export const GetScreenHierarchySchema = z.object({
  deviceId: z.string().describe('The unique identifier of the device'),
  mode: z.enum(['full', 'interactive', 'summary']).optional().default('interactive')
    .describe('Hierarchy mode: "full" returns complete tree, "interactive" returns only clickable/editable elements (default), "summary" returns minimal info'),
  maxDepth: z.number().optional().describe('Maximum depth to traverse (default: unlimited)'),
  includeInvisible: z.boolean().optional().default(false).describe('Include invisible elements'),
  searchText: z.string().optional().describe('Filter elements containing this text'),
});

export const InputGestureSchema = z.object({
  deviceId: z.string().describe('The unique identifier of the device'),
  gestureType: z.enum(['TAP', 'SWIPE', 'LONG_PRESS']).describe('Type of gesture'),
  points: z.array(z.object({
    x: z.number().describe('X coordinate'),
    y: z.number().describe('Y coordinate'),
  })).describe('Points for the gesture (1 for tap, 2 for swipe)'),
  duration: z.number().optional().default(100).describe('Duration in milliseconds'),
});

export const InputTextSchema = z.object({
  deviceId: z.string().describe('The unique identifier of the device'),
  text: z.string().describe('Text to input'),
});

export const GlobalActionSchema = z.object({
  deviceId: z.string().describe('The unique identifier of the device'),
  action: z.enum(['BACK', 'HOME', 'RECENTS', 'NOTIFICATIONS', 'POWER_DIALOG', 'LOCK_SCREEN'])
    .describe('Global action to perform'),
});

export const SpeakTtsSchema = z.object({
  deviceId: z.string().describe('The unique identifier of the device'),
  text: z.string().describe('Text to speak'),
});

export const GetBatterySchema = z.object({
  deviceId: z.string().describe('The unique identifier of the device'),
});

export const ShowOverlaySchema = z.object({
  deviceId: z.string().describe('The unique identifier of the device'),
  url: z.string().optional().describe('URL to load in the overlay WebView'),
  html: z.string().optional().describe('HTML content to render'),
  showCloseButton: z.boolean().optional().default(true).describe('Show a close (X) button on the overlay (default: true)'),
  timeout: z.number().optional().describe('Auto-dismiss overlay after this many seconds'),
});

export const VibrateSchema = z.object({
  deviceId: z.string().describe('The unique identifier of the device'),
  pattern: z.array(z.number()).describe('Vibration pattern in milliseconds [wait, vibrate, wait, ...]'),
});

export const PlayAudioSchema = z.object({
  deviceId: z.string().describe('The unique identifier of the device'),
  source: z.string().describe('Audio source URL or base64 encoded audio'),
});

export const PostNotificationSchema = z.object({
  deviceId: z.string().describe('The unique identifier of the device'),
  title: z.string().describe('Notification title'),
  body: z.string().describe('Notification body'),
  actions: z.array(z.object({
    id: z.string(),
    label: z.string(),
  })).optional().describe('Action buttons'),
});

export const SendSmsSchema = z.object({
  deviceId: z.string().describe('The unique identifier of the device'),
  number: z.string().describe('Phone number to send SMS to'),
  message: z.string().describe('Text message content to send'),
});

export const MakeCallSchema = z.object({
  deviceId: z.string().describe('The unique identifier of the device'),
  number: z.string().describe('Phone number to call'),
});

export const MakeCallWithVoiceSchema = z.object({
  deviceId: z.string().describe('The unique identifier of the device'),
  number: z.string().describe('Phone number to call'),
  text: z.string().describe('Text for the AI to speak during the call via TTS'),
  waitSeconds: z.number().optional().default(8).describe('Seconds to wait after dialing before speaking (default: 8, to allow the call to be answered)'),
});

export const SetClipboardSchema = z.object({
  deviceId: z.string().describe('The unique identifier of the device'),
  text: z.string().describe('Text to copy to clipboard'),
});

export const GetClipboardSchema = z.object({
  deviceId: z.string().describe('The unique identifier of the device'),
});

export const ShowToastSchema = z.object({
  deviceId: z.string().describe('The unique identifier of the device'),
  message: z.string().describe('Toast message'),
  duration: z.enum(['short', 'long']).optional().default('short').describe('Toast duration'),
});

export const LaunchIntentSchema = z.object({
  deviceId: z.string().describe('The unique identifier of the device'),
  packageName: z.string().optional().describe('Target package name'),
  action: z.string().optional().describe('Intent action'),
  data: z.string().optional().describe('Intent data URI'),
});

export const FindElementSchema = z.object({
  deviceId: z.string().describe('The unique identifier of the device'),
  text: z.string().describe('Text to search for in element text or contentDescription'),
  exact: z.boolean().optional().default(false).describe('Require exact match instead of contains'),
});

export const ClickByTextSchema = z.object({
  deviceId: z.string().describe('The unique identifier of the device'),
  text: z.string().describe('Text to find and click'),
});

export const ClickByIdSchema = z.object({
  deviceId: z.string().describe('The unique identifier of the device'),
  viewId: z.string().describe('View ID resource name'),
});

export const AnalyzeStorageSchema = z.object({
  deviceId: z.string().describe('The unique identifier of the device'),
  path: z.string().optional().default('/sdcard').describe('Path to analyze (default: /sdcard)'),
  maxDepth: z.number().optional().default(3).describe('Maximum directory depth to traverse'),
  minSizeMB: z.number().optional().default(0).describe('Only include items larger than X MB'),
  includeHidden: z.boolean().optional().default(false).describe('Include hidden files/folders'),
});

export const FindLargeFilesSchema = z.object({
  deviceId: z.string().describe('The unique identifier of the device'),
  minSizeMB: z.number().describe('Minimum file size in MB'),
  path: z.string().optional().default('/sdcard').describe('Path to search (default: /sdcard)'),
  fileTypes: z.array(z.string()).optional().describe('Filter by file extensions (e.g., ["mp4", "jpg"])'),
  limit: z.number().optional().default(50).describe('Maximum number of files to return'),
});

export const IndexMediaMetadataSchema = z.object({
  deviceId: z.string().describe('The unique identifier of the device'),
  path: z.string().optional().default('/sdcard/DCIM').describe('Path to scan for media files (default: /sdcard/DCIM)'),
  includeLocation: z.boolean().optional().default(true).describe('Include GPS location data from EXIF'),
  includeExif: z.boolean().optional().default(true).describe('Include camera and EXIF metadata'),
  limit: z.number().optional().default(100).describe('Maximum number of media files to process'),
});

export const SearchMediaSchema = z.object({
  deviceId: z.string().describe('The unique identifier of the device'),
  query: z.string().optional().describe('Natural language search query (e.g., "photos from last year same month", "pictures taken at mumbai")'),
  path: z.string().optional().default('/sdcard/DCIM').describe('Path to search (default: /sdcard/DCIM)'),
  dateFrom: z.string().optional().describe('Start date in ISO format (YYYY-MM-DD)'),
  dateTo: z.string().optional().describe('End date in ISO format (YYYY-MM-DD)'),
  location: z.object({
    city: z.string().optional(),
    country: z.string().optional(),
    latitude: z.number().optional(),
    longitude: z.number().optional(),
    radiusKm: z.number().optional().default(50),
  }).optional().describe('Location filter'),
  fileTypes: z.array(z.string()).optional().describe('File extensions to include (e.g., ["jpg", "png", "mp4"])'),
  minSizeMB: z.number().optional().describe('Minimum file size in MB'),
  maxSizeMB: z.number().optional().describe('Maximum file size in MB'),
  cameraModel: z.string().optional().describe('Filter by camera model'),
  sortBy: z.enum(['DATE_ASC', 'DATE_DESC', 'SIZE_ASC', 'SIZE_DESC', 'NAME_ASC', 'NAME_DESC']).optional().default('DATE_DESC'),
  limit: z.number().optional().default(100).describe('Maximum results to return'),
});

export const StopAudioSchema = z.object({
  deviceId: z.string().describe('The unique identifier of the device'),
});

export const GetVolumeSchema = z.object({
  deviceId: z.string().describe('The unique identifier of the device'),
});

export const SetVolumeSchema = z.object({
  deviceId: z.string().describe('The unique identifier of the device'),
  stream: z.enum(['media', 'ring', 'notification', 'alarm', 'call', 'system']).describe('Audio stream to control'),
  level: z.number().optional().describe('Volume level to set (0 to stream max)'),
  mute: z.boolean().optional().describe('Mute (true) or unmute (false) the stream'),
});

export const SearchContactsSchema = z.object({
  deviceId: z.string().describe('The unique identifier of the device'),
  name: z.string().optional().describe('Search by contact name'),
  number: z.string().optional().describe('Search by phone number'),
  limit: z.number().optional().default(20).describe('Maximum number of results (default: 20)'),
});

export const GetAlarmsSchema = z.object({
  deviceId: z.string().describe('The unique identifier of the device'),
});

export const SetAlarmSchema = z.object({
  deviceId: z.string().describe('The unique identifier of the device'),
  hour: z.number().describe('Hour in 24-hour format (0-23)'),
  minute: z.number().describe('Minute (0-59)'),
  message: z.string().optional().describe('Alarm label/message'),
  days: z.array(z.number()).optional().describe('Recurring days (Calendar constants: 1=Sun, 2=Mon, ..., 7=Sat)'),
  skipUi: z.boolean().optional().default(true).describe('Skip the alarm app UI (default: true)'),
});

export const DismissAlarmSchema = z.object({
  deviceId: z.string().describe('The unique identifier of the device'),
});

export const DeleteAlarmSchema = z.object({
  deviceId: z.string().describe('The unique identifier of the device'),
  alarmId: z.string().describe('Alarm ID to delete (from get_alarms)'),
});

export const TakePhotoSchema = z.object({
  deviceId: z.string().describe('The unique identifier of the device'),
  camera: z.enum(['front', 'back']).optional().default('back').describe('Camera to use (default: back)'),
  quality: z.number().optional().default(75).describe('JPEG quality 1-100 (default: 75)'),
});

export const RecordVideoSchema = z.object({
  deviceId: z.string().describe('The unique identifier of the device'),
  camera: z.enum(['front', 'back']).optional().default('back').describe('Camera to use (default: back)'),
  maxDuration: z.number().optional().default(8).describe('Maximum recording duration in seconds, 1-8 (default: 8)'),
});

// Tool definitions for MCP
export const TOOL_DEFINITIONS = [
  {
    name: 'aster_list_devices',
    description: 'List all paired devices with their connection status and basic information',
    inputSchema: {
      type: 'object',
      properties: {},
      required: [],
    },
  },
  {
    name: 'aster_get_device_info',
    description: 'Get detailed information about a specific device including battery, storage, and system specs',
    inputSchema: {
      type: 'object',
      properties: {
        deviceId: { type: 'string', description: 'The unique identifier of the device' },
      },
      required: ['deviceId'],
    },
  },
  {
    name: 'aster_read_notifications',
    description: 'Read active notifications from the device notification shade',
    inputSchema: {
      type: 'object',
      properties: {
        deviceId: { type: 'string', description: 'The unique identifier of the device' },
        limit: { type: 'number', description: 'Maximum number of notifications to fetch', default: 20 },
      },
      required: ['deviceId'],
    },
  },
  {
    name: 'aster_read_sms',
    description: 'Read SMS messages from the device',
    inputSchema: {
      type: 'object',
      properties: {
        deviceId: { type: 'string', description: 'The unique identifier of the device' },
        limit: { type: 'number', description: 'Maximum number of messages to fetch', default: 20 },
        threadId: { type: 'string', description: 'Filter by conversation thread ID' },
      },
      required: ['deviceId'],
    },
  },
  {
    name: 'aster_send_sms',
    description: 'Send an SMS text message to a phone number',
    inputSchema: {
      type: 'object',
      properties: {
        deviceId: { type: 'string', description: 'The unique identifier of the device' },
        number: { type: 'string', description: 'Phone number to send SMS to' },
        message: { type: 'string', description: 'Text message content to send' },
      },
      required: ['deviceId', 'number', 'message'],
    },
  },
  {
    name: 'aster_get_location',
    description: 'Get the current GPS/network location of the device',
    inputSchema: {
      type: 'object',
      properties: {
        deviceId: { type: 'string', description: 'The unique identifier of the device' },
      },
      required: ['deviceId'],
    },
  },
  {
    name: 'aster_execute_shell',
    description: 'Execute a shell command within the Android app sandbox. Runs as an unprivileged app process (no root) — restricted to the app\'s own data directory, standard Android utilities, and user-accessible storage. Cannot modify system files, access other app data, or bypass Android permissions. Commands are subject to a 30-second timeout and 1MB output limit.',
    inputSchema: {
      type: 'object',
      properties: {
        deviceId: { type: 'string', description: 'The unique identifier of the device' },
        command: { type: 'string', description: 'Shell command to execute' },
      },
      required: ['deviceId', 'command'],
    },
  },
  {
    name: 'aster_list_packages',
    description: 'List installed Android packages/applications',
    inputSchema: {
      type: 'object',
      properties: {
        deviceId: { type: 'string', description: 'The unique identifier of the device' },
        includeSystem: { type: 'boolean', description: 'Include system packages', default: false },
      },
      required: ['deviceId'],
    },
  },
  {
    name: 'aster_list_files',
    description: 'List files and directories at a specified path',
    inputSchema: {
      type: 'object',
      properties: {
        deviceId: { type: 'string', description: 'The unique identifier of the device' },
        path: { type: 'string', description: 'Directory path to list' },
      },
      required: ['deviceId', 'path'],
    },
  },
  {
    name: 'aster_read_file',
    description: 'Read the content of a file from the device',
    inputSchema: {
      type: 'object',
      properties: {
        deviceId: { type: 'string', description: 'The unique identifier of the device' },
        path: { type: 'string', description: 'File path to read' },
      },
      required: ['deviceId', 'path'],
    },
  },
  {
    name: 'aster_write_file',
    description: 'Write content to a file on the device',
    inputSchema: {
      type: 'object',
      properties: {
        deviceId: { type: 'string', description: 'The unique identifier of the device' },
        path: { type: 'string', description: 'File path to write' },
        content: { type: 'string', description: 'Content to write' },
      },
      required: ['deviceId', 'path', 'content'],
    },
  },
  {
    name: 'aster_delete_file',
    description: 'Delete a file from the device',
    inputSchema: {
      type: 'object',
      properties: {
        deviceId: { type: 'string', description: 'The unique identifier of the device' },
        path: { type: 'string', description: 'File path to delete' },
      },
      required: ['deviceId', 'path'],
    },
  },
  {
    name: 'aster_take_screenshot',
    description: 'Capture a screenshot of the current screen',
    inputSchema: {
      type: 'object',
      properties: {
        deviceId: { type: 'string', description: 'The unique identifier of the device' },
      },
      required: ['deviceId'],
    },
  },
  {
    name: 'aster_get_screen_hierarchy',
    description: 'Get the accessibility node tree of the current screen for UI analysis. Supports filtering for efficiency.',
    inputSchema: {
      type: 'object',
      properties: {
        deviceId: { type: 'string', description: 'The unique identifier of the device' },
        mode: {
          type: 'string',
          enum: ['full', 'interactive', 'summary'],
          default: 'interactive',
          description: 'Hierarchy mode: "full" returns complete tree, "interactive" returns only clickable/editable elements (default, recommended), "summary" returns minimal info',
        },
        maxDepth: { type: 'number', description: 'Maximum depth to traverse (optional, helps reduce data size)' },
        includeInvisible: { type: 'boolean', default: false, description: 'Include invisible elements' },
        searchText: { type: 'string', description: 'Filter elements containing this text (case-insensitive)' },
      },
      required: ['deviceId'],
    },
  },
  {
    name: 'aster_input_gesture',
    description: 'Perform touch gestures (tap, swipe, long press) on the screen',
    inputSchema: {
      type: 'object',
      properties: {
        deviceId: { type: 'string', description: 'The unique identifier of the device' },
        gestureType: { type: 'string', enum: ['TAP', 'SWIPE', 'LONG_PRESS'], description: 'Type of gesture' },
        points: {
          type: 'array',
          items: {
            type: 'object',
            properties: {
              x: { type: 'number' },
              y: { type: 'number' },
            },
            required: ['x', 'y'],
          },
          description: 'Points for the gesture',
        },
        duration: { type: 'number', description: 'Duration in milliseconds', default: 100 },
      },
      required: ['deviceId', 'gestureType', 'points'],
    },
  },
  {
    name: 'aster_input_text',
    description: 'Input text into the currently focused field',
    inputSchema: {
      type: 'object',
      properties: {
        deviceId: { type: 'string', description: 'The unique identifier of the device' },
        text: { type: 'string', description: 'Text to input' },
      },
      required: ['deviceId', 'text'],
    },
  },
  {
    name: 'aster_global_action',
    description: 'Perform global actions like BACK, HOME, RECENTS, etc.',
    inputSchema: {
      type: 'object',
      properties: {
        deviceId: { type: 'string', description: 'The unique identifier of the device' },
        action: {
          type: 'string',
          enum: ['BACK', 'HOME', 'RECENTS', 'NOTIFICATIONS', 'POWER_DIALOG', 'LOCK_SCREEN'],
          description: 'Global action to perform',
        },
      },
      required: ['deviceId', 'action'],
    },
  },
  {
    name: 'aster_speak_tts',
    description: 'Speak text using device text-to-speech',
    inputSchema: {
      type: 'object',
      properties: {
        deviceId: { type: 'string', description: 'The unique identifier of the device' },
        text: { type: 'string', description: 'Text to speak' },
      },
      required: ['deviceId', 'text'],
    },
  },
  {
    name: 'aster_get_battery',
    description: 'Get detailed battery information and statistics',
    inputSchema: {
      type: 'object',
      properties: {
        deviceId: { type: 'string', description: 'The unique identifier of the device' },
      },
      required: ['deviceId'],
    },
  },
  {
    name: 'aster_show_overlay',
    description: 'Show a system overlay with web content. Supports close button and auto-timeout.',
    inputSchema: {
      type: 'object',
      properties: {
        deviceId: { type: 'string', description: 'The unique identifier of the device' },
        url: { type: 'string', description: 'URL to load in the overlay' },
        html: { type: 'string', description: 'HTML content to render' },
        showCloseButton: { type: 'boolean', description: 'Show a close (X) button on the overlay (default: true)', default: true },
        timeout: { type: 'number', description: 'Auto-dismiss overlay after this many seconds' },
      },
      required: ['deviceId'],
    },
  },
  {
    name: 'aster_vibrate',
    description: 'Vibrate the device with a custom pattern',
    inputSchema: {
      type: 'object',
      properties: {
        deviceId: { type: 'string', description: 'The unique identifier of the device' },
        pattern: {
          type: 'array',
          items: { type: 'number' },
          description: 'Vibration pattern [wait, vibrate, wait, vibrate, ...]',
        },
      },
      required: ['deviceId', 'pattern'],
    },
  },
  {
    name: 'aster_play_audio',
    description: 'Play audio from URL or base64 data',
    inputSchema: {
      type: 'object',
      properties: {
        deviceId: { type: 'string', description: 'The unique identifier of the device' },
        source: { type: 'string', description: 'Audio URL or base64 data' },
      },
      required: ['deviceId', 'source'],
    },
  },
  {
    name: 'aster_post_notification',
    description: 'Post a notification on the device',
    inputSchema: {
      type: 'object',
      properties: {
        deviceId: { type: 'string', description: 'The unique identifier of the device' },
        title: { type: 'string', description: 'Notification title' },
        body: { type: 'string', description: 'Notification body' },
        actions: {
          type: 'array',
          items: {
            type: 'object',
            properties: {
              id: { type: 'string' },
              label: { type: 'string' },
            },
            required: ['id', 'label'],
          },
          description: 'Action buttons',
        },
      },
      required: ['deviceId', 'title', 'body'],
    },
  },
  {
    name: 'aster_make_call',
    description: 'Initiate a phone call',
    inputSchema: {
      type: 'object',
      properties: {
        deviceId: { type: 'string', description: 'The unique identifier of the device' },
        number: { type: 'string', description: 'Phone number to call' },
      },
      required: ['deviceId', 'number'],
    },
  },
  {
    name: 'aster_make_call_with_voice',
    description: 'Make a phone call and speak AI-generated text via TTS on speakerphone. The call is placed, speakerphone is enabled, and after a configurable wait (for the recipient to answer), the provided text is spoken through TTS routed to the call audio.',
    inputSchema: {
      type: 'object',
      properties: {
        deviceId: { type: 'string', description: 'The unique identifier of the device' },
        number: { type: 'string', description: 'Phone number to call' },
        text: { type: 'string', description: 'Text for the AI to speak during the call' },
        waitSeconds: { type: 'number', description: 'Seconds to wait before speaking (default: 8)', default: 8 },
      },
      required: ['deviceId', 'number', 'text'],
    },
  },
  {
    name: 'aster_set_clipboard',
    description: 'Copy text to the device clipboard',
    inputSchema: {
      type: 'object',
      properties: {
        deviceId: { type: 'string', description: 'The unique identifier of the device' },
        text: { type: 'string', description: 'Text to copy' },
      },
      required: ['deviceId', 'text'],
    },
  },
  {
    name: 'aster_get_clipboard',
    description: 'Read text from the device clipboard',
    inputSchema: {
      type: 'object',
      properties: {
        deviceId: { type: 'string', description: 'The unique identifier of the device' },
      },
      required: ['deviceId'],
    },
  },
  {
    name: 'aster_show_toast',
    description: 'Show a toast message on the device',
    inputSchema: {
      type: 'object',
      properties: {
        deviceId: { type: 'string', description: 'The unique identifier of the device' },
        message: { type: 'string', description: 'Toast message' },
        duration: { type: 'string', enum: ['short', 'long'], description: 'Toast duration', default: 'short' },
      },
      required: ['deviceId', 'message'],
    },
  },
  {
    name: 'aster_launch_intent',
    description: 'Launch an app or intent on the device',
    inputSchema: {
      type: 'object',
      properties: {
        deviceId: { type: 'string', description: 'The unique identifier of the device' },
        packageName: { type: 'string', description: 'Target package name' },
        action: { type: 'string', description: 'Intent action' },
        data: { type: 'string', description: 'Intent data URI' },
      },
      required: ['deviceId'],
    },
  },
  {
    name: 'aster_find_element',
    description: 'Find UI elements by text or content description. More efficient than full hierarchy for element search.',
    inputSchema: {
      type: 'object',
      properties: {
        deviceId: { type: 'string', description: 'The unique identifier of the device' },
        text: { type: 'string', description: 'Text to search for in element text or contentDescription (case-insensitive)' },
        exact: { type: 'boolean', default: false, description: 'Require exact match instead of contains' },
      },
      required: ['deviceId', 'text'],
    },
  },
  {
    name: 'aster_click_by_text',
    description: 'Click an element by its text or content description. Combines find and click in one operation.',
    inputSchema: {
      type: 'object',
      properties: {
        deviceId: { type: 'string', description: 'The unique identifier of the device' },
        text: { type: 'string', description: 'Text to find and click' },
      },
      required: ['deviceId', 'text'],
    },
  },
  {
    name: 'aster_click_by_id',
    description: 'Click an element by its view ID resource name',
    inputSchema: {
      type: 'object',
      properties: {
        deviceId: { type: 'string', description: 'The unique identifier of the device' },
        viewId: { type: 'string', description: 'View ID resource name (e.g., "com.app:id/button_name")' },
      },
      required: ['deviceId', 'viewId'],
    },
  },
  {
    name: 'aster_analyze_storage',
    description: 'Comprehensive storage analysis with directory breakdown, file type categorization, and large file detection. Returns detailed statistics for AI-powered recommendations.',
    inputSchema: {
      type: 'object',
      properties: {
        deviceId: { type: 'string', description: 'The unique identifier of the device' },
        path: { type: 'string', default: '/sdcard', description: 'Path to analyze (default: /sdcard)' },
        maxDepth: { type: 'number', default: 3, description: 'Maximum directory depth to traverse (default: 3)' },
        minSizeMB: { type: 'number', default: 0, description: 'Only include items larger than X MB (default: 0)' },
        includeHidden: { type: 'boolean', default: false, description: 'Include hidden files/folders (default: false)' },
      },
      required: ['deviceId'],
    },
  },
  {
    name: 'aster_find_large_files',
    description: 'Find large files quickly for storage cleanup. Optimized search with optional file type filtering.',
    inputSchema: {
      type: 'object',
      properties: {
        deviceId: { type: 'string', description: 'The unique identifier of the device' },
        minSizeMB: { type: 'number', description: 'Minimum file size in MB' },
        path: { type: 'string', default: '/sdcard', description: 'Path to search (default: /sdcard)' },
        fileTypes: { type: 'array', items: { type: 'string' }, description: 'Filter by file extensions (e.g., ["mp4", "jpg"])' },
        limit: { type: 'number', default: 50, description: 'Maximum number of files to return (default: 50)' },
      },
      required: ['deviceId', 'minSizeMB'],
    },
  },
  {
    name: 'aster_index_media_metadata',
    description: 'Extract EXIF metadata from photos and videos including date taken, GPS location, camera info, and dimensions. Scans media files recursively and returns structured metadata for intelligent search and organization.',
    inputSchema: {
      type: 'object',
      properties: {
        deviceId: { type: 'string', description: 'The unique identifier of the device' },
        path: { type: 'string', default: '/sdcard/DCIM', description: 'Path to scan for media files (default: /sdcard/DCIM)' },
        includeLocation: { type: 'boolean', default: true, description: 'Include GPS location data from EXIF' },
        includeExif: { type: 'boolean', default: true, description: 'Include camera and EXIF metadata' },
        limit: { type: 'number', default: 100, description: 'Maximum number of media files to process (default: 100)' },
      },
      required: ['deviceId'],
    },
  },
  {
    name: 'aster_search_media',
    description: 'Search media files using natural language queries or structured filters. Supports queries like "photos from last year same month" or "pictures taken at mumbai". Can combine natural language with explicit filters.',
    inputSchema: {
      type: 'object',
      properties: {
        deviceId: { type: 'string', description: 'The unique identifier of the device' },
        query: { type: 'string', description: 'Natural language search query (e.g., "photos from last year", "videos in Mumbai")' },
        path: { type: 'string', default: '/sdcard/DCIM', description: 'Path to search (default: /sdcard/DCIM)' },
        dateFrom: { type: 'string', description: 'Start date in ISO format (YYYY-MM-DD)' },
        dateTo: { type: 'string', description: 'End date in ISO format (YYYY-MM-DD)' },
        location: {
          type: 'object',
          properties: {
            city: { type: 'string' },
            country: { type: 'string' },
            latitude: { type: 'number' },
            longitude: { type: 'number' },
            radiusKm: { type: 'number', default: 50 },
          },
          description: 'Location filter (city/country name or GPS coordinates with radius)'
        },
        fileTypes: { type: 'array', items: { type: 'string' }, description: 'File extensions (e.g., ["jpg", "png"])' },
        minSizeMB: { type: 'number', description: 'Minimum file size in MB' },
        maxSizeMB: { type: 'number', description: 'Maximum file size in MB' },
        cameraModel: { type: 'string', description: 'Filter by camera model' },
        sortBy: { type: 'string', enum: ['DATE_ASC', 'DATE_DESC', 'SIZE_ASC', 'SIZE_DESC', 'NAME_ASC', 'NAME_DESC'], default: 'DATE_DESC' },
        limit: { type: 'number', default: 100 },
      },
      required: ['deviceId'],
    },
  },
  {
    name: 'aster_stop_audio',
    description: 'Stop currently playing audio on the device',
    inputSchema: {
      type: 'object',
      properties: {
        deviceId: { type: 'string', description: 'The unique identifier of the device' },
      },
      required: ['deviceId'],
    },
  },
  {
    name: 'aster_get_volume',
    description: 'Get all audio stream volume levels (media, ring, notification, alarm, call, system) and ringer mode',
    inputSchema: {
      type: 'object',
      properties: {
        deviceId: { type: 'string', description: 'The unique identifier of the device' },
      },
      required: ['deviceId'],
    },
  },
  {
    name: 'aster_set_volume',
    description: 'Set volume level or mute/unmute a specific audio stream',
    inputSchema: {
      type: 'object',
      properties: {
        deviceId: { type: 'string', description: 'The unique identifier of the device' },
        stream: { type: 'string', enum: ['media', 'ring', 'notification', 'alarm', 'call', 'system'], description: 'Audio stream to control' },
        level: { type: 'number', description: 'Volume level to set (0 to stream max)' },
        mute: { type: 'boolean', description: 'Mute (true) or unmute (false) the stream' },
      },
      required: ['deviceId', 'stream'],
    },
  },
  {
    name: 'aster_search_contacts',
    description: 'Search contacts by name or phone number. Returns matching contacts with all phone numbers and emails.',
    inputSchema: {
      type: 'object',
      properties: {
        deviceId: { type: 'string', description: 'The unique identifier of the device' },
        name: { type: 'string', description: 'Search by contact name (fuzzy match)' },
        number: { type: 'string', description: 'Search by phone number' },
        limit: { type: 'number', description: 'Maximum number of results (default: 20)', default: 20 },
      },
      required: ['deviceId'],
    },
  },
  {
    name: 'aster_get_alarms',
    description: 'List device alarms. Tries stock Android clock provider first, falls back to next-alarm-only from AlarmManager. Not all OEMs expose full alarm lists.',
    inputSchema: {
      type: 'object',
      properties: {
        deviceId: { type: 'string', description: 'The unique identifier of the device' },
      },
      required: ['deviceId'],
    },
  },
  {
    name: 'aster_set_alarm',
    description: 'Create a new alarm using the device clock app. Note: there is no standard Android API to edit or delete existing alarms by ID — to change an alarm, create a new one with the desired properties.',
    inputSchema: {
      type: 'object',
      properties: {
        deviceId: { type: 'string', description: 'The unique identifier of the device' },
        hour: { type: 'number', description: 'Hour in 24-hour format (0-23)' },
        minute: { type: 'number', description: 'Minute (0-59)' },
        message: { type: 'string', description: 'Alarm label/message' },
        days: { type: 'array', items: { type: 'number' }, description: 'Recurring days (Calendar constants: 1=Sun, 2=Mon, ..., 7=Sat)' },
        skipUi: { type: 'boolean', description: 'Skip the alarm app UI (default: true)', default: true },
      },
      required: ['deviceId', 'hour', 'minute'],
    },
  },
  {
    name: 'aster_dismiss_alarm',
    description: 'Dismiss an alarm that is currently ringing/firing. Only works while an alarm is actively going off. Requires Android 6.0+.',
    inputSchema: {
      type: 'object',
      properties: {
        deviceId: { type: 'string', description: 'The unique identifier of the device' },
      },
      required: ['deviceId'],
    },
  },
  {
    name: 'aster_delete_alarm',
    description: 'Delete a saved alarm by its ID. Use get_alarms first to find alarm IDs. Works on devices with accessible clock content providers (stock Android, Samsung).',
    inputSchema: {
      type: 'object',
      properties: {
        deviceId: { type: 'string', description: 'The unique identifier of the device' },
        alarmId: { type: 'string', description: 'Alarm ID to delete (from get_alarms results)' },
      },
      required: ['deviceId', 'alarmId'],
    },
  },
  {
    name: 'aster_take_photo',
    description: 'Capture a photo using the device camera. Returns the image directly for viewing. Resolution: 1280x720.',
    inputSchema: {
      type: 'object',
      properties: {
        deviceId: { type: 'string', description: 'The unique identifier of the device' },
        camera: { type: 'string', enum: ['front', 'back'], description: 'Camera to use (default: back)', default: 'back' },
        quality: { type: 'number', description: 'JPEG quality 1-100 (default: 75)', default: 75 },
      },
      required: ['deviceId'],
    },
  },
  {
    name: 'aster_record_video',
    description: 'Record a short video clip (max 8 seconds) at 480p. No audio. Returns base64 MP4 if under 5MB, otherwise returns file path.',
    inputSchema: {
      type: 'object',
      properties: {
        deviceId: { type: 'string', description: 'The unique identifier of the device' },
        camera: { type: 'string', enum: ['front', 'back'], description: 'Camera to use (default: back)', default: 'back' },
        maxDuration: { type: 'number', description: 'Max recording duration 1-8 seconds (default: 8)', default: 8 },
      },
      required: ['deviceId'],
    },
  },
];
