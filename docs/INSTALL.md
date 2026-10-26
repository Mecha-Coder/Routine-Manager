# Install Routine Manager

Routine Manager isn't in the Play Store. You build it on a Mac and install it on the phone over a cable or Wi-Fi with `adb`.

**Never uninstall the app.** All your lists, your timetable and your settings live only on the phone, with no backup. Uninstalling deletes them. Updates are installed over the old version (step 5), which keeps everything.

## What you need

- A Mac with Android Studio. It brings Java and `adb`.
- In Terminal, `adb version` and `java -version` both print a version. If not:
  - set `JAVA_HOME` to Android Studio's Java: `/Applications/Android Studio.app/Contents/jbr/Contents/Home`;
  - add `~/Library/Android/sdk/platform-tools` (where `adb` lives) to your `PATH`.
- The phone with Developer options and USB debugging (or Wireless debugging) on. `adb devices` lists it as `device`.
- A copy of this repository. The commands below start in its folder (`Routine-Manager`).

## 1. Create the signing key (once)

Android installs an update only if it is signed with the same key as the app already on the phone. So you make one key, keep it for good, and sign every version with it.

**a. Make the key.** It goes in the project folder as `routine-manager-release.jks`. Git ignores every `.jks` file, so it is never committed. From the `Routine-Manager` folder, paste this into Terminal:

```sh
"/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin/keytool" -genkeypair -v -keystore routine-manager-release.jks -alias routine-manager -keyalg RSA -keysize 4096 -validity 10000 -dname "CN=Routine Manager"
```

It asks for a password twice. Nothing shows while you type; that's normal. Use letters and numbers only (other characters can break step b), at least 12 of them. Write the password down now (step 2 says where).

**b. Tell the build where the key is.** From the `Routine-Manager` folder, paste this and type the same password when it asks:

```sh
read -rsp "Key password: " P && printf 'storeFile=%s\nstorePassword=%s\nkeyAlias=routine-manager\nkeyPassword=%s\n' "$PWD/routine-manager-release.jks" "$P" "$P" > routine-manager/keystore.properties && unset P && echo && echo "Saved."
```

This is for bash. In zsh (the Mac's default shell), start with `read -rs "P?Key password: "` instead of `read -rsp "Key password: " P`.

This writes `routine-manager/keystore.properties`. It looks like this, with the project folder's path and your password:

```properties
storeFile=/Users/<you>/<folder>/Routine-Manager/routine-manager-release.jks
storePassword=<your password>
keyAlias=routine-manager
keyPassword=<your password>
```

Both passwords are the same: this kind of key has only one. Git ignores this file and every `.jks` file, so neither is committed. Never commit them, and never paste the password into a chat, an issue or a commit.

## 2. Back up the key (once)

Back up two things: the key file `routine-manager-release.jks` and its password.

- **The key file:** this project's owner keeps a password-protected zip of it, `routine-manager-backup.zip`, in this repository (the owner's choice). A zip password is weak protection and the repository is public, so also keep a private copy of the `.jks` file somewhere only you can reach, for example a USB stick kept at home or your own cloud drive.
- **The passwords** (the key's, and the zip's if you make one): keep them in a password manager (or on paper in a safe place), **not** in the same place as the key file.

Why:

- If the key or the password is lost, the next version can't be installed over the old one. The only way left is to uninstall, which deletes all your data.
- One copy isn't enough: it dies with the Mac or the USB stick.
- Kept apart, someone who finds the key file still can't sign an app that replaces yours without the password.

`keystore.properties` doesn't need a backup: with the key file back in the project folder and the password, step 1b makes it again.

## 3. Build

```sh
cd routine-manager
./gradlew assembleRelease
```

The app is `routine-manager/app/build/outputs/apk/release/app-release.apk`.

If the build stops with "Release signing isn't set up", `keystore.properties` is missing: do step 1b. If it says the key file is not found, put `routine-manager-release.jks` back in the project folder from your backup. If it says the password is wrong, do step 1b again.

## 4. Install (first time)

Still in `routine-manager`:

```sh
adb install app/build/outputs/apk/release/app-release.apk
```

The phone now has **Routine Manager**. On its first start it shows the sample routine and lists; change them to your own.

A test version, "Routine Manager (debug)", can sit beside it. It is a separate app with its own data and never touches the real one.

If the phone blocks the install, turn off Auto Blocker (Settings → Security and privacy → Auto Blocker), install, then turn it back on. Leave it on otherwise.

## 5. Update to a new version

Get the new code, build (step 3), then:

```sh
adb install -r app/build/outputs/apk/release/app-release.apk
```

`-r` replaces the app and keeps all its data.

- **Never uninstall** the app first, on the phone or with `adb uninstall`: that deletes every list, the timetable and the settings.
- If `adb` says `INSTALL_FAILED_UPDATE_INCOMPATIBLE`, the new APK was signed with a different key. Don't uninstall. Restore the original key from your backup (step 2), redo step 1b, and build again.

## 6. Recommended phone settings

- **Motion smoothness: Standard** (Settings → Display → Motion smoothness). 60 Hz is enough for this app and uses less power on a screen that stays on all day.
- **Battery protection on** (Settings → Battery → Battery protection; "Protect battery" on older versions). The phone sits on the charger all day; this stops it from staying at 100 %, which wears the battery.
- **Auto Blocker:** leave it on. Turn it off only if an install is blocked (step 4).
