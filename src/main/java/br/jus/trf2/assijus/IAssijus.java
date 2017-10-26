package br.jus.trf2.assijus;

import java.io.InputStream;
import java.util.Date;
import java.util.List;

import com.crivano.swaggerservlet.ISwaggerMethod;
import com.crivano.swaggerservlet.ISwaggerModel;
import com.crivano.swaggerservlet.ISwaggerRequest;
import com.crivano.swaggerservlet.ISwaggerResponse;
import com.crivano.swaggerservlet.ISwaggerResponseFile;

public interface IAssijus {
	public class Document implements ISwaggerModel {
		public String system;
		public String id;
		public String secret;
		public String code;
		public String descr;
		public String kind;
		public String origin;
		public String extra;
	}

	public class ListStatus implements ISwaggerModel {
		public String system;
		public String errormsg;
		public String stacktrace;
	}

	public class Warning implements ISwaggerModel {
		public String label;
		public String description;
	}

	public class Policy implements ISwaggerModel {
	}

	public class PolicyVersion implements ISwaggerModel {
	}

	public class PolicyOID implements ISwaggerModel {
	}

	public class CN implements ISwaggerModel {
	}

	public class CPF implements ISwaggerModel {
	}

	public class AuthKey implements ISwaggerModel {
	}

	public class AuthKind implements ISwaggerModel {
	}

	public class Token implements ISwaggerModel {
	}

	public class Name implements ISwaggerModel {
	}

	public class Certificate implements ISwaggerModel {
	}

	public class Time implements ISwaggerModel {
	}

	public class Hash implements ISwaggerModel {
	}

	public class Envelope implements ISwaggerModel {
	}

	public class Sha1 implements ISwaggerModel {
	}

	public class Sha256 implements ISwaggerModel {
	}

	public class Extra implements ISwaggerModel {
	}

	public class Signature implements ISwaggerModel {
		public String ref;
		public String signer;
		public String kind;
	}

	public class Movement implements ISwaggerModel {
		public Date time;
		public String department;
		public String kind;
	}

	public class error implements ISwaggerModel {
		public String error;
	}

	public class TokenPostRequest implements ISwaggerRequest {
	}

	public class TokenPostResponse implements ISwaggerResponse {
		public String token;
		public String policy;
	}

	public interface ITokenPost extends ISwaggerMethod {
		public void run(TokenPostRequest req, TokenPostResponse resp) throws Exception;
	}

	public class AuthPostRequest implements ISwaggerRequest {
		public String authkey;
		public String token;
	}

	public class AuthPostResponse implements ISwaggerResponse {
		public String authkey;
		public String cpf;
		public String name;
		public String token;
		public String kind;
		public String cn;
	}

	public interface IAuthPost extends ISwaggerMethod {
		public void run(AuthPostRequest req, AuthPostResponse resp) throws Exception;
	}

	public class LoginPostRequest implements ISwaggerRequest {
		public String authkey;
		public String callback;
	}

	public class LoginPostResponse implements ISwaggerResponse {
		public String url;
	}

	public interface ILoginPost extends ISwaggerMethod {
		public void run(LoginPostRequest req, LoginPostResponse resp) throws Exception;
	}

	public class ListPostRequest implements ISwaggerRequest {
		public String key;
		public String authkey;
		public byte[] certificate;
	}

	public class ListPostResponse implements ISwaggerResponse {
		public List<Document> list;
		public List<ListStatus> status;
	}

	public interface IListPost extends ISwaggerMethod {
		public void run(ListPostRequest req, ListPostResponse resp) throws Exception;
	}

	public class ViewPostRequest implements ISwaggerRequest {
		public String authkey;
		public String system;
		public String id;
		public String secret;
		public String cpf;
	}

	public class ViewPostResponse implements ISwaggerResponse, ISwaggerResponseFile {
		public String contenttype = "application/pdf";
		public String contentdisposition = "attachment";

		public Long contentlength;
		public InputStream inputstream;

		public String getContenttype() {
			return contenttype;
		}

		public void setContenttype(String contenttype) {
			this.contenttype = contenttype;
		}

		public String getContentdisposition() {
			return contentdisposition;
		}

		public void setContentdisposition(String contentdisposition) {
			this.contentdisposition = contentdisposition;
		}

		public Long getContentlength() {
			return contentlength;
		}

		public void setContentlength(Long contentlength) {
			this.contentlength = contentlength;
		}

		public InputStream getInputstream() {
			return inputstream;
		}

		public void setInputstream(InputStream inputstream) {
			this.inputstream = inputstream;
		}
	}

	public interface IViewPost extends ISwaggerMethod {
		public void run(ViewPostRequest req, ViewPostResponse resp) throws Exception;
	}

	public class ViewSystemIdSecretGetRequest implements ISwaggerRequest {
		public String system;
		public String id;
		public String secret;
	}

	public class ViewSystemIdSecretGetResponse implements ISwaggerResponse, ISwaggerResponseFile {
		public String contenttype = "application/pdf";
		public String contentdisposition = "attachment";

