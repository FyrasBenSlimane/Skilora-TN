package com.skilora.utils;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

/**
 * Utility for safely reading optional columns from a {@link ResultSet}.
 * <p>
 * Useful when a query may or may not include JOIN columns (e.g. user_name, job_title)
 * depending on the caller. Avoids empty catch blocks that hide real SQL errors.
 */
public final class ResultSetUtils {

    private ResultSetUtils() { /* utility class */ }

    /**
     * Returns the String value of the named column, or {@code null} if the column
     * does not exist in this ResultSet.
     */
    public static String getOptionalString(ResultSet rs, String column) throws SQLException {
        if (hasColumn(rs, column)) {
            return rs.getString(column);
        }
        return null;
    }

    /**
     * Checks whether the ResultSet contains a column with the given name.
     */
    public static boolean hasColumn(ResultSet rs, String column) {
        try {
            ResultSetMetaData meta = rs.getMetaData();
            int count = meta.getColumnCount();
            for (int i = 1; i <= count; i++) {
                if (meta.getColumnLabel(i).equalsIgnoreCase(column)) {
                    return true;
                }
            }
        } catch (SQLException e) {
            // metadata not available — fall through
        }
        return false;
    }
}
