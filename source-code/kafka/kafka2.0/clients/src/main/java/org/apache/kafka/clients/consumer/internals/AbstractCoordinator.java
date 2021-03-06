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
package org.apache.kafka.clients.consumer.internals;

import org.apache.kafka.clients.ClientResponse;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.errors.AuthenticationException;
import org.apache.kafka.common.errors.DisconnectException;
import org.apache.kafka.common.errors.GroupAuthorizationException;
import org.apache.kafka.common.errors.IllegalGenerationException;
import org.apache.kafka.common.errors.InterruptException;
import org.apache.kafka.common.errors.RebalanceInProgressException;
import org.apache.kafka.common.errors.RetriableException;
import org.apache.kafka.common.errors.UnknownMemberIdException;
import org.apache.kafka.common.metrics.Measurable;
import org.apache.kafka.common.metrics.MetricConfig;
import org.apache.kafka.common.metrics.Metrics;
import org.apache.kafka.common.metrics.Sensor;
import org.apache.kafka.common.metrics.stats.Avg;
import org.apache.kafka.common.metrics.stats.Count;
import org.apache.kafka.common.metrics.stats.Max;
import org.apache.kafka.common.metrics.stats.Meter;
import org.apache.kafka.common.protocol.Errors;
import org.apache.kafka.common.requests.FindCoordinatorRequest;
import org.apache.kafka.common.requests.FindCoordinatorResponse;
import org.apache.kafka.common.requests.HeartbeatRequest;
import org.apache.kafka.common.requests.HeartbeatResponse;
import org.apache.kafka.common.requests.JoinGroupRequest;
import org.apache.kafka.common.requests.JoinGroupRequest.ProtocolMetadata;
import org.apache.kafka.common.requests.JoinGroupResponse;
import org.apache.kafka.common.requests.LeaveGroupRequest;
import org.apache.kafka.common.requests.LeaveGroupResponse;
import org.apache.kafka.common.requests.OffsetCommitRequest;
import org.apache.kafka.common.requests.SyncGroupRequest;
import org.apache.kafka.common.requests.SyncGroupResponse;
import org.apache.kafka.common.utils.KafkaThread;
import org.apache.kafka.common.utils.LogContext;
import org.apache.kafka.common.utils.Time;
import org.slf4j.Logger;

