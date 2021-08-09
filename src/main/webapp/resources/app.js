var app = angular.module('app', [ 'angularModalService', 'ngAnimate',
		'ngRoute', 'cgBusy' ]);

app.config([ '$routeProvider', '$locationProvider',
		function($routeProvider, $locationProvider) {
			$routeProvider.when('/home', {
				templateUrl : 'resources/home.html',
				controller : 'ctrl'
			}).when('/autenticar-form', {
				templateUrl : 'resources/autenticar-form.html',
				controller : 'authCtrl'
			}).when('/autenticar/:system/:id/:secret', {
				templateUrl : 'resources/autenticar.html',
				controller : 'authCtrl'
			}).when('/login/:logincallback*', {
				templateUrl : 'resources/home.html',
				controller : 'ctrl'
			}).when('/sugestoes', {
				templateUrl : 'resources/sugestoes.html',
				controller : 'ctrl2'
			}).when('/sobre', {
				templateUrl : 'resources/sobre.html',
				controller : 'ctrl2'
			}).when('/instalacao', {
				templateUrl : 'resources/instalacao.html',
				controller : 'ctrl2'
			}).when('/utilizacao', {
				templateUrl : 'resources/comousar.html',
				controller : 'ctrl2' 
			}).when('/instalacao-a1', {
				templateUrl : 'resources/instalacao-a1.html',
				controller : 'ctrl2' 
			}).otherwise({
				redirectTo : '/home'
			});
			// enable html5Mode for pushstate ('#'-less URLs)
			$locationProvider.html5Mode(false);
		} ]);

app
		.controller(
				'routerCtrl',
				function($scope, $http, $window, $q, $location) {
					$scope.assijusexe = "assijus-v0-9-3.exe";
					$scope.isMacOs = navigator.platform.toUpperCase().indexOf('MAC')>=0;

					$scope.parseLocation = function(location) {
						var pairs = location.substring(1).split("&");
						var obj = {};
						var pair;
						var i;

						for (i in pairs) {
							if (pairs[i] === "")
								continue;

							var idx = pairs[i].indexOf("=");
							obj[decodeURIComponent(pairs[i].substring(0, idx))] = decodeURIComponent(pairs[i]
									.substring(idx + 1));
						}

						return obj;
					};

					$scope.querystring = $scope
							.parseLocation($window.location.search);

					$scope.myhttp = function(conf) {
						// The ID of the extension we want to talk to.
						var editorExtensionId = "ifabfihopbhogohngopafekijckmpmka";
						// if ($location.absUrl().indexOf("//localhost/")
						// !== -1)
						// editorExtensionId =
						// "lnifncldepnkbfaedkdkcmbfbbfhhchm";
						var deferred = $q.defer();
						
						if (chrome.runtime !== undefined) {
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
						} else { 
							deferred.reject(undefined);
							return deferred.promise;
						}

					};

					$http({
						url : 'api/v1/test?skip=all',
						method : "GET"
					}).then(function successCallback(response) {
						$scope.test = response.data;
					}, function errorCallback(response) {
					});
					
					$scope.versionAssijusNativeClient = "0";
					$scope.myhttp({
						url : $scope.urlBluCRESTSigner + '/test-extension',
						method : "GET"
					}).then(function successCallback(response) {
						$scope.versionAssijusChromeExtension = response.data.version;

						if ($scope.versionAssijusChromeExtension != "0"
							&& $scope.versionAssijusNativeClient == "0") {
							$scope.myhttp({
								url : $scope.urlBluCRESTSigner + '/test',
								method : "GET"
							}).then(function successCallback(response) {
								$scope.versionAssijusNativeClient = response.data.version;
								
								$http({ 
									url : 'api/v1/test?skip=all',
									method : "GET"
								}).then(function successCallback(response) {
									$scope.test = response.data;

									//Version Extension control
									if ($scope.isMacOs) {
										$scope.versionMacOsDesatualizada = false; 
										if ($scope.test.properties["assijus.extensao.macos.version"] !== undefined) {
											$scope.versionMacOsLatest = $scope.formatProperty($scope.test.properties["assijus.extensao.macos.version"]);
											if ($scope.versionMacOsLatest.indexOf($scope.versionAssijusNativeClient) === -1 ) {
												$scope.versionMacOsDesatualizada = true; 
											}	
										}		
									} else {
										$scope.versionWinDesatualizada = false;
										if ($scope.test.properties["assijus.extensao.windows.version"] !== undefined) {
											$scope.versionWinLastest = $scope.formatProperty($scope.test.properties["assijus.extensao.windows.version"]);
											if ($scope.versionWinLastest.indexOf($scope.versionAssijusNativeClient) === -1 ) {
												$scope.versionWinDesatualizada = true; 
											}	
										}
									}
									//---- END ----
	
								}, function errorCallback(response) {
								});
							}, function errorCallback(response) {
								$scope.versionAssijusNativeClient = "-";
							});
						}
					}, function errorCallback(response) {
						$scope.versionAssijusChromeExtension = "0";
					});
					
					$scope.formatProperty = function(property) {
						var propertyFormatted = "";

						if (property !== undefined && property != null && property !== "") {
							propertyFormatted = property
								.replace("[default: ", "")
								.replace("]", "")
								.replace("[undefined]", "");
						}

						return propertyFormatted;
					}
					
					$('ul.navbar-nav > li ').click(function() {
					    $('ul.navbar-nav > li').removeClass('active');
					    $(this).addClass('active'); 
					}); 
				});

