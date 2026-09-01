import { toolJson, toolText } from '~/composables/useApi';

/**
 * Run a device tool and hand back parsed JSON plus loading/error state.
 *
 * Every Tier 2 panel needs the same shape — call a tool, parse the JSON text
 * block, surface failures as a toast — so this keeps ten panels from each
 * reimplementing it.
 */
export function useDeviceTool<T>(deviceId: string) {
  const api = useApi();
  const toast = useToast();

  const data = ref<T | null>(null);
  const raw = ref<string>('');
  const loading = ref(false);
  const error = ref<string | null>(null);

  async function run(name: string, args: Record<string, unknown> = {}, label?: string) {
    loading.value = true;
    error.value = null;
    try {
      const result = await api.executeTool(deviceId, name, args);
      raw.value = toolText(result);
      if (result.isError) {
        error.value = raw.value || 'Tool returned an error';
        toast.error(`${label ?? name} failed`, error.value.slice(0, 200));
        return null;
      }
      data.value = toolJson<T>(result);
      return data.value;
    } catch (e) {
      error.value = e instanceof Error ? e.message : String(e);
      toast.error(`${label ?? name} failed`, error.value.slice(0, 200));
      return null;
    } finally {
      loading.value = false;
    }
  }

  return { data, raw, loading, error, run, toast };
}
