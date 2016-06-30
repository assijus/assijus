var app = angular.module('app', [ 'ngAnimate', 'ngRoute', 'cgBusy' ]);

app.config([ '$routeProvider', '$locationProvider', function($routeProvider, $locationProvider) {
	$routeProvider.when('/home', {
		templateUrl : 'resources/home.html',
		controller : 'ctrl'
	}).when('/sugestoes', {
		templateUrl : 'resources/sugestoes.html',
		controller : 'ctrlSugerir'
	}).when('/sobre', {
		templateUrl : 'resources/sobre.html',
		controller : 'ctrlSugerir'
	}).otherwise({
		redirectTo : '/home'
	});
	// enable html5Mode for pushstate ('#'-less URLs)
	$locationProvider.html5Mode(false);
} ]);

app.controller('routerCtrl', function($scope, $http, $templateCache) {
	$scope.assijusexe = "assijus-v0-91.exe";
});

app.controller('ctrlSugerir', function($scope, $http, $templateCache, $interval, $window) {

	// Sugestoes

	$scope.fdSugerir = function() {
		var obj = {
			nome : $scope.sugestao.nome,
			email : $scope.sugestao.email,
			mensagem : $scope.sugestao.mensagem
		};
		return formdata(obj);
	}

	$scope.sugerir = function() {
		$http({
			url : '/app/sugerir',
			method : "POST",
			data : $scope.fdSugerir(),
			headers : {
				'Content-Type' : 'application/x-www-form-urlencoded'
			}
		}).success(function(data, status, headers, config) {
			alert("Sua mensagem foi enviada. Muito obrigado!");
			$scope.sugestao = {};
		}).error(function(data, status, headers, config) {
			alert(data.errorMessage.error);
		});
	}

});

