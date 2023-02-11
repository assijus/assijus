package br.jus.trf2.assijus;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.jus.trf2.assijus.IAssijus.Document;
import br.jus.trf2.assijus.IAssijus.ISupportListCpfGet;
import br.jus.trf2.assijus.IAssijus.SupportDocument;
public class SupportListCpfGet implements ISupportListCpfGet {
	private static final Logger log = LoggerFactory.getLogger(SupportListCpfGet.class);

	@Override
	public void run(Request req, Response resp, AssijusContext ctx) throws Exception {
		ListPost.Response lpr = new ListPost.Response();
		ListPost.produceListPostResponse(req.cpf, lpr);
		resp.status = lpr.status;
		resp.list = new ArrayList<>();
		for (Document i : lpr.list) {
			SupportDocument d = new SupportDocument();
			d.code = i.code;
			d.id = i.id;
			d.kind = i.kind;
			d.origin = i.origin;
			d.system = i.system;
			resp.list.add(d);
		}
	}

	@Override
	public String getContext() {
		return "obter lista de documentos para suporte";
	}

}