app
		.controller(
				'authCtrl',
				function($scope, $http, $sce, $routeParams) {
					$scope.system = $routeParams.system;
					$scope.id = $routeParams.id;
					$scope.secret = $routeParams.secret;
					$scope.link = 'api/v1/view/' + $scope.system + '/'
							+ $scope.id + '/' + $scope.secret;
					$scope.verify = function(signature) {
						signature.status = $sce
								.trustAsHtml('<span class="status-alert" data-toggle="tooltip" title="Verificando...">&#8987;</span>');

						$http({
							url : 'api/v1/verify',
							method : "POST",
							data : {
								system : $scope.system,
								id : $scope.id,
								ref : signature.ref
							}
						})
								.then(
										function successCallback(response) {
											var data = response.data;
											signature.signer = data.cn;
											signature.status = data.status;
											if (signature.status == 'GOOD') {
												signature.status = $sce
														.trustAsHtml('<span class="status-ok" data-toggle="tooltip" title="Assinatura, OK!">&#10003;</span>');
											}
											signature.download = $sce
													.trustAsHtml('<a href="">'
															+ data.policy
															+ (data.policyversion ? ' v'
																	+ data.policyversion
																	: '')
															+ '</a>');
										}, function errorCallback(response) {
											signature.status = "Erro";
										});

					}

					document.getElementById("pdf").innerHTML = '<iframe src="'
							+ $scope.link
							+ '" width="100%" height="700" align="center" style="margin-top: 10px;"></iframe>';
					document.getElementById("pdf-download").innerHTML = '<a href="'
							+ $scope.link + '" download="documento">PDF</a>';

					$http(
							{
								url : 'api/v1/info/' + $scope.system + '/'
										+ $scope.id + '/' + $scope.secret,
								method : "GET"
							}).then(function successCallback(response) {
						var data = response.data;
						$scope.status = data.status;
						$scope.movement = data.movement;
						$scope.signature = data.signature;
						if ($scope.signature !== undefined)
							for (var i = 0; i < $scope.signature.length; i++) {
								$scope.verify($scope.signature[i]);
							}
					}, function errorCallback(response) {
					});
				});

app.controller('ctrl2', function($scope, $http, $interval, $window) {
	$http({
		url : 'api/v1/test?skip=all',
		method : "GET"
	}).then(function successCallback(response) {
		$scope.test = response.data;

		if ($scope.test.properties["assijus.siga.url"] !== undefined)
			$scope.sigaUrl = $scope.formatProperty($scope.test.properties["assijus.siga.url"]);
	
		if ($scope.test.properties["assijus.dotnet.download.url"] !== undefined)
			$scope.dotNetUrl =  $scope.formatProperty($scope.test.properties["assijus.dotnet.download.url"]);
			
		if ($scope.test.properties["assijus.java8.download.url"] !== undefined)
			$scope.javatUrl =  $scope.formatProperty($scope.test.properties["assijus.java8.download.url"]);

	}, function errorCallback(response) {
	});
	
});

