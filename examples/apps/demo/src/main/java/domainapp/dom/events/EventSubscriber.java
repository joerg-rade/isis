/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package domainapp.dom.events;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import org.apache.isis.applib.annotation.DomainService;
import org.apache.isis.applib.annotation.NatureOfService;
import org.apache.isis.applib.events.domain.AbstractDomainEvent;
import org.apache.isis.applib.services.eventbus.EventBusService;
import org.apache.isis.applib.services.wrapper.WrapperFactory;

import static domainapp.utils.DemoUtils.emphasize;

import domainapp.dom.events.EventLogMenu.EventTestProgrammaticEvent;
import lombok.extern.log4j.Log4j2;

@DomainService(nature=NatureOfService.DOMAIN)
@Log4j2 @Component
public class EventSubscriber {

    @Inject private WrapperFactory wrapper;
    @Inject private EventBusService eventBusService;
    @Inject private EventLogRepository eventLogRepository;

    public static class EventSubscriberEvent extends AbstractDomainEvent<Object> {}

    @PostConstruct
    public void init() {
        log.info(emphasize("EventSubscriber - PostConstruct"));
        eventBusService.post(new EventSubscriberEvent());
    }

    @EventListener(EventTestProgrammaticEvent.class)
    public void on(EventTestProgrammaticEvent ev) {

        if(ev.getEventPhase() != null && !ev.getEventPhase().isExecuted()) {
            return;
        }

        log.info(emphasize("DomainEvent: "+ev.getClass().getName()));
        
        // store in event log
        wrapper.async(this)
        .run(EventSubscriber::storeEvent, ev);

    }

    public void storeEvent(EventTestProgrammaticEvent ev) {
        eventLogRepository.add(EventLogEntry.of(ev));
    }

}
