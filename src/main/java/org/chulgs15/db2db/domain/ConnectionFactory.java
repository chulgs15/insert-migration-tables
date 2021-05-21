package org.chulgs15.db2db.domain;

import org.chulgs15.db2db.domain.connection.DatabaseConnection;
import org.chulgs15.db2db.dto.ConnectionInfo;

public class ConnectionFactory {
    private static ConnectionFactory factory = new ConnectionFactory();

    private ConnectionInfo sourceInfo;
    private ConnectionInfo targetInfo;

    private ConnectionFactory() {
    }

    public static ConnectionFactory getFactory() {
        if (factory == null) {
            factory = new ConnectionFactory();
        }
        return factory;
    }

    public void setSourceConnection(ConnectionInfo connectionInfo) {
        this.sourceInfo = connectionInfo;
    }

    public void setTargetConnection(ConnectionInfo connectionInfo) {
        this.targetInfo = connectionInfo;
    }


    public DatabaseConnection getSource() {
        DatabaseConnection databaseConnection = sourceInfo.getVendor().getDatabaseConnection(sourceInfo, null);
        databaseConnection.testConnection();
        return databaseConnection;
    }

    public DatabaseConnection getTarget() {
        DatabaseConnection databaseConnection = targetInfo.getVendor().getDatabaseConnection(targetInfo, null);
        databaseConnection.testConnection();
        return databaseConnection;
    }

    public DatabaseConnection getTargetWithTable(Table table) {
        return targetInfo.getVendor().getDatabaseConnection(targetInfo, table);
    }

    public void testSourceConnection() {
        getSource().testConnection();
    }

    public void testTargetConnection() {
        getTarget().testConnection();
    }
}
