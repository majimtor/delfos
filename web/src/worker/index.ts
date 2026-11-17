/**
 * The Worker behind the web. Cloudflare answers navigations and asset requests from the built
 * files without running this code, so it exists for two things: the scheduled handler requests
 * the liveness probe of the API so the free instance stays awake during the day (plan, section
 * 12), and the fetch handler hands the few requests that reach it, such as a crawler asking for a
 * path that is not a file, to the assets, where unknown paths fall back to index.html. The
 * runtime accepts handlers only as exports of this module; the logic lives in liveness.ts.
 */
import { requestLiveness } from './liveness'

interface Env {
  /** The liveness probe of the API, set in wrangler.jsonc. */
  API_LIVENESS_URL: string
  /** The built web. */
  ASSETS: { fetch(request: Request): Promise<Response> }
}

interface ScheduledController {
  cron: string
  scheduledTime: number
}

export default {
  async scheduled(_controller: ScheduledController, env: Env): Promise<void> {
    await requestLiveness(env.API_LIVENESS_URL)
  },
  fetch(request: Request, env: Env): Promise<Response> {
    return env.ASSETS.fetch(request)
  },
}
