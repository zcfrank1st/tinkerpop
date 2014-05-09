package com.tinkerpop.gremlin.driver.ser;

import com.tinkerpop.gremlin.driver.message.RequestMessage;
import com.tinkerpop.gremlin.driver.message.ResponseMessage;
import com.tinkerpop.gremlin.structure.Compare;
import com.tinkerpop.gremlin.structure.Edge;
import com.tinkerpop.gremlin.structure.Graph;
import com.tinkerpop.gremlin.structure.Property;
import com.tinkerpop.gremlin.structure.Vertex;
import com.tinkerpop.gremlin.structure.io.graphson.GraphSONTokens;
import com.tinkerpop.gremlin.tinkergraph.structure.TinkerFactory;
import com.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * These tests focus on message serialization and not "result" serialization as test specific to results (e.g.
 * vertices, edges, annotated values, etc.) are handled in the IO packages.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class JsonMessageSerializerV1d0Test {

    public static final JsonMessageSerializerV1d0 SERIALIZER = new JsonMessageSerializerV1d0();
    private static final RequestMessage msg = RequestMessage.create("op")
            .overrideRequestId(UUID.fromString("2D62161B-9544-4F39-AF44-62EC49F9A595")).build();

    @Test
    public void serializeToJsonNullResultReturnsNull() throws Exception {
        final ResponseMessage message = ResponseMessage.create(msg).build();
        final String results = SERIALIZER.serializeResponseAsString(message);
        final JSONObject json = new JSONObject(results);
        assertNotNull(json);
        assertEquals(msg.getRequestId().toString(), json.getString(SerTokens.TOKEN_REQUEST));
        assertEquals(JSONObject.NULL, json.get(SerTokens.TOKEN_RESULT));
    }

    @Test
    public void serializeToJsonIterable() throws Exception {
        final ArrayList<FunObject> funList = new ArrayList<>();
        funList.add(new FunObject("x"));
        funList.add(new FunObject("y"));

        final String results = SERIALIZER.serializeResponseAsString(ResponseMessage.create(msg).result(funList).build());
        final JSONObject json = new JSONObject(results);

        assertNotNull(json);
        assertEquals(msg.getRequestId().toString(), json.getString(SerTokens.TOKEN_REQUEST));
        final JSONArray converted = json.getJSONArray(SerTokens.TOKEN_RESULT);

        assertEquals(2, converted.length());

        assertEquals("x", converted.get(0));
        assertEquals("y", converted.get(1));
    }

    @Test
    public void serializeToJsonIterator() throws Exception {
        final ArrayList<FunObject> funList = new ArrayList<>();
        funList.add(new FunObject("x"));
        funList.add(new FunObject("y"));

        final String results = SERIALIZER.serializeResponseAsString(ResponseMessage.create(msg).result(funList.iterator()).build());
        final JSONObject json = new JSONObject(results);

        assertNotNull(json);
        assertEquals(msg.getRequestId().toString(), json.getString(SerTokens.TOKEN_REQUEST));
        final JSONArray converted = json.getJSONArray(SerTokens.TOKEN_RESULT);

        assertEquals(2, converted.length());

        assertEquals("x", converted.get(0));
        assertEquals("y", converted.get(1));
    }

    @Test
    public void serializeToJsonIteratorNullElement() throws Exception {

        ArrayList<FunObject> funList = new ArrayList<>();
        funList.add(new FunObject("x"));
        funList.add(null);
        funList.add(new FunObject("y"));

        final String results = SERIALIZER.serializeResponseAsString(ResponseMessage.create(msg).result(funList.iterator()).build());
        final JSONObject json = new JSONObject(results);

        assertNotNull(json);
        assertEquals(msg.getRequestId().toString(), json.getString(SerTokens.TOKEN_REQUEST));
        final JSONArray converted = json.getJSONArray(SerTokens.TOKEN_RESULT);

        assertEquals(3, converted.length());

        assertEquals("x", converted.get(0));
        assertEquals(JSONObject.NULL, converted.opt(1));
        assertEquals("y", converted.get(2));
    }

    @Test
    public void serializeToJsonMap() throws Exception {
        final Map<String, Object> map = new HashMap<>();
        final Map<String, String> innerMap = new HashMap<>();
        innerMap.put("a", "b");

        map.put("x", new FunObject("x"));
        map.put("y", "some");
        map.put("z", innerMap);

        final String results = SERIALIZER.serializeResponseAsString(ResponseMessage.create(msg).result(map).build());
        final JSONObject json = new JSONObject(results);

        assertNotNull(json);
        assertEquals(msg.getRequestId().toString(), json.getString(SerTokens.TOKEN_REQUEST));
        final JSONObject jsonObject = json.getJSONObject(SerTokens.TOKEN_RESULT);

        assertNotNull(jsonObject);
        assertEquals("some", jsonObject.optString("y"));
        assertEquals("x", jsonObject.optString("x"));

        final JSONObject innerJsonObject = jsonObject.optJSONObject("z");
        assertNotNull(innerJsonObject);
        assertEquals("b", innerJsonObject.optString("a"));
    }

    @Test
    public void serializeHiddenProperties() throws Exception {
        final Graph g = TinkerGraph.open();
        final Vertex v = g.addVertex("abc", 123);
        v.setProperty(Property.Key.hidden("hidden"), "stephen");

        final Iterable iterable = g.V().toList();
        final String results = SERIALIZER.serializeResponseAsString(ResponseMessage.create(msg).result(iterable).build());
        final JSONObject json = new JSONObject(results);

        assertNotNull(json);
        assertEquals(msg.getRequestId().toString(), json.getString(SerTokens.TOKEN_REQUEST));
        final JSONArray converted = json.getJSONArray(SerTokens.TOKEN_RESULT);

        assertNotNull(converted);
        assertEquals(1, converted.length());

        final JSONObject vertexAsJson = converted.optJSONObject(0);
        assertNotNull(vertexAsJson);

        assertEquals(((Long) v.getId()).intValue(), vertexAsJson.get(GraphSONTokens.ID)); // lossy
        assertEquals(GraphSONTokens.VERTEX, vertexAsJson.get(GraphSONTokens.TYPE));

        final JSONObject properties = vertexAsJson.optJSONObject(GraphSONTokens.PROPERTIES);
        assertNotNull(properties);

        assertEquals(123, properties.getInt("abc"));
        assertEquals("stephen", properties.getString(Property.Key.hidden("hidden")));
    }

    @Test
    public void serializeEdge() throws Exception {
        final Graph g = TinkerGraph.open();
        final Vertex v1 = g.addVertex();
        final Vertex v2 = g.addVertex();
        final Edge e = v1.addEdge("test", v2);
        e.setProperty("abc", 123);

        final Iterable<Edge> iterable = g.E().toList();
        final String results = SERIALIZER.serializeResponseAsString(ResponseMessage.create(msg).result(iterable).build());

        final JSONObject json = new JSONObject(results);

        assertNotNull(json);
        assertEquals(msg.getRequestId().toString(), json.getString(SerTokens.TOKEN_REQUEST));
        final JSONArray converted = json.getJSONArray(SerTokens.TOKEN_RESULT);

        assertNotNull(converted);
        assertEquals(1, converted.length());

        final JSONObject edgeAsJson = converted.optJSONObject(0);
        assertNotNull(edgeAsJson);

        assertEquals(((Long) e.getId()).intValue(), edgeAsJson.get(GraphSONTokens.ID));  // lossy
        assertEquals(((Long) v1.getId()).intValue(), edgeAsJson.get(GraphSONTokens.OUT));// lossy
        assertEquals(((Long) v2.getId()).intValue(), edgeAsJson.get(GraphSONTokens.IN)); // lossy
        assertEquals(e.getLabel(), edgeAsJson.get(GraphSONTokens.LABEL));
        assertEquals(GraphSONTokens.EDGE, edgeAsJson.get(GraphSONTokens.TYPE));

        final JSONObject properties = edgeAsJson.optJSONObject(GraphSONTokens.PROPERTIES);
        assertNotNull(properties);
        assertEquals(123, properties.getInt("abc"));

    }

    @Test
    public void serializeToJsonIteratorWithEmbeddedMap() throws Exception {
        final Graph g = TinkerGraph.open();
        final Vertex v = g.addVertex();
        final Map<String, Object> map = new HashMap<>();
        map.put("x", 500);
        map.put("y", "some");

        final ArrayList<Object> friends = new ArrayList<>();
        friends.add("x");
        friends.add(5);
        friends.add(map);

        v.setProperty("friends", friends);

        final Iterable iterable = g.V().toList();
        final String results = SERIALIZER.serializeResponseAsString(ResponseMessage.create(msg).result(iterable).build());
        final JSONObject json = new JSONObject(results);

        assertNotNull(json);
        assertEquals(msg.getRequestId().toString(), json.getString(SerTokens.TOKEN_REQUEST));
        final JSONArray converted = json.getJSONArray(SerTokens.TOKEN_RESULT);

        assertNotNull(converted);
        assertEquals(1, converted.length());

        final JSONObject vertexAsJson = converted.optJSONObject(0);
        assertNotNull(vertexAsJson);

        final JSONObject properties = vertexAsJson.optJSONObject(GraphSONTokens.PROPERTIES);
        assertNotNull(properties);

        final JSONArray friendsProperty = properties.optJSONArray("friends");
        assertNotNull(friendsProperty);
        assertEquals(3, friends.size());

        final String object1 = friendsProperty.getString(0);
        assertEquals("x", object1);

        final int object2 = friendsProperty.getInt(1);
        assertEquals(5, object2);

        final JSONObject object3 = friendsProperty.getJSONObject(2);
        assertEquals(500, object3.getInt("x"));
        assertEquals("some", object3.getString("y"));
    }

    @Test
    public void serializeToJsonMapWithElementForKey() throws Exception {
        final TinkerGraph g = TinkerFactory.createClassic();
        final Map<Vertex, Integer> map = new HashMap<>();
        map.put(g.V().<Vertex>has("name", Compare.EQUAL, "marko").next(), 1000);

        final String results = SERIALIZER.serializeResponseAsString(ResponseMessage.create(msg).result(map).build());
        final JSONObject json = new JSONObject(results);

        assertNotNull(json);
        assertEquals(msg.getRequestId().toString(), json.getString(SerTokens.TOKEN_REQUEST));
        final JSONObject converted = json.getJSONObject(SerTokens.TOKEN_RESULT);

        assertNotNull(converted);

        // with no embedded types the key (which is a vertex) simply serializes out to an id
        // {"result":{"1":1000},"code":200,"requestId":"2d62161b-9544-4f39-af44-62ec49f9a595","type":0}
        assertEquals(1000, converted.optInt("1"));
    }

    @Test
    public void deserializeRequestNicelyWithNoArgs() throws Exception {
        final UUID request = UUID.fromString("011CFEE9-F640-4844-AC93-034448AC0E80");
        final RequestMessage m = SERIALIZER.deserializeRequest(String.format("{\"requestId\":\"%s\",\"op\":\"eval\"}", request));
        assertEquals(request, m.getRequestId());
        assertEquals("eval", m.getOp());
        assertNotNull(m.getArgs());
        assertEquals(0, m.getArgs().size());
    }

    @Test
    public void deserializeRequestNicelyWithArgs() throws Exception {
        final UUID request = UUID.fromString("011CFEE9-F640-4844-AC93-034448AC0E80");
        final RequestMessage m = SERIALIZER.deserializeRequest(String.format("{\"requestId\":\"%s\",\"op\":\"eval\",\"args\":{\"x\":\"y\"}}", request));
        assertEquals(request, m.getRequestId());
        assertEquals("eval", m.getOp());
        assertNotNull(m.getArgs());
        assertEquals("y", m.getArgs().get("x"));
    }

    @Test(expected = SerializationException.class)
    public void deserializeRequestParseMessage() throws Exception{
        SERIALIZER.deserializeRequest("{\"requestId\":\"%s\",\"op\":\"eval\",\"args\":{\"x\":\"y\"}}");
    }

    private class FunObject {
        private String val;

        public FunObject(String val) {
            this.val = val;
        }

        public String toString() {
            return this.val;
        }
    }
}
