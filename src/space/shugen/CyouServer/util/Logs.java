package space.shugen.CyouServer.util;

import arc.util.Log;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;

public class Logs {
    private static final Logger mainlogger = LogManager.getLogger("Main");

    public void init() {
        final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);

        ctx.reconfigure();

        ctx.updateLoggers();
        Thread.currentThread().setName("Server");
        Log.logger = (logLevel, s) -> {
            switch (logLevel) {
                case info -> //noinspection DuplicateBranchesInSwitch
                        mainlogger.info(s);
                case err -> mainlogger.error(s);
                case warn -> mainlogger.warn(s);
                case debug -> mainlogger.debug(s);
                case none -> //noinspection DuplicateBranchesInSwitch
                        mainlogger.info(s);
                default -> mainlogger.info(s);
            }
        };
    }
}
