//redFrik

//--related:
//RedTapTempoGUI

//TODO: make embeddable with parent

RedTempoClockGUI {
	var <win;
	*new {|position|
		^super.new.initRedTempoClockGUI(position);
	}
	initRedTempoClockGUI {|position|
		var view, pop, num, bpm, sld;
		var spec= #[0.5, 5].asSpec, index= 0, clocks= [];
		var bw= 36, lh= 14;
		position= position ?? {Point(100, 200)};
		win= Window("TempoClock.all", Rect(position.x, position.y, 214, 60), false);
		view= View(win, Rect(0, 0, win.bounds.width, win.bounds.height));
		view.background= GUI.skins.redFrik.background;
		view.layout= VLayout(
			pop= RedPopUpMenu(view).maxHeight_(lh),
			HLayout(
				RedStaticText(view, nil, "bps:").maxWidth_(30).maxHeight_(lh),
				num= RedNumberBox(view).maxWidth_(bw).maxHeight_(lh),
				bpm= RedStaticText(view, nil, "(bpm:)").maxHeight_(lh)
			),
			sld= RedSlider(view).maxHeight_(lh)
		).margins_(4).spacing_(4);
		Routine({
			inf.do{
				if(clocks!=TempoClock.all.collect{|x| x.hash}, {
					(this.class.name++": new TempoClock detected").postln;
					clocks= TempoClock.all.collect{|x| x.hash};
					pop.items= TempoClock.all.collect{|x, i| "TempoClock["++i++"]"};
				});
				1.wait;
			};
		}).play(AppClock);
		num.action= {|view|
			sld.value= spec.unmap(view.value);
			bpm.string= "(bpm:"++(view.value*60)++")";
			TempoClock.all[index].tempo= view.value;
		};
		sld.action= {|view|
			num.valueAction_(spec.map(view.value));
		};
		pop.action= {|view|
			index= view.value;
			num.valueAction_(TempoClock.all[index].tempo);
		};
		if(TempoClock.all.notEmpty, {
			num.valueAction_(TempoClock.all[0].tempo);
		});
		win.alpha= GUI.skins.redFrik.unfocus;
		win.front;
		CmdPeriod.doOnce({win.close});
	}
	close {
		if(win.isClosed.not, {
			win.close;
		});
	}
}
