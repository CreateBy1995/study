/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.kafka.clients.producer.internals;

import org.apache.kafka.clients.ApiVersions;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.common.Cluster;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.MetricName;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.UnsupportedVersionException;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.metrics.Measurable;
import org.apache.kafka.common.metrics.MetricConfig;
import org.apache.kafka.common.metrics.Metrics;
import org.apache.kafka.common.metrics.Sensor;
import org.apache.kafka.common.metrics.stats.Meter;
import org.apache.kafka.common.record.AbstractRecords;
import org.apache.kafka.common.record.CompressionRatioEstimator;
import org.apache.kafka.common.record.CompressionType;
import org.apache.kafka.common.record.MemoryRecords;
import org.apache.kafka.common.record.MemoryRecordsBuilder;
import org.apache.kafka.common.record.Record;
import org.apache.kafka.common.record.RecordBatch;
import org.apache.kafka.common.record.TimestampType;
import org.apache.kafka.common.utils.CopyOnWriteMap;
import org.apache.kafka.common.utils.LogContext;
import org.apache.kafka.common.utils.Time;
import org.apache.kafka.common.utils.Utils;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * This class acts as a queue that accumulates records into {@link MemoryRecords}
 * instances to be sent to the server.
 * <p>
 * The accumulator uses a bounded amount of memory and append calls will block when that memory is exhausted, unless
 * this behavior is explicitly disabled.
 */
public final class RecordAccumulator {

    private final Logger log;
    private volatile boolean closed;
    private final AtomicInteger flushesInProgress;
    private final AtomicInteger appendsInProgress;
    private final int batchSize;
    private final CompressionType compression;
    private final long lingerMs;
    private final long retryBackoffMs;
    private final org.apache.kafka.clients.producer.internals.BufferPool free;
    private final Time time;
    private final ApiVersions apiVersions;
    private final ConcurrentMap<TopicPartition, Deque<org.apache.kafka.clients.producer.internals.ProducerBatch>> batches;
    private final org.apache.kafka.clients.producer.internals.IncompleteBatches incomplete;
    // The following variables are only accessed by the sender thread, so we don't need to protect them.
    private final Map<TopicPartition, Long> muted;
    private int drainIndex;
    private final org.apache.kafka.clients.producer.internals.TransactionManager transactionManager;

    /**
     * Create a new record accumulator
     *
     * @param logContext The log context used for logging
     * @param batchSize The size to use when allocating {@link MemoryRecords} instances
     * @param totalSize The maximum memory the record accumulator can use.
     * @param compression The compression codec for the records
     * @param lingerMs An artificial delay time to add before declaring a records instance that isn't full ready for
     *        sending. This allows time for more records to arrive. Setting a non-zero lingerMs will trade off some
     *        latency for potentially better throughput due to more batching (and hence fewer, larger requests).
     * @param retryBackoffMs An artificial delay time to retry the produce request upon receiving an error. This avoids
     *        exhausting all retries in a short period of time.
     * @param metrics The metrics
     * @param time The time instance to use
     * @param apiVersions Request API versions for current connected brokers
     * @param transactionManager The shared transaction state object which tracks producer IDs, epochs, and sequence
     *                           numbers per partition.
     */
    public RecordAccumulator(LogContext logContext,
                             int batchSize,
                             long totalSize,
                             CompressionType compression,
                             long lingerMs,
                             long retryBackoffMs,
                             Metrics metrics,
                             Time time,
                             ApiVersions apiVersions,
                             org.apache.kafka.clients.producer.internals.TransactionManager transactionManager) {
        this.log = logContext.logger(RecordAccumulator.class);
        this.drainIndex = 0;
        this.closed = false;
        this.flushesInProgress = new AtomicInteger(0);
        this.appendsInProgress = new AtomicInteger(0);
        this.batchSize = batchSize;
        this.compression = compression;
        this.lingerMs = lingerMs;
        this.retryBackoffMs = retryBackoffMs;
        this.batches = new CopyOnWriteMap<>();
        String metricGrpName = "producer-metrics";
        this.free = new BufferPool(totalSize, batchSize, metrics, time, metricGrpName);
        this.incomplete = new org.apache.kafka.clients.producer.internals.IncompleteBatches();
        this.muted = new HashMap<>();
        this.time = time;
        this.apiVersions = apiVersions;
        this.transactionManager = transactionManager;
        registerMetrics(metrics, metricGrpName);
    }

