package org.chulgs15.db2db.exception;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class InsertMigrationException extends RuntimeException {

    public InsertMigrationException(InsertMigrationErrors errors) {
        super(errors.getMessage());
        log.error("Raise InsertMigrationException code   : {} / message : {}", errors.name(), errors.getMessage());
        log.error("Raise InsertMigrationException action : {}", errors.getAction());
    }
    public InsertMigrationException(InsertMigrationErrors errors, String message) {
        super(message);
        log.error("Raise InsertMigrationException code   : {} / message : {}", errors.name(), errors.getMessage());
        log.error("Raise InsertMigrationException action : {}", message);
    }
}
