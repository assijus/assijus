var app = angular.module('app', [ 'ngPostMessage' ]);

app
		.controller(
				'ctrlPopup',
				function($scope, $http, $interval, $window, $location, $filter,
						$timeout, $q) {
					$scope.params = {};

					// Obter a URL de quem está chamando o iFrame para verificar
					// se é um site confiável.
					if (parent !== window) {
						var parser = document.createElement('a');
						parser.href = document.referrer;
						$scope.parentUrl = parser.protocol + '//' + parser.host;
					}

					$scope.myhttp = function(conf) {
						// The ID of the extension we want to talk to.
						var editorExtensionId = "ifabfihopbhogohngopafekijckmpmka";
						var deferred = $q.defer();

						// Make a simple request:
						chrome.runtime.sendMessage(editorExtensionId, conf,
								function(response) {
									try {
										if (response.success) {
											deferred.resolve(response)
										} else {
											deferred.reject(response);
										}
									} catch (err) {
										deferred.reject(response);
									}
								});
						return deferred.promise;
					}

					$scope.urlBaseAPI = "/assijus/api/v1";
					$scope.showPIN = false;

					$scope.showErrorDetails = false;
					$scope.promise = null;

					$scope.errorDetails = {};
					$scope.errorMsgMissingSigner = "Assijus.exe não encontrado.";
					$scope.errorMsgMissingCertificate = "Nenhum certificado encontrado.";

					$scope.clearError = function(codigo) {
						delete $scope.errorDetails[codigo];
					}

					$scope.reportSuccess = function(codigo, data) {
					}

					$scope.reportErrorAndResume = function(codigo, context,
							response) {
						var msg = "Erro " + context;
						if (response.statusText)
							msg = "Erro " + context + ': '
									+ response.statusText;
						if (response.data.hasOwnProperty("errormsg")
								&& response.statusText === undefined)
							msg = response.data.errormsg;
						try {
							var detail = {
								presentable : false,
								logged : false
							};
							if (response.data.hasOwnProperty("errordetails")
									&& response.data.errordetails.length > 0) {
								detail = response.data.errordetails[response.data.errordetails.length - 1];
								msg = "Não foi possível " + detail.context;
							}
							if (response.data.hasOwnProperty("errormsg")
									&& detail.presentable)
								msg = response.data.errormsg;
							if (detail.logged)
								msg += ", a TI já foi notificada.";
						} catch (err) {

						}
					}

					$scope.presentError = function(id) {
						$scope.showErrorDetails = true;
						$scope.currentErrorId = id;
					}

					$scope.setError = function(response) {
						if (response === undefined) {
							delete $scope.errorDetails.geral;
							return;
						}
						var data;
						if (typeof response === 'string')
							data = {
								errormsg : response
							};
						else {
							data = response.data;
							if (response.data == null
									&& typeof response.statusText === 'string'
									&& response.statusText != '')
								data = {
									errormsg : response.statusText
								};
							else if (response.data == null
									&& typeof response.status === 'number')
								data = {
									errormsg : "http status " + response.status
								};
							else if (data != null
									&& (typeof data.errormsg == 'string')
									&& data.errormsg.lastIndexOf(
											"O conjunto de chaves não", 0) === 0)
								data.errormsg = $scope.errorMsgMissingCertificate;
						}
						$scope.errorDetails.geral = data;
					}

					$scope.setCert = function(data) {
						if (data === undefined) {
							delete $scope.cert;
							delete $scope.documentos;
							return;
						}
						if (data.subject != ($scope.cert || {}).subject)
							delete $scope.documentos;
						$scope.cert = data;
						var cn = '';
						if ($scope.assinanteIdentificado()) {
							cn = $scope.cert.subject;
							cn = cn.split(",")[0];
							cn = cn.split(":")[0];
							cn = cn.replace("CN=", "");
						}
						$scope.assinante = cn;
					}

					$scope.progress = {
						active : false,
						csteps : 0,
						isteps : 0,
						minwidth : 0,
						maxwidth : 100,
						start : function(title, steps, min, max) {
							$scope.noProgress.stop(); // disable pending
							// updates
							$scope.progressbarTitle = title;
							$scope.progressbarWidth = this.minwidth;
							$scope.progressbarShow = true;
							$scope.progressbarHide = function() {
								$scope.progress.active = false;
							}
							this.active = true;
							this.isteps = 0;
							this.csteps = steps;
							this.minwidth = min;
							this.maxwidth = max;
						},
						step : function(caption, skip) {
							if (!this.active) {
								console.log(this.isteps + "/" + this.csteps
										+ ": [SKIPPED] " + caption);
								return;
							}
							this.isteps += 1 + (skip || 0);
							console.log(this.isteps + "/" + this.csteps + ": "
									+ caption);
							$scope.progressbarWidth = this.minwidth
									+ this.maxwidth
									* (this.isteps / this.csteps);
							$scope.progressbarShow = true;
							$scope.progressbarCaption = caption;
							if (this.isteps == this.csteps)
								this.stop();
						},
						startperc : function(title, caption) {
							this.start(title, 100, 0, 100);
							$scope.progressbarCaption = caption;
						},
						perc : function(caption, percentage) {
							if (!this.active)
								return;
							$scope.progressbarWidth = percentage;
							$scope.progressbarShow = true;
							$scope.progressbarCaption = caption;
						},
						stop : function() {
							if (this.maxwidth == 100) {
								$scope.postMessage({
									command : '<END-REQUEST>'
								});
							}
							$scope.progressbarTitle = '';
							$scope.progressbarWidth = this.maxwidth;
							$scope.progressbarShow = false;
							$scope.progressbarCaption = '';
							this.active = false;
							this.csteps = 0;
							this.isteps = 0;
						}
					}

					$scope.noProgress = {
						active : false,
						start : function() {
						},
						step : function() {
							this.active = true;
						},
						stop : function() {
							this.active = false;
						}
					}

					$scope.assinanteIdentificado = function() {
						return $scope.hasOwnProperty("cert");
					}

					//
					// Sign
					//

					$scope.assinarDocumentos = function(progress) {
						$scope.operacoes = [];
						$scope.iOperacao = -1;
						var docs = $scope.params.documentos;
						for (var i = 0; i < docs.length; i++) {
							var doc = docs[i];
							var operacao = {
								codigo : doc.id,
								nome : doc.code,
								extra : doc.extra
							};
							$scope.operacoes.push(operacao);
						}

						progress.start($scope.PROCESSING,
								$scope.operacoes.length * 10 + 6, 20, 100);
						$scope.validarAuthKey(progress, $scope.executar);
					}

					$scope.executar = function(progress) {
						if (!$scope.progress.active)
							return;

						for (i = $scope.iOperacao + 1; i < $scope.operacoes.length; i++) {
							var o = $scope.operacoes[i];
							$scope.iOperacao = i;

							window.setTimeout(function() {
								$scope.assinar({
									nome : o.nome,
									codigo : o.codigo,
									extra : o.extra
								}, progress);
							}, 10);
							return;
						}
					}

					$scope.assinar = function(state, progress) {
						if (progress.active)
							$scope.obterHash(state, progress);
					}

					// 2 steps
					$scope.obterHash = function(state, progress) {
						progress.step(state.nome + ": Obterndo hash...");
						$scope.postMessage({
							command : '<HASH-REQUEST>',
							id : state.codigo
						}, function(data) {
							progress.step(state.nome + ": Hash obtido...");
							state.sha1 = data.sha1;
							state.sha256 = data.sha256;

							$scope.obterPacote(state, progress);
						})
					}

					// 2 steps
					$scope.obterPacote = function(state, progress) {
						progress.step(state.nome
								+ ": Preparando pacote assinável...");
						$http({
							url : $scope.urlBaseAPI + "/signed-attrs",
							method : "POST",
							data : {
								authkey : $scope.getAuthKey(),
								certificate : $scope.cert.certificate,
								policy : 'AD-RB',
								sha1 : state.sha1,
								sha256 : state.sha256,
							}
						}).then(
								function successCallback(response) {
									progress.step(state.nome
											+ ": Pacote preparado...");
									var data = response.data;
									state.policy = data.policy;
									state.policyversion = data.policyversion;
									state.time = data.time;
									state.hash = data.hash;
									state.sha1 = data.sha1;
									state.sha256 = data.sha256;
									state.hash = data.hash;
									if (progress.active)
										$scope.produzirAssinatura(state,
												progress);
								},
								function errorCallback(response) {
									progress.step(state.nome
											+ ": Pacote não preparado...", 4);
									logEvento("erro", "obtendo o hash",
											state.system);
									$scope.reportErrorAndResume(state.codigo,
											"obtendo o hash", response);
									$scope.executar(progress);
								});
					}

					// 2 steps
					$scope.produzirAssinatura = function(state, progress) {
						progress.step(state.nome + ": Assinando...");

						$scope
								.myhttp({
									url : $scope.urlBluCRESTSigner + "/sign",
									method : "POST",
									data : {
										system : state.system,
										id : state.codigo,
										code : state.nome,
										policy : state.policy,
										payload : state.hash,
										certificate : $scope.cert.certificate,
										subject : $scope.cert.subject
									}
								})
								.then(
										function successCallback(response) {
											var data = response.data;
											progress.step(state.nome
													+ ": Assinado.");
											if (data.sign != "")
												state.assinaturaB64 = data.sign;
											if (data.signkey != "")
												state.signkey = data.signkey;
											state.assinante = data.cn;
											var re = /CN=([^,]+),/gi;
											var m;
											if ((m = re.exec(state.assinante)) != null) {
												state.assinante = m[1];
											}
											$scope.clearError(state.codigo);
											if (progress.active) {
												// $scope.executar(progress);
												$scope.obterEnvelope(state,
														progress);
											}
										},
										function errorCallback(response) {
											progress.step(state.nome
													+ ": Não assinado.", 2);
											logEvento("erro", "assinando",
													state.system);
											$scope.reportErrorAndResume(
													state.codigo, "assinando",
													response);
											if ($scope.endpoint)
												$scope.endpoint.usecallback = false;
											$scope.executar(progress);
										});
					}

					$scope.obterEnvelope = function(state, progress) {
						progress
								.step(state.nome + ": Preparando o envelope...");
						$http({
							url : $scope.urlBaseAPI + "/envelope",
							method : "POST",
							data : {
								certificate : $scope.cert.certificate,
								policy : state.policy,
								policyversion : state.policyversion,
								signature : state.assinaturaB64,
								sha1 : state.sha1,
								sha256 : state.sha256,
								time : state.time
							}
						})
								.then(
										function successCallback(response) {
											progress
													.step(state.nome
															+ ": Envelope preparado...");
											var data = response.data;
											state.envelopeB64 = data.envelope;
											state.cpf = data.cpf;
											state.name = data.name;
											if (progress.active)
												$scope.gravarAssinatura(state,
														progress);
										},
										function errorCallback(response) {
											progress
													.step(
															state.nome
																	+ ": Envelope não preparado...",
															4);
											logEvento("erro",
													"obtendo o envelope",
													state.system);
											$scope.reportErrorAndResume(
													state.codigo,
													"obtendo o envelope",
													response);
											$scope.executar(progress);
										});
					}

					// 2
					$scope.gravarAssinatura = function(state, progress) {
						progress.step(state.nome + ": Gravando assinatura...");
						$scope.postMessage({
							command : '<SAVE-REQUEST>',
							id : state.codigo,
							sign : {
								envelope : state.envelopeB64,
								time : state.time,
								policy : state.policy,
								policyversion : state.policyversion,
								sha1 : state.sha1,
								sha256 : state.sha256,
								certificate : $scope.cert.certificate,
								code : state.nome,
								cpf : $scope.cpf,
								extra : state.extra
							}
						},
								function(success) {
									if (success) {
										progress.step(state.nome
												+ ": Assinatura gravada.");
										logEvento("assinatura", "assinar",
												state.system);
									} else {
										progress.step(state.nome
												+ ": Assinatura não gravada.");
									}
									$scope.executar(progress);
								})
					}

					//
					// Initialize
					//
					$scope.getAuthKey = function() {
						return $scope.authkey;
					}

					$scope.setAuthKey = function(authkey) {
						$scope.authkey = authkey;
					}

					$scope.hasAuthKey = function() {
						return $scope.hasOwnProperty('authkey');
					}

					// 2 steps
					$scope.assinarToken = function(token, progress, cont) {
						// Assinar string para formar o
						// token
						$scope.myhttp({
							url : $scope.urlBluCRESTSigner + '/token',
							method : "POST",
							data : {
								"certificate" : $scope.cert.certificate,
								"token" : token,
								"subject" : $scope.cert.subject,
								"policy" : "AD-RB"
							}
						}).then(function successCallback(response) {
							var data = response.data;
							progress.step("Usuário autenticado.");
							var token = data.token + ";" + data.sign;

							// Armazenar o token e obter a authkey
							$http({
								url : $scope.urlBaseAPI + '/auth',
								method : "POST",
								data : {
									"token" : token
								}
							}).then(function successCallback(response) {
								var data = response.data;
								progress.step("Chave de autenticação obtida.");
								$scope.setAuthKey(data.authkey);
								$scope.cpf = data.cpf;
								cont(progress);
							}, function errorCallback(response) {
								delete $scope.documentos;
								progress.stop();
								$scope.setError(response);
							});
						}, function errorCallback(response) {
							delete $scope.documentos;
							progress.stop();
							$scope.setError(response);
						});
					}

					// 2 steps
					$scope.obterToken = function(progress, cont) {
						// Obter string para ser assinada
						$http({
							url : $scope.urlBaseAPI + '/token',
							method : "POST",
							data : {
								"certificate" : $scope.cert.certificate
							}
						}).then(function successCallback(response) {
							var data = response.data;
							progress.step("Senha de autenticação preparada.");
							var token = data.token;
							progress.step("Autenticando usuário");

							$scope.assinarToken(token, progress, cont);
						}, function errorCallback(response) {
							delete $scope.documentos;
							progress.stop();
							$scope.setError(response);
						});
					}

					// 2 steps
					$scope.validarAuthKey = function(progress, cont) {
						// Verificar se a authkey existe e é valida
						if (!$scope.hasAuthKey()) {
							progress.step("Chave de autenticação inexistente",
									1);
							return $scope.obterToken(progress, cont);
						}
						progress.step("Verificando chave de autenticação...");
						$http({
							url : $scope.urlBaseAPI + '/auth',
							method : "POST",
							data : {
								"authkey" : $scope.getAuthKey()
							}
						}).then(function successCallback(response) {
							progress.step("Chave de autenticação válida.", 4);
							$scope.cpf = response.data.cpf;
							cont(progress);
						}, function errorCallback(response) {
							progress.step("Chave de autenticação inválida.");
							$scope.obterToken(progress, cont);
						});
					}

					// 2 steps
					$scope.selecionarCertificado = function(progress, cont) {
						progress.step("Selecionando certificado...");
						$scope
								.myhttp({
									url : $scope.urlBluCRESTSigner + '/cert',
									method : "POST",
									data : {
										userPIN : $scope.userPIN,
										subject : $scope.userSubject
									}
								})
								.then(
										function successCallback(response) {
											var data = response.data;

											if ($scope.p11 && data && data.list) {
												$scope
														.showDialogForCerts(data.list);
												return;
											}

											progress
													.step("Certificado selecionado.");
											if (data.hasOwnProperty('errormsg')
													&& data.errormsg != null) {
												delete $scope.documentos;
												progress.stop();
												$scope.setError(response);
												return;
											}
											$scope.setCert(data);
											$scope.validarAuthKey(progress,
													cont);
										},
										function errorCallback(response) {
											delete $scope.documentos;
											progress.stop();
											if (response.data
													&& response.data.errormsg
															.indexOf("CKR_") !== -1) {
												var err = response.data.errormsg;
												if (err == "CKR_PIN_INCORRECT")
													err = "PIN incorreto. Atenção: muitas tentativas incorretas podem bloquear seu token!";
												if (err == "CKR_PIN_LOCKED")
													err = "Seu token está bloqueado por excesso de tentativas incorretas de informar o PIN.";
												$scope.showDialogForPIN(err,
														cont);
												return;
											}
											$scope.setError(response);
										});
					}

					$scope.pinProsseguir = function() {
						delete $scope.errormsg;
						if (($scope.pinField || "") == "") {
							delete $scope.userPIN;
							return;
						}
						$scope.userPIN = $scope.pinField;
						$scope.pinDialog = false;
						$scope.progress.start("Inicializando", 10);
						$scope.selecionarCertificado($scope.progress,
								$scope.cont);
					}

					$scope.showDialogForPIN = function(err, cont) {
						$scope.errormsg = err;
						$scope.cont = cont;
						$scope.pinDialog = true;
					}

					$scope.certsProsseguir = function() {
						if (($scope.userSubjectField || "") == "") {
							delete $scope.userSubject;
							return;
						}
						$scope.userSubject = $scope.userSubjectField;
						if ($scope.hasOwnProperty('userSubject')) {
							$scope.progress.start("Inicializando", 10);
							$scope.selecionarCertificado($scope.progress,
									$scope.cont);
						}
					}

					$scope.showDialogForCerts = function(list, cont) {
						$scope.cont = cont;
						$scope.certsDialog = true;
					};

					// 3 steps
					$scope.buscarCertificadoCorrente = function(progress, cont) {
						progress.step("Buscando certificado corrente...");
						$scope
								.myhttp(
										{
											url : $scope.urlBluCRESTSigner
													+ '/currentcert',
											method : "GET"
										})
								.then(
										function successCallback(response) {
											var data = response.data;
											if (data.hasOwnProperty('subject')
													&& data.subject !== null) {
												progress
														.step(
																"Certificado corrente localizado.",
																2);
												$scope.setCert(data);
												$scope.validarAuthKey(progress,
														cont);
											} else {
												if ($scope.p11
														&& !$scope
																.hasOwnProperty('userPIN')) {
													$scope.showDialogForPIN(
															undefined, cont);
													progress.stop();
													return;
												}
												$scope.selecionarCertificado(
														progress, cont);
											}
										}, function errorCallback(response) {
											delete $scope.documentos;
											progress.stop();
											$scope.setError(response);
										});
					}

					// 2 steps
					$scope.testarSigner = function(progress, cont) {
						progress.step("Testando Assijus.exe");
						$scope
								.myhttp({
									url : $scope.urlBluCRESTSigner + '/test',
									method : "GET"
								})
								.then(
										function successCallback(response) {
											progress
													.step("Assijus.exe está ativo.");
											if (response.data.status == "OK") {
												$scope.p11 = response.data.provider
														.indexOf("PKCS#11") !== -1;
												document
														.getElementById("native-client-active").value = response.data.version;
												$scope
														.buscarCertificadoCorrente(
																progress, cont);
											} else {
												progress.stop();
												$scope
														.setError($scope.errorMsgMissingSigner)
											}
										},
										function errorCallback(response) {
											var data = undefined;
											if (response !== undefined
													&& typeof response === 'object')
												data = response.data;
											delete $scope.documentos;
											progress.stop();
											if (typeof data === 'object'
													&& data != null
													&& data
															.hasOwnProperty('errormsg')) {
												$scope.setError(response);
											} else {
												$scope
														.setError($scope.errorMsgMissingSigner)
											}
										});
					};

					$scope.postMessage = function(data, cont) {
						parent.postMessage(data, $scope.parentUrl);
						$scope.cont = cont;
					};

					$scope.$root.$on('$messageIncoming', function(event, data) {
						if (data.origin !== $scope.parentUrl)
							return;
						var permitido = false;
						for (var i = 0; i < $scope.allowedParents.length; i++) {
							if (data.origin
									.startsWith($scope.allowedParents[i])) {
								permitido = true;
								continue;
							}
						}
						if (!permitido) {
							$scope.setError('Assijus não está configurado'
									+ ' para permitir assinaturas do site '
									+ data.origin);
							$scope.presentError('geral');
							return;
						}
						// console.log('Assijus AngularJS recebeu mensagem: ',
						// data)
						if (data.command === '<GO>') {
							delete $scope.documentos;
							$scope.params.documentos = data.docs;
							$scope.progress.start("Inicializando", 14, 0, 20);
							$scope.testarSigner($scope.progress, function() {
								$scope.postMessage({
									command : '<BEGIN-REQUEST>'
								});
								$scope.assinarDocumentos($scope.progress)
							});
						}
						if (data.command === '<HASH-RESPONSE>') {
							$scope.cont(data.params);
						}
						if (data.command === '<SAVE-RESPONSE>') {
							$scope.cont(data.params);
						}

						$timeout(function() {
							parent.postMessage({
								command : '<SET-HEIGHT>',
								height : document.body.scrollHeight + 'px'
							}, $scope.parentUrl)
						}, 100);
					});

					$http({
						url : 'api/v1/test?skip=all',
						method : "GET"
					})
							.then(
									function successCallback(response) {
										$scope.test = response.data;
										var popupUrls = response.data.properties['assijus.popup.urls'];
										$scope.allowedParents = popupUrls !== '[undefined]' ? popupUrls
												.split(';')
												: [];
										$scope.postMessage({
											command : '<READY>'
										});
									}, function errorCallback(response) {
									});
				});
