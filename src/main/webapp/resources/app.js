var app = angular.module('app', [ 'ngAnimate', 'ngRoute', 'cgBusy' ]);

app.config([ '$routeProvider', '$locationProvider',
		function($routeProvider, $locationProvider) {
			$routeProvider.when('/home', {
				templateUrl : 'resources/home.html',
				controller : 'ctrl'
			}).when('/sugestoes', {
				templateUrl : 'resources/sugestoes.html',
				controller : 'ctrl2'
			}).when('/sobre', {
				templateUrl : 'resources/sobre.html',
				controller : 'ctrl2'
			}).otherwise({
				redirectTo : '/home'
			});
			// enable html5Mode for pushstate ('#'-less URLs)
			$locationProvider.html5Mode(false);
		} ]);

app.controller('routerCtrl', function($scope, $http, $window, $location) {
	$scope.assijusexe = "assijus-v0-9-3.exe";

	$scope.parseLocation = function(location) {
		var pairs = location.substring(1).split("&");
		var obj = {};
		var pair;
		var i;

		for (i in pairs) {
			if (pairs[i] === "")
				continue;

			pair = pairs[i].split("=");
			obj[decodeURIComponent(pair[0])] = decodeURIComponent(pair[1]);
		}

		return obj;
	};

	$scope.querystring = $scope.parseLocation($window.location.search);
});

app.controller('ctrl2', function($scope, $http, $interval, $window) {
});

