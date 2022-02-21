package com.github.mrmks.mc.status;

import com.github.mrmks.mc.status.adapt.IDataAccessor;
import com.github.mrmks.mc.status.adapt.IEntityConvert;
import com.github.mrmks.mc.status.adapt.IGuiCallback;
import com.github.mrmks.mc.status.adapt.ILogger;
import com.github.mrmks.mc.status.api.IExtension;
import com.github.mrmks.mc.status.api.BuffType;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class StatusMain<T> {
    private final IDataAccessor dataAccessor;
    private final IEntityConvert<T> entityConvert;
    private final ILogger logger;
    private final IGuiCallback guiCallback;
    private final ConfigMap configMap;
    private EntityManager<T> entityManager;
    private HandlerList<T> handlerList;
    private TaskManager taskManager;
    private BuffManager<T> buffManager;
    private STATE state;
    private final long instanceTime;

    public StatusMain(IDataAccessor ida, IEntityConvert<T> iec, IGuiCallback guiCallback, ILogger logger, ConfigMap configMap) {
        state = STATE.INSTANCE;
        instanceTime = System.currentTimeMillis();
        this.dataAccessor = ida;
        this.entityConvert = iec;
        this.logger = new LoggerWrap(logger);
        this.guiCallback = guiCallback;
        this.configMap = configMap;
        state = STATE.INIT;
        if (dataAccessor == null || entityConvert == null) {
            state = STATE.ERROR;
            logger.severe("StatusValue run into error: initializing with null DataAccessor or null EntityConvert");
            return;
        }
        try {
            dataAccessor.connect();
        } catch (IOException e) {
            dataAccessor.close();
            state = STATE.ERROR;
            logger.severe("StatusValue run into error: can't connect to DataAccessor");
            e.printStackTrace();
            return;
        }
        state = STATE.EXTENSION;
    }

    /**
     * After this method, we run into running state, at this stage you can register entities and then call transactions.
     */
    public void setup(List<IExtension<T>> list) {
        if (state == STATE.EXTENSION) {
            state = STATE.BAKING;
            Registry.BakeResult<T> bakeResult;
            try {
                bakeResult = Registry.bakeAll(list == null ? Collections.emptyList() : list, dataAccessor, entityConvert, logger, guiCallback, configMap);
            } catch (Throwable tr) {
                state = STATE.ERROR;
                logger.severe("StatusValue run into error: Can't setup with given extensions");
                return;
            }
            this.handlerList = bakeResult.handlerList;
            this.entityManager = bakeResult.entityManager;
            this.taskManager = bakeResult.taskManager;
            this.buffManager = bakeResult.buffManager;
            state = STATE.RUNNING;
        }
    }

    // ======== functional methods begin ========
    // -------- query methods --------
    public short queryAttributeId(String key) {
        return (short) entityManager.queryAttribute(key);
    }

    public short queryModifierId(String key) {
        return (short) handlerList.queryModifier(key);
    }

    public int queryHandlerParamId(String key) {
        return handlerList.queryHandler(key);
    }

    // -------- session methods --------
    public void startSession() {
        handlerList.beginSession();
    }

    public void finishSession() {
        handlerList.finishSession();
    }

    // if we are not in session, we will start a session automate and finishSession automate.
    public Transaction startTransaction(T src, T tar) {
        return handlerList.beginTransaction(src, tar);
    }

    // -------- update provider --------
    public int updateProvider(T e, String key) {
        return entityManager.updateProvider(e, key);
    }

    public void updateProvider(T e, int id) {
        entityManager.updateProvider(e, id);
    }

    // -------- entity manage --------
    public void createEntity(T entity, boolean shouldSave) {
        entityManager.createEntity(entity, shouldSave);
    }

    public void removeEntity(T entity) {
        entityManager.removeEntity(entity);
    }

    // -------- buff ticks --------
    public void tick() {
        taskManager.tick();
        handlerList.tick();
        entityManager.tick();
    }

    // remove buff for tar, search by type and classifier, remove if canRemove or force, remove random one if any or every one if not any.
    public void removeBuff(T tar, BuffType type, String key, boolean asKey, boolean once, boolean force) {

    }

    public void stop() {
        if (state == STATE.RUNNING) {
            state = STATE.STOPPING;

            state = STATE.STOPPED;
        }
    }

    public long timestamp() {
        return instanceTime;
    }

    public STATE getState() {
        return state;
    }

    public enum STATE {
        INSTANCE, INIT, EXTENSION, BAKING, RUNNING, STOPPING, STOPPED, ERROR
    }

    private static class LoggerWrap implements ILogger {

        private final ILogger logger;
        private LoggerWrap(ILogger logger) {
            this.logger = logger;
        }

        @Override
        public void info(String msg) {
            if (logger != null) logger.info(msg);
        }

        @Override
        public void warn(String msg) {
            if (logger != null) logger.warn(msg);
        }

        @Override
        public void severe(String msg) {
            if (logger != null) logger.severe(msg);
        }

        @Override
        public void debug(String msg) {
            if (logger != null) logger.debug(msg);
        }
    }
}
