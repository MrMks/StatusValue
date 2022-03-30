package com.github.mrmks.status;

import com.github.mrmks.status.adapt.IDataAccessor;
import com.github.mrmks.status.adapt.IEntityConvert;
import com.github.mrmks.status.adapt.IGuiCallback;
import com.github.mrmks.status.adapt.ILogger;
import com.github.mrmks.status.api.*;
import com.github.mrmks.status.api.SimpleDependency;
import com.github.mrmks.status.api.SortableDependency;
import com.github.mrmks.utils.IntMap;

import java.util.*;

import static com.github.mrmks.status.Constants.*;

/**
 * Consider to performance reasons, we can accept 32768(index from 0 to 32767) attributes, modifiers and handlers in maximum
 * This means we can use short type for id use.
 *
 * todo: update adapt layer api
 */
public class Registry {

    private static boolean chkName(String k, int length) {
        return k != null && !k.isEmpty() && k.length() <= length && k.indexOf(SPLITTER) < 0;
    }

    static <T> BakeResult<T> bakeAll(Collection<IExtension<T>> extensions, IDataAccessor ida, IEntityConvert<T> iec, ILogger logger, IGuiCallback guiCallback, ConfigMap configMap) {
        HashSet<String> srcPrefixSet = new HashSet<>();
        extensions = new ArrayList<>(extensions);
        extensions.removeIf(ext -> ext == null || !chkName(ext.getPrefix(), 31) || !srcPrefixSet.add(ext.getPrefix()));

        // attributes up to 32768;
        HashMap<String, IndexedAttribute<T>> attributeMap = new HashMap<>(64);
        int resourceSize = 0;
        // providers up to 128
        HashMap<String, WrappedAttributeProvider<T>> providerMap = new HashMap<>(16);
        // modifiers up to 32768
        HashMap<String, IndexedModifier> modifierMap = new HashMap<>(64);
        // handler up to 32768
        HashMap<String, HandlerRef> handlerMap = new HashMap<>(64);

        // iterate extensions
        for (IExtension<T> ext : extensions) {
            String pref = ext.getPrefix().concat(SPLITTER_STR);
            String nameTmp;

            // prepare attributes
            for (IAttribute<T> attr : ext.getAttributes()) {
                if (attributeMap.size() + resourceSize << 1 > Short.MAX_VALUE) break;
                if (attr == null) continue;
                boolean r = attr instanceof IResource;
                byte flag = r ? FLAG_RESOURCE : FLAG_ATTRIBUTE;
                if (chkName(nameTmp = attr.getName(), r ? 30 : 32)) {
                    nameTmp = pref.concat(nameTmp);
                    if (r && attributeMap.size() + (resourceSize << 1) > 32765) break;
                    IndexedAttribute<T> ia = attributeMap.put(nameTmp, new IndexedAttribute<>(attr, nameTmp, flag));
                    if (r) {
                        if (ia == null || ia.flag == 0) resourceSize ++;
                    } else {
                        if (ia != null && ia.flag > 0) resourceSize --;
                    }
                }
            }

            // prepare attributeProviderList;
            for (IAttributeProvider<T> ap : ext.getAttributeProviders()) {
                if (providerMap.size() > 127) break;
                if (ap == null || !chkName(nameTmp = ap.getName(), 32)) continue;
                providerMap.put(nameTmp = pref.concat(nameTmp), new WrappedAttributeProvider<>(nameTmp, ap));
            }

            // prepare modifier map;
            for (IModifier mdf : ext.getModifiers()) {
                if (modifierMap.size() > Short.MAX_VALUE) break;
                if (mdf != null && chkName(nameTmp = mdf.getName(), 32)) {
                    nameTmp = pref.concat(nameTmp);
                    modifierMap.put(nameTmp, new IndexedModifier(mdf, nameTmp));
                }
            }

            // prepare handlerList;
            for (IHandler h : ext.getHandlers()) {
                if (handlerMap.size() > Short.MAX_VALUE) break;
                if (h != null && chkName(nameTmp = h.getName(), 32)) {
                    handlerMap.put(nameTmp = pref.concat(nameTmp), new HandlerRef(new WrappedHandler(nameTmp, h)));
                }
            }

        }

        // bake attribute;
        @SuppressWarnings("unchecked")
        IndexedAttribute<T>[] attributeAry = new IndexedAttribute[attributeMap.size() + (resourceSize << 1)];
        int[][] intervalIds = new int[16][];
        int[] intervals = new int[16];
        int[] baseValues;
        boolean[] autoStepZero;
        {
            int i = 0, j = 0;
            int rs2 = resourceSize << 1;
            int rs3 = rs2 + resourceSize;
            baseValues = new int[rs3];
            autoStepZero = new boolean[resourceSize];
            String[] resourcesKey = new String[resourceSize];
            int[] versionAry = new int[resourceSize];
            IResource.Updater[] updaterAry = new IResource.Updater[resourceSize];
            for (IndexedAttribute<T> ia : attributeMap.values()) {
                if (ia.flag == 0) {
                    attributeAry[rs3 + j] = ia;
                    ia.index = (short) (rs3 + j);
                    ++j;
                } else {
                    attributeAry[i] = ia;
                    ia.index = (short) i;
                    IResource<?> ir = (IResource<?>) ia.attribute;
                    versionAry[i] = ir.valueVersion();
                    updaterAry[i] = ir.valueUpdater();
                    attributeAry[resourceSize + i] = new IndexedAttribute<>(ia.attribute, ia.name.concat(SUFFIX_BOUND), FLAG_SPECIAL);
                    attributeAry[resourceSize + i].index = (short) (resourceSize + i);
                    attributeAry[rs2 + i] = new IndexedAttribute<>(ia.attribute, ia.name.concat(SUFFIX_STEP), FLAG_STEP);
                    attributeAry[rs2 + i].index = (short) (rs2 + i);

                    baseValues[i] = ir.baseValue();
                    baseValues[i + resourceSize] = ir.baseUBound();
                    baseValues[i + rs2] = ir.baseStep();

                    autoStepZero[i] = ir.canStepZero();
                    ++i;
                }
            }

            ida.updateValue(resourcesKey, versionAry, updaterAry);
            int[] intervalIdsSize = new int[16];
            int intervalSize = 0;
            for (i = 0; i < resourceSize; i++) {
                IndexedAttribute<T> ia = attributeAry[i + resourceSize];
                attributeMap.put(ia.name, ia);
                ia = attributeAry[i + rs2];
                attributeMap.put(ia.name, ia);

                ia = attributeAry[i];
                IResource<T> r = (IResource<T>) ia.attribute;

                int index = -1;
                for (int k = 0; k < intervalSize; k++) {
                    if (intervals[k] == r.interval()) {
                        index = k;
                        break;
                    }
                }
                if (index < 0) {
                    if (intervalSize >= intervals.length) {
                        intervals = Arrays.copyOf(intervals, intervalSize << 1);
                        intervalIds = Arrays.copyOf(intervalIds, intervalSize << 1);
                        intervalIdsSize = Arrays.copyOf(intervalIdsSize, intervalSize << 1);
                    }
                    intervals[intervalSize] = r.interval();
                    intervalIds[intervalSize] = new int[8];
                    intervalIds[intervalSize][0] = i;
                    intervalIdsSize[intervalSize] = 1;
                    intervalSize++;
                } else {
                    if (intervalIdsSize[index] >= intervalIds[index].length) {
                        intervalIds[index] = Arrays.copyOf(intervalIds[index], intervalIdsSize[index] << 1);
                    }
                    intervalIds[index][intervalIdsSize[index] ++] = i;
                }
            }
            intervals = Arrays.copyOf(intervals, intervalSize);
            int[][] tmp = new int[intervalSize][];
            for (i = 0; i < intervalSize; ++i) {
                tmp[i] = Arrays.copyOf(intervalIds[i], intervalIdsSize[i]);
            }
            intervalIds = tmp;
        }

        // bake attributeProvider;
        @SuppressWarnings("unchecked")
        WrappedAttributeProvider<T>[] attributeProviderAry = new WrappedAttributeProvider[providerMap.size()];
        {
            Iterator<WrappedAttributeProvider<T>> it = providerMap.values().iterator();
            int i, size = 0;
            while (it.hasNext()) {
                WrappedAttributeProvider<T> wap = it.next();
                SimpleDependency[] dps = wap.provider.getDependencies();
                short[] ids = dps.length == 0 ? EMPTY_ARY_SHORT : new short[dps.length];
                for (i = 0; i < dps.length; i++) {
                    if ((ids[i] = findDependency(dps[i], attributeMap)) == -2) break;
                }
                if (i == dps.length) {
                    wap.ids = ids;
                    wap.index = size;
                    attributeProviderAry[size++] = wap;
                } else {
                    it.remove();
                }
            }
            if (size < attributeProviderAry.length) attributeProviderAry = Arrays.copyOf(attributeProviderAry, size);
        }

        // bake modifiers
        // the array is sorted by names.
        // todo: deal with modifier dependencies for modifier buff usage
        IndexedModifier[] modifierAry = new IndexedModifier[modifierMap.size()];
        {
            short i;
            short j = 0;
            short size = 0;
            Iterator<IndexedModifier> it = modifierMap.values().iterator();
            int[] modifierStoreCvt = new int[modifierMap.size()];
            while (it.hasNext()) {
                IndexedModifier im = it.next();
                SimpleDependency[] dps = im.modifier.attributeDependencies();
                short[] ids = dps.length == 0 ? EMPTY_ARY_SHORT : new short[dps.length];
                for (i = 0; i < dps.length; ++i) {
                    if ((ids[i] = findDependency(dps[i], attributeMap)) == -2) break;
                }
                if (i == dps.length) {
                    im.ids = ids;
                    im.index = size;
                    modifierAry[size++] = im;
                    IModifier m = im.modifier;
                    if (m.storeVersion() != 0 && m.storeSize() > 0) {
                        modifierStoreCvt[j] = size - 1;
                        im.dataIndex = j++;
                    }
                } else {
                    it.remove();
                }
            }
            if (size < modifierAry.length) modifierAry = Arrays.copyOf(modifierAry, size);
            Arrays.sort(modifierAry, Comparator.comparing(o -> o.name));
            String[] modifierStoreKey = new String[j];
            int[] versionAry = new int[j];
            byte[] sizeAry = new byte[j];
            IModifier.Updater[] updaterAry = new IModifier.Updater[j];
            for (i = 0; i < j; i++) {
                IndexedModifier im = modifierAry[modifierStoreCvt[i]];
                IModifier m = im.modifier;
                modifierStoreKey[i] = im.name;
                versionAry[i] = m.storeVersion();
                sizeAry[i] = m.storeSize();
                if (sizeAry[i] > 16) sizeAry[i] = 16; else if (sizeAry[i] < 0) sizeAry[i] = 0;
                updaterAry[i] = m.storeUpdater();
            }
            ida.updateStore(modifierStoreKey, versionAry, sizeAry, updaterAry);
        }

        ida.updateFinish();

        // bake handlers
        IntMap<WrappedHandler[]> handlerAry = new IntMap<>(modifierAry.length >>> 2);
        HashMap<String, WrappedHandler> wrappedHandlerMap;
        {
            HashMap<String, HashMap<String, HandlerBaking>> splitHandlerMap = new HashMap<>(modifierAry.length >>> 2);

            Iterator<HandlerRef> it = handlerMap.values().iterator();
            while (it.hasNext()) {
                HandlerRef hr = it.next();
                WrappedHandler wh = hr.wh;
                Optional<short[]> opt = generateDependency(wh.handler.targetAttributes(), attributeMap);
                if (!opt.isPresent()) {
                    it.remove();
                } else {
                    wh.aIds = opt.get();
                    opt = generateDependency(wh.handler.targetModifiers(), modifierMap);
                    if (!opt.isPresent()) {
                        it.remove();
                    } else {
                        wh.mIds = opt.get();

                        for (int i = 0; i < wh.mIds.length; ++i) {
                            if (wh.mIds[i] >= 0) {
                                splitHandlerMap.computeIfAbsent(modifierAry[wh.mIds[i]].name, o -> new HashMap<>())
                                        .put(wh.name, new HandlerBaking(hr, wh.handler.getDependencies()[i]));
                            }
                        }

                        wh.mCvt = sortConvertor(wh.mIds);
                    }
                }
            }

            HashMap<String, LinkedList<String>> reverseDepMap = new HashMap<>();
            LinkedList<String> removing = new LinkedList<>();
            for (Map.Entry<String, HashMap<String, HandlerBaking>> e : splitHandlerMap.entrySet()) {
                HashMap<String, HandlerBaking> subHandlerMap = e.getValue();

                Iterator<HandlerBaking> itE = subHandlerMap.values().iterator();
                boolean flag = false;
                while (itE.hasNext()) {
                    HandlerBaking hp = itE.next();
                    String name = hp.hr.wh.name;
                    for (SortableDependency sd : hp.dps) {
                        if (sd.isRequired()) {
                            if (subHandlerMap.containsKey(sd.getKey())) {
                                reverseDepMap.computeIfAbsent(sd.getKey(), o -> new LinkedList<>())
                                        .add(name);
                            } else {
                                itE.remove();
                                flag = true;
                                break;
                            }
                        }
                    }
                    if (flag) {
                        removing.add(name);
                        LinkedList<String> lst = reverseDepMap.remove(name);
                        if (lst != null) removing.addAll(lst);
                    }
                }
                while (!removing.isEmpty()) {
                    String key = removing.remove();
                    subHandlerMap.remove(key);
                    LinkedList<String> lst = reverseDepMap.remove(key);
                    if (lst != null) removing.addAll(lst);
                }
                HashMap<String, HashSet<String>> sortDepMap = new HashMap<>();
                itE = subHandlerMap.values().iterator();
                while (itE.hasNext()) {
                    HandlerBaking hb = itE.next();
                    String name = hb.hr.wh.name;
                    for (SortableDependency sd : hb.dps) {
                        if (subHandlerMap.containsKey(sd.getKey())) {
                            if (sd.isBefore()) {
                                sortDepMap.computeIfAbsent(sd.getKey(), o -> new HashSet<>()).add(name);
                            } else {
                                sortDepMap.computeIfAbsent(name, o -> new HashSet<>()).add(sd.getKey());
                            }
                        }
                    }
                }

                WrappedHandler[] subHandlerAry = new WrappedHandler[subHandlerMap.size()];
                short size = 0;
                itE = subHandlerMap.values().iterator();
                while (itE.hasNext()) {
                    HandlerBaking hb = itE.next();
                    String name = hb.hr.wh.name;
                    if (!sortDepMap.containsKey(name) || sortDepMap.get(name).isEmpty()) {
                        removing.add(name);
                        itE.remove();
                        subHandlerAry[size++] = hb.hr.wh;
                        hb.hr.count++;
                    }
                }
                while (!removing.isEmpty()) {
                    for (String key : removing) {
                        sortDepMap.values().forEach(set -> set.remove(key));
                    }
                    removing.clear();
                    Iterator<Map.Entry<String, HashSet<String>>> itEE = sortDepMap.entrySet().iterator();
                    while (itEE.hasNext()) {
                        Map.Entry<String, HashSet<String>> ee = itEE.next();
                        if (ee.getValue().isEmpty()) {
                            removing.add(ee.getKey());
                            HandlerBaking hb = subHandlerMap.remove(ee.getKey());
                            hb.hr.count++;
                            subHandlerAry[size++] = hb.hr.wh;
                            itEE.remove();
                        }
                    }
                }

                if (size > 0) {
                    if (size < subHandlerAry.length) {
                        subHandlerAry = Arrays.copyOf(subHandlerAry, size);
                    }
                    handlerAry.put(modifierMap.get(e.getKey()).index, subHandlerAry);
                }

                reverseDepMap.clear();
            }

            handlerMap.values().removeIf(ref -> ref.count == 0);
            wrappedHandlerMap = new HashMap<>(handlerMap.size());
            for (Map.Entry<String, HandlerRef> entry : handlerMap.entrySet()) {
                wrappedHandlerMap.put(entry.getKey(), entry.getValue().wh);
            }
        }

        // create paramMap;
        int[] paramMap = new int[handlerMap.size()];
        {
            int id = 0, pSize;
            for (HandlerRef hr : handlerMap.values()) {
                pSize = hr.wh.handler.paramSize();
                if (pSize > 0) {
                    paramMap[id] = pSize;
                    hr.wh.paramIndex = id;
                    id ++;
                } else {
                    hr.wh.paramIndex = -1;
                }
            }
        }

        // set handlerSize in modifier;
        for (short i = 0; i < modifierAry.length; i++) {
            WrappedHandler[] ary = handlerAry.get(i);
            modifierAry[i].size = ary == null ? 0 : ary.length;
        }

        TaskManager tm = new TaskManager();
        StateControl stateControl = new StateControl();
        BuffManager<T> bm = new BuffManager<>(tm, guiCallback, configMap, stateControl);
        EntityManager<T> em = new EntityManager<>(tm, iec, ida, logger, stateControl, attributeAry, resourceSize, intervals, intervalIds, baseValues, autoStepZero, attributeProviderAry);
        HandlerList<T> hl = new HandlerList<>(stateControl, em, bm, handlerAry, modifierAry, wrappedHandlerMap, paramMap, resourceSize);
        bm.setup(em, hl);

        return new BakeResult<>(hl, em, tm, bm);
    }

