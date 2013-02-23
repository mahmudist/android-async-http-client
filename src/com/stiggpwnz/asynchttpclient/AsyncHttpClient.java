package com.stiggpwnz.asynchttpclient;

import java.util.Map;
import java.util.Map.Entry;

import org.apache.http.HttpEntity;
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

	private static AsyncHttpClient instance;

	public static void init() {
		instance = new AsyncHttpClient();
	}

	public static AsyncHttpClient getInstance() {
		return instance;
	}

	private final HttpClient client = createNewThreadSafeHttpClient();
	private final Handler handler = new Handler();

	public HttpPost post(String url, Map<String, String> params, ResponseHandler responseHandler) {
		if (TextUtils.isEmpty(url))
			return null;

		StringBuilder builder = appendParams(url, params);
		HttpPost post = new HttpPost(builder.toString());
		execute(responseHandler, post);
		return post;
	}

	public HttpGet get(String url, Map<String, String> params, ResponseHandler responseHandler) {
		if (TextUtils.isEmpty(url))
			return null;

		StringBuilder builder = appendParams(url, params);
		HttpGet get = new HttpGet(builder.toString());
		execute(responseHandler, get);
		return get;
	}

	private void execute(final ResponseHandler responseHandler, final HttpUriRequest request) {
		new Thread() {

			@Override
			public void run() {
				try {
					HttpResponse httpResponse = client.execute(request);
					if (responseHandler == null)
						return;

					HttpEntity entity = httpResponse.getEntity();
					final String response = EntityUtils.toString(entity);

					if (responseHandler instanceof JsonResponseHandler) {
						final JsonResponseHandler jsonResponseHandler = (JsonResponseHandler) responseHandler;
						handler.post(new Runnable() {

							@Override
							public void run() {
								try {
									jsonResponseHandler.onSuccess(new JSONObject(response));
								} catch (JSONException e) {
									jsonResponseHandler.onFailure(e);
								}
							}
						});
					} else if (responseHandler instanceof JsonArrayResponseHandler) {
						final JsonArrayResponseHandler jsonArrayResponseHandler = (JsonArrayResponseHandler) responseHandler;
						handler.post(new Runnable() {

							@Override
							public void run() {
								try {
									jsonArrayResponseHandler.onSuccess(new JSONArray(response));
								} catch (JSONException e) {
									jsonArrayResponseHandler.onFailure(e);
								}
							}
						});
					} else {
						handler.post(new Runnable() {

							@Override
							public void run() {
								responseHandler.onSuccess(response);
							}
						});
					}
				} catch (final Exception e) {
					handler.post(new Runnable() {

						@Override
						public void run() {
							responseHandler.onFailure(e);
						}
					});
				}
			};
		}.start();
	}

	private static StringBuilder appendParams(String url, Map<String, String> params) {
		StringBuilder builder = new StringBuilder(url);

		if (params == null || params.isEmpty())
			return builder;

		builder.append("?");
		for (Entry<String, String> entry : params.entrySet()) {
			builder.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
		}
		builder.deleteCharAt(builder.length() - 1);
		return builder;
	}

	public static interface ResponseHandler {
		public void onSuccess(String response);

		public void onFailure(Exception e);
	}

	public static interface JsonResponseHandler extends ResponseHandler {
		public void onSuccess(JSONObject jsonObject);

	}

	public static interface JsonArrayResponseHandler extends ResponseHandler {
		public void onSuccess(JSONArray jsonArray);
	}

	private static final int TIMEOUT_CONNECTION = 6000;
	private static final int TIMEOUT_SOCKET = 10000;

	private static HttpClient createNewThreadSafeHttpClient() {
		HttpClient defaultHttpClient = new DefaultHttpClient();
		HttpParams params = defaultHttpClient.getParams();
		HttpConnectionParams.setConnectionTimeout(params, TIMEOUT_CONNECTION);
		HttpConnectionParams.setSoTimeout(params, TIMEOUT_SOCKET);
		SchemeRegistry registry = defaultHttpClient.getConnectionManager().getSchemeRegistry();
		ClientConnectionManager manager = new ThreadSafeClientConnManager(params, registry);
		return new DefaultHttpClient(manager, params);
	}

}