    private void registerMetrics(Metrics metrics, String metricGrpName) {
        MetricName metricName = metrics.metricName("waiting-threads", metricGrpName, "The number of user threads blocked waiting for buffer memory to enqueue their records");
        Measurable waitingThreads = new Measurable() {
            public double measure(MetricConfig config, long now) {
                return free.queued();
            }
        };
        metrics.addMetric(metricName, waitingThreads);

        metricName = metrics.metricName("buffer-total-bytes", metricGrpName, "The maximum amount of buffer memory the client can use (whether or not it is currently used).");
        Measurable totalBytes = new Measurable() {
            public double measure(MetricConfig config, long now) {
                return free.totalMemory();
            }
        };
        metrics.addMetric(metricName, totalBytes);

        metricName = metrics.metricName("buffer-available-bytes", metricGrpName, "The total amount of buffer memory that is not being used (either unallocated or in the free list).");
        Measurable availableBytes = new Measurable() {
            public double measure(MetricConfig config, long now) {
                return free.availableMemory();
            }
        };
        metrics.addMetric(metricName, availableBytes);

        Sensor bufferExhaustedRecordSensor = metrics.sensor("buffer-exhausted-records");
        MetricName rateMetricName = metrics.metricName("buffer-exhausted-rate", metricGrpName, "The average per-second number of record sends that are dropped due to buffer exhaustion");
        MetricName totalMetricName = metrics.metricName("buffer-exhausted-total", metricGrpName, "The total number of record sends that are dropped due to buffer exhaustion");
        bufferExhaustedRecordSensor.add(new Meter(rateMetricName, totalMetricName));
    }

