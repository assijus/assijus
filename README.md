# Assijus

Assijus é um site que produz assinaturas digitais no padrão da ICP-Brasil (AD-RB) para diversos sistemas conectados simultaneamente. É compatível com todos os navegadores e independente de Applets ou ActiveX.

![captura_de_tela_051216_120220_pm](https://cloud.githubusercontent.com/assets/4137623/16231009/914fa6d2-379a-11e6-8e70-937ef7fa94f1.jpg)

## Integrando com um novo sistema
Para incluir um novo sistema, é necessário apenas que ele seja capaz de responder a 4 métodos REST:

1. list: a partir do CPF do usuário, faz uma pesquisa no banco de dados e retorna a lista de documentos a serem assinados. Para cada documento, deve ser informado o identificador, o número, a descrição e o tipo.
2. hash: a partir do identificador do documento, retornar os hashes SHA1 e SHA256 do PDF. Além disso, se houver interesse em produzir assinaturas sem política no padrão PKCS7, retornar o conteúdo do PDF se for solicitado.
3. save: a partir do identificador do documento e de uma assintura, gravar essa assinatura no banco de dados.
4. view: a partir do identificador do documento, retornar o conteúdo do PDF.

## Arquitetura

Completamente baseado em micro-serviços, o Assijus é composto dos seguintes componentes:
- Site do Assijus: desenvolvido em AngularJS e Java.
- Assijus.exe: um assinador REST que roda em http://localhost:8612 e foi desenvolvido em .NET
- BluCService: Servidor REST do BlueCrystalSign, serviço que auxilia na criação do pacote assinável no padrão da ICP-Brasil, além de produzir o envelope e validar assinaturas.
- Sistemas integrados: qualquer sistema que implemente os 4 métodos REST descritos acima

## Ambiente

Para executar o Assijus, é necessário que algumas propriedades sejam definidas.

Por exemplo, se estivermos operando com as 3 integrações abaixo, devemos configurar a propriedade assijus.systems para indicar o nome de cada sistema provedor de documentos e depois especificar em nome_do_sistema.url a localização do respectivo webservice:
- assijus.systems=sigadocsigner,apolosigner,textowebsigner
- sigadocsigner.url=http://macmini.local/sigaex/public/app/assinador
- apolosigner.url=http://apolosigner:8080/apolosigner/api/v1
- textowebsigner.url=http://textoweb.jfrj.jus.br:8080/textowebsigner/api/v1

Além disso, precisamos indicar onde está o serviço BluCService:
- blucservice.url=http://blucservice:8080/blucservice/api/v1

Quando se trata de um servidor que não é de produção, precisamos informar onde fica a produção para que ele consiga recuperar a assinatura que foi enviada pelo assijus.exe
- assijus.keyvalue.url=http://assijus.jfrj.jus.br/assijus/api/v1
- assijus.keyvalue.password=375258df-d2fe-46c1-a976-61102546d451
