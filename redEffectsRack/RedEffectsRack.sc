//redFrik

//--related:
//RedEffectsRackGUI RedEffectModule RedAbstractMix

RedEffectsRack {
	classvar <>defaultClasses;

	var <group, <cvs, <specs, <isReady= false, groupPassedIn,
	<efxs, controllers;

	*new {|efxClasses, out= 0, group, lag= 0|
		^super.new.initRedEffectsRack(efxClasses, out, group, lag);
	}

	*initClass {
		defaultClasses= [	//just some favourites
			RedEfxRing,
			RedEfxTape,
			RedEfxComb,
			RedEfxDist,
			RedEfxTank,
			RedEfxComp
		];
	}

	initRedEffectsRack {|efxClasses, argOut, argGroup, lag|
		var server;
		if(efxClasses.isNil or:{efxClasses.isEmpty}, {
			(this.class.name++": efxClasses empty so using the default classes").inform;
			efxClasses= defaultClasses;
		});
		if(argGroup.isNil, {
			server= Server.default;
			groupPassedIn= false;
		}, {
			server= argGroup.server;
			groupPassedIn= true;
		});
		forkIfNeeded{
			if(groupPassedIn.not, {	//boot server and create group
				server.bootSync;
				group= Group.after(server.defaultGroup);
				server.sync;
			}, {
				group= argGroup;
			});

			//--create efxs
			efxs= efxClasses.collect{|x| x.new(argOut, group, \addToTail, lag)};

			//--one bus that controlls them all
			cvs= ();
			specs= (out: \audiobus.asSpec);
			controllers= List.new;
			cvs.out= Ref(specs.out.default);
			controllers.add(
				SimpleController(cvs.out).put(\value, {|ref|
					efxs.do{|x| x.out= ref.value};
				})
			);

			//--add all efx cvs to this cvs
			efxs.do{|x|
				x.cvs.keysValuesDo{|k, v|
					var suffix= 1, ks, spec;
					if(k!=\out, {
						spec= x.specs[x.cvsToParam(k)];
						//--add suffix if >1 of the same efx class
						if(cvs[k].notNil, {	//duplicates
							ks= k.asString;
							cvs.keysValuesDo{|kk, vv|
								kk= kk.asString;
								if(kk.contains(ks), {
									if(suffix<=kk.split($_).last.asInteger, {
										suffix= kk.split($_).last.asInteger+1;
									});
								});
							};
							k= (k++"_"++suffix).asSymbol;
						});
						cvs.put(k, v);
						this.addUniqueMethod((k++"_").asSymbol, {|obj, val|
							cvs[k].value_(spec.constrain(val)).changed(\value);
							this;
						});
						this.addUniqueMethod(k, {|obj|
							cvs[k].value;
						});

					});
				};
			};
			isReady= true;
		};
	}

	free {
		controllers.do{|x| x.remove};
		efxs.do{|x| x.free};
		if(groupPassedIn.not, {group.free});
	}

	out_ {|val|
		cvs.out.value_(specs.out.constrain(val)).changed(\value);
	}
	out {
		^cvs.out.value;
	}

	gui {|position|	//parent here???
		^RedEffectsRackGUI(this, position);
	}
}
