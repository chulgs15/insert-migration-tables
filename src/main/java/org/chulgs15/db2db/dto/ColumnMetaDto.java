package org.chulgs15.db2db.dto;

import lombok.Getter;
import org.chulgs15.db2db.domain.column.Column;

@Getter
public class ColumnMetaDto {

    private final String columnName;
    private final Column.ColumnType columnType;
    private final int length;
    private final int scale;

    public ColumnMetaDto(String columnName, Column.ColumnType columnType, int length, int scale) {
        this.columnName = columnName;
        this.columnType = columnType;
        this.length = length;
        this.scale = scale;
    }

    public String getColumnName() {
        return columnName;
    }

    public Column.ColumnType getColumnType() {
        return columnType;
    }

    @Override
    public String toString() {
        return "ColumnMetaDto{" +
                "columnName='" + columnName + '\'' +
                ", columnType=" + columnType +
                ", length=" + length +
                '}';
    }
}
