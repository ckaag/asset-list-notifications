# Asset List Notifications

## What is this?
A way to subscribe to dynamic Asset Lists, making Asset Publishers unnecessary for such a usage. It also includes a fully customizable HTML template for the mail sent out to users. 


## What do I need?
This is built for Liferay 7.2.1. Currently it has not been tested for 7.3 or 7.4. 

## How to configure this?
Place the portlet on any page. Configure it and select an Asset List / Content Set on the same site (this only makes sense with dynamic Content Sets!). Also configure a mail template for your users to enjoy. Once saved, the process will listen at most once per minute for new entries, and send a notification & mail to every site member.
Please never use more than one portlet instance per underlying Content Set. Configuration may be in conflict otherwise.

The portlet itself is a button to subscribe / unsubscribe. If you don't want people to do so, make it invisible (e.g. give it the css class 'd-none' in Look&Feel) or place it on a hidden page. It needs to be placed somewhere to work. Configuration is done at that portlet instance.


## How does it work?
It routinely checks new entries to the Content Set, and looks up for all entries that were modified last after a given date, stored itself as a CustomField / ExpandoValue on the underlying AssetListEntry. Once it has found at least one such entry, it will use Liferay's API to send a mail and a notification to each target user. Afterwards, it sets the Custom Field value to be the current date, so the next check will go from there.