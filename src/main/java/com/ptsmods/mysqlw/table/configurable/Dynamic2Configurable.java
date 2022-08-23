package com.ptsmods.mysqlw.table.configurable;

import com.ptsmods.mysqlw.Pair;

import java.util.function.BiFunction;

/**
 * A function that requires a parameter to be configured. May also use a default parameter if none is supplied.
 * @param <T1> The first type that's required to configure this function.
 * @param <T2> The second type that's required to configure this function.
 */
@FunctionalInterface
public interface Dynamic2Configurable<T1, T2> extends BiFunction<T1, T2, String>, Defaultable<Pair<T1, T2>, Dynamic2Configurable<T1, T2>> {

    default Dynamic2Configurable<T1, T2> withDefault(Pair<T1, T2> def) {
        return new DefaultedDynamic2Configurable<>(this, def.getLeft(), def.getRight());
    }

    class DefaultedDynamic2Configurable<T1, T2> implements Dynamic2Configurable<T1, T2> {
        private final Dynamic2Configurable<T1, T2> parent;
        private final T1 def1;
        private final T2 def2;

        private DefaultedDynamic2Configurable(Dynamic2Configurable<T1, T2> parent, T1 def1, T2 def2) {
            this.parent = parent;
            this.def1 = def1;
            this.def2 = def2;
        }

        @Override
        public String apply(T1 t1, T2 t2) {
            return parent.apply(t1, t2);
        }

        @Override
        public String applyDefault() {
            return apply(def1, def2);
        }

        @Override
        public boolean hasDefault() {
            return true;
        }
    }
}
