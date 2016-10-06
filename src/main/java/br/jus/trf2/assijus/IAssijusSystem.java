package br.jus.trf2.assijus;

import java.util.Date;
import java.util.List;

import com.crivano.swaggerservlet.ISwaggerMethod;
import com.crivano.swaggerservlet.ISwaggerModel;
import com.crivano.swaggerservlet.ISwaggerRequest;
import com.crivano.swaggerservlet.ISwaggerResponse;

interface IAssijusSystem {
	class Document implements ISwaggerModel {
		String id;
		String code;
		String descr;
		String kind;
		String origin;
		String urlView;
		String urlHash;
		String urlSave;
	}

	class Warning implements ISwaggerModel {
		String label;
		String description;
	}

	class Error implements ISwaggerModel {
		String error;
	}

	class DocListGetRequest implements ISwaggerRequest {
		String cpf;
		String urlapi;
	}

	class DocListGetResponse implements ISwaggerResponse {
		List<Document> list;
	}

	interface IDocListGet extends ISwaggerMethod {
		void run(DocListGetRequest req, DocListGetResponse resp)
				throws Exception;
	}

	class DocIdPdfGetRequest implements ISwaggerRequest {
		String id;
		String cpf;
	}

	class DocIdPdfGetResponse implements ISwaggerResponse {
		byte[] doc;
	}

	interface IDocIdPdfGet extends ISwaggerMethod {
		void run(DocIdPdfGetRequest req, DocIdPdfGetResponse resp)
				throws Exception;
	}

	class DocIdHashGetRequest implements ISwaggerRequest {
		String id;
		String cpf;
	}

	class DocIdHashGetResponse implements ISwaggerResponse {
		byte[] sha1;
		byte[] sha256;
		String policy;
		String extra;
		byte[] doc;
	}

	interface IDocIdHashGet extends ISwaggerMethod {
		void run(DocIdHashGetRequest req, DocIdHashGetResponse resp)
				throws Exception;
	}

	class DocIdSignPutRequest implements ISwaggerRequest {
		String id;
		String cpf;
		String name;
		Date time;
		byte[] sha1;
		String extra;
		byte[] envelope;
	}

	class DocIdSignPutResponse implements ISwaggerResponse {
		String status;
		List<Warning> warning;
	}

	interface IDocIdSignPut extends ISwaggerMethod {
		void run(DocIdSignPutRequest req, DocIdSignPutResponse resp)
				throws Exception;
	}

}
