package io.github.slackapi4j.objects.blocks;

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

import io.github.slackapi4j.internal.Utilities;
import io.github.slackapi4j.objects.BaseObject;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.lang.reflect.Type;

/**
 * Created for use for the Add5tar MC Minecraft server
 * Created by benjamincharlton on 20/02/2019.
 */
@NoArgsConstructor
@Getter
public class Block extends BaseObject {
    @Setter
    private BlockType type;
    private String block_id;
    
    
    public static void addGsonAdapters(GsonBuilder builder)
    {
        builder.registerTypeAdapter(Section.class, getGsonAdapter());
        builder.registerTypeAdapter(ImageBlock.class,ImageBlock.getGsonAdapter());
        builder.registerTypeAdapter(Divider.class, getGsonAdapter());
        builder.registerTypeAdapter(ActionBlock.class, getGsonAdapter());
        builder.registerTypeAdapter(ContextBlock.class,ContextBlock.getGsonAdapter());


    }

    public static BlockJSONAdapter getGsonAdapter(){
        return new BlockJSONAdapter();
    }

    protected void load(JsonObject root, JsonDeserializationContext context) {
        this.type = BlockType.valueOf(root.get("type").getAsString().toUpperCase());
        this.block_id = Utilities.getAsString(root.get("block_id"));
    }

    protected JsonObject save(JsonObject root, JsonSerializationContext context) {
        root.addProperty("type", this.type.toString());
        if (this.block_id != null) {
            root.addProperty("block_id", this.block_id);
        }
        return root;
    }

    @AllArgsConstructor
    enum BlockType {
        SECTION("section"),
        DIVIDER("divider"),
        IMAGE("image"),
        ACTIONS("actions"),
        CONTEXT("context");

        private final String name;


        /**
         * Returns the name of this enum constant, as contained in the declaration.  This method may be overridden,
         * though it typically isn't necessary or desirable.  An enum type should override this method when a more
         * "programmer-friendly" string form exists.
         *
         * @return the name of this enum constant
         */
        @Override
        public String toString() {
            return this.name;
        }
    }

    private static class BlockJSONAdapter implements JsonDeserializer<Block>, JsonSerializer<Block>{

        @Override
        public Block deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
            if (!(json instanceof JsonObject)) {
                throw new JsonParseException("Expected JSONObject as channel root");
            }
            final JsonObject root = (JsonObject)json;
            final Block object;
            if(type.equals(Section.class)) {
                object = new Section();
            }  else  if(type.equals(Divider.class)) {
                object = new Divider();
            } else  if(type.equals(ImageBlock.class)) {
                object = new ImageBlock();
            }  else  if(type.equals(ActionBlock.class)) {
                object = new ImageBlock();
            }  else  if(type.equals(ContextBlock.class)) {
                object = new ContextBlock();
            }
            else {
                throw new JsonParseException("Cant load unknown channel type");
            }
            object.load(root,context);
            return object;
        }

        @Override
        public JsonElement serialize(Block block, Type type, JsonSerializationContext context) {
            JsonObject root = new JsonObject();
            block.save(root,context);
            return root;
        }
    }


}
