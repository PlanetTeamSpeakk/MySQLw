package com.ptsmods.mysqlw.table.configurable;

import java.util.function.Function;

/**
 * A function that requires an amount of parameters to be configured. May also use a set of default parameters if none is supplied.
 * @param <T> The type that's required to configure this function.
 */
@FunctionalInterface
public interface DynamicNConfigurable<T> extends Function<T[], String>, Defaultable<T[], DynamicNConfigurable<T>> {

    default String applyDefault() {
        throw new UnsupportedOperationException();
    }

    default boolean hasDefault() {
        return false;
    }

    @SuppressWarnings("unchecked")
    default DynamicNConfigurable<T> withDefault(T... def) {
        return new DefaultedDynamicNConfigurable<>(this, def);
    }

    class DefaultedDynamicNConfigurable<T> implements DynamicNConfigurable<T> {
        private final DynamicNConfigurable<T> parent;
        private final T[] def;

        @SafeVarargs
        private DefaultedDynamicNConfigurable(DynamicNConfigurable<T> parent, T... def) {
            this.parent = parent;
            this.def = def;
        }

        @Override
        public String apply(T[] t) {
            return parent.apply(t);
        }

        @Override
        public String applyDefault() {
            return apply(def);
        }

        @Override
        public boolean hasDefault() {
            return true;
        }
    }
}
