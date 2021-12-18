package frontend.irgen.optimize;

import frontend.stdir.FuncDecl;
import frontend.stdir.IRCode;
import frontend.stdir.Label;
import frontend.stdir.Sym;

import java.util.ArrayList;

public class CompileUnit {
    ArrayList<IRCode> irCodes = new ArrayList<>();
    ArrayList<IRCode> globaldecls = new ArrayList<>();
    ArrayList<FuncBlock> funcBlocks = new ArrayList<>();

    public CompileUnit(ArrayList<IRCode> inirCodes) {
        irCodes = inirCodes;
        divide();
        irCodes = new ArrayList<>();
        irCodes.addAll(globaldecls);
        for (FuncBlock funcBlock : funcBlocks) {
            irCodes.addAll(funcBlock.getIrCodes());
        }
    }

    void divide() {
        int i = 0;
        if (irCodes.get(0) instanceof Label
                && ((Label) irCodes.get(0)).getName().equals("decline_init_begin")) {
            i = 1;
            while (!(irCodes.get(i) instanceof FuncDecl) && !(irCodes.get(i) instanceof Label)) {
                globaldecls.add(irCodes.get(i));
                i++;
            }
        }
        ArrayList<IRCode> funcircodes = new ArrayList<>();
        for (int j = i; j < irCodes.size(); j++) {
            if (irCodes.get(j) instanceof FuncDecl) {
                funcircodes.add(irCodes.get(j));
                for (int k = j + 1; k < irCodes.size(); k++) {
                    if (!(irCodes.get(k) instanceof FuncDecl ||
                            (irCodes.get(k) instanceof Label &&
                                    ((Label) irCodes.get(k)).getName().equals("block_main_begin")))) {
                        funcircodes.add(irCodes.get(k));
                    } else {
                        j = k - 1;
                        break;
                    }
                }
                funcBlocks.add(new FuncBlock(funcircodes, false));
                funcircodes.clear();
            } else if (irCodes.get(j) instanceof Label &&
                    ((Label) irCodes.get(j)).getName().equals("block_main_begin")) {
                for (int k = j; k < irCodes.size(); k++) {
                    funcircodes.add(irCodes.get(k));
                }
                funcBlocks.add(new FuncBlock(funcircodes, true));
                funcircodes.clear();
                break;
            }
        }
    }

    public ArrayList<FuncBlock> getFuncBlocks() {
        return funcBlocks;
    }

    public ArrayList<IRCode> getGlobaldecls() {
        return globaldecls;
    }

    public ArrayList<IRCode> getIrCodes() {
        return irCodes;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("");
        for (IRCode irCode : globaldecls) {
            sb.append(irCode.toString());
        }
        for (FuncBlock funcBlock : funcBlocks) {
            sb.append(funcBlock.toString());
        }
        return sb.toString();
    }
}
