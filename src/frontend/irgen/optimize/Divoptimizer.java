package frontend.irgen.optimize;

import frontend.stdir.Exp;
import frontend.stdir.IRCode;
import frontend.stdir.Sym;

import java.util.ArrayList;
import java.util.Collection;

public class Divoptimizer {
    ArrayList<IRCode> irCodes;
    int tmpnum = 0;

    public Divoptimizer(ArrayList<IRCode> irCodes, int tmpnum) {
        this.irCodes = irCodes;
        this.tmpnum = tmpnum;
        optimize();
    }

    private void optimize() {
        ArrayList<IRCode> newircodes = new ArrayList<>();
        for (int i = 0; i < irCodes.size(); i++) {
            if (irCodes.get(i) instanceof Exp
                    && (((Exp) irCodes.get(i)).getOp().equals("/")
                    || ((Exp) irCodes.get(i)).getOp().equals("%"))
                    && ((Exp) irCodes.get(i)).getRsym2().getType() == 0) {
                newircodes.addAll(mydiv((Exp) irCodes.get(i)));
            } else {
                newircodes.add(irCodes.get(i));
            }
        }
        irCodes = newircodes;
    }

    int abs(int n) {
        return n < 0 ? -n : n;
    }

    int max(int a, int b) {
        return Math.max(a, b);
    }

    int log2n(int n) {
        int cnt = 0;
        while (n != 0) {
            cnt++;
            n >>= 1;
        }
        return cnt;
    }

    int xsign(int n) {
        return n >= 0 ? 0 : -1;
    }

    int mulsh(int a, int b) {
        long res = (long) a * b;
        return (int) res >> 32;
    }

    int sra(int a, int b) {
        return a >> b;
    }

    int xor(int a, int b) {
        return a ^ b;
    }

    long pow2n(int n) {
        long ans = 1;
        for (int i = 0; i < n; i++) {
            ans *= 2;
        }
        return ans;
    }

    private ArrayList<IRCode> mydiv(Exp exp) {
        ArrayList<IRCode> res = new ArrayList<>();
        Sym lsym = exp.getLsym();
        String op = exp.getOp();
        Sym n = exp.getRsym1();
        int d = exp.getRsym2().getValue();
        int l = max(1, log2n(abs(d)));
        long m = (long) 1 + pow2n(31 + l) / abs(d);
        int m_ = (int) (m - pow2n(32));
        int d_sign = xsign(d);
        int sh_post = l - 1;
        Sym q_0 = gettmpreg();
        res.add(new Exp("**", q_0, n, new Sym(m_)));
        res.add(new Exp("+", q_0, q_0, n));
        res.add(new Exp(">>", q_0, q_0, new Sym(sh_post)));
        Sym n_sign = gettmpreg();
        res.add(new Exp(">>>", n_sign, n, new Sym(31)));
        res.add(new Exp("+", q_0, q_0, n_sign));
        res.add(new Exp("^", q_0, q_0, new Sym(d_sign)));
        if (op.equals("/"))
            res.add(new Exp("-", lsym, q_0, new Sym(d_sign)));
        else {
            res.add(new Exp("-", q_0, q_0, new Sym(d_sign)));
            res.add(new Exp("*", q_0, q_0, new Sym(d)));
            res.add(new Exp("-", lsym, n, q_0));
        }
        return res;
    }


    public Sym gettmpreg() {
        return new Sym("t" + tmpnum++);
    }

    public ArrayList<IRCode> getIrCodes() {
        return irCodes;
    }
}
