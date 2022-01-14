RedGUICV {
	var <view, <savedValue, <ref, <map, <unmap, <args;

	*new {|parent, bounds, ref, map, unmap, args|
		^super.new.init(parent, bounds, ref, map, unmap, args);
	}

	init {|parent, bounds, argRef, argMap, argUnmap, argArgs|
		args= argArgs;
		view= this.prMake(parent, bounds);
		ref= argRef;
		map= argMap ? {|x| x};
		unmap= argUnmap ? {|x| x};
		view.value= unmap.value(ref.value);
		this.prConnect;
	}

	asView {^view}
	interp {|target, val| this.value_(savedValue.blend(target, val))}
	value_ {|val| ref.value_(val).changed(\value)}
	value {^map.value(view.value)}
	save {savedValue= this.value}

	//--private
	prMake {|parent, bounds|
		^this.subclassResponsibility(thisMethod);
	}

	prConnect {
		var controller;
		view.action_({|view|
			ref.value_(map.value(view.value)).changed(\value);
		});
		controller= SimpleController(ref).put(\value, {|ref|
			view.value= unmap.value(ref.value);
		});
		view.onClose_({controller.remove});
		this.save;
	}
}
