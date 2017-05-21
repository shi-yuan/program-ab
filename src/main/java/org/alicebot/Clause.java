package org.alicebot;

public class Clause {
    private String subj;
    private String pred;
    private String obj;
    private Boolean affirm;

    public Clause(String s, String p, String o) {
        this(s, p, o, true);
    }

    public Clause(String s, String p, String o, Boolean affirm) {
        subj = s;
        pred = p;
        obj = o;
        this.affirm = affirm;
    }

    public Clause(Clause clause) {
        this(clause.subj, clause.pred, clause.obj, clause.affirm);
    }

    public String getSubj() {
        return subj;
    }

    public void setSubj(String subj) {
        this.subj = subj;
    }

    public String getPred() {
        return pred;
    }

    public void setPred(String pred) {
        this.pred = pred;
    }

    public String getObj() {
        return obj;
    }

    public void setObj(String obj) {
        this.obj = obj;
    }

    public Boolean getAffirm() {
        return affirm;
    }

    public void setAffirm(Boolean affirm) {
        this.affirm = affirm;
    }
}
