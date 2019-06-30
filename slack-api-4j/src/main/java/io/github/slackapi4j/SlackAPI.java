package io.github.slackapi4j;

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

import java.io.IOException;
import java.util.List;
import java.util.Map;


import io.github.slackapi4j.objects.blocks.Block;
import io.github.slackapi4j.objects.blocks.composition.CompositionObject;
import io.github.slackapi4j.objects.blocks.elements.Element;
import com.google.common.collect.Maps;
import com.google.gson.*;

import io.github.slackapi4j.exceptions.SlackException;
import io.github.slackapi4j.internal.SlackConnection;
import io.github.slackapi4j.internal.SlackConstants;
import com.google.gson.reflect.TypeToken;
import io.github.slackapi4j.objects.*;

@SuppressWarnings("WeakerAccess")
public class SlackAPI
{
    private static boolean s_debug;
    private final SlackConnection connection;
    private final Gson gson;
    @SuppressWarnings("deprecation")
    private final ChannelManager channels;
    @SuppressWarnings("deprecation")
    private final GroupManager groups;
    private final ConversationsManager conversations;

    public SlackAPI(final String token, String url){
        if(url!=null)
            this.connection = new SlackConnection(token,url);
        else
            this.connection = new SlackConnection(token);
        final GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(NormalChannel.class, NormalChannel.getGsonAdapter());
        builder.registerTypeAdapter(GroupChannel.class, GroupChannel.getGsonAdapter());
        builder.registerTypeAdapter(DirectChannel.class, DirectChannel.getGsonAdapter());
        builder.registerTypeAdapter(Conversation.class,Conversation.getGsonAdapter());
        builder.registerTypeAdapter(User.class, User.getGsonAdapter());
        builder.registerTypeAdapter(Message.class, Message.getGsonAdapter());
        Attachment.addGsonAdapters(builder);
        Block.addGsonAdapters(builder);
        CompositionObject.addGsonAdapters(builder);
        Element.addGsonAdapters(builder);
        this.gson = builder.create();

        this.channels = new ChannelManager(this);
        this.groups = new GroupManager(this);
        this.conversations = new ConversationsManager(this);
    }

    @SuppressWarnings("deprecation")
    public SlackAPI(final String token)
    {
        this(token,null);
    }

    public static boolean isDebug() {
        return s_debug;
    }

    public static void setDebug(final boolean debug) {
        SlackAPI.s_debug = debug;
    }

    public ConversationsManager getConversations() {
        return this.conversations;
    }

    /**
     * @return ChannelManager
     * @deprecated use {@link this#getConversations}
     */
    @Deprecated
    public ChannelManager getChannelManager() {
        return this.channels;
    }

    /**
     * @return groupmanager
     * @deprecated use {@link this#getConversations}
     */
    @Deprecated
    public GroupManager getGroupManager() {
        return this.groups;
    }

    public RealTimeSession startRTSession() throws SlackException, IOException
    {
        final JsonObject root = this.connection.callMethodHandled(SlackConstants.RTM_START);
        if(SlackAPI.s_debug)System.out.println(root);
        return new RealTimeSession(root, this);
    }

    /**
     * Ideally encode the target conversation into the message.
     * @param message a Message
     * @param channel the target channel
     * @return the message as it arrived - it will now have a timestamp
     * @throws SlackException if there was a error with the api
     * @throws IOException encoding errors
     * @deprecated use {@link this#sendMessage(Message)}
     */
    @Deprecated
    public Message sendMessage(final String message, final IdBaseObject channel) throws SlackException, IOException {
        return this.sendMessage(message, channel, MessageOptions.DEFAULT);
    }

    /**
     * Sends a message
     * @param message a Message
     * @return the message as it arrived - it will now have a timestamp
     * @throws IOException encoding errors
     * @throws SlackException sending errors
     */
    public Message sendMessage(final Message message) throws IOException, SlackException {
        return this.sendMessage(message, MessageOptions.DEFAULT);
    }

    public Message sendMessage(final Message message, final MessageOptions options) throws IOException, SlackException {
        final JsonElement elem = this.gson.toJsonTree(message);
        final JsonObject obj = elem.getAsJsonObject();
        this.addDefaultOptions(obj,options);
        final JsonObject root = this.connection.callMethodHandled(SlackConstants.CHAT_POST, obj);
        return this.gson.fromJson(root.get("message"), Message.class);
    }