    /**
     * Add a record to the accumulator, return the append result
     * <p>
     * The append result will contain the future metadata, and flag for whether the appended batch is full or a new batch is created
     * <p>
     *
     * @param tp The topic/partition to which this record is being sent
     * @param timestamp The timestamp of the record
     * @param key The key for the record
     * @param value The value for the record
     * @param headers the Headers for the record
     * @param callback The user-supplied callback to execute when the request is complete
     * @param maxTimeToBlock The maximum time in milliseconds to block for buffer memory to be available
     */
    public RecordAppendResult append(TopicPartition tp,
                                     long timestamp,
                                     byte[] key,
                                     byte[] value,
                                     Header[] headers,
                                     Callback callback,
                                     long maxTimeToBlock) throws InterruptedException {
        // 记录正在进行append的线程，每一个线程在进入这个方法都会使这个值自增1
        // 然后在方法结束时再自减1
        // 之所以要记录 是因为Sender的run方法中在退出的时候需要去关闭这些还未完成append方法的线程
        appendsInProgress.incrementAndGet();
        ByteBuffer buffer = null;
        if (headers == null) headers = Record.EMPTY_HEADERS;
        try {
            // 在累加器RecordAccumulator中有一个属性batches，这个属性是一个ConcurrentMap
            // 这个map里面通过TopicPartition做key，Deque<ProducerBatch>做值，Deque<ProducerBatch>就是一个批次队列
            // 也就是说每一个TopicPartition(表示了消息对应的topic和所属分区)都有一个对应的消息批次队列
            // 此处也就是去获取TopicPartition对应的批次队列，如果获取不到则新创建一个与之对应的批次队列
            // 三者的关系如下  消息 -> 批次 -> 批次队列  ( -> 表示属于)
            Deque<org.apache.kafka.clients.producer.internals.ProducerBatch> dq = getOrCreateDeque(tp);
            synchronized (dq) {
                if (closed)
                    throw new KafkaException("Producer closed while send in progress");
                // 如果对应的批次队列是上面新创建的 那么这个方法会返回null
                // 如果对应的批次队列是已经存在的，那么会尝试将消息加入到批次队列中的最后一个批次上,如果成功加入会返回一个appendResult
                // 加入的过程会去判断该批次是否能容纳这个消息，如果不够会返回null(没有) 那么还是会走下面的新建批次的流程
                RecordAppendResult appendResult = tryAppend(timestamp, key, value, headers, callback, dq);
                if (appendResult != null)
                    return appendResult;
            }

            // we don't have an in-progress record batch try to allocate a new batch
            byte maxUsableMagic = apiVersions.maxUsableProduceMagic();
            // 取(batch.size)和当前这条消息的大小两者中的最大值作为这个批次的大小
            // 比如默认(batch.size)为16KB 当前这条消息只有1KB 那么就分配 16KB作为批次的大小
            // 而当前消息如果大于(batch.size)比如为48KB，那么就将48KB作为批次的大小
            // 也就是说当消息太大  而默认的(batch.size)又配置得太小时，一个批次就只能放一条消息，同理，一个批次最少也会放一条消息
            int size = Math.max(this.batchSize, AbstractRecords.estimateSizeInBytesUpperBound(maxUsableMagic, compression, key, value, headers));
            log.trace("Allocating a new {} byte message buffer for topic {} partition {}", size, tp.topic(), tp.partition());
            // 按照上面计算出的size作为这个批次的空间大小，生产者可使用的总内存大小取决于buffer.memory大小
            // 比如说buffer.memory设置为32MB，那我们生产者的就拥有一个大小为32MB的缓冲区，每次为消息分配消息批次的时候，就是用这个缓冲区去分配
            // 如果此时有一条新消息要发送，并且这个时候也需要创建一个新的消息批次，而刚好这32MB全部分配完(或者说剩下1MB，但是要创建的消息批次大于1MB)，
            // 那么在allocate方法中会阻塞，如果在max.block.ms时间内还没有可以足够分配的内存，那么就会抛出异常
            buffer = free.allocate(size, maxTimeToBlock);
            synchronized (dq) {
                // Need to check if producer is closed again after grabbing the dequeue lock.
                if (closed)
                    throw new KafkaException("Producer closed while send in progress");
                // 双重检测机制
                // 再进行一次上面的判断，因为在多线程并发的情况下，例如进来了线程A和线程B，两个要发送的消息都是在同一个topic同一个分区，
                // 那么可能一开始A和B在上面判断中都发现这个批次需要新建，但是此处代码是加锁的，所以可能线程A先执行，并且创建了一个批次,
                // 当线程A执行完后，线程B在执行的时候其实这个批次已经存在了，所以只要直接获取就可以了
                RecordAppendResult appendResult = tryAppend(timestamp, key, value, headers, callback, dq);
                if (appendResult != null) {
                    // Somebody else found us a batch, return the one we waited for! Hopefully this doesn't happen often...
                    return appendResult;
                }
                // MemoryRecordsBuilder的作用就是将消息写入到上面分配的内存中(buffer)
                // 此处的步骤就是创建一个消息批次，并且将消息加入到消息批次(batch.tryAppend)中，然后返回一个异步FutureRecordMetadata任务回来
                // FutureRecordMetadata有一个属性为ProduceRequestResult，这个异步任务的get方法就是去获取ProduceRequestResult这个值
                // 这个值在IO线程发送消息后会去将broker返回的一些信息写入到ProduceRequestResult上
                MemoryRecordsBuilder recordsBuilder = recordsBuilder(buffer, maxUsableMagic);
                org.apache.kafka.clients.producer.internals.ProducerBatch batch = new org.apache.kafka.clients.producer.internals.ProducerBatch(tp, recordsBuilder, time.milliseconds());
                org.apache.kafka.clients.producer.internals.FutureRecordMetadata future = Utils.notNull(batch.tryAppend(timestamp, key, value, headers, callback, time.milliseconds()));
                // 将批次加入到批次队列中
                dq.addLast(batch);
                incomplete.add(batch);

                // Don't deallocate this buffer in the finally block as it's being used in the record batch
                buffer = null;
                // 返回一个结果对象  对象中的future属性就是上面返回的异步任务
                return new RecordAppendResult(future, dq.size() > 1 || batch.isFull(), true);
            }
        } finally {
            if (buffer != null)
                free.deallocate(buffer);
            appendsInProgress.decrementAndGet();
        }
    }

    private MemoryRecordsBuilder recordsBuilder(ByteBuffer buffer, byte maxUsableMagic) {
        if (transactionManager != null && maxUsableMagic < RecordBatch.MAGIC_VALUE_V2) {
            throw new UnsupportedVersionException("Attempting to use idempotence with a broker which does not " +
                    "support the required message format (v2). The broker must be version 0.11 or later.");
        }
        return MemoryRecords.builder(buffer, maxUsableMagic, compression, TimestampType.CREATE_TIME, 0L);
    }

    /**
     *  Try to append to a ProducerBatch.
     *
     *  If it is full, we return null and a new batch is created. We also close the batch for record appends to free up
     *  resources like compression buffers. The batch will be fully closed (ie. the record batch headers will be written
     *  and memory records built) in one of the following cases (whichever comes first): right before send,
     *  if it is expired, or when the producer is closed.
     */
    private RecordAppendResult tryAppend(long timestamp, byte[] key, byte[] value, Header[] headers,
                                         Callback callback, Deque<org.apache.kafka.clients.producer.internals.ProducerBatch> deque) {
        org.apache.kafka.clients.producer.internals.ProducerBatch last = deque.peekLast();
        if (last != null) {
            org.apache.kafka.clients.producer.internals.FutureRecordMetadata future = last.tryAppend(timestamp, key, value, headers, callback, time.milliseconds());
            if (future == null)
                last.closeForRecordAppends();
            else
                return new RecordAppendResult(future, deque.size() > 1 || last.isFull(), false);
        }
        return null;
    }

