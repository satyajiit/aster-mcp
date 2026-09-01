export interface Toast {
  id: number;
  type: 'success' | 'error' | 'info';
  title: string;
  description?: string;
}

let nextId = 0;

/**
 * Shared toast queue. Replaces the five native `alert()` calls the old
 * FileBrowser used for download/delete/create-folder failures.
 */
export function useToast() {
  const toasts = useState<Toast[]>('aster-toasts', () => []);

  function dismiss(id: number) {
    const i = toasts.value.findIndex((t) => t.id === id);
    if (i !== -1) toasts.value.splice(i, 1);
  }

  function push(type: Toast['type'], title: string, description?: string, ttl = 5000) {
    const id = ++nextId;
    toasts.value.push({ id, type, title, description });
    if (import.meta.client && ttl > 0) setTimeout(() => dismiss(id), ttl);
    return id;
  }

  return {
    toasts,
    dismiss,
    success: (title: string, description?: string) => push('success', title, description),
    error: (title: string, description?: string) => push('error', title, description, 8000),
    info: (title: string, description?: string) => push('info', title, description),
  };
}
