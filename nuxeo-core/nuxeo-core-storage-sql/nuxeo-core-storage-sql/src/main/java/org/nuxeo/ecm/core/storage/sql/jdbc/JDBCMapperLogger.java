/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.sql.jdbc;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.ecm.core.storage.sql.Binary;
import org.nuxeo.ecm.core.storage.sql.Model;
import org.nuxeo.ecm.core.storage.sql.SimpleFragment;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Column;

/**
 * Logger for a mapper instance, used for debugging.
 */
public class JDBCMapperLogger {

    public static final Log log = LogFactory.getLog(JDBCMapperLogger.class);

    public static final int DEBUG_MAX_STRING = 100;

    public static final int DEBUG_MAX_ARRAY = 10;

    public final long instanceNumber;

    public JDBCMapperLogger(long instanceNumber) {
        this.instanceNumber = instanceNumber;
    }

    public boolean isLogEnabled() {
        return log.isTraceEnabled();
    }

    public void error(Object message, Throwable t) {
        log.error(message, t);
    }

    public void warn(Object message) {
        log.error(message);
    }

    public void log(String string) {
        log.trace("(" + instanceNumber + ") SQL: " + string);
    }

    public void logCount(int count) {
        if (count > 0 && isLogEnabled()) {
            log("  -> " + count + " row" + (count > 1 ? "s" : ""));
        }
    }

    public void logResultSet(ResultSet rs, List<Column> columns)
            throws SQLException {
        List<String> res = new LinkedList<String>();
        int i = 0;
        for (Column column : columns) {
            i++;
            Serializable v = column.getFromResultSet(rs, i);
            res.add(column.getKey() + "=" + loggedValue(v));
        }
        log("  -> " + StringUtils.join(res, ", "));
    }

    public void logMap(Map<String, Serializable> map) throws SQLException {
        List<String> res = new LinkedList<String>();
        for (Entry<String, Serializable> en : map.entrySet()) {
            res.add(en.getKey() + "=" + loggedValue(en.getValue()));
        }
        log("  -> " + StringUtils.join(res, ", "));
    }

    public void logIds(List<Serializable> ids, boolean countTotal,
            long totalSize) {
        int i;
        List<Serializable> debugIds = ids;
        String end = "";
        if (ids.size() > DEBUG_MAX_ARRAY) {
            debugIds = new ArrayList<Serializable>(DEBUG_MAX_ARRAY);
            i = 0;
            for (Serializable id : ids) {
                debugIds.add(id);
                i++;
                if (i == DEBUG_MAX_ARRAY) {
                    break;
                }
            }
            end = "...(" + ids.size() + " ids)...";
        }
        if (countTotal) {
            end += " (total " + totalSize + ')';
        }
        log("  -> " + debugIds + end);
    }

    public void logSQL(String sql, List<Column> columns, SimpleFragment row) {
        List<Serializable> values = new ArrayList<Serializable>(columns.size());
        for (Column column : columns) {
            String key = column.getKey();
            Serializable value;
            if (key.equals(Model.MAIN_KEY)) {
                value = row.getId();
            } else {
                try {
                    value = row.get(key);
                } catch (StorageException e) {
                    // cannot happen
                    value = "ACCESSFAILED";
                }
            }
            values.add(value);
        }
        logSQL(sql, values);
    }

    public void logSQL(String sql, List<Serializable> values) {
        StringBuilder buf = new StringBuilder();
        int start = 0;
        for (Serializable v : values) {
            int index = sql.indexOf('?', start);
            if (index == -1) {
                // mismatch between number of ? and number of values
                break;
            }
            buf.append(sql, start, index);
            buf.append(loggedValue(v));
            start = index + 1;
        }
        buf.append(sql, start, sql.length());
        log(buf.toString());
    }

    /**
     * Returns a loggable value using pseudo-SQL syntax.
     */
    @SuppressWarnings("boxing")
    public static String loggedValue(Serializable value) {
        if (value == null) {
            return "NULL";
        }
        if (value instanceof String) {
            String v = (String) value;
            if (v.length() > DEBUG_MAX_STRING) {
                v = v.substring(0, DEBUG_MAX_STRING) + "...(" + v.length()
                        + " chars)...";
            }
            return "'" + v.replace("'", "''") + "'";
        }
        if (value instanceof Calendar) {
            Calendar cal = (Calendar) value;
            char sign;
            int offset = cal.getTimeZone().getOffset(cal.getTimeInMillis()) / 60000;
            if (offset < 0) {
                offset = -offset;
                sign = '-';
            } else {
                sign = '+';
            }
            return String.format(
                    "TIMESTAMP '%04d-%02d-%02dT%02d:%02d:%02d.%03d%c%02d:%02d'",
                    cal.get(Calendar.YEAR), //
                    cal.get(Calendar.MONTH) + 1, //
                    cal.get(Calendar.DAY_OF_MONTH), //
                    cal.get(Calendar.HOUR_OF_DAY), //
                    cal.get(Calendar.MINUTE), //
                    cal.get(Calendar.SECOND), //
                    cal.get(Calendar.MILLISECOND), //
                    sign, offset / 60, offset % 60);
        }
        if (value instanceof Binary) {
            return "'" + ((Binary) value).getDigest() + "'";
        }
        if (value.getClass().isArray()) {
            Serializable[] v = (Serializable[]) value;
            StringBuilder b = new StringBuilder();
            b.append('[');
            for (int i = 0; i < v.length; i++) {
                if (i > 0) {
                    b.append(',');
                    if (i > DEBUG_MAX_ARRAY) {
                        b.append("...(" + v.length + " items)...");
                        break;
                    }
                }
                b.append(loggedValue(v[i]));
            }
            b.append(']');
            return b.toString();
        }
        return value.toString();
    }
}