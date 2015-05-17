/*
 * Copyright (C) 2015 Red Bull Media House GmbH <http://www.redbullmediahouse.com> - all rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.rbmhtechnology.eventuate

import DurableEvent._

import scala.collection.immutable.Seq

/**
 * Provider API.
 *
 * Event storage format.
 *
 * @param payload Custom, application-defined event.
 * @param systemTimestamp Wall-clock timestamp, generated by emitter ([[EventsourcedActor]]).
 * @param vectorTimestamp Vector timestamp, generated by emitter ([[EventsourcedActor]]).
 * @param emitterReplicaId Replica id of emitter ([[EventsourcedActor]]).
 * @param emitterAggregateId Aggregate id of emitter ([[EventsourcedActor]]). This is also the default routing destination
 *                           of this event. If defined, the event is routed to [[Eventsourced]] actors and views with a
 *                           matching `aggregateId`. In any case, the event is routed to [[Eventsourced]] actors and
 *                           views with an undefined `aggregateId`.
 * @param customRoutingDestinations Aggregate ids of additional, custom routing destinations. If non-empty, the event is
 *                                  additionally routed to [[Eventsourced]] actors and views with a matching `aggregateId`.
 * @param sourceLogId Source log id from last replication. Equal to `targetLogId` if not replicated yet.
 * @param targetLogId Target log id from last replication.
 * @param sourceLogSequenceNr Source log sequence number from last replication.
 * @param targetLogSequenceNr Target log sequence number from last replication.
 */
case class DurableEvent(
  payload: Any,
  systemTimestamp: Long,
  vectorTimestamp: VectorTime,
  emitterReplicaId: String,
  emitterAggregateId: Option[String] = None,
  customRoutingDestinations: Set[String] = Set(),
  sourceLogId: String = UndefinedLogId,
  targetLogId: String = UndefinedLogId,
  sourceLogSequenceNr: Long = UndefinedSequenceNr,
  targetLogSequenceNr: Long = UndefinedSequenceNr) {

  /**
   * Local sequence number (= `targetLogSequenceNr`).
   */
  def sequenceNr: Long =
    targetLogSequenceNr

  /**
   * Process id of emitter ([[EventsourcedActor]]).
   */
  def emitterProcessId: String =
    DurableEvent.processId(emitterReplicaId, emitterAggregateId)

  /**
   * Returns `true` if this event was emitted by an emitter with given `processId`.
   *
   * @see [[EventsourcedActor]]
   */
  def emitter(processId: String): Boolean =
    this.emitterProcessId == processId

  /**
   * The default routing destination of this event is its `emitterAggregateId`. If defined, the event is
   * routed to [[Eventsourced]] actors and views with a matching `aggregateId`. In any case, the event is
   * routed to [[Eventsourced]] actors and views with an undefined `aggregateId`.
   */
  def defaultRoutingDestination: Option[String] =
    emitterAggregateId

  /**
   * The union of [[defaultRoutingDestination]] and [[customRoutingDestinations]].
   */
  def routingDestinations: Set[String] =
    if (defaultRoutingDestination.isDefined) customRoutingDestinations + defaultRoutingDestination.get else customRoutingDestinations
}

object DurableEvent {
  val UndefinedLogId = ""
  val UndefinedSequenceNr = 1L

  /**
   * Returns a process id for given `replicaId`.
   */
  def processId(replicaId: String): String =
    processId(replicaId, None)

  /**
   * Returns a process id for given `replicaId` and optional `aggregateId`.
   */
  def processId(replicaId: String, aggregateId: Option[String]): String =
    aggregateId.map(id => s"${replicaId}-${id}").getOrElse(replicaId)
}

/**
 * Event batch storage format.
 *
 * @param events Event batch.
 * @param sourceLogId Source log id if the batch was read from a source log (= replicated).
 * @param lastSourceLogSequenceNrRead Last source log sequence number read after reading this batch from the source log.
 */
case class DurableEventBatch(events: Seq[DurableEvent], sourceLogId: Option[String] = None, lastSourceLogSequenceNrRead: Option[Long] = None) {

  /**
   * `true` if this batch was replicated.
   */
  def replicated: Boolean =
    sourceLogId.isDefined

  /**
   * Highest event sequence number contained in the event batch.
   */
  def highestSequenceNr: Option[Long] =
    events.lastOption.map(_.sequenceNr)
}