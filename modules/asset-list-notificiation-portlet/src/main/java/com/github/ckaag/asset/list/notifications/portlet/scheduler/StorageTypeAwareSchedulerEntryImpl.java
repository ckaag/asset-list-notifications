package com.github.ckaag.asset.list.notifications.portlet.scheduler;

import com.liferay.portal.kernel.scheduler.*;

public class StorageTypeAwareSchedulerEntryImpl extends SchedulerEntryImpl implements SchedulerEntry, StorageTypeAware {

    public StorageTypeAwareSchedulerEntryImpl(final SchedulerEntryImpl schedulerEntry, final StorageType storageType) {
        super(schedulerEntry.getEventListenerClass(), schedulerEntry.getTrigger(), schedulerEntry.getDescription());
        _schedulerEntry = schedulerEntry;
        _storageType = storageType;
    }

    @Override
    public String getDescription() {
        return _schedulerEntry.getDescription();
    }

    @Override
    public String getEventListenerClass() {
        return _schedulerEntry.getEventListenerClass();
    }

    @Override
    public StorageType getStorageType() {
        return _storageType;
    }

    @Override
    public Trigger getTrigger() {
        return _schedulerEntry.getTrigger();
    }

    private final SchedulerEntryImpl _schedulerEntry;
    private final StorageType _storageType;
}

