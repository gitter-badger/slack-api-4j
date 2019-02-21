package au.com.addstar.slackapi.internal;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HttpsURLConnection;

import au.com.addstar.slackapi.exceptions.SlackAuthException;
import au.com.addstar.slackapi.exceptions.SlackException;
import au.com.addstar.slackapi.exceptions.SlackRequestLimitException;
import au.com.addstar.slackapi.exceptions.SlackRestrictedException;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

@SuppressWarnings("WeakerAccess")
public class SlackConnection
{
    private final String token;
    private boolean isRateLimited;
    private long retryEnd;
    
    public SlackConnection(final String token)
    {
        this.token = token;
        
        this.isRateLimited = false;
        this.retryEnd = 0;
    }
    
    public JsonObject callMethodHandled(final String method) throws SlackException, IOException
    {
        return this.callMethodHandled(method, Utilities.EMPTY_MAP);
    }
    
    public JsonObject callMethodHandled(final String method, final Map<String, Object> params) throws SlackException, IOException
    {
        final JsonObject base = this.callMethod(method, params).getAsJsonObject();
        final boolean ok = base.get("ok").getAsBoolean();
        
        if (!ok)
        {
            final String code = base.get("error").getAsString();
            switch (code)
            {
            case "not_authed":
            case "invalid_auth":
            case "account_inactive":
                throw new SlackAuthException(code);
            
            case "restricted_action":
            case "user_is_bot":
            case "user_is_restricted":
                throw new SlackRestrictedException(code);
            
            default:
                throw new SlackException(code);
            }
        }
        else {
            return base;
        }
    }
    
    public JsonElement callMethod(final String method, final Map<String, Object> params) throws IOException {
        if (this.isRateLimited)
        {
            if (System.currentTimeMillis() < this.retryEnd) {
                throw new SlackRequestLimitException(this.retryEnd);
            }
            
            this.isRateLimited = false;
        }
        
        final HttpsURLConnection connection = this.createConnection(method, params);
        connection.connect();
        
        if (connection.getResponseCode() == 429) // Too many requests
        {
            final int delay = connection.getHeaderFieldInt("Retry-After", 2);
            this.isRateLimited = true;
            this.retryEnd = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(delay);
            throw new SlackRequestLimitException(this.retryEnd);
        }
        
        final JsonReader reader = new JsonReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
        
        final JsonParser parser = new JsonParser();
        final JsonElement result = parser.parse(reader);
        
        reader.close();
        
        return result;
    }
    
    private HttpsURLConnection createConnection(final String method, final Map<String, Object> params) throws IOException {
        try
        {
            final URL queryUrl = new URL("https", SlackConstants.HOST_URL, "/api/" + method);
            
            final HttpsURLConnection connection = (HttpsURLConnection)queryUrl.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setDoInput(true);
            connection.setDoOutput(true);
            
            // Add request params
            final String request = this.encodeRequest(params);
            final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream(), StandardCharsets.UTF_8));
            writer.write(request);
            writer.close();
            
            return connection;
        }
        catch ( final ProtocolException e )
        {
            // Should not happen
            throw new AssertionError();
        }
    }
    
    private String encodeRequest(final Map<String, Object> params)
    {
        try
        {
            final StringBuilder data = new StringBuilder();
            data.append("token=");
            data.append(URLEncoder.encode(this.token, "UTF-8"));
            for (final Entry<String, Object> param : params.entrySet())
            {
                data.append('&');
                data.append(URLEncoder.encode(param.getKey(), "UTF-8"));
                data.append('=');
                data.append(URLEncoder.encode(String.valueOf(param.getValue()), "UTF-8"));
            }
            
            return data.toString();
        }
        catch (final UnsupportedEncodingException e)
        {
            // Should never happen
            throw new AssertionError();
        }
    }
}