    private static short findDependency(SimpleDependency sd, Map<String, ? extends IndexedEntry> map) {
        String k = sd.getKey();
        IndexedEntry ie = map.get(k);
        return ie == null ? (short) (sd.isRequired() ? -2 : -1) : ie.index();
    }

    private static Optional<short[]> generateDependency(SimpleDependency[] dps, Map<String, ? extends IndexedEntry> map) {
        short[] ids = new short[dps.length];
        for (int i = 0; i < dps.length; ++i) {
            SimpleDependency sd = dps[i];
            IndexedEntry ie = map.get(sd.getKey());
            if (ie == null && sd.isRequired()) return Optional.empty();
            ids[i] = ie == null ? -1 : ie.index();
        }
        return Optional.of(ids);
    }

    private static short[] sortConvertor(short[] src) {
        short[][] sorter = new short[src.length][];
        for (short i = 0; i < src.length; i++) sorter[i] = new short[]{i};
        Arrays.sort(sorter, Comparator.comparingInt(o -> src[o[0]]));
        short[] cvt = new short[src.length];
        Arrays.fill(cvt, (short) -1);
        for (short i = 0; i < src.length; i++) {
            if (sorter[i][0] != i) {
                short tmp = src[i];
                short j = i;
                do {
                    cvt[j] = sorter[j][0];
                    src[j] = src[cvt[j]];
                    sorter[j][0] = j;
                    j = cvt[j];
                } while (sorter[j][0] != i);
                src[j] = tmp;
                sorter[j][0] = j;
                cvt[j] = i;
            } else if (cvt[i] < 0) {
                cvt[i] = i;
            }
        }
        return cvt;
    }