    private boolean isMuted(TopicPartition tp, long now) {
        boolean result = muted.containsKey(tp) && muted.get(tp) > now;
        if (!result)
            muted.remove(tp);
        return result;
    }

    /**
     * Get a list of batches which have been sitting in the accumulator too long and need to be expired.
     */
    public List<org.apache.kafka.clients.producer.internals.ProducerBatch> expiredBatches(int requestTimeout, long now) {
        List<org.apache.kafka.clients.producer.internals.ProducerBatch> expiredBatches = new ArrayList<>();
        for (Map.Entry<TopicPartition, Deque<org.apache.kafka.clients.producer.internals.ProducerBatch>> entry : this.batches.entrySet()) {
            Deque<org.apache.kafka.clients.producer.internals.ProducerBatch> dq = entry.getValue();
            TopicPartition tp = entry.getKey();
            // We only check if the batch should be expired if the partition does not have a batch in flight.
            // This is to prevent later batches from being expired while an earlier batch is still in progress.
            // Note that `muted` is only ever populated if `max.in.flight.request.per.connection=1` so this protection
            // is only active in this case. Otherwise the expiration order is not guaranteed.
            if (!isMuted(tp, now)) {
                synchronized (dq) {
                    // iterate over the batches and expire them if they have been in the accumulator for more than requestTimeOut
                    org.apache.kafka.clients.producer.internals.ProducerBatch lastBatch = dq.peekLast();
                    Iterator<org.apache.kafka.clients.producer.internals.ProducerBatch> batchIterator = dq.iterator();
                    while (batchIterator.hasNext()) {
                        org.apache.kafka.clients.producer.internals.ProducerBatch batch = batchIterator.next();
                        boolean isFull = batch != lastBatch || batch.isFull();
                        // Check if the batch has expired. Expired batches are closed by maybeExpire, but callbacks
                        // are invoked after completing the iterations, since sends invoked from callbacks
                        // may append more batches to the deque being iterated. The batch is deallocated after
                        // callbacks are invoked.
                        if (batch.maybeExpire(requestTimeout, retryBackoffMs, now, this.lingerMs, isFull)) {
                            expiredBatches.add(batch);
                            batchIterator.remove();
                        } else {
                            // Stop at the first batch that has not expired.
                            break;
                        }
                    }
                }
            }
        }
        return expiredBatches;
    }

    /**
     * Re-enqueue the given record batch in the accumulator to retry
     */
    public void reenqueue(org.apache.kafka.clients.producer.internals.ProducerBatch batch, long now) {
        batch.reenqueued(now);
        Deque<org.apache.kafka.clients.producer.internals.ProducerBatch> deque = getOrCreateDeque(batch.topicPartition);
        synchronized (deque) {
            if (transactionManager != null)
                insertInSequenceOrder(deque, batch);
            else
                deque.addFirst(batch);
        }
    }

    /**
     * Split the big batch that has been rejected and reenqueue the split batches in to the accumulator.
     * @return the number of split batches.
     */
    public int splitAndReenqueue(org.apache.kafka.clients.producer.internals.ProducerBatch bigBatch) {
        // Reset the estimated compression ratio to the initial value or the big batch compression ratio, whichever
        // is bigger. There are several different ways to do the reset. We chose the most conservative one to ensure
        // the split doesn't happen too often.
        CompressionRatioEstimator.setEstimation(bigBatch.topicPartition.topic(), compression,
                                                Math.max(1.0f, (float) bigBatch.compressionRatio()));
        Deque<org.apache.kafka.clients.producer.internals.ProducerBatch> dq = bigBatch.split(this.batchSize);
        int numSplitBatches = dq.size();
        Deque<org.apache.kafka.clients.producer.internals.ProducerBatch> partitionDequeue = getOrCreateDeque(bigBatch.topicPartition);
        while (!dq.isEmpty()) {
            org.apache.kafka.clients.producer.internals.ProducerBatch batch = dq.pollLast();
            incomplete.add(batch);
            // We treat the newly split batches as if they are not even tried.
            synchronized (partitionDequeue) {
                if (transactionManager != null) {
                    // We should track the newly created batches since they already have assigned sequences.
                    transactionManager.addInFlightBatch(batch);
                    insertInSequenceOrder(partitionDequeue, batch);
                } else {
                    partitionDequeue.addFirst(batch);
                }
            }
        }
        return numSplitBatches;
    }

