package org.alicebot.ab.constant;

public final class Constants {

    private Constants() {
    }

    public static final int node_activation_cnt = 4;  // minimum number of activations to suggest atomic pattern
    public static final int node_size = 4;  // minimum number of branches to suggest wildcard pattern
    public static final int displayed_input_sample_size = 6;
    public static final int max_history = 32;
    public static final int repetition_count = 2;
    public static final int max_stars = 1000;
    public static final int max_graph_height = 100000;
    public static final int max_substitutions = 10000;
    public static final int max_recursion_depth = 765; // assuming java -Xmx512M
    public static final int max_recursion_count = 2048;
    public static final int max_trace_length = 2048;
    public static final int max_loops = 10000;
    public static final int estimated_brain_size = 5000;
    public static final int max_natural_number_digits = 10000;
    public static final int brain_print_size = 100; // largest size of brain to print to System.out

    // General global strings
    public static final String program_name_version = "Program AB 0.0.6.26 beta -- AI Foundation Reference AIML 2.1 implementation";
    public static final String comment = "Added repetition detection.";
    public static final String aimlif_split_char = ",";
    public static final String default_bot = "alice2";
    public static final String default_language = "EN";
    public static final String aimlif_split_char_name = "\\#Comma";
    public static final String aimlif_file_suffix = ".csv";
    public static final String ab_sample_file = "sample.txt";
    public static final String text_comment_mark = ";;";
    // <sraix> defaults
    public static final String pannous_api_key = "guest";
    public static final String pannous_login = "test-user";
    public static final String sraix_failed = "SRAIXFAILED";
    public static final String repetition_detected = "REPETITIONDETECTED";
    public static final String sraix_no_hint = "nohint";
    public static final String sraix_event_hint = "event";
    public static final String sraix_pic_hint = "pic";
    public static final String sraix_shopping_hint = "shopping";
    // AIML files
    public static final String unknown_aiml_file = "unknown_aiml_file.aiml";
    public static final String deleted_aiml_file = "deleted.aiml";
    public static final String learnf_aiml_file = "learnf.aiml";
    public static final String null_aiml_file = "null.aiml";
    public static final String inappropriate_aiml_file = "inappropriate.aiml";
    public static final String profanity_aiml_file = "profanity.aiml";
    public static final String insult_aiml_file = "insults.aiml";
    public static final String reductions_update_aiml_file = "reductions_update.aiml";
    public static final String predicates_aiml_file = "client_profile.aiml";
    public static final String update_aiml_file = "update.aiml";
    public static final String personality_aiml_file = "personality.aiml";
    public static final String sraix_aiml_file = "sraix.aiml";
    public static final String oob_aiml_file = "oob.aiml";
    public static final String unfinished_aiml_file = "unfinished.aiml";
    // filter responses
    public static final String inappropriate_filter = "FILTER INAPPROPRIATE";
    public static final String profanity_filter = "FILTER PROFANITY";
    public static final String insult_filter = "FILTER INSULT";
    // default templates
    public static final String deleted_template = "deleted";
    public static final String unfinished_template = "unfinished";
    // AIML defaults
    public static final String bad_javascript = "JSFAILED";
    public static final String js_enabled = "true";
    public static final String unknown_history_item = "unknown";
    public static final String default_bot_response = "I have no answer for that.";
    public static final String error_bot_response = "Something is wrong with my brain.";
    public static final String schedule_error = "I'm unable to schedule that event.";
    public static final String system_failed = "Failed to execute system command.";
    public static final String default_get = "unknown";
    public static final String default_property = "unknown";
    public static final String default_map = "unknown";
    public static final String default_Customer_id = "unknown";
    public static final String default_bot_name = "unknown";
    public static final String default_that = "unknown";
    public static final String default_topic = "unknown";
    public static final String default_list_item = "NIL";
    public static final String undefined_triple = "NIL";
    public static final String unbound_variable = "unknown";
    public static final String template_failed = "Template failed.";
    public static final String too_much_recursion = "Too much recursion in AIML";
    public static final String too_much_looping = "Too much looping in AIML";
    public static final String blank_template = "blank template";
    public static final String null_input = "NORESP";
    public static final String null_star = "nullstar";
    // sets and maps
    public static final String set_member_string = "ISA";
    public static final String remote_map_key = "external";
    public static final String remote_set_key = "external";
    public static final String natural_number_set_name = "number";
    public static final String map_successor = "successor";
    public static final String map_predecessor = "predecessor";
    public static final String map_singular = "singular";
    public static final String map_plural = "plural";
}
