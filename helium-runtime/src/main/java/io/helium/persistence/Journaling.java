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

package io.helium.persistence;

import com.google.common.base.Strings;
import io.helium.event.HeliumEvent;
import journal.io.api.Journal;
import journal.io.api.Journal.WriteType;
import journal.io.api.Location;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

import java.io.File;
import java.util.Optional;

public class Journaling extends Verticle {
    public static final String SUBSCRIPTION = "eventsource";

    private Journal journal = new Journal();
    private Optional<Location> currentLocation = Optional.empty();

    @Override
    public void start() {
        try {
            String directory = container.config().getString("directory", "helium/journal");
            File file = new File(Strings.isNullOrEmpty(directory) ? "helium/journal" : directory);
            journal.setDirectory(file);
            journal.open();
        } catch (Exception exp) {
            exp.printStackTrace();
        }
        vertx.eventBus().registerHandler(SUBSCRIPTION, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                onReceive(event.body());
            }
        });
    }

    public void onReceive(JsonObject message) {
        try {
            HeliumEvent event = HeliumEvent.of(message);

            long startTime = System.currentTimeMillis();
            if (!event.isFromHistory() || event.isNoAuth()) {
                container.logger().info("storing event: " + event);
                Location write = journal.write(event.toString().getBytes(), WriteType.SYNC);
                journal.sync();
                currentLocation = Optional.of(write);
            }
            container.logger().info("onEvent " + (System.currentTimeMillis() - startTime) + "ms; event processing time " + (System.currentTimeMillis() - event.getLong("creationDate")) + "ms");
        } catch (Exception exp) {
            exp.printStackTrace();
        }
    }
}
