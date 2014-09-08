Sugarcast
=======
A highly configurable Android framework for continous glucose monitors (CGM) that allows the user to upload or remotely monitor values captured by the CGM
This application is still in early phases of development and requires some development experience to get started.

Features
=======
*Nightscout compatability via the API or direct mongo upload
*Remote monitoring and alerting for multiple devices
*Configurable thresholds for alerting
*Push notifications using MQTT
*Pebble support with a companion app
*Quick actions to call/text the PWD from the notification shade.

DISCLAIMER
=======
All information, thought, and code described here is intended for informational and educational purposes only. Use of code from github.com is without warranty or support of any kind.
Each component of the system can fail at any time rendering the system unusable. There is no password protected privacy or security provided by these tools; all data you upload can be available for anyone on the internet to read if they have your specific URL to view your data. Please review the LICENSE found within each repository for further details. This is not a project affiliated with Dexcom.
Do not use any of the Sugarcast information or code to make medical decisions.

Getting started
=======
The basic concept of the uploader functionality is the same as the android uploader for Nightscout.

Download the repo, import the project into Android studio, compile and install on the uploader and any remote monitors you wish to use. (more information this to come later)

This application is designed to complement and enhance the Nightscout suite of tools as a remote monitor using push notifications.

The push notification functionality relies on MQTT. You can roll your own using any MQTT server or use a cloud service like http://www.cloudmqtt.com/.

With Sugarcast there are "devices" and "monitors." A "device" can be a physical CGM like a Dexcom G4 or it can be a remote device.
A "monitor" provides an action for the data that is received by the device. Current monitors:
*The Mongo Uploader monitor uploads the data to MongoDB using the Nightscout format.
*The Nightscout API monitor uploads the data to Nightscout using the web service
*The Pebble monitor acts as a companion app when the Pebble is connected and the compatible watchface is installed and active on the Pebble
*The Android notification monitor uses the notification shade to quickly give you an idea of where the PWD is - The icon is one of 3 colors: Yellow for high, Green for in range, and Red for low. The icon also has an arrow that indicates the direction that BG value is trending. The text of the notification contains detailed information on reading value, trending, state of the uploader, state of the CGM, etc. It is also possible to associate a contact number with the device. If a contact is associated to a device the notification will contain 2 actions by default - call and text.

Configuring the uploader for push notifications
=======
If you've made it this far it is assumed that you are familiar with how to physically connect the uploader - if not, Nightscout.info has a good deal of documentation on the topic at http://www.nightscout.info/wiki/welcome/basic-requirements

Have an active MQTT service listening on a remote server
Go into the application settings and select "Device 1".
Enable the device
Set the display name
Select Dexcom G4 as the "Type of device"
Change thresholds if necessary
Select the G4 options you wish to use.
Enable Android notifications (recommended but not required for now)
Select "Push notification settings" and select "Enable Push upload"
Add the MQTT endpoint (I recommend using ssl:// over tcp:// for more security)
Add the MQTT user and password in the provided fields.
Physically connect the CGM to the uploader
Back out to the main activity, open the hamburger/navigation drawer and select "Start".

You should see the reading on the main screen shortly after hitting start.

Configuring a remote monitor with push notifications
=======
Go through the "Configuring the uploader for push notifications" step - without the remote monitor will not receive data
Go into the application settings and select "Device 1".
Enable the device
Set the display name (optional - this field will be overwritten by the value from the remote device)
Select Remote monitor as the "Type of device"
Change thresholds if necessary
Select "Push notification settings" and select "Enable Push upload"
Add the MQTT endpoint (again I recommend using ssl:// over tcp:// for more security)
Add the MQTT user and password in the provided fields.
Back out to the main activity, open the hamburger/navigation drawer and select "Start".

Configuring uploader with Nightscout
=======
Have Nightscout set up.
Open Sugarcast. Go to Settings and select the device that is associated with the uploader
Scroll down to the "Monitors" section
Select "Mongo Settings" and check "Enable direct mongo upload"
Set the Mongo DB URI (currently mislabled as "Mongo DB for Remote devices")
Set your collection, backout to the main menu and hit start
