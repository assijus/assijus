package br.jus.trf2.assijus;

import java.util.Date;

import com.crivano.swaggerservlet.ISwaggerMethod;
import com.crivano.swaggerservlet.ISwaggerModel;
import com.crivano.swaggerservlet.ISwaggerRequest;
import com.crivano.swaggerservlet.ISwaggerResponse;

interface IBlueCrystal {
	class CertDetails implements ISwaggerModel {
		String aki0;
		String basicConstraint0;
		String birth_date0;
		String certPolOid0;
		String certPolQualifier0;
		String certSha2560;
		String cert_type0;
		String cert_usage0;
		String chain0;
		String cpf0;
		String crlDP0;
		String eku0;
		String eleitor0;
		String issuer0;
		String key_length0;
		String ku0;
		String notAfter0;
		String notBefore0;
		String rg0;
		String rg_org0;
		String rg_uf0;
		String san_email0;
		String serial0;
		String subject0;
		String version0;
	}

	class Policy implements ISwaggerModel {
	}

	class PolicyVersion implements ISwaggerModel {
	}

	class PolicyOID implements ISwaggerModel {
	}

	class CN implements ISwaggerModel {
	}

	class HashB64 implements ISwaggerModel {
	}

	class EnvelopeB64 implements ISwaggerModel {
	}

	class Status implements ISwaggerModel {
	}

	class Message implements ISwaggerModel {
	}

	class Sha256Hex implements ISwaggerModel {
	}

	class Error implements ISwaggerModel {
		String error;
	}

	class TestGetRequest implements ISwaggerRequest {
	}

	class TestGetResponse implements ISwaggerResponse {
		String provider;
		String version;
		String status;
	}

	interface ITestGet extends ISwaggerMethod {
		void run(TestGetRequest req, TestGetResponse resp) throws Exception;
	}

	class CertificatePostRequest implements ISwaggerRequest {
		byte[] certificate;
	}

	class CertificatePostResponse implements ISwaggerResponse {
		String cn;
		String name;
		String subject;
		String cpf;
		CertDetails certdetails;
	}

	interface ICertificatePost extends ISwaggerMethod {
		void run(CertificatePostRequest req, CertificatePostResponse resp)
				throws Exception;
	}

	class AttachPostRequest implements ISwaggerRequest {
		byte[] envelope;
		byte[] content;
	}

	class AttachPostResponse implements ISwaggerResponse {
		byte[] envelope;
		String sha256hex;
	}

	interface IAttachPost extends ISwaggerMethod {
		void run(AttachPostRequest req, AttachPostResponse resp)
				throws Exception;
	}

	class HashPostRequest implements ISwaggerRequest {
		String policy;
		byte[] certificate;
		byte[] sha1;
		byte[] sha256;
		Date time;
		Boolean crl;
	}

	class HashPostResponse implements ISwaggerResponse {
		byte[] hash;
		String policy;
		String policyversion;
		String policyoid;
		String cn;
		CertDetails certdetails;
	}

	interface IHashPost extends ISwaggerMethod {
		void run(HashPostRequest req, HashPostResponse resp) throws Exception;
	}

	class EnvelopePostRequest implements ISwaggerRequest {
		byte[] signature;
		String policy;
		byte[] certificate;
		byte[] sha1;
		byte[] sha256;
		Date time;
		Boolean crl;
	}

	class EnvelopePostResponse implements ISwaggerResponse {
		byte[] envelope;
		String policy;
		String policyversion;
		String policyoid;
		String cn;
		CertDetails certdetails;
	}

	interface IEnvelopePost extends ISwaggerMethod {
		void run(EnvelopePostRequest req, EnvelopePostResponse resp)
				throws Exception;
	}

	class ValidatePostRequest implements ISwaggerRequest {
		byte[] envelope;
		byte[] sha1;
		byte[] sha256;
		Date time;
		Boolean crl;
	}

	class ValidatePostResponse implements ISwaggerResponse {
		String policy;
		String policyversion;
		String policyoid;
		String cn;
		CertDetails certdetails;
		String status;
		String error;
	}

	interface IValidatePost extends ISwaggerMethod {
		void run(ValidatePostRequest req, ValidatePostResponse resp)
				throws Exception;
	}

}
