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

package io.opendmp.dataflow.api.controller

import com.amazonaws.services.s3.AmazonS3
import com.c4_soft.springaddons.security.oauth2.test.annotations.WithMockAuthentication
import com.c4_soft.springaddons.security.oauth2.test.webflux.OidcIdAuthenticationTokenWebTestClientConfigurer.oidcId
import com.mongodb.client.result.DeleteResult
import io.opendmp.common.model.DataEvent
import io.opendmp.common.model.DataEventType
import io.opendmp.common.model.properties.DestinationType
import io.opendmp.dataflow.TestConfig
import io.opendmp.dataflow.api.response.DatasetDetail
import io.opendmp.dataflow.api.response.DownloadRequestResponse
import io.opendmp.dataflow.config.MongoConfig
import io.opendmp.dataflow.messaging.ProcessRequester
import io.opendmp.dataflow.messaging.RunPlanDispatcher
import io.opendmp.dataflow.model.CollectionModel
import io.opendmp.dataflow.model.DatasetModel
import io.opendmp.dataflow.service.DatasetService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.mongo.MongoProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.ComponentScan
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.findAllAndRemove
import org.springframework.data.mongodb.core.query.Query
import org.springframework.http.MediaType
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import java.time.Instant
import java.util.*

@ExtendWith(SpringExtension::class)
@WebFluxTest(DatasetController::class)
@ComponentScan(basePackages = [
    "io.opendmp.dataflow.service",
    "io.opendmp.dataflow.messaging",
    "import com.c4_soft.springaddons.security.oauth2.test.webflux"
])
@ContextConfiguration(classes = [MongoConfig::class, DatasetController::class, TestConfig::class])
@EnableConfigurationProperties(MongoProperties::class)
class DatasetControllerTest @Autowired constructor(
        private val client: WebTestClient,
        private val mongoTemplate: ReactiveMongoTemplate,
        private val datasetService: DatasetService
) {
    fun cleanUp() {
        mongoTemplate.findAllAndRemove<DatasetModel>(Query()).blockLast()
        mongoTemplate.findAllAndRemove<CollectionModel>(Query()).blockLast()
    }

    private val baseUri : String = "/dataflow_api/dataset"

    fun createBasicDataset(name: String, destType: DestinationType, location: String)
            : DatasetModel{
        val tag = UUID.randomUUID().toString()
        val history: List<List<DataEvent>> = listOf(listOf(DataEvent(
                dataTag = tag,
                eventType = DataEventType.INGESTED,
                processorId = UUID.randomUUID().toString(),
                processorName = "THE INGESTINATOR")))
        val datasetModel = DatasetModel(
                name = name,
                collectionId = UUID.randomUUID().toString(),
                createdOn = Instant.now(),
                dataflowId = UUID.randomUUID().toString(),
                destinationType = destType,
                location = location,
                dataTag = tag,
                history = history)
        return mongoTemplate.save(datasetModel).block()!!
    }

    @Test
    @WithMockAuthentication(name = "odmp-user", authorities = ["user"])
    fun `should return a dataset`(){
        val dataset = createBasicDataset("FOOBAR", DestinationType.FOLDER, "earth")
        val collection = CollectionModel(
                id = dataset.collectionId,
                name = "FOOBAR",
                creator = "user",
                group = null)
        mongoTemplate.save(collection).block()
        val response = client.get()
                .uri("$baseUri/${dataset.id}")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().is2xxSuccessful
                .expectBody<DatasetDetail>()
                .returnResult()

        val dd = response.responseBody
        assertNotNull(dd)
        assertNotNull(dd!!.collection)
        assertNotNull(dd.dataset)
    }

    @Test
    @WithMockAuthentication(name = "odmp-user", authorities = ["user"])
    fun `should delete a dataset`(){
        val dataset = createBasicDataset("FOOBAR", DestinationType.FOLDER, "earth")
        val response = client.mutateWith(csrf())
                .delete()
                .uri("$baseUri/${dataset.id}")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().is2xxSuccessful
                .expectBody<Any>()
                .returnResult()
        val result = response.responseBody as LinkedHashMap<*, *>
        assertNotNull(result)
        assertEquals(1, result["deletedCount"])
    }

    @Test
    @WithMockAuthentication(name = "odmp-user", authorities = ["user"])
    fun `should request and receive a download token`() {
        val dataset = createBasicDataset("FOOBAR", DestinationType.FOLDER, "earth")

        val response = client.mutateWith(oidcId()).get()
                .uri("$baseUri/${dataset.id}/request_download")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().is2xxSuccessful
                .expectBody<DownloadRequestResponse>()
                .returnResult()

        val dr = response.responseBody
        assertNotNull(dr)
        assertNotNull(dr!!.token)
        println(dr)
    }

    @Test
    @WithMockAuthentication()
    fun `token should validate for file download`() {
        val dataset = createBasicDataset("FOOBAR", DestinationType.NONE, "earth")
        val dlReq = datasetService.requestDownloadToken(dataset)

        val response = client.get()
                .uri("$baseUri/download?token=${dlReq.token}")
                .accept(MediaType.APPLICATION_OCTET_STREAM)
                .exchange()
                .expectStatus().is2xxSuccessful

    }

}