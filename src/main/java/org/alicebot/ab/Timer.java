package org.alicebot.ab;

/**
 * Specialized timer function for program instrumentation
 */
public class Timer {
    private long startTimeMillis;

    public Timer() {
        start();
    }

    public void start() {
        startTimeMillis = System.currentTimeMillis();
    }

    private long elapsedTimeMillis() {
        return System.currentTimeMillis() - startTimeMillis + 1;
    }

    public float elapsedTimeSecs() {
        return elapsedTimeMillis() / 1000F;
    }

    public float elapsedTimeMins() {
        return elapsedTimeSecs() / 60F;
    }
}
