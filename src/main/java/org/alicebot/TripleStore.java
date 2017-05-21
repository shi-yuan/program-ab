package org.alicebot;

import org.alicebot.constant.MagicBooleans;
import org.alicebot.constant.MagicStrings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class TripleStore {
    private int idCnt = 0;
    public String name = "unknown";
    public Bot bot;
    public HashMap<String, Triple> idTriple = new HashMap<>();
    private HashMap<String, String> tripleStringId = new HashMap<>();
    private HashMap<String, HashSet<String>> subjectTriples = new HashMap<>();
    private HashMap<String, HashSet<String>> predicateTriples = new HashMap<>();
    private HashMap<String, HashSet<String>> objectTriples = new HashMap<>();

    public TripleStore(String name, Chat chatSession) {
        this.name = name;
        this.bot = chatSession.bot;
    }

    public class Triple {
        public String id;
        public String subject;
        public String predicate;
        public String object;

        public Triple(String s, String p, String o) {
            Bot bot = TripleStore.this.bot;
            if (bot != null) {
                s = bot.getPreProcessor().normalize(s);
                p = bot.getPreProcessor().normalize(p);
                o = bot.getPreProcessor().normalize(o);
            }
            if (s != null && p != null && o != null) {
                //System.out.println("New triple "+s+":"+p+":"+o);
                subject = s;
                predicate = p;
                object = o;
                id = name + idCnt++;
                // System.out.println("New triple "+id+"="+s+":"+p+":"+o);
            }
        }
    }

    private String mapTriple(Triple triple) {
        String id = triple.id;
        idTriple.put(id, triple);
        String s, p, o;
        s = triple.subject;
        p = triple.predicate;
        o = triple.object;

        s = s.toUpperCase();
        p = p.toUpperCase();
        o = o.toUpperCase();

        String tripleString = s + ":" + p + ":" + o;
        tripleString = tripleString.toUpperCase();

        if (tripleStringId.keySet().contains(tripleString)) {
            //System.out.println("Found "+tripleString+" "+tripleStringId.get(tripleString));
            return tripleStringId.get(tripleString); // triple already exists
        } else {
            //System.out.println(tripleString+" not found");
            tripleStringId.put(tripleString, id);

            HashSet<String> existingTriples;
            if (subjectTriples.containsKey(s)) existingTriples = subjectTriples.get(s);
            else existingTriples = new HashSet<String>();
            existingTriples.add(id);
            subjectTriples.put(s, existingTriples);

            if (predicateTriples.containsKey(p)) existingTriples = predicateTriples.get(p);
            else existingTriples = new HashSet<String>();
            existingTriples.add(id);
            predicateTriples.put(p, existingTriples);

            if (objectTriples.containsKey(o)) existingTriples = objectTriples.get(o);
            else existingTriples = new HashSet<String>();
            existingTriples.add(id);
            objectTriples.put(o, existingTriples);

            return id;
        }
    }

    private String unMapTriple(Triple triple) {
        String id;
        String s, p, o;
        s = triple.subject;
        p = triple.predicate;
        o = triple.object;

        s = s.toUpperCase();
        p = p.toUpperCase();
        o = o.toUpperCase();

        String tripleString = s + ":" + p + ":" + o;

        System.out.println("unMapTriple " + tripleString);
        tripleString = tripleString.toUpperCase();

        triple = idTriple.get(tripleStringId.get(tripleString));

        System.out.println("unMapTriple " + triple);
        if (triple != null) {
            id = triple.id;
            idTriple.remove(id);
            tripleStringId.remove(tripleString);

            HashSet<String> existingTriples;
            if (subjectTriples.containsKey(s)) existingTriples = subjectTriples.get(s);
            else existingTriples = new HashSet<String>();
            existingTriples.remove(id);
            subjectTriples.put(s, existingTriples);

            if (predicateTriples.containsKey(p)) existingTriples = predicateTriples.get(p);
            else existingTriples = new HashSet<String>();
            existingTriples.remove(id);
            predicateTriples.put(p, existingTriples);

            if (objectTriples.containsKey(o)) existingTriples = objectTriples.get(o);
            else existingTriples = new HashSet<String>();
            existingTriples.remove(id);
            objectTriples.put(o, existingTriples);
        } else id = MagicStrings.undefined_triple;

        return id;
    }

    private Set<String> allTriples() {
        return new HashSet<>(idTriple.keySet());
    }

    public String addTriple(String subject, String predicate, String object) {
        if (subject == null || predicate == null || object == null) return MagicStrings.undefined_triple;
        Triple triple = new Triple(subject, predicate, object);
        return mapTriple(triple);
    }

    public String deleteTriple(String subject, String predicate, String object) {
        if (subject == null || predicate == null || object == null) return MagicStrings.undefined_triple;
        if (MagicBooleans.trace_mode) System.out.println("Deleting " + subject + " " + predicate + " " + object);
        Triple triple = new Triple(subject, predicate, object);
        return unMapTriple(triple);
    }

    HashSet<String> emptySet() {
        return new HashSet<>();
    }

    private HashSet<String> getTriples(String s, String p, String o) {
        Set<String> subjectSet;
        Set<String> predicateSet;
        Set<String> objectSet;
        Set<String> resultSet;
        if (MagicBooleans.trace_mode)
            System.out.println("TripleStore: getTriples [" + idTriple.size() + "] " + s + ":" + p + ":" + o);
        //printAllTriples();
        if (s == null || s.startsWith("?")) {
            subjectSet = allTriples();
        } else {
            s = s.toUpperCase();
            // System.out.println("subjectTriples.keySet()="+subjectTriples.keySet());
            // System.out.println("subjectTriples.get("+s+")="+subjectTriples.get(s));
            // System.out.println("subjectTriples.containsKey("+s+")="+subjectTriples.containsKey(s));
            if (subjectTriples.containsKey(s)) subjectSet = subjectTriples.get(s);
            else subjectSet = emptySet();
        }
        // System.out.println("subjectSet="+subjectSet);

        if (p == null || p.startsWith("?")) {
            predicateSet = allTriples();
        } else {
            p = p.toUpperCase();
            if (predicateTriples.containsKey(p)) predicateSet = predicateTriples.get(p);
            else predicateSet = emptySet();
        }

        if (o == null || o.startsWith("?")) {
            objectSet = allTriples();
        } else {
            o = o.toUpperCase();
            if (objectTriples.containsKey(o)) objectSet = objectTriples.get(o);
            else objectSet = emptySet();
        }

        resultSet = new HashSet<>(subjectSet);
        resultSet.retainAll(predicateSet);
        resultSet.retainAll(objectSet);

        //System.out.println("TripleStore.getTriples: "+finalResultSet.size()+" results");
        /* System.out.println("getTriples subjectSet="+subjectSet);
        System.out.println("getTriples predicateSet="+predicateSet);
        System.out.println("getTriples objectSet="+objectSet);
        System.out.println("getTriples result="+resultSet);*/

        return new HashSet<>(resultSet);
    }

    private String getSubject(String id) {
        if (idTriple.containsKey(id)) return idTriple.get(id).subject;
        else return "Unknown subject";
    }

    private String getPredicate(String id) {
        if (idTriple.containsKey(id)) return idTriple.get(id).predicate;
        else return "Unknown predicate";
    }

    private String getObject(String id) {
        if (idTriple.containsKey(id)) return idTriple.get(id).object;
        else return "Unknown object";
    }

    public HashSet<Tuple> select(HashSet<String> vars, HashSet<String> visibleVars, ArrayList<Clause> clauses) {
        HashSet<Tuple> result = new HashSet<>();
        try {
            Tuple tuple = new Tuple(vars, visibleVars);
            //System.out.println("TripleStore: select vars = "+tuple.printVars());
            result = selectFromRemainingClauses(tuple, clauses);
            if (MagicBooleans.trace_mode)
                for (Tuple t : result) {
                    System.out.println(t.printTuple());
                }
        } catch (Exception ex) {
            System.out.println("Something went wrong with select " + visibleVars);
            ex.printStackTrace();
        }
        return result;
    }

    private Clause adjustClause(Tuple tuple, Clause clause) {
        Set vars = tuple.getVars();
        String subj = clause.getSubj();
        String pred = clause.getPred();
        String obj = clause.getObj();
        Clause newClause = new Clause(clause);
        if (vars.contains(subj)) {
            String value = tuple.getValue(subj);
            if (!value.equals(MagicStrings.unbound_variable)) {/*System.out.println("adjusting "+subj+" "+value);*/
                newClause.setSubj(value);
            }
        }
        if (vars.contains(pred)) {
            String value = tuple.getValue(pred);
            if (!value.equals(MagicStrings.unbound_variable)) {/*System.out.println("adjusting "+pred+" "+value);*/
                newClause.setPred(value);
            }
        }
        if (vars.contains(obj)) {
            String value = tuple.getValue(obj);
            if (!value.equals(MagicStrings.unbound_variable)) {/*System.out.println("adjusting "+obj+" "+value); */
                newClause.setObj(value);
            }
        }
        return newClause;
    }

    private Tuple bindTuple(Tuple partial, String triple, Clause clause) {
        Tuple tuple = new Tuple(partial);
        if (clause.getSubj().startsWith("?")) tuple.bind(clause.getSubj(), getSubject(triple));
        if (clause.getPred().startsWith("?")) tuple.bind(clause.getPred(), getPredicate(triple));
        if (clause.getObj().startsWith("?")) tuple.bind(clause.getObj(), getObject(triple));
        return tuple;
    }

    public HashSet<Tuple> selectFromSingleClause(Tuple partial, Clause clause, Boolean affirm) {
        HashSet<Tuple> result = new HashSet<>();
        HashSet<String> triples = getTriples(clause.getSubj(), clause.getPred(), clause.getObj());
        //System.out.println("TripleStore: selected "+triples.size()+" from single clause "+clause.subj+" "+clause.pred+" "+clause.obj);
        if (affirm) {
            for (String triple : triples) {
                Tuple tuple = bindTuple(partial, triple, clause);
                result.add(tuple);
            }
        } else {
            if (triples.size() == 0) result.add(partial);
        }
        return result;
    }

    private HashSet<Tuple> selectFromRemainingClauses(Tuple partial, ArrayList<Clause> clauses) {
        //System.out.println("TripleStore: partial = "+partial.printTuple()+" clauses.size()=="+clauses.size());
        HashSet<Tuple> result = new HashSet<>();
        Clause clause = clauses.get(0);
        clause = adjustClause(partial, clause);
        HashSet<Tuple> tuples = selectFromSingleClause(partial, clause, clause.getAffirm());
        if (clauses.size() > 1) {
            ArrayList<Clause> remainingClauses = new ArrayList<Clause>(clauses);
            remainingClauses.remove(0);
            for (Tuple tuple : tuples) {
                result.addAll(selectFromRemainingClauses(tuple, remainingClauses));
            }
        } else result = tuples;
        return result;
    }
}
