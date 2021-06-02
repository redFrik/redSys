//redFrik

//related: RedBMP

RedGIF {
	var	<>type,  //string

	<>width, <>height,  //integers
	<>resolution,  //integer
	<>sorted,  //integer
	<>background,  //color
	<>aspectRatio,  //integer
	<>depth,  //integer
	<>globalColorMap,  //array of colors

	<>controls,  //array of RedGIFControl
	<>comments,  //array of strings

	<>appId, <>appCode,  //strings
	<>appData,  //array

	<>images,  //array of images

	codeSize;  //integer used for lzw decompression

	*read {|path, action|
		^super.new.read(path, action);
	}
	read {|path, action|
		path= path.standardizePath;
		if(File.exists(path), {
			this.prRead(path);
			{action.value(this)}.fork;
		}, {"%: file % not found".format(this.class.name, path).error});
	}

	makeWindow {|bounds, rate= 1.0|
		var b= bounds ?? {Rect(300, 300, width, height)};
		var win= Window(this.class.name, b);
		var image= Image(width, height).interpolation_(\fast);
		var index= 0, img, row;
		var interlace= {|passes, step, offset|
			passes.do{|j|
				img.data.copyRange(row*width, row+1*width-1).do{|col, i|
					image.setColor(col, i, j*step+offset);//TODO test and check transparent
				};
				row= row+1;
			};
		};
		win.drawFunc= {
			var i, j, size, x, y, col;
			var left, right, top, bottom;
			var clear= Color.clear;
			img= images.wrapAt(index);
			win.view.background= background?clear;
			if(img.interlaced, {
				row= 0;
				interlace.value((height+7).div(8), 8, 0);
				interlace.value((height+3).div(8), 8, 4);
				interlace.value((height+1).div(4), 4, 2);
				interlace.value(height.div(2), 2, 1);
			}, {
				i= 0;
				j= 0;
				left= img.bounds.left;
				right= img.bounds.width+left;
				top= img.bounds.top;
				bottom= img.bounds.height+top;
				size= width*height;
				while({i<size}, {
					x= i.mod(width);
					y= i.div(width);
					if(x>=left and:{x<right and:{y>=top and:{y<bottom}}}, {
						col= img.data[j];
						if(img.control.transparentFlag and:{col==img.control.transparent}, {
							col= clear;
						});
						image.setColor(col, x, y);
						j= j+1;
					});
					i= i+1;
				});
				Pen.drawImage(Rect(0, 0, win.bounds.width, win.bounds.height), image);
				switch(img.control.disposalMethod,
					1, nil,  //leave image for overwrite
					2, {image.fill(background)},  //restore background
					3, {"TODO disposalMethod==3 restore previous".postln}
				);
			});
		};
		if(images.size>1, {  //if animated gif
			Routine({
				while({win.isClosed.not}, {
					//TODO check userinputflag here
					if(img.notNil, {
						(img.control.duration*0.01/rate).max(0.01).wait;
						win.refresh;
						index= index+1;
					}, {
						0.01.wait;
					});
				});
			}).play(AppClock);
		});
		win.onClose= {image.free};
		win.front;
		^win;
	}

	//--private
	prRead {|path|
		var file= File(path, "rb");
		var separator;
		this.prReadHeader(file);
		if(type=="GIF87a" or:{type=="GIF89a"}, {
			this.prReadLogicalScreenDescriptor(file);
			images= [];
			"reading".post;
			while({separator= file.getInt8.bitAnd(0xff); separator!=0x3b}, {
				switch(separator,
					0x21, {this.prReadExtensionBlock(file)},
					0x2c, {this.prReadImageDescriptor(file); ".".post},
					0x00, {},
					{"%: unknown separator %".format(this.class.name, separator).warn}
				);
			});
			file.close;
			"\nuncompressing".post;
			images.do{|image, i|  //copy over control objects to image objects
				image.data= this.prDecode(image.data).collect{|x| image.colorMap[x]};
				".".post;
				if(i<controls.size, {  //set corresponding control object
					image.control= controls[i];
				}, {
					if(controls.size>0, {  //use last found control object
						image.control= controls.last;
					}, {  //use default control
						image.control= RedGIFControl.new;
						image.control.duration= 0;
						image.control.transparent= Color.clear;
						image.control.disposalMethod= 0;
						image.control.userInputFlag= false;
						image.control.transparentFlag= true;
					});
				});
			};
			"".postln;
		}, {
			"%: type % not recognized".format(this.class.name, type).error;
			file.close;
		});
	}

	prReadHeader {|file|  //header with signature and version (6bytes)
		type= {file.getChar}.dup(6).join;
	}

	prReadLogicalScreenDescriptor {|file|  //logical screen descriptor (7bytes)
		var flags;
		width= file.getInt16LE;  //logical screen width
		height= file.getInt16LE;  //logical screen height
		flags= file.getInt8.bitAnd(0xff);
		background= file.getInt8.bitAnd(0xff);  //background color index
		aspectRatio= file.getInt8.bitAnd(0xff);  //pixel aspect ratio
		if(aspectRatio!=0, {
			aspectRatio= aspectRatio+15/64;
		});
		depth= flags.bitAnd(0x07)+1;  //size of global color table
		resolution= flags.rightShift(4).bitAnd(0x07)+1;
		sorted= flags.rightShift(3).bitAnd(0x01);
		if(flags.bitTest(7), {  //global color map (3bytes*numColors)
			globalColorMap= {
				Color(*file.read(Int8Array[0, 0, 0]).bitAnd(0xff)/255);
			}.dup(2.leftShift(depth-1));
			background= globalColorMap[background];
		}, {
			globalColorMap= [];
			background= nil;
		});
	}

	prReadExtensionBlock {|file|  //extension block (?bytes)
		var flags, blockSize, ctrl;
		switch(file.getInt8.bitAnd(0xff),
			0x01, {
				"%: plain text not yet supported".format(this.class.name).warn;
				blockSize= file.getInt8.bitAnd(0xff);
				if(blockSize!=12, {"blockSize in plaintext not 12".warn});
				("\ttextgrid left pos:"+file.getInt16.bitAnd(0xff)).postln;
				("\ttextgrid top pos :"+file.getInt16.bitAnd(0xff)).postln;
				("\ttextgrid width   :"+file.getInt16.bitAnd(0xff)).postln;
				("\ttextgrid height  :"+file.getInt16.bitAnd(0xff)).postln;
				("\tchar cell width  :"+file.getInt8.bitAnd(0xff)).postln;
				("\tchar cell height :"+file.getInt8.bitAnd(0xff)).postln;
				("\tforecolor index  :"+file.getInt8.bitAnd(0xff)).postln;
				("\tbackcolor index  :"+file.getInt8.bitAnd(0xff)).postln;
				while({blockSize= file.getInt8.bitAnd(0xff); blockSize!=0}, {
					"plaintext block...".postln;
					{file.getChar}.dup(blockSize).join.postln;
				});
			},
			0xf9, {  //graphic control extension block
				ctrl= RedGIFControl.new;
				blockSize= file.getInt8.bitAnd(0xff);
				flags= file.getInt8.bitAnd(0xff);  //packed field
				ctrl.duration= file.getInt16LE;  //delay time (1/100ths of a second)
				ctrl.transparent= globalColorMap[file.getInt8.bitAnd(0xff)];
				ctrl.disposalMethod= flags.bitAnd(2r00011100).rightShift(2);
				ctrl.userInputFlag= flags.bitTest(1);  //wait for user input flag
				ctrl.transparentFlag= flags.bitTest(0);  //transparency flag
				controls= controls.add(ctrl);
			},
			0xfe, {  //comment extension block
				while({blockSize= file.getInt8.bitAnd(0xff); blockSize!=0}, {
					comments= comments.add({file.getChar}.dup(blockSize).join);
				});
			},
			0xff, {  //application extension block
				blockSize= file.getInt8.bitAnd(0xff);
				appId= {file.getChar}.dup(8).join;
				appCode= {file.getChar}.dup(3).join;
				while({blockSize= file.getInt8.bitAnd(0xff); blockSize!=0}, {
					appData= file.read(Int8Array.newClear(blockSize)).bitAnd(0xff);
				});
			},
			{"%: extensionblock - code not recognised".format(this.class.name).warn}
		);
	}

	prReadImageDescriptor {|file|  //local descriptor (10bytes)
		var flags, bounds, image;
		bounds= {file.getInt16LE}.dup(4).asRect;
		flags= file.getInt8.bitAnd(0xff);
		image= RedGIFImage(bounds, flags);

		if(image.hasColorMap, {
			image.colorMap= {
				Color(*file.read(Int8Array[0, 0, 0]).bitAnd(0xff)/255);
			}.dup(2.leftShift(image.depth-1));
		}, {
			image.colorMap= globalColorMap;
		});

		//--read table based image data
		codeSize= file.getInt8.bitAnd(0xff);
		image.data= this.prReadData(file);
		images= images.add(image);
	}

	prReadData {|file|
		var blockSize, data= List.new;
		while({blockSize= file.getInt8.bitAnd(0xff); blockSize!=0}, {
			data.addAll(file.read(Int8Array.newClear(blockSize)).bitAnd(0xff));
		});
		^data
	}

	//--lzw decompression
	prDecode {|arr|
		var clearCode= 2.leftShift(codeSize-1);
		var endCode= clearCode+1;
		var cnt, dict, tempCodeSize;
		var clear= {
			cnt= endCode+1;
			dict= Array.newClear(4096).overWrite({|i| [i]}.dup(cnt));
			tempCodeSize= codeSize+1;
		};
		var str= Int8Array.fill(arr.size*8, {|i| arr[i.div(8)].rightShift(i.mod(8)).bitAnd(1)});
		var old, val, out= List[], sub, more= true;
		var k, index= 0;
		clear.value;
		while({more}, {
			k= 0;
			tempCodeSize.do{|i|
				k= k+(str[index]*2.leftShift(i-1));
				index= index+1;
			};
			if(k==clearCode, {
				clear.value;
				if(index>tempCodeSize, {
					index= index+3;
				});
			}, {
				if(k==endCode, {
					more= false;
				}, {
					if(dict[k].notNil, {
						sub= dict[k];
					}, {
						sub= dict[old]++val;
					});
					out.addAll(sub);
					val= sub[0];
					if(old.notNil, {
						dict[cnt]= dict[old]++val;
						cnt= cnt+1;
					});
					old= k;
					if(cnt==2.leftShift(tempCodeSize-1), {
						tempCodeSize= tempCodeSize+1;
						if(tempCodeSize==13, {
							tempCodeSize= codeSize+1;
							old= nil;
							val= nil;
						});
					});
				});
			});
		});
		^out;
	}
}

RedGIFImage {
	var <bounds, <flags, <>colorMap, <>data, <>control;
	*new {|bounds, flags| ^super.newCopyArgs(bounds, flags)}
	depth {^flags.bitAnd(0x07)+1}
	hasColorMap {^flags.bitTest(7)}
	interlaced {^flags.bitTest(6)}
	sorted {^flags.bitTest(5)}
}

RedGIFControl {
	var	<>duration,  //integer
	<>transparent,  //color
	<>disposalMethod,  //integer
	<>userInputFlag,  //boolean
	<>transparentFlag;  //boolean
}
