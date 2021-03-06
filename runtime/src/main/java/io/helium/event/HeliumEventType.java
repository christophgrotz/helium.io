/*
 * Copyright 2012 The Helium Project
 *
 * The Helium Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package io.helium.event;

import io.helium.persistence.Persistence;

/**
 * Helium Event Types
 *
 * @author Christoph Grotz
 */
public enum HeliumEventType {
    PUSH(Persistence.PUSH),
    SET(Persistence.SET),
    DELETE(Persistence.DELETE),
    UPDATE(Persistence.UPDATE),
    GET(Persistence.GET);

    public final String eventBus;

    private HeliumEventType(String eventBus) {
        this.eventBus = eventBus;
    }


}
