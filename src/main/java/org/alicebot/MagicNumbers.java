package org.alicebot;

/**
 * Integers with specific values in Program AB
 */
public class MagicNumbers {
    public static int node_activation_cnt = 4;  // minimum number of activations to suggest atomic pattern
    public static int node_size = 4;  // minimum number of branches to suggest wildcard pattern
    public static int displayed_input_sample_size = 6;
    public static int max_history = 32;
    public static int repetition_count = 2;
    public static int max_stars = 1000;
    public static int max_graph_height = 100000;
    public static int max_substitutions = 10000;
    public static int max_recursion_depth = 765; // assuming java -Xmx512M
    public static int max_recursion_count = 2048;
    public static int max_trace_length = 2048;
    public static int max_loops = 10000;
    public static int estimated_brain_size = 5000;
    public static int max_natural_number_digits = 10000;
    public static int brain_print_size = 100; // largest size of brain to print to System.out
}
