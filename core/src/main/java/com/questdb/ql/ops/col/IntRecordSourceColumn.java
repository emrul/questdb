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
 ******************************************************************************/

package com.questdb.ql.ops.col;

import com.questdb.ql.Record;
import com.questdb.ql.StorageFacade;
import com.questdb.ql.ops.AbstractVirtualColumn;
import com.questdb.store.ColumnType;

public class IntRecordSourceColumn extends AbstractVirtualColumn {
    private final int index;

    public IntRecordSourceColumn(int index) {
        super(ColumnType.INT);
        this.index = index;
    }

    @Override
    public double getDouble(Record rec) {
        int v = rec.getInt(index);
        return v != Integer.MIN_VALUE ? v : Double.NaN;
    }

    @Override
    public float getFloat(Record rec) {
        int v = rec.getInt(index);
        return v != Integer.MIN_VALUE ? v : Float.NaN;
    }

    @Override
    public int getInt(Record rec) {
        return rec.getInt(index);
    }

    @Override
    public long getLong(Record rec) {
        int v = rec.getInt(index);
        return v != Integer.MIN_VALUE ? v : Long.MIN_VALUE;
    }

    @Override
    public boolean isConstant() {
        return false;
    }

    @Override
    public void prepare(StorageFacade facade) {
    }
}
