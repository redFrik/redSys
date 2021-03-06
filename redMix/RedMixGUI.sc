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
		var cmp= this.prContainer;
		var inA, inB, out, lag;
		var mixamp, mixSpec= ControlSpec(-1, 1, 'lin', 0, -1);
		var controllers= List.new;

		inA= RedNumberBox(cmp);
		inA.value= redMix.inA;
		inA.action= {|view| redMix.inA= view.value.round.max(0)};
		controllers.add(
			SimpleController(redMix.cvs.inA).put(\value, {|ref| inA.value= ref.value})
		);
		RedStaticText(cmp, nil, "inA ("++redMix.def.metadata.info.inA++")");

		cmp.decorator.nextLine;
		inB= RedNumberBox(cmp);
		inB.value= redMix.inB;
		inB.action= {|view| redMix.inB= view.value.round.max(0)};
		controllers.add(
			SimpleController(redMix.cvs.inB).put(\value, {|ref| inB.value= ref.value})
		);
		RedStaticText(cmp, nil, "inB ("++redMix.def.metadata.info.inB++")");

		cmp.decorator.nextLine;
		out= RedNumberBox(cmp);
		out.value= redMix.out;
		out.action= {|view| redMix.out= view.value.round.max(0)};
		controllers.add(
			SimpleController(redMix.cvs.out).put(\value, {|ref| out.value= ref.value})
		);
		RedStaticText(cmp, nil, "out (stereo)");

		cmp.decorator.nextLine;
		lag= RedNumberBox(cmp);
		lag.value= redMix.lag;
		lag.action= {|view| redMix.lag= view.value.max(0)};
		controllers.add(
			SimpleController(redMix.cvs.lag).put(\value, {|ref| lag.value= ref.value})
		);
		RedStaticText(cmp, nil, "lag");

		cmp.decorator.nextLine;
		mixamp= Red2DSlider(cmp, cmp.decorator.indentedRemaining.extent);
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
		var cmp, width, height, margin= Point(4, 4), gap= Point(4, 4);
		position= position ?? {Point(600, 400)};
		width= 120;
		height= 140;
		if(parent.isNil, {
			parent= Window(redMix.class.name, Rect(position.x, position.y, width, height), false);
			win= parent;
			if(Main.versionAtMost(3, 4) and:{GUI.scheme!=\cocoa}, {
				win.alpha= GUI.skins.redFrik.unfocus;
			});
			win.front;
			CmdPeriod.doOnce({this.close});
		});
		cmp= CompositeView(parent, Point(width, height))
		.background_(GUI.skins.redFrik.background);
		cmp.decorator= FlowLayout(cmp.bounds, margin, gap);
		^cmp;
	}
}