    // We will have to do extra work to ensure the queue is in order when requests are being retried and there are
    // multiple requests in flight to that partition. If the first inflight request fails to append, then all the subsequent
    // in flight requests will also fail because the sequence numbers will not be accepted.
    //
    // Further, once batches are being retried, we are reduced to a single in flight request for that partition. So when
    // the subsequent batches come back in sequence order, they will have to be placed further back in the queue.
    //
    // Note that this assumes that all the batches in the queue which have an assigned sequence also have the current
    // producer id. We will not attempt to reorder messages if the producer id has changed, we will throw an
    // IllegalStateException instead.
    private void insertInSequenceOrder(Deque<org.apache.kafka.clients.producer.internals.ProducerBatch> deque, org.apache.kafka.clients.producer.internals.ProducerBatch batch) {
        // When we are requeing and have enabled idempotence, the reenqueued batch must always have a sequence.
        if (batch.baseSequence() == RecordBatch.NO_SEQUENCE)
            throw new IllegalStateException("Trying to reenqueue a batch which doesn't have a sequence even " +
                    "though idempotence is enabled.");

        if (transactionManager.nextBatchBySequence(batch.topicPartition) == null)
            throw new IllegalStateException("We are reenqueueing a batch which is not tracked as part of the in flight " +
                    "requests. batch.topicPartition: " + batch.topicPartition + "; batch.baseSequence: " + batch.baseSequence());

        org.apache.kafka.clients.producer.internals.ProducerBatch firstBatchInQueue = deque.peekFirst();
        if (firstBatchInQueue != null && firstBatchInQueue.hasSequence() && firstBatchInQueue.baseSequence() < batch.baseSequence()) {
            // The incoming batch can't be inserted at the front of the queue without violating the sequence ordering.
            // This means that the incoming batch should be placed somewhere further back.
            // We need to find the right place for the incoming batch and insert it there.
            // We will only enter this branch if we have multiple inflights sent to different brokers and we need to retry
            // the inflight batches.
            //
            // Since we reenqueue exactly one batch a time and ensure that the queue is ordered by sequence always, it
            // is a simple linear scan of a subset of the in flight batches to find the right place in the queue each time.
            List<org.apache.kafka.clients.producer.internals.ProducerBatch> orderedBatches = new ArrayList<>();
            while (deque.peekFirst() != null && deque.peekFirst().hasSequence() && deque.peekFirst().baseSequence() < batch.baseSequence())
                orderedBatches.add(deque.pollFirst());

            log.debug("Reordered incoming batch with sequence {} for partition {}. It was placed in the queue at " +
                    "position {}", batch.baseSequence(), batch.topicPartition, orderedBatches.size());
            // Either we have reached a point where there are batches without a sequence (ie. never been drained
            // and are hence in order by default), or the batch at the front of the queue has a sequence greater
            // than the incoming batch. This is the right place to add the incoming batch.
            deque.addFirst(batch);

            // Now we have to re insert the previously queued batches in the right order.
            for (int i = orderedBatches.size() - 1; i >= 0; --i) {
                deque.addFirst(orderedBatches.get(i));
            }

            // At this point, the incoming batch has been queued in the correct place according to its sequence.
        } else {
            deque.addFirst(batch);
        }
    }

