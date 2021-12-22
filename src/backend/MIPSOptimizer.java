package backend;

import backend.mips.*;
import frontend.stdir.Exp;

import java.util.ArrayList;

/**
 * 窥孔优化
 */
public class MIPSOptimizer {
    ArrayList<MipsCode> mipsCodes = new ArrayList<>();

    public MIPSOptimizer(ArrayList<MipsCode> mipsCodes) {
        this.mipsCodes = mipsCodes;
        deleteCodes();
    }

    void deleteCodes() {
        while (true) {
            ArrayList<MipsCode> lastmips = new ArrayList<>(mipsCodes);
            ArrayList<MipsCode> newmips = new ArrayList<>();
            for (int i = 0; i < mipsCodes.size(); i++) {
                Calculate calculate1 = judgeWhileIMore(i);
                if (calculate1 != null) {
                    newmips.add(calculate1);
                    i += 2;
                    continue;
                }
                MipsCode assign1 = judgeAssign(i);
                if (assign1 != null) {
                    newmips.add(assign1);
                    i += 1;
                    continue;
                }
                newmips.add(mipsCodes.get(i));
            }
            mipsCodes = newmips;
            if (lastmips.size() == mipsCodes.size()) break;
        }
    }

    /**
     * move reg1 reg2
     * addi reg1 reg1 value
     * move reg2 reg1
     * ==================
     * addi reg2 reg2 0x1
     */
    Calculate judgeWhileIMore(int i) {
        if (i > mipsCodes.size() - 3) return null;
        MipsCode first = mipsCodes.get(i);
        MipsCode second = mipsCodes.get(i + 1);
        MipsCode third = mipsCodes.get(i + 2);
        if (first instanceof Assign && second instanceof Calculate && third instanceof Assign &&
                ((Calculate) second).getType() == 0 && ((Assign) first).getType() == 1 && ((Assign) third).getType() == 1) {
            Namespace reg1 = ((Assign) first).getSym1();
            Namespace reg2 = ((Assign) first).getSym2();
            Namespace reg3 = ((Calculate) second).getReg1();
            Namespace reg4 = ((Calculate) second).getReg2();
            Namespace reg5 = ((Assign) third).getSym1();
            Namespace reg6 = ((Assign) third).getSym2();
            String op = ((Calculate) second).getOp();
            if (reg1.equals(reg3) && reg3.equals(reg4) && reg4.equals(reg6)
                    && reg2.equals(reg5) && reg2.getReg() >= 16 && reg2.getReg() <= 23
                    && (reg1.getReg() < 16 || reg1.getReg() > 23)
                    && (op.equals("addi") || op.equals("subi"))
                    && !useReg(reg1, i + 3)) {
                return new Calculate(reg2, reg2, ((Calculate) second).getValue(), op);
            }
        }
        return null;
    }

    /**
     * move reg1 reg2
     * move reg3 reg4
     */
    MipsCode judgeAssign(int i) {
        if (i > mipsCodes.size() - 2) return null;
        MipsCode first = mipsCodes.get(i);
        MipsCode second = mipsCodes.get(i + 1);
        if (first instanceof Assign && second instanceof Assign) {
            Namespace reg1 = ((Assign) first).getSym1();
            Namespace reg2 = ((Assign) first).getSym2();
            Namespace reg3 = ((Assign) second).getSym1();
            Namespace reg4 = ((Assign) second).getSym2();
            if (reg1.equals(reg4) && !useReg(reg1, i + 2)) {
                return new Assign(reg3, reg2);
            }
        }
        return null;
    }


    boolean useReg(Namespace reg, int pos) {
        if (reg.isGlobal()) return true;
        for (int i = pos; i < mipsCodes.size(); i++) {
            MipsCode mipsCode = mipsCodes.get(i);
            if (mipsCode instanceof Assign) {
                if (reg.equals(((Assign) mipsCode).getSym2())) {
                    return true;
                }
                if (reg.equals(((Assign) mipsCode).getSym1())) {
                    return false;
                }
            }
            if (mipsCode instanceof BrJump) {
                if (reg.equals(((BrJump) mipsCode).getReg1()) || reg.equals(((BrJump) mipsCode).getReg2())) return true;
            }
            if (mipsCode instanceof Calculate) {
                if (reg.equals(((Calculate) mipsCode).getReg2()) || reg.equals(((Calculate) mipsCode).getReg3()))
                    return true;
                if (reg.equals(((Calculate) mipsCode).getReg1())) return false;
            }
            if (mipsCode instanceof LoadStore) {
                if ((!((LoadStore) mipsCode).isload()) &&
                        (reg.equals(((LoadStore) mipsCode).getReg2()) || reg.equals(((LoadStore) mipsCode).getReg1())))
                    return true;
                if (((LoadStore) mipsCode).isload() &&
                        (reg.equals(((LoadStore) mipsCode).getReg2())))
                    return true;
                if (((LoadStore) mipsCode).isload() &&
                        (reg.equals(((LoadStore) mipsCode).getReg1())))
                    return false;
            }
            if (mipsCode instanceof MTF) {
                if (((MTF) mipsCode).getTransType() == 1 && reg.equals(((MTF) mipsCode).getReg())) return false;
                if (((MTF) mipsCode).getTransType() == 0 && reg.equals(((MTF) mipsCode).getReg())) return true;
            }
            if (mipsCode instanceof StackManage) {
                if (((StackManage) mipsCode).getType() == 0 && reg.equals(((StackManage) mipsCode).getReg()))
                    return true;
                if (((StackManage) mipsCode).getType() == 1 && reg.equals(((StackManage) mipsCode).getReg()))
                    return false;
            }
        }
        return false;
    }

    public ArrayList<MipsCode> getMipsCodes() {
        return mipsCodes;
    }
}
