package net.voldrich.test.graal;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScriptDataStore {
    private static final Logger logger = LoggerFactory.getLogger(ScriptDataStore.class);

    private final Context context;

    public ScriptDataStore(Context context) {
        this.context = context;
    }

    @HostAccess.Export
    public void save(String model, Value dataObj) {
        if (dataObj == null) {
            logger.warn("Stored data is null");
            return;
        }
        save(model, dataObj.toString());
    }

    @HostAccess.Export
    public void save(String model, String data) {
        if (data == null) {
            logger.warn("Stored data is null");
            return;
        }
        if (data.length() > 200) {
            logger.info("Storing {}: {}", model, data.substring(0, 200) + "...");
        } else {
            logger.info("Storing {}: {}", model, data);
        }
    }
}
