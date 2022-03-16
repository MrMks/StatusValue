package com.github.mrmks.status;

import com.github.mrmks.status.adapt.IDataAccessor;
import com.github.mrmks.status.adapt.IEntityConvert;
import com.github.mrmks.status.adapt.IGuiCallback;
import com.github.mrmks.status.adapt.ILogger;
import com.github.mrmks.status.api.BuffType;
import com.github.mrmks.status.api.IExtension;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class StatusService<T> {
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

    public StatusService(IDataAccessor ida, IEntityConvert<T> iec, IGuiCallback guiCallback, ILogger logger, ConfigMap configMap) {
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
            logger.severe("StatusValue run into error: can't connect to DataAccessor", e);
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
                logger.severe("StatusValue run into error: Can't setup with given extensions", tr);
                return;
            }
            this.handlerList = bakeResult.handlerList;
            this.entityManager = bakeResult.entityManager;
            this.taskManager = bakeResult.taskManager;
            this.buffManager = bakeResult.buffManager;
            state = STATE.RUNNING;
        }
    }

    private void chkRunning() {
        if (state != STATE.RUNNING) {
            if (state == STATE.ERROR) {
                throw new IllegalStateException("StatusValue run into error");
            } else if (state == STATE.EXTENSION) {
                throw new IllegalStateException("You must setup StatusValue before you call other methods");
            } else if (state == STATE.INIT || state == STATE.INSTANCE) {
                throw new IllegalStateException("StatusValue run into error while initializing");
            } else if (state == STATE.STOPPING || state == STATE.STOPPED) {
                throw new IllegalStateException("Outdated instance, please create a new instance");
            }
        }
    }

    // ======== functional methods begin ========
    // -------- query methods --------
    public short queryAttributeId(String key) {
        chkRunning();
        return (short) entityManager.queryAttribute(key);
    }

    public short queryModifierId(String key) {
        chkRunning();
        return handlerList.queryModifier(key);
    }

    public int queryHandlerParamId(String key) {
        chkRunning();
        return handlerList.queryHandler(key);
    }

    // -------- session methods --------
    public void startSession() {
        chkRunning();
        handlerList.beginSession();
    }

    public void finishSession() {
        chkRunning();
        handlerList.finishSession();
    }

    // if we are not in session, we will start an auto-finish session.
    public Transaction startTransaction(T src, T tar) {
        chkRunning();
        return handlerList.beginTransaction(src, tar);
    }

    public Transaction startTransaction(T tar) {
        chkRunning();
        return handlerList.beginTransaction(tar);
    }

    // -------- update provider --------
    public int updateProvider(T e, String key) {
        chkRunning();
        return entityManager.updateProvider(e, key);
    }

    public void updateProvider(T e, int id) {
        chkRunning();
        entityManager.updateProvider(e, id);
    }

    // -------- entity manage --------
    public void createEntity(T entity, boolean shouldSave) {
        chkRunning();
        entityManager.createEntity(entity, shouldSave);
    }

    public void removeEntity(T entity) {
        chkRunning();
        entityManager.removeEntity(entity);
    }

    // -------- tick buffs --------
    public void tick() {
        chkRunning();
        handlerList.tick();
        taskManager.tick();
        entityManager.tick();
    }

    // -------- system src ----------
    public void removeBuff(T tar, BuffType type, String str, boolean asKey, boolean once, boolean force) {
        chkRunning();
        int id = entityManager.findEntityIndex(tar);
        if (id < 0) return;
        buffManager.removeBuff(id, 0, new BuffData(type, asKey ? str : null, asKey ? null : str, null, force, true), once);
    }

    // -------- other src ----------
    public void removeBuff(T tar, T src, BuffType type, String key, String tag, boolean anySource, boolean once, boolean force) {
        chkRunning();
        int eidSrc = entityManager.findEntityIndex(src), eidTar = entityManager.findEntityIndex(tar);
        if (eidTar < 0 || (eidSrc < 0 && !anySource)) return;
        buffManager.removeBuff(eidTar, eidSrc, new BuffData(type, key, tag, null, force, anySource), once);
    }

    public void stop() {
        if (state == STATE.RUNNING) {
            state = STATE.STOPPING;
            taskManager.stopAll();
            entityManager.stopAll();
            try {
                dataAccessor.flushAll();
            } catch (IOException e) {
                logger.severe("Error while flushing data accessor", e);
            }
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
            else System.out.println(msg);
        }

        @Override
        public void severe(String msg, Throwable tr) {
            if (logger != null) {
                logger.severe(msg, tr);
            } else {
                System.out.println(msg);
                tr.printStackTrace(System.err);
            }
        }

        @Override
        public void debug(String msg) {
            if (logger != null) logger.debug(msg);
        }
    }
}
