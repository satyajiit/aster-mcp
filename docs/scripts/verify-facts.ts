import { readFile, readdir } from 'node:fs/promises'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'
import { FACTS, ON_DEVICE_PORT, TOOL_COUNTS, TOOL_PREFIX } from '../app/data/site'
import { ALL_TOOLS } from '../app/data/tools'
import { ALL_ON_DEVICE_ACTIONS } from '../app/data/on-device'

/**
 * Build-time assertions over the claims the site makes about the product.
 *
 * Every number on this site is written down twice: once in app/data/*.ts, where
 * the pages and the twins read it, and once in the code it describes. This file
 * recomputes the second copy from source and throws when the two disagree, so a
 * drifted fact fails the build instead of being published and then cited by an
 * assistant. It is the reason app/data/site.ts can carry a provenance comment
 * that stays true.
 *
 * It runs from the same `nitro:build:public-assets` hook as the generator (see
 * nuxt.config.ts); `pnpm nuxt generate` fires nitro hooks but never package.json
 * pre/post scripts, so this is the only place it reliably runs in CI.
 *
 * If the sibling source trees are absent — a docs-only checkout — the checks are
 * SKIPPED with a warning rather than failing: the point is to catch drift in CI,
 * where actions/checkout gives us the whole repo, not to make the site
 * unbuildable elsewhere.
 */

const HERE = dirname(fileURLToPath(import.meta.url))
const REPO = join(HERE, '..', '..')

const problems: string[] = []

/**
 * Which source probes actually resolved. A gate that silently degrades to
 * "nothing to check" is worse than no gate: the build goes green and the exit
 * status cannot tell a verified run from a skipped one. CI sets ASTER_STRICT_FACTS
 * (the docs workflow checks out the whole repo, so every probe must resolve
 * there); locally a partial checkout still builds, but says loudly what it did
 * not verify.
 */
const ran = new Set<string>()
const skipped: string[] = []

function skip(name: string, why: string): void {
  skipped.push(`${name} (${why})`)
  console.warn(`[verify-facts] SKIPPED ${name} — ${why}`)
}

function check(ok: boolean, message: string): void {
  if (!ok) problems.push(message)
}

async function read(rel: string): Promise<string | null> {
  try {
    return await readFile(join(REPO, rel), 'utf8')
  } catch {
    return null
  }
}

/** The 49 `aster_*` names an MCP client sees. */
function serverToolNames(src: string): string[] {
  return [...src.matchAll(/name:\s*'(aster_[a-z0-9_]+)'/g)].map((m) => m[1]!)
}

/** Body of the object whose opening brace is at `open`, braces balanced. */
function balanced(src: string, open: number): string {
  let depth = 0
  for (let i = open; i < src.length; i++) {
    if (src[i] === '{') depth++
    else if (src[i] === '}') {
      depth--
      if (depth === 0) return src.slice(open + 1, i)
    }
  }
  throw new Error('unbalanced object in mcp/src/mcp/tools.ts')
}

/** Keys at depth 0 of an object body — nested schemas must not leak in. */
function topLevelKeys(body: string): string[] {
  const keys: string[] = []
  let depth = 0
  for (let i = 0; i < body.length; i++) {
    const c = body[i]!
    if (c === '{' || c === '[') depth++
    else if (c === '}' || c === ']') depth--
    else if (depth === 0 && (i === 0 || ',\n{ '.includes(body[i - 1]!))) {
      const m = /^\s*([A-Za-z_$][\w$]*)\s*:/.exec(body.slice(i))
      if (m) {
        keys.push(m[1]!)
        i += m[0].length - 1
      }
    }
  }
  return keys
}

/** Value text following a depth-0 key, or null. */
function topLevelValue(body: string, key: string): string | null {
  let depth = 0
  for (let i = 0; i < body.length; i++) {
    const c = body[i]!
    if (c === '{' || c === '[') depth++
    else if (c === '}' || c === ']') depth--
    else if (depth === 0 && (i === 0 || ',\n{ '.includes(body[i - 1]!))) {
      const m = new RegExp(`^\\s*${key}\\s*:\\s*`).exec(body.slice(i))
      if (m) return body.slice(i + m[0].length)
    }
  }
  return null
}

