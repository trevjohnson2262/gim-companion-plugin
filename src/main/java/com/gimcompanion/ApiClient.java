package com.gimcompanion;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.IOException;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Talks to the GIM Companion site. Requests run on OkHttp's own threads (never the game's),
 * and the answer is handed to a {@link Handler} on that thread.
 */
@Slf4j
@Singleton
public class ApiClient
{
	private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

	interface Handler
	{
		/** @param code HTTP status, or -1 if the site couldn't be reached */
		void done(int code, String body);
	}

	@Inject
	private OkHttpClient http;

	@Inject
	private Gson gson;

	void sendBatch(String server, String key, Object batch, Handler handler)
	{
		HttpUrl url = endpoint(server, "events");
		if (url == null)
		{
			handler.done(-1, "Bad server address");
			return;
		}
		RequestBody body = RequestBody.create(JSON, gson.toJson(batch));
		call(new Request.Builder().url(url).header("Authorization", "Bearer " + key).post(body).build(), handler);
	}

	void whoAmI(String server, String key, Handler handler)
	{
		HttpUrl url = endpoint(server, "me");
		if (url == null)
		{
			handler.done(-1, "Bad server address");
			return;
		}
		call(new Request.Builder().url(url).header("Authorization", "Bearer " + key).get().build(), handler);
	}

	/** The "error" message from a JSON error answer, or null. */
	String errorMessage(String body)
	{
		try
		{
			JsonObject json = gson.fromJson(body, JsonObject.class);
			return json != null && json.has("error") ? json.get("error").getAsString() : null;
		}
		catch (RuntimeException e)
		{
			return null;
		}
	}

	private static HttpUrl endpoint(String server, String path)
	{
		HttpUrl base = HttpUrl.parse(server.trim());
		if (base == null)
		{
			return null;
		}
		return base.newBuilder().addPathSegments("api/plugin/" + path).build();
	}

	private void call(Request request, Handler handler)
	{
		http.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.debug("GIM Companion request failed", e);
				handler.done(-1, e.getMessage());
			}

			@Override
			public void onResponse(Call call, Response response)
			{
				try (ResponseBody body = response.body())
				{
					handler.done(response.code(), body != null ? body.string() : "");
				}
				catch (IOException e)
				{
					handler.done(-1, e.getMessage());
				}
			}
		});
	}
}
