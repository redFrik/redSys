//redFrik

//--related:
//RedEffectsRackGUI, RedMixerGUI

//--ideas:
//time should also be a ref too or?
//multisliderview inte continuerligt
//allt i db istället?
//knobs+numberbox eller annat sätt att visa exakta värden?
//preset med mus xy gui där cirklar visar hur mycket varje kanal. slider att styra omfång, max. jmf processing exempel

//TODO: make embeddable

RedMatrixMixerGUI {
	var <win, <redMatrixMixer, <time, lastTime= 0,
	mainGUI, mirrorGUI;

	*new {|redMatrixMixer, position|
		^super.new.initRedMatrixMixerGUI(redMatrixMixer, position);
	}

	initRedMatrixMixerGUI {|argRedMatrixMixer, position|
		Routine({
			var tab,
			macroMenu, macroFunctions, macroItems, makeMirror,
			inNumbers, outNumbers,
			inBox, outBox, lagBox, bw= 44, lh= 14, sw, controllers= List.new,
			nIn= argRedMatrixMixer.nIn,
			nOut= argRedMatrixMixer.nOut;
			while({argRedMatrixMixer.isReady.not}, {0.02.wait});

			macroFunctions= {|index|
				var gui;
				if(tab.activeTab.index==0, {
					gui= mainGUI;
				}, {
					gui= mirrorGUI;
				});
				[
					{},
					{gui.do{|x, i|
						var arr= 0.dup(nIn);
						if(i<nIn, {arr= arr.put(i, 1)});
						x.value= arr;
					}},
					{gui.do{|x, i|
						var arr= 0.dup(nIn);
						if(i<nIn, {arr= arr.put(nIn-1-i, 1)});
						x.value= arr;
					}},
					{gui.do{|x, i|
						var arr= 0.dup(nIn);
						if(i<nIn, {arr= arr.put(i, 1).put(nIn-i-1, 1)});
						x.value= arr;
					}},
					{gui.do{|x| x.value= x.value.scramble}},
					{gui.do{|x| x.value= 0.dup(nIn)}},
					{var tmp= (0..nIn-1).scramble; gui.do{|x, i|
						var arr= 0.dup(nIn);
						if(i<nIn, {arr= arr.put(tmp[i], 1)});
						x.value= arr;
					}},
					{var tmp= (0..nIn-1).scramble; gui.do{|x, i|
						var arr= 0.dup(nIn);
						if(i<nIn, {arr= arr.put(tmp[i], 1.0.rand)});
						x.value= arr;
					}},
					{gui.do{|x, i| x.value= {0.2.coin.binaryValue}.dup(nIn)}},
					{gui.do{|x, i| x.value= {if(0.2.coin, {1.0.rand}, {0})}.dup(nIn)}},
					{gui.do{|x| x.value= x.value.rotate(-1)}},
					{gui.do{|x| x.value= x.value.rotate(1)}},
					{
						var tmp= gui.collect{|x| x.value};
						gui.do{|x, i| x.value= tmp.wrapAt(i+1)}
					},
					{
						var tmp= gui.collect{|x| x.value};
						gui.do{|x, i| x.value= tmp.wrapAt(i-1)}
					},
					{mainGUI.do{|x, i| x.value= mirrorGUI[i].value}},
					{mirrorGUI.do{|x, i| x.value= mainGUI[i].value}},
					{Dialog.savePanel{|x| redMatrixMixer.cvs.writeArchive(x)}},
					{Dialog.openPanel{|x| Object.readArchive(x).keysValuesDo{|k, v|
						redMatrixMixer.cvs[k].value_(v.value).changed(\value);
						mainGUI.do{|x| x.save};
					}}}
				][index].value;
			};
			macroItems= #[
				"_macros_",
				"default",
				"backwards",
				"cross",
				"scramble",
				"clear",
				"urn binary",
				"urn float",
				"random binary",
				"random float",
				"shift up",
				"shift down",
				"shift left",
				"shift right",
				"copy <",
				"copy >",
				"save preset",
				"load preset"
			];

			redMatrixMixer= argRedMatrixMixer;
			position= position ?? {Point(700, 400)};

			sw= "o99".bounds(RedFont.new).width;
			win= Window(
				"redMatrixMixer nIn"++nIn+"nOut"++nOut,
				Rect(
					position.x,
					position.y,
					(RedGUICVMultiSliderView.defaultWidth+4*nOut+8+sw).max(280),
					RedGUICVMultiSliderView.defaultHeight*nIn+14+14+46
				),
				false
			);
			win.front;
			win.view.background= GUI.skins.redFrik.background;

			win.view.layout= VLayout(
				HLayout(
					inBox= RedNumberBox().maxHeight_(lh).maxWidth_(bw)
					.value_(redMatrixMixer.in)
					.action_({|v|
						var val= v.value.round.max(0);
						redMatrixMixer.in= val;
						inNumbers.do{|x, i| x.string= "i"++(i+val)};
					});
					controllers.add(
						SimpleController(redMatrixMixer.cvs.in).put(\value, {|ref|
							inBox.value= ref.value;
							inNumbers.do{|x, i| x.string= "i"++(i+ref.value)};
						})
					);
					inBox,
					RedStaticText(nil, nil, "in").maxHeight_(lh).maxWidth_(12),

					outBox= RedNumberBox().maxHeight_(lh).maxWidth_(bw)
					.value_(redMatrixMixer.out)
					.action_({|v|
						var val= v.value.round.max(0);
						redMatrixMixer.out= val;
						outNumbers.do{|x, i| x.string= "o"++(i+val)};
					});
					controllers.add(
						SimpleController(redMatrixMixer.cvs.out).put(\value, {|ref|
							outBox.value= ref.value;
							outNumbers.do{|x, i| x.string= "o"++(i+ref.value)};
						})
					);
					outBox,
					RedStaticText(nil, nil, "out").maxHeight_(lh).maxWidth_(16),

					lagBox= RedNumberBox().maxHeight_(lh).maxWidth_(bw)
					.value_(redMatrixMixer.lag)
					.action_({|v|
						redMatrixMixer.lag= v.value.max(0);
					});
					controllers.add(
						SimpleController(redMatrixMixer.cvs.lag).put(\value, {|ref|
							lagBox.value= ref.value;
						})
					);
					lagBox,
					RedStaticText(nil, nil, "lag").maxHeight_(lh).maxWidth_(20),

					macroMenu= RedPopUpMenu().maxHeight_(lh)
					.items_(macroItems)
					.action_({|view|
						macroFunctions.value(view.value);
						mainGUI.do{|x| x.save};
					}),
					RedButton(nil, Point(14, 14), "<").maxHeight_(lh).maxWidth_(lh)
					.action_({
						macroFunctions.value(macroMenu.value);
						mainGUI.do{|x| x.save};
					})
				),

				HLayout(
					100,  //spacer

					time= RedSlider().maxHeight_(lh).maxWidth_(100)
					.action_({|v|
						var bg;
						if(tab.activeTab.index==0, {
							if(lastTime==0 and:{v.value>0}, {
								mainGUI.do{|x| x.save};
							});
							mainGUI.do{|x, i|
								x.interp(mirrorGUI[i].value, v.value);
							};
							bg= GUI.skins.redFrik.foreground.copy.alpha_(1-v.value);
							tab.activeTab.background= bg;
						});
						lastTime= v.value;
					}),

					View().maxHeight_(lh)  //spacer
				),

				HLayout(
					VLayout(*[
						View().fixedHeight_(12),  //spacer
						inNumbers= {|i|
							RedStaticText(nil, nil, "i"++i).fixedWidth_(12)
						}.dup(nIn)
					].flat),

					tab= TabbedView2(win);
					tab.backgrounds= [
						GUI.skins.redFrik.foreground.copy.alpha_(1),
						GUI.skins.redFrik.background
					];
					tab.labelColors= [GUI.skins.redFrik.background];
					tab.unfocusedColors= [Color.grey(0.2, 0.2)];
					tab.stringFocusedColors= [GUI.skins.redFrik.foreground];
					tab.stringColors= [GUI.skins.redFrik.foreground];
					tab.font= RedFont.new;
					tab.tabHeight= 12;
					tab.add("now");
					tab.add("later");
					tab.changeAction= {|v|
						if(v.activeTab.index==0, {
							time.valueAction= lastTime;
							time.enabled= true;
							time.canFocus= true;
						}, {
							mainGUI.do{|x| x.save};
							time.value= 1;
							time.enabled= false;
							time.canFocus= false;
						});
					};
					tab.views[0].flow{|v|
						mainGUI= {|i|
							var ref= redMatrixMixer.cvs[("o"++i).asSymbol];
							var vi= RedGUICVMultiSliderView(v, nil, ref, nil, nil, (num: ref.value.size));
							vi.view.action_({vi.view.action+{vi.save}});
							vi;
						}.dup(nOut);
					};
					tab.view,

					View().maxWidth_(14)  //spacer
				),

				HLayout(*[
					View().fixedWidth_(12+8).maxHeight_(lh),  //spacer
					outNumbers= {|i|
						RedStaticText(nil, nil, "o"++i)
						.fixedWidth_(RedGUICVMultiSliderView.defaultWidth).maxHeight_(lh)
					}.dup(nOut),
					View().maxHeight_(lh)  //spacer
				].flat)
			).margins_(4).spacing_(4);

			makeMirror= {|v, i|
				var ref= redMatrixMixer.cvs[("o"++i).asSymbol].copy;
				var rect= mainGUI[i].view.bounds.copy;
				var vi= RedGUICVMultiSliderView(v, rect, ref, nil, nil, (num: ref.value.size));
				vi.view.action_({vi.view.action+{vi.save}});
				vi;
			};
			tab.view.toFrontAction_({
				mirrorGUI= {|i|
					makeMirror.value(tab.views[1], i);
				}.dup(nOut);
			});

			win.onClose_({controllers.do{|x| x.remove}});
			CmdPeriod.doOnce({this.close});
		}).play(AppClock);
	}

	close {
		if(win.isClosed.not, {win.close});
	}
}
