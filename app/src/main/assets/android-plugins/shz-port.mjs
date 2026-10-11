/**
 * [dsh-android] Managed shell variable: DSH_SHZ_PORT.
 *
 * Why this plugin exists: the app exports DSH_SHZ_PORT into the engine process
 * environment (Shizuku bridge HTTP port, = engine_port + 2), but
 * dsh-subprocess.scrubbedParentEnv() drops every DSH_* variable before spawning
 * agent shells. Without this registration, the agent's shell sees an empty value
 * and cannot construct direct HTTP calls to the Shizuku bridge (127.0.0.1:3082).
 *
 * Registering it through shellEnv.register() lets the agent discover the bridge
 * port dynamically — matching how dsh-web-app registers DSH_WEB_URL.
 *
 * Loaded from the app's overlay patch by relative path (the insert entry turns
 * the filesystem path into a file:// URL). Needs no package installation.
 *
 * @module dsh-android-shz-port
 */

/** Environment variable the app exports into the engine process. */
const PORT_ENV = "DSH_SHZ_PORT";

/** Managed variable name the agent's shell reads (keeps the DSH_ prefix). */
const PORT_KEY = "DSH_SHZ_PORT";

/** Cordis plugin name. */
const name = "android-shz-port";

/** Service this plugin needs before it can register its contribution. */
const inject = ["shellEnv"];

/**
 * Provide the managed variable.
 * @param ctx - plugin context whose `shellEnv` registry receives the contribution.
 */
function apply(ctx) {
  ctx.shellEnv.register({
    name,
    variables: {
      [PORT_KEY]: {
        description:
          "HTTP port for the Shizuku bridge (adb-level command execution). " +
          "Set to engine_port + 2 (default 3082) in Shizuku mode; empty string " +
          "in normal/root mode. The `shz` wrapper reads this directly.",
      },
    },
    // Resolve per shell call: the port is stable for the app process lifetime,
    // but resolving keeps the registry from caching a stale value across restarts.
    // Empty string outside Shizuku mode (the shz wrapper is not injected then).
    resolve: () => ({ [PORT_KEY]: process.env[PORT_ENV] || "" }),
  });
}

export { apply, inject, name };
