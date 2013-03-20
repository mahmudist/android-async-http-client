package com.stiggpwnz.asynchttpclient;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.Handler;
import android.text.TextUtils;

public class AsyncHttpClient {

	private static final int TIMEOUT_CONNECTION = 6000;
	private static final int TIMEOUT_SOCKET = 10000;

	private static AsyncHttpClient instance;

	public static void init(int threadPoolSize) {
		instance = new AsyncHttpClient(threadPoolSize);
	}

	public static AsyncHttpClient getInstance() {
		return instance;
	}

	private final HttpClient httpClient = createNewThreadSafeHttpClient();

	private final Handler uiThreadHandler = new Handler();
	private final ExecutorService executor;

	private AsyncHttpClient(int threadPoolSize) {
		executor = Executors.newFixedThreadPool(threadPoolSize);
	}

	public HttpPost post(String url, Map<String, String> params, boolean respondOnUiThread, AsyncResponseHandler responseHandler) {
		if (TextUtils.isEmpty(url))
			return null;

		StringBuilder builder = appendParams(url, params);
		HttpPost post = new HttpPost(builder.toString());
		execute(responseHandler, post, respondOnUiThread);
		return post;
	}

	public HttpGet get(String url, Map<String, String> params, boolean respondOnUiThread, AsyncResponseHandler responseHandler) {
		if (TextUtils.isEmpty(url))
			return null;

		StringBuilder builder = appendParams(url, params);
		HttpGet get = new HttpGet(builder.toString());
		execute(responseHandler, get, respondOnUiThread);
		return get;
	}

	private class RequestTask implements Runnable {

		private final AsyncResponseHandler responseHandler;
		private final HttpUriRequest request;
		private final boolean respondOnUiThread;

		public RequestTask(AsyncResponseHandler responseHandler, HttpUriRequest request, boolean respondOnUiThread) {
			this.responseHandler = responseHandler;
			this.request = request;
			this.respondOnUiThread = respondOnUiThread;
		}

		@Override
		public void run() {
			try {
				HttpResponse httpResponse = httpClient.execute(request);
				if (responseHandler == null)
					return;

				final String response = EntityUtils.toString(httpResponse.getEntity());

				if (responseHandler instanceof JsonResponseHandler) {
					final JsonResponseHandler jsonResponseHandler = (JsonResponseHandler) responseHandler;
					if (respondOnUiThread) {
						uiThreadHandler.post(new Runnable() {

							@Override
							public void run() {
								respondWithJson(response, jsonResponseHandler);
							}
						});
					} else {
						respondWithJson(response, jsonResponseHandler);
					}
				} else if (responseHandler instanceof JsonArrayResponseHandler) {
					final JsonArrayResponseHandler jsonArrayResponseHandler = (JsonArrayResponseHandler) responseHandler;
					if (respondOnUiThread) {
						uiThreadHandler.post(new Runnable() {

							@Override
							public void run() {
								respondWithJsonArray(response, jsonArrayResponseHandler);
							}
						});
					} else {
						respondWithJsonArray(response, jsonArrayResponseHandler);
					}
				} else if (responseHandler instanceof StringResponseHandler) {
					final StringResponseHandler stringResponseHandler = (StringResponseHandler) responseHandler;
					if (respondOnUiThread) {
						uiThreadHandler.post(new Runnable() {

							@Override
							public void run() {
								stringResponseHandler.onSuccess(response);
							}
						});
					} else {
						stringResponseHandler.onSuccess(response);
					}
				}
			} catch (final Exception e) {
				uiThreadHandler.post(new Runnable() {

					@Override
					public void run() {
						responseHandler.onFailure(e);
					}
				});
			}
		}

		private void respondWithJson(final String response, final JsonResponseHandler jsonResponseHandler) {
			try {
				jsonResponseHandler.onSuccess(new JSONObject(response));
			} catch (JSONException e) {
				jsonResponseHandler.onFailure(e);
			}
		}

		private void respondWithJsonArray(final String response, final JsonArrayResponseHandler jsonArrayResponseHandler) {
			try {
				jsonArrayResponseHandler.onSuccess(new JSONArray(response));
			} catch (JSONException e) {
				jsonArrayResponseHandler.onFailure(e);
			}
		}
	}

	private void execute(AsyncResponseHandler responseHandler, HttpUriRequest request, boolean respondOnUiThread) {
		RequestTask requestTask = new RequestTask(responseHandler, request, respondOnUiThread);
		executor.submit(requestTask);
	}

	private static StringBuilder appendParams(String url, Map<String, String> params) {
		StringBuilder builder = new StringBuilder(url);

		if (params == null || params.isEmpty())
			return builder;

		builder.append("?");
		for (Entry<String, String> entry : params.entrySet()) {
			builder.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
		}
		builder.setLength(builder.length() - 1);
		return builder;
	}

	public static interface AsyncResponseHandler {
		public void onFailure(Exception e);
	}

	public static interface JsonResponseHandler extends AsyncResponseHandler {
		public void onSuccess(JSONObject jsonObject);
	}

	public static interface JsonArrayResponseHandler extends AsyncResponseHandler {
		public void onSuccess(JSONArray jsonArray);
	}

	public static interface StringResponseHandler extends AsyncResponseHandler {
		public void onSuccess(String response);
	}

	private static HttpClient createNewThreadSafeHttpClient() {
		HttpClient defaultHttpClient = new DefaultHttpClient();
		HttpParams params = defaultHttpClient.getParams();
		HttpConnectionParams.setConnectionTimeout(params, TIMEOUT_CONNECTION);
		HttpConnectionParams.setSoTimeout(params, TIMEOUT_SOCKET);
		SchemeRegistry registry = defaultHttpClient.getConnectionManager().getSchemeRegistry();
		ClientConnectionManager manager = new ThreadSafeClientConnManager(params, registry);
		return new DefaultHttpClient(manager, params);
	}

	public HttpClient getHttpClient() {
		return httpClient;
	}

}
