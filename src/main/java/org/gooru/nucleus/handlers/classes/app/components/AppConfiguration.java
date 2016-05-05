package org.gooru.nucleus.handlers.classes.app.components;

import org.gooru.nucleus.handlers.classes.bootstrap.startup.Initializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

/**
 * Created by ashish on 5/5/16.
 */
public final class AppConfiguration implements Initializer {
    private static final String APP_CONFIG_KEY = "app.configuration";
    private static final String KEY = "__KEY__";
    private static final String CLASS_END_DATE_KEY = "class.end.date";
    private static final JsonObject configuration = new JsonObject();
    private static final Logger LOGGER = LoggerFactory.getLogger(AppConfiguration.class);

    public static AppConfiguration getInstance() {
        return Holder.INSTANCE;
    }

    private volatile boolean initialized = false;

    private AppConfiguration() {
    }

    @Override
    public void initializeComponent(Vertx vertx, JsonObject config) {
        if (!initialized) {
            synchronized (Holder.INSTANCE) {
                if (!initialized) {
                    JsonObject appConfiguration = config.getJsonObject(APP_CONFIG_KEY);
                    if (appConfiguration == null || appConfiguration.isEmpty()) {
                        LOGGER.warn("App configuration is not available");
                    } else {
                        configuration.put(KEY, appConfiguration);
                    }
                }
            }
        }
    }

    public String getClassEndDate() {
        return configuration.getJsonObject(KEY).getString(CLASS_END_DATE_KEY);
    }

    private static final class Holder {
        private static final AppConfiguration INSTANCE = new AppConfiguration();
    }

}