import java.io.Closeable;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * AbstractCoordinator implements group management for a single group member by interacting with
 * a designated Kafka broker (the coordinator). Group semantics are provided by extending this class.
 * See {@link ConsumerCoordinator} for example usage.
 *
 * From a high level, Kafka's group management protocol consists of the following sequence of actions:
 *
 * <ol>
 *     <li>Group Registration: Group members register with the coordinator providing their own metadata
 *         (such as the set of topics they are interested in).</li>
 *     <li>Group/Leader Selection: The coordinator select the members of the group and chooses one member
 *         as the leader.</li>
 *     <li>State Assignment: The leader collects the metadata from all the members of the group and
 *         assigns state.</li>
 *     <li>Group Stabilization: Each member receives the state assigned by the leader and begins
 *         processing.</li>
 * </ol>
 *
 * To leverage this protocol, an implementation must define the format of metadata provided by each
 * member for group registration in {@link #metadata()} and the format of the state assignment provided
 * by the leader in {@link #performAssignment(String, String, Map)} and becomes available to members in
 * {@link #onJoinComplete(int, String, String, ByteBuffer)}.
 *
 * Note on locking: this class shares state between the caller and a background thread which is
 * used for sending heartbeats after the client has joined the group. All mutable state as well as
 * state transitions are protected with the class's monitor. Generally this means acquiring the lock
 * before reading or writing the state of the group (e.g. generation, memberId) and holding the lock
 * when sending a request that affects the state of the group (e.g. JoinGroup, LeaveGroup).
 */
public abstract class AbstractCoordinator implements Closeable {
    public static final String HEARTBEAT_THREAD_PREFIX = "kafka-coordinator-heartbeat-thread";

    private enum MemberState {
        UNJOINED,    // the client is not part of a group
        REBALANCING, // the client has begun rebalancing
        STABLE,      // the client has joined and is sending heartbeats
    }

    private final Logger log;
    private final int sessionTimeoutMs;
    private final boolean leaveGroupOnClose;
    private final GroupCoordinatorMetrics sensors;
    private final org.apache.kafka.clients.consumer.internals.Heartbeat heartbeat;
    protected final int rebalanceTimeoutMs;
    protected final String groupId;
    protected final org.apache.kafka.clients.consumer.internals.ConsumerNetworkClient client;
    protected final Time time;
    protected final long retryBackoffMs;

    private HeartbeatThread heartbeatThread = null;
    private boolean rejoinNeeded = true;
    private boolean needsJoinPrepare = true;
    private MemberState state = MemberState.UNJOINED;
    private org.apache.kafka.clients.consumer.internals.RequestFuture<ByteBuffer> joinFuture = null;
    private Node coordinator = null;
    private Generation generation = Generation.NO_GENERATION;

    private org.apache.kafka.clients.consumer.internals.RequestFuture<Void> findCoordinatorFuture = null;
    
    /**
     * Initialize the coordination manager.
     */
    public AbstractCoordinator(LogContext logContext,
                               org.apache.kafka.clients.consumer.internals.ConsumerNetworkClient client,
                               String groupId,
                               int rebalanceTimeoutMs,
                               int sessionTimeoutMs,
                               org.apache.kafka.clients.consumer.internals.Heartbeat heartbeat,
                               Metrics metrics,
                               String metricGrpPrefix,
                               Time time,
                               long retryBackoffMs,
                               boolean leaveGroupOnClose) {
        this.log = logContext.logger(AbstractCoordinator.class);
        this.client = client;
        this.time = time;
        this.groupId = groupId;
        this.rebalanceTimeoutMs = rebalanceTimeoutMs;
        this.sessionTimeoutMs = sessionTimeoutMs;
        this.leaveGroupOnClose = leaveGroupOnClose;
        this.heartbeat = heartbeat;
        this.sensors = new GroupCoordinatorMetrics(metrics, metricGrpPrefix);
        this.retryBackoffMs = retryBackoffMs;
    }

    public AbstractCoordinator(LogContext logContext,
                               org.apache.kafka.clients.consumer.internals.ConsumerNetworkClient client,
                               String groupId,
                               int rebalanceTimeoutMs,
                               int sessionTimeoutMs,
                               int heartbeatIntervalMs,
                               Metrics metrics,
                               String metricGrpPrefix,
                               Time time,
                               long retryBackoffMs,
                               boolean leaveGroupOnClose) {
        this(logContext, client, groupId, rebalanceTimeoutMs, sessionTimeoutMs,
                new org.apache.kafka.clients.consumer.internals.Heartbeat(sessionTimeoutMs, heartbeatIntervalMs, rebalanceTimeoutMs, retryBackoffMs),
                metrics, metricGrpPrefix, time, retryBackoffMs, leaveGroupOnClose);
    }

    /**
     * Unique identifier for the class of supported protocols (e.g. "consumer" or "connect").
     * @return Non-null protocol type name
     */
    protected abstract String protocolType();

    /**
     * Get the current list of protocols and their associated metadata supported
     * by the local member. The order of the protocols in the list indicates the preference
     * of the protocol (the first entry is the most preferred). The coordinator takes this
     * preference into account when selecting the generation protocol (generally more preferred
     * protocols will be selected as long as all members support them and there is no disagreement
     * on the preference).
     * @return Non-empty map of supported protocols and metadata
     */
    protected abstract List<ProtocolMetadata> metadata();

    /**
     * Invoked prior to each group join or rejoin. This is typically used to perform any
     * cleanup from the previous generation (such as committing offsets for the consumer)
     * @param generation The previous generation or -1 if there was none
     * @param memberId The identifier of this member in the previous group or "" if there was none
     */
    protected abstract void onJoinPrepare(int generation, String memberId);

    /**
     * Perform assignment for the group. This is used by the leader to push state to all the members
     * of the group (e.g. to push partition assignments in the case of the new consumer)
     * @param leaderId The id of the leader (which is this member)
     * @param allMemberMetadata Metadata from all members of the group
     * @return A map from each member to their state assignment
     */
    protected abstract Map<String, ByteBuffer> performAssignment(String leaderId,
                                                                 String protocol,
                                                                 Map<String, ByteBuffer> allMemberMetadata);

    /**
     * Invoked when a group member has successfully joined a group. If this call fails with an exception,
     * then it will be retried using the same assignment state on the next call to {@link #ensureActiveGroup()}.
     *
     * @param generation The generation that was joined
     * @param memberId The identifier for the local member in the group
     * @param protocol The protocol selected by the coordinator
     * @param memberAssignment The assignment propagated from the group leader
     */
    protected abstract void onJoinComplete(int generation,
                                           String memberId,
                                           String protocol,
                                           ByteBuffer memberAssignment);

    /**
     * Visible for testing.
     *
     * Ensure that the coordinator is ready to receive requests.
     *
     * @param timeoutMs Maximum time to wait to discover the coordinator
     * @return true If coordinator discovery and initial connection succeeded, false otherwise
     */
    protected synchronized boolean ensureCoordinatorReady(final long timeoutMs) {
        final long startTimeMs = time.milliseconds();
        long elapsedTime = 0L;

        while (coordinatorUnknown()) {
            final org.apache.kafka.clients.consumer.internals.RequestFuture<Void> future = lookupCoordinator();
            client.poll(future, remainingTimeAtLeastZero(timeoutMs, elapsedTime));
            if (!future.isDone()) {
                // ran out of time
                break;
            }
            // 如果获取组协调器的请求失败了，并且是可重试异常(超时异常)，那么就会重新进入循环
            // 如果是不可重试异常 则直接抛出
            if (future.failed()) {
                if (future.isRetriable()) {
                    elapsedTime = time.milliseconds() - startTimeMs;

                    if (elapsedTime >= timeoutMs) break;

                    log.debug("Coordinator discovery failed, refreshing metadata");
                    client.awaitMetadataUpdate(remainingTimeAtLeastZero(timeoutMs, elapsedTime));
                    elapsedTime = time.milliseconds() - startTimeMs;
                } else
                    throw future.exception();
            } else if (coordinator != null && client.isUnavailable(coordinator)) {
                // we found the coordinator, but the connection has failed, so mark
                // it dead and backoff before retrying discovery
                markCoordinatorUnknown();
                final long sleepTime = Math.min(retryBackoffMs, remainingTimeAtLeastZero(timeoutMs, elapsedTime));
                time.sleep(sleepTime);
                elapsedTime += sleepTime;
            }
        }

        return !coordinatorUnknown();
    }

    protected synchronized org.apache.kafka.clients.consumer.internals.RequestFuture<Void> lookupCoordinator() {
        if (findCoordinatorFuture == null) {
            // 寻找一个节点负载最低的发起请求
            Node node = this.client.leastLoadedNode();
            if (node == null) {
                log.debug("No broker available to send FindCoordinator request");
                return org.apache.kafka.clients.consumer.internals.RequestFuture.noBrokersAvailable();
            } else
                findCoordinatorFuture = sendFindCoordinatorRequest(node);
        }
        return findCoordinatorFuture;
    }

    private synchronized void clearFindCoordinatorFuture() {
        findCoordinatorFuture = null;
    }

    /**
     * Check whether the group should be rejoined (e.g. if metadata changes) or whether a
     * rejoin request is already in flight and needs to be completed.
     *
     * @return true if it should, false otherwise
     */
    protected synchronized boolean rejoinNeededOrPending() {
        // if there's a pending joinFuture, we should try to complete handling it.
        return rejoinNeeded || joinFuture != null;
    }

    /**
     * Check the status of the heartbeat thread (if it is active) and indicate the liveness
     * of the client. This must be called periodically after joining with {@link #ensureActiveGroup()}
     * to ensure that the member stays in the group. If an interval of time longer than the
     * provided rebalance timeout expires without calling this method, then the client will proactively
     * leave the group.
     * @param now current time in milliseconds
     * @throws RuntimeException for unexpected errors raised from the heartbeat thread
     */
    protected synchronized void pollHeartbeat(long now) {
        if (heartbeatThread != null) {
            if (heartbeatThread.hasFailed()) {
                // set the heartbeat thread to null and raise an exception. If the user catches it,
                // the next call to ensureActiveGroup() will spawn a new heartbeat thread.
                RuntimeException cause = heartbeatThread.failureCause();
                heartbeatThread = null;
                throw cause;
            }
            // Awake the heartbeat thread if needed
            if (heartbeat.shouldHeartbeat(now)) {
                notify();
            }
            heartbeat.poll(now);
        }
    }

    protected synchronized long timeToNextHeartbeat(long now) {
        // if we have not joined the group, we don't need to send heartbeats
        if (state == MemberState.UNJOINED)
            return Long.MAX_VALUE;
        return heartbeat.timeToNextHeartbeat(now);
    }

    /**
     * Ensure that the group is active (i.e. joined and synced)
     */
    public void ensureActiveGroup() {
        while (!ensureActiveGroup(Long.MAX_VALUE)) {
            log.warn("still waiting to ensure active group");
        }
    }

    /**
     * Ensure the group is active (i.e., joined and synced)
     *
     * @param timeoutMs A time budget for ensuring the group is active
     * @return true iff the group is active
     */
    boolean ensureActiveGroup(final long timeoutMs) {
        return ensureActiveGroup(timeoutMs, time.milliseconds());
    }

    // Visible for testing
    boolean ensureActiveGroup(long timeoutMs, long startMs) {
        // always ensure that the coordinator is ready because we may have been disconnected
        // when sending heartbeats and does not necessarily require us to rejoin the group.
        // 获取对应的组协调器
        if (!ensureCoordinatorReady(timeoutMs)) {
            return false;
        }
        // 开启心跳线程 (等到加入群组之后才会开始发送心跳包)
        startHeartbeatThreadIfNeeded();

        long joinStartMs = time.milliseconds();
        long joinTimeoutMs = remainingTimeAtLeastZero(timeoutMs, joinStartMs - startMs);
        // 发起加入群组的请求
        return joinGroupIfNeeded(joinTimeoutMs, joinStartMs);
    }

    private synchronized void startHeartbeatThreadIfNeeded() {
        if (heartbeatThread == null) {
            heartbeatThread = new HeartbeatThread();
            heartbeatThread.start();
        }
    }

    private synchronized void disableHeartbeatThread() {
        if (heartbeatThread != null)
            heartbeatThread.disable();
    }

    private void closeHeartbeatThread() {
        HeartbeatThread thread = null;
        synchronized (this) {
            if (heartbeatThread == null)
                return;
            heartbeatThread.close();
            thread = heartbeatThread;
            heartbeatThread = null;
        }
        try {
            thread.join();
        } catch (InterruptedException e) {
            log.warn("Interrupted while waiting for consumer heartbeat thread to close");
            throw new InterruptException(e);
        }
    }

    /**
     * Joins the group without starting the heartbeat thread.
     *
     * Visible for testing.
     *
     * @param timeoutMs Time to complete this action
     * @param startTimeMs Current time when invoked
     * @return true iff the operation succeeded
     */
    boolean joinGroupIfNeeded(final long timeoutMs, final long startTimeMs) {
        long elapsedTime = 0L;

        while (rejoinNeededOrPending()) {
            if (!ensureCoordinatorReady(remainingTimeAtLeastZero(timeoutMs, elapsedTime))) {
                return false;
            }
            elapsedTime = time.milliseconds() - startTimeMs;

            // call onJoinPrepare if needed. We set a flag to make sure that we do not call it a second
            // time if the client is woken up before a pending rebalance completes. This must be called
            // on each iteration of the loop because an event requiring a rebalance (such as a metadata
            // refresh which changes the matched subscription set) can occur while another rebalance is
            // still in progress.
            if (needsJoinPrepare) {
                // 重新加入群组之前的前置操作
                onJoinPrepare(generation.generationId, generation.memberId);
                needsJoinPrepare = false;
            }
            // 初始化一个加入群组的请求
            final org.apache.kafka.clients.consumer.internals.RequestFuture<ByteBuffer> future = initiateJoinGroup();
            // 请求发送
            client.poll(future, remainingTimeAtLeastZero(timeoutMs, elapsedTime));
            if (!future.isDone()) {
                // we ran out of time
                return false;
            }

            if (future.succeeded()) {
                // Duplicate the buffer in case `onJoinComplete` does not complete and needs to be retried.
                ByteBuffer memberAssignment = future.value().duplicate();
                onJoinComplete(generation.generationId, generation.memberId, generation.protocol, memberAssignment);

                // We reset the join group future only after the completion callback returns. This ensures
                // that if the callback is woken up, we will retry it on the next joinGroupIfNeeded.
                resetJoinGroupFuture();
                needsJoinPrepare = true;
            } else {
                resetJoinGroupFuture();
                final RuntimeException exception = future.exception();
                if (exception instanceof UnknownMemberIdException ||
                        exception instanceof RebalanceInProgressException ||
                        exception instanceof IllegalGenerationException)
                    continue;
                else if (!future.isRetriable())
                    throw exception;
                time.sleep(retryBackoffMs);
            }

            if (rejoinNeededOrPending()) {
                elapsedTime = time.milliseconds() - startTimeMs;
            }
        }
        return true;
    }

    private long remainingTimeAtLeastZero(final long timeout, final long elapsedTime) {
        return Math.max(0, timeout - elapsedTime);
    }

    private synchronized void resetJoinGroupFuture() {
        this.joinFuture = null;
    }

    private synchronized org.apache.kafka.clients.consumer.internals.RequestFuture<ByteBuffer> initiateJoinGroup() {
        // we store the join future in case we are woken up by the user after beginning the
        // rebalance in the call to poll below. This ensures that we do not mistakenly attempt
        // to rejoin before the pending rebalance has completed.
        // 还未发送加入群组的请求
        if (joinFuture == null) {
            // fence off the heartbeat thread explicitly so that it cannot interfere with the join group.
            // Note that this must come after the call to onJoinPrepare since we must be able to continue
            // sending heartbeats if that callback takes some time.
            // 禁用心跳线程
            disableHeartbeatThread();
            // 修改消费者状态
            //  UNJOINED 尚未加入群组
            //  REBALANCING, 正在执行rebalance
            //  STABLE 已经加入群组
            state = MemberState.REBALANCING;
            // 发送加入群组的请求
            joinFuture = sendJoinGroupRequest();
            joinFuture.addListener(new org.apache.kafka.clients.consumer.internals.RequestFutureListener<ByteBuffer>() {
                @Override
                public void onSuccess(ByteBuffer value) {
                    // handle join completion in the callback so that the callback will be invoked
                    // even if the consumer is woken up before finishing the rebalance
                    synchronized (AbstractCoordinator.this) {
                        log.info("Successfully joined group with generation {}", generation.generationId);
                        state = MemberState.STABLE;
                        rejoinNeeded = false;

                        if (heartbeatThread != null)
                            heartbeatThread.enable();
                    }
                }

                @Override
                public void onFailure(RuntimeException e) {
                    // we handle failures below after the request finishes. if the join completes
                    // after having been woken up, the exception is ignored and we will rejoin
                    synchronized (AbstractCoordinator.this) {
                        state = MemberState.UNJOINED;
                    }
                }
            });
        }
        return joinFuture;
    }

    /**
     * Join the group and return the assignment for the next generation. This function handles both
     * JoinGroup and SyncGroup, delegating to {@link #performAssignment(String, String, Map)} if
     * elected leader by the coordinator.
     *
     * NOTE: This is visible only for testing
     *
     * @return A request future which wraps the assignment returned from the group leader
     */
    org.apache.kafka.clients.consumer.internals.RequestFuture<ByteBuffer> sendJoinGroupRequest() {
        if (coordinatorUnknown())
            return org.apache.kafka.clients.consumer.internals.RequestFuture.coordinatorNotAvailable();

        // send a join group request to the coordinator
        log.info("(Re-)joining group");
        JoinGroupRequest.Builder requestBuilder = new JoinGroupRequest.Builder(
                groupId,
                this.sessionTimeoutMs,
                this.generation.memberId,
                protocolType(),
                metadata()).setRebalanceTimeout(this.rebalanceTimeoutMs);

        log.debug("Sending JoinGroup ({}) to coordinator {}", requestBuilder, this.coordinator);

        // Note that we override the request timeout using the rebalance timeout since that is the
        // maximum time that it may block on the coordinator. We add an extra 5 seconds for small delays.
        // 会在max.poll.interval.ms的基础上再加5s 来作为加入群组请求的超时时间
        int joinGroupTimeoutMs = Math.max(rebalanceTimeoutMs, rebalanceTimeoutMs + 5000);
        return client.send(coordinator, requestBuilder, joinGroupTimeoutMs)
                .compose(new JoinGroupResponseHandler());
    }

    private class JoinGroupResponseHandler extends CoordinatorResponseHandler<JoinGroupResponse, ByteBuffer> {
        @Override
        public void handle(JoinGroupResponse joinResponse, org.apache.kafka.clients.consumer.internals.RequestFuture<ByteBuffer> future) {
            Errors error = joinResponse.error();
            if (error == Errors.NONE) {
                log.debug("Received successful JoinGroup response: {}", joinResponse);
                sensors.joinLatency.record(response.requestLatencyMs());

                synchronized (AbstractCoordinator.this) {
                    if (state != MemberState.REBALANCING) {
                        // if the consumer was woken up before a rebalance completes, we may have already left
                        // the group. In this case, we do not want to continue with the sync group.
                        future.raise(new UnjoinedGroupException());
                    } else {
                        AbstractCoordinator.this.generation = new Generation(joinResponse.generationId(),
                                joinResponse.memberId(), joinResponse.groupProtocol());
                        if (joinResponse.isLeader()) {
                            onJoinLeader(joinResponse).chain(future);
                        } else {
                            onJoinFollower().chain(future);
                        }
                    }
                }
            } else if (error == Errors.COORDINATOR_LOAD_IN_PROGRESS) {
                log.debug("Attempt to join group rejected since coordinator {} is loading the group.", coordinator());
                // backoff and retry
                future.raise(error);
            } else if (error == Errors.UNKNOWN_MEMBER_ID) {
                // reset the member id and retry immediately
                resetGeneration();
                log.debug("Attempt to join group failed due to unknown member id.");
                future.raise(Errors.UNKNOWN_MEMBER_ID);
            } else if (error == Errors.COORDINATOR_NOT_AVAILABLE
                    || error == Errors.NOT_COORDINATOR) {
                // re-discover the coordinator and retry with backoff
                markCoordinatorUnknown();
                log.debug("Attempt to join group failed due to obsolete coordinator information: {}", error.message());
                future.raise(error);
            } else if (error == Errors.INCONSISTENT_GROUP_PROTOCOL
                    || error == Errors.INVALID_SESSION_TIMEOUT
                    || error == Errors.INVALID_GROUP_ID) {
                // log the error and re-throw the exception
                log.error("Attempt to join group failed due to fatal error: {}", error.message());
                future.raise(error);
            } else if (error == Errors.GROUP_AUTHORIZATION_FAILED) {
                future.raise(new GroupAuthorizationException(groupId));
            } else {
                // unexpected error, throw the exception
                future.raise(new KafkaException("Unexpected error in join group response: " + error.message()));
            }
        }
    }

    private org.apache.kafka.clients.consumer.internals.RequestFuture<ByteBuffer> onJoinFollower() {
        // send follower's sync group with an empty assignment
        SyncGroupRequest.Builder requestBuilder =
                new SyncGroupRequest.Builder(groupId, generation.generationId, generation.memberId,
                        Collections.<String, ByteBuffer>emptyMap());
        log.debug("Sending follower SyncGroup to coordinator {}: {}", this.coordinator, requestBuilder);
        return sendSyncGroupRequest(requestBuilder);
    }

    private org.apache.kafka.clients.consumer.internals.RequestFuture<ByteBuffer> onJoinLeader(JoinGroupResponse joinResponse) {
        try {
            // perform the leader synchronization and send back the assignment for the group
            Map<String, ByteBuffer> groupAssignment = performAssignment(joinResponse.leaderId(), joinResponse.groupProtocol(),
                    joinResponse.members());

            SyncGroupRequest.Builder requestBuilder =
                    new SyncGroupRequest.Builder(groupId, generation.generationId, generation.memberId, groupAssignment);
            log.debug("Sending leader SyncGroup to coordinator {}: {}", this.coordinator, requestBuilder);
            return sendSyncGroupRequest(requestBuilder);
        } catch (RuntimeException e) {
            return org.apache.kafka.clients.consumer.internals.RequestFuture.failure(e);
        }
    }

    private org.apache.kafka.clients.consumer.internals.RequestFuture<ByteBuffer> sendSyncGroupRequest(SyncGroupRequest.Builder requestBuilder) {
        if (coordinatorUnknown())
            return org.apache.kafka.clients.consumer.internals.RequestFuture.coordinatorNotAvailable();
        return client.send(coordinator, requestBuilder)
                .compose(new SyncGroupResponseHandler());
    }

    private class SyncGroupResponseHandler extends CoordinatorResponseHandler<SyncGroupResponse, ByteBuffer> {
        @Override
        public void handle(SyncGroupResponse syncResponse,
                           org.apache.kafka.clients.consumer.internals.RequestFuture<ByteBuffer> future) {
            Errors error = syncResponse.error();
            if (error == Errors.NONE) {
                sensors.syncLatency.record(response.requestLatencyMs());
                future.complete(syncResponse.memberAssignment());
            } else {
                requestRejoin();

                if (error == Errors.GROUP_AUTHORIZATION_FAILED) {
                    future.raise(new GroupAuthorizationException(groupId));
                } else if (error == Errors.REBALANCE_IN_PROGRESS) {
                    log.debug("SyncGroup failed because the group began another rebalance");
                    future.raise(error);
                } else if (error == Errors.UNKNOWN_MEMBER_ID
                        || error == Errors.ILLEGAL_GENERATION) {
                    log.debug("SyncGroup failed: {}", error.message());
                    resetGeneration();
                    future.raise(error);
                } else if (error == Errors.COORDINATOR_NOT_AVAILABLE
                        || error == Errors.NOT_COORDINATOR) {
                    log.debug("SyncGroup failed: {}", error.message());
                    markCoordinatorUnknown();
                    future.raise(error);
                } else {
                    future.raise(new KafkaException("Unexpected error from SyncGroup: " + error.message()));
                }
            }
        }
    }

    /**
     * Discover the current coordinator for the group. Sends a GroupMetadata request to
     * one of the brokers. The returned future should be polled to get the result of the request.
     * @return A request future which indicates the completion of the metadata request
     */
    private org.apache.kafka.clients.consumer.internals.RequestFuture<Void> sendFindCoordinatorRequest(Node node) {
        // initiate the group metadata request
        log.debug("Sending FindCoordinator request to broker {}", node);
        FindCoordinatorRequest.Builder requestBuilder =
                new FindCoordinatorRequest.Builder(FindCoordinatorRequest.CoordinatorType.GROUP, this.groupId);
        return client.send(node, requestBuilder)
                     .compose(new FindCoordinatorResponseHandler());
    }

    private class FindCoordinatorResponseHandler extends org.apache.kafka.clients.consumer.internals.RequestFutureAdapter<ClientResponse, Void> {

        @Override
        public void onSuccess(ClientResponse resp, org.apache.kafka.clients.consumer.internals.RequestFuture<Void> future) {
            log.debug("Received FindCoordinator response {}", resp);
            clearFindCoordinatorFuture();

            FindCoordinatorResponse findCoordinatorResponse = (FindCoordinatorResponse) resp.responseBody();
            Errors error = findCoordinatorResponse.error();
            if (error == Errors.NONE) {
                synchronized (AbstractCoordinator.this) {
                    // use MAX_VALUE - node.id as the coordinator id to allow separate connections
                    // for the coordinator in the underlying network client layer
                    int coordinatorConnectionId = Integer.MAX_VALUE - findCoordinatorResponse.node().id();

                    AbstractCoordinator.this.coordinator = new Node(
                            coordinatorConnectionId,
                            findCoordinatorResponse.node().host(),
                            findCoordinatorResponse.node().port());
                    log.info("Discovered group coordinator {}", coordinator);
                    client.tryConnect(coordinator);
                    heartbeat.resetTimeouts(time.milliseconds());
                }
                future.complete(null);
            } else if (error == Errors.GROUP_AUTHORIZATION_FAILED) {
                future.raise(new GroupAuthorizationException(groupId));
            } else {
                log.debug("Group coordinator lookup failed: {}", error.message());
                future.raise(error);
            }
        }

        @Override
        public void onFailure(RuntimeException e, org.apache.kafka.clients.consumer.internals.RequestFuture<Void> future) {
            clearFindCoordinatorFuture();
            super.onFailure(e, future);
        }
    }

    /**
     * Check if we know who the coordinator is and we have an active connection
     * @return true if the coordinator is unknown
     */
    public boolean coordinatorUnknown() {
        return checkAndGetCoordinator() == null;
    }

    /**
     * Get the coordinator if its connection is still active. Otherwise mark it unknown and
     * return null.
     *
     * @return the current coordinator or null if it is unknown
     */
    protected synchronized Node checkAndGetCoordinator() {
        if (coordinator != null && client.isUnavailable(coordinator)) {
            markCoordinatorUnknown(true);
            return null;
        }
        return this.coordinator;
    }

    private synchronized Node coordinator() {
        return this.coordinator;
    }

    protected synchronized void markCoordinatorUnknown() {
        markCoordinatorUnknown(false);
    }

    protected synchronized void markCoordinatorUnknown(boolean isDisconnected) {
        if (this.coordinator != null) {
            log.info("Group coordinator {} is unavailable or invalid, will attempt rediscovery", this.coordinator);
            Node oldCoordinator = this.coordinator;

            // Mark the coordinator dead before disconnecting requests since the callbacks for any pending
            // requests may attempt to do likewise. This also prevents new requests from being sent to the
            // coordinator while the disconnect is in progress.
            this.coordinator = null;

            // Disconnect from the coordinator to ensure that there are no in-flight requests remaining.
            // Pending callbacks will be invoked with a DisconnectException on the next call to poll.
            if (!isDisconnected)
                client.disconnectAsync(oldCoordinator);
        }
    }

    /**
     * Get the current generation state if the group is stable.
     * @return the current generation or null if the group is unjoined/rebalancing
     */
    protected synchronized Generation generation() {
        if (this.state != MemberState.STABLE)
            return null;
        return generation;
    }

    /**
     * Reset the generation and memberId because we have fallen out of the group.
     */
    protected synchronized void resetGeneration() {
        this.generation = Generation.NO_GENERATION;
        this.rejoinNeeded = true;
        this.state = MemberState.UNJOINED;
    }

    protected synchronized void requestRejoin() {
        this.rejoinNeeded = true;
    }

    /**
     * Close the coordinator, waiting if needed to send LeaveGroup.
     */
    @Override
    public final void close() {
        close(0);
    }

    protected void close(long timeoutMs) {
        try {
            closeHeartbeatThread();
        } finally {

            // Synchronize after closing the heartbeat thread since heartbeat thread
            // needs this lock to complete and terminate after close flag is set.
            synchronized (this) {
                if (leaveGroupOnClose) {
                    maybeLeaveGroup();
                }

                // At this point, there may be pending commits (async commits or sync commits that were
                // interrupted using wakeup) and the leave group request which have been queued, but not
                // yet sent to the broker. Wait up to close timeout for these pending requests to be processed.
                // If coordinator is not known, requests are aborted.
                Node coordinator = checkAndGetCoordinator();
                if (coordinator != null && !client.awaitPendingRequests(coordinator, timeoutMs))
                    log.warn("Close timed out with {} pending requests to coordinator, terminating client connections",
                            client.pendingRequestCount(coordinator));
            }
        }
    }

    /**
     * Leave the current group and reset local generation/memberId.
     */
    public synchronized void maybeLeaveGroup() {
        if (!coordinatorUnknown() && state != MemberState.UNJOINED && generation != Generation.NO_GENERATION) {
            // this is a minimal effort attempt to leave the group. we do not
            // attempt any resending if the request fails or times out.
            log.debug("Sending LeaveGroup request to coordinator {}", coordinator);
            LeaveGroupRequest.Builder request =
                    new LeaveGroupRequest.Builder(groupId, generation.memberId);
            client.send(coordinator, request)
                    .compose(new LeaveGroupResponseHandler());
            client.pollNoWakeup();
        }

        resetGeneration();
    }

    private class LeaveGroupResponseHandler extends CoordinatorResponseHandler<LeaveGroupResponse, Void> {
        @Override
        public void handle(LeaveGroupResponse leaveResponse, org.apache.kafka.clients.consumer.internals.RequestFuture<Void> future) {
            Errors error = leaveResponse.error();
            if (error == Errors.NONE) {
                log.debug("LeaveGroup request returned successfully");
                future.complete(null);
            } else {
                log.debug("LeaveGroup request failed with error: {}", error.message());
                future.raise(error);
            }
        }
    }

    // visible for testing
    synchronized org.apache.kafka.clients.consumer.internals.RequestFuture<Void> sendHeartbeatRequest() {
        log.debug("Sending Heartbeat request to coordinator {}", coordinator);
        HeartbeatRequest.Builder requestBuilder =
                new HeartbeatRequest.Builder(this.groupId, this.generation.generationId, this.generation.memberId);
        return client.send(coordinator, requestBuilder)
                .compose(new HeartbeatResponseHandler());
    }

    private class HeartbeatResponseHandler extends CoordinatorResponseHandler<HeartbeatResponse, Void> {
        @Override
        public void handle(HeartbeatResponse heartbeatResponse, org.apache.kafka.clients.consumer.internals.RequestFuture<Void> future) {
            sensors.heartbeatLatency.record(response.requestLatencyMs());
            Errors error = heartbeatResponse.error();
            if (error == Errors.NONE) {
                log.debug("Received successful Heartbeat response");
                future.complete(null);
            } else if (error == Errors.COORDINATOR_NOT_AVAILABLE
                    || error == Errors.NOT_COORDINATOR) {
                log.info("Attempt to heartbeat failed since coordinator {} is either not started or not valid.",
                        coordinator());
                markCoordinatorUnknown();
                future.raise(error);
            } else if (error == Errors.REBALANCE_IN_PROGRESS) {
                log.info("Attempt to heartbeat failed since group is rebalancing");
                requestRejoin();
                future.raise(Errors.REBALANCE_IN_PROGRESS);
            } else if (error == Errors.ILLEGAL_GENERATION) {
                log.info("Attempt to heartbeat failed since generation {} is not current", generation.generationId);
                resetGeneration();
                future.raise(Errors.ILLEGAL_GENERATION);
            } else if (error == Errors.UNKNOWN_MEMBER_ID) {
                log.info("Attempt to heartbeat failed for since member id {} is not valid.", generation.memberId);
                resetGeneration();
                future.raise(Errors.UNKNOWN_MEMBER_ID);
            } else if (error == Errors.GROUP_AUTHORIZATION_FAILED) {
                future.raise(new GroupAuthorizationException(groupId));
            } else {
                future.raise(new KafkaException("Unexpected error in heartbeat response: " + error.message()));
            }
        }
    }

    protected abstract class CoordinatorResponseHandler<R, T> extends org.apache.kafka.clients.consumer.internals.RequestFutureAdapter<ClientResponse, T> {
        protected ClientResponse response;

        public abstract void handle(R response, org.apache.kafka.clients.consumer.internals.RequestFuture<T> future);

        @Override
        public void onFailure(RuntimeException e, org.apache.kafka.clients.consumer.internals.RequestFuture<T> future) {
            // mark the coordinator as dead
            if (e instanceof DisconnectException) {
                markCoordinatorUnknown(true);
            }
            future.raise(e);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void onSuccess(ClientResponse clientResponse, org.apache.kafka.clients.consumer.internals.RequestFuture<T> future) {
            try {
                this.response = clientResponse;
                R responseObj = (R) clientResponse.responseBody();
                handle(responseObj, future);
            } catch (RuntimeException e) {
                if (!future.isDone())
                    future.raise(e);
            }
        }

    }

    protected Meter createMeter(Metrics metrics, String groupName, String baseName, String descriptiveName) {
        return new Meter(new Count(),
                metrics.metricName(baseName + "-rate", groupName,
                        String.format("The number of %s per second", descriptiveName)),
                metrics.metricName(baseName + "-total", groupName,
                        String.format("The total number of %s", descriptiveName)));
    }

    private class GroupCoordinatorMetrics {
        public final String metricGrpName;

        public final Sensor heartbeatLatency;
        public final Sensor joinLatency;
        public final Sensor syncLatency;

        public GroupCoordinatorMetrics(Metrics metrics, String metricGrpPrefix) {
            this.metricGrpName = metricGrpPrefix + "-coordinator-metrics";

            this.heartbeatLatency = metrics.sensor("heartbeat-latency");
            this.heartbeatLatency.add(metrics.metricName("heartbeat-response-time-max",
                this.metricGrpName,
                "The max time taken to receive a response to a heartbeat request"), new Max());
            this.heartbeatLatency.add(createMeter(metrics, metricGrpName, "heartbeat", "heartbeats"));

            this.joinLatency = metrics.sensor("join-latency");
            this.joinLatency.add(metrics.metricName("join-time-avg",
                    this.metricGrpName,
                    "The average time taken for a group rejoin"), new Avg());
            this.joinLatency.add(metrics.metricName("join-time-max",
                    this.metricGrpName,
                    "The max time taken for a group rejoin"), new Max());
            this.joinLatency.add(createMeter(metrics, metricGrpName, "join", "group joins"));


            this.syncLatency = metrics.sensor("sync-latency");
            this.syncLatency.add(metrics.metricName("sync-time-avg",
                    this.metricGrpName,
                    "The average time taken for a group sync"), new Avg());
            this.syncLatency.add(metrics.metricName("sync-time-max",
                    this.metricGrpName,
                    "The max time taken for a group sync"), new Max());
            this.syncLatency.add(createMeter(metrics, metricGrpName, "sync", "group syncs"));

            Measurable lastHeartbeat =
                new Measurable() {
                    public double measure(MetricConfig config, long now) {
                        return TimeUnit.SECONDS.convert(now - heartbeat.lastHeartbeatSend(), TimeUnit.MILLISECONDS);
                    }
                };
            metrics.addMetric(metrics.metricName("last-heartbeat-seconds-ago",
                this.metricGrpName,
                "The number of seconds since the last controller heartbeat was sent"),
                lastHeartbeat);
        }
    }

    private class HeartbeatThread extends KafkaThread {
        private boolean enabled = false;
        private boolean closed = false;
        private AtomicReference<RuntimeException> failed = new AtomicReference<>(null);

        private HeartbeatThread() {
            super(HEARTBEAT_THREAD_PREFIX + (groupId.isEmpty() ? "" : " | " + groupId), true);
        }

        public void enable() {
            synchronized (AbstractCoordinator.this) {
                log.debug("Enabling heartbeat thread");
                this.enabled = true;
                heartbeat.resetTimeouts(time.milliseconds());
                AbstractCoordinator.this.notify();
            }
        }

        public void disable() {
            synchronized (AbstractCoordinator.this) {
                log.debug("Disabling heartbeat thread");
                this.enabled = false;
            }
        }

        public void close() {
            synchronized (AbstractCoordinator.this) {
                this.closed = true;
                AbstractCoordinator.this.notify();
            }
        }

        private boolean hasFailed() {
            return failed.get() != null;
        }

        private RuntimeException failureCause() {
            return failed.get();
        }

        @Override
        public void run() {
            try {
                log.debug("Heartbeat thread started");
                while (true) {
                    synchronized (AbstractCoordinator.this) {
                        if (closed)
                            return;
                        // 当消费者协调器 (ConsumerCoordinator,存在于消费者端) 向组协调器 (GroupCoordinator,存在于服务端)
                        // 获取到最新的元数据之后(也就是消费者加入群组之后)
                        // 会将enabled置为true,此时心跳线程开始和组协调器进行交互
                        if (!enabled) {
                            AbstractCoordinator.this.wait();
                            continue;
                        }
                        // 如果状态不是稳定状态  则将enabled置为false 直到状态趋于稳定
                        // MemberState状态分为3种
                        // 1、UNJOINED 尚未加入组
                        // 2、REBALANCING  处于重新平衡状态
                        // 3、STABLE 稳定状态
                        if (state != MemberState.STABLE) {
                            // the group is not stable (perhaps because we left the group or because the coordinator
                            // kicked us out), so disable heartbeats and wait for the main thread to rejoin.
                            disable();
                            continue;
                        }

                        client.pollNoWakeup();
                        long now = time.milliseconds();

                        if (coordinatorUnknown()) {
                            if (findCoordinatorFuture != null || lookupCoordinator().failed())
                                // the immediate future check ensures that we backoff properly in the case that no
                                // brokers are available to connect to.
                                AbstractCoordinator.this.wait(retryBackoffMs);
                        // 如果上次接收到心跳包的距离现在的时间超过session.timeout.ms
                        // 那么就会将群组协调器coordinator置为null(coordinator为一个Node对象 表示群组协调器所在节点)
                        // 并且会将该节点放入到pendingDisconnects中(pendingDisconnects是一个队列，存放等待断开的连接)
                        // 客户端再次去发送请求之前 都会先与这个队列中的节点一一断开连接
                        } else if (heartbeat.sessionTimeoutExpired(now)) {
                            // the session timeout has expired without seeing a successful heartbeat, so we should
                            // probably make sure the coordinator is still healthy.
                            markCoordinatorUnknown();
                        // 如果上次进行轮询时间距离现在超过max.poll.interval.ms
                        // 那么就会发送一个离开群组的请求到服务端 并且将MemberState置为UNJOINED
                        } else if (heartbeat.pollTimeoutExpired(now)) {
                            // the poll timeout has expired, which means that the foreground thread has stalled
                            // in between calls to poll(), so we explicitly leave the group.
                            maybeLeaveGroup();
                        // 1、如果上一次心跳包发送是成功的，那么上次发送心跳包的时间距离现在需要大于heartbeat.interval.ms
                        // 才会再次发送，(也就是执行下面的heartbeat.sentHeartbeat(now))
                        // 2、如果上一次心跳包发送是失败的,那么上次发送心跳包的时间距离现在只需要大于retry.backoff.ms就可以再次发送
                        } else if (!heartbeat.shouldHeartbeat(now)) {
                            // poll again after waiting for the retry backoff in case the heartbeat failed or the
                            // coordinator disconnected
                            // 如果没有符合上面说的两个条件 那么线程会挂起一定时间再重新开始循环(retry.backoff.ms)
                            // 或者说心跳请求发送失败的回调函数里面会唤醒线程  AbstractCoordinator.this.notify();
                            AbstractCoordinator.this.wait(retryBackoffMs);
                        } else {
                            // 发送心跳包 在这个方法中会将lastHeartbeatSend(上一次发送心跳包的时间)更新当前时间
                            heartbeat.sentHeartbeat(now);

                            sendHeartbeatRequest().addListener(new org.apache.kafka.clients.consumer.internals.RequestFutureListener<Void>() {
                                @Override
                                public void onSuccess(Void value) {
                                    synchronized (AbstractCoordinator.this) {
                                        heartbeat.receiveHeartbeat(time.milliseconds());
                                    }
                                }

                                @Override
                                public void onFailure(RuntimeException e) {
                                    synchronized (AbstractCoordinator.this) {
                                        if (e instanceof RebalanceInProgressException) {
                                            // it is valid to continue heartbeating while the group is rebalancing. This
                                            // ensures that the coordinator keeps the member in the group for as long
                                            // as the duration of the rebalance timeout. If we stop sending heartbeats,
                                            // however, then the session timeout may expire before we can rejoin.
                                            // 如果接收到的异常是服务端正在重新平衡的异常，那么此处还是会将上次接收到心跳包的时间(lastHeartbeatReceive)
                                            // 更新为当前时间 这是为了不使seesion超时
                                            // (因为session是否超时就是通过判断上次接收到心跳包距离现在的时间是否超过session.timeout.ms)
                                            heartbeat.receiveHeartbeat(time.milliseconds());
                                        } else {
                                            // 将心跳包发送状态置为失败
                                            heartbeat.failHeartbeat();

                                            // wake up the thread if it's sleeping to reschedule the heartbeat
                                            // 唤醒上面的 AbstractCoordinator.this.wait(retryBackoffMs)
                                            AbstractCoordinator.this.notify();
                                        }
                                    }
                                }
                            });
                        }
                    }
                }
            } catch (AuthenticationException e) {
                log.error("An authentication error occurred in the heartbeat thread", e);
                this.failed.set(e);
            } catch (GroupAuthorizationException e) {
                log.error("A group authorization error occurred in the heartbeat thread", e);
                this.failed.set(e);
            } catch (InterruptedException | InterruptException e) {
                Thread.interrupted();
                log.error("Unexpected interrupt received in heartbeat thread", e);
                this.failed.set(new RuntimeException(e));
            } catch (Throwable e) {
                log.error("Heartbeat thread failed due to unexpected error", e);
                if (e instanceof RuntimeException)
                    this.failed.set((RuntimeException) e);
                else
                    this.failed.set(new RuntimeException(e));
            } finally {
                log.debug("Heartbeat thread has closed");
            }
        }

    }

    protected static class Generation {
        public static final Generation NO_GENERATION = new Generation(
                OffsetCommitRequest.DEFAULT_GENERATION_ID,
                JoinGroupRequest.UNKNOWN_MEMBER_ID,
                null);

        public final int generationId;
        public final String memberId;
        public final String protocol;

        public Generation(int generationId, String memberId, String protocol) {
            this.generationId = generationId;
            this.memberId = memberId;
            this.protocol = protocol;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            final Generation that = (Generation) o;
            return generationId == that.generationId &&
                Objects.equals(memberId, that.memberId) &&
                Objects.equals(protocol, that.protocol);
        }

        @Override
        public int hashCode() {
            return Objects.hash(generationId, memberId, protocol);
        }
    }

    private static class UnjoinedGroupException extends RetriableException {

    }

}
