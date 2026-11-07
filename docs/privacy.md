# Privacy policy: Sổ Xe

Draft, published with the first release. Last updated: 2026-11-08.

Sổ Xe is a logbook for your own vehicles. This page lists what the app stores, where, and which
permissions it asks for. It is updated in the same change as any feature that alters it.

## Data the app stores

| Data | Where | Shared with |
| --- | --- | --- |
| Vehicles, fill-ups, maintenance items and services you enter | On your device, in the app's database | Nobody |
| Settings (selected vehicle, reminders on or off) | On your device | Nobody |
| Approximate location, only while the Nearby screen is open | Sent once per search to the Overpass API (overpass-api.de, a public OpenStreetMap service) to find places within 3 km. Kept in memory for 30 minutes to avoid repeated searches, never written to disk | The Overpass service sees the coordinates and your IP address, as with any web request |

Android Auto Backup may include this data in your device backup to your Google account, as for any
app that allows backup. You can turn backup off in your device settings.

There is no account, no advertising and no tracking in the app.

## Permissions

| Permission | Why | When it is asked |
| --- | --- | --- |
| Location (approximate or precise) | To list fuel stations and garages near you | When you tap "Allow location" on the Nearby screen |
| Notifications (Android 13 and above) | To remind you when a maintenance item is due soon or overdue | Only when you tap "Allow" in the Maintenance tab |

You can withdraw the permission in system settings, or turn reminders off in the app.

## Data safety notes (Google Play)

- Data collected: approximate location, used for the Nearby feature, processed on the device and sent
  to the public Overpass service for the search. Not stored on a server by the app. Not linked to an
  identity: there is no account.
- Data shared: the coordinates of a search with the Overpass service, to answer it.
- Encryption in transit: yes, HTTPS.
- Deletion: deleting a vehicle deletes its data; uninstalling the app deletes everything on the
  device.

## Contact

Questions: open an issue at https://github.com/diiafyra/motoman-android/issues.
