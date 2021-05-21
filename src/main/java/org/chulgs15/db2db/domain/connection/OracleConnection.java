package org.chulgs15.db2db.domain.connection;

import lombok.extern.slf4j.Slf4j;
import org.chulgs15.db2db.domain.Table;
import org.chulgs15.db2db.domain.column.Column;
import org.chulgs15.db2db.dto.ColumnMetaDto;
import org.chulgs15.db2db.dto.ConnectionInfo;
import org.chulgs15.db2db.exception.InsertMigrationErrors;
import org.chulgs15.db2db.exception.InsertMigrationException;

import java.sql.*;
import java.util.LinkedList;
import java.util.List;

@Slf4j
public class OracleConnection extends DatabaseConnection {

    public final static String columnSql = "select * from all_tab_cols t WHERE t.table_name = ? order by t.column_id";

    public OracleConnection(ConnectionInfo connectionInfo, Table table) {
        super(connectionInfo, table);
    }

    @Override
    public List<ColumnMetaDto> generateColumnInfo() {
        log.info("OracleConnection.generateColumnInfo start...");

        List<ColumnMetaDto> result = new LinkedList<>();

        try (Connection connection = newConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(columnSql)) {

            preparedStatement.setString(1, super.table.getTableName());
            ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                String columnName = resultSet.getString("COLUMN_NAME");
                Column.ColumnType columnType = getColumnType(resultSet.getString("DATA_TYPE"));
                int length = resultSet.getInt("DATA_LENGTH");
                int scale = resultSet.getInt("DATA_SCALE");

                ColumnMetaDto metaDto = new ColumnMetaDto(columnName, columnType, length, scale);
                result.add(metaDto);

                log.debug(metaDto.toString());
            }
        } catch (SQLException e) {
            throw new InsertMigrationException(InsertMigrationErrors.UNEXPECTED_ERROR, e.getLocalizedMessage());
        }

        if (result.size() == 0) {
            throw new InsertMigrationException(InsertMigrationErrors.NO_COLUMN_DATA);
        }

        log.info("[{}] The size({}) columns are collected", this.table.getTableName(),
                result.size());

        // TODO: 컬럼 명에 # 이 들어가면 다른 이름으로 변경할 것.


        return result;
    }

    private Column.ColumnType getColumnType(String columnTypeName) {
        Column.ColumnType columnType;
        switch (columnTypeName) {
            case "VARCHAR2":
            case "VARCHAR":
                columnType = Column.ColumnType.STRING;
                break;
            case "NUMBER":
                columnType = Column.ColumnType.NUMERIC;
                break;
            case "DATE":
                columnType = Column.ColumnType.DATETIME;
                break;
            case "BLOB":
                columnType = Column.ColumnType.BLOB;
                break;
            case "CLOB":
                columnType = Column.ColumnType.CLOB;
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + columnTypeName);
        }
        return columnType;
    }

    @Override
    protected Column _getColumnFromMeta(ColumnMetaDto metaDto) {
        Column column;
        switch (metaDto.getColumnType()) {
            case STRING:
                column = new OracleStringColumn(metaDto);
                break;
            case NUMERIC:
                column = new OracleNumericColumn(metaDto);
                break;
            case DATETIME:
                column = new OracleDateColumn(metaDto);
                break;
            case BLOB:
                column = new OracleBlobColumn(metaDto);
                break;
            case CLOB:
                column = new OracleClobColumn(metaDto);
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + metaDto.getColumnType());
        }

        return column;
    }

}

class OracleDateColumn extends Column {
    public OracleDateColumn(ColumnMetaDto metaDto) {
        super(metaDto.getColumnName(), metaDto.getColumnType(), metaDto.getLength(), metaDto.getScale(), Types.DATE);
    }

    @Override
    public String getCreateTableColumnName() {
        return String.format("%s DATE", super.getColumnName());
    }
}

class OracleStringColumn extends Column {

    public OracleStringColumn(ColumnMetaDto metaDto) {
        super(metaDto.getColumnName(), metaDto.getColumnType(), metaDto.getLength(), metaDto.getScale(), Types.VARCHAR);
    }

    @Override
    public String getCreateTableColumnName() {
        String result = String.format("%s VARCHAR2(%d)", super.getColumnName(), super.getLength());

        if (super.getLength() > 4000) {
            result = String.format("%s CLOB", super.getColumnName());
        }

        return result;
    }
}

class OracleBlobColumn extends Column {

    public OracleBlobColumn(ColumnMetaDto metaDto) {
        super(metaDto.getColumnName(), metaDto.getColumnType(), metaDto.getLength(), metaDto.getScale(), Types.BLOB);
    }

    @Override
    public String getCreateTableColumnName() {
        return String.format("%s BLOB", super.getColumnName());
    }
}

class OracleNumericColumn extends Column {
    public OracleNumericColumn(ColumnMetaDto metaDto) {
        super(metaDto.getColumnName(), metaDto.getColumnType(), metaDto.getLength(), metaDto.getScale(), Types.NUMERIC);
    }

    @Override
    public String getCreateTableColumnName() {
        String result;

        if (super.getScale() == 0) {
            result = String.format("%s NUMBER(%d) ", super.getColumnName(), super.getLength());
        } else {
            result = String.format("%s NUMBER(%d, %d) ", super.getColumnName(), super.getLength(), super.getScale());
        }

        return result;
    }
}

class OracleClobColumn extends Column {

    public OracleClobColumn(ColumnMetaDto metaDto) {
        super(metaDto.getColumnName(), metaDto.getColumnType(), metaDto.getLength(), metaDto.getScale(), Types.VARCHAR);
    }

    @Override
    public String getCreateTableColumnName() {
        return String.format("%s CLOB", super.getColumnName());
    }
}
