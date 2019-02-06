# Assijus

Assijus é um site que produz assinaturas digitais no padrão da ICP-Brasil (AD-RB) para diversos sistemas conectados simultaneamente. É compatível apenas com o navegador Chrome, através da instalação de uma Chrome Extension.

![captura_de_tela_051216_120220_pm](https://cloud.githubusercontent.com/assets/4137623/16231009/914fa6d2-379a-11e6-8e70-937ef7fa94f1.jpg)

## Veja o Assijus funcionando
[![Apresentação do Assijus](https://img.youtube.com/vi/5qRObgaNG-E/0.jpg)](https://www.youtube.com/watch?v=5qRObgaNG-E)

## Integrando com um novo sistema

### Utilizando Modal
A maneira mais simples de integrar um novo sistema ao Assijus é utilizando uma modal do Bootstrap, veja as instruções abaixo:

* Inclua CSS e JS do Bootstrap 3 na sua página (atualmente só está disponível a integração via Bootstrap 3)
* Carregue o JS: https://assijus.trf2.jus.br/assijus/popup-api.js
* Execute o método produzirAssinaturaDigital passando os parâmetros conforme o exemplo abaixo:

```JS
produzirAssinaturaDigital({
		ui: 'bootstrap-3',
		
		docs: [
			{id: 1, code: 'TRF-MEM-2017/00001'},
			{id: 2, code: 'TRF-MEM-2017/00002'}
		],

		beginCallback: function() {
			// código de inicialização (opcional)
		},
		
		hashCallback: function(id, cont) {
			// retorna o hashes sha1 e sha256 de um documento a partir da id		
			var hash = {
				sha1: 'vBpvCtThfEl+PXn6ZpkQEcWEIyw\u003d', 
				sha256: '9wjEyeorr2HA78aSNQNK7OqZ/rkhw/Br+0BzwAO2TYQ\u003d'
			};
			cont(hash);
		},

		saveCallback: function(id, sign, cont) {
			// grava a assinatura recebida no parâmetro sign.envelope		
			cont({success: true});
		},

		errorCallback: function(id, err, cont) {
			// apresenta mensagem de erro		
			cont();
		},

		endCallback: function() {
			// código de finalização (opcional)		
		}
	});
```

### Utilizando Webservices

Também é possível conectar um sistema através de webservices. Para tanto, é necessário apenas que o sistema implemente 6 métodos REST:

1. /doc/list: a partir do CPF do usuário, faz uma pesquisa no banco de dados e retorna a lista de documentos a serem assinados. Para cada documento, deve ser informado o identificador, o número, a descrição e o tipo.
2. /doc/{id}/hash: a partir do identificador do documento, retornar os hashes SHA1 e SHA256 do PDF.
3. /doc/{id}/pdf: obtem o PDF a partir do identificador do documento. O Assijus só repassará o documento se o usuário tiver permissão para visualizá-lo.
4. /doc/{id}/sign: a partir do identificador do documento e de uma assintura, gravar essa assinatura no banco de dados.
5. /doc/{id}/info: obtém informações sobre o documento.
6. /sign/{ref}: a partir do identificador de uma assinatura, o pacote CMS.


Além dos métodos acima, é importante que os sistemas conectados respondam ao método "/test" com um JSON semelhante a este:

```JSON
{
  "service": "AssijusSystem",
  "url": "/testsigner/api/v1/test",
  "partial": false,
  "available": true,
  "pass": true,
  "ms": 0
}
```

A documentação detalhada dos métodos pode ser vista no arquivo [swagger.yaml](https://github.com/assijus/assijus-system-api/blob/master/src/main/resources/br/jus/trf2/assijus/system/api/swagger.yaml).

Quando é feita a integração via webservices, o Assijus ganha a capacidade de listar todos os documentos do sistema que estão pendentes em sua página inicial. Dessa forma, o Assijus funciona como um concentrador e o usuário não precisa entrar em vários sistemas diferentes só para assinar seus documentos.

Exemplos de integração podem ser vistos no [repositório](https://github.com/assijus) do projeto.

## Arquitetura

Completamente baseado em micro-serviços, o Assijus é composto dos seguintes componentes:
- Site do Assijus: desenvolvido em AngularJS e Java.
- Assijus Chrome Extension: um assinador que se comunica com o navegador Chrome através de Native Messaging API. Existem duas versões: a versão Windows que foi desenvolvida em .NET e a versão MacOS que foi desenvolvida em Java.
- BluCService: Servidor REST do [BlueCrystalSign](https://github.com/bluecrystalsign/signer-source), serviço que auxilia na criação do pacote assinável no padrão da ICP-Brasil, além de produzir o envelope e validar assinaturas.
- Sistemas integrados: qualquer sistema que chame o Assijus via JavaScript ou implemente os métodos REST descritos acima

## Ambiente

Para executar o Assijus, é necessário que algumas propriedades sejam definidas.

Para habilitar um novo sistema a utlizar a integração via Modal, é necessário incluir sua URL em uma propriedade, conforme exemplo abaixo:

- assijus.popup.urls=http://siga.jfrj.jus.br;https://siga.jfrj.jus.br

Por exemplo, se estivermos operando com 3 integrações via webservices, devemos configurar a propriedade assijus.systems para indicar o nome de cada sistema provedor de documentos e depois especificar em nome_do_sistema.url a localização do respectivo webservice:
- assijus.systems=sigadocsigner,apolosigner,textowebsigner
- sigadocsigner.url=http://macmini.local/sigaex/public/app/assinador
- apolosigner.url=http://apolosigner:8080/apolosigner/api/v1
- textowebsigner.url=http://textoweb.jfrj.jus.br:8080/textowebsigner/api/v1

Além disso, precisamos indicar onde está o serviço BluCService:
- blucservice.url=http://blucservice:8080/blucservice/api/v1

Quando se trata de um servidor que não é de produção, precisamos informar onde fica a produção para que ele consiga recuperar a assinatura que foi enviada pelo assijus.exe
- assijus.keyvalue.url=http://assijus.jfrj.jus.br/assijus/api/v1
- assijus.keyvalue.password=375258df-d2fe-46c1-a976-61102546d451

## Docker

O Assijus pode ser facilmente instalado a partir do Docker. Para maiores informações, acesse o repositório [assijus-docker](https://github.com/assijus/assijus-docker).

## Recompilando o Assijus

Embora o Assijus seja uma aplicação open-source, não é muito fácil instalá-lo em outro ambiente que não seja o do TRF2 pois cada extensão do Chrome tem um identificador único. Uma nova implantação iria gerar um novo identificador e vários dos componentes trazem dentro de si referências para esse número e precisariam ser recompilados também. Além disso, seria necessário pagar para abrir uma conta na Chrome Web Store e cadastrar lá a nova versão da extensão. Ou seja, é perfeitamente possível, mas é bem trabalhoso.

Uma alternativa para simplificar esse procedimento consiste em utilizar, o [Fusion](https://ittrufusion.appspot.com/#/about), um site desenvolvido pela empresa Ittru, que é compatível com o Assijus, é gratuito para assinaturas AD-RB e funciona 100% na nuvem.
