package frontend.irgen;

import frontend.IOtool;
import frontend.irgen.optimize.*;
import frontend.stdir.IRCode;

import java.util.ArrayList;

public class Optimizer {
    ArrayList<IRCode> irCodes;
    int tmpnum;
    CompileUnit compileUnit;

    public Optimizer(ArrayList<IRCode> inirCodes, int tmpnum) {
        irCodes = inirCodes;
        Flatter flatter = new Flatter(irCodes);
        irCodes = flatter.getIrCodes();
        Divoptimizer divoptimizer = new Divoptimizer(irCodes, tmpnum);
        irCodes = divoptimizer.getIrCodes();
        outputIrcodes();
        compileUnit = new CompileUnit(irCodes);
        IOtool.output("compileunit.txt", compileUnit.toString());
        irCodes = compileUnit.getIrCodes();
        //outputIrcodes();
        this.tmpnum = tmpnum;
    }

    public ArrayList<IRCode> getIrCodes() {
        return irCodes;
    }

    void outputIrcodes() {
        StringBuilder sb = new StringBuilder("");
        for (IRCode irCode : irCodes) {
            sb.append(irCode.toString());
        }
        IOtool.output("ircode_optimizer.txt", sb.toString());
    }

    public CompileUnit getCompileUnit() {
        return compileUnit;
    }
}
