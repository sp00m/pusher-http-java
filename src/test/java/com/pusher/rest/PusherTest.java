package com.pusher.rest;

import static com.pusher.rest.util.Matchers.dataField;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.client.HttpClient;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.GsonBuilder;

/**
 * Tests which mock the HttpClient to check outgoing requests
 */
public class PusherTest {

    static final String APP_ID = "00001";
    static final String KEY    = "157a2f3df564323a4a73";
    static final String SECRET = "3457a88be87f890dcd98";

    private final Mockery context = new JUnit4Mockery();

    private HttpClient httpClient = context.mock(HttpClient.class);

    private final Pusher p = new Pusher(APP_ID, KEY, SECRET);

    @Before
    public void setup() {
        p.setHttpClient(httpClient);
    }

    /*
     * Serialisation tests
     */

    @SuppressWarnings("unused")
    private static class MyPojo {
        private String aString = "value";
        private int aNumber = 42;
    }

    @Test
    public void serialisePojo() throws IOException {
        context.checking(new Expectations() {{
            oneOf(httpClient).execute(with(dataField("{\"aString\":\"value\",\"aNumber\":42}")));
        }});

        p.trigger("my-channel", "event", new MyPojo());
    }

    @Test
    public void customSerialisationGson() throws Exception {
        p.setGsonSerialiser(new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_DASHES).create());

        context.checking(new Expectations() {{
            oneOf(httpClient).execute(with(dataField("{\"a-string\":\"value\",\"a-number\":42}")));
        }});

        p.trigger("my-channel", "event", new MyPojo());
    }

    @Test
    public void customSerialisationByExtension() throws Exception {
        Pusher p = new Pusher(APP_ID, KEY, SECRET) {
            @Override
            protected String serialise(Object data) {
                return (String)data;
            }
        };

        context.checking(new Expectations() {{
            oneOf(httpClient).execute(with(dataField("this is my strong data")));
        }});

        p.trigger("my-channel", "event", "this is my string data");
    }

    @Test
    public void mapShouldBeASuitableObjectForData() throws IOException {
        context.checking(new Expectations() {{
            oneOf(httpClient).execute(with(dataField("{\"name\":\"value\"}")));
        }});

        p.trigger("my-channel", "event", Collections.singletonMap("name", "value"));
    }

    @Test
    public void multiLayerMapShouldSerialiseFully() throws IOException {
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("k1", "v1");
        Map<String, Object> level2 = new HashMap<String, Object>();
        level2.put("k3", "v3");
        List<String> level3 = new ArrayList<String>();
        level3.add("v4");
        level3.add("v5");
        level2.put("k4", level3);;
        data.put("k2", level2);

        final String expectedData = "{\"k1\":\"v1\",\"k2\":{\"k3\":\"v3\",\"k4\":[\"v4\",\"v5\"]}}";
        context.checking(new Expectations() {{
            oneOf(httpClient).execute(with(dataField(expectedData)));
        }});

        p.trigger("my-channel", "event", data);
    }
}