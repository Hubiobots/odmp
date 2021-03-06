/*
 * Copyright (c) 2020. The Open Data Management Platform contributors.
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

package io.opendmp.dataflow.service

import io.opendmp.common.message.StopFlowRequestMessage
import io.opendmp.common.message.StopRunPlanRequestMessage
import io.opendmp.dataflow.api.response.RunPlanStatus
import io.opendmp.dataflow.messaging.RunPlanDispatcher
import io.opendmp.dataflow.model.DataflowModel
import io.opendmp.dataflow.model.ProcessorModel
import io.opendmp.dataflow.model.runplan.RunError
import io.opendmp.dataflow.model.runplan.RunPlanModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrElse
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitLast
import kotlinx.coroutines.reactor.mono
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.*
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.adapter.rxjava.toFlowable
import reactor.kotlin.core.publisher.toFlux
import java.util.*

@Service
class RunPlanService(@Autowired private val mongoTemplate: ReactiveMongoTemplate,
                     @Autowired private val dispatcher: RunPlanDispatcher) {

    private val log = LoggerFactory.getLogger(RunPlanService::class.java)
    private val coroutineContext = Dispatchers.IO + SupervisorJob()

    suspend fun generateRunPlan(dataflow: DataflowModel) : RunPlanModel {
        log.debug("Loading Dataflow ${dataflow.name}")
        val pQ = Query(Criteria.where("flowId").isEqualTo(dataflow.id))
        val procs = mongoTemplate.find<ProcessorModel>(pQ).asFlow()
        return RunPlanModel.createRunPlan(dataflow, procs.toList())
    }

    fun updateRunPlan(updatedPlan: RunPlanModel) : Mono<RunPlanModel> {
        return mongoTemplate.save(updatedPlan)
    }

    fun get(id: String) : Mono<RunPlanModel> {
        return mongoTemplate.findById(id)
    }

    fun getForDataflow(dataflowId: String) : Mono<RunPlanModel> {
        return mongoTemplate.findOne(
                Query(Criteria.where("flowId").isEqualTo(dataflowId))
                        .with(Sort.by(Sort.Direction.DESC,"updatedOn")))
    }

    fun getStatusForDataflow(dataflowId: String) : Mono<RunPlanStatus> {
        return getForDataflow(dataflowId).map { rp ->
            val errMap: MutableMap<String, MutableList<RunError>> = mutableMapOf()
            rp.errors.values.filter{it.processorId != null}.forEach{ re ->
                if(errMap.containsKey(re.processorId)) {
                    errMap[re.processorId]!!.add(re)
                } else {
                    errMap[re.processorId!!] = mutableListOf(re)
                }
            }
            RunPlanStatus(
                    id = rp.id,
                    flowId = dataflowId,
                    runState = rp.runState,
                    processorErrors = errMap
            )
        }
    }

    suspend fun dispatchDataflow(dataflow: DataflowModel) {
        val runPlan = mongoTemplate.save(generateRunPlan(dataflow))
        log.info("Dispatching Dataflow ${dataflow.name}")
        runPlan.toFuture().thenAccept {
            dispatcher.dispatchRunPlan(it.createStartMessage())
        }
    }

    fun redispatchRunPlan(runPlan: RunPlanModel) {
        dispatcher.dispatchRunPlan(runPlan.createStartMessage())
    }

    fun dispatchDataflow(dataflow: Mono<DataflowModel>) {
        dataflow.toFuture().thenAccept{
            CoroutineScope(coroutineContext).launch {
                dispatchDataflow(it)
            }
        }
    }

    suspend fun dispatchDataflows() {
        log.info("Loading enabled dataflows")
        val dfQ = Query(Criteria.where("enabled").isEqualTo(true))
        mongoTemplate.find<DataflowModel>(dfQ).asFlow().collect { df ->
            val rp = getForDataflow(df.id).awaitFirstOrNull()
            if(rp == null){ dispatchDataflow(df) } else { redispatchRunPlan(rp) }
        }
    }

    fun stopDataflow(dataflowId: String) {
        dispatcher.stopDataflow(StopFlowRequestMessage(UUID.randomUUID().toString(), dataflowId))
        mongoTemplate.remove<RunPlanModel>(
                Query(Criteria.where("flowId").isEqualTo(dataflowId))).block()
    }

    /**
     * On application start, we want to immediately start loading and dispatching Dataflows
     */
    @EventListener
    fun onApplicationStart(event: ApplicationReadyEvent) {
        val scope = CoroutineScope(coroutineContext)
        scope.launch {
            dispatchDataflows()
        }
    }
}