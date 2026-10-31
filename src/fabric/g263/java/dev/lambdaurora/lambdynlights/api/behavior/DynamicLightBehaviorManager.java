package dev.lambdaurora.lambdynlights.api.behavior;

/** Compile-only copy of the LambDynamicLights API (MIT). Not shipped in the jar. */
public interface DynamicLightBehaviorManager {
    void add(DynamicLightBehavior source);

    boolean remove(DynamicLightBehavior source);
}
