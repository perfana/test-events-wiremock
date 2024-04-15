/*
 * Copyright (C) 2024 Peter Paul Bakker, Perfana
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.perfana.event.wiremock;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.perfana.eventscheduler.log.EventLoggerStdOut;
import org.junit.Rule;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

public class WiremockClientTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(options().port(8568).httpsPort(8569));

    private static final String MESSAGE = "{\n"+
            "    \"request\": {\n"+
            "        \"method\": \"GET\",\n"+
            "        \"url\": \"/delay\"\n"+
            "    },\n"+
            "    \"response\": {\n"+
            "        \"status\": 200,\n"+
            "        \"body\": \"Hello world! from Wiremock, with delay :-)\",\n"+
            "        \"fixedDelayMilliseconds\": ${delay},\n"+
            "\t\t\"headers\": {\n"+
            "            \"Content-Type\": \"text/plain\"\n"+
            "        }\n"+
            "    }\n"+
            "}";

    @Test
    public void uploadFileWithReplacements() {
        WiremockClient client = new WiremockClient("http://localhost:8568", EventLoggerStdOut.INSTANCE_DEBUG, false);

        Map<String,String> replacements = new HashMap<>();
        replacements.put("delay", "2000");
        client.uploadFileWithReplacements(MESSAGE, replacements, "/__admin/mappings");
    }
}