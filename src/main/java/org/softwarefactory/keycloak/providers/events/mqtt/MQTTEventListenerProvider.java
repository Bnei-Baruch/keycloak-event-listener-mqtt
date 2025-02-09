/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
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

package org.softwarefactory.keycloak.providers.events.mqtt;

import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.simple.JSONObject;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.OperationType;
import org.softwarefactory.keycloak.providers.events.models.MQTTMessageOptions;

/**
 * @author <a href="mailto:mhuin@redhat.com">Matthieu Huin</a>
 */
public class MQTTEventListenerProvider implements EventListenerProvider {
    private static final Logger logger = Logger.getLogger(MQTTEventListenerProvider.class.getName());

    private IMqttClient client;
    
    private Set<EventType> excludedEvents;
    private Set<OperationType> excludedAdminEvents;
    private MQTTMessageOptions messageOptions;


    public MQTTEventListenerProvider(Set<EventType> excludedEvents, Set<OperationType> excludedAdminEvents, MQTTMessageOptions messageOptions, IMqttClient client) {
        this.excludedEvents = excludedEvents;
        this.excludedAdminEvents = excludedAdminEvents;
        this.client = client;
        this.messageOptions = messageOptions;
    }

    @Override
    public void onEvent(Event event) {
        // Take only login events
        if (event.getType().toString() == "LOGIN") {
            sendMqttMessage(convertEvent(event));
        }
    }

    @Override
    public void onEvent(AdminEvent event, boolean includeRepresentation) {
        // Ignore excluded operations
        //if (excludedAdminEvents == null || !excludedAdminEvents.contains(event.getOperationType())) {
        //    sendMqttMessage(convertAdminEvent(event));
        //}
    }

    private void sendMqttMessage(String event) {
        try {
            logger.log(Level.FINE, "Event: {0}", event);
            MqttMessage payload = toPayload(event);
            payload.setQos(messageOptions.qos);
            payload.setRetained(messageOptions.retained);
            client.publish(messageOptions.topic, payload);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Publishing failed!", e);
        }
    }

    private MqttMessage toPayload(String s) {
        byte[] payload = s.getBytes();
        return new MqttMessage(payload);
    }

    private String convertEvent(Event event) {
        JSONObject ev = new JSONObject();

        ev.put("clientId", event.getClientId());
        ev.put("error", event.getError());
        ev.put("ipAddress", event.getIpAddress());
        ev.put("realmId", event.getRealmId());
        ev.put("sessionId", event.getSessionId());
        ev.put("time", event.getTime());
        ev.put("type", event.getType().toString());
        ev.put("userId", event.getUserId());

        JSONObject evDetails = new JSONObject();
        if (event.getDetails() != null) {
            for (Map.Entry<String, String> e : event.getDetails().entrySet()) {
                evDetails.put(e.getKey(), e.getValue());
            }
        }
        ev.put("details", evDetails);

        return ev.toString();
    }

    private String convertAdminEvent(AdminEvent adminEvent) {
        JSONObject ev = new JSONObject();


        ev.put("clientId", adminEvent.getAuthDetails().getClientId());
        ev.put("error", adminEvent.getError());
        ev.put("ipAddress", adminEvent.getAuthDetails().getIpAddress());
        ev.put("realmId", adminEvent.getAuthDetails().getRealmId());
        ev.put("representation", adminEvent.getRepresentation());
        ev.put("resourcePath", adminEvent.getResourcePath());
        ev.put("resourceType", adminEvent.getResourceTypeAsString());
        ev.put("time", adminEvent.getTime());
        ev.put("type", adminEvent.getOperationType().toString());
        ev.put("userId", adminEvent.getAuthDetails().getUserId());

        return ev.toString();
    }

    @Override
    public void close() {
    }

}
