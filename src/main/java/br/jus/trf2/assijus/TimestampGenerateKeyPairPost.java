package br.jus.trf2.assijus;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;

import br.jus.trf2.assijus.IAssijus.ITimestampGenerateKeyPairPost;
import br.jus.trf2.assijus.IAssijus.TimestampGenerateKeyPairPostRequest;
import br.jus.trf2.assijus.IAssijus.TimestampGenerateKeyPairPostResponse;

import com.crivano.swaggerservlet.SwaggerUtils;

public class TimestampGenerateKeyPairPost implements
		ITimestampGenerateKeyPairPost {

	@Override
	public void run(TimestampGenerateKeyPairPostRequest req,
			TimestampGenerateKeyPairPostResponse resp) throws Exception {
		// create the key instance
		PublicKey publicKey = null;
		PrivateKey privateKey = null;

		// creating the object of KeyPairGenerator
		KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");

		// initializing with 1024
		kpg.initialize(2048);

		// getting key pairs
		// using generateKeyPair() method
		KeyPair kp = kpg.genKeyPair();

		// getting public key
		publicKey = kp.getPublic();

		privateKey = kp.getPrivate();

		// Produce response
		resp.publickey = publicKey.getEncoded();
		resp.privatekey = privateKey.getEncoded();
	}

	@Override
	public String getContext() {
		return "gerar par de chaves";
	}

}