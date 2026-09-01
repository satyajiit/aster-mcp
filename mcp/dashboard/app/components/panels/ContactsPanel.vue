<script setup lang="ts">
const props = defineProps<{ deviceId: string }>();

/**
 * Normalised contact. The device speaks snake_case — `contact_id`,
 * `display_name`, and `phones` as {number, type} objects — so every camelCase
 * read here used to come back undefined: names rendered "Unknown", numbers were
 * blank, and selection (keyed on `id`) never enabled the delete button.
 */
interface Contact { id?: string; name?: string; numbers?: string[] }

function normalise(raw: any): Contact {
  const phones = Array.isArray(raw?.phones)
    ? raw.phones.map((ph: any) => (typeof ph === 'string' ? ph : ph?.number)).filter(Boolean)
    : (raw?.numbers ?? (raw?.number ? [raw.number] : []));
  return {
    id: String(raw?.contact_id ?? raw?.id ?? ''),
    name: raw?.display_name ?? raw?.displayName ?? raw?.name ?? '',
    numbers: phones,
  };
}

const { data, loading, error, run, toast } = useDeviceTool<any>(props.deviceId);
const query = ref('');
const cursor = ref<string | null>(null);
const all = ref<Contact[]>([]);
const nextCursor = ref<string | null>(null);
const selected = ref<Set<string>>(new Set());
const confirming = ref(false);

function extract(d: any): { contacts: Contact[]; cursor: string | null } {
  if (!d) return { contacts: [], cursor: null };
  const rows = Array.isArray(d) ? d : (d.contacts ?? []);
  return { contacts: rows.map(normalise), cursor: d.next_cursor ?? null };
}

async function search() {
  // The device rejects a search with neither name nor number. Clearing the box
  // used to wipe the loaded address book and replace it with an error toast;
  // now it just reloads the full list.
  if (!query.value.trim()) { await loadPage(true); return; }
  const d = await run('aster_search_contacts', { name: query.value.trim(), limit: 50 }, 'Search contacts');
  const { contacts } = extract(d);
  if (contacts.length || !error.value) all.value = contacts;
  nextCursor.value = null;
}

async function loadPage(reset = false) {
  if (reset) { all.value = []; cursor.value = null; }
  // The generic tool form could never drive `cursor`, so the full address book
  // was unreachable from the dashboard even though the server pages it.
  const d = await run('aster_list_contacts_full', { cursor: cursor.value ?? undefined, limit: 200 }, 'List contacts');
  const { contacts, cursor: next } = extract(d);
  all.value = [...all.value, ...contacts];
  nextCursor.value = next;
  cursor.value = next;
}

function toggle(id: string) {
  if (selected.value.has(id)) selected.value.delete(id);
  else selected.value.add(id);
  selected.value = new Set(selected.value);
}

async function remove() {
  const ids = [...selected.value];
  const res: any = await run('aster_delete_contacts', { ids }, 'Delete contacts');
  confirming.value = false;
  if (res !== null) {
    // Report what the device actually deleted, not what we asked for.
    const done = res?.deleted ?? ids.length;
    const failed = res?.failed?.length ?? 0;
    if (failed) toast.error(`Deleted ${done}, ${failed} failed`);
    else toast.success(`Deleted ${done} contact${done === 1 ? '' : 's'}`);
    selected.value = new Set();
    await loadPage(true);
  }
}

function nameOf(c: Contact) { return c.name || 'Unknown'; }
function numbersOf(c: Contact) { return c.numbers?.join(', ') || ''; }

onMounted(() => loadPage(true));
</script>

<template>
  <PanelShell
    title="Contacts"
    :description="`${all.length} loaded${nextCursor ? ' — more available' : ''}`"
    :loading="loading && all.length === 0"
    :error="error"
    :empty="!loading && all.length === 0"
    empty-title="No contacts"
    empty-icon="ph:address-book"
  >
    <template #actions>
      <AButton v-if="selected.size" variant="danger" size="sm" icon="ph:trash" @click="confirming = true">
        Delete {{ selected.size }}
      </AButton>
      <AButton size="sm" icon="ph:arrows-clockwise" @click="loadPage(true)">Reload</AButton>
    </template>

    <template #controls>
      <form class="bar" @submit.prevent="search">
        <input v-model="query" class="control" placeholder="Search by name" />
        <AButton type="submit" size="sm" icon="ph:magnifying-glass">Search</AButton>
      </form>
    </template>

    <div class="list">
      <label v-for="(c, i) in all" :key="c.id ?? i" class="row">
        <input
          type="checkbox"
          :checked="c.id ? selected.has(c.id) : false"
          :disabled="!c.id"
          @change="c.id && toggle(c.id)"
        />
        <AIconTile icon="ph:user" :size="30" accent="var(--color-info)" />
        <div class="row__body">
          <p class="row__name">{{ nameOf(c) }}</p>
          <p class="row__num mono">{{ numbersOf(c) }}</p>
        </div>
      </label>
    </div>

    <AButton v-if="nextCursor" class="more" :loading="loading" @click="loadPage()">
      Load more
    </AButton>

    <AModal
      :open="confirming"
      title="Delete contacts?"
      :description="`${selected.size} contact(s) will be removed from the device.`"
      @update:open="(v) => !v && (confirming = false)"
    >
      <template #footer>
        <AButton size="sm" @click="confirming = false">Cancel</AButton>
        <AButton variant="danger" size="sm" :loading="loading" @click="remove">Delete</AButton>
      </template>
    </AModal>
  </PanelShell>
</template>

<style scoped>
.bar { display: flex; gap: 0.5rem; margin-bottom: 1rem; }
.list { display: grid; }
.row { display: flex; align-items: center; gap: 0.625rem; padding: 0.4375rem 0; border-bottom: 1px solid var(--color-border); cursor: pointer; }
.row:last-child { border-bottom: none; }
.row__body { min-width: 0; }
.row__name { margin: 0; font-size: var(--text-body-md); }
.row__num { margin: 0; font-size: var(--text-label-sm); color: var(--color-fg-muted); }
.more { width: 100%; margin-top: 0.75rem; }
.control { flex: 1; min-height: 36px; padding: 0.375rem 0.625rem; border-radius: var(--radius-md); border: 1px solid var(--color-border); background: var(--color-surface-2); color: var(--color-fg); font: inherit; }
.control:focus { outline: none; border-color: var(--color-primary); }
</style>
