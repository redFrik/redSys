a [Quark](https://supercollider-quarks.github.io/quarks/) for [SuperCollider](https://supercollider.github.io)

install it from within supercollider with the command `Quarks.install("redSys")` and then recompile.

# redSys

red system including mixers, effects, instruments, presets, tools, gui

dependencies: TabbedView quark (should be automatically installed)

see redSysOverview in help

## changelog:

* 210610 - huge speed improvements to most data compression classes, RedGIF massive speed improvements and important fixes
* 210526 - RedBMP add support for Image
* 180330 - RedIntelHex checksum bugfix
* 180317 - redModule and redEffectsRack: added global lag
* 180218 - replaced a few .interpret in hex-string to integer conversion
* 180116 - new readme. dropped Conductor quark dependency. cleanup helpfiles. fixed RedTapeRecorder. minor bugfix to RedTest. major rewrite of redMatrixMixer, redMix, redMixer, redGUICV, redModule, redEffectsRack
* 151124 - RedTapTempoGUI - alpha
* 131228 - added default values for RedTime-new and some more examples
* 130927 - minor speed up by replacing & with bitAnd, << with leftShift and >> with rightShift
* 130902 - added RedTween and RedInterpolator
* 130807 - added RedArduino
* 130612 - added RedLFSR4 and RedLFSR4BitStream
* 130524 - added RedIntelHex
* 130522 - added RedManchesterCode, RedDifferentialManchesterCode, RedDifferentialManchesterCodeNegative. removed all old html helpfiles and the redSys.html overview
* 130226 - added RedBencode
* 130131 - RedTest: added dur arguments
* 130108 - RedTest: added amp arguments
* 121126 - RedEfxKomp: postGain was in the wrong place, changed default postGain from 0.25 to 1. RedTest: speaker and speaker2 classmethods also boot default server, fix initRedTestSF to read correct sound
* 121007 - RedMixer: thanks to Roberto Lombardo .store now also saves effect inserts and their settings
* 121005 - RedMixer: improved helpfile with more examples, bugfix for when only one stereo input, changes for save&recall
* 121001 - added RedTempoClockGUI
* 120825 - fix crashing bug in RedToolsMenu under qt
* 120608 - many gui windows did not show under sc3.5 cocoa osx due to alpha channel bug, now fixed
* 120317 - some minor fixes to look better with gui qt, fix for 3.5 random helpfile lookup in redtoolsmenu, removed RedEfxFFTA from redEffects because it needs sc3-plugins installed
* 120221 - added RedOnePole, RedSlide
* 120208 - added RedRamp
* 120205 - added RedLine
* 120122 - added RedImpulse
* 111115 - added RedEfxFreq, RedEfxRvrs, RedOverdub
* 111113 - added RedEfxAuto, RedEfxBoom, RedEfxFFTA, RedEfxKomp, RedEfxPch2, RedEfxPchN, RedEfxVoco. minor fix to RedAbstractModule - avoid multiple synthdef builds with a pdDef variable
* 110927 - all helpfiles converted to scdoc format
* 110920 - some first fixes to sc3.5 qt gui
* 110216 - added RedALF class. minor fix to RedAutoScale - changed to inf and -inf for min and max
* 101129 - RedBMP bugfixes, added support for reading 16bit files, added writing and creating 2, 4, 8, 16 and 24bit files.
* 101121 - RedFrik now needs GlitchRHPF from sc3-plugins. added RedBMP class. changed RedGIF to use smoothing_ instead of setSmoothing
* 100907 - some changes to RedToolsMenu, no separate Red menu any longer, using Library.
* 100728 - added RedSF, RedEfxBit8, RedEfxBitx, RedEfxRoom, RedEfxZzzz. added functions in comments for each effect, useful for pasting into ndef filters
* 100703 - changed some more memStore to add
* 100702 - changed some store and memStore to add. improvements to RedToolMenu: normalize and userview. bugfix in RedMatrixMixerGUI
* 100210 - minor changes to RedToolsMenu. bugfix for RedEfxComp
* 100124 - added RedGIF and RedBitStream. minor optimisation to RedLZW
* 100109 - bugfix for RedLZ78, swapped distance and length for RedLZ77 and RedLZSS, small optimisation to RedLZW
* 100107 - complete rewrite of RedLZ77 and RedLZSS
* 100101 - added RedLZ78. improved helpfiles for all compressors. RedHuffman changed from encode/decode to compress/decompress and to have it take all kinds of data. RedLZW now works with 8bit integers
* 091230 - added RedLZ77, RedLZSS. modified RedRLE to take arrays instead of strings
* 091229 - added RedRLE
* 091226 - added RedHuffman
* 091025 - some changes to RedToolsMenu. added Redraw
* 091009 - new effects: RedEfxDely, RedEfxWave, RedEfxDelp, RedEfxTanh. added a limiter to RedEfxTank. argument name changes for RedEfxOcta and RedEfxGuit. added template to RedEffectModule.sc file
* 090916 - added RedEfxTanh, RedEfxWave and a template in the RedEffectModule.sc file
