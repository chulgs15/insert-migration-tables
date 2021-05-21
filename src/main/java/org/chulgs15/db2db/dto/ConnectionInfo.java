package org.chulgs15.db2db.dto;

import lombok.Getter;
import org.chulgs15.db2db.enums.DBVendor;

@Getter
public class ConnectionInfo {
    private final String url;
    private final String id;
    private final String password;
    private final DBVendor vendor;

    public ConnectionInfo(String url, String id, String password, DBVendor vendor) {
        this.url = url;
        this.id = id;
        this.password = password;
        this.vendor = vendor;
    }
}
