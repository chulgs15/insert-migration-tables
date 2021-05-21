package org.chulgs15.db2db.domain.connection;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.chulgs15.db2db.domain.Table;
import org.chulgs15.db2db.domain.column.Column;
import org.chulgs15.db2db.dto.ColumnMetaDto;
import org.chulgs15.db2db.dto.ConnectionInfo;
import org.chulgs15.db2db.enums.DBVendor;
import org.chulgs15.db2db.exception.InsertMigrationErrors;
import org.chulgs15.db2db.exception.InsertMigrationException;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

@Getter
@Slf4j
public abstract class DatabaseConnection implements TransactionSet, Callable<String> {
    protected Table table;
    private final String url;
    private final String id;
    private final String password;
    private final DBVendor vendor;
    private List<Map<Column, Object>> data = new ArrayList<>(1000);


    public DatabaseConnection(ConnectionInfo connectionInfo, Table table) {
        this.url = connectionInfo.getUrl();
        this.id = connectionInfo.getId();
        this.password = connectionInfo.getPassword();
        this.vendor = connectionInfo.getVendor();
        this.table = table;
    }

    public void testConnection() {
        try (Connection connection = newConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement(
                    this.vendor.getConnectionTestQuery());
            ResultSet resultSet = preparedStatement.executeQuery();
            resultSet.next();
        } catch (SQLException e) {
            throw new InsertMigrationException(InsertMigrationErrors.UNEXPECTED_ERROR, e.getLocalizedMessage());
        }
    }

    public Table addTable(String tableName) {
        this.table = new Table(tableName);
        return this.table;
    }

    public Connection newConnection() {
        log.debug("url : {} | user : {} | pw : {}" + "*".repeat(this.password.length()-1),
                this.url,
                this.id,
                this.password.charAt(0));

        Connection conn = null;
        try {
            Class.forName(this.vendor.getClassName());

            conn = DriverManager.getConnection(
                    this.vendor.getJdbcPrefix().replace("%s", this.url), this.id, this.password);

            log.info("{} Connection Created (URL : {})", this.vendor, this.url);

        } catch (ClassNotFoundException | SQLException connException) {
            throw new InsertMigrationException(InsertMigrationErrors.UNEXPECTED_ERROR,
                    connException.getLocalizedMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return conn;
    }

    public void createTable() {
        _doDDL(this.table.generateCreateStatement());
    }

    public void dropTable() {
        try {
            _doDDL(this.table.generateDropStatement());
        } catch (Exception e) {
            log.info("An error occurred in dropTable(). However, the program continues.");
            log.info("The error occurred because the table does not exist.");
        }
    }


    private void _doDDL(String sql) {
        try (Connection connection = newConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.execute();
        } catch (SQLException e) {
            throw new InsertMigrationException(InsertMigrationErrors.UNEXPECTED_ERROR,
                    e.getLocalizedMessage());
        }
    }

    public void accept(Table table, List<ColumnMetaDto> columnMetaDtoList) {
        log.info("{} accept Start...", this.vendor);
        for (ColumnMetaDto metaDto : columnMetaDtoList) {
            Column column = _getColumnFromMeta(metaDto);
            table.addColumn(column);
            log.trace(column.toString());
        }
        this.table = table;
    }

    public double getRowCount() {
        log.info("{} getRowCount Start...", this.vendor);

        double result;
        try (Connection connection = newConnection()) {
            ResultSet resultSet = connection
                    .prepareStatement("SELECT count(1) FROM " + this.table.getTableName())
                    .executeQuery();
            resultSet.next();

            result = resultSet.getDouble(1);
            log.info("Total Row Count : {} rows.", result);
        } catch (SQLException e) {
            log.error("{} getRowCount Error : " + e.getLocalizedMessage(), this.vendor);
            throw new InsertMigrationException(InsertMigrationErrors.UNEXPECTED_ERROR, e.getLocalizedMessage());
        }

        return result;
    }


    public ResultSet getResultSet() {
        log.info("{} getResultSet Start...", this.vendor);
        ResultSet resultSet;

        try {
            Connection connection = newConnection();
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM " + this.table.getTableName());

            preparedStatement.setFetchSize (100);

            resultSet = preparedStatement.executeQuery();
        } catch (SQLException e) {
            log.error("{} getResultSet Error : " + e.getLocalizedMessage(), this.vendor);
            throw new InsertMigrationException(InsertMigrationErrors.UNEXPECTED_ERROR, e.getLocalizedMessage());
        }

        return resultSet;
    }

    public String call() throws Exception {
        try (Connection connection = newConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(this.table.generateInsertStatement())) {

            List<Column> columns = this.table.getColumns();
            int size = columns.size();
            for (Map<Column, Object> datum : this.data) {
                for (int i = 0; i < size; i++) {
                    Column column = columns.get(i);
                    Object value = datum.get(column);

                    if (column.getJdbcType() == Types.BLOB) {
                        Blob blob = (Blob) value;
                        preparedStatement.setBlob(i + 1, blob.getBinaryStream());
                    } else {
                        preparedStatement.setObject(i + 1, value, column.getJdbcType());
                    }
                }

                preparedStatement.addBatch();
                preparedStatement.clearParameters();
            }

            preparedStatement.executeBatch();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
        log.trace("Call Closed");
        return "Success";
    }

    public void addMap(Map<Column, Object> map) {
        this.data.add(map);
    }

    protected abstract Column _getColumnFromMeta(ColumnMetaDto metaDto);

    public abstract List<ColumnMetaDto> generateColumnInfo();


}
