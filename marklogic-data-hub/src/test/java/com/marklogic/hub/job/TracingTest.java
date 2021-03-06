/*
 * Copyright 2012-2018 MarkLogic Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.marklogic.hub.job;

import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.marklogic.hub.FlowManager;
import com.marklogic.hub.HubConfig;
import com.marklogic.hub.HubTestBase;
import com.marklogic.hub.Tracing;
import org.junit.*;
import org.w3c.dom.Document;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marklogic.client.DatabaseClient;
import com.marklogic.client.document.DocumentRecord;
import com.marklogic.client.io.DOMHandle;
import com.marklogic.client.io.JacksonHandle;
import com.marklogic.client.io.StringHandle;
import com.marklogic.client.query.RawStructuredQueryDefinition;
import com.marklogic.client.query.StructuredQueryBuilder;
import com.marklogic.hub.flow.Flow;
import com.marklogic.hub.flow.FlowRunner;
import com.marklogic.hub.impl.HubConfigImpl;
import com.marklogic.mgmt.ManageClient;

public class TracingTest extends HubTestBase {

    private static final String BINARY_HEX_ENCODED_XQY = "89504e470d0a1a0a0000000d494844520000000d0000001308060000004b378797000000017352474200aece1ce900000006624b474400ff00ff00ffa0bda793000000097048597300000b1300000b1301009a9c180000000774494d4507da0811012332d018a204000002af4944415428cf9592cb6e1c4500454f555757573fc6f3308e9f40ac286c909122082b36acf317ec59b2e4a7f8098422a17889402436a3d8c61ecfabbba7bbebc502943d573afb239d2bbeffe1c7d7428849aa539565d9814a9298ea340a04ce39a15325807bebbccb8d591963be534aabaf46d98caa2c383898b13fdb23cd6054569455c5fe74c2d9c95159b73b16cb3521c673a9303c7966397aeee9d47bfe5afc815639c664e42623d50a21252a4970ced1b4bb4e4a34c5f192a75f248cceeff8fdfe35432bf13e60ad63182c2104ac7338ef8921a02c35b7bf1e53ff2610ea635e9c5e90950e44865209b9314829d15a93e7062913a9bc77d44d438c91e9788cd18675bdc5c788f39ec7e58af5664bbbeb98dfdc320cb6550021044288586769bb0e1f5204026b2d006591635d40a72926334a299532998cd199444a49a14b9e1c8d1915157b7b15fbfb5366d329fd60b9b9bb67d7f7a954b1e2f0c50d2f5fc1f4e22dbf5cff44b791f8e0e9074bddb48410e8fb9eaeefb1838dcac78ef575c9add30ced8cd3598e5001008440fe479224a4a98288508e8e873fa7ec6e2db3e909cf8ff791da12a2012211e807cb765bf3f0b0a06e765e451fb1b1c14618a2e261e5c8bb9cb6ed596d362c168fbcbb9e7f501dac8b4a2a8949a648afc06ac6e50c530acabca42c4baaaae4ecf888c13996eb2d314425534a0e2f167cfeada338fb9babd51b525191e739459e331e555455894e53acb5f4d646e93de8d23239481145cd7cf1166f81183fdce7df8e81444a887150cbe68eab9f3fe5fe32a0d353befee44bcc28a0544296698a3c474a496e0c7be3316f2e2fbf519f3d3b6f04b2cc32c5a82cf00cdc3f3cd2343bd6db86f56643bbdbd1b41d8be5aadf6eeb97eae4a3c92ba1d2a70879e87c98eefabe68ba906cb60d4a253ed5babb9ebf5f393bdcd59bf595526a2e628cfcdffd03c6146669f7b691ab0000000049454e44ae426082";
    private static final String BINARY_HEX_ENCODED_SJS = "BinaryNode(\"89504e470d0a1a0a0000000d494844520000000d0000001308060000004b378797000000017352474200aece1ce900000006624b474400ff00ff00ffa0bda793000000097048597300000b1300000b1301009a9c180000000774494d4507da0811012332d018a204000002af4944415428cf9592cb6e1c4500454f555757573fc6f3308e9f40ac286c909122082b36acf317ec59b2e4a7f8098422a17889402436a3d8c61ecfabbba7bbebc502943d573afb239d2bbeffe1c7d7428849aa539565d9814a9298ea340a04ce39a15325807bebbccb8d591963be534aabaf46d98caa2c383898b13fdb23cd6054569455c5fe74c2d9c95159b73b16cb3521c673a9303c7966397aeee9d47bfe5afc815639c664e42623d50a21252a4970ced1b4bb4e4a34c5f192a75f248cceeff8fdfe35432bf13e60ad63182c2104ac7338ef8921a02c35b7bf1e53ff2610ea635e9c5e90950e44865209b9314829d15a93e7062913a9bc77d44d438c91e9788cd18675bdc5c788f39ec7e58af5664bbbeb98dfdc320cb6550021044288586769bb0e1f5204026b2d006591635d40a72926334a299532998cd199444a49a14b9e1c8d1915157b7b15fbfb5366d329fd60b9b9bb67d7f7a954b1e2f0c50d2f5fc1f4e22dbf5cff44b791f8e0e9074bddb48410e8fb9eaeefb1838dcac78ef575c9add30ced8cd3598e5001008440fe479224a4a98288508e8e873fa7ec6e2db3e909cf8ff791da12a2012211e807cb765bf3f0b0a06e765e451fb1b1c14618a2e261e5c8bb9cb6ed596d362c168fbcbb9e7f501dac8b4a2a8949a648afc06ac6e50c530acabca42c4baaaae4ecf888c13996eb2d314425534a0e2f167cfeada338fb9babd51b525191e739459e331e555455894e53acb5f4d646e93de8d23239481145cd7cf1166f81183fdce7df8e81444a887150cbe68eab9f3fe5fe32a0d353befee44bcc28a0544296698a3c474a496e0c7be3316f2e2fbf519f3d3b6f04b2cc32c5a82cf00cdc3f3cd2343bd6db86f56643bbdbd1b41d8be5aadf6eeb97eae4a3c92ba1d2a70879e87c98eefabe68ba906cb60d4a253ed5babb9ebf5f393bdcd59bf595526a2e628cfcdffd03c6146669f7b691ab0000000049454e44ae426082\")";

    
    @Before
    public void setup() throws IOException, URISyntaxException {
        basicSetup();

        enableDebugging();
        clearDatabases(HubConfig.DEFAULT_STAGING_NAME,  HubConfig.DEFAULT_TRACE_NAME, HubConfig.DEFAULT_FINAL_NAME);

        URL url = TracingTest.class.getClassLoader().getResource("tracing-test");
        String path = Paths.get(url.toURI()).toFile().getAbsolutePath();
        installUserModules(getHubConfig(path), true);

       
     }

    @After
    public void beforeEach() {
    	disableTracing();
        clearDatabases(HubConfig.DEFAULT_JOB_NAME, HubConfig.DEFAULT_TRACE_NAME, HubConfig.DEFAULT_FINAL_NAME);
    }


    @Test
    public void runXMLFlowSansTracing() {
        assertEquals(0, getFinalDocCount());
        assertEquals(0, getTracingDocCount());

        Tracing t = Tracing.create(stagingClient);
        assertFalse(t.isEnabled());

        FlowManager fm = FlowManager.create(getHubConfig());
        Flow flow = fm.getFlow("trace-entity", "tracemeXML");

        FlowRunner flowRunner = fm.newFlowRunner()
            .withFlow(flow)
            .withBatchSize(10)
            .withThreadCount(1);
        flowRunner.run();
        flowRunner.awaitCompletion();

        assertEquals(5, getFinalDocCount());
        assertEquals(0, getTracingDocCount());
    }

    @Test
    public void runJSONFlowSansTracing() {
        assertEquals(0, getFinalDocCount());
        assertEquals(0, getTracingDocCount());

        Tracing t = Tracing.create(stagingClient);
        assertFalse(t.isEnabled());

        FlowManager fm = FlowManager.create(getHubConfig());
        Flow flow = fm.getFlow("trace-entity", "tracemeJSON");

        FlowRunner flowRunner = fm.newFlowRunner()
            .withFlow(flow)
            .withBatchSize(10)
            .withThreadCount(1);
        flowRunner.run();
        flowRunner.awaitCompletion();

        assertEquals(5, getFinalDocCount());
        assertEquals(0, getTracingDocCount());
    }

    @Test
    public void runXMLFlowWithTracing() {
        assertEquals(0, getFinalDocCount());
        assertEquals(0, getTracingDocCount());

        Tracing t = Tracing.create(stagingClient);
        assertFalse(t.isEnabled());

        enableTracing();
        assertTrue(t.isEnabled());

        FlowManager fm = FlowManager.create(getHubConfig());
        Flow flow = fm.getFlow("trace-entity", "tracemeXML");

        FlowRunner flowRunner = fm.newFlowRunner()
            .withFlow(flow)
            .withBatchSize(10)
            .withThreadCount(1);
        flowRunner.run();
        flowRunner.awaitCompletion();

        assertEquals(5, getFinalDocCount());
        assertEquals(6, getTracingDocCount());
    }

    @Test
    public void runXqyXmlFlowWithBinaryContent() {
        assertEquals(0, getFinalDocCount());
        assertEquals(0, getTracingDocCount());

        Tracing t = Tracing.create(stagingClient);
        assertFalse(t.isEnabled());

        enableTracing();
        assertTrue(t.isEnabled());

        FlowManager fm = FlowManager.create(getHubConfig());
        Flow flow = fm.getFlow("trace-entity", "tracemeXqyXmlWithBinary");

        FlowRunner flowRunner = fm.newFlowRunner()
            .withFlow(flow)
            .withBatchSize(10)
            .withThreadCount(1);
        flowRunner.run();
        flowRunner.awaitCompletion();

        assertEquals(5, getFinalDocCount());
        assertEquals(6, getTracingDocCount());


        DocumentRecord doc = finalDocMgr.read("/doc/1.xml").next();
        Document finalDoc = doc.getContent(new DOMHandle()).get();
        assertXMLEqual(getXmlFromResource("tracing-test/traces/finalSjsXmlDoc.xml"), finalDoc);

        StructuredQueryBuilder sqb = traceClient.newQueryManager().newStructuredQueryBuilder();
        RawStructuredQueryDefinition sqd = sqb.build(sqb.not(sqb.value(sqb.jsonProperty("label"), "collector")));
        Document node = traceDocMgr.search(sqd, 1).next().getContent(new DOMHandle()).get();
        Assert.assertEquals(4, node.getElementsByTagName("step").getLength());
        Assert.assertEquals("content", node.getElementsByTagName("label").item(0).getTextContent());
        Assert.assertEquals(BINARY_HEX_ENCODED_XQY, node.getElementsByTagName("output").item(0).getTextContent().toLowerCase());
    }

    @Test
    public void runXqyJsonFlowWithBinaryContent() {
        assertEquals(0, getFinalDocCount());
        assertEquals(0, getTracingDocCount());

        Tracing t = Tracing.create(stagingClient);
        assertFalse(t.isEnabled());

        enableTracing();
        assertTrue(t.isEnabled());

        FlowManager fm = FlowManager.create(getHubConfig());
        Flow flow = fm.getFlow("trace-entity", "tracemeXqyJsonWithBinary");

        FlowRunner flowRunner = fm.newFlowRunner()
            .withFlow(flow)
            .withBatchSize(10)
            .withThreadCount(1);
        flowRunner.run();
        flowRunner.awaitCompletion();

        assertEquals(5, getFinalDocCount());
        assertEquals(6, getTracingDocCount());

        DocumentRecord doc = finalDocMgr.read("/doc/1.json").next();
        String finalDoc= doc.getContent(new StringHandle()).get();
        assertJsonEqual(getResource("tracing-test/traces/finalXqyJsonDoc.json"), finalDoc, true);


        StructuredQueryBuilder sqb = traceClient.newQueryManager().newStructuredQueryBuilder();
        RawStructuredQueryDefinition sqd = sqb.build(sqb.not(sqb.value(sqb.jsonProperty("label"), "collector")));
        JsonNode node = traceDocMgr.search(sqd, 1).next().getContent(new JacksonHandle()).get();
        Assert.assertEquals(4, node.get("trace").get("steps").size());
        Assert.assertEquals("content", node.get("trace").get("steps").get(0).get("label").asText());
        Assert.assertEquals(BINARY_HEX_ENCODED_XQY, node.get("trace").get("steps").get(0).get("output").asText().toLowerCase());
    }

    @Test
    public void runJSONFlowWithTracing() {
        assertEquals(0, getFinalDocCount());
        assertEquals(0, getTracingDocCount());

        Tracing t = Tracing.create(stagingClient);
        assertFalse(t.isEnabled());

        enableTracing();
        assertTrue(t.isEnabled());

        FlowManager fm = FlowManager.create(getHubConfig());
        Flow flow = fm.getFlow("trace-entity", "tracemeJSON");

        FlowRunner flowRunner = fm.newFlowRunner()
            .withFlow(flow)
            .withBatchSize(10)
            .withThreadCount(1);
        flowRunner.run();
        flowRunner.awaitCompletion();

        assertEquals(5, getFinalDocCount());
        assertEquals(6, getTracingDocCount());
    }


    @Test
    public void runSjsJsonFlowWithBinaryContent() {
        assertEquals(0, getFinalDocCount());
        assertEquals(0, getTracingDocCount());

        Tracing t = Tracing.create(stagingClient);
        assertFalse(t.isEnabled());

        enableTracing();
        assertTrue(t.isEnabled());

        FlowManager fm = FlowManager.create(getHubConfig());
        Flow flow = fm.getFlow("trace-entity", "tracemeSjsJsonWithBinary");

        FlowRunner flowRunner = fm.newFlowRunner()
            .withFlow(flow)
            .withBatchSize(10)
            .withThreadCount(1);
        flowRunner.run();
        flowRunner.awaitCompletion();

        assertEquals(5, getFinalDocCount());
        assertEquals(6, getTracingDocCount());

        DocumentRecord doc = finalDocMgr.read("1").next();
        String finalDoc= doc.getContent(new StringHandle()).get();
        assertJsonEqual(getResource("tracing-test/traces/finalSjsJsonDoc.json"), finalDoc, true);

        StructuredQueryBuilder sqb = traceClient.newQueryManager().newStructuredQueryBuilder();
        RawStructuredQueryDefinition sqd = sqb.build(sqb.not(sqb.value(sqb.jsonProperty("label"), "collector")));
        JsonNode node = traceDocMgr.search(sqd, 1).next().getContent(new JacksonHandle()).get();
        Assert.assertEquals(4, node.get("trace").get("steps").size());
        Assert.assertEquals("content", node.get("trace").get("steps").get(0).get("label").asText());
        Assert.assertEquals(BINARY_HEX_ENCODED_SJS, node.get("trace").get("steps").get(0).get("output").asText());
    }

    @Test
    public void runSjsXmlFlowWithBinaryContent() {
        assertEquals(0, getFinalDocCount());
        assertEquals(0, getTracingDocCount());

        Tracing t = Tracing.create(stagingClient);
        assertFalse(t.isEnabled());

        enableTracing();
        assertTrue(t.isEnabled());

        FlowManager fm = FlowManager.create(getHubConfig());
        Flow flow = fm.getFlow("trace-entity", "tracemeSjsXmlWithBinary");

        FlowRunner flowRunner = fm.newFlowRunner()
            .withFlow(flow)
            .withBatchSize(10)
            .withThreadCount(1);
        flowRunner.run();
        flowRunner.awaitCompletion();

        assertEquals(5, getFinalDocCount());
        assertEquals(6, getTracingDocCount());

        DocumentRecord doc = finalDocMgr.read("1").next();
        Document finalDoc = doc.getContent(new DOMHandle()).get();
        assertXMLEqual(getXmlFromResource("tracing-test/traces/finalSjsXmlDoc.xml"), finalDoc);

        StructuredQueryBuilder sqb = traceClient.newQueryManager().newStructuredQueryBuilder();
        RawStructuredQueryDefinition sqd = sqb.build(sqb.not(sqb.value(sqb.jsonProperty("label"), "collector")));
        Document node = traceDocMgr.search(sqd, 1).next().getContent(new DOMHandle()).get();
        Assert.assertEquals(4, node.getElementsByTagName("step").getLength());
        Assert.assertEquals("content", node.getElementsByTagName("label").item(0).getTextContent());
        Assert.assertEquals(BINARY_HEX_ENCODED_SJS, node.getElementsByTagName("output").item(0).getTextContent());
    }


    @Test
    public void runXMLErrorFlowWithoutTracing() {
        assertEquals(0, getFinalDocCount());
        assertEquals(0, getTracingDocCount());

        Tracing t = Tracing.create(stagingClient);
        assertFalse(t.isEnabled());

        FlowManager fm = FlowManager.create(getHubConfig());
        Flow flow = fm.getFlow("trace-entity", "tracemeXMLError");

        FlowRunner flowRunner = fm.newFlowRunner()
            .withFlow(flow)
            .withBatchSize(10)
            .withThreadCount(1);
        flowRunner.run();
        flowRunner.awaitCompletion();

        assertEquals(0, getFinalDocCount());
        assertEquals(5, getTracingDocCount());

        StructuredQueryBuilder sqb = traceClient.newQueryManager().newStructuredQueryBuilder();
        RawStructuredQueryDefinition sqd = sqb.build(sqb.not(sqb.value(sqb.jsonProperty("label"), "collector")));
        Document node = traceDocMgr.search(sqd, 1).next().getContent(new DOMHandle()).get();
        Assert.assertEquals(1, node.getElementsByTagName("step").getLength());
        Assert.assertEquals("content", node.getElementsByTagName("label").item(0).getTextContent());
    }

    @Test
    public void runXMLWriterErrorFlowWithoutTracing() {
        assertEquals(0, getFinalDocCount());
        assertEquals(0, getTracingDocCount());

        Tracing t = Tracing.create(stagingClient);
        assertFalse(t.isEnabled());

        FlowManager fm = FlowManager.create(getHubConfig());
        Flow flow = fm.getFlow("trace-entity", "tracemeXMLWriterError");

        FlowRunner flowRunner = fm.newFlowRunner()
            .withFlow(flow)
            .withBatchSize(10)
            .withThreadCount(1);
        flowRunner.run();
        flowRunner.awaitCompletion();

        assertEquals(0, getFinalDocCount());
        assertEquals(5, getTracingDocCount());

        StructuredQueryBuilder sqb = traceClient.newQueryManager().newStructuredQueryBuilder();
        RawStructuredQueryDefinition sqd = sqb.build(sqb.not(sqb.value(sqb.jsonProperty("label"), "collector")));
        Document node = traceDocMgr.search(sqd, 1).next().getContent(new DOMHandle()).get();
        Assert.assertEquals(1, node.getElementsByTagName("step").getLength());
        Assert.assertEquals("writer", node.getElementsByTagName("label").item(0).getTextContent());
    }

    @Test
    public void runJSONErrorFlowWithoutTracing() {
        assertEquals(0, getFinalDocCount());
        assertEquals(0, getTracingDocCount());

        Tracing t = Tracing.create(stagingClient);
        assertFalse(t.isEnabled());

        FlowManager fm = FlowManager.create(getHubConfig());
        Flow flow = fm.getFlow("trace-entity", "tracemeJSONError");

        FlowRunner flowRunner = fm.newFlowRunner()
            .withFlow(flow)
            .withBatchSize(10)
            .withThreadCount(1);
        flowRunner.run();
        flowRunner.awaitCompletion();

        assertEquals(0, getFinalDocCount());
        assertEquals(5, getTracingDocCount());

        StructuredQueryBuilder sqb = traceClient.newQueryManager().newStructuredQueryBuilder();
        RawStructuredQueryDefinition sqd = sqb.build(sqb.not(sqb.value(sqb.jsonProperty("label"), "collector")));
        JsonNode node = traceDocMgr.search(sqd, 1).next().getContent(new JacksonHandle()).get();
        System.out.println(node.asText());
        Assert.assertEquals(1, node.get("trace").get("steps").size());
        Assert.assertEquals("content", node.get("trace").get("steps").get(0).get("label").asText());
    }


    @Test
    public void runJSONWriterErrorFlowWithoutTracing() {
        assertEquals(0, getFinalDocCount());
        assertEquals(0, getTracingDocCount());

        Tracing t = Tracing.create(stagingClient);
        assertFalse(t.isEnabled());

        FlowManager fm = FlowManager.create(getHubConfig());
        Flow flow = fm.getFlow("trace-entity", "tracemeJSONWriterError");

        FlowRunner flowRunner = fm.newFlowRunner()
            .withFlow(flow)
            .withBatchSize(10)
            .withThreadCount(1);
        flowRunner.run();
        flowRunner.awaitCompletion();

        assertEquals(0, getFinalDocCount());
        assertEquals(5, getTracingDocCount());

        StructuredQueryBuilder sqb = traceClient.newQueryManager().newStructuredQueryBuilder();
        RawStructuredQueryDefinition sqd = sqb.build(sqb.not(sqb.value(sqb.jsonProperty("label"), "collector")));
        JsonNode node = traceDocMgr.search(sqd, 1).next().getContent(new JacksonHandle()).get();
        Assert.assertEquals(1, node.get("trace").get("steps").size());
        Assert.assertEquals("writer", node.get("trace").get("steps").get(0).get("label").asText());
    }
}