app.controller('ctrl', function($scope, $http, $templateCache, $interval, $window, $location) {

	var querystring = $location.search();
	if (querystring.hasOwnProperty('urlsigner')) {
		$scope.urlBluCRESTSigner = querystring.urlsigner;
	} else {
		$scope.urlBluCRESTSigner = "http://localhost:8612";
	}

	if ($location.host() == 'localhost') {
		$scope.urlBaseAPI = "/assijus/api/v1";
	} else {
		$scope.urlBaseAPI = "/assijus/api/v1"
	}

	$scope.showErrorDetails = false;
	$scope.filterErrorDetails = function(v) {
		return !v.hasOwnProperty('hideAlert');
	};
	$scope.promise = null;
	$scope.checkall = true;
	$scope.instalarBluC = false;
	$scope.documentos = [];

	$scope.errorDetails = {};

	$scope.progress = {
		active : false,
		csteps : 0,
		isteps : 0,
		start : function(title, steps) {
			$scope.noProgress.stop(); // disable pending updates
			$scope.progressbarTitle = title;
			$scope.progressbarShow = true;
			$scope.progressbarHide = function() {
				$scope.progress.active = false;
			}
			this.active = true;
			this.csteps = steps;
		},
		step : function(caption, skip) {
			this.isteps += 1 + (skip||0);
			console.log(this.csteps + " - " + this.isteps);
			$scope.progressbarWidth = 100 * (this.isteps / this.csteps);
			$scope.progressbarShow = true;
			$scope.progressbarCaption = caption;
			if (this.isteps == this.csteps)
				this.stop();
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

	$scope.assinante = function() {
		if ($scope.assinanteIdentificado()) {
			var cn = $scope.cert.subject;
			cn = cn.split(",")[0];
			cn = cn.split(":")[0];
			cn = cn.replace("CN=", "");
			return cn;
		}
	}

	$scope.documentosCarregados = function() {
		return $scope.hasOwnProperty("documentos") && $scope.documentos.length != 0;
	}

	$scope.zeroDocumentosCarregados = function() {
		return $scope.hasOwnProperty("documentos") && $scope.documentos.length == 0;
	}

	$scope.marcarTodos = function() {
		var checked = $scope.checkall;
		$("input:checkbox.chk-assinar").each(function() {
			if (!$(this).prop('disabled')) {
				$(this).prop('checked', checked);
			}
		});
	}

	// 0 - Nenhuma, 1 = digital
	$scope.verificarTipoDeAssinatura = function() {
		var useToken = false;

		for (var i = 0, len = $scope.operacoes.length; i < len; i++) {
			if ($scope.operacoes[i].enabled) {
				useToken = true;
			}
		}
		return (useToken ? 1 : 0);
	}

	$scope.identificarOperacoes = function() {
		$scope.operacoes = [];

		var NodeList = document.getElementsByTagName("INPUT");
		for (var i = 0, len = NodeList.length; i < len; i++) {
			var Elem = NodeList[i];
			if (Elem.name.substr(0, 9) == "ad_descr_") {
				var operacao = {};

				operacao.codigo = Elem.name.substr(9);
				operacao.nome = document.getElementsByName("ad_descr_" + operacao.codigo)[0].value;
				operacao.urlPdf = document.getElementsByName("ad_url_pdf_" + operacao.codigo)[0].value;
				operacao.urlPost = document.getElementsByName("ad_url_post_" + operacao.codigo)[0].value;
				operacao.urlPostPassword = document.getElementsByName("ad_url_post_password_" + operacao.codigo)[0].value;
				operacao.usePassword = false;

				var oChk = document.getElementsByName("ad_chk_" + operacao.codigo)[0];
				if (oChk == null) {
					operacao.enabled = true;
				} else {
					operacao.enabled = oChk.checked;
				}
				if (operacao.enabled)
					$scope.operacoes.push(operacao);
			}
		}
	}

	//
	// View
	//
	$scope.view = function(doc) {
		$scope.progress.start("Preparando Visualização", 4);
		$scope.obterToken($scope.progress, function(progress) {
			progress.stop();
			var form = document.createElement('form');
			form.action = $scope.urlBaseAPI + "/view";
			form.method = 'POST';
			form.target = '_blank';
			form.style.display = 'none';

			var token = document.createElement('input');
			token.type = 'text';
			token.name = 'token';
			token.value = $scope.token;

			var urlView = document.createElement('input');
			urlView.type = 'text';
			urlView.name = 'urlView';
			urlView.value = doc.urlView;

			var submit = document.createElement('input');
			submit.type = 'submit';
			submit.id = 'submitView';

			form.appendChild(token);
			form.appendChild(urlView);
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
		var operacao = {};

		operacao.codigo = id;
		operacao.nome = document.getElementsByName("ad_descr_" + operacao.codigo)[0].value;
		operacao.urlPdf = document.getElementsByName("ad_url_pdf_" + operacao.codigo)[0].value;
		operacao.urlPost = document.getElementsByName("ad_url_post_" + operacao.codigo)[0].value;
		operacao.urlPostPassword = document.getElementsByName("ad_url_post_password_" + operacao.codigo)[0].value;
		operacao.usePassword = false;
		operacao.enabled = true;
		$scope.operacoes = [ operacao ];
		$scope.iOperacao = -1;

		$scope.progress.start("Processando Assinatura Digital", 6 + 4);
		$scope.obterToken($scope.progress, $scope.executar);
	}

	$scope.assinarDocumentos = function(progress) {
		$scope.identificarOperacoes();
		$scope.iOperacao = -1;

		var tipo = $scope.verificarTipoDeAssinatura();
		if (tipo == 0)
			return;

		progress.start("Processando Assinaturas Digitais", $scope.operacoes.length * 6 + 4);
		$scope.obterToken(progress, $scope.executar);
	}

	$scope.executar = function(progress) {
		if (!$scope.progress.active)
			return;

		for (i = $scope.iOperacao + 1, len = $scope.operacoes.length; i < len; i++) {
			var o = $scope.operacoes[i];
			if (!o.enabled)
				continue;
			$scope.iOperacao = i;

			window.setTimeout(function() {
				$scope.assinar({
					nome : o.nome,
					codigo : o.codigo,
					urlPost : o.urlPost,
					urlHash : o.urlPdf
				}, progress);
			}, 10);
			return;
		}
		// $scope.progress.stop();
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
				urlHash : state.urlHash,
				certificate : $scope.cert.certificate,
				token : $scope.token
			}
		}).success(function(data, status, headers, config) {
			progress.step(state.nome + ": Encontrado...");
			state.policy = data.policy;
			state.policyversion = data.policyversion;
			state.time = data.time;
			state.hash = data.hash;
			state.sha1 = data.sha1;
			state.sha256 = data.sha256;
			state.hash = data.hash;
			if (data.hasOwnProperty('urlSave'))
				state.urlPost = data.urlSave;
			$scope.clearError(state.codigo);
			if (progress.active)
				$scope.produzirAssinatura(state, progress);
		}).error(function(data, status, headers, config) {
			progress.step(state.nome + ": Não encontrado...", 4);
			$scope.reportErrorAndResume(state.codigo, "obtendo o hash", data, status);
			$scope.executar(progress);
		});
	}

	$scope.produzirAssinatura = function(state, progress) {
		progress.step(state.nome + ": Assinando...");

		$http({
			url : $scope.urlBluCRESTSigner + "/sign",
			method : "POST",
			data : {
				policy : state.policy,
				payload : state.hash,
				certificate : $scope.cert.certificate,
				subject : $scope.cert.subject
			}
		}).success(function(data, status, headers, config) {
			progress.step(state.nome + ": Assinado.");
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
				$scope.gravarAssinatura(state, progress);
			}
		}).error(function(data, status, headers, config) {
			progress.step(state.nome + ": Não assinado.", 2);
			$scope.reportErrorAndResume(state.codigo, "assinando", data, status);
			$scope.executar(progress);
		});
	}

	$scope.gravarAssinatura = function(state, progress) {
		progress.step(state.nome + ": Gravando assinatura...");

		$http({
			url : $scope.urlBaseAPI + "/save",
			method : "POST",
			data : {
				signature : state.assinaturaB64,
				signkey : state.signkey,
				time : state.time,
				policy : state.policy,
				policyversion : state.policyversion,
				sha1 : state.sha1,
				sha256 : state.sha256,
				urlSave : state.urlPost,
				certificate : $scope.cert.certificate
			}
		}).success(function(data, status, headers, config) {
			progress.step(state.nome + ": Assinatura gravada.");
			// $('#status' + state.codigo).goTo();
			var sts = '<span class="status-ok">&#10003;</span>';
			if (data.hasOwnProperty('warning')) {
				sts += ' <span class="status-warning">'
				for (var i = 0, len = data.warning.length; i < len; i++) {
					if (i != 0)
						sts += ',';
					sts += data.warning[i].label;
				}
				sts += '</span>';
			}
			$('#status' + state.codigo).html(sts);
			$scope.disable(state.codigo);
			$scope.clearError(state.codigo);
		}).error(function(data, status, headers, config) {
			progress.step(state.nome + ": Assinatura não gravada.");
			$scope.reportErrorAndResume(state.codigo, "gravando assinatura", data, status);
		});
	}

	$scope.disable = function(id) {
		var chk = $("#ad_chk_" + id);
		chk.attr('disabled', 'disabled');
		chk.attr('readonly', 'readonly');
		chk.prop('checked', false);
	}

	$scope.clearError = function(codigo) {
		delete $scope.errorDetails[codigo];
	}

	$scope.reportErrorAndResume = function(codigo, context, data, status) {
		var msg = "Erro " + context + ': ' + status;
		try {
			if (data.hasOwnProperty("error"))
				msg = data.error;
		} catch (err) {

		}

		$scope.errorDetails[codigo] = data;
		$scope.errorDetails[codigo].hideAlert = true;

		// $('#status' + state.codigo).goTo();
		$('#status' + codigo).html('<span class="status-error">' + msg + '</span>');
		$('#details' + codigo).html('<span>' + msg + '</span>');
	}

	$scope.presentError = function(id) {
		$scope.showErrorDetails = true;
		$scope.currentErrorId = id;
	}

	//
	// Initialize
	//

	$scope.list = function(progress) {
		progress.step("Listando documentos...");
		$http({
			url : $scope.urlBaseAPI + '/list',
			method : "POST",
			data : {
				"certificate" : $scope.cert.certificate,
				"token" : $scope.token
			}
		}).success(function(data, status, headers, config) {
			for ( var property in data) {
				if (data.hasOwnProperty(property)) {
					if (property.indexOf("status-") == 0) {
						var system = property.substring(7);
						if (data[property] == "OK") {
							delete $scope.errorDetails[system];
						} else if (data[property] == "Error") {
							$scope.errorDetails[system] = {
								"error" : data["error-" + system],
								"error-details" : data["stacktrace-" + system]
							};
						}
					}
				}
			}
			if (progress.active)
				$scope.update(data.list);
			progress.step("Lista de documentos recebida.");
			progress.stop();
		}).error(function(data, status, headers, config) {
			progress.stop();
			$scope.errorDetails.geral = data;
		});
	}

	$scope.update = function(l) {
		var checked = $("#progress_checkall").prop('checked');
		$scope.lastUpdate = new Date();
		var d = $scope.lastUpdate;
		$scope.lastUpdateFormatted = "Última atualização: " + ("0" + d.getDate()).substr(-2) + "/" + ("0" + (d.getMonth() + 1)).substr(-2) + "/" + d.getFullYear() + " " + ("0" + d.getHours()).substr(-2) + ":" + ("0" + d.getMinutes()).substr(-2) + ":" + ("0" + d.getSeconds()).substr(-2);
		var prev = {};
		for (var i = 0; i < $scope.documentos.length; i++) {
			prev[$scope.documentos[i].id] = $scope.documentos[i];
		}
		var next = {};
		for (var i = 0; i < l.length; i++) {
			next[l[i].id] = l[i];
			if (!prev.hasOwnProperty(l[i].id)) {
				// insert
				l[i].checked = checked;
				$scope.documentos.push(l[i])
				prev[l[i].id] = l[i];
			}
		}
		for (var i = 0; i < $scope.documentos.length; i++) {
			if (!next.hasOwnProperty($scope.documentos[i].id)) {
				// remove
				$scope.documentos[i].checked = false;
				var sts = '<span class="status-removed">&#10007;</span>';
				$('#status' + $scope.documentos[i].id).html(sts);
				$scope.disable($scope.documentos[i].id);
			}
		}

	}

	$scope.obterToken = function(progress, cont) {
		progress.step("Obtendo senha de autenticação...");
		$http({
			url : $scope.urlBaseAPI + '/token',
			method : "POST",
			data : {
				"certificate" : $scope.cert.certificate
			}
		}).success(function(data, status, headers, config) {
			progress.step("Senha de autenticação preparada.");
			var token = data.token;
			progress.step("Autenticando usuário");
			$http({
				url : $scope.urlBluCRESTSigner + '/token',
				method : "POST",
				data : {
					"certificate" : $scope.cert.certificate,
					"token" : token,
					"subject" : $scope.cert.subject,
					"policy" : "AD-RB"
				}
			}).success(function(data, status, headers, config) {
				progress.step("Usuário autenticado.");
				$scope.token = data.token + ";" + data.sign;
				cont(progress);
			}).error(function(data, status, headers, config) {
				progress.stop();
				$scope.errorDetails.geral = data;
			});
		}).error(function(data, status, headers, config) {
			progress.stop();
			$scope.errorDetails.geral = data;
		});
	}

	$scope.buscarCertificado = function(progress) {
		progress.step("Buscando certificado corrente...");
		$http({
			// url : '/api/bluc-rest-signer/cert.json',
			url : $scope.urlBluCRESTSigner + '/currentcert',
			method : "GET"
		}).success(function(data, status, headers, config) {
			progress.step("Certificado corrente localizado.", 2);
			if (data.subject !== null) {
				$scope.cert = data;
				$scope.obterToken(progress, $scope.list);
			} else {
				progress.step("Selecionando certificado...");
				$http({
					url : $scope.urlBluCRESTSigner + '/cert',
					method : "GET"
				}).success(function(data, status, headers, config) {
					progress.step("Certificado selecionado.");
					if (data.hasOwnProperty('errormsg') && data.errormsg != null) {
						progress.stop();
						$scope.errorDetails.geral = {
							error : data.errormsg
						};
						return;
					}
					$scope.cert = data;
					$scope.obterToken(progress, $scope.list);
				}).error(function(data, status, headers, config) {
					progress.stop();
				});
			}
		}).error(function(data, status, headers, config) {
			progress.stop();
		});
	}

	$scope.testarSigner = function(progress) {
		progress.step("Testando Assijus.exe");
		$http({
			// url : '/api/bluc-rest-signer/test.json',
			url : $scope.urlBluCRESTSigner + '/test',
			method : "GET"
		}).success(function(data, status, headers, config) {
			progress.step("Assijus.exe está ativo.");
			if (data.status == "OK") {
				$scope.instalarBluC = false;
				$scope.buscarCertificado(progress);
			} else {
				progress.stop();
				$scope.instalarBluC = true;
				$scope.blucrestsignerInativo = "";
			}
		}).error(function(data, status, headers, config) {
			progress.stop();
			if (typeof data === 'object' && data != null && data.hasOwnProperty('error')) {
				$scope.errorDetails.geral = data;
			} else {
				$scope.instalarBluC = true;
			}
		});
	}

	$scope.autoRefresh = function() {
		// $scope.progress.start("AutoRefresh", 6);
		// $scope.testarSigner($scope.progress);
		if (!$scope.progress.active)
			$scope.testarSigner($scope.noProgress);
	}

	$scope.forceRefresh = function() {
		$scope.progress.start("Inicializando", 12);
		$scope.documentos = [ {
			id : "1",
			code : "2",
			descr : "test"
		} ];
		$scope.documentos = [];
		$scope.testarSigner($scope.progress);
	}

	$scope.forceRefresh();

	$interval($scope.autoRefresh, 3 * 60 * 1000);
});

app.directive('modal', function() {
	return {
		template : '<div class="modal fade">' + '<div class="modal-dialog">' + '<div class="modal-content">' + '<div class="modal-header">' + '<button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>' + '<h4 class="modal-title">{{ title }}</h4>' + '</div>' + '<div class="modal-body" ng-transclude></div>' + '<div class="modal-footer"><button type="button" class="btn btn-default" data-dismiss="modal">Cancelar</button></div>' + '</div>' + '</div>' + '</div>',
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

			$(element).on('hide.bs.modal', function() {
				scope.onHide({});
			});
		}
	};
});