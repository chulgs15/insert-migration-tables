package org.chulgs15.db2db.domain.connection;

import lombok.extern.slf4j.Slf4j;
import org.chulgs15.db2db.domain.Table;
import org.chulgs15.db2db.domain.column.Column;
import org.chulgs15.db2db.dto.ColumnMetaDto;
import org.chulgs15.db2db.dto.ConnectionInfo;
import org.chulgs15.db2db.exception.InsertMigrationErrors;
import org.chulgs15.db2db.exception.InsertMigrationException;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class MariaDBConnection extends DatabaseConnection {

    public final String columnSql = "SELECT c.column_name, upper(c.data_type) AS data_type, c.column_type, c.CHARACTER_MAXIMUM_LENGTH, " +
            " c.NUMERIC_PRECISION, " +
            " c.NUMERIC_SCALE " +
            "FROM information_schema.columns c " +
            "WHERE c.table_name = ? ORDER BY c.ordinal_position";


    public MariaDBConnection(ConnectionInfo connectionInfo, Table table) {
        super(connectionInfo, table);
    }

    @Override
    public List<ColumnMetaDto> generateColumnInfo() {
        List<ColumnMetaDto> result = new ArrayList<>();

        try (Connection connection = newConnection()) {
            PreparedStatement preparedStatement = connection
                    .prepareStatement(columnSql);
            preparedStatement.setString(1, this.table.getTableName());
            ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                String columnName = resultSet.getString("COLUMN_NAME");
                Column.ColumnType columnType = _getColumnType(resultSet.getString("DATA_TYPE"));

                int length = 0;
                int scale = 0;
                if (columnType.equals(Column.ColumnType.STRING)) {
                    length = (int) resultSet.getDouble("CHARACTER_MAXIMUM_LENGTH");
                } else if (columnType.equals(Column.ColumnType.NUMERIC)) {
                    length = resultSet.getInt("NUMERIC_PRECISION");
                    scale = resultSet.getInt("NUMERIC_SCALE");
                }

                ColumnMetaDto metaDto = new ColumnMetaDto(columnName, columnType, length, scale);
                result.add(metaDto);
            }
        } catch (SQLException e) {
            throw new InsertMigrationException(InsertMigrationErrors.UNEXPECTED_ERROR, e.getLocalizedMessage());
        }

        if (result.size() == 0) {
            throw new InsertMigrationException(InsertMigrationErrors.NO_COLUMN_DATA);
        }

        log.info("[{}] The size({}) columns are collected", this.table.getTableName(),
                result.size());

        return result;
    }

    private Column.ColumnType _getColumnType(String dataType) {
        Column.ColumnType result;
        switch (dataType) {
            case "VARCHAR":
            case "TEXT":
                result = Column.ColumnType.STRING;
                break;
            case "DECIMAL":
            case "INT":
            case "BIGINT":
                result = Column.ColumnType.NUMERIC;
                break;
            case "DATETIME":
                result = Column.ColumnType.DATETIME;
                break;
            case "BLOB":
            case "LONGBLOB":
                result = Column.ColumnType.BLOB;
                break;
            case "LONGTEXT":
                result = Column.ColumnType.CLOB;
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + dataType);
        }
        return result;
    }


    @Override
    protected Column _getColumnFromMeta(ColumnMetaDto metaDto) {
        Column column;
        switch (metaDto.getColumnType()) {
            case STRING:
                column = new MariaDBStringColumn(metaDto);
                break;
            case NUMERIC:
                column = new MariaDBNumericColumn(metaDto);
                break;
            case DATETIME:
                column = new MariaDBDateColumn(metaDto);
                break;
            case BLOB:
                column = new MariaDBBlobColumn(metaDto);
                break;
            case CLOB:
                column = new MariaDBClobColumn(metaDto);
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + metaDto.getColumnType());
        }

        return column;
    }
}

class MariaDBDateColumn extends Column {
    public MariaDBDateColumn(ColumnMetaDto metaDto) {
        super(metaDto.getColumnName(), metaDto.getColumnType(), metaDto.getLength(), metaDto.getScale(), Types.DATE);
    }

    @Override
    public String getCreateTableColumnName() {
        return String.format("%s DATETIME", super.getColumnName());
    }
}


class MariaDBStringColumn extends Column {

    public MariaDBStringColumn(ColumnMetaDto metaDto) {
        super(metaDto.getColumnName(), metaDto.getColumnType(), metaDto.getLength(), metaDto.getScale(), Types.VARCHAR);
    }

    @Override
    public String getCreateTableColumnName() {
        String result = String.format("%s VARCHAR(%d)", super.getColumnName(), super.getLength());

        if (super.getLength() >= 2000 && super.getLength() < 4000) {
            result = String.format("%s TEXT", super.getColumnName());
        } else if (super.getLength() >= 4000) {
            result = String.format("%s LONGTEXT", super.getColumnName());
        }

        return result;
    }
}


class MariaDBNumericColumn extends Column {
    public MariaDBNumericColumn(ColumnMetaDto metaDto) {
        super(metaDto.getColumnName(), metaDto.getColumnType(), metaDto.getLength(), metaDto.getScale(), Types.NUMERIC);
    }

    @Override
    public String getCreateTableColumnName() {
        String format = String.format("%s DECIMAL(%d, %d) ", super.getColumnName(), super.getLength(), super.getScale());

        if (super.getScale() == 0) {
            format = String.format("%s DECIMAL(%d, %d) ", super.getColumnName(),
                    super.getLength() + 10, 10);
        }

        return format;
    }
}

class MariaDBBlobColumn extends Column {
    public MariaDBBlobColumn(ColumnMetaDto metaDto) {
        super(metaDto.getColumnName(), metaDto.getColumnType(), metaDto.getLength(), metaDto.getScale(), Types.NUMERIC);
    }

    @Override
    public String getCreateTableColumnName() {
        return String.format("%s LONGBLOB ", super.getColumnName());
    }
}

class MariaDBClobColumn extends Column {
    public MariaDBClobColumn(ColumnMetaDto metaDto) {
        super(metaDto.getColumnName(), metaDto.getColumnType(), metaDto.getLength(), metaDto.getScale(), Types.VARCHAR);
    }

    @Override
    public String getCreateTableColumnName() {
        return String.format("%s LONGTEXT ", super.getColumnName());
    }
}

