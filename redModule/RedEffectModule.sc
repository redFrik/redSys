//redFrik

//--related:
//RedEffectModuleGUI RedEffectsRack RedAbstractMix RedEfxRing
//RedEffectModule.subclasses

RedEffectModule : RedAbstractModule {	//abstract class
	var <synth;

	*new {|out= 0, group, addAction= \addToTail, lag= 0|
		^super.new(group, addAction, lag).initRedEffectModule(out);
	}

	initModule {|group|
		thisdef.add;
		group.server.sync;
		all.add(this);
		super.initMethods;
		this.initRedEffectModule(cvs.out.value);
	}

	initRedEffectModule {|out|
		var args= List.new;
		forkIfNeeded{
			condition.wait;
			cvs.out.value= out;	//override spec default with bus argument
			cvs.keysValuesDo{|k, v|
				args.add(this.cvsToParam(k));
				args.add(v.value);
			};
			synth= Synth(thisdef.name, args, group, addAction);
			this.prAddControllers;
		};
	}

	free {
		synth.free;
		super.free;
	}

	gui {|parent, position|
		^RedEffectModuleGUI(this, parent, position);
	}

	//--private
	prAddControllers {
		cvs.keysValuesDo{|k, v|
			var param= this.cvsToParam(k);
			controllers.add(
				SimpleController(v).put(\value, {|ref|
					synth.set(param, ref.value);
				})
			);
		};
	}

	//--for subclasses
	*def {^this.subclassResponsibility(thisMethod)}
}
