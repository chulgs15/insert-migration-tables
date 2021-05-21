package org.chulgs15.db2db;

import org.chulgs15.db2db.domain.ConnectionFactory;
import org.chulgs15.db2db.dto.ConnectionInfo;
import org.chulgs15.db2db.enums.DBVendor;

import java.sql.SQLException;
import java.util.Scanner;

public class Hello {

    public static void main(String[] args) throws SQLException {
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.print("Choose Source Database Vendor Type");
            System.out.print(" (ORACLE, MARIADB) : ");
            String vendor = scanner.next();

            System.out.print("url : ");
            String url = scanner.next();

            System.out.print("id : ");
            String id = scanner.next();

            System.out.print("password : ");
            String password = scanner.next();

            DBVendor dbVendor = DBVendor.valueOf(vendor.toUpperCase());

            ConnectionInfo connectionInfo = new ConnectionInfo(url, id, password, dbVendor);

            ConnectionFactory factory = ConnectionFactory.getFactory();
            factory.setSourceConnection(connectionInfo);
            factory.getSource();

            System.out.print("Choose target Database Vendor Type");
            System.out.print(" (ORACLE, MARIADB) : ");
            vendor = scanner.next();

            System.out.print("url : ");
            url = scanner.next();

            System.out.print("id : ");
            id = scanner.next();

            System.out.print("password : ");
            password = scanner.next();

            dbVendor = DBVendor.valueOf(vendor.toUpperCase());

            connectionInfo = new ConnectionInfo(url, id, password, dbVendor);

            factory.setTargetConnection(connectionInfo);
            factory.getTarget();

            System.out.print("Table Name : ");
            String tableName = scanner.next();

            ApplicationService applicationService = new ApplicationService();
            applicationService.beginApplication(tableName);
        }
    }
}