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

import io.perfana.eventscheduler.api.config.EventContext;

public class WiremockEventContext extends EventContext {
    private final String wiremockFilesDir;
    private final String wiremockUrl;
    private final boolean useProxy;
    private final boolean continueOnUploadError;

    protected WiremockEventContext(EventContext context, String wiremockFilesDir, String wiremockUrl, boolean useProxy, boolean continueOnUploadError) {
        super(context, WiremockEventFactory.class.getName());
        this.wiremockFilesDir = wiremockFilesDir;
        this.wiremockUrl = wiremockUrl;
        this.useProxy = useProxy;
        this.continueOnUploadError = continueOnUploadError;
    }

    public String getWiremockFilesDir() {
        return wiremockFilesDir;
    }

    public String getWiremockUrl() {
        return wiremockUrl;
    }

    public boolean isUseProxy() {
        return useProxy;
    }

    public boolean isContinueOnUploadError() {
        return continueOnUploadError;
    }

    @Override
    public String toString() {
        return "WiremockEventConfig{" +
            "wiremockFilesDir='" + wiremockFilesDir + '\'' +
            ", wiremockUrl='" + wiremockUrl + '\'' +
            ", useProxy=" + useProxy +
            ", continueOnUploadError=" + continueOnUploadError +
            "} " + super.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        WiremockEventContext that = (WiremockEventContext) o;

        if (useProxy != that.useProxy) return false;
        if (continueOnUploadError != that.continueOnUploadError) return false;
        if (!wiremockFilesDir.equals(that.wiremockFilesDir)) return false;
        return wiremockUrl.equals(that.wiremockUrl);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + wiremockFilesDir.hashCode();
        result = 31 * result + wiremockUrl.hashCode();
        result = 31 * result + (useProxy ? 1 : 0);
        result = 31 * result + (continueOnUploadError ? 1 : 0);
        return result;
    }
}
