package br.jus.trf2.assijus;

import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;

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

	public class SupportDocument implements ISwaggerModel {
		public String system;
		public String id;
		public String code;
		public String kind;
		public String origin;
	}

	public class ListStatus implements ISwaggerModel {
		public String system;
		public String errormsg;
		public String stacktrace;
		public Double miliseconds;
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

	public interface ITokenPost extends ISwaggerMethod {
		public static class Request implements ISwaggerRequest {
			public byte[] certificate;
			public Boolean digest;
		}

		public static class Response implements ISwaggerResponse {
			public String token;
			public String policy;
			public String policyversion;
			public Date time;
			public byte[] hash;
		}

		public void run(Request req, Response resp, AssijusContext ctx) throws Exception;
	}

	public interface IAuthPost extends ISwaggerMethod {
		public static class Request implements ISwaggerRequest {
			public String authkey;
			public String token;
			public byte[] certificate;
			public String policy;
			public String policyversion;
			public byte[] signature;
			public Date time;
		}

		public static class Response implements ISwaggerResponse {
			public String authkey;
			public String cpf;
			public String name;
			public String token;
			public String kind;
			public String cn;
		}
		public void run(Request req, Response resp, AssijusContext ctx) throws Exception;
	}

	public interface ILoginPost extends ISwaggerMethod {
		public static class Request implements ISwaggerRequest {
			public String authkey;
			public String callback;
		}

		public static class Response implements ISwaggerResponse {
			public String url;
		}
		
		public void run(Request req, Response resp, AssijusContext ctx) throws Exception;
	}
	
	public interface IListPost extends ISwaggerMethod {
		public static class Request implements ISwaggerRequest {
			public String key;
			public String authkey;
			public byte[] certificate;
		}

		public static class Response implements ISwaggerResponse {
			public List<Document> list;
			public List<ListStatus> status;
		}

		public void run(Request req, Response resp, AssijusContext ctx) throws Exception;
	}

	
	public interface ISupportListCpfGet extends ISwaggerMethod {
		public static class Request implements ISwaggerRequest {
			public String cpf;
		}

		public static class Response implements ISwaggerResponse {
			public List<SupportDocument> list;
			public List<ListStatus> status;
		}

		public void run(Request req, Response resp, AssijusContext ctx) throws Exception;
	}

	
	public interface IViewSystemIdSecretAuthkeyGet extends ISwaggerMethod {
		public static class Request implements ISwaggerRequest {
			public String authkey;
			public String system;
			public String id;
			public String secret;
			public String cpf;
		}

		public static class Response implements ISwaggerResponse, ISwaggerResponseFile {
			public String contenttype = "application/pdf";
			public String contentdisposition = "attachment";
			public Long contentlength;
			public InputStream inputstream;
			public Map<String, List<String>> headerFields;

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
			public Map<String, List<String>> getHeaderFields() {
				return headerFields;
			}
			public void setHeaderFields(Map<String, List<String>> headerFields) {
				this.headerFields = headerFields;
			}
		}

		public void run(Request req, Response resp, AssijusContext ctx) throws Exception;
	}

	
	public interface IInfoSystemIdSecretGet extends ISwaggerMethod {
		public static class Request implements ISwaggerRequest {
			public String system;
			public String id;
			public String secret;
		}

		public static class Response implements ISwaggerResponse {
			public String status;
			public List<Signature> signature;
			public List<Movement> movement;
		}

		public void run(Request req, Response resp, AssijusContext ctx) throws Exception;
	}

	
	public interface IHashPost extends ISwaggerMethod {
		public static class Request implements ISwaggerRequest {
			public String authkey;
			public byte[] certificate;
			public String system;
			public String id;
			public String secret;
			public Boolean digest;
		}

		public static class Response implements ISwaggerResponse {
			public String policy;
			public String policyversion;
			public Date time;
			public byte[] hash;
			public String extra;
			public byte[] sha1;
			public byte[] sha256;
		}

		public void run(Request req, Response resp, AssijusContext ctx) throws Exception;
	}

	
	public interface ISignedAttrsPost extends ISwaggerMethod {
		public static class Request implements ISwaggerRequest {
			public String authkey;
			public byte[] certificate;
			public String policy;
			public byte[] sha1;
			public byte[] sha256;
		}

		public static class Response implements ISwaggerResponse {
			public String policy;
			public String policyversion;
			public Date time;
			public byte[] hash;
			public byte[] sha1;
			public byte[] sha256;
		}

		public void run(Request req, Response resp, AssijusContext ctx) throws Exception;
	}

	
	public interface IEnvelopePost extends ISwaggerMethod {
		public static class Request implements ISwaggerRequest {
			public byte[] certificate;
			public String policy;
			public String policyversion;
			public byte[] signature;
			public byte[] sha1;
			public byte[] sha256;
			public Date time;
		}

		public static class Response implements ISwaggerResponse {
			public byte[] envelope;
			public String policy;
			public String policyversion;
			public Date time;
			public byte[] sha1;
			public byte[] sha256;
			public String cpf;
			public String name;
		}

		public void run(Request req, Response resp, AssijusContext ctx) throws Exception;
	}

	
	public interface ISavePost extends ISwaggerMethod {
		public static class Request implements ISwaggerRequest {
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

		public static class Response implements ISwaggerResponse {
			public String status;
			public List<Warning> warning;
		}

		public void run(Request req, Response resp, AssijusContext ctx) throws Exception;
	}

	
	public interface IValidatePost extends ISwaggerMethod {
		public static class Request implements ISwaggerRequest {
			public byte[] envelope;
			public byte[] sha1;
			public byte[] sha256;
			public Date time;
		}

		public static class Response implements ISwaggerResponse {
			public String policy;
			public String policyversion;
			public String policyoid;
			public String cn;
			public String cpf;
			public String status;
		}

		public void run(Request req, Response resp, AssijusContext ctx) throws Exception;
	}

	
	public interface IVerifyPost extends ISwaggerMethod {
		public static class Request implements ISwaggerRequest {
			public String system;
			public String id;
			public String ref;
		}

		public static class Response implements ISwaggerResponse {
			public String policy;
			public String policyversion;
			public String policyoid;
			public String cn;
			public String cpf;
			public String status;
		}

		public void run(Request req, Response resp, AssijusContext ctx) throws Exception;
	}

	
	public interface IStorePost extends ISwaggerMethod {
		public static class Request implements ISwaggerRequest {
			public String payload;
		}

		public static class Response implements ISwaggerResponse {
			public String status;
			public String key;
		}

		public void run(Request req, Response resp, AssijusContext ctx) throws Exception;
	}

	
	public interface ITimestampPost extends ISwaggerMethod {
		public static class Request implements ISwaggerRequest {
			public String system;
			public byte[] sha256;
			public String tipo;
			public String nome;
			public String cpf;
			public String json;
		}

		public static class Response implements ISwaggerResponse {
			public String jwt;
			public String id;
			public Date time;
			public String url;
			public String host;
		}

		public void run(Request req, Response resp, AssijusContext ctx) throws Exception;
	}

	public interface ITimestampGenerateKeyPairPost extends ISwaggerMethod {
		public static class Request implements ISwaggerRequest {
		}

		public static class Response implements ISwaggerResponse {
			public byte[] publickey;
			public byte[] privatekey;
		}
		
		public void run(Request req, Response resp, AssijusContext ctx) throws Exception;
	}

}