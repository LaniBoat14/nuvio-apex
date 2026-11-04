package com.nuvio.tv.core.player.thumbnail

import java.io.IOException

/**
 * The server asked us to slow down (429/503). [retryAfterMs] is the server's Retry-After, null when it sent none;
 * [url] is the address that answered, after redirects.
 *
 * Ported unchanged from Nuvio Arc (it lives beside Arc's range reader, which Apex does not carry).
 */
internal class RateLimitedException(
    val retryAfterMs: Long?,
    val status: Int = TOO_MANY_REQUESTS,
    val url: String? = null,
) : IOException("rate limited") {
    companion object {
        const val TOO_MANY_REQUESTS = 429
    }
}
