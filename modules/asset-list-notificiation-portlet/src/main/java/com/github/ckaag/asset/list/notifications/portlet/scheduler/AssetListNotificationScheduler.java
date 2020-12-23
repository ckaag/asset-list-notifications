package com.github.ckaag.asset.list.notifications.portlet.scheduler;

import com.github.ckaag.asset.list.notifications.portlet.service.ListNotificationSender;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.messaging.BaseMessageListener;
import com.liferay.portal.kernel.messaging.DestinationNames;
import com.liferay.portal.kernel.messaging.Message;
import com.liferay.portal.kernel.module.framework.ModuleServiceLifecycle;
import com.liferay.portal.kernel.scheduler.*;
import com.liferay.portal.kernel.service.CompanyLocalService;
import com.liferay.portal.kernel.service.ContactLocalService;
import com.liferay.portal.kernel.service.UserLocalService;
import com.liferay.portal.kernel.service.UserNotificationEventLocalService;
import com.liferay.portal.kernel.util.GetterUtil;
import org.osgi.service.component.annotations.*;

import java.util.Date;
import java.util.Map;

@Component(
        immediate = true, property = {"cron.expression=" + AssetListNotificationScheduler._DEFAULT_CRON_EXPRESSION},
        service = AssetListNotificationScheduler.class
)
public class AssetListNotificationScheduler extends BaseMessageListener {

    @Override
    protected void doReceive(Message message) throws Exception {
        _log.info("Scheduled task executed...");
        listNotificationSender.tickScheduler();
    }

    @Reference
    private ListNotificationSender listNotificationSender;

    @Reference
    private ContactLocalService contactLocalService;

    @Reference
    private UserLocalService userLocalService;

    @Reference
    private CompanyLocalService companyLocalService;

    @Reference
    private UserNotificationEventLocalService userNotificationEventLocalService;

    @Activate
    @Modified
    protected void activate(Map<String, Object> properties) throws SchedulerException {
        String cronExpression = GetterUtil.getString(properties.get("cron.expression"), _DEFAULT_CRON_EXPRESSION);
        String listenerClass = getClass().getName();
        Trigger jobTrigger = _triggerFactory.createTrigger(listenerClass, listenerClass, new Date(), null, cronExpression);
        _schedulerEntryImpl = new SchedulerEntryImpl(getClass().getName(), jobTrigger);
        _schedulerEntryImpl = new StorageTypeAwareSchedulerEntryImpl(_schedulerEntryImpl, StorageType.MEMORY);
        if (_initialized) {
            deactivate();
        }
        _schedulerEngineHelper.register(this, _schedulerEntryImpl, DestinationNames.SCHEDULER_DISPATCH);
        _initialized = true;
    }

    @Deactivate
    protected void deactivate() {
        if (_initialized) {
            try {
                _schedulerEngineHelper.unschedule(_schedulerEntryImpl, getStorageType());
            } catch (SchedulerException se) {
                if (_log.isWarnEnabled()) {
                    _log.warn("Unable to unschedule trigger", se);
                }
            }
            _schedulerEngineHelper.unregister(this);
        }
        _initialized = false;
    }

    protected StorageType getStorageType() {
        if (_schedulerEntryImpl instanceof StorageTypeAware) {
            return ((StorageTypeAware) _schedulerEntryImpl).getStorageType();
        }

        return StorageType.MEMORY_CLUSTERED;
    }

    @Reference(target = ModuleServiceLifecycle.PORTAL_INITIALIZED, unbind = "-")
    protected void setModuleServiceLifecycle(@SuppressWarnings("unused") ModuleServiceLifecycle moduleServiceLifecycle) {
    }

    @Reference(unbind = "-")
    protected void setTriggerFactory(TriggerFactory triggerFactory) {
        _triggerFactory = triggerFactory;
    }

    @Reference(unbind = "-")
    protected void setSchedulerEngineHelper(SchedulerEngineHelper schedulerEngineHelper) {
        _schedulerEngineHelper = schedulerEngineHelper;
    }

    static final String _DEFAULT_CRON_EXPRESSION = "0 * * * * ?";

    private static final Log _log = LogFactoryUtil.getLog(AssetListNotificationScheduler.class);

    private volatile boolean _initialized;
    private TriggerFactory _triggerFactory;
    private SchedulerEngineHelper _schedulerEngineHelper;
    private SchedulerEntryImpl _schedulerEntryImpl = null;
}
