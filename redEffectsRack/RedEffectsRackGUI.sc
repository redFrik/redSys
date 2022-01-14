//redFrik

//time should also be a cv or?
//lag box?
//window height fix

//--related:
//RedEffectsRack RedEffectModule RedEffectModuleGUI RedMixerGUI

RedEffectsRackGUI {
	classvar <>numEffectsBeforeScroll= 9;

	var <win, <redEffectsRack, <time, lastTime= 0, controllers,
	mainGUI, mainGUIviews, mirrorGUIviews,
	width1= 264, height1;

	*new {|redEffectsRack, position|
		^super.new.initRedEffectsRackGUI(redEffectsRack, position);
	}

	initRedEffectsRackGUI {|argRedEffectsRack, position|
		Routine({
			var tab,
			macroMenu, macroFunctions, macroItems, makeMirror, defaults,
			outBox, rows, bw= 44, lh= 14;
			while({argRedEffectsRack.isReady.not}, {0.02.wait});
			controllers= List.new;

			macroFunctions= {|index|
				var gui;
				if(tab.activeTab==0, {
					gui= mainGUIviews;
				}, {
					gui= mirrorGUIviews;
				});
				[
					{},
					{gui.do{|x, i| x.do{|y, j| y.view.valueAction= defaults[i][j]}}},
					{gui.do{|x| x.do{|y, j| if(j==0 or:{j%2==1}, {y.view.valueAction= 1.0.rand})}}},
					{gui.do{|x| x.do{|y, j| if(j==0 or:{j%2==1}, {if(0.3.coin, {y.view.valueAction= 1.0.rand})})}}},
					{gui.do{|x| x.do{|y, j| if(j==0 or:{j%2==1}, {y.view.valueAction= y.view.value+0.08.rand2})}}},
					{gui.do{|x| x.do{|y, j| if(j==0 or:{j%2==1}, {if(0.3.coin, {y.view.valueAction= y.view.value+0.08.rand2})})}}},
					{gui.collect{|x| x.select{|y, j| j==0 or:{j%2==1}}.choose}.choose.view.valueAction= #[0, 0.5, 1].choose},
					{gui.do{|x| x.do{|y, j| if(j==0 or:{j%2==1}, {y.view.valueAction= 0})}}},
					{mainGUIviews.do{|x, i| x.do{|y, j| y.view.valueAction= mirrorGUIviews[i][j].view.value}}},
					{mirrorGUIviews.do{|x, i| x.do{|y, j| y.view.valueAction= mainGUIviews[i][j].view.value}}},
					{Dialog.savePanel{|x| redEffectsRack.cvs.postln.writeArchive(x)}},
					/*{Dialog.openPanel{|x|
					redMixer.free;
					redMixer= RedMixer.restoreFile(x);
					redMixer.init;
					redMixer.gui(Point(win.bounds.left, win.bounds.top));
					this.close;
					}}*/
					{Dialog.openPanel{|x| Object.readArchive(x).keysValuesDo{|k, v|
						redEffectsRack.cvs[k].value_(v.value).changed(\value);
					}}}
				][index].value;
			};
			macroItems= #[
				"_macros_",
				"defaults",
				"randomize all",
				"randomize some",
				"vary all",
				"vary some",
				"surprise",
				"clear",
				"copy <",
				"copy >",
				"save preset",
				"load preset",
				"#1", "#2", "#3", "#4", "#5"
			];

			redEffectsRack= argRedEffectsRack;
			position= position ?? {Point(300, 360)};

			redEffectsRack.efxs.do{|x|
				var cols= x.def.metadata[\order].count{|x| x.key!=\out}-1;
				var tmp= 4*2.5+RedGUICVSlider.defaultWidth+4;
				tmp= tmp+(RedGUICVKnob.defaultWidth+4*cols);
				if(tmp>width1, {width1= tmp});
			};
			height1= 4+RedGUICVSlider.defaultHeight+4;

			rows= redEffectsRack.efxs.size.min(numEffectsBeforeScroll);

			win= Window(
				redEffectsRack.class.name.asString.put(0, $r),
				Rect(
					position.x,
					position.y,
					width1+8,
					height1+4*rows+56
				),
				false
			);
			win.front;
			win.view.background= GUI.skins.redFrik.background;

			win.view.layout= VLayout(
				HLayout(
					outBox= RedNumberBox().maxHeight_(lh).maxWidth_(bw)
					.value_(redEffectsRack.out)
					.action_({|v|
						redEffectsRack.out= v.value;
					});
					controllers.add(
						SimpleController(redEffectsRack.cvs.out).put(\value, {|ref|
							outBox.value= ref.value;
						})
					);
					outBox,
					RedStaticText(nil, nil, "out").maxHeight_(lh).maxWidth_(16),

					RedButton(nil, nil, "monitor", "monitor").maxHeight_(lh).maxWidth_(52)
					.action_({|view| "todo".postln}),  //TODO
					RedNumberBox().maxHeight_(lh).maxWidth_(bw)
					.value_(7).action_({|view| "todo".postln}),

					macroMenu= RedPopUpMenu().maxHeight_(lh)
					.items_(macroItems)
					.action_({|view| macroFunctions.value(view.value)}),
					RedButton(nil, Point(14, 14), "<").maxHeight_(lh).maxWidth_(lh)
					.action_({macroFunctions.value(macroMenu.value)}),

					View()  //spacer
				),

				HLayout(
					100,  //spacer

					time= RedSlider().maxHeight_(lh).maxWidth_(100)
					.action_({|v|
						if(tab.activeTab==0, {
							if(lastTime==0 and:{v.value>0}, {
								mainGUIviews.do{|x| x.do{|y| y.save}};
							});
							mainGUIviews.do{|x, i|
								x.do{|y, j|
									y.interp(mirrorGUIviews[i][j].value, v.value);
								};
							};
							tab.backgrounds_([
								GUI.skins.redFrik.foreground.copy.alpha_(1-v.value),
								GUI.skins.redFrik.background
							]);
						}, {
							v.value= 1;
						});
						lastTime= v.value;
					}),

					View()  //spacer
				),

				tab= TabbedView(
					win,
					nil,
					#[\now, \later],
					[Color.grey(0.2, 0.2), GUI.skins.redFrik.background],
					scroll: true
				);
				tab.view.minHeight_(height1+4*rows+14);
				tab.view
			).margins_(4).spacing_(4);

			if(redEffectsRack.efxs.size<=numEffectsBeforeScroll, {
				tab.views.do{|x| x.hasVerticalScroller= false};
			});
			tab.views.do{|x| x.hasHorizontalScroller= false};
			tab.font= RedFont.new;
			tab.stringFocusedColor= GUI.skins.redFrik.foreground;
			tab.stringColor= GUI.skins.redFrik.foreground;
			tab.backgrounds= [GUI.skins.redFrik.foreground, GUI.skins.redFrik.background];
			tab.unfocusedColors= [Color.grey(0.2, 0.2), GUI.skins.redFrik.background];
			tab.focusActions= [
				{
					time.valueAction= lastTime;
					time.enabled= true;
					time.canFocus= true;
				},
				{
					mainGUIviews.do{|x| x.do{|y| y.save}};
					time.value= 1;
					time.enabled= false;
					time.canFocus= false;
				}
			];
			tab.views[0].flow{|v|
				mainGUI= redEffectsRack.efxs.collect{|x|
					var gui= RedEffectModuleGUI(x, v);
					v.decorator.nextLine;
					gui;
				};
			};
			mainGUIviews= mainGUI.collect{|x| x.views};
			//mainGUIviews= mainGUI.select{|x, i| i==0 or:{i%2==1}}.collect{|x| x.views};
			defaults= mainGUIviews.collect{|x| x.collect{|y| y.view.value}};

			makeMirror= {|v, x|
				var views= List.new;
				var m= x.views[0];	//first the mixer slider
				var view= View(v, m.view.parent.bounds);
				views.add(m.class.new(view, m.view.bounds, m.ref.copy, m.map, m.unmap, m.args));
				x.views.copyToEnd(1).pairsDo{|y, z, j|
					var p= x.params[j.div(2)+1].value;
					var r= x.redEffectModule.cvs[p].copy;	//then the knobs and numbers
					views.add(y.class.new(view, y.view.bounds, r, y.map, y.unmap, y.args));
					views.add(z.class.new(view, z.view.bounds, r, z.map, z.unmap, z.args));
				};
				views;
			};
			tab.view.toFrontAction_({
				mirrorGUIviews= mainGUI.collect{|x|
					makeMirror.value(tab.views[1], x);
				};
			});

			win.onClose_({controllers.do{|x| x.remove}});
			CmdPeriod.doOnce({this.close});
		}).play(AppClock);
	}

	close {
		if(win.isClosed.not, {win.close});
	}
}
