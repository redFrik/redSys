//redFrik

//--related:
//RedAbstractMix RedMixGUI

RedTapTempoGUI {
	var <parent, position;
	var <clock, task, monOn= false, monAmp= 0.5, monBus= 7;
	var win;
	var bpmView, bpsView, tapView, monOnView, monBusView;

	*new {|clock, n= 4, timeout= 3, server, parent, position|
		^super.new.initRedTapTempoGUI(clock, n, timeout, server, parent, position);
	}

	initRedTapTempoGUI {|argClock, n, timeout, server, argParent, argPosition|
		var view, times= 0.dup(n), counter, lastTime= 0;
		clock= argClock ?? TempoClock.default;
		server= server ?? Server.default;
		parent= argParent;
		position= argPosition;
		view= this.prContainer;

		forkIfNeeded{
			server.bootSync;

			//--send definition
			this.def.add;
			server.sync;

			//--gui
			{
				var bw= 36, lh= 14;
				view.layout= VLayout(
					tapView= RedButton(nil, nil, "tap tempo", "tap tempo").action_{|view|
						var newTempo, nowTime= SystemClock.seconds;
						if(nowTime-timeout>lastTime, {
							(this.class.name++": timeout").postln;
							counter= 0;
						});
						if(counter<(n-1), {
							times= times.put(counter, SystemClock.seconds);
							counter= counter+1;
						}, {
							times= times.put(counter, SystemClock.seconds);
							newTempo= times.differentiate.drop(1).mean;
							bpmView.valueAction_(60/newTempo);
							times= times.rotate(-1);
						});
						lastTime= nowTime;
						view.value= 0;
					}.maxHeight_(60).focus,

					RedButton(nil, nil, "sync").action_{|view|
						task.stop;
						task= this.prRoutine.play(clock);
					}.maxHeight_(lh),

					HLayout(
						bpmView= RedNumberBox().value_(clock.tempo*60).action_{|view|
							var bps= (view.value/60).round(0.0001);
							clock.tempo= bps;
							bpsView.value= bps;
							(this.class.name++": new tempo... bps:"+bps+" bpm:"+(bps*60)).postln;
						}.maxWidth_(bw).maxHeight_(lh),
						RedStaticText(nil, nil, "bpm").maxHeight_(lh),
						bpsView= RedNumberBox().value_(clock.tempo).action_{|view|
							this.tempo= view.value.max(0);
						}.maxWidth_(bw).maxHeight_(lh),
						RedStaticText(nil, nil, "bps").maxHeight_(lh)
					),

					HLayout(
						monOnView= RedButton(nil, nil, "monitor", "monitor").action_{|view|
							monOn= view.value.booleanValue;
						}.maxWidth_(53).maxHeight_(lh),
						monBusView= RedNumberBox().value_(monBus).action_{|view|
							monBus= view.value.asInteger.max(0);
							view= monBus;
						}.maxWidth_(bw).maxHeight_(lh),
						View().maxHeight_(lh)
					)
				).margins_(4).spacing_(4);

				task= this.prRoutine.play(clock, quant:1);
				view.onClose_({task.stop});
			}.defer;
		};
	}

	tempo {^clock.tempo}
	tempo_ {|bps| bpmView.valueAction= bps*60}

	monitor_ {|bool| monOnView.valueAction= bool.binaryValue}
	monitorAmp_ {|val| monAmp= val}
	monitorBus_ {|index|
		monBus= index;
		monBusView.value= index;
	}

	close {
		task.stop;
		if(win.notNil and:{win.isClosed.not}, {win.close});
	}

	def {^this.class.def}
	*def {
		^SynthDef(\redTapTempo, {|out= 0, amp= 0.5| //mono only
			var e= EnvGen.kr(Env.perc(0.01, 0.1), doneAction:2);
			var z= SinOsc.ar(400, e*2pi, e*amp);
			Out.ar(out, z);
		}, metadata: (
			specs: (
				\out: \audiobus.asSpec,
				\amp: ControlSpec(0, 1, \lin, 0, 0.5)
			)
		));
	}

	//--private
	prContainer {
		var width, height;
		position= position ?? {Point(400, 600)};
		width= 130;
		height= 122;
		if(parent.isNil, {
			parent= Window("redTapTempo", Rect(position.x, position.y, width, height), false);
			win= parent;
			win.alpha= GUI.skins.redFrik.unfocus;
			win.front;
			CmdPeriod.doOnce({if(win.isClosed.not, {win.close})});
		});
		^View(parent, Point(width, height))
		.layout_(VLayout())
		.background_(GUI.skins.redFrik.background);
	}

	prRoutine {
		^Routine({
			inf.do{
				if(monOn, {Synth(\redTapTempo, [\out, monBus, \amp, monAmp])});
				{
					if(bpmView.hasFocus.not and:{bpsView.hasFocus.not}, {
						if(bpmView.value!=this.tempo, {
							this.tempo= this.tempo;
						});
					});
				}.defer;
				{tapView.value= 1}.defer;
				0.25.wait;
				{tapView.value= 0}.defer;
				0.75.wait;
			};
		});
	}
}
