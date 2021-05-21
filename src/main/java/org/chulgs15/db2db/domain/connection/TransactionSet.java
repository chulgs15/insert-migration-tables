package org.chulgs15.db2db.domain.connection;

import org.chulgs15.db2db.domain.Table;
import org.chulgs15.db2db.domain.column.Column;
import org.chulgs15.db2db.dto.ColumnMetaDto;

import java.sql.ResultSet;
import java.util.List;
import java.util.Map;

public interface TransactionSet {
    void createTable();

    void dropTable();

    void accept(Table table, List<ColumnMetaDto> columnMetaDtoList);

    ResultSet getResultSet();

    double getRowCount();

    void addMap(Map<Column, Object> map);
}
