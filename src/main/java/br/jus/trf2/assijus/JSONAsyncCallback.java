package br.jus.trf2.assijus;

import org.json.JSONObject;

public interface JSONAsyncCallback {
	void completed(JSONObject obj) throws Exception;

	void failed(Exception e) throws Exception;

	void cancelled() throws Exception;
}
