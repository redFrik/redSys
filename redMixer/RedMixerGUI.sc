//redFrik

//--related:
//RedMixerChannelGUI RedEffectsRackGUI RedMatrixMixerGUI

//--todo:
//differentiate mixers and channels more graphically
//fix the rest of the presets
//textviews at the bottom for temporary notes
//multiple channels (at least mono/stero)
//preview/monitor synth and connect to numbox

RedMixerGUI {

	classvar <>numMixerChannelsBeforeScroll= 11;  //max window width

	var <win, <redMixer, <time, lastTime= 0, controllers,
	mainGUIchannels, mainGUImixers, mainGUIviews, mirrorGUIviews;

	*new {|redMixer, position|
		^super.new.initRedMixerGUI(redMixer, position);
	}

	initRedMixerGUI {|argRedMixer, position|
		Routine({
			var tab, channelWidth,
			macroMenu, macroFunctions, macroItems, makeMirror, defaults,
			lagBox, bw= 44, lh= 14;

			while({argRedMixer.isReady.not}, {0.02.wait});
			controllers= List.new;

			macroFunctions= {|index|
				var gui;
				if(tab.activeTab.index==0, {
					gui= mainGUIviews;
				}, {
					gui= mirrorGUIviews;
				});
				[
					{},
					{gui.do{|x, i| x.do{|y, j| y.view.valueAction= defaults[i][j]}}},
					{gui.do{|x| x.do{|y| y.view.valueAction= 1.0.rand}}},
					{gui.do{|x| x.do{|y| if(0.3.coin, {y.view.valueAction= 1.0.rand})}}},
					{gui.do{|x| x.do{|y| y.view.valueAction= y.view.value+0.08.rand2}}},
					{gui.do{|x| x.do{|y| if(0.3.coin, {y.view.valueAction= y.view.value+0.08.rand2})}}},
					//{gui.do{|x| x.valueEq= {|y, cv| cv.spec.unmap(cv.spec.default)}}},
					//{gui.do{|x| x.valueEq= {1.0.rand}}},
					//{gui.do{|x| x.valueEq= {|y| if(0.3.coin, {1.0.rand}, {y.value})}}},
					//{gui.do{|x| x.valueEq= {|y| y.value+0.08.rand2}}},
					//{gui.do{|x| x.valueEq= {|y| if(0.3.coin, {y.value+0.08.rand2}, {y.value})}}},
					{mainGUIviews.do{|x, i| x.do{|y, j| y.view.valueAction= mirrorGUIviews[i][j].view.value}}},
					{mirrorGUIviews.do{|x, i| x.do{|y, j| y.view.valueAction= mainGUIviews[i][j].view.value}}},
					{Dialog.savePanel{|x| redMixer.store(x)}},
					{Dialog.openPanel{|x|
						redMixer.free;
						redMixer= RedMixer.restoreFile(x);
						redMixer.init;
						redMixer.gui(Point(win.bounds.left, win.bounds.top));
						this.close;
					}}
				][index].value;
			};
			macroItems= #[
				"_macros_",
				"defaults",
				"randomize all",
				"randomize some",
				"vary all",
				"vary some",
				/*"eq: defaults",
				"eq: randomize all",
				"eq: randomize some",
				"eq: vary all",
				"eq: vary some",*/
				"copy <",
				"copy >",
				"save preset",
				"load preset"
			];

			redMixer= argRedMixer;
			position= position ?? {Point(300, 200)};
			channelWidth= {|n| RedMixerChannelGUI.width+4*n+10};

			win= Window(
				redMixer.class.name.asString.put(0, $r),
				Rect(
					position.x,
					position.y,
					channelWidth.value(redMixer.mixers.size+redMixer.channels.size)
					.max(260)
					.min(channelWidth.value(RedMixerGUI.numMixerChannelsBeforeScroll)),
					RedMixerChannelGUI.height+58
				),
				false
			);
			win.front;
			win.view.background= GUI.skins.redFrik.background;

			win.view.layout= VLayout(
				HLayout(
					lagBox= RedNumberBox().maxHeight_(lh).maxWidth_(bw)
					.value_(redMixer.lag)
					.action_({|v| redMixer.lag= v.value.max(0)});
					controllers.add(
						SimpleController(redMixer.cvs.lag).put(\value, {|ref|
							lagBox.value= ref.value;
						})
					);
					lagBox,
					RedStaticText(nil, nil, "lag").maxHeight_(lh).maxWidth_(20),

					RedButton(nil, nil, "monitor", "monitor").maxHeight_(lh).maxWidth_(52)
					.action_({|v| "todo".postln}),  //TODO
					RedNumberBox().maxHeight_(lh).maxWidth_(bw)
					.value_(7).action_({|v| "todo".postln}),

					macroMenu= RedPopUpMenu().maxHeight_(lh)
					.items_(macroItems)
					.action_({|v| macroFunctions.value(v.value)}),
					RedButton(nil, nil, "<").maxHeight_(lh).maxWidth_(lh)
					.action_({macroFunctions.value(macroMenu.value)}),

					View()  //spacer
				),

				HLayout(
					100,  //spacer

					time= RedSlider().maxHeight_(lh).maxWidth_(100)
					.action_({|v|
						var bg;
						if(tab.activeTab.index==0, {
							if(lastTime==0 and:{v.value>0}, {
								mainGUIviews.do{|x| x.do{|y| y.save}};
							});
							mainGUIviews.do{|x, i|
								x.do{|y, j| y.interp(mirrorGUIviews[i][j].value, v.value)};
							};
							bg= GUI.skins.redFrik.foreground.blend(GUI.skins.redFrik.background, v.value);
							tab.activeTab.background= bg;
						});
						lastTime= v.value;
					}),

					View()  //spacer
				),

				tab= TabbedView2(win);
				tab.backgrounds= [
					GUI.skins.redFrik.foreground,
					Color.red
				];
				tab.labelColors= [GUI.skins.redFrik.background];
				tab.unfocusedColors= [Color.grey(0.2, 0.2)];
				tab.stringFocusedColors= [GUI.skins.redFrik.foreground];
				tab.stringColors= [GUI.skins.redFrik.foreground];
				tab.font= RedFont.new;
				tab.tabHeight= 12;
				tab.add("now", scroll:true);
				tab.add("later", scroll:true);
				tab.view.minHeight_(RedMixerChannelGUI.height+14);
				tab.view
			).margins_(4).spacing_(4);

			tab.changeAction= {|v|
				if(v.activeTab.index==0, {
					time.valueAction= lastTime;
					time.enabled= true;
					time.canFocus= true;
				}, {
					mainGUIviews.do{|x| x.do{|y| y.save}};
					time.value= 1;
					time.enabled= false;
					time.canFocus= false;
				});
			};
			tab.views[0].flow{|v|
				mainGUIchannels= redMixer.channels.collect{|x, i|
					RedMixerChannelGUI(x, v, nil, "in[%,%]".format(x.out, x.out+1));
				};
				mainGUImixers= redMixer.mixers.collect{|x, i|
					RedMixerChannelGUI(x, v, nil, "out[%,%]".format(x.out, x.out+1));
				};
			};
			mainGUIviews= (mainGUIchannels++mainGUImixers).collect{|x| x.views};
			defaults= mainGUIviews.collect{|x| x.collect{|y| y.view.value}};

			makeMirror= {|v, x|
				var views= List.new;
				var view= View(v, x.view.bounds);
				var ampRef= x.redMixerChannel.cvs.amp;	//special case to keep this link
				var ampRefCopy= ampRef.copy;
				x.views.do{|y|
					var r, nv;
					if(y.ref==ampRef, {
						r= ampRefCopy;
					}, {
						r= y.ref.copy;
					});
					nv= y.class.new(view, y.view.bounds, r, y.map, y.unmap, y.args);
					if(y.isKindOf(RedGUICVSlider), {
						nv.view.orientation= y.view.orientation;
					});
					views.add(nv);
				};
				views;
			};
			tab.view.toFrontAction_({
				mirrorGUIviews= (mainGUIchannels++mainGUImixers).collect{|x|
					makeMirror.value(tab.views[1], x);
				};
			});

			redMixer.channels.do{|x, i|
				controllers.add(
					SimpleController(x.cvs.out).put(\value, {|ref|
						mainGUIchannels[i].text_("in[%,%]".format(ref.value, ref.value+1));
					})
				);
			};
			redMixer.mixers.do{|x, i|
				controllers.add(
					SimpleController(x.cvs.out).put(\value, {|ref|
						mainGUImixers[i].text_("out[%,%]".format(ref.value, ref.value+1));
					})
				);
			};

			win.onClose_({controllers.do{|x| x.remove}});
			CmdPeriod.doOnce({this.close});
		}).play(AppClock);
	}

	close {
		if(win.isClosed.not, {win.close});
	}
}
