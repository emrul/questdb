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

package org.questdb.examples.append;

import com.lmax.disruptor.*;
import com.questdb.JournalWriter;
import com.questdb.ex.JournalException;
import com.questdb.factory.JournalFactory;
import com.questdb.misc.Files;
import com.questdb.misc.Rnd;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.questdb.examples.model.ModelConfiguration;
import org.questdb.examples.model.Quote;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SuppressFBWarnings({"SACM_STATIC_ARRAY_CREATED_IN_METHOD", "PREDICTABLE_RANDOM"})
public class MultiplexAppend {

    /**
     * Appends 2 million quotes to two journals simultaneously.
     */
    public static void main(String[] args) throws JournalException, InterruptedException {

        if (args.length != 1) {
            System.out.println("Usage: " + MultiplexAppend.class.getName() + " <path>");
            System.exit(1);
        }

        final ExecutorService service = Executors.newCachedThreadPool();
        final CountDownLatch latch = new CountDownLatch(2);

        final RingBuffer<Quote> ringBuffer = RingBuffer.createSingleProducer(new QuoteFactory(), 1024 * 64, new YieldingWaitStrategy());
        final SequenceBarrier sequenceBarrier = ringBuffer.newBarrier();

        try (JournalFactory factory = new JournalFactory(ModelConfiguration.CONFIG.build(args[0]))) {

            Files.delete(new File(factory.getConfiguration().getJournalBase(), "quote_1"));
            Files.delete(new File(factory.getConfiguration().getJournalBase(), "quote_2"));

            try (JournalWriter<Quote> writer1 = factory.writer(Quote.class, "quote_1")) {
                try (JournalWriter<Quote> writer2 = factory.writer(Quote.class, "quote_2")) {

                    BatchEventProcessor<Quote> processor1 = new BatchEventProcessor<>(ringBuffer, sequenceBarrier, new Handler(writer1, latch, 0));
                    BatchEventProcessor<Quote> processor2 = new BatchEventProcessor<>(ringBuffer, sequenceBarrier, new Handler(writer2, latch, 1));
                    ringBuffer.addGatingSequences(processor1.getSequence(), processor2.getSequence());

                    service.submit(processor1);
                    service.submit(processor2);

                    final int count = 2000000;
                    final String symbols[] = {"AGK.L", "BP.L", "TLW.L", "ABF.L", "LLOY.L", "BT-A.L", "WTB.L", "RRS.L", "ADM.L", "GKN.L", "HSBA.L"};
                    final Rnd r = new Rnd();

                    long t = System.nanoTime();
                    for (int i = 0; i < count; i++) {
                        long sequence = ringBuffer.next();
                        Quote q = ringBuffer.get(sequence);
                        // generate some data
                        String sym = symbols[Math.abs(r.nextInt() % (symbols.length - 1))];
                        q.setSym(sym);
                        q.setAsk(Math.abs(r.nextDouble()));
                        q.setBid(Math.abs(r.nextDouble()));
                        q.setAskSize(Math.abs(r.nextInt() % 10000));
                        q.setBidSize(Math.abs(r.nextInt() % 10000));
                        q.setEx("LXE");
                        q.setMode("Fast trading");
                        q.setTimestamp(System.currentTimeMillis());
                        ringBuffer.publish(sequence);
                    }

                    // publish special EOF event
                    long sequence = ringBuffer.next();
                    Quote q = ringBuffer.get(sequence);
                    q.setTimestamp(-1);
                    ringBuffer.publish(sequence);

                    // wait for handlers to flush
                    latch.await();

                    System.out.println("Published " + count + " quotes in " + (System.nanoTime() - t) / 1000000 + "ms.");

                    // stop the threads
                    processor1.halt();
                    processor2.halt();

                    //
                    writer1.commit();
                    writer2.commit();
                }
            }
        }
        service.shutdown();
    }

    private static class Handler implements EventHandler<Quote> {
        private final JournalWriter<Quote> writer;
        private final CountDownLatch latch;
        private final int condition;

        private Handler(JournalWriter<Quote> writer, CountDownLatch latch, int condition) {
            this.writer = writer;
            this.latch = latch;
            this.condition = condition;
        }

        @Override
        public void onEvent(Quote event, long sequence, boolean endOfBatch) throws Exception {

            if (event.getTimestamp() == -1) {
                latch.countDown();
            } else {
                if (event.getSym().hashCode() % 2 == condition) {
                    writer.append(event);
                }
            }
        }
    }

    private static class QuoteFactory implements EventFactory<Quote> {
        @Override
        public Quote newInstance() {
            return new Quote();
        }
    }
}
