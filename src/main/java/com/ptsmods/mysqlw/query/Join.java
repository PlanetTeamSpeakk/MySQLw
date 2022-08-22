package com.ptsmods.mysqlw.query;

import com.ptsmods.mysqlw.Database;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.Singular;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.stream.Collectors;

@Data
@Builder(builderClassName = "Builder")
public class Join {
    private boolean natural;
    @NonNull
    @lombok.Builder.Default
    private JoinType type = JoinType.INNER;
    @NonNull
    private final String table;
    private final String alias;
    @Singular("using")
    private final Set<String> using;
    private final QueryCondition condition;

    private Join(boolean natural, @NotNull JoinType type, @NotNull String table, String alias, Set<String> using, QueryCondition condition) {
        this.natural = natural;
        this.type = type;
        this.table = table;
        this.alias = alias;
        this.using = using;
        this.condition = condition;

        if (!using.isEmpty() && condition != null)
            throw new IllegalArgumentException("Joins may only contain either a using statement or a condition, not both.");
    }

    @Override
    public String toString() {
        return (natural && type != JoinType.CROSS ? "NATURAL " : "") + type.name() +
                " JOIN " + Database.engrave(table) +
                (alias == null ? "" : " AS " + Database.engrave(alias)) +
                (using.isEmpty() && condition == null ? "" : condition != null ? " ON " + condition : " USING (" + using.stream()
                        .map(Database::engrave)
                        .collect(Collectors.joining(", ")) + ")");
    }
}
