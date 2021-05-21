package org.chulgs15.db2db.domain;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.chulgs15.db2db.domain.column.Column;

import java.util.ArrayList;
import java.util.List;

@Getter
@Slf4j
public class Table {

    private final String tableName;
    private final List<Column> columns = new ArrayList<>();
    private boolean isClobContain = false;
    private boolean isBlobContain = false;

    public Table(String tableName) {
        this.tableName = tableName;
    }

    public void addColumn(Column column) {
        this.columns.add(column);
        if (column.getColumnType().equals(Column.ColumnType.BLOB)) {
            this.isBlobContain = true;
        }

        if (column.getColumnType().equals(Column.ColumnType.CLOB)) {
            this.isClobContain = true;
        }
    }

    @Override
    public String toString() {
        return "Table{" + "tableName='" + tableName + '\'' + ", columns=" + columns + '}';
    }

    public String generateDropStatement() {
        return "DROP TABLE " + this.tableName;
    }

    public String generateCreateStatement() {

        StringBuilder builder = new StringBuilder();

        builder.append("CREATE TABLE ");
        builder.append(this.tableName);
        builder.append(" ( ");

        Column column = this.columns.get(0);
        builder.append(column.getCreateTableColumnName());

        for (int i = 1; i < this.columns.size(); i++) {
            column = this.columns.get(i);
            builder.append(", ");
            builder.append(column.getCreateTableColumnName());
        }

        builder.append(" ) ");
        log.debug(builder.toString());

        return builder.toString();
    }

    public String generateInsertStatement() {
        StringBuilder builder = new StringBuilder();

        builder.append("INSERT INTO ");
        builder.append(this.tableName);
        builder.append("( ");

        Column column = this.columns.get(0);
        builder.append(column.getColumnName());

        for (int i = 1; i < this.columns.size(); i++) {
            column = this.columns.get(i);
            builder.append(", ");
            builder.append(column.getColumnName());
        }

        builder.append(") values ( ? ");

        // Value 부분
        builder.append(", ? ".repeat(Math.max(0, this.columns.size() - 1)));

        builder.append(" ) ");
        log.debug(builder.toString());
        return builder.toString();
    }
}