app
		.controller(
				'ctrl',
				function($scope, $http, $interval, $window, $location, $filter,
						$timeout, $routeParams, ModalService) {
							
						$http({
							url : 'api/v1/test?skip=all',
							method : "GET"
						}).then(function successCallback(response) {
							$scope.test = response.data;
							if ($scope.test.properties["assijus.siga.url"] !== undefined)
								$scope.sigaUrl =  $scope.formatProperty($scope.test.properties["assijus.siga.url"]);
					
							if ($scope.test.properties["assijus.dotnet.download.url"] !== undefined)
								$scope.dotNetUrl =  $scope.formatProperty($scope.test.properties["assijus.dotnet.download.url"]);
								
							if ($scope.test.properties["assijus.java8.download.url"] !== undefined)
								$scope.javatUrl =  $scope.formatProperty($scope.test.properties["assijus.java8.download.url"]);

								$scope.apresentarTitulo = function() {
									if ($scope.test.properties["assijus.exibe.titulo.dr"] !== undefined)
										return  $scope.formatProperty($scope.test.properties["assijus.exibe.titulo.dr"]) === "true";
								}
						}, function errorCallback(response) {
						});

					$scope.isChromeExtensionActive = function() {
						return document.getElementById("chrome-extension-active").value != "0";
					}
					


					$scope.PROCESSING = "Processando Assinaturas Digitais";
					$scope.urlBluCRESTSigner = "http://localhost:8612";

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
						if ($scope.endpoint.callback !== undefined)
							$scope.endpoint.callback = $scope.endpoint.callback.replace('__hashsign__', '#')
						$scope.endpoint.autostart = $scope.$parent.querystring.endpointautostart == "true";
					}
					$scope.urlBaseAPI = "/assijus/api/v1";
					$scope.showPIN = false;

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
						
						if (window.wootric !== undefined) {
							window.wootricSettings = {
								email : cn,
								created_at : 1234567890,
								account_token : 'NPS-0f40366d'
							};
							window.wootric('run');
						}
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
							$scope.progressbarTitle = '';
							$scope.progressbarWidth = this.maxwidth;
							if (this.maxwidth == 100) 
								$timeout(function() {
									$scope.progressbarWidth = 0;
								}, 500);
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
					
					$scope.permiteLogout = function() {
						return $scope.clearCurrentCertificateEnable === true;  
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
									segredo : doc.secret,
									nome : doc.code,
									extra : doc.extra,
									enabled : true,
								};
								$scope.operacoes.push(operacao);
							}
						}
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
									segredo : doc.secret,
									nome : doc.code,
									extra : doc.extra,
									enabled : true,
								};
								$scope.operacoes.push(operacao);
								break;
							}
						}
						$scope.iOperacao = -1;

						$scope.progress.start("Processando Assinatura Digital",
								6 + 6, 0, 100);

						if ($scope.endpoint)
							$scope.endpoint.usecallback = false;
						$scope.validarAuthKey($scope.progress, $scope.executar);
					}

					$scope.assinarDocumentos = function(progress) {
						$scope.identificarOperacoes();
						$scope.iOperacao = -1;

						var tipo = $scope.verificarTipoDeAssinatura();
						if (tipo == 0)
							return;

						if ($scope.endpoint && $scope.endpoint.autostart)
							progress.start($scope.PROCESSING,
									$scope.operacoes.length * 6 + 6, 20, 100);
						else
							progress.start($scope.PROCESSING,
									$scope.operacoes.length * 6 + 6, 0, 100);
						if ($scope.endpoint && $scope.endpoint.callback)
							$scope.endpoint.usecallback = true;
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
									segredo : o.segredo,
									system : o.system,
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

					$scope.obterHash = function(state, progress) {
						progress.step(state.nome + ": Buscando no servidor...");

						$http({
							url : $scope.urlBaseAPI + "/hash",
							method : "POST",
							data : {
								system : state.system,
								id : state.codigo,
								secret : state.segredo,
								extra : state.extra,
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
									logEvento("erro", "obtendo o hash",
											state.system);
									$scope.reportErrorAndResume(state.codigo,
											"obtendo o hash", response);
									if ($scope.endpoint)
										$scope.endpoint.usecallback = false;
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
						})
								.then(
										function successCallback(response) {
											var data = response.data;
											progress.step(state.nome
													+ ": Assinatura gravada.");
											logEvento("assinatura", "assinar",
													state.system);
											$scope.reportSuccess(state.codigo,
													data);
											if (!progress.active
													&& $scope.endpoint
													&& $scope.endpoint.usecallback
													&& $scope.endpoint.callback) {
												ModalService
														.showModal(
																{
																	templateUrl : "resources/dialog-callback.html",
																	controller : "PINController",
																	inputs : {
																		title : "---",
																		errormsg : "---"
																	}
																})
														.then(
																function(modal) {
																	modal.element
																			.modal();
																});
												window.location.href = $scope.endpoint.callback;
												return;
											}
											;
										},
										function errorCallback(response) {
											progress
													.step(state.nome
															+ ": Assinatura não gravada.");
											logEvento("erro",
													"gravando assinatura",
													state.system);
											$scope.reportErrorAndResume(
													state.codigo,
													"gravando assinatura",
													response);
											if ($scope.endpoint)
												$scope.endpoint.usecallback = false;
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
							if ($scope.endpoint.autostart)
								$scope.assinarDocumentos($scope.progress);
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
											if (data.hasOwnProperty("status")) {
												for (var i = 0; i < data.status.length; i++) {
													var sts = data.status[i];
													if (!sts
															.hasOwnProperty("errormsg")) {
														delete $scope.errorDetails[sts.system];
													} else {
														$scope.errorDetails[sts.system] = {
															errormsg : sts.errormsg,
															errordetails : [ {
																stacktrace : sts.stacktrace,
																context : "listar documentos",
																service : sts.system
															} ]
														};
														logEvento("erro",
																"listando",
																sts.system);
													}
												}
											}
											if (progress.active)
												$scope.update(data.list);
											progress
													.step("Lista de documentos recebida.");
											progress.stop();
											if ($scope
													.hasOwnProperty('endpoint')
													&& $scope.endpoint.autostart)
												$scope
														.assinarDocumentos($scope.progress);
											logEvento("listagem", "listar");
											return;
										},
										function errorCallback(response) {
											delete $scope.documentos;
											progress.stop();
											$scope.setError(response);
											logEvento("erro", "listando",
													"todos");
										});
					}

					$scope.update = function(l) {
						$scope.lastUpdate = new Date();
						var d = $scope.lastUpdate;
						$scope.lastUpdateFormatted = ""
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

					// 2 steps
					$scope.assinarToken = function(token, progress, cont) {
						$scope.assertCont(cont);
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

							// Armazenar o
							// token e obter
							// a authkey
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
						$scope.assertCont(cont);
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
						$scope.assertCont(cont);
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
							cont(progress);
						}, function errorCallback(response) {
							progress.step("Chave de autenticação inválida.");
							$scope.obterToken(progress, cont);
						});
					}
					
					$scope.logout = function(progress) {
					
						$scope.myhttp({
							url : $scope.urlBluCRESTSigner + '/clearcurrentcert',
							method : "POST",
						}).then(function successCallback(response) {	
							delete $scope.cert;
							delete $scope.keystore;
							delete $scope.userSubject;
							delete $scope.userPIN;
							delete $scope.authkey;
							$scope.forceRefresh();
							console.log(response.data.errormsg);
						},	function errorCallback(response) {
							console.log(response.data.errormsg);
						});
					}

					// 2 steps
					$scope.selecionarCertificado = function(progress, cont) {
						$scope.assertCont(cont);
						progress.step("Selecionando certificado...");
						
						if ($scope.keystore == null) {
							if ($scope.keystoreSupported != null) {
								if ($scope.keystoreSupported.length > 1) {
									$scope.showDialogForKeytore($scope.keystoreSupported,cont);
									progress.stop();
									return;
								} else {
									$scope.keystore == $scope.keystoreSupported[0];
								}
							}
						} 
						
						if (isPkcsEnabled($scope.keystore) && !$scope.hasOwnProperty('userPIN')) {
							$scope.showDialogForPIN(undefined, cont);
							progress.stop();
							return;
						}

						$scope.myhttp({
									url : $scope.urlBluCRESTSigner + '/cert',
									method : "POST",
									data : {
										userPIN : $scope.userPIN,
										subject : $scope.userSubject,
										keystore : $scope.keystore
									}
								})
								.then(
										function successCallback(response) {
											var data = response.data;

											if (data && data.list) {
												$scope.showDialogForCerts(data.list,cont);
												progress.stop();
												return;
											}

											progress.step("Certificado selecionado.");
											if (data.hasOwnProperty('errormsg')
													&& data.errormsg != null) {
												delete $scope.documentos;
												progress.stop();
												$scope.setError(response);
												return;
											}
											$scope.setCert(data);
											$scope.validarAuthKey(progress,cont);
										},
										function successCallback(response) {
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
												$scope.showDialogForPIN(err);
												return;
											}
											$scope.setError(response);
										});
					}

					$scope.prosseguirComPIN = function(userPIN, cont) {
						$scope.assertCont(cont);
						if ((userPIN || "") == "") {
							delete $scope.userPIN;
							$scope.setError("PIN não informado. ");
							return;
						}
						$scope.userPIN = userPIN;
						if ($scope.hasOwnProperty('userPIN')) {
							$scope.progress.start("Inicializando", 10);
							$scope.selecionarCertificado($scope.progress, cont);
						}
					}

					$scope.showDialogForPIN = function(errormsg, cont) {
						$scope.assertCont(cont);
						ModalService.showModal({
							templateUrl : "resources/dialog-pin.html",
							controller : "PINController",
							inputs : {
								title : "Autenticação",
								errormsg : errormsg
							}
						}).then(function(modal) {
							modal.element.modal();
							modal.close.then(function(result) {
								$scope.prosseguirComPIN(result.pin, cont);
							});
						});
					};

					$scope.prosseguirComCertificado = function(userSubject,cont) {
						$scope.assertCont(cont);
						if ((userSubject || "") == "") {
							delete $scope.userSubject;
							return;
						}
						$scope.userSubject = userSubject;
						if ($scope.hasOwnProperty('userSubject')) {
							$scope.progress.start("Inicializando", 10);
							$scope.selecionarCertificado($scope.progress, cont);
						}
					}

					$scope.showDialogForCerts = function(list, cont) {
						$scope.assertCont(cont);
						ModalService.showModal({
							templateUrl : "resources/dialog-certs.html",
							controller : "CertsController",
							inputs : {
								title : "Seleção de Certificado",
								list : list
							}
						}).then(
								function(modal) {
									modal.element.modal();
									$('#modalDialogCerts').on('shown.bs.modal', function () {
									    $('#certificadoList a').on('click', function (e) {
										  	e.preventDefault();
											$('#selectCert').val($(this).attr("data-cert"));     
										}); 
									});
									modal.close.then(function(result) {
										if (result.cert == null) {
											$scope.setError("Nenhum certificado selecionado");
											return;
										} else {
											$scope.setCert(JSON.parse(result.cert));
										}
										$scope.prosseguirComCertificado($scope.cert.subject, cont);
									});
								});
					};
					
					
					$scope.showDialogForKeytore = function(list, cont) {
						$scope.assertCont(cont);
						ModalService.showModal({
							templateUrl : "resources/dialog-keystore.html",
							controller : "KeystoreController",
							inputs : {
								title : "Tipo de Certificado",
								list : list
							}
						}).then(function(modal) {
							modal.element.modal(); 
							$('#modalDialogKeystore').on('shown.bs.modal', function () {
								$('#keystoreList a').on('click', function (e) {
								  	e.preventDefault(); 
									$('#selectKeystore').val($(this).attr("data-keystore"));  
								});
							});
							modal.close.then(function(result) { 
								if (result.keystore == null || result.keystore == "") {
									$scope.setError("Nenhum certificado selecionado");
									return;
								}
								$scope.keystore = result.keystore;
								$scope.progress.start("Inicializando...", 10);
								$scope.selecionarCertificado($scope.progress, cont);
							});
						});
					};

					// 3 steps
					$scope.buscarCertificadoCorrente = function(progress, cont) {
						$scope.assertCont(cont);
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
												if (isPkcsEnabled($scope.keystore) && !$scope.hasOwnProperty('userPIN')) {
													$scope.showDialogForPIN(undefined, cont);
													progress.stop();
													return;
												}
												$scope.selecionarCertificado(progress, cont);
											}
										}, function errorCallback(response) {
											delete $scope.documentos;
											progress.stop();
											$scope.setError(response);
										});
					}

					// 2 steps
					$scope.testarSigner = function(progress, cont) {
						$scope.assertCont(cont);
						progress.step("Testando Assijus.exe");
						$scope
								.myhttp({
									// url : '/api/bluc-rest-signer/test.json',
									url : $scope.urlBluCRESTSigner + '/test',
									method : "GET"
								})
								.then(
										function successCallback(response) {
											progress.step("Assijus.exe está ativo.");
											if (response.data.status == "OK") {
												$scope.keystoreSupported = response.data.keystoreSupported;
												$scope.clearCurrentCertificateEnable = response.data.clearCurrentCertificateEnabled;
												document.getElementById("native-client-active").value = response.data.version;
												$scope.buscarCertificadoCorrente(progress, cont);
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
					}

					$scope.assertCont = function(cont) {
						if (typeof cont !== 'function')
							throw "continuação deve ser informada";
					}

					$scope.autoRefresh = function() {
						if (!$scope.progress.active
								&& !$scope.noProgress.active) {
							// $scope.noProgress.start("Inicializando", 12);
							$scope.testarSigner($scope.noProgress, $scope.list);
						}
					}

					$scope.forceRefresh = function() {
						delete $scope.documentos;
						delete $scope.lastUpdateFormatted;
						if ($scope.endpoint && $scope.endpoint.autostart)
							$scope.progress.start("Inicializando", 14, 0, 20);
						else
							$scope.progress.start("Inicializando", 14, 0, 100);
						$scope.testarSigner($scope.progress, $scope.list);
					}

					// 3 steps
					$scope.login = function(progress) {
						progress.step("Gerando JSON Web Token...");
						$http({
							url : $scope.urlBaseAPI + '/login',
							method : "POST",
							data : {
								"authkey" : $scope.getAuthKey(),
								"callback" : $routeParams.logincallback
							}
						})
								.then(
										function successCallback(response) {
											progress.step("JWT gerado.");
											progress
													.step("Login realizado. Redirecionando...");
											$window.location.href = response.data.url;
										}, function errorCallback(response) {
											progress.stop();
											$scope.setError(response);
										});
					}

					$scope.startLogin = function() {
						// Um passo a mais para ficar mostrando o redirect na
						// progressbar.
						$scope.progress.start("Processando Login", 16, 0, 100);
						$scope.testarSigner($scope.progress, $scope.login);
					}
					
					if ($routeParams.logincallback) {
						$timeout($scope.startLogin, 10);
					} else {
						$timeout($scope.forceRefresh, 10);
					}
					
					function isPkcsEnabled(keystoreSupported) {
						if (keystoreSupported !== undefined)
							return keystoreSupported.indexOf("PKCS") !== -1; 
						return false;
					}
					
					function isAppleKeystoreEnabled(keystoreSupported) {
						if (keystoreSupported !== undefined)
							return keystoreSupported.indexOf("APPLE") !== -1; 
						return false;
					}
					
					function isMsCapiKeystoreEnabled(keystoreSupported) {
						if (keystoreSupported !== undefined)
							return keystoreSupported.indexOf("MSCAPI") !== -1; 
						return false;
					}
				});
				
app.controller('PINController', function($scope, $element, $timeout, title,
		errormsg, close) {

	$scope.pin = null;
	$scope.title = title;
	$scope.errormsg = errormsg;

	$scope.clickclose = function() {
		if (($scope.pin || "") == "") {
			$scope.errormsg = "PIN deve ser preenchido.";
		}
		$scope.close();
		// Manually hide the modal.
		$element.modal('hide');
	};

	// This close function doesn't need to use jQuery or bootstrap, because
	// the button has the 'data-dismiss' attribute.
	$scope.close = function() {
		if (($scope.pin || "") == "") {
			$scope.errormsg = "PIN deve ser preenchido.";
		}
		close({
			pin : $scope.pin
		}, 500); // close, but give 500ms for bootstrap to animate
	};

	// This cancel function must use the bootstrap, 'modal' function because
	// the doesn't have the 'data-dismiss' attribute.
	$scope.cancel = function() {

		// Manually hide the modal.
		$element.modal('hide');

		// Now call close, returning control to the caller.
		close({
			name : $scope.name
		}, 500); // close, but give 500ms for bootstrap to animate
	};

});

app.controller('KeystoreController', function($scope, $element, $timeout, title, list, close) {

	$scope.keystore = null;
	$scope.title = title;
	$scope.list = list;
	
	keystore = list[0].value;

	$scope.clickclose = function() {
		$scope.close();
		// Manually hide the modal.
		$element.modal('hide');
	};

	// This close function doesn't need to use jQuery or bootstrap, because
	// the button has the 'data-dismiss' attribute.
	$scope.close = function() {
		close({
			keystore : selectKeystore.value
		}, 500); // close, but give 500ms for bootstrap to animate
	};
	
	
	$scope.prosseguir = function() {
		if (selectKeystore.value == "") {
			console.log("Load default keystore")
			selectKeystore.value = keystore;
		}
		$scope.close();
	};

	// This cancel function must use the bootstrap, 'modal' function because
	// the doesn't have the 'data-dismiss' attribute.
	$scope.cancel = function() {

		// Manually hide the modal.
		$element.modal('hide');
		selectKeystore.value = "";
		// Now call close, returning control to the caller.
		close({}, 500); // close, but give 500ms for bootstrap to animate
	};

});

app.controller('CertsController', function($scope, $element, $timeout, title,
		list, close) {

	$scope.pin = null;
	$scope.title = title;
	$scope.list = list;

	$scope.clickclose = function() {
		$scope.close();
		// Manually hide the modal.
		$element.modal('hide');
	};

	// This close function doesn't need to use jQuery or bootstrap, because
	// the button has the 'data-dismiss' attribute.
	$scope.close = function() {
		close({
			cert : selectCert.value
		}, 500); // close, but give 500ms for bootstrap to animate
	};
	
	$scope.prosseguir = function() {
		//Load first
		if (selectCert.value == "" && $scope.list !== null) {
			selectCert.value = JSON.stringify($scope.list[0]);
		}
		$scope.close();
	};

	// This cancel function must use the bootstrap, 'modal' function because
	// the doesn't have the 'data-dismiss' attribute.
	$scope.cancel = function() {
		$scope.list = null;
		selectCert.value = "";
		// Manually hide the modal.
		$element.modal('hide');

		// Now call close, returning control to the caller.
		close({}, 500); // close, but give 500ms for bootstrap to animate
	};

});

app.directive(
				'modal',
				function($parse) {
					return {
						template : '<div class="modal">'
								+ '<div class="modal-dialog">'
								+ '<div class="modal-content">'
								+ '<div class="modal-header">'
								+ '<h5 class="modal-title">{{ title }}</h5>'
								+ '<button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>'
								+ '</div>'
								+ '<div class="modal-body" ng-transclude></div>'
								+ '<div class="modal-footer"><button type="button" class="btn btn-secondary" data-dismiss="modal">Cancelar</button></div>'
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
								keyboard : attrs.keyboard == undefined ? true : attrs.keyboard,
								backdrop : attrs.backdrop == undefined ? true : attrs.backdrop,
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