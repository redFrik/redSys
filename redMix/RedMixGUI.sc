//redFrik

//--todo:
//use controlspecs from RedMix objects

//--related:
//RedAbstractMix RedEffectModuleGUI RedMixerGUI RedMatrixMixerGUI RedTapTempoGUI

RedMixGUI {
	var <redMix, <parent, position,
	win;
	*new {|redMix, parent, position|
		^super.newCopyArgs(redMix, parent, position).initRedMixGUI;
	}
	initRedMixGUI {
		var view= this.prContainer;
		var bw= 36, lh= 14;
		var inA, inB, out, lag;
		var mixamp, mixSpec= ControlSpec(-1, 1, 'lin', 0, -1);
		var controllers= List.new;

		view.layout= VLayout(
			HLayout(
				inA= RedNumberBox(view).maxWidth_(bw).maxHeight_(lh),
				RedStaticText(view, nil, "inA ("++redMix.def.metadata.info.inA++")").maxHeight_(lh)
			),
			HLayout(
				inB= RedNumberBox(view).maxWidth_(bw).maxHeight_(lh),
				RedStaticText(view, nil, "inB ("++redMix.def.metadata.info.inB++")").maxHeight_(lh)
			),
			HLayout(
				out= RedNumberBox(view).maxWidth_(bw).maxHeight_(lh),
				RedStaticText(view, nil, "out (stereo)").maxHeight_(lh)
			),
			HLayout(
				lag= RedNumberBox(view).maxWidth_(bw).maxHeight_(lh),
				RedStaticText(view, nil, "lag").maxHeight_(lh)
			),
			mixamp= Red2DSlider(view)
		).margins_(4).spacing_(4);

		inA.value= redMix.inA;
		inA.action= {|view| redMix.inA= view.value.round.max(0)};
		controllers.add(
			SimpleController(redMix.cvs.inA).put(\value, {|ref| inA.value= ref.value})
		);

		inB.value= redMix.inB;
		inB.action= {|view| redMix.inB= view.value.round.max(0)};
		controllers.add(
			SimpleController(redMix.cvs.inB).put(\value, {|ref| inB.value= ref.value})
		);

		out.value= redMix.out;
		out.action= {|view| redMix.out= view.value.round.max(0)};
		controllers.add(
			SimpleController(redMix.cvs.out).put(\value, {|ref| out.value= ref.value})
		);

		lag.value= redMix.lag;
		lag.action= {|view| redMix.lag= view.value.max(0)};
		controllers.add(
			SimpleController(redMix.cvs.lag).put(\value, {|ref| lag.value= ref.value})
		);

		mixamp.x= mixSpec.unmap(redMix.mix);
		mixamp.y= redMix.amp;
		mixamp.action= {|view| redMix.mix= mixSpec.map(view.x); redMix.amp= view.y};
		controllers.add(
			SimpleController(redMix.cvs.mix).put(\value, {|ref| mixamp.x= mixSpec.unmap(ref.value)})
		);
		controllers.add(
			SimpleController(redMix.cvs.amp).put(\value, {|ref| mixamp.y= ref.value})
		);
		mixamp.mouseUpAction= {|view, x, y, mod| if(mod.isCtrl, {{redMix.mix= 0}.defer(0.1)})};
		mixamp.onClose_({controllers.do{|x| x.remove}});
	}
	close {
		if(win.notNil and:{win.isClosed.not}, {win.close});
	}

	//--private
	prContainer {
		var width, height;
		position= position ?? {Point(600, 400)};
		width= 120;
		height= 140;
		if(parent.isNil, {
			parent= Window(redMix.class.name, Rect(position.x, position.y, width, height), false);
			win= parent;
			win.alpha= GUI.skins.redFrik.unfocus;
			win.front;
			CmdPeriod.doOnce({this.close});
		});
		^View(parent, Point(width, height))
		.layout_(VLayout())
		.background_(GUI.skins.redFrik.background);
	}
}
