var module = function(moduleName) {
	var parts = moduleName.split('\.');
	var parent;
	try {
		parent = eval(parts[0]);
	} catch (e) {
		parent = undefined;
	}
	if (typeof parent==='undefined') {
		window.eval('with (window) {var '+parts[0]+'={};};');
		parent = eval(parts[0]);
	}
	for ( var i = 1; i < parts.length; i++) {
		if (typeof parent[parts[i]]==='undefined') {
			parent[parts[i]] = {};
		}
		parent = parent[parts[i]];
	}
	return parent;
};