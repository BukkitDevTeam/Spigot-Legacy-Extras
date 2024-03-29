package com.md_5.craftbukkit;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.CraftServer;

public class WatchdogThread extends Thread {

    private static final String LINE = "------------------------------";
    private AtomicLong lastTick = new AtomicLong(System.currentTimeMillis());
    private final long timeoutTime;
    private final boolean restart;

    public WatchdogThread(long timeoutTime, boolean restart) {
        super("CraftBukkit-- Watchdog Thread");
        this.timeoutTime = timeoutTime;
        this.restart = restart;
    }

    public void tick() {
        lastTick.set(System.currentTimeMillis());
    }

    @Override
    public void run() {
        while (!this.isInterrupted()) {
            try {
                sleep(10000);
            } catch (InterruptedException ignore) {
            }

            if (System.currentTimeMillis() > (lastTick.get() + timeoutTime)) {
                Logger log = ((CraftServer) Bukkit.getServer()).getLogger();
                log.log(Level.SEVERE, "The server has stopped responding!");
                log.log(Level.SEVERE, "Please report this to http://spout.in/cbpp!");
                log.log(Level.SEVERE, "Begin Exception Trace For All Threads:");
                Map<Thread, StackTraceElement[]> traces = Thread.getAllStackTraces();
                Iterator<Entry<Thread, StackTraceElement[]>> i = traces.entrySet().iterator();
                while (i.hasNext()) {
                    Entry<Thread, StackTraceElement[]> entry = i.next();
                    Thread thread = entry.getKey();
                    if (thread.getState() != State.WAITING) {
                        System.err.println(LINE);

                        log.log(Level.SEVERE, "Current Thread: " + thread.getName());
                        log.log(Level.SEVERE, "    PID: " + thread.getId() + " | Alive: " + thread.isAlive() + " | State: " + thread.getState());
                        log.log(Level.SEVERE, "    Stack:");
                        StackTraceElement[] stack = entry.getValue();
                        for (int line = 0; line < stack.length; line++) {
                            log.log(Level.SEVERE, "        " + stack[line].toString());
                        }
                    }
                }
                System.err.println(LINE);

                if (this.restart) {
                    Extras.instance.restart();
                }

                //Give up
                this.interrupt();
            }
        }
    }
}