    /**
     * Get a list of nodes whose partitions are ready to be sent, and the earliest time at which any non-sendable
     * partition will be ready; Also return the flag for whether there are any unknown leaders for the accumulated
     * partition batches.
     * <p>
     * A destination node is ready to send data if:
     * <ol>
     * <li>There is at least one partition that is not backing off its send
     * <li><b>and</b> those partitions are not muted (to prevent reordering if
     *   {@value org.apache.kafka.clients.producer.ProducerConfig#MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION}
     *   is set to one)</li>
     * <li><b>and <i>any</i></b> of the following are true</li>
     * <ul>
     *     <li>The record set is full</li>
     *     <li>The record set has sat in the accumulator for at least lingerMs milliseconds</li>
     *     <li>The accumulator is out of memory and threads are blocking waiting for data (in this case all partitions
     *     are immediately considered ready).</li>
     *     <li>The accumulator has been closed</li>
     * </ul>
     * </ol>
     */
    public ReadyCheckResult ready(Cluster cluster, long nowMs) {
        Set<Node> readyNodes = new HashSet<>();
        long nextReadyCheckDelayMs = Long.MAX_VALUE;
        Set<String> unknownLeaderTopics = new HashSet<>();

        boolean exhausted = this.free.queued() > 0;
        for (Map.Entry<TopicPartition, Deque<org.apache.kafka.clients.producer.internals.ProducerBatch>> entry : this.batches.entrySet()) {
            TopicPartition part = entry.getKey();
            Deque<org.apache.kafka.clients.producer.internals.ProducerBatch> deque = entry.getValue();

            Node leader = cluster.leaderFor(part);
            synchronized (deque) {
                if (leader == null && !deque.isEmpty()) {
                    // This is a partition for which leader is not known, but messages are available to send.
                    // Note that entries are currently not removed from batches when deque is empty.
                    unknownLeaderTopics.add(part.topic());
                } else if (!readyNodes.contains(leader) && !isMuted(part, nowMs)) {
                    org.apache.kafka.clients.producer.internals.ProducerBatch batch = deque.peekFirst();
                    if (batch != null) {
                        long waitedTimeMs = batch.waitedTimeMs(nowMs);
                        boolean backingOff = batch.attempts() > 0 && waitedTimeMs < retryBackoffMs;
                        long timeToWaitMs = backingOff ? retryBackoffMs : lingerMs;
                        boolean full = deque.size() > 1 || batch.isFull();
                        boolean expired = waitedTimeMs >= timeToWaitMs;
                        boolean sendable = full || expired || exhausted || closed || flushInProgress();
                        if (sendable && !backingOff) {
                            readyNodes.add(leader);
                        } else {
                            long timeLeftMs = Math.max(timeToWaitMs - waitedTimeMs, 0);
                            // Note that this results in a conservative estimate since an un-sendable partition may have
                            // a leader that will later be found to have sendable data. However, this is good enough
                            // since we'll just wake up and then sleep again for the remaining time.
                            nextReadyCheckDelayMs = Math.min(timeLeftMs, nextReadyCheckDelayMs);
                        }
                    }
                }
            }
        }

        return new ReadyCheckResult(readyNodes, nextReadyCheckDelayMs, unknownLeaderTopics);
    }

    /**
     * Check whether there are any batches which haven't been drained
     */
    public boolean hasUndrained() {
        for (Map.Entry<TopicPartition, Deque<org.apache.kafka.clients.producer.internals.ProducerBatch>> entry : this.batches.entrySet()) {
            Deque<org.apache.kafka.clients.producer.internals.ProducerBatch> deque = entry.getValue();
            synchronized (deque) {
                if (!deque.isEmpty())
                    return true;
            }
        }
        return false;
    }

