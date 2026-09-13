<script setup lang="ts">
/**
 * A real <table> for a real comparison.
 *
 * The site had no table, list or definition list anywhere, so every comparison
 * was a grid of divs — invisible as structure to a screen reader and to an
 * answer engine looking for the row that says "Connection". This renders the
 * README's scrcpy/ADB comparison as semantic markup: <thead>, a <th scope="col">
 * per column, and a <th scope="row"> per row, so "Interface / Aster" is
 * announced with both of its headers.
 *
 * The scroll container carries role="region", an accessible name and tabindex=0
 * so that a keyboard user who cannot swipe can still scroll it horizontally on
 * a narrow screen (WCAG 2.1.1), and so the region is announced when focused.
 */
import type { Comparison } from '~/data/faq'

const props = withDefaults(
  defineProps<{
    comparison: Comparison
    /** Header for the row-label column, which the source table leaves blank. */
    rowHeader?: string
    /** Accessible name for the table and its scroll region. */
    label?: string
    /** Visible caption. Falls back to the label. */
    caption?: string | null
  }>(),
  {
    rowHeader: 'Aspect',
    label: 'Aster compared to scrcpy and raw ADB',
    caption: null,
  },
)

// The region attributes are measured, not assumed — see useScrollableRegion.
// This table fits at 1440px, where an unconditional tabindex left a tab stop
// announcing a scrollable region that does not scroll.
const region = useScrollableRegion(props.label)
</script>

<template>
  <div
    :ref="(el) => (region.el.value = el as HTMLElement)"
    v-bind="region.attrs.value"
    class="cmp rounded-2xl border border-border-dim bg-surface-raised/60"
  >
    <table class="cmp__table">
      <caption class="cmp__caption">
        {{ caption ?? label }}
      </caption>
      <thead>
        <tr>
          <th scope="col" class="cmp__th cmp__th--row-label">{{ rowHeader }}</th>
          <th
            v-for="(column, i) in comparison.columns"
            :key="column"
            scope="col"
            class="cmp__th"
            :class="{ 'cmp__th--accent': i === comparison.columns.length - 1 }"
          >
            {{ column }}
          </th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="row in comparison.rows" :key="row.label">
          <th scope="row" class="cmp__rowhead">{{ row.label }}</th>
          <td
            v-for="(value, i) in row.values"
            :key="`${row.label}-${i}`"
            class="cmp__td"
            :class="{ 'cmp__td--accent': i === row.values.length - 1 }"
          >
            {{ value }}
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<style scoped>
.cmp {
  overflow-x: auto;
}

.cmp:focus-visible {
  outline: 2px solid var(--color-primary);
  outline-offset: 2px;
}

.cmp__table {
  width: 100%;
  min-width: 40rem;
  border-collapse: collapse;
  text-align: left;
}

.cmp__caption {
  caption-side: top;
  padding: 1rem 1.25rem 0.75rem;
  text-align: left;
  font-size: 0.75rem;
  font-weight: 600;
  letter-spacing: 0.2em;
  text-transform: uppercase;
  color: var(--color-text-tertiary);
}

.cmp__th {
  padding: 0.75rem 1.25rem;
  border-bottom: 1px solid var(--color-border-subtle);
  font-size: 0.8125rem;
  font-weight: 600;
  color: var(--color-text-primary);
  vertical-align: bottom;
}

.cmp__th--row-label {
  width: 9rem;
  color: var(--color-text-tertiary);
}

.cmp__th--accent {
  color: var(--color-aster);
}

.cmp__rowhead {
  padding: 0.875rem 1.25rem;
  border-bottom: 1px solid var(--color-border-dim);
  font-size: 0.8125rem;
  font-weight: 600;
  color: var(--color-text-primary);
  vertical-align: top;
  white-space: nowrap;
}

.cmp__td {
  padding: 0.875rem 1.25rem;
  border-bottom: 1px solid var(--color-border-dim);
  font-size: 0.8125rem;
  line-height: 1.6;
  color: var(--color-text-secondary);
  vertical-align: top;
}

.cmp__td--accent {
  color: var(--color-text-primary);
}

.cmp__table tbody tr:last-child .cmp__rowhead,
.cmp__table tbody tr:last-child .cmp__td {
  border-bottom: 0;
}
</style>
