//redFrik

//related:
//RedEffectModule RedInstrumentModule RedAbstractMix

RedAbstractModule {	//abstract class
	classvar <all;

	var <group, addAction, <cvs, <specs;
	var condition, controllers;
	var thisdef;

	*initClass {
		all= List.new;
	}

	*new {|group, addAction, lag= 0|
		^super.new.initRedAbstractModule(group, addAction, lag);
	}

	initRedAbstractModule {|argGroup, argAddAction, lag|
		var server;
		condition= Condition();
		if(argGroup.isNil, {
			server= Server.default;
		}, {
			group= argGroup;
			server= group.server;
		});
		addAction= argAddAction;
		cvs= ();
		specs= ();
		controllers= List.new;

		thisdef= this.def(lag);
		thisdef.metadata[\order].do{|assoc|
			var k= assoc.key;
			var v= assoc.value;
			var spec= thisdef.metadata[\specs][k];
			if(k==\out, {v= \out});	//special case
			cvs.put(v, Ref(spec.default));
			specs.put(k, spec);
		};
		this.initMethods;

		forkIfNeeded{
			server.bootSync;
			thisdef.add;
			server.sync;
			condition.test= true;
			condition.signal;
			all.add(this);
		};
	}

	initMethods {
		cvs.keysValuesDo{|k, v|
			this.addUniqueMethod((k++"_").asSymbol, {|obj, val|
				cvs[k].value_(specs[this.cvsToParam(k)].constrain(val)).changed(\value);
				this;
			});
			this.addUniqueMethod(k, {|obj|
				cvs[k].value;
			});
		};
	}

	cvsToParam {|name|
		if(name==\out, {
			^\out;
		}, {
			^thisdef.metadata[\order].detect{|x| x.value==name}.key;
		});
	}

	free {
		controllers.do{|x| x.remove};
		all.remove(this);
	}

	def {|lag| ^this.class.def(lag)}

	//--for subclasses
	*def {^this.subclassResponsibility(thisMethod)}
	gui {|parent, position| ^this.subclassResponsibility(thisMethod)}
}
