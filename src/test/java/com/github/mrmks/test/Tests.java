package com.github.mrmks.test;

import com.github.mrmks.status.ConfigMap;
import com.github.mrmks.status.StatusService;
import com.github.mrmks.status.adapt.IDataAccessor;
import com.github.mrmks.status.adapt.IEntityConvert;
import com.github.mrmks.status.adapt.IEntityDataAccessor;
import com.github.mrmks.status.api.*;
import com.github.mrmks.status.api.simple.SimpleAttribute;
import com.github.mrmks.status.api.simple.SimpleResource;
import com.github.mrmks.utils.IntMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.*;

public class Tests {
    private static class PlayerEntity {
        int health = 20;
        int id = 0;
        int attack = 0;
        int defence = 0;
    }

    private static class TestAttributeProvider implements IAttributeProvider<PlayerEntity> {

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

    private static class AttackModifier implements IModifier {

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
    }

    private static class HealModifier implements IModifier {

        @Override
        public String getName() {
            return "heal";
        }

        @Override
        public SimpleDependency[] getDependencies() {
            return new SimpleDependency[]{
                    SimpleDependency.newRequired("test:health"),
            };
        }

        @Override
        public void handle(short[] ids, int[] v, ModificationCache mt, int sessionId, int[] dataSrc, int[] dataTar) {
            mt.modifyTar(ids[0], 1);
            mt.buffResource("heal", "", "", BuffType.POSITIVE, new int[]{ids[0]}, new int[]{1}, null, null, 0, 5, true, true, false);
        }
    }

    private static class WeakenModifier implements IModifier {

        @Override
        public String getName() {
            return "weaken";
        }

        @Override
        public SimpleDependency[] getDependencies() {
            return new SimpleDependency[] {
                    SimpleDependency.newRequired("test:attack")
            };
        }

        @Override
        public void handle(short[] ids, int[] v, ModificationCache mt, int sessionId, int[] dataSrc, int[] dataTar) {
            mt.buffAttribute("weaken", "", "", BuffType.NEGATIVE, new int[]{ids[0]}, new int[]{-2}, null, null, 2, true, true, false);
        }
    }

    private static class TestExtension implements IExtension<PlayerEntity> {


        @Override
        public String getPrefix() {
            return "test";
        }

        @Override
        public List<IAttribute<PlayerEntity>> getAttributes() {
            return Arrays.asList(
                    new SimpleResource<>("health", 20, 20, 1, 1, false, (e, p, n) -> e.health = n),
                    new SimpleAttribute<>("attack", (e, p, n) -> e.attack = n),
                    new SimpleAttribute<>("defence", (e, p, n) -> e.defence = n));
        }

        @Override
        public List<IAttributeProvider<PlayerEntity>> getAttributeProviders() {
            return Collections.singletonList(new TestAttributeProvider());
        }

        @Override
        public List<IModifier> getModifiers() {
            return Arrays.asList(new AttackModifier(), new HealModifier(), new WeakenModifier());
        }
    }

    private static class EntityConvert implements IEntityConvert<PlayerEntity> {

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

    private static class DataAccessor implements IDataAccessor {

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
        public IEntityDataAccessor withEntity(byte[] entityKey) {
            return new IEntityDataAccessor() {
                @Override
                public ValuePair[] readValue() {
                    return null;
                }

                @Override
                public void writeValue(String[] keys, int[] vs) {

                }

                @Override
                public StorePair[] readStore() {
                    return null;
                }

                @Override
                public void writeStore(String[] keys, int[][] vs) {

                }

                @Override
                public OptionalInt getValue(int resourceId) {
                    return OptionalInt.of(0);
                }

                @Override
                public int[] getValue() {
                    return new int[0];
                }

                @Override
                public void writeValue(int resourceId, int value) throws IOException {

                }

                @Override
                public void writeValue(int[] values) throws IOException {

                }

                @Override
                public int[][] getStore() {
                    return new int[0][];
                }

                @Override
                public void writeStore(int id, int[] store) throws IOException {

                }

                @Override
                public void writeStore(int[][] store) throws IOException {

                }

                @Override
                public void flushAndClose() throws IOException {

                }
            };
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

        StatusService<PlayerEntity> statusService = new StatusService<>(new DataAccessor(), convert, null, null, configMap);
        statusService.setup(Collections.singletonList(new TestExtension()));

        PlayerEntity self = new PlayerEntity(), enemy = new PlayerEntity();
        enemy.id = 1;
        self.defence = 2;
        enemy.attack = 23;
        convert.map.put(0, self);
        convert.map.put(1, enemy);
        statusService.createEntity(self, false);
        statusService.createEntity(enemy, false);

        short mid = statusService.queryModifierId("test:attack");
        short h_mid = statusService.queryModifierId("test:heal");
        if (mid >= 0) {
            statusService.startTransaction(enemy, self).modify(mid, null);
        }
        Assertions.assertEquals(0, self.health);
        for (int i = 0; i < 50; i++) statusService.tick();
        Assertions.assertEquals(0, self.health);

        statusService.startTransaction(self, self).modify(h_mid, null);
        for (int i = 0; i < 10; i++) statusService.tick();
        Assertions.assertEquals(11, self.health);

        for (int i = 0; i < 10; i++) statusService.tick();
        Assertions.assertEquals(16, self.health);

        for (int i = 0; i < 10; i++) statusService.tick();

        short w_mid = statusService.queryModifierId("test:weaken");
        statusService.startTransaction(enemy).modify(w_mid, null);
        statusService.startTransaction(enemy, self).modify(mid, null);
        Assertions.assertEquals(1, self.health);
    }
}
