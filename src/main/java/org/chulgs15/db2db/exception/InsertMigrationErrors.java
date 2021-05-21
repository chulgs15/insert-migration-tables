package org.chulgs15.db2db.exception;


import lombok.Getter;

@Getter
public enum InsertMigrationErrors {
    UNEXPECTED_ERROR("SQLException occurred while running Select.", "Please check the log."),
    CLASS_FOR_NAME_ERROR("JDBC Class not found.", "Please add Library."),
    NO_COLUMN_DATA("Column data not found in Source Connection.", "Please check Meta Data permission and table name."),
    ;


    private final String message;
    private final String action;

    InsertMigrationErrors(String message, String action) {
        this.message = message;
        this.action = action;
    }


}
