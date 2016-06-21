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
