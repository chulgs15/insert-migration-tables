package org.chulgs15.db2db;

import lombok.extern.slf4j.Slf4j;
import org.chulgs15.db2db.domain.ConnectionFactory;
import org.chulgs15.db2db.domain.Table;
import org.chulgs15.db2db.domain.column.Column;
import org.chulgs15.db2db.domain.connection.DatabaseConnection;
import org.chulgs15.db2db.dto.ColumnMetaDto;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

@Slf4j
public class ApplicationService {

    private final ExecutorService executors;
    private final List<FutureTask<String>> threads;
    private final DatabaseConnection source;
    private final DatabaseConnection target;
    private final ConnectionFactory factory;
    private final int coreNumber = Runtime.getRuntime().availableProcessors() * 2;

    public ApplicationService() {
        this.executors = Executors.newFixedThreadPool(coreNumber);
        this.threads = new ArrayList<>();
        this.factory = ConnectionFactory.getFactory();
        this.source = this.factory.getSource();
        this.target = this.factory.getTarget();
    }

    public void beginApplication(String tableName) throws SQLException {
        log.info("It runs Thread as many CPU cores * 2({} core). ", coreNumber / 2);
        log.info("Table Name : {}", tableName);

        Table table = _dropAndCreateTAble(tableName);

        _insert(table);

        if (!table.isClobContain() || !table.isBlobContain()) {
            _closeAndShutdown();
        }
    }

    private Table _dropAndCreateTAble(String tableName) {
        Table table = this.source.addTable(tableName);
        List<ColumnMetaDto> columnMetaList = this.source.generateColumnInfo();

        this.target.accept(table, columnMetaList);

        this.target.dropTable();

        this.target.createTable();

        return table;
    }

    private void _insert(Table table) throws SQLException {
        double rowCount = this.source.getRowCount();
        log.info("rowCount : {}", rowCount);

        ResultSet resultSet = this.source.getResultSet();

        double loop = 0;
        List<Map<Column, Object>> list = new ArrayList<>();

        while (resultSet.next()) {
            Map<Column, Object> map = new HashMap<>();
            List<Column> columns = table.getColumns();

            for (Column column : columns) {
                Object object = resultSet.getObject(column.getColumnName());
                if (column.getJdbcType() == Types.BLOB) {
                    object = resultSet.getBlob(column.getColumnName());
                }


                map.put(column, object);
            }

            list.add(map);
            loop++;

            if (loop % 1000 == 0 || rowCount == loop) {
                log.info("Loop {} Start", loop);

                DatabaseConnection target = factory.getTargetWithTable(table);

                for (Map<Column, Object> columnObjectMap : list) {
                    target.addMap(columnObjectMap);
                }

                while ((int) threads.stream().filter(x -> !x.isDone()).count() == coreNumber) {
                }

                FutureTask<String> futureTask = new FutureTask<>(target);
                this.executors.submit(futureTask);
                this.threads.add(futureTask);

                list = new ArrayList<>();
            }
        }
    }

    private void _closeAndShutdown() {
        while (true) {
            if (((int) threads.stream().filter(FutureTask::isDone).count()) == threads.size()) {
                log.info("All Thread is complete");
                executors.shutdown();
                return;
            }
        }
    }

}