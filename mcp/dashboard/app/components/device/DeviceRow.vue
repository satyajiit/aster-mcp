<script setup lang="ts">
import type { Device } from '~/composables/useApi';

defineProps<{ device: Device }>();
defineEmits<{ approve: [string]; reject: [string]; unreject: [string]; remove: [string] }>();

const PLATFORM_ICON: Record<string, string> = {
  android: 'ph:android-logo',
  ios: 'ph:apple-logo',
};
</script>

<template>
  <div class="drow">
    <NuxtLink :to="`/devices/${device.id}`" class="drow__main focus-ring">
      <AIconTile
        :icon="PLATFORM_ICON[device.platform] ?? 'ph:device-mobile'"
        :accent="device.online ? 'var(--color-primary)' : 'var(--color-fg-muted)'"
      />
      <div class="drow__id">
        <span class="drow__name">{{ device.name }}</span>
        <span class="drow__meta mono truncate-mid">{{ shortId(device.id, 12) }}</span>
      </div>
      <div class="drow__model">
        <span>{{ device.manufacturer }} {{ device.model }}</span>
        <span class="drow__os">{{ device.platform }} {{ device.osVersion }}</span>
      </div>
      <AStatusPill :status="device.online ? 'online' : 'offline'" />
      <AStatusPill :status="device.status" />
      <span class="drow__seen">{{ formatRelativeTime(device.lastSeen) }}</span>
    </NuxtLink>

    <div class="drow__actions">
      <template v-if="device.status === 'pending'">
        <AButton variant="primary" size="sm" @click="$emit('approve', device.id)">Approve</AButton>
        <AButton variant="ghost" size="sm" @click="$emit('reject', device.id)">Reject</AButton>
      </template>
      <template v-else-if="device.status === 'rejected'">
        <!-- Rejecting used to be a dead end: there was no way back. -->
        <AButton variant="secondary" size="sm" @click="$emit('unreject', device.id)">
          Restore
        </AButton>
        <AButton variant="ghost" size="sm" icon="ph:trash" @click="$emit('remove', device.id)" />
      </template>
      <AButton
        v-else
        variant="ghost"
        size="sm"
        icon="ph:trash"
        title="Remove device"
        @click="$emit('remove', device.id)"
      />
    </div>
  </div>
</template>

<style scoped>
.drow {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.625rem 0.75rem;
  border-bottom: 1px solid var(--color-border);
}

.drow:last-child {
  border-bottom: none;
}

.drow:hover {
  background: var(--color-surface-2);
}

.drow__main {
  display: grid;
  grid-template-columns: auto minmax(9rem, 1.4fr) minmax(9rem, 1.4fr) auto auto minmax(5rem, auto);
  align-items: center;
  gap: 0.75rem;
  flex: 1;
  min-width: 0;
  text-decoration: none;
  color: inherit;
}

.drow__id,
.drow__model {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.drow__name {
  font-size: var(--text-body-md);
  font-weight: 600;
  color: var(--color-fg);
}

.drow__meta,
.drow__os {
  font-size: var(--text-label-sm);
  color: var(--color-fg-muted);
}

.drow__model span:first-child {
  font-size: var(--text-body-sm);
  color: var(--color-fg-subtle);
}

.drow__seen {
  font-size: var(--text-label-md);
  color: var(--color-fg-muted);
  text-align: right;
}

.drow__actions {
  display: flex;
  gap: 0.25rem;
  flex: none;
}

@media (max-width: 900px) {
  .drow__main {
    grid-template-columns: auto 1fr auto;
  }

  .drow__model,
  .drow__seen {
    display: none;
  }
}
</style>
