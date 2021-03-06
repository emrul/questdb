/*******************************************************************************
 *     ___                  _   ____  ____
 *    / _ \ _   _  ___  ___| |_|  _ \| __ )
 *   | | | | | | |/ _ \/ __| __| | | |  _ \
 *   | |_| | |_| |  __/\__ \ |_| |_| | |_) |
 *    \__\_\\__,_|\___||___/\__|____/|____/
 *
 *  Copyright (c) 2014-2016 Appsicle
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 ******************************************************************************/

package org.questdb.examples.query;

import com.questdb.ex.JournalException;
import com.questdb.ex.ParserException;
import com.questdb.factory.JournalFactory;
import com.questdb.ql.RecordSource;
import com.questdb.ql.parser.QueryCompiler;
import com.questdb.ql.parser.QueryError;

import static org.questdb.examples.query.GenericAppend.*;

public class QueryParameters {
    public static void main(String[] args) throws JournalException, ParserException {

        if (args.length < 1) {
            System.out.println("Usage: GenericAppend <path>");
            System.exit(1);
        }

        final String location = args[0];

        deleteCustomers(location);

        // factory can be reused in application and must be explicitly closed when no longer needed.
        try (JournalFactory factory = new JournalFactory(location)) {
            createCustomers(factory);

            QueryCompiler compiler = new QueryCompiler();

            // compile record source
            try (RecordSource rs = compiler.compile(factory, "customers")) {
                // execute query once
                int count = fetchCursor(rs.prepareCursor(factory));
                // execute query twice
                count += fetchCursor(rs.prepareCursor(factory));
                System.out.println(count);
            }

            // Query compilation is a complex process and it can be expensive.
            // Use parameters to avoid compilation cost. Parameter values can be
            // set before creating cursor.

            try {
                try (RecordSource rs = compiler.compile(factory, "customers where name ~ :regex")) {
                    rs.getParam(":regex").set("XYZK");
                    System.out.println(fetchCursor(rs.prepareCursor(factory)));

                    rs.getParam(":regex").set("X.+Z");
                    System.out.println(fetchCursor(rs.prepareCursor(factory)));
                }
            } catch (ParserException e) {
                System.out.println(QueryError.getMessage());
            }
        }
    }
}
