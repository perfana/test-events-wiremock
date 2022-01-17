/*
 * Copyright (C) 2022 Peter Paul Bakker, Perfana
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

import io.perfana.eventscheduler.api.EventLogger;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.function.Function;

class WiremockClient {

    private static final Charset CHARSET_UTF8 = StandardCharsets.UTF_8;

    private final HttpClient httpClient;
    private final String baseUrl;
    private final EventLogger logger;

    WiremockClient(String url, EventLogger logger, boolean useProxy) {
        this.httpClient = createHttpClient(useProxy);
        this.baseUrl = url;
        this.logger = logger;
    }

    private HttpClient createHttpClient(boolean useProxy) {

        HttpClientBuilder httpClientBuilder = HttpClients.custom();

        if (useProxy) {
            HttpHost httpProxy = new HttpHost("localhost", 8888);
            DefaultProxyRoutePlanner routePlanner = new DefaultProxyRoutePlanner(httpProxy);
            httpClientBuilder.setRoutePlanner(routePlanner);
        }

        return httpClientBuilder.build();
    }

    private static String responseToString(HttpResponse response) throws IOException {
        StringBuilder result = new StringBuilder(1024);
        try (BufferedReader rd = new BufferedReader(
                new InputStreamReader(response.getEntity().getContent()))) {

            String line;
            while ((line = rd.readLine()) != null) {
                result.append(line);
            }
        }
        return result.toString();
    }

    private HttpResponse executeRequest(HttpUriRequest request) throws IOException {
        HttpResponse response = httpClient.execute(request);
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode < 200 || statusCode > 299) {
            throw new WiremockClientException(String.format("Unexpected status code: %d for request: %s", statusCode, request));
        }
        return response;
    }

    void uploadFileWithReplacements(String fileContents, Map<String, String> replacements) {
        String uri = String.format("%s/__admin/mappings", baseUrl);

        try {
            URIBuilder uriBuilder = new URIBuilder(uri);

            HttpPost httpPost = new HttpPost(uriBuilder.build());
            String replaced = injectReplacements(fileContents, replacements);

            StringEntity data = new StringEntity(replaced, CHARSET_UTF8);

            httpPost.setEntity(data);

            HttpResponse response = executeRequest(httpPost);
            String result = responseToString(response);
            logger.debug(result);
        } catch (URISyntaxException | IOException e) {
            throw new WiremockClientException("call to wiremock failed", e);
        }
    }

    private String injectReplacements(String fileContents, Map<String, String> replacements) {
        return replacements.entrySet().stream()
                .map(token -> (Function<String, String>) s -> replaceAllTokensInString(s, token))
                .reduce(Function.identity(), Function::andThen)
                .apply(fileContents);
    }

    private String replaceAllTokensInString(String text, Map.Entry<String, String> token) {
        String replacement = token.getValue() == null ? "null" : token.getValue();
        return text.replaceAll("\\$\\{" + token.getKey() + "}", replacement);
    }
}