/**
 * Argument names and requiredness per tool, straight out of each inputSchema.
 *
 * Brace-balanced on purpose. A naive "first `required:` after the tool name"
 * scan picks up the one nested inside an array item's schema — on
 * aster_post_notification that yields ['id','label'] and every real argument
 * reads as optional.
 */
function serverToolArgs(src: string): Map<string, { name: string; required: boolean }[]> {
  const out = new Map<string, { name: string; required: boolean }[]>()
  for (const m of src.matchAll(/name:\s*'(aster_[a-z0-9_]+)'/g)) {
    const schemaAt = src.indexOf('inputSchema:', m.index! + m[0].length)
    if (schemaAt === -1) continue
    const body = balanced(src, src.indexOf('{', schemaAt))
    const propsValue = topLevelValue(body, 'properties')
    const params = propsValue?.trimStart().startsWith('{')
      ? topLevelKeys(balanced(propsValue, propsValue.indexOf('{')))
      : []
    const requiredValue = topLevelValue(body, 'required')
    const required = new Set<string>(
      requiredValue?.trimStart().startsWith('[')
        ? [...requiredValue.slice(0, requiredValue.indexOf(']')).matchAll(/'([\w$]+)'/g)].map((r) => r[1]!)
        : [],
    )
    out.set(
      m[1]!,
      [...params.filter((p) => required.has(p)), ...params.filter((p) => !required.has(p))].map((name) => ({
        name,
        required: required.has(name),
      })),
    )
  }
  return out
}

/**
 * The on-device action set: the union of every handler's supportedActions().
 *
 * Paren-balanced on purpose. A line-oriented scan stops at the first `)` and
 * silently under-counts by eight — AccessibilityHandler alone declares eighteen
 * names across several lines.
 */
function onDeviceActionNames(sources: string[]): string[] {
  const names = new Set<string>()
  for (const src of sources) {
    const at = src.indexOf('supportedActions()')
    if (at === -1) continue
    const open = src.indexOf('(', src.indexOf('listOf', at))
    if (open === -1) continue
    let depth = 0
    let end = open
    for (let i = open; i < src.length; i++) {
      if (src[i] === '(') depth++
      else if (src[i] === ')') {
        depth--
        if (depth === 0) {
          end = i
          break
        }
      }
    }
    for (const m of src.slice(open, end + 1).matchAll(/"([a-z0-9_.]+)"/g)) names.add(m[1]!)
  }
  return [...names]
}

function diff(label: string, expected: string[], actual: string[]): void {
  const e = new Set(expected)
  const a = new Set(actual)
  const missing = [...e].filter((x) => !a.has(x)).sort()
  const extra = [...a].filter((x) => !e.has(x)).sort()
  if (missing.length) problems.push(`${label}: the site claims ${missing.length} name(s) the source does not have — ${missing.join(', ')}`)
  if (extra.length) problems.push(`${label}: the source has ${extra.length} name(s) the site never lists — ${extra.join(', ')}`)
}