app
		.controller(
				'ctrl',
				function($scope, $http, $interval, $window, $location, $filter,
						$q, $timeout) {

					$scope.isChromeExtensionActive = function() {
						return document
								.getElementById("chrome-extension-active").value != "0";
					}

					$scope.myhttp = function(conf) {
						if ($scope.isChromeExtensionActive()) {
							// The ID of the extension we want to talk to.
							var editorExtensionId = "ifabfihopbhogohngopafekijckmpmka";
							// if ($location.absUrl().indexOf("//localhost/")
							// !== -1)
							// editorExtensionId =
							// "lnifncldepnkbfaedkdkcmbfbbfhhchm";
							var deferred = $q.defer()

							// Make a simple request:
							chrome.runtime.sendMessage(editorExtensionId, conf,
									function(response) {
										if (!response.success) {
											deferred.reject(response);
										} else {
											deferred.resolve(response)
										}
									});
							return deferred.promise
						} else {
							return $http(conf);
						}
					}

					$scope.PROCESSING = "Processando Assinaturas Digitais";

					if ($scope.$parent.querystring.hasOwnProperty('urlsigner')) {
						$scope.urlBluCRESTSigner = $scope.$parent.querystring.urlsigner;
					} else {
						$scope.urlBluCRESTSigner = "http://localhost:8612";
					}
					if ($scope.$parent.querystring
							.hasOwnProperty('endpointlist')
							|| $scope.$parent.querystring
									.hasOwnProperty('endpointlistkey')) {
						$scope.endpoint = {};
						if ($scope.$parent.querystring
								.hasOwnProperty('endpointlistkey'))
							$scope.endpoint.listkey = $scope.$parent.querystring.endpointlistkey;
						else
							$scope.endpoint.list = JSON
									.parse($scope.$parent.querystring.endpointlist);
						$scope.endpoint.callback = $scope.$parent.querystring.endpointcallback;
					}
					$scope.urlBaseAPI = "/assijus/api/v1";

					$scope.showErrorDetails = false;
					$scope.filterErrorDetails = function(v) {
						return !v.hasOwnProperty('hideAlert');
					};
					$scope.promise = null;
					$scope.checkall = true;

					$scope.errorDetails = {};
					$scope.errorMsgMissingSigner = "Assijus.exe não encontrado.";
					$scope.errorMsgMissingCertificate = "Nenhum certificado encontrado.";

					$scope.clearError = function(codigo) {
						delete $scope.errorDetails[codigo];
					}

					$scope.reportSuccess = function(codigo, data) {
						// $('#status' + state.codigo).goTo();
						var sts = '<span class="status-ok" data-toggle="tooltip" title="Assinado, OK!">&#10003;</span>';
						if (data.hasOwnProperty('warning')) {
							sts += ' <span class="status-warning">'
							for (var i = 0, len = data.warning.length; i < len; i++) {
								if (i != 0)
									sts += ',';
								sts += '<span data-toggle="tooltip" title="'
										+ data.warning[i].description + '">'
										+ data.warning[i].label + '</span>';
							}
							sts += '</span>';
						}
						$('#status' + codigo).html(sts);
						$scope.disable(codigo);
						$scope.clearError(codigo);
					}

					$scope.reportErrorAndResume = function(codigo, context,
							response) {
						var msg = "Erro " + context + ': '
								+ response.statusText;
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

						$scope.errorDetails[codigo] = response.data;
						$scope.errorDetails[codigo].hideAlert = true;

						$('#status' + codigo)
								.html(
										'<span class="status-error">' + msg
												+ '</span>');
						$('#details' + codigo).html('<span>' + msg + '</span>');
					}

					$scope.composeErrorMessage = function(errordata) {
						var msg = "Erro.";
						try {
							if (errordata.hasOwnProperty("errordetails")) {
								var detail = {
									presentable : false,
									logged : false
								};
								if (errordata.hasOwnProperty("errordetails")
										&& errordata.errordetails.length > 0) {
									detail = errordata.errordetails[errordata.errordetails.length - 1];
									msg = "Não foi possível " + detail.context;
								}
								if (errordata.hasOwnProperty("errormsg")
										&& detail.presentable)
									msg = errordata.errormsg;
								if (detail.logged)
									msg += ", a TI já foi notificada.";
							} else if (errordata.hasOwnProperty("errormsg")) {
								msg = errormsg;
							}
						} catch (err) {

						}
						return msg;
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
						start : function(title, steps) {
							$scope.noProgress.stop(); // disable pending
							// updates
							$scope.progressbarTitle = title;
							$scope.progressbarWidth = 0;
							$scope.progressbarShow = true;
							$scope.progressbarHide = function() {
								$scope.progress.active = false;
							}
							this.active = true;
							this.isteps = 0;
							this.csteps = steps;
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
							$scope.progressbarWidth = 100 * (this.isteps / this.csteps);
							$scope.progressbarShow = true;
							$scope.progressbarCaption = caption;
							if (this.isteps == this.csteps)
								this.stop();
						},
						startperc : function(title, caption) {
							this.start(title, 100);
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
							$scope.progressbarTitle = '';
							$scope.progressbarWidth = 100;
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

					$scope.documentosCarregados = function() {
						return $scope.hasOwnProperty("documentos")
								&& $scope.documentos.length != 0;
					}

					$scope.zeroDocumentosCarregados = function() {
						return $scope.hasOwnProperty("documentos")
								&& $scope.documentos.length == 0;
					}

					$scope.docs = function() {
						var docs = $filter('filter')($scope.documentos || [],
								$scope.filtro);
						return docs;
					}

					$scope.marcarTodos = function() {
						var docs = $scope.docs();
						for (var i = 0; i < docs.length; i++) {
							var doc = docs[i];
							if (!doc.disabled)
								doc.checked = $scope.checkall;
						}
					}

					$scope.contarChecked = function() {
						var c = 0;
						var docs = $scope.docs();
						for (var i = 0; i < docs.length; i++) {
							var doc = docs[i];
							if (docs[i].checked)
								c++;
						}
						return c;
					}

					// 0 - Nenhuma, 1 = digital
					$scope.verificarTipoDeAssinatura = function() {
						var usehw = false;

						for (var i = 0, len = $scope.operacoes.length; i < len; i++) {
							if ($scope.operacoes[i].enabled) {
								usehw = true;
							}
						}
						return (usehw ? 1 : 0);
					}

					$scope.identificarOperacoes = function() {
						$scope.operacoes = [];
						var docs = $scope.docs();
						for (var i = 0; i < docs.length; i++) {
							var doc = docs[i];
							if (doc.checked) {
								var operacao = {
									system : doc.system,
									codigo : doc.id,
									nome : doc.code,
									enabled : true,
								};
								$scope.operacoes.push(operacao);
							}
						}
					}

					//
					// View
					//
					$scope.view = function(doc) {
						$scope.progress.start("Preparando Visualização", 6);
						$scope.validarAuthKey($scope.progress, function(progress) {
							progress.stop();
							var form = document.createElement('form');
							form.action = $scope.urlBaseAPI + "/view";
							form.method = 'POST';
							form.target = '_blank';
							form.style.display = 'none';

							var authkey = document.createElement('input');
							authkey.type = 'text';
							authkey.name = 'authkey';
							authkey.value = $scope.getAuthKey();

							var subject = document.createElement('input');
							subject.type = 'text';
							subject.name = 'subject';
							subject.value = $scope.cert.subject;

							var system = document.createElement('input');
							system.type = 'text';
							system.name = 'system';
							system.value = doc.system;

							var docid = document.createElement('input');
							docid.type = 'text';
							docid.name = 'id';
							docid.value = doc.id;

							var submit = document.createElement('input');
							submit.type = 'submit';
							submit.id = 'submitView';

							form.appendChild(authkey);
							form.appendChild(subject);
							form.appendChild(system);
							form.appendChild(docid);
							form.appendChild(submit);
							document.body.appendChild(form);

							$('#submitView').click();

							document.body.removeChild(form);
						});
					}

					//
					// Sign
					//

					$scope.assinarDocumento = function(id) {
						$scope.operacoes = [];

						var docs = $scope.docs();
						for (var i = 0; i < docs.length; i++) {
							var doc = docs[i];
							if (doc.id == id) {
								var operacao = {
									system : doc.system,
									codigo : doc.id,
									nome : doc.code,
									enabled : true,
								};
								$scope.operacoes.push(operacao);
								break;
							}
						}
						$scope.iOperacao = -1;

						$scope.progress.start("Processando Assinatura Digital",
								6 + 6);
						$scope.validarAuthKey($scope.progress, $scope.executar);
					}

					$scope.assinarDocumentos = function(progress) {
						$scope.identificarOperacoes();
						$scope.iOperacao = -1;

						var tipo = $scope.verificarTipoDeAssinatura();
						if (tipo == 0)
							return;

						progress.start($scope.PROCESSING,
								$scope.operacoes.length * 6 + 6);
						$scope.validarAuthKey(progress, $scope.executar);
					}

					$scope.executar = function(progress) {
						if (!$scope.progress.active)
							return;

						for (i = $scope.iOperacao + 1,
								len = $scope.operacoes.length; i < len; i++) {
							var o = $scope.operacoes[i];
							if (!o.enabled)
								continue;
							$scope.iOperacao = i;

							window.setTimeout(function() {
								$scope.assinar({
									nome : o.nome,
									codigo : o.codigo,
									system : o.system
								}, progress);
							}, 10);
							return;
						}
					}

					$scope.assinar = function(state, progress) {
						if (progress.active)
							$scope.obterHash(state, progress);
					}

					$scope.obterHash = function(state, progress) {
						progress.step(state.nome + ": Buscando no servidor...");

						$http({
							url : $scope.urlBaseAPI + "/hash",
							method : "POST",
							data : {
								system : state.system,
								id : state.codigo,
								certificate : $scope.cert.certificate,
								subject : $scope.cert.subject,
								authkey : $scope.getAuthKey()
							}
						}).then(
								function successCallback(response) {
									progress.step(state.nome
											+ ": Encontrado...");
									var data = response.data;
									state.policy = data.policy;
									state.policyversion = data.policyversion;
									state.time = data.time;
									state.hash = data.hash;
									state.sha1 = data.sha1;
									state.sha256 = data.sha256;
									state.hash = data.hash;
									if (data.hasOwnProperty('extra'))
										state.extra = data.extra;
									$scope.clearError(state.codigo);
									if (progress.active)
										$scope.produzirAssinatura(state,
												progress);
								},
								function errorCallback(response) {
									progress.step(state.nome
											+ ": Não encontrado...", 4);
									$scope.reportErrorAndResume(state.codigo,
											"obtendo o hash", response);
									$scope.executar(progress);
								});
					}

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
												$scope.executar(progress);
												$scope.gravarAssinatura(state,
														progress);
											}
										},
										function errorCallback(response) {
											progress.step(state.nome
													+ ": Não assinado.", 2);
											$scope.reportErrorAndResume(
													state.codigo, "assinando",
													response);
											$scope.executar(progress);
										});
					}

					$scope.gravarAssinatura = function(state, progress) {
						progress.step(state.nome + ": Gravando assinatura...");

						$http({
							url : $scope.urlBaseAPI + "/save",
							method : "POST",
							data : {
								system : state.system,
								id : state.codigo,
								signature : state.assinaturaB64,
								signkey : state.signkey,
								time : state.time,
								policy : state.policy,
								policyversion : state.policyversion,
								sha1 : state.sha1,
								sha256 : state.sha256,
								certificate : $scope.cert.certificate,
								code : state.nome,
								extra : state.extra
							}
						}).then(
								function successCallback(response) {
									var data = response.data;
									progress.step(state.nome
											+ ": Assinatura gravada.");
									$scope.reportSuccess(state.codigo, data);
								},
								function errorCallback(response) {
									progress.step(state.nome
											+ ": Assinatura não gravada.");
									$scope.reportErrorAndResume(state.codigo,
											"gravando assinatura", response);
								});
					}

					$scope.disable = function(id) {
						for (var i = 0; i < $scope.documentos.length; i++) {
							var doc = $scope.documentos[i];
							if (doc.id == id) {
								doc.disabled = true;
								doc.checked = false;
							}
						}
					}

					$scope.isDisabled = function(id) {
						for (var i = 0; i < $scope.documentos.length; i++) {
							var doc = $scope.documentos[i];
							if (doc.id == id)
								return doc.disabled;
						}
						return true;
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
					$scope.list = function(progress) {
						if ($scope.hasOwnProperty('endpoint')
								&& $scope.endpoint.hasOwnProperty('list')) {
							$scope.update($scope.endpoint.list);
							progress.stop();
							return;
						}
						progress.step("Listando documentos...");
						$http(
								{
									url : $scope.urlBaseAPI + '/list',
									method : "POST",
									data : {
										certificate : $scope.cert.certificate,
										subject : $scope.cert.subject,
										authkey : $scope.getAuthKey(),
										key : $scope.hasOwnProperty('endpoint') ? $scope.endpoint.listkey
												: undefined
									}
								})
								.then(
										function successCallback(response) {
											var data = response.data;
											$scope.setError();
											for ( var property in data) {
												if (data
														.hasOwnProperty(property)) {
													if (property
															.indexOf("status-") == 0) {
														var system = property
																.substring(7);
														if (data[property] == "OK") {
															delete $scope.errorDetails[system];
														} else if (data[property] == "Error") {
															$scope.errorDetails[system] = {
																errormsg : data["errormsg-"
																		+ system],
																errordetails : [ {
																	stacktrace : data["stacktrace-"
																			+ system],
																	context : "listar documentos",
																	service : system
																} ]
															};
														}
													}
												}
											}
											if (progress.active)
												$scope.update(data.list);
											progress
													.step("Lista de documentos recebida.");
											progress.stop();
										}, function errorCallback(response) {
											delete $scope.documentos;
											progress.stop();
											$scope.setError(response);
										});
					}

					$scope.update = function(l) {
						$scope.lastUpdate = new Date();
						var d = $scope.lastUpdate;
						$scope.lastUpdateFormatted = "Última atualização: "
								+ ("0" + d.getDate()).substr(-2) + "/"
								+ ("0" + (d.getMonth() + 1)).substr(-2) + "/"
								+ d.getFullYear() + " "
								+ ("0" + d.getHours()).substr(-2) + ":"
								+ ("0" + d.getMinutes()).substr(-2) + ":"
								+ ("0" + d.getSeconds()).substr(-2);
						var prev = {};
						if ($scope.documentos == undefined)
							$scope.documentos = [];
						for (var i = 0; i < $scope.documentos.length; i++) {
							prev[$scope.documentos[i].id] = $scope.documentos[i];
						}
						var next = {};
						for (var i = 0; i < l.length; i++) {
							next[l[i].id] = l[i];
							if (!prev.hasOwnProperty(l[i].id)) {
								// insert
								l[i].checked = $scope.checkall;
								$scope.documentos.push(l[i])
								prev[l[i].id] = l[i];
							}
						}
						for (var i = 0; i < $scope.documentos.length; i++) {
							if (!next.hasOwnProperty($scope.documentos[i].id)) {
								if ($scope.isDisabled($scope.documentos[i].id))
									continue;
								// remove
								$scope.documentos[i].checked = false;
								var sts = '<span class="status-removed" data-toggle="tooltip" title="Não está mais disponível para ser assinado.">&#10007;</span>';
								$('#status' + $scope.documentos[i].id)
										.html(sts);
								$scope.disable($scope.documentos[i].id);
							}
						}

					}

					$scope.isSecure = function() {
						return $location.protocol() == "https";
					}

					// 4 steps
					$scope.obterToken = function(progress, cont) {
						// Obter string para ser assinada
						$http({
							url : $scope.urlBaseAPI + '/token',
							method : "POST",
							data : {
								"certificate" : $scope.cert.certificate
							}
						})
								.then(
										function successCallback(response) {
											var data = response.data;
											progress
													.step("Senha de autenticação preparada.");
											var token = data.token;
											progress
													.step("Autenticando usuário");

											// Assinar string para formar o
											// token
											$scope
													.myhttp(
															{
																url : $scope.urlBluCRESTSigner
																		+ '/token',
																method : "POST",
																data : {
																	"certificate" : $scope.cert.certificate,
																	"token" : token,
																	"subject" : $scope.cert.subject,
																	"policy" : "AD-RB"
																}
															})
													.then(
															function successCallback(
																	response) {
																var data = response.data;
																progress
																		.step("Usuário autenticado.");
																var token = data.token
																		+ ";"
																		+ data.sign;

																// Armazenar o
																// token e obter
																// a authkey
																$http(
																		{
																			url : $scope.urlBaseAPI
																					+ '/auth',
																			method : "POST",
																			data : {
																				"token" : token
																			}
																		})
																		.then(
																				function successCallback(
																						response) {
																					var data = response.data;
																					progress
																							.step("Chave de autenticação obtida.");
																					$scope
																							.setAuthKey(data.authkey);
																					cont(progress);
																				},
																				function errorCallback(
																						response) {
																					delete $scope.documentos;
																					progress
																							.stop();
																					$scope
																							.setError(response);
																				});
															},
															function errorCallback(
																	response) {
																delete $scope.documentos;
																progress.stop();
																$scope
																		.setError(response);
															});
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
							progress.step("Chave de autenticação inexistente", 1);
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
							cont(progress);
						}, function errorCallback(response) {
							progress.step("Chave de autenticação inválida.");
							$scope.obterToken(progress, cont);
						});
					}

					// 3 steps
					$scope.buscarCertificado = function(progress) {
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
											if (data.subject !== null) {
												progress
														.step(
																"Certificado corrente localizado.",
																2);
												$scope.setCert(data);
												$scope.validarAuthKey(progress,
														$scope.list);
											} else {
												progress
														.step("Selecionando certificado...");
												$scope
														.myhttp(
																{
																	url : $scope.urlBluCRESTSigner
																			+ '/cert',
																	method : "GET"
																})
														.then(
																function successCallback(
																		response) {
																	var data = response.data;
																	progress
																			.step("Certificado selecionado.");
																	if (data
																			.hasOwnProperty('errormsg')
																			&& data.errormsg != null) {
																		delete $scope.documentos;
																		progress
																				.stop();
																		$scope
																				.setError(response);
																		return;
																	}
																	$scope
																			.setCert(data);
																	$scope
																			.validarAuthKey(
																					progress,
																					$scope.list);
																},
																function errorCallback(
																		response) {
																	delete $scope.documentos;
																	progress
																			.stop();
																	$scope
																			.setError(response);
																});
											}
										}, function errorCallback(response) {
											delete $scope.documentos;
											progress.stop();
											$scope.setError(response);
										});
					}

					// 2 steps
					$scope.testarSigner = function(progress) {
						progress.step("Testando Assijus.exe");
						$scope
								.myhttp({
									// url : '/api/bluc-rest-signer/test.json',
									url : $scope.urlBluCRESTSigner + '/test',
									method : "GET"
								})
								.then(
										function successCallback(response) {
											progress
													.step("Assijus.exe está ativo.");
											if (response.data.status == "OK") {
												$scope
														.buscarCertificado(progress);
											} else {
												progress.stop();
												$scope
														.setError($scope.errorMsgMissingSigner)
											}
										},
										function errorCallback(response) {
											var data = response.data;
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
					}

					$scope.autoRefresh = function() {
						if (!$scope.progress.active
								&& !$scope.noProgress.active) {
							// $scope.noProgress.start("Inicializando", 12);
							$scope.testarSigner($scope.noProgress);
						}
					}

					$scope.forceRefresh = function() {
						delete $scope.documentos;
						delete $scope.lastUpdateFormatted;
						$scope.progress.start("Inicializando", 14);
						$scope.testarSigner($scope.progress);
					}

					$timeout($scope.forceRefresh, 10);
				});

