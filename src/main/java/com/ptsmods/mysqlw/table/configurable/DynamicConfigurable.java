package com.ptsmods.mysqlw.table.configurable;

import java.util.function.Function;

/**
 * A function that requires a parameter to be configured. May also use a default parameter if none is supplied.
 * @param <T> The type that's required to configure this function.
 */
@FunctionalInterface
public interface DynamicConfigurable<T> extends Function<T, String>, Defaultable<T, DynamicConfigurable<T>> {

    default DynamicConfigurable<T> withDefault(T def) {
        return new DefaultedDynamicConfigurable<>(this, def);
    }

    class DefaultedDynamicConfigurable<T> implements DynamicConfigurable<T> {
        private final DynamicConfigurable<T> parent;
        private final T def;

        private DefaultedDynamicConfigurable(DynamicConfigurable<T> parent, T def) {
            this.parent = parent;
            this.def = def;
        }

        @Override
        public String apply(T t) {
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