export async function verifyFacts(publicDir: string): Promise<void> {
  // ---------------------------------------------------- the 49 MCP tools ---
  const toolsTs = await read('mcp/src/mcp/tools.ts')
  if (toolsTs) {
    const names = serverToolNames(toolsTs)
    const unique = [...new Set(names)]
    check(
      unique.length === TOOL_COUNTS.mcpServer,
      `TOOL_COUNTS.mcpServer is ${TOOL_COUNTS.mcpServer} but mcp/src/mcp/tools.ts declares ${unique.length} unique aster_* tools`,
    )
    diff('MCP tools', ALL_TOOLS.map((t) => TOOL_PREFIX + t.name.replace(TOOL_PREFIX, '')), unique)

    // /tools publishes the argument names, so they are a claim like any other.
    const args = serverToolArgs(toolsTs)
    for (const tool of ALL_TOOLS) {
      const actual = args.get(TOOL_PREFIX + tool.name)
      if (!actual) continue
      const shown = tool.args.map((a) => `${a.name}${a.required ? '!' : '?'}`).join(',')
      const real = actual.map((a) => `${a.name}${a.required ? '!' : '?'}`).join(',')
      check(shown === real, `${TOOL_PREFIX}${tool.name} arguments drifted — site says [${shown}], schema says [${real}]`)
    }
  } else {
    skip('mcp-tools', 'mcp/src/mcp/tools.ts not found')
  }

  // --------------------------------------------- the on-device catalogue ---
  const handlerDir = 'apps/android/app/src/main/java/com/aster/service/handlers'
  let handlerFiles: string[] = []
  try {
    handlerFiles = (await readdir(join(REPO, handlerDir))).filter((f) => f.endsWith('.kt'))
  } catch {
    /* handled below */
  }
  if (handlerFiles.length) {
    const sources = (await Promise.all(handlerFiles.map((f) => read(join(handlerDir, f))))).filter(
      (s): s is string => s !== null,
    )
    const actions = onDeviceActionNames(sources)
    check(
      actions.length === TOOL_COUNTS.onDevice,
      `TOOL_COUNTS.onDevice is ${TOOL_COUNTS.onDevice} but the handler map registers ${actions.length} unique actions`,
    )
    diff('On-device actions', ALL_ON_DEVICE_ACTIONS.map((a) => a.action), actions)

    // The arithmetic the pages quote, re-derived rather than trusted.
    if (toolsTs) {
      const server = new Set(serverToolNames(toolsTs).map((n) => n.replace(TOOL_PREFIX, '')))
      const device = new Set(actions)
      const onlyServer = [...server].filter((s) => !device.has(s))
      const onlyDevice = ALL_ON_DEVICE_ACTIONS.filter((a) => a.server === null).length
      // Shared is REACHABILITY, not name equality. Counting names gives 47 and
      // leaves click_by_view_id (reached by aster_click_by_id) in neither
      // bucket, so neither published sum adds up to a catalogue size.
      const shared = ALL_ON_DEVICE_ACTIONS.filter((a) => a.server !== null).length
      check(shared === TOOL_COUNTS.shared, `TOOL_COUNTS.shared is ${TOOL_COUNTS.shared} but ${shared} on-device actions map to an aster_* tool`)
      // The two sums the pages print. If either fails, some page is published
      // with arithmetic a reader can disprove with a calculator.
      check(
        TOOL_COUNTS.shared + TOOL_COUNTS.onDeviceOnly === TOOL_COUNTS.onDevice,
        `${TOOL_COUNTS.shared} reachable + ${TOOL_COUNTS.onDeviceOnly} on-device-only !== ${TOOL_COUNTS.onDevice} on-device actions`,
      )
      check(
        TOOL_COUNTS.shared + TOOL_COUNTS.serverOnly === TOOL_COUNTS.mcpServer,
        `${TOOL_COUNTS.shared} reachable + ${TOOL_COUNTS.serverOnly} server-only !== ${TOOL_COUNTS.mcpServer} MCP tools`,
      )
      // Every `server` mapping must name a tool that exists.
      for (const a of ALL_ON_DEVICE_ACTIONS) {
        if (a.server && !server.has(a.server.replace(TOOL_PREFIX, ''))) {
          problems.push(`on-device.ts maps ${a.action} to ${a.server}, which is not a registered MCP tool`)
        }
      }
      check(
        onlyDevice === TOOL_COUNTS.onDeviceOnly,
        `TOOL_COUNTS.onDeviceOnly is ${TOOL_COUNTS.onDeviceOnly} but on-device.ts marks ${onlyDevice} actions unreachable from MCP`,
      )
      // click_by_id is a rename of click_by_view_id, not a missing capability,
      // so it is excluded before counting genuinely server-only tools.
      const trulyServerOnly = onlyServer.filter((s) => s !== 'click_by_id')
      check(
        trulyServerOnly.length === TOOL_COUNTS.serverOnly,
        `TOOL_COUNTS.serverOnly is ${TOOL_COUNTS.serverOnly} but ${trulyServerOnly.length} server tools have no on-device action — ${trulyServerOnly.join(', ')}`,
      )
      check(
        onlyServer.includes('click_by_id'),
        'aster_click_by_id no longer looks like a rename of click_by_view_id — re-check the TOOL_COUNTS comment in app/data/site.ts',
      )
    }
  } else {
    skip('on-device-catalogue', 'apps/android handlers not found')
  }

  // --------------------------------------------------- versions and floor ---
  const gradle = await read('apps/android/app/build.gradle.kts')
  if (gradle) {
    const minSdk = gradle.match(/minSdk\s*=\s*(\d+)/)?.[1]
    const versionName = gradle.match(/versionName\s*=\s*"([^"]+)"/)?.[1]
    check(minSdk === String(FACTS.androidMinApi), `FACTS.androidMinApi is ${FACTS.androidMinApi} but build.gradle.kts sets minSdk = ${minSdk}`)
    check(versionName === FACTS.appVersion, `FACTS.appVersion is ${FACTS.appVersion} but build.gradle.kts sets versionName = "${versionName}"`)
  } else {
    skip('android-versions', 'apps/android/app/build.gradle.kts not found')
  }

  // ------------------------------------------------- on-device MCP port ---
  // Stated as a default on four surfaces. Before ON_DEVICE_PORT existed it was
  // typed out by hand in five places, which is exactly how a number drifts.
  const settings = await read(
    'apps/android/app/src/main/java/com/aster/data/local/SettingsDataStore.kt',
  )
  if (settings) {
    // `prefs[Keys.MCP_PORT] ?: 8080` — the elvis default is the shipped default.
    const port = settings.match(/Keys\.MCP_PORT\]\s*\?:\s*(\d+)/)?.[1]
    check(
      port === String(ON_DEVICE_PORT),
      `ON_DEVICE_PORT is ${ON_DEVICE_PORT} but SettingsDataStore.kt defaults the on-device MCP port to ${port ?? '(not found)'}`,
    )
  } else {
    skip('on-device-port', 'SettingsDataStore.kt not found')
  }

  const pkg = await read('mcp/package.json')
  if (pkg) {
    const version = JSON.parse(pkg).version
    check(version === FACTS.serverVersion, `FACTS.serverVersion is ${FACTS.serverVersion} but mcp/package.json is at ${version}`)
  } else {
    skip('server-version', 'mcp/package.json not found')
  }

  // ------------------------------------- the emitted files themselves ------
  // A renamed FIELD interpolates as the literal string "undefined" and the
  // build stays green; only a renamed module export throws. This closes that.
  // Every prerendered page too, not just the generated four: a renamed field in
  // a component-local literal interpolates as the string "undefined" and ships
  // with a green build otherwise.
  const htmlPages: string[] = []
  async function collectHtml(dir: string, prefix = ''): Promise<void> {
    for (const entry of await readdir(join(publicDir, dir), { withFileTypes: true })) {
      const rel = prefix ? `${prefix}/${entry.name}` : entry.name
      // _nuxt holds minified framework code; "undefined" appears there as a
      // legitimate string literal and is not a content defect.
      if (entry.isDirectory() && entry.name !== '_nuxt') await collectHtml(join(dir, entry.name), rel)
      else if (entry.isFile() && entry.name.endsWith('.html')) htmlPages.push(rel)
    }
  }
  await collectHtml('.')

  const emitted = ['llms.txt', 'llms-full.txt', 'sitemap.xml', 'robots.txt', ...htmlPages]
  for (const name of emitted) {
    let body: string
    try {
      body = await readFile(join(publicDir, name), 'utf8')
    } catch {
      problems.push(`${name} was not written`)
      continue
    }
    for (const poison of ['undefined', 'NaN', '[object Object]']) {
      if (body.includes(poison)) problems.push(`${name} contains the literal "${poison}" — a data field was renamed or removed`)
    }
    if (!name.endsWith('.html') && body.length < 200) {
      problems.push(`${name} is only ${body.length} bytes — it rendered empty`)
    }
  }

  const EXPECTED = ['mcp-tools', 'on-device-catalogue', 'android-versions', 'on-device-port', 'server-version']
  for (const name of EXPECTED) if (!skipped.some((k) => k.startsWith(name))) ran.add(name)
  if (skipped.length && process.env.ASTER_STRICT_FACTS === 'true') {
    problems.push(`${skipped.length} check(s) could not run and ASTER_STRICT_FACTS is set: ${skipped.join('; ')}`)
  }

  if (problems.length) {
    throw new Error(
      `[verify-facts] ${problems.length} claim(s) on the site no longer match the source:\n` +
        problems.map((p) => `  - ${p}`).join('\n'),
    )
  }
  console.log(
    `[verify-facts] ${ran.size}/${EXPECTED.length} source checks ran and passed` +
      (skipped.length ? ` — NOT VERIFIED: ${skipped.join('; ')}` : ': MCP tools, on-device catalogue, versions, Android floor, on-device port'),
  )
}
