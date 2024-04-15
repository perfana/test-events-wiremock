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

import io.perfana.eventscheduler.api.Event;
import io.perfana.eventscheduler.api.EventFactory;
import io.perfana.eventscheduler.api.EventLogger;
import io.perfana.eventscheduler.api.config.TestContext;
import io.perfana.eventscheduler.api.message.EventMessageBus;

public class WiremockEventFactory implements EventFactory<WiremockEventContext> {
    @Override
    public Event create(WiremockEventContext context, TestContext testContext, EventMessageBus messageBus, EventLogger logger) {
        return new WiremockEvent(context, testContext, messageBus, logger);
    }
}
