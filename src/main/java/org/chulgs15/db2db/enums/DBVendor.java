package org.chulgs15.db2db.enums;

import lombok.Getter;
import org.chulgs15.db2db.domain.Table;
import org.chulgs15.db2db.domain.connection.DatabaseConnection;
import org.chulgs15.db2db.domain.connection.MariaDBConnection;
import org.chulgs15.db2db.domain.connection.OracleConnection;
import org.chulgs15.db2db.dto.ConnectionInfo;

@Getter
public enum DBVendor {
    ORACLE("select 1 from dual", "oracle.jdbc.driver.OracleDriver", "jdbc:oracle:thin:@%s") {
        @Override
        public DatabaseConnection getDatabaseConnection(ConnectionInfo connectionInfo, Table table) {
            return new OracleConnection(connectionInfo, table);
        }
    },
    MARIADB("select 1", "org.mariadb.jdbc.Driver", "jdbc:mariadb://%s?rewriteBatchedStatements=true&useCursorFetch=true") {
        @Override
        public DatabaseConnection getDatabaseConnection(ConnectionInfo connectionInfo, Table table) {
            return new MariaDBConnection(connectionInfo, table);
        }
    },
    ;

    private final String connectionTestQuery;
    private final String className;
    private final String jdbcPrefix;

    DBVendor(String connectionTestQuery, String className, String jdbcPrefix) {
        this.connectionTestQuery = connectionTestQuery;
        this.className = className;
        this.jdbcPrefix = jdbcPrefix;
    }

    public abstract DatabaseConnection getDatabaseConnection(ConnectionInfo connectionInfo, Table table);
}
