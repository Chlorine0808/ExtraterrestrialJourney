package chlorine.etjourney.core.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/** Reads fields that subclasses declare to hide one of their base class, found by type rather than by name. */
public final class ShadowFields {

    private ShadowFields() {}

    /**
     * The non-null value of the most derived instance field of the given type, declared below base; null when no
     * subclass declares one.
     */
    public static <T> T find(Object owner, Class<?> base, Class<T> type) {
        for (Class<?> c = owner.getClass(); c != null && c != base; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers()) || !type.isAssignableFrom(f.getType())) continue;
                try {
                    f.setAccessible(true);
                    Object value = f.get(owner);
                    if (value != null) return type.cast(value);
                } catch (ReflectiveOperationException | RuntimeException e) {
                    // An inaccessible field is treated as absent.
                }
            }
        }
        return null;
    }
}