		public Long contentlength;
		public InputStream inputstream;

		public String getContenttype() {
			return contenttype;
		}

		public void setContenttype(String contenttype) {
			this.contenttype = contenttype;
		}

		public String getContentdisposition() {
			return contentdisposition;
		}

		public void setContentdisposition(String contentdisposition) {
			this.contentdisposition = contentdisposition;
		}

		public Long getContentlength() {
			return contentlength;
		}

		public void setContentlength(Long contentlength) {
			this.contentlength = contentlength;
		}

		public InputStream getInputstream() {
			return inputstream;
		}

		public void setInputstream(InputStream inputstream) {
			this.inputstream = inputstream;
		}
	}

	public interface IViewSystemIdSecretGet extends ISwaggerMethod {
		public void run(ViewSystemIdSecretGetRequest req, ViewSystemIdSecretGetResponse resp) throws Exception;
	}

	public class InfoSystemIdSecretGetRequest implements ISwaggerRequest {
		public String system;
		public String id;
		public String secret;
	}

	public class InfoSystemIdSecretGetResponse implements ISwaggerResponse {
		public String status;
		public List<Signature> signature;
		public List<Movement> movement;
	}

	public interface IInfoSystemIdSecretGet extends ISwaggerMethod {
		public void run(InfoSystemIdSecretGetRequest req, InfoSystemIdSecretGetResponse resp) throws Exception;
	}

	public class HashPostRequest implements ISwaggerRequest {
		public String authkey;
		public byte[] certificate;
		public String system;
		public String id;
		public String secret;
	}

	public class HashPostResponse implements ISwaggerResponse {
		public String policy;
		public String policyversion;
		public Date time;
		public byte[] hash;
		public String extra;
		public byte[] sha1;
		public byte[] sha256;
	}

	public interface IHashPost extends ISwaggerMethod {
		public void run(HashPostRequest req, HashPostResponse resp) throws Exception;
	}

	public class SignedAttrsPostRequest implements ISwaggerRequest {
		public String authkey;
		public byte[] certificate;
		public String policy;
		public byte[] sha1;
		public byte[] sha256;
	}

	public class SignedAttrsPostResponse implements ISwaggerResponse {
		public String policy;
		public String policyversion;
		public Date time;
		public byte[] hash;
		public byte[] sha1;
		public byte[] sha256;
	}

	public interface ISignedAttrsPost extends ISwaggerMethod {
		public void run(SignedAttrsPostRequest req, SignedAttrsPostResponse resp) throws Exception;
	}

	public class EnvelopePostRequest implements ISwaggerRequest {
		public byte[] certificate;
		public String policy;
		public String policyversion;
		public byte[] signature;
		public byte[] sha1;
		public byte[] sha256;
		public Date time;
	}

	public class EnvelopePostResponse implements ISwaggerResponse {
		public byte[] envelope;
		public String policy;
		public String policyversion;
		public Date time;
		public byte[] sha1;
		public byte[] sha256;
		public String cpf;
		public String name;
	}

	public interface IEnvelopePost extends ISwaggerMethod {
		public void run(EnvelopePostRequest req, EnvelopePostResponse resp) throws Exception;
	}

	public class SavePostRequest implements ISwaggerRequest {
		public byte[] certificate;
		public String system;
		public String id;
		public String code;
		public String policy;
		public String policyversion;
		public byte[] signature;
		public byte[] sha1;
		public byte[] sha256;
		public Date time;
		public String extra;
	}

	public class SavePostResponse implements ISwaggerResponse {
		public String status;
		public List<Warning> warning;
	}

	public interface ISavePost extends ISwaggerMethod {
		public void run(SavePostRequest req, SavePostResponse resp) throws Exception;
	}

	public class ValidatePostRequest implements ISwaggerRequest {
		public byte[] envelope;
		public byte[] sha1;
		public byte[] sha256;
		public Date time;
	}

	public class ValidatePostResponse implements ISwaggerResponse {
		public String policy;
		public String policyversion;
		public String policyoid;
		public String cn;
		public String cpf;
		public String status;
	}

	public interface IValidatePost extends ISwaggerMethod {
		public void run(ValidatePostRequest req, ValidatePostResponse resp) throws Exception;
	}

	public class VerifyPostRequest implements ISwaggerRequest {
		public String system;
		public String id;
		public String ref;
	}

	public class VerifyPostResponse implements ISwaggerResponse {
		public String policy;
		public String policyversion;
		public String policyoid;
		public String cn;
		public String cpf;
		public String status;
	}

	public interface IVerifyPost extends ISwaggerMethod {
		public void run(VerifyPostRequest req, VerifyPostResponse resp) throws Exception;
	}

	public class StorePostRequest implements ISwaggerRequest {
		public String payload;
	}

	public class StorePostResponse implements ISwaggerResponse {
		public String status;
		public String key;
	}

	public interface IStorePost extends ISwaggerMethod {
		public void run(StorePostRequest req, StorePostResponse resp) throws Exception;
	}

}