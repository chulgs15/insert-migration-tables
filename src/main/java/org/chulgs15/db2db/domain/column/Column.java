package org.chulgs15.db2db.domain.column;

import lombok.Getter;

@Getter
public abstract class Column {

    private final String columnName;
    private final ColumnType columnType;
    private final int length;
    private final int scale;
    private final int jdbcType;

    public Column(String columnName, ColumnType columnType, int length, int scale, int jdbcType) {
            this.columnName = columnName;
            this.columnType = columnType;
            this.length = length;
            this.scale = scale;
            this.jdbcType = jdbcType;
        }

        public abstract String getCreateTableColumnName();

        public enum ColumnType {
            STRING, NUMERIC, DATETIME, BLOB, CLOB
        }

        @Override
        public String toString() {
            return "Column{" +
                    "columnName='" + columnName + '\'' +
                ", columnType=" + columnType +
                ", length=" + length +
                ", scale=" + scale +
                ", jdbcType=" + jdbcType +
                '}';
    }
}
