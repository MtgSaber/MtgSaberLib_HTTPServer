package net.mtgsaber.lib.httpserver.util;

import com.google.gson.*;

import java.lang.reflect.Type;

/**
 * Source code modified from:
 *      https://technology.finra.org/code/serialize-deserialize-interfaces-in-java.html
 *
 * @param <T> should be an interface.
 */
public class InterfaceAdapter<T> implements JsonSerializer<T>, JsonDeserializer<T> {

    private static final String CLASSNAME = "CLASSNAME";
    private static final String DATA = "DATA";

    @Override
    public T deserialize(
            JsonElement jsonElement, Type type,
            JsonDeserializationContext jsonDeserializationContext
    ) throws JsonParseException {

        JsonObject jsonObject = jsonElement.getAsJsonObject();
        JsonPrimitive prim = (JsonPrimitive) jsonObject.get(CLASSNAME);
        String className = prim.getAsString();
        Class<?> clazz = getObjectClass(className);
        return jsonDeserializationContext.deserialize(jsonObject.get(DATA), clazz);
    }

    @Override
    public JsonElement serialize(
            Object jsonElement, Type type,
            JsonSerializationContext jsonSerializationContext
    ) {
        JsonObject jsonObject = new JsonObject();
        Class<?> clazz = jsonElement.getClass();
        jsonObject.addProperty(CLASSNAME, clazz.getName());
        System.out.println(clazz.getName());
        // The original line in the web article produces stack overflow error. They forgot to specify class of data.
        jsonObject.add(DATA, jsonSerializationContext.serialize(jsonElement, clazz));
        return jsonObject;
    }

    /****** Helper method to get the className of the object to be deserialized *****/
    public Class<?> getObjectClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            //e.printStackTrace();
            throw new JsonParseException(e.getMessage());
        }
    }
}
