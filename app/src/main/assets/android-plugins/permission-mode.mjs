/**
 * [dsh-android] Managed shell variable: DSH_PERMISSION_MODE.
 *
 * Why this plugin exists: the app exports DSH_PERMISSION_MODE into the engine
 * process environment (always "danger-full-access" on Android since no sandbox
 * backend exists), but dsh-subprocess.scrubbedParentEnv() drops DSH_* vars
 * before spawning agent shells. Without registration, the agent cannot inspect
 * the current permission preset programmatically.
 *
 * The value mirrors the `permission` row's `defaultPreset` in android-overlay.yml
 * (see EngineConfig.ensureAndroidOverlay). The security boundary on Android is
 * the app's privilege mode (DSH_ANDROID_PRIV_MODE) and the su gate, not this
 * preset — the agent can use it to confirm the sandbox is in the expected state.
 *
 * Loaded from the app's overlay patch by relative path. Needs no package
 * installation.
 *
 * @module dsh-android-permission-mode
 */

/** Environment variable the app exports into the engine process. */
const MODE_ENV = "DSH_PERMISSION_MODE";

/** Managed variable name the agent's shell reads (keeps the DSH_ prefix). */
const MODE_KEY = "DSH_PERMISSION_MODE";

/** Cordis plugin name. */
const name = "android-permission-mode";

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
      [MODE_KEY]: {
        description:
          "dsh permission preset for this session. On Android always " +
          "'danger-full-access' (no landlock/seatbelt sandbox backend exists). " +
          "Security boundary is the app's privilege mode " +
          "(DSH_ANDROID_PRIV_MODE) and the su gate, not this preset.",
      },
    },
    // Resolve per shell call: the mode is fixed for the app process lifetime,
    // but per-call resolution ensures a regenerated value (if the overlay
    // changes) is picked up without restarting the registry.
    resolve: () => ({ [MODE_KEY]: process.env[MODE_ENV] || "" }),
  });
}

export { apply, inject, name };