    private interface IndexedEntry {
        short index();
    }

    private static class IndexedAttribute<T> extends WrappedAttribute<T> implements IndexedEntry {

        private short index = -1;

        public IndexedAttribute(IAttribute<T> attr, String name, byte flag) {
            super(attr, name, flag);
        }

        @Override
        public short index() {
            return index;
        }
    }

    private static class IndexedModifier extends WrappedModifier implements IndexedEntry {

        private short index;
        private short[] ids;
        private short[] sortedIds;
        private int size;

        IndexedModifier(IModifier modifier, String name) {
            super(modifier, name);
        }

        @Override
        protected short[] getIds() {
            return ids;
        }

        @Override
        protected short[] getSortedIds() {
            if (sortedIds == null) Arrays.sort(sortedIds = Arrays.copyOf(ids, ids.length));
            return sortedIds;
        }

        @Override
        protected int handlerSize() {
            return size;
        }

        @Override
        public short index() {
            return index;
        }
    }

    private static class HandlerBaking {
        private final HandlerRef hr;
        private final SortableDependency[] dps;
        private HandlerBaking(HandlerRef hr, SortableDependency[] dps) {
            this.hr = hr;
            this.dps = dps;
        }
    }

    private static class HandlerRef {
        private final WrappedHandler wh;
        private int count = 0;
        private HandlerRef(WrappedHandler wh) {
            this.wh = wh;
        }
    }

    static class BakeResult<T> {
        final HandlerList<T> handlerList;
        final EntityManager<T> entityManager;
        final TaskManager taskManager;
        final BuffManager<T> buffManager;
        private BakeResult(HandlerList<T> hl, EntityManager<T> em, TaskManager tm, BuffManager<T> bm) {
            this.handlerList = hl;
            this.entityManager = em;
            this.taskManager = tm;
            this.buffManager = bm;
        }
    }
}
