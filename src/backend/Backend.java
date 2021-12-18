package backend;

import backend.mips.IRTranslator;
import frontend.IOtool;
import frontend.irgen.IRGenerater;
import frontend.irgen.optimize.CompileUnit;
import frontend.stdir.IRCode;
import frontend.stdir.Sym;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class Backend {

    private ArrayList<IRCode> irCodes;
    private CompileUnit compileUnit;

    public Backend(ArrayList<IRCode> irCodes, int level, CompileUnit compileUnit) throws IOException {
        if (level <= 0 || irCodes == null) return;
        if (level == 1) {
            this.irCodes = irCodes;
            IOtool iotool = new IOtool();
            IRTranslator irTranslator = new IRTranslator(irCodes);
            iotool.output("mips.txt", irTranslator.getMipsout());
        } else {
            this.irCodes = irCodes;
            IOtool iotool = new IOtool();
            IR2Mips ir2Mips = new IR2Mips(compileUnit);
            iotool.output("mips.txt", ir2Mips.getMipsout());
        }
    }

    private ArrayList<String> splitByLine(String str) {
        return new ArrayList<String>(Arrays.asList(str.split("\\n")));
    }
}