    private void addDefaultOptions(final JsonObject object, final MessageOptions options) {
        object.addProperty("as_user", options.isAsUser());
        object.addProperty("link_names", options.isLinkNames() ? 1 : 0);
        object.addProperty("unfurl_links", options.isUnfurlLinks());
        object.addProperty("unfurl_media", options.isUnfurlMedia());
        if (options.getIconEmoji() != null) {
            object.addProperty("icon_emoji", options.getIconEmoji());
        } else if (options.getIconUrl() != null) {
            object.addProperty("icon_url", options.getIconUrl().toExternalForm());
        }
        if (options.getMode() != null) {
            switch (options.getMode()) {
                case Full:
                    object.addProperty("parse", "full");
                    break;
                case None:
                    object.addProperty("parse", "none");
                    break;
                default:
                    break;
            }
        }
    }
    /**
     * Sends an ephemeral message.
     * @param message a Message
     * @return The message as send -  you can check the timestamp to see when it arrived
     * @throws IOException encoding errors
     * @throws SlackException sending errors
     */
    public Message sendEphemeral(final Message message) throws IOException, SlackException {
        return this.sendEphemeral(message, MessageOptions.DEFAULT);
    }

    public Message sendEphemeral(final Message message, final MessageOptions options) throws IOException, SlackException {
        final JsonElement obj = this.gson.toJsonTree(message);
        final JsonObject out = obj.getAsJsonObject();
        this.addDefaultOptions(out, options);
        final JsonObject root = this.connection.callMethodHandled(SlackConstants.CHAT_POSTEMPHEMERAL, out);
        return this.gson.fromJson(root.get("message"), Message.class);
    }
    /**
     * @deprecated use {@link #sendMessage(Message)}
     * @param message The string message
     * @param channel the channel to send it too
     * @param options a set of options to apply
     * @return a Message
     * @throws IOException encoding errors
     * @throws SlackException sending errors
     */
    @Deprecated
    public Message sendMessage(final String message, final IdBaseObject channel, final MessageOptions options) throws SlackException, IOException
    {
        final Map<String, Object> params = Maps.newHashMap();
        params.put("channel", channel.getId().toString());
        params.put("text", message);
        if (options.getUsername() != null) {
            params.put("username", options.getUsername());
        }
        params.put("as_user", options.isAsUser());
        params.put("link_names", options.isLinkNames() ? 1 : 0);
        params.put("unfurl_links", options.isUnfurlLinks());
        params.put("unfurl_media", options.isUnfurlMedia());
        if (options.getIconEmoji() != null) {
            params.put("icon_emoji", options.getIconEmoji());
        } else if (options.getIconUrl() != null) {
            params.put("icon_url", options.getIconUrl().toExternalForm());
        }
        if (options.getMode() != null)
        {
            switch (options.getMode())
            {
            case Full:
                params.put("parse", "full");
                break;
            case None:
                params.put("parse", "none");
                break;
            default:
                break;
            }
        }

        if (options.getAttachments() != null)
        {
            final JsonArray attachments = new JsonArray();
            for (final Attachment attachment : options.getAttachments()) {
                attachments.add(this.gson.toJsonTree(attachment));
            }
            params.put("attachments", attachments);
        }

        params.put("mrkdwn", options.isFormat());

        final JsonObject root = this.connection.callMethodHandled(SlackConstants.CHAT_POST, params);
        final Message out = this.gson.fromJson(root.get("message"), Message.class);
        out.setSubtype(Message.MessageType.Sent);
        return out;
    }

    @SuppressWarnings("unused")
    List<User> getUsers() throws SlackException, IOException {
        final JsonObject root = this.connection.callMethodHandled(SlackConstants.USER_LIST);
        final TypeToken<List<User>> token = new TypeToken<List<User>>() {
        };
        return this.gson.fromJson(root.get("members"), token.getType());
    }
    SlackConnection getSlack()
    {
        return this.connection;
    }

    Gson getGson()
    {
        return this.gson;
    }
}
