package com.jnimble.sdk.hook;

import java.util.Map;

/**
 * Template fragment contributed by a plugin to a hook point.
 *
 * @param view plugin template fragment path
 * @param model optional template model values
 * @param order ascending render order within the hook
 * @param permission optional permission expression required for rendering
 * @param activeWhen optional activation expression
 */
public record HookViewContribution(
        String view,
        Map<String, Object> model,
        int order,
        String permission,
        String activeWhen
) {
}