    /**
     * Drain all the data for the given nodes and collate them into a list of batches that will fit within the specified
     * size on a per-node basis. This method attempts to avoid choosing the same topic-node over and over.
     *
     * @param cluster The current cluster metadata
     * @param nodes The list of node to drain
     * @param maxSize The maximum number of bytes to drain
     * @param now The current unix time in milliseconds
     * @return A list of {@link ProducerBatch} for each node specified with total size less than the requested maxSize.
     */
    public Map<Integer, List<org.apache.kafka.clients.producer.internals.ProducerBatch>> drain(Cluster cluster,
                                                                                               Set<Node> nodes,
                                                                                               int maxSize,
                                                                                               long now) {
        if (nodes.isEmpty())
            return Collections.emptyMap();

        Map<Integer, List<org.apache.kafka.clients.producer.internals.ProducerBatch>> batches = new HashMap<>();
        for (Node node : nodes) {
            int size = 0;
            // 获取要发送的broker节点上的分区信息
            List<PartitionInfo> parts = cluster.partitionsForNode(node.id());
            List<org.apache.kafka.clients.producer.internals.ProducerBatch> ready = new ArrayList<>();
            /* to make starvation less likely this loop doesn't start at 0 */
            int start = drainIndex = drainIndex % parts.size();
            do {
                PartitionInfo part = parts.get(drainIndex);
                TopicPartition tp = new TopicPartition(part.topic(), part.partition());
                // Only proceed if the partition has no in-flight batches.
                if (!isMuted(tp, now)) {
                    Deque<org.apache.kafka.clients.producer.internals.ProducerBatch> deque = getDeque(tp);
                    if (deque != null) {
                        synchronized (deque) {
                            org.apache.kafka.clients.producer.internals.ProducerBatch first = deque.peekFirst();
                            if (first != null) {
                                boolean backoff = first.attempts() > 0 && first.waitedTimeMs(now) < retryBackoffMs;
                                // Only drain the batch if it is not during backoff period.
                                if (!backoff) {
                                    if (size + first.estimatedSizeInBytes() > maxSize && !ready.isEmpty()) {
                                        // there is a rare case that a single batch size is larger than the request size due
                                        // to compression; in this case we will still eventually send this batch in a single
                                        // request
                                        break;
                                    } else {
                                        org.apache.kafka.clients.producer.internals.ProducerIdAndEpoch producerIdAndEpoch = null;
                                        boolean isTransactional = false;
                                        if (transactionManager != null) {
                                            if (!transactionManager.isSendToPartitionAllowed(tp))
                                                break;

                                            producerIdAndEpoch = transactionManager.producerIdAndEpoch();
                                            if (!producerIdAndEpoch.isValid())
                                                // we cannot send the batch until we have refreshed the producer id
                                                break;

                                            isTransactional = transactionManager.isTransactional();

                                            if (!first.hasSequence() && transactionManager.hasUnresolvedSequence(first.topicPartition))
                                                // Don't drain any new batches while the state of previous sequence numbers
                                                // is unknown. The previous batches would be unknown if they were aborted
                                                // on the client after being sent to the broker at least once.
                                                break;

                                            int firstInFlightSequence = transactionManager.firstInFlightSequence(first.topicPartition);
                                            if (firstInFlightSequence != RecordBatch.NO_SEQUENCE && first.hasSequence()
                                                    && first.baseSequence() != firstInFlightSequence)
                                                // If the queued batch already has an assigned sequence, then it is being
                                                // retried. In this case, we wait until the next immediate batch is ready
                                                // and drain that. We only move on when the next in line batch is complete (either successfully
                                                // or due to a fatal broker error). This effectively reduces our
                                                // in flight request count to 1.
                                                break;
                                        }

                                        org.apache.kafka.clients.producer.internals.ProducerBatch batch = deque.pollFirst();
                                        if (producerIdAndEpoch != null && !batch.hasSequence()) {
                                            // If the batch already has an assigned sequence, then we should not change the producer id and
                                            // sequence number, since this may introduce duplicates. In particular,
                                            // the previous attempt may actually have been accepted, and if we change
                                            // the producer id and sequence here, this attempt will also be accepted,
                                            // causing a duplicate.
                                            //
                                            // Additionally, we update the next sequence number bound for the partition,
                                            // and also have the transaction manager track the batch so as to ensure
                                            // that sequence ordering is maintained even if we receive out of order
                                            // responses.
                                            batch.setProducerState(producerIdAndEpoch, transactionManager.sequenceNumber(batch.topicPartition), isTransactional);
                                            transactionManager.incrementSequenceNumber(batch.topicPartition, batch.recordCount);
                                            log.debug("Assigned producerId {} and producerEpoch {} to batch with base sequence " +
                                                            "{} being sent to partition {}", producerIdAndEpoch.producerId,
                                                    producerIdAndEpoch.epoch, batch.baseSequence(), tp);

                                            transactionManager.addInFlightBatch(batch);
                                        }
                                        batch.close();
                                        size += batch.records().sizeInBytes();
                                        ready.add(batch);
                                        batch.drained(now);
                                    }
                                }
                            }
                        }
                    }
                }
                this.drainIndex = (this.drainIndex + 1) % parts.size();
            } while (start != drainIndex);
            batches.put(node.id(), ready);
        }
        return batches;
    }

    private Deque<org.apache.kafka.clients.producer.internals.ProducerBatch> getDeque(TopicPartition tp) {
        return batches.get(tp);
    }

    /**
     * Get the deque for the given topic-partition, creating it if necessary.
     */
    private Deque<org.apache.kafka.clients.producer.internals.ProducerBatch> getOrCreateDeque(TopicPartition tp) {
        Deque<org.apache.kafka.clients.producer.internals.ProducerBatch> d = this.batches.get(tp);
        if (d != null)
            return d;
        d = new ArrayDeque<>();
        Deque<org.apache.kafka.clients.producer.internals.ProducerBatch> previous = this.batches.putIfAbsent(tp, d);
        if (previous == null)
            return d;
        else
            return previous;
    }

    /**
     * Deallocate the record batch
     */
    public void deallocate(org.apache.kafka.clients.producer.internals.ProducerBatch batch) {
        incomplete.remove(batch);
        // Only deallocate the batch if it is not a split batch because split batch are allocated outside the
        // buffer pool.
        if (!batch.isSplitBatch())
            free.deallocate(batch.buffer(), batch.initialCapacity());
    }

    /**
     * Package private for unit test. Get the buffer pool remaining size in bytes.
     */
    long bufferPoolAvailableMemory() {
        return free.availableMemory();
    }

    /**
     * Are there any threads currently waiting on a flush?
     *
     * package private for test
     */
    boolean flushInProgress() {
        return flushesInProgress.get() > 0;
    }

