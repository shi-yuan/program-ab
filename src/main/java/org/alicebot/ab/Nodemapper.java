package org.alicebot.ab;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Nodemapper data structure.  In order to minimize memory overhead this class has no methods.
 * Operations on Nodemapper objects are performed by NodemapperOperator class
 */
public class Nodemapper {
    /*    public static int idCnt=0;
        public int id;*/
    public Category category = null;
    public int height = MagicNumbers.max_graph_height;
    public StarBindings starBindings = null;
    public HashMap<String, Nodemapper> map = null;
    public String key = null;
    public Nodemapper value = null;
    public boolean shortCut = false;
    public ArrayList<String> sets;
/*    public Nodemapper () {
        id = idCnt++;
    }*/
}


