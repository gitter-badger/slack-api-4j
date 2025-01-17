package io.github.slackapi4j.internal;

/*-
 * #%L
 * slack-api-4j
 * %%
 * Copyright (C) 2018 - 2019 SlackApi4J
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */

import java.util.Collections;
import java.util.Map;

import io.github.slackapi4j.objects.blocks.composition.TextObject;
import com.google.gson.*;

public class Utilities
{
    /**
     * Parses the element as a unix timestamp
     * @param element The element to convert
     * @return The time in milliseconds
     */
    public static long getAsTimestamp(final JsonElement element)
    {
        final String raw = element.getAsString();

        final long time;
        if (raw.contains("."))
        {
            final double precTime = Double.parseDouble(raw);
            time = (long)(precTime * 1000);
        }
        else {
            time = Long.parseLong(raw) * 1000;
        }

        return time;
    }
    public static TextObject getTextObject(final JsonElement element, JsonDeserializationContext context, TextObject.TextType type){
        TextObject textObject = context.deserialize(element,TextObject.class);
        if(type == null){
            return textObject;
        }else{
            if(textObject.getType() != type){
                throw new JsonParseException("Invalid textType: " +textObject.getType().name());
            }else{
                return textObject;
            }
        }
    }

    public static void serializeTextObject(JsonObject root, String name, TextObject object, JsonSerializationContext context){
        if (object == null) {
            return;
        }
        root.add(name,context.serialize(object,TextObject.class));
    }

    public static String getAsString(final JsonElement element)
    {
        if (element == null || element instanceof JsonNull) {
            return null;
        }

        return element.getAsString();
    }

    public static boolean getAsBoolean(final JsonElement element, final boolean def)
    {
        if (element == null || element instanceof JsonNull) {
            return def;
        }

        return element.getAsBoolean();
    }

    public static int getAsInt(final JsonElement element)
    {
        if (element == null || element instanceof JsonNull) {
            return 0;
        }

        return element.getAsInt();
    }

    /**
     * So I dont have to force type Collections.emptyMap() for parameters
     */
    public static final Map<String, Object> EMPTY_MAP = Collections.emptyMap();
}
