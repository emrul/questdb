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

package org.questdb.examples.query_old;

import com.questdb.Journal;
import com.questdb.JournalWriter;
import com.questdb.ex.JournalException;
import com.questdb.factory.JournalFactory;
import com.questdb.misc.Files;
import com.questdb.store.KVIndex;
import com.questdb.store.SymbolTable;
import org.questdb.examples.model.ModelConfiguration;
import org.questdb.examples.model.Quote;
import org.questdb.examples.support.QuoteGenerator;

import java.io.File;
import java.util.concurrent.TimeUnit;

/**
 * This example is SQL equivalent of:
 * <p/>
 * select sym, count(*) from quote group by sym
 */
public class ClassificationExample {

    public static void main(String[] args) throws JournalException {
        if (args.length != 1) {
            System.out.println("Usage: " + ClassificationExample.class.getName() + " <path>");
            System.exit(1);
        }
        String journalLocation = args[0];

        try (JournalFactory factory = new JournalFactory(ModelConfiguration.CONFIG.build(journalLocation))) {

            // delete existing quote journal
            Files.delete(new File(factory.getConfiguration().getJournalBase(), "quote"));

            int count = 1000000;
            long t = System.nanoTime();

            // get some data in :)
            try (JournalWriter<Quote> w = factory.writer(Quote.class)) {
                QuoteGenerator.generateQuoteData(w, count, 1, QuoteGenerator.randomSymbols(1400));
            }

            System.out.println("Created " + count + " records in " + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t) + "ms");

            try (Journal<Quote> journal = factory.reader(Quote.class)) {
                t = System.nanoTime();
                // symbol table is a Map<int, String> equivalent
                // it contains (key,value) pairs for all values of column "sym"
                // key is a 0-based dense sequence of INTs
                SymbolTable tab = journal.getSymbolTable("sym");

                // index is a sparse matrix (key x value) where key is "sym" key and values are localRowIDs
                KVIndex index = journal.getLastPartition().getIndexForColumn("sym");

                long total = 0;
                for (int i = 0, sz = tab.size(); i < sz; i++) {
                    int cnt = index.getValueCount(i);
                    System.out.println(tab.value(i) + ": " + cnt);
                    total += cnt;
                }
                System.out.println("total: " + total + " in " + TimeUnit.NANOSECONDS.toMicros(System.nanoTime() - t) + "μs");
            }
        }
    }
}
