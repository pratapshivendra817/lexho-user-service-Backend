package com.lexho.user_service.integration;

import java.sql.Connection;
import java.sql.ResultSet;

final class MySqlSchemaInspector {

    private MySqlSchemaInspector() {
    }

    static boolean hasTable(Connection connection, String tableName) throws Exception {
        try (ResultSet tables = connection.getMetaData().getTables(connection.getCatalog(), null, tableName, new String[]{"TABLE"})) {
            return tables.next();
        }
    }

    static boolean hasColumn(Connection connection, String tableName, String columnName) throws Exception {
        try (ResultSet columns = connection.getMetaData().getColumns(connection.getCatalog(), null, tableName, columnName)) {
            return columns.next();
        }
    }

    static boolean isNullable(Connection connection, String tableName, String columnName) throws Exception {
        try (ResultSet columns = connection.getMetaData().getColumns(connection.getCatalog(), null, tableName, columnName)) {
            if (!columns.next()) {
                throw new IllegalStateException("Column not found: " + tableName + "." + columnName);
            }
            return "YES".equalsIgnoreCase(columns.getString("IS_NULLABLE"));
        }
    }

    static String defaultValue(Connection connection, String tableName, String columnName) throws Exception {
        try (ResultSet columns = connection.getMetaData().getColumns(connection.getCatalog(), null, tableName, columnName)) {
            if (!columns.next()) {
                throw new IllegalStateException("Column not found: " + tableName + "." + columnName);
            }
            return columns.getString("COLUMN_DEF");
        }
    }

    static String dataType(Connection connection, String tableName, String columnName) throws Exception {
        try (ResultSet columns = connection.getMetaData().getColumns(connection.getCatalog(), null, tableName, columnName)) {
            if (!columns.next()) {
                throw new IllegalStateException("Column not found: " + tableName + "." + columnName);
            }
            return columns.getString("TYPE_NAME");
        }
    }

    static boolean hasPrimaryKey(Connection connection, String tableName, String columnName) throws Exception {
        try (ResultSet keys = connection.getMetaData().getPrimaryKeys(connection.getCatalog(), null, tableName)) {
            while (keys.next()) {
                if (columnName.equalsIgnoreCase(keys.getString("COLUMN_NAME"))) {
                    return true;
                }
            }
        }
        return false;
    }

    static boolean hasForeignKey(Connection connection, String tableName, String foreignKeyName) throws Exception {
        try (ResultSet foreignKeys = connection.getMetaData().getImportedKeys(connection.getCatalog(), null, tableName)) {
            while (foreignKeys.next()) {
                if (foreignKeyName.equalsIgnoreCase(foreignKeys.getString("FK_NAME"))) {
                    return true;
                }
            }
        }
        return false;
    }

    static boolean hasIndex(Connection connection, String tableName, String indexName) throws Exception {
        try (ResultSet indexes = connection.getMetaData().getIndexInfo(connection.getCatalog(), null, tableName, false, false)) {
            while (indexes.next()) {
                if (indexName.equalsIgnoreCase(indexes.getString("INDEX_NAME"))) {
                    return true;
                }
            }
        }
        return false;
    }

    static boolean hasUniqueIndex(Connection connection, String tableName, String indexName) throws Exception {
        try (ResultSet indexes = connection.getMetaData().getIndexInfo(connection.getCatalog(), null, tableName, true, false)) {
            while (indexes.next()) {
                if (indexName.equalsIgnoreCase(indexes.getString("INDEX_NAME"))) {
                    return true;
                }
            }
        }
        return false;
    }
}
