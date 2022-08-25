package com.ptsmods.mysqlw.query;

import com.ptsmods.mysqlw.Database;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder(builderClassName = "Builder")
public class GroupBy {
    @Singular
    private final List<String> columns;
    private final boolean rollup;
    private final QueryCondition having;

    public List<String> getColumns() {
        return Collections.unmodifiableList(columns);
    }

    @Override
    public String toString() {
        return toString(Database.RDBMS.UNKNOWN);
    }

    public String toString(Database.RDBMS type) {
        return "GROUP BY " + columns.stream()
                .map(Database::engrave)
                .collect(Collectors.joining(", ")) + (rollup && type != Database.RDBMS.SQLite ? " WITH ROLLUP" : "") +
                (having == null ? "" : " HAVING " + having);
    }
}
