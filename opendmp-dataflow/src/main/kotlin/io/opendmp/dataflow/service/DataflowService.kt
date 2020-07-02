package io.opendmp.dataflow.service

import com.mongodb.client.result.DeleteResult
import io.opendmp.dataflow.api.request.CreateDataflowRequest
import io.opendmp.dataflow.model.DataflowModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import org.springframework.data.mongodb.core.*
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class DataflowService (private val mongoTemplate: ReactiveMongoTemplate) {

    fun createDataflow(data : CreateDataflowRequest) : Mono<DataflowModel> {

        val dataflow = DataflowModel(
                name = data.name,
                description = data.description,
                group = data.group,
                creator = "")
        return mongoTemplate.save<DataflowModel>(dataflow)
    }

    fun get(id: String) : Mono<DataflowModel> {
        return mongoTemplate.findById<DataflowModel>(id)
    }

    suspend fun getList() : Flow<DataflowModel> {
        return mongoTemplate.findAll<DataflowModel>().asFlow()
    }

    fun delete(id: String) : Mono<DeleteResult> {
        val query = Query(Criteria.where("id").isEqualTo(id))
        return mongoTemplate.remove<DataflowModel>(query)
    }

}