package frontend.irgen.optimize;

import frontend.irgen.symtable.Var;
import frontend.stdir.*;

import java.util.ArrayList;

/**
 * 将数组定义和数组存取全部展开成原子表达式
 */
public class Flatter {
    ArrayList<IRCode> irCodes;
    ArrayList<IRCode> outircodes = new ArrayList<>();

    public Flatter(ArrayList<IRCode> irCodes) {
        this.irCodes = irCodes;
        flatter();
    }

    public void flatter() {
        for (int i = 0; i < irCodes.size(); i++) {
            if (irCodes.get(i) instanceof ArrayDecl) {
                ArrayDecl arrayDecl = (ArrayDecl) irCodes.get(i);
                if (arrayDecl.getSym().toString().charAt(0) == '%' && arrayDecl.getHasInitVal() && !arrayDecl.getArrayval().isEmpty()) {
                    ArrayList<Sym> syms = arrayDecl.getArrayval();
                    arrayDecl.setArrayval(new ArrayList<>());
                    arrayDecl.setHasInitVal(false);
                    outircodes.add(irCodes.get(i));
                    for (int j = 0; j < syms.size(); j++) {
                        outircodes.add(new ArrayLoadStore(syms.get(j), new Sym(j), new Var(arrayDecl.getSym().toString().substring(1), 1), 0));
                    }
                } else {
                    outircodes.add(irCodes.get(i));
                }
            } else if (irCodes.get(i) instanceof ArrayLoadStore && (((ArrayLoadStore) irCodes.get(i)).getType() == 1 || ((ArrayLoadStore) irCodes.get(i)).getType() == 3)) {
                ArrayList<IRCode> ans = ((ArrayLoadStore) irCodes.get(i)).trans();
                outircodes.addAll(ans);
                outircodes.add(irCodes.get(i));
            } else {
                outircodes.add(irCodes.get(i));
            }
        }
        irCodes = outircodes;
    }

    public void processLabel() {
        outircodes = new ArrayList<>();
        //去掉连续的标签
        ArrayList<Integer> removepos = new ArrayList<>();
        for (int i = 0; i < outircodes.size(); i++) {
            if (outircodes.get(i) instanceof Label) {
                int j = i + 1;
                while (j < outircodes.size() && outircodes.get(j) instanceof Label) {
                    j++;
                }
                if (j > i) {
                    for (int k = i + 1; k < j; k++) {
                        removepos.add(k);
                        for (int m = 0; m < outircodes.size(); m++) {
                            if (outircodes.get(m) instanceof CondBranch && ((CondBranch) outircodes.get(m)).getLabel().equals(outircodes.get(k))) {
                                ((CondBranch) outircodes.get(m)).setLabel((Label) outircodes.get(i));
                            }
                            if (outircodes.get(m) instanceof Jump && ((Jump) outircodes.get(m)).getLabel().equals(outircodes.get(k))) {
                                ((Jump) outircodes.get(m)).setLabel((Label) outircodes.get(i));
                            }
                        }
                    }
                }
                i = j - 1;
            }
        }
        //System.out.println(removepos);
        ArrayList<IRCode> newoutircodes = new ArrayList<>();
        for (int i = 0; i < outircodes.size(); i++) {
            if (!removepos.contains(i)) {
                /*
                if (outircodes.get(i) instanceof Label) {
                    String name = ((Label) outircodes.get(i)).getName();
                    if (name.startsWith("while") && (name.endsWith("begin"))) {
                        int pos = name.indexOf("_begin");
                        String ident = name.substring(0, pos);
                        boolean flag = false;
                        for (int j = i + 1; j < outircodes.size(); j++) {
                            if (outircodes.get(j) instanceof Label && ((Label) outircodes.get(j)).getName().startsWith(ident)) {
                                flag = true;
                                break;
                            }
                        }
                        if (flag) {
                            newoutircodes.add(outircodes.get(i));
                        }
                    }
                    if (name.startsWith("while") && (name.endsWith("end"))) {
                        int pos = name.indexOf("_end");
                        String ident = name.substring(0, pos);
                        boolean flag = false;
                        for (int j = i - 1; j >= 0; j--) {
                            if (outircodes.get(j) instanceof Label && ((Label) outircodes.get(j)).getName().startsWith(ident)) {
                                flag = true;
                                break;
                            }
                        }
                        if (flag) {
                            newoutircodes.add(outircodes.get(i));
                        }
                        continue;
                    }
                }
                */
                newoutircodes.add(outircodes.get(i));
            } else if (removepos.contains(i) && outircodes.get(i) instanceof Label) {
                String name = ((Label) outircodes.get(i)).getName();
                if (name.equals("mainfunc_ret_0") || name.equals("decline_init_begin")) {
                    newoutircodes.add(outircodes.get(i));
                }
                if (name.length() >= 10) {
                    String prefix = name.substring(0, 10);
                    if (name.startsWith("block") || prefix.equals("block_func") || prefix.equals("block_main")) {
                        newoutircodes.add(outircodes.get(i));
                    }
                }
            }
        }
        irCodes = newoutircodes;


//        outircodes = new ArrayList<>();
//        //去掉一个Jump语句的下一条是要jump到的label
//        for (int i = 0; i < irCodes.size(); i++) {
//            if (irCodes.get(i) instanceof Jump && i + 1 < irCodes.size() && irCodes.get(i + 1) instanceof Label
//                    && ((Jump) irCodes.get(i)).getLabel().equals(irCodes.get(i + 1))) {
//                outircodes.add(irCodes.get(i + 1));
//                i = i + 1;
//            } else {
//                outircodes.add(irCodes.get(i));
//            }
//        }
//        irCodes = outircodes;
    }

    public ArrayList<IRCode> getIrCodes() {
        return irCodes;
    }
}
