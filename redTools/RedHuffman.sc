//redFrik

RedHuffman {
	classvar <>tree, <>dict, <>pad;

	*compress {|arr|
		var out, n0, n1;

		//--build a forest of small trees
		tree= List[];
		arr.do{|val|
			var n= tree.detect{|x| x.key==val};
			if(n.isNil, {
				tree.add((val -> 1));			//value and counter association
			}, {
				n.value= n.value+1;				//increase counter
			});
		};

		//--collect greedy as a single big tree
		while({tree.size>2}, {
			n0= this.prTakeMinimum;
			n1= this.prTakeMinimum;
			tree.add((List[n0, n1] -> (n0.value+n1.value)));
		});

		//--remove counters from tree
		tree= this.prRebuildTree(tree, List.new);

		//--build dictionary
		dict= ();
		this.prBuildDict(tree, "");

		//--create binary string
		out= "";
		arr.do{|val|
			out= out++dict[val];
		};
		^out;
	}
	*decompress {|str|
		var out= List[], tmp= tree;
		str.do{|x|
			tmp= tmp[x.digit];
			if(tmp.isCollection.not, {
				out.add(tmp);
				tmp= tree;
			});
		};
		^out.array;
	}
	*binaryStringToBytes {|str|
		pad= 0;
		^str.clump(8).collect{|x|
			while({x.size<8}, {
				x= x++0;
				pad= pad+1;
			});
			x.sum{|bit, i| 1<<(x.size-1-i)*bit.digit};
		};
	}
	*bytesToBinaryString {|arr|
		var str= arr.collect{|x|
			x.asStringToBase(2, 8);
		}.join;
		^str.copyRange(0, str.size-1-pad);
	}


	//--private
	*prTakeMinimum {
		var ii, nn, min= 2147483647;
		tree.do{|x, i|
			if(x.value<min, {
				nn= x;
				ii= i;
				min= x.value;
			});
		};
		tree.removeAt(ii);
		^nn;
	}

	*prRebuildTree {|list, res|
		list.do{|x|
			res.add(
				if(x.key.isCollection, {
					this.prRebuildTree(x.key, List.new);
				}, {
					x.key;
				})
			);
		};
		^res;
	}

	*prBuildDict {|list, str|
		list.do{|x, i|
			if(x.isCollection, {
				this.prBuildDict(x, str++i);
			}, {
				dict.put(x, str++i);
			});
		};
	}
}
