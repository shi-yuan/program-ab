package org.alicebot.ab;

/**
 * Global boolean values that control various actions in Program AB
 */
public class MagicBooleans {
    public static boolean trace_mode = true;
    public static boolean enable_external_sets = true;
    public static boolean enable_external_maps = true;
    public static boolean jp_tokenize = false;
    public static boolean fix_excel_csv = true;
    public static boolean enable_network_connection = true;
    public static boolean cache_sraix = false;
    public static boolean qa_test_mode = false;

    public static void trace(String traceString) {
        if (trace_mode) {
            System.out.println(traceString);
        }
    }
}
