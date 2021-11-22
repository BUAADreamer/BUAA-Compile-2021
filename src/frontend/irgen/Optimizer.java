package frontend.irgen;

import frontend.IOtool;
import frontend.stdir.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class Optimizer {
    ArrayList<IRCode> irCodes;
    ArrayList<ArrayList<IRCode>> basicblocks = new ArrayList<>();
    private int bbpos = 0; //basic block rangescan posptr
    HashSet<Integer> firstposls = new HashSet<>();
    HashSet<Integer> brjtocodeposls = new HashSet<>();
    String basicblockoutput = new String("");
    IOtool iOtool = new IOtool();

    public Optimizer(ArrayList<IRCode> irCodes) {
        this.irCodes = irCodes;
        parsebasicblock();
    }

    private void parsebasicblock() {
        IRCode lastcode = irCodes.get(0);
        while (bbpos < irCodes.size()) {
            IRCode curcode = irCodes.get(bbpos);
            if (curcode instanceof Label) {
                //process global var decline
                if (((Label) curcode).getName().equals("decline_init_begin")) {
                    firstposls.add(bbpos);
                    /*
                    while (!(irCodes.get(bbpos) instanceof FuncDecl)
                            && !(irCodes.get(bbpos) instanceof Label
                            && ((Label) irCodes.get(bbpos)).getName().equals("block_func_min_begin"))) {
                        bbpos++;
                    }
                    */
                }
            } else if (curcode instanceof CondBranch) {
                int pos = findlabelpos(((CondBranch) curcode).getLabel());
                firstposls.add(pos);
            } else if (curcode instanceof Jump) {
                int pos = findlabelpos(((Jump) curcode).getLabel());
                firstposls.add(pos);
            } else if (curcode instanceof FuncDecl) {
                firstposls.add(bbpos);
            }
            if (lastcode instanceof CondBranch || lastcode instanceof Jump) {
                firstposls.add(bbpos);
            } else if (brjtocodeposls.contains(bbpos)) {
                firstposls.add(bbpos);
            }
            lastcode = curcode;
            bbpos++;
        }
        bbpos = 0;
        while (bbpos < irCodes.size()) {
            if (firstposls.contains(bbpos)) {
                ArrayList<IRCode> block = new ArrayList<>();
                block.add(irCodes.get(bbpos));
                bbpos++;
                while (!firstposls.contains(bbpos) && bbpos < irCodes.size()) {
                    block.add(irCodes.get(bbpos));
                    bbpos++;
                }
                basicblocks.add(block);
            }
        }
        StringBuilder stringBuilder = new StringBuilder("");
        for (int i = 0; i < basicblocks.size(); i++) {
            if (i != 0) stringBuilder.append("********************************************\n");
            stringBuilder.append("basic block " + i + "\n");
            for (int j = 0; j < basicblocks.get(i).size(); j++) {
                stringBuilder.append(basicblocks.get(i).get(j).toString());
            }
        }
        basicblockoutput = stringBuilder.toString();
        iOtool.output("basiclock_ircode.txt", basicblockoutput);
    }

    private int findlabelpos(Label label) {
        for (int i = 0; i < irCodes.size(); i++) {
            if (irCodes.get(i) instanceof Label && ((Label) irCodes.get(i)).equals(label)) {
                return i;
            }
        }
        return 0;
    }

    public ArrayList<IRCode> getIrcodes() {
        return irCodes;
    }
}
