package io.github.slackapi4j.objects;

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

import java.lang.reflect.Type;
import java.util.List;

import io.github.slackapi4j.internal.Utilities;

import io.github.slackapi4j.objects.blocks.Block;
import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import lombok.*;

/**
 * An object that represents a message sent in a conversation
 * A builder is created for the object
 *
 */
@Builder
@Getter
@EqualsAndHashCode
@AllArgsConstructor
public class Message extends IdBaseObject
{
    /**
     * The user object
     */
    @Setter
    private ObjectID userId;
    /**
     * The text of  the message
     */
    @Setter
    private String text;
    /**
     * The conversion that represents the source or destination of the message. - generally a conversation (which could be private or public
     */
    private ObjectID conversationID;
    /**
     * When the message was created
     */
    private long timestamp;
    /**
     * When the message was posted to the thread/conversation
     */
    @Setter
    private String thread_ts;
    /**
     * String representation of the timestamp
     */
    private String ts;
    /**
     * The MessageType
     */
    @Setter
    private MessageType subtype;
    /**
     * The last user to edit the message.
     */
    private ObjectID editUserId;
    /**
     * Time stamp of the edits.
     */
    private long editTimestamp;
    /**
     * Message sent as a user.
     */
    private boolean as_user = false;
    /**
     * A list of attachments
     * @Deprecated use {@code Message#blocks}
     */
    @Deprecated
    @Setter
    private List<Attachment> attachments;
    /**
     * A list of blocks to send
     */
    @Setter
    private List<Block> blocks;

    public Message(){
        this.subtype = MessageType.Normal;
        this.as_user = true;
    }

    public Message(String text, IdBaseObject channel)
    {
        this.conversationID = channel.getId();
        this.text = text;
        this.subtype = MessageType.Sent;
        this.as_user = false;
    }

    public static Object getGsonAdapter()
    {
        return new MessageJsonAdapter();
    }
    public void addBlock(Block block){
        this.blocks.add(block);
    }
    @Override
    public String toString()
    {
        return String.format("%s: '%s' from %s", this.subtype, this.text, this.userId);
    }

    public enum MessageType {
        Normal(""),
        Sent(""),
        FromBot("bot_message"),
        FromMeCommand("me_message"),

        Edit("message_changed"),
        Delete("message_deleted"),

        ChannelJoin("channel_join"),
        ChannelLeave("channel_leave"),
        ChannelTopic("channel_topic"),
        ChannelPurpose("channel_purpose"),
        ChannelName("channel_name"),
        ChannelArchive("channel_archive"),
        ChannelUnarchive("channel_unarchive"),

        GroupJoin("group_join"),
        GroupLeave("group_leave"),
        GroupTopic("group_topic"),
        GroupPurpose("group_purpose"),
        GroupName("group_name"),
        GroupArchive("group_archive"),
        GroupUnarchive("group_unarchive"),

        FileShare("file_share"),
        FileComment("file_comment"),
        FileMention("file_mention");

        private final String id;

        private MessageType(String id) {
            this.id = id;
        }

        static MessageType fromId(String id) {
            if (id == null) {
                return Normal;
            }

            for (MessageType type : values()) {
                if (type.id.equals(id)) {
                    return type;
                }
            }

            return Normal;
        }
    }

    private static class MessageJsonAdapter implements JsonDeserializer<Message>, JsonSerializer<Message>
    {
        @Override
        @SuppressWarnings("deprecation")
        public Message deserialize( JsonElement element, Type typeOfT, JsonDeserializationContext context ) throws JsonParseException
        {
            if (!(element instanceof JsonObject)) {
                throw new JsonParseException("Expected JSONObject as message root");
            }

            JsonObject root = (JsonObject)element;

            Message message = new Message();
            if (root.has("user")) {
                message.userId = new ObjectID(root.get("user").getAsString());
            }

            message.text = Utilities.getAsString(root.get("text"));
            message.thread_ts = Utilities.getAsString(root.get("thread_ts"));
            message.ts = Utilities.getAsString(root.get("ts"));
            message.as_user = Utilities.getAsBoolean(root.get("as_user"),true);
            message.timestamp = Utilities.getAsTimestamp(root.get("ts"));
            if (root.has("channel")) {
                message.conversationID = new ObjectID(root.get("channel").getAsString());
            }

            if (root.has("edited"))
            {
                JsonObject edited = root.getAsJsonObject("edited");
                message.editUserId = new ObjectID(edited.get("user").getAsString());
                message.editTimestamp = Utilities.getAsTimestamp(edited.get("ts"));
            }

            message.subtype = MessageType.fromId(Utilities.getAsString(root.get("subtype")));

            if (root.has("attachments"))
            {
                message.attachments = Lists.newArrayList();
                JsonArray attachments = root.getAsJsonArray("attachments");
                for (JsonElement rawAttachment : attachments) {
                    message.attachments.add(context.<Attachment>deserialize(rawAttachment, Attachment.class));
                }
            }
            if (root.has("blocks"))
            {
                message.blocks = Lists.newArrayList();
                JsonArray blocks = root.getAsJsonArray("blocks");
                for (JsonElement rawBlock : blocks) {
                    message.blocks.add(context.<Block>deserialize(rawBlock, Block.class));
                }
            }
            return message;
        }

        @Override
        public JsonElement serialize( Message src, Type typeOfSrc, JsonSerializationContext context )
        {
            JsonObject object = new JsonObject();
            object.addProperty("type", "message");
            object.addProperty("channel", src.conversationID.toString());
            object.addProperty("text", src.text);
            object.addProperty("thread_ts",src.thread_ts);
            object.addProperty("as_user",src.as_user);
            if (src.userId != null) {
                object.addProperty("user", src.userId.toString());
            }
            if (src.attachments != null)
            {
                JsonArray attachments = new JsonArray();
                for (Attachment attachment : src.attachments) {
                    attachments.add(context.serialize(attachment));
                }
                object.add("attachments", attachments);
            }
            if(src.blocks != null){
                JsonArray blocks = new JsonArray();
                for(Block block:src.blocks){
                    blocks.add(context.serialize(block));
                }
                object.add("blocks",blocks);
            }
            return object;
        }
    }
}
