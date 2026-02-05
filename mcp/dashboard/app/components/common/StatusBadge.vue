<script setup lang="ts">
defineProps<{
  status: 'online' | 'offline' | 'pending' | 'approved' | 'rejected';
  showDot?: boolean;
}>();

const statusConfig = {
  online: { label: 'ONLINE', class: 'badge-emerald', dotClass: 'status-online' },
  offline: { label: 'OFFLINE', class: 'badge-muted', dotClass: 'status-offline' },
  pending: { label: 'PENDING', class: 'badge-amber', dotClass: 'status-pending' },
  approved: { label: 'APPROVED', class: 'badge-emerald', dotClass: 'status-online' },
  rejected: { label: 'REJECTED', class: 'badge-rose', dotClass: 'status-error' },
};
</script>

<template>
  <span class="badge" :class="statusConfig[status].class">
    <span v-if="showDot !== false" class="status-dot" :class="statusConfig[status].dotClass"></span>
    {{ statusConfig[status].label }}
  </span>
</template>
