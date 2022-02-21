package com.github.mrmks.mc.status;

import com.github.mrmks.mc.status.adapt.IDataAccessor;
import com.github.mrmks.mc.status.adapt.IEntityConvert;
import com.github.mrmks.mc.status.adapt.IEntityDataAccessor;
import com.github.mrmks.mc.status.api.*;
import com.github.mrmks.mc.status.utils.IntMap;
import com.github.mrmks.mc.status.utils.SimpleDependency;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.*;

public class Tests {
    private class PlayerEntity {
        int health = 20;
        int id = 0;
        int attack = 0;
        int defence = 0;
    }

    private class HealthResource implements IResource<PlayerEntity> {

        @Override
        public void update(PlayerEntity entity, int prev, int changed) {
            entity.health = prev + changed;
        }

        @Override
        public String getName() {
            return "health";
        }

        @Override
        public int baseValue() {
            return 20;
        }

        @Override
        public int baseUBound() {
            return 20;
        }

        @Override
        public int baseStep() {
            return 1;
        }

        @Override
        public int interval() {
            return 1;
        }

        @Override
        public void updateUBound(PlayerEntity entity, int prev, int now) {

        }

        @Override
        public void updateStep(PlayerEntity entity, int prev, int now) {

        }

        @Override
        public int valueVersion() {
            return 0;
        }

        @Override
        public Updater valueUpdater() {
            return null;
        }
    }

    private class AttackAttribute implements IAttribute<PlayerEntity> {

        @Override
        public String getName() {
            return "attack";
        }

        @Override
        public void update(PlayerEntity entity, int prev, int changed) {

        }
    }

    private class DefenceAttribute implements IAttribute<PlayerEntity> {

        @Override
        public String getName() {
            return "defence";
        }

        @Override
        public void update(PlayerEntity entity, int prev, int changed) {

        }
    }

    private class TestAttributeProvider implements IAttributeProvider<PlayerEntity> {

        @Override
        public String getName() {
            return "testProvider";
        }

        @Override
        public SimpleDependency[] getDependencies() {
            return new SimpleDependency[]{
                    SimpleDependency.newRequired("test:health"),
                    SimpleDependency.newRequired("test:attack"),
                    SimpleDependency.newRequired("test:defence")
            };
        }

        @Override
        public void update(short[] ids, PlayerEntity entity, WritingStatus ws) {
            ws.write(ids[0], 0);
            ws.write(ids[1], entity.attack);
            ws.write(ids[2], entity.defence);
        }
    }

    private class AttackModifier implements IModifier {

        @Override
        public String getName() {
            return "attack";
        }

        @Override
        public SimpleDependency[] getDependencies() {
            return new SimpleDependency[]{
                    SimpleDependency.newRequired("test:health"),
                    SimpleDependency.newRequired("test:attack"),
                    SimpleDependency.newRequired("test:defence")
            };
        }

        @Override
        public void handle(short[] ids, int[] v, ModificationCache mt, int sessionId, int[] dataSrc, int[] dataTar) {
            mt.modifyTar(ids[0], Math.min(mt.getTar(ids[2]) - mt.getSrc(ids[1]),0));
        }

        @Override
        public Updater storeUpdater() {
            return null;
        }
    }
    private class TestExtension implements IExtension<PlayerEntity> {


        @Override
        public String getPrefix() {
            return "test";
        }

        @Override
        public List<IAttribute<PlayerEntity>> getAttributes() {
            return Arrays.asList(new HealthResource(), new AttackAttribute(), new DefenceAttribute());
        }
        @Override
        public List<IAttributeProvider<PlayerEntity>> getAttributeProviders() {
            return Collections.singletonList(new TestAttributeProvider());
        }

        @Override
        public List<IModifier> getModifiers() {
            return Collections.singletonList(new AttackModifier());
        }
    }

    private class EntityConvert implements IEntityConvert<PlayerEntity> {

        IntMap<PlayerEntity> map = new IntMap<>();

        @Override
        public PlayerEntity fromBytes(byte[] bytes) {
            if (bytes.length == 4) {
                int id = bytes[0] << 24 | bytes[1] << 16 | bytes[2] << 8 | bytes[3];
                return map.get(id);
            }
            return null;
        }

        @Override
        public byte[] toBytes(PlayerEntity token) {
            int id = token.id;
            return new byte[] {(byte) (id >>> 24), (byte) (id >>> 16 & 0xff), (byte) (id >>> 8 & 0xff), (byte) (id & 0xff)};
        }
    }

    private class DataAccessor implements IDataAccessor {

        @Override
        public void connect() throws IOException {

        }

        @Override
        public void updateValue(String[] resourceName, int[] valueVersion, IResource.Updater[] updater) {

        }

        @Override
        public void updateStore(String[] modifierName, int[] storeVersion, byte[] storeSize, IModifier.Updater[] updater) {

        }

        @Override
        public void updateFinish() {

        }

        @Override
        public OptionalInt getValue(byte[] entityKey, int resourceId) {
            return null;
        }

        @Override
        public void writeValue(byte[] entityKey, int[] values) {

        }

        @Override
        public void writeValue(byte[] entityKey, int resourceId, int value) {

        }

        @Override
        public int[][] getStore(byte[] entityKey) {
            return new int[0][];
        }

        @Override
        public void writeStore(byte[] entityKey, int[][] store) {

        }

        @Override
        public void writeStore(byte[] entityKey, int id, int[] val) {

        }

        @Override
        public void writeBuff(byte[] entityKey, byte[][] data) {

        }

        @Override
        public byte[][] readBuff(byte[] entityKey) {
            return new byte[0][];
        }

        @Override
        public IEntityDataAccessor withEntity(byte[] entityKey) {
            return null;
        }

        @Override
        public void flushEntity(byte[] entityKey) {

        }

        @Override
        public void flushAll() {

        }

        @Override
        public void close() {

        }
    }

    @Test
    public void testRun() {
        ConfigMap configMap = new ConfigMap();
        configMap.setGuiInterval(1800);
        EntityConvert convert = new EntityConvert();

        StatusMain<PlayerEntity> statusMain = new StatusMain<>(new DataAccessor(), convert, null, null, configMap);
        statusMain.setup(Collections.singletonList(new TestExtension()));

        PlayerEntity self = new PlayerEntity(), enemy = new PlayerEntity();
        enemy.id = 1;
        self.defence = 2;
        enemy.attack = 7;
        convert.map.put(0, self);
        convert.map.put(1, enemy);
        statusMain.createEntity(self, false);
        statusMain.createEntity(enemy, false);

        short mid = statusMain.queryModifierId("test:attack");
        if (mid >= 0) {
            statusMain.startTransaction(enemy, self).modify(mid, null);
        }
        for (int i = 0; i < 5; i++) statusMain.tick();
    }
}
