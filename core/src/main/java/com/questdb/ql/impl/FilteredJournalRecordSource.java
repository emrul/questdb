/*******************************************************************************
 *    ___                  _   ____  ____
 *   / _ \ _   _  ___  ___| |_|  _ \| __ )
 *  | | | | | | |/ _ \/ __| __| | | |  _ \
 *  | |_| | |_| |  __/\__ \ |_| |_| | |_) |
 *   \__\_\\__,_|\___||___/\__|____/|____/
 *
 * Copyright (C) 2014-2016 Appsicle
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * As a special exception, the copyright holders give permission to link the
 * code of portions of this program with the OpenSSL library under certain
 * conditions as described in each individual source file and distribute
 * linked combinations including the program with the OpenSSL library. You
 * must comply with the GNU Affero General Public License in all respects for
 * all of the code used other than as permitted herein. If you modify file(s)
 * with this exception, you may extend this exception to your version of the
 * file(s), but you are not obligated to do so. If you do not wish to do so,
 * delete this exception statement from your version. If you delete this
 * exception statement from all source files in the program, then also delete
 * it in the license file.
 *
 ******************************************************************************/

package com.questdb.ql.impl;

import com.questdb.ex.JournalException;
import com.questdb.factory.JournalReaderFactory;
import com.questdb.factory.configuration.RecordMetadata;
import com.questdb.ql.Record;
import com.questdb.ql.RecordCursor;
import com.questdb.ql.RecordSource;
import com.questdb.ql.StorageFacade;
import com.questdb.ql.model.ExprNode;
import com.questdb.ql.ops.AbstractCombinedRecordSource;
import com.questdb.ql.ops.VirtualColumn;
import com.questdb.std.CharSink;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class FilteredJournalRecordSource extends AbstractCombinedRecordSource {

    private final RecordSource delegate;
    private final VirtualColumn filter;
    private final ExprNode filterNode;
    private RecordCursor cursor;
    private Record record;

    public FilteredJournalRecordSource(RecordSource delegate, VirtualColumn filter, ExprNode filterNode) {
        this.delegate = delegate;
        this.filter = filter;
        // this value is borrowed from pool
        // and only here to print out plan
        // so plan must be serialized in sink before compiler reused
        // this is quite fragile approach but we better not create tree of objects
        // unnecessarily
        this.filterNode = filterNode;
    }

    @Override
    public Record getByRowId(long rowId) {
        return cursor.getByRowId(rowId);
    }

    @Override
    public StorageFacade getStorageFacade() {
        return cursor.getStorageFacade();
    }

    @Override
    public RecordMetadata getMetadata() {
        return delegate.getMetadata();
    }

    @Override
    public RecordCursor prepareCursor(JournalReaderFactory factory) throws JournalException {
        this.cursor = delegate.prepareCursor(factory);
        filter.prepare(cursor.getStorageFacade());
        return this;
    }

    @Override
    public void reset() {
        delegate.reset();
    }

    @Override
    public boolean supportsRowIdAccess() {
        return delegate.supportsRowIdAccess();
    }

    @Override
    public boolean hasNext() {
        while (cursor.hasNext()) {
            record = cursor.next();
            if (filter.getBool(record)) {
                return true;
            }
        }
        return false;
    }

    @SuppressFBWarnings({"IT_NO_SUCH_ELEMENT"})
    @Override
    public Record next() {
        return record;
    }

    @Override
    public void toSink(CharSink sink) {
        sink.put('{');
        sink.putQuoted("op").put(':').putQuoted("FilteredJournalRecordSource").put(',');
        sink.putQuoted("filter").put(':').put('"').put(filterNode).put('"');
        sink.put('}');
    }
}