app
		.directive(
				'modal',
				function($parse) {
					return {
						template : '<div class="modal fade">'
								+ '<div class="modal-dialog">'
								+ '<div class="modal-content">'
								+ '<div class="modal-header">'
								+ '<button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>'
								+ '<h4 class="modal-title">{{ title }}</h4>'
								+ '</div>'
								+ '<div class="modal-body" ng-transclude></div>'
								+ '<div class="modal-footer"><button type="button" class="btn btn-default" data-dismiss="modal">Cancelar</button></div>'
								+ '</div>' + '</div>' + '</div>',
						restrict : 'E',
						transclude : true,
						replace : true,
						scope : {
							title : '@',
							visible : '=',
							onSown : '&',
							onHide : '&'
						},
						link : function postLink(scope, element, attrs) {

							$(element).modal({
								show : false,
								keyboard : attrs.keyboard,
								backdrop : attrs.backdrop,
								title : attrs.title
							});

							scope.$watch(function() {
								return scope.visible;
							}, function(value) {

								if (value == true) {
									$(element).modal('show');
								} else {
									$(element).modal('hide');
								}
							});

							$(element).on('show.bs.modal', function() {
								scope.onSown({});
							});

							$(element).on(
									'hide.bs.modal',
									function() {
										scope.onHide({});
										$parse(attrs.visible).assign(
												scope.$parent, false);
										if (!scope.$parent.$$phase
												&& !scope.$root.$$phase)
											scope.$parent.$apply();
									});
						}
					};
				});