package br.jus.trf2.assijus;

import java.util.Date;
import java.util.List;

import com.crivano.swaggerservlet.ISwaggerMethod;
import com.crivano.swaggerservlet.ISwaggerModel;
import com.crivano.swaggerservlet.ISwaggerRequest;
import com.crivano.swaggerservlet.ISwaggerResponse;

interface IAssijus {
	class Document implements ISwaggerModel {
		String system;
		String id;
		String code;
		String descr;
		String kind;
		String origin;
		String extra;
	}

	class ListStatus implements ISwaggerModel {
		String system;
		String errormsg;
		String stacktrace;
	}

	class Warning implements ISwaggerModel {
		String label;
		String description;
	}

	class Policy implements ISwaggerModel {
	}

	class PolicyVersion implements ISwaggerModel {
	}

	class PolicyOID implements ISwaggerModel {
	}

	class CN implements ISwaggerModel {
	}

	class CPF implements ISwaggerModel {
	}

	class AuthKey implements ISwaggerModel {
	}

	class AuthKind implements ISwaggerModel {
	}

	class Token implements ISwaggerModel {
	}

	class Name implements ISwaggerModel {
	}

	class Certificate implements ISwaggerModel {
	}

	class Time implements ISwaggerModel {
	}

	class Hash implements ISwaggerModel {
	}

	class Sha1 implements ISwaggerModel {
	}

	class Sha256 implements ISwaggerModel {
	}

	class Extra implements ISwaggerModel {
	}

	class error implements ISwaggerModel {
		String error;
	}

	class TokenPostRequest implements ISwaggerRequest {
	}

	class TokenPostResponse implements ISwaggerResponse {
		String token;
		String policy;
	}

	interface ITokenPost extends ISwaggerMethod {
		void run(TokenPostRequest req, TokenPostResponse resp) throws Exception;
	}

	class AuthPostRequest implements ISwaggerRequest {
		String authkey;
		String token;
	}

	class AuthPostResponse implements ISwaggerResponse {
		String authkey;
		String cpf;
		String name;
		String token;
		String kind;
		byte[] certificate;
		String cn;
	}

	interface IAuthPost extends ISwaggerMethod {
		void run(AuthPostRequest req, AuthPostResponse resp) throws Exception;
	}

	class ListPostRequest implements ISwaggerRequest {
		String key;
		String authkey;
		byte[] certificate;
	}

	class ListPostResponse implements ISwaggerResponse {
		List<Document> list;
		List<ListStatus> status;
	}

	interface IListPost extends ISwaggerMethod {
		void run(ListPostRequest req, ListPostResponse resp) throws Exception;
	}

	class ViewPostRequest implements ISwaggerRequest {
		String authkey;
		String id;
		String system;
	}

	class ViewPostResponse implements ISwaggerResponse {
		byte[] payload;
		String contenttype;
	}

	interface IViewPost extends ISwaggerMethod {
		void run(ViewPostRequest req, ViewPostResponse resp) throws Exception;
	}

	class HashPostRequest implements ISwaggerRequest {
		String authkey;
		byte[] certificate;
		String id;
		String system;
	}

	class HashPostResponse implements ISwaggerResponse {
		String policy;
		String policyversion;
		Date time;
		byte[] hash;
		String extra;
		byte[] sha1;
		byte[] sha256;
	}

	interface IHashPost extends ISwaggerMethod {
		void run(HashPostRequest req, HashPostResponse resp) throws Exception;
	}

	class SavePostRequest implements ISwaggerRequest {
		byte[] certificate;
		String system;
		String id;
		String code;
		String policy;
		String policyversion;
		byte[] signature;
		byte[] sha1;
		byte[] sha256;
		Date time;
		String extra;
	}

	class SavePostResponse implements ISwaggerResponse {
		String status;
		List<Warning> warning;
	}

	interface ISavePost extends ISwaggerMethod {
		void run(SavePostRequest req, SavePostResponse resp) throws Exception;
	}

	class ValidatePostRequest implements ISwaggerRequest {
		byte[] envelope;
		byte[] sha1;
		byte[] sha256;
		Date time;
	}

	class ValidatePostResponse implements ISwaggerResponse {
		String policy;
		String policyversion;
		String policyoid;
		String cn;
		String cpf;
		String status;
	}

	interface IValidatePost extends ISwaggerMethod {
		void run(ValidatePostRequest req, ValidatePostResponse resp)
				throws Exception;
	}

	class StorePostRequest implements ISwaggerRequest {
		String payload;
	}

	class StorePostResponse implements ISwaggerResponse {
		String status;
		String key;
	}

	interface IStorePost extends ISwaggerMethod {
		void run(StorePostRequest req, StorePostResponse resp) throws Exception;
	}

}
