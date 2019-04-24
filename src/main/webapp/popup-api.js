var frameSrc = "https://assijus.trf2.jus.br/assijus/popup.html";

function receiveMessageAssinaturaDigital(event) {
	var iframe = document.getElementById('iframeAssinaturaDigital');
	var iframeWindow = iframe.contentWindow;

	if (event.data.command === '<READY>') {
		iframeWindow.postMessage({
			command : '<GO>',
			docs : window._AssinaturaDigitalParametros.docs
		}, '*');
	}

	if (event.data.command === '<BEGIN-REQUEST>') {
		window._AssinaturaDigitalParametros.beginCallback(function(params) {
			iframeWindow.postMessage({
				command : '<BEGIN-RESPONSE>'
			}, '*');
		})
	}

	if (event.data.command === '<HASH-REQUEST>') {
		window._AssinaturaDigitalParametros.hashCallback(event.data.id,
				function(params) {
					iframeWindow.postMessage({
						command : '<HASH-RESPONSE>',
						params : params
					}, '*');
				})
	}

	if (event.data.command === '<SAVE-REQUEST>') {
		window._AssinaturaDigitalParametros.saveCallback(event.data.id,
				event.data.sign, function(params) {
					iframeWindow.postMessage({
						command : '<SAVE-RESPONSE>',
						params : params
					}, '*');
				})
	}

	if (event.data.command === '<END-REQUEST>') {
		window._AssinaturaDigitalParametros.dismissCallback(function(params) {
			iframeWindow.postMessage({
				command : '<END-RESPONSE>'
			}, '*');
		})
	}

	if (event.data.command === '<SET-HEIGHT>') {
		iframe.style.height = event.data.height;
	}
}

var produzirAssinaturaDigital = function(params) {
	window._AssinaturaDigitalParametros = params;
	window.addEventListener("message", receiveMessageAssinaturaDigital, false);

	if (!params.beginCallback)
		params.beginCallback = function() {
		};

	if (!params.endCallback)
		params.endCallback = function() {
		};

	var popupTemplate = '<div class="modal fade">'
			+ '  <div class="modal-dialog">'
			+ '    <div class="modal-content">'
			+ '       <div class="modal-header">'
			+ '          <h5 class="modal-title">Assinatura Digital</h5>'
			+ '          <button type="button" class="close" data-dismiss="modal" aria-label="Close">'
			+ '            <span aria-hidden="true">&times;</span>'
			+ '          </button>'
			+ '       </div><div class="modal-body">'
			+ '        <iframe id="iframeAssinaturaDigital" src="' + frameSrc
			+ '" width="99.6%" frameborder="0"></iframe></div>';

	var dlg = $(popupTemplate);
	dlg.modal({
		show : true
	});
	
	dlg.on('hidden.bs.modal', function (e) {
		setTimeout(function() {
			dlg.remove();
		}, 1000);
	});

	params.dismissCallback = function() {
		params.endCallback();
		dlg.modal('hide');
		setTimeout(function() {
			dlg.remove();
		}, 1000);
	}

}