    /* Visible for testing */
    Map<TopicPartition, Deque<org.apache.kafka.clients.producer.internals.ProducerBatch>> batches() {
        return Collections.unmodifiableMap(batches);
    }

    /**
     * Initiate the flushing of data from the accumulator...this makes all requests immediately ready
     */
    public void beginFlush() {
        this.flushesInProgress.getAndIncrement();
    }

    /**
     * Are there any threads currently appending messages?
     */
    private boolean appendsInProgress() {
        return appendsInProgress.get() > 0;
    }

    /**
     * Mark all partitions as ready to send and block until the send is complete
     */
    public void awaitFlushCompletion() throws InterruptedException {
        try {
            for (org.apache.kafka.clients.producer.internals.ProducerBatch batch : this.incomplete.copyAll())
                batch.produceFuture.await();
        } finally {
            this.flushesInProgress.decrementAndGet();
        }
    }

    /**
     * Check whether there are any pending batches (whether sent or unsent).
     */
    public boolean hasIncomplete() {
        return !this.incomplete.isEmpty();
    }

    /**
     * This function is only called when sender is closed forcefully. It will fail all the
     * incomplete batches and return.
     */
    public void abortIncompleteBatches() {
        // We need to keep aborting the incomplete batch until no thread is trying to append to
        // 1. Avoid losing batches.
        // 2. Free up memory in case appending threads are blocked on buffer full.
        // This is a tight loop but should be able to get through very quickly.
        do {
            abortBatches();
        } while (appendsInProgress());
        // After this point, no thread will append any messages because they will see the close
        // flag set. We need to do the last abort after no thread was appending in case there was a new
        // batch appended by the last appending thread.
        abortBatches();
        this.batches.clear();
    }

    /**
     * Go through incomplete batches and abort them.
     */
    private void abortBatches() {
        abortBatches(new KafkaException("Producer is closed forcefully."));
    }

    /**
     * Abort all incomplete batches (whether they have been sent or not)
     */
    void abortBatches(final RuntimeException reason) {
        for (org.apache.kafka.clients.producer.internals.ProducerBatch batch : incomplete.copyAll()) {
            Deque<org.apache.kafka.clients.producer.internals.ProducerBatch> dq = getDeque(batch.topicPartition);
            synchronized (dq) {
                batch.abortRecordAppends();
                dq.remove(batch);
            }
            batch.abort(reason);
            deallocate(batch);
        }
    }

    /**
     * Abort any batches which have not been drained
     */
    void abortUndrainedBatches(RuntimeException reason) {
        for (org.apache.kafka.clients.producer.internals.ProducerBatch batch : incomplete.copyAll()) {
            Deque<org.apache.kafka.clients.producer.internals.ProducerBatch> dq = getDeque(batch.topicPartition);
            boolean aborted = false;
            synchronized (dq) {
                if ((transactionManager != null && !batch.hasSequence()) || (transactionManager == null && !batch.isClosed())) {
                    aborted = true;
                    batch.abortRecordAppends();
                    dq.remove(batch);
                }
            }
            if (aborted) {
                batch.abort(reason);
                deallocate(batch);
            }
        }
    }

    public void mutePartition(TopicPartition tp) {
        muted.put(tp, Long.MAX_VALUE);
    }

    public void unmutePartition(TopicPartition tp, long throttleUntilTimeMs) {
        muted.put(tp, throttleUntilTimeMs);
    }

    /**
     * Close this accumulator and force all the record buffers to be drained
     */
    public void close() {
        this.closed = true;
    }

    /*
     * Metadata about a record just appended to the record accumulator
     */
    public final static class RecordAppendResult {
        public final org.apache.kafka.clients.producer.internals.FutureRecordMetadata future;
        public final boolean batchIsFull;
        public final boolean newBatchCreated;

        public RecordAppendResult(org.apache.kafka.clients.producer.internals.FutureRecordMetadata future, boolean batchIsFull, boolean newBatchCreated) {
            this.future = future;
            this.batchIsFull = batchIsFull;
            this.newBatchCreated = newBatchCreated;
        }
    }

    /*
     * The set of nodes that have at least one complete record batch in the accumulator
     */
    public final static class ReadyCheckResult {
        public final Set<Node> readyNodes;
        public final long nextReadyCheckDelayMs;
        public final Set<String> unknownLeaderTopics;

        public ReadyCheckResult(Set<Node> readyNodes, long nextReadyCheckDelayMs, Set<String> unknownLeaderTopics) {
            this.readyNodes = readyNodes;
            this.nextReadyCheckDelayMs = nextReadyCheckDelayMs;
            this.unknownLeaderTopics = unknownLeaderTopics;
        }
    }

}
