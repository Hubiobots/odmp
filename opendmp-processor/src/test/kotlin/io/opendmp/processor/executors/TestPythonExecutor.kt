/*
 * Copyright (c) 2020. James Adam and the Open Data Management Platform contributors.
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

package io.opendmp.processor.executors

import io.opendmp.processor.config.RedisConfig
import io.opendmp.processor.handler.RunPlanRequestHandler
import io.opendmp.processor.messaging.RunPlanRequestRouter
import io.opendmp.processor.messaging.RunPlanStatusDispatcher
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.cloud.consul.serviceregistry.ConsulAutoServiceRegistration
import org.springframework.test.context.junit.jupiter.SpringExtension

@SpringBootTest
@ExtendWith(SpringExtension::class)
class TestPythonExecutor {

    @MockBean
    lateinit var  redisConfig: RedisConfig

    @MockBean
    lateinit var runPlanRequestHandler: RunPlanRequestHandler

    @MockBean
    lateinit var runPlanRequestRouter: RunPlanRequestRouter

    @MockBean
    lateinit var runPlanStatusDispatcher: RunPlanStatusDispatcher

    @MockBean
    lateinit var consulAutoServiceRegistration: ConsulAutoServiceRegistration

    @Test
    fun `Python executor should return result as byte array`() {
        val pyEx = PythonExecutor()
        val script = """
            def process(data):
              result = map(lambda x: x * 2, data)
              return array("b", result)
        """.trimIndent()
        val data = listOf(1, 2, 3, 4, 5).map{it.toByte()}.toByteArray()
        val result = pyEx.executeScript(script, data)
        assertNotNull(result)
        val resultInts = result.toList().map { it.toInt() }
        assertEquals(listOf(2, 4, 6, 8, 10), resultInts)
    }

}