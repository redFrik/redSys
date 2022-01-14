//redFrik

//unfinished and untested

RedInstrumentModule : RedAbstractModule {		//abstract class
	var <voices;

	*new {|out= 0, group, addAction= \addToHead|
		^super.new(group, addAction).initRedInstrumentModule(out);
	}

	initRedInstrumentModule {|out|
		//initAction= {
			cvs.out.value= out;	//override spec default with bus argument
			voices= List.new;
		//};
	}

	play {|key, args, aGroup, anAddAction|
		var arr= cvs.asKeyValuePairs++args;
		var synth;
		synth= Synth(this.def.name, arr, aGroup?group, anAddAction?addAction);
		voices.add(key -> synth);
	}

	free {
		voices.do{|x| x.free};
		super.free;
	}

	gui {|parent, position|
		^RedInstrumentGUI(parent, position);
	}

	stop {|key|
		var i;
		if(key.isNil and:{voices.size>0}, {
			voices.removeAt(0).value.release;
		}, {
			i= voices.detectIndex{|x| x.key==key};
			if(i.notNil, {
				voices.removeAt(i).value.release;
			}, {
				(this.class.name++": couldn't find key"+key).warn;
			});
		});
	}

	stopAll {
		voices.do{|x| x.value.release}.clear;
	}

	synth {|key|
		^voices.detect{|x| x.key==key}.value;
	}

	//--for subclasses
	*def {^this.subclassResponsibility(thisMethod)}
}
