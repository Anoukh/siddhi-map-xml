/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.siddhi.extension.output.mapper.xml;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.wso2.siddhi.core.ExecutionPlanRuntime;
import org.wso2.siddhi.core.SiddhiManager;
import org.wso2.siddhi.core.exception.NoSuchAttributeException;
import org.wso2.siddhi.core.stream.input.InputHandler;
import org.wso2.siddhi.core.util.transport.InMemoryBroker;
import org.wso2.siddhi.core.stream.output.sink.InMemoryOutputTransport;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class XMLOutputMapperWithSiddhiQLTestCase {
    static final Logger log = Logger.getLogger(XMLOutputMapperWithSiddhiQLTestCase.class);
    private AtomicInteger wso2Count = new AtomicInteger(0);
    private AtomicInteger ibmCount = new AtomicInteger(0);

    @Before
    public void init() {
        wso2Count.set(0);
        ibmCount.set(0);
    }

    //    from FooStream
    //    select symbol,price,volume
    //    publish inMemory options ("topic", "{{symbol}}")
    //    map xml
    @Test
    public void testXMLOutputMapperDefaultMappingWithSiddhiQL() throws InterruptedException {
        log.info("Test default xml mapping with SiddhiQL");

        InMemoryBroker.Subscriber subscriberWSO2 = new InMemoryBroker.Subscriber() {
            @Override
            public void onMessage(Object msg) {
                wso2Count.incrementAndGet();
            }

            @Override
            public String getTopic() {
                return "WSO2";
            }
        };

        InMemoryBroker.Subscriber subscriberIBM = new InMemoryBroker.Subscriber() {
            @Override
            public void onMessage(Object msg) {
                ibmCount.incrementAndGet();
            }

            @Override
            public String getTopic() {
                return "IBM";
            }
        };

        //subscribe to "inMemory" broker per topic
        InMemoryBroker.subscribe(subscriberWSO2);
        InMemoryBroker.subscribe(subscriberIBM);

        String streams = "" +
                "@Plan:name('TestExecutionPlan')" +
                "define stream FooStream (symbol string, price float, volume long); " +
                "@sink(type='inMemory', topic='{{symbol}}', @map(type='xml')) " +
                "define stream BarStream (symbol string, price float, volume long); ";

        String query = "" +
                "from FooStream " +
                "select * " +
                "insert into BarStream; ";

        SiddhiManager siddhiManager = new SiddhiManager();
        siddhiManager.setExtension("outputtransport:inMemory", InMemoryOutputTransport.class);
        ExecutionPlanRuntime executionPlanRuntime = siddhiManager.createExecutionPlanRuntime(streams + query);
        InputHandler stockStream = executionPlanRuntime.getInputHandler("FooStream");

        executionPlanRuntime.start();
        stockStream.send(new Object[]{"WSO2", 55.6f, 100L});
        stockStream.send(new Object[]{"IBM", 75.6f, 100L});
        stockStream.send(new Object[]{"WSO2", 57.6f, 100L});
        Thread.sleep(100);

        //assert event count
        Assert.assertEquals("Number of WSO2 events", 2, wso2Count.get());
        Assert.assertEquals("Number of IBM events", 1, ibmCount.get());
        executionPlanRuntime.shutdown();

        //unsubscribe from "inMemory" broker per topic
        InMemoryBroker.unsubscribe(subscriberWSO2);
        InMemoryBroker.unsubscribe(subscriberIBM);
    }

    //    from FooStream
    //    select symbol,price
    //    publish inMemory options ("topic", "{{symbol}}")
    //    map xml custom
    @Test
    public void testXMLOutputCustomMappingWithSiddhiQL() throws InterruptedException {
        log.info("Test custom xml mapping with SiddhiQL");
        List<Object> onMessageList = new ArrayList<Object>();

        InMemoryBroker.Subscriber subscriberWSO2 = new InMemoryBroker.Subscriber() {
            @Override
            public void onMessage(Object msg) {
                wso2Count.incrementAndGet();
                onMessageList.add(msg);
            }

            @Override
            public String getTopic() {
                return "WSO2";
            }
        };

        InMemoryBroker.Subscriber subscriberIBM = new InMemoryBroker.Subscriber() {
            @Override
            public void onMessage(Object msg) {
                ibmCount.incrementAndGet();
                onMessageList.add(msg);
            }

            @Override
            public String getTopic() {
                return "IBM";
            }
        };

        //subscribe to "inMemory" broker per topic
        InMemoryBroker.subscribe(subscriberWSO2);
        InMemoryBroker.subscribe(subscriberIBM);

        String streams = "" +
                "@Plan:name('TestExecutionPlan')" +
                "define stream FooStream (symbol string, price float, volume long); " +
                "@sink(type='inMemory', topic='{{symbol}}', @map(type='xml', @payload(" +
                "\"<StockData><Symbol>{{symbol}}</Symbol><Price>{{price}}</Price></StockData>\"))) " +
                "define stream BarStream (symbol string, price float, volume long); ";

        String query = "" +
                "from FooStream " +
                "select * " +
                "insert into BarStream; ";

        SiddhiManager siddhiManager = new SiddhiManager();
        siddhiManager.setExtension("outputtransport:inMemory", InMemoryOutputTransport.class);
        ExecutionPlanRuntime executionPlanRuntime = siddhiManager.createExecutionPlanRuntime(streams + query);
        InputHandler stockStream = executionPlanRuntime.getInputHandler("FooStream");

        executionPlanRuntime.start();
        stockStream.send(new Object[]{"WSO2", 55.6f, 100L});
        stockStream.send(new Object[]{"IBM", 75.6f, 100L});
        stockStream.send(new Object[]{"WSO2", 57.6f, 100L});
        Thread.sleep(100);

        //assert event count
        Assert.assertEquals("Number of WSO2 events", 2, wso2Count.get());
        Assert.assertEquals("Number of IBM events", 1, ibmCount.get());
        //assert custom xml
        Assert.assertEquals("<StockData><Symbol>WSO2</Symbol><Price>55.6</Price></StockData>", onMessageList.get(0).toString());
        Assert.assertEquals("<StockData><Symbol>IBM</Symbol><Price>75.6</Price></StockData>", onMessageList.get(1).toString());
        Assert.assertEquals("<StockData><Symbol>WSO2</Symbol><Price>57.6</Price></StockData>", onMessageList.get(2).toString());
        executionPlanRuntime.shutdown();

        //unsubscribe from "inMemory" broker per topic
        InMemoryBroker.unsubscribe(subscriberWSO2);
        InMemoryBroker.unsubscribe(subscriberIBM);
    }

    //    from FooStream
    //    select symbol,price
    //    publish inMemory options ("topic", "{{symbol}}")
    //    map xml custom
    @Test(expected = NoSuchAttributeException.class)
    public void testNoSuchAttributeExceptionForXMLOutputMapping() throws InterruptedException {
        log.info("Test for non existing attribute in xml mapping with SiddhiQL - expects NoSuchAttributeException");

        String streams = "" +
                "@Plan:name('TestExecutionPlan')" +
                "define stream FooStream (symbol string, price float, volume long); " +
                "@sink(type='inMemory', topic='{{symbol}}', @map(type='xml', @payload(" +
                "\"<StockData><Symbol>{{non-exist}}</Symbol><Price>{{price}}</Price></StockData>\"))) " +
                "define stream BarStream (symbol string, price float, volume long); ";

        String query = "" +
                "from FooStream " +
                "select * " +
                "insert into BarStream; ";

        SiddhiManager siddhiManager = new SiddhiManager();
        siddhiManager.setExtension("outputtransport:inMemory", InMemoryOutputTransport.class);
        ExecutionPlanRuntime executionPlanRuntime = siddhiManager.createExecutionPlanRuntime(streams + query);
        InputHandler stockStream = executionPlanRuntime.getInputHandler("FooStream");

        executionPlanRuntime.start();
        stockStream.send(new Object[]{"WSO2", 55.6f, 100L});
        stockStream.send(new Object[]{"IBM", 75.6f, 100L});
        stockStream.send(new Object[]{"WSO2", 57.6f, 100L});
        Thread.sleep(100);
        executionPlanRuntime.shutdown();
    }